package net.commerce.zocalo.JspSupport;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.freechart.ChartGenerator;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.history.CostAccounter;
import net.commerce.zocalo.html.*;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.JspSupport.printOrders.NoCancelOrdersPrintStrategy;
import net.commerce.zocalo.JspSupport.printOrders.OrdersPrintStrategy;
import net.commerce.zocalo.JspSupport.printOrders.PQOrdersPrintStrategy;
import net.commerce.zocalo.ajax.events.MakerTrade;
import net.commerce.zocalo.ajax.events.Trade;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.currency.*;

import org.antlr.stringtemplate.StringTemplate;
import org.jfree.data.time.TimePeriodValue;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** support for a JSP page for trading.   */
public class ClaimPurchase extends UserPage {
    static final public String CLOSE_AND_DECIDE = "Close the market and Declare the outcome for ";
    static final public String PURCHASE_CLAIM_JSP = "purchaseClaim.jsp";
    static final public String PURCHASE_CLAIM_NAME = "purchaseClaim";
    static final public String PURCHASE_COST_JSP = "purchaseCost.jsp";
    static final public String PURCHASE_COST_NAME = "purchaseCost";
    static final public String TRADE_FIELD_LABEL = "Trade";
    static final public String REFRESH_FIELD_LABEL = "Refresh";
    private boolean useCookie = false;
    private int chartSize = 250;
    private String close = "";
    private String positionName = "";
    public final String TradeHistoryLabel = "Trading History";
    private String cost;
    private static Logger tradeLogger = Logger.getLogger("Trading");
    private String description;
    private String claimName;
    private String action = "";
    private String buySell = "";
    private String deleteOrderPrice = "";
    private String deleteOrderPosition = "";
    private String price;
    private String quantity;
    private final String MARKET_CLOSED = "market is closed.";

    public ClaimPurchase() {
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        Market market = getMarket(getClaimName());
        setUserFromCookie(request);
        detectAdminCookie(request);
        if (getUser() == null || market == null) {
            redirectResult(request, response);
        } else if (! q().isZero() && ! p().isZero()) {
            if (! market.isOpen()) {
                getUser().warn(MARKET_CLOSED);
                return;
            }
            logTradeRequest(p(), q());
            String bs = request.getParameter("buySell");
            if (bs != null) {
                setBuySell(bs);
            }
            buy();
        } else if (null != getDescription() && ! getDescription().equals(MarketOwner.DEFAULT_CLAIM_DESCRIPTION)) {
            if (HtmlSimpleElement.detectHtmlTextSpecial(getDescription())) {
                getUser().warn("HTML special characters are not allowed in claim Descriptions.");
                redirectResult(request, response);
            } else if (! currentUserOwnsCurrentClaim(market)) {
                getUser().warn("Only the owner can change the market description.");
                redirectResult(request, response);
                return;
            } else {
                market.getClaim().setDescription(getDescription());
            }
        } else if (! "".equals(deleteOrderPrice)) {
            if (! market.isOpen()) {
                getUser().warn(MARKET_CLOSED);
                return;
            }
            TradeSupport.deleteOrder(deleteOrderPosition, getUser(), deleteOrderPrice, getMarket(getClaimName()));
        } else if (! "".equals(close)) {
            if (! market.getOwner().equals(getUser())) {
                getUser().warn("Only the owner can close a market.");
                redirectResult(request, response);
                return;
            }
            closeClaim();
        } else if ("sell holdings".equals(request.getParameter("action"))) {
            if (! market.isOpen()) {
                getUser().warn(MARKET_CLOSED);
                return;
            }
            sellHoldings(market);
            positionName = "";
            redirectResult(request, response);
        } else {
            if (! market.isOpen()) {
                getUser().warn(MARKET_CLOSED);
                return;
            }
            costLimitPurchase(request, market, market.maxPrice());
        }
    }

    private void buy() {
        Market market = getMarket(getClaimName());
        if (! market.verifyPriceRange(p())) {
            getUser().warn("price must be greater than 0 and less than " + market.maxPrice() + ".");
            return;
        }

        market.marketCallBack(purchaseCallback());
    }

