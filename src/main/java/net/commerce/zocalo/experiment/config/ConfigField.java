package net.commerce.zocalo.experiment.config;

import javax.servlet.http.HttpServletRequest;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Abstract parent of all classes representing individual fields.  Provides some
    default implementations and requires that descendants provide readString() and writeString().
 */
public abstract class ConfigField implements ValueHolder {
    private String name;

    public ConfigField(String title) {
        name = title;
    }

    public ConfigField() {
    }

    public boolean hasValue(String fieldName) {
        return getTitle().equals(fieldName);
    }

    public String getTitle() {
        return name;
    }

    abstract public void readString(String val);
    abstract public String writeString();

    public void setAllValues(HttpServletRequest request) {
        String newValue = request.getParameter(name);
        if (newValue == null) {
            return;
        }
        setValue(name, newValue);
    }

    public String getValue(String fieldName) {
        if (hasValue(fieldName)) {
            return writeString();
        } else {
            return null;
        }
    }

    public ConfigField getConfigField(String fieldName) {
        if (name.equals(fieldName)) {
            return this;
        } else {
            return null;
        }
    }

    /** @deprecated */
    public String getName() {
        return name;
    }

    /** @deprecated */
    public void setName(String name) {
        this.name = name;
    }
}
