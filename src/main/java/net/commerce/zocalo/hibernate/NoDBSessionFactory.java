package net.commerce.zocalo.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.Interceptor;
import org.hibernate.HibernateException;
import org.hibernate.stat.Statistics;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.classic.Session;

import javax.naming.Reference;
import javax.naming.NamingException;
import java.sql.Connection;
import java.util.Map;
import java.io.Serializable;

import net.commerce.zocalo.hibernate.NoDBSession;
// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a do-nothing implementation of Hibernate's SessionFactory that can be used when
 hibernate is disabled. */
public class NoDBSessionFactory implements SessionFactory {
    public static SessionFactory make() {
        return new NoDBSessionFactory();
    }

    public Session openSession(Connection connection) {
        return null;
    }

    public org.hibernate.classic.Session openSession() throws HibernateException {
        return NoDBSession.make();
    }

    public Session openSession(Interceptor interceptor) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Session openSession(Connection connection, Interceptor interceptor) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Session getCurrentSession() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getAllClassMetadata() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map getAllCollectionMetadata() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Statistics getStatistics() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isClosed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evict(Class persistentClass) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evict(Class persistentClass, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evictEntity(String entityName) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evictEntity(String entityName, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evictCollection(String roleName) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evictCollection(String roleName, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evictQueries() throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evictQueries(String cacheRegion) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Reference getReference() throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
