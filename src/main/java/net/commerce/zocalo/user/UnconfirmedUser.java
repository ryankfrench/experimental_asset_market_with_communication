package net.commerce.zocalo.user;

import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.mail.MailUtil;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

// Copyright 2007, 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** User who has registered, but has not yet confirmed the email address. */
public class UnconfirmedUser extends Warnable {
    private String name;
    private String password;
    private String emailAddress;
    private String confirmationToken;
    private long id;
    static public final String CONFIRMATION_CODE = "confirmCode";
    static public final String CONFIRMATION_URL = "confirmURL";

    public UnconfirmedUser(String userName, String password, String emailAddress) {
        this.name = userName;
        this.password = password;
        this.emailAddress = emailAddress;
        confirmationToken = Registry.newToken();
    }

    /** @deprecated */
    public UnconfirmedUser() {
    }

    public void sendEmailNotification(String requestURL) throws MessagingException {
        StringTemplateGroup group = MailUtil.EmailTemplates;
        StringTemplate tpl = group.getInstanceOf("ConfirmRegistration");
        Properties props = new Properties();
        String constantsFileName = MailUtil.SITE_CONSTANTS_FILE_NAME;
        try {
            InputStream inputStream = null;
            inputStream = new FileInputStream(MailUtil.TemplateDir + "/" + constantsFileName);
            props.load(inputStream);
        } catch (FileNotFoundException e) {
            Logger.getLogger(MailUtil.class).warn("unable to open " + constantsFileName + ".", e);
        } catch (IOException e) {
            Logger.getLogger(MailUtil.class).warn("unable to load " + constantsFileName + ".", e);
        }

        props.put("userName", name);
        props.put(CONFIRMATION_CODE, confirmationToken);
        props.put(CONFIRMATION_URL, requestURL + "?userName=" + name + "&confirmation=" + confirmationToken);
        Config.sendMail(emailAddress, tpl, props);
    }

    public void setToken(String token) {
        confirmationToken = token;
    }

    public boolean confirm(String token) {
        if (token.equals(confirmationToken)) {
            MarketOwner.createUser(getName(), getPassword(), getEmailAddress());
            MarketOwner.removeUnconfirmed(getName(), getConfirmationToken());
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    /** @deprecated */
    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    /** @deprecated */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /** @deprecated */
    public String getConfirmationToken() {
        return confirmationToken;
    }

    /** @deprecated */
    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    private String getPassword() {
        return password;
    }

    /** @deprecated */
    public void setPassword(String password) {
        this.password = password;
    }

    /** @deprecated */
    public long getId() {
        return id;
    }

    /** @deprecated */
    public void setId(long id) {
        this.id = id;
    }

    public void register(Map<String, String> registrants) {
        registrants.put(getConfirmationToken(), getName());
    }
}
