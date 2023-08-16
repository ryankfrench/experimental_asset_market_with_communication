package net.commerce.zocalo.service;

import net.commerce.zocalo.PersistentTestHelper;

// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class ServiceStarterTest extends PersistentTestHelper {
    public void testInitialStartup() throws Exception {
        manualSetUpForCreate("data/ServiceStartTest");
    }
}
