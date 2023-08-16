package net.commerce.zocalo.freechart;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.ajax.events.Ask;
import net.commerce.zocalo.ajax.events.Bid;
import net.commerce.zocalo.ajax.events.Trade;
import net.commerce.zocalo.ajax.events.BookTrade;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.PersistentTestHelper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.*;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;

import java.io.*;
import java.util.Date;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class ChartTest extends PersistentTestHelper {
    protected void setUp() throws Exception {
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBasicGraph() {
        BinaryClaim claim = BinaryClaim.makeClaim("chartClaim", new User("joe", null), "a claim");
        Position yes = claim.getYesPosition();

        Ask ask1 = makeNewAsk("p1", "70", 20, yes);
        Bid bid1 = makeNewBid("p2", "30", 10, yes);
        Ask ask2 = makeNewAsk("p3", "53", 20, yes);
        Trade trade1 = makeNewBookTrade("p4", "53", 10, yes);
        Bid bid2 = makeNewBid("p4", "42", 10, yes);
        Ask ask3 = makeNewAsk("p3", "65", 20, yes);
        Trade trade2 = makeNewBookTrade("p2", "65", 20, yes);
        Bid bid3 = makeNewBid("p1", "45", 10, yes);

        TimePeriodValuesCollection valueColl;
        TimePeriodValues values = new TimePeriodValues("testing");
        values.add(ask1.timeAndPrice());
        values.add(bid1.timeAndPrice());
        values.add(ask2.timeAndPrice());
        values.add(trade1.timeAndPrice());
        values.add(bid2.timeAndPrice());
        values.add(ask3.timeAndPrice());
        values.add(trade2.timeAndPrice());
        values.add(bid3.timeAndPrice());
        assertEquals("Time", values.getDomainDescription());
        assertEquals(ask1.getTime(), values.getTimePeriod(0).getStart());
        assertQEquals(values.getValue(3).doubleValue(), trade1.getPrice());
        assertEquals(bid3.getTime(), values.getTimePeriod(7).getEnd());
        assertQEquals(values.getValue(7).doubleValue(), bid3.getPrice());

        valueColl = new TimePeriodValuesCollection(values);
        assertTrue(valueColl.getDomainIsPointsInTime());
    }

    public void testOHLCChart() throws IOException {
        File jpgFile = newEmptyFile(".", "testChart.jpg");
        File pngFile = newEmptyFile(".", "testChart.png");
        assertFalse(jpgFile.exists());
        assertFalse(pngFile.exists());

        OutputStream jpgStream = new FileOutputStream(jpgFile);
        OutputStream pngStream = new FileOutputStream(pngFile);

        OHLCDataset data2 = createOHLCDataSet(new Minute());
        JFreeChart chart = ChartFactory.createHighLowChart("testChart", "date", "values",  data2, false);

        ChartUtilities.writeChartAsJPEG(jpgStream, chart, 200, 500);
        ChartUtilities.writeChartAsPNG(pngStream, chart, 200, 500);

        assertTrue(jpgFile.exists());
        assertTrue(pngFile.exists());
        jpgFile.delete();
        pngFile.delete();
    }

    private static OHLCDataset createOHLCDataSet(Minute now) {
        OHLCDataItem item1 = new OHLCDataItem(new Date(now.getFirstMillisecond()),               .34, .40, .22, .22, 5);
        OHLCDataItem item2 = new OHLCDataItem(new Date(now.getFirstMillisecond() + (1000 *  5)), .22, .35, .21, .31, 54);
        OHLCDataItem item3 = new OHLCDataItem(new Date(now.getFirstMillisecond() + (1000 * 12)), .31, .35, .30, .34, 4);
        OHLCDataItem item4 = new OHLCDataItem(new Date(now.getFirstMillisecond() + (1000 * 18)), .34, .43, .32, .41, 8);
        OHLCDataItem item5 = new OHLCDataItem(new Date(now.getFirstMillisecond() + (1000 * 35)), .41, .41, .20, .21, 14);
        OHLCDataItem item6 = new OHLCDataItem(new Date(now.getFirstMillisecond() + (1000 * 42)), .21, .40, .21, .40, 14);
        OHLCDataItem item7 = new OHLCDataItem(new Date(now.getFirstMillisecond() + (1000 * 54)), .40, .53, .40, .52, 14);
        OHLCDataItem[] items = {item1, item2, item3, item4, item5, item6, item7 } ;
        return new DefaultOHLCDataset(new Comparable() { public int compareTo(Object object) { return 1; } }, items );
    }

    public TimePeriodValuesCollection makeTimePeriodValues() {
        TimePeriodValues values = new TimePeriodValues("testing");
        Minute now = new Minute();

        values.add(new TimePeriodValue(new Second(2, now), .2));
        values.add(new TimePeriodValue(new Second(15, now), .5));
        values.add(new TimePeriodValue(new Second(22, now), .2));
        values.add(new TimePeriodValue(new Second(32, now), .3));
        values.add(new TimePeriodValue(new Second(45, now), .9));

        return new TimePeriodValuesCollection(values);
    }

    public void testGraphWriter() throws IOException {
        String fileName = "timeChart";
        TimePeriodValuesCollection values = makeTimePeriodValues();
        assertFalse(newEmptyFile("TEST", fileName).exists());

        ensureDirExists(new File("data"));
        Date lastTrade = new Date(0);

        File pngFile = ChartGenerator.writeChartFile(".", "data", fileName, values, ChartGenerator.CHART_SIZE, lastTrade);
        assertTrue(pngFile.exists());
    }

    private void ensureDirExists(File testDir) {
        if (!testDir.exists()) {
            testDir.mkdir();
        }
    }

    public void testEmptyGraph() throws IOException {
        String fileName = "emptyChart";
        TimePeriodValuesCollection values =
                new TimePeriodValuesCollection(new TimePeriodValues("testing"));
        assertFalse("should have created " + fileName + ".png", newEmptyFile("TEST", fileName).exists());

        ensureDirExists(new File("data"));
        Date lastTrade = new Date(0);
        File pngFile = ChartGenerator.writeChartFile(".", "data", fileName, values, ChartGenerator.CHART_SIZE, lastTrade);
        assertEquals(new File("./data/" + fileName + "-p.png"), new File(pngFile.getPath()));
        assertTrue("empty file should be small", 10000 > pngFile.length());
    }

    public void testSimpleBarChart() throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String seriesKey = "prices";
        dataset.addValue(.2, seriesKey, "apples");
        dataset.addValue(.15, seriesKey, "bananas");
        dataset.addValue(.18, seriesKey, "cherries");
        dataset.addValue(.32, seriesKey, "watermelon");
        dataset.addValue(.06, seriesKey, "peaches");
        dataset.addValue(.09, seriesKey, "persimmons");

        int hSize = 300;
        int vSize = 150;
        JFreeChart chart = ChartGenerator.buildBarChart(dataset, PlotOrientation.HORIZONTAL, hSize, vSize);

        File jpgFile = newEmptyFile(".", "BarChart.jpg");
        File pngFile = newEmptyFile(".", "BarChart.png");
        assertFalse(jpgFile.exists());
        assertFalse(pngFile.exists());

        OutputStream jpgStream = new FileOutputStream(jpgFile);
        OutputStream pngStream = new FileOutputStream(pngFile);

        ChartUtilities.writeChartAsJPEG(jpgStream, chart, hSize, vSize);
        ChartUtilities.writeChartAsPNG(pngStream, chart, hSize, vSize);

        assertTrue(jpgFile.exists());
        assertTrue(pngFile.exists());
        jpgFile.delete();
        pngFile.delete();
    }

    public void testMultiGraph() throws IOException {
        String fileName = "multiChart";
        TimePeriodValuesCollection series = makeupSeriesValues();
        assertFalse(newEmptyFile("TEST", fileName).exists());

        ensureDirExists(new File("data"));
        Date lastTrade = new Date(0);
        File pngFile = ChartGenerator.writeMultiChartFile(".", "data", fileName, series, ChartGenerator.CHART_SIZE, lastTrade);
        assertTrue(pngFile.exists());
    }

    public void testMultiOpenCloseGraph() throws IOException {
        String fileName = "openCloseChart";
        TimePeriodValuesCollection series = makeupSeriesValues();
        assertFalse(newEmptyFile("TEST", fileName).exists());

        ensureDirExists(new File("data"));
        Date lastTrade = new Date(0);

        File pngFile = ChartGenerator.writeMultiStepChartFile(".", "data", fileName, series, ChartGenerator.CHART_SIZE, lastTrade);
        assertTrue(pngFile.exists());
    }

    private TimePeriodValuesCollection makeupSeriesValues() {
        TimePeriodValues aValues = new TimePeriodValues("volleyball");
        TimePeriodValues bValues = new TimePeriodValues("climbing");
        TimePeriodValues cValues = new TimePeriodValues("hockey");
        Minute now = new Minute();

        aValues.add(new TimePeriodValue(new Second(2, now), .2));
        bValues.add(new TimePeriodValue(new Second(2, now), .4));
        cValues.add(new TimePeriodValue(new Second(2, now), .4));
        aValues.add(new TimePeriodValue(new Second(14, now), .25));
        bValues.add(new TimePeriodValue(new Second(14, now), .35));
        cValues.add(new TimePeriodValue(new Second(14, now), .4));
        aValues.add(new TimePeriodValue(new Second(22, now), .26));
        bValues.add(new TimePeriodValue(new Second(22, now), .35));
        cValues.add(new TimePeriodValue(new Second(22, now), .39));
        aValues.add(new TimePeriodValue(new Second(32, now), .23));
        bValues.add(new TimePeriodValue(new Second(32, now), .37));
        cValues.add(new TimePeriodValue(new Second(32, now), .38));
        aValues.add(new TimePeriodValue(new Second(45, now), .33));
        bValues.add(new TimePeriodValue(new Second(45, now), .31));
        cValues.add(new TimePeriodValue(new Second(45, now), .37));
        aValues.add(new TimePeriodValue(new Second(58, now), .33));
        bValues.add(new TimePeriodValue(new Second(58, now), .31));
        cValues.add(new TimePeriodValue(new Second(58, now), .37));

        TimePeriodValuesCollection series = new TimePeriodValuesCollection();

        series.addSeries(aValues);
        series.addSeries(bValues);
        series.addSeries(cValues);
        return series;
    }

    public void testXYAreaStepChart() throws IOException {
        Minute now = new Minute();

        TimePeriodValuesCollection aSeries = createBottomValues(now);
        TimePeriodValuesCollection bSeries = createTopValues(now);

        File jpgFile = newEmptyFile(".", "StepAreaChart.jpg");
        File pngFile = newEmptyFile(".", "StepAreaChart.png");
        assertFalse(jpgFile.exists());
        assertFalse(pngFile.exists());

        OutputStream jpgStream = new FileOutputStream(jpgFile);
        OutputStream pngStream = new FileOutputStream(pngFile);

        JFreeChart chart = ChartGenerator.createCustomXYStepAreaChart(aSeries, bSeries);

        ChartUtilities.writeChartAsJPEG(jpgStream, chart, 500, 500);
        ChartUtilities.writeChartAsPNG(pngStream, chart, 500, 500);

        assertTrue(jpgFile.exists());
        assertTrue(pngFile.exists());
        jpgFile.delete();
        pngFile.delete();
    }

    private File newEmptyFile(String webDir, String fileName) {
        File jpgFile21 = new File(webDir, fileName);
        jpgFile21.delete();
        return jpgFile21;
    }

    private TimePeriodValuesCollection createTopValues(Minute now) {
        TimePeriodValues bValues = new TimePeriodValues("hockey");
        bValues.add(new TimePeriodValue(new Second(2, now), .6));
        bValues.add(new TimePeriodValue(new Second(12, now), .55));
        bValues.add(new TimePeriodValue(new Second(24, now), .46));
        bValues.add(new TimePeriodValue(new Second(35, now), .21));
        bValues.add(new TimePeriodValue(new Second(42, now), .43));
        bValues.add(new TimePeriodValue(new Second(54, now), .53));
        bValues.add(new TimePeriodValue(new Second(58, now), .53));
        TimePeriodValuesCollection bSeries = new TimePeriodValuesCollection();
        bSeries.addSeries(bValues);
        return bSeries;
    }

    private TimePeriodValuesCollection createBottomValues(Minute now) {
        TimePeriodValues aValues = new TimePeriodValues("volleyball");
        aValues.add(new TimePeriodValue(new Second(2, now), .2));
        aValues.add(new TimePeriodValue(new Second(12, now), .25));
        aValues.add(new TimePeriodValue(new Second(29, now), .26));
        aValues.add(new TimePeriodValue(new Second(35, now), .18));
        aValues.add(new TimePeriodValue(new Second(45, now), .33));
        aValues.add(new TimePeriodValue(new Second(58, now), .33));
        TimePeriodValuesCollection aSeries = new TimePeriodValuesCollection();
        aSeries.addSeries(aValues);
        return aSeries;
    }

    public void testOverlaidOHLCPlusStepChart() throws IOException {
        Minute now = new Minute();
        File jpgFile = newEmptyFile(".", "OverlaidChart.jpg");
        File pngFile = newEmptyFile(".", "OverlaidChart.png");

        assertFalse(jpgFile.exists());
        assertFalse(pngFile.exists());

        TimePeriodValuesCollection bottom = createBottomValues(now);
        TimePeriodValuesCollection top = createTopValues(now);

        OHLCDataset OHLCdata = createOHLCDataSet(now);
        JFreeChart chart = ChartGenerator.createOverlaidOHLCAndStepChart(bottom, top, OHLCdata);

        OutputStream jpgStream = new FileOutputStream(jpgFile);
        OutputStream pngStream = new FileOutputStream(pngFile);
        ChartUtilities.writeChartAsJPEG(jpgStream, chart, 500, 500);
        ChartUtilities.writeChartAsPNG(pngStream, chart, 500, 500);

        assertTrue(jpgFile.exists());
        assertTrue(pngFile.exists());
        jpgFile.delete();
        pngFile.delete();
    }
}
