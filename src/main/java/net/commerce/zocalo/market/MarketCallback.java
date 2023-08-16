package net.commerce.zocalo.market;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MarketCallback {
    public void binaryMarket(BinaryMarket mkt) { /* do nothing unless overridden */ }
    public void multiMarket(MultiMarket mkt) { /* do nothing unless overridden */ }
    public void unaryMarket(UnaryMarket mkt) {
    }
}
