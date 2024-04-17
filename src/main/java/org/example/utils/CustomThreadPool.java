package org.example.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

public class CustomThreadPool {

    public int max_thread_num = 1; // 最大连接数

    public List<PoolThread> idleThreads = new ArrayList<>(); // 空闲线程列表

    public List<PoolThread> allThreads = new ArrayList<>(); // 所有创建过的线程

    public volatile boolean stop = false; // 停止标志，可以"温和"地停止线程

    public Semaphore semaphore; // 控制线程数的信号量

    public volatile boolean allowCommit = true;

    public CustomThreadPool() {
        semaphore = new Semaphore(max_thread_num);
    }

    public CustomThreadPool(int max_thread_num) {
        this.max_thread_num = max_thread_num;
        semaphore = new Semaphore(max_thread_num);
    }


    /**
     * 提交任务
     *
     */
//    public void submit(Runnable task) {
//        if (!allowCommit) {
//            throw new RuntimeException("fail to commit task because the thread pool has been closed");
//        }
//
//        try {
//            semaphore.acquire();
//        } catch (InterruptedException e) {
//            throw new RuntimeException("parent thread fail to acquire semaphore!", e);
//        }
//        PoolThread poolThread;
//        synchronized (this) {
//            if (!idleThreads.isEmpty()) {
//                poolThread = idleThreads.remove(idleThreads.size() - 1);
//                poolThread.task = task;
//                while (poolThread.giveBack) {
//                    synchronized (poolThread.lock) {
//                        poolThread.lock.notify();
//                    }
//                }
//                poolThread.semaphore.release(); // V
//
//            } else {
//                poolThread = new PoolThread(this);
//                poolThread.task = task;
//                allThreads.add(poolThread);
//                poolThread.start();
//            }
//        }
//    }

    public <T> Future<T> submit(Callable<T> task) {
        if (!allowCommit) {
            throw new RuntimeException("fail to commit task because the thread pool has been closed");
        }

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("parent thread fail to acquire semaphore!", e);
        }

        PoolThread poolThread;
        FutureTask<T> future = new FutureTask<>(task);

        synchronized (this) {
            if (!idleThreads.isEmpty()) {
                poolThread = idleThreads.remove(idleThreads.size() - 1);
                poolThread.task = task;
                poolThread.future = future;
                while (poolThread.giveBack) {
                    synchronized (poolThread.lock) {
                        poolThread.lock.notify();
                    }
                }
                poolThread.semaphore.release(); // V

            } else {
                poolThread = new PoolThread(this);
                poolThread.task = task;
                poolThread.future = future;
                allThreads.add(poolThread);
                poolThread.start();
            }
        }

        return future;
    }

    /**
     * 不再提交任务，等待已有任务完成，温和停止工作线程，并等待他们结束。
     * shutdown()后不能再调用commitTask()
     * 保证已提交任务都完成
     */
    public void shutdown() {
        allowCommit = false;
        for (int i = 0; i < max_thread_num; i++) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException("the parent thread fail to acquire");
            }
        }
        // 此时idleThreads和allThreads大小相同
        stop = true;
        for (PoolThread poolThread : allThreads) {
            while (poolThread.giveBack) {
                synchronized (poolThread.lock) {
                    poolThread.lock.notify();
                }
            }
        }
        for (PoolThread poolThread : allThreads) {
            try {
                poolThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("the parent thread waiting for child to finish error");
            }
        }
        allThreads.clear();
        idleThreads.clear();
        System.out.println("线程池正确结束");
    }

    public int idleThreads() {
        return idleThreads.size();
    }

    public int maxThreads() {
        return max_thread_num;
    }


    public int currentThreads() {
        return allThreads.size();
    }
}