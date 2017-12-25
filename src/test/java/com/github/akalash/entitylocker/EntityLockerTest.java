package com.github.akalash.entitylocker;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kalashnikov Anton <akalash.dev@gmail.com>
 * @since 24.12.2017
 */
public class EntityLockerTest {
    @Test
    public void testLock() throws Exception {
        EntityLocker entityLocker = new EntityLocker();
        AtomicLong executionTimeThread1 = new AtomicLong(0);
        AtomicLong executionTimeThread2 = new AtomicLong(0);
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);
        Thread thread1 = new Thread(() -> {
            EntityLocker.EntityLock lock = entityLocker.lock(1, 2);
            ensureFirstThreadHasLockFirst.countDown();
            interruptWrapper(() -> Thread.sleep(20));
            EntityLocker.EntityLock lock2 = entityLocker.lock(1);
            interruptWrapper(() -> Thread.sleep(20));
            lock2.unlock();
            lock.unlock();
            executionTimeThread1.set(System.currentTimeMillis());
        });
        Thread thread2 = new Thread(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            EntityLocker.EntityLock lock = entityLocker.lock(2);
            interruptWrapper(() -> Thread.sleep(10));
            lock.unlock();
            executionTimeThread2.set(System.currentTimeMillis());
        });
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        Assert.assertTrue(executionTimeThread2.get() > executionTimeThread1.get());

    }

    private void interruptWrapper(InterruptExecutor executor) {
        try {
            executor.execute();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    interface InterruptExecutor {
        void execute() throws InterruptedException;
    }

}