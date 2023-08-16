package net.commerce.zocalo.JspSupport.printOrders;

import net.commerce.zocalo.html.HtmlElement;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.JspSupport.MarketDisplay;
import net.commerce.zocalo.JspSupport.ClaimPurchase;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.orders.SortedOrders;
import net.commerce.zocalo.currency.Probability;

import java.util.*;

// Copyright 2007-2009  Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** OrdersPrintStrategy for buttons that don't cancel the order. */
public class NoCancelOrdersPrintStrategy extends OrdersPrintStrategy {
    private final String bestOfferClass = "class=bestOffer";

    public NoCancelOrdersPrintStrategy(String buyColor, String sellColor) {
        super(buyColor, sellColor, "ignore");
    }

    public HtmlElement[] renderAsCells(Order order, boolean buying, Market market) {
        return renderAsPQNoCancelCells(order, market);
    }

    public void renderTable(StringBuffer buf, Book book, Market market) {
        if (book.hasOrdersToSell()) {
            String[] sellAttributes = new String[]{"bgcolor=" + sellColor, bestOfferClass, HtmlSimpleElement.CENTERED};
            HtmlSimpleElement.cell("Best Asks", sellAttributes).render(buf);
            buf.append("</tr><tr><td>\n");
            printPriceTable(buf, sellColor, false, market);
            renderMakerPrice(market, buf);
            printPriceTable(buf, buyColor, true, market);
            buf.append("</td></tr><tr>\n");
            String[] buyAttributes = new String[]{"bgcolor=" + buyColor, bestOfferClass, HtmlSimpleElement.CENTERED};
            HtmlSimpleElement.cell("Best Bids", buyAttributes).render(buf);
        } else {
            renderMakerPrice(market, buf);
            buf.append("No Outstanding Orders ");
        }
    }

    private void printPriceTable(StringBuffer buf, String color, boolean buying, Market market) {
        HtmlTable.start(buf, color, HtmlTable.CLASS, "bestOffer");
        renderOffers(buf, buying, market);
        HtmlTable.endTag(buf);
    }

    private void renderMakerPrice(Market market, StringBuffer buf) {
        if (market.hasMaker()) {
            BinaryClaim claim = (BinaryClaim) market.getClaim();
            Probability prob = market.currentProbability(claim.getYesPosition());
            buf.append("<center><small><small>market maker: </small> &nbsp; <span id='yes-price'>" + prob.printAsCents() + "</span></small></center>");
        }
    }

    public void renderTable(StringBuffer buf, SecureUser user) {
        List orders = user.getOrders();
        Collection<Position> claimsWithAssets = user.claimsWithAssets();
        if (orders.size() == 0) {
            buf.append("<p>No Outstanding Orders");
        } else {
            Set<String> claims = listClaimsWithOrdersOrAssets(orders, claimsWithAssets);
            render(buf, claims, ClaimPurchase.claimPurchasePage(user));
        }
    }

    private void render(StringBuffer buf, Set<String> claimNames, String pageName) {
        String[] title = new String[]{"Claims in which<br>you have orders"};
        HtmlTable.start(buf, title);
        for (Iterator<String> iterator = claimNames.iterator(); iterator.hasNext();) {
            HtmlRow.startTag(buf);
            HtmlSimpleElement.claimLink(iterator.next(), pageName).render(buf);
            HtmlRow.endTag(buf);
        }
        HtmlTable.endTagWithP(buf);
    }

    private Set<String> listClaimsWithOrdersOrAssets(List orders, Collection claimsWithAssets) {
        Set<String> claimNames = new HashSet<String>();
        for (Iterator iterator = orders.iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            claimNames.add(order.position().getClaim().getName());
        }
        for (Iterator iterator = claimsWithAssets.iterator(); iterator.hasNext();) {
            Position pos = (Position) iterator.next();
            claimNames.add(pos.getClaim().getName());
        }
        return claimNames;
    }

    private void renderOffers(StringBuffer buf, boolean buying, Market market) {
        Position pos;
        Book book = market.getBook();
        BinaryClaim claim = (BinaryClaim) book.getClaim();
        if (buying) {
            pos = claim.getYesPosition();
        } else {
            pos = claim.getNoPosition();
        }
        SortedOrders offers = book.getOffers(pos);

        Order[] allOrders = offers.usersOwnOrders((User)null, buying);
        Order[] best5Orders = MarketDisplay.bestOrders(5, buying, allOrders);
        for (int i = 0; i < best5Orders.length; i++) {
            Order order = best5Orders[i];
            renderRow(buf, order, buying, market);
        }
    }
}
