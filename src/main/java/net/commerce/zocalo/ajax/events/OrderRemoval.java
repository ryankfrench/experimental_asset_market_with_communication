package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;

/** Action representing the fact that a user removed an order from the book.  */
public class OrderRemoval extends PriceAction {
    private Quantity quantityFulfilled;
    private Quantity quantityVoided;

    private OrderRemoval(String name, Price price, Quantity quantity, Position pos, Quantity fulfilled, Quantity voided) {
        super(name, price, quantity, pos, PriceAction.getActionLogger());
        quantityFulfilled = fulfilled;
        quantityVoided = voided;
        log();
    }

    static public OrderRemoval newOrderRemoval(String owner, Price price, Quantity quantity, Position pos, Quantity fulfilled, Quantity voided) {
        OrderRemoval orderRemoval = new OrderRemoval(owner, price, quantity, pos, fulfilled, voided);
        HibernateUtil.save(orderRemoval);
        return orderRemoval;
    }

    /** @deprecated */
    public OrderRemoval() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return " cancelled Order ";
    }

    /** @deprecated */
    public Quantity getQuantityFulfilled() {
        return quantityFulfilled;
    }

    /** @deprecated */
    public void setQuantityFulfilled(Quantity quantityFulfilled) {
        this.quantityFulfilled = quantityFulfilled;
    }

    /** @deprecated */
    public Quantity getQuantityVoided() {
        return quantityVoided;
    }

    /** @deprecated */
    public void setQuantityVoided(Quantity quantityVoided) {
        this.quantityVoided = quantityVoided;
    }
}
