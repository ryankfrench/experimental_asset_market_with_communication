package net.commerce.zocalo.JspSupport;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MockHttpServletRequest implements MockHttpServletRequestI {
    private Map cookies = new HashMap();
    private String nextUrl;
    private Map<String, String> paramMap;

    public MockHttpServletRequest() {
    }

    public void setCookie(Cookie cookie) {
        if (cookie != null) {
        cookies.put(cookie.getName(), cookie.getValue());
        }
    }

    public Cookie[] getCookies() {
        Set keys = cookies.keySet();
        Cookie[] c = new Cookie[keys.size()];
        int i = 0;
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            String key =  (String) iterator.next();
            c[i++] = new Cookie(key, (String) cookies.get(key));
        }
        return c;
    }

    public void setNextUrl(String url) {
        nextUrl = url;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public StringBuffer getRequestURL() {
        return new StringBuffer(nextUrl);
    }

    public Map getParameterMap() {
        if (paramMap == null) {
            paramMap = new HashMap<String, String>();
        }
        return paramMap;
    }

    public String getParameter(String string) {
        if (paramMap == null) return null;

        return paramMap.get(string);
    }

    public Enumeration getParameterNames() {
        ConcurrentHashMap map = new ConcurrentHashMap(paramMap);
        return map.keys();
    }

    public String getAuthType() { return null; }
    public long getDateHeader(String string) { return 0; }
    public String getHeader(String string) { return null; }
    public Enumeration getHeaders(String string) { return null; }
    public Enumeration getHeaderNames() { return null; }
    public int getIntHeader(String string) { return 0; }
    public String getMethod() { return null; }
    public String getPathInfo() { return null; }
    public String getPathTranslated() { return null; }
    public String getContextPath() { return null; }
    public String getQueryString() { return null; }
    public String getRemoteUser() { return null; }
    public boolean isUserInRole(String string) { return false; }
    public Principal getUserPrincipal() { return null; }
    public String getRequestedSessionId() { return null; }
    public String getRequestURI() { return null; }
    public String getServletPath() { return null; }
    public HttpSession getSession(boolean b) { return null; }
    public HttpSession getSession() { return null; }
    public boolean isRequestedSessionIdValid() { return false; }
    public boolean isRequestedSessionIdFromCookie() { return false; }
    public boolean isRequestedSessionIdFromURL() { return false; }
    public boolean isRequestedSessionIdFromUrl() { return false; }
    public Object getAttribute(String string) { return null; }
    public Enumeration getAttributeNames() { return null; }
    public String getCharacterEncoding() { return null; }
    public void setCharacterEncoding(String string) throws UnsupportedEncodingException { }
    public int getContentLength() { return 0; }
    public String getContentType() { return null; }
    public ServletInputStream getInputStream() throws IOException { return null; }
    public String[] getParameterValues(String string) { return new String[0]; }
    public String getProtocol() { return null; }
    public String getScheme() { return null; }
    public String getServerName() { return null; }
    public int getServerPort() { return 0; }
    public BufferedReader getReader() throws IOException { return null; }
    public String getRemoteAddr() { return null; }
    public String getRemoteHost() { return null; }
    public void setAttribute(String string, Object object) { }
    public void removeAttribute(String string) { }
    public Locale getLocale() { return null; }
    public Enumeration getLocales() { return null; }
    public boolean isSecure() { return false; }
    public RequestDispatcher getRequestDispatcher(String string) { return null; }
    public String getRealPath(String string) { return null; }
    public int getRemotePort() { return 0; }
    public String getLocalName() { return null; }
    public String getLocalAddr() { return null; }
    public int getLocalPort() { return 0; }

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void login(String username, String password) throws ServletException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getContentLengthLong() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}
}
