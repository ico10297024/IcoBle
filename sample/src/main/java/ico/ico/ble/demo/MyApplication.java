package ico.ico.ble.demo;

import android.content.Context;
import android.support.multidex.MultiDex;

import ico.ico.ble.demo.base.BaseApplication;
import ico.ico.ble.demo.db.DaoMaster;
import ico.ico.ble.demo.db.DaoSession;
import ico.ico.ble.demo.db.DeviceDao;

/**
 * Created by root on 18-2-1.
 */

public class MyApplication extends BaseApplication {

    public static MyApplication that;
    DaoMaster daoMaster;
    DaoSession daoSession;
    DeviceDao deviceDao;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        that = this;
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(this, "db");
        daoMaster = new DaoMaster(openHelper.getWritableDatabase());
        daoSession = daoMaster.newSession();
        deviceDao = daoSession.getDeviceDao();
    }
}
