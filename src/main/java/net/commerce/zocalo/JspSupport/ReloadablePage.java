package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.hibernate.HibernateUtil;

import org.hibernate.Transaction;
import org.hibernate.HibernateException;
import org.apache.log4j.Logger;

import javax.servlet.http.*;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public abstract class ReloadablePage extends HttpServlet {
    private Transaction transaction;

    abstract public void warn(String s);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    abstract public void processRequest(HttpServletRequest request, HttpServletResponse response);

    protected void redirectResult(HttpServletRequest request, HttpServletResponse response) {
        redirectResult(request, new HashSet(), response);
    }

    protected void redirectResult(HttpServletRequest request, Set cookies, HttpServletResponse response) {
        if (request != null) {
            String requestURL = getRequestURL(request);

            if (request instanceof MockHttpServletRequestI) {
                MockHttpServletRequestI mock = (MockHttpServletRequestI) request;
                setupMockRedirect(mock, requestURL, cookies);
            } else if (request instanceof HttpServletRequestWrapper) {
                HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                MockHttpServletRequestI mock = (MockHttpServletRequestI) wrapper.getRequest();
                setupMockRedirect(mock, requestURL, cookies);
            } else {
            	setupRedirectResponse(request, requestURL, cookies, response);
            }
        }
    }

    private void setupMockRedirect(MockHttpServletRequestI mock, String requestURL, Set cookies) {
        mock.setNextUrl(requestURL);
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();
            mock.setCookie(cookie);
        }
    }

    private void setupRedirectResponse(HttpServletRequest request, String requestURL, Set cookies, HttpServletResponse response) {
        addCookiesToResponse(cookies, request, response);
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        if (requestURL != null) {
            String url = response.encodeRedirectURL(requestURL);
            if (url != null) {
                try {
                    response.sendRedirect(url);
                } catch (IOException e) {
                    warn("problem sending http response; accept POST update");
                }
            }
        }
    }

    public static void addCookiesToResponse(Set cookies, HttpServletRequest request, HttpServletResponse response) {
        ;
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            Cookie cookie = (Cookie) iterator.next();
            response.addCookie(cookie);
        }
        return;
    }

    abstract public String getRequestURL(HttpServletRequest request);

    public void beginTransaction() {
        transaction = HibernateUtil.beginTransactionForJsp();
    }

    public void commitTransaction() {
        if (null == transaction) {
            warn("Unable to commit transaction.  Please resubmit action.");
            return;
        }
        try {
            transaction.commit();
            transaction = null;
        } catch (HibernateException e) {
            transaction.rollback();
            e.printStackTrace();
            Logger logger = Logger.getLogger(HibernateUtil.class);
            logger.error(e);
            warn("Problems processing request; please try again.");
        } finally {
            HibernateUtil.closeSession();
        }
    }
}
