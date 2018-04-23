package ico.ico.ble.demo.base;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ico.ico.ble.demo.constant.ImageLoaderPrefixConstant;

/**
 * Created by Administrator on 2017/8/9.
 */

public abstract class BaseAdapter<DATA, HOLDER extends BaseAdapter.BaseViewHolder> extends RecyclerView.Adapter<HOLDER> {

    public final static DisplayImageOptions.Builder build = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true);

    protected int selectPosition = -1;
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected int count = 0;
    protected int mLayoutId;  //布局id
    protected List<DATA> data;

    public BaseAdapter(Context context, int layoutId) {
        mContext = context;
        mLayoutId = layoutId;
        mInflater = LayoutInflater.from(context);
    }

    public BaseAdapter(Context context, int layoutId, int count) {
        this.mContext = context;
        this.mLayoutId = layoutId;
        this.mInflater = LayoutInflater.from(context);
        this.count = count;
    }

    public BaseAdapter(Context context, int layoutId, List<DATA> data) {
        this.mContext = context;
        this.mLayoutId = layoutId;
        this.mInflater = LayoutInflater.from(context);
        setData(data);
    }


    @Override
    public HOLDER onCreateViewHolder(ViewGroup parent, int viewType) {
        return (HOLDER) new BaseViewHolder(mInflater.inflate(mLayoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(HOLDER holder, int position) {
        if (getData() != null) {
            holder.itemData = getData().get(position);
        }
        onWidgetInit(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);

    }

    @Override
    public int getItemCount() {
        if (data != null) {
            return data.size();
        }
        return count;
    }


    public BaseAdapter setCount(int count) {
        this.count = count;
        return this;
    }

    protected void onWidgetInit(BaseViewHolder holder, int position) {

    }

    public List<DATA> getData() {
        return data;
    }

    public BaseAdapter setData(List<DATA> data) {
        this.data = data;
        return this;
    }

    public BaseAdapter addData(DATA... _data) {
        if (this.data == null) {
            this.data = new ArrayList<>();
        }
        Collections.addAll(this.data, _data);
        return this;
    }

    public boolean contains(DATA _data) {
        if (this.data == null) return false;
        return this.data.contains(_data);
    }

    public boolean remove(DATA _data) {
        if (this.data == null) return true;
        return this.data.remove(_data);
    }

    public BaseAdapter addData(List<DATA> data) {
        if (this.data == null) this.data = new ArrayList<>();
        this.data.addAll(data);
        return this;
    }

    public int getSelectPosition() {
        return selectPosition;
    }

    public void setSelectPosition(int _selectPosition) {
        int __selectPosition = selectPosition;
        selectPosition = _selectPosition;
        if (__selectPosition != -1) notifyItemChanged(__selectPosition);
        if (selectPosition != -1) notifyItemChanged(selectPosition);
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        public HashMap<String, Object> extra = new HashMap<>();
        public DATA itemData;
        private View mView;
        private SparseArray<View> mViews;   //视图集合

        public BaseViewHolder(View itemView) {
            super(itemView);
            mViews = new SparseArray<>();
            mView = itemView;
        }

        /*根据控件id获取视图*/
        public View getView(int widgetId) {
            View view = mViews.get(widgetId);
            if (view == null) {
                view = mView.findViewById(widgetId);
                mViews.put(widgetId, view);
            }
            return view;
        }

        /*设置文字控件*/
        public BaseViewHolder setText(int widgetId, CharSequence str) {
            ((TextView) getView(widgetId)).setText(str);
            return this;
        }

        /*设置控件的background属性*/
        public BaseViewHolder setBackground(int widgetId, @DrawableRes int resId) {
            getView(widgetId).setBackgroundResource(resId);
            return this;
        }

        /*设置控件的TextColor属性*/
        public BaseViewHolder setTextColor(int widgetId, @ColorInt int color) {
            ((TextView) getView(widgetId)).setTextColor(color);
            return this;
        }

        /*设置CompoundButton控件的checked属性*/
        public BaseViewHolder setChecked(int widgetId, boolean isChecked) {
            ((CompoundButton) getView(widgetId)).setChecked(isChecked);
            return this;
        }


        /*设置的background属性*/
        public BaseViewHolder toogleChecked(int widgetId) {
            CompoundButton button = ((CompoundButton) getView(widgetId));
            button.setChecked(!button.isChecked());
            return this;
        }

        /*设置文字控件,从tag中取字符串,通过格式化占位*/
        public BaseViewHolder setTagText(int widgetId, Object... obj) {
            TextView textView = (TextView) getView(widgetId);
            textView.setText(String.format(textView.getTag().toString(), obj));
            return this;
        }

        /*设置本地图形控件*/
        public BaseViewHolder loadImage(int widgetId, int resorceId) {
            ImageLoader.getInstance().displayImage(ImageLoaderPrefixConstant.DRAWABLE + resorceId, ((ImageView) getView(widgetId)), build.build());
            return this;
        }

        /*设置本地图形控件*/
        public BaseViewHolder loadImage(int widgetId, String url) {
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url不能为空");
            }
            ImageLoader.getInstance().displayImage(url, ((ImageView) getView(widgetId)), build.build());
            return this;
        }

        /*设置本地图形控件*/
        public BaseViewHolder loadImage(int widgetId, int resorceId, int holderResId, int errorResId) {
            ImageLoader.getInstance().displayImage(ImageLoaderPrefixConstant.DRAWABLE + resorceId, ((ImageView) getView(widgetId)), build.showImageOnLoading(holderResId).showImageOnFail(errorResId).showImageForEmptyUri(errorResId).build());
            return this;
        }

        /*设置本地图形控件*/
        public BaseViewHolder loadImage(int widgetId, String url, int holderResId, int errorResId) {
            if (TextUtils.isEmpty(url)) {
                ImageLoader.getInstance().displayImage(ImageLoaderPrefixConstant.DRAWABLE + errorResId, ((ImageView) getView(widgetId)), build.showImageOnLoading(holderResId).showImageOnFail(errorResId).showImageForEmptyUri(errorResId).build());
            } else {
                ImageLoader.getInstance().displayImage(url, ((ImageView) getView(widgetId)), build.showImageOnLoading(holderResId).showImageOnFail(errorResId).showImageForEmptyUri(errorResId).build());
            }
            return this;
        }

        /*设置控件点击监听*/
        public BaseViewHolder setOnClickListner(View.OnClickListener listner, int... widgetId) {
            for (int i = 0; i < widgetId.length; i++) {
                getView(widgetId[i]).setTag(this);
                getView(widgetId[i]).setOnClickListener(listner);
            }
            return this;
        }

        /*设置控件选中监听*/
        public BaseViewHolder setOnCheckChangedListner(CompoundButton.OnCheckedChangeListener listner, int... widgetId) {
            for (int i = 0; i < widgetId.length; i++) {
                getView(widgetId[i]).setTag(this);
                ((CheckBox) getView(widgetId[i])).setOnCheckedChangeListener(listner);
            }
            return this;
        }

        /*设置控件长按监听*/
        public BaseViewHolder setOnLongClickListner(View.OnLongClickListener listener, int... widgetId) {
            for (int i = 0; i < widgetId.length; i++) {
                getView(widgetId[i]).setTag(this);
                getView(widgetId[i]).setOnLongClickListener(listener);
            }
            return this;
        }

        /*设置Enable属性*/
        public BaseViewHolder setEnable(boolean enable, int... widgetId) {
            for (int i = 0; i < widgetId.length; i++) {
                getView(widgetId[i]).setEnabled(enable);
            }
            return this;
        }

        /*设置visibility属性*/
        public BaseViewHolder setVisibility(int visibility, int... widgetId) {
            for (int i = 0; i < widgetId.length; i++) {
                getView(widgetId[i]).setVisibility(visibility);
            }
            return this;
        }

        /*获取Checked属性*/
        public boolean isChecked(int widgetId) {
            return ((CompoundButton) getView(widgetId)).isChecked();
        }

        public void putExtra(String key, Object value) {
            extra.put(key, value);
        }

        public Object getExtra(String key) {
            return extra.get(key);
        }

    }
}
