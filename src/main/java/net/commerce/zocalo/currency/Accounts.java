package net.commerce.zocalo.currency;

// Copyright 2007-2009 Chris Hibbert.  Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.market.Market;

import java.util.*;

/** holds assets of value for a user.  Includes both cash and claim positions.
    positions indexed by Positions objects.  Holding this object provides authority
    to spend money and dispose of assets.  Must be closely held.  */
public class Accounts {
    private Long id;
    private Map<Position, Coupons> positions;
    private Funds cash;

    public Accounts(Funds initialDeposit) {
        positions(new HashMap<Position, Coupons>());
        cash(initialDeposit);
    }

    /** @deprecated */
    Accounts() {
    }

////// QUERIES ///////////////////////////////////////////////////////////////

    public Quantity cashValue() {
        return cash().getBalance();
    }

    public boolean cashSameOrGreaterThan(Quantity amount) {
        return cash().getBalance().compareTo((Quantity)amount) >= 0;
    }

    public boolean neglibleCash() {
        return cash().negligible();
    }

    public Quantity couponCount(Position position) {
        Coupons coupons = getCouponsForPosition(position);
        if (coupons == null || coupons.negligible()) {
            return Quantity.ZERO;
        }
        return coupons.getBalance();
    }

    private Coupons getCouponsForPosition(Position position) {
        return (Coupons)positions().get(position);
    }

    public Quantity countBalancedSets(Claim claim) {
        Quantity minHolding = null;
        for (int i = 0; i < claim.positions().length; i++) {
            Position position = claim.positions()[i];
            Quantity balance = couponCount(position);
            if (balance.isZero()) {
                return Quantity.ZERO;
            }
            if (minHolding == null) {
                minHolding = balance;
            } else {
                minHolding = minHolding.min(balance);
            }
        }
        return minHolding;
    }

//// TRANSFERRING VALUE //////////////////////////////////////////////////////

    public void receiveCash(Funds payment) {
        payment.transfer(payment.getBalance(), cash());
    }

    public Funds provideCash(Quantity amount) {
        Funds result = cash().provide(amount);
        if (result.getBalance().compareTo(amount) == 0) {
            return result;
        } else {
            result.transfer(result.getBalance(), cash());
            return result;
        }
    }

    public void addCoupons(Coupons coupons) {
        if (coupons == null) { return; }

        Position position = coupons.getPosition();
        Coupons holdings = getCouponsForPosition(position);
        if (holdings == null) {
            holdings = coupons.makeEmpty();
            HibernateUtil.save(holdings);
            positions().put(position, holdings);
        }
        coupons.transfer(coupons.getBalance(), holdings);
    }

    public void addCoupons(Set couponsSet) {
        if (couponsSet == null) { return; }
        for (Iterator iterator = couponsSet.iterator(); iterator.hasNext();) {
            Coupons coupons = (Coupons) iterator.next();
            addCoupons(coupons);
        }
    }

    public void addAll(Coupons[] couponsArray) {
        for (int i = 0; i < couponsArray.length; i++) {
            Coupons coupons = couponsArray[i];
            addCoupons(coupons);
        }
    }

    public Coupons provideCoupons(Position position, Quantity amount) {
        Coupons holdings = getCouponsForPosition(position);
        if (holdings == null || holdings.getBalance().compareTo(amount) < 0) {
            return null;
        }
        return holdings.provide(amount);
    }

    public Set provideAllCoupons(Position pos) {
        Set<Coupons> couponsSet = new HashSet<Coupons>();
        Position[] allPositions = pos.getClaim().positions();
        for (int i = 0; i < allPositions.length; i++) {
            Position position = allPositions[i];
            Coupons coupons = getCouponsForPosition(position);
            if (coupons != null) {
                couponsSet.add(coupons);
            }
        }
        return couponsSet;
    }

    public void settle(Market market) {
        Quantity quantity = countBalancedSets(market.getClaim());
        if (quantity.isZero()) {
            return;
        }
        Map<Position, Coupons> couponsMap = provideBalancedSets(market.getClaim(), quantity);
        Accounts change = market.settle(quantity, couponsMap);
        addAll(change);
    }

    private Map<Position, Coupons> provideBalancedSets(Claim claim, Quantity quantity) {
        Map<Position, Coupons> couponsMap = new HashMap<Position, Coupons>();
        Position[] positions = claim.positions();
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            couponsMap.put(position, provideCoupons(position, quantity));
        }
        return couponsMap;
    }


