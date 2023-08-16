package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
import javax.servlet.http.HttpServletRequest;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing a boolean value. */
public class BooleanField extends ConfigField {
    private boolean value;

    public BooleanField(String title, boolean val) {
        super(title);
        value = val;
    }

    /** @deprecated */
    public BooleanField() {
    }

    public String asString(String fieldName) {
        return writeString();
    }

    public void setValue(String fieldName, String newValue) {
        if (getTitle().equals(fieldName)) {
            readString(newValue);
        }
    }

    public void setAllValues(HttpServletRequest request) {
        String newValue = request.getParameter(getName());
        if (newValue == null) {
            setValue(getTitle(), "off");
        }
        setValue(getTitle(), newValue);
    }

    public void readString(String val) {
        value = "on".equalsIgnoreCase(val);
    }

    public String writeString() {
        return value ? "true" : "false";
    }

    public String type() {
        return "boolean";
    }

    public void render(StringBuffer buf) {
        HtmlSimpleElement.checkBox(buf, getTitle(), value);
    }

    public boolean booleanValue() {
        return value;
    }

    /** @deprecated */
    public boolean isValue() {
        return value;
    }

    /** @deprecated */
    public void setValue(boolean value) {
        this.value = value;
    }
}
