package ico.ico.ble;

import android.bluetooth.BluetoothDevice;

/**
 * 使用于BleHelper和BleSocket的回调函数
 * <p>
 * BleHelper：{@link BleCallback#found(BluetoothDevice, int)}
 * <p>
 * BleSocket：{@link BleCallback#receive(BleSocket, String, byte[]),BleCallback#sendSuccess(BleSocket, byte[]),BleCallback#sendFail(BleSocket, byte[], String),BleCallback#connectSuccess(BleSocket),BleCallback#connectFail(BleSocket),BleCallback#disconnect(BleSocket)}
 */
public class BleCallback {

    /**
     * 当成功接收到数据时进行回调
     *
     * @param bleSocket 蓝牙的连接对象
     * @param uuid      数据对应的通道
     * @param instruct  数据
     */
    public void receive(BleSocket bleSocket, String uuid, byte[] instruct) {
        log.w(String.format("%s,接收数据：%s;UUID:%s;", bleSocket.toString(), Common.bytes2Int16(" ", instruct), uuid), BleSocket.TAG);
//        log.w(String.format("%s,接收数据", bleSocket.toString()), BleSocket.TAG);
    }

    /**
     * 当数据发送成功时进行回调
     *
     * @param bleSocket 蓝牙的连接对象
     * @param instruct  数据
     */
    public void sendSuccess(BleSocket bleSocket, byte[] instruct) {
        log.w(String.format("%s,发送数据成功：%s", bleSocket.toString(), Common.bytes2Int16(" ", instruct)), BleSocket.TAG);
//        log.w(String.format("%s,发送数据成功", bleSocket.toString()), BleSocket.TAG);
    }

    /**
     * 当数据发送失败时进行回调
     *
     * @param bleSocket  蓝牙的连接对象
     * @param instruct   数据
     * @param failStatus 失败状态码{@link BleSocket#FAIL_STATUS_NONE,BleSocket#FAIL_STATUS_PATH_NOT_FOUND,BleSocket#FAIL_STATUS_PATH_NOT_WRITE}
     */
    public void sendFail(BleSocket bleSocket, byte[] instruct, int failStatus) {
        log.w(String.format("%s,发送数据失败,错误状态码：%d,数据：%s", bleSocket.toString(), failStatus, Common.bytes2Int16(" ", instruct)), BleSocket.TAG);
//        log.w(String.format("%s,发送数据失败,错误状态码：%d", bleSocket.toString(), failStatus), BleSocket.TAG);
    }

    /**
     * 当蓝牙连接失败时进行回调
     *
     * @param bleSocket  蓝牙的连接对象
     * @param failStatus 失败状态码{@link BleSocket#FAIL_STATUS_SERVICES_UNDISCOVER,BleSocket#FAIL_STATUS_UNCONNECT_DISCONNECT}
     */
    public void connectFail(BleSocket bleSocket, int failStatus) {
        log.w(String.format("%s,连接失败,failStatus：%d", bleSocket.toString(), failStatus), BleSocket.TAG);
    }

    /**
     * 当蓝牙连接成功时进行回调
     *
     * @param bleSocket 蓝牙的连接对象
     */
    public void connectSuccess(BleSocket bleSocket) {
        log.w(String.format("%s,连接成功", bleSocket.toString()), BleSocket.TAG);
    }

    /**
     * 当蓝牙在连接成功后,连接被断开时,回调
     *
     * @param bleSocket 蓝牙的连接对象
     */
    public void disconnect(BleSocket bleSocket) {
        log.w(String.format("%s,连接断开", bleSocket.toString()), BleSocket.TAG);
    }

    //BleHelper

    /**
     * 当搜索到蓝牙时进行回调
     *
     * @param device 搜索到的蓝牙设备
     * @param rssi   信号强度
     */
    public void found(BluetoothDevice device, int rssi) {
        log.w(String.format("name:%s,mac:%s,rssi:%d,发现设备", device.getName(), device.getAddress(), rssi), BleSocket.TAG);
    }
}
