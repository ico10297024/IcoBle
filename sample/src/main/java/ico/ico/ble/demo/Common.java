package ico.ico.ble.demo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.regex.Pattern;

/**
 * Created by root on 18-4-23.
 */

public class Common {
    /**
     * 获取设备的mac地址
     * <p>
     * 由于安卓手机的不确定性,在application初始化时获取了本地的mac地址并进行了保存,后续的使用直接调用{@link ico.ico.ico.BaseApplication#localMac}
     * <p>
     * 推荐使用{@link #getUniqueId(Context)}获取手机唯一标识码
     *
     * @param callback 成功获取到mac地址之后会回调此方法
     */
    public static void getLocalMac(final Context context, final LocalMacCallback callback) {
        final WifiManager wm = (WifiManager) context.getSystemService(Service.WIFI_SERVICE);

        // 尝试打开WIFI，并获取mac地址
        if (wm.isWifiEnabled()) {
            callback.onLocalMac(getLocalMac());
            return;
        }
        wm.setWifiEnabled(true);

        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLING || wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {

                    callback.onLocalMac(getLocalMac());
                    context.unregisterReceiver(this);
                    wm.setWifiEnabled(false);
                }
            }
        }, intentFilter);
    }

    private static String getLocalMac() {
        String macAddress = null;
        StringBuffer buf = new StringBuffer();
        NetworkInterface networkInterface = null;
        try {
            networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0");
            }
            if (networkInterface == null) {
                return "02:00:00:00:00:02";
            }
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            macAddress = buf.toString();
        } catch (SocketException e) {
            e.printStackTrace();
            return "02:00:00:00:00:02";
        }
        return macAddress;
    }

    /**
     * 将16进制字符串转换为byte
     *
     * @param hexStr
     * @return
     */
    public static byte hexstr2Byte(String hexStr) {
        char[] chars = hexStr.toUpperCase().toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) "0123456789ABCDEF".indexOf(chars[i]);
        }
        byte buffer = 0;
        if (bytes.length == 2) {
            buffer = (byte) (((bytes[0] << 4) & 0xf0) | (bytes[1]));
        } else {
            buffer = bytes[0];
        }
        return buffer;
    }

    /**
     * 将一个16进制的字符串转化成一个字节数组
     * 字符串以16进制字节为一个单位以冒号分隔
     * 如 AA:BB:CC:DD...
     * 分隔符可以通过{@link Common#insert(String, String, int)}进行插入
     *
     * @param mac 会对格式进行检查
     *            如果只有一位,左边加0,变两位,如 A->0A
     * @return
     */
    public static byte[] hexstr2Bytes(String mac, String joinStr) throws IllegalArgumentException {
        mac = mac.toUpperCase();
        //检查数据中是否存在16进制以外的数字
        Pattern pattern = Pattern.compile("[GHIJKLMNOPQRSTUVWXYZ]+");
        if (pattern.matcher(mac).find()) {
            throw new IllegalArgumentException("mac中存在16进制以外的英文,mac=" + mac);
        }
        if (mac.length() != 2) {
            if (!mac.matches("([0123456789ABCDEF]{2}[:]{1})+[0123456789ABCDEF]{2}")) {
                throw new IllegalArgumentException("输入参数mac格式为AA:BB:CC...,如果是一个字节的16进制字符串,请使用hexstr2Byte,mac=" + mac);
            }
        }
        String[] _mac = mac.split(joinStr);
        byte[] buffer = new byte[_mac.length];
        for (int i = 0; i < _mac.length; i++) {
            buffer[i] = Common.hexstr2Byte(_mac[i]);
        }
        return buffer;
    }


    /**
     * 每隔几个字符插入一个指定字符
     *
     * @param s        原字符串
     * @param iStr     要插入的字符串
     * @param interval 间隔时间
     * @return
     */
    public static String insert(String s, String iStr, int interval) {
        StringBuffer s1 = new StringBuffer(s);
        int index;
        for (index = interval; index < s1.length(); index += (interval + 1)) {
            s1.insert(index, iStr);
        }
        return s1.toString();
    }

    public interface LocalMacCallback {
        void onLocalMac(String result);
    }
}
