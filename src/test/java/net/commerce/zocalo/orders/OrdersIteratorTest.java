package net.commerce.zocalo.orders;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.IncompatibleOrderException;
import net.commerce.zocalo.hibernate.HibernateTestUtil;

import java.util.Iterator;

public class OrdersIteratorTest extends OrdersTestCase {
    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testSimpleIteration() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        addOrder(book, yes, "30", 10.0, makeUser("buyer"));
        Iterator it = book.iterateOffers(yes);
        assertTrue(it.hasNext());
        addOrder(book, yes, "40", 0.0, makeUser("buyer"));
        assertTrue(it.hasNext());
    }

    public void testIterationWithOneZero() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        addOrder(book, yes, "40", 10.0, makeUser("buyer"));
        addOrder(book, yes, "30", 0.0, makeUser("buyer"));
        addOrder(book, yes, "50", 40, makeUser("buyer"));
        addOrder(book, yes, "20", 23, makeUser("buyer"));
        Iterator it = book.iterateOffers(yes);
        assertTrue(it.hasNext());
        assertQEquals(20, ((Order) it.next()).price());
        assertTrue(it.hasNext());
        assertQEquals(40, ((Order) it.next()).price());
        assertTrue(yesOrders.toString().matches(".*,.*,.*"));
    }

    public void testIterateWithNext() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "20", 23, makeUser("buyer"));
        addOrder(book, yes, "30", 0.0, makeUser("buyer"));
        addOrder(book, yes, "35", 10.0, makeUser("buyer"));
        addOrder(book, yes, "40", 0, makeUser("buyer"));
        addOrder(book, yes, "50", 40, makeUser("buyer"));
        Iterator it = book.iterateOffers(yes);

        assertQEquals(20, ((Order) it.next()).price());
        assertQEquals(35, ((Order) it.next()).price());
        assertQEquals(50, ((Order) it.next()).price());
        assertFalse(it.hasNext());
    }
}
