package ico.ico.ble;


import java.net.SocketTimeoutException;

/**
 * 增加了结束标记和超时标记
 * <p>
 * 增加了同步运行的方法，需要设置超时时间
 */
public class IcoThread extends Thread {
    public IcoThread mThread;
    protected boolean exitFlag = false;
    protected boolean timeoutFlag = false;

    public IcoThread() {
        mThread = this;
    }

    /**
     * 设置该线程已运行完毕，并调用{@link #interrupt}
     */
    public void close() {
        this.interrupt();
        this.exitFlag = true;
    }

    /**
     * 标志该线程已超时，并调用{@link #interrupt}
     */
    public void timeout() {
        this.interrupt();
        this.timeoutFlag = true;
    }

    /**
     * 判断当前线程是否已运行结束，关闭
     *
     * @return 标志当前线程是否已运行结束
     */
    public boolean isClosed() {
        return (this.exitFlag || this.timeoutFlag || mThread.getState() == State.TERMINATED ? true : false);
    }

    /**
     * 同步执行线程
     *
     * @param timeout 设置线程运行超时时间
     * @throws SocketTimeoutException 当线程运行超时后将抛出该异常
     */
    public void execute(Long timeout) throws SocketTimeoutException {
        IcoThread.this.start();
        try {
            IcoThread.this.join(timeout);
        } catch (InterruptedException e) {
            log.e("sleep-->" + e.toString(), mThread.getClass().getSimpleName(), "execute");
            // e.printStackTrace();
        }
        if (IcoThread.this.timeoutFlag) {
            throw new SocketTimeoutException();
        }
    }
}