    private MarketCallback purchaseCallback() {

        return new MarketCallback() {
            public void binaryMarket(BinaryMarket market) {
                try {
                    Price p;
                    Position pos;
                    if (isBuy()) {
                        p = market.asPrice(p());
                        pos = market.getBinaryClaim().getYesPosition();
                    } else {
                        p = market.asPrice(p()).inverted();
                        pos = market.getBinaryClaim().getNoPosition();
                    }

                    market.limitOrder(pos, p, q(), getUser());
                } catch (DuplicateOrderException e) {
                    TradeSupport.warnAboutDuplicateOrders(getUser());
                }
            }

            public void multiMarket(MultiMarket market) {
                market.marketOrder(getPosition(), market.asPrice(p()), q(), getUser());
            }
        };
    }

    private void sellHoldings(Market market) {
        if (market.hasMaker()) {
            tradeLogger.debug(getUserName() + " wants to liquidate " + getPositionName());

            market.sellHoldings(getUser(), getPosition());
        }
    }

    private void logTradeRequest(Quantity p, Quantity q) {
        StringBuffer buff = new StringBuffer();
        buff.append(getUserName()).append(" requested a purchase of ");
        buff.append(q).append(" at a price of ").append(p).append(".");
        tradeLogger.debug(buff.toString());
    }

    private void logCostLimitRequest(String costString, String priceString, Position position, boolean buying) {
        StringBuffer buff = new StringBuffer();
        buff.append(getUserName()).append(" wants to ").append(buying ? "buy " : "sell ").append(position.getName());
        buff.append(" with limit of ").append(costString);
        buff.append(" @ ").append(priceString).append(".");
        tradeLogger.debug(buff.toString());
    }

    private void logPurchaseRequest(boolean priceLimit, Position position, boolean buying) {
        StringBuffer buff = new StringBuffer();
        buff.append(getUserName()).append(" wants to ").append(buying ? "buy " : "sell ").append(position.getName());
        buff.append(" @ ").append(priceLimit).append(".");
        tradeLogger.debug(buff.toString());
    }

    private void costLimitPurchase(HttpServletRequest request, Market market, Price samplePrice) {
        String row = request.getParameter("selectedRow");
        String claim = request.getParameter("selectedClaim");
        if (row != null && ! "".equals(row) && claim != null && ! "".equals(claim)) {
            setPositionName(claim);
            String priceString = request.getParameter(row + "price");
            if (priceString == null) {
                priceString = request.getParameter(row + "Target");
            }

            boolean priceLimit = priceString != null && ! "".equals(priceString);

            String costString = request.getParameter(row + "cost");
            boolean costLimit = costString != null && ! "".equals(costString);
            setPrice(priceString);
            setCost(costString);
            Quantity p = p();
            if (! market.verifyPriceRange(p)) {
                getUser().warn("price must be greater than 0 and less than " + market.maxPrice() + ".");
                return;
            }

            if (TRADE_FIELD_LABEL.equals(request.getParameter("action"))) {
                if (! priceLimit) {
                    getUser().warn("Please specify a price limit");
                    return;
                }

                Probability curProb = market.currentProbability(getPosition());

                String displayedString = request.getParameter(row + "Reference");
                Quantity displayedPrice = TradeSupport.parseDecimal(displayedString, getUser());
                Price requested = new Price(p, samplePrice);
                boolean buying = displayedPrice.compareTo(requested) <= 0;
                if (buying == (curProb.compareTo(requested.asProbability()) > 0)) {
                    getUser().warn("the price is already " + (buying ? "higher" : "lower") + " than you requested.");
                    return;
                }

                if (costLimit) {
                    setCost(costString);
                    logCostLimitRequest(costString, priceString, getPosition(), buying);
                    market.buyWithCostLimit(getPosition(), market.asPrice(p), c().times(Quantity.Q100), getUser(), buying);
                } else {
                    logPurchaseRequest(priceLimit, getPosition(), buying);
                    market.buyWithCostLimit(getPosition(), market.scaleToPrice(p), getUser().cashOnHand(), getUser(), buying);
                }
            }
        }
    }

    private void closeClaim() {
        final Market market = getMarket(getClaimName());
        Position winner = market.getClaim().lookupPosition(getPositionName());
        Quantity couponsRedeemed = market.decideClaimAndRecord(winner);
        getUser().warn("Redeemed " + couponsRedeemed.printAsQuantity() + " pairs.");
    }

    private void setUserFromCookie(HttpServletRequest request) {
        setUser(MarketOwner.registryLookup(request));
        if (getUser() != null) {
            useCookie = true;
        }
    }

    public String getRequestURL(HttpServletRequest request) {
        if (getUser() == null || getMarket(getClaimName()) == null) {
            return WelcomeScreen.WELCOME_JSP;
        }
        return null;
    }

