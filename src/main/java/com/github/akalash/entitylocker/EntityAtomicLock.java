package com.github.akalash.entitylocker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represent of minimum lock unit for locking entity.
 * Contains information about locked object, current locks and number of waiters for lock.
 *
 * @author Kalashnikov Anton <kaa.dev@yandex.ru>
 * @since 24.12.2017
 */
class EntityAtomicLock {
    private final Object lockedObject;
    private final ReentrantLock lock;
    private final ReentrantReadWriteLock globalLock;
    private final AtomicInteger acquiredLockCount;

    EntityAtomicLock(Object lockedObject, ReentrantReadWriteLock globalLock) {
        this.lockedObject = lockedObject;
        this.lock = new ReentrantLock();
        this.globalLock = globalLock;
        this.acquiredLockCount = new AtomicInteger(0);
    }

    /**
     * Retrieve current locked object
     *
     * @return object which locked current lock
     */
    Object getLockedObject() {
        return lockedObject;
    }

    /**
     * @return true if current lock has waiters which took the lock or will take the lock or false otherwise
     */
    boolean hasWaiters() {
        return acquiredLockCount.get() != 0;
    }

    /**
     * Informing to lock about wish to acquire it.
     * lightweight action for using it in blocked section
     *
     * @return current lock
     */
    EntityAtomicLock prelock() {
        acquiredLockCount.incrementAndGet();
        return this;
    }

    void lock() {
        globalLock.readLock().lock();
        lock.lock();
    }

    void unlock() {
        lock.unlock();
        globalLock.readLock().unlock();
        acquiredLockCount.decrementAndGet();
    }
}
