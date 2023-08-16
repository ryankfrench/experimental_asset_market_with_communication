package net.commerce.zocalo.user;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.currency.Currency;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.hibernate.HibernateUtil;

import java.util.*;

import org.apache.log4j.Logger;

/** The agent who trades in the market.  Users have accounts, which track their assets, which
    consist of funds and coupons. */
public class User extends Warnable {
    private String name;
    private long id;
    private Accounts accounts;
    private Set allOrders = new HashSet();
    // TODO: Once persistent, orders should be retrieved from DB rather than being owned by User
    private String hint;
    private Map<Position, Funds> reserveHoldings = new HashMap<Position, Funds>();
    private Map<Position, Quantity> reservesPerShare = new HashMap<Position, Quantity>();

    public User(String name, Funds funds) {
        this.name = name;
        accounts = new Accounts(funds);
        allOrders = new HashSet();
    }

    /** @deprecated */
    User() {
    }

    public Quantity cashOnHand() {
        return getAccounts().cashValue();
    }

    public Accounts getAccounts() {
        return accounts;
    }

    public String getName() {
        return name;
    }

    public void receiveCash(Funds payment) {
        getAccounts().receiveCash(payment);
    }

    public void endow(Coupons balance) {
        getAccounts().addCoupons(balance);
    }

    public String toString() {
        return "User(" + getName() + ")";
    }

    public boolean cashSameOrGreaterThan(Quantity amount) {
        return getAccounts().cashSameOrGreaterThan(amount);
    }

    public boolean negligibleCashOnHand() {
        return getAccounts().neglibleCash();
    }

    public Collection<Position> claimsWithAssets() {
        return getAccounts().getPositionKeys();
    }

    public void displayAccounts(StringBuffer buf, Accounts.PositionDisplayAdaptor printer) {
        getAccounts().display(buf, null, printer);
    }

    public void displayMultiMarketAccounts(StringBuffer buf, Claim claim, Accounts.PositionDisplayAdaptor printer) {
        getAccounts().display(buf, claim, printer);
    }

    public void displayAccounts(StringBuffer buf, Claim claim, Accounts.PositionDisplayAdaptor printer) {
        getAccounts().display(buf, claim, printer);
    }

    public void settle(Market market) {
        getAccounts().settle(market);
    }

    public void add(Order order) {
        allOrders.add(order);
    }

    public List getOrders() {
        List ordersList = new ArrayList();
        if (allOrders.isEmpty()) {
            allOrders.addAll(HibernateUtil.getOrdersForUser(this));
        }
        ordersList.addAll(allOrders);
        return ordersList;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public void remove(Order order) {
        allOrders.remove(order);
    }

    public Quantity outstandingOrderCost(Position pos) {
        Quantity total = Quantity.ZERO;
        for (Iterator iterator = getOrders().iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            if (order.quantity().isPositive() && order.position() == pos) {
                total = total.plus(order.price());
            }
        }
        return total;
    }

    public Quantity outstandingAskQuantity(Position pos) {
        Quantity total = Quantity.ZERO;
        for (Iterator iterator = getOrders().iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            if (order.quantity().isPositive() && order.position() == pos) {
                total = total.plus(order.quantity());
            }
        }
        return total;
    }

    public void resetOutstandingOrders() {
        allOrders = new HashSet();
    }

    public boolean canAfford(Position pos, Quantity quantity, Price price, Quantity outstandingOrderCost) {
        Quantity couponsAvailable = getAccounts().minCouponsVersus(pos).max(Quantity.ZERO);
        return couponsAvailable.plus((cashOnHand().minus(outstandingOrderCost)).div(price)).compareTo(quantity) >= 0;
    }

    public Funds provideCash(Quantity amount) {
        return getAccounts().provideCash(amount);
    }

    public Quantity couponCount(Claim claim) {
        return getAccounts().couponCount(claim);
    }

    public Quantity couponCount(Position position) {
        return getAccounts().couponCount(position);
    }

    public boolean useCostLimitUI() {
        return false;
    }

    /** @deprecated */
    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    /** @deprecated */
    void setAccounts(Accounts accounts) {
        this.accounts = accounts;
    }

    /** @deprecated */
    void setOrders(Set orders) {
        this.allOrders = orders;
    }

    /** @deprecated */
    void setName(String name) {
        this.name = name;
    }

    public void reportBookPurchase(Quantity quant, Position pos) {
        // No behavior
    }

    public void reportMarketMakerPurchase(Quantity quant, Position pos, boolean isBuy) {
        // No behavior
    }

    public Quantity minCouponsVersus(Position pos) {
        return getAccounts().minCouponsVersus(pos);
    }

    public boolean reserveFunds(Quantity amount, Quantity quantity, Position pos) {
        Quantity total = amount.times(quantity);
        if (!cashSameOrGreaterThan(total)) {
            Logger logger = Logger.getLogger("Score");
            logger.warn("User '" + getName() + "' couldn't afford to set aside reserves: " + total);
            return false;
        }

        Funds holdings = reserveHoldings.get(pos);
        Quantity perShare = reservesPerShare.get(pos);
        if (holdings == null || holdings.getBalance().isZero()) {
            reserveHoldings.put(pos, provideCash(total));
            reservesPerShare.put(pos, amount);
            return true;
        } else if (perShare.equals(amount)) {
            provideCash(total).transfer(total, holdings);
            return true;
        } else {
            return false;
        }
    }

    public Quantity reserveBalance(Position pos) {
        Funds rsv = reserveHoldings.get(pos);
        if (rsv == null) {
            return Quantity.ZERO;
        }

        return rsv.getBalance();
    }

    /** we covered the position for TOTAL shares, so the reserves are no longer needed */
    public Quantity releaseReserves(Quantity total, Position pos) {
        Funds holdings = reserveHoldings.get(pos);
        Quantity value = Quantity.ZERO;
        if (holdings != null) {
            Funds payment = holdings.provide(reservesPerShare.get(pos).times(total));
            value = payment.getBalance();
            receiveCash(payment);
            if (holdings.getBalance().isZero()) {
                reservesPerShare.put(pos, Quantity.ZERO);
            }
        }
        return value;
    }

    public void reduceReservesTo(Quantity perShare, Position pos) {
        if (reservesPerShare.containsKey(pos)) {
            Funds funds = reserveHoldings.get(pos);
            Quantity holdings = reservesPerShare.get(pos);
            reservesPerShare.put(pos, perShare);
            Quantity amount;
            if (holdings.isZero()) {
                amount = Quantity.ZERO;
            } else {
                amount = (Quantity.ONE.minus(perShare.div(holdings))).times(funds.getBalance());
            }
            receiveCash(funds.provide(amount));
        }
    }

    /** we're paying dividends of PERSHAREDIVIDEND on SHARES shares */
    public Funds releaseDividends(Quantity shares, Quantity perShareDividend, Position pos) {
        Funds holdings = reserveHoldings.get(pos);
        Quantity perShare = reservesPerShare.get(pos);
        if (holdings == null || holdings.getBalance().compareTo(shares.times(perShareDividend)) < 0
                || perShare.compareTo(perShareDividend) < 0) {
            return null;
        } else {
            reservesPerShare.put(pos, perShare.minus(perShareDividend));
            return holdings.provide(perShareDividend.times(shares));
        }
    }
}
