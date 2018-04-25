package ico.ico.ble.demo.blemgr;

/**
 * Created by ICO on 2017/3/21 0021.
 */

import java.util.concurrent.TimeUnit;

import ico.ico.ble.BleSocket;
import ico.ico.ble.demo.uuid.BleUUID;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * 根据协议编写的一个设备管理器
 */
public class BleMgr {
    //当前操作标识
    CurrentOperationFlag currentOperationFlag = new CurrentOperationFlag();
    //超时时间
    private long TIMESOUT = 15 * 1000L;
    //蓝牙连接
    private BleSocket mBleSocket;
    /**
     * TODO 支持的设备列表,通过{@link BleSocket#setSupportBle(BleSocket.BLeUUIDI...)}设置可支持的设备列表,在BleSocket成功连接上蓝牙设备后会自动匹配列表,匹配成功后将使用对应设备的UUID进行收发数据
     */
    private BleUUID[] mSupportBle = new BleUUID[]{BleUUID.getInstance()};

    /**
     * 电量
     */
    private int power;

    public BleMgr() {
    }

    public BleMgr(BleSocket bleSocket) {
        setBleSocket(bleSocket);
    }

    /**
     * view层接收到数据回调,调用这个函数去分析收到的数据,对数据进行校验和处理,然后返回这条指令的动作标识
     * TODO 根据实际的项目协议进行改写
     *
     * @param data 接收到的数据
     * @return
     */
    public byte analyze(byte[] data) {
        if (data.length < 14) {
            return -1;
        }
        if ((data[0] & 0xff) == 0xf5 &&
                (data[1] & 0xff) == 0xf5 &&
                (data[4] & 0xff) == 0x0d &&
                (data[5] & 0xff) == 0x0a
                ) {
            currentOperationFlag.finishOper(data[2]);
            switch (data[2]) {
                case Command.CMD_OPEN:
                    break;
                case Command.CMD_QUERY_POWER:
                    power = data[3];
                    break;
            }
            return data[2];
        } else {
            return -1;
        }
    }

    //region Control
    public synchronized void open(Action1 action1) {
        mBleSocket.send(Command.getCommonI(Command.CMD_OPEN));
        Subscription sub = Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).delay(TIMESOUT, TimeUnit.MILLISECONDS).subscribe(action1);
        currentOperationFlag.saveOpering(Command.CMD_OPEN, sub);
    }

    public synchronized void queryPower(Action1 action1) {
        mBleSocket.send(Command.getCommonI(Command.CMD_QUERY_POWER));
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

        //我随便写的协议,根据实际的情况改写,一般来说手机-设备和设备-手机的协议格式都是相同的
        public static byte[] getCommonI(byte cmd) {
            byte[] data = new byte[14];
            //起始 唯一码
            data[0] = (byte) 0xF5;
            data[1] = (byte) 0xF5;
            //动作
            data[2] = cmd;
            //预留位,在获取电量的情况下使用
            data[3] = 0x00;
            //结尾 唯一码
            data[4] = 0x0D;
            data[5] = 0x0A;
            return data;
        }
    }
}
