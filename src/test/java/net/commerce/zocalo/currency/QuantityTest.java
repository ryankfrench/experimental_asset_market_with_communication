package net.commerce.zocalo.currency;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.math.MathContext;
import java.math.RoundingMode;

import net.commerce.zocalo.JunitHelper;

public class QuantityTest extends JunitHelper {
    public void testTruncation() {
        Quantity q = new Quantity("3.14");
        assertEquals(0, new Quantity("3").compareTo(q.newScale(0)));
        assertEquals(0, new Quantity("3.1").compareTo(q.newScale(1)));
        assertEquals(0, q.compareTo(q.newScale(2)));
    }

    public void testNegligibility() {
        Quantity q = new Quantity(".001");
        assertFalse(q.isNegligible());
        assertFalse(q.times(q).isNegligible());
        assertTrue(q.times(q).times(q).isNegligible());
    }

    public void testQEquals() {
        Quantity three = new Quantity("3");
        assertQEquals(three, new Quantity("3.0"));
    }

    public void testRounding() {
        Quantity pi = new Quantity(Math.PI);
        assertQNotEquals(pi, new Quantity("3.14"));
        assertQEquals(pi.newScale(2), new Quantity("3.14"));
        assertQEquals(pi.round(new MathContext(3, RoundingMode.HALF_EVEN)), new Quantity("3.14"));
    }

    public void testPrinting() {
        Quantity pi = new Quantity(Math.PI);
        assertEquals("3", pi.newScale(0).toString());
        assertEquals("3.1", pi.newScale(1).toString());
        assertEquals("3.1415", new Quantity("3.1415").toString());
        assertEquals("3.14", pi.newScale(2).toString());
        assertEquals("3.15", new Quantity("3.149932").newScale(2).toString());
    }
}
