package org.example.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

/**
 * 池化的线程
 *
 * @author aldebran
 * @since 2021-08-25
 */
public class PoolThread<T>  extends Thread {

    public CustomThreadPool threadPool;

    public volatile Callable<T> task;

    final Object lock = new Object();

    volatile boolean giveBack = false;

    Semaphore semaphore = new Semaphore(1);

    FutureTask<T> future;

    /**
     * 初始化线程，需要指定父线程
     *
     * @param threadPool
     */
    public PoolThread(CustomThreadPool threadPool) {
        this.threadPool = threadPool;
    }


    @Override
    public void run() {
        while (!threadPool.stop) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException("pool thread fail to acquire semaphore!", e);
            }
            try {
                future.run();
            } catch (Exception e) {
                // 处理任务执行过程中的异常
                e.printStackTrace();
            }
            giveBack();
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException("fail to wait!", e);
                }
            }
            giveBack = false;
        }
    }

    /**
     * 线程归还池中
     */
    public void giveBack() {
        if (!giveBack) {
            synchronized (threadPool) {
                giveBack = true;
                task = null;
                threadPool.idleThreads.add(this);
                threadPool.semaphore.release();
            }
        }
    }
}