    public String buyOrEditClaimHtml() {
        StringBuffer buf = new StringBuffer();

        Market market = getMarket(getClaimName());
        if (market == null) {
            buf.append("claim not found.");
            return buf.toString();
        }

        if (! market.isOpen()) {
            HtmlTable.start(buf, "", HtmlTable.BORDER, "0");
            HtmlRow.oneCell(buf, "<b><Center>Market closed</b>");
            HtmlRow.oneCell(buf, "Outcome is " + market.describeOutcome());
            if (market instanceof MultiMarket) {
                MultiMarket multiMarket = (MultiMarket)market;
                HtmlRow.oneCell(buf, finalProbabilityTable(multiMarket.finalProbs()));
            } else if (market.hasMaker()) {
                Position yes = market.getClaim().lookupPosition("yes");
                String prob = market.currentProbability(yes).printAsCents();
                HtmlRow.oneCell(buf, "<center>final probability was " + prob);
            }
            HtmlTable.endTag(buf);
        } else if (currentUserOwnsCurrentClaim(market)) {
            allowEditingDescription(buf);
            buf.append("<br>");
            closeClaimHtml(buf, market);
        } else {
            market.marketCallBack(claimHtmlCallback(buf));
        }
        return buf.toString();
    }

    private MarketCallback claimHtmlCallback(final StringBuffer buf) {
        return new MarketCallback() {
            public void binaryMarket(BinaryMarket market) { htmlForBuyingBinaryClaim(buf); }
            public void unarymarket(UnaryMarket market) { htmlForBuyingBinaryClaim(buf); }
            public void multiMarket(MultiMarket market) { htmlForBuyingMultiClaim(buf, market); }
        };
    }

    static public String finalProbabilityTable(Dictionary<Position,Probability> finalProbs) {
        StringBuffer buf = new StringBuffer();
        Enumeration<Position> keys = finalProbs.keys();
        if (keys.hasMoreElements()) {
            HtmlTable.start(buf, "lightgreen", new String[] { "Position", "Final Price" });

            while (keys.hasMoreElements()) {
                Position position = keys.nextElement();
                HtmlElement[] cells = new HtmlElement[] {
                        HtmlSimpleElement.cell(position.getName()),
                        HtmlSimpleElement.cell(finalProbs.get(position).printAsCents()),
                };
                HtmlRow row = new HtmlRow(cells);
                row.render(buf);
            }
            HtmlTable.endTag(buf);
        }

        return buf.toString();
    }

    public String allowEditingDescription(StringBuffer buf) {
        String claimName = getClaimName();
        buf.append("<h2> Edit " + claimName + "</h2>" +
            HtmlSimpleElement.
                simplePostFormHeader("purchaseClaim.jsp",
                        "Edit the Claim Description for <b>" + claimName + "</b>",
                        "claimName", claimName) +
            "<br> <textarea rows=\"3\" name=description>" +
                getMarket(claimName).getClaim().getDescription() + "</textarea>\n<br> " +
                HtmlSimpleElement.submitInputField("Submit") + "\n</form>\n");
        return buf.toString();
    }

    private void closeClaimHtml(StringBuffer buf, Market market) {
        HtmlSimpleElement.printToggledCloseClaimForm(buf, market, claimPurchasePage(getUser()));
    }

    static public String claimPurchasePage(User user) {
        if (user.useCostLimitUI()) {
            return PURCHASE_COST_JSP;
        } else {
            return PURCHASE_CLAIM_JSP;
        }
    }

    public String claimDeletionFormHtml() {
        StringBuffer buf = new StringBuffer();

        Market market = getMarket(getClaimName());
        if (market == null) {
            buf.append("claim not found.");
            return buf.toString();
        }

        if (! currentUserOwnsCurrentClaim(market)) {
            allowDeletingOrders(buf);
        }
        return buf.toString();
    }

    protected boolean currentUserOwnsCurrentClaim(Market market) {
        return getUserName().equals(market.getOwner().getName());
    }

    public String getClaimDescription() {
        return getMarket(getClaimName()).getClaim().getDescription();
    }

    public String cashOnHandHtml() {
        return HtmlSimpleElement.bold("" + getUser().cashOnHand().printAsDollars());
    }

