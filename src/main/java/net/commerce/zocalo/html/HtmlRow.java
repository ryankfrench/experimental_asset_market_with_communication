package net.commerce.zocalo.html;

import java.util.List;
import java.util.Iterator;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Render Html Rows (tr elements) based on arrays of cell contents. */
public class HtmlRow implements HtmlElement {
    final private String tag;
    private HtmlElement[] cells;

    public HtmlRow(HtmlElement[] cells) {
        tag = "";
        this.cells = cells;
    }

    public HtmlRow(String tag, HtmlElement[] cells) {
        this.cells = cells;
        this.tag = tag;
    }

    public HtmlRow(List<Double> cellList) {
        tag = "";
        cells = new HtmlElement[cellList.size()];
        Iterator<Double> iterator = cellList.iterator();
        for (int i = 0; i < cells.length; i++) {
            cells[i] = HtmlSimpleElement.cell(iterator.next().toString());
        }
    }

    public void render(StringBuffer buf) {
        if ("" == tag) {
            buf.append("<tr>\n");
        } else {
            buf.append("<tr " + tag + ">\n");
        }
        for (int i = 0; i < cells.length; i++) {
            HtmlElement cell = cells[i];
            cell.render(buf);
            buf.append("\n");
        }
        buf.append("</tr>\n");
    }

    static public void startTag(StringBuffer buf) {
        buf.append("<tr>");
    }

    static public void startTag(StringBuffer buf, String className) {
        buf.append("<tr class='" + className + "'>");
    }

    static public void endTag(StringBuffer buf) {
        buf.append("</tr>");
    }

    public static void labelFirstColumn(String firstColumnLabel, String[] labels) {
        if (null == firstColumnLabel || "".equals(firstColumnLabel)) {
            labels[0] = "&nbsp;";
        } else {
            labels[0] = firstColumnLabel;
        }
    }

    public static void labelColumns(int startColumn, int rounds, String[] labels, String roundLabel) {
        for (int i = 0; i < rounds; i++) {
            labels[startColumn + i] = roundLabel + " " + (i + 1);
        }
    }

    public static void labelColumns(List<Integer> columns, String[] columnLabels, String roundLabel) {
        int index = 1;
        for (Iterator<Integer> iter = columns.iterator(); iter.hasNext();) {
            int votingRound = iter.next();
            columnLabels[index] = roundLabel + " " + votingRound;
            index++;
        }
    }

    static public void oneCell(StringBuffer buf, String content) {
        HtmlSimpleElement oneCellContent = HtmlSimpleElement.cell(content);
        HtmlRow row = new HtmlRow( new HtmlElement[] { oneCellContent } );
        row.render(buf);
        buf.append("\n");
    }

    static public void twoCells(StringBuffer buf, String content1, String content2) {
        HtmlSimpleElement cellOneContent = HtmlSimpleElement.cell(content1);
        HtmlSimpleElement cellTwoContent = HtmlSimpleElement.cell(content2);
        HtmlRow row = new HtmlRow( new HtmlElement[] { cellOneContent, cellTwoContent } );
        row.render(buf);
        buf.append("\n");
    }
}
