/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.service.d_l;

import com.akiban.server.service.ServiceManagerImpl;
import com.akiban.server.service.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class DDLReadWriteLockHook implements DStarLFunctionsHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(DDLReadWriteLockHook.class);
    private static final Session.Key<Lock> LOCK_KEY = Session.Key.of("READWRITE_LOCK");
    static final String IS_LOCK_FAIR_PROPERTY = "akserver.dstarl.lock.fair";

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock( isFair() );

    @Override
    public void hookFunctionIn(Session session, DDLFunction function) {
        final Lock lock;
        if (DStarLFunctionsHook.DStarLType.DDL_FUNCTIONS_WRITE.equals(function.getType())) {
            assert (!readWriteLock.isWriteLocked()) || readWriteLock.isWriteLockedByCurrentThread()
                    : "Another thread has the write lock! Writes are supposed to be single-threaded";
            lock = readWriteLock.writeLock();
        }
        else {
            lock = readWriteLock.readLock();
        }
        Lock oldLock = session.put(LOCK_KEY, lock);
        lock.lock();
        assert oldLock == null : oldLock;
    }

    @Override
    public void hookFunctionCatch(Session session, DDLFunction function, Throwable throwable) {
        // nothing to do
    }

    @Override
    public void hookFunctionFinally(Session session, DDLFunction function) {
        Lock lock = session.remove(LOCK_KEY);
        if (lock == null) {
            String errString = "Lock was null! Some lock has escaped, and this could be a deadlock!";
            LOGGER.error(errString);
            throw new NullPointerException(errString);
        }
        lock.unlock();
    }

    private static boolean isFair() {
        String isFairString = ServiceManagerImpl.get().getConfigurationService().getProperty(IS_LOCK_FAIR_PROPERTY);
        return Boolean.parseBoolean(isFairString);
    }
}
