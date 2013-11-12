package classviewer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Settings for the whole app that know how to read and write themselves.
 * 
 * @author TK
 */
public class Settings {
	// KEY NAMES
	public static final String BASE_DIR = "BaseDirectory";
	public static final String STATIC_DATA_FNAME = "DataFile";
	public static final String STATUS_DATA_FNAME = "StatusFile";
	public static final String COURSERA_URL = "CourseraJsonURL";
	public static final String EDX_URL = "EdXListURL";
	public static final String LOOK_BACK_WEEKS = "LookBackWeeks";
	public static final String OLD_AGE_IN_DAYS = "EdXOldAgeInDays";

	// Prefix for colors
	public static final String COLOR = "Color";

	/** Name of the settings file. Not part of the settings themselves */
	private String settingsFilename;
	public static final String DEFAULT_SETTINGS_FILENAME = "courseviewer.ini";

	/** Cached colors */
	private HashMap<String, Color> cachedColors = new HashMap<String, Color>();

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
		try {
			load();
		} catch (IOException e) {
			System.err.println("Could not load file " + settingsFilename
					+ ":\n" + e);
		}
	}

	/** Load default values */
	private void loadDefaults() {
		settings.put(BASE_DIR, ".");
		settings.put(STATIC_DATA_FNAME, "static-data.xml");
		settings.put(STATUS_DATA_FNAME, "statuses.txt");
		settings.put(COURSERA_URL,
				"https://www.coursera.org/maestro/api/topic/list?full=1");
		settings.put(EDX_URL, "https://www.edx.org");

		// How many weeks from today to look back in the calendar
		settings.put(LOOK_BACK_WEEKS, "10");

		// Define "too old to be removed" for EdX classes
		settings.put(OLD_AGE_IN_DAYS, "210");

		// Calendar colors
		settings.put(COLOR + "UCalBg", "B0B0B0");
		settings.put(COLOR + "YCalBg", "78FAFA");
		settings.put(COLOR + "NCalBg", "FF0000");
		settings.put(COLOR + "MCalBg", "F0F000");
		settings.put(COLOR + "DCalBg", "005000");
		settings.put(COLOR + "RCalBg", "00FF00");

		settings.put(COLOR + "UCalFg", "000000");
		settings.put(COLOR + "YCalFg", "001478");
		settings.put(COLOR + "NCalFg", "780000");
		settings.put(COLOR + "MCalFg", "007800");
		settings.put(COLOR + "DCalFg", "00FF00");
		settings.put(COLOR + "RCalFg", "005000");

		settings.put(COLOR + "TodayBg", "E0A0E0");
	}

	private void load() throws IOException {
		File file = new File(settingsFilename);
		if (!file.exists()) {
			System.out.println("Settings file " + settingsFilename
					+ " is not found");
			return;
		}

		FileReader reader = new FileReader(file);
		BufferedReader br = new BufferedReader(reader);
		String s = br.readLine();

		while (s != null) {
			s = s.trim();
			if (!s.isEmpty() && !s.startsWith("#")) {
				int i = s.indexOf("=");
				if (i > 0) {
					String key = s.substring(0, i).trim();
					if (!settings.containsKey(key))
						System.err.println("Warning: unknown settings key "
								+ key);
					settings.put(key, s.substring(i + 1).trim());
				} else
					System.err.println("Ignoring line " + s);
			}
			s = br.readLine();
		}

		reader.close();
	}

	public File getStaticFile() {
		File dir = new File(settings.get(BASE_DIR));
		return new File(dir, settings.get(STATIC_DATA_FNAME));
	}

	public File getStatusFile() {
		File dir = new File(settings.get(BASE_DIR));
		return new File(dir, settings.get(STATUS_DATA_FNAME));
	}

	public String getString(String key) {
		return settings.get(key);
	}

	public Color getColor(String name) {
		Color clr = cachedColors.get(name);
		if (clr != null)
			return clr;
		String str = settings.get(COLOR + name);
		if (str == null) {
			System.err.println("No color defined: " + name);
			return Color.gray;
		}
		try {
			clr = new Color(Integer.parseInt(str, 16));
		} catch (NumberFormatException e) {
			System.err.println("Cannot parse hex RGB " + str + ": " + e);
			return Color.gray;
		}

		cachedColors.put(name, clr);
		return clr;
	}

	public int getInt(String name, int def) {
		try {
			return Integer.parseInt(settings.get(name));
		} catch (Exception e) {
			return def;
		}
	}

}
