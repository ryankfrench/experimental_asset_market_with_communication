package net.commerce.zocalo.experiment;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.commerce.zocalo.hibernate.NoDBHibernateUtil;
import net.commerce.zocalo.logging.GID;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.service.BayeuxSingleton;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Provides access to a single instance of Session.  Should only be called from JSP pages, which
 aren't called with sufficient context to recieve a Session as an argument.  */
public class SessionSingleton {
    final static String SERVER_PORT = SERVER_PROPERTY_WORD + DOT_SEPARATOR + PORT_PROPERTY_WORD;
    final static private String defaultConfigFile = "etc/zocalo.conf";

    static private String serverPort = "";
    static private boolean initializedOnce = false;

    static private Session session;

    static public Session getSession() {
        initializeOnce();
        return session;
    }

    static public void setSession(Properties props) {
        endPreviousSession();
        initializeOnce();
        session = Session.make(props, BayeuxSingleton.getInstance().getBayeux(), serverPort);
    }

    static public void setSession(Properties props, String logfile) {
        endPreviousSession();
        initializeOnce();
        session = Session.make(props, BayeuxSingleton.getInstance().getBayeux(), logfile);
    }

    private static void endPreviousSession() {
        if (session != null) {
            Logger logger = Session.sessionLogger();
            logger.warn(GID.log() + "Attempted to reset Session object when one already existed!  Allowing reset.");
            session.endSession();
        }
    }

    static private void initializeOnce() {
        if (initializedOnce) {
            return;
        }
        initalizeFromFile(new File(defaultConfigFile));
    }

    static public void resetSession() {
        session.closeSessionAppenders();
        session = null;
    }

    static public void initalizeFromFile(File configFile) {
        InputStream stream;
        Properties props;
        new NoDBHibernateUtil().setupSessionFactory();
        /* JJDM zocalo.conf No longer used to find port (moved to Tomcat)
        try {
            stream = new FileInputStream(configFile);
            props = new Properties();
            props.load(stream);
        } catch (IOException e) {
            Logger sessionLogger = Logger.getLogger(Session.class);
            sessionLogger.warn(GID.log() + "couldn't open config file " + configFile.getName());
            return;
        }
        serverPort = props.getProperty(SERVER_PORT);
        if (serverPort == null || serverPort.equals("")) {
            Logger sessionLogger = Logger.getLogger(Session.class);
            sessionLogger.warn(GID.log() + SERVER_PORT + " was not set in " + configFile.getName());
            return;
        }
        serverPort = serverPort.trim();
        initializedOnce = true;
        */
    }

    static public String getServerPort() {
        initializeOnce();
        return serverPort;
    }
}
