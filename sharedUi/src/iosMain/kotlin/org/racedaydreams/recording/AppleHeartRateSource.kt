@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.recording

import kotlinx.cinterop.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSThread
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/** Optional Bluetooth SIG Heart Rate Service. Construct only after the athlete chooses to scan. */
internal class AppleHeartRateSource(private val onValue: (Double?) -> Unit) :
    NSObject(), CBCentralManagerDelegateProtocol, CBPeripheralDelegateProtocol {
    private val service = CBUUID.UUIDWithString("180D")
    private val measurement = CBUUID.UUIDWithString("2A37")
    private val peripherals = mutableMapOf<String, CBPeripheral>()
    private val mutableDevices = MutableStateFlow<Map<String, String>>(emptyMap())
    val devices = mutableDevices.asStateFlow()
    private var scanRequested = false
    private var selected: CBPeripheral? = null
    private val central = CBCentralManager(this, dispatch_get_main_queue())

    fun scan() {
        check(NSThread.isMainThread)
        stop()
        peripherals.clear()
        mutableDevices.value = emptyMap()
        scanRequested = true
        scanIfReady()
    }

    fun stopScan() {
        scanRequested = false
        central.stopScan()
    }

    fun connect(id: String) {
        check(NSThread.isMainThread)
        val device = peripherals[id] ?: return
        stop()
        selected = device
        device.delegate = this
        central.connectPeripheral(device, options = null)
    }

    fun stop() {
        check(NSThread.isMainThread)
        stopScan()
        val device = selected
        selected = null
        if (device != null) {
            device.delegate = null
            central.cancelPeripheralConnection(device)
        }
        onValue(null)
    }

    private fun scanIfReady() {
        if (scanRequested && central.state == CBManagerStatePoweredOn) {
            central.scanForPeripheralsWithServices(listOf(service), options = null)
        }
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state == CBManagerStatePoweredOn) scanIfReady() else onValue(null)
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: platform.Foundation.NSNumber,
    ) {
        if (!scanRequested) return
        val id = didDiscoverPeripheral.identifier.UUIDString
        peripherals[id] = didDiscoverPeripheral
        mutableDevices.value =
            mutableDevices.value + (id to (didDiscoverPeripheral.name ?: "Heart-rate monitor"))
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        if (selected === didConnectPeripheral)
            didConnectPeripheral.discoverServices(listOf(service))
    }

    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?,
    ) {
        if (selected === didDisconnectPeripheral) {
            selected = null
            didDisconnectPeripheral.delegate = null
            onValue(null)
        }
    }

    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        if (selected !== peripheral || didDiscoverServices != null) return
        peripheral.services
            .orEmpty()
            .filterIsInstance<CBService>()
            .firstOrNull { it.UUID == service }
            ?.let { peripheral.discoverCharacteristics(listOf(measurement), it) }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?,
    ) {
        if (selected !== peripheral || error != null) return
        didDiscoverCharacteristicsForService.characteristics
            .orEmpty()
            .filterIsInstance<CBCharacteristic>()
            .firstOrNull { it.UUID == measurement }
            ?.let { peripheral.setNotifyValue(true, it) }
    }

    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?,
    ) {
        if (
            selected !== peripheral ||
                error != null ||
                didUpdateValueForCharacteristic.UUID != measurement
        )
            return
        val data = didUpdateValueForCharacteristic.value ?: return
        val size = minOf(data.length.toLong(), 3L).toInt()
        if (size < 2) return
        val bytes = data.bytes?.readBytes(size) ?: return
        val low = bytes[1].toInt() and 255
        val bpm =
            if ((bytes[0].toInt() and 1) == 0) low
            else if (size == 3) low or ((bytes[2].toInt() and 255) shl 8) else return
        if (bpm in 25..250) onValue(bpm.toDouble())
    }
}
