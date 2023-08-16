package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.service.PropertyKeywords;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing a list of integer values.  Knows how to parse a comma-separated list, and
 expects to use a wide input field.   */
public class IntListField extends StringField {
    public IntListField(String fieldName, String value) {
        super(fieldName, value);
    }

    public IntListField() { }

    public void render(StringBuffer buf) {
        buf.append(HtmlSimpleElement.labelledWideTextInputField(getTitle(), writeString()));
    }

    public int[] getInts(String name) {
        if (getName().equals(name)) {
            String[] strings = getValue().split(PropertyKeywords.COMMA_SPLIT_RE);
            int[] ints = new int[strings.length];
            for (int i = 0; i < strings.length; i++) {
                ints[i] = Integer.parseInt(strings[i]);
            }
            return ints;
        } else {
            return null;
        }
    }
}
