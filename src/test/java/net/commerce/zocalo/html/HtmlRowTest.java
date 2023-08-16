package net.commerce.zocalo.html;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;
import net.commerce.zocalo.html.HtmlElement;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlSimpleElement;

public class HtmlRowTest extends TestCase {

    public void testEmptyRowElement() {
        StringBuffer buf = new StringBuffer();
        HtmlElement emptyRow = new HtmlRow(new HtmlElement[0]);
        emptyRow.render(buf);
        String result = buf.toString();
        assertTrue(result.matches("<tr.*>\n</tr>\n"));
    }

    public void testSingleRowElement() {
        StringBuffer buf = new StringBuffer();
        HtmlElement singleElementRow = new HtmlRow(new HtmlElement[] { HtmlSimpleElement.centeredCell("foo"), } );
        singleElementRow.render(buf);
        String result = buf.toString();
        assertTrue(result.matches("<tr>\n<td.*>foo</td>\n</tr>\n"));
    }

    public void testTaggedRowElement() {
        StringBuffer buf = new StringBuffer();
        String tag = "bgcolor=\"limegreen\"";
        HtmlElement singleElementRow = new HtmlRow(tag, new HtmlElement[] { HtmlSimpleElement.centeredCell("foo"), } );
        singleElementRow.render(buf);
        String result = buf.toString();
        assertTrue(result.matches("<tr " + tag + ">\n<td.*>foo</td>\n</tr>\n"));
    }

    public void testMultipleElementRow() {
        StringBuffer buf = new StringBuffer();
        HtmlElement multipleElementRow = new HtmlRow(new HtmlElement[]
            { HtmlSimpleElement.centeredCell("foo"),
              HtmlSimpleElement.centeredCell("bar"),
            }
        );
        multipleElementRow.render(buf);
        String result = buf.toString();
        assertTrue(result.matches("<tr>\n<td.*>foo</td>\n<td.*>bar</td>\n</tr>\n"));
    }
}
