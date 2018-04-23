package ico.ico.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙设备连接器
 * <p>
 * 该类负责与蓝牙设备的连接，并且与蓝牙设备的通信
 * <p>
 * 同时还整合了BleHelper的搜索功能，在发送数据时根据当前状态来执行不同的操作，具体请看{@link BleSocket#send(byte[], String)}
 * <p>
 * 注意事项：
 * <p>
 * 1.蓝牙的数据发送后需要延迟N毫秒再关闭连接，否则发送的数据将会丢失
 * <p>
 * 2.如果是从外部传入BleHelper，那么BleHelper需要实现found函数，设置蓝牙设备对象进行连接操作，具体可以看构造函数中的found实现
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleSocket {
    public static final String TAG = "BLE";
    /**
     * 服务未发现
     * 连接错误
     */
    public static final int FAIL_STATUS_SERVICES_UNDISCOVER = 1;
    /**
     * 蓝牙连接未连接，直接断开
     * 连接错误
     */
    public static final int FAIL_STATUS_UNCONNECT_DISCONNECT = 2;
    /**
     * 通道发送函数直接返回false
     * 发送错误
     */
    public static final int FAIL_STATUS_NONE = 3;
    /**
     * 用于发送数据的通道未找到
     * 发送错误
     */
    public static final int FAIL_STATUS_PATH_NOT_FOUND = 4;
    /**
     * 发送数据通道没有写入特性
     * 发送错误
     */
    public static final int FAIL_STATUS_PATH_NOT_WRITE = 5;
    /*列举蓝牙操作时各个阶段的状态*/
    /**
     * 蓝牙处于断开状态,这种状态是在连接后再断开的状态
     */
    public static final int STATE_DISCONNECTED = 0;
    /**
     * 蓝牙处于连接中
     */
    public static final int STATE_CONNECTING = 1;
    /**
     * 蓝牙已连接成功,正在查找服务中
     */
    public static final int STATE_CONNECTED = 2;
    /**
     * 蓝牙真正意义上的连接成功,可以进行数据的交互
     */
    public static final int STATE_CONNECTED_DISCOVER = 3;
    /**
     * 默认状态下,处于刚创建
     */
    public static final int STATE_UNKNOWN = -1;
    /**
     * 当前界面的上下文
     */
    public Context mContext;
    /**
     * 用于读取数据的通道UUID
     */
    public String readUUID;
    /**
     * 蓝牙设备对象
     */
    protected BluetoothDevice mBluetoothDevice;
    /**
     * 蓝牙设备连接对象
     */
    protected BluetoothGatt mBluetoothGatt;
    /**
     * 连接状态
     */
    protected int mConnectionState = STATE_UNKNOWN;
    /**
     * 连接状态的对象锁
     */
    protected Object mConnectionStateLock = new Object();
    /**
     * 用于搜索的通道ID,根据系统api,加了这个搜索效率比较快
     */
    String[] mServicesUUID;
    /**
     * 在蓝牙搜索时,用于筛选的关键字
     */
    String mKeywordFilter;
    /**
     * 在蓝牙搜索时,用于何种类型的筛选
     */
    int mType;

    Handler mHandler;
    /**
     * 回调
     */
    BleCallback mBleCallback;
    /**
     * 数据通道
     */
    private List<BluetoothGattService> mBluetoothLeServices;
    /**
     * 蓝牙操作器
     */
    private BleHelper mBleHelper;
    /**
     * 数据缓冲
     */
    private List<BleInstruct> mDataBuffer = new ArrayList<>();
    /**
     * 标志该连接是否已设置关闭
     */
    private boolean closed = false;

    public BleSocket(Context context, BleCallback bleCallback) {
        this.mContext = context;
        this.mBleCallback = bleCallback;
        this.mHandler = new Handler(mContext.getMainLooper());
    }

    public BleSocket(Context context, BleCallback bleCallback, BluetoothDevice bluetoothDevice) {
        this(context, bleCallback);
        this.mBluetoothDevice = bluetoothDevice;
    }

    /**
     * 通过给定参数创建蓝牙筛选器
     * <p>
     * 使用该函数创建的Socket，在发送时会根据当前不同的状态来执行不同的操作，具体请看{@link BleSocket#send(byte[], String)}
     *
     * @param context
     * @param bleCallback
     * @param filter      筛选关键字，如果是mac，格式为ABCDEFGHIJKL
     * @param type        筛选类型，0mac，1name
     */
    public BleSocket(Context context, BleCallback bleCallback, String filter, int type) {
        this(context, bleCallback);
        setBleFilterCondition(filter, type);
    }

    /**
     * 通过给定参数创建蓝牙筛选器
     * <p>
     * 使用该函数创建的Socket，在发送时会根据当前不同的状态来执行不同的操作，具体请看{@link BleSocket#send(byte[], String)}
     *
     * @param context
     * @param bleCallback
     * @param servicesUUID 筛选具有该UUID的设备
     */
    public BleSocket(Context context, BleCallback bleCallback, UUID[] servicesUUID) {
        this(context, bleCallback);
        BleHelper bleHelper = new BleHelper(context, servicesUUID, new BleCallback() {
            @Override
            public void found(BluetoothDevice device, int rssi) {
                synchronized (this) {
                    if (mBluetoothDevice == null) {
                        //设置蓝牙设备对象
                        mBluetoothDevice = device;
                        //停止搜索
                        mBleHelper.stopScan();
                        //调用回调
                        mBleCallback.found(device, rssi);
                        //连接设备
                        connect();
                    }
                }
            }
        });
        setBleHelper(bleHelper);
    }

    /**
     * 同{@link BleSocket#BleSocket(Context, BleCallback, String, int)}
     *
     * @param context
     * @param bleCallback
     * @param bleHelper
     */
    public BleSocket(Context context, BleCallback bleCallback, BleHelper bleHelper) {
        this(context, bleCallback);
        setBleHelper(bleHelper);
    }

    /**
     * 连接蓝牙设备，并获取服务通道
     */
    public void connect() {
        if (getConnectionState() == STATE_CONNECTED) {
            log.w(String.format("%s,设备已连接，连接操作被取消", toString()), TAG);
            return;
        }
        log.w(String.format("%s,设备开始连接", toString()), TAG);
//        Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
//            @Override
//            public void call(String s) {
        synchronized (mConnectionStateLock) {
            mConnectionState = STATE_CONNECTING;
        }
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, new MyBluetoothGattCallback());
//            }
//        });
    }

    /**
     * 关闭socket,关闭后该socket不可用,必须使用{@link this#reset()}函数来恢复到初始状态
     */
    public void close() {
        log.e(String.format("%s,close", BleSocket.this.toString()), TAG);
        closed = true;
//        Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
//            @Override
//            public void call(String s) {
        //移除数据缓存区的数据
        mDataBuffer.clear();
        //关闭搜索
        if (mBleHelper != null) {
            mBleHelper.stopScan();
        }
        //关闭连接
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mBluetoothLeServices = null;
        }
        //更改连接状态
        synchronized (mConnectionStateLock) {
            mConnectionState = STATE_DISCONNECTED;
        }
//            }
//        });
    }


    /**
     * 不使用该类时请调用该函数销毁
     */
    public void onDestroy() {
        close();
        if (mBleHelper != null) {
            mBleHelper.onDestroy();
        }
    }

    /**
     * 获取当前socket的关闭状态
     *
     * @return
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 将socket恢复到初始状态
     */
    public void reset() {
        synchronized (mConnectionStateLock) {
            mConnectionState = STATE_UNKNOWN;
        }
        mBluetoothDevice = null;
        mBluetoothGatt = null;
        mBluetoothLeServices = null;
        closed = false;
    }

    /**
     * 获取当前socket的连接状态
     *
     * @return
     */
    public int getConnectionState() {
        return mConnectionState;
    }

    /**
     * 发送蓝牙数据
     * <p>
     * 无蓝牙设备对象-无蓝牙操作器-日志
     * <p>
     * 无蓝牙设备对象-有蓝牙操作器-搜索
     * <p>
     * 有蓝牙设备对象-未连接-连接
     * <p>
     * 有蓝牙设备对象-已连接-通道未找到-日志
     * <p>
     * 有蓝牙设备对象-已连接-通道找到-发送
     *
     * @param _sendInstruct 发送的数据
     * @param uuid          数据发送的通道ID
     * @return
     */
    public boolean send(byte[] _sendInstruct, String uuid) {
        mDataBuffer.add(new BleInstruct(_sendInstruct, uuid));
        return push();
    }

    /**
     * 发送数据缓冲区中的指令
     * <p>
     * 无蓝牙设备对象-无蓝牙操作器-日志
     * <p>
     * 无蓝牙设备对象-有蓝牙操作器-搜索
     * <p>
     * 有蓝牙设备对象-未连接-连接
     * <p>
     * 有蓝牙设备对象-已连接-通道未找到-日志
     * <p>
     * 有蓝牙设备对象-已连接-通道找到-发送
     *
     * @return
     */
    public boolean push() {
        if (mDataBuffer.size() == 0) {
            return true;
        }
        /*有无蓝牙设备对*/
        if (mBluetoothDevice == null) {
            if (mBleHelper != null) {
                mBleHelper.startScan();
                //TODO
//                log.w("===" + BleHelper.getBleAdapter(mContext).getBondedDevices().toString());
//                log.w("===" + Common.insert(mKeywordFilter, ":", 2));
//                if (mType == 0 && !TextUtils.isEmpty(mKeywordFilter)) {
//                    String mac = Common.insert(mKeywordFilter, ":", 2);
//                    if (BleHelper.getBleAdapter(mContext).checkBluetoothAddress(mac)) {
//                        BluetoothDevice bluetoothDevice = BleHelper.getBleAdapter(mContext).getRemoteDevice(mac);
//                        if (bluetoothDevice != null) {
//                            log.w(String.format("已知%s,通过getRemoteDevice已成功创建设备对象", mac), TAG);
//                            //设置蓝牙设备对象
//                            mBluetoothDevice = bluetoothDevice;
//                            //连接设备
//                            connect();
//                        } else {
//                            mBleHelper.startScan();
//                        }
//                    } else {
//                        mBleHelper.startScan();
//                    }
//                } else {
//                    mBleHelper.startScan();
//                }
            } else {
                log.e("无设备对象，或请设置BleHelper来帮助自动化", TAG);
            }
        } else if (getConnectionState() == STATE_UNKNOWN) {
            connect();
        } else {
            String uuid = mDataBuffer.get(0).getUuid();
            byte[] data = mDataBuffer.get(0).getInstruct();
            mDataBuffer.remove(0);
            //找到要发送的通道
            BluetoothGattCharacteristic characteristic = find(uuid);
            if (characteristic == null) {
                mBleCallback.sendFail(this, data, FAIL_STATUS_PATH_NOT_FOUND);
                return false;
            }
            if (!canWrite(characteristic)) {
                mBleCallback.sendFail(this, data, FAIL_STATUS_PATH_NOT_WRITE);
                return false;
            }
            characteristic.setValue(data);
            log.w(String.format("%s,发送数据：%s；通道：%s", this.toString(), Common.bytes2Int16(" ", data), uuid), TAG);
            boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
            if (!success) {
                mBleCallback.sendFail(this, data, FAIL_STATUS_NONE);
            }
            return success;
        }
        return false;
    }

    /**
     * 判断传入的通道是否具有读取特性
     *
     * @param characteristic 服务通道对象
     * @return
     */
    public boolean canRead(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & characteristic.PROPERTY_READ) != 0;
    }

    /**
     * 判断传入的通道是否具有写入特性
     *
     * @param characteristic 服务通道对象
     * @return
     */
    public boolean canWrite(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * 判断传入的通道是否具有通知的特性
     *
     * @param characteristic 服务通道对象
     * @return
     */
    public boolean canNotify(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & characteristic.PROPERTY_NOTIFY) != 0;
    }

    /**
     * 设置要读取数据的通道的UUID
     *
     * @param readUUID 通道的id
     */
    public void setReadUUID(String readUUID) {
        this.readUUID = readUUID;
    }

    public BleSocket setServicesUUID(String... servicesUUID) {
        this.mServicesUUID = servicesUUID;
        if (mBleHelper != null) {
            mBleHelper.setServicesUUID(servicesUUID);
        }
        return this;
    }

    /**
     * 获取该socket对应的蓝牙操作对象
     *
     * @return
     */
    public BleHelper getBleHelper() {
        return mBleHelper;
    }

    /**
     * 设置该socket的蓝牙操作对象
     *
     * @param bleHelper
     */
    public void setBleHelper(BleHelper bleHelper) {
        this.mBleHelper = bleHelper;
        if (mBleHelper != null && mServicesUUID != null) {
            mBleHelper.setServicesUUID(mServicesUUID);
        }
    }

    /**
     * 设置蓝牙过滤的条件
     *
     * @param filter 筛选关键字，如果是mac，格式为ABCDEFGHIJKL
     * @param type   筛选类型，0mac，1name
     */
    public void setBleFilterCondition(String filter, int type) {
        this.mKeywordFilter = filter;
        this.mType = type;
        if (mBleHelper == null) {
            BleHelper bleHelper = new BleHelper(mContext, new BleCallback() {
                @Override
                public void found(BluetoothDevice device, int rssi) {
                    synchronized (this) {
                        if (mBluetoothDevice == null) {
                            //设置蓝牙设备对象
                            mBluetoothDevice = device;
                            //停止搜索
                            mBleHelper.stopScan();
                            //调用回调
                            mBleCallback.found(device, rssi);
                            //连接设备
                            connect();
                        }
                    }
                }
            });
            setBleHelper(bleHelper);
        }
        //设置过滤器
        mBleHelper.setBleFilter(new BleHelper.BleFilter() {
            @Override
            public boolean onBleFilter(BluetoothDevice device) {
                switch (type) {
                    case 0:
                        return device.getAddress().replace(":", "").equalsIgnoreCase(mKeywordFilter);
                    case 1:
                        if (TextUtils.isEmpty(device.getName())) {
                            return false;
                        }
                        return device.getName().indexOf(mKeywordFilter) != -1;
                }
                return true;
            }
        });
    }

    /**
     * 设置蓝牙操作的回调对象
     *
     * @param bleCallback
     */
    public void setBleCallback(BleCallback bleCallback) {
        this.mBleCallback = bleCallback;
    }

    /**
     * 设置通道通知,调用该函数前需先调用{@link this#setReadUUID(String)}设置通道的id
     */
    public void setReadCharacteristicNotification() {
        if (TextUtils.isEmpty(readUUID)) {
            log.e("没有设置readUUID，请先设置readUUID", TAG);
            return;
        }
        BluetoothGattCharacteristic characteristic = find(readUUID);
        if (characteristic == null) {
            log.e("没有找到readUUID对应的通道，UUID：" + readUUID, TAG);
            return;
        }
        if (!canNotify(characteristic)) {
            log.e("通道没有通知的特性，UUID：" + readUUID, TAG);
            return;
        }
        if (mBluetoothGatt == null) {
            return;
        }
        synchronized (mBluetoothGatt) {
            boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            log.w(String.format("设置通道通知%s，UUID：" + characteristic.getUuid(), isEnableNotification ? "成功" : "失败"), TAG);
            //TODO ble和dm的uuid相同,write后需要延迟
            if (isEnableNotification) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

    /**
     * 根据UUID查找通道
     *
     * @param uuid
     * @return
     */
    public BluetoothGattCharacteristic find(String uuid) {
        log.d("BleSocket find");
        if (mBluetoothGatt != null) {
            BluetoothGattCharacteristic characteristic = null;
            for (BluetoothGattService service : mBluetoothLeServices) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (int i = 0; i < characteristics.size(); i++) {
                    characteristic = characteristics.get(i);
                    log.d("BleSocket find characteristic：" + characteristic.getUuid().toString());
                    if (characteristic.getUuid().toString().equalsIgnoreCase(uuid)) {
                        return characteristic;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将该socket的信息拼接成字符串返回,设备名+设备mac地址
     *
     * @return
     */
    @Override
    public String toString() {
        if (mBluetoothDevice != null) {
            return "name:" + mBluetoothDevice.getName() + ",mac:" + mBluetoothDevice.toString();
        } else if (!TextUtils.isEmpty(mKeywordFilter)) {
            return "mKeywordFilter:" + mKeywordFilter;
        } else {
            return null;
        }
    }

    /**
     * 获取蓝牙设备对象
     *
     * @return
     */
    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    /**
     * 设置蓝牙设备对象
     *
     * @param bluetoothDevice
     */
    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.mBluetoothDevice = bluetoothDevice;
    }

    /**
     * 停止蓝牙搜索
     */
    public void stopScan() {
        if (mBleHelper != null) {
            mBleHelper.stopScan();
            mBleHelper = null;
        }
    }

    class MyBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            log.i(String.format("onCharacteristicWrite: uuid：%s； status：%d；value：%s", characteristic.getUuid().toString(), status, Common.bytes2Int16(" ", characteristic.getValue())));
//            log.i(String.format("onCharacteristicWrite: status：%d；", status));
            mBleCallback.sendSuccess(BleSocket.this, characteristic.getValue());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            log.i(String.format("onCharacteristicRead: uuid：%s；status：%d；value：%s", characteristic.getUuid().toString(), status, Common.bytes2Int16(" ", characteristic.getValue())));
//            log.i(String.format("onCharacteristicRead: status：%d；", status));
            mBleCallback.receive(BleSocket.this, characteristic.getUuid().toString(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            log.i(String.format("onCharacteristicChanged: uuid：%s；value：%s", characteristic.getUuid().toString(), Common.bytes2Int16(" ", characteristic.getValue())));
//            log.i(String.format("onCharacteristicChanged"));
            mBleCallback.receive(BleSocket.this, characteristic.getUuid().toString(), characteristic.getValue());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            log.i(String.format("onServicesDiscovered: status：%d；", status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothLeServices = mBluetoothGatt.getServices();
                synchronized (mConnectionStateLock) {
                    mConnectionState = STATE_CONNECTED_DISCOVER;
                }
                setReadCharacteristicNotification();
                //这是使用延迟,是由于setReadCharacteristicNotification后不能立即发送数据,立即发送会直接返回false
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBleCallback.connectSuccess(BleSocket.this);
                        push();
                    }
                }, 500);
            } else {
                mBleCallback.connectFail(BleSocket.this, FAIL_STATUS_SERVICES_UNDISCOVER);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            log.i(String.format("onConnectionStateChange: mConnectionState：%d；newState：%d；" + mBluetoothGatt, mConnectionState, newState));
            if (mBluetoothGatt == null) {
                return;
            }
            if (getConnectionState() == BluetoothAdapter.STATE_CONNECTING
                    && newState == BluetoothAdapter.STATE_CONNECTED) {
                synchronized (mConnectionStateLock) {
                    mConnectionState = STATE_CONNECTED;
                }
                mBluetoothGatt.discoverServices();
            }
            if (newState == BluetoothAdapter.STATE_DISCONNECTED) {//连接断开
                if (getConnectionState() == STATE_CONNECTED || getConnectionState() == STATE_CONNECTED_DISCOVER) {//设备已连接上
                    close();
                    mBleCallback.disconnect(BleSocket.this);
                } else if (getConnectionState() == STATE_CONNECTING) {//设备没有连接上，直接断开
                    close();
                    mBleCallback.connectFail(BleSocket.this, FAIL_STATUS_UNCONNECT_DISCONNECT);
                }
            }
        }
    }

    /**
     * 封装的指令对象,封装了数据和对应的数据通道
     */
    public class BleInstruct {
        //要发送的指令
        byte[] instruct;
        //发送使用的通道
        String uuid;

        public BleInstruct() {
        }

        public BleInstruct(byte[] instruct, String uuid) {
            this.instruct = instruct;
            this.uuid = uuid;
        }

        public byte[] getInstruct() {
            return instruct;
        }

        public void setInstruct(byte[] instruct) {
            this.instruct = instruct;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
}
