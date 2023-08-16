package net.commerce.zocalo.JspSupport;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.experiment.ExperimentSubject;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.experiment.states.StatusAdaptor;
import net.commerce.zocalo.experiment.states.NoActionStatusAdaptor;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.user.User;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Jsp support for Trader's screen in experiments.  */
public class TraderScreen extends ExperimentPage {
    final static public String MARKET_ORDER = "marketOrder";
    final static public String NEW_ORDER = "newOrder";
    final static String DELETE_ORDER_PHRASE = "delete an order";
    final static private String NEW_ORDER_PHRASE = "enter a new order";
    final static private String MARKET_ORDER_PHRASE = "buy a standing order";
    private String orderType = "";
    private String claimName;
    private String action = "";
    private String deleteOrderPrice = "";
    private String deleteOrderPosition = "";
    private String price;
    private String quantity;

    public TraderScreen() {
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        Market market = getMarket(getClaimName());
        if (market == null) {
            Logger logger = Logger.getLogger(Log4JHelper.UserError);
            logger.warn(GID.log() + "Market not initialized.  Please wait for the experiment to begin.");
            return;
        }

        String order;
        if (! "".equals(deleteOrderPrice)) {
            order = DELETE_ORDER_PHRASE;
        } else if (orderType.equals(MARKET_ORDER)) {
            order = MARKET_ORDER_PHRASE;
        } else if (orderType.equals(NEW_ORDER)) {
            order = NEW_ORDER_PHRASE;
        } else {
            String parameter = request.getParameter("action");
            Session session = SessionSingleton.getSession();
            if (session != null && parameter != null) {
                session.webAction(getUserName(), parameter);
                redirectResult(request, response);
            }
            return;
        }

        if (! market.isOpen()) {
            String warning = "market closed";
            warnNonNullUser(warning);
            Logger logger = Logger.getLogger(Log4JHelper.UserError);
            logger.warn(GID.log() + "Market is closed.  Request by '" + getUserName() + "' to " + order + " not processed");
            return;
        }

        if (! "".equals(deleteOrderPrice)) {
            User user = getUser();
            if (user != null) {
                TradeSupport.deleteOrder(deleteOrderPosition, user, deleteOrderPrice, getMarket(getClaimName()));
            }
            redirectResult(request, response);
            return;
        }

        if (! market.verifyPriceRange(p())) {
            warnNonNullUser("price must be greater than 0 and less than " + market.maxPrice() + ".");
            return;
        }

        buy();
        redirectResult(request, response);
    }

    private void warnNonNullUser(String warning) {
        getUser().addWarning(warning);
    }

    private void buy() {
        final User user = getUser();
        if (user == null) {
            return;
        }
        MarketCallback callback = new MarketCallback() {
            public void binaryMarket(BinaryMarket mkt) { buyInMarket(mkt, user); }
            public void unaryMarket(UnaryMarket mkt) { buyInMarket(mkt, user); }
        };

        getMarket(getClaimName()).marketCallBack(callback);
    }

    private void buyInMarket(BinaryMarket mkt, User user) {
        try {
            buy(mkt, user);
        } catch (DuplicateOrderException e) {
            TradeSupport.warnAboutDuplicateOrders(user);
        }
    }

    private void buy(BinaryMarket mkt, User user) throws DuplicateOrderException {
        BinaryClaim claim = mkt.getBinaryClaim();
        Session session = SessionSingleton.getSession();
        if (session == null) {
            warn("Please wait for experiment to start.");
            return;
        }
        Position pos;
        Price price;
        boolean proceed;
        ExperimentSubject player = session.getPlayer(user.getName());
        if (action.equalsIgnoreCase(TradeSupport.BUY_ACTION)) {
            proceed = player.canBuy(session.getCurrentRound());
            price = p();
            pos = claim.getYesPosition();
        } else {
            proceed = player.canSell(session.getCurrentRound());
            price = p().inverted();
            pos = claim.getNoPosition();
        }

        if (proceed) {
            if (orderType.equals(MARKET_ORDER)) {
                mkt.marketOrder(pos, price, q(), user);
            } else {
                mkt.limitOrder(pos, price, q(), user);
            }
        } else {
            warn("You aren't allowed to " + action);
        }
    }

