package classviewer.model;

public enum Source {
	MISSING, COURSERA, EDX, UDACITY, STANFORD;
	private char[] letters = { 'm', 'c', 'e', 'u', 's' };
	private String[] words = { "Unknown", "Coursera", "EdX", "Udacity", "Stanford" };

	public char oneLetter() {
		return letters[this.ordinal()];
	}

	public String pretty() {
		return words[this.ordinal()];
	}

	public static Source fromStringOrNull(String src) {
		if (src == null)
			return MISSING;
		for (Source v : Source.values())
			if (v.oneLetter() == src.charAt(0))
				return v;
		System.err.println("Unknown Source code [" + src + "]");
		return MISSING;
	}
}
