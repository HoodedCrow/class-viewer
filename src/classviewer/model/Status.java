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
}
