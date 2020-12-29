package ico.ico.ble.demo.helper;

import android.app.Dialog;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示帮助类
 * 帮助activity来管理Toast，dialog
 */
public class PromptHelper {
    private FragmentActivity mActivity;
    private Handler mHandler;
    public final static String DEFAULT_DIALOG = "default";

    public ConcurrentHashMap<String, Dialog> mDialogs = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, DialogFragment> mDialogFrags = new ConcurrentHashMap<>();
    public Toast mToast;

    private PromptHelper(FragmentActivity activity) {
        this.mActivity = activity;
        mHandler = new Handler(mActivity.getMainLooper());
    }

    /** 与指定页面绑定 */
    public static PromptHelper bind(FragmentActivity activity) {
        PromptHelper promptHelper = new PromptHelper(activity);
        return promptHelper;
    }

    /** 与页面解绑 */
    public void unbind() {
        dismissDialogs();
        dismissDialogFrags();
        this.mActivity = null;
    }

    private String getString(@StringRes final int stringResId) {
        return mActivity.getResources().getString(stringResId);
    }

    /** 弹出土司，循环 */
    public void showToast(@StringRes final int stringResId) {
        showToast(getString(stringResId));
    }


    /** 弹出土司，循环 */
    public void showToast(final CharSequence text) {
        if (mActivity == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mActivity == null) {
                    return;
                }
                CharSequence content = TextUtils.isEmpty(text) ?/* getResources().getString(R.string.ico_application_error)*/"程序出错，请稍候再试!" : text;
                if (mToast != null) {
                    mToast.setText(content);
                    return;
                } else {
                    mToast = Toast.makeText(mActivity, content, Toast.LENGTH_LONG);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mToast = null;
                        }
                    }, 3500);
                }
                mToast.show();
            }
        });
    }

    /** 弹出新土司 */
    public void showToasts(@StringRes final int stringResId) {
        showToasts(getString(stringResId));
    }

    /** 弹出新土司 */
    public void showToasts(final CharSequence text) {
        if (mActivity == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mActivity == null) {
                    return;
                }
                CharSequence content = TextUtils.isEmpty(text) ? /*getResources().getString(R.string.ico_application_error) */"程序出错，请稍候再试!" : text;
                Toast toast = Toast.makeText(mActivity, content, Toast.LENGTH_SHORT);
                if (mToast == null) {
                    mToast = toast;
                }
                toast.show();
            }
        });
    }

    /** 弹出新土司 */
    public void showToastss(final CharSequence text) {
        if (mActivity == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mActivity == null) {
                    return;
                }
                CharSequence content = TextUtils.isEmpty(text) ? /*getResources().getString(R.string.ico_application_error) */"程序出错，请稍候再试!" : text;
                Toast toast = Toast.makeText(mActivity, content, Toast.LENGTH_LONG);
                if (mToast == null) {
                    mToast = toast;
                }
                toast.show();
            }
        });
    }

    /** 显示对话框并以默认key纳入管理 */
    public synchronized void showDialog(Dialog _dialog) {
        showDialog(_dialog, DEFAULT_DIALOG);
    }


    /**
     * 显示对话框并以指定key纳入管理
     * <p>
     * 内部会先获取key对应的dialog，如果与本次不同，则会将原来的dialog移除并关闭，然后保存并显示本次dialog
     *
     * @param _dialog 对话框
     * @param key     对话框对应的key
     */
    public synchronized void showDialog(Dialog _dialog, String key) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        Dialog dialog = mDialogs.get(key);
        //同一个则不管
        if (dialog == _dialog && dialog.isShowing()) {
            return;
        }
        //移除并关闭原来的
        if (dialog != null) {
            mDialogs.remove(key);
            dialog.dismiss();
        }
        //存储并显示
        mDialogs.put(key, _dialog);
        _dialog.show();
    }

    /** 关闭并移除默认key对应对话框 */
    public synchronized void dismissDialog() {
        dismissDialog(DEFAULT_DIALOG);
    }

    /**
     * 关闭并移除指定key对应对话框
     *
     * @param key 对话框对应的key
     */
    public synchronized void dismissDialog(String key) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        Dialog dialog = mDialogs.remove(key);
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /** 关闭全部的对话框 */
    public synchronized void dismissDialogs() {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        Iterator<Map.Entry<String, Dialog>> iter = mDialogs.entrySet().iterator();
        while (iter.hasNext()) {
            dismissDialog(iter.next().getKey());
        }
    }

    /**
     * 显示并以默认key纳入对话框碎片
     * <p>
     * 内部会先获取key对应的dialog，如果与本次不同，则会将原来的dialog移除并关闭，然后保存并显示本次dialog
     *
     * @param dialogFragment 对话框碎片
     */
    public synchronized void showDialogFrag(DialogFragment dialogFragment) {
        showDialogFrag(dialogFragment, DEFAULT_DIALOG);
    }

    /**
     * 显示并以指定key纳入对话框碎片
     * <p>
     * 内部会先获取key对应的dialog，如果与本次不同，则会将原来的dialog移除并关闭，然后保存并显示本次dialog
     *
     * @param dialogFragment 对话框碎片
     * @param key            对话框碎片对应的key
     */
    public synchronized void showDialogFrag(DialogFragment dialogFragment, String key) {
        showDialogFrag(dialogFragment, key, mActivity.getSupportFragmentManager());
    }


    /**
     * 显示并以指定key纳入对话框碎片
     * <p>
     * 内部会先获取key对应的dialog，如果与本次不同，则会将原来的dialog移除并关闭，然后保存并显示本次dialog
     *
     * @param dialogFragment  对话框碎片
     * @param key             对话框碎片对应的key
     * @param fragmentManager 碎片管理器，可能是fragment调用
     */
    public synchronized void showDialogFrag(DialogFragment dialogFragment, String key, FragmentManager fragmentManager) {
        if (TextUtils.isEmpty(key) || mActivity.isFinishing()) {
            return;
        }
        DialogFragment tmpDialogFrag = mDialogFrags.get(key);
        if (tmpDialogFrag == dialogFragment && tmpDialogFrag.isVisible()) {
            return;
        }
        //移除并关闭
        if (tmpDialogFrag != null) {
            mDialogFrags.remove(tmpDialogFrag);
            tmpDialogFrag.dismiss();
        }
        //保存并显示
        if (!dialogFragment.isAdded() || dialogFragment.isHidden()) {
            mDialogFrags.put(key, dialogFragment);
            dialogFragment.show(fragmentManager, key);
        }
    }

    /** 关闭默认key对应的对话框碎片 */
    public synchronized void dismissDialogFrag() {
        dismissDialogFrag(DEFAULT_DIALOG);
    }

    /**
     * 关闭指定key对应的对话框碎片
     *
     * @param key 指定的key
     */
    public synchronized void dismissDialogFrag(String key) {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        DialogFragment dialogFrag = mDialogFrags.remove(key);
        if (dialogFrag != null && dialogFrag.isVisible()) {
            dialogFrag.dismiss();
        }
    }

    /** 关闭全部的对话框碎片 */
    public synchronized void dismissDialogFrags() {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        Iterator<Map.Entry<String, DialogFragment>> iter = mDialogFrags.entrySet().iterator();
        while (iter.hasNext()) {
            dismissDialogFrag(iter.next().getKey());
        }
    }
}
