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

    ConcurrentHashMap<Object, AtomLockHolder> lockStorage = new ConcurrentHashMap<>();
    ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    public EntityLock lock(Object id, Object... ids) {
        HashSet<Object> lockIds = new HashSet<>(Arrays.asList(ids));
        lockIds.add(id);

        List<AtomLockHolder.AtomLock> lockHolders = lockIds.stream()
                .sorted(Comparator.comparingInt(Object::hashCode))
                .map(idKey -> lockStorage.compute(
                        idKey,
                        (key, holder) -> (holder == null ? new AtomLockHolder(globalLock, key) : holder).acquire()
                ))
                .map(AtomLockHolder::getLock)
                .collect(Collectors.toList());
        EntityLock entityLock = new EntityLock(lockStorage, lockHolders);
        entityLock.lock();
        return entityLock;
    }


    public static class EntityLock {
        private static final AtomLockHolder UNUSED_HOLDER = new AtomLockHolder(null, null);
        private final ConcurrentHashMap<Object, AtomLockHolder> lockStorage;
        private final List<AtomLockHolder.AtomLock> atomLocks;

        public EntityLock(ConcurrentHashMap<Object, AtomLockHolder> lockStorage, List<AtomLockHolder.AtomLock> atomLocks) {
            this.lockStorage = lockStorage;
            this.atomLocks = atomLocks;
        }

        private void lock() {
            for (AtomLockHolder.AtomLock atomLock : atomLocks) {
                atomLock.lock();
            }
        }

        public void unlock() {
            ListIterator<AtomLockHolder.AtomLock> atomLockListIterator = atomLocks.listIterator(atomLocks.size());
            while (atomLockListIterator.hasPrevious()) {
                AtomLockHolder.AtomLock previous = atomLockListIterator.previous();
                previous.unlock();
                lockStorage.compute(previous.getLockedObject(), (key, lock) -> lock.getAcquiredLockCount() == 0 ? null : lock);
            }
        }
    }
}
