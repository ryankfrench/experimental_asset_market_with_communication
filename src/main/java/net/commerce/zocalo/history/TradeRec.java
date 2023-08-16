package net.commerce.zocalo.history;

import net.commerce.zocalo.user.SecureUser;

// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** TradeRec is the abstract base class for MarketTradeRec and BookTradeRec. */
public abstract class TradeRec extends PriceHistoryRecord {
    protected SecureUser taker;

    public TradeRec(SecureUser taker) {
        this.taker = taker;
    }
}
