package net.commerce.zocalo.market;

import net.commerce.zocalo.orders.Order;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** An Exception that is generated when someone tries to enter an order that would
    be indistinguishable from an existing order based on (owner, price, claim).  */

public class DuplicateOrderException extends Exception {
    public DuplicateOrderException(Order order) {
        super("Each player may only have one order at each price: " + order);
    }
}
