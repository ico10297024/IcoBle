package ico.ico.ble.demo.constant;

/**
 * 该类列举了ImageLoader框架在加载除Http图片的uri情况下，需要添加的前缀
 */

public final class ImageLoaderPrefixConstant {
    //用于ImageLoader加载除Http图片以外的图片时，uri的前缀
    public final static String DRAWABLE = "drawable://";
    public final static String FILE = "file://";
    public final static String PROVIDER = "content://";
    public final static String ASSETS = "assets://";
}
