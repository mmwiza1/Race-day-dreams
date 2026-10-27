package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.racedaydreams.domain.activity.Shoe;
import org.racedaydreams.domain.activity.ShoeRepository;
import java.util.ArrayList;
import java.util.List;

/** SQLite adapter for shoe data. Shares the application database and its transaction boundary. */
public final class SqliteShoeRepository extends SqliteStoreAccess implements ShoeRepository {
    public SqliteShoeRepository(Context context) { super(context); }
    public List<Shoe> shoes() {
        List<Shoe> out=new ArrayList<>();
        try(Cursor c=read().rawQuery("SELECT payload,is_default FROM shoes ORDER BY is_default DESC,id",null)) {while(c.moveToNext()){Shoe s=FeatureJson.shoe(FeatureJson.object(c.getString(0)));out.add(new Shoe(s.id(),s.brand(),s.model(),s.purchaseDate(),s.startingDistanceMetres(),s.retired(),s.notes(),c.getInt(1)==1));}}
        return out;
    }
    public void saveShoe(Shoe shoe) {
        SQLiteDatabase db=write();db.beginTransaction();try{putShoe(db,shoe);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    static void putShoe(SQLiteDatabase db,Shoe shoe) {
        required(shoe.id(),"shoe ID");
        if(shoe.defaultShoe()){ContentValues unset=new ContentValues();unset.put("is_default",0);db.update("shoes",unset,null,null);}
        ContentValues v=values("id",shoe.id());v.put("payload",FeatureJson.shoe(shoe).toString());v.put("is_default",shoe.defaultShoe()?1:0);
        // UPDATE preserves foreign-key shoe assignments; REPLACE would delete the old row.
        if(db.update("shoes",v,"id=?",new String[]{shoe.id()})==0)db.insertOrThrow("shoes",null,v);
    }

}
