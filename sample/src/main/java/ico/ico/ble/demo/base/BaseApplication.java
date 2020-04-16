package ico.ico.ble.demo.base;

import android.app.Application;

import ico.ico.ble.demo.Common;

//
//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                            O\ = /O
//                        ____/`---'\____
//                      .   ' \\| |// `.
//                       / \\||| : |||// \
//                     / _||||| -:- |||||- \
//                       | | \\\ - /// | |
//                     | \_| ''\---/'' | |
//                      \ .-\__ `-` ___/-. /
//                   ___`. .' /--.--\ `. . __
//                ."" '< `.___\_<|>_/___.' >'"".
//               | | : `- \`.;`\ _ /`;.`/ - ` : | |
//                 \ \ `-. \_ __\ /__ _/ .-` / /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='
//
//         .............................................
//                  佛祖镇楼                  BUG辟易
//          佛曰:
//                  写字楼里写字间，写字间里程序员；
//                  程序人员写程序，又拿程序换酒钱。
//                  酒醒只在网上坐，酒醉还来网下眠；
//                  酒醉酒醒日复日，网上网下年复年。
//                  但愿老死电脑间，不愿鞠躬老板前；
//                  奔驰宝马贵者趣，公交自行程序员。
//                  别人笑我忒疯癫，我笑自己命太贱；
//                  不见满街漂亮妹，哪个归得程序员？
public class BaseApplication extends Application {
    /**
     * 自身
     */
    private static BaseApplication APPLICATION;
    private String localMac = "00:00:00:00:00:02";

    public static BaseApplication getInstance() {
        return APPLICATION;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BaseApplication.APPLICATION = this;
        //获取mac地址保存
        initLocalMac();
    }

    /**
     * 由于安卓的碎片化,再加上不打开wifi难以获取到设备的mac地址
     * 所以在程序启动时获取mac地址进行保存
     */
    public void initLocalMac() {
        Common.getLocalMac(this, new Common.LocalMacCallback() {
            @Override
            public void onLocalMac(String result) {
                setLocalMac(result);
            }
        });
    }

    public String getLocalMac() {
        return localMac;
    }

    public void setLocalMac(String localMac) {
        this.localMac = localMac;
    }
}
