package ico.ico.ble;

/**
 * Created by root on 18-4-19.
 */

public class Common {
    /**
     * 将一个字符串数组根据某个字符串连接
     *
     * @param texts
     * @param str
     */
    public static String concat(String str, String... texts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texts.length; i++) {
            String tmp = texts[i];
            sb.append(tmp);
            if (i < texts.length - 1) {
                sb.append(str);
            }
        }
        return sb.toString();
    }

    /**
     * 将单字节转化为16进制
     *
     * @param buffer
     * @return
     */
    public static String byte2Int16(byte buffer) {
        String str = Integer.toString(buffer & 0xFF, 16).toUpperCase();
        return str.length() == 1 ? 0 + str : str;
    }

    /**
     * 将一个字节数组转化为16进制然后通过连接符拼接在一起
     *
     * @param bytes   字符数组
     * @param joinStr 连接符
     * @return
     */
    public static String bytes2Int16(String joinStr, byte... bytes) {
        if (joinStr == null) joinStr = "";
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(byte2Int16(bytes[i]));
            if (i != bytes.length - 1) {
                sb.append(joinStr);
            }
        }
        return sb.toString().toUpperCase();
    }

    /**
     * 每隔几个字符插入一个指定字符
     *
     * @param s        原字符串
     * @param iStr     要插入的字符串
     * @param interval 间隔时间
     * @return String
     */
    public static String insert(String s, String iStr, int interval) {
        StringBuffer s1 = new StringBuffer(s);
        int index;
        for (index = interval; index < s1.length(); index += (interval + 1)) {
            s1.insert(index, iStr);
        }
        return s1.toString();
    }
}
