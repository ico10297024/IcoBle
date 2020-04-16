package ico.ico.ble.demo;

import android.util.Log;

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
        String tag = COMMON_TAG + "v_" + Common.concat("_", tags);
        Log.v(tag, msg);
    }

    public static void d(String msg, String... tags) {
        if (LEVEL < 4) {
            return;
        }
        String tag = COMMON_TAG + "d_" + Common.concat("_", tags);
        Log.d(tag, msg);
    }

    public static void i(String msg, String... tags) {
        if (LEVEL < 3) {
            return;
        }
        String tag = COMMON_TAG + "i_" + Common.concat("_", tags);
        Log.i(tag, msg);
    }

    public static void w(String msg, String... tags) {
        if (LEVEL < 2) {
            return;
        }
        String tag = COMMON_TAG + "w_" + Common.concat("_", tags);
        Log.w(tag, msg);
    }

    public static void e(String msg, String... tags) {
        if (LEVEL < 1) {
            return;
        }
        String tag = COMMON_TAG + "e_" + Common.concat("_", tags);
        Log.e(tag, msg);
    }

    public static void out(String msg, String... tags) {
        if (LEVEL < 3) {
            return;
        }
        String tag = COMMON_TAG + Common.concat("_", tags);
        System.out.println(tag + "," + msg);
    }

    public static void err(String msg, String... tags) {
        if (LEVEL < 1) {
            return;
        }
        String tag = COMMON_TAG + Common.concat("_", tags);
        System.err.println(tag + "," + msg);
    }
}
