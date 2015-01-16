package net.sf.cobol2j.examples;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.sf.cobol2j.FieldFormat;
import net.sf.cobol2j.FieldsGroup;
import net.sf.cobol2j.FieldsList;
import net.sf.cobol2j.FileFormat;
import net.sf.cobol2j.FileFormatException;
import net.sf.cobol2j.RecordParseException;
import net.sf.cobol2j.RecordSet;
import net.sf.cobol2j.RecordsMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Dat2csv {
	private static Log log = LogFactory.getLog(Dat2csv.class);

	public static void convert(InputStream xc2j, InputStream dat, PrintStream ps)
			throws FileFormatException, JAXBException, FileNotFoundException,
			IOException, RecordParseException {
		JAXBContext context = JAXBContext.newInstance("net.sf.cobol2j");
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object o = unmarshaller.unmarshal(xc2j);
		FileFormat fF = (FileFormat) o;
		RecordSet rset = new RecordSet(dat, fF);

		Map<?, ?> recdefs = new RecordsMap(fF);

		List<FieldFormat> headers = new LinkedList<FieldFormat>();
		for (Object entry : recdefs.values()) {
			FieldsList rf = (FieldsList) entry;
			header(headers, rf);
		}

		boolean afterfirst = false;
		if (headers != null && headers.size() > 0) {
			for (FieldFormat column : headers) {
				String columnName = column.getName();
				if (afterfirst) {
					ps.print(',');
				}
				ps.print("\"" + columnName + "\"");
				afterfirst = true;
			}
			ps.println();
		}

		Iterator<?> fields;

		while (rset.hasNext()) {
			try {
				fields = rset.next().iterator();

				afterfirst = false;

				while (fields.hasNext()) {
					if (afterfirst) {
						ps.print(',');
					}

					// ps.print(fields.next().toString());
					ps.print("\"" + fields.next().toString().replace("\n", "")
							+ "\"");
					afterfirst = true;
				}
			} catch (RecordParseException ex) {
				displayErrorRecord(ex, headers);

				break;
			}

			ps.println();
		}
	}

	private static void header(List<FieldFormat> values, FieldsList fields) {
		for (Object ffOrFg : fields.getFieldFormatOrFieldsGroup()) {
			if (ffOrFg instanceof FieldsGroup) {
				FieldsGroup fg = (FieldsGroup) ffOrFg;
				for (int i = 0; i < fg.getOccurs().intValue(); i++) {
					header(values, fg);
				}
			} else if (ffOrFg instanceof FieldFormat) {
				FieldFormat ff = (FieldFormat) ffOrFg;
				for (int i = 0; i < ff.getOccurs().intValue(); i++) {
					values.add(ff);
				}
			}
		}
	}

	// private static void createFields(Iterator data, FieldsList recformat,
	// FieldFormat parentelement) {
	// Iterator def = recformat.getFieldFormatOrFieldsGroup().listIterator();
	// while (def.hasNext()) {
	// Object u = def.next();
	// if ((u instanceof FieldFormat)) {
	// int occurs = 1;
	// FieldFormat fF = (FieldFormat) u;
	// String dependingon = fF.getDependingOn();
	// if (dependingon.length() > 0) {
	// Node previous = parentelement.getLastChild()
	// .getPreviousSibling();
	// while (previous != null) {
	// if (previous.getNodeName().equals(dependingon)) {
	// @SuppressWarnings("unused")
	// Element e = (Element) previous;
	// occurs = Integer
	// .parseInt(previous.getTextContent());
	//
	// break;
	// }
	// previous = previous.getPreviousSibling();
	// }
	// } else {
	// occurs = fF.getOccurs().intValue();
	// }
	// while (occurs-- > 0) {
	// Object o = data.next();
	// Element fieldelement = parentelement.getOwnerDocument()
	// .createElement(fF.getName());
	//
	// fieldelement.setTextContent(o.toString());
	// parentelement.appendChild(fieldelement);
	// }
	// } else if ((u instanceof FieldsGroup)) {
	// int occurs = 1;
	// FieldsGroup fG = (FieldsGroup) u;
	// String dependingon = fG.getDependingOn();
	// if (dependingon.length() > 0) {
	// Node previous = parentelement.getLastChild()
	// .getPreviousSibling();
	// while (previous != null) {
	// if (previous.getNodeName().equals(dependingon)) {
	// @SuppressWarnings("unused")
	// Element e = (Element) previous;
	// occurs = Integer
	// .parseInt(previous.getTextContent());
	//
	// break;
	// }
	// previous = previous.getPreviousSibling();
	// }
	// } else {
	// occurs = fG.getOccurs().intValue();
	// }
	// while (occurs-- > 0) {
	// Element groupelement = parentelement.getOwnerDocument()
	// .createElement(fG.getName());
	//
	// createFields(data, fG, groupelement);
	// parentelement.appendChild(groupelement);
	// }
	// }
	// }
	// }

	public static void main(String[] args) throws FileFormatException,
			JAXBException, FileNotFoundException, IOException,
			RecordParseException {
		String xc2jfilename = args[0];
		JAXBContext context = JAXBContext.newInstance("net.sf.cobol2j");
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Object o = unmarshaller.unmarshal(new FileInputStream(xc2jfilename));
		FileFormat fF = (FileFormat) o;
		RecordSet rset = new RecordSet(System.in, fF);

		Iterator<?> fields;

		while (rset.hasNext()) {
			try {
				fields = rset.next().iterator();

				boolean afterfirst = false;

				while (fields.hasNext()) {
					if (afterfirst) {
						System.out.print(',');
					}

					System.out.print(fields.next().toString());
					afterfirst = true;
				}
			} catch (RecordParseException ex) {
				displayErrorRecord(ex, Collections.<FieldFormat>emptyList());

				break;
			}

			System.out.println();
		}
	}

	private static void displayErrorRecord(RecordParseException e, List<FieldFormat> headers) {
		StringBuffer b = new StringBuffer();
		b.append(e.getMessage() + "\n");
		b.append("Partial record. Fields values until failed field:" + "\n");

		boolean afterfirst = false;
		if (headers != null && headers.size() > 0) {
			for (FieldFormat column : headers) {
				if (afterfirst) {
					b.append('|');
				}
				b.append(String.format("%s", column.getName()));
				afterfirst = true;
			}
			b.append("\n");
		}
		
		for (Object o : e.getPartialRecord()) {
			b.append(o.toString() + "|");
		}

		log.error(b);
	}
}
