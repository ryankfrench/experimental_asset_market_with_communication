package net.commerce.zocalo.currency;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.ajax.events.Redemption;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** CouponBank doesn't charge for coupons, so sellers get the proceeds of the sale.  */
public class UnaryCouponBank extends CouponBank {
    public UnaryCouponBank(Claim claim, Funds empty) {
        super(claim, empty);
    }

    static public UnaryCouponBank makeUnaryBank(Claim claim, Funds empty) {
        return new UnaryCouponBank(claim, empty);
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

        return couponSets;
    }

    /**  couponsMap should contain QUANTITY of each coupon in this claim.  If it
     contains fewer, the owner will only get credit for the full sets.  Any odd
     coupons will be returned.  */
    public Accounts settle(Quantity quantity, Map couponsMap) {
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
        Funds redemptions = getCash().provide(minCoupons);
        change.receiveCash(redemptions);
        return change;
    }

    public Quantity redeem(Position pos, Set users) {
        Map<Position, Quantity> redemptions = new HashMap<Position, Quantity>();
        for (Iterator userIter = users.iterator(); userIter.hasNext();) {
            User owner = (User) userIter.next();
            Set couponsSet = owner.getAccounts().provideAllCoupons(pos);
            for (Iterator couponIter = couponsSet.iterator(); couponIter.hasNext();) {
                Coupons coupons = (Coupons) couponIter.next();
                Quantity balance = coupons.getBalance();
                Quantity transferred = coupons.transfer(coupons.makeEmpty());
                if (transferred.isZero() && ! coupons.negligible()) {
                    Logger log = Logger.getLogger(CouponBank.class);
                    log.error("Unable to redeem " + balance + " coupons for " + owner.getName());
                }
                Position position = coupons.getPosition();
                if (position.equals(pos)) {
                    owner.receiveCash(getCash().provide(balance));
                    increment(redemptions, pos, balance);
                    Redemption.newRedemption(owner.getName(), Price.ONE_DOLLAR, balance, pos);
                } else {
                    increment(redemptions, position, balance);
                    Redemption.newRedemption(owner.getName(), Price.ZERO_DOLLARS, balance, position);
                }
            }
        }

        validateBalancedRedemptions(redemptions, pos);
        return getSetsRedeemed();
    }
}
