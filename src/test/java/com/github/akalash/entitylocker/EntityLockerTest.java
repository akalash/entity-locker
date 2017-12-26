package com.github.akalash.entitylocker;


import org.testng.annotations.Test;

import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertTrue;

/**
 * Test for {@link EntityLocker}
 *
 * @author Kalashnikov Anton <kaa.dev@yandex.ru>
 * @since 24.12.2017
 */
public class EntityLockerTest {
    @Test
    public void should_isolateExecuted_when_twoThreadsHasSameId() throws Exception {
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);
        EntityLocker entityLocker = new EntityLocker();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> firstThreadTime = executorService.submit(() -> {
            entityLocker.exclusive(1, () -> {
                ensureFirstThreadHasLockFirst.countDown();
                doWork(10);
            });
            return System.currentTimeMillis();
        });
        Future<Long> secondThreadTime = executorService.submit(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            entityLocker.exclusive(1, () -> doWork(1));
            return System.currentTimeMillis();
        });

        assertThat(secondThreadTime.get(), greaterThan(firstThreadTime.get()));
    }

    @Test
    public void should_reentrantLock_when_takeLockInsideOtherLock() throws Exception {
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);
        EntityLocker entityLocker = new EntityLocker();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> firstThreadTime = executorService.submit(() -> {
            entityLocker.exclusive(1, () -> {
                ensureFirstThreadHasLockFirst.countDown();
                entityLocker.exclusive(2, 1, () -> doWork(10));
                doWork(10);
            });
            return System.currentTimeMillis();
        });
        Future<Long> secondThreadTime = executorService.submit(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            entityLocker.exclusive(1, () -> doWork(1));
            return System.currentTimeMillis();
        });

        assertThat(secondThreadTime.get(), greaterThan(firstThreadTime.get()));
    }

    @Test
    public void should_globalLockExecuteIsolated_when_tryToTakeLockInOtherThreads() throws Exception {
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);
        EntityLocker entityLocker = new EntityLocker();

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Future<Long> firstThreadTime = executorService.submit(() -> {
            entityLocker.globalExclusive(() -> {
                ensureFirstThreadHasLockFirst.countDown();
                doWork(10);
            });
            return System.currentTimeMillis();
        });
        Future<Long> secondThreadTime = executorService.submit(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            entityLocker.exclusive(1, () -> doWork(1));
            return System.currentTimeMillis();
        });
        Future<Long> thirdThreadTime = executorService.submit(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            entityLocker.globalExclusive(() -> doWork(1));
            return System.currentTimeMillis();
        });

        assertThat(secondThreadTime.get(), greaterThan(firstThreadTime.get()));
        assertThat(thirdThreadTime.get(), greaterThan(firstThreadTime.get()));
    }

    private void doWork(long time) {
        interruptWrapper(() -> Thread.sleep(time));
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