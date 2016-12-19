package com.speedtune.client;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private final String TAG_FRAG_MAIN = "FragMain";
    private final String TAG_FRAG_CODES = "FragCodes";
    private static final byte[] reset_cmd = {'B'};
    private static final byte[] update_clock_cmd = {'E'};
    private static final byte[] auto_param = {'A'};
    private static final String uuid_template = "0000%04x-0000-1000-8000-00805f9b34fb";

    public static final int PLATFORM_TYPE_E_SERIES_N54 = 0;
    public static final int PLATFORM_TYPE_E_SERIES_N55 = 1;
    public static final int PLATFORM_TYPE_F_SERIES = 2;
    protected final int PLATFORM_TYPE_MIN = PLATFORM_TYPE_E_SERIES_N54;
    protected final int PLATFORM_TYPE_MAX = PLATFORM_TYPE_F_SERIES;
    protected final String PREF_PLATFORM_TYPE = "pref_platform";

    private static final int CMD_INTERVAL = 8;
    private static final HashMap<Integer, String> mapSupportedBtModule;
    protected String serv_uuid = "";
    protected String char_uuid = "";

    static{
        mapSupportedBtModule = new HashMap<>();
        mapSupportedBtModule.put(0x88F8, "SpeedTune");
        //add more supported bt modules here
    }

    private String mDeviceAddress;
	private boolean result;
	private BluetoothLeService mBluetoothLeService;
	
    private BluetoothGattCharacteristic mNotifyCharacteristic;

	// 代码管理服务生命周期。
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			Log.i(TAG, "init bluetooth service");
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "failed to init bluetooth service");
				finish();
			}
			// 自动连接到装置上成功启动初始化。
            if(mDeviceAddress != null)
            {
                result = mBluetoothLeService.connect(mDeviceAddress);
            }
			
			
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};

