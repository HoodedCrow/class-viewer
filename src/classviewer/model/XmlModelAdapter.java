package classviewer.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML read/write for a model. Moving into a separate class in case the format
 * changes later.
 * 
 * @author TK
 */
public class XmlModelAdapter {
	protected static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	private static SimpleDateFormat dformat = new SimpleDateFormat("MM/dd/yy");

	protected boolean addIndent = true;

	/** Load the model. Assume the model is initially empty */
	public void readModel(InputStream input, CourseModel model)
			throws IOException {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		Document doc;
		try {
			doc = dBuilder.parse(input);
			doc.getDocumentElement().normalize();
		} catch (SAXException e) {
			throw new IOException(e);
		}

		NodeList list = doc.getElementsByTagName("Categories");
		if (list != null) {
			if (list.getLength() != 1)
				System.err.println("Expecting one Categories node, got "
						+ list.getLength());
			Node node = list.item(0).getFirstChild();
			while (node != null) {
				if ("Desc".equals(node.getNodeName()))
					model.addCategory(parseDesc(node));
				node = node.getNextSibling();
			}
		}

		list = doc.getElementsByTagName("Universities");
		if (list != null) {
			if (list.getLength() != 1)
				System.err.println("Expecting one Universities node, got "
						+ list.getLength());
			Node node = list.item(0).getFirstChild();
			while (node != null) {
				if ("Desc".equals(node.getNodeName()))
					model.addUniversity(parseDesc(node));
				node = node.getNextSibling();
			}
		}

		// TODO Languages

		list = doc.getElementsByTagName("Classes");
		if (list != null) {
			if (list.getLength() != 1)
				System.err.println("Expecting one Classes node, got "
						+ list.getLength());
			Node node = list.item(0).getFirstChild();
			while (node != null) {
				if ("Class".equals(node.getNodeName()))
					model.addCourse(parseCourse(node, model));
				node = node.getNextSibling();
			}
		}
		
		model.fireModelReloaded();
	}

	private CourseRec parseCourse(Node item, CourseModel model)
			throws IOException {
		if (!"Class".equals(item.getNodeName()))
			return null;
		NamedNodeMap attrs = item.getAttributes();
		Integer id;
		String shortName, categories, universities; // TODO , language;
		try {
			id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		} catch (Exception e) {
			throw new IOException("Cannot have a course without id");
		}
		shortName = attrOrNull(attrs, "short");
		categories = attrOrNull(attrs, "categories");
		universities = attrOrNull(attrs, "universities");
		// TODO language = attrOrNull(attrs, "lang");
		String name = valueOrNull(item, "Name");
		String desc = valueOrNull(item, "Long");
		String prof = valueOrNull(item, "Prof");
		String link = valueOrNull(item, "Link");

		// Pass status -1 until overridden later from another file
		CourseRec res = new CourseRec(id, shortName, name, desc, prof, link, -1);

		// Dereference categories and universities
		if (categories != null && !categories.isEmpty()) {
			String[] parts = categories.split(" ");
			for (String s : parts)
				res.addCategory(model.getCategory(s));
		}
		if (universities != null && !universities.isEmpty()) {
			String[] parts = universities.split(" ");
			for (String s : parts)
				res.addUniversity(model.getUniversity(s));
		}

		// Find node for offerings
		Node node = item.getFirstChild();
		while (node != null && !node.getNodeName().equals("Offerings"))
			node = node.getNextSibling();

		if (node != null) {
			node = node.getFirstChild();
			while (node != null) {
				if ("Off".equals(node.getNodeName()))
					res.addOffering(parseOffering(node));
				node = node.getNextSibling();
			}
		}

		return res;
	}

	private OffRec parseOffering(Node item) throws IOException {
		if (!"Off".equals(item.getNodeName()))
			return null;
		NamedNodeMap attrs = item.getAttributes();
		int id, duration, spread, active;
		try {
			id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		} catch (Exception e) {
			throw new IOException("Cannot have an offering without id");
		}
		Date start = dateOrNull(item, "Start");
		duration = attrOrInt(attrs, "dur");
		spread = attrOrInt(attrs, "spread");
		active = attrOrInt(attrs, "active");
		String startStr = valueOrNull(item, "StartS");
		String durStr = valueOrNull(item, "DurS");
		String link = valueOrNull(item, "Home");

		// status -1 until overridden from another file
		return new OffRec(id, start, duration, spread, -1, link, active > 0,
				startStr, durStr);
	}

	private DescRec parseDesc(Node item) throws IOException {
		if (!"Desc".equals(item.getNodeName()))
			return null;
		String id;
		try {
			id = item.getAttributes().getNamedItem("id").getNodeValue();
		} catch (Exception e) {
			throw new IOException("Cannot have a description without id");
		}
		String name = valueOrNull(item, "Name");
		String desc = valueOrNull(item, "Long");
		return new DescRec(id, name, desc);
	}

