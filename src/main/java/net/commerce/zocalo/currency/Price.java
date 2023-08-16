package net.commerce.zocalo.currency;

import java.math.BigDecimal;
import java.math.MathContext;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Prices have a quantity, a maximum, and a scale.  The Maximum is the
    maximum price in some market, the scale tells how to scale prices for printing (when
    holdings are in dollars and prices are in cents, scale would be 2.)

    RoundingMode wants PRECISION instead of SCALE, so we have to be careful in converting.
    We want to know how many digits to the right of the decimal place to maintain in
    various operations (SCALE).  PRECISION means how many significant digits in total,
    not how many to the right of the decimal.  BigDecimal uses SCALE the way we want, but
    we have to be careful to round with movePointLeft(x).newScale(0). 

Need maxPrice, conversion for Currency scale.  (Plus computation resolution)
 Example:
      1-99, 0, 0     (Chapman experiments)
.01-99.99, -2, 0 (holdings should print to .00)
     1-399, 0, 0     (Chapman experiments)
  .1-99.9, -1, 0   (InTrade)
      1-99, 0, 0 (NewsFutures)

 */
public class Price extends RangedQuantity {
    static public final Price ONE_DOLLAR = new Price(Quantity.Q100);
    static public final Price ZERO_DOLLARS = new Price(Quantity.ZERO, ONE_DOLLAR);

    /** @deprecated */
    Price() {
    }

    public Price(BigDecimal q, Price prior) {
        super(q, prior.getMaxValue());
    }

    public Price(double q, Price prior) {
        super(q, prior.getMaxValue());
    }

    public Price(String q, Price prior) {
        super(new BigDecimal(q), prior.getMaxValue());
    }

    public Price(Quantity q, Price prior) {
        super(q, prior.getMaxValue());
    }

    public Price(Quantity price, Quantity max) {
        super(price, max);
    }

    public Price(Quantity priceLimit) {
        super(priceLimit, priceLimit);
    }

    public static Price dollarPrice(String p) {
        return new Price(p, ONE_DOLLAR);
    }

    public static Price dollarPrice(double p) {
        return new Price(p, ONE_DOLLAR);
    }

    public Quantity negate() {
        return new Quantity(quant().negate());
    }

    public Probability asProbability() {
        return new Probability(quant().divide(getMaxValue().asValue()));
    }

    public String printQuant() {
        return quant().toString();
    }

    public Price inverted() {
        return new Price(getMaxValue().minus(this), this);
    }

    public Price round(MathContext c)  {
        return new Price(quant().round(c), this);
    }

    public Price newValue(Quantity quantity) {
        return new Price(quantity, this);
    }

    public Price newValue(BigDecimal quantity) {
        return new Price(quantity, this);
    }

    public Price simplify() {
        return new Price(quant().stripTrailingZeros(), this);
    }

    public int compareTo(Quantity quantity) {
        if (quantity instanceof Price) {
            Price other = (Price)quantity;
            return super.compareTo(other);
        } else {
            throw new ClassCastException("don't compare Prices with other Quantities.");
        }
    }

    public String printAsWholeCents() {
        return newScale(0).toString();
    }

    public String printHighPrecision() {
        return newScale(4).simplify().toString();
    }

    public String toString() {
        return round(NINE_DIGITS).printQuant();
    }
}
