package com.github.akalash.entitylocker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author Kalashnikov Anton <akalash.dev@gmail.com>
 * @since 24.12.2017
 */
public class EntityLocker {

    ConcurrentHashMap<Object, AtomLock> lockStorage = new ConcurrentHashMap<>();
    ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    public EntityUnlocker lock(Object id, Object... ids) {
        return lock(resolveLock(id, ids));
    }

    public void isolate(Object id, Runnable runnable) {
        isolate(runnable, id);
    }

    public void isolate(Object id1, Object id2, Runnable runnable) {
        isolate(runnable, id1, id2);
    }

    private void isolate(Runnable runnable, Object id, Object... ids) {
        EntityLock entityLock = resolveLock(id, ids);
        entityLock.lock();
        try {
            runnable.run();
        } finally {
            entityLock.unlock();
        }
    }

    public EntityUnlocker globalLock() {
        return lock(new GlobalEntityLock(globalLock));
    }

    private EntityUnlocker lock(EntityLock lock) {
        lock.lock();
        return lock;
    }

    private EntityLock resolveLock(Object id, Object... ids) {
        if (ids.length == 0) {
            return new ComplexEntityLock(lockStorage, Collections.singletonList(id), globalLock);
        }

        HashSet<Object> lockIds = new HashSet<>(Arrays.asList(ids));
        lockIds.add(id);
        return new ComplexEntityLock(lockStorage, lockIds, globalLock);
    }
}