//// DISPLAY /////////////////////////////////////////////////////////////////

    public void display(StringBuffer buf, Claim restrictToClaim, PositionDisplayAdaptor printer) {
        if (positions().isEmpty()) {
            printer.printEmpty(buf);
            return;
        }

        printer.printHeader(buf);
        Collection<Position> keys = getPositionKeys();

        TreeSet<Position> sortedKeys = new TreeSet<Position>(sorterByName());
        sortedKeys.addAll(keys);
        for (Iterator iter = sortedKeys.iterator(); iter.hasNext();) {
            Position position = (Position) iter.next();
            if (couponCount(position).isZero()) {
                continue;
            }
            if (restrictToClaim == null || restrictToClaim == position.getClaim()) {
                printer.printRow(buf, position, couponCount(position));
            }
        }
        printer.afterTable(buf);
    }

    private Comparator<Position> sorterByName() {
        return new Comparator<Position>() {
            public int compare(Position one, Position another) {
                if (one.getClaim().getName().equals(another.getClaim().getName())) {
                    if (one.getName().equals(another.getName())) {
                        return 0;
                    } else {
                        return one.getName().compareTo(another.getName());
                    }
                } else {
                    return one.getClaim().getName().compareTo(another.getClaim().getName());
                }
            }
        };
    }

    public Quantity couponCount(Claim claim) {
        Position[] positions = claim.positions();
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Coupons coupons = getCouponsForPosition(position);
            if (coupons != null) {
                Quantity balance = coupons.getBalance();
                if (! balance.isNegligible() && balance.isPositive()) {
                    if (position.isInvertedPosition()) {
                        return balance.negate();
                    } else {
                        return balance;
                    }
                }
            }
        }
        return Quantity.ZERO;
    }

    public Quantity minCouponsVersus(Position position) {
        Quantity min = null;
        Position[] positions = position.getClaim().positions();
        for (int i = 0; i < positions.length; i++) {
            Position p = positions[i];
            if (!p.equals(position)) {
                Coupons coupons = getCouponsForPosition(p);
                if (coupons == null) {
                    min = Quantity.ZERO;
                } else {
                    Quantity balance = coupons.getBalance();
                    if (min == null) {
                        min = balance;
                    } else {
                        min = balance.min(min);
                    }
                }
            }
        }
        return min;
    }

    public void addAll(Accounts contribution) {
        for (Iterator iterator = contribution.getPositionKeys().iterator(); iterator.hasNext();) {
            Position position = (Position) iterator.next();
            Coupons donor = contribution.getCouponsForPosition(position);

            addCoupons(contribution.provideCoupons(position, donor.getBalance()));
        }
        contribution.cash().transfer(cash());
    }

    public Collection<Position> getPositionKeys() {
        return positions().keySet();
    }

    private Map<Position, Coupons> positions() {
        return getPositions();
    }

    /** @deprecated */
    Map<Position, Coupons> getPositions() {
        return positions;
    }

    private void positions(Map<Position, Coupons> positions) {
        setPositions(positions);
    }

    /** @deprecated */
    void setPositions(Map<Position, Coupons> positions) {
        this.positions = positions;
    }

    private Funds cash() {
        return getCash();
    }

    /** @deprecated */
    Funds getCash() {
        return cash;
    }

    private void cash(Funds f) {
        setCash(f);
    }

    /** @deprecated */
    void setCash(Funds f) {
        cash = f;
    }

    /** @deprecated */
    Long getId() {
        return id;
    }

    /** @deprecated */
    void setId(Long id) {
        this.id = id;
    }

    public Set<Coupons> provideCouponSets(Position position, Quantity quantity, boolean complement) {
        Set<Position> positions;
        if (complement) {
            positions = new HashSet<Position>();
            positions.add(position);
        } else {
            positions = position.complement();
        }

        Set<Coupons> couponses = new HashSet<Coupons>();
        for (Iterator<Position> iter = positions.iterator(); iter.hasNext();) {
            Position pos = iter.next();
            couponses.add(provideCoupons(pos, quantity));
        }
        return couponses;
    }

    public interface PositionDisplayAdaptor {
        public void printEmpty(StringBuffer buf);
        public void printHeader(StringBuffer buf);
        public void printRow(StringBuffer buf, Position pos, Quantity count);
        public void afterTable(StringBuffer buf);
    }
}
