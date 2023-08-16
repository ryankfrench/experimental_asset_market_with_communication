package net.commerce.zocalo.html;

// Copyright 2007, 2009 Chris Hibbert.  
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;
import net.commerce.zocalo.html.HtmlSimpleElement;

public class HtmlSimpleElementTest extends TestCase {
    public void testCellElement() {
        StringBuffer buf = new StringBuffer();
        HtmlSimpleElement.centeredCell("foo").render(buf);
        String result = buf.toString();
        assertTrue(result.matches("<td.*>foo</td>"));
    }

    public void testHeaders() {
        String label = "foo";
        StringBuffer buf = new StringBuffer();
        HtmlSimpleElement.printHeader(buf, 1, label);
        String expected = "<center><h1>" + label + "</h1></center>\n";
        assertEquals(expected, buf.toString());
    }

    public void testCells() {
        assertEquals("<td>foo</td>", HtmlSimpleElement.printTableCell("foo"));
        assertEquals("<td>&nbsp;</td>", HtmlSimpleElement.printTableCell(""));
    }

    public void testRangeInput() {
        String name = "foo";
        String expected1 = name + ": <input type=text autocomplete=off size=3 name='" + name + "'>";
        assertEquals(expected1, HtmlSimpleElement.labelledTextInputField(name));
    }

    public void testSelectionList() {
        String list = HtmlSimpleElement.selectList("position", new String[]{"one", "red", "tuesday"}, "");
        assertEquals("<select name='position'><option>one<option>red<option>tuesday</select>", list);
    }

}
