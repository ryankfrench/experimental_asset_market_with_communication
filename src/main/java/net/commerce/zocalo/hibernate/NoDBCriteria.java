package net.commerce.zocalo.hibernate;

import org.hibernate.*;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;

import java.util.List;
import java.util.ArrayList;
// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a do-nothing implementation of Hibernate's Criteria that can be used when
 hibernate is disabled. */
public class NoDBCriteria implements Criteria {
    public NoDBCriteria() {
    }

    public Criteria add(Criterion criterion) {
        return this;
    }

    public Criteria addOrder(Order order) {
        return this;
    }

    public Criteria setFetchMode(String associationPath, FetchMode mode) throws HibernateException {
        return this;
    }

    public Criteria createAlias(String associationPath, String alias) throws HibernateException {
        return this;
    }

    public Criteria createCriteria(String associationPath) throws HibernateException {
        return this;
    }

    public Criteria createCriteria(String associationPath, String alias) throws HibernateException {
        return this;
    }

    public Criteria setProjection(Projection projection) {
        return this;
    }

    public String getAlias() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Criteria setResultTransformer(ResultTransformer resultTransformer) {
        return this;
    }

    public Criteria setMaxResults(int maxResults) {
        return this;
    }

    public Criteria setFirstResult(int firstResult) {
        return this;
    }

    public Criteria setFetchSize(int fetchSize) {
        return this;
    }

    public Criteria setTimeout(int timeout) {
        return this;
    }

    public Criteria setCacheable(boolean cacheable) {
        return this;
    }

    public Criteria setCacheRegion(String cacheRegion) {
        return this;
    }

    public List list() throws HibernateException {
        return new ArrayList();
    }

    public ScrollableResults scroll() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object uniqueResult() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Criteria setLockMode(LockMode lockMode) {
        return this;
    }

    public Criteria setLockMode(String alias, LockMode lockMode) {
        return this;
    }

    public Criteria setComment(String comment) {
        return this;
    }

    public Criteria setFlushMode(FlushMode flushMode) {
        return this;
    }

    public Criteria setCacheMode(CacheMode cacheMode) {
        return this;
    }
}
