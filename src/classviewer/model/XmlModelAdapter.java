package classviewer.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	}

	private CourseRec parseCourse(Node item, CourseModel model)
			throws IOException {
		if (!"Class".equals(item.getNodeName()))
			return null;
		NamedNodeMap attrs = item.getAttributes();
		String id;
		String shortName, categories, universities, language;
		try {
			id = attrs.getNamedItem("id").getNodeValue();
		} catch (Exception e) {
			throw new IOException("Cannot have a course without id: " + e);
		}
		shortName = attrOrNull(attrs, "short");
		categories = attrOrNull(attrs, "categories");
		universities = attrOrNull(attrs, "universities");
		language = attrOrNull(attrs, "lang");
		Source source = Source.fromStringOrNull(attrOrNull(attrs, "src"));
		String name = valueOrNull(item, "Name");
		String desc = valueOrNull(item, "Long");
		String prof = valueOrNull(item, "Prof");
		String link = valueOrNull(item, "Link");
		boolean selfStudy = "1".equals(attrOrNull(attrs, "ondem"));

		CourseRec res = new CourseRec(source, String.valueOf(id), shortName,
				name, desc, prof, link, language, selfStudy);

		// Dereference categories and universities
		if (categories != null && !categories.isEmpty()) {
			String[] parts = categories.split(" ");
			for (String s : parts) {
				DescRec cat = model.getCategory(source, s);
				if (cat == null)
					System.err.println("Cannot find category " + s + ", specified in " + universities);
				else
					res.addCategory(cat);
			}
		}
		if (universities != null && !universities.isEmpty()) {
			String[] parts = universities.split(" ");
			for (String s : parts) {
				DescRec uni = model.getUniversity(source, s);
				if (uni == null)
					System.err.println("Cannot find university " + s + ", specified in " + universities);
				else
					res.addUniversity(uni);
			}
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
			// Clean up. TODO Remove eventually.
			if (id < 0) id = -id;
		} catch (Exception e) {
			throw new IOException("Cannot have an offering without id");
		}
		Date start = dateOrNull(item, "Start");
		duration = attrOrInt(attrs, "dur");
		spread = attrOrInt(attrs, "spread");
		active = attrOrInt(attrs, "active");
		String startStr = valueOrNull(item, "StartS");
		String link = valueOrNull(item, "Home");

		return new OffRec(id, start, duration, spread, link, active > 0,
				startStr);
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
		String src = attrOrNull(item.getAttributes(), "src");
		// TODO Temporary clean up.
		if ("e".equals(src) && id.startsWith("X"))
			id = id.substring(1); 
		return new DescRec(Source.fromStringOrNull(src), id, name, desc);
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
			ArrayList<DescRec> list = new ArrayList<DescRec>();
			for (Source source : Source.values())
				list.addAll(model.getCategories(source));
			Collections.sort(list);
			for (DescRec rec : list) {
				group.appendChild(makeDescNode(rec, dom));
			}

			// Universities
			list = new ArrayList<DescRec>();
			for (Source source : Source.values())
				list.addAll(model.getUniversities(source));
			Collections.sort(list);
			group = dom.createElement("Universities");
			root.appendChild(group);
			for (DescRec rec : list) {
				group.appendChild(makeDescNode(rec, dom));
			}

			// Courses
			group = dom.createElement("Classes");
			root.appendChild(group);
			ArrayList<CourseRec> list2 = new ArrayList<CourseRec>();
			for (Source source : Source.values())
				list2.addAll(model.getCourses(source));
			Collections.sort(list2, new Comparator<CourseRec>() {
				@Override
				public int compare(CourseRec o1, CourseRec o2) {
					return o1.getId().compareTo(o2.getId());
				}
			});
			for (CourseRec rec : list2) {
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
		node.setAttribute("src", ""+rec.getSource().oneLetter());
		addChildIfNotNull(rec.getName(), "Name", node, dom);
		addChildIfNotNull(rec.getDescription(), "Long", node, dom);
		return node;
	}

	private Node makeCourseNode(CourseRec rec, Document dom, CourseModel model) {
		Element node = dom.createElement("Class");
		node.setAttribute("id", String.valueOf(rec.getId()));
		node.setAttribute("short", rec.getShortName());
		node.setAttribute("lang", rec.getLanguage());
		node.setAttribute("src", "" + rec.getSource().oneLetter());
		if (rec.isSelfStudy())
			node.setAttribute("ondem", "1");
		String str = "";
		for (DescRec dr : rec.getCategories())
			str = str + " " + dr.getId();
		if (!str.isEmpty())
			node.setAttribute("categories", str.substring(1));
		str = "";		
		for (DescRec dr : rec.getUniversities())
			try {
			str = str + " " + dr.getId();
			} catch (Exception e) {
				e.printStackTrace();				
			}
		if (!str.isEmpty())
			node.setAttribute("universities", str.substring(1));
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
