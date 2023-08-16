package net.commerce.zocalo.hibernate;

import net.commerce.zocalo.PersistentTestHelper;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class HibernateSingletonTest extends PersistentTestHelper {
    public void testNoHibernate() {
        HibernateTestUtil.resetSessionFactory();
        new NoDBHibernateUtil().setupSessionFactory();
        Session session = HibernateUtil.currentSession();
        Object bar = new Object();
        try {
            storeObject(bar);
        } catch (Exception e) {
            fail("bar should be persistable with NoDBSession.");
        }

        assertFalse(session.contains(new Object()));
        HibernateTestUtil.resetSessionFactory();
    }

    public void xtestSchemaUpdate() {
        Configuration config = new Configuration();
        HibernateTestUtil.addClasses(config);
        SchemaExport export = new SchemaExport(config);
        export.create(true, false);

        assertEquals(0, export.getExceptions().size());
    }

    public void testStoringNoHibernate() throws Exception {
        HibernateTestUtil.resetSessionFactory();
        Object foo = new Object();
        manualSetUpForCreate("data/HibernateSingletonTest");
        try {
            storeObject(foo);
            fail("foo should not be persistable");
        } catch (Exception e) {
        }
        manualTearDown();

        new NoDBHibernateUtil().setupSessionFactory();
        try {
            storeObject(foo);
        } catch (Exception e) {
            fail("foo should be persistable with NoDBSession.");
        }
        HibernateTestUtil.resetSessionFactory();

        try {
            storeObject(foo);
        } catch (Exception e) {
            fail("Shouldn't be a persistent session hanging around");
        }
        manualTearDown();
    }
}
