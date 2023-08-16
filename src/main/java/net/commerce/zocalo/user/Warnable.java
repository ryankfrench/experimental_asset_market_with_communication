package net.commerce.zocalo.user;

import net.commerce.zocalo.logging.Log4JHelper;
import org.apache.log4j.Logger;
import net.commerce.zocalo.logging.GID;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**  Base class for User-type classes that reflect warnings to the user. */
abstract public class Warnable {
    private String warnings = "";
    final static public String WARNING_SEPARATOR = "<br>";

    abstract public String getName();

    public boolean addWarning(String messages) {
        if (warnings.indexOf(messages) >= 0) {
            return false;
        } else {
            warnings += messages + WARNING_SEPARATOR;
            return true;
        }
    }

    public boolean hasWarnings() {
        return warnings.length() > 0;
    }

    public String getWarningsHTML() {
        String retval = warnings;
        warnings = "";
        return retval;
    }

    public void warn(String warning, String logMessage) {
        if (addWarning(warning)) {
            Logger logger = Logger.getLogger(Log4JHelper.UserError);
            logger.warn(GID.log() + logMessage);
        }
    }

    public void warn(String warning) {
        if (addWarning(warning)) {
            Logger logger = Logger.getLogger(Log4JHelper.UserError);
            logger.warn(GID.log() + "told " + getName() + " \"" + warning + "\".");
        }
    }
}
