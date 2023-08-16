package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing an Integer value. */
public class IntField extends ConfigField {
    private int value;

    public IntField(String title, int val) {
        super(title);
        value = val;
    }

    /** @deprecated */
    public IntField() {
    }

    public int intValue() {
        return value;
    }

    public void render(StringBuffer buf) {
        buf.append(HtmlSimpleElement.labelledTextInputField(getTitle(), "" + writeString()));
    }

    public String asString(String fieldName) {
        return writeString();
    }

    public void setValue(String fieldName, String newValue) {
        if (getTitle().equals(fieldName)) {
            readString(newValue);
        }
    }

    public void readString(String val) {
        value = Integer.parseInt(val);
    }

    public String writeString() {
        return "" + value;
    }

    public String type() {
        return "int";
    }

    /** @deprecated */
    public int getValue() {
        return value;
    }

    /** @deprecated */
    public void setValue(int val) {
        value = val;
    }
}
