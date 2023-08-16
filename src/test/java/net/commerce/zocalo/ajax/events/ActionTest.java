package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Logger;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;

public class ActionTest extends PersistentTestHelper {
    private final Logger logger = Logger.getLogger("test");

    private Price price;
    private Quantity quantity;
    private Ask ask1;
    private Ask ask2;
    private Bid bid1;
    private Bid bid2;

    protected void setUp() throws Exception {
        BinaryClaim claim = BinaryClaim.makeClaim("actionable", new User("joe", null), "a claim");

        final String owner = "someone";
        price = Price.dollarPrice(30);
        quantity = q(2);
        Position yes = claim.getYesPosition();
        ask1 = new Ask(owner, price, Quantity.ONE, yes, logger);
        ask2 = new Ask(owner, price, quantity, yes, logger);
        bid1 = new Bid(owner, price, Quantity.ONE, yes, logger);
        bid2 = new Bid(owner, price, quantity, yes, logger);
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testAsk() {
        assertQEquals(price, ask1.getPrice());
        assertQEquals(price, ask2.getPrice());
        assertQEquals(quantity, ask2.getQuantity());
    }

    public void testPrintAsk() {
        assertREMatches("\\d+# someone added Sell at 30", ask1.toLogString());
        assertREMatches("\\d+# someone added Sell for 2( of .*)? at 30", ask2.toLogString());
    }

    public void testBid() {
        assertQEquals(price, bid1.getPrice());
        assertQEquals(price, bid2.getPrice());
        assertQEquals(quantity, bid2.getQuantity());
    }

    public void testPrintBid() {
        assertREMatches("\\d+# someone added Buy( of .*)? at 30", bid1.toLogString());
        assertREMatches("\\d+# someone added Buy for 2( of .*)? at 30", bid2.toLogString());
    }

    public void testTrade() {
        BinaryClaim claim = BinaryClaim.makeClaim("tradableClaim", new User("joe", null), "a claim");

        BookTrade trade = BookTrade.newBookTrade("buyer", Price.dollarPrice(70), q(5), claim.getYesPosition());
        final String expected = "\\d+# buyer accepted offer for 5( of .*)? at 70";
        assertREMatches(expected, trade.toLogString());
    }

    public void testStartTrading() {
        String transitionName = "startTransition";
        String label = "Start Trading";
        String expected = "\\d+# State transition: " + transitionName +", round: 2.";

        Logger logger = Logger.getLogger("testLogger");
        MockAppender appender = new MockAppender();
        logger.addAppender(appender);
        Transition start = new Transition(transitionName, label, 2, "3457");
        assertREMatches(expected, start.toLogString());

        logger.callAppenders(start);
        Action action = (Action) appender.getEvents().iterator().next();
        assertREMatches(expected, action.toString());
    }
}
