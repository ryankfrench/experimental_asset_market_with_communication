package net.commerce.zocalo.experiment.config;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Objects holding values or collections of values.  Can access and modify values given
    the field name.   */
public interface ValueHolder extends ConfigHolder {
    boolean hasValue(String fieldName);
    void setValue(String fieldName, String newValue);
    String getValue(String fieldName);
    String asString(String fieldName);
    String getTitle();
    ConfigField getConfigField(String fieldName);
}
