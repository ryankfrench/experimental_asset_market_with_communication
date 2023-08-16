package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.user.Registry;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Set;
import java.util.HashSet;

// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class WelcomeScreen extends LoginScreen {
    static final public String USERNAME_AND_PASSWORD_WARNING = "Please type an account name and password";
    static final public String UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING = "unknown account or password";
    static final public String BAD_REQUEST = "Bad HttpRequest received.";
    static final public String WELCOME_JSP = "Welcome.jsp";
    static final public String PLEASE_USE_PRINTING_CHARS = "Please use ordinary characters in user names.";
    static final public String ADMIN_CREDENTIAL_MSG = "Added Admin Credentials.  Login to an account to trade or create markets.";
    static final public String WELCOME_NAME = "Welcome";

    private String password;
    private boolean successfulLogin = false;

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        successfulLogin = false;
        Set<Cookie> cookies = new HashSet<Cookie>();
        removeOldUserTokenFromRequest(request, response);
        boolean adminCookiePresent = MarketOwner.detectAdminCookie(request);

        if (null == request) {
            warn(BAD_REQUEST);
        } else if (defined(password)) {
            String adminTokenFromPassword = Config.matchAdminPassword(password);
            if (null != adminTokenFromPassword) {
                cookies.add(new Cookie(Registry.ADMIN_TOKEN, adminTokenFromPassword));
                redirectResult(request, cookies, response);
                warn(ADMIN_CREDENTIAL_MSG);
            } else if (!defined(getUserName())) {
                removeExistingRegistrationCookie(request, response);
                warn(UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING);
            } else {
                if (!SecureUser.validateUserName(getUserName())) {
                    removeExistingRegistrationCookie(request, response);
                    warn(PLEASE_USE_PRINTING_CHARS);
                    return;
                }

                Cookie cookie = MarketOwner.login(getUserName(), password);
                if (null == cookie) {
                    removeExistingRegistrationCookie(request, response);
                    warn(UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING);
                    return;
                }

                cookies.add(cookie);
                successfulLogin = true;
                if (adminCookiePresent) {
                    cookies.add(MarketOwner.adminCookieIfPresent(request));
                }
                redirectResult(request, cookies, response);
            }
        } else if (adminCookiePresent && defined(getUserName())) {
            removeAdminCookie(request, response);
            warn("removed Admin Authorization.  Login as Admin again when needed.");
        } else {
            warn(USERNAME_AND_PASSWORD_WARNING);
            removeExistingRegistrationCookie(request, response);
        }
    }

    public void warn(String msg) {
        getUserAsWarnable().warn(msg);
    }

    private void removeOldUserTokenFromRequest(HttpServletRequest request, HttpServletResponse response) {
        SecureUser registeredUser = MarketOwner.registryLookup(request);
        if (registeredUser != null && ! registeredUser.getName().equals(getUserName())) {
            removeExistingRegistrationCookie(request, response);
        }
    }

    void removeExistingRegistrationCookie(HttpServletRequest request, HttpServletResponse response) {
        setCookieValueNull(Registry.REGISTRATION, request, response);
    }

    void removeAdminCookie(HttpServletRequest request, HttpServletResponse response) {
        setCookieValueNull(Registry.ADMIN_TOKEN, request, response);
    }

    private void setCookieValueNull(String cookieNameToken, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieNameToken, null);
        cookie.setMaxAge(0);
        if (request != null) {
            if (request instanceof HttpServletRequestWrapper) {
                HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                MockHttpServletRequestI mock = (MockHttpServletRequestI) wrapper.getRequest();
                mock.setCookie(cookie);
            } else if (request instanceof MockHttpServletRequestI) {
                MockHttpServletRequestI mock = (MockHttpServletRequestI) request;
                mock.setCookie(cookie);
            } else if (request instanceof HttpServletRequest) {
                response.addCookie(cookie);
            }
        }
    }

    private boolean defined(String string) {
        return null != string && !"".equals(string);
    }

    public String getRequestURL(HttpServletRequest request) {
        if (successfulLogin) {
            return "account.jsp";
        } else {
            return null;
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean loginSucceeded() {
        return successfulLogin;
    }
}
