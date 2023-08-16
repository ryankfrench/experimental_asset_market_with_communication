package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.JspSupport.ClaimPurchase;
import net.commerce.zocalo.hibernate.HibernateTestUtil;

import java.io.File;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentHistoryGraphTest extends PersistentTestHelper {
    protected void setUp() throws Exception {
        HibernateTestUtil.resetSessionFactory();
    }

    // PersistentHistoryTest.* works just fine
    public void testHistoryGraph() throws Exception {
        String marketName = "rain";
        copyFile("data/PersistentGraphSave.script", "data/PersistentGraphRead.script");
        copyFile("data/PersistentGraphSave.log", "data/PersistentGraphRead.log");
        String propertiesFileName = "data/PersistentGraphSave.properties";
        assertTrue("PersistentGraphSave.properties should exist.",  new File(propertiesFileName).exists());
        copyFile("data/PersistentGraphSave.data", "data/PersistentGraphRead.data");
        copyFile(propertiesFileName, "data/PersistentGraphRead.properties");

        String expectedFileName = "charts/" + marketName + "-pv.png";
        File chartFile = new File(expectedFileName);
        chartFile.delete();
        String webDirName = "webpages";
        File webDir = new File(webDirName);
        if (!webDir.exists()) {
            webDir.mkdir();
        }
        File chartsDir = new File(webDirName + "/charts");
        if (!chartsDir.exists()) {
            chartsDir.mkdir();
        }
        assertTrue("charts directory must exist.", chartsDir.exists());

        {
            manualSetUpForUpdate("data/PersistentGraphRead");

            ClaimPurchase screen = new ClaimPurchase();
            screen.setClaimName(marketName);

            String generatedName = screen.historyChartNameForJsp();
            assertEquals(new File(expectedFileName), new File(generatedName));
            assertTrue("generate a chart in the right place", new File("webpages/" + generatedName).exists());
            HibernateTestUtil.resetSessionFactory();
        }
    }
}
