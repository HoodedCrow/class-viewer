package classviewer;

import java.io.File;
import java.util.HashMap;

/**
 * Settings for the whole app that know how to read and write themselves.
 * 
 * @author TK
 */
public class Settings {
	// KEY NAMES
	public static String BASE_DIR = "BaseDirectory";
	public static String STATIC_DATA_FNAME = "DataFile";
	public static String STATUS_DATA_FNAME = "StatusFile";

	/** Name of the settings file. Not part of the settings themselves */
	private String settingsFilename;
	public static final String DEFAULT_SETTINGS_FILENAME = "courseviewer.ini";

	/**
	 * The main hash containing all the values. Note that numbers are stored as
	 * Strings.
	 */
	private HashMap<String, String> settings = new HashMap<String, String>();

	/** Settings with a specific file name */
	public Settings(String fileName) {
		if (fileName != null)
			settingsFilename = fileName;
		else
			settingsFilename = DEFAULT_SETTINGS_FILENAME;
		loadDefaults();
		load();
	}

	/** Load default values */
	private void loadDefaults() {
		settings.put(BASE_DIR, "./data");
		settings.put(STATIC_DATA_FNAME, "static-data.xml");
		settings.put(STATUS_DATA_FNAME, "statuses.txt");
	}

	private void load() {
		File file = new File(settingsFilename);
		if (!file.exists()) {
			System.out.println("Settings file " + settingsFilename
					+ " is not found");
			return;
		}

		// TODO Auto-generated method stub

	}

	public File getStaticFile() {
		File dir = new File(settings.get(BASE_DIR));
		return new File(dir, settings.get(STATIC_DATA_FNAME));
	}

	public String getString(String key) {
		return settings.get(key);
	}

}
