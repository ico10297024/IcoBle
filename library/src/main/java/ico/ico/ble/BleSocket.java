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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙设备连接器
 * <p>
 * 该类负责与蓝牙设备的连接，并且与蓝牙设备的通信
 * <p>
 * 同时还整合了BleHelper的搜索功能，在发送数据时根据当前状态来执行不同的操作，具体请看{@link BleSocket#send(byte[])} {@link BleSocket#push()}
 * <p>
 * 注意事项：
 * <p>
 * 1.蓝牙的数据发送后需要延迟N毫秒再关闭连接，否则发送的数据将会丢失
 * <p>
 * 2.如果是从外部传入BleHelper，那么BleHelper需要实现found函数，设置蓝牙设备对象进行连接操作，具体可以看构造函数中的found实现
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleSocket {
    public static final String TAG = BleSocket.class.getSimpleName();
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
    /**
     * 无法识别的设备种类
     */
    public static final int FAIL_STATUS_KNOWN_DEVICE = 6;
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
    protected Context mContext;

    /**
     * 设置支持的蓝牙设备种类
     * <p>
     * BleSocket将在连接成功后通过搜索通道的方式确定当前连接设备所对应的设备种类
     * <p>
     * 并从对应的BLeUUIDI中获取该设备收发数据的通道UUID
     */
    protected HashSet<BLeUUIDI> mSupportBle;
    /**
     * 标识当前设备的种类
     */
    protected BLeUUIDI mCurrentBleUUID;
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
     * 蓝牙操作器
     */
    private BleHelper mBleHelper;
    /**
     * 数据缓冲
     */
    private List<byte[]> mDataBuffer = new ArrayList<>();
    /**
     * 标志该连接是否已设置关闭
     */
    private boolean closed = false;

    /**
     * 蓝牙设备连接使用的回调
     */
    private MyBluetoothGattCallback mMyBluetoothGattCallback = new MyBluetoothGattCallback();


    /**
     * 通过给定参数创建蓝牙筛选器
     *
     * @param context     上下文
     * @param bleCallback 蓝牙回调
     * @param supportBle  支持的设备种类列表,连接成功后将自动进行匹配并使用对应的收发通道UUID
     */
    public BleSocket(Context context, BleCallback bleCallback, BLeUUIDI... supportBle) {
        this.mContext = context;
        this.mBleCallback = bleCallback;
        setSupportBle(supportBle);
        this.mHandler = new Handler(mContext.getMainLooper());
    }

    /**
     * 通过给定参数创建蓝牙筛选器
     *
     * @param context         上下文
     * @param bleCallback     蓝牙回调
     * @param bluetoothDevice 直接给定蓝牙设备对象,进行操作
     * @param supportBle      支持的设备种类列表,连接成功后将自动进行匹配并使用对应的收发通道UUID
     */
    public BleSocket(Context context, BleCallback bleCallback, BluetoothDevice bluetoothDevice, BLeUUIDI... supportBle) {
        this(context, bleCallback, supportBle);
        this.mBluetoothDevice = bluetoothDevice;
    }

    /**
     * 通过给定参数创建蓝牙筛选器
     *
     * @param context     上下文
     * @param bleCallback 蓝牙回调
     * @param filter      筛选关键字，如果是mac，格式为ABCDEFGHIJKL
     * @param type        筛选类型，0mac，1name
     * @param supportBle  支持的设备种类列表,连接成功后将自动进行匹配并使用对应的收发通道UUID
     */
    public BleSocket(Context context, BleCallback bleCallback, String filter, int type, BLeUUIDI... supportBle) {
        this(context, bleCallback, supportBle);
        setBleFilterCondition(filter, type);
    }

    /**
     * 通过给定参数创建蓝牙筛选器
     *
     * @param context     上下文
     * @param bleCallback 蓝牙回调
     * @param bleHelper   蓝牙的帮助工具,一般用于搜索蓝牙和蓝牙开关
     * @param supportBle  支持的设备种类列表,连接成功后将自动进行匹配并使用对应的收发通道UUID
     */
    public BleSocket(Context context, BleCallback bleCallback, BleHelper bleHelper, BLeUUIDI... supportBle) {
        this(context, bleCallback, supportBle);
        setBleHelper(bleHelper);
    }

    /**
     * 连接蓝牙设备，并获取服务通道
     */
    public void connect() {
        if (getConnectionState() == STATE_CONNECTED) {
            log.w(String.format("%s,设备已连接，连接操作被取消", toString()), TAG, BleSocket.this.hashCode() + "");
            return;
        }
        synchronized (mConnectionStateLock) {
            mConnectionState = STATE_CONNECTING;
        }
        log.w(String.format("%s,设备开始连接", toString()), TAG, BleSocket.this.hashCode() + "");
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mMyBluetoothGattCallback);
    }

    /**
     * 关闭socket,关闭后该socket不可用,必须使用{@link #reset()}函数来恢复到初始状态
     */
    public void close() {
        log.e(String.format("%s,close", BleSocket.this.toString()), TAG, BleSocket.this.hashCode() + "");
        closed = true;
        //移除数据缓存区的数据
        mDataBuffer.clear();
        //关闭搜索
        if (mBleHelper != null) {
            mBleHelper.stopScan();
        }
        //关闭连接
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * 不使用该类时请调用该函数销毁
     */
    public void onDestroy() {
        if (!isClosed()) {
            close();
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        if (mBleHelper != null) {
            mBleHelper.onDestroy();
        }
    }

    /**
     * 获取当前socket的关闭状态
     *
     * @return boolean
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
        mCurrentBleUUID = null;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        closed = false;
    }

    /**
     * 获取当前socket的连接状态
     *
     * @return int {@link #STATE_UNKNOWN}
     * {@link #STATE_CONNECTING}
     * {@link #STATE_CONNECTED}
     * {@link #STATE_CONNECTED_DISCOVER}
     * {@link #STATE_DISCONNECTED}
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
     * @param _data 发送的数据
     * @return boolean
     */
    public boolean send(byte[] _data) {
        mDataBuffer.add(_data);
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
     * @return boolean
     */
    public synchronized boolean push() {
        if (mDataBuffer.size() == 0) {
            return true;
        }
        /*有无蓝牙设备对*/
        if (mBluetoothDevice == null) {
            if (mBleHelper != null) {
                mBleHelper.startScan();
            } else {
                log.e("无设备对象，或请设置BleHelper来帮助自动化", TAG, BleSocket.this.hashCode() + "");
            }
        } else if (getConnectionState() == STATE_UNKNOWN) {
            mBluetoothDevice = BleHelper.getBleAdapter(mContext).getRemoteDevice(mBluetoothDevice.getAddress());
            connect();
        } else {
            String uuid = mCurrentBleUUID.getWriteUUID();
            byte[] data = mDataBuffer.get(0);
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
            log.w(String.format("%s,发送数据：%s；UUID:%s", this.toString(), Common.bytes2Int16(" ", data), uuid), TAG, BleSocket.this.hashCode() + "");
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
     * @return boolean
     */
    public boolean canRead(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & characteristic.PROPERTY_READ) != 0;
    }

    /**
     * 判断传入的通道是否具有写入特性
     *
     * @param characteristic 服务通道对象
     * @return boolean
     */
    public boolean canWrite(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * 判断传入的通道是否具有通知的特性
     *
     * @param characteristic 服务通道对象
     * @return boolean
     */
    public boolean canNotify(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & characteristic.PROPERTY_NOTIFY) != 0;
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
                switch (mType) {
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
     * 设置蓝牙搜索过滤器
     *
     * @param bleFilter 蓝牙搜索过滤器
     */
    public void setBleFilterCondition(BleHelper.BleFilter bleFilter) {
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
        mBleHelper.setBleFilter(bleFilter);
    }

    /**
     * 设置通道通知
     * <p>
     * 连接成功后,确定设备种类后,设置设备的通知通道
     */
    private void setReadCharacteristicNotification() {
        BluetoothGattCharacteristic characteristic = find(mCurrentBleUUID.getReadUUID());
        if (!canNotify(characteristic)) {
            log.e("通道没有通知的特性，UUID：" + mCurrentBleUUID.getReadUUID(), TAG, BleSocket.this.hashCode() + "");
            return;
        }
        if (mBluetoothGatt == null) {
            return;
        }
        synchronized (mBluetoothGatt) {
            boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            log.w(String.format("设置通道通知%s，UUID：" + characteristic.getUuid(), isEnableNotification ? "成功" : "失败"), TAG, BleSocket.this.hashCode() + "");
            //TODO ble和dm的uuid相同,write后需要延迟
            if (isEnableNotification) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(mCurrentBleUUID.getEnableNotificationUUID()));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                log.w("启用通道通知成功，UUID：" + descriptor.getUuid(), TAG, BleSocket.this.hashCode() + "");
            }
        }
    }

    /**
     * 根据UUID查找通道
     *
     * @param uuid 服务通过的ID
     * @return BluetoothGattCharacteristic
     */
    public BluetoothGattCharacteristic find(String uuid) {
        if (mBluetoothGatt != null) {
            List<BluetoothGattService> mBluetoothLeServices = mBluetoothGatt.getServices();
            if (mBluetoothLeServices == null || mBluetoothLeServices.size() == 0) {
                return null;
            }
            for (BluetoothGattService service : mBluetoothLeServices) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (int i = 0; i < characteristics.size(); i++) {
                    BluetoothGattCharacteristic characteristic = characteristics.get(i);
                    log.d("BleSocket find characteristic：" + characteristic.getUuid().toString());
                    if (characteristic.getUuid().toString().equalsIgnoreCase(uuid)) {
                        return characteristic;
                    }
                }
            }
        }
        return null;
    }

    //region GET/SET/toString

    public BleHelper getBleHelper() {
        return mBleHelper;
    }

    public void setBleHelper(BleHelper bleHelper) {
        this.mBleHelper = bleHelper;
    }

    public void setBleCallback(BleCallback bleCallback) {
        this.mBleCallback = bleCallback;
    }

    public ArrayList<BLeUUIDI> getSupportBle() {
        if (mSupportBle == null) return new ArrayList<>();
        return new ArrayList<>(mSupportBle);
    }

    public void setSupportBle(BLeUUIDI... supportBle) {
        if (supportBle == null || supportBle.length == 0) return;
        if (mSupportBle == null) mSupportBle = new HashSet<>();
        mSupportBle.clear();
        Collections.addAll(mSupportBle, supportBle);
    }

    public void addSupportBle(BLeUUIDI... supportBle) {
        if (supportBle == null || supportBle.length == 0) return;
        if (mSupportBle == null) mSupportBle = new HashSet<>();
        Collections.addAll(mSupportBle, supportBle);
    }

    public void removeSupportBle(BLeUUIDI... supportBle) {
        if (supportBle == null || supportBle.length == 0) return;
        if (mSupportBle == null || mSupportBle.size() == 0) return;
        for (int i = 0; i < supportBle.length; i++) {
            this.mSupportBle.remove(supportBle[i]);
        }
    }

    public BLeUUIDI getCurrentBleUUID() {
        return mCurrentBleUUID;
    }

    public void setCurrentBleUUID(BLeUUIDI currentBleUUID) {
        this.mCurrentBleUUID = currentBleUUID;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.mBluetoothDevice = bluetoothDevice;
    }

    /**
     * 将该socket的信息拼接成字符串返回,设备名+设备mac地址
     *
     * @return String
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
    //endregion

    /**
     * 停止蓝牙搜索
     */
    public void stopScan() {
        if (mBleHelper != null) {
            mBleHelper.stopScan();
            mBleHelper = null;
        }
    }

    /**
     * 不同的蓝牙设备,有不同的uuid,根据UUID来确定是哪种设备,然后设置收发通道
     */
    public interface BLeUUIDI {
        public String getBleName();

        public String getEnableNotificationUUID();

        public String getWriteUUID();

        public String getReadUUID();
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
                synchronized (mConnectionStateLock) {
                    mConnectionState = STATE_CONNECTED_DISCOVER;
                }
                //确定当前连接的蓝牙设备属于给定的设备类别表中的哪一种
                if (mSupportBle == null || mSupportBle.size() == 0) {
                    mBleCallback.connectFail(BleSocket.this, FAIL_STATUS_KNOWN_DEVICE);
                    return;
                }
                Iterator<BLeUUIDI> iter = mSupportBle.iterator();
                while (iter.hasNext()) {
                    BLeUUIDI bLeUUIDI = iter.next();
                    BluetoothGattCharacteristic chars = find(bLeUUIDI.getReadUUID());
                    if (chars != null) {
                        mCurrentBleUUID = bLeUUIDI;
                        break;
                    }
                }
                if (mCurrentBleUUID == null) {
                    mBleCallback.connectFail(BleSocket.this, FAIL_STATUS_KNOWN_DEVICE);
                    return;
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
            if (getConnectionState() == BluetoothAdapter.STATE_CONNECTING
                    && newState == BluetoothAdapter.STATE_CONNECTED) {
                //更改连接状态
                synchronized (mConnectionStateLock) {
                    mConnectionState = STATE_CONNECTED;
                }
                mBluetoothGatt.discoverServices();
            }
            if (newState == BluetoothAdapter.STATE_DISCONNECTED) {//连接断开
                switch (getConnectionState()) {
                    case STATE_CONNECTED://设备已连接上
                    case STATE_CONNECTED_DISCOVER:
                        if (!isClosed()) close();
                        mBleCallback.disconnect(BleSocket.this);
                        break;
                    case STATE_CONNECTING://设备没有连接上，直接断开
                        if (!isClosed()) close();
                        mBleCallback.connectFail(BleSocket.this, FAIL_STATUS_UNCONNECT_DISCONNECT);
                        break;
                }
                //更改连接状态
                synchronized (mConnectionStateLock) {
                    mConnectionState = STATE_DISCONNECTED;
                }
            }
        }
    }
}
