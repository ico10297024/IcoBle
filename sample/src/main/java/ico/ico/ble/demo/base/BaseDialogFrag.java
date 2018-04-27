package ico.ico.ble.demo.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by admin on 2015/3/11.
 */
public abstract class BaseDialogFrag extends DialogFragment {
    public BaseFragActivity mActivity;
    public BaseDialogFrag mFragment;
    public View mContentView;
    public Dialog mDialog;
    public Handler mHandler = new Handler();
    OnDismissListener onDismissListener;
    private boolean saveStateFlag = true;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFragment = this;
        mActivity = (BaseFragActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContentView == null || !isSaveStateFlag()) {
            onCreateView(inflater, container, savedInstanceState, isSaveStateFlag());
        }
        return mContentView;
    }

    public abstract void onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, boolean isSaveStateFlag);

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog == null || !isSaveStateFlag()) {
            onCreateDialog(savedInstanceState, isSaveStateFlag());
        }
        if (mDialog == null) {
            return super.onCreateDialog(savedInstanceState);
        }
        return mDialog;
    }

    public abstract void onCreateDialog(Bundle savedInstanceState, boolean isSaveStateFlag);

    public View getContentView() {
        return mContentView;
    }

    public void setContentView(int contentRes) {
        mContentView = LayoutInflater.from(getContext()).inflate(contentRes, null);
    }

    public void setDialogView(int contentRes) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(contentRes, null, false);
        mDialog = new Dialog(getActivity());
        mDialog.setContentView(view);
    }

    public Dialog getDialog() {
        if (mDialog == null) {
            return super.getDialog();
        } else {
            return mDialog;
        }
    }

    /**
     * 重写函数
     * 根据隐藏显示状态手动调用来模拟Activity启动、播放，暂停，停止的生命周期
     * 根据测试，dialogFragment的该函数不会被触发
     *
     * @param hidden
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            onStart();
            onResume();
        } else {
            onPause();
            onStop();
        }
    }

    @Override
    public void onDestroyView() {
        if (mContentView != null && mContentView.getParent() != null && isSaveStateFlag()) {
            ((ViewGroup) mContentView.getParent()).removeView(mContentView);
        }
        if (mDialog != null && !isSaveStateFlag()) {
            mDialog.dismiss();
            mDialog = null;
        }
        super.onDestroyView();
    }

    /**
     * 设置状态保存标记
     * 若改标记为true，则该碎片所表示的视图在onDestoryView函数中不会被销毁，在下一次onCreateView中可以复用
     * 默认为true
     *
     * @return boolean boolean
     */
    public boolean isSaveStateFlag() {
        return saveStateFlag;
    }

    /**
     * 设置状态保存标记
     * 若改标记为true，则该碎片所表示的视图在onDestoryView函数中不会被销毁，在下一次onCreateView中可以复用
     * 默认为true
     *
     * @param saveStateFlag
     */
    public void setSaveStateFlag(boolean saveStateFlag) {
        this.saveStateFlag = saveStateFlag;
    }

    /**
     * 在contentView视图中，根据ID获取组件
     *
     * @param id 唯一标识
     * @return View
     */
    public View findViewById(int id) {
        if (mContentView != null) {
            return mContentView.findViewById(id);
        }
        if (mDialog != null) {
            return mDialog.findViewById(id);
        }
        return null;
    }

    /**
     * 弹出土司
     *
     * @param stringResId
     */
    public void showToast(@StringRes int stringResId) {
        mActivity.showToast(stringResId);
    }

    /**
     * 弹出土司
     *
     * @param text
     */
    public void showToast(CharSequence text) {
        mActivity.showToast(text);
    }

    /**
     * 弹出土司
     *
     * @param text
     */
    public void showToasts(CharSequence text) {
        mActivity.showToast(text);
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.onDismiss(this);
        }
    }

    public interface OnDismissListener {
        void onDismiss(DialogFragment dialogFragment);
    }
}