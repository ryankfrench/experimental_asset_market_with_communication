package net.commerce.zocalo.hibernate;

import org.hibernate.*;
import org.hibernate.type.Type;

import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
// Copyright 2006-2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a do-nothing implementation of Hibernate's Query that can be used when
 hibernate is disabled. */
public class NoDBQuery implements Query {
    public String getQueryString() {
        return null;
    }

    public Type[] getReturnTypes() throws HibernateException {
        return new Type[0];
    }

    public String[] getReturnAliases() throws HibernateException {
        return new String[0];
    }

    public String[] getNamedParameters() throws HibernateException {
        return new String[0];
    }

    public Iterator iterate() throws HibernateException {
        return null;
    }

    public ScrollableResults scroll() throws HibernateException {
        return null;
    }

    public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
        return null;
    }

    public List list() throws HibernateException {
        return new ArrayList();
    }

    public Object uniqueResult() throws HibernateException {
        return null;
    }

    public int executeUpdate() throws HibernateException {
        return 0;
    }

    public Query setMaxResults(int maxResults) {
        return null;
    }

    public Query setFirstResult(int firstResult) {
        return null;
    }

    public Query setReadOnly(boolean readOnly) {
        return null;
    }

    public Query setCacheable(boolean cacheable) {
        return null;
    }

    public Query setCacheRegion(String cacheRegion) {
        return null;
    }

    public Query setTimeout(int timeout) {
        return null;
    }

    public Query setFetchSize(int fetchSize) {
        return null;
    }

    public Query setLockMode(String alias, LockMode lockMode) {
        return null;
    }

    public Query setComment(String comment) {
        return null;
    }

    public Query setFlushMode(FlushMode flushMode) {
        return null;
    }

    public Query setCacheMode(CacheMode cacheMode) {
        return null;
    }

    public Query setParameter(int position, Object val, Type type) {
        return null;
    }

    public Query setParameter(String name, Object val, Type type) {
        return null;
    }

    public Query setParameter(int position, Object val) throws HibernateException {
        return null;
    }

    public Query setParameter(String name, Object val) throws HibernateException {
        return null;
    }

    public Query setParameters(Object[] values, Type[] types) throws HibernateException {
        return null;
    }

    public Query setParameterList(String name, Collection vals, Type type) throws HibernateException {
        return null;
    }

    public Query setParameterList(String name, Collection vals) throws HibernateException {
        return null;
    }

    public Query setParameterList(String name, Object[] vals, Type type) throws HibernateException {
        return null;
    }

    public Query setParameterList(String name, Object[] vals) throws HibernateException {
        return null;
    }

    public Query setProperties(Object bean) throws HibernateException {
        return null;
    }

    public Query setString(int position, String val) {
        return null;
    }

    public Query setCharacter(int position, char val) {
        return null;
    }

    public Query setBoolean(int position, boolean val) {
        return null;
    }

    public Query setByte(int position, byte val) {
        return null;
    }

    public Query setShort(int position, short val) {
        return null;
    }

    public Query setInteger(int position, int val) {
        return null;
    }

    public Query setLong(int position, long val) {
        return null;
    }

    public Query setFloat(int position, float val) {
        return null;
    }

    public Query setDouble(int position, double val) {
        return null;
    }

    public Query setBinary(int position, byte[] val) {
        return null;
    }

    public Query setText(int position, String val) {
        return null;
    }

    public Query setSerializable(int position, Serializable val) {
        return null;
    }

    public Query setLocale(int position, Locale locale) {
        return null;
    }

    public Query setBigDecimal(int position, BigDecimal number) {
        return null;
    }

    public Query setBigInteger(int position, BigInteger number) {
        return null;
    }

    public Query setDate(int position, Date date) {
        return null;
    }

    public Query setTime(int position, Date date) {
        return null;
    }

    public Query setTimestamp(int position, Date date) {
        return null;
    }

    public Query setCalendar(int position, Calendar calendar) {
        return null;
    }

    public Query setCalendarDate(int position, Calendar calendar) {
        return null;
    }

    public Query setString(String name, String val) {
        return null;
    }

    public Query setCharacter(String name, char val) {
        return null;
    }

    public Query setBoolean(String name, boolean val) {
        return null;
    }

    public Query setByte(String name, byte val) {
        return null;
    }

    public Query setShort(String name, short val) {
        return null;
    }

    public Query setInteger(String name, int val) {
        return null;
    }

    public Query setLong(String name, long val) {
        return null;
    }

    public Query setFloat(String name, float val) {
        return null;
    }

    public Query setDouble(String name, double val) {
        return null;
    }

    public Query setBinary(String name, byte[] val) {
        return null;
    }

    public Query setText(String name, String val) {
        return null;
    }

    public Query setSerializable(String name, Serializable val) {
        return null;
    }

    public Query setLocale(String name, Locale locale) {
        return null;
    }

    public Query setBigDecimal(String name, BigDecimal number) {
        return null;
    }

    public Query setBigInteger(String name, BigInteger number) {
        return null;
    }

    public Query setDate(String name, Date date) {
        return null;
    }

    public Query setTime(String name, Date date) {
        return null;
    }

    public Query setTimestamp(String name, Date date) {
        return null;
    }

    public Query setCalendar(String name, Calendar calendar) {
        return null;
    }

    public Query setCalendarDate(String name, Calendar calendar) {
        return null;
    }

    public Query setEntity(int position, Object val) // use setParameter for null values
    {
        return null;
    }

    public Query setEntity(String name, Object val) // use setParameter for null values
    {
        return null;
    }
}
