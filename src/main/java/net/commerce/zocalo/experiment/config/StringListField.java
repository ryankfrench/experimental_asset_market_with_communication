package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.service.PropertyKeywords;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing a list of String values. Knows how to parse a comma-separated list of Strings. */
public class StringListField extends StringField {
    public StringListField(String fieldName, String value) {
        super(fieldName, value);
    }

    public StringListField() { }

    public void render(StringBuffer buf) {
        buf.append(HtmlSimpleElement.labelledWideTextInputField(getTitle(), writeString()));
    }

    public String[] getStrings(String name) {
        if (getName().equals(name)) {
            return getValue().split(PropertyKeywords.COMMA_SPLIT_RE);
        } else {
            return null;
        }
    }
}