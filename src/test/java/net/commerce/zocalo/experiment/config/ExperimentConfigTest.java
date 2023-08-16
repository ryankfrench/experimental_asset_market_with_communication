package net.commerce.zocalo.experiment.config;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.JspSupport.MockHttpServletRequest;
import net.commerce.zocalo.html.HtmlSimpleElement;

import java.util.Map;
import java.io.File;

import junit.framework.TestCase;

public class ExperimentConfigTest extends TestCase {
    public void testSubmit() throws TypeMisMatch {
        ConfigEditor page = new ConfigEditor();
        MockHttpServletRequest req = new MockHttpServletRequest();
        Map params = req.getParameterMap();

        ExperimentConfig c;
        c = new ExperimentConfig(1, "3:00", "title");

        assertEquals(true, c.getBoolean(ExperimentConfig.WHOLE_SHARE_TRADING));
        assertEquals(false, c.getBoolean(ExperimentConfig.PRICE_BETTERING));

        params.put("duration", "2:00");
        params.put("rounds", "4");
        params.put("title", "experiment");
        params.put("action", ConfigEditor.UPDATE_COMMAND);
        page.processRequest(req, null);
        c = page.getConfig();
        assertEquals(false, c.getBoolean(ExperimentConfig.WHOLE_SHARE_TRADING));
        assertEquals(false, c.getBoolean(ExperimentConfig.PRICE_BETTERING));
        assertEquals(120, c.getTime("duration"));
        assertEquals(4, c.getInt("rounds"));
        assertEquals("experiment", c.getString("title"));
        assertEquals("", c.getString("saveFile"));

        params.put(ExperimentConfig.WHOLE_SHARE_TRADING, "on");
        params.put("action", ConfigEditor.UPDATE_COMMAND);
        page.processRequest(req, null);
        c = page.getConfig();
        assertEquals(true, c.getBoolean(ExperimentConfig.WHOLE_SHARE_TRADING));
        assertEquals(false, c.getBoolean(ExperimentConfig.PRICE_BETTERING));
    }

    public void testSave() throws TypeMisMatch {
        File file = new File("save.xml");
        file.delete();
        ConfigEditor page = new ConfigEditor();
        MockHttpServletRequest req = new MockHttpServletRequest();
        Map params = req.getParameterMap();
        params.put("duration", "2:00");
        params.put("title", "experiment");
        params.put(ExperimentConfig.WHOLE_SHARE_TRADING, "on");
        params.put("action", ConfigEditor.UPDATE_COMMAND);
        page.processRequest(req, null);
        ExperimentConfig c = page.getConfig();
        assertEquals("experiment", c.getString("title"));
        assertEquals("", c.getString("saveFile"));

        assertFalse(file.exists());
        params.put(ExperimentConfig.PRICE_BETTERING, "off");
        params.put(ExperimentConfig.WHOLE_SHARE_TRADING, "on");
        params.put("saveFile", "save");
        page.processRequest(req, null);
        c = page.getConfig();
        assertEquals("save", c.getString("saveFile"));
        assertEquals("2:00", c.getString("duration"));
        assertEquals(true, c.getBoolean(ExperimentConfig.WHOLE_SHARE_TRADING));
        assertEquals(false, c.getBoolean(ExperimentConfig.PRICE_BETTERING));

        params.put("action", ConfigEditor.SAVE_COMMAND);
        page.processRequest(req, null);
        assertEquals(true, c.getBoolean(ExperimentConfig.WHOLE_SHARE_TRADING));
        assertTrue("file should exist", file.exists());
        file.delete();
    }

    public void testConfigForm() {
        final StringBuffer buf = new StringBuffer();
        String expected = "<form method=POST action='config.jsp' autocomplete=\"off\">";
        buf.append(HtmlSimpleElement.formHeaderWithPost(ExperimentConfig.CONFIG_PAGE + ".jsp"));
        assertEquals(expected, buf.toString());
    }

    public void xtestChoiceField() throws TypeMisMatch {
        ConfigEditor page = new ConfigEditor();
        MockHttpServletRequest req = new MockHttpServletRequest();
        Map params = req.getParameterMap();
        params.put("title", "experiment");
        params.put("duration", "2:00");

        params.put("action", ConfigEditor.UPDATE_COMMAND);
        page.processRequest(req, null);
        ExperimentConfig c = page.getConfig();
        assertEquals("experiment", c.getString("title"));
        assertEquals("", c.getString("saveFile"));

        assertEquals("conventional", c.getChoice("main"));
    }

}

