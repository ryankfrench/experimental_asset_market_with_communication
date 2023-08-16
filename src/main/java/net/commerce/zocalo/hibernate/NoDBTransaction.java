package net.commerce.zocalo.hibernate;

import org.hibernate.Transaction;
import org.hibernate.HibernateException;

import javax.transaction.Synchronization;
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a do-nothing implementation of Hibernate's transaction that can be used when
 hibernate is disabled.
 */
public class NoDBTransaction implements Transaction {
    public static Transaction make() {
        return new NoDBTransaction();
    }

    public void commit() throws HibernateException {
        // NOOP
    }

    public void rollback() throws HibernateException {
        // NOOP
    }

    public boolean wasRolledBack() throws HibernateException {
        return false;
    }

    public boolean wasCommitted() throws HibernateException {
        return false;
    }

    public boolean isActive() throws HibernateException {
        return false;
    }

    public void registerSynchronization(Synchronization synchronization) throws HibernateException {
        // NOOP
    }
}
