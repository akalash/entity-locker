package com.github.akalash.entitylocker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Kalashnikov Anton <akalash.dev@gmail.com>
 * @since 24.12.2017
 */
public class AtomLockHolder {
    private AtomicInteger acquiredLockCount = new AtomicInteger(0);
    private AtomLock atomLock;

    public AtomLockHolder(ReentrantReadWriteLock globalLock, Object lockedObject) {
        atomLock = new AtomLock(lockedObject, globalLock, acquiredLockCount);
    }

    public AtomLockHolder acquire() {
        acquiredLockCount.incrementAndGet();
        return this;
    }

    public AtomLock getLock() {
        return atomLock;
    }

    public Integer getAcquiredLockCount() {
        return acquiredLockCount.get();
    }

    class AtomLock {
        private final Object lockedObject;
        private final ReentrantLock lock;
        private final ReentrantReadWriteLock globalLock;
        private final AtomicInteger acquiredLockCount;

        AtomLock(Object lockedObject, ReentrantReadWriteLock globalLock, AtomicInteger acquiredLockCount) {
            this.lockedObject = lockedObject;
            this.lock = new ReentrantLock();
            this.globalLock = globalLock;
            this.acquiredLockCount = acquiredLockCount;
        }

        public Object getLockedObject() {
            return lockedObject;
        }

        public AtomLock prelock() {
            acquiredLockCount.incrementAndGet();
            return this;
        }

        public void lock() {
            globalLock.readLock().lock();
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
            globalLock.readLock().unlock();
            acquiredLockCount.decrementAndGet();
        }
    }
}
