package classviewer.model;

/**
 * Enum for class status.
 * 
 * @author TK
 */
public class Status {
	public static final Status UNKNOWN = new Status('U', "unknown");
	public static final Status YES = new Status('Y', "yes");
	public static final Status NO = new Status('N', "no");
	public static final Status MAYBE = new Status('M', "maybe");
	public static final Status REGISTERED = new Status('R', "registered");
	public static final Status DONE = new Status('D', "done");

	private char value;
	private String name;

	private Status(char value, String name) {
		this.value = value;
		this.name = name;
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
		if ("r".equals(str.toLowerCase()) || "6".equals(str))
			return REGISTERED;
		System.err.println("Unknown status value: " + str);
		return UNKNOWN;
	}
}
