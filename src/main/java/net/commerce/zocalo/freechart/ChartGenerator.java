package net.commerce.zocalo.freechart;

import net.commerce.zocalo.ajax.events.MakerTrade;
import net.commerce.zocalo.ajax.events.NewChart;
import net.commerce.zocalo.ajax.events.Trade;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.TimePeriodValue;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.xy.OHLCDataset;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

// Copyright 2006-2010 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**
Manage use of <a href="http://jfreechart.org">JFreeChart</a>.
 */
public class ChartGenerator {
    public static final int CHART_SIZE = 250;
    private static final JFreeChart EMPTY_CHART = priceHistoryChart("No Transactions", null);

    static public File writeChartFile(String webDirName, String chartDirName, String name,
                          TimePeriodValuesCollection prices, int chartSize, Date lastTrade)
            throws IOException {
        String chartStyle = "p";
        File existingPngFile = pngFile(webDirName, chartDirName, name, chartStyle);
        if (isFileMoreRecent(lastTrade, existingPngFile)) {
            return existingPngFile;
        }

        JFreeChart chart = EMPTY_CHART;
        if (prices.getItemCount(0) != 0) {
            chart = priceHistoryChart(name, prices);
        }
        return writeChartAsPNG(chartSize, chart, existingPngFile);
    }

    static public File writeMultiChartFile(String webDirName, String chartDirName, String name,
                               TimePeriodValuesCollection prices, int chartSize, Date lastTrade)
            throws IOException {
        String chartStyle = "p";
        File existingPngFile = pngFile(webDirName, chartDirName, name, chartStyle);
        if (isFileMoreRecent(lastTrade, existingPngFile)) {
            return existingPngFile;
        }

        JFreeChart chart = EMPTY_CHART;
        if (prices.getItemCount(0) != 0) {
            chart = multiPriceHistoryChart(prices);
        }
        return writeChartAsPNG(chartSize, chart, existingPngFile);
    }

    static public File writeMultiStepChartFile(String webDirName, String chartDirName, String name,
                               TimePeriodValuesCollection prices, int chartSize, Date lastTrade)
            throws IOException {
        String chartStyle = "p";
        File existingPngFile = pngFile(webDirName, chartDirName, name, chartStyle);
        if (isFileMoreRecent(lastTrade, existingPngFile)) {
            return existingPngFile;
        }

        JFreeChart chart = EMPTY_CHART;
        if (prices.getItemCount(0) != 0) {
            chart = multiStepChart(prices);
        }
        return writeChartAsPNG(chartSize, chart, existingPngFile);
    }

    static public File writeMultiStepChartFile(String webDirName, String chartDirName,
                                               final int chartSize, Date lastTrade, final Claim claim)
            throws IOException {
        String chartStyle = "p";
        final String market = claim.getName();
        final File existingPngFile = pngFile(webDirName, chartDirName, market, chartStyle);
        if (isFileMoreRecent(lastTrade, existingPngFile)) {
            return existingPngFile;
        }

        Callable<Boolean> worker = new Callable<Boolean>() {
            public Boolean call() throws Exception {
                List trades = HibernateUtil.tradeListForJsp(market);
                TimePeriodValuesCollection prices = ChartGenerator.getOpenCloseValues(trades, claim);
                JFreeChart chart = EMPTY_CHART;
                if (prices.getItemCount(0) != 0) {
                    chart = multiStepChart(prices);
                }
                try {
                    writeChartAsPNG(chartSize, chart, existingPngFile);
                    new NewChart(market);
                    return true;
                } catch (IOException e) {
                    Logger log = Logger.getLogger("trace");
                    log.error("Couldn't save updated chart for '" + market + "'.", e);
                    return false;
                }
            }
        };
        ChartScheduler sched = ChartScheduler.create(market, worker);
        sched.generateNewChart();
        return existingPngFile;
    }

    public static File updateChartFile(String webDirName, String chartDirName,
                                       int chartSize, Date lastTrade, String claimName)
            throws IOException {
        return updateChartFile(webDirName, chartDirName, lastTrade, claimName, false, chartSize, chartSize);
    }

