package com.github.akalash.entitylocker;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ComplexEntityLock implements EntityLock {
    private final ConcurrentHashMap<Object, AtomLock> lockStorage;
    private final List<AtomLock> atomLocks;

    public ComplexEntityLock(ConcurrentHashMap<Object, AtomLock> lockStorage, Collection<Object> lockIds, ReentrantReadWriteLock globalLock) {
        this.lockStorage = lockStorage;
        atomLocks = lockIds.stream()
                .sorted(Comparator.comparingInt(Object::hashCode))
                .map(idKey -> lockStorage.compute(
                        idKey,
                        (key, holder) -> (holder == null ? new AtomLock(key, globalLock) : holder).prelock()
                ))
                .collect(Collectors.toList());
    }

    public void lock() {
        for (AtomLock atomLock : atomLocks) {
            atomLock.lock();
        }
    }

    public void unlock() {
        ListIterator<AtomLock> atomLockListIterator = atomLocks.listIterator(atomLocks.size());
        while (atomLockListIterator.hasPrevious()) {
            AtomLock previous = atomLockListIterator.previous();
            previous.unlock();
            lockStorage.compute(previous.getLockedObject(), (key, lock) -> lock.hasWaiters() ? lock : null);
        }
    }
}
