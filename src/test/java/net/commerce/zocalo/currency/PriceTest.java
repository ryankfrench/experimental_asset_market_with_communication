package net.commerce.zocalo.currency;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.JunitHelper;

public class PriceTest extends JunitHelper {
    public void test100PointScale() {
        Price zeroPrice = Price.dollarPrice(0);
        Price fifty = new Price("50", zeroPrice);
        assertQEquals(0, fifty.minus(fifty));
        assertQEquals(Quantity.Q100, zeroPrice.getMaxValue());
        assertQEquals(0, zeroPrice);
        assertQEquals(Quantity.Q100, zeroPrice.inverted());
    }

    public void testRounding() {
        Price fiftyCents = Price.dollarPrice("50");
        Price thirtySevenCents = Price.dollarPrice("37");

        Price fractionalCents = Price.dollarPrice("37.8");
        Price tenCents = Price.dollarPrice("10");
        Price oneCent = Price.dollarPrice("01");
        Price twoDigits = Price.dollarPrice("37.23");

        assertQEquals(50, fiftyCents);
        assertQEquals(37, thirtySevenCents);
        assertQEquals(37.8, fractionalCents);
        assertQEquals(10, tenCents);
        assertQEquals( 1, oneCent);
        assertQEquals(37.23, twoDigits);

        assertEquals("50", fiftyCents.toString());
        assertEquals("37", thirtySevenCents.toString());
        assertEquals("37.8", fractionalCents.toString());
        assertEquals("10", tenCents.toString());
        assertEquals("1", oneCent.toString());
        assertEquals("37.23", twoDigits.toString());

        assertEquals("50", fiftyCents.printAsWholeCents());
        assertEquals("37", thirtySevenCents.printAsWholeCents());
        assertEquals("38", fractionalCents.printAsWholeCents());
        assertEquals("10", tenCents.printAsWholeCents());
        assertEquals("1", oneCent.printAsWholeCents());
        assertEquals("37", twoDigits.printAsWholeCents());

        assertQEquals(new Quantity("37.23"), twoDigits);
        assertQEquals(new Probability(".3723"), twoDigits.asProbability());
    }

    public void testRoundingNonDollarPrices() {
        Quantity maxQ400 = new Quantity("400");
        Price fourHundredMax = new Price(maxQ400, maxQ400);
        Price fiftyCents = new Price("50", fourHundredMax);
        Price thirtySevenCents = new Price("37", fourHundredMax);
        Price twoDigits = new Price("37.23", fourHundredMax);

        assertQEquals(50, fiftyCents);
        assertQEquals(37, thirtySevenCents);
        assertQEquals(37.23, twoDigits);

        assertEquals("50", fiftyCents.toString());
        assertEquals("37", thirtySevenCents.toString());
        assertEquals("37.23", twoDigits.toString());

        assertEquals("50", fiftyCents.printAsWholeCents());
        assertEquals("37", thirtySevenCents.printAsWholeCents());
        assertEquals("37", twoDigits.printAsWholeCents());

        assertQEquals(new Quantity("37.23"), twoDigits);
        assertQEquals(new Quantity("37.23").div(fourHundredMax), twoDigits.asProbability());
    }

    public void testRoundIrrationals() {
        Price thirtyThreeDotDot = Price.dollarPrice(100.0 / 3.0);

        assertEquals("33.3333333", thirtyThreeDotDot.toString());
        assertEquals("33", thirtyThreeDotDot.printAsWholeCents());
        assertEquals("33", thirtyThreeDotDot.round(Probability.PRINT_CONTEXT).printQuant());
        assertEquals("33.3333", thirtyThreeDotDot.printHighPrecision());
        assertEquals("33.3333333", thirtyThreeDotDot.round(Quantity.NINE_DIGITS).printQuant());
        assertTrue(thirtyThreeDotDot.printQuant().startsWith("33.3333333"));
        assertTrue(thirtyThreeDotDot.printQuant().length() > 12);
    }
}
