package net.commerce.zocalo.orders;

// Copyright 2008-2010 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.ajax.events.BestAsk;
import net.commerce.zocalo.ajax.events.BestBid;
import net.commerce.zocalo.market.ReserveVerifier;
import net.commerce.zocalo.ajax.events.OrderRemoval;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.experiment.ScoreException;

/** An Order by a User to buy a quantity of an asset at a particular price.  */
public class Order implements Comparable {
    final static double epsilon = 0.000001;
    private Position position;
    private long id;
    private User owner;
    private Price price;
    private Quantity quantity;
    private Quantity quantityFulfilled;
    private Quantity quantityVoided;

    public Order(Position position, Price price, Quantity quantity, User owner) {
        this.position = position;
        this.owner = owner;
        this.quantity = quantity;
        this.price = price;
        owner.add(this);
        quantityFulfilled = Quantity.ZERO;
        quantityVoided = Quantity.ZERO;
    }

    /** @deprecated */
    Order() {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(quantity().printAsQuantity());
        buf.append("@");
        buf.append(naturalPrice().newScale(0));
        buf.append("(");
        buf.append(ownerName());
        buf.append(")");

        return buf.toString();
    }

    public int compareTo(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }

        Order other = (Order)o;
        double priceComparison = price.compareTo(other.price);
        if (priceComparison < 0) {
            return -1;
        } else if (priceComparison > 0) {
            return 1;
        } else {
            int quantityCompare = originalQuantity().compareTo(other.originalQuantity());
            if (quantityCompare != 0) {
                return quantityCompare;
            }
            return ownerName().compareTo(other.ownerName());
        }
    }

    /** @deprecated */
    void setQuantity(Quantity quantity) {
        this.quantity = quantity;
    }

    public void reduceQuantityDueToSale(Quantity reduction) {
        Quantity newQ = getQuantityFulfilled().plus(reduction);
        setQuantityFulfilled(newQ);
    }

    public void reduceQuantityDueToUnaffordability(Quantity reduction) {
        Quantity newQ = getQuantityVoided().plus(reduction);
        setQuantityVoided(newQ);
    }

    public Quantity quantity() {
        return originalQuantity().minus(getQuantityFulfilled().plus(getQuantityVoided()));
    }

    public Quantity originalQuantity() {
        return getQuantity();
    }
    public Price price() {
        return price;
    }

    public Position position() {
        return position;
    }

    static public int comparePrices(double onePrice, double otherPrice) {
        if (onePrice == otherPrice) {
            return 0;
        } else if (onePrice > otherPrice) {
            return 1;
        } else {
            return -1;
        }
    }

    static public int compareQuantities(double oneQuantity, double otherQuantity) {
        if (oneQuantity == otherQuantity) {
            return 0;
        } else if (oneQuantity < otherQuantity) {
            return -1;
        } else {
            return 1;
        }
    }

    public Funds provideFundsForTrade(Quantity quantity, Price price) {
        Quantity availableCash = owner.cashOnHand();
        Quantity desiredCash = price.times(quantity);
        if (availableCash.compareTo(desiredCash) < 0) {
            Quantity unavailableCash = desiredCash.minus(availableCash);
            reduceQuantityDueToUnaffordability(unavailableCash.div(price));

            desiredCash = availableCash;
        }
        return getOwner().getAccounts().provideCash(desiredCash);
    }

    public void withholdReserves(ReserveVerifier verifier, Quantity quantity, Position position) {
        if (null != verifier) {
            verifier.enforce(getOwner(), quantity, position);
        }
    }

    public void releaseReserves(Quantity quantity, Position position) {
        getOwner().releaseReserves(quantity,  position);
    }

    public void receive(Coupons coupons) {
        Quantity balance = coupons.getBalance();
        getOwner().getAccounts().addCoupons(coupons);
        reduceQuantityDueToSale(balance);
    }

    public boolean priceNotLessThan(Price v) {
        return price().compareTo(v) >= 0;
    }

    public Quantity requiredReserves(Market market) throws ScoreException {
        return market.requiredReserves(getOwner());
    }

    public void returnCash(Funds funds) {
        getOwner().receiveCash(funds);
    }

    public boolean userIsOwner(User user) {
        return owner == user;
    }

    /** convert balanced pairs (or sets) of coupons in position into cash.  */
    public void settleOffsettingPositions(Market market) {
        getOwner().settle(market);
    }

    public Price naturalPrice() {
        Claim claim = position.getClaim();

        return claim.naturalPrice(position, price);
    }

    public String ownerName() {
        return getOwner().getName();
    }

    public void removeFromOwner() {
        getOwner().remove(this);
    }

    public Quantity availableCoupons(Position position) {
        Quantity coupons = owner.couponCount(position);
        return coupons.min(quantity());
    }

    public Coupons provideCoupons(Position position, Quantity quantity) {
        return getOwner().getAccounts().provideCoupons(position, quantity);
    }

    public Quantity affordableQuantity(Quantity reservePrice, Quantity quantityDesired) {
        Quantity cash = owner.cashOnHand();
        Quantity affordable = quantity().min(cash.div(reservePrice)).min(quantityDesired);
        if (cash.compareTo(affordable.times(reservePrice)) < 0) {
            reduceQuantityDueToUnaffordability(quantity().minus(affordable));
        }
        return affordable;
    }

    /** @deprecated */
    Position getPosition() {
        return position;
    }

    /** @deprecated */
    void setPosition(Position position) {
        this.position = position;
    }

    /** @deprecated */
    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    User getOwner() {
        return owner;
    }

    /** @deprecated */
    Quantity getQuantity() {
        return quantity;
    }

    /** @deprecated */
    void setOwner(User owner) {
        this.owner = owner;
    }

    /** @deprecated */
    Price getPrice() {
        return price;
    }

    /** @deprecated */
    void setPrice(Price price) {
        this.price = price;
    }

    /** @deprecated */
    Quantity getQuantityFulfilled() {
        return quantityFulfilled;
    }

    /** @deprecated */
    void setQuantityFulfilled(Quantity quantityFulfilled) {
        this.quantityFulfilled = quantityFulfilled;
    }

    /** @deprecated */
    Quantity getQuantityVoided() {
        return quantityVoided;
    }

    /** @deprecated */
    void setQuantityVoided(Quantity quantityVoided) {
        this.quantityVoided = quantityVoided;
    }

    public BestBid makeBestBid() {
        return BestBid.newBest(getOwner().getName(), getPrice(), getQuantity(), position);
    }

    public BestAsk makeBestAsk() {
        return BestAsk.newBest(getOwner().getName(), getPrice(), getQuantity(), position);
    }

    public OrderRemoval makeRemovalRecord() {
        String ownerName = getOwner().getName();
        return OrderRemoval.newOrderRemoval(ownerName, getPrice(), getQuantity(), position, getQuantityFulfilled(), getQuantityVoided());
    }
}
