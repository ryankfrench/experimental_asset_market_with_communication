package net.commerce.zocalo.currency;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.ajax.events.Redemption;
import net.commerce.zocalo.market.MarketMaker;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.logging.GID;

import java.util.*;

import org.apache.log4j.Logger;

/** CouponBank is an issuer of Coupons for a particular claim.  It issues a
 non-forgeable currency, and is the only holder of the token that allows
 creation of new Coupons.  Anyone who has access to the bank can
 instruct the bank to issue coupon pairs or individual coupons.  The pairs
 must be paid for, so that interface doesn't need to be closely held, but
 the ability to create unpaired coupons must be treated as a closely held
 capability. */
public class CouponBank {
    private Map<Position, CurrencyToken> tokens;
    private Quantity setsMinted;
    private Claim claim;
    private long id;
    private Quantity setsRedeemed;
    private Funds cash;

////// FACTORY ///////////////////////////////////////////////////////////////
    CouponBank(Claim claim, Funds empty) {
        this.claim = claim;
        cash = empty.makeEmpty();
        setsMinted = Quantity.ZERO;
        setsRedeemed = Quantity.ZERO;
        tokens = new HashMap<Position, CurrencyToken>();
        for (int i = 0; i < claim.positions().length; i++) {
            Position position = claim.positions()[i];
            CurrencyToken token = new CurrencyToken(position.getName());
            HibernateUtil.save(token);
            tokens.put(position, token);
        }
    }

    /** @deprecated */
    CouponBank() {
    }

    static public CouponBank makeBank(Claim claim, Funds empty) {
        return new CouponBank(claim, empty);
    }

////// ACCESSORS ///////////////////////////////////////////////////////////////

    protected void incrementSetsMinted(Quantity amount) {
        Quantity minted = getSetsMinted().plus(amount);
        setSetsMinted(minted);
    }

    protected void incrementSetsRedeemed(Quantity amount) {
        Quantity redeemed = getSetsRedeemed().plus(amount);
        setSetsRedeemed(redeemed);
    }

    public Quantity getSetsMinted() {
        return setsMinted;
    }

    void setSetsMinted(Quantity setsMinted) {
        this.setsMinted = setsMinted;
    }

    public Quantity getSetsRedeemed() {
        return setsRedeemed;
    }

    void setSetsRedeemed(Quantity setsRedeemed) {
        this.setsRedeemed = setsRedeemed;
    }

    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    Map<Position, CurrencyToken> getTokens() {
        return tokens;
    }

    void setTokens(Map<Position, CurrencyToken> tokens) {
        this.tokens = tokens;
    }

    Claim getClaim() {
        return claim;
    }

    void setClaim(Claim claim) {
        this.claim = claim;
    }

    Funds getCash() {
        return cash;
    }

    void setCash(Funds cash) {
        this.cash = cash;
    }

    CurrencyToken getToken(Position position) {
        return (CurrencyToken) tokens.get(position);
    }

    public Quantity balance() {
        return cash.getBalance();
    }

////// ISSUING AND REDEEMING COUPONS /////////////////////////////////////////
    public Coupons[] printNewCouponSets(Quantity amount, Funds funds, Quantity couponCost) {
        Position[] positions = getClaim().positions();
        Coupons[] couponSets = new Coupons[positions.length];
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            couponSets[i] = Coupons.newPosition(getToken(position), amount, position);
        }
        incrementSetsMinted(amount);

