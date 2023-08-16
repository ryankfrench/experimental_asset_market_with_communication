package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.MultiMarket;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.currency.Quantity;
import static net.commerce.zocalo.service.PropertyKeywords.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** support for a JSP page for creating new Markets. */
public class MarketCreation extends UserPage {
    static final public String CREATE_MARKETS_JSP = "createMarkets.jsp";
    static final public String CREATE_MARKETS_NAME = "createMarkets";
    public static final String CREATE_MARKETS = "Create Markets";
    private String marketName = "";
    private String marketMakerEndowment = "";
    private String positions = "";
    private String outcomes = "";

    public MarketCreation() {
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        setUser(MarketOwner.registryLookup(request));
        detectAdminCookie(request);
        if (getUser() == null) {
            redirectResult(request, response);
        } else if (marketName == null || marketName.length() == 0) {
            getUser().warn("Please specify a name for the market.");
        } else if (MarketOwner.getMarket(marketName.trim()) != null) {
            getUser().warn("A market with the name '" + marketName + "' already exists.");
        } else if (!isAdminUser()) {
            getUser().warn("Intruder Alert!  Please log in with the admin password to create markets.");
        } else if (HtmlSimpleElement.detectHtmlLinkSpecial(marketName)) {
            getUser().warn("HTML Special Characters are not allowed in market names.");
        } else {
            createClaim();
            return;
        }
            redirectResult(request, response);
    }

    private void createClaim() {
        if (outcomes.trim().equals("multi")) {
            initializeMultiClaim();
        } else {
            initializeBinaryClaim();
        }
    }

    private void initializeMultiClaim() {
        if (marketMakerEndowment == null || marketMakerEndowment.equals("")) {
            getUser().addWarning("Multi-outcome markets must have a Market Maker.  Please specify the Market Maker's endowment.");
            return;
        }

        Quantity initialEndowment = new Quantity(marketMakerEndowment).times(Quantity.Q100);
        boolean sufficientFunds = getUser().cashOnHand().compareTo(initialEndowment) >= 0;
        if (! sufficientFunds) {
            getUser().addWarning("You can't afford an endowment of " + marketMakerEndowment);
            return;
        }

       if (! verifyMarketName()) {
            return;
        }
        MultiMarket market = MarketOwner.newMultiMarket(marketName.trim(), getUser(), positions.split(COMMA_SPLIT_RE));
        if (initialEndowment.isPositive()) {
            market.makeMarketMaker(initialEndowment, getUser());
        }
    }

    private void initializeBinaryClaim() {
        if (marketMakerEndowment == null || marketMakerEndowment.equals("")) {
            MarketOwner.newBinaryMarket(marketName, getUser());
            return;
        }

        Quantity initialEndowment = new Quantity(Integer.parseInt(marketMakerEndowment));
        boolean hasEnufCash = getUser().cashOnHand().compareTo(initialEndowment) >= 0;
        if (! initialEndowment.isPositive() || !hasEnufCash) {
            getUser().addWarning("You can't afford to fund the market maker with " + initialEndowment + ".");
            return;
        }

        if (! verifyMarketName()) {
            return;
        }
        BinaryMarket market = MarketOwner.newBinaryMarket(marketName.trim(), getUser());
        if (initialEndowment.isPositive()) {
            market.makeMarketMaker(getUser(), initialEndowment);
        }
    }

//// REPORTING ///////////////////////////////////////////////////////////////

    public String navButtons() {
        return navButtons(CREATE_MARKETS_JSP);
    }

    public String getRequestURL(HttpServletRequest request) {
        if (getUser() == null) {
            return WelcomeScreen.WELCOME_JSP;
        } else {
            if (! verifyMarketName()) {
                return "";
            }
            String name = marketName.trim();
            if (MarketOwner.getMarket(name) != null) {
                return ClaimPurchase.claimPurchasePage(getUser()) + "?claimName=" + name;
            } else {
                return null;
            }
        }
    }

    private boolean verifyMarketName() {
        if (marketName == null) {
            getUser().addWarning("Please specify a name for the market.");
            return false;
        }
        return true;
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

    public void setPositions(String positions) {
        this.positions = positions;
    }

    public String getPositions() {
        return positions;
    }

    public String getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(String outcomes) {
        this.outcomes = outcomes;
    }
}
