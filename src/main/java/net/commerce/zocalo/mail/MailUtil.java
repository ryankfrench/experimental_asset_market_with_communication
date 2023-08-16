package net.commerce.zocalo.mail;

import com.sun.mail.smtp.SMTPTransport;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.log4j.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Simple utility that provides the ability to send mail.  Normal entry point is
 {@link #sendSMTPMail(Properties, String, String, String, boolean) sendSMTPMail()}.
 {@link #sendMailMessage(String, String, String) SendMailMessage()} is vestigial, but might be worth
 resurrecting if you have access to a mail executable, and SMTP is hard to access.  */

public class MailUtil {
    final static public String mailx = "/usr/bin/mailx";
    final static public String TemplateDir    = "./templates";
    final static public StringTemplateGroup EmailTemplates = new StringTemplateGroup("email", TemplateDir);
    final static public String SITE_CONSTANTS_FILE_NAME = "/siteConstants.st";

    final static public String SSL_FACTORY    = "javax.net.ssl.SSLSocketFactory";
    final static public String MAIL_EXECUTABLE = "mail.executable";
    final static public String MAIL_USER      = "mail.user";
    final static public String MAIL_PASSWORD  = "mail.password";
    final static public String MAIL_HOST      = "mail.host";
    final static public String MAIL_PORT      = "mail.port";
    final static public String MAIL_SECURE    = "mail.secure";
    final static public String MAIL_SENDER    = "mail.sender";
    final static public String MAIL_AUTH      = "mail.auth";
    final static public String FROM_ADDRESS   = "from";

    public static int sendMailMessage(String subject, String recipient, String body) {
        return sendMailMessage(mailx, subject, recipient, body);
    }

    public static int sendMailMessage(String mailer, String subject, String recipient, String body) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec(new String[] {mailer.trim(), "-s", subject, recipient });
            OutputStream outputStream = process.getOutputStream();
            outputStream.write(body.getBytes()) ;
            outputStream.close();
            process.waitFor();
        } catch (IOException e) {
            return -3;
        } catch (InterruptedException e) {
            return -5;
        }
        return process.exitValue();
    }

    public static void sendSMTPMail(Properties inputProps, String to, String subject, String body, boolean debug) throws MessagingException {
        String mailExecutable = inputProps.getProperty(MAIL_EXECUTABLE);
        if (mailExecutable != null) {
            int code = sendMailMessage(mailExecutable, subject, to, body);
            if (code < 0) {
                reportError("Couldn't send mail.  Return code: " + code);
            } else {
                return;
            }
        }
        String mailHost = inputProps.getProperty(MAIL_HOST);
        if (mailHost == null) {
            reportError("mailhost must be specified.");
        }

        String sender = inputProps.getProperty(MAIL_SENDER);
        if (sender == null) {
            String message = "'" + MAIL_SENDER + "' must be a valid email address in zocalo.conf: '" + sender + "'.";
            reportError(message);
        }

        String mailUser = inputProps.getProperty(MAIL_USER);
        String mailPassword = inputProps.getProperty(MAIL_PASSWORD);

        Session session = createSMTPSession(inputProps, mailUser, mailPassword, debug);
        Message msg = createSMTPMessage(session, sender.trim(), to, subject, body);
        sendSMTPMessage(session, mailHost.trim(), mailUser, mailPassword, msg);
    }

    private static void reportError(String message) throws MessagingException {
        Logger logger = Logger.getLogger(MailUtil.class);
        logger.error(message);
        throw new MessagingException(message);
    }

    private static Session createSMTPSession(Properties inputProps, final String mailUser, final String mailPassword, boolean debug) {
        Properties sysProps = addToSystemProperties(inputProps);
        return createSession(sysProps, mailUser, mailPassword, debug);
    }

    private static Properties addToSystemProperties(Properties inputProps) {
        Properties sysProps = System.getProperties();
        sysProps.put("mail.smtp.host", inputProps.getProperty(MAIL_HOST));
        String auth = inputProps.getProperty(MAIL_AUTH);
        if (null == auth || "".equals(auth)) {
            auth = "true";
        } else {
            auth = auth.trim();
        }
        sysProps.put("mail.smtp.auth", auth);
        String secureProp = inputProps.getProperty(MAIL_SECURE);

        if(secureProp != null && secureProp.equals("true")){
            String mailPort = inputProps.getProperty(MAIL_PORT);

            //set up for mail that requires secure SMTP (ie~ GMail)
            sysProps.put("mail.transport.protocol", "smtp");
            sysProps.put("mail.smtp.port", mailPort);
            sysProps.put("mail.smtp.socketFactory.port", mailPort);
            sysProps.put("mail.smtp.socketFactory.class", SSL_FACTORY);
            sysProps.put("mail.smtp.socketFactory.fallback", "false");
            sysProps.put("mail.smtp.starttls.enable","true");
        }
        return sysProps;
    }

    private static Session createSession(Properties sysProps, final String mailUser, final String mailPassword, boolean debug) {
        Session session = Session.getDefaultInstance(sysProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUser, mailPassword);
            }
        });

        session.setDebug(debug);
        return session;
    }

    private static Message createSMTPMessage(Session session, String from, String to, String subject, String body) throws MessagingException {
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSentDate(new Date());
        msg.setSubject(subject);
        msg.setHeader("X-Mailer", "Java Mail API");
        msg.setText(body);
        return msg;
    }

    private static void sendSMTPMessage(Session session, String mailHost, String user, String password, Message msg) throws MessagingException {
        SMTPTransport t = (SMTPTransport)session.getTransport("smtp");
        t.connect(mailHost, 25, user, password);
        t.sendMessage(msg, msg.getAllRecipients());
        t.close();
    }

    static public boolean validateAddress(String address) {
        try {
            InternetAddress[] addresses = InternetAddress.parse(address, true);
            return 1 == addresses.length;
        } catch (AddressException e) {
            return false;
        }
    }
}
