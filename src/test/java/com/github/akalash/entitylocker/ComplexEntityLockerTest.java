package com.github.akalash.entitylocker;


import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertTrue;

/**
 * @author Kalashnikov Anton <akalash.dev@gmail.com>
 * @since 24.12.2017
 */
public class ComplexEntityLockerTest {
    @Test
    public void should_executeProtectedCodeContinuously() throws Exception {
        EntityLocker entityLocker = new EntityLocker();

        ThreadTestResult result = twoThreadTest(() -> entityLocker.lock(1), () -> entityLocker.lock(1));

        assertThat(result.secondThreadFinishTime, greaterThan(result.firstThreadFinishTime));
    }

    @Test
    public void should_executeProtectedCodeContinuously_when_lockMoreThanOneId() throws Exception {
        EntityLocker entityLocker = new EntityLocker();

        ThreadTestResult result = twoThreadTest(() -> entityLocker.lock(1, 2, 3), () -> entityLocker.lock(3));

        assertThat(result.secondThreadFinishTime, greaterThan(result.firstThreadFinishTime));
    }

    @Test
    public void should_executeInParallel_when_lockOnDifferentId() throws Exception {
        EntityLocker entityLocker = new EntityLocker();

        ThreadTestResult result = twoThreadTest(() -> entityLocker.lock(1), () -> entityLocker.lock(2));

        assertThat(result.secondThreadFinishTime, lessThan(result.firstThreadFinishTime));
    }

    @Test
    public void should_executeInParallel_when_lockOnDifferentObjects() throws Exception {
        EntityLocker entityLocker = new EntityLocker();

        ThreadTestResult result = twoThreadTest(() -> entityLocker.lock(1L), () -> entityLocker.lock(1));

        assertThat(result.secondThreadFinishTime, lessThan(result.firstThreadFinishTime));
    }

    @Test
    public void should_executeProtectedCodeContinuously_when_globalLock() throws Exception {
        EntityLocker entityLocker = new EntityLocker();

        ThreadTestResult result = twoThreadTest(entityLocker::globalLock, () -> entityLocker.lock(3));

        assertThat(result.secondThreadFinishTime, greaterThan(result.firstThreadFinishTime));
    }

    @Test
    public void should_reentrantLock_when_lockInsideProtectedCode() throws Exception {
        EntityLocker entityLocker = new EntityLocker();
        ThreadTestResult result = twoThreadTest(() -> {
                    EntityUnlocker lock = entityLocker.lock(1);
                    EntityUnlocker innerLock = entityLocker.lock(1);
                    doWork(10);
                    innerLock.unlock();
                    return lock;
                },
                () -> entityLocker.lock(1)
        );

        assertThat(result.secondThreadFinishTime, greaterThan(result.firstThreadFinishTime));
    }

    private ThreadTestResult twoThreadTest(Supplier<EntityUnlocker> lockFirstThreadSupplier, Supplier<EntityUnlocker> lockSecondThreadSupplier) throws Exception {
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> firstThreadTime = executorService.submit(
                () -> {
                    EntityUnlocker lock = lockFirstThreadSupplier.get();
                    ensureFirstThreadHasLockFirst.countDown();
                    doWork(10);
                    lock.unlock();
                    return System.currentTimeMillis();
                }
        );
        Future<Long> secondThreadTime = executorService.submit(
                () -> {
                    interruptWrapper(ensureFirstThreadHasLockFirst::await);
                    EntityUnlocker lock = lockSecondThreadSupplier.get();
                    doWork(1);
                    lock.unlock();
                    return System.currentTimeMillis();
                }
        );

        return new ThreadTestResult(firstThreadTime.get(), secondThreadTime.get());


    }

    @Test
    public void should_isolateExecute_when() throws Exception {
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);
        EntityLocker entityLocker = new EntityLocker();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> firstThreadTime = executorService.submit(() -> {
            entityLocker.isolate(1, () -> {
                ensureFirstThreadHasLockFirst.countDown();
                doWork(10);
            });
            return System.currentTimeMillis();
        });
        Future<Long> secondThreadTime = executorService.submit(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            entityLocker.isolate(1, () -> doWork(1));
            return System.currentTimeMillis();
        });

        assertThat(secondThreadTime.get(), greaterThan(firstThreadTime.get()));
    }

    @Test
    public void should_reentraceIsolateCode_when_() throws Exception {
        CountDownLatch ensureFirstThreadHasLockFirst = new CountDownLatch(1);
        EntityLocker entityLocker = new EntityLocker();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> firstThreadTime = executorService.submit(() -> {
            entityLocker.isolate(1, () -> {
                ensureFirstThreadHasLockFirst.countDown();
                entityLocker.isolate(2, 1, () -> doWork(10));
                doWork(10);
            });
            return System.currentTimeMillis();
        });
        Future<Long> secondThreadTime = executorService.submit(() -> {
            interruptWrapper(ensureFirstThreadHasLockFirst::await);
            entityLocker.isolate(1, () -> doWork(1));
            return System.currentTimeMillis();
        });

        assertThat(secondThreadTime.get(), greaterThan(firstThreadTime.get()));
    }


    class ThreadTestResult {
        long firstThreadFinishTime;
        long secondThreadFinishTime;

        public ThreadTestResult(long firstThreadFinishTime, long secondThreadFinishTime) {
            this.firstThreadFinishTime = firstThreadFinishTime;
            this.secondThreadFinishTime = secondThreadFinishTime;
        }
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