package ico.ico.ble.demo.blemgr;

/**
 * Created by ICO on 2017/3/21 0021.
 */

import android.content.Context;

import java.util.concurrent.TimeUnit;

import ico.ico.ble.BleCallback;
import ico.ico.ble.BleSocket;
import ico.ico.ble.demo.uuid.NbeeUUID;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * 根据协议编写的一个设备管理器
 */
public class NbeeMgr {
    Context mContext;
    BleCallback mBleCallback;
    //当前操作标识
    CurrentOperationFlag currentOperationFlag = new CurrentOperationFlag();
    //超时时间
    private long TIMESOUT = 5 * 1000L;
    //蓝牙连接
    private BleSocket mBleSocket;
    /**
     * TODO 支持的设备列表,通过{@link BleSocket#setSupportBle(BleSocket.BLeUUIDI...)}设置可支持的设备列表,在BleSocket成功连接上蓝牙设备后会自动匹配列表,匹配成功后将使用对应设备的UUID进行收发数据
     */
    private BleSocket.BLeUUIDI[] mSupportBle = new BleSocket.BLeUUIDI[]{NbeeUUID.getInstance()};

    public NbeeMgr(Context context, BleCallback bleCallback) {
        this.mContext = context;
        this.mBleCallback = bleCallback;
    }

    /** TODO 这里对接收到的数据进行处理，根据具体的协议进行修改，返回具体的操作标识 */
    public byte analyze(byte[] buffer) {
        if (buffer.length < 6) {
            return -1;
        }
        if ((buffer[0] & 0xff) == 0xff
                && (buffer[1] & 0xff) == 0xff
                && (buffer[4] & 0xff) == 0x0d
                && (buffer[5] & 0xff) == 0x0a
                ) {
            currentOperationFlag.finishOper(buffer[3]);
            //TODO 在这里根据不同的操作，做成功处理
            switch (buffer[3]) {
                case Command.CMD_OPEN:
                    //开门成功了
                    break;
            }
            return buffer[3];//返回本次接收到的数据代表什么操作
        } else {
            return -1;
        }
    }

    //region Control

    /** 开门控制，filter设置筛选关键字，type设置筛选类型，{@link BleSocket#setBleFilterCondition(String, int)} */
    public synchronized void open(Action1 action1, String filter, int type) {
        //新建蓝牙socket对象
        closeSocket();
        BleSocket bleSocket = new BleSocket(mContext, mBleCallback);
        bleSocket.setBleFilterCondition(filter, type);
        setBleSocket(bleSocket);
        //通过发送来启动流程
        bleSocket.send(Command.getCommon(Command.CMD_OPEN));
        Subscription sub = Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).delay(TIMESOUT, TimeUnit.MILLISECONDS).subscribe(action1);
        currentOperationFlag.saveOpering(Command.CMD_OPEN, sub);
    }
    //endregion


    //region GETSET
    public BleSocket getBleSocket() {
        return mBleSocket;
    }

    public void setBleSocket(BleSocket bleSocket) {
        this.mBleSocket = bleSocket;
        this.mBleSocket.setSupportBle(mSupportBle);
    }

    public void closeSocket() {
        currentOperationFlag.finishOper();
        if (mBleSocket != null) {
            mBleSocket.close();
            mBleSocket = null;
        }
    }

    public CurrentOperationFlag getCurrentOperationFlag() {
        return currentOperationFlag;
    }
    //endregion

    /**
     * 关于指令命令的封装
     * TODO 根据实际的项目协议进行改写
     */
    public static class Command {
        public static final byte CMD_OPEN = 0x11;//开门

        private static byte[] getCommon(byte cmd) {
            byte[] data = new byte[6];
            //起始 唯一码
            data[0] = (byte) 0xFF;
            data[1] = (byte) 0xFF;
            data[2] = 0x55;
            //动作
            data[3] = cmd;
            //结尾 唯一码
            data[4] = 0x0D;
            data[5] = 0x0A;
            return data;
        }
    }
}
