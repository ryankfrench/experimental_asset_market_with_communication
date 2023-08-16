package net.commerce.zocalo.ajax.dispatch;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.ajax.events.*;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;

import java.io.ByteArrayOutputStream;
import java.io.StringBufferInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class PriceActionAppenderTest extends OrdersTestCase {
    private final Logger logger = Logger.getLogger("test");

    private Quantity quantity;
    private MockBayeuxChannel livingChannel;
    private MockBayeuxChannel historyChannel;
    private String liveURIString;
    private String historicURIString;

    public void setUp() throws Exception {
        super.setUp();
        quantity = Quantity.ONE;
        URI liveURI = URI.create(BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI);
        URI historicalURI = URI.create(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX);
        liveURIString = liveURI.toString();
        historicURIString = historicalURI.toString();

        livingChannel = (MockBayeuxChannel) mockBayeux.getChannel(liveURIString, true);
        historyChannel = (MockBayeuxChannel) mockBayeux.getChannel(historicURIString, true);
        book.addOrder(no, Price.dollarPrice(20), quantity, makeUser("seller1"));
        book.addOrder(yes, Price.dollarPrice(45), quantity, makeUser("buyer2"));
        book.addOrder(yes, Price.dollarPrice(30), quantity, makeUser("buyer1"));
        HibernateTestUtil.resetSessionFactory();
    }

    public void testPriceActionAppender() {
        Ask ask = new Ask("seller", Price.dollarPrice(80), quantity, yes, logger);
        chart.processAction(ask);

        assertEquals(1, livingChannel.getEvents(liveURIString).size());
    }

    public void testXaction() {
        Trade trade = new BookTrade("buyer", Price.dollarPrice(70), quantity, yes, logger);
        chart.processAction(trade);

        List liveEvents = livingChannel.getEvents(liveURIString);
        assertEquals(1, liveEvents.size());
        Map liveEvent = (Map)liveEvents.iterator().next();
        assertEquals(null, liveEvent.get("traded"));
        assertEquals("80", liveEvent.get("sell"));
        assertEquals("30,45", liveEvent.get("buy"));

        List historicEvents = historyChannel.getEvents(historicURIString);
        assertEquals(1, historicEvents.size());
        Map historicEvent = (Map)historicEvents.iterator().next();
        assertQEquals(70, (Quantity)historicEvent.get("traded"));
        assertEquals("0", historicEvent.get("round"));
    }

    public void testBid() {
        PriceAction bid = new Bid("buyer", Price.dollarPrice(45), quantity, yes, logger);
        chart.processAction(bid);

        List events = livingChannel.getEvents(liveURIString);
        assertEquals(1, events.size());
        for (Iterator iterator = events.iterator(); iterator.hasNext();) {
            Map event = (Map) iterator.next();
            if (event.get("traded") != null) {
                fail();
            }

            assertEquals("80", event.get("sell"));
            assertEquals("30,45", event.get("buy"));
        }
    }

    public void testLogBidToPriceActionAppender() {
        Logger eventLogger = Logger.getLogger(PriceActionAppender.class);
        eventLogger.addAppender(chart);
        assertEquals(0, livingChannel.getEvents(liveURIString).size());
        new Bid("buyer", Price.dollarPrice(20), q(30), yes, eventLogger);

        assertEquals(0, historyChannel.getEvents(historicURIString).size());
        assertEquals(1, livingChannel.getEvents(liveURIString).size());
    }

    public void testLogTradeToPriceActionAppender() {
        Logger eventLogger = Logger.getLogger(PriceActionAppender.class);
        eventLogger.addAppender(chart);
        assertEquals(0, livingChannel.getEvents(liveURIString).size());
        assertEquals(0, historyChannel.getEvents(historicURIString).size());

        new BookTrade("acceptor", Price.dollarPrice(40), q(70), yes, eventLogger);
        assertEquals(1, livingChannel.getEvents(liveURIString).size());
        assertEquals(1, historyChannel.getEvents(historicURIString).size());

        List liveEvents = livingChannel.getEvents(liveURIString);
        assertEquals(1, liveEvents.size());
        Map liveEvent = (Map) liveEvents.iterator().next();
        assertEquals("0", liveEvent.get("round"));
        List historicEvents = historyChannel.getEvents(historicURIString);
        assertEquals(1, historicEvents.size());
        Map historicEvent = (Map) liveEvents.iterator().next();
        assertEquals("0", historicEvent.get("round"));
    }

    public void testPrintActionAsString() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        WriterAppender appender = new WriterAppender(new SimpleLayout(), stream);
        logger.addAppender(appender);
        new Bid("buyer", Price.dollarPrice(45), q(80), yes, logger);
        assertREMatches("INFO - \\d+# buyer added Buy for 80 of silly:yes at 45\n", stream.toString());
    }

    public void testDropAppendersAtSessionEnd() throws IOException {
        String script =
            "sessionTitle: dropAppendersAtSessionEndTest\n" +
            "rounds: 3\n" +
            "players: traderA, traderB, judgeD\n" +
            "timeLimit: 5\n" +
            "traderA.role: trader\n" +
            "traderB.role: trader\n" +
            "judgeD.role: judge\n" +
            "endowment.trader: 30\n" +
            "endowment.manipulator: 50\n" +
            "tickets.trader: 30\n" +
            "tickets.manipulator: 20\n" +
            "scoringFactor.judge: 0.02\n" +
            "scoringConstant.judge: 250\n" +
            "scoringFactor.manipulator: 2\n" +
            "scoringConstant.manipulator: 200\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          0,          100,         40\n" +
            "traderA.hint:         not100,     not40,       not100\n" +
            "traderB.hint:         not40,      notZero,     notZero\n";

        Ask ask = new Ask("seller", Price.dollarPrice(80), quantity, yes, logger);
        chart.processAction(ask);

        assertEquals(1, livingChannel.getEvents(liveURIString).size());

        Properties props = new Properties();
        props.load(new StringBufferInputStream(script));
        SessionSingleton.setSession(props, null);
        SessionSingleton.getSession();

        Bid bid = new Bid("buyer", Price.dollarPrice(30), quantity, yes, logger);
        chart.processAction(bid);

        MockBayeuxChannel newLiveChannel = (MockBayeuxChannel) mockBayeux.getChannel(liveURIString, true);
        assertEquals(2, newLiveChannel.getEvents(liveURIString).size());
    }
}
