package ico.ico.ble.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import ico.ico.ble.BleCallback;
import ico.ico.ble.BleHelper;
import ico.ico.ble.BleSocket;
import ico.ico.ble.demo.base.BaseAdapter;
import ico.ico.ble.demo.base.BaseDialogFrag;
import ico.ico.ble.demo.base.BaseFragActivity;
import ico.ico.ble.demo.blemgr.NbeeMgr;
import ico.ico.ble.demo.constant.RegularConstant;
import ico.ico.ble.demo.db.Device;
import ico.ico.ble.demo.db.DeviceDao;
import pub.devrel.easypermissions.EasyPermissions;
import rx.functions.Action1;

/**
 * 协议版本:芝麻云
 * 针对设备:Ble设备
 */
public class BleNbeeListActivity extends BaseFragActivity implements EasyPermissions.PermissionCallbacks, android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener {
    static int DEVICE_TYPE = DeviceType.BLE_NBEE;
    static String keyword = "NBee";
    static String format = RegularConstant.MAC_S;
    //region ButterKnife
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.btn_all)
    Button btnAll;
    //endregion

    int RE_BLUETOOTH = 0x001;
    MyAdapter myAdapter;
    AboutControl aboutControl;
    AboutDialog aboutDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ble);
        ButterKnife.bind(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(myAdapter = new MyAdapter(mActivity));
        swipeRefreshLayout.setOnRefreshListener(this);
        //设置UI关键字
        btnAll.setText(keyword);
        aboutControl = new AboutControl();
        aboutDialog = new AboutDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onRefresh();
    }

    @Override
    public void onRefresh() {
        //从数据库获取数据
        List<Device> list = ((MyApplication) getApplication()).deviceDao.queryBuilder().where(DeviceDao.Properties.Type.eq(DEVICE_TYPE)).list();
        myAdapter.setData(list);
        myAdapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }

    @OnClick(R.id.btn_all)
    public void onClick() {
        aboutControl.open();
    }

    @OnLongClick(R.id.btn_all)
    public boolean onLongClick() {
        aboutDialog.showDeviceDialog();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aboutControl.onDestroy();
    }

    //region Permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        log.w("=========onPermissionsGranted");
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        log.w("=========onPermissionsDenied");
    }

    //endregion

    class MyAdapter extends BaseAdapter<Device, BaseAdapter.BaseViewHolder> implements View.OnClickListener, View.OnLongClickListener {

        public MyAdapter(Context context) {
            super(context, R.layout.item_device);
        }

        public MyAdapter(Context context, int count) {
            super(context, R.layout.item_device, count);
        }

        public MyAdapter(Context context, List<Device> data) {
            super(context, R.layout.item_device, data);
        }

        @Override
        protected void onWidgetInit(BaseViewHolder holder, int position) {
            super.onWidgetInit(holder, position);
            holder.itemData = getData().get(position);
            holder.setText(R.id.tv_name, holder.itemData.getName());
            holder.setText(R.id.tv_mac, holder.itemData.getSerial());

            holder.setOnClickListner(this, R.id.btn_open);
            holder.setOnLongClickListner(this, R.id.item);
        }

        @Override
        public void onClick(View view) {
            BaseViewHolder holder = (BaseViewHolder) view.getTag();
            switch (view.getId()) {
                case R.id.btn_open:
                    aboutControl.open(holder.itemData.getSerial());
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            BaseViewHolder holder = (BaseViewHolder) v.getTag();
            aboutDialog.showDeviceDialog(holder.itemData);
            return true;
        }
    }


    class AboutControl {


        MyBleCallback mBleCallback;
        NbeeMgr mNbeeMgr;
        Action1 timeTask = new Action1() {
            @Override
            public void call(Object o) {
                String msg = "";
                if (mNbeeMgr.getBleSocket().getBluetoothDevice() == null) {
                    msg = "周边未发现该设备！";
                } else if (mNbeeMgr.getBleSocket().getConnectionState() == BleSocket.STATE_CONNECTING) {
                    msg = "连接超时！";
                } else if (mNbeeMgr.getBleSocket().getConnectionState() == BleSocket.STATE_CONNECTED) {
                    msg = "操作超时！";
                } else {
                    msg = "超时！";
                }
                mNbeeMgr.closeSocket();
                dismissProgressDialog();
                mPromptHelper.showToasts(msg);
            }
        };

        public AboutControl() {
            mBleCallback = new MyBleCallback();
            mNbeeMgr = new NbeeMgr(mActivity, mBleCallback);
        }

        ProgressDialog progressDialog;

        void showProgressDialog(final String msg) {
            log.w("===showProgressDialog " + msg);
            if (progressDialog == null) {
                progressDialog = ProgressDialog.show(mActivity, "", msg, false, false);
            } else {
                progressDialog.setMessage(msg);
            }
            if (!progressDialog.isShowing()) {
                synchronized (progressDialog) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.show();
                        }
                    });
                }
            }
        }

        void dismissProgressDialog() {
            if (progressDialog != null && progressDialog.isShowing()) {
                synchronized (progressDialog) {
                    progressDialog.dismiss();
                }
            }
        }

        /** 通过名字随机控制 */
        void open() {
            if (!check()) return;
            showProgressDialog("正在搜索中...");
            mNbeeMgr.open(timeTask, keyword, 1);
        }

        /** 指定序列号控制 */
        void open(String serial) {
            if (!check()) return;
            showProgressDialog("正在搜索中...");
            mNbeeMgr.open(timeTask, serial, 0);
        }

        boolean check() {
            if (!BleHelper.isEnable(mActivity)) {
                BleHelper.enable(mActivity);
                return false;
            }
            if (!EasyPermissions.hasPermissions(mActivity, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})) {
                EasyPermissions.requestPermissions(mActivity, "使用蓝牙门锁需要以下权限", RE_BLUETOOTH, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
                return false;
            }
            if (!BleHelper.isEnable(mActivity)) {
                BleHelper.enable(mActivity);
                return false;
            }
            return true;
        }

        void onDestroy() {
            mNbeeMgr.closeSocket();
        }

        class MyBleCallback extends BleCallback {

            @Override
            public void found(BluetoothDevice device, int rssi) {
                super.found(device, rssi);

                Device device1 = new Device();
                device1.setName(device.getName());
                device1.setSerial(device.getAddress().toUpperCase().replaceAll("[-|:]+", ""));
                device1.setType(DEVICE_TYPE);

                if (MyApplication.that.deviceDao.queryBuilder().where(DeviceDao.Properties.Serial.eq(device1.getSerial())).buildCount().count() == 0) {
                    MyApplication.that.deviceDao.insert(device1);
                }
                onRefresh();
                showProgressDialog("正在连接中...");
            }

            @Override
            public void receive(BleSocket bleSocket, String uuid, byte[] instruct) {
                super.receive(bleSocket, uuid, instruct);
                byte cmd = mNbeeMgr.analyze(instruct);
                if (cmd == -1) {
                    return;
                }
                switch (cmd) {
                    case NbeeMgr.Command.CMD_OPEN://开门指令发送成功
                        //UI处理,蓝牙处理
                        dismissProgressDialog();
                        mPromptHelper.showToasts("开门成功");
                        mNbeeMgr.closeSocket();
                        break;
                }
            }

            @Override
            public void sendFail(BleSocket bleSocket, byte[] instruct, int failStatus) {
                super.sendFail(bleSocket, instruct, failStatus);
                byte cmd = mNbeeMgr.analyze(instruct);
                if (cmd == -1) {
                    return;
                }
                dismissProgressDialog();
                String msg = "";
                switch (failStatus) {
                    case BleSocket.FAIL_STATUS_NONE:
                        msg = "操作失败";
                        break;
                    case BleSocket.FAIL_STATUS_PATH_NOT_FOUND:
                        msg = "数据通道未找到";
                        break;
                    case BleSocket.FAIL_STATUS_PATH_NOT_WRITE:
                        msg = "数据通道没有写入特性";
                        break;
                }
                mNbeeMgr.closeSocket();
                mPromptHelper.showToasts(msg);
            }

            @Override
            public void sendSuccess(BleSocket bleSocket, byte[] instruct) {
                super.sendSuccess(bleSocket, instruct);
                if (!bleSocket.isClosed()) {
                    showProgressDialog("正在等待操作结果...");
                }
            }

            @Override
            public void connectSuccess(BleSocket bleSocket) {
                super.connectSuccess(bleSocket);
                if (!bleSocket.isClosed()) {
                    showProgressDialog("正在发送数据中...");
                }
            }

            @Override
            public void connectFail(BleSocket bleSocket, int failStatus) {
                super.connectFail(bleSocket, failStatus);
                dismissProgressDialog();
                if (mNbeeMgr.getCurrentOperationFlag().isOpering()) {
                    String msg = "";
                    switch (failStatus) {
                        case BleSocket.FAIL_STATUS_SERVICES_UNDISCOVER:
                            msg = "服务通道未发现";
                            break;
                        case BleSocket.FAIL_STATUS_UNCONNECT_DISCONNECT:
                            msg = "连接失败";
                            break;
                        case BleSocket.FAIL_STATUS_KNOWN_DEVICE:
                            msg = "无法识别的设备";
                            break;
                    }
                    mNbeeMgr.closeSocket();
                    mPromptHelper.showToasts(msg);
                }
            }

            @Override
            public void disconnect(BleSocket _BleSocket) {
                super.disconnect(_BleSocket);
                dismissProgressDialog();
                if (mNbeeMgr.getCurrentOperationFlag().isOpering()) {
                    mPromptHelper.showToasts("连接失败");
                    mNbeeMgr.closeSocket();
                }
            }
        }
    }

    class AboutDialog {
        DeviceDialogFragment deviceDialogFragment;

        public void showDeviceDialog() {
            if (deviceDialogFragment == null) {
                deviceDialogFragment = DeviceDialogFragment.newInstance(new Device(DEVICE_TYPE), format);
                deviceDialogFragment.setOnDismissListener(new BaseDialogFrag.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogFragment dialogFragment) {
                        onRefresh();
                    }
                });
            } else {
                deviceDialogFragment.setDevice(new Device(DEVICE_TYPE), format);
            }
            mPromptHelper.showDialogFrag(deviceDialogFragment, DeviceDialogFragment.class.getSimpleName());
        }

        public void showDeviceDialog(Device device) {
            if (deviceDialogFragment == null) {
                deviceDialogFragment = DeviceDialogFragment.newInstance(device, format);
                deviceDialogFragment.setOnDismissListener(new BaseDialogFrag.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogFragment dialogFragment) {
                        onRefresh();
                    }
                });
            } else {
                deviceDialogFragment.setDevice(device, format);
            }
            mPromptHelper.showDialogFrag(deviceDialogFragment, DeviceDialogFragment.class.getSimpleName());
        }

    }
}
