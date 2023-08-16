package net.commerce.zocalo.ajax.events;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.ajax.dispatch.*;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Dictionary;
import java.util.Hashtable;

public class TransitionTest extends PersistentTestHelper {
    private String testLabel = "testing";
    private String testMessage = "testing displayed messages";
    private String expected = "\\d+# State transition: " + testLabel + ", round: 2.";

    public void setUp() {
        Log4JHelper.getInstance();
    }

    public void testMessage() {
        Logger logger = Transition.getActionLogger();
        MockAppender appender = new MockAppender();
        logger.addAppender(appender);

        Transition t = new Transition(testLabel, "foo", 2, "3:57");

        assertREMatches(expected, t.toLogString());

        logger.callAppenders(t);
        Action action = (Action) appender.getEvents().iterator().next();
        assertREMatches(expected, action.toString());
    }

    public void testDispatch() throws MalformedURLException {
        MockBayeux bayeux = new MockBayeux();
        String transitionURI = TransitionDispatcher.TRANSITION_TOPIC_URI;
        MockBayeuxChannel channel = (MockBayeuxChannel) bayeux.getChannel(transitionURI, true);
        TransitionAppender tAppender = new TransitionAppender(bayeux);
        HibernateTestUtil.resetSessionFactory();
        Logger eventLogger = Transition.getActionLogger();
        eventLogger.addAppender(tAppender);
        assertEquals(0, channel.getEvents(transitionURI).size());
        new Transition(testMessage, "bar", 2, "3:57");

        List transitionEvents = channel.getEvents(transitionURI);
        assertEquals(1, transitionEvents.size());
    }

    public void testNewChartMessage() {
        Logger logger = NewChart.getNewChartLogger();
        MockAppender appender = new MockAppender();
        logger.addAppender(appender);
        NewChart chart = new NewChart("football");
        String expectedString = "The chart for football has been updated.";
        assertMatches(expectedString, chart.toLogString());

        logger.callAppenders(chart);
        NewChart NewChart = (NewChart) appender.getEvents().iterator().next();
        assertREMatches(expectedString, NewChart.toString());
    }

    public void testNewChartDispatch() throws MalformedURLException {
        MockBayeux bayeux = new MockBayeux();
        String marketName = "newChartTest";
        String newChartURI = NewChartDispatcher.buildChannelName(marketName, NewChartDispatcher.NEW_CHART_TOPIC_URI);
        MockBayeuxChannel channel = (MockBayeuxChannel) bayeux.getChannel(newChartURI, true);
        NewChartAppender.registerNewAppender(bayeux, marketName);
        HibernateTestUtil.resetSessionFactory();
        assertEquals(0, channel.getEvents(newChartURI).size());
        new NewChart("football");

        List chartEvents = channel.getEvents(newChartURI);
        assertEquals(1, chartEvents.size());
    }

    public void testPriceChangeMessage() {
        PriceChange change = buildProbList("football");
        Logger logger = PriceChange.getPriceChangeLogger();
        MockAppender appender = new MockAppender();
        logger.addAppender(appender);

        String expectedString = "prices in market 'football' changed.  (football:yes: 23|, |football:no: 77){3}";
        assertREMatches(expectedString, change.toLogString());

        logger.callAppenders(change);
        PriceChange priceChange = (PriceChange) appender.getEvents().iterator().next();
        assertREMatches(expectedString, priceChange.toString());
    }

    private PriceChange buildProbList(String marketName) {
        CashBank rootBank = new CashBank("cash");
        Dictionary<Position, Probability> probs = new Hashtable<Position, Probability>();
        BinaryClaim football = BinaryClaim.makeClaim("football", rootBank.makeEndowedUser("Candace", q(1000)), "Nebraska will win");
        probs.put(football.getYesPosition(), new Probability(.23));
        probs.put(football.getNoPosition(), new Probability(.77));

        return new PriceChange(marketName, probs);
    }

    public void testPriceChangeDispatch() throws MalformedURLException {
        MockBayeux bayeux = new MockBayeux();
        String testMktName = "testMarket";
        String PriceChangeURI = Dispatcher.buildChannelName(testMktName, PriceChangeDispatcher.PRICE_CHANGE_TOPIC_URI);
        MockBayeuxChannel channel = (MockBayeuxChannel) bayeux.getChannel(PriceChangeURI, true);
        PriceChangeAppender.registerNewAppender(bayeux, testMktName);
        HibernateTestUtil.resetSessionFactory();
        assertEquals(0, channel.getEvents(PriceChangeURI).size());

        buildProbList("football");
        List chartEvents = channel.getEvents(PriceChangeURI);
        assertEquals(0, chartEvents.size());

        buildProbList(testMktName);
        chartEvents = channel.getEvents(PriceChangeURI);
        assertEquals(1, chartEvents.size());
    }
}