    public String displayHoldingsHtml() {
        Claim claim = HibernateUtil.getClaimByName(getClaimName());
        StringBuffer buf = new StringBuffer();

        HtmlTable.start(buf, AccountDisplay.HOLDINGS_BG_COLOR, "border", "0");
        HtmlRow.startTag(buf);
        buf.append("<td>");
        if (getMarket(getClaimName()).isOpen()) {
            if (null == getMarket(getClaimName()).getBook()) {
                Accounts.PositionDisplayAdaptor multiPrinter;
                multiPrinter = AccountDisplay.claimHoldingsMultiMarketPrinter(getUser());
                getUser().displayMultiMarketAccounts(buf, claim, multiPrinter);
            } else {
                Accounts.PositionDisplayAdaptor printer;
                printer = AccountDisplay.claimHoldingsBinaryMarketPrinter();
                getUser().displayAccounts(buf, claim, printer);
            }
        }
        showCostOfHoldings(buf, AccountDisplay.HOLDINGS_BG_COLOR);

        HtmlTable.endTag(buf);

        return buf.toString();
    }

    public void warn(String s) {
        getUser().warn(s);
    }

    public String displayTradeHistory() {
        StringBuffer buf = new StringBuffer();
        List trades;
        Market market = getMarket(getClaimName());
        trades = HibernateUtil.getTrades(getUser(), market);
        if (trades.size() == 0) {
            return "";
        }

        HtmlTable outer = new HtmlTable();
        outer.add("bgcolor", "lightblue");
        outer.add(HtmlTable.BORDER, "0");
        outer.add("align", "right");
        outer.render(buf);

        buf.append("<tr><td>\n   ");

        boolean printPosition = displayPositions(market);
        HtmlSimpleElement.printHeader(buf, 2, TradeHistoryLabel);
        buf.append("<tr><td>\n   ");
        if (printPosition) {
            HtmlTable.start(buf, "lightblue", new String[] { "position", "date", "price", "quant" }, "orderTable");
        } else {
            HtmlTable.start(buf, "lightblue", new String[] { "date", "price", "quant" }, "orderTable");
        }

        for (Iterator iterator = trades.iterator(); iterator.hasNext();) {
            Trade t = (Trade) iterator.next();
            String posLabel = null;
            if (printPosition) {
                Position pos = t.getPos();
                posLabel = pos.getName();
            }
            TradeHistory.printRow(buf, t, posLabel);
        }
        HtmlTable.endTagWithP(buf);
        HtmlTable.endTagWithP(buf);
        return buf.toString();
    }

    private boolean displayPositions(Market market) {
        final boolean[] showPositions = new boolean[] { true };
        MarketCallback cb = new MarketCallback() {
            public void binaryMarket(BinaryMarket mkt) { showPositions[0] = false; }
        };
        market.marketCallBack(cb);
        return showPositions[0];
    }

    private void showCostOfHoldings(StringBuffer buf, String color) {
        Market market = getMarket(getClaimName());
        CostAccounter accounter = new CostAccounter(getUser(), market);
        if (market.isOpen()) {
            HtmlTable.start(buf, color, new String[] { "total cost" }, "costTable");
        } else {
            HtmlTable.start(buf, color, new String[] { "total cost", "redemption value" }, "costTable");
        }
        HtmlRow.startTag(buf);

        Quantity cost = accounter.totalCost();
        buf.append(HtmlSimpleElement.printTableCell(cost.printAsDollars()));
        if (! market.isOpen()) {
            buf.append(HtmlSimpleElement.printTableCell(accounter.totalRedemptions().printAsDollars()));
        }

        HtmlRow.endTag(buf);
        HtmlTable.endTagWithP(buf);
    }

    public String displayOrdersHtml(Market market) {
        Claim claim = HibernateUtil.getClaimByName(getClaimName());
        if (! getMarket(getClaimName()).isOpen()) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        OrdersPrintStrategy.displayOrders(buf, claim, market, getUser());
        return buf.toString();
    }

    public boolean isOpen() {
        return getMarket(getClaimName()).isOpen();
    }

    public void allowDeletingOrders(StringBuffer buf) {
        Market market = getMarket(getClaimName());
        if (! market.isOpen()) {
            return;
        }
        Book book = market.getBook();
        if (book == null) {
            buf.append("&nbsp;");
            return;
        }
        buf.append("<p>\n<table align=center bgcolor=\"lightgrey\">\n<tr><td>\n");
        String targetPage = ClaimPurchase.claimPurchasePage(getUser());
        PQOrdersPrintStrategy pqStrat = OrdersPrintStrategy.makePQ("limegreen", "orange", targetPage);
        pqStrat.renderTable(buf, getUser(), market);
        buf.append("</td></tr>\n</table>\n");
    }

    public boolean marketHasBookOrders() {
        Market market = getMarket(getClaimName());
        return market.getBook() != null;
    }

