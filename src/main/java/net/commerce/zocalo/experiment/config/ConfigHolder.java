package net.commerce.zocalo.experiment.config;

import javax.servlet.http.HttpServletRequest;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Collection of values for configuring an experiment.  The hierarchy can be rendered
    as HTML to provide a web-based interface.  When the experimenter updates the
    configuration to the server, the values can be read back out of the HTML fields. */
public interface ConfigHolder {
    void render(StringBuffer buf);
    void setAllValues(HttpServletRequest request);

}