    public static File updateChartFile(String webDirName, String chartDirName, Date lastTrade, final String claimName,
                                       final boolean scalePrices, final int chartHeight, final int chartWidth)
            throws IOException {
        String chartStyle = "pv";
        final File existingPngFile = pngFile(webDirName, chartDirName, claimName, chartStyle);
        if (isFileMoreRecent(lastTrade, existingPngFile)) {
            return existingPngFile;
        }

        Callable<Boolean> worker = new Callable<Boolean>() {
            public Boolean call() throws Exception {
                List trades = HibernateUtil.tradeListForJsp(claimName);
                TimePeriodValuesCollection prices = ChartGenerator.getHistoricalPrices(claimName, trades);
                JFreeChart chart = EMPTY_CHART;
                if (prices.getItemCount(0) != 0) {
                    TimePeriodValuesCollection volumes = ChartGenerator.getHistoricalVolumes(claimName, trades);
                    chart = priceVolumeHistoryChart("", prices, volumes, scalePrices);
                }
                try {
                    writeChartAsPNG(chartWidth, chartHeight, chart, existingPngFile);
                    new NewChart(claimName);
                    return true;
                } catch (IOException e) {
                    Logger log = Logger.getLogger("trace");
                    log.error("Couldn't save updated chart for '" + claimName + "'.", e);
                    return false;
                }
            }
        };
        ChartScheduler sched = ChartScheduler.create(claimName, worker);
        sched.generateNewChart();

        return existingPngFile;
    }

    private static File writeChartAsPNG(int chartSize, JFreeChart chart, File existingPngFile)
            throws IOException
    {
        return writeChartAsPNG(chartSize, chartSize, chart, existingPngFile);
    }

    private static File writeChartAsPNG(int chartWidth, int chartHeight, JFreeChart chart, File existingPngFile)
            throws IOException
    {
        if (! existingPngFile.getParentFile().exists()) {
            existingPngFile.getParentFile().mkdirs();
        }
        File pngFile = new File(existingPngFile.getAbsolutePath() + ".next");
        OutputStream pngStream = new FileOutputStream(pngFile);
        ChartUtilities.writeChartAsPNG(pngStream, chart, chartWidth, chartHeight);
        pngStream.close();
        if (pngFile.renameTo(existingPngFile)) {
            return existingPngFile;
        } else {
            return pngFile;
        }
    }

    static public File pngFile(String webDirName, String chartDirName, String name, String chartStyle) {
        return new File(dirName(webDirName, chartDirName), fileName(name, chartStyle));
    }

    static public boolean isFileMoreRecent(Date lastTrade, File pngFile) {
        long lastMod = pngFile.lastModified();
        return pngFile.exists() && lastMod > lastTrade.getTime();
    }

    static private String fileName(String name, String chartStyle) {
        return name + "-" + chartStyle + ".png";
    }

    private static String dirName(String webDirName, String chartDirName) {
        return webDirName + "/" + chartDirName;
    }

