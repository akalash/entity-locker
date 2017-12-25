package com.github.akalash.entitylocker;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GlobalEntityLock implements EntityLock {
    private final ReentrantReadWriteLock globalLock;

    public GlobalEntityLock(ReentrantReadWriteLock globalLock) {
        this.globalLock = globalLock;
    }

    public void lock() {
        globalLock.writeLock().lock();
    }

    @Override
    public void unlock() {
        globalLock.writeLock().unlock();
    }
}
