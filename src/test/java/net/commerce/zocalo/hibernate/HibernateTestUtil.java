package net.commerce.zocalo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;

import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;

import net.commerce.zocalo.ajax.dispatch.MockDatum;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class HibernateTestUtil extends HibernateUtil {
    static public void initializeSessionFactory(String dbFilePath, boolean mode) {
        String connectionURL = HibernateUtil.connectionUrl(dbFilePath, mode);
        String schemaCreateMode = mode ? HibernateUtil.SCHEMA_CREATE : HibernateUtil.SCHEMA_UPDATE;
        try {
            Configuration configuration = new Configuration();
            addClasses(configuration);
            configuration.addClass(MockDatum.class);
            if (connectionURL != null && ! "".equals(connectionURL)) {
                configuration.setProperty("hibernate.connection.url", connectionURL);
            }
            configuration.setProperty("hibernate.hbm2ddl.auto", schemaCreateMode);

            sessionFactory.close();
            sessionFactory = configuration.buildSessionFactory();
            resetSession();
        } catch (HibernateException ex) {
            System.err.println("Hibernate Exception Thrown in buildSessionFactory().  " +
                    "Is HIBERNATE.JAR Missing?\n" + ex);
            throw new ExceptionInInitializerError(ex);
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed for test." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Session currentSession() throws HibernateException {
        Session s = (Session) sessionHolder.get();
        if (s == null) {
            if (sessionFactory != null) {
                s = sessionFactory.openSession();
                sessionHolder.set(s);
            } else {
                s = new NoDBSession();
                return s;
            }
        }
        return s;
    }

    public static void resetSession() throws HibernateException {
        Session s = (Session) sessionHolder.get();
        if (s != null) {
            s.close();
        }
        sessionHolder.set(null);
    }

    public static void closeSession() throws HibernateException {
        Session s = (Session) sessionHolder.get();
        boolean cleanShutdown = true;
        if (s != null) {
            Connection connection = s.connection();
            if (connection != null) {
                String url = null;
                try {
                    url = connection.getMetaData().getURL();
                } catch (SQLException e) {
                    System.out.print("unable to get database URL.\n");
                    cleanShutdown = false;
                }
                if (cleanShutdown) {
                    try {
                        if (url.indexOf("derby") >= 0) {
                            DriverManager.getConnection(url + ":;shutdown=true");
                            cleanShutdown = false;
                        }
                    } catch (SQLException e) {
                        System.out.print("shutdown database cleanly.\n");
                    }
                }
                if (! cleanShutdown) {
                    System.out.print("Didn't shutdown database cleanly.\n");
                }
            }
        }
        resetSession();
    }

    public static void resetSessionFactory() {
        resetSession();
        sessionFactory.close();
        sessionFactory = new NoDBSessionFactory();
    }
}
