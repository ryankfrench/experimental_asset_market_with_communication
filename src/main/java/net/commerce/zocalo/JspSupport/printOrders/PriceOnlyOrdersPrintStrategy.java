package net.commerce.zocalo.JspSupport.printOrders;

import net.commerce.zocalo.html.HtmlElement;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.JspSupport.printOrders.OrdersPrintStrategy;
import net.commerce.zocalo.JspSupport.MarketDisplay;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.orders.SortedOrders;
// Copyright 2007-2009 Chris Hibbert.  Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

class PriceOnlyOrdersPrintStrategy extends OrdersPrintStrategy {
    private String targetPage;
    private int nBest;

    public PriceOnlyOrdersPrintStrategy(String buyColor, String sellColor, String targetPage, int nBest) {
        super(buyColor, sellColor, targetPage);
        this.targetPage = targetPage;
        this.nBest = nBest;
    }

    public HtmlElement[] renderAsCells(Order order, boolean buying, Market market) {
        return renderAsPriceCell(order, targetPage, buying, market);
    }

    HtmlRow createRow(HtmlElement[] cells, boolean buying) {
        return createColoredRow(buying, cells);
    }

    public void renderTable(StringBuffer buf, User owner, Market market)  {
        StringBuffer rowBuffer = new StringBuffer();
        Book book = market.getBook();
        BinaryClaim claim = (BinaryClaim) book.getClaim();
        SortedOrders offersForYes = book.getOffers(claim.getYesPosition());
        SortedOrders offersForNo = book.getOffers(claim.getNoPosition());

        boolean yesButtonsExist = printNBestOrders(rowBuffer, offersForYes, true, owner, market);
        rowBuffer.append("<td>&nbsp;</td>\n");
        boolean noButtonsExist = printNBestOrders(rowBuffer, offersForNo, false, owner, market);

        if (yesButtonsExist || noButtonsExist) {
            buf.append("<table align=center id='orders_table' border='0' cellpadding=2 cellspacing='2'>\n");
            buf.append("<tr><td colspan=7 align=center>Cancel Orders<br><small>click on an order to cancel it.</small></td></tr>\n");
            buf.append(rowBuffer.toString());
            buf.append("\n</table>");
        }
    }

    private boolean printNBestOrders(StringBuffer buffer, SortedOrders offers, boolean increasing, User owner, Market market) {
        Order[] usersOrders = offers.usersOwnOrders(owner, ! increasing);
        Order[] bestOrders = MarketDisplay.bestOrders(nBest, ! increasing, usersOrders);
        for (int i = 0; i < bestOrders.length; i++) {
            Order order = bestOrders[i];
            HtmlElement[] cells = renderAsCells(order, increasing, market);
            renderCells(cells, buffer);
        }
        return bestOrders.length > 0;
    }

    private void renderCells(HtmlElement[] cells, StringBuffer buffer) {
        for (int i = 0; i < cells.length; i++) {
            HtmlElement cell = cells[i];
            cell.render(buffer);
        }
    }
}
