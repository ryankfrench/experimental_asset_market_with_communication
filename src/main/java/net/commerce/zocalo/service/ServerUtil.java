package net.commerce.zocalo.service;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** utilities common to running a web server.  Currently aggregates configuration file tools. */
public abstract class ServerUtil {
    final static public String DEFAULT_DB_FILE_KEY = "database.url.file";
    final static public String DEFAULT_DB_URL_KEY  = "database.url.base";

    public static Properties readConfigFile() {
        File configFile = new File(Config.DEFAULT_CONFIG_FILE_KEY);
        if (! configFile.exists()) {
             configFile = new File("../" + Config.DEFAULT_CONFIG_FILE_KEY);
        }
        Properties props = new Properties();
        InputStream stream;
        try {
            stream = new FileInputStream(configFile);
            props.load(stream);
            return props;
        } catch (IOException e) {
            Logger sessionLogger = Logger.getLogger(ServerUtil.class);
            sessionLogger.warn("couldn't open config file " + Config.DEFAULT_CONFIG_FILE_KEY);
            System.exit(10);
            return null;
        }
    }

    protected static String readDBConfigFromProps(Properties props) {
        String dbUrlKey = props.getProperty(DEFAULT_DB_URL_KEY);
        String dbFileKey = props.getProperty(DEFAULT_DB_FILE_KEY);
        if (dbUrlKey == null || dbUrlKey.length() == 0 || dbFileKey == null || dbFileKey.length() == 0) {
            System.err.print("illegal DB URL: " + dbUrlKey + dbFileKey + ", check configuration file.");
            System.exit(3);
        }
        return dbUrlKey.trim() + dbFileKey.trim();
    }
}
