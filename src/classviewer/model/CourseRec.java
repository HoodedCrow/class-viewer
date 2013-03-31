package classviewer.model;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Course record, includes offerings
 * 
 * @author TK
 */
public class CourseRec implements Named, Linked {

	private int id;
	private String shortName;
	private String name;
	private String description;
	private String instructor;
	private String link;
	private Status status = Status.UNKNOWN;
	private ArrayList<DescRec> categories = new ArrayList<DescRec>();
	private ArrayList<DescRec> universities = new ArrayList<DescRec>();
	private ArrayList<OffRec> offerings = new ArrayList<OffRec>();

	public CourseRec(int id, String shortName, String name, String description,
			String instructor, String link) {
		this.id = id;
		this.shortName = shortName;
		this.name = name == null ? "" : name;
		this.description = description == null ? "" : description;
		this.instructor = instructor == null ? "" : instructor;
		this.link = link == null ? "" : link;
	}

	@Override
	public boolean equals(Object other) {
		try {
			return ((CourseRec) other).id == this.id;
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return id + " " + status + " " + shortName;
	}

	public void addCategory(DescRec descRec) {
		categories.add(descRec);
	}

	public void addUniversity(DescRec descRec) {
		universities.add(descRec);
	}

	public void addOffering(OffRec r) {
		offerings.add(r);
		r.setCourse(this);
	}

	public int getId() {
		return id;
	}

	public Status getStatus() {
		return status;
	}

	/**
	 * If a course set to no, all offerings set to no, unless already done or
	 * registered.
	 */
	public void setStatus(Status status) {
		if (this.status == status)
			return; // to avoid deep checks
		this.status = status;

		// If course status flipped to REGISTERED or DONE, all UNKNOWN or MAYBE
		// offerings go to NO. No need to propagate from the offering back, so
		// use setStatusDirect.
		if (status == Status.REGISTERED || status == Status.DONE)
			for (OffRec o : offerings)
				if (o.getStatus() == Status.UNKNOWN
						|| o.getStatus() == Status.MAYBE)
					o.setStatusDirect(Status.NO);
		// If course is a no, anything other than registered or done should be a
		// NO. In fact, if there is a DONE/REGISTERED offering, we should
		// probably not flip the course to NO
		if (status == Status.NO)
			for (OffRec o : offerings)
				if (o.getStatus() != Status.REGISTERED
						&& o.getStatus() != Status.DONE)
					o.setStatusDirect(Status.NO);
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInstructor() {
		return instructor;
	}

	public void setInstructor(String instructor) {
		this.instructor = instructor;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public ArrayList<DescRec> getCategories() {
		return categories;
	}

	public ArrayList<DescRec> getUniversities() {
		return universities;
	}

	public ArrayList<OffRec> getOfferings() {
		return offerings;
	}

	public String getLongHtml() {
		String str = "<html><b>" + name + "</b> (" + id + ", "
				+ status.getName() + ")<br/>\n";
		str += "<b>Instructor(s):</b> " + instructor + "<br/>";
		str += "<b>Categories:</b> " + recNames(categories) + "<br/>";
		str += "<b>University:</b> " + recNames(universities) + "<br/>";
		str += description + "<br/>\n";
		str += "<a href=\"" + link + "\">" + link + "</a><br/>\n";
		return str;
	}

	private String recNames(ArrayList<DescRec> recs) {
		if (recs.isEmpty())
			return "";
		String res = recs.get(0).getName();
		for (int i = 1; i < recs.size(); i++)
			res = res + ", " + recs.get(i).getName();
		return res;
	}

	public OffRec getOffering(int id) {
		for (OffRec r : offerings)
			if (r.getId() == id)
				return r;
		return null;
	}

	public void setStatusDirect(Status stat) {
		this.status = stat;
	}

	/*
	 * public void diff(CourseRec other, ArrayList<Change> changes) { assert
	 * (this.id == other.id); if (!this.name.equals(other.name))
	 * changes.add(Change.changed("Name changed", Change.Type.NAME, this,
	 * other.name)); if (!this.description.equals(other.description))
	 * changes.add(Change.changed("Description changed",
	 * Change.Type.DESCRIPTION, this, other.description)); if
	 * (!this.shortName.equals(other.shortName))
	 * changes.add(Change.changed("Short name changed", Change.Type.SHORT_NAME,
	 * this, other.shortName)); if (!this.instructor.equals(other.instructor))
	 * changes.add(Change.changed("Instructor changed", Change.Type.INSTRUCTOR,
	 * this, other.instructor)); if (!this.link.equals(other.link))
	 * changes.add(Change.changed("Link changed", Change.Type.LINK, this,
	 * other.link));
	 * 
	 * // Categories HashSet<String> oldIds = getIdSet(this.categories);
	 * HashSet<String> newIds = getIdSet(other.categories); if
	 * (!oldIds.equals(newIds)) {
	 * changes.add(Change.changed("Categories for class " + name,
	 * Change.Type.CATEGORIES, this, newIds)); }
	 * 
	 * // Universities oldIds = getIdSet(this.universities); newIds =
	 * getIdSet(other.universities); if (!oldIds.equals(newIds)) {
	 * changes.add(Change.changed("Universities for class " + name,
	 * Change.Type.UNIVERSITIES, this, newIds)); }
	 * 
	 * // Offerings HashSet<Object> ioldIds = new HashSet<Object>(); for (OffRec
	 * r : this.offerings) { ioldIds.add(r.getId()); } HashSet<Object> inewIds =
	 * new HashSet<Object>(); for (OffRec r : other.offerings) {
	 * inewIds.add(r.getId()); } HashSet<Object> ids = new
	 * HashSet<Object>(ioldIds); ids.removeAll(inewIds); for (Object o : ids) {
	 * changes.add(Change.remove("Offering removed", this.getOffering((Integer)
	 * o))); } ids = new HashSet<Object>(inewIds); ids.removeAll(ioldIds); for
	 * (Object o : ids) { changes.add(Change.add("Offering added",
	 * other.getOffering((Integer) o))); } ids = new HashSet<Object>(inewIds);
	 * ids.retainAll(ioldIds); for (Object o : ids) { this.getOffering((Integer)
	 * o).diff(other.getOffering((Integer) o), changes); } }
	 */
	public static HashSet<String> getIdSet(ArrayList<DescRec> list) {
		HashSet<String> set = new HashSet<String>();
		for (DescRec r : list) {
			set.add(r.getId());
		}
		return set;
	}

	public boolean hasUnknown() {
		for (OffRec r : offerings)
			if (r.getStatus() == Status.UNKNOWN)
				return true;
		return false;
	}

	public boolean hasDateless() {
		for (OffRec r : offerings)
			if (r.getStart() == null)
				return true;
		return false;
	}
}
