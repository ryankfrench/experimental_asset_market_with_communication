package net.commerce.zocalo.experiment;

import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.role.TradingSubject;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** collect matched HTML header, log labels and values for experiment scoring so we'll be able to
    print htmlTables or log the info. */
public class ScoreExplanationAccumulator {
    private List<String> headers = new ArrayList<String>();
    private List<String> cells = new ArrayList<String>();
    private List<String> logLabels = new ArrayList<String>();
    private List<String> logValues = new ArrayList<String>();
    private List<Integer> widths = new ArrayList<Integer>();
    private boolean hasWidths = false;


    public void addEntry(String htmlLabel, int width, String logLabel, Quantity v) {
        String score = v.printAsScore();
        if (null != htmlLabel && 0 != htmlLabel.length()) {
            headers.add(htmlLabel);
            cells.add(score);
            widths.add(width);
            hasWidths = true;
        }
        if (null != logLabel && 0 != logLabel.length()) {
            logLabels.add(logLabel);
            logValues.add(score);
        }
    }

    public void addEntry(String htmlLabel, String logLabel, Quantity v) {
        String score = v == null ? "MISSING" : v.printAsScore();
        if (null != htmlLabel && 0 != htmlLabel.length()) {
            headers.add(htmlLabel);
            cells.add(score);
            widths.add(0);
        }
        if (null != logLabel && 0 != logLabel.length()) {
            logLabels.add(logLabel);
            logValues.add(score);
        }
    }

    public void addEntry(String htmlLabel, String logLabel, String value) {
        if (null != htmlLabel && 0 != htmlLabel.length()) {
            headers.add(htmlLabel);
            cells.add(value);
            widths.add(0);
        }
        if (null != logLabel && 0 != logLabel.length()) {
            logLabels.add(logLabel);
            logValues.add(value);
        }
    }

    public void addEntryIfDefined(String htmlLabel, Object key, String logLabel, TradingSubject subject) {
        Quantity scoreComponent = subject.getScoreComponent(key);
        if (scoreComponent != null) {
            addEntry(htmlLabel, logLabel, scoreComponent);
        }
    }

    public void renderAsColumns(StringBuffer buf) {
        String[] headerArray = headers.toArray(new String[headers.size()]);
        String[] valueArray = cells.toArray(new String[cells.size()]);

        if (hasWidths) {
            Integer[] widths = this.widths.toArray(new Integer[this.widths.size()]);
            HtmlTable.oneRowTable(buf, headerArray, valueArray, widths);
        } else {
            HtmlTable.oneRowTable(buf, headerArray, valueArray);
        }
    }

    public void log(String pref, Logger logger) {
        StringBuffer buf = new StringBuffer();
        buf.append(GID.log());
        buf.append(pref);
        if (!pref.substring(pref.length() -1 ).equals(" ")) {
            buf.append(" ");
        }
        Iterator<String> valueIterator = logValues.iterator();
        for (Iterator<String> labelIterator = logLabels.iterator(); labelIterator.hasNext();) {
            String label = labelIterator.next();
            String body = valueIterator.next();
            buf.append(label).append(": ").append(body);
            if (labelIterator.hasNext()) {
                buf.append(" ");
            }
        }
        logger.info(buf.toString());
    }

    public static void renderAsTwoColumns(ScoreExplanationAccumulator left, ScoreExplanationAccumulator right, StringBuffer buf) {
        String[] leftHeaders = left.headers.toArray(new String[left.headers.size()]);
        String[] leftValues = left.cells.toArray(new String[left.cells.size()]);
        String[] rightHeaders = right.headers.toArray(new String[right.headers.size()]);
        String[] rightValues = right.cells.toArray(new String[right.cells.size()]);
        buf.append("<table border=1>\n <tbody>\n\t");
        int maxLen = Math.max(left.cells.size(), right.cells.size());
        HtmlSimpleElement emptyCell = HtmlSimpleElement.cell("&nbsp;");
        for (int i = 0 ; i < maxLen ; i++ ) {
            HtmlRow.startTag(buf);
            if (leftHeaders.length > i && rightHeaders.length > i) {
                HtmlSimpleElement.cell(leftHeaders[i]).render(buf);
                HtmlSimpleElement.centeredCell(leftValues[i]).render(buf);
                emptyCell.render(buf);
                HtmlSimpleElement.cell(rightHeaders[i]).render(buf);
                HtmlSimpleElement.centeredCell(rightValues[i]).render(buf);
            } else if (leftHeaders.length > i) {
                HtmlSimpleElement.cell(leftHeaders[i]).render(buf);
                HtmlSimpleElement.centeredCell(leftValues[i]).render(buf);
                emptyCell.render(buf);
                emptyCell.render(buf);
                emptyCell.render(buf);
            } else {
                emptyCell.render(buf);
                emptyCell.render(buf);
                emptyCell.render(buf);
                HtmlSimpleElement.cell(rightHeaders[i]).render(buf);
                HtmlSimpleElement.centeredCell(rightValues[i]).render(buf);
            }
            HtmlRow.endTag(buf);
        }
        buf.append("</tbody></table>");
    }

    public static void renderAsOneColumn(ScoreExplanationAccumulator column, StringBuffer buf) {
        String[] headers = column.headers.toArray(new String[column.headers.size()]);
        String[] values = column.cells.toArray(new String[column.cells.size()]);
        buf.append("<table border=1>\n <tbody>\n\t");
        for (int i = 0 ; i < column.cells.size(); i++ ) {
            HtmlRow.startTag(buf);
            HtmlSimpleElement.cell(headers[i]).render(buf);
            HtmlSimpleElement.centeredCell(values[i]).render(buf);
            HtmlRow.endTag(buf);
        }
        buf.append("</tbody></table>");
    }
}
