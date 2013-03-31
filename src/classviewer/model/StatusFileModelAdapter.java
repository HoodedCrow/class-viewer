package classviewer.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Helper class to read/write status information. Made into a separate class to
 * keep IO separate from the CourseModel
 * 
 * @author TK
 */
public class StatusFileModelAdapter {
	public void saveStatuses(Writer writer, Iterable<CourseRec> courses)
			throws IOException {
		for (CourseRec cr : courses) {
			writer.append("c " + cr.getId() + " " + cr.getStatus() + "\n");
			for (OffRec or : cr.getOfferings())
				writer.append("o " + or.getId() + " " + or.getStatus() + "\n");
		}
	}

	public void updateStatuses(Reader reader, CourseModel model)
			throws IOException {
		BufferedReader br = new BufferedReader(reader);
		CourseRec rec = null;
		while (br.ready()) {
			String[] str = br.readLine().split(" ");
			if (str.length != 3) {
				System.out.println("Expected exactly 3 items per line, got "
						+ str.length);
				continue;
			}
			int id = new Integer(str[1]);
			if ("c".equals(str[0])) {
				rec = model.getClassById(id);
				if (rec == null)
					System.err.println("Unknown class id in the status file: "
							+ id);
				else
					rec.setStatusDirect(Status.parse(str[2]));
			} else if ("o".equals(str[0])) {
				OffRec r = rec.getOffering(id);
				if (r == null)
					System.out
							.println("No offering " + id + " in class " + rec);
				else
					r.setStatusDirect(Status.parse(str[2]));
			} else {
				System.out.println("Unknown code " + str[0]);
			}
		}
	}
}
