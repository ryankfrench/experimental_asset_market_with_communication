package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.service.PropertyHelper;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Field containing a duration (written as xx:xx or just xxx). */
public class TimeField extends StringField {
    public TimeField() { }

    public TimeField(String title, String value) {
        super(title, value);
    }

    public TimeField(String title, int value) {
        super(title, "" + value);
    }

    public void render(StringBuffer buf) {
        buf.append(HtmlSimpleElement.labelledTextInputField(getTitle(), writeString()));
    }

    public int getTime(String timeName) {
        if (getTitle().equals(timeName)) {
            return PropertyHelper.parseTimeStringAsSeconds(getValue());
        } else {
            return -1;
        }
    }

    public int intValue() {
        return getTime(getTitle());
    }
}
