package net.commerce.zocalo.currency;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;

import java.math.MathContext;
import java.math.RoundingMode;
import java.math.BigDecimal;

public class BigDecimalTest extends TestCase {
    public void testQuantityClass() {
        Quantity q145 = new Quantity("145");
        Quantity q400 = new Quantity("400");
        Quantity q1 = new Quantity("1");
        double d = 145.0;
        d /= 400.0;
        d = 1.0 - d;
        assertEquals(1.0 - .3625, d, .000001);
//  1.0 - (785.0 / 1000.0)
//  new BigDecimal("1.0").subtract(new BigDecimal("785.0").divide(new BigDecimal("1000.0"))
        Quantity qDiv = q145.div(q400);
        Quantity qDiff = q1.minus(qDiv);
        assertTrue(new Quantity(".6375").eq(qDiff));
    }

    public void testLogsAndE() {
        Quantity e = new Quantity(Math.E);
        Quantity log = e.absLog();

        assertEquals(0, log.compareTo(Quantity.ONE));
        Quantity ten = new Quantity("10");
        assertEquals(0, ten.exp().absLog().compareTo(ten));
        assertTrue(ten.approaches(ten.absLog().exp()));
        assertEquals(0, ten.round().compareTo(ten.absLog().exp().round()));
    }

    public void testPrecision() {
        Quantity ten = new Quantity("10");
        Quantity three = new Quantity("3");
        assertTrue(ten.approaches(ten.div(three).times(three)));
        Quantity ten_5 = new Quantity("10");
        Quantity three_5 = new Quantity("3");
        assertTrue(ten_5.approaches(ten_5.div(three_5).times(three_5)));
        Quantity ten_5_U = new Quantity("10");
        Quantity three_5_U = new Quantity("3");
        assertTrue(ten_5_U.approaches(ten_5_U.div(three_5_U).times(three_5_U)));
    }

    public void testDoubleToBigDecimalConversion() {
        BigDecimal viaPrinting = BigDecimal.valueOf(1.0 - (145.0/400.0));
        BigDecimal exact = new BigDecimal(1.0 - (145.0/400.0));
        assertTrue(viaPrinting.compareTo(exact) != 0);
        BigDecimal calculated = new BigDecimal("1").subtract(new BigDecimal("145").divide(new BigDecimal("400")));
        assertTrue(calculated.compareTo(viaPrinting) == 0);
    }
}
