package ico.ico.ble.demo;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ico.ico.ble.demo.base.BaseDialogFrag;
import ico.ico.ble.demo.db.Device;

/**
 * Created by root on 18-2-2.
 */

public class DeviceDialogFragment extends BaseDialogFrag {

    static final String FORMAT = "format";
    static final String DEVICE = "device";
    //region ButterKnife
    @BindView(R.id.et_name)
    EditText etName;
    @BindView(R.id.et_serial)
    EditText etSerial;
    @BindView(R.id.btn_confirm)
    Button btnConfirm;
    @BindView(R.id.btn_delete)
    Button btnDelete;
    //endregion
    Device mDevice;

    /**
     * @param device 需要修改的设备对象,如果意图设备添加,请想要添加的设备类型Type的设备对象
     * @return {@link DeviceDialogFragment}
     */
    public static DeviceDialogFragment newInstance(Device device, String reg) {
        if (device == null) {
            throw new NullPointerException();
        }
        Bundle args = new Bundle();
        args.putParcelable(DEVICE, device);
        args.putString(FORMAT, reg);
        DeviceDialogFragment fragment = new DeviceDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_Dialog_None);
    }

    @Override
    public void onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, boolean isSaveStateFlag) {
        setContentView(R.layout.dialog_add_device);
        ButterKnife.bind(this, getContentView());
        log.w("===onCreateView");

        if (getArguments() != null && getArguments().getParcelable(DEVICE) != null && getArguments().getString(FORMAT) != null) {
            setDevice(getArguments().getParcelable(DEVICE), getArguments().getString(FORMAT));
        } else {
            throw new NullPointerException();
        }
    }

    public void setDevice(Device device, String reg) {
        if (device == null) {
            throw new NullPointerException();
        }
        if (getArguments() == null) {
            Bundle args = new Bundle();
            this.setArguments(args);
        }
        getArguments().putParcelable(DEVICE, device);
        getArguments().putString(FORMAT, reg);
        this.mDevice = device;
        getContentView().post(new Runnable() {
            @Override
            public void run() {
                if (mDevice.getId() == null) {
                    etName.setText("");
                    etSerial.setText("");
                    btnDelete.setVisibility(View.GONE);
                    btnConfirm.setText("添加");
                } else {
                    etName.setText(mDevice.getName());
                    etSerial.setText(mDevice.getSerial());
                    btnDelete.setVisibility(View.VISIBLE);
                    btnConfirm.setText("更改");
                }
            }
        });
    }


    @Override
    public void onCreateDialog(Bundle savedInstanceState, boolean isSaveStateFlag) {
    }

    @OnClick({R.id.btn_confirm, R.id.btn_delete})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_confirm:
                if (TextUtils.isEmpty(etName.getText().toString())) {
                    mActivity.mPromptHelper.showToast("设备名称不能为空");
                    return;
                }
                if (TextUtils.isEmpty(etSerial.getText().toString())) {
                    mActivity.mPromptHelper.showToast("设备序列号不能为空");
                    return;
                }
                if (!etSerial.getText().toString().matches(getArguments().getString(FORMAT))) {
                    mActivity.mPromptHelper.showToast("设备序列号格式错误");
                    return;
                }
                mDevice.setName(etName.getText().toString());
                mDevice.setSerial(etSerial.getText().toString().toUpperCase());
                MyApplication.that.deviceDao.insertOrReplace(mDevice);
                dismissAllowingStateLoss();
                break;
            case R.id.btn_delete:
                MyApplication.that.deviceDao.delete(mDevice);
                dismissAllowingStateLoss();
                break;
        }
    }
}
