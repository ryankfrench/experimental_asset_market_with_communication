package net.commerce.zocalo.market;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.currency.Coupons;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.ajax.events.MakerTrade;
import net.commerce.zocalo.ajax.events.PriceChange;
import net.commerce.zocalo.freechart.ChartScheduler;

import java.util.*;

/** MultiMarketMaker is a MarketMaker for a Multi-position claim.  See {@link MarketMaker} for
    more details */
public class MultiMarketMaker extends MarketMaker {
    private Map<Position, Probability> probabilities;

    MultiMarketMaker(MultiMarket market, Quantity subsidy, User owner) {
        super(market, subsidy, owner);
        Position[] positions = market.getClaim().positions();
        probabilities = new HashMap<Position, Probability>(positions.length);
        Probability initialValue = new Probability(1.0 / positions.length);
        for (int i = 0; i < positions.length; i++) {
            Position pos = positions[i];
            probabilities.put(pos, initialValue);
        }
        recordInitialProbabilities(positions, initialValue, owner);
        initBeta(subsidy, positions.length);
    }

    /** @deprecated */
    MultiMarketMaker() {
    }

    public Probability currentProbability(Position position) {
        return probabilities.get(position);
    }

    void recordTrade(String name, Quantity coupons, Quantity cost, Position position, Dictionary<Position, Probability> startProbs) {
        Dictionary<Position, Probability> endProbs = currentProbabilities(position);
        Enumeration keys = endProbs.keys();
        while (keys.hasMoreElements()) {
            Position pos = (Position) keys.nextElement();
            Price startP = scaleToPrice(startProbs.get(pos));
            Price endP = scaleToPrice(endProbs.get(pos));
            Price avePrice = market().asPrice(cost.div(coupons));
            if (pos.equals(position)) {
                if (startP.compareTo(endP) < 0) {
                    MakerTrade.newMakerTrade(name, avePrice, coupons, pos, startP, endP);
                } else {
                    MakerTrade.newMakerTrade(name, avePrice, coupons.negate(), pos, startP, endP);
                }
            } else {
                MakerTrade.newMakerTrade(name, avePrice, Quantity.ZERO, pos, startP, endP);
            }
        }
        scheduleChartGeneration();
        new PriceChange(position.getClaim().getName(), currentProbabilities(position));
    }

    private void recordInitialProbabilities(Position[] positions, Probability initialValue, User owner) {
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Price scaledPrice = scaleToPrice(initialValue);
            MakerTrade.newMakerTrade(owner.getName(), scaledPrice, Quantity.ZERO, position, scaledPrice, scaledPrice);
        }
    }

    Dictionary<Position, Probability> currentProbabilities(Position ignore) {
        Hashtable<Position, Probability> probs = new Hashtable<Position, Probability>();
        Set keys = probabilities.keySet();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            Position pos = (Position) iterator.next();
            probs.put(pos, currentProbability(pos));
        }
        return probs;
    }

//// TRADING /////////////////////////////////////////////////////////////////

    Set<Coupons> provideCouponSet(Position position, Quantity totalShares, boolean increasing) {
        HashSet<Coupons> couponSet = new HashSet<Coupons>();
        if (increasing) {
            Coupons coupons = provideCoupons(position, totalShares);
            couponSet.add(coupons);
        } else {
            ensureOpposingSetsAvailable(position, totalShares);
            addOpposingShares(position, couponSet, totalShares);
        }
        return couponSet;
    }

    private void addOpposingShares(Position position, HashSet<Coupons> couponSet, Quantity totalShares) {
        Position[] positions = position.getClaim().positions();
        for (int i = 0; i < positions.length; i++) {
            Position pos = positions[i];
            if (! pos.equals(position)) {
                couponSet.add(accounts().provideCoupons(pos, totalShares));
            }
        }
    }

    void scaleProbabilities(Position position, Probability newProb) {
       // scale others by (1 - newProb)/(1 - oldProb) ; position gets remainder
        Probability oldProb = currentProbability(position);
        Quantity scale = newProb.inverted().div(oldProb.inverted());
        Quantity totalProb = Quantity.ZERO;
        for (Iterator iterator = probabilities.keySet().iterator(); iterator.hasNext();) {
            Position pos = (Position)iterator.next();
            if (pos != position) {
                Probability newP = new Probability(scale.times(currentProbability(pos)));
                setProbability(pos, newP);
                totalProb = totalProb.plus(newP);
            }
        }
        setProbability(position, new Probability(totalProb).inverted());
    }

    void setProbability(Position position, Probability probability) {
        probabilities.put(position, probability);
    }

    /** @deprecated */
    public Map getProbabilities() {
        return probabilities;
    }

    /** @deprecated */
    public void setProbabilities(Map<Position, Probability> probabilities) {
        this.probabilities = probabilities;
    }
}
