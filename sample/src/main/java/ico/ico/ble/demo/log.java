package ico.ico.ble.demo;

import android.util.Log;

import ico.ico.ble.Common;

/**
 * Created by admin on 2015/4/21.
 */
public class log {
    final static String COMMON_TAG = "ico_";
    /**
     * 日志等级,从e到v依次为1到5，若输出全关则设置0
     * out等同i，err等同e
     */
    public static int LEVEL = 5;

    public static void v(String msg, String... tags) {
        if (LEVEL < 5) {
            return;
        }
        String tag = COMMON_TAG + "v_" + ico.ico.ble.Common.concat(tags, "_");
        Log.v(tag, msg);
    }

    public static void d(String msg, String... tags) {
        if (LEVEL < 4) {
            return;
        }
        String tag = COMMON_TAG + "d_" + ico.ico.ble.Common.concat(tags, "_");
        Log.d(tag, msg);
    }

    public static void i(String msg, String... tags) {
        if (LEVEL < 3) {
            return;
        }
        String tag = COMMON_TAG + "i_" + ico.ico.ble.Common.concat(tags, "_");
        Log.i(tag, msg);
    }

    public static void w(String msg, String... tags) {
        if (LEVEL < 2) {
            return;
        }
        String tag = COMMON_TAG + "w_" + ico.ico.ble.Common.concat(tags, "_");
        Log.w(tag, msg);
    }

    public static void e(String msg, String... tags) {
        if (LEVEL < 1) {
            return;
        }
        String tag = COMMON_TAG + "e_" + ico.ico.ble.Common.concat(tags, "_");
        Log.e(tag, msg);
    }

    public static void out(String msg, String... tags) {
        if (LEVEL < 3) {
            return;
        }
        String tag = COMMON_TAG + ico.ico.ble.Common.concat(tags, "_");
        System.out.println(tag + "," + msg);
    }

    public static void err(String msg, String... tags) {
        if (LEVEL < 1) {
            return;
        }
        String tag = COMMON_TAG + Common.concat(tags, "_");
        System.err.println(tag + "," + msg);
    }
}
