package net.commerce.zocalo.service;

import net.commerce.zocalo.user.Registry;
import net.commerce.zocalo.mail.MailUtil;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.antlr.stringtemplate.StringTemplate;

import javax.mail.MessagingException;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** set up initial configuration, provide access to configuration settings */
public class Config {
    final static public String INITIAL_USER_FUNDS_KEY   = "initial.user.funds";
    final static public String USE_COST_LIMIT_PAGES     = "useCostLimit";
    final static public String ADMIN_PASSWORD_KEY       = "admin.password";
    final static public String DEFAULT_CONFIG_FILE_KEY  = "etc/zocalo.conf";
    final static public String SUPPRESS_HISTORY_LINKS   = "suppress.history.links";
    final static public String ANNOUNCE_NEW_MARKETS     = "announce.new.markets";

    private static String defaultDBFile = null;
    private static boolean useCostLimits = false;
    private static Properties allProperties;
    private static boolean hideHistory = false;
    private static boolean announceNewMarkets = false;

    public static void initializeConfiguration(Properties props) {
        Registry.initPasswdSeed(props.getProperty(ADMIN_PASSWORD_KEY));
        allProperties = new Properties();
        allProperties.putAll(props);
        defaultDBFile = ServerUtil.readDBConfigFromProps(props);
        hideHistory = PropertyHelper.parseBoolean(SUPPRESS_HISTORY_LINKS, props, false);
        announceNewMarkets = PropertyHelper.parseBoolean(ANNOUNCE_NEW_MARKETS, props, false);
        if (PropertyHelper.parseBoolean(Config.USE_COST_LIMIT_PAGES, props, false)) {
            Logger logger = Logger.getLogger(Config.class);
            logger.warn("Using Cost Limit Purchase pages");
            Config.useCostLimits = true;
        }
    }

    static public String matchAdminPassword(String password) {
        if (null == password) {
            return null;
        }
        Properties props = ServerUtil.readConfigFile();
        String storedPassword = props.getProperty(ADMIN_PASSWORD_KEY);
        if (storedPassword == null) {
            return null;
        }
        boolean newAdminFound = password.equalsIgnoreCase(storedPassword.trim());
        if (newAdminFound) {
            return MarketOwner.registerAdminToken();
        }
        return null;
    }

    static public void initPasswdGen() {
        Registry.initPasswdSeed(ServerUtil.readConfigFile().getProperty(ADMIN_PASSWORD_KEY));
    }

    static public void sendMail(String emailAddress, StringTemplate tpl, Properties props) throws MessagingException {
        props.putAll(allProperties);
        tpl.setAttributes(props);
        String name = props.getProperty("userName");
        String subject = "Please Register your new account, " + name;
        MailUtil.sendSMTPMail(props, emailAddress, subject, tpl.toString(), false);
    }

    public static int getInitialUserFunds() {
        Properties props = ServerUtil.readConfigFile();
        String fundsString = props.getProperty(INITIAL_USER_FUNDS_KEY);
        if (fundsString == null || fundsString.equals("")) {
            fundsString = "1000";
        }
        return 100 * Integer.parseInt(fundsString.trim());
    }

    public static boolean suppressHistoryLinks() {
        return hideHistory;
    }

    public static boolean announceNewMarkets() {
        return announceNewMarkets;
    }

    public static String getDefaultDBFile() {
        return defaultDBFile;
    }

    public static boolean getCostLimitBuying() {
        return useCostLimits;
    }

    /** @deprecated */
    public static void setUseCostLimits(boolean costLimits) {
        useCostLimits = costLimits;
    }
}
