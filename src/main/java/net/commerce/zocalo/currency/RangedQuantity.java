package net.commerce.zocalo.currency;

import java.math.BigDecimal;
import java.math.MathContext;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**  RangedQuantities are BigDecimals whose values range from zero to some maximum value.
     They're useful for limited prices and for probabilities.  */
public abstract class RangedQuantity extends Quantity {
    protected Quantity maxValue;

    public RangedQuantity(BigDecimal q, String maximum) {
        super(q);
        this.maxValue = new Quantity(maximum);
        verifyRange();
    }

    /** @deprecated */
    RangedQuantity() {
        super();
    }

    public RangedQuantity(String q, String maximum) {
        super(q);
        maxValue = new Quantity(maximum);
        verifyRange();
    }

    public RangedQuantity(Quantity q, BigDecimal maximum) {
        super(q.quant);
        maxValue = new Quantity(maximum);
        verifyRange();
    }

    public RangedQuantity(BigDecimal quantity, Quantity max) {
        super(quantity);
        this.maxValue = max.asQuantity();
        verifyRange();
    }

    public RangedQuantity(double quantity, Quantity max) {
        super(quantity);
        this.maxValue = max.asQuantity();
        verifyRange();
    }

    public RangedQuantity(String q, BigDecimal maximum) {
        super(q);
        maxValue = new Quantity(maximum);
        verifyRange();
    }

    public RangedQuantity(String q, Quantity maximum) {
        super(q);
        maxValue = maximum;
        verifyRange();
    }

    public RangedQuantity(BigDecimal d, BigDecimal maximum) {
        super(d);
        maxValue = new Quantity(maximum);
        verifyRange();
    }

    public RangedQuantity(Quantity quantity, Quantity newRange) {
        super(quantity.quant());
        maxValue = newRange.asQuantity();
        verifyRange();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        RangedQuantity that = (RangedQuantity)o;

        if (maxValue != null ? !maxValue.equals(that.maxValue) : that.maxValue != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (maxValue != null ? maxValue.hashCode() : 0);
        return result;
    }

    public Quantity getMaxValue() {
        return maxValue;
    }

    /** @deprecated */
    public void setMaxValue(Quantity maxValue) {
        this.maxValue = maxValue;
    }

    public Quantity asQuantity() {
        return new Quantity(this.asValue());
    }

    private void verifyRange() {
        if (getMaxValue().compareTo(this) < 0 || Quantity.ZERO.compareTo(this) > 0) {
            throw new IllegalArgumentException("quantity exceeds maximum");
        }
    }

    public boolean eq(Quantity q) {
        if (q instanceof RangedQuantity) {
            return super.eq(q);
        } else {
            return false;
        }
    }

    public abstract RangedQuantity round(MathContext c);

    public abstract RangedQuantity newValue(Quantity quantity);
    public abstract RangedQuantity newValue(BigDecimal quantity);
    public abstract RangedQuantity inverted();

    public Probability asProbability() {
        return new Probability(quant().divide(getMaxValue().quant()));
    }
}

