package org.racedaydreams.recording

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

interface HeartRateSource {fun connect(address:String,onValue:(Double?)->Unit,onConnection:(Boolean)->Unit);fun stop()}
/** Standard Bluetooth SIG Heart Rate profile only. Never logs device IDs or measurements. */
@SuppressLint("MissingPermission")
class BleHeartRateSource(private val context:Context):HeartRateSource {
    private val adapter=context.getSystemService(BluetoothManager::class.java)?.adapter
    private var gatt:BluetoothGatt?=null;private var stopped=false;private var retries=0
    private val handler=Handler(Looper.getMainLooper());private var sink:((Double?)->Unit)?=null;private var state:((Boolean)->Unit)?=null
    companion object {val devices=MutableStateFlow<Map<String,String>>(emptyMap());val HR_SERVICE:UUID=UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");val HR_VALUE:UUID=UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");val CCC:UUID=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")}
    private var scanner:BluetoothLeScanner?=null
    private val scan=object:ScanCallback(){override fun onScanResult(callbackType:Int,result:ScanResult){val d=result.device;devices.value=devices.value+(d.address to (result.scanRecord?.deviceName?:d.name?:"Heart-rate monitor"))}}
    fun scan(){devices.value=emptyMap();scanner=adapter?.bluetoothLeScanner;scanner?.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_SERVICE)).build()),ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(),scan);handler.postDelayed({scanner?.stopScan(scan)},15000)}
    override fun connect(address:String,onValue:(Double?)->Unit,onConnection:(Boolean)->Unit){stopped=false;sink=onValue;state=onConnection;gatt=adapter?.getRemoteDevice(address)?.connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE)}
    private val callback=object:BluetoothGattCallback(){
        override fun onConnectionStateChange(g:BluetoothGatt,status:Int,newState:Int){handler.post{if(stopped)return@post;if(newState==BluetoothProfile.STATE_CONNECTED){retries=0;g.discoverServices()}else if(newState==BluetoothProfile.STATE_DISCONNECTED){sink?.invoke(null);state?.invoke(false);if(retries++<3)handler.postDelayed({if(!stopped)g.connect()},10000)}}}
        override fun onServicesDiscovered(g:BluetoothGatt,status:Int){if(status!=BluetoothGatt.GATT_SUCCESS)return;val c=g.getService(HR_SERVICE)?.getCharacteristic(HR_VALUE)?:return;g.setCharacteristicNotification(c,true);val d=c.getDescriptor(CCC)?:return;d.value=BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;g.writeDescriptor(d)}
        override fun onDescriptorWrite(g:BluetoothGatt,d:BluetoothGattDescriptor,status:Int){if(d.uuid==CCC&&status==BluetoothGatt.GATT_SUCCESS)handler.post{if(!stopped)state?.invoke(true)}}
        @Deprecated("Legacy callback for API 28") override fun onCharacteristicChanged(g:BluetoothGatt,c:BluetoothGattCharacteristic){if(c.uuid==HR_VALUE)consume(c.value)}
        override fun onCharacteristicChanged(g:BluetoothGatt,c:BluetoothGattCharacteristic,value:ByteArray){if(c.uuid==HR_VALUE)consume(value)}
    }
    private fun consume(value:ByteArray?){if(value==null||value.size<2)return;val flags=value[0].toInt()and 255;val bpm=if(flags and 1==0)value[1].toInt()and 255 else if(value.size>=3)(value[1].toInt()and 255)or((value[2].toInt()and 255)shl 8)else return;if(bpm in 25..250)handler.post{if(!stopped)sink?.invoke(bpm.toDouble())}}
    override fun stop(){stopped=true;handler.removeCallbacksAndMessages(null);scanner?.stopScan(scan);gatt?.disconnect();gatt?.close();gatt=null;sink=null;state=null}
}
