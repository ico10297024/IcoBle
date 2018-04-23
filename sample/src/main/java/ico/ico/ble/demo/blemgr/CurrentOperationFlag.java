package ico.ico.ble.demo.blemgr;

/**
 * Created by root on 18-3-30.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import rx.Subscription;

/**
 * 当前操作标识
 */
public class CurrentOperationFlag {
    //保存当前所有操作
    private Set<Byte> currOper = new HashSet<>();
    //保存当前所有操作的超时任务
    private Map<Byte, Subscription> currOperTask = new HashMap();

    /**
     * 检查当前有操作在进行
     *
     * @return
     */
    public boolean isOpering() {

        return currOper.size() != 0;
    }

    /**
     * 检查指定操作当前是否在执行
     *
     * @return
     */
    public boolean isOpering(byte cmd) {
        return currOper.contains(cmd);
    }

    /**
     * 结束所有操作
     */
    public void finishOper() {
        if (currOper.size() == 0) {
            return;
        }
        Iterator<Byte> iter = currOper.iterator();
        while (iter.hasNext()) {
            byte cmd = iter.next();
            Subscription sub = currOperTask.get(cmd);
            //结束超时任务
            sub.unsubscribe();
            //移除
            currOperTask.remove(sub);
            currOper.remove(cmd);
        }
    }

    /**
     * 结束指定操作
     */
    public void finishOper(byte cmd) {
        currOper.remove(cmd);
        Subscription sub = currOperTask.get(cmd);
        if (sub != null) {
            sub.unsubscribe();
            currOperTask.remove(sub);
        }
    }

    /**
     * 保存一个操作标记和对应的超时任务
     */
    public void saveOpering(byte cmd, Subscription sub) {
        currOper.add(cmd);
        //如果存在先停止再重新插入
        Subscription _sub = currOperTask.get(cmd);
        if (_sub != null) {
            _sub.unsubscribe();
            currOperTask.remove(_sub);
        }
        currOperTask.put(cmd, sub);
    }
}
