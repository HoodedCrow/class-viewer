package classviewer.model;

import java.util.ArrayList;

/**
 * Common record for universities and categories.
 * 
 * @author TK
 */
public class DescRec implements Named, Comparable<DescRec> {
	String id, name, description;

	public DescRec(String id, String name, String desc) {
		this.id = id;
		this.name = name;
		this.description = desc;
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
			return ((DescRec) other).id.equals(this.id);
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return id;
	}
/*
	public void diff(DescRec other, ArrayList<Change> changes) {
		assert (this.id.equals(other.id));
		if (!this.name.equals(other.name))
			changes.add(Change.changed("Name changed", Change.Type.NAME, this,
					other.name));
		if (!this.description.equals(other.description))
			changes.add(Change.changed("Description changed",
					Change.Type.DESCRIPTION, this, other.description));
	}*/

	/** Compare by name. Used for sorting in GUI */
	@Override
	public int compareTo(DescRec o) {
		return this.name.compareTo(o.getName());
	}
}
