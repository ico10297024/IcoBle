package ico.ico.ble.demo.base;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import ico.ico.ble.demo.helper.PromptHelper;


public abstract class BaseFragActivity extends AppCompatActivity {
    public final static String DEFAULT_DIALOG = "default";
    public BaseFragActivity mActivity;
    public PromptHelper mPromptHelper;
    /** 媒介，用他的post方法来执行线程 */
    public Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivity = this;
        mPromptHelper = PromptHelper.bind(mActivity);
        super.onCreate(savedInstanceState);
    }

    @ColorInt
    public int getColors(@ColorRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(id, getTheme());
        } else {
            return getResources().getColor(id);
        }
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
        mPromptHelper.dismissDialogs();
    }

    /**
     * “返回”按钮
     *
     * @param v
     */
    public void onClickBack(View v) {
        mActivity.finish();
    }
}
