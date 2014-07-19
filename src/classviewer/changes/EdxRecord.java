package classviewer.changes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Result of parsing EdX list.
 * 
 * @author TK
 */
public class EdxRecord {
	public static SimpleDateFormat dformat1 = new SimpleDateFormat(
			"dd MMM yyyy");
	public static SimpleDateFormat dformat3 = new SimpleDateFormat(
			"MMMMM yyyy");

	private String courseId;
	private String name;
	private String descr;
	private String univer;
	private String home;
	private String startStr;
	private Date date;
	private int duration;
	private boolean isNew;

	public EdxRecord(String courseId, String name, String descr, String univer,
			Date date, String startStr, int duration, String home, boolean isNew) {
		this.courseId = courseId;
		this.name = name;
		this.descr = descr;
		this.univer = univer;
		this.date = date;
		this.startStr = startStr;
		this.duration = duration;
		this.home = home;
		this.isNew = isNew;
	}

	public String toString() {
		return courseId + ", " + name + "\n\t" + descr + "\n\t" + univer + ", "
				+ (date==null?"No start":dformat1.format(date)) + ", " + duration + " weeks, " + home + ", " + isNew;

	}

	public String getUniversity() {
		return univer;
	}

	public String getCourseId() {
		return courseId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return descr;
	}

	public String getHome() {
		return home;
	}

	public Date getStart() {
		return date;
	}
	
	public String getStartStr() {
		return startStr;
	}
	
	public static Date parseDate(String str) {
		if (str == null)
			return null;
		str = str.replace("<span class=\"date-display-single\">", "");
		str = str.replace("<strong>", "");
		str = str.replace(",", "");
		str = str.replace("Sept ", "Sep "); // More of these?
		
		try {
			return dformat1.parse(str);
		} catch (Exception e) {
		}
		try {
			return dformat3.parse(str);
		} catch (Exception e) {
		}
		if (str.toLowerCase().contains("self")) {
			// Self-paced class, start whenever
			return null;
		}
		try {
			return parseQuarter(str);
		} catch (Exception e) {
			System.err.println("Cannot parse date " + str);
		}
		return null;
	}

	private static Date parseQuarter(String str) throws ParseException {
		String[] parts = str.trim().split(" ");
		if (parts.length != 2)
			throw new ParseException("Need 2 parts for quater", 0);
		if (parts[0].toUpperCase().charAt(0) != 'Q')
			throw new ParseException("Quater should start with Q", 0);
		int quater = Integer.parseInt(parts[0].substring(1));
		if (quater < 1 || quater > 4)
			throw new ParseException("Quater should be between 1 and 4", 1);
		int year = Integer.parseInt(parts[1]);
			
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.MONTH, 3*(quater-1));
		
		return c.getTime();
	}

	public int getDuration() {
		return duration;
	}

	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}
}
