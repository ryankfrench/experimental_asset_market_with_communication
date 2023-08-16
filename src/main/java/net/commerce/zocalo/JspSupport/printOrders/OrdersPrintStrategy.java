package net.commerce.zocalo.JspSupport.printOrders;

import net.commerce.zocalo.html.HtmlElement;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.market.Market;

import java.util.Iterator;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Embodies strategies for displaying orders during an experiment.
Alternative choices include displaying horizontally or vertically; using text,
html, or graphics; displaying as a button that can be used to cancel the order;
including price and quantity or just one; and including all open orders or just
those belonging to a particular user. */
public abstract class OrdersPrintStrategy {
    protected String buyColor;
    protected String sellColor;
    protected String targetPage;

    protected OrdersPrintStrategy(String buyColor, String sellColor, String targetPage) {
        this.buyColor = buyColor;
        this.sellColor = sellColor;
        this.targetPage = targetPage;
    }

    abstract HtmlElement[] renderAsCells(Order order, boolean buying, Market market);

    public String getBGColor() { return buyColor; }

    static public PQOrdersPrintStrategy makePQ(String buyColor, String sellColor, String targetPage) {
        return new PQOrdersPrintStrategy(buyColor, sellColor, targetPage);
    }

    public static NoCancelOrdersPrintStrategy makeNoCancel(String buyColor, String sellColor) {
        return new NoCancelOrdersPrintStrategy(buyColor, sellColor);
    }

    public void renderRow(StringBuffer buf, Order order, boolean buying, Market market) {
        HtmlElement[] cells = renderAsCells(order, buying, market);
        HtmlRow row = createRow(cells, buying);
        row.render(buf);
    }

    /** default implementation */
    HtmlRow createRow(HtmlElement[] cells, boolean buying) {
        return new HtmlRow(cells);
    }

    /** an implementation of renderAsCells() that renders as priceCell (with cancel) plus quantity */
    HtmlElement[] renderAsPQCells(Order order, boolean buying, Market market) {
        HtmlElement[] cells = new HtmlElement[2];
        cells[0] = HtmlSimpleElement.centeredCell(order.quantity().printAsQuantity());
        cells[1] = HtmlSimpleElement.priceCell(order, buying, targetPage, market);
        return cells;
    }

    public HtmlElement[] renderAsPQNoCancelCells(Order order, Market market) {
        String quantString = order.quantity().printAsQuantity();
        String priceString = order.naturalPrice().printAsWholeCents();
        HtmlSimpleElement cell = HtmlSimpleElement.centeredCell(quantString + " @ " + priceString + "&cent;");
        return new HtmlElement[] { cell };
    }

    /** an implementation of renderAsCells() that renders only the price */
    HtmlElement[] renderAsPriceCell(Order order, String targetPage, boolean buying, Market market) {
        return new HtmlElement [] { HtmlSimpleElement.priceCell(order, buying, targetPage, market) };
    }

    /** an implementation of createRow() that uses BUYING to decide how to color the row */
    HtmlRow createColoredRow(boolean buying, HtmlElement[] cells) {
        return new HtmlRow("bgcolor=" + (buying ? buyColor : sellColor), cells);
    }

    static public void displayOrders(StringBuffer buf, Market market, User user) {
        displayOrders(buf, null, market, user);
    }

    static public void displayOrders(StringBuffer buf, Claim claim, Market market, User user) {
        Iterator iterator = user.getOrders().iterator();
        if (iterator.hasNext()) {
            HtmlSimpleElement.printHeader(buf, 2, "Outstanding Orders");
            HtmlTable.start(buf, "lightBlue", new String[] { "Claim Name", "Order" } );
            while (iterator.hasNext()) {
                Order order = (Order) iterator.next();
                if (claim == null || order.position().getClaim() == claim) {
                    HtmlSimpleElement.printAccountsTableRow(buf, order, market);
                }
            }
        } else {
            HtmlSimpleElement.printEmptyOrders(buf, "Outstanding Orders");
        }
        HtmlTable.endTagWithP(buf);
    }
}
