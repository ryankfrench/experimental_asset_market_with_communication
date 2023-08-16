package net.commerce.zocalo.JspSupport;

// Copyright 2007-2009 Chris Hibbert.  Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.html.*;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.market.MultiMarket;
import net.commerce.zocalo.orders.SortedOrders;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.currency.Price;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

/** support for a JSP page displaying markets in a market group. */
public class MarketDisplay extends UserPage {
    final static public String MARKET_JSP = "Market.jsp";
    final static public String MARKET_NAME = "Market";
    final static public String PAGE_TITLE = "All Markets";

    final static private String ORDERS_TABLE_HEADER =
            "<table id='orders_table' width='30%' align='center' border='0' cellspacing='2'>\n";
    final static private String[] BINARY_COL_LABELS = new String[]
            { "Market", "Market Maker's Price Level", "Best Buy Offer", "Best Sell Offer", "owner" };
    final static private String[] MULTIMARKET_COL_LABELS = new String[] {"Market", "Positions", "Owner"};
    final static private String[] CLOSED_COL_LABELS = new String[] {"Market", "Positions", "owner", "outcome", };

    private String marketName;
    private String marketMakerEndowment;

    public MarketDisplay() {
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        setUser(MarketOwner.registryLookup(request));
        if (getUser() == null) {
            redirectResult(request, response);
        }
        detectAdminCookie(request);
    }

//// REPORTING ///////////////////////////////////////////////////////////////

    public String getMarketNamesTable() {
        StringBuffer buf = new StringBuffer();
        printOpenBinaryMarkets(buf);
        printOpenMultiMarkets(buf);
        if (isAdminUser() || !Config.suppressHistoryLinks()) {
            printClosedMarkets(buf);
        }
        return buf.toString();
    }

    public String getCashBalanceDisplay() {
        SecureUser user = getUser();
        if (user == null) {
            return "";
        }
        return HtmlSimpleElement.labelledLine("Cash", "$" + user.cashOnHand().printAsDollars());
    }

    private void printOpenBinaryMarkets(StringBuffer buf) {
        Iterator iterator = HibernateUtil.allOpenBinaryMarkets().iterator();
        if (iterator.hasNext()) {
            buf.append("<p><h1><center>Binary Markets</center></h1>");
            printMarketTableStart(buf);
            HtmlSimpleElement.headerRow(buf, BINARY_COL_LABELS);
            String page = ClaimPurchase.claimPurchasePage(getUser());
            while (iterator.hasNext()) {
                MarketDisplay.printLinkedTableRow((BinaryMarket) iterator.next(), buf, page);
            }
            buf.append("</table>");
        }
    }

    static public void printLinkedTableRow(BinaryMarket market, StringBuffer buf, String pageName) {
        Book book = market.getBook();
        BinaryClaim claim = market.getBinaryClaim();
        HtmlElement[] cells = new HtmlElement[] {
                HtmlSimpleElement.claimLink(claim.getName(), pageName),
                currentEstimateCell(market, claim),
                bestYesCell(book, claim),
                bestNoCell(book, claim),
                ownerNameCell(market),
        };
        HtmlRow row = new HtmlRow(cells);
        row.render(buf);
    }

    private void printOpenMultiMarkets(StringBuffer buf) {
        Iterator iterator = HibernateUtil.allOpenMultiMarkets().iterator();
        if (iterator.hasNext()) {
            buf.append("<p><h2><b><center>Multi-outcome Markets</center></b></h2>");
            printMarketTableStart(buf);
            HtmlSimpleElement.headerRow(buf, MULTIMARKET_COL_LABELS);
            String name = ClaimPurchase.claimPurchasePage(getUser());
            while (iterator.hasNext()) {
                MarketDisplay.printMultiMarketTableRow((MultiMarket)iterator.next(), buf, name);
            }
            buf.append("</table>");
        }
    }

