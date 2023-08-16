package net.commerce.zocalo.ajax.events;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.  
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.IncompatibleOrderException;
import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.ajax.dispatch.MockBayeux;
import net.commerce.zocalo.ajax.dispatch.BidUpdateDispatcher;
import net.commerce.zocalo.ajax.dispatch.MockBayeuxChannel;
import net.commerce.zocalo.ajax.dispatch.PriceActionAppender;
import net.commerce.zocalo.ajax.dispatch.TradeEventDispatcher;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import org.mortbay.cometd.AbstractBayeux;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

public class LiveStripTest extends OrdersTestCase {
    private String liveURI;
    private String historicURI;

    protected void setUp() throws Exception {
        super.setUp();
        liveURI = BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI;
        historicURI = TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX;
        HibernateTestUtil.resetSessionFactory();
    }

    public void testLiveStripReceivesFirstAction() throws DuplicateOrderException, IncompatibleOrderException {
        Price price = Price.dollarPrice(30);
        MockBayeux bayeux = new MockBayeux();
        String name = "seller";
        PriceActionAppender.registerNewAppender(bayeux, book, makeUpdaterCallback());
        book.addOrder(no, price.inverted(), q(20), makeUser(name));

        MockBayeuxChannel channel = (MockBayeuxChannel) bayeux.getChannel(liveURI, false);
        List events = channel.getEvents(liveURI);
        assertEquals(2, events.size());
        Object event =  events.iterator().next();
        assertTrue(event instanceof Map);
        Map e = (Map) event;
        assertEquals("30", e.get("sell"));
    }

    public void testLiveStripReceivesActionWithOtherBidsPresent() throws DuplicateOrderException, IncompatibleOrderException {
        final Quantity quantity = q(20.0);
        MockBayeux bayeux = new MockBayeux();
        PriceActionAppender.registerNewAppender(bayeux, book, makeUpdaterCallback());

        book.addOrder(no, Price.dollarPrice(20), quantity, makeUser("seller1"));
        book.addOrder(yes, Price.dollarPrice(45), quantity, makeUser("buyer2"));
        book.addOrder(no, Price.dollarPrice(30), quantity, makeUser("seller2"));
        book.addOrder(yes, Price.dollarPrice(30), quantity, makeUser("buyer1"));

        MockBayeuxChannel channel = (MockBayeuxChannel) bayeux.getChannel(liveURI, false);
        List events = channel.getEvents(liveURI);
        assertEquals(7, events.size());
        Object event =  events.get(6);
        assertTrue(event instanceof Map);
        Map e = (Map) event;
        assertEquals("80,70", e.get("sell"));
        assertEquals("30,45", e.get("buy"));
    }

    public void testXaction() throws DuplicateOrderException, IncompatibleOrderException {
        AbstractBayeux bayeux = new MockBayeux();
        MockBayeuxChannel historyChannel = (MockBayeuxChannel)bayeux.getChannel(historicURI, true);
        MockBayeuxChannel livingChannel = (MockBayeuxChannel)bayeux.getChannel(liveURI, true);
        PriceActionAppender.registerNewAppender(bayeux, book, makeUpdaterCallback());

        book.addOrder(no, Price.dollarPrice(20), Quantity.ONE, makeUser("seller1"));
        List events = livingChannel.getEvents(liveURI);
        assertNotNull(events);
        assertEquals(2, events.size());
        Iterator eventIter = events.iterator();
        assertTrue(eventIter.next() instanceof Map);
        Map event = (Map) eventIter.next();
        assertEquals("80", event.get("sell"));

        book.addOrder(yes, Price.dollarPrice(45), Quantity.ONE, makeUser("buyer2"));
        book.addOrder(yes, Price.dollarPrice(30), Quantity.ONE, makeUser("buyer1"));

        eventIter = events.iterator();
        while(eventIter.hasNext()) {
            event = (Map) eventIter.next();
        }
        assertEquals(5, events.size());
        assertEquals("30,45", event.get("buy"));

        market.marketOrder(no, Price.dollarPrice(55), Quantity.ONE, makeUser("seller2"));

        List tradeEvents = historyChannel.getEvents(historicURI);
        assertEquals(2, tradeEvents.size());
        Object laterEv = tradeEvents.iterator().next();
        assertTrue(laterEv instanceof Map);
        Map later = (Map) laterEv;
        assertQEquals(45, (Quantity)later.get("traded"));
    }
}
