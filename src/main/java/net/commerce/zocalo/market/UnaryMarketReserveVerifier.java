package net.commerce.zocalo.market;

import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a callback object for use in experiments with Unary Markets
 requiring reserves for short holdings. */
public class UnaryMarketReserveVerifier extends ReserveVerifier {
    public UnaryMarketReserveVerifier(Session session) {
        super(session);
    }

    public Quantity costToSell(Price price, Quantity quantity, Quantity remainingDividend) {
        return (remainingDividend.minus(price)).times(quantity);
    }
}
