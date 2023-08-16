package net.commerce.zocalo.currency;

import java.math.MathContext;
import java.math.RoundingMode;
import java.math.BigDecimal;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Quantities represent numbers using BigDecimal.  They carry a MathContext for computations.
    Subclasses represent Price and Probability by limiting the range of possible values and being
    able to scale.  */
public class Quantity implements Comparable {
    static final public MathContext NINE_DIGITS = new MathContext(9, RoundingMode.HALF_EVEN);
    static final public MathContext ROUNDING_CONTEXT = new MathContext(NINE_DIGITS.getPrecision() - 3, NINE_DIGITS.getRoundingMode());
    static final public Quantity ZERO = new Quantity(0);
    static final public Quantity EPSILON = new Quantity(".0001");
    static final public Quantity ONE = new Quantity(BigDecimal.ONE);
    static final public Quantity Q100 = new Quantity("100");

    protected BigDecimal quant;
    private long id;

    public Quantity(String q) {
        quant = new BigDecimal(q);
    }

    public Quantity(BigDecimal q) {
        quant = q;
    }

    public Quantity(double q) {
        quant = new BigDecimal(q);
    }

    public String toString() {
        return quant().toString();
    }

    public String printAsQuantity() {
        return newScale(3).simplify().toString();
    }

    public String printAsDetailedQuantity() {
        return newScale(3).toString();
    }

    public String printAsIntegerQuantity() {
        return newScale(0).toString();
    }

    public String printAsScore() {
        return newScale(3).simplify().toString();
    }

    public String printAsDollars() {
        return movePointLeft(2).newScale(2).toString();
    }

    public BigDecimal asValue() {
        return quant();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Quantity quantity = (Quantity)o;

        if (quant != null ? !quant.equals(quantity.quant) : quantity.quant != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return quant != null ? quant.hashCode() : 0;
    }

    /** @deprecated */
    public Quantity() {
    }

    public Quantity div(Quantity q) {
        BigDecimal decimal = quant().divide(q.quant(), NINE_DIGITS);
        return new Quantity(decimal);
    }

    public Quantity div(int q) {
        BigDecimal decimal = quant().divide(new BigDecimal(q), NINE_DIGITS);
        return new Quantity(decimal);
    }

    public Quantity minus(Quantity q) {
        BigDecimal decimal = quant().subtract(q.quant(), NINE_DIGITS);
        return new Quantity(decimal);
    }

    public Quantity times(Quantity q) {
        return new Quantity(quant().multiply(q.quant(), NINE_DIGITS));
    }

    public boolean eq(Quantity q) {
        return 0 == quant().compareTo(q.quant());
    }

    public Quantity absLog() {
        return new Quantity(Math.abs(Math.log(quant().doubleValue())));
    }

    public boolean approaches(Quantity quantity) {
        return EPSILON.compareTo(this.minus(quantity).abs()) != -1;
    }

    public int compareTo(Object o) {
        if (o instanceof Quantity) {
            return -((Quantity)o).compareTo(this);
        }
        return 0;
    }

    public int compareTo(Quantity quantity) {
        BigDecimal otherRounded = quantity.quant().round(ROUNDING_CONTEXT);
        BigDecimal rounded = quant().round(ROUNDING_CONTEXT);
        return rounded.compareTo(otherRounded);
    }

    public Quantity abs() {
        return new Quantity(quant().abs());
    }

    public Quantity exp() {
        return new Quantity(Math.exp(quant().doubleValue()));
    }

    public Quantity round(MathContext c) {
        return new Quantity(quant().round(c));
    }

    public Quantity round() {
        return new Quantity(quant().round(NINE_DIGITS));
    }

    public Quantity remainder(Quantity dividend) {
        return new Quantity(quant().remainder(dividend.quant()));
    }

    public Quantity plus(Quantity value) {
        return new Quantity(quant().add(value.quant()));
    }

    public Quantity min(Quantity other) {
        return new Quantity(quant().min(other.quant()));
    }

    public Quantity max(Quantity other) {
        return new Quantity(quant().max(other.quant()));
    }

    public boolean isPositive() {
        return ZERO.compareTo(this) < 0;
    }

    public boolean isZero() {
        return ZERO.compareTo(this) == 0;
    }

    public boolean isNegative() {
        return ZERO.compareTo(this) > 0;
    }

    public boolean isNonNegative() {
        return isPositive() || isZero();
    }

    public Quantity negate() {
        return new Quantity(quant().negate());
    }

    public boolean isNegligible() {
        return newScale(6).isZero();
    }

    public Quantity scale(int scale) {
        return new Quantity(quant().setScale(scale, NINE_DIGITS.getRoundingMode()));
    }

    public Quantity movePointLeft(int i) {
        return new Quantity(quant().movePointLeft(i));
    }

    public Quantity newScale(int scale) {
        return new Quantity(quant().setScale(scale, NINE_DIGITS.getRoundingMode()));
    }

    BigDecimal quant() {
        return getQuant();
    }

    /** @deprecated */
    public BigDecimal getQuant() {
        return quant;
    }

    /** @deprecated */
    public void setQuant(BigDecimal quant) {
        this.quant = quant;
    }

    /** @deprecated */
    public long getId() {
        return id;
    }

    /** @deprecated */
    public void setId(long id) {
        this.id = id;
    }

    public Quantity asQuantity() {
        return this;
    }

    public Quantity simplify() {
        BigDecimal stripped = quant().stripTrailingZeros();
        if (new Quantity("0").compareTo(this) == 0) {
            return ZERO;
        }
        int scale = Math.max(0, stripped.scale());
        return newScale(scale);
    }

    public Quantity roundFloor() {
        return new Quantity(quant.setScale(0, BigDecimal.ROUND_FLOOR));
    }

    public Quantity floor() {
        return newScale(0);
    }
}