//    private ArrayList<BluetoothGattCharacteristic> charas;
    private BluetoothAdapter mBluetoothAdapter;

    public HashMap<String, SpeedTuneData> deviceParams;
    public HashMap<String, SpeedTuneData> deviceSetting;
    public ArrayList<String> deviceCodes;

    private LinkedBlockingQueue<byte[]> rawDataQueue;
    private Thread rawDataHandler;
    private Long heartBeatTimeStamp = 0L;
    private Long lastSaveCmdTimeStamp = 0L;
    private Timer autoParamUpdateTimer;
    private Timer updateClockCmdTimer;
    private int nPlatformType;
    private boolean bAllUserSettingReceived = false;
    private boolean bAllMethSettingReceived = false;
    private Semaphore semBtScan;

    private LinkedBlockingQueue<byte[]> getRawDataQueue()
    {
        if(rawDataQueue == null) { //lazy init
            rawDataQueue = new LinkedBlockingQueue<byte[]>();
            rawDataHandler = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (checkRawDataQueue()) {
                        try
                        {
                            Thread.sleep(2L);
                        }
                        catch (InterruptedException ex)
                        {
                            ex.printStackTrace();
                            break;
                        }

                    }

                }
            });
            rawDataHandler.start();
        }

        return rawDataQueue;
    }

    private void initDeviceData()
    {
        deviceParams = new HashMap<String, SpeedTuneData>();

        deviceParams.put("A", new SpeedTuneData("0", "RPM"));
        deviceParams.put("B", new SpeedTuneData("0", "Boost"));
        deviceParams.put("C", new SpeedTuneData("0", "Pedal"));
        deviceParams.put("D", new SpeedTuneData("0", "PWM"));
        deviceParams.put("E", new SpeedTuneData("0", "FuelEn"));
        deviceParams.put("G", new SpeedTuneData("0", "IAT"));
        deviceParams.put("H", new SpeedTuneData("0", "Clock"));
        deviceParams.put("I", new SpeedTuneData("0", "Ign 2"));
        deviceParams.put("J", new SpeedTuneData("0", "Ign 3"));
        deviceParams.put("K", new SpeedTuneData("0", "Ign 5"));
        deviceParams.put("M", new SpeedTuneData("0", "ECU PSI"));
        deviceParams.put("N", new SpeedTuneData("0", "Target"));
        deviceParams.put("O", new SpeedTuneData("0", "Ign 4"));
        deviceParams.put("W", new SpeedTuneData("0", "Thrtl"));
        deviceParams.put("X", new SpeedTuneData("0", "FP H"));
        deviceParams.put("e", new SpeedTuneData("0", "TransF"));
        deviceParams.put("y", new SpeedTuneData("0", "Ign 6"));
        deviceParams.put("%", new SpeedTuneData("0", "Meth"));
        deviceParams.put("$", new SpeedTuneData("0", "FP L"));
        deviceParams.put(">", new SpeedTuneData("0", "OilF"));
        deviceParams.put("<", new SpeedTuneData("0", "WaterF"));
        deviceParams.put("&", new SpeedTuneData("0", "Trims"));
        deviceParams.put("!", new SpeedTuneData("0", "Gear"));
        deviceParams.put("^", new SpeedTuneData("0", "DutyC"));
        deviceParams.put("-", new SpeedTuneData("0", "FF"));
        deviceParams.put(":", new SpeedTuneData("0", "AFR"));
        deviceParams.put("(", new SpeedTuneData("0", "I Avg"));
        deviceParams.put(")", new SpeedTuneData("0", "Ign 1"));
        deviceParams.put("*", new SpeedTuneData("0", "DME BT"));
        deviceParams.put("=", new SpeedTuneData("0", "Load"));
        deviceParams.put("~", new SpeedTuneData("0", "AFR 2"));

        deviceSetting = new HashMap<String, SpeedTuneData>();

        deviceSetting.put("P", new SpeedTuneData("0", "Min Flow Boost (PSI)"));
        deviceSetting.put("Q", new SpeedTuneData("0", "Enabled"));
        deviceSetting.put("R", new SpeedTuneData("0", "Min meth flow"));
        deviceSetting.put("S", new SpeedTuneData("0", "Min RPM"));
        deviceSetting.put("T", new SpeedTuneData("0", "Max RPM"));
        deviceSetting.put("U", new SpeedTuneData("0", "Min TPS"));
        deviceSetting.put("w", new SpeedTuneData("0", "Boost Additive"));
        deviceSetting.put("@", new SpeedTuneData("0", "Signal Scaling"));
        deviceSetting.put("V", new SpeedTuneData("0", "Shift Reduction"));
        deviceSetting.put("{", new SpeedTuneData("0", "Min Gear"));
        deviceSetting.put("}", new SpeedTuneData("0", "Min AFR"));
        deviceSetting.put("|", new SpeedTuneData("0", "Min Advance"));
        deviceSetting.put("L", new SpeedTuneData("0", "TMAP Voltage"));
        deviceSetting.put("q", new SpeedTuneData("0", "Firmware Ver"));
        deviceSetting.put("o", new SpeedTuneData("0", "Future Use D"));
        deviceSetting.put("+", new SpeedTuneData("0", "Fuel Open Loop"));
        deviceSetting.put("p", new SpeedTuneData("0", "PID Gain"));
        deviceSetting.put("r", new SpeedTuneData("0", "Auto Shift Boost Reduction"));
        deviceSetting.put("v", new SpeedTuneData("0", "Max Boost 1st"));
        deviceSetting.put("n", new SpeedTuneData("0", "Future Use A"));
        deviceSetting.put("s", new SpeedTuneData("0", "Default Wastegate Position"));
        deviceSetting.put("t", new SpeedTuneData("0", "Boost Safety"));
        deviceSetting.put("u", new SpeedTuneData("0", "FF/Wastegate Adaption"));
        deviceSetting.put("x", new SpeedTuneData("0", "Max Boost 2nd"));
        deviceSetting.put("z", new SpeedTuneData("0", "Avg Ign"));
        deviceSetting.put("#", new SpeedTuneData("0", "Last Safety"));
        deviceSetting.put("`", new SpeedTuneData("0", "Meth Safety Mode"));
        deviceSetting.put("m", new SpeedTuneData("0", "Meth Trigger Mode"));
        deviceSetting.put("F", new SpeedTuneData("0", "Map"));
        deviceSetting.put("Z_0", new SpeedTuneData("0", "VIN"));
        deviceSetting.put("a_0", new SpeedTuneData("0", ""));
        deviceSetting.put("a_1", new SpeedTuneData("0", ""));
        deviceSetting.put("a_2", new SpeedTuneData("0", ""));
        deviceSetting.put("a_3", new SpeedTuneData("0", ""));
        deviceSetting.put("a_4", new SpeedTuneData("0", ""));
        deviceSetting.put("a_5", new SpeedTuneData("0", ""));
        deviceSetting.put("a_6", new SpeedTuneData("0", ""));
        deviceSetting.put("a_7", new SpeedTuneData("0", ""));
        deviceSetting.put("a_8", new SpeedTuneData("0", ""));
        deviceSetting.put("a_9", new SpeedTuneData("0", ""));
        deviceSetting.put("a_10", new SpeedTuneData("0", ""));
        deviceSetting.put("a_11", new SpeedTuneData("0", ""));
        deviceSetting.put("a_12", new SpeedTuneData("0", ""));
        deviceSetting.put("a_13", new SpeedTuneData("0", ""));
        deviceSetting.put("a_14", new SpeedTuneData("0", ""));
        deviceSetting.put("a_15", new SpeedTuneData("0", ""));
        deviceSetting.put("a_16", new SpeedTuneData("0", ""));
        deviceSetting.put("a_17", new SpeedTuneData("0", ""));
        deviceSetting.put("a_18", new SpeedTuneData("0", ""));
        deviceSetting.put("a_19", new SpeedTuneData("0", ""));
        deviceSetting.put("a_20", new SpeedTuneData("0", ""));
        deviceSetting.put("a_21", new SpeedTuneData("0", ""));
        deviceSetting.put("a_22", new SpeedTuneData("0", ""));
        deviceSetting.put("a_23", new SpeedTuneData("0", ""));
        deviceSetting.put("a_24", new SpeedTuneData("0", ""));
        deviceSetting.put("a_25", new SpeedTuneData("0", ""));
        deviceSetting.put("a_26", new SpeedTuneData("0", ""));
        deviceSetting.put("a_27", new SpeedTuneData("0", ""));
        deviceSetting.put("a_28", new SpeedTuneData("0", ""));
        deviceSetting.put("a_29", new SpeedTuneData("0", ""));
        deviceSetting.put("a_30", new SpeedTuneData("0", ""));
        deviceSetting.put("a_31", new SpeedTuneData("0", ""));
        deviceSetting.put("a_32", new SpeedTuneData("0", ""));
        deviceSetting.put("a_33", new SpeedTuneData("0", ""));
        deviceSetting.put("a_34", new SpeedTuneData("0", ""));
        deviceSetting.put("a_35", new SpeedTuneData("0", ""));

        deviceCodes = new ArrayList<>();
    }

    private void initPlatformType()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        try
        {
            nPlatformType = pref.getInt(PREF_PLATFORM_TYPE, PLATFORM_TYPE_E_SERIES_N54);
        }
        catch( ClassCastException e)
        {
            e.printStackTrace();
            nPlatformType = PLATFORM_TYPE_E_SERIES_N54;
        }
    }

    private void handleBtDisconnect()
    {
        mBluetoothLeService.close();
        updateConnectionStatus(MainFragment.CONN_STATUS_NONE);
        bAllUserSettingReceived = false;
        bAllMethSettingReceived = false;
        setAutoParamUpdateEnable(false);
        setUpdateClockEnable(false);
        SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
        recv.isAutoParseParam = false;
        recv.clearParamQueue();
    }

	// 处理各种事件的服务了。
	// action_gatt_connected连接到服务器：关贸总协定。
	// action_gatt_disconnected：从关贸总协定的服务器断开。
	// action_gatt_services_discovered：关贸总协定的服务发现。
	// action_data_available：从设备接收数据。这可能是由于阅读
	// 或通知操作。
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				result = true;
				Log.i(TAG, "get boardcast 1");
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				result = false;
				Log.i(TAG, "get boardcast 2");
                handleBtDisconnect();

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// 显示所有的支持服务的特点和用户界面。
				Log.i(TAG, "get boardcast 3");
				List<BluetoothGattService> supportedGattServices = mBluetoothLeService
						.getSupportedGattServices();
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
				for(int i=0;i<supportedGattServices.size();i++){
					Log.w(TAG,"1:BluetoothGattService UUID=:"+supportedGattServices.get(i).getUuid());
					List<BluetoothGattCharacteristic> cs = supportedGattServices.get(i).getCharacteristics();
					for(int j=0;j<cs.size();j++){
						Log.w(TAG,"2:   BluetoothGattCharacteristic UUID=:"+cs.get(j).getUuid());
						
					
							List<BluetoothGattDescriptor> ds = cs.get(j).getDescriptors();
							for(int f=0;f<ds.size();f++){
								Log.w(TAG,"3:      BluetoothGattDescriptor UUID=:"+ds.get(f).getUuid());
								
								 byte[] value = ds.get(f).getValue();
								
								 Log.w(TAG,"4:     			value=:"+Arrays.toString(value));
								 Log.w(TAG,"5:     			value=:"+Arrays.toString( ds.get(f).getCharacteristic().getValue()));
							}
					}
				}
                connectDevice();
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				Log.i(TAG, "get broadcast 4--->");
                final byte[] rawData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if(rawData != null && rawData.length > 0) {
//                    Log.w(TAG, "get raw data:" + ByteArrayToString(rawData));
                    if (!getRawDataQueue().offer(rawData)) {
                        Log.e(TAG, "rawDataQueue full");
                    }
                }

			}else if(BluetoothLeService.ACTION_RSSI.equals(action)){
				Log.i(TAG, "get broadcast 5---> RSSI:" + intent
                        .getStringExtra(BluetoothLeService.ACTION_DATA_RSSI));
			}
		}
	};

    private class MainTabListener<T extends Fragment> implements TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        public MainTabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        Tab tab = actionBar
                .newTab()
                .setText(R.string.tab_main)
                .setTabListener(
                        new MainTabListener<MainFragment>(this, TAG_FRAG_MAIN,
                                MainFragment.class));
        actionBar.addTab(tab);
        tab = actionBar
                .newTab()
                .setText(R.string.tab_codes)
                .setTabListener(
                        new MainTabListener<CodesFragment>(this, TAG_FRAG_CODES,
                                CodesFragment.class));
        actionBar.addTab(tab);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this,"Bluetooth feature not found", Toast.LENGTH_SHORT).show();
			finish();
		}

		// 初始化一个蓝牙适配器。对API 18级以上，可以参考 bluetoothmanager。
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		//  检查是否支持蓝牙的设备。
		if (mBluetoothAdapter == null) {
			Toast.makeText(this,"Bluetooth not supported", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

        initDeviceData();
        initPlatformType();

        semBtScan = new Semaphore(0);

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
        //make sure bt is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        /**
		 * register receiver
		 */
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        //reconnect on resume
		if (mBluetoothLeService != null) {
			result = mBluetoothLeService.connect(mDeviceAddress);
			Log.w(TAG, "connect result=" + result);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.action_settings:
			if (result) {
				result = false;
                handleBtDisconnect();
			}
			onBackPressed();
			break;

		case R.id.action_close:
			if (result) {
				result = false;
				Log.w(TAG, "disconnected");
                handleBtDisconnect();
			}

			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 销毁广播接收器
	 */
	@Override
	protected void onPause() {
		super.onPause();
        setAutoParamUpdateEnable(false);
        setUpdateClockEnable(false);
		unregisterReceiver(mGattUpdateReceiver);
	
	}
	/**
	 * 结束服务
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        if(commandHandler != null)
        {
            commandHandler.setStopFlag(true);
            commandHandler.interrupt();
        }

        if(rawDataHandler != null)
        {
            rawDataHandler.interrupt();
        }
		unbindService(mServiceConnection);

	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        try {
            Log.d(TAG, "onConfigurationChanged");
            super.onConfigurationChanged(newConfig);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // land
            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // port
            }
        } catch (Exception ex) {
        }

    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
        	return;
        String uuid = null;
        BluetoothGattCharacteristic characteristic = null;
        // 循环遍历服务
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString().toLowerCase(Locale.US);
            Log.w(TAG, "service uuid : " + uuid);
            if(uuid.equals(serv_uuid))
            {
                characteristic = gattService.getCharacteristic(UUID.fromString(char_uuid));
            }
        }

        if(characteristic != null)
        {
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0)
            {
                if (mNotifyCharacteristic != null)
                {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);

            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
            {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
        }
        else
        {
            Log.e(TAG, "unable to find char_uuid: " + char_uuid);
        }
    }

    private void clearUI() {

    }

	/**
	 * 注册广播
	 * @return
	 */
    
    
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_RSSI);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_RSSI);
		return intentFilter;
	}

	// 扫描装置的回调。
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {

				@Override
				public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							checkDevice(device, rssi, scanRecord);
							Log.e(TAG,"发现蓝牙"+device.getAddress()+"状态"+device.getBondState()+"type"+device.getType()+device.describeContents());
						}
					});
				}
			};

    private static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");

        return hex.toString();
    }


    public static class AdRecord {
		private int iLength;
		private int iType;
		private byte[] rawData;
        public AdRecord(int length, int type, byte[] data) {
			iLength = length;
			iType = type;
			rawData = data;

            Log.i(TAG, "Length: " + length + " Type : " + type + " Data : " + ByteArrayToString(data));
        }

        // ...

        public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<AdRecord>();

            int index = 0;
            while (index < scanRecord.length) {
                int length = scanRecord[index++];
                //Done once we run out of records
                if (length == 0) break;

                int type = scanRecord[index];
                //Done if our record isn't a valid type
                if (type == 0) break;

                byte[] data = Arrays.copyOfRange(scanRecord, index+1, index+length);

                records.add(new AdRecord(length, type, data));
                //Advance
                index += length;
            }

            return records;
        }

        public int getUUID()
        {
            int ret = -1;
            if(iType == 2)
            {
                ret = ((rawData[0] & 0xFF) | (rawData[1] & 0xFF) << 8) & 0xFFFF;
//                Log.w(TAG, "getUUID: " + ret);
            }
            return ret;
        }
    }
	private void checkDevice(final BluetoothDevice device, final int rssi, byte[] scanRecord)
	{
        Log.w(TAG, "check device:" + device.getName() + ", " + rssi);

		List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);
		boolean uuid_match = false;
        int uuid = 0;

        String supportedBtName = null;
		for(AdRecord rec:records)
		{
            uuid = rec.getUUID();

            if((supportedBtName = mapSupportedBtModule.get(uuid)) != null)
            {
                uuid_match = true;
                break;
            }
		}

        if(uuid_match && device.getName().equals(supportedBtName))
        {
            serv_uuid = String.format(Locale.US, uuid_template, uuid);
            char_uuid = String.format(Locale.US, uuid_template, uuid + 1);
            semBtScan.release();
            mDeviceAddress = device.getAddress();
            mBluetoothLeService.connect(mDeviceAddress);
        }
	}

    public void handleConnect()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                semBtScan.drainPermits();
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                while(true) {
                    try {
                        boolean foundDevice = semBtScan.tryAcquire(4L, TimeUnit.SECONDS);
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);

                        if(!foundDevice)
                        {
                            updateConnectionStatusOnUiThread(MainFragment.CONN_STATUS_NONE, getResources().getString(R.string.mainact_device_not_found));
                        }

                        Log.w(TAG, "le scan stopped");
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        continue;
                    }
                    break;
                }
            }
        }).start();
    }

    private void updateConnectionStatusOnUiThread(final int connStatus)
    {
        updateConnectionStatusOnUiThread(connStatus, null);
    }

    private void updateConnectionStatusOnUiThread(final int connStatus, final String toastText)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateConnectionStatus(connStatus);
                if(toastText != null)
                {
                    showToastText(toastText);
                }
            }
        });
    }

    private void handleBtDisconnectOnUiThread(final String toastText)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleBtDisconnect();
                if(toastText != null)
                {
                    showToastText(toastText);
                }
            }
        });
    }


    private MainFragment getMainFragment()
    {
        return (MainFragment) getFragmentManager().findFragmentByTag(TAG_FRAG_MAIN);
    }

    private CodesFragment getCodesFragment()
    {
        return (CodesFragment) getFragmentManager().findFragmentByTag(TAG_FRAG_CODES);
    }

    private void updateConnectionStatus(int status) {
        MainFragment frag = getMainFragment();
        if (frag != null ) { //&& frag.isInLayout()
            frag.setConnStatus(status);
        }
    }

    private boolean checkRawDataQueue()
    {
        boolean ret = true;
        try{
            byte[] rawData = getRawDataQueue().take();
            SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
            recv.receive(rawData);

        }catch(InterruptedException ex)
        {
            ex.printStackTrace();
            ret = false;
        }
        return ret;
    }

    private class CommandThread extends Thread{
        private LinkedBlockingQueue<Runnable> commandQueue;
        private boolean stopFlag;

        public CommandThread()
        {
            commandQueue = new LinkedBlockingQueue<Runnable>();
            stopFlag = false;
        }

        public void addCommand(Runnable cmd){
            commandQueue.add(cmd);
        }

        public void setStopFlag(boolean stopFlag) {
            this.stopFlag = stopFlag;
        }

        @Override
        public void run() {
            while(!stopFlag)
            {
                try
                {
                    Thread.sleep(2);
                    Runnable cmd = commandQueue.take();
                    cmd.run();
                }
                catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private CommandThread commandHandler;
    private CommandThread getCommandHandler()
    {
        if(commandHandler == null)
        {
            commandHandler = new CommandThread();
            commandHandler.start();
        }

        return commandHandler;
    }

    private void threadSleep(int delay)
    {
        try
        {
            Thread.sleep((long)delay);
        }
        catch(InterruptedException ex)
        {
            ex.printStackTrace();
        }
        return;
    }

    private void connectDevice()
    {
        getCommandHandler().addCommand(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "connecting to device");
                byte[] setting_dollar = {'$'};
                byte[] setting_sharp = {'#'};
                byte[] setting_C = {'C'};
                byte[] setting_J = {'J'};
                SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
                threadSleep(500);
                sendToDevice(reset_cmd);
                recv.isAutoParseParam = true;
                threadSleep(700);
                sendToDevice(update_clock_cmd);
                sendToDevice(update_clock_cmd);
                threadSleep(200);
                updateAllDeviceParams();

                if(System.currentTimeMillis()/1000 - heartBeatTimeStamp > 1)
                {
                    //no heartbeat response from device
                    handleBtDisconnectOnUiThread(getResources().getString(R.string.mainact_device_error));
                }
                else
                {
                    updateConnectionStatusOnUiThread(MainFragment.CONN_STATUS_CONNECTED);
                    sendToDevice(setting_sharp);
                    sendToDevice(setting_dollar);
                    sendToDevice(setting_C);
                    sendToDevice(setting_J);
                    threadSleep(500);
                    recv.isAutoParseParam = false;
    //                recv.dumpParamQueue();
                    updateAllDeviceParamsOnUiThread();
                    threadSleep(100);
                    setAutoParamUpdateEnableOnCommandThread(true);
                    sendToDevice(auto_param);
                    setUpdateClockEnableOnCommandThread(true);
                }
            }
        });
    }

    private void setAutoParamUpdateEnable(final boolean enable)
    {
        getCommandHandler().addCommand(new Runnable() {
            @Override
            public void run() {
                setAutoParamUpdateEnableOnCommandThread(enable);
            }
        });
    }
    private void setAutoParamUpdateEnableOnCommandThread(boolean enable)
    {
        SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
        if(enable && autoParamUpdateTimer == null)
        {
            recv.isAutoParseParam = true;
            autoParamUpdateTimer = new Timer();
            autoParamUpdateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateAllDeviceParamsOnUiThread();
                }
            }, 0L, 1000 / 40);
        }
        else if(!enable && autoParamUpdateTimer != null)
        {
            recv.isAutoParseParam = true;
            autoParamUpdateTimer.cancel();
            autoParamUpdateTimer = null;
        }
    }

    private void setUpdateClockEnable(final boolean enable)
    {
        getCommandHandler().addCommand(new Runnable() {
            @Override
            public void run() {
                setUpdateClockEnableOnCommandThread(enable);
            }
        });
    }
    private void setUpdateClockEnableOnCommandThread(boolean enable)
    {
        SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
        if(enable && updateClockCmdTimer == null)
        {
            updateClockCmdTimer = new Timer();
            updateClockCmdTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    getCommandHandler().addCommand(new Runnable() {
                        @Override
                        public void run() {
                            sendToDevice(update_clock_cmd);
                        }
                    });
                }
            }, 0L, 1000L);
        }
        else if(!enable && updateClockCmdTimer != null)
        {
            updateClockCmdTimer.cancel();
            updateClockCmdTimer = null;
        }
    }
    private void sendToDevice(byte[] data)
    {
        if(mBluetoothLeService != null && data != null)
        {
            byte[] singleByte = new byte[1];

            for(int i = 0; i < data.length; i++)
            {
                singleByte[0] = data[i];
                mBluetoothLeService.write(mNotifyCharacteristic, singleByte);
                threadSleep(100);
            }
        }
        else
        {
            threadSleep(100);
        }
    }

    private void updateAllDeviceParamsOnUiThread()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAllDeviceParams();
            }
        });
    }

    private void updateAllDeviceParams()
    {
        MainFragment frag = getMainFragment();
        if (frag != null )
        {
            SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
            while(!recv.paramQueue.isEmpty())
            {
                SpeedTuneParam paramUpdate = recv.paramQueue.poll();
                if(Looper.myLooper() == Looper.getMainLooper())
                {
                    //only call ui update on UI thread
                    frag.updateParam(paramUpdate);
                }

                if (paramUpdate instanceof SpeedTuneParamVL) {
                    String dispVal = ((SpeedTuneParamVL) paramUpdate).getDisplayValue();
                    String strTag = SpeedTuneReceiver.byteToString(paramUpdate.tag);
                    SpeedTuneData data = deviceParams.get(strTag);
                    if (data == null) data = deviceSetting.get(strTag);
                    if (data != null) {
                        data.displayVal = dispVal;
                        data.updatedVal = null;
                    }
                } else if (paramUpdate instanceof SpeedTuneParamFL) {
                    SpeedTuneParamFL paramFL = (SpeedTuneParamFL) paramUpdate;
                    for (HashMap.Entry<Integer, String> entry : paramFL.getMapEntrySet()) {
                        String dispVal = entry.getValue();
                        String strTag = SpeedTuneReceiver.byteToString(paramUpdate.tag) + "_" + entry.getKey();
                        SpeedTuneData data = deviceSetting.get(strTag);
                        if (data != null) {
                            data.displayVal = dispVal;
                            data.updatedVal = null;
                        }
                    }
                }
                if(paramUpdate.tag == 'H')
                {
                    heartBeatTimeStamp = System.currentTimeMillis()/1000;
                }

                if(paramUpdate.tag == 'm')
                {
                    bAllUserSettingReceived = true;
                }

                if(paramUpdate.tag == '|')
                {
                    bAllMethSettingReceived = true;
                }
            }
        }
    }

    public void sendSaveMapCmd()
    {
        getCommandHandler().addCommand(new Runnable() {
            @Override
            public void run() {
                Long now = System.currentTimeMillis()/1000;
                if(now - lastSaveCmdTimeStamp > CMD_INTERVAL)
                {
                    lastSaveCmdTimeStamp = now;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainFragment frag = getMainFragment();
                            if (frag != null) frag.setMapSpinnerEnabled(false);
                        }
                    });
                    SpeedTuneSaveMap saveMap = new SpeedTuneSaveMap();
                    if(saveMap.generateData(deviceSetting))
                    {
//                        Log.w(TAG, "saveMap: " + saveMap.toString());
                        sendToDevice(saveMap.cmdData);
                    }
                    else
                    {
                        Log.w(TAG, "unable to generate save data " + saveMap.toString());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainFragment frag = getMainFragment();
                            if (frag != null) frag.setMapSpinnerEnabled(true);
                        }
                    });
                }
                else
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showCommandIntervalWarning();
                            MainFragment frag = getMainFragment();
                            if (frag != null) frag.resetSpinner();
                        }
                    });

                }

            }
        });
    }

    public void sendSaveMethCmd()
    {
        if(bAllMethSettingReceived)
        {
            getCommandHandler().addCommand(new Runnable()
            {
                @Override
                public void run()
                {
                    Long now = System.currentTimeMillis() / 1000;
                    if (now - lastSaveCmdTimeStamp > CMD_INTERVAL)
                    {
                        lastSaveCmdTimeStamp = now;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MainFragment frag = getMainFragment();
                                if (frag != null) frag.setSaveMethEnabled(false);
                            }
                        });
                        setUpdateClockEnableOnCommandThread(false);
                        sendToDevice(reset_cmd);
                        SpeedTuneSaveMeth saveMeth = new SpeedTuneSaveMeth();
                        if (saveMeth.generateData(deviceSetting))
                        {
//                        Log.w(TAG, "saveMeth: " + saveMeth.toString());
                            sendToDevice(saveMeth.cmdData);
                        } else
                        {
                            Log.w(TAG, "unable to generate save data " + saveMeth.toString());
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MainFragment frag = getMainFragment();
                                if (frag != null) frag.setSaveMethEnabled(true);
                            }
                        }, 4000);
                    } else
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                showCommandIntervalWarning();
                            }
                        });
                    }

                }
            });
        }
        else
        {
            showToastText(getResources().getString(R.string.mainact_warn_reading_meth));
        }
    }

    public void sendSaveSettingCmd()
    {
        if(bAllUserSettingReceived)
        {
            getCommandHandler().addCommand(new Runnable()
            {
                @Override
                public void run()
                {
                    Long now = System.currentTimeMillis() / 1000;
                    if (now - lastSaveCmdTimeStamp > CMD_INTERVAL)
                    {
                        lastSaveCmdTimeStamp = now;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MainFragment frag = getMainFragment();
                                if (frag != null) frag.setSaveSettingsEnabled(false);
                            }
                        });
                        setUpdateClockEnableOnCommandThread(false);
                        sendToDevice(reset_cmd);
                        SpeedTuneSaveSetting saveSetting = new SpeedTuneSaveSetting();
                        if (saveSetting.generateData(deviceSetting))
                        {
//                        Log.w(TAG, "saveSetting: " + saveSetting.toString());
                            sendToDevice(saveSetting.cmdData);
                        } else
                        {
                            Log.w(TAG, "unable to generate save data " + saveSetting.toString());
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MainFragment frag = getMainFragment();
                                if (frag != null) frag.setSaveSettingsEnabled(true);
                            }
                        }, 4000);
                    } else
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                showCommandIntervalWarning();
                            }
                        });
                    }

                }
            });
        }
        else
        {
            showToastText(getResources().getString(R.string.mainact_warn_reading_user));
        }
    }

    public void setPlatformType(int type)
    {
        if(type >= PLATFORM_TYPE_MIN && type <= PLATFORM_TYPE_MAX)
        {
            nPlatformType = type;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putInt(PREF_PLATFORM_TYPE, type);
            editor.commit();
        }
    }

    public int getPlatformType()
    {
        return nPlatformType;
    }

    private void updateAllCodes()
    {
        CodesFragment frag = getCodesFragment();
        if (frag != null)
        {
            SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
            if(recv.isCodesReadDone)
            {
                deviceCodes.clear();
                while (!recv.codesQueue.isEmpty())
                {
                    SpeedTuneCode codeUpdate = recv.codesQueue.poll();
                    deviceCodes.add(codeUpdate.codes);
                }

                if(Looper.myLooper() == Looper.getMainLooper())
                {
                    frag.showAllCodes(deviceCodes);
                }
            }
            else
            {
                frag.showAllCodes(null);
            }
        }
    }

    public void sendReadCodesCmd(final boolean deleteFirst)
    {
        getCommandHandler().addCommand(new Runnable() {
            @Override
            public void run() {
                Long now = System.currentTimeMillis()/1000;
                if(now - lastSaveCmdTimeStamp > CMD_INTERVAL)
                {
                    byte[] read_codes = {'G'};
                    byte[] delete_codes = {'H'};
                    lastSaveCmdTimeStamp = now;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CodesFragment frag = getCodesFragment();
                            if (frag != null )
                            {
                                if(deleteFirst)
                                {
                                    frag.updateUIStatusDeleting();
                                }
                                else
                                {
                                    frag.updateUIStatusReading();
                                }
                            }
                        }
                    });

                    if(deleteFirst)
                    {
                        sendToDevice(delete_codes);
                        threadSleep(1000);
                    }

                    setUpdateClockEnableOnCommandThread(false);
                    setAutoParamUpdateEnableOnCommandThread(false);
                    SpeedTuneReceiver recv = SpeedTuneReceiver.getInstance();
                    recv.isAutoParseParam = false;
                    sendToDevice(reset_cmd);
                    threadSleep(400);
                    recv.clearCodesQueue();
                    recv.isAutoParseCodes = true;
                    recv.nPlatformType = nPlatformType;
                    sendToDevice(read_codes);
                    int counterTimeOut = 0;
                    while(!recv.isCodesReadDone && counterTimeOut < 50)
                    {
                        threadSleep(100);
                        counterTimeOut ++;
                    }
                    recv.isAutoParseCodes = false;
                    recv.isAutoParseParam = true;
                    setAutoParamUpdateEnableOnCommandThread(true);
                    sendToDevice(auto_param);
                    setUpdateClockEnableOnCommandThread(true);

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateAllCodes();
                            CodesFragment frag = getCodesFragment();
                            if (frag != null ) frag.setButtonsEnabled(true);
                        }
                    });

                }
                else
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            showCommandIntervalWarning();
                        }
                    });
                }

            }
        });
    }

    private void showCommandIntervalWarning()
    {
        showToastText(getResources().getString(R.string.mainact_warn_cmd_interval));
    }

    private void showToastText(String str)
    {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