	private String valueOrNull(Node item, String tag) {
		Node node = item.getFirstChild();
		while (node != null) {
			if (tag.equals(node.getNodeName())) {
				String txt = node.getTextContent();
				if (txt == null || txt.isEmpty())
					return null;
				return txt;
			}
			node = node.getNextSibling();
		}
		return null;
	}

	private String attrOrNull(NamedNodeMap attrs, String name) {
		Node node = attrs.getNamedItem(name);
		if (node != null)
			return node.getNodeValue();
		return null;
	}

	private int attrOrInt(NamedNodeMap attrs, String name) {
		Node node = attrs.getNamedItem(name);
		if (node != null)
			try {
				return Integer.parseInt(node.getNodeValue());
			} catch (Exception e) {
				System.err.println("Cannot parse integer "
						+ node.getNodeValue());
			}
		return 0;
	}

	private Date dateOrNull(Node item, String tag) throws IOException {
		String val = valueOrNull(item, tag);
		if (val != null)
			try {
				return dformat.parse(val);
			} catch (ParseException e) {
				throw new IOException("Bad date format " + e);
			}
		return null;
	}

	/** Save the model as a single XML file. Status info is not included. */
	public void writeModel(OutputStream output, CourseModel model)
			throws IOException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();

			Element root = dom.createElement("StaticData");
			dom.appendChild(root);

			// Categories
			Element group = dom.createElement("Categories");
			root.appendChild(group);
			for (DescRec rec : model.getCategories()) {
				group.appendChild(makeDescNode(rec, dom));
			}

			// Universities
			group = dom.createElement("Universities");
			root.appendChild(group);
			for (DescRec rec : model.getUniversities()) {
				group.appendChild(makeDescNode(rec, dom));
			}

			// Courses
			group = dom.createElement("Classes");
			root.appendChild(group);
			for (CourseRec rec : model.getCourses()) {
				group.appendChild(makeCourseNode(rec, dom, model));
			}

			try {
				Transformer tr = TransformerFactory.newInstance()
						.newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(
						"{http://xml.apache.org/xslt}indent-amount", "2");

				tr.transform(new DOMSource(dom), new StreamResult(output));

			} catch (TransformerException te) {
				throw new IOException(te);
			}
		} catch (ParserConfigurationException pce) {
			System.out
					.println("UsersXML: Error trying to instantiate DocumentBuilder "
							+ pce);
		}
	}

	private Node makeDescNode(DescRec rec, Document dom) {
		Element node = dom.createElement("Desc");
		node.setAttribute("id", rec.getId());
		addChildIfNotNull(rec.getName(), "Name", node, dom);
		addChildIfNotNull(rec.getDescription(), "Long", node, dom);
		return node;
	}

	private Node makeCourseNode(CourseRec rec, Document dom, CourseModel model) {
		Element node = dom.createElement("Class");
		node.setAttribute("id", String.valueOf(rec.getId()));
		node.setAttribute("short", rec.getShortName());
		String str = "";
		for (DescRec dr : rec.getCategories())
			str = str + " " + dr.getId();
		if (!str.isEmpty())
			node.setAttribute("categories", str.substring(1));
		str = "";
		for (DescRec dr : rec.getUniversities())
			str = str + " " + dr.getId();
		if (!str.isEmpty())
			node.setAttribute("universities", str.substring(1));
		// TODO language
		addChildIfNotNull(rec.getName(), "Name", node, dom);
		addChildIfNotNull(rec.getDescription(), "Long", node, dom);
		addChildIfNotNull(rec.getInstructor(), "Prof", node, dom);
		addChildIfNotNull(rec.getLink(), "Link", node, dom);

		Element group = dom.createElement("Offerings");
		node.appendChild(group);

		for (OffRec off : rec.getOfferings())
			group.appendChild(makeOfferingNode(off, dom));
		return node;
	}

	private Node makeOfferingNode(OffRec rec, Document dom) {
		Element node = dom.createElement("Off");
		node.setAttribute("id", String.valueOf(rec.getId()));
		node.setAttribute("dur", String.valueOf(rec.getDuration()));
		node.setAttribute("spread", String.valueOf(rec.getSpread()));
		node.setAttribute("active", String.valueOf(rec.isActive() ? 1 : 0));
		addChildIfNotNull(rec.getStartStr(), "StartS", node, dom);
		addChildIfNotNull(rec.getDurStr(), "DurS", node, dom);
		addChildIfNotNull(rec.getLink(), "Home", node, dom);
		if (rec.getStart() != null)
			addChildIfNotNull(dformat.format(rec.getStart()), "Start", node,
					dom);
		return node;
	}

	private void addChildIfNotNull(String value, String tag, Element target,
			Document dom) {
		if (value != null && !value.isEmpty()) {
			Element e = dom.createElement(tag);
			e.appendChild(dom.createTextNode(value));
			target.appendChild(e);
		}
	}
}
