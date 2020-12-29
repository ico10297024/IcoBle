package ico.ico.ble.demo.blemgr;

/**
 * Created by ICO on 2017/3/21 0021.
 */

import android.content.Context;

import java.util.concurrent.TimeUnit;

import ico.ico.ble.BleCallback;
import ico.ico.ble.BleSocket;
import ico.ico.ble.demo.uuid.DmModuleUUID;
import ico.ico.ble.demo.uuid.NbeeUUID;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * 根据协议编写的一个设备管理器
 */
public class BleMgr {
    Context mContext;
    BleCallback mBleCallback;
    //当前操作标识
    CurrentOperationFlag currentOperationFlag = new CurrentOperationFlag();
    //超时时间
    public static final long TIMESOUT = 10 * 1000L;
    //蓝牙连接器
    private BleSocket mBleSocket;
    /**
     * TODO 支持的设备列表,通过{@link BleSocket#setSupportBle(BleSocket.BLeUUIDI...)}设置可支持的设备列表,在BleSocket成功连接上蓝牙设备后会自动匹配列表,匹配成功后将使用对应设备的UUID进行收发数据
     */
    public static final BleSocket.BLeUUIDI[] mSupportBle = new BleSocket.BLeUUIDI[]{DmModuleUUID.getInstance(), NbeeUUID.getInstance()};

    /** 电量 */
    private int power;

    public BleMgr(Context context, BleCallback bleCallback) {
        this.mContext = context;
        this.mBleCallback = bleCallback;
    }

    //region Control

    /** 开门控制，filter设置筛选关键字，type设置筛选类型，{@link BleSocket#setBleFilterCondition(String, int)} */
    public synchronized void open(Action1 action1, String filter, int type) {
        //新建蓝牙socket对象
        if (getBleSocket() == null || getBleSocket().isClosed()) {
            BleSocket bleSocket = new BleSocket(mContext, mBleCallback);
            bleSocket.setBleFilterCondition(filter, type);
            setBleSocket(bleSocket);
        }
        //通过发送来启动流程
        mBleSocket.send(Command.getNewCommon(Command.CMD_OPEN));
        Subscription sub = Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).delay(TIMESOUT, TimeUnit.MILLISECONDS).subscribe(action1);
        currentOperationFlag.saveOpering(Command.CMD_OPEN, sub);
    }

    public synchronized void queryPower(Action1 action1, String filter, int type) {
        //新建蓝牙socket对象
        if (getBleSocket() == null || getBleSocket().isClosed()) {
            BleSocket bleSocket = new BleSocket(mContext, mBleCallback);
            bleSocket.setBleFilterCondition(filter, type);
            setBleSocket(bleSocket);
        }
        mBleSocket.send(Command.getCommon(Command.CMD_QUERY_POWER));
        Subscription sub = Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).delay(TIMESOUT, TimeUnit.MILLISECONDS).subscribe(action1);
        currentOperationFlag.saveOpering(Command.CMD_QUERY_POWER, sub);
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
            BleSocket bleSocket = mBleSocket;
            mBleSocket = null;
            bleSocket.close();
        }
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
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
        public static final byte CMD_OPEN = 0x01;//开门
        public static final byte CMD_QUERY_POWER = 0x02;//查询电量

        /**
         * 我测试使用的蓝牙设备通信协议
         * 获取通用协议,适用大部分动作
         *
         * @param cmd 协议中代表动作的字节,{@link #CMD_OPEN}等
         * @return byte[]
         */
        public static byte[] getCommon(byte cmd) {
            byte[] data = new byte[19];
            //校验位
            data[0] = (byte) 0xf5;
            data[1] = (byte) 0xf5;
            //类型
            data[2] = (byte) 1;
            //操作类型 1开门， 2复位， 3开授权， 4关授权， 5查询列表， 6删除指定权限， 7删除全部权限 8 授权主动返回
            data[3] = cmd;
            //第一张卡
            data[4] = 0x00;
            data[5] = 0x00;
            data[6] = 0x00;
            data[7] = 0x00;
            //第二张卡
            data[8] = 0x00;
            data[9] = 0x00;
            data[10] = 0x00;
            data[11] = 0x00;
            //第三张卡
            data[12] = 0x00;
            data[13] = 0x00;
            data[14] = 0x00;
            data[15] = 0x00;
            //异或结果
            int b = data[0];
            for (int i = 0; i < 15; i++) {
                b = b ^ data[i + 1];
            }
            data[16] = (byte) b;
            //校验位
            data[17] = 0x0D;
            data[18] = 0x0A;

            return data;
        }

        public static byte[] getNewCommon(byte cmd) {
            byte[] data = new byte[6];
            //校验位
            data[0] = (byte) 0xf5;
            data[1] = (byte) 0xf5;
            //类型
            data[2] = (byte) 1;
            //操作类型 1开门， 2复位， 3开授权， 4关授权， 5查询列表， 6删除指定权限， 7删除全部权限 8 授权主动返回 9查询电量
            data[3] = cmd;
            //校验位
            data[4] = 0x0D;
            data[5] = 0x0A;

            return data;
        }
    }

    /** TODO 这里对接收到的数据进行处理，根据具体的协议进行修改，返回具体的操作标识 */
    public byte analyze(byte[] buffer) {
        if (buffer.length < 6) {
            return -1;
        }
        if ((buffer[0] & 0xff) == 0xf5
                && (buffer[1] & 0xff) == 0xf5
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
}
