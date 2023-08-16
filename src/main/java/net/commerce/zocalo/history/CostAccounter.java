package net.commerce.zocalo.history;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.ajax.events.Redemption;
import net.commerce.zocalo.ajax.events.Trade;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Quantity;

import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Compute costs of acquiring positions.  Relies on DB Trade and Redemption records.  Distinguish
 costs of buying shares, costs to buy complements when selling, and value of complete sets liquidated,
 as well as shares redeemed when markets close.  */
public class CostAccounter {
    private HashMap<String, Quantity> buyCosts;
    private HashMap<String, Quantity> buyQuants;
    private HashMap<String, Quantity> sellCosts;
    private HashMap<String, Quantity> sellQuants;
    private HashMap<String, Quantity> redemptionQuant;
    private HashMap<String, Quantity> redemptionValue;
    private Quantity totalSales = Quantity.ZERO;
    private Quantity minBuy = Quantity.ZERO;

    public CostAccounter(SecureUser user, Market market) {
        final List trades = HibernateUtil.getTrades(user, market);
        final Claim claim = market.getClaim();
        final List redemptions = HibernateUtil.getRedemptions(user, market);
        initializeStats(trades, redemptions, claim);
    }

    private void initializeStats(List trades, List redeems, Claim claim) {
        Position[] positions = claim.positions();
        buyCosts = new HashMap<String, Quantity>(positions.length);
        buyQuants = new HashMap<String, Quantity>(positions.length);
        sellCosts = new HashMap<String, Quantity>(positions.length);
        sellQuants = new HashMap<String, Quantity>(positions.length);
        this.redemptionQuant = new HashMap<String, Quantity>(positions.length);
        this.redemptionValue = new HashMap<String, Quantity>(positions.length);
        for (int i = 0; i < positions.length; i++) {
            String pos = positions[i].getName();
            buyCosts.put(pos, Quantity.ZERO);
            buyQuants.put(pos, Quantity.ZERO);
            sellCosts.put(pos, Quantity.ZERO);
            sellQuants.put(pos, Quantity.ZERO);
            this.redemptionQuant.put(pos, Quantity.ZERO);
            this.redemptionValue.put(pos, Quantity.ZERO);
        }
        for (Iterator iterator = trades.iterator(); iterator.hasNext();) {
            Trade t = (Trade) iterator.next();
            String pos = t.getPos().getName();
            Quantity quant = t.getQuantity();
            if (quant.isPositive()) {
                buyCosts.put(pos, buyCosts.get(pos).plus(quant.times(t.getPrice())));
                buyQuants.put(pos, buyQuants.get(pos).plus(quant));
            } else if (quant.isNegative()) {
                sellCosts.put(pos, sellCosts.get(pos).minus((quant.times(t.getPrice()))));
                sellQuants.put(pos, sellQuants.get(pos).minus(quant));
            }
        }
        for (Iterator iterator = redeems.iterator(); iterator.hasNext();) {
            Redemption r = (Redemption) iterator.next();
            redemptionQuant.put(r.getPos().getName(), r.getQuantity());
            redemptionValue.put(r.getPos().getName(), r.getQuantity().times(r.getPrice()));
        }
        minBuy = minQuantity();
        totalSales = totalNetSales();
    }

    private Quantity totalNetSales() {
        Quantity totalNetSales = Quantity.ZERO;
        for (Iterator<String> positions = buyQuants.keySet().iterator(); positions.hasNext();) {
            String position =  positions.next();
            Quantity netQ = getBuyQuantity(position).minus(getSellQuantity(position));
            if (netQ.isNegative()) {
                totalNetSales = totalNetSales.minus(netQ);
            }
        }
        return totalNetSales;
    }

    private Quantity minQuantity() {
        Quantity minusOne = Quantity.ONE.negate();
        Quantity minBuy = minusOne;
        for (Iterator<String> positions = buyQuants.keySet().iterator(); positions.hasNext();) {
            String position =  positions.next();
            Quantity q = getBuyQuantity(position);
            if (minBuy.equals(minusOne) && !q.isNegative()) {
                minBuy = q;
            }
            if (!q.isNegative()) {
                minBuy = q.min(minBuy);
            }
        }

        return minBuy;
    }

    /** How much did the user pay for current holdings in all positions of this claim? */
    public Quantity totalCost() {
        Quantity total = Quantity.ZERO;
        for (Iterator<String> positions = buyQuants.keySet().iterator(); positions.hasNext();) {
            total = total.plus(getCost(positions.next()));
        }
        return total;
    }

    /** How many coupons did the user hold in all positions when the claim was settled?  */
    public Quantity totalRedemptions() {
        Quantity total = Quantity.ZERO;
        for (Iterator<String> positions = buyQuants.keySet().iterator(); positions.hasNext();) {
            total = total.plus(getRedemptionValue(positions.next()));
        }
        return total;
    }

    /** How many units of position did the user buy?  */
    public Quantity getBuyQuantity(String position) {
        return buyQuants.get(position);
    }

    /** How much did the user pay to acquire holdings in position?  Doesn't count sales.  See getCost()
    for total cost of holdings.  */
    public Quantity getBuyCost(String position) {
        return buyCosts.get(position);
    }

    /** How many units of position did the user sell?  In multi-outcome markets, how many complementary
    sets did the user sell?  */
    public Quantity getSellQuantity(String position) {
        return sellQuants.get(position);
    }

    /** how much did the user receive by selling position?  In a unary market, the user received
     this much in exchange for coupons already held, otherwise the user spent this amount to
     acquire complements to position. */
    public Quantity getSellCost(String position) {
        return sellCosts.get(position);
    }

    /** How many shares of position did the user hold? */
    public Quantity getRedemptions(String position) {
        return redemptionQuant.get(position);
    }

    /** How much did the user receive when the position was closed out?  */
    public Quantity getRedemptionValue(String pos) {
        return redemptionValue.get(pos);
    }

    /** How much of position does the user hold? */
    public Quantity getQuantity(String pos) {
        return getBuyQuantity(pos).minus(getSellQuantity(pos)).plus(totalSales).minus(minBuy).minus(getRedemptions(pos));
    }

    /** How much did the user spend acquiring the current holdings in position? */
    public Quantity getCost(String pos) {
        return getBuyCost(pos).minus(getSellCost(pos)).abs();
    }
}
