package ico.ico.ble.demo.base;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public abstract class BaseFragActivity extends AppCompatActivity {
    public final static String DEFAULT_DIALOG = "default";
    public BaseFragActivity mActivity;
    public BaseApplication mApp;
    public LinkedHashMap<String, Dialog> mDialogs = new LinkedHashMap<>();
    public DialogFragment mDialogFrag;
    public Toast mToast;

    /**
     * 媒介，用他的post方法来执行线程
     */
    public Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        mApp = (BaseApplication) mActivity.getApplication();
        //设置通知栏透明
//        Window window = getWindow();
//        window.requestFeature(Window.FEATURE_NO_TITLE);
//      window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//      windo.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    /*点击空白处隐藏软键盘*/
    //region 点击空白处缩回软键盘
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
//            View v = getCurrentFocus();
//            if (isShouldHideKeyboard(v, ev)) {
//                hideKeyboard(v.getWindowToken());
//            }
//        }
//        return super.dispatchTouchEvent(ev);
//    }
//
//    /**
//     * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘，因为当用户点击EditText时则不能隐藏
//     *
//     * @param v
//     * @param event
//     * @return
//     */
//    protected boolean isShouldHideKeyboard(View v, MotionEvent event) {
//        if (v != null && (v instanceof EditText)) {
//            int[] log = {0, 0};
//            v.getLocationInWindow(log);
//            int left = log[0],
//                    top = log[1],
//                    bottom = top + v.getHeight(),
//                    right = left + v.getWidth();
//            if (event.getX() > left && event.getX() < right
//                    && event.getY() > top && event.getY() < bottom) {
//                // 点击EditText的事件，忽略它。
//                return false;
//            } else {
//                return true;
//            }
//        }
//        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditText上，和用户用轨迹球选择其它的焦点
//        return false;
//    }
//
//    /**
//     * 获取InputMethodManager，隐藏软键盘
//     *
//     * @param token
//     */
//    private void hideKeyboard(IBinder token) {
//        if (token != null) {
//            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
//        }
//    }
    //endregion

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissDialogs();
    }

    /**
     * “返回”按钮
     *
     * @param v
     */
    public void onClickBack(View v) {
        mActivity.finish();
    }

    /**
     * 弹出土司
     *
     * @param stringResId
     */
    public void showToast(@StringRes final int stringResId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence content = getResources().getString(stringResId);
                if (mToast != null) {
                    mToast.setText(content);
                    mToast.show();
                    return;
                }
                mToast = Toast.makeText(mActivity, content, Toast.LENGTH_LONG);
                mToast.show();
            }
        });
    }


    /**
     * 弹出土司
     *
     * @param text
     */
    public void showToast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence content = TextUtils.isEmpty(text) ?/* getResources().getString(R.string.ico_application_error)*/"程序出错，请稍候再试!" : text;
                if (mToast != null) {
                    mToast.setText(content);
                    mToast.show();
                    return;
                }
                mToast = Toast.makeText(mActivity, content, Toast.LENGTH_LONG);
                mToast.show();
            }
        });
    }

    /**
     * 弹出土司
     *
     * @param text
     */
    public void showToasts(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CharSequence content = TextUtils.isEmpty(text) ? /*getResources().getString(R.string.ico_application_error) */"程序出错，请稍候再试!" : text;
                Toast toast = Toast.makeText(mActivity, content, Toast.LENGTH_LONG);
                if (mToast == null) {
                    mToast = toast;
                }
                toast.show();
            }
        });
    }

    /**
     * 关闭当前对话框，显示输入参数所表示的对话框
     *
     * @param _dialog
     */
    public void showDialog(Dialog _dialog) {
        showDialog(_dialog, DEFAULT_DIALOG);
    }

    /**
     * 关闭对话框
     */
    public void dismissDialog() {
        dismissDialog(DEFAULT_DIALOG);
    }

    /**
     * 关闭当前对话框，显示输入参数所表示的对话框
     *
     * @param _dialog
     */
    public void showDialog(Dialog _dialog, String key) {
        dismissDialog(key);
        mDialogs.put(key, _dialog);
        _dialog.show();
    }

    /**
     * 关闭对话框
     */
    public void dismissDialog(String key) {
        Dialog dialog = mDialogs.remove(key);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    /**
     * 关闭全部的对话框
     */
    public void dismissDialogs() {
        Iterator<Map.Entry<String, Dialog>> iter = mDialogs.entrySet().iterator();
        while (iter.hasNext()) {
            dismissDialog(iter.next().getKey());
        }
        //下面的代码通过findbugs插件表示影响性能
//        for (String key : mDialogs.keySet()) {
//            dismissDialog(key);
//        }
    }

    /**
     * 显示对话框碎片
     *
     * @param dialogFragment
     */
    public void showDialogFrag(DialogFragment dialogFragment, String tag) {
        showDialogFrag(dialogFragment, tag, getSupportFragmentManager());
    }


    /**
     * 显示对话框碎片
     *
     * @param dialogFragment
     */
    public void showDialogFrag(DialogFragment dialogFragment, String tag, FragmentManager fragmentManager) {
        dismissDialogFrag();
        if (!TextUtils.isEmpty(tag)) {
            if (!dialogFragment.isAdded() || dialogFragment.isHidden()) {
                dialogFragment.show(fragmentManager, tag);
            }
        }
        mActivity.mDialogFrag = dialogFragment;
    }

    /**
     * 关闭对话框碎片
     */
    public void dismissDialogFrag() {
        if (mDialogFrag != null && mDialogFrag.isVisible()) {
            mDialogFrag.dismiss();
            mDialogFrag = null;
        }
    }
}
