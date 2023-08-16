package net.commerce.zocalo.hibernate;

import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.logging.GID;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.sql.SQLException;
// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Shut the database down cleanly. */
public class ShutdownDB extends net.commerce.zocalo.service.ServerUtil {
    static public void main(String[] args) {
        Properties props = readConfigFile();
        Log4JHelper.getInstance().openLogFile(null);
        String connectionURL = readDBConfigFromProps(props);

        Logger trace = Logger.getLogger("trace");
        trace.info(GID.log() + "   shutting down DB.");
        HibernateUtil.initializeSessionFactory(connectionURL, HibernateUtil.SCHEMA_UPDATE);
        try {
            HibernateUtil.shutdown(connectionURL);
        } catch (SQLException e) {
            trace.info(GID.log() + "   ... failed to shut down DB.");
        }
    }
}
