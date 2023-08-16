package net.commerce.zocalo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;

// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**
Session management utilities for dealing with an absent database.
 */

public class NoDBHibernateUtil extends HibernateUtil {
    static public void initializeSessionFactory(String connectionURL, String schemaCreateMode) {
        try {
            Configuration configuration = new Configuration();
            addClasses(configuration);
            if (connectionURL != null && ! "".equals(connectionURL)) {
                configuration.setProperty("hibernate.connection.url", connectionURL);
            }
            configuration.setProperty("hibernate.hbm2ddl.auto", schemaCreateMode);

            sessionFactory = configuration.buildSessionFactory();
            closeSession();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed for test." + ex);
            throw new ExceptionInInitializerError(ex);
        }

    }

    public void setupSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = NoDBSessionFactory.make();
        }
    }

    public static Session currentSession() throws HibernateException {
        Session s = NoDBSession.make();
        if (s == null && sessionFactory != null) {
            s = sessionFactory.openSession();
            sessionHolder.set(s);
        }
        return s;
    }

    public static void closeSession() throws HibernateException {
        Session s = (Session) sessionHolder.get();
        if (s != null)
            s.close();
        sessionHolder.set(null);
    }
}
