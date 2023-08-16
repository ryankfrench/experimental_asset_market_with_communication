package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import org.jfree.data.time.TimePeriodValue;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Action representing the fact that a user traded with the Market Maker.  */
public class MakerTrade extends Trade {
    Price openingPrice;
    Price closingPrice;

    public TimePeriodValue timeAndPrice() {
        if (getQuantity().isZero()) {
            return null;
        }
        return new TimePeriodValue(makePeriod(), getClosingPrice().asValue());
    }

    public TimePeriodValue timeAndPriceInverted() {
        if (getQuantity().isZero()) {
            return null;
        }
        return new TimePeriodValue(makePeriod(), getClosingPrice().inverted().asValue());
    }

    private MakerTrade(String trader, Price price, Quantity quantity, Position pos, Price open, Price close) {
        super(trader, price, quantity, pos, PriceAction.getActionLogger());
        openingPrice = open;
        closingPrice = close;
        log();
    }

    static public MakerTrade newMakerTrade(String trader, Price price, Quantity quant, Position pos, Price open, Price close) {
        MakerTrade trade = new MakerTrade(trader, price, quant, pos, open, close);
        HibernateUtil.save(trade);
        return trade;
    }

    public TimePeriodValue openValue() {
        return new TimePeriodValue(makePeriod(), openingPrice.asValue());
    }

    public TimePeriodValue closeValue() {
        return new TimePeriodValue(makePeriod(), closingPrice.asValue());
    }

    /** @deprecated */
    public MakerTrade() {
        super();
    }

    public String toLogString() {
        String priceAndQuantity;
        Price price = getPrice();
        String priceDesc;
        if (openingPrice.equals(closingPrice)) {
            priceDesc = " at " + price.printHighPrecision();
        } else {
            priceDesc = " changing the price from " + openingPrice.printHighPrecision()
                         + " to " + closingPrice.printHighPrecision();
        }
        String quantDesc = "for " + getQuantity().printAsQuantity();
        String posDesc = " of " + getPosition(getPos());
        priceAndQuantity = quantDesc + posDesc + priceDesc;
        return getGID() + getOwner() + actionString() + priceAndQuantity;
    }

    protected String actionString() {
        return " traded with the market maker ";
    }

    public boolean isBuy() {
        boolean rising = getClosingPrice().compareTo(getOpeningPrice()) >= 0;
        return getPos().isBuy(rising);
    }

    /** @deprecated */
    public Price getClosingPrice() {
        return closingPrice;
    }

    /** @deprecated */
    public void setClosingPrice(Price closingPrice) {
        this.closingPrice = closingPrice;
    }

    /** @deprecated */
    public Price getOpeningPrice() {
        return openingPrice;
    }

    /** @deprecated */
    public void setOpeningPrice(Price openingPrice) {
        this.openingPrice = openingPrice;
    }
}
