package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.service.PropertyHelper;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.currency.Quantity;

import java.util.Properties;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Roles describe how to initialize subjects playing the various roles. */
public abstract class Role {
    private Quantity initialCash = Quantity.ZERO;
    private Quantity initialCoupons = Quantity.ZERO;
    private boolean initialized = false;
    private boolean canBuy;
    private boolean canSell;

    private boolean[] dormant;

    public boolean requiresJudges() {
        return false;
    }

    public void initializeFromProps(Properties props) {
        if (initialized) {
            return;
        }
        if (needsUser()) {
            String endowmentProp = PropertyHelper.dottedWords(ENDOWMENT_PROPERTY_WORD, roleKeyword());
            String ticketsProp = PropertyHelper.dottedWords(TICKETS_PROPERTY_WORD, roleKeyword());
            String restrictionProp = PropertyHelper.dottedWords(RESTRICTION, roleKeyword());

            initialCash = new Quantity(PropertyHelper.parseDecimal(endowmentProp, props));
            initialCoupons = new Quantity(PropertyHelper.parseInteger(ticketsProp, props));

            String restriction = props.getProperty(restrictionProp);
            if ("".equals(restriction) || null == restriction) {
                canBuy = true;
                canSell = true;
            } else if (SELL_ONLY.equals(restriction.trim())) {
                canBuy = false;
                canSell = true;
            } else if (BUY_ONLY.equals(restriction.trim())) {
                canBuy = true;
                canSell = false;
            } else {
                canBuy = true;
                canSell = true;
            }
        }
        initialized = true;
    }

    abstract public String roleKeyword();

    public void initializeDormancy(boolean[] booleans) {
        dormant = booleans;
    }

    public Quantity getInitialCoupons() {
        return initialCoupons;
    }

    protected void setInitialCoupons(Quantity initialCoupons) {
        this.initialCoupons = initialCoupons;
    }

    public Quantity getInitialCash() {
        return initialCash;
    }

    protected void setInitialCash(Quantity initialCash) {
        this.initialCash = initialCash;
    }

    public boolean needsUser() {
        return true;
    }

    public abstract AbstractSubject createSubject(User user, int rounds, String playerName);

    public boolean canBuy() {
        return canBuy;
    }

    public boolean canSell() {
        return canSell;
    }

    public boolean canBuy(int round) {
        if (dormant(round)) {
            return false;
        }
        return canBuy;
    }

    public boolean canSell(int round) {
        if (dormant(round)) {
            return false;
        }
        return canSell;
    }

    protected boolean dormant(int round) {
        if (dormant == null) {
            return false;
        }
        return dormant[round];
    }

    public boolean[] dormantPeriods() {
        return dormant;
    }
}
