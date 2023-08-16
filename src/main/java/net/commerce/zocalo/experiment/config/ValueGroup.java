package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlTable;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Collections of fields grouped together either semantically or for convenience. */
public interface ValueGroup extends ValueHolder, RoleValueGroup {
    ConfigField getField(String fieldName);
    void renderVisible(StringBuffer buf, boolean visible);
}
