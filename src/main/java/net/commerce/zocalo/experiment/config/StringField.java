package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing a String. */
public class StringField extends ConfigField {
    private String value;

    public StringField(String title, String val) {
        super(title);
        value = val;
    }

    /** @deprecated */
    public StringField() {
    }

    public void render(StringBuffer buf) {
        buf.append(HtmlSimpleElement.labelledMediumTextInputField(getTitle(), writeString()));
    }

    public String asString(String fieldName) {
        return getValue(fieldName);
    }

    public void setValue(String fieldName, String newValue) {
        if (getTitle().equals(fieldName)) {
            value = newValue;
        }
    }

    public void readString(String val) {
        value = val;
    }

    public String writeString() {
        return value;
    }

    public String stringValue() {
        return value;
    }

    /** @deprecated */
    public String getValue() {
        return value;
    }

    /** @deprecated */
    public void setValue(String value) {
        this.value = value;
    }
}
