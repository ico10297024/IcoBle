package ico.ico.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import java.util.List;
import java.util.UUID;

/**
 * 蓝牙4.0帮助类，用于操作蓝牙
 * <p>
 * 目前功能：开启搜索，关闭搜索，设置开关。
 * <p>
 * 搜索支持筛选功能，通过{@link BleFilter}
 */
public class BleHelper {

    public static final String ACTION_FOUND = "com.sunruncn.ble.FOUND";
    /**
     * 蓝牙适配器
     */
    private static BluetoothAdapter mBleAdapter;
    /**
     * 用于搜索的通道ID,根据系统api,加了这个搜索效率比较快
     */
    UUID[] mServicesUUID;
    /**
     * 监听蓝牙状态更改
     */
    BroadcastReceiver stateChangedReceiver;
    /**
     * 当前界面的上下文
     */
    private Context mContext;
    /**
     * 设备发现的广播接收器
     */
    private BroadcastReceiver foundReceiver;
    /**
     * 回调
     */
    private BleCallback mBleCallback;
    /**
     * 蓝牙设备筛选器
     */
    private BleFilter mBleFilter;

    private IScanner scanner;

    /**
     * 该构造函数要求传入一个上下文以及回调对象,没有默认的蓝牙筛选器,也就是搜索到的所有ble都会进行回调
     *
     * @param context     当前界面的上下文
     * @param bleCallback 回调
     */
    public BleHelper(Context context, BleCallback bleCallback) {
        this(context, null, bleCallback);
    }

    /**
     * 该构造函数要求传入上下文,搜索服务的uuid以及回调对象,可以根据搜索服务器的uuid进行定向搜索
     *
     * @param context      当前界面的上下文
     * @param servicesUUID 用于定向搜索的服务ID
     * @param bleCallback  回调
     */
    public BleHelper(Context context, UUID[] servicesUUID, BleCallback bleCallback) {
        this.mContext = context;
        this.mBleCallback = bleCallback;
        this.mServicesUUID = servicesUUID;
        //绑定广播
        foundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) -1);
                synchronized (mBleCallback) {
                    if (mBleFilter != null && mBleFilter.onBleFilter(device)) {
                        mBleCallback.found(device, rssi);
                    } else {
                        log.d(String.format("name:%s,mac:%s,rssi:%d,过滤设备", device.getName(), device.getAddress(), rssi));
                    }
                }
            }
        };
        mContext.registerReceiver(foundReceiver, new IntentFilter(ACTION_FOUND));
        mContext.registerReceiver(foundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //初始化搜索器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = new Scanner21();
        } else {
            scanner = new Scanner();
        }
