package net.commerce.zocalo.market;

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.currency.Currency;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** In Experiments in which holders of short shares must pay dividends, this callback interface
     allows Markets to defer to Sessions for enforcement of the proper balance of reserves. */
public abstract class ReserveVerifier {
    private final Session session;
    private Market market;

    protected ReserveVerifier(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public Market getMarket() {
        return market;
    }

    public abstract Quantity costToSell(Price price, Quantity quantity, Quantity remainingDividend);

    public void addMarket(Market market) {
        this.market = market;
    }

    public boolean verify(User user, Position pos, Price price, Quantity quantity) {
        TradingSubject trader = getSession().getTrader(user);
        if (! pos.isInvertedPosition()) {
            return true;
        }
        if (trader == null) { return false; }
        Quantity remainingDividend = Quantity.ZERO;
        try {
            remainingDividend = getSession().getRemainingDividend(trader, getSession().getCurrentRound());
        } catch (ScoreException e) {
            Session.sessionLogger().warn("Attempting to verify reserves for " + user.getName() + " but got a score exception.");
            return false;
        }
        Quantity assets = user.couponCount(pos.opposite());
        Quantity assetValue = getMarket().maxPrice().times(assets);
        boolean approve = user.cashSameOrGreaterThan(costToSell(price, quantity, remainingDividend).minus(assetValue));
        if (!approve) {
            user.warn("You don't have enough reserves for that purchase.");
            return false;
        }
        return true;
    }

    public void enforce(User user, Quantity quantityPurchased, Position pos) {
        if (! market.requireReserves()) { return; }

        TradingSubject trader = session.getTrader(user);
        if (trader == null) { return; }

        Quantity remaining = Quantity.ZERO;
        try {
            remaining = session.getRemainingDividend(trader, session.getCurrentRound());
        } catch (ScoreException e) {
            Session.sessionLogger().warn("Attempting to enforce reserves for " + user.getName() + " but got a score exception.");
        }

        Position reservePosition =
                pos.isInvertedPosition()
                        ? pos
                        : pos.opposite();

        Quantity requiredReserveQ = user.couponCount(pos.getClaim()).negate().max(Quantity.ZERO);

        Quantity reserveQ = user.reserveBalance(reservePosition).div(remaining);
        if (reserveQ.equals(requiredReserveQ)) { return; }

        if (reserveQ.compareTo(requiredReserveQ) > 0) {
            user.releaseReserves(reserveQ.minus(requiredReserveQ).max(Quantity.ZERO), reservePosition);
        } else {
            user.reserveFunds(remaining, requiredReserveQ.minus(reserveQ), reservePosition);
        }
    }

    public Quantity requiredReserves(User user) throws ScoreException {
        TradingSubject trader = session.getTrader(user);
        return session.getRemainingDividend(trader, session.getCurrentRound());
    }
}
