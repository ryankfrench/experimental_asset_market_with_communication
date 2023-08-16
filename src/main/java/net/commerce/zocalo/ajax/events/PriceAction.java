package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.util.Comparator;
import java.util.Date;

import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.logging.GID;

import org.apache.log4j.Logger;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValue;

/** base class of actions that represent price changes.  This class hierarchy makes use of log4j.
    Leaf descendents (other than those provided solely for Hibernate) MUST call log() at end
    of construction.  */
public abstract class PriceAction extends Action {
    final String ACTION_KEY = "action";
    private String owner;
    private Price price;
    private Quantity quantity;
    private Date time;
    private Position pos;
    private Long id;
    private Logger logTemp;
    private final String logGID = GID.log();

	public static class TimeComparator implements Comparator<PriceAction>
	{
		protected boolean desc;
		public TimeComparator(boolean desc) {
			this.desc = desc;
		}
		public int compare(PriceAction o1, PriceAction o2)
		{
			// Sort by time desc
			int ret = o1.getTime().compareTo(o2.getTime());
			if (desc)
				ret = ret * -1;
			return ret;
		}
	}
    
    protected PriceAction(Logger logger) {
        super(logger);
        time = new Date();
        logTemp = logger;
    }

    static public Logger getActionLogger() {
        return Logger.getLogger(PriceAction.class);
    }

    protected PriceAction(String owner, Price price, Quantity quantity, Position pos, Logger logger) {
        super(logger);
        this.owner = owner;
        this.price = price;
        this.quantity = quantity;
        this.pos = pos;
        time = new Date();
        logTemp = logger;
    }

    void log() {
        if (logTemp != null) {
            logTemp.callAppenders(this);
        }
    }

    public String toLogString() {
        String priceAndQuantity;
        Price price = getPrice();
        if (getQuantity().equals(Quantity.ONE)) {
            priceAndQuantity = "at " + price;
        } else {
            priceAndQuantity = "for " + getQuantity().printAsQuantity()
                    + " of " + getPosition(pos)
                    + " at " + price;
        }
        return logGID + getOwner() + actionString() + priceAndQuantity;
    }

    protected String getGID() {
        return logGID;
    }

    protected String getPosition(Position position) {
        String posName;
        if (position == null) {
            posName = "unknown position";
        } else {
            posName = position.qualifiedName();
        }
        return posName;
    }

    abstract protected String actionString();

    public TimePeriodValue timeAndPrice() {
        if (getQuantity().isZero()) {
            return null;
        }
        return new TimePeriodValue(makePeriod(), getPrice().asValue());
    }

    public TimePeriodValue timeAndPriceInverted() {
        if (getQuantity().isZero()) {
            return null;
        }
        return new TimePeriodValue(makePeriod(), getPrice().inverted().asValue());
    }

    public TimePeriodValue timeAndVolume() {
        Quantity quantity = getQuantity();
        return new TimePeriodValue(makePeriod(), quantity.asValue());
    }

    SimpleTimePeriod makePeriod() {
        return new SimpleTimePeriod(getTime(), getTime());
    }

    public Price getNaturalPrice() {
        if (pos.isInvertedPosition()) {
            return getPrice().inverted();
        } else {
            return getPrice();
        }
    }

    public Position getPos() {
        return pos;
    }

    /** @deprecated */
    private Long getId() {
        return id;
    }

    /** @deprecated */
    private void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    /** @deprecated */
    void setOwner(String owner) {
        this.owner = owner;
    }

    public Price getPrice() {
        return price;
    }

    /** @deprecated */
    void setPrice(Price price) {
        this.price = price;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    /** @deprecated */
    void setQuantity(Quantity quantity) {
        this.quantity = quantity;
    }

    public Date getTime() {
        return time;
    }

    /** @deprecated */
    public void setTime(Date time) {
        this.time = time;
    }

    /** @deprecated */
    public void setPos(Position pos) {
        this.pos = pos;
    }
}
