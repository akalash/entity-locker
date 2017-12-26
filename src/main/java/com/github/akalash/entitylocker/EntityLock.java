package com.github.akalash.entitylocker;

/**
 * Interface of entity lock.
 *
 * @author Kalashnikov Anton <kaa.dev@yandex.ru>
 * @since 24.12.2017
 */
interface EntityLock {
    /**
     * Take the lock.
     */
    void lock();

    /**
     * Release the lock.
     */
    void unlock();
}
