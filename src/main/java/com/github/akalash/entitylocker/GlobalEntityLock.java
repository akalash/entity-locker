package com.github.akalash.entitylocker;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link EntityLock} which take the global lock.
 *
 * @author Kalashnikov Anton <kaa.dev@yandex.ru>
 * @since 24.12.2017
 */
class GlobalEntityLock implements EntityLock {
    private final ReentrantReadWriteLock globalLock;

    GlobalEntityLock(ReentrantReadWriteLock globalLock) {
        this.globalLock = globalLock;
    }

    @Override
    public void lock() {
        globalLock.writeLock().lock();
    }

    @Override
    public void unlock() {
        globalLock.writeLock().unlock();
    }
}
