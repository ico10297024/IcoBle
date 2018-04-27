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
import ico.ico.ble.demo.blemgr.BleMgr;
import ico.ico.ble.demo.constant.RegularConstant;
import ico.ico.ble.demo.db.Device;
import ico.ico.ble.demo.db.DeviceDao;
import pub.devrel.easypermissions.EasyPermissions;
import rx.functions.Action1;

/**
 * 协议版本:芝麻云
 * 针对设备:Ble设备
 */
public class BleZmyListActivity extends BaseFragActivity implements EasyPermissions.PermissionCallbacks, android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener {
    static int DEVICE_TYPE = DeviceType.BLE_ZMY;
    static String keyword = "DM";
    static String format = RegularConstant.MAC;
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
        aboutControl.mBleSocket.setBleFilterCondition(keyword, 1);
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

            holder.setOnClickListner(this, R.id.btn_open, R.id.btn_query);
            holder.setOnLongClickListner(this, R.id.item);
        }

        @Override
        public void onClick(View view) {
            BaseViewHolder holder = (BaseViewHolder) view.getTag();
            aboutControl.setBleSerial(holder.itemData.getSerial());
            switch (view.getId()) {
                case R.id.btn_open:
                    aboutControl.open();
                    break;
                case R.id.btn_query:
                    aboutControl.queryPower();
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
        BleMgr mBleMgr;
        BleSocket mBleSocket;
        BluetoothDevice mBluetoothDevice;
        Action1 timeTask = new Action1() {
            @Override
            public void call(Object o) {
                dismissDialog();
                String msg = "";
                if (mBleSocket.getBluetoothDevice() == null) {
                    msg = "周边未发现该设备！";
                } else if (mBleSocket.getConnectionState() == BleSocket.STATE_CONNECTING) {
                    msg = "连接超时！";
                } else if (mBleSocket.getConnectionState() == BleSocket.STATE_CONNECTED) {
                    msg = "操作超时！";
                } else {
                    msg = "超时！";
                }
                if (!mBleSocket.isClosed()) {
                    mBleSocket.close();
                }
                mBleMgr.getCurrentOperationFlag().finishOper();
                showToast(msg);
            }
        };

        public AboutControl() {
            mBleCallback = new MyBleCallback();
            mBleSocket = new BleSocket(mActivity, mBleCallback);
            mBleMgr = new BleMgr(mBleSocket);
        }


        public void setBleSerial(String serial) {
            mBluetoothDevice = null;
            if (!mBleSocket.isClosed()) {
                mBleSocket.close();
            }
            mBleSocket.reset();
            mBleSocket.setBleFilterCondition(serial, 0);
        }

        void showProgressDialog() {
            showDialog(ProgressDialog.show(mActivity, "", "正在操作中", false, false));
        }

        public void open() {
            if (!check()) return;
            mBleSocket.reset();
            showProgressDialog();
            mBleMgr.open(timeTask);
        }

        public void queryPower() {
            if (!check()) return;
            mBleSocket.reset();
            showProgressDialog();
            mBleMgr.open(timeTask);
        }


        public boolean check() {
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

        public void onDestroy() {
            mBleSocket.onDestroy();
        }

        class MyBleCallback extends BleCallback {
            @Override
            public void found(BluetoothDevice device, int rssi) {
                super.found(device, rssi);

                Device device1 = new Device();
                device1.setName(device.getName());
                device1.setSerial(device.getAddress().replaceAll("[-|:]+", ""));
                device1.setType(DEVICE_TYPE);

                if (MyApplication.that.deviceDao.queryBuilder().where(DeviceDao.Properties.Serial.eq(device1.getSerial())).buildCount().count() == 0) {
                    MyApplication.that.deviceDao.insert(device1);
                }
                onRefresh();
            }

            @Override
            public void receive(BleSocket bleSocket, String uuid, byte[] instruct) {
                super.receive(bleSocket, uuid, instruct);
                byte cmd = mBleMgr.analyze(instruct);
                if (cmd == -1) {
                    return;
                }
                switch (cmd) {
                    case BleMgr.Command.CMD_OPEN://开门指令发送成功
                        //UI处理,蓝牙处理
                        dismissDialog();
                        showToast("开门成功");
                        mBleSocket.close();
                        break;
                    case BleMgr.Command.CMD_QUERY_POWER://查询电量成功
                        dismissDialog();
                        showToast("查询电量成功,电量为" + mBleMgr.getBleSocket());
                        mBleSocket.close();
                        break;
                }
            }

            @Override
            public void sendFail(BleSocket bleSocket, byte[] instruct, int failStatus) {
                super.sendFail(bleSocket, instruct, failStatus);
                byte cmd = mBleMgr.analyze(instruct);
                if (cmd == -1) {
                    return;
                }
                dismissDialog();
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
                mBleSocket.close();
                showToast(msg);
            }

            @Override
            public void connectFail(BleSocket bleSocket, int failStatus) {
                super.connectFail(bleSocket, failStatus);
                dismissDialog();
                if (mBleMgr.getCurrentOperationFlag().isOpering()) {
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
                    mBleMgr.getCurrentOperationFlag().finishOper();
                    showToast(msg);
                }
            }

            @Override
            public void disconnect(BleSocket _BleSocket) {
                super.disconnect(_BleSocket);
                dismissDialog();
                if (mBleMgr.getCurrentOperationFlag().isOpering()) {
                    showToast("连接失败");
                    mBleMgr.getCurrentOperationFlag().finishOper();
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
            showDialogFrag(deviceDialogFragment, DeviceDialogFragment.class.getSimpleName());
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
            showDialogFrag(deviceDialogFragment, DeviceDialogFragment.class.getSimpleName());
        }

    }
}
