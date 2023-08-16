package net.commerce.zocalo.JspSupport.printOrders;

import net.commerce.zocalo.html.HtmlElement;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.JspSupport.printOrders.OrdersPrintStrategy;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.orders.SortedOrders;
// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** OrdersPrintStrategy for display that shows Price and Quantity. */
public class PQOrdersPrintStrategy extends OrdersPrintStrategy {
    public PQOrdersPrintStrategy(String buyColor, String sellColor, String targetPage) {
        super(buyColor, sellColor, targetPage);
    }

    public HtmlElement[] renderAsCells(Order order, boolean buying, Market market) {
        return renderAsPQCells(order, buying, market);
    }

    HtmlRow createRow(HtmlElement[] cells, boolean buying) {
        return createColoredRow(buying, cells);
    }

    public void renderTable(StringBuffer buf, User owner, Market market) {
        buf.append("<table id='orders_table' align='center' border='0' cellspacing='2'>\n");
        buf.append("<tr><th colspan=2>Your orders</th></tr>\n");
        Book book = market.getBook();
        SortedOrders ordersForNo = book.getOffers(((BinaryClaim) book.getClaim()).getNoPosition());
        if (ordersForNo != null) {
            printOrdersHeader(buf, "Offers to Sell");
            renderRows(buf, ordersForNo, owner, false, market);
        }
        buf.append("<tr><td colspan=2>&nbsp;</td></tr>\n");
        SortedOrders ordersForYes = book.getOffers(((BinaryClaim) book.getClaim()).getYesPosition());
        if (ordersForYes != null) {
            renderRows(buf, ordersForYes, owner, true, market);
            printOrdersHeader(buf, "Offers to Buy");
        }
        buf.append("\n</table>");
        buf.append("<center><small>click on an order to cancel it.</small></center>");
    }

    private void renderRows(StringBuffer buf, SortedOrders orders, User owner, boolean buying, Market market) {
        Order[] usersOrders = orders.usersOwnOrders(owner, ! buying);
        for (int i = 0; i < usersOrders.length; i++) {
            Order order = usersOrders[i];
            renderRow(buf, order, buying, market);
        }
    }

    private void printOrdersHeader(StringBuffer buf, String ordersLabel) {
        buf.append("<tr><td align=center colspan=2><font size=1>");
        buf.append(ordersLabel);
        buf.append("</font></td></tr>\n");
    }
}