    public void htmlForBuyingMultiClaim(StringBuffer buf, MultiMarket market) {
        if (Config.getCostLimitBuying()) {
            htmlForMultiClaimCostBuy(buf, market);
        } else {
            htmlForMultiClaimQuantityBuy(buf, market);
        }
    }

    public void htmlForMultiClaimCostBuy(StringBuffer buf, MultiMarket market) {
        String purchasePage = ClaimPurchase.claimPurchasePage(getUser());
        SortedSet<Position> sorted = market.getClaim().sortPositions();
        Dictionary<Position, String> shortNames = makeAliasDictionary(sorted);

        buf.append(HtmlSimpleElement.simplePostFormHeader(purchasePage, "", "claimName", getClaimName()));

        buf.append("\n");
        HtmlTable t = new HtmlTable();
        t.add("bgcolor", "DADBEC");
        t.add("id", "buysell");
        t.render(buf);
        buf.append("  <tr>\n" +
                "\t<th rowspan=2>Prediction\n" +
                "\t<th rowspan=2 align=center>Current<br>Price\n" +
                "\t<th colspan=2>Buy or<br>Sell Until\n" +
                "  <tr>\n" +
                "\t<th>price goes<br>to ...\n" +
                "\t<th>or I've<br>spent ...\n");
        String selectedRowName = "";

        for (Iterator<Position> iter = sorted.iterator(); iter.hasNext();) {
            Position position =  iter.next();
            String price = market.currentProbability(position).printAsCents();
            String name = position.getName();

            String defaultCost = " value='100' ";
            String defaultPrice = "";
            String chosenString = "";
            if (name.equals(positionName)) {
                chosenString = " class='chosen'";
                selectedRowName = shortNames.get(position);
                if (cost != null) {
                    defaultCost = " value='" + cost + "' ";
                }
                if (getPrice() != null) {
                    defaultPrice = " value='" + getPrice() + "' ";
                }
            }

            String rowTemplate =
    "    <tr$chosen$ id=$id$> <td>$label$\n" +
    "        <td align='center'><span id='$row$Latest'>$price$&cent;</span><span style='float:right;opacity:0' id='$row$Reference'>$price$&cent;</span>\n" +
    "          <input type=hidden name='$row$Reference' value='$price$'>\n" +
    "        <td align='center'>\n" +
    "          <input type=text size=2 maxLength=2 name='$row$Target' onblur='$row$.handleBlur()' $defaultPrice$ onfocus='$row$.handleFocus()' onchange='$row$.handleChange()' autocomplete='off' >&cent;\n" +
    "        <td align='center'>\\$\n" +
    "          <input type=text size=3 maxLength=4 name='$row$cost' $defaultCost$  onchange=\"highlight('$id$')\" autocomplete='off' >\n";
            StringTemplate row = new StringTemplate(rowTemplate);
            row.setAttribute("label", name);
            row.setAttribute("id", shortNames.get(position));
            row.setAttribute("price", price);
            row.setAttribute("row", shortNames.get(position));
            row.setAttribute("chosen", chosenString);
            row.setAttribute("defaultCost", defaultCost);
            row.setAttribute("defaultPrice", defaultPrice);

            buf.append(row.toString());
        }

        buf.append("</table>\n");
        buf.append("\t\t<input id=rowSelection type=hidden name=selectedRow value=").append(selectedRowName).append(">\n");
        buf.append("\t\t<input id=claimSelection type=hidden name=selectedClaim value=").append(positionName).append(">\n");
        String refreshButton = HtmlSimpleElement.submitInputField(REFRESH_FIELD_LABEL, "onSubmit=unsubscribeAll()");
        String submitButton = HtmlSimpleElement.submitInputField(TRADE_FIELD_LABEL, "onSubmit=unsubscribeAll()");
        buf.append("<center>").append(submitButton).append("&nbsp; &nbsp;").append(refreshButton);
        buf.append("</form>\n");
        addReactiveMarketJavascript(buf, shortNames);
    }

    private Dictionary<Position, String> makeAliasDictionary(SortedSet<Position> sorted) {
        Dictionary<Position, String> aliases = new Hashtable<Position, String>();
        int i = 0;
        for (Iterator<Position> positionIterator = sorted.iterator(); positionIterator.hasNext();) {
            Position position =  positionIterator.next();
            aliases.put(position, "r" + i++);
        }
        return aliases;
    }

