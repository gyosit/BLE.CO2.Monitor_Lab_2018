package com.example.shout.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ResidentActivity extends AppCompatActivity {


    // 定数
    private static final int    REQUEST_ENABLEBLUETOOTH = 1; // Bluetooth機能の有効化要求時の識別コード
    private static final int    REQUEST_CONNECTDEVICE   = 2; // デバイス接続要求時の識別コード

    //DB
    private ManageDB managedb;
    private SQLiteDatabase db;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resident);
        managedb = new ManageDB(getApplicationContext(),"esplist",1);
        db = managedb.getReadableDatabase();

        //GUI
        final TextView devicename = (TextView)findViewById(R.id.textview_devicename);
        final TextView deviceadress = (TextView)findViewById(R.id.textview_deviceaddress);
        final EditText service_uuid = (EditText)findViewById(R.id.service_uuid);
        final EditText chara_uuid = (EditText)findViewById(R.id.character_uuid);
        Button residentbtn = (Button)findViewById(R.id.residentbtn);
        residentbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uuids = devicename.getText().toString()+","+deviceadress.getText().toString()+","+service_uuid.getText().toString().trim()+","+chara_uuid.getText().toString().trim();
                managedb.saveData(db, devicename.getText().toString(), deviceadress.getText().toString(), service_uuid.getText().toString().trim(),chara_uuid.getText().toString().trim());
                /*try{
                    OutputStream out = openFileOutput("resister.txt", MODE_APPEND);
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.append(uuids+"\n");
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }*/
            }
        });
    }

    // オプションメニュー作成時の処理
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.activity_resident, menu );
        return true;
    }

    // オプションメニューのアイテム選択時の処理
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuitem_search:
                Intent devicelistactivityIntent = new Intent( this, DeviceListActivity.class );
                startActivityForResult( devicelistactivityIntent, REQUEST_CONNECTDEVICE );
                return true;
        }
        return false;
    }

    // 機能の有効化ダイアログの操作結果(onResumeの前に呼ばれる)
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        switch( requestCode )
        {
            case REQUEST_ENABLEBLUETOOTH: // Bluetooth有効化要求
                if( Activity.RESULT_CANCELED == resultCode )
                {    // 有効にされなかった
                    Toast.makeText( this, "DISABLE", Toast.LENGTH_SHORT ).show();
                    finish();    // アプリ終了宣言
                    return;
                }
                break;
            case REQUEST_CONNECTDEVICE: // デバイス接続要求
                String strDeviceName;
                String mDeviceAddress;
                if( Activity.RESULT_OK == resultCode )
                {
                    // デバイスリストアクティビティからの情報の取得
                    strDeviceName = data.getStringExtra( DeviceListActivity.EXTRAS_DEVICE_NAME );
                    mDeviceAddress = data.getStringExtra( DeviceListActivity.EXTRAS_DEVICE_ADDRESS );
                }
                else
                {
                    strDeviceName = "";
                    mDeviceAddress = "";
                }
                ( (TextView)findViewById( R.id.textview_devicename ) ).setText( strDeviceName );
                ( (TextView)findViewById( R.id.textview_deviceaddress ) ).setText( mDeviceAddress );
                //( (TextView)findViewById( R.id.textview_readchara1 ) ).setText( "" );
                //( (TextView)findViewById( R.id.textview_readchara2 ) ).setText( "" );
                break;
                default:
        }
        super.onActivityResult( requestCode, resultCode, data );
    }
}