//        scanner = new Scanner();
    }

    //region static

    /**
     * 获取默认的蓝牙适配器
     *
     * @param context
     * @return
     */
    public static BluetoothAdapter getBleAdapter(Context context) {
        if (mBleAdapter == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bleMgr = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                mBleAdapter = bleMgr.getAdapter();
            } else {
                mBleAdapter = BluetoothAdapter.getDefaultAdapter();
            }
        }
        return mBleAdapter;
    }

    /**
     * 获取蓝牙的开启状态
     *
     * @return
     */
    public static boolean isEnable(Context context) {
        return BleHelper.getBleAdapter(context).isEnabled();
    }

    /**
     * 启动蓝牙
     */
    public static void enable(Context context) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivity(enableBtIntent);
    }

    /**
     * 关闭蓝牙
     */
    public static void disable(Context context) {
        BleHelper.getBleAdapter(context).disable();
    }
    //endregion

    /**
     * 不使用该类时请调用该函数销毁
     */
    public void onDestroy() {
        mContext.unregisterReceiver(foundReceiver);
        unregisterStateChanged();
        stopScan();
    }

    /**
     * 开启搜索
     */
    public void startScan() {
        scanner.startScan();
    }

    /**
     * 停止搜索
     */
    public void stopScan() {
        scanner.stopScan();
    }

    public BleHelper setServicesUUID(String... servicesUUID) {
        this.mServicesUUID = new UUID[servicesUUID.length];
        for (int i = 0; i < servicesUUID.length; i++) {
            mServicesUUID[i] = UUID.fromString(servicesUUID[i]);
        }
        return this;
    }

    /**
     * 注册蓝牙状态更改的广播
     *
     * @param receiver
     */
    public void registerStateChanged(BroadcastReceiver receiver) {
        unregisterStateChanged();
        stateChangedReceiver = receiver;
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(stateChangedReceiver, intentFilter);
    }

    /**
     * 注销蓝牙状态更改的广播
     */
    public void unregisterStateChanged() {
        if (stateChangedReceiver != null) {
            mContext.unregisterReceiver(stateChangedReceiver);
            stateChangedReceiver = null;
        }
    }

    //region GETSET
    public BleCallback getBleSmCallback() {
        return mBleCallback;
    }

    public BleHelper setBleSmCallback(BleCallback mBleCallback) {
        this.mBleCallback = mBleCallback;
        return this;
    }

    public BleFilter getBleFilter() {
        return mBleFilter;
    }

    public BleHelper setBleFilter(BleFilter bleFilter) {
        this.mBleFilter = bleFilter;
        return this;
    }

    //endregion

    /**
     * 蓝牙筛选器
     * <p>
     * 当搜索到一个蓝牙设备,先调用筛选器,当筛选器返回true时,才会调用回调
     */
    public interface BleFilter {
        boolean onBleFilter(BluetoothDevice device);
    }

    interface IScanner {
        void startScan();

        void stopScan();
    }


    /**
     * 5.0-使用的搜索器
     */
    class Scanner implements IScanner {
        ScanThread scanThread;
        /**
         * 5.0以下,蓝牙搜索使用的回调函数
         * 蓝牙设备搜索的回调,{@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)}
         */
        BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Intent intent = new Intent(ACTION_FOUND);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                intent.putExtra(BluetoothDevice.EXTRA_RSSI, (short) rssi);
                mContext.sendBroadcast(intent);
            }
        };

        @Override
        public void startScan() {
            if (scanThread == null || scanThread.isClosed()) {
                scanThread = new ScanThread();
                scanThread.start();
            }
        }

        @Override
        public void stopScan() {
            if (scanThread != null) {
                scanThread.close();
                scanThread = null;
            }
        }

        /**
         * 用于定时间隔关闭和开启搜索的线程
         * <p>
         * 由于5.0以下安卓系统在搜索到一个设备后将不会再搜索到这个设备,所以需要做定时开关来反复搜索
         */
        class ScanThread extends IcoThread {

            @Override
            public void run() {
                while (!isClosed()) {
                    if (mServicesUUID != null) {
                        mBleAdapter.startLeScan(mServicesUUID, leScanCallback);
                    } else {
                        mBleAdapter.startLeScan(leScanCallback);
                    }
                    try {
                        Thread.currentThread().sleep(1000l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mBleAdapter.stopLeScan(leScanCallback);
                }
            }
        }
    }

    /**
     * 5.0+使用的搜索器
     */
    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    class Scanner21 implements IScanner {
        /**
         * 5.0+,蓝牙搜索使用的回调函数
         */
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                log.w("onScanResult=======" + callbackType + "||" + result);
                Intent intent = new Intent(ACTION_FOUND);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, result.getDevice());
                intent.putExtra(BluetoothDevice.EXTRA_RSSI, (short) result.getRssi());
                mContext.sendBroadcast(intent);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                log.w("onBatchScanResults=======" + results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                log.w("onBatchScanResults=======" + errorCode);
            }
        };

        @Override
        public void startScan() {
            getBleAdapter(mContext).getBluetoothLeScanner().startScan(scanCallback);
        }

        @Override
        public void stopScan() {
            getBleAdapter(mContext).getBluetoothLeScanner().stopScan(scanCallback);
        }
    }
}