    private void addReactiveMarketJavascript(StringBuffer buf, Dictionary<Position, String> sorted) {
        StringBuffer names = new StringBuffer();
        Enumeration<Position> positions = sorted.keys();
        while (positions.hasMoreElements()) {
            Position position = positions.nextElement();
            names.append(sorted.get(position));
            if(positions.hasMoreElements()){
                names.append("', '");
            }
        }
        buf.append("<script type=\"text/javascript\">\n");
        buf.append("\tvar market = reactiveMarket( [");
        namesAndLabelsForJS(buf, sorted);
        buf.append("] );\n");
        positions = sorted.keys();
        while (positions.hasMoreElements()) {
            Position position = positions.nextElement();
            String alias = sorted.get(position);
            buf.append("\tvar " + alias + " = market.row('" + alias + "');\n");
        }
        buf.append("</script>\n");
    }

    private void namesAndLabelsForJS(StringBuffer buf, Dictionary<Position, String> sorted) {
        Enumeration<Position> positions = sorted.keys();
        while (positions.hasMoreElements()) {
            Position position = positions.nextElement();
            String alias = sorted.get(position);
            buf.append("['").append(alias).append("', '").append(position.getName());
            if (positions.hasMoreElements()) {
                buf.append("'], ");
            } else {
                buf.append("']");
            }
        }
    }

    private String namesAndLabels() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    public void htmlForMultiClaimQuantityBuy(StringBuffer buf, MultiMarket market) {
        String[] positions = market.getClaim().positionNames();
        String selectionList = HtmlSimpleElement.selectList("positionName", positions, null);
        String quantInput = HtmlSimpleElement.labelledQuantInputField("quantity", 1);
        String priceInput = HtmlSimpleElement.labelledTextInputField("price");
        String submitButton = HtmlSimpleElement.submitInputField("Submit");

        buf.append("\n<table cellspacing=3 bgcolor='DADBEC'>\n<tr><td>\n");
        buf.append(HtmlSimpleElement.simplePostFormHeader(ClaimPurchase.claimPurchasePage(getUser()),
                "Specify position, target price, and quantity:",
                "claimName", getClaimName()));
        buf.append("\n<table><tr><td colspan='2' align='center'>" + selectionList + "</td></tr>");
        buf.append("\n<tr><td align='right'>" + quantInput + "<br>" + priceInput + "</td>\n");
        buf.append("<td width='10%'></td>");
        buf.append("<td align='center'>" + submitButton + "</td></tr>\n");
        buf.append("</table>\n</form>\n</td></tr>\n</table>\n<p>\n");
    }

    public void htmlForBuyingBinaryClaim(StringBuffer buf) {
        buf.append("<p>\n<table align=center bgcolor=\"DADBEC\">\n<tr><td>\n");
        String targetPage = ClaimPurchase.claimPurchasePage(getUser());
        if (useCookie) {
            buf.append(
                HtmlSimpleElement.simplePostFormHeader(targetPage,
                    "Purchase a stake in the claim '" + getClaimName() + "' ",
                    "claimName", getClaimName()));
        } else {
            buf.append(
                HtmlSimpleElement.simplePostFormHeader(targetPage,
                    "Purchase a stake in the claim '" + getClaimName() + "' ",
                    "claimName", getClaimName(), "userName", getUserName()));
        }
        String priceInput = HtmlSimpleElement.labelledTextInputField("price");
        String quantInput = HtmlSimpleElement.labelledQuantInputField("quantity", 1);
        String submitButton = HtmlSimpleElement.submitInputField("Submit");
        buf.append("\n<table><tr><td colspan='2' align='center'> ");
        HtmlSimpleElement.radioButtons(buf, "buySell", "buy", "sell", false);
        buf.append("</td></tr>\n");
        buf.append("<tr><td align='right'> ").append(quantInput).append("<br>").append(priceInput).append("</td>\n");
        buf.append("<td width='10%'></td>");
        buf.append("<td align='center'> ").append(submitButton).append("</td></tr>\n");
        buf.append("</table>\n</form>\n</td></tr>\n</table>\n<p><br>\n");
    }

    public String displayBestOrdersHtml() {
        Market market = getMarket(getClaimName());
        Book book = market.getBook();
        if (book != null && ! currentUserOwnsCurrentClaim(market)) {
            StringBuffer buf = new StringBuffer();
            NoCancelOrdersPrintStrategy noCancelStrat = OrdersPrintStrategy.makeNoCancel("lightGreen", "orange");
            noCancelStrat.renderTable(buf, book, market);
            return buf.toString();
        } else if (! currentUserOwnsCurrentClaim(market)) {
            StringBuffer buf = new StringBuffer();
            buf.append(renderMakerPriceTable(market));
            return buf.toString();
        } else {
            return "<td></td>";
        }
    }

