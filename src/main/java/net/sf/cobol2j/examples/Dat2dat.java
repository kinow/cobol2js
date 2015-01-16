package net.sf.cobol2j.examples;

import net.sf.cobol2j.FileFormat;
import net.sf.cobol2j.FileFormatException;
import net.sf.cobol2j.RecordParseException;
import net.sf.cobol2j.RecordSet;
import net.sf.cobol2j.RecordWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Dat2dat {
    private static Log log = LogFactory.getLog(Dat2dat.class);

    public static void main(String[] args)
        throws FileFormatException, JAXBException, FileNotFoundException,
            IOException, RecordParseException {
        String xc2jsrc = args[0];
				String xc2jtarget = args[1];
        JAXBContext context = JAXBContext.newInstance("net.sf.cobol2j");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object in = unmarshaller.unmarshal(new FileInputStream(xc2jsrc));
				Object out = unmarshaller.unmarshal(new FileInputStream(xc2jtarget));
        FileFormat fFin = (FileFormat) in;
				FileFormat fFout = (FileFormat) out;
        RecordSet rset = new RecordSet(System.in, fFin);
				
				try{
					RecordWriter rw = new RecordWriter( System.out, fFout );
					 while (rset.hasNext())
                rw.writeRecord(rset.next());
				} catch (FileFormatException ex) {
					ex.printStackTrace();
        } catch (Exception ex) {
					ex.printStackTrace();
        }
    }
}
