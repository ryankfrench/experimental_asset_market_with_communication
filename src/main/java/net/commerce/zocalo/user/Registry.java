package net.commerce.zocalo.user;

import net.commerce.zocalo.hibernate.HibernateUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Hashtable;
import java.util.Map;
import java.util.Date;

import org.apache.log4j.Logger;

// Copyright 2007, 2008 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** The Registry tracks users who have logged in.  When someone logs in, their SecureUser object
 is stored in the registry, keyed by a randomly generated token.  The token is placed in a
 Cookie, keyed by the REGISTRATION keyword.  If they connect with the appropriate cookie,
 we'll be able to find their SecureUser object when they next connect from the cookie they provide.

 Users who authenticate as admin will get a Cookie with the ADMIN_TOKEN keyed to a different random
 token that can be used to look up the ADMIN_TOKEN in the Registry.  Anyone with an appropriate
 ADMIN_TOKEN Cookie will be able to edit the user list and add claims.  (I expect these powers to
 devolve to separate markets later, but this starting point allows me to enforce logins and give
 someone the power to create users and markets before making the devolved powers work.) */
public class Registry {
    static public final String REGISTRATION = "registered";
    static public final String ADMIN_TOKEN = "ADMIN_TOKEN";

    private final Map<String, String> registrants = new Hashtable<String, String>();
    static public PasswordUtil passwdGen;

    public Registry() {
    }

    public long userCount() {
        return registrants.size();
    }

    public Cookie register(SecureUser user) {
        String token = newToken();
        registrants.put(token, user.getName());
        return new Cookie(REGISTRATION, token);
    }

    public void register(UnconfirmedUser user) {
        user.register(registrants);
        return;
    }

    public SecureUser lookupUser(HttpServletRequest request) {
        if (request == null) { return null; }

        Cookie[] cookies = request.getCookies();
        if (null == cookies) {
            return null;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (null != cookie && REGISTRATION.equals(cookie.getName()) && null != cookie.getValue()) {
                return lookupUser(cookie.getValue());
            }
        }
        return null;
    }

    public SecureUser lookupUser(String token) {
        Object o = registrants.get(token);
        if (o instanceof String) {
            return(HibernateUtil.getUserByName((String) o));
        } else {
            return null;
        }
    }

    public void addAdminToken(String token) {
        registrants.put(token, ADMIN_TOKEN);
    }

    public boolean isAdminToken(String token) {
        return ADMIN_TOKEN.equals(registrants.get(token));
    }

    public void removeUnconfirmed(String token) {
        registrants.remove(token);
    }

    public static void initPasswdSeed(String password) {
        long seed = new Date().getTime();
        seed = seed + password.hashCode();
        passwdGen = PasswordUtil.make(seed);
    }

    static public String newToken() {
        String base64Tok = passwdGen.nextToken(10);
        base64Tok = base64Tok.replaceAll("\\+", "@");  // At this point, we're constructing a token for
        //  the web, and PLUS is weird in URLs.  The token doesn't have to be base64.
        return base64Tok;
    }
}