    static public void printMultiMarketTableRow(MultiMarket market, StringBuffer buf, String pageName) {
        Claim claim = market.getClaim();
        HtmlElement[] cells = new HtmlElement[] {
                HtmlSimpleElement.claimLink(claim.getName(), pageName),
                HtmlSimpleElement.centeredCell(claim.printPositions()),
                ownerNameCell(market),
        };
        HtmlRow row = new HtmlRow(cells);
        row.render(buf);
    }

    private void printClosedMarkets(StringBuffer buf) {
        Iterator iterator = HibernateUtil.allClosedMarkets().iterator();
        if (iterator.hasNext()) {
            buf.append("<p><br>");
            buf.append("<h2><b><center>Closed Markets</center></b></h2> ");
            buf.append("<center> <input class=\"smallFontButton\" type=button value='toggle display of closed markets' onclick=\"toggleVisibility('closedMarkets');\"></center>");
            buf.append("<div id='closedMarkets' style='display:none;'>");
            printMarketTableStart(buf);
            HtmlSimpleElement.headerRow(buf, CLOSED_COL_LABELS);
            String page = ClaimPurchase.claimPurchasePage(getUser());
            while (iterator.hasNext()) {
                MarketDisplay.printClosedMarketTableRow((Market) iterator.next(), buf, page);
            }
            buf.append("</table></div>");
        }
    }

    static public void printClosedMarketTableRow(Market market, StringBuffer buf, String pageName) {
        Claim claim = market.getClaim();
        HtmlElement[] cells = new HtmlElement[] {
                HtmlSimpleElement.claimLink(claim.getName(), pageName),
                HtmlSimpleElement.centeredCell(claim.printPositions()),
                ownerNameCell(market),
                HtmlSimpleElement.centeredCell(market.describeOutcome()),
        };
        HtmlRow row = new HtmlRow(cells);
        row.render(buf);
    }

    private void printMarketTableStart(StringBuffer buf) {
        HtmlTable.start(buf, "lightgrey", HtmlTable.TABLE_WIDTH, "80");
    }

    static public HtmlElement currentEstimateCell(Market market, BinaryClaim claim) {
        if (! market.hasMaker()) {
            return HtmlSimpleElement.centeredCell("-");
        }
        Price price = market.currentPrice(claim.getYesPosition());  // TODO round?
        return HtmlSimpleElement.centeredPriceCell(price);
    }

    static private HtmlSimpleElement ownerNameCell(Market market) {
        return HtmlSimpleElement.centeredCell(market.getOwner().getName());
    }

    static private HtmlSimpleElement bestNoCell(Book book, BinaryClaim claim) {
        return HtmlSimpleElement.centeredPriceCell(book.bestSellOfferFor(claim.getYesPosition()));
    }

    static private HtmlSimpleElement bestYesCell(Book book, BinaryClaim claim) {
        return HtmlSimpleElement.centeredPriceCell(book.bestBuyOfferFor(claim.getYesPosition()));
    }

    static private void printOrdersHeader(StringBuffer buf, String ordersLabel) {
        buf.append("<tr><td align=center colspan=2><font size=1>");
        buf.append(ordersLabel);
        buf.append("</font></td></tr>\n");
    }

    static public void printOrdersTable(Market market, StringBuffer buf, User user, String targetPage) {
        buf.append(ORDERS_TABLE_HEADER);
        buf.append("<tr><th>price</th><th>quantity</th></tr>\n");
        Book book = market.getBook();
        SortedOrders ordersForNo = book.getOffers(((BinaryClaim) book.getClaim()).getNoPosition());
        if (ordersForNo != null) {
            printOrdersHeader(buf, "Offers to Sell");
            printRowsFor(buf, user, ordersForNo, market, true, targetPage);
        }
        buf.append("<tr><td colspan=2>&nbsp;</td></tr>\n");
        SortedOrders ordersForYes = book.getOffers(((BinaryClaim) book.getClaim()).getYesPosition());
        if (ordersForYes != null) {
            printRowsFor(buf, user, ordersForYes, market, false, targetPage);
            printOrdersHeader(buf, "Offers to Buy");
        }
        buf.append("\n</table>");
        buf.append("<center><small>click on an order to cancel it.</small></center>");
    }

