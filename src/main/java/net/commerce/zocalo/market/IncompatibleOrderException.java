package net.commerce.zocalo.market;

import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.user.User;
// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** An Exception that is generated when we try to create an order that should
    be immediately accepted because it would cause the total offer for all positions
    to exceed their par value.  */
public class IncompatibleOrderException extends Exception {
    public IncompatibleOrderException(String errorMsg) {
        super(errorMsg);
    }
}