    public Position getPosition() {
        BinaryClaim claim = ((BinaryMarket)getMarket(getClaimName())).getBinaryClaim();
        if (isBuy()) {
            return claim.getYesPosition();
        } else {
            return claim.getNoPosition();
        }
    }

    public String marketOrderFormRow() {
        BinaryMarket market = (BinaryMarket) getMarket(getClaimName());
        if (market == null) { return "Experiment not started."; }

        BinaryClaim claim = market.getBinaryClaim();
        Book book = market.getBook();
        String bestSellPrice = book.bestBuyOfferFor(claim.getNoPosition()).inverted().printAsWholeCents();
        String bestBuyPrice = book.bestBuyOfferFor(claim.getYesPosition()).printAsWholeCents();
        StringBuffer buff = new StringBuffer();
        buff.append("<tr>\n");
        buff.append("<td>Immediate Offer</td>\n");
        Session session = SessionSingleton.getSession();
        ExperimentSubject player = session.getPlayer(getUserName());
        if (player == null || ! player.canBuy(session.getCurrentRound())) {
            bestSellPrice = "0";
        }
        if (player == null || ! player.canSell(session.getCurrentRound())) {
            bestBuyPrice = "0";
        }
        int round = session.getCurrentRound();

        boolean canSell = player == null || player.canSell(round);
        buff.append(printFormForMarketOrder(TradeSupport.BUY_ACTION, bestSellPrice, canSell));
        boolean canBuy = player == null || player.canBuy(round);
        buff.append(printFormForMarketOrder(TradeSupport.SELL_ACTION, bestBuyPrice, canBuy));
        buff.append("</tr>\n");
        return buff.toString();
    }

