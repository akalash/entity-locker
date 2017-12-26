package com.github.akalash.entitylocker;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EntityLock} which lock several object in one time.
 *
 * @author Kalashnikov Anton <kaa.dev@yandex.ru>
 * @since 24.12.2017
 */
class ComplexEntityLock implements EntityLock {
    private final ConcurrentHashMap<Object, EntityAtomicLock> lockStorage;
    private final List<EntityAtomicLock> entityAtomicLocks;

    ComplexEntityLock(ConcurrentHashMap<Object, EntityAtomicLock> lockStorage, List<EntityAtomicLock> entityAtomicLocks) {
        this.lockStorage = lockStorage;
        this.entityAtomicLocks = entityAtomicLocks;
    }

    @Override
    public void lock() {
        for (EntityAtomicLock entityAtomicLock : entityAtomicLocks) {
            entityAtomicLock.lock();
        }
    }

    /**
     * Unlocked current locks and remove their from common storage if it have not use.
     */
    @Override
    public void unlock() {
        ListIterator<EntityAtomicLock> atomLockListIterator = entityAtomicLocks.listIterator(entityAtomicLocks.size());
        while (atomLockListIterator.hasPrevious()) {
            EntityAtomicLock previous = atomLockListIterator.previous();
            previous.unlock();
            lockStorage.compute(previous.getLockedObject(), (key, lock) -> lock.hasWaiters() ? lock : null);
        }
    }
}
