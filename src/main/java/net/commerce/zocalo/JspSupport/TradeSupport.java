package net.commerce.zocalo.JspSupport;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.user.Warnable;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;

/** Common support for JSP pages that support trading.  */
public abstract class TradeSupport {
    final static public String SELL_ACTION = "sell";
    final static public String BUY_ACTION = "buy";

    static void warnAboutDuplicateOrders(Warnable user) {
        user.warn("You can only create one order at each price.  " +
                "Please delete existing order before entering a new one.");
    }

    static Quantity parseInteger(String val, Warnable user) {
        if (val == null || "".equals(val)) {
            return Quantity.ZERO;
        }

        try {
            Quantity retVal = new Quantity(val.trim());
            if (retVal.compareTo(Quantity.ZERO) <= 0) {
                warnNonNullUser(user, "prices and quantities must be positive: ", val);
                return Quantity.ZERO;
            } else if (retVal.compareTo(retVal.scale(0)) == 0) {
                return retVal;
            } else {
                warnNonNullUser(user, "prices and quantities must be whole numbers: ", val);
                return Quantity.ZERO;
            }
        } catch (NumberFormatException e) {
            try {
                Float.parseFloat(val);
                warnNonNullUser(user, "prices and quantities must be whole numbers: ", val);
            } catch (NumberFormatException e2) {
                warnNonNullUser(user, "Couldn't interpret as number: ", val);
            }
            return Quantity.ZERO;
        }
    }

    private static void warnNonNullUser(Warnable user, String warning, String val) {
        if (user != null) {
            user.addWarning(warning + val);
        }
    }

    static Quantity parseDecimal(String val, Warnable user) {
        if (val == null || "".equals(val)) {
            return Quantity.ZERO;
        }
        try {
            Quantity retVal = new Quantity(val.trim());
            if (retVal.compareTo(Quantity.ZERO) < 0) {
                user.addWarning("prices and quantities must be positive: " + val);
                return Quantity.ZERO;
            } else {
                return retVal;
            }
        } catch (NumberFormatException e) {
            user.addWarning("Couldn't interpret as number: " + val);
            return Quantity.ZERO;
        }
    }

    static protected void deleteOrder(String buySell, Warnable user, String deletePrice, Market mkt) {
        BinaryMarket market = (BinaryMarket)mkt;
        Position pos;
        if (BUY_ACTION.equals(buySell)) {
            pos = market.getBinaryClaim().getYesPosition();
        } else if (SELL_ACTION.equals(buySell)) {
            pos = market.getBinaryClaim().getNoPosition();
        } else {
            pos = null;
        }
        Price price = new Price(parseInteger(deletePrice, user), market.maxPrice());
        market.getBook().removeOrder(user.getName(), price, pos);
    }
}
