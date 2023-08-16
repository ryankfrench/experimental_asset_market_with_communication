package net.commerce.zocalo.hibernate;

import org.hibernate.stat.SessionStatistics;
import org.hibernate.*;
import org.hibernate.type.Type;

import java.sql.Connection;
import java.io.Serializable;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a do-nothing implementation of Hibernate's {@link org.hibernate.classic.Session}
 that can be used when hibernate is disabled. */
public class NoDBSession implements org.hibernate.classic.Session {
    public Session getSession(EntityMode entityMode) {
        return new NoDBSession();
    }

    public SessionFactory getSessionFactory() {
        return NoDBSessionFactory.make();
    }

    public static org.hibernate.classic.Session make() {
        return new NoDBSession();
    }

    public EntityMode getEntityMode() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void flush() throws HibernateException {
        // NOOP
    }

    public void setFlushMode(FlushMode flushMode) {
        // NOOP
    }

    public FlushMode getFlushMode() {
        return null;
    }

    public void setCacheMode(CacheMode cacheMode) {
        // NOOP
    }

    public CacheMode getCacheMode() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Connection connection() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Connection disconnect() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void reconnect() throws HibernateException {
    }

    public void reconnect(Connection connection) throws HibernateException {
    }

    public Connection close() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cancelQuery() throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isOpen() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isConnected() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isDirty() throws HibernateException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Serializable getIdentifier(Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean contains(Object object) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void evict(Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object load(Class theClass, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object load(String entityName, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void load(Object object, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Serializable save(Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void save(Object object, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Serializable save(String entityName, Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void save(String entityName, Object object, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void saveOrUpdate(Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void saveOrUpdate(String entityName, Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void update(Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void update(Object object, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void update(String entityName, Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void update(String entityName, Object object, Serializable id) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object merge(Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object merge(String entityName, Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void persist(Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void persist(String entityName, Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void delete(Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void lock(Object object, LockMode lockMode) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void refresh(Object object) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void refresh(Object object, LockMode lockMode) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public LockMode getCurrentLockMode(Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Transaction beginTransaction() throws HibernateException {
        return NoDBTransaction.make();
    }

    public Criteria createCriteria(Class persistentClass) {
        return new NoDBCriteria();
    }

    public Criteria createCriteria(Class persistentClass, String alias) {
        return new NoDBCriteria();
    }

    public Criteria createCriteria(String entityName) {
        return new NoDBCriteria();
    }

    public Criteria createCriteria(String entityName, String alias) {
        return new NoDBCriteria();
    }

    public Query createQuery(String queryString) throws HibernateException {
        return new NoDBQuery();
    }

    public SQLQuery createSQLQuery(String queryString) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Query createFilter(Object collection, String queryString) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Query getNamedQuery(String queryName) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clear() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object get(Class clazz, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object get(String entityName, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getEntityName(Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Filter enableFilter(String filterName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Filter getEnabledFilter(String filterName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void disableFilter(String filterName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public SessionStatistics getStatistics() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object saveOrUpdateCopy(Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object saveOrUpdateCopy(Object object, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object saveOrUpdateCopy(String entityName, Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List find(String query) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List find(String query, Object value, Type type) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List find(String query, Object[] values, Type[] types) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator iterate(String query) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator iterate(String query, Object value, Type type) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection filter(Object collection, String filter) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection filter(Object collection, String filter, Object[] values, Type[] types) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int delete(String query) throws HibernateException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int delete(String query, Object value, Type type) throws HibernateException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int delete(String query, Object[] values, Type[] types) throws HibernateException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Query createSQLQuery(String sql, String returnAlias, Class returnClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Query createSQLQuery(String sql, String[] returnAliases, Class[] returnClasses) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