    static public String printHorizontalPriceTable(Market market, User user) {
        String buttons = printCancellationButtons(market, user, "TraderSubSubFrame.jsp");
        boolean buttonsExist = buttons.split("form").length > 1;

        StringBuffer buf = new StringBuffer();
        if (buttonsExist) {
            buf.append("<table align=center id='orders_table' border='0' cellpadding=2 cellspacing='2'>\n");
            buf.append("<tr><td colspan=7 align=center>Cancel Offers<br><small>click on an offer to cancel it.</small></td></tr>\n");
            buf.append(buttons);
            buf.append("\n</table>");
        }
        return buf.toString();
    }

    static public String printCancellationButtons(Market market, User user, String targetPage) {
        int bestN = 3;
        StringBuffer buf = new StringBuffer();

        Book book = market.getBook();
        SortedOrders ordersForYes = book.getOffers(((BinaryClaim) book.getClaim()).getYesPosition());
        if (ordersForYes != null) {
            printNBestOrders(buf, user, false, targetPage, bestN, market, ordersForYes);
        }

        buf.append("<td>&nbsp;</td>\n");

        SortedOrders ordersForNo = book.getOffers(((BinaryClaim) book.getClaim()).getNoPosition());
        if (ordersForNo != null) {
            printNBestOrders(buf, user, true, targetPage, bestN, market, ordersForNo);
        }
        return buf.toString();
    }

    static public void printRowsFor(StringBuffer buf, User user, SortedOrders sorted, Market market, boolean increasingOrder, String targetPage) {
        Order[] orders = sorted.usersOwnOrders(user, increasingOrder);
        if (orders.length == 0) {
            buf.append("\n<tr><td colspan=2><center>No Offers</center></td></tr>");
            return;
        }

        for (int i = 0; i < orders.length; i++) {
            Order order = orders[i];
            renderOrderAsRow(buf, increasingOrder, order, targetPage, market);
        }
    }

    public static void renderOrderAsRow(StringBuffer buf, boolean increasingOrder, Order order, String targetPage, Market market) {
        HtmlElement[] cells = new HtmlElement[2];
        cells[0] = HtmlSimpleElement.priceCell(order, ! increasingOrder, targetPage, market);
        cells[1] = HtmlSimpleElement.centeredCell(order.quantity().printAsQuantity());

        HtmlRow row;
        if (increasingOrder) {
            row = new HtmlRow("bgcolor=orange", cells);
        } else {
            row = new HtmlRow("bgcolor=limegreen", cells);
        }
        row.render(buf);
    }

    static public void printNBestOrders(StringBuffer buf, User user, boolean increasingOrder, String targetPage, int bestN, Market market, SortedOrders sorted) {
        Order orders[] = sorted.usersOwnOrders(user, increasingOrder);
        Order bestOrders[] = bestOrders(bestN, increasingOrder, orders);

        for (int i = 0; i < bestOrders.length; i++) {
            Order order = bestOrders[i];
            HtmlSimpleElement.priceCell(order, ! increasingOrder, targetPage, market).render(buf);
        }
    }

    public static Order[] bestOrders(int bestN, boolean increasingOrder, Order[] allOrders) {
        int minLength = Math.min(bestN, allOrders.length);
        Order orders[] = new Order[minLength];
        int startPoint = increasingOrder ? allOrders.length : minLength;
        for (int i = 0; i < minLength; i++) {
            orders[i] = allOrders[startPoint - 1 - i];
        }
        return orders;
    }

    public String navButtons() {
        return navButtons(MARKET_JSP);
    }

    public boolean marketsExist() {
        try {
            return HibernateUtil.marketsExist();
        } catch (Exception e) {
            return false;
        }
    }

    public String getRequestURL(HttpServletRequest request) {
        if (getUser() == null) {
            return WelcomeScreen.WELCOME_JSP;
        }
        return null;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getMarketMakerEndowment() {
        return marketMakerEndowment;
    }

    public void setMarketMakerEndowment(String marketMakerEndowment) {
        this.marketMakerEndowment = marketMakerEndowment;
    }
}
