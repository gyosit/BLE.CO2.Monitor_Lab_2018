package com.example.shout.bluetoothtest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ManageDB extends SQLiteOpenHelper {
    private static final String SQL_CREATE_ENTRY =
            "CREATE TABLE " + "esplist" + " (" +
            "_id" + " INTEGER PRIMARY KEY," +
            "name" + " TEXT," +
            "adress" + " TEXT," +
            "uuid" + " TEXT," +
            "charauuid" + " TEXT)";
    private static final String SQL_DELETE_ENTRY =
            "DROP TABLE IF EXISTS " + "esplist";

    public ManageDB(Context c, String dbname, int version){
        super(c, dbname, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRY);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_ENTRY);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }

    public void saveData(SQLiteDatabase db, String name, String adress, String uuid, String charauuid){
        //データ挿入
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("adress", adress);
        values.put("uuid", uuid);
        values.put("charauuid", charauuid);

        db.insert("esplist", null, values);
    }

    public Cursor readAll(SQLiteDatabase db){
        //全データ取得
        Cursor cursor = db.query("esplist",
                new String[]{"name","adress","uuid","charauuid"},
                null,null,null,null,null);
        return cursor;
    }

    public void deleteData(SQLiteDatabase db, String adress){
        //アドレス指定のデータ削除
        try {
            db.delete("esplist", "adress=\"" + adress + "\"", null);
        }catch(Exception e){
            Log.e("SQLERROR:", e.toString());
        }
    }

    public void allDelete(SQLiteDatabase db){
        db.execSQL(SQL_DELETE_ENTRY);
    }
}
