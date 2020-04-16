package ico.ico.ble.demo.constant;

/**
 * 列举常用的一些正则表达式
 */

public class RegularConstant {
    /**
     * 音频文件
     */
    public final static String MUSIC = ".(wma|mmf|mp3|flac|ape)";
    /**
     * IP地址
     */
    public final static String IP = "(\\d{1,3}[.]{1}){3}\\d{1,3}";
    /**
     * Email
     */
    public final static String EMAIL = "([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)";
    /**
     * 国内手机号码
     */
    public final static String DOMESTIC_MOBILE_PHONE = "(0|86|17951)?(1[0-9]{10})";//"13[\\d]{9}$|14^[0-9]\\d{8}|^15[0-9]\\d{8}$|^18[0-9]\\d{8}";
    /**
     * 国内电话号码(0511-4405222、021-87888822)
     */
    public final static String DOMESTIC_FIXED_PHONE = "\\d{3}-\\d{8}|\\d{4}-{7,8}";
    /**
     * 密码，允许大小写英文，数字，长度为6~30
     */
    public final static String PASSWORD = "\\w{6,30}";
    /**
     * 带符号的mac地址
     */
    public final static String MAC = "([0-9a-fA-Z]{2}[:]){5}[0-9a-fA-Z]{2}";
    /**
     * 不带符号的mac地址
     */
    public final static String MAC_S = "[0-9a-fA-F]{12}";
    /**
     * 身份证
     */
    public final static String IDENTITY_CARD = "\\d{15}|\\d{18}";
    /**
     * 军官证
     */
    public final static String OFFICERS_CARD = "南字第(\\d{8})号|北字第(\\d{8})号|沈字第(\\d{8})号|兰字第(\\d{8})号|成字第(\\d{8})号|济字第(\\d{8})号|广字第(\\d{8})号|海字第(\\d{8})号|空字第(\\d{8})号|参字第(\\d{8})号|政字第(\\d{8})号|后字第(\\d{8})号|装字第(\\d{8})号";
    /**
     * 护照
     */
    public final static String PASSPORT = "[a-zA-Z]{5,17}|[a-zA-Z0-9]{5,17}";
    /**
     * 台湾通行证
     */
    public final static String PASSPORT_TAIWAN = "[0-9]{8}|[0-9]{10}";
}
