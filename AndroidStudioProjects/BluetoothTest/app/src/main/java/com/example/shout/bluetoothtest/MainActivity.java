/*
 * Copyright (C) 2013 Philipp Jahoda
 *      https://github.com/PhilJay/MPAndroidChart
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.shout.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

//グラフ

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //ListView
    static class DeviceSet {
        String deviceName;
        String deviceAdress;
        String serviceUUID;
        String charaUUID;
        boolean swi;
    }

    //DB
    private static ManageDB managedb;
    private static SQLiteDatabase db;

    static class DeviceListAdapter extends BaseAdapter {


        static class ViewHolder {
            TextView devicename;
            TextView deviceadress;
            TextView serviceUUID;
            TextView charaUUID;
            Switch toggle;
            Button list_delete;
        }

        private ArrayList<DeviceSet> mDeviceList;
        private LayoutInflater mInflator;

        public DeviceListAdapter(Activity activity) {
            super();
            mDeviceList = new ArrayList<DeviceSet>();
            mInflator = activity.getLayoutInflater();
        }

        // リストへの追加
        public void addDevice(DeviceSet device) {
            if (!mDeviceList.contains(device)) {    // 加えられていなければ加える
                mDeviceList.add(device);
                notifyDataSetChanged();    // ListViewの更新
            }
        }

        //リストの削除
        public void deleteDevice(int position) {
            mDeviceList.remove(position);
            notifyDataSetChanged();
        }

        // リストのクリア
        public void clear() {
            mDeviceList.clear();
            notifyDataSetChanged();    // ListViewの更新
        }

        //リストの更新
        public void refresh() {
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;

            // General ListView optimization code.
            if (null == convertView) {
                convertView = mInflator.inflate(R.layout.listitem_uuid, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.devicename = (TextView) convertView.findViewById(R.id.residented_devicename);
                viewHolder.deviceadress = (TextView) convertView.findViewById(R.id.residented_deviceadress);
                viewHolder.serviceUUID = (TextView) convertView.findViewById(R.id.residented_service);
                viewHolder.charaUUID = (TextView) convertView.findViewById(R.id.residented_chara);
                viewHolder.toggle = (Switch) convertView.findViewById(R.id.toggle);
                viewHolder.list_delete = (Button) convertView.findViewById(R.id.list_btn_delete);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            DeviceSet device = mDeviceList.get(position); //作成中の階層の中身を取得
            String deviceName = device.deviceName;
            String deviceAdress = device.deviceAdress;
            String serviceUUID = device.serviceUUID;
            String charaUUID = device.charaUUID;
            boolean swi = device.swi;

            viewHolder.devicename.setText(deviceName);
            viewHolder.deviceadress.setText(deviceAdress);
            viewHolder.serviceUUID.setText(serviceUUID);
            viewHolder.charaUUID.setText(charaUUID);
            viewHolder.toggle.setChecked(swi);

            viewHolder.list_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    managedb.deleteData(db, viewHolder.deviceadress.getText().toString());
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }
    }

    // 定数（Bluetooth LE Gatt UUID）
    // Private Service
    private static UUID UUID_SERVICE_PRIVATE = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static UUID UUID_CHARACTERISTIC_PRIVATE1 = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9");
    private static UUID UUID_CHARACTERISTIC_PRIVATE2 = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    // 定数
    private static final int REQUEST_ENABLEBLUETOOTH = 1; // Bluetooth機能の有効化要求時の識別コード
    private static final int REQUEST_CONNECTDEVICE = 2; // デバイス接続要求時の識別コード
    private DeviceListAdapter mDeviceListAdapter;    // リストビューの内容

    // メンバー変数
    private BluetoothAdapter mBluetoothAdapter;    // BluetoothAdapter : Bluetooth処理で必要
    private String mDeviceAddress = "";    // デバイスアドレス
    private BluetoothGatt mBluetoothGatt = null;
    private Map<String, BluetoothGatt> connectedDeviceMap;    // Gattサービスの検索、キャラスタリスティックの読み書き
    private Map<String, DeviceSet> adressList; //アドレスとデバイスの対応
    private static int init = 0;

    // GUIアイテム
    private Button mButton_Disconnect;    // 切断ボタン
    private Button mButton_ReadChara2;    // キャラクタリスティック２の読み込みボタン
    private Button mButton_Delete;
    private Button mButton_Download; //ダウンロードボタン
    private EditText mEditText;
    private EditText mSheet, mHour, mMinute, mInterval;
    private ListView listView;
    private Button mListDelete;
    private static LineChart mChart;

    private HttpGet task;

    //データロガー用
    private String sheetName;
    private ArrayList<String> ppm;
    private boolean stopflag = true; //true:今はOFF false:今はON
    ArrayList<DeviceSet> onDevices;

    //タイマー
    int delay = 1000;
    final Handler handler = new Handler();
    final Runnable r = new Runnable() {
        @Override
        public void run() {
            String vals = "";
            for (String monoval : ppm) {
                vals += monoval + ",";
            }
            vals = vals.substring(0, vals.length() - 1);
            String param0 = "https://script.google.com/macros/s/AKfycbzp-EMcDSIrz2QfssZialXWf5q7w2uEpb50OFr5yJjm/dev?"
                    + "target=" + sheetName
                    + "&val=" + vals;
            OutputStream out;
            try {
                DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date(System.currentTimeMillis());
                String now = df.format(date);
                File file = new File(getExternalFilesDir(null), sheetName+".csv");
                FileOutputStream outputStream = new FileOutputStream(file, true);
                outputStream.write((now+","+vals+"\n").getBytes());
                outputStream.close();
                /*out = openFileOutput(sheetName+".csv", MODE_APPEND);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.append(now + "," + vals + "\n");
                writer.close();*/
            } catch (IOException e) {
                e.printStackTrace();
            }
            setData();

            if (param0.length() != 0 && ppm != null) {
                // 非同期処理を開始
                task = new HttpGet();
                // Listenerを設定
                task.setListener(createListener());
                task.execute(param0);
            }

            for (DeviceSet device : onDevices) {
                readCharacteristic(UUID.fromString(device.serviceUUID), UUID.fromString(device.charaUUID));
            }
            handler.postDelayed(this, delay);
        }
    };

    //DB
    //private ManageDB managedb;
    //private SQLiteDatabase db;

    //設定変更監視
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.i("changed", "on");
        }

        @Override
        public void afterTextChanged(Editable s) {
            Log.i("changed", "after");
            //一つ以上のデバイスと接続しており、必要なデータが記入されている
            if (connectedDeviceMap.size() > 0 && !mSheet.getText().toString().equals("") && !mHour.getText().toString().equals("") && !mMinute.getText().toString().equals("") && !mInterval.getText().toString().equals(""))
                mButton_ReadChara2.setEnabled(true);
            else
                mButton_ReadChara2.setEnabled(false);
        }
    };

    // BluetoothGattコールバック
    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        // 接続状態変更（connectGatt()の結果として呼ばれる。）
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice device = gatt.getDevice();
            String adress = device.getAddress();

            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }

            if (BluetoothProfile.STATE_CONNECTED == newState) {    // 接続完了
                if (!connectedDeviceMap.containsKey(adress)) connectedDeviceMap.put(adress, gatt);
                connectedDeviceMap.get(adress).discoverServices();    // サービス検索
                runOnUiThread(new Runnable() {
                    public void run() {
                        // GUIアイテムの有効無効の設定
                        // 切断ボタンを有効にする
                        mButton_Disconnect.setEnabled(true);
                    }
                });
                return;
            }
            if (BluetoothProfile.STATE_DISCONNECTED == newState) {    // 切断完了（接続可能範囲から外れて切断された）
                // 接続可能範囲に入ったら自動接続するために、mBluetoothGatt.connect()を呼び出す。
                connectedDeviceMap.get(adress).connect();
                runOnUiThread(new Runnable() {
                    public void run() {
                        // GUIアイテムの有効無効の設定
                        // 読み込みボタンを無効にする（通知チェックボックスはチェック状態を維持。通知ONで切断した場合、再接続時に通知は再開するので）
                        //mButton_ReadChara1.setEnabled( false );
                        mButton_ReadChara2.setEnabled(false);
                    }
                });
                return;
            }
        }

        // サービス検索が完了したときの処理（mBluetoothGatt.discoverServices()の結果として呼ばれる。）
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }

            // 発見されたサービスのループ
            for (BluetoothGattService service : gatt.getServices()) {
                // サービスごとに個別の処理
                if ((null == service) || (null == service.getUuid())) {
                    continue;
                }
                String serviceUUID = service.getUuid().toString();
                //Log.i("tmp",""+UUID_SERVICE_PRIVATE.toString());
                //Log.i("SUUID", service.getUuid().toString());
                if (UUID_SERVICE_PRIVATE.equals(service.getUuid())) {    // プライベートサービス
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // GUIアイテムの有効無効の設定
                            //mButton_ReadChara1.setEnabled( true );
                            if (connectedDeviceMap.size() > 0 && !mSheet.getText().toString().equals("") && !mHour.getText().toString().equals("") && !mMinute.getText().toString().equals("") && !mInterval.getText().toString().equals("")) {
                                mButton_ReadChara2.setEnabled(true);
                            } else
                                mButton_ReadChara2.setEnabled(false);
                        }
                    });
                    continue;
                }
            }
        }

        // キャラクタリスティックが読み込まれたときの処理
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            Log.i("UUID", characteristic.getUuid().toString());
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }
            final String strChara = characteristic.getStringValue(0);

            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceSet device = adressList.get(characteristic.getUuid().toString()); //キャラクタリスティックからデバイスの特定
                    ppm.set(onDevices.indexOf(device), strChara); //目標デバイスの列を指定
                    Log.i("ppm", "" + ppm);
                }
            });
            return;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // GUIアイテム
        int orientation = getResources().getConfiguration().orientation;
        mChart = (LineChart) findViewById(R.id.chart);
        init = 1;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mButton_Disconnect = (Button) findViewById(R.id.button_disconnect);
            mButton_Disconnect.setOnClickListener(this);
            mEditText = (EditText) findViewById(R.id.sheetname);
            //mButton_ReadChara1 = (Button)findViewById( R.id.button_readchara1 );
            //mButton_ReadChara1.setOnClickListener( this );
            mButton_ReadChara2 = (Button) findViewById(R.id.button_readchara2);
            mButton_ReadChara2.setOnClickListener(this);
            mButton_Delete = (Button) findViewById(R.id.button_delete);
            mButton_Delete.setOnClickListener(this);
            mButton_Download = (Button) findViewById(R.id.button_download);
            mButton_Download.setOnClickListener(this);

            mSheet = (EditText) findViewById(R.id.sheetname);
            mHour = (EditText) findViewById(R.id.hour);
            mMinute = (EditText) findViewById(R.id.min);
            mInterval = (EditText) findViewById(R.id.interval);

            //リストビュー
            mDeviceListAdapter = new DeviceListAdapter(this); // ビューアダプターの初期化
            listView = (ListView) findViewById(R.id.residentlist);    // リストビューの取得
            //listView.setAdapter(mDeviceListAdapter);    // リストビューにビューアダプターをセット

            //changeイベントの実装
            mSheet.addTextChangedListener(textWatcher);
            mHour.addTextChangedListener(textWatcher);
            mMinute.addTextChangedListener(textWatcher);
            mInterval.addTextChangedListener(textWatcher);

            //}else if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            // 線グラフView
            //mChart = (LineChart) findViewById(R.id.land_chart);

            // グラフ説明テキストを表示するか
            mChart.getDescription().setEnabled(true);
            // グラフ説明テキストのテキスト設定
            mChart.getDescription().setText("説明");
            // グラフ説明テキストの文字色設定
            mChart.getDescription().setTextColor(Color.BLACK);
            // グラフ説明テキストの文字サイズ設定
            mChart.getDescription().setTextSize(10f);
            // グラフ説明テキストの表示位置設定
            mChart.getDescription().setPosition(0, 0);

            // グラフへのタッチジェスチャーを有効にするか
            mChart.setTouchEnabled(true);

            // グラフのスケーリングを有効にするか
            mChart.setScaleEnabled(true);
            //mChart.setScaleXEnabled(true);     // X軸のみに対しての設定
            //mChart.setScaleYEnabled(true);     // Y軸のみに対しての設定

            // グラフのドラッギングを有効にするか
            mChart.setDragEnabled(true);

            // グラフのピンチ/ズームを有効にするか
            mChart.setPinchZoom(true);

            // グラフの背景色設定
            mChart.setBackgroundColor(Color.WHITE);

            // Y軸(左)の設定
            // Y軸(左)の取得
            YAxis leftYAxis = mChart.getAxisLeft();
            // Y軸(左)の最大値設定
            leftYAxis.setAxisMaximum(2000f);
            // Y軸(左)の最小値設定
            leftYAxis.setAxisMinimum(200f);

            // Y軸(右)の設定
            // Y軸(右)は表示しない
            mChart.getAxisRight().setEnabled(false);

            // X軸の設定
            XAxis xAxis = mChart.getXAxis();
            // X軸の最大値設定
            //xAxis.setAxisMaximum(100f);
            // X軸の最小値設定
            xAxis.setAxisMinimum(0f);
            // X軸の値表示設定
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    // nullを返すと落ちるので、値を書かない場合は空文字を返す
                    return "";
                }
            });
            mChart.setMinimumHeight(500);
            mChart.setData(new LineData());
            //setData();
        }

        // Android端末がBLEをサポートしてるかの確認
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "NOT supported", Toast.LENGTH_SHORT).show();
            finish();    // アプリ終了宣言
            return;
        }

        // Bluetoothアダプタの取得
        connectedDeviceMap = new HashMap<String, BluetoothGatt>();
        adressList = new HashMap<String, DeviceSet>();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (null == mBluetoothAdapter) {    // Android端末がBluetoothをサポートしていない
            Toast.makeText(this, "NOT supported", Toast.LENGTH_SHORT).show();
            finish();    // アプリ終了宣言
            return;
        }

        //DB関連
        managedb = new ManageDB(getApplicationContext(), "esplist", 1);
        db = managedb.getReadableDatabase();
    }

    void setData() {
        //  追加描画するデータを追加
        LineData data = mChart.getLineData();

        if (data != null) {
            int i = 0;
            for(String val:ppm) {
                ILineDataSet set = data.getDataSetByIndex(i);
                if (set == null) {
                    set = createSet(ppm.get(i), Color.BLACK);
                    data.addDataSet(set);
                }
                try {
                    data.addEntry(new Entry(set.getEntryCount(), Float.parseFloat(ppm.get(i))), i);
                } catch (Exception e) {
                    data.addEntry(new Entry(set.getEntryCount(), 0), i);
                }
                data.notifyDataChanged();
                i++;
            }

            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(20);
            mChart.moveViewToX(data.getEntryCount());
        } else {
            mChart.setData(new LineData());
        }
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setLineWidth(2.5f);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setDrawValues(false);

        return set;
    }

    private HttpGet.Listener createListener() {
        return new HttpGet.Listener() {
            @Override
            public void onSuccess() {
                Log.d("SUCCESS", "Success");
            }
        };
    }

    // 初回表示時、および、ポーズからの復帰時
    @Override
    protected void onResume() {
        super.onResume();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // GUIアイテムの有効無効の設定
            if (stopflag) {
                mButton_Disconnect.setEnabled(false);
                //mButton_ReadChara1.setEnabled( false );
                mButton_ReadChara2.setEnabled(false);
            }

            //初期化
            onDevices = new ArrayList<DeviceSet>();
            //DB読み込み
            try {
                Cursor cursor = managedb.readAll(db);
                cursor.moveToFirst();
                StringBuilder sbuilder = new StringBuilder();
                //リセット
                DeviceSet deviceSet = new DeviceSet();
                deviceSet.deviceName = "";
                deviceSet.deviceAdress = "";
                deviceSet.serviceUUID = "";
                deviceSet.charaUUID = "";
                mDeviceListAdapter.addDevice(deviceSet);
                mDeviceListAdapter.clear();
                for (int i = 0; i < cursor.getCount(); i++) {
                    deviceSet = new DeviceSet();
                    deviceSet.deviceName = cursor.getString(0);
                    deviceSet.deviceAdress = cursor.getString(1);
                    deviceSet.serviceUUID = cursor.getString(2);
                    deviceSet.charaUUID = cursor.getString(3);
                    mDeviceListAdapter.addDevice(deviceSet);
                    adressList.put(cursor.getString(3), deviceSet);
                    cursor.moveToNext();
                }
                cursor.close();

                listView.setAdapter(mDeviceListAdapter);
                int totalHeight = 0;
                for (int i = 0; i < mDeviceListAdapter.getCount(); i++) {
                    View item = mDeviceListAdapter.getView(i, null, listView);
                    item.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    totalHeight += item.getMeasuredHeight();
                }
                ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                layoutParams.height = totalHeight + (listView.getDividerHeight() * (mDeviceListAdapter.getCount() - 1));
                listView.setLayoutParams(layoutParams);

            } catch (Exception e) {
                Log.e("DBException:", e.toString());
            }
        }
        // Android端末のBluetooth機能の有効化要求
        requestBluetoothFeature();
    }

    // 別のアクティビティ（か別のアプリ）に移行したことで、バックグラウンドに追いやられた時
    @Override
    protected void onPause() {
        super.onPause();

        // 切断
        //disconnect();
    }

    // アクティビティの終了直前
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mBluetoothGatt) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    // Android端末のBluetooth機能の有効化要求
    private void requestBluetoothFeature() {
        if (mBluetoothAdapter.isEnabled()) {
            return;
        }
        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLEBLUETOOTH);
    }

    // 機能の有効化ダイアログの操作結果(onResumeの前に呼ばれる)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLEBLUETOOTH: // Bluetooth有効化要求
                if (Activity.RESULT_CANCELED == resultCode) {    // 有効にされなかった
                    Toast.makeText(this, "DISABLE", Toast.LENGTH_SHORT).show();
                    finish();    // アプリ終了宣言
                    return;
                }
                break;
            case REQUEST_CONNECTDEVICE: // デバイス接続要求
                String strDeviceName;
                if (Activity.RESULT_OK == resultCode) {
                    // デバイスリストアクティビティからの情報の取得
                    strDeviceName = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_NAME);
                    mDeviceAddress = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_ADDRESS);
                } else {
                    strDeviceName = "";
                    mDeviceAddress = "";
                }
                //( (TextView)findViewById( R.id.textview_devicename ) ).setText( strDeviceName );
                //( (TextView)findViewById( R.id.textview_deviceaddress ) ).setText( mDeviceAddress );
                //( (TextView)findViewById( R.id.textview_readchara1 ) ).setText( "" );
                //( (TextView)findViewById( R.id.textview_readchara2 ) ).setText( "" );
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // オプションメニュー作成時の処理
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    // オプションメニューのアイテム選択時の処理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.resident:
                Intent residentactivityIntent = new Intent(this, ResidentActivity.class);
                startActivityForResult(residentactivityIntent, REQUEST_CONNECTDEVICE); //遷移先への値の受け渡し
                if (!stopflag) disconnect();
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (mButton_Disconnect.getId() == v.getId()) {
            mButton_Disconnect.setEnabled(false);    // 切断ボタンの無効化（連打対策）
            disconnect();            // 切断
            return;
        }
        if (mButton_ReadChara2.getId() == v.getId()) {
            sheetName = mEditText.getText().toString();
            stopflag = !stopflag;
            delay = Integer.parseInt(mInterval.getText().toString()) * 1000; //インターバル変更

            //数値のアップロード
            if (!stopflag) { //OFF->ON
                onDevices = onDevice(listView);
                ppm = new ArrayList<String>();
                for (DeviceSet device : onDevices) {
                    ppm.add(device.deviceName);
                }
                //handler.postDelayed(r, delay);
                handler.post(r);
                mButton_ReadChara2.setText("OFF");
            } else { //OFF->ON
                handler.removeCallbacks(r);
                mButton_ReadChara2.setText("ON");
            }
            return;
        }
        if (mButton_Delete.getId() == v.getId()) {
            managedb.allDelete(db);
            adressList.clear();
        }
        if (mButton_Download.getId() == v.getId()) {
            try {
                String filename = mSheet.getText().toString()+".csv";
                File uploadFile = new File(getExternalFilesDir(null), filename);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                Uri uri = Uri.fromFile(uploadFile);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, ""));
            }catch(Exception e){
                Log.e("IOException",e.toString());
            }
        }
    }

    // 接続
    private void connect() {
        if (mDeviceAddress.equals("")) {    // DeviceAddressが空の場合は処理しない
            return;
        }

        if (null != mBluetoothGatt) {    // mBluetoothGattがnullでないなら接続済みか、接続中。
            return;
        }

        // 接続
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        //mBluetoothGatt = device.connectGatt( this, false, mGattcallback );
    }

    // 接続
    private void connect2(String adress, int position) {
        if (adress.equals("")) {    // DeviceAddressが空の場合は処理しない
            return;
        }

        if (null != connectedDeviceMap.get(adress)) {    // mBluetoothGattがnullでないなら接続済みか、接続中。
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(adress);
        device.connectGatt(this, false, mGattcallback);
    }

    // 切断
    private void disconnect() {
        mButton_ReadChara2.setText("ON");
        handler.removeCallbacks(r);
        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        for (String key : connectedDeviceMap.keySet()) {
            connectedDeviceMap.get(key).close();
            connectedDeviceMap.remove(key);
        }
        for (DeviceSet deviceSet : mDeviceListAdapter.mDeviceList) {
            deviceSet.swi = false;
        }
        // GUIアイテムの有効無効の設定
        // すべてOFF
        mButton_Disconnect.setEnabled(false);
        //mButton_ReadChara1.setEnabled( false );
        mButton_ReadChara2.setEnabled(false);

        listView.setAdapter(mDeviceListAdapter);
    }

    // 切断
    private void disconnect2(String adress, int position) {
        BluetoothGatt bluetoothGatt = connectedDeviceMap.get(adress);
        handler.removeCallbacks(r);
        if (null == bluetoothGatt) {
            return;
        }

        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        bluetoothGatt.close();
        connectedDeviceMap.remove(adress);
        enableButton();
    }

    // キャラクタリスティックの読み込み
    private void readCharacteristic(UUID uuid_service, UUID uuid_characteristic) {
        DeviceSet deviceSet = adressList.get(uuid_characteristic.toString());
        if (deviceSet == null) {
            return;
        }
        String adress = deviceSet.deviceAdress.toString();
        BluetoothGatt bluetoothGatt = connectedDeviceMap.get(adress);
        Log.i("LOGgatt", "" + (bluetoothGatt == null));

        if (!connectedDeviceMap.containsKey(adress) || bluetoothGatt == null) {
            return;
        }

        BluetoothGattCharacteristic blechar = bluetoothGatt.getService(uuid_service).getCharacteristic(uuid_characteristic);
        //mBluetoothGatt.readCharacteristic(blechar)
        bluetoothGatt.readCharacteristic(blechar);
    }

    //ListViewの読み込み(ONを返す)
    private ArrayList<DeviceSet> onDevice(ListView listView) {
        ArrayList<DeviceSet> deviceSets = new ArrayList<DeviceSet>();
        for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); i++) {
            DeviceSet deviceSet = (DeviceSet) listView.getItemAtPosition(i);
            if (deviceSet.swi) deviceSets.add(deviceSet);
        }
        return deviceSets;
    }

    public void changeSwitch(View v) {
        ListView lv = listView;
        int position = lv.getPositionForView(v);
        DeviceSet deviceSet = mDeviceListAdapter.mDeviceList.get(position);
        deviceSet.swi = !deviceSet.swi;
        mDeviceListAdapter.mDeviceList.set(position, deviceSet);
        if (deviceSet.swi) connect2(deviceSet.deviceAdress, position);
        else disconnect2(deviceSet.deviceAdress, position);
        enableButton();
    }

    public void editList(View v) {

    }

    public void deleteList(View v) {
        ListView lv = listView;
        int position = lv.getPositionForView(v);
        mDeviceListAdapter.deleteDevice(position);
        //ファイルの読み込み
        ArrayList<String> sentences = new ArrayList<String>();
        try {
            InputStream in = openFileInput("resister.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String in_s;
            //リセット
            DeviceSet deviceSet = new DeviceSet();
            while ((in_s = reader.readLine()) != null) {
                sentences.add(in_s);
                deviceSet.swi = false;
            }
            reader.close();

            deleteFile("resister.txt");

            //書き込み
            OutputStream out = openFileOutput("resister.txt", MODE_APPEND);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            for (int i = 0; i < sentences.size(); i++) {
                if (i == position) continue;
                writer.append(sentences.get(i) + "\n");
            }
            writer.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enableButton() {
        //一つ以上のデバイスと接続しており、必要なデータが記入されている
        if (connectedDeviceMap.size() > 0 && !mSheet.getText().toString().equals("") && !mHour.getText().toString().equals("") && !mMinute.getText().toString().equals("") && !mInterval.getText().toString().equals("")) {
            mButton_ReadChara2.setEnabled(true);
            mButton_Disconnect.setEnabled(true);
        } else {
            mButton_ReadChara2.setEnabled(false);
            mButton_Disconnect.setEnabled(false);
        }
    }
}