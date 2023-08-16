package net.commerce.zocalo.logging;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Initialize the Log4J logging subsystem.
 */
public class Log4JHelper {

	private static Log4JHelper INSTANCE = new Log4JHelper();

	static public final String UserError = "UserError";
	static private final Layout LAYOUT = new PatternLayout("%d{MM/dd hh:mm:ss.SSS/zzz} %6p - %12c{1} - %m%n");
	private String logFileDir;

	private Log4JHelper() {
		String baseDir = System.getenv().get("CATALINA_BASE");
		if (baseDir == null) {
			// Getting around the testing environment.
			baseDir = System.getenv().get("CATALINA_HOME");
		}
		this.logFileDir = baseDir + File.separator + "logs";
		LogManager.resetConfiguration();
		URL config = Log4JHelper.class.getClassLoader().getResource("log4j.properties");
		PropertyConfigurator.configure(config);
	}

	public static Log4JHelper getInstance() {
		return INSTANCE;
	}

	public String getFilenameFromAppender(FileAppender appender) {
		String fullPath = appender.getFile();
		return fullPath.replace(this.logFileDir, "").replace(File.separator, "");
	}

	public String getLogFileDir() {
		return this.logFileDir;
	}

	public FileAppender openLogFile(String title) {
		if (title == null) {
			title = "NO_TITLE";
		}
		File logFile = null;
		FileAppender fileAppender = null;
		try {
			logFile = File.createTempFile(title + "-", ".log", new File(logFileDir));
			fileAppender = new FileAppender(LAYOUT, logFile.getAbsolutePath());
		} catch (IOException e) {
			Logger logger = Logger.getLogger("NoLogFile");
			logger.error("Couldn't create a log file", e);
		}
		if (fileAppender != null) {
			BasicConfigurator.configure(fileAppender);
		}
		return fileAppender;
	}
}
