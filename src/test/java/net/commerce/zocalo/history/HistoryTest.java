package net.commerce.zocalo.history;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.html.HtmlSimpleElement;
import org.jfree.data.time.TimePeriodValuesCollection;

import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;
// Copyright 2007 Chris Hibbert.  Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class HistoryTest extends TestCase {
    private final String password = "secure";
    private BinaryClaim weather;
    private MultiClaim climate;

    protected void setUp() throws Exception {
        CashBank bank = new CashBank("money");
        SecureUser owner = new SecureUser("owner", bank.makeFunds(300), password, "someone@example.com");
        weather = BinaryClaim.makeClaim("rain", owner, "will it rain tomorrow?");
        climate = MultiClaim.makeClaim("climate", owner, "what climate should we expect?", new String[] {"hot", "cold", "wet", "dry"} );
    }

    public void testBinaryBookHistory() {
        Date baseDate = new Date();

        PriceHistory hist = new PriceHistory(weather);
        Date timeA = new Date(baseDate.getTime() + 400);
        hist.add(timeA, .2, .25, .15, .3);
        Date timeB = new Date(baseDate.getTime() + 1220);
        hist.add(timeB, .25, .22, .2, .27);
        TimePeriodValuesCollection vals = hist.getCollection(weather.getYesPosition());
        assertTrue(vals.getDomainIsPointsInTime());
        assertEquals(.2, vals.getSeries(0).getDataItem(0).getValue().doubleValue(), .01);
        assertEquals(.22, vals.getSeries(1).getDataItem(1).getValue().doubleValue(), .01);
        assertEquals(.2, vals.getSeries(2).getDataItem(1).getValue().doubleValue(), .01);
        assertEquals(.3, vals.getSeries(3).getDataItem(0).getValue().doubleValue(), .01);
        assertEquals(timeA, vals.getSeries(0).getDataItem(0).getPeriod().getStart());
        assertEquals(timeB, vals.getSeries(2).getDataItem(1).getPeriod().getEnd());
    }

    public void testBinaryMarketMakerHistory() {
        Date baseDate = new Date();

        PriceHistory hist = new PriceHistory(weather);
        Date timeA = new Date(baseDate.getTime() + 400);
        hist.add(timeA, .2, .25);
        Date timeB = new Date(baseDate.getTime() + 1220);
        hist.add(timeB, .25, .35);
        TimePeriodValuesCollection vals = hist.getCollection(weather.getYesPosition());
        assertTrue(vals.getDomainIsPointsInTime());
        assertEquals(4, vals.getSeriesCount());
        assertEquals(2, vals.getItemCount(0));
        assertEquals(2, vals.getItemCount(1));
        assertEquals(0, vals.getItemCount(2));
        assertEquals(0, vals.getItemCount(3));
        assertEquals(vals.getSeries(0).getDataItem(0).getValue().doubleValue(), .2, .01);
        assertEquals(vals.getSeries(1).getDataItem(1).getValue().doubleValue(), .35, .01);
        assertEquals(timeA, vals.getSeries(1).getDataItem(0).getPeriod().getStart());
        assertEquals(timeB, vals.getSeries(0).getDataItem(1).getPeriod().getEnd());
    }

    public void testMultiMarketMakerHistory() {
        Date baseDate = new Date();
        Position hot = climate.lookupPosition("hot");
        Position cold = climate.lookupPosition("cold");
        Position wet = climate.lookupPosition("wet");
        Position dry = climate.lookupPosition("dry");

        PriceHistory hist = new PriceHistory(climate);
        Date timeA = new Date(baseDate.getTime() + 400);
        hist.add(timeA, hot, .2, .25);
        hist.add(timeA, wet, .2, .25);
        hist.add(timeA, cold, .3, .25);
        hist.add(timeA, dry, .3, .25);
        Date timeB = new Date(baseDate.getTime() + 1220);
        hist.add(timeB, hot, .25, .23);
        hist.add(timeB, wet, .25, .19);
        hist.add(timeB, dry, .25, .29);
        hist.add(timeB, cold, .25, .29);

        TimePeriodValuesCollection hotVals = hist.getCollection(hot);
        assertTrue(hotVals.getDomainIsPointsInTime());
        assertEquals(4, hotVals.getSeriesCount());
        assertEquals(2, hotVals.getItemCount(0));
        assertEquals(2, hotVals.getItemCount(1));
        assertEquals(0, hotVals.getItemCount(2));
        assertEquals(0, hotVals.getItemCount(3));
        assertEquals(.2, hotVals.getSeries(0).getDataItem(0).getValue().doubleValue(), .01);
        assertEquals(.23, hotVals.getSeries(1).getDataItem(1).getValue().doubleValue(), .01);
        assertEquals(timeA, hotVals.getSeries(1).getDataItem(0).getPeriod().getStart());
        assertEquals(timeB, hotVals.getSeries(0).getDataItem(1).getPeriod().getEnd());
    }

    public void testDatePrinting() {
        Date baseDate = new Date();
        TimeZone GMT = TimeZone.getTimeZone("GMT");
        TimeZone LOSANGELES = TimeZone.getTimeZone("America/Los_Angeles");
        TimeZone BOSTON = TimeZone.getTimeZone("EST");

        SimpleDateFormat laPrinter = new SimpleDateFormat("MM/dd/yy HH:mm");
        laPrinter.setTimeZone(LOSANGELES);
        SimpleDateFormat bostonPrinter = new SimpleDateFormat("MM/dd/yy HH:mm");
        bostonPrinter.setTimeZone(BOSTON);
        SimpleDateFormat gmtPrinter = new SimpleDateFormat("MM/dd/yy HH:mm");
        gmtPrinter.setTimeZone(GMT);

        String boston = bostonPrinter.format(baseDate);
        String losAngeles = laPrinter.format(baseDate);
        String gmt = gmtPrinter.format(baseDate);

        assertNotSame(boston, losAngeles);
        assertNotSame(losAngeles, gmt);
        assertNotSame(boston, gmt);
    }
}
