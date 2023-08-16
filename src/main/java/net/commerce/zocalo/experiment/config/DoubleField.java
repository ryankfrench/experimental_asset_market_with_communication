package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing a double value. */
public class DoubleField extends StringField {
    public DoubleField(String fieldName, float value) {
        super(fieldName, "" + value);
    }

    public DoubleField() { }

    public void render(StringBuffer buf) {
        buf.append(HtmlSimpleElement.labelledTextInputField(getTitle(), writeString()));
    }

    public double getDouble() {
        return Double.valueOf(getValue());
    }
}
