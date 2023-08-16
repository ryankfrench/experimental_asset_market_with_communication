package net.commerce.zocalo.currency;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;

public class ProbabilityTest extends TestCase {
    public void testProbCreation() {
        Probability p = new Probability(".3");
        Probability pNot = new Probability(".7");
        assertEquals(0, pNot.compareTo(p.inverted()));
    }

    public void testArithmetic() {
        Probability oneFifth = new Probability(".2");
        Probability oneHalf = new Probability(".5");
        Probability oneTenth = new Probability(".1");
        assertEquals(0, oneFifth.times(oneHalf).compareTo(oneTenth));

        // when changing p from .2 to .3, we calculate (targetProbability * (1.0 - curProb )) / (curProb * (1.0 - targetProbability))
        Probability oldP = oneFifth;
        Probability newP = new Probability(".3");
        Probability oldPPrime = oldP.inverted();
        Probability newPPrime = newP.inverted();
        Quantity val = new Quantity((.3 * .8) / (.2 * .7));
        assertEquals(0, newP.times(oldPPrime).div(oldP.times(newPPrime)).compareTo(val.round(Quantity.NINE_DIGITS)));
    }

    public void testPrinting() {
        Probability fivePercent = new Probability(".05");
        Probability ohOhFivePercent = new Probability(".005");
        assertEquals("0.05", fivePercent.toString());
        assertEquals("0.005", ohOhFivePercent.toString());
        assertEquals("5", fivePercent.printAsIntegerPercent());
        assertEquals("37", new Probability(".3678").printAsIntegerPercent());
        assertEquals("0", ohOhFivePercent.printAsIntegerPercent());
    }

}