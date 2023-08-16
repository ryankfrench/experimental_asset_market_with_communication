package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Logger;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.ajax.dispatch.PriceActionAppender;

/** Action representing the fact that a user accepted a standing offer.  */
public abstract class Trade extends PriceAction {
    /** @deprecated
     *  TEST ONLY */
    public Trade(String owner, Price price, Quantity quantity, Position pos, Logger logger) {
        super(owner, price, quantity, pos, logger);
    }

    /** @deprecated */
    public Trade() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return "";  // Hibernate creates a default Trade object even though it's an abstract class.
                    // cglib doesn't like the abstract method. 
    }

    public void callBackPublish(PriceActionAppender appender) {
        appender.publishBidUpdate(this);
        appender.publishTradeEvent(this);
    }

    abstract public boolean isBuy();
}