    public String showEarningsSummary() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.showEarningsSummary());
        final Session session = SessionSingleton.getSession();
        if (session != null) {
            buff.append(session.stateSpecificTraderHtml(getClaimName(), getUserName()));
        }
        return buff.toString();
    }

    public String claimPurchaseFormRow() {
        StringBuffer buff = new StringBuffer();
        buff.append("<tr valign=top>\n");
        buff.append("<td>Submit New Offer</td>\n");
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "Please wait for the experiment to start.";
        }
        ExperimentSubject player = session.getPlayer(getUserName());
        if (player != null) {
            int round = session.getCurrentRound();
            buff.append(printFormForNewOrder(TradeSupport.BUY_ACTION, player.canBuy(round)));
            buff.append(printFormForNewOrder(TradeSupport.SELL_ACTION, player.canSell(round)));
        }
        buff.append("</tr>\n");
        return buff.toString();
    }

    public String pricesTable() {
        final Session session = SessionSingleton.getSession();
        if (session == null) {
            return "Experiment not started.  Please wait.";
        }
        final StringBuffer buff = new StringBuffer();

        StatusAdaptor ad = new NoActionStatusAdaptor() {
            public void trading() {
                Book book = getMarket(getClaimName()).getBook();
                buff.append(MarketDisplay.printHorizontalPriceTable(getMarket(getClaimName()), session.getUserOrNull(getUserName())));
            }
        };
        session.ifTrading(ad);
        return buff.toString();
    }

    public String getMessage() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        ExperimentSubject player = session.getPlayer(getUserName());
        if (player == null) {
            return "";
        }
        String message = player.getHint();
        if (message == null) {
            return "";
        } else {
            return message;
        }
    }

    public String getBalanceMessage() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        User user = session.getUserOrNull(getUserName());
        if (user == null) {
            return "";
        }
        return user.cashOnHand().printAsIntegerQuantity();
    }

    public String getHoldingsMessage() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        BinaryClaim claim = session.getClaim();
        User user = session.getUserOrNull(getUserName());
        if (user == null) {
            return "";
        }
        return user.couponCount(claim).printAsIntegerQuantity();
    }

    public String getReservesRow() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        BinaryClaim claim = session.getClaim();
        User user = session.getUserOrNull(getUserName());
        if (user ==  null || ! user.couponCount(claim).isNegative()) {
            return "";
        }
        Quantity reserves = user.reserveBalance(claim.getNoPosition());
        if (reserves.isZero()) {
            return "";
        }

        StringBuffer buff = new StringBuffer();
        HtmlRow.twoCells(buff, "Reserves", reserves.toString());
        return buff.toString();
    }

    public boolean hasErrorMessages() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return false;
        }
        User user = session.getUserOrNull(getUserName());
        if (user == null) {
            return false;
        }
        return user.hasWarnings();
    }

    public String getErrorMessages() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        User user = session.getUserOrNull(getUserName());
        if (user == null) {
            return "";
        }
        return user.getWarningsHTML();
    }

    public void warn(String s) {
        Session session = SessionSingleton.getSession();
        if (session == null) {
        }
        User user = session.getUserOrNull(getUserName());
        if (user == null) {
            return;
        } else {
            user.warn(s);
        }
    }

    public Market getMarket(String claimName) {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return null;
        }
        return(session.getMarket());
     }

    public User getUser() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return null;
        }
        return session.getUserOrNull(getUserName());
    }

    public void setOrderType(String type) {
        orderType = type;
    }

    public String getCommonMessageLabel() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        return session.getCommonMessageLabel();
    }

    public String getMessageLabel() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        return session.getMessageLabel();
    }

    public String getSharesLabel() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        return session.getSharesLabel();
    }

    public boolean isMarketOpen() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return false;
        }

        Market market = getMarket(getClaimName());
        if (market == null) {
            Logger logger = Logger.getLogger(Log4JHelper.UserError);
            logger.warn(GID.log() + "Market not initialized.  Please wait for the experiment to begin.");
            return false;
        }

        return (! market.isOpen());        
    }

    String printFormForMarketOrder(String orderAction, String price, boolean enabled) {
        String submitField;
        String priceField;
        BinaryMarket market = (BinaryMarket) getMarket(getClaimName());
        String maxValue = market.maxPrice().printAsWholeCents();

        if ("0".equals(price) || maxValue.equals(price) || !enabled) {
            submitField = disabledSubmitField(orderAction);
            priceField = marketPriceField("");
        } else {
            submitField = submitField(orderAction);
            priceField = marketPriceField(price);
        }
        return "<td>\n" +
                formHeader(orderAction + "Market") + "\n" + submitField + " " +
                typeField(MARKET_ORDER) + priceField +
                "\n</form>\n</td>\n";
    }

    String printFormForNewOrder(String orderAction, boolean enabled) {
        String submitField = enabled
                ? submitField(orderAction)
                : disabledSubmitField(orderAction);
        return "<td>\n" +
                formHeader(orderAction + "New") + "\n" + submitField + " " +
                typeField(NEW_ORDER) + blankPriceField() +
                "\n</form>\n</td>\n";
    }

    private String formHeader(String orderAction) {
        return HtmlSimpleElement.
            postFormHeaderWithClass("TraderSubFrame.jsp", orderAction + "OrderForm", "quantity", "1", "userName", getUserName());
    }

    private String typeField(String type) {
        return HtmlSimpleElement.hiddenInputField("orderType", type);
    }

    private String submitField(String inputAction) {
        return HtmlSimpleElement.submitInputField(inputAction);
    }

    private String disabledSubmitField(String inputAction) {
        return HtmlSimpleElement.disabledSubmitInputField(inputAction);
    }

    private String marketPriceField(String price) {
        return HtmlSimpleElement.disabledTextField("price", price);
    }

    private String blankPriceField() {
        return HtmlSimpleElement.textInputField("price");
    }

	public String getRequestURL(HttpServletRequest request) {
		String requestUrl = request.getRequestURL() + "?userName=" + getUserName();
		String queryString = request.getQueryString();
		if (queryString != null && queryString.contains("disabled=true")) {
			requestUrl += "&disabled=true";
		}
		return requestUrl;
	}

    public String getClaimName() {
        return claimName;
    }

    public void setClaimName(String claimName) {
        this.claimName = claimName;
    }

    public boolean isBuy() {
        return action.equalsIgnoreCase(TradeSupport.BUY_ACTION);
    }

    public void setAction(String submitAction) {
        action = submitAction;
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

    public Price p() {
        Market market = getMarket(getClaimName());
        Price max = market.maxPrice();
        Price result;
        try {
            result = new Price(TradeSupport.parseInteger(price, getUser()), max);
        } catch (IllegalArgumentException e) {
            result = new Price(max, max);
        }
        return result;
    }

    public Quantity q() {
        return TradeSupport.parseInteger(quantity, getUser());
    }

    public String defaultJavascriptSettings() {
        String label = Session.getRoundLabelOrDefault();

        return "<script type=\"text/javascript\">\n" +
                "    roundLabel = '" + label + "';\n" +
                "</script>";
    }
}
