package net.commerce.zocalo.experiment;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.PersistentTestHelper;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

public class ScoreExplanationAccumulatorTest extends PersistentTestHelper {
    String header = "<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}>\n";
    MockAppender mock;
    ScoreExplanationAccumulator scores;
    Logger logger;
    StringBuffer buf;
    private String htmlHeader = "Long Header";
    private String logLabel = "label";

    protected void setUp() throws Exception {
        scores = new ScoreExplanationAccumulator();
        mock = new MockAppender();
        logger = Logger.getLogger("ScoreTableTest");
        logger.addAppender(mock);
        buf = new StringBuffer();
    }

    public void testOneColumn() {
        scores.addEntry(htmlHeader, 0, logLabel, q(3.7));
        scores.renderAsColumns(buf);
        assertREMatches(header + "<tr><th>" + htmlHeader + "</th></tr>\n" + "<tr><td align=center width=\"0%\">3.7</td></tr>\n</table><p>", buf.toString());
    }

    public void testLogging() {
        scores.addEntry(htmlHeader, 0, logLabel, q(3.7));
        scores.log("Player foo score info is ", logger);

        LoggingEvent event = (LoggingEvent) mock.getEvents().iterator().next();
        assertREMatches(".*" + logLabel + ".*3.7.*", event.getRenderedMessage());
    }

    public void testDifferentHeaders() {
        scores.addEntry(htmlHeader, 0, logLabel, q(3.7));
        scores.renderAsColumns(buf);
        assertREMatches(header + "<tr><th>" + htmlHeader + "</th></tr>\n" + "<tr><td align=center width=\"0%\">3.7</td></tr>\n</table><p>", buf.toString());

        scores.log("Player foo score info is ", logger);
        LoggingEvent event = (LoggingEvent) mock.getEvents().iterator().next();
        assertREMatches(".*" + logLabel + ".*3.7.*", event.getRenderedMessage());
    }

    public void testMissingLogLabels() {
        scores.addEntry(htmlHeader, logLabel, q(3.7));
        scores.addEntry(htmlHeader, "", q(4.2));
        scores.addEntry(htmlHeader, null, q(3.7));
        scores.renderAsColumns(buf);
        String headerCell = "<th>" + htmlHeader + "</th>";
        assertREMatches(header + "<tr>" + headerCell + headerCell + headerCell + "</tr>\n" + "<tr><td align=center>3.7</td><td align=center>4.2</td><td align=center>3.7</td></tr>\n</table><p>", buf.toString());

        String label = "Player foo score info is ";
        scores.log(label, logger);
        LoggingEvent event = (LoggingEvent) mock.getEvents().iterator().next();
        assertREMatches("[0-9]+# " + label + logLabel + ": 3.7", event.getRenderedMessage());
    }

    public void testMissingHtmlLabels() {
        scores.addEntry(htmlHeader, logLabel, q(3.7));
        scores.addEntry("", "foo", q(4.2));
        scores.addEntry(null, "bar", q(3.7));
        scores.renderAsColumns(buf);
        String headerCell = "<th>" + htmlHeader + "</th>";
        assertREMatches(header + "<tr>" + headerCell + "</tr>\n" + "<tr><td align=center>3.7</td></tr>\n</table><p>", buf.toString());

        String label = "Player foo score info is ";
        scores.log(label, logger);
        LoggingEvent event = (LoggingEvent) mock.getEvents().iterator().next();
        assertREMatches("[0-9]+# " + label + logLabel + ": 3.7 foo: 4.2 bar: 3.7", event.getRenderedMessage());
    }
}
