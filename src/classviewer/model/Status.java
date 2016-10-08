package classviewer.model;

/**
 * Enum for class status.
 * 
 * @author TK
 */
public class Status {
	public static final Status UNKNOWN = new Status('U', "unknown", 5);
	public static final Status YES = new Status('Y', "yes", 3);
	public static final Status NO = new Status('N', "no", 7);
	public static final Status MAYBE = new Status('M', "maybe", 4);
	public static final Status REGISTERED = new Status('R', "registered", 0);
	public static final Status AUDITED = new Status('A', "audited", 2);
	public static final Status DONE = new Status('D', "done", 1);
	public static final Status CHAIN = new Status('C', "chain", 6);

	private static final Status[] allValues = { UNKNOWN, YES, NO, MAYBE,
			REGISTERED, AUDITED, DONE, CHAIN };

	private char value;
	private String name;
	private int calendarOrder;

	private Status(char value, String name, int calendarOrder) {
		this.value = value;
		this.name = name;
		this.calendarOrder = calendarOrder;
	}

	public String toString() {
		return "" + value;
	}

	public String getName() {
		return name;
	}

	public static Status parse(String str) {
		if ("u".equals(str.toLowerCase()) || "1".equals(str))
			return UNKNOWN;
		if ("y".equals(str.toLowerCase()) || "2".equals(str))
			return YES;
		if ("n".equals(str.toLowerCase()) || "3".equals(str))
			return NO;
		if ("m".equals(str.toLowerCase()) || "4".equals(str))
			return MAYBE;
		if ("d".equals(str.toLowerCase()) || "5".equals(str))
			return DONE;
		// Audited added after conversion to letters from numbers.
		if ("a".equals(str.toLowerCase()))
			return AUDITED;
		if ("r".equals(str.toLowerCase()) || "6".equals(str))
			return REGISTERED;
		if ("c".equals(str.toLowerCase()) || "7".equals(str))
			return CHAIN;
		System.err.println("Unknown status value: " + str);
		return UNKNOWN;
	}

	public int getCalendarOrder() {
		return calendarOrder;
	}

	public static Status[] getAll() {
		return allValues;
	}

	/**
	 * Compute the new status of a course given one of its offerings just
	 * changed to offStat
	 */
	public Status updateByOffering(Status offStat) {
		// DONE offering makes the whole course DONE regardless of the old
		// value. DONE course never changes
		if (offStat == DONE || this == DONE)
			return DONE;
		// If passed that, same thing with AUDITED, REGISTERED, YES, and MAYBE
		if (offStat == AUDITED || this == AUDITED)
			return AUDITED;
		if (offStat == REGISTERED || this == REGISTERED)
			return REGISTERED;
		if (offStat == YES || this == YES)
			return YES;
		if (offStat == MAYBE || this == MAYBE)
			return MAYBE;
		// The rest don't change course status
		return this;
	}
}
