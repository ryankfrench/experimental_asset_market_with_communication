package net.commerce.zocalo.html;
// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;
import net.commerce.zocalo.PersistentTestHelper;

public class HtmlTableTest extends PersistentTestHelper {
    public void testTablePrinting() {
        StringBuffer buf = new StringBuffer();
        HtmlTable.start(buf, "blue", new String[] { "one", "two" } );
        String tableStart = "<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}";
        String expected = tableStart + "bgcolor='blue'>\n<tr><th>one</th><th>two</th></tr>\n";
        assertREMatches(expected, buf.toString());

        HtmlTable.start(buf, "red", new String[] { "one", "two" } );
        String expectedTwo = expected + tableStart + "bgcolor='red'>\n<tr><th>one</th><th>two</th></tr>\n";
        assertREMatches(expectedTwo, buf.toString());
    }

    public void testTableCreation() {
        HtmlTable t = new HtmlTable();
        t.addBGColor("blue");
        StringBuffer sb1 = new StringBuffer();
        t.render(sb1);
        assertREMatches("<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}bgcolor='blue'>\n", sb1.toString());

        t.add("border", 3);
        StringBuffer sb2 = new StringBuffer();
        t.render(sb2);
        assertREMatches("<table (cellpadding='3' |align='center' |cellspacing='0' |border='3' ){4}bgcolor='blue'>\n", sb2.toString());
    }

    public void testTableContents() {
        StringBuffer buf = new StringBuffer();
        HtmlTable tbl = new HtmlTable();
        tbl.addBGColor("blue");
        tbl.addHeaders("one", "two");
        tbl.render(buf);
        String tableStart = "<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}";
        String expected = tableStart + "bgcolor='blue'>\n<tr><th>one</th><th>two</th></tr>\n";
        assertREMatches(expected, buf.toString());

        StringBuffer buf2 = new StringBuffer();
        HtmlTable tbl2 = new HtmlTable();
        tbl2.addHeaders("one", "two");
        tbl2.render(buf2);
        String expectedTwo = tableStart + ">\n<tr><th>one</th><th>two</th></tr>\n";
        assertREMatches(expectedTwo, buf2.toString());
    }

    public void testOneRowTable() {
        StringBuffer buff = new StringBuffer();
        String tableStart = "<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}>\n";
        HtmlTable.oneRowTable(buff, new String[] { "label", "body" }, new Double[] { 2.1, 3.4 } );
        assertREMatches(tableStart + "<tr><th>label</th><th>body</th></tr>\n" + "<tr><td align=center>2.1</td><td align=center>3.4</td></tr>\n</table><p>", buff.toString());
    }

}
