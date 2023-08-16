package net.commerce.zocalo.JspSupport.printOrders;

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.IncompatibleOrderException;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.html.HtmlElement;
import net.commerce.zocalo.JspSupport.MarketDisplay;
import net.commerce.zocalo.orders.OrdersTestCase;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.orders.SortedOrders;

import java.util.regex.Pattern;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class OrdersPrintStrategyTest extends OrdersTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testRenderPQCancelCell() throws DuplicateOrderException, IncompatibleOrderException {
        final User buyer = makeUser("buyer");
        final StringBuffer buf = new StringBuffer();
        addOrder(book, yes, "30", 10.0, buyer);
        OrdersPrintStrategy strat = new PQOrdersPrintStrategy("limegreen", "blue", "target");

        Order order = yesOrders.getBest();
        strat.renderRow(buf, order, true, market);
        assertEquals("<tr bgcolor=limegreen>\n<td align=center>10</td>\n<td align=center><form method=POST action='target?userName=buyer' autocomplete=\"off\">" +
                "<input type=hidden name='deleteOrderPrice' value='30'>" +
                "<input type=hidden name='deleteOrderPosition' value='buy'>" +
                "<input type=hidden name='userName' value='buyer'><input type=hidden name='claimName' value='silly'>" +
                "<input type=submit class='smallFontButton' style=\"background-color:limegreen;\" name=action value='30'>&cent;\n</form>\n</td>\n</tr>\n",
                buf.toString());
    }

    public void testRenderNoCancelCell() throws DuplicateOrderException, IncompatibleOrderException {
        final User buyer = makeUser("buyer");
        final StringBuffer buf = new StringBuffer();
        addOrder(book, yes, "30", 10.0, buyer);
        OrdersPrintStrategy strat = new NoCancelOrdersPrintStrategy("lightGreen", "orange");

        Order order = yesOrders.getBest();
        strat.renderRow(buf, order, true, market);
        assertEquals("<tr>\n<td align=center>10 @ 30&cent;</td>\n</tr>\n",
                buf.toString());
    }

    public void testRenderPriceOnlyCell() throws DuplicateOrderException, IncompatibleOrderException {
        final User buyer = makeUser("buyer");
        final StringBuffer buf = new StringBuffer();
        addOrder(book, yes, "30", 10.0, buyer);
        OrdersPrintStrategy strat = new PriceOnlyOrdersPrintStrategy("limegreen", "orange", "foo", 3);

        Order order = yesOrders.getBest();
        strat.renderRow(buf, order, true, market);
        assertEquals("<tr bgcolor=limegreen>\n<td align=center><form method=POST action='foo?userName=buyer' autocomplete=\"off\">" +
                "<input type=hidden name='deleteOrderPrice' value='30'>" +
                "<input type=hidden name='deleteOrderPosition' value='buy'>" +
                "<input type=hidden name='userName' value='buyer'>" +
                "<input type=hidden name='claimName' value='silly'>" +
                "<input type=submit class='smallFontButton' style=\"background-color:limegreen;\" name=action value='30'>&cent;\n</form>\n</td>\n</tr>\n",
                buf.toString());
    }

    public void testPriceOnlyWithOneOrder() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        Order order = addOrder(book, yes, "40", 10.0, buyer);
        SortedOrders orders = (SortedOrders) book.getOffers(yes);
        String expected = "<td align=center><form method=POST action='foo?userName=buyer' autocomplete=\"off\"><input type=hidden name='deleteOrderPrice' value='40'><input type=hidden name='deleteOrderPosition' value='sell'><input type=hidden name='userName' value='buyer'><input type=hidden name='claimName' value='silly'><input type=submit class='smallFontButton' style=\"background-color:orange;\" name=action value='40'>&cent;\n</form>\n</td>";

        StringBuffer strategyBuf = new StringBuffer();
        OrdersPrintStrategy priceOnlyStrat = new PriceOnlyOrdersPrintStrategy("limegreen", "orange", "foo", 3);
        HtmlElement[] cells = priceOnlyStrat.renderAsCells(order, false, market);
        assertEquals(1, cells.length);
        cells[0].render(strategyBuf);
        assertEquals(expected, strategyBuf.toString());

        StringBuffer oldStyleBuf = new StringBuffer();
        MarketDisplay.printNBestOrders(oldStyleBuf, buyer, true, "foo", 3, market, orders);
        assertEquals(expected, oldStyleBuf.toString());
    }

    public void testPQWithOneOrder() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        Order order = addOrder(book, yes, "40", 10.0, buyer);
        SortedOrders orders = (SortedOrders) book.getOffers(yes);
        String quantCell = "\n<td align=center>10</td>";
        String expectedStart = "<tr bgcolor=limegreen>";
        String priceCell = "\n<td align=center><form method=POST action='foo?userName=buyer' autocomplete=\"off\">" +
                "<input type=hidden name='deleteOrderPrice' value='40'><input type=hidden name='deleteOrderPosition' value='buy'>" +
                "<input type=hidden name='userName' value='buyer'><input type=hidden name='claimName' value='silly'>" +
                "<input type=submit class='smallFontButton' style=\"background-color:limegreen;\" name=action value='40'>&cent;\n</form>\n</td>";
        String expectedEnd = "\n</tr>\n";
        String expectedOld = expectedStart + priceCell + quantCell + expectedEnd;
        String expectedNew = expectedStart + quantCell + priceCell + expectedEnd;

        StringBuffer strategyBuf = new StringBuffer();
        OrdersPrintStrategy pqStrat = new PQOrdersPrintStrategy("limegreen", "orange", "foo");
        pqStrat.renderRow(strategyBuf, order, true, market);
        assertEquals(expectedNew, strategyBuf.toString());

        StringBuffer oldStyleBuf = new StringBuffer();
        MarketDisplay.printRowsFor(oldStyleBuf, buyer, orders, market, false, "foo");
        assertEquals(expectedOld, oldStyleBuf.toString());
    }

    public void testNoCancelWithOneOrder() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        Order order = addOrder(book, yes, "40", 10.0, buyer);
        String prefix = "<tr>\n";
        String postFix    = "<td align=center>10 @ 40&cent;</td>\n</tr>\n";
        String oldPostFix = "<td align=center>10 @ 40</td>\n</tr>\n";
        String claimNameCell = "<td align=center>silly:yes</td>\n";
        String rowStringWithName = prefix + claimNameCell + oldPostFix;
        String rowString = prefix + postFix;

        StringBuffer strategyBuf = new StringBuffer();
        OrdersPrintStrategy noCancelStrat = new NoCancelOrdersPrintStrategy("lightGreen", "orange");
        noCancelStrat.renderRow(strategyBuf, order, true, market);
        assertEquals(rowString, strategyBuf.toString());

        StringBuffer oldStyleBuf = new StringBuffer();
        OrdersPrintStrategy.displayOrders(oldStyleBuf, market, buyer);
        assertREMatches("<center><h2>Outstanding Orders</h2></center>\n<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}bgcolor='lightBlue'>\n" +
                "<tr><th>Claim Name</th><th>Order</th></tr>\n" + rowStringWithName + "</table><p>",
                oldStyleBuf.toString());
    }

    public void testPriceOnlyOrderedDisplayYesOrders() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        addOrder(book, yes, "40", 10.0, buyer);
        addOrder(book, yes, "30", 0.0, buyer);
        addOrder(book, yes, "50", 40, buyer);
        addOrder(book, yes, "20", 23, buyer);
        String expected = ".*value='20'>&cent;\n.*value='40'>&cent;\n.*value='50'>&cent;.*";

        String oldStyleString = MarketDisplay.printHorizontalPriceTable(market, buyer);
        assertPatternMatches(expected, oldStyleString);

        StringBuffer strategyBuf = new StringBuffer();
        PriceOnlyOrdersPrintStrategy priceOnlyStrat = new PriceOnlyOrdersPrintStrategy("limegreen", "orange", "TraderSubSubFrame.jsp", 3);
        priceOnlyStrat.renderTable(strategyBuf, buyer, market);
        assertPatternMatches(expected, strategyBuf.toString());

        assertEquals(oldStyleString, strategyBuf.toString());
    }

    public void testPriceOnlyOrderedDisplayBothOrders() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        addOrder(book, yes, "40", 10.0, buyer);
        addOrder(book, yes, "30", 0.0, buyer);
        addOrder(book, yes, "50", 40, buyer);
        addOrder(book, yes, "20", 23, buyer);
        addOrder(book, no, "30", 50, buyer);
        addOrder(book, no, "15", 15, buyer);
        String expected = ".*value='20'>&cent;\n.*value='40'>&cent;\n.*value='50'>&cent;\n.*value='70'>&cent;\n.*value='85'>&cent;.*";

        String oldStyleString = MarketDisplay.printHorizontalPriceTable(market, buyer);
        assertPatternMatches(expected, oldStyleString);

        StringBuffer strategyBuf = new StringBuffer();
        PriceOnlyOrdersPrintStrategy priceOnlyStrat = new PriceOnlyOrdersPrintStrategy("limegreen", "orange", "TraderSubSubFrame.jsp", 3);
        priceOnlyStrat.renderTable(strategyBuf, buyer, market);
        assertPatternMatches(expected, strategyBuf.toString());

        assertEquals(oldStyleString, strategyBuf.toString());
    }

    private void assertPatternMatches(String expected, String oldStyleString) {
        Pattern exp = Pattern.compile(expected, Pattern.DOTALL);
        assertTrue("'" + expected + "'" + "\n\n doesn't match: " + oldStyleString, exp.matcher(oldStyleString).matches());
    }

    public void testPQOrderedDisplay() throws DuplicateOrderException, IncompatibleOrderException {
        String targetPage = "target";
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        addOrder(book, yes, "40", 10.0, buyer);
        addOrder(book, yes, "50", 40, buyer);
        addOrder(book, yes, "20", 23, buyer);
        addOrder(book, no, "30", 50, buyer);
        addOrder(book, no, "15", 15, buyer);
        SortedOrders orders = (SortedOrders) book.getOffers(yes);

        StringBuffer oldStyleBuf = new StringBuffer();
        String expected = ".*value='85'.*background-color:orange.*value='85'.*" +
                "value='70'.*background-color:orange.*value='70'.*" +
                "value='50'.*background-color:limegreen.*value='50'.*" +
                "value='40'.*background-color:limegreen.*value='40'.*" +
                "value='20'.*background-color:limegreen.*value='20'.*";
        MarketDisplay.printOrdersTable(market, oldStyleBuf, buyer, targetPage);
        assertPatternMatches(expected, oldStyleBuf.toString());

        StringBuffer strategyBuf = new StringBuffer();
        PQOrdersPrintStrategy pqStrat = new PQOrdersPrintStrategy("limegreen", "orange", targetPage);
        pqStrat.renderTable(strategyBuf, buyer, market);
        assertPatternMatches(expected, strategyBuf.toString());

//        assertEquals(oldStyleBuf.toString(), strategyBuf.toString());
    }

    public void testNoCancelOrderedDisplay() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(book.iterateOffers(yes).hasNext());
        User buyer = makeUser("buyer");
        addOrder(book, yes, "40", 10, buyer);
        addOrder(book, yes, "50", 10, buyer);
        addOrder(book, yes, "20", 10, buyer);
        addOrder(book, no,  "30", 10, buyer);
        addOrder(book, no,  "15", 10, buyer);

        StringBuffer oldStyleBuf = new StringBuffer();
        String oldExpected = ".*silly:yes.*silly:yes.*silly:yes.*";
        OrdersPrintStrategy.displayOrders(oldStyleBuf, market, buyer);
        String nameListString = oldStyleBuf.toString();
        assertPatternMatches(oldExpected, nameListString);
        assertTrue(nameListString.indexOf("85") > 0);
        assertTrue(nameListString.indexOf("40") > 0);
        assertTrue(nameListString.indexOf("70") > 0);
        assertTrue(nameListString.indexOf("50") > 0);

        String expected = ".*85.*70.*50.*40.*20.*";
        StringBuffer strategyBuf = new StringBuffer();
        NoCancelOrdersPrintStrategy noCancelStrat = new NoCancelOrdersPrintStrategy("lightGreen", "orange");
        noCancelStrat.renderTable(strategyBuf, book, market);
        assertPatternMatches(expected, strategyBuf.toString());
    }
}
