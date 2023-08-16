package net.commerce.zocalo.JspSupport;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.html.*;
import net.commerce.zocalo.JspSupport.printOrders.NoCancelOrdersPrintStrategy;
import net.commerce.zocalo.JspSupport.printOrders.OrdersPrintStrategy;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.currency.Accounts;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** support for a JSP page that displays a user's accounts.   */
public class AccountDisplay extends UserPage {
    static final public String ACCOUNT_JSP = "account.jsp";
    static final public String ACCOUNT_NAME = "account";
    static final public String PAGE_TITLE = "View Account";
    static final public String HOLDINGS_LABEL = "Holdings";
    public static final String HOLDINGS_BG_COLOR = "tan";

    public AccountDisplay() {
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        setUser(MarketOwner.registryLookup(request));
        if (getUser() == null) {
            redirectResult(request, response);
        }
        detectAdminCookie(request);
    }

    public String descriptionHtml() {
        StringBuffer buf = new StringBuffer();
        buf.append("<h1>" + getUserName() + "</h1>");

        SecureUser user = getUser();
        if (user == null) {
            return "Please login via the market page.";
        }
        buf.append(HtmlSimpleElement.labelledLine("Cash", "$" + user.cashOnHand().printAsDollars()));

        user.displayAccounts(buf, allHoldingsPrinter(user));

        NoCancelOrdersPrintStrategy strat = OrdersPrintStrategy.makeNoCancel("lightGreen", "orange");
        strat.renderTable(buf, user);
        return buf.toString();
    }

    public String getRequestURL(HttpServletRequest request) {
        if (getUser() == null) {
            return WelcomeScreen.WELCOME_JSP;
        }
        return null;
    }

    public String navButtons() {
        return navButtons(ACCOUNT_JSP);
    }

    static public  Accounts.PositionDisplayAdaptor allHoldingsPrinter(final User user) {
        return new DefaultPositionDisplayAdaptor() {
            public void printHeader(StringBuffer buf) {
                aboveAccountsTable(buf);
                String[] headers = {"Claim Name", "Position", HOLDINGS_LABEL};
                HtmlTable.start(buf, HOLDINGS_BG_COLOR, headers);
            }

            public void printRow(StringBuffer buf, Position pos, Quantity count) {
                HtmlElement[] cells = new HtmlElement[] {
                    HtmlSimpleElement.claimLink(pos.getClaim().getName(), ClaimPurchase.claimPurchasePage(user)),
                    HtmlSimpleElement.centeredCell(pos.getName()),
                    HtmlSimpleElement.centeredCell(count.printAsDetailedQuantity()),
                };
                new HtmlRow(cells).render(buf);
            }
        };
    }

    public static void postTable(StringBuffer buf) {
        HtmlTable.endTagWithP(buf);
        HtmlTable.endTagWithP(buf);
    }

    public static void emptyTable(StringBuffer buf) {
        HtmlSimpleElement.printEmptyOrders(buf, HOLDINGS_LABEL);
    }

    static public Accounts.PositionDisplayAdaptor claimHoldingsMultiMarketPrinter(final SecureUser user) {
        return new DefaultPositionDisplayAdaptor() {
            public void printHeader(StringBuffer buf) {
                aboveAccountsTable(buf);
                String[] headers = { "Position", HOLDINGS_LABEL, "Action" };
                HtmlTable.start(buf, HOLDINGS_BG_COLOR, headers);
            }

            public void printRow(StringBuffer buf, Position pos, Quantity count) {
                String pageName = ClaimPurchase.claimPurchasePage(user);
                HtmlElement[] cells;
                cells = new HtmlElement[] {
                        HtmlSimpleElement.centeredCell(pos.getName()),
                        HtmlSimpleElement.centeredCell(count.printAsDetailedQuantity()),
                        HtmlSimpleElement.centeredCell(sellHoldingsButton(pos, pageName)),
                };
                new HtmlRow(cells).render(buf);
            }
        };
    }

    static private String sellHoldingsButton(Position position, final String pageName) {
        StringBuffer buf = new StringBuffer();

        buf.append(
            HtmlSimpleElement.simplePostFormHeader(pageName, "",
                "positionName", position.getName(), "claimName", position.getClaim().getName()));

        String submitButton = HtmlSimpleElement.submitInputField("sell holdings");
        buf.append(submitButton);
        buf.append("</form>");
        return buf.toString();
    }
    
    static public Accounts.PositionDisplayAdaptor claimHoldingsBinaryMarketPrinter() {
        return new DefaultPositionDisplayAdaptor() {
            public void printHeader(StringBuffer buf) {
                aboveAccountsTable(buf);
                String[] headers = { "Position", HOLDINGS_LABEL };
                HtmlTable.start(buf, HOLDINGS_BG_COLOR, headers);
            }

            public void printRow(StringBuffer buf, Position pos, Quantity count) {
                HtmlElement[] cells;
                cells = new HtmlElement[] {
                        HtmlSimpleElement.centeredCell(pos.getName()),
                        HtmlSimpleElement.centeredCell(count.printAsDetailedQuantity()),
                };
                new HtmlRow(cells).render(buf);
            }
        };
    }

    static public void aboveAccountsTable(StringBuffer buf) {
        HtmlTable.start(buf, HOLDINGS_BG_COLOR, HtmlTable.BORDER, "0");
        buf.append("<tr><td>\n   ");
        HtmlSimpleElement.printHeader(buf, 2, HOLDINGS_LABEL);
        buf.append("</td></tr>\n   ");
        buf.append("<tr><td>\n   ");
    }

    abstract public static class DefaultPositionDisplayAdaptor implements Accounts.PositionDisplayAdaptor {
        public void printEmpty(StringBuffer buf) { emptyTable(buf); }
        public void afterTable(StringBuffer buf) { postTable(buf); }
    }
}
