package net.commerce.zocalo.mail;

import junit.framework.TestCase;

import javax.mail.MessagingException;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;

// Copyright 2007 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MailTest  extends TestCase {
    public void testSendingMail() {
        assertTrue("mail executable must be accessible", new File(MailUtil.mailx).exists());
        String recipient = "test@example.com";
        String subject = "testing from java";
        String body = "foo the bar";

        assertEquals(0, MailUtil.sendMailMessage(subject, recipient, body));
        assertEquals(0, MailUtil.sendMailMessage("/usr/bin/mail", subject, recipient, body));
        assertEquals(-3, MailUtil.sendMailMessage("foo", subject, recipient, body));
    }

    // This test reads configuration from MailTest.conf.  That file is shipped with 
    // passwords stripped out (surprise!) so you'll have to fill in your own info before
    // the test will pass.
    public void testJavaMail() throws IOException {
        Properties mailProps = readMailTestParametersFromFile();
        if (mailProps == null) {
            return;
        }
        String to = mailProps.getProperty("to");
        String subject = mailProps.getProperty("subject");
        String body = mailProps.getProperty("body");

        try {
            MailUtil.sendSMTPMail(mailProps, to, subject, body, true);
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("exception during SMTP send.");
        }
    }

    static private Properties readMailTestParametersFromFile() throws IOException {
        File configFile = new File("MailTest.conf"); //NOTE: configure this yourself!  I'm not giving out MY passwords!
        if (! configFile.exists()) {
            return null;
        } else {
            Properties props = new Properties();
            InputStream stream;
            stream = new FileInputStream(configFile);
            props.load(stream);
            return props;
        }
    }

    public void xtestSendGmail() {
        Properties smtpProps = new Properties();
        String sender = "carver.on.zocalo@gmail.com";
        smtpProps.put(MailUtil.MAIL_USER, sender);
        smtpProps.put(MailUtil.MAIL_PASSWORD, "OBFUSCATED");
        smtpProps.put(MailUtil.MAIL_HOST, "smtp.gmail.com");
        smtpProps.put(MailUtil.MAIL_PORT, "465");
        smtpProps.put(MailUtil.MAIL_SECURE, "true");
        smtpProps.put(MailUtil.MAIL_AUTH, "true");
        try {
            MailUtil.sendSMTPMail(smtpProps, "yourAddress@example.com", "testing smtp sending", "no content", true);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
