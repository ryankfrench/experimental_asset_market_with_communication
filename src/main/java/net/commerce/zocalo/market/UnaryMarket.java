package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.ajax.events.BookTrade;
import net.commerce.zocalo.ajax.events.LimitTrade;
import net.commerce.zocalo.orders.Order;

import java.util.Properties;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a Market in a claim with only one position; only supports book orders.
    If short selling is not allowed, then coupons can only be sold by holders.
    If short selling is allowed, then sellers receive money rather than paying,
    but they must be able to pay dividends.  Responsibilities include
    reporting on price levels and book contents as well as buying claims. */
public class UnaryMarket extends BinaryMarket {

/////////// FACTORY //////////////////////////////////////////////////////////
    private UnaryMarket(CouponBank bank, SecureUser owner, BinaryClaim claim, Quantity maxPrice) {
        super(bank, owner, claim, maxPrice, 2);
        setBook(new UnaryBook(claim, this));
    }

    static public UnaryMarket make(SecureUser owner, BinaryClaim claim, Funds empty, ReserveVerifier v) {
        return make(claim, owner, empty, Quantity.Q100, v);
    }

    public static UnaryMarket make(BinaryClaim claim, SecureUser owner, Funds empty, Quantity maxPrice, ReserveVerifier v) {
        CouponBank couponMint = CouponBank.makeBank(claim, empty);
        UnaryMarket market = new UnaryMarket(couponMint, owner, claim, maxPrice);
        HibernateUtil.save(market);
        HibernateUtil.save(market.getBook());
        if (v != null) {
            market.setReserveVerifier(v);
        }
        return market;
    }

    /** This factory method allows the owner to create unbalanced pairs of coupons. */
    static public UnaryMarket make(BinaryClaim claim, CouponBank couponMint, SecureUser owner, Properties props, ReserveVerifier verifier) {
        Quantity maxTradePrice = PropertyHelper.getMaxTradingPrice(props);
        UnaryMarket market = new UnaryMarket(couponMint, owner, claim, maxTradePrice);
        HibernateUtil.save(market);
        HibernateUtil.save(market.getBook());
        market.setPriceBetteringRequired(PropertyHelper.getBetterPriceRequired(props));
        market.setWholeShareTradingOnly(PropertyHelper.getWholeShareTradingOnly(props));
        if (verifier != null) {
            market.setReserveVerifier(verifier);
        }
        return market;
    }

    public MarketMaker makeMarketMaker(SecureUser owner, double endowment) {
        throw new UnsupportedOperationException("UnaryMarkets don't support market makers.");
    }

    /** @deprecated */
    UnaryMarket() {
    }

//// EXCHANGE ////////////////////////////////////////////////////////////////

    protected Quantity purchaseAtMarketPrice(Position pos, Price price, Quantity quantity, User user) {
        Quantity quantityPurchased;
        quantityPurchased = getBook().buyFromBookOrders(pos, price, quantity, user);
        postPurchaseActions(pos, user, quantityPurchased);
        return quantityPurchased;
    }

    protected boolean gateKeeper(Position pos, Price price, Quantity quantity, User user, boolean marketOrder) {
        if (super.gateKeeper(pos, price, quantity, user, marketOrder)) {
            return true;
        }
        if (! requireReserves() && unaryAssetViolation(pos, price, quantity, user)) {
            return true;
        }
        return false;
    }

    public void marketCallBack(MarketCallback callback) {
        callback.unaryMarket(this);
    }

    public Quantity buyWithCostLimit(Position pos, Price price, Quantity costLimit, User user) {
        Quantity startingCash = user.cashOnHand();
        Quantity actualLimit = costLimit.min(startingCash);
        Quantity availableCash = actualLimit;
        Quantity cumulativeShares = Quantity.ZERO;

        while (availableCash.isPositive()) {
            Quantity shares;
            Price bestPrice = getBook().bestSellOfferFor(pos);
            if (bestPrice.equals(Price.ONE_DOLLAR)) {
                break;
            }
            shares = getBook().buyFromBestBookOrders(pos, bestPrice, availableCash.div(bestPrice), user);
            cumulativeShares = cumulativeShares.plus(shares);
            Quantity spent = startingCash.min(user.cashOnHand());
            availableCash = actualLimit.minus(spent);
        }

        if (! cumulativeShares.isNegligible()) {
            updateLastTraded();
        }
        return cumulativeShares;
    }

    private boolean unaryAssetViolation(Position pos, Price price, Quantity quantity, User user) {
        if (!pos.isInvertedPosition()) {
            if (user.outstandingOrderCost(pos).plus(price).compareTo(user.cashOnHand()) > 0) {
                UnaryMarket.warnCantAfford(user);
                return true;
            }
        } else {
            Quantity assets = user.couponCount(getBinaryClaim().getYesPosition());

            if (user.outstandingAskQuantity(pos).plus(quantity).compareTo(assets) > 0) {
                UnaryMarket.warnInsufficientAssets(assets, user);
                return true;
            }
        }
        return false;
    }

    public void recordLimitTrade(Order order, Quantity quantityTraded, Position opposite) {
        Price price = order.price();
        String userName = order.ownerName();
        if (opposite.isInvertedPosition()) {
            LimitTrade.newLimitTrade(userName, price, quantityTraded.negate(), opposite.opposite());
        } else {
            LimitTrade.newLimitTrade(userName, price.inverted(), quantityTraded, opposite);
        }
    }

    public void recordBookTrade(User acceptor, Price price, Quantity quantity, Position position) {
        if (quantity.isZero()) { return; }

        if (position.isInvertedPosition()) {
            BookTrade.newBookTrade(acceptor.getName(), price, quantity.negate(), position.opposite());
            acceptor.reportBookPurchase(quantity.negate(), position.opposite());
        } else {
            BookTrade.newBookTrade(acceptor.getName(), price, quantity, position);
            acceptor.reportBookPurchase(quantity, position);
        }
    }

    public Quantity decideClaimAndRecord(Position pos) {
        close();
        final List ownersRedundant = HibernateUtil.couponOwners(pos.getClaim());
        Set owners = new HashSet(ownersRedundant);
        MarketMaker maker = getMaker();
        Quantity couponsRedeemed = getUnaryCouponMint().redeem(pos, owners);
        if (null != maker) {
            Quantity refundAmount = maker.cashInAccount();
            getOwner().receiveCash(maker.provideCash(refundAmount));
            getOwner().warn(refundAmount + " was refunded at Market close.");
        }
        setOutcome(Outcome.newSingle(pos));
        return couponsRedeemed;
    }

    UnaryCouponBank getUnaryCouponMint() {
        return (UnaryCouponBank) getCouponMint();
    }
}