    public static Market getMarket(String claimName) {
        return HibernateUtil.getMarketByName(claimName);
    }

    public SecureUser getUser() {
        if (useCookie) {
            return super.getUser();
        }
        return MarketOwner.getUser(getUserName());
    }

    public void setAction(String value) {
        action = value;
    }

    public String historyChartNameForJsp() {
    	return historyChartNameForJsp(getClaimName(), false, getChartSize(), getChartSize());
    }

    /**
     * Helper method allowing generation of a chart without an instance of ClaimPurchase
     */
    public static String historyChartNameForJsp(String claimName, boolean scalePrice, int chartHeight, int chartWidth) {
        String chartDirName = "charts";
        Date lastTrade = getMarket(claimName).getLastTrade();
        try {
            File chartFile = ChartGenerator.updateChartFile("webpages", chartDirName, lastTrade, claimName, scalePrice, chartHeight, chartWidth);
            return new File(chartDirName, chartFile.getName()).getPath().replace('\\', '/');
        } catch (IOException e) {
            e.printStackTrace();
            return "UnableToCreateChart";
        }
    }

    public String multiChartNameForJsp() {
        String chartDirName = "charts";
        Market market = getMarket(getClaimName());
        Date lastTrade = market.getLastTrade();
        try {
            File chartFile = ChartGenerator.writeMultiStepChartFile("webpages", chartDirName, getChartSize(), lastTrade, market.getClaim());
            return new File(chartDirName, chartFile.getName()).getPath();
        } catch (IOException e) {
            e.printStackTrace();
            return "UnableToCreateChart";
        }
    }

    public String displayClaimName() {
        String claimName = getClaimName();
        Market market = getMarket(claimName);
        if (null == market) {
            return "";
        }
        if (market.isOpen()) {
            return claimName;
        } else {
            return HtmlSimpleElement.coloredText("Closed Market: ", "red", "closedMarketName") + claimName;
        }
    }

    public String navButtons() {
        return navButtons(PURCHASE_CLAIM_JSP);
    }

    public int getChartSize() {
        return chartSize;
    }

    public String getClose() {
        return close;
    }

    public void setClose(String close) {
        this.close = close;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Position getPosition() {
        Market market = getMarket(getClaimName());
        Claim claim = market.getClaim();
        return claim.lookupPosition(getPositionName());
    }

    public String renderMakerPriceTable(Market market) {
        Claim claim = market.getClaim();
        Position[] positions = claim.positions();
        SortedMap<Probability, Set<String>> orderedLines = new TreeMap<Probability, Set<String>>();
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Probability prob = market.currentProbability(position);
            String value = prob.printAsPercent(3);
            saveLine(prob, orderedLines, printOffer(position, value));
        }
        return printTableFromLines(orderedLines);
    }

    private StringBuffer printOffer(Position position, String value) {
        StringBuffer lineBuf = new StringBuffer();
        HtmlRow.startTag(lineBuf, "bestOffer");
        String name = position.getName();
        lineBuf.append(HtmlSimpleElement.printTableCell(name));
        lineBuf.append(HtmlSimpleElement.printTableCell(value, name + "-price"));
        HtmlRow.endTag(lineBuf);
        return lineBuf;
    }

    private void saveLine(Probability prob, SortedMap<Probability, Set<String>> orderedLines, StringBuffer lineBuf) {
        Probability p = prob.inverted(); // invert prob to sort decreasing
        Set<String> s = orderedLines.get(p);
        if (s == null) {
            s = new HashSet<String>();
            orderedLines.put(p, s);
        }
        s.add(lineBuf.toString());
    }

    private String printTableFromLines(SortedMap orderedLines) {
        StringBuffer buf = new StringBuffer();
        HtmlTable.start(buf, "lightblue", new String[] { "Position", "Price" } );
        Collection lineSets = orderedLines.values();
        for (Iterator iterator = lineSets.iterator(); iterator.hasNext();) {
            Set lines = (Set) iterator.next();
            for (Iterator it = lines.iterator(); it.hasNext();) {
                String line = (String) it.next();
                buf.append(line);
            }
        }
        HtmlTable.endTagWithP(buf);
        return buf.toString();
    }

