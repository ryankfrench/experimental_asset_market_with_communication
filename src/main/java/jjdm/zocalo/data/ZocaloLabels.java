package jjdm.zocalo.data;

import java.util.Properties;

/**
 * Used to hold pass labels for UI.
 */
public class ZocaloLabels {

	private static ZocaloLabels INSTANCE = new ZocaloLabels();
	private Properties properties = null;

	/**
	 * Used on the trader screen to show participation cash.
	 *
	 * @return
	 */
	public static String getParticipationCash() {
		return getLabel("participation.cash", "Participation");
	}

	/**
	 * Add a new set of properties to lookup.
	 *
	 * @param properties
	 */
	public static synchronized void setProperties(Properties properties) {
		INSTANCE.properties = properties;
	}

	/**
	 * Lookup a label from the properties, and return value or default if not found.
	 *
	 * @param name         The property name (minus the preceding label.).
	 * @param defaultValue The default value to return if not found.
	 * @return The property value or default.
	 */
	private static String getLabel(String name, String defaultValue) {
		if (INSTANCE.properties == null) {
			return defaultValue;
		}
		String result = INSTANCE.properties.getProperty("label." + name);
		return result == null ? defaultValue : result.trim();
	}
}
