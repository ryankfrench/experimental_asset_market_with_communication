package net.commerce.zocalo.orders;

import net.commerce.zocalo.currency.Quantity;

import java.util.Iterator;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Iterator for Orders.  */
public class OrdersIterator implements Iterator {
    final private Iterator it;
    private Order next;

    public OrdersIterator(Iterator it) {
        this.it = it;
    }

    public boolean hasNext() {
        if (null == next) {
            if (!it.hasNext()) {
                return false;
            }
            next = (Order) it.next();
            return hasNext();
        }

        if (!next.quantity().approaches(Quantity.ZERO)) {
            return true;
        }
        next = null;
        return hasNext();
    }

    public Object next() {
        if (null == next) {
            next = (Order) it.next();
            return next();
        }
        if (next.quantity().approaches(Quantity.ZERO)) {
            next = (Order) it.next();
            return next();
        }

        Object retVal = next;
        next = null;
        return retVal;
    }

    public void remove() {
        throw new RuntimeException("can't remove orders this way");
    }
}
