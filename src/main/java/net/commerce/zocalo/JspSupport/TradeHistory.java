package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.ajax.events.Trade;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.claim.Position;
import org.jfree.data.time.TimePeriodValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** TradeHistory displays trading history in a tabular form. */
public class TradeHistory extends UserPage {
    static final public String HISTORY_JSP = "history.jsp";
    static final public String HISTORY_NAME = "history";
    private SecureUser currentUser;
    static final public SimpleDateFormat datePrinter = new SimpleDateFormat("MM/dd/yy HH:mm z");
    public static final String PAGE_TITLE = "Trade History";

    public TradeHistory() {
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        currentUser = MarketOwner.registryLookup(request);
        if (currentUser == null) {
            redirectResult(request, response);
        }
        setUserName(currentUser.getName());
        detectAdminCookie(request);
    }

    public String tradeTable() {
        List trades;
        try {
            trades = HibernateUtil.getTrades(currentUser);
        } catch (NullPointerException e) {
            trades = null;
        }

        StringBuffer buff = new StringBuffer();
        if (trades != null) {
            HtmlTable.start(buff, "lightblue", new String[] { "claim", "date", "price", "quant" }, "orderTable");
            for (Iterator iterator = trades.iterator(); iterator.hasNext();) {
                Trade trade = (Trade) iterator.next();
                Position pos = trade.getPos();
                printRow(buff, trade, pos.getSimpleName());
            }
            HtmlTable.endTag(buff);
        }
        return buff.toString();
    }

    static public void printRow(StringBuffer buff, Trade trade, String posLabel) {
        TimePeriodValue timeVolume = trade.timeAndVolume();
        double quant = timeVolume.getValue().doubleValue();
        String quantString = NumberDisplay.printAsQuantity(quant);
        if (quant == 0 || "0".equals(quantString)) {
            return;
        }

        HtmlRow.startTag(buff);
        if (posLabel != null) {
            buff.append(HtmlSimpleElement.printTableCell(posLabel));
        }
        Date tradeTime = timeVolume.getPeriod().getStart();
        buff.append(HtmlSimpleElement.printTableCell(datePrinter.format(tradeTime)));
        buff.append(HtmlSimpleElement.printTableCell(trade.getPrice().printHighPrecision()));
        String buySell = trade.isBuy() ? "Buy " : "Sell ";
        buff.append(HtmlSimpleElement.printTableCell(buySell + NumberDisplay.printAsQuantity(quant)));
        HtmlRow.endTag(buff);
    }

    public String getRequestURL(HttpServletRequest request) {
        if (currentUser == null) {
            return WelcomeScreen.WELCOME_JSP;
        } else {
            return null;
        }
    }

    public SecureUser getUser() {
        return currentUser;
    }

    public String navButtons() {
        return navButtons(HISTORY_JSP);
    }
}
