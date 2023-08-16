package net.commerce.zocalo.JspSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public interface MockHttpServletRequestI extends HttpServletRequest {
    public void setNextUrl(String url);
    public void setCookie(Cookie cookie);
    public String getNextUrl();
}