    private static JFreeChart priceVolumeHistoryChart(String title, TimePeriodValuesCollection prices,
                                                      TimePeriodValuesCollection volumes, boolean scalePrices) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, null, "Price", prices, false, false, false);
        XYPlot plot = chart.getXYPlot();
        if (scalePrices) {
        	setBoundsForPrice(chart);
        } else {
        	setBoundsForPercent(chart);
		}

        NumberAxis rangeAxis2 = new NumberAxis("Volume");
        rangeAxis2.setUpperMargin(2);
        plot.setRangeAxis(1, rangeAxis2);
        plot.setDataset(1, volumes);
        plot.mapDatasetToRangeAxis(1, 1);
        XYBarRenderer renderer2 = new XYBarRenderer(0.2);
        plot.setRenderer(1, renderer2);
        return chart;
    }

    private static JFreeChart priceHistoryChart(String title, TimePeriodValuesCollection prices) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, null, "Price", prices, false, false, false);
        setBoundsForPercent(chart);
        return chart;
    }

    private static JFreeChart multiPriceHistoryChart(TimePeriodValuesCollection prices) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, "Price", prices, true, false, false);
        setLowerBoundsZero(chart);
        return chart;
    }

    private static JFreeChart multiStepChart(TimePeriodValuesCollection prices) {
        JFreeChart chart = createCustomXYStepChart(prices);
        setBoundsLoosely(chart);
        return chart;
    }

    private static JFreeChart createCustomXYStepChart(TimePeriodValuesCollection prices) {
        DateAxis xAxis = new DateAxis(null);
        NumberAxis yAxis = new NumberAxis("price");
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());


        XYPlot plot = new XYPlot(prices, xAxis, yAxis, null);
        plot.setRenderer(new XYStepRenderer(null, null));
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        return new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }

    public static JFreeChart createCustomXYStepAreaChart(TimePeriodValuesCollection top, TimePeriodValuesCollection bottom) {
        DateAxis xAxis = new DateAxis(null);
        NumberAxis yAxis = new NumberAxis("price");
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        yAxis.setUpperBound(100);
        yAxis.setLowerBound(0.0);

        XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
        plot.setDataset(0, top);
        plot.setDataset(1, bottom);
        XYStepAreaRenderer bottomRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA, null, null);
        XYStepAreaRenderer topRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA, null, null);
        topRenderer.setRangeBase(1.0);
        topRenderer.setSeriesPaint(0, new Color(204, 255, 153));
        bottomRenderer.setSeriesPaint(0, new Color(51, 255, 204));
        plot.setRenderer(bottomRenderer);
        plot.setRenderer(1, topRenderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        return new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }

    private static ValueAxis setBoundsForPercent(JFreeChart chart) {
        ValueAxis rangeAxis = setLowerBoundsZero(chart);
        rangeAxis.setUpperBound(100);
        return rangeAxis;
    }

    private static ValueAxis setBoundsLoosely(JFreeChart chart) {
        ValueAxis rangeAxis = setLowerBoundsZero(chart);
        double highY = rangeAxis.getRange().getUpperBound();
        double top = Math.min((Math.ceil(highY / 10) + 1) * 10, 100);
        rangeAxis.setUpperBound(top);
        return rangeAxis;
    }

    private static ValueAxis setBoundsForPrice(JFreeChart chart) {
        ValueAxis rangeAxis = setLowerBoundsZero(chart);
        rangeAxis.setUpperBound(1);
        return rangeAxis;
    }

    private static ValueAxis setLowerBoundsZero(JFreeChart chart) {
        ValueAxis rangeAxis = chart.getXYPlot().getRangeAxis();
        rangeAxis.setLowerMargin(40);
        rangeAxis.setLowerBound(0.0);
        return rangeAxis;
    }

    public static JFreeChart createOverlaidOHLCAndStepChart(TimePeriodValuesCollection bottom, TimePeriodValuesCollection top, OHLCDataset ohlCdata) {
        DateAxis xAxis = new DateAxis(null);
        NumberAxis yAxis = new NumberAxis("price");
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        yAxis.setUpperBound(100);
        yAxis.setLowerBound(0.0);

        XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
        plot.setDataset(0, bottom);
        plot.setDataset(1, top);
        plot.setDataset(2, ohlCdata);

        XYStepAreaRenderer bottomRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA, null, null);
        XYStepAreaRenderer topRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA, null, null);
        HighLowRenderer hiLoRenderer = new HighLowRenderer();

        topRenderer.setRangeBase(1.0);
        topRenderer.setSeriesPaint(0, new Color(204, 255, 153));
        bottomRenderer.setSeriesPaint(0, new Color(51, 255, 204));
        plot.setRenderer(bottomRenderer);
        plot.setRenderer(1, topRenderer);
        plot.setRenderer(2, hiLoRenderer);

        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        return new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }

    public static JFreeChart buildBarChart(DefaultCategoryDataset dataset, PlotOrientation orientation, int hSize, int vSize) {
        JFreeChart chart = ChartFactory.createBarChart(
            null,        // chart title
            null,        // domain axis label
            null,        // range axis label
            dataset,     // data
            orientation, // the plot orientation
            false,       // include legend
            false,       // tooltips
            false        // generate urls
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setNoDataMessage("NO DATA!");

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBaseItemLabelsVisible(true);
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        plot.setRenderer(renderer);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setUpperMargin(0.15);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(hSize, vSize));
        chart.setBackgroundPaint(Color.white);
        return chart;
    }

    static public TimePeriodValuesCollection getHistoricalPrices(String claimName, List trades) {
        TimePeriodValues values = new TimePeriodValues(claimName);
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
        TimePeriodValues values = new TimePeriodValues(claimName);
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
        Dictionary<String, TimePeriodValues> positions = new Hashtable<String, TimePeriodValues>();
        Dictionary originalValue = new Hashtable();
        TimePeriodValuesCollection allValues = new TimePeriodValuesCollection();

        initializeSeries(positions, allValues, claim);
        addPriceSeries(trades, positions, originalValue);
        return allValues;
    }

    static public void initializeSeries(Dictionary<String, TimePeriodValues> positions, TimePeriodValuesCollection allValues, Claim claim) {
        Position[] allPositions = claim.positions();
        if (allPositions.length == 2) {
            int less = allPositions[0].comparePersistentId(allPositions[1]);
            Position pos = allPositions[less < 0 ? 0 : 1];   // pick one stably

            TimePeriodValues values = new TimePeriodValues(pos.getName());
            positions.put(pos.getName(), values);
            allValues.addSeries(values);
        } else {
            SortedSet<Position> sortedPositions = claim.sortPositions();
            for (Iterator<Position> positionIterator = sortedPositions.iterator(); positionIterator.hasNext();) {
                Position pos = positionIterator.next();
                TimePeriodValues values = new TimePeriodValues(pos.getName());
                positions.put(pos.getName(), values);
                allValues.addSeries(values);
            }
        }
    }

    public static void addPriceSeries(List trades, Dictionary<String, TimePeriodValues> positions, Dictionary originalValue) {
        for (Iterator iterator = trades.iterator() ; iterator.hasNext() ; ) {
            MakerTrade trade = (MakerTrade) iterator.next();
            TimePeriodValues values = positions.get(trade.getPos().getName());
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
}
