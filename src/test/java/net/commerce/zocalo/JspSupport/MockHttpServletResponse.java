package net.commerce.zocalo.JspSupport;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletOutputStream;

import java.util.*;
import java.security.Principal;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
// Copyright 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MockHttpServletResponse implements MockHttpServletResponseI {
    public void addCookie(Cookie cookie) { }
    public boolean containsHeader(String string) { return false; }
    public String encodeURL(String string) { return null; }
    public String encodeRedirectURL(String string) { return null; }
    public String encodeUrl(String string) { return null; }
    public String encodeRedirectUrl(String string) { return null; }
    public void sendError(int i, String string) throws IOException { }
    public void sendError(int i) throws IOException { }
    public void sendRedirect(String string) throws IOException { }
    public void setDateHeader(String string, long l) { }
    public void addDateHeader(String string, long l) { }
    public void setHeader(String string, String string1) { }
    public void addHeader(String string, String string1) { }
    public void setIntHeader(String string, int i) { }
    public void addIntHeader(String string, int i) { }
    public void setStatus(int i) { }
    public void setStatus(int i, String string) { }
    public String getCharacterEncoding() { return null; }
    public String getContentType() { return null; }
    public ServletOutputStream getOutputStream() throws IOException { return null; }
    public PrintWriter getWriter() throws IOException { return null; }
    public void setCharacterEncoding(String string) { }
    public void setContentLength(int i) { }
    public void setContentType(String string) { }
    public void setBufferSize(int i) { }
    public int getBufferSize() { return 0; }
    public void flushBuffer() throws IOException { }
    public void resetBuffer() { }
    public boolean isCommitted() { return false; }
    public void reset() { }
    public void setLocale(Locale locale) { }
    public Locale getLocale() { return null; }
	@Override
	public int getStatus() {
		return 0;
	}
	@Override
	public String getHeader(String name) {
		return null;
	}
	@Override
	public Collection<String> getHeaders(String name) {
		return null;
	}
	@Override
	public Collection<String> getHeaderNames() {
		return null;
	}
	@Override
	public void setContentLengthLong(long len) {

	}
}
