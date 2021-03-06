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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 蓝牙4.0帮助类，用于操作蓝牙
 * <p>
 * 目前功能：开启搜索，关闭搜索，设置开关。
 * <p>
 * 搜索支持筛选功能，通过{@link BleFilter}
 */
public class BleHelper {

    public static final String TAG = BleHelper.class.getSimpleName();

    public static final String ACTION_FOUND = "com.sunruncn.ble.FOUND";
    /**
     * 蓝牙适配器
     */
    private static BluetoothAdapter mBleAdapter;
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

    /**
     * 搜索器,根据不同版本使用不同的搜索api,不过最终都通过{@link BluetoothDevice#ACTION_FOUND}来接收结果
     * 在最新版本中使用混合搜索器,也就是同时调用不同版本的api来实现最大范围的搜索
     */
    private IScanner scanner;

    /**
     * 经过测试,某些设备在被搜索到一次后不会被再一次搜索到,所以间隔几秒停止再开启搜索的方式进行搜索
     * 而某些设备需要持续开启搜索几秒之后才可能被搜索到
     * 综上,所以需要修改这个间隔时间来达到一个平衡
     * 目前10S并不是一个标准值
     */
    private long interval = 10 * 1000l;

    /**
     * 该构造函数要求传入一个上下文以及回调对象,没有默认的蓝牙筛选器,也就是搜索到的所有ble都会进行回调
     *
     * @param context     当前界面的上下文
     * @param bleCallback 回调
     */
    public BleHelper(Context context, BleCallback bleCallback) {
        this.mContext = context;
        this.mBleCallback = bleCallback;
        //绑定广播
        foundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) -1);
                synchronized (mBleCallback) {
                    if (mBleFilter == null) {
                        mBleCallback.found(device, rssi);
                    } else if (mBleFilter.onBleFilter(device)) {
                        mBleCallback.found(device, rssi);
                    } else {
                        log.d(String.format("name:%s,mac:%s,rssi:%d,过滤设备", device.getName(), device.getAddress(), rssi, TAG, BleHelper.this.hashCode() + ""));
                    }
                }
            }
        };
        mContext.registerReceiver(foundReceiver, new IntentFilter(ACTION_FOUND));
        mContext.registerReceiver(foundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //初始化搜索器
        //TODO 根据测试,高版本系统使用低版本的搜索函数,比使用高版本的搜索函数效率高
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            scanner = new Scanner21();
//        } else {
//            scanner = new Scanner();
//        }
        scanner = new MixScanner();
//        scanner = new Scanner21();
    }

    //region static

    /**
     * 获取默认的蓝牙适配器
     *
     * @param context 当前上下文
     * @return BluetoothAdapter
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
     * @param context 当前上下文
     * @return boolean
     */
    public static boolean isEnable(Context context) {
        return BleHelper.getBleAdapter(context).isEnabled();
    }

    /**
     * 启动蓝牙
     *
     * @param context 当前上下文
     */
    public static void enable(Context context) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivity(enableBtIntent);
    }

    /**
     * 关闭蓝牙
     *
     * @param context 当前上下文
     */
    public static void disable(Context context) {
        BleHelper.getBleAdapter(context).disable();
    }
    //endregion

    /** 销毁 */
    public void onDestroy() {
        mContext.unregisterReceiver(foundReceiver);
        unregisterStateChanged();
        stopScan();
    }

    private AtomicBoolean isScanning = new AtomicBoolean();

    /** 开启搜索 */
    public synchronized boolean isScanning() {
        return isScanning.get();
    }

    /** 开启搜索 */
    public synchronized void startScan() {
        log.d("开启搜索", TAG, BleHelper.this.hashCode() + "");
        scanner.startScan();
        isScanning.set(true);
    }

    /** 停止搜索 */
    public synchronized void stopScan() {
        log.d("结束搜索", TAG, BleHelper.this.hashCode() + "");
        scanner.stopScan();
        isScanning.set(false);
    }

    /**
     * 注册蓝牙状态更改的广播
     *
     * @param receiver 广播接收器
     */
    public void registerStateChanged(BroadcastReceiver receiver) {
        unregisterStateChanged();
        stateChangedReceiver = receiver;
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(stateChangedReceiver, intentFilter);
    }

    /** 注销蓝牙状态更改的广播 */
    public void unregisterStateChanged() {
        if (stateChangedReceiver != null) {
            mContext.unregisterReceiver(stateChangedReceiver);
            stateChangedReceiver = null;
        }
    }
    //region GETSET

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

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
        public synchronized void startScan() {
            if (scanThread == null || scanThread.isClosed()) {
                scanThread = new ScanThread();
                scanThread.start();
            }
        }

        @Override
        public synchronized void stopScan() {
            if (scanThread != null) {
                scanThread.close();
                scanThread = null;
            }
        }

        /**
         * 用于定时间隔关闭和开启搜索的线程
         * <p>
         * 由于5.0以下安卓系统在搜索到一个设备后将不会再搜索到这个设备,所以需要做定时开关来反复搜索
         * <p>
         * 20180425 华为荣耀6,api19,测试可以反复搜索到同一个设备,所以暂时不用这个线程
         */
        class ScanThread extends IcoThread {

            @Override
            public void run() {
                while (!isClosed()) {
                    getBleAdapter(mContext).startLeScan(leScanCallback);
                    getBleAdapter(mContext).startDiscovery();
                    try {
                        Thread.currentThread().sleep(interval);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                    getBleAdapter(mContext).stopLeScan(leScanCallback);
                    getBleAdapter(mContext).cancelDiscovery();
                }
            }
        }
    }

    /** 5.0+使用的搜索器 */
    class Scanner21 implements IScanner {
        /**
         * 5.0+,蓝牙搜索使用的回调函数
         */
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                log.d("onScanResult=======" + callbackType + "||" + result, TAG, BleHelper.this.hashCode() + "");
                Intent intent = new Intent(ACTION_FOUND);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, result.getDevice());
                intent.putExtra(BluetoothDevice.EXTRA_RSSI, (short) result.getRssi());
                mContext.sendBroadcast(intent);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                log.d("onBatchScanResults=======" + results, TAG, BleHelper.this.hashCode() + "");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                log.d("onBatchScanResults=======" + errorCode, TAG, BleHelper.this.hashCode() + "");
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


    /** 混合搜索器，将同时启动老版本和新版本的搜索 */
    class MixScanner implements IScanner {

        IScanner scanner;
        IScanner scanner21;


        public MixScanner() {
            scanner = new Scanner();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scanner21 = new Scanner21();
            }
        }

        @Override
        public void startScan() {
            scanner.startScan();
            if (scanner21 != null) {
                scanner21.startScan();
            }
        }

        @Override
        public void stopScan() {
            scanner.stopScan();
            if (scanner21 != null) {
                scanner21.stopScan();
            }
        }
    }
}
