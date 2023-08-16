package jjdm.zocalo;

import net.commerce.zocalo.logging.Log4JHelper;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Log file processor.
 */
public class DownloadHelper {

	public static String TYPE_RAW = "raw";
	public static String TYPE_CSV = "csv";
	private static Logger logger = Logger.getLogger(DownloadHelper.class);

	/**
	 * Process a log file and return the output.
	 *
	 * @param fileName The name (without full path) of the file.
	 * @param type     The type of download (e.g. raw, csv).
	 * @return The output of the processing.
	 */
	public static String downloadLogFile(String fileName, String type) {

		if (fileName == null || type == null) {
			throw new IllegalArgumentException("The fileName and type are required.");
		}

		Path path = Paths.get(Log4JHelper.getInstance().getLogFileDir(), fileName);

		if (Files.notExists(path)) {
			throw new IllegalArgumentException("The file could not be found: " + path);
		}

		if (TYPE_RAW.equals(type)) {
			try {
				StringBuilder builder = new StringBuilder();
				List<String> lines = Files.readAllLines(path, Charset.defaultCharset());
				for (String line : lines) {
					builder.append(line + "\r\n");
				}
				return builder.toString();
			} catch (IOException e) {
				throw new RuntimeException("Unable to read log file.", e);
			}

		} else if (TYPE_CSV.equals(type)) {
			try {

				String SEP = File.separator;
				ProcessBuilder pb = new ProcessBuilder("perl", "ParseLogFile.pl", path.toString());
				String pathToCgi = ZocaloSetupServlet.SERVLET_CONTEXT.getResource("/WEB-INF/cgi").getPath();
				pb.directory(new File(pathToCgi));
				pb.redirectErrorStream(true);
				Process p = pb.start();
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = br.readLine()) != null) {
					builder.append(line + "\r\n");
				}
				int exitValue = p.waitFor();
				logger.debug("Exit Value: " + exitValue);
				return builder.toString();
			} catch (Exception e) {
				throw new RuntimeException("Unable to parse log file to CSV.", e);
			}

		} else {
			throw new IllegalArgumentException("The type of download is not recognized: " + type);
		}

	}

}