        Quantity transferred = funds.transfer(amount.times(couponCost), this.cash);
        if (transferred.compareTo(amount) < 0) {
            Logger logger = Logger.getLogger(CouponBank.class);
            logger.warn("CouponBank received " + transferred + " instead of " + amount + " in printNewCouponSets().");
        }
        return couponSets;
    }

    /**  couponsMap should contain QUANTITY of each coupon in this claim.  If it
     contains fewer, the owner will only get credit for the full sets.  Any odd
     coupons will be returned.  */
    public Accounts settle(Quantity quantity, Map couponsMap, Quantity couponCost) {
        Quantity minCoupons = quantity;
        Accounts deposits = new Accounts(getCash().makeEmpty());
        Accounts change = new Accounts(getCash().makeEmpty());
        Position[] positions = getClaim().positions();
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Coupons coupons = (Coupons)couponsMap.get(position);
            Quantity balance = coupons.getBalance();
            deposits.addCoupons(coupons);
            minCoupons = balance.min(minCoupons);
        }
        if (minCoupons.compareTo(quantity) < 0) {
            for (int i = 0; i < positions.length; i++) {
                Position position = positions[i];
                Coupons coupons = (Coupons)couponsMap.get(position);
                Coupons chg = coupons.provide(coupons.getBalance().minus(minCoupons));
                change.addCoupons(chg);
            }
        }
        incrementSetsRedeemed(minCoupons);
        Funds redemptions = getCash().provide(minCoupons.times(couponCost));
        change.receiveCash(redemptions);
        return change;
    }

    public Coupons issueUnpairedCoupons(Quantity quantity, Position pos) {
        return Coupons.newPosition(getToken(pos), quantity, pos);
    }

    public Quantity redeem(Position pos, Set users, Market market, MarketMaker maker) {
        Map<Position, Quantity> redemptions = new HashMap<Position, Quantity>();
        Price couponPrice = market.maxPrice();
        for (Iterator userIter = users.iterator(); userIter.hasNext();) {
            User owner = (User) userIter.next();
            Set couponsSet = owner.getAccounts().provideAllCoupons(pos);
            for (Iterator couponIter = couponsSet.iterator(); couponIter.hasNext();) {
                Coupons coupons = (Coupons) couponIter.next();
                Quantity balance = coupons.getBalance();
                Coupons couponsToDiscard = coupons.makeEmpty();
                Quantity transferred = coupons.transfer(couponsToDiscard);
                if (transferred.isZero() && ! coupons.negligible()) {
                    Logger log = Logger.getLogger(CouponBank.class);
                    String bal = balance.printAsDetailedQuantity();
                    log.error("Unable to redeem " + bal + " coupons for " + owner.getName());
                }
                Position position = coupons.getPosition();
                if (position.equals(pos)) {
                    owner.receiveCash(getCash().provide(balance.times(couponPrice)));
                    increment(redemptions, pos, balance);
                    Redemption.newRedemption(owner.getName(), couponPrice, balance, pos);
                } else {
                    increment(redemptions, position, balance);
                    Redemption.newRedemption(owner.getName(), couponPrice.inverted(), balance, position);
                }
            }
        }

        if (maker != null) {
            Accounts makerAcct = maker.redeem(this);
            Set winners = makerAcct.provideAllCoupons(pos);
            for (Iterator iterator = winners.iterator(); iterator.hasNext();) {
                Coupons coupons = (Coupons) iterator.next();
                Position position = coupons.getPosition();
                Quantity balance = coupons.getBalance();
                if (position.equals(pos)) {
                    Quantity amount = balance.times(couponPrice);
                    Funds payment = getCash().provide(amount);
                    makerAcct.receiveCash(payment);
                    Redemption.newRedemption("Market Maker", couponPrice, balance, position);
                } else {
                    Quantity amount = balance.times(couponPrice); // HACK: tickle balance so Hibernate pages it in.
                    Redemption.newRedemption("Market Maker", couponPrice.inverted(), balance, position);
                }
                increment(redemptions, position, balance);
            }
        }

        validateBalancedRedemptions(redemptions, pos);
        return getSetsRedeemed();
    }

    void validateBalancedRedemptions(Map<Position, Quantity> redemptions, Position pos) {
        Logger log = Logger.getLogger(CouponBank.class);
        Quantity redemptionLevel = lookupRedemptions(redemptions, pos);
        boolean balanced = true;

        for (Iterator iterator = redemptions.keySet().iterator(); iterator.hasNext();) {
            Position position =  (Position)iterator.next();
            if (! redemptionLevel.minus(lookupRedemptions(redemptions, position)).isNegligible()) {
                balanced = false;
            }
        }
        if (balanced) {
            incrementSetsRedeemed(redemptionLevel);
            log.info(GID.log() + "Redeemed " + redemptionLevel + " '" + pos.getName() + "' Coupons for " + pos.getClaim().getName());
        } else {
            StringBuffer buf = new StringBuffer();
            for (Iterator iterator = redemptions.keySet().iterator(); iterator.hasNext();) {
                Position position =  (Position)iterator.next();
                buf.append(position.getName() + "(" + lookupRedemptions(redemptions, position)+ ")");
                if (iterator.hasNext()) {
                    buf.append(", ");
                }
            }

            log.warn(GID.log() + "Redeemed mismatched Coupons for " + pos.getClaim().getName() + ":" + buf.toString());
        }
    }

    protected void increment(Map<Position, Quantity> redemptions, Position position, Quantity balance) {
        Quantity newBalance = balance.plus(lookupRedemptions(redemptions, position));
        redemptions.put(position, newBalance);
    }

    private Quantity lookupRedemptions(Map<Position, Quantity> redemptions, Position position) {
        Quantity value = redemptions.get(position);
        if (value == null) {
            return Quantity.ZERO;
        }
        return value;
    }

    public boolean identify(CouponBank bank) {
        Map<Position, CurrencyToken> bankTokens = bank.getTokens();
        Map myTokens = getTokens();
        if (myTokens.size() != bankTokens.size()) {
            return false;
        }

        Set<Position> positions = myTokens.keySet();
        for (Iterator<Position> positionIterator = positions.iterator(); positionIterator.hasNext();) {
            Position position = positionIterator.next();
            if (bankTokens.get(position) == null) {
                return false;
            }
            if (! bankTokens.get(position).equals(getToken(position))) {
                return false;
            }
        }
        return true;
    }
}
