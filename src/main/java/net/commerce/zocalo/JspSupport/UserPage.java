package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.JspSupport.TradeHistory;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;

import javax.servlet.http.HttpServletRequest;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public abstract class UserPage extends ReloadablePage {
    private SecureUser user;
    private boolean isAdminUser = false;

    public String navButtons(String currentPage) {
        String[] labels;
        if (isAdminUser()) {
            labels = new String[] {
                    MarketDisplay.MARKET_JSP, MarketDisplay.PAGE_TITLE,
                    AccountDisplay.ACCOUNT_JSP, AccountDisplay.PAGE_TITLE,
                    TradeHistory.HISTORY_JSP, TradeHistory.PAGE_TITLE,
                    MarketCreation.CREATE_MARKETS_JSP, MarketCreation.CREATE_MARKETS,
            };
        } else if (Config.suppressHistoryLinks()) {
            labels = new String[] {
                    MarketDisplay.MARKET_JSP, MarketDisplay.PAGE_TITLE,
                    AccountDisplay.ACCOUNT_JSP, AccountDisplay.PAGE_TITLE,
            };
        } else {
            labels = new String[] {
                    MarketDisplay.MARKET_JSP, MarketDisplay.PAGE_TITLE,
                    AccountDisplay.ACCOUNT_JSP, AccountDisplay.PAGE_TITLE,
                    TradeHistory.HISTORY_JSP, TradeHistory.PAGE_TITLE,
            };
        }
        return HtmlSimpleElement.navButtonTable(labels, currentPage);
    }

    public boolean detectAdminCookie(HttpServletRequest request) {
        isAdminUser = MarketOwner.detectAdminCookie(request);
        return isAdminUser;
    }

    public boolean isAdminUser() {
        return isAdminUser;
    }

    public void setAdminUser(boolean adminUser) {
        isAdminUser = adminUser;
    }

    public void setUserName(String userName) {
        user = MarketOwner.getUser(userName);
    }

    public void warn(String s) {
        getUser().warn(s);
    }

    public String getUserName() {
        if (user == null) {
            return "no User";
        }
        return user.getName();
    }

    public SecureUser getUser() {
        return user;
    }

    void setUser(SecureUser secureUser) {
        this.user = secureUser;
    }
}
