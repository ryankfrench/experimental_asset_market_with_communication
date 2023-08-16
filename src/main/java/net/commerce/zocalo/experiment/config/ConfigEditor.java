package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.JspSupport.ReloadablePage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Controller for editing configuration parameters for an experiment.  */
public class ConfigEditor extends ReloadablePage {
    private ExperimentConfig config;
    private String warning;
    public final String CONFIG_JSP = "config.jsp";
    static public final String UPDATE_COMMAND = "update Config";
    static public final String SAVE_COMMAND = "save Config";

    public void warn(String s) {
        warning = warning + "<br>" + s;
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        ExperimentConfig experimentConfig = new ExperimentConfig(request);
        if (UPDATE_COMMAND.equals(request.getParameter("action"))) {
            if (experimentConfig.validate()) {
                config = experimentConfig;
            } else {
                warning = "incoming request parameters nonsensical.";
                setDefaultConfigIfUnset();
            }
        } else if (SAVE_COMMAND.equals(request.getParameter("action"))) {
            if (experimentConfig.validate()) {
                experimentConfig.save();
                config = experimentConfig;
            } else {
                warning = "parameter settings inconsistent.";
                setDefaultConfigIfUnset();
            }
        } else {
            setDefaultConfigIfUnset();
        }
    }

    private void setDefaultConfigIfUnset() {
        if (config == null) {
            config = new ExperimentConfig(3, "0", "default");
        }
    }

    public String getRequestURL(HttpServletRequest request) {
        return CONFIG_JSP;
    }

    public String renderProperties() {
        StringBuffer buf = new StringBuffer();
        config.renderHtml(buf);
        if (warning != null && ! "".equals(warning)) {
            buf.append("<br>" + warning);
        }
        return buf.toString();
    }

    public ExperimentConfig getConfig() {
        return config;
    }
}
