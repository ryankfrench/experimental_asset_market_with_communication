package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.ajax.events.LimitTrade;
import net.commerce.zocalo.ajax.events.BookTrade;

import java.util.Dictionary;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a Market in a multi-position claim; doesn't currently support book orders,
    always has a marketMaker.  Responsibilities include reporting on price
    levels as well as buying claims. */
public class MultiMarket extends Market {
    private Claim claim = null;
    private MultiMarketMaker maker;

    public MultiMarket(CouponBank mint, SecureUser owner, MultiClaim claim) {
        super(mint, owner, Price.ONE_DOLLAR);
        this.claim = claim;
    }

    /** @deprecated */
    public MultiMarket() {
    }

    public MultiMarketMaker makeMarketMaker(Quantity endowment, User owner) {
        if (maker == null) {
            maker = new MultiMarketMaker(this, endowment, owner);
            if (maker.cashInAccount().compareTo(endowment) != 0) {
                maker = null;
                return null;
            }
            HibernateUtil.save(maker);
        }
        return maker;
    }

    public Price maxPrice() {
        return Price.ONE_DOLLAR;
    }

    public static MultiMarket make(SecureUser owner, MultiClaim claim, Funds empty) {
        CouponBank couponMint = CouponBank.makeBank(claim, empty);
        return new MultiMarket(couponMint, owner, claim);
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }

    public MultiClaim getMultiClaim() {
        return (MultiClaim) getClaim();
    }

    public void setMaker(MarketMaker mm) {
        this.maker = (MultiMarketMaker) mm;
    }

    public void buyOrAddBookOrder(Position pos, Price price, Quantity quantity, User user) {
        buyAtMarketPrice(pos, price, quantity, user);  // No book orders for MultiMarkets yet
    }

    public void buyAtMarketPrice(Position pos, Price price, Quantity quantity, User user) {
        if (gateKeeper(pos, price, quantity, user, true)) {
            return;
        }

        Quantity quantityPurchased = buyFromMarketMaker(pos, price, quantity, user);
        if (! quantityPurchased.isZero()) {
            updateLastTraded();
            if (requireReserves()) {
                withholdReserves(user, quantityPurchased, pos);
            }
        }

        if (quantityPurchased.isZero()) {
            user.warn("No transaction was recorded.");
        }

        return;
    }

    public void recordLimitTrade(Order order, Quantity quantityTraded, Position position) {
        LimitTrade.newLimitTrade(order.ownerName(), order.price(), quantityTraded, position); // TODO SCALE?
    }

    public void recordBookTrade(User acceptor, Price price, Quantity quantity, Position position) {
        BookTrade.newBookTrade(acceptor.getName(), price, quantity, position);
        acceptor.reportBookPurchase(quantity, position);
    }

    public Quantity buyWithCostLimit(Position position, Price price, Quantity costLimit, User user, boolean isBuy) {
        if (getMarketClosed()) {
            warnMarketClosedCostLimit(position, price, costLimit, user);
            return Quantity.ZERO;
        }
        Quantity quantityPurchased = getMaker().buyWithCostLimit(position, price.asProbability(), costLimit, user);
        if (! quantityPurchased.isZero()) {
            updateLastTraded();
        }
        return quantityPurchased;
    }

    private Quantity buyFromMarketMaker(Position position, Price price, Quantity quantity, User user) {
        if (getMarketClosed()) {
            warnMarketClosed(position, price, quantity, user);
            return Quantity.ZERO;
        }
        Quantity quantityPurchased = getMaker().buyOrSellUpToQuantity(position, price, quantity, user);
        if (! quantityPurchased.isZero()) {
            updateLastTraded();
        }
        return quantityPurchased;
    }

    MarketMaker getMaker() {
        return maker;
    }

    public Dictionary<Position, Probability> finalProbs() {
        return getMaker().currentProbabilities(null);
    }

    public void marketCallBack(MarketCallback callback) {
        callback.multiMarket(this);
    }
}
