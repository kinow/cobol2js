package net.sf.cobol2j.examples;

import net.sf.cobol2j.FieldFormat;
import net.sf.cobol2j.FieldsGroup;
import net.sf.cobol2j.FieldsList;
import net.sf.cobol2j.FileFormat;
import net.sf.cobol2j.FileFormatException;
import net.sf.cobol2j.RecordFormat;
import net.sf.cobol2j.RecordParseException;
import net.sf.cobol2j.RecordSet;
import net.sf.cobol2j.RecordsMap;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Dat2xml {
    public static void main(String[] args)
        throws FileNotFoundException, IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, FileFormatException,
            JAXBException, RecordParseException {
        String xc2jfilename = args[0];
        JAXBContext context = JAXBContext.newInstance("net.sf.cobol2j");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object o = unmarshaller.unmarshal(new FileInputStream(xc2jfilename));
        FileFormat fF = (FileFormat) o;
        RecordSet rset = new RecordSet(System.in, fF);
        Document doc = DOMImplementationRegistry.newInstance()
                                                .getDOMImplementation("Core")
                                                .createDocument(null,
                "Dat2Xml", null);
        Element docelement = doc.getDocumentElement();
        Map recdefs = new RecordsMap(fF);

        while (rset.hasNext()) {
            List fields = rset.next();
            RecordFormat rF = (RecordFormat) recdefs.get("0");

            if (recdefs.size() > 1) {
                String first1 = fields.get(0).toString();
                rF = (RecordFormat) recdefs.get(first1);
            }

            Element recelement = doc.createElement(rF.getCobolRecordName());
            Iterator data = fields.listIterator();
            createFields(data, rF, recelement);
            docelement.appendChild(recelement);
        }

        // Still ( 2006-01-27 ) the prettiest ( Xerces ) serialization
        // Have to wait for inmplementation independence
        OutputFormat format = new OutputFormat(doc);
        format.setLineWidth(120);
        format.setIndenting(true);
        format.setIndent(2);

        XMLSerializer serializer = new XMLSerializer(System.out, format);
        serializer.serialize(doc);
    }

    private static void createFields(Iterator data, FieldsList recformat,
        Element parentelement) {
        Iterator def = recformat.getFieldFormatOrFieldsGroup().listIterator();

        while (def.hasNext()) {
            Object u = def.next();

            if (u instanceof FieldFormat) {
                int occurs = 1;
                FieldFormat fF = (FieldFormat) u;
                String dependingon = fF.getDependingOn();

                if (dependingon.length() > 0) {
                    Node previous = parentelement.getLastChild()
                                                 .getPreviousSibling();

                    while (previous != null) {
                        if (previous.getNodeName().equals(dependingon)) {
                            Element e = (Element) previous;
                            occurs = (Integer.parseInt(previous.getTextContent()));

                            break;
                        } else {
                            previous = previous.getPreviousSibling();
                        }
                    }
                } else {
                    occurs = fF.getOccurs().intValue();
                }

                while (occurs-- > 0) {
                    Object o = data.next();
                    Element fieldelement = parentelement.getOwnerDocument()
                                                        .createElement(fF.getName());
                    fieldelement.setTextContent(o.toString());
                    parentelement.appendChild(fieldelement);
                }
            } else if (u instanceof FieldsGroup) {
                int occurs = 1;
                FieldsGroup fG = (FieldsGroup) u;
                String dependingon = fG.getDependingOn();

                if (dependingon.length() > 0) {
                    Node previous = parentelement.getLastChild()
                                                 .getPreviousSibling();

                    while (previous != null) {
                        if (previous.getNodeName().equals(dependingon)) {
                            Element e = (Element) previous;
                            occurs = (Integer.parseInt(previous.getTextContent()));

                            break;
                        } else {
                            previous = previous.getPreviousSibling();
                        }
                    }
                } else {
                    occurs = fG.getOccurs().intValue();
                }

                while (occurs-- > 0) {
                    Element groupelement = parentelement.getOwnerDocument()
                                                        .createElement(fG.getName());
                    createFields(data, fG, groupelement);
                    parentelement.appendChild(groupelement);
                }
            }
        }
    }
}
