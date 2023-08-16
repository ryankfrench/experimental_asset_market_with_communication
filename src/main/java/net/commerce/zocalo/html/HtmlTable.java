package net.commerce.zocalo.html;

import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.currency.Quantity;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Render Html Tables. */
public class HtmlTable implements HtmlElement {
    final static public String BORDER = "border";
    public static final String CLASS = "class";
    public static final String TABLE_WIDTH = "tableWidth";

    private Map<String, String> attributes;
    static private Map<String, String> defaults;
    private String[] headers;

    static {
        defaults = new HashMap<String, String>();
        defaults.put("border", "1");
        defaults.put("cellspacing", "0");
        defaults.put("cellpadding", "3") ;
        defaults.put("align", "center");
    }

    public HtmlTable() {
        attributes = new HashMap<String, String>();
    }

    public void render(StringBuffer buf) {
        buf.append("<table ");
        appendDefaultAttributes(buf);
        appendAttributes(buf);
        buf.append(">\n");
        printHeaders(buf);
    }

    private void printHeaders(StringBuffer buf) {
        if (headers == null || headers.length == 0) { return; }

        HtmlRow.startTag(buf);
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            headingCell(buf, header);
        }
        HtmlRow.endTag(buf);
        buf.append("\n");
    }

    private void appendDefaultAttributes(StringBuffer buf) {
        Set<String> attNames = defaults.keySet();
        for (Iterator<String> stringIterator = attNames.iterator(); stringIterator.hasNext();) {
            String name = stringIterator.next();
            buf.append(name).append("='");
            String att = attributes.get(name);
            if (att != null) {
                buf.append(att);
            } else {
                buf.append(defaults.get(name));
            }
            buf.append("' ");
        }
    }

    private void appendAttributes(StringBuffer buf) {
        Set<String> attNames = attributes.keySet();
        for (Iterator<String> stringIterator = attNames.iterator(); stringIterator.hasNext();) {
            String name = stringIterator.next();
            String att = attributes.get(name);
            if (defaults.get(name) == null) {
                buf.append(name).append("='");
                buf.append(att);
                buf.append("'");
                if (stringIterator.hasNext()) {
                    buf.append(" ");
                }
            }
        }
    }

    public void addBGColor(String color) {
        attributes.put("bgcolor", color);
    }

    public void add(String name, int value) {
        attributes.put(name, Integer.toString(value));
    }

    public void add(String name, String value) {
        attributes.put(name, value);
    }

    static public void addVisibleStyle(boolean visible, HtmlTable table) {
        String visibleString = visible ? "block" : "none";
        table.add("style", "display:" + visibleString + ";background:lightgray");
    }

    public void addHeaders(String colOne, String colTwo) {
        headers = new String[] { colOne, colTwo };
    }

    public void addHeaders(String[] strings) {
        if (strings == null) {
            return;
        }
        headers = strings.clone();
    }

    static public void headingCell(StringBuffer buf, String s) {
        buf.append("<th>").append(s).append("</th>");
    }

    static public void endTagWithP(StringBuffer buf) {
        buf.append("</table><p>");
    }

    static public void endTag(StringBuffer buf) {
        buf.append("</table>");
    }

    static public void start(StringBuffer buf, String color, String[] colHeaders) {
        HtmlTable tbl = new HtmlTable();
        tbl.addBGColor(color);
        tbl.addHeaders(colHeaders);
        tbl.render(buf);
    }

    static public void start(StringBuffer buf, String[] colHeaders) {
        HtmlTable tbl = new HtmlTable();
        tbl.addHeaders(colHeaders);
        tbl.render(buf);
    }

    static public void start(StringBuffer buf, String[] colHeaders, String tableId) {
        HtmlTable tbl = new HtmlTable();
        tbl.add("id", tableId);
        tbl.addHeaders(colHeaders);
        tbl.render(buf);
        buf.append("\n");
    }

    static public void start(StringBuffer buf, String color, String[] colHeaders, String cssClass) {
        HtmlTable tbl = new HtmlTable();
        tbl.addBGColor(color);
        tbl.add("class", cssClass);
        tbl.addHeaders(colHeaders);
        tbl.render(buf);
        buf.append("\n");
    }

    static public void start(StringBuffer buf, String color, String att, String value) {
        HtmlTable tbl = new HtmlTable();
        tbl.addBGColor(color);
        tbl.add(att, value);
        tbl.render(buf);
        buf.append("\n");
    }

    static public void start(StringBuffer buf) {
        HtmlTable tbl = new HtmlTable();
        tbl.render(buf);
        buf.append("\n");
    }

    static public void startWideBorderLess(StringBuffer buf, String color, String cellSpacing) {
        HtmlTable tbl = new HtmlTable();
        tbl.add("width", "100%");
        tbl.add("border", "0");
        tbl.addBGColor(color);
        tbl.add("cellspacing", cellSpacing);
        tbl.render(buf);
        buf.append("\n");
    }

    static public void oneRowTable(StringBuffer buff, String[] headers, Quantity[] cells, String[] widths) {
        start(buff, headers);
        HtmlRow.startTag(buff);
        for (int i = 0; i < cells.length; i++) {
            Quantity cell = cells[i];
            String width = widths[i];
            HtmlSimpleElement.centeredCellWithWidth(cell.printAsScore(), width).render(buff);
        }
        HtmlRow.endTag(buff);
        buff.append("\n");
        endTagWithP(buff);
    }

    static public void oneRowTable(StringBuffer buff, String[] headers, Quantity[] cells) {
        start(buff, headers);
        for (int i = 0; i < cells.length; i++) {
            Quantity cell = cells[i];
            HtmlSimpleElement.centeredCell(cell.printAsScore()).render(buff);
        }
        HtmlRow.endTag(buff);
        endTagWithP(buff);
    }

    static public void oneRowTable(StringBuffer buff, String[] headers, String[] cells, Integer[] widths) {
        start(buff, headers);
        HtmlRow.startTag(buff);
        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i];
            Integer width = widths[i];
            HtmlSimpleElement.centeredCellWithWidth(cell, width).render(buff);
        }
        HtmlRow.endTag(buff);
        buff.append("\n");
        endTagWithP(buff);
    }

    public static void oneRowTable(StringBuffer buff, String[] headers, Double[] cells) {
        start(buff, headers);
        HtmlRow.startTag(buff);
        for (int i = 0; i < cells.length; i++) {
            double cell = cells[i];
            HtmlSimpleElement.centeredCell(NumberDisplay.printAsScore(cell)).render(buff);
        }
        HtmlRow.endTag(buff);
        buff.append("\n");
        endTagWithP(buff);
    }

    public static void oneRowTable(StringBuffer buff, String[] headers, String[] cells) {
        start(buff, headers);
        HtmlRow.startTag(buff);
        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i];
            HtmlSimpleElement.centeredCell(cell).render(buff);
        }
        HtmlRow.endTag(buff);
        buff.append("\n");
        endTagWithP(buff);
    }

    public static void oneColumnTable(StringBuffer buff, String[] headers, String[] cells) {
        start(buff);
        for (int i = 0; i < cells.length; i++) {
            String header = headers[i];
            String cell = cells[i];
            HtmlRow.startTag(buff);
            HtmlSimpleElement.centeredCell(header).render(buff);
            HtmlSimpleElement.centeredCell(cell).render(buff);
            HtmlRow.endTag(buff);
            buff.append("\n");
        }
        endTagWithP(buff);
    }
}
