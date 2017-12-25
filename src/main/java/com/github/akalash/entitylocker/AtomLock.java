package com.github.akalash.entitylocker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class AtomLock {
    private final Object lockedObject;
    private final ReentrantLock lock;
    private final ReentrantReadWriteLock globalLock;
    private final AtomicInteger acquiredLockCount;

    AtomLock(Object lockedObject, ReentrantReadWriteLock globalLock) {
        this.lockedObject = lockedObject;
        this.lock = new ReentrantLock();
        this.globalLock = globalLock;
        this.acquiredLockCount = new AtomicInteger(0);
    }

    public Object getLockedObject() {
        return lockedObject;
    }

    public boolean hasWaiters() {
        return acquiredLockCount.get() != 0;
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
