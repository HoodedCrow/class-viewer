package classviewer.model;

/**
 * Common record for universities and categories.
 * 
 * @author TK
 */
public class DescRec implements Named, Comparable<DescRec> {
	Source source;
	String id, name, description;

	public DescRec(Source source, String id, String name, String desc) {
		this.source = source;
		this.id = id;
		this.name = name;
		this.description = desc;
	}

	public Source getSource() {
		return source;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public boolean equals(Object other) {
		try {
			DescRec o = (DescRec) other;
			return o.source == this.source && o.id.equals(this.id);
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return source.oneLetter() + "" + id;
	}

	/** Compare by name. Used for sorting in GUI */
	@Override
	public int compareTo(DescRec o) {
		return this.name.compareTo(o.getName());
	}
}