    static public TimePeriodValuesCollection getHistoricalPrices(String claimName, List trades) {
        TimePeriodValues values = new TimePeriodValues("Price");
        for (Iterator iterator = trades.iterator(); iterator.hasNext();) {
            Trade trade = (Trade) iterator.next();
            if (! trade.getQuantity().isZero()) {
                if (trade.getPos().isInvertedPosition()) {
                    values.add(trade.timeAndPriceInverted());
                } else {
                    values.add(trade.timeAndPrice());
                }
            }
        }
        return new TimePeriodValuesCollection(values);
    }

    static public TimePeriodValuesCollection getHistoricalVolumes(String claimName, List trades) {
        TimePeriodValues values = new TimePeriodValues("Volume");
        for (Iterator iterator = trades.iterator(); iterator.hasNext();) {
            Trade trade = (Trade) iterator.next();
            TimePeriodValue volume = trade.timeAndVolume();
            if (volume != null) {
                values.add(volume);
            }
        }
        return new TimePeriodValuesCollection(values);
    }

    static public TimePeriodValuesCollection getOpenCloseValues(List trades, Claim claim) {
        Dictionary positions = new Hashtable();
        Dictionary originalValue = new Hashtable();
        TimePeriodValuesCollection allValues = new TimePeriodValuesCollection();

        initializeSeries(positions, allValues, claim);
        addPriceSeries(trades, positions, originalValue);
        return allValues;
    }

    private static void initializeSeries(Dictionary positions, TimePeriodValuesCollection allValues, Claim claim) {
        Position[] allPositions = claim.positions();
        if (allPositions.length == 2) {
            int less = allPositions[0].comparePersistentId(allPositions[1]);
            Position pos = allPositions[less < 0 ? 0 : 1];   // pick one stably

            TimePeriodValues values = new TimePeriodValues(pos.getName());
            positions.put(pos, values);
            allValues.addSeries(values);
        } else {
            SortedSet<Position> sortedPositions = claim.sortPositions();
            for (Iterator<Position> positionIterator = sortedPositions.iterator(); positionIterator.hasNext();) {
                Position pos = positionIterator.next();
                TimePeriodValues values = new TimePeriodValues(pos.getName());
                positions.put(pos, values);
                allValues.addSeries(values);
            }
        }
    }

    private static void addPriceSeries(List trades, Dictionary positions, Dictionary originalValue) {
        for (Iterator iterator = trades.iterator() ; iterator.hasNext() ; ) {
            MakerTrade trade = (MakerTrade) iterator.next();
            TimePeriodValues values = (TimePeriodValues) positions.get(trade.getPos());
            if (values == null) {
                continue;
            }
            if (originalValue.get(trade.getPos()) == null) {
                if (! trade.openValue().equals(trade.closeValue())) { // TODO This is only a transient, since opening prices weren't stored originally
                    values.add(trade.openValue());
                }
                originalValue.put(trade.getPos(), trade);
            }
            values.add(trade.closeValue());
        }
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public Quantity c() {
        return TradeSupport.parseDecimal(cost, getUser());
    }

    public boolean hasMarketMaker() {
        return getMarket(getClaimName()).hasMaker();
    }

    public String getDescription() {
      return description;
  }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClaimName() {
        return claimName;
    }

    public void setClaimName(String claimName) {
        this.claimName = claimName;
    }

    public boolean isBuy() {
        return getBuySell().equalsIgnoreCase(TradeSupport.BUY_ACTION);
    }

    public String getBuySell() {
        return buySell == null ? "" : buySell;
    }

    public void setBuySell(String buySell) {
        this.buySell = buySell;
    }

    public String getAction() {
        return action;
    }

    public void setDeleteOrderPrice(String deleteOrderPrice) {
        this.deleteOrderPrice = deleteOrderPrice;
    }

    public void setDeleteOrderPosition(String deleteOrderPosition) {
        this.deleteOrderPosition = deleteOrderPosition;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String q) {
        quantity = q;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Quantity p() {
        return TradeSupport.parseInteger(price, getUser());
    }

    public Quantity q() {
        return TradeSupport.parseDecimal(quantity, getUser());
    }

    /**
     * JJDM.  Always returns null as of 2014-OCT.
     * Removed as part of decoupling with Jetty.  AllMarkets was deleted.
     * @return
     */
    @Deprecated
    public String priceUpdateChannel() {
    	return null;
    	// JJDM - Removed 
    	/*
        String name = getClaimName();
        if (name == null) {
            return "";
        }
        return AllMarkets.channelName(name);
        */
    }
}
