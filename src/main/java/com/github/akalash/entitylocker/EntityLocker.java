package com.github.akalash.entitylocker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Helper for protected execution code by some object.
 * Execution code have isolated from other code with same id.
 *
 * @author Kalashnikov Anton <kaa.dev@yandex.ru>
 * @since 24.12.2017
 */
public class EntityLocker {

    private final ConcurrentHashMap<Object, EntityAtomicLock> lockStorage = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final EntityLock globalEntityLock = new GlobalEntityLock(globalLock);

    /**
     * Execution protected code which not executed concurrently with any other protected code.
     *
     * @param protectedCode code which should be executed isolated
     */
    public void globalExclusive(Runnable protectedCode) {
        exclusive(globalEntityLock, protectedCode);
    }

    /**
     * Execution protected code which not executed concurrently with protected code with same id.
     *
     * @param protectedCode code which should be executed isolated
     */
    public void exclusive(Object id, Runnable protectedCode) {
        exclusive(protectedCode, id);
    }

    /**
     * Execution protected code which not executed concurrently with protected code with same ids.
     *
     * @param protectedCode code which should be executed isolated
     */
    public void exclusive(Object id1, Object id2, Runnable protectedCode) {
        exclusive(protectedCode, id1, id2);
    }

    private void exclusive(Runnable protectedCode, Object... ids) {
        exclusive(resolveLock(ids), protectedCode);
    }

    private void exclusive(EntityLock entityLock, Runnable protectedCode) {
        entityLock.lock();
        try {
            protectedCode.run();
        } finally {
            entityLock.unlock();
        }
    }

    private EntityLock resolveLock(Object... ids) {
        List<EntityAtomicLock> entityAtomicLocks = new HashSet<>(Arrays.asList(ids)).stream()
                .sorted(Comparator.comparingInt(Object::hashCode))
                .map(idKey -> lockStorage.compute(
                        idKey,
                        (key, holder) -> (holder == null ? new EntityAtomicLock(key, globalLock) : holder).prelock()
                ))
                .collect(Collectors.toList());

        return new ComplexEntityLock(lockStorage, entityAtomicLocks);
    }
}
