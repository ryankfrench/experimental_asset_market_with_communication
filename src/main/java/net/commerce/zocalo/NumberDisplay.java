package net.commerce.zocalo;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.text.NumberFormat;
import java.util.Locale;

/** support for displaying Numbers as prices, quantities, or scores.  Understands
    how to format floating point numbers reasonably. */
public class NumberDisplay {
    final static private NumberFormat f;

    static {
        f = NumberFormat.getInstance(Locale.US);
        f.setGroupingUsed(false);
    }

    static public String printAsPrice(double price) {
        String formatted = f.format(price);
        if ("-0".equals(formatted)) {
            return "0";
        }
        return formatted;
    }

    static public String printAsQuantity(double quant) {
        double rounded = Math.round(quant * 1000.0);
        double quantAgain = rounded/1000.0;

        String formatted = f.format(quantAgain);
        if ("-0".equals(formatted)) {
            return "0";
        }
        return formatted;
    }

    static public String printAsScore(double score) {
        return f.format(score);
    }
}
