package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.mail.MailUtil;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.UnconfirmedUser;
import net.commerce.zocalo.service.MarketOwner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Copyright 2007, 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A screen that allows users to create new accounts. */
public class AccountCreationScreen extends LoginScreen {
    static final public String CREATE_ACCOUNT_JSP  = "createAccount.jsp";
    static final public String CREATE_ACCOUNT_NAME = "createAccount";
    static final public String CREATE_ACCOUNT      = "Create Account";
    static final public String ACCOUNT_NOT_AVAILABLE = "account name not available: ";
    static final public String LOOK_FOR_CONFIRMATION = "look for an email from us with a confirmation link for you to visit.";
    static final public String PASSWORD_WARNING    = "Passwords have at least 7 printing characters, with some non-alphabetic.";
    static final public String NO_MATCH            = "Confirmation token didn't match account name.";
    static final public String ACCOUNT_ALREADY_CONFIRMED = "Account already confirmed";

    private boolean successfulCreation;
    private String emailAddress;
    private String password;
    private String password2;
    private String confirmation;

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        // inputs are name, email, password, password2, and confirmation.  If a proposed name
        // is turned down, we can leave the email.  We should never remember or repopulate the
        // passwords or confirmation tokens across sessions.

        successfulCreation = false;
        String name = getUserName();
        if (null == name || "".equals(name)) {
            if (null != emailAddress || null != password || null != password2)  {
                warn("please enter a proposed user name.");
            }
            resetPasswords();
            return;
        }
        SecureUser existingUser = HibernateUtil.getUserByName(name);
        UnconfirmedUser unconfirmedUser = HibernateUtil.getUnconfirmedUserByName(name, HibernateUtil.currentSession());
        if (unconfirmedUser != null) {
            if (confirmation != null) {
                if (unconfirmedUser.confirm(confirmation)) {
                    warn("confirmed account for " + name);
                    successfulCreation = true;
                    redirectResult(request, response);
                } else {
                    warn(NO_MATCH);
                    setUserName("");
                }
            } else {
                warn(ACCOUNT_NOT_AVAILABLE + name + ".");
                setUserName("");
            }
        } else if (existingUser != null && confirmation != null) {
            warn(ACCOUNT_ALREADY_CONFIRMED);
            successfulCreation = true;  // 
        } else if (confirmation != null) {
            warn(NO_MATCH);
            setUserName("");
        } else if (existingUser != null) {
            warn(ACCOUNT_NOT_AVAILABLE + name);
            setUserName("");
        } else if (validateNameAddressPassword()) {
            UnconfirmedUser unconfirmed = createUnconfirmedUser(getUserName(), password, emailAddress, request.getRequestURL().toString());
            if (unconfirmed == null) {
                warn("Unable to create account.  Please retry or report problems to admin.");
            } else {
                warn(LOOK_FOR_CONFIRMATION);
            }
        } else {
            redirectResult(request, response);
            setUserName("");
        }
        resetPasswords();
    }

    private void resetPasswords() {
        setPassword(null);
        setPassword2(null);
        setConfirmation(null);
    }

    private UnconfirmedUser createUnconfirmedUser(String userName, String password, String emailAddress, String requestURL) {
        return MarketOwner.createUnconfirmedUserAndNotifyOwner(userName, password, emailAddress, requestURL);
    }

    public void warn(String arg) {
        getUserAsWarnable().warn(arg);
    }

    private boolean validateNameAddressPassword() {
        if (! validateUserName()) {
            warn("user Name doesn't conform to requirements.");
            return false;
        }
        if (null == emailAddress) {
            warn("please enter an email address.");
            return false;
        } else if (!MailUtil.validateAddress(emailAddress)) {
            warn("this doesn't look like an email address to me: '" + emailAddress + "'.");
            return false;
        }
        if (password2 != null && !"".equals(password2) && !password2.equals(password)) {
            warn("passwords don't match.");
            return false;
        }
        if (! SecureUser.validatePassword(password)) {
            warn(PASSWORD_WARNING);
            return false;
        }
        return true;
    }

    private boolean validateUserName() {
        String name = getUserName();
        if (name == null || "".equals(name)) {
            return false;
        }
        return SecureUser.validateUserName(name);
    }

    public String getRequestURL(HttpServletRequest request) {
        if (successfulCreation) {
            return WelcomeScreen.WELCOME_JSP;
        } else {
            return null;
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    private String getEmailAddress() {
        return emailAddress;
    }

    public String getEmailAddressForWeb() {
        String address = getEmailAddress();
        if (null == address) {
            return "";
        }
        return address;
    }

    public String getUserNameForWeb() {
        String userName = getUserName();
        if (userName == null) {
            return "";
        }
        return userName;
    }

    public boolean hasWarnings() {
        return getUserAsWarnable().hasWarnings();
    }

    public String getWarnings() {
        return getUserAsWarnable().getWarningsHTML();
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }
}
