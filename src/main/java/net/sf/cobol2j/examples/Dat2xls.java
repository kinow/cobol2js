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

import org.apache.commons.lang.mutable.MutableInt;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Dat2xls {
    public static void main(String[] args)
        throws FileNotFoundException, IOException, FileFormatException,
            JAXBException, RecordParseException {
        String xc2jfilename = args[0];
        JAXBContext context = JAXBContext.newInstance("net.sf.cobol2j");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object o = unmarshaller.unmarshal(new FileInputStream(xc2jfilename));
        FileFormat fF = (FileFormat) o;
        RecordSet rset = new RecordSet(System.in, fF);
        int rowNr = 0;
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("COBOL2J - dat2xls");
        HSSFRow titlerow;
        HSSFRow datarow;
        Map recdefs = new RecordsMap(fF);

        while (rset.hasNext()) {
            List fields = rset.next();
            RecordFormat rF = (RecordFormat) recdefs.get("0");

            if (recdefs.size() > 1) {
                String first1 = fields.get(0).toString();
                rF = (RecordFormat) recdefs.get(first1);
            }

            MutableInt cellNr = new MutableInt(0);
            titlerow = sheet.createRow((short) rowNr);
            datarow = sheet.createRow((short) rowNr + 1);

            Iterator data = fields.listIterator();
            createFields(data, rF, titlerow, datarow, cellNr);
            rowNr = rowNr + 2;
        }

        wb.write(System.out);
    }

    private static void createFields(Iterator data, FieldsList deflist,
        HSSFRow titlerow, HSSFRow datarow, MutableInt cellNr) {
        Iterator def = deflist.getFieldFormatOrFieldsGroup().listIterator();

        while (def.hasNext()) {
            Object u = def.next();

            if (u instanceof FieldFormat) {
                int occurs = 1;
                FieldFormat ff = (FieldFormat) u;
                String dependingon = ff.getDependingOn();

                if (dependingon.length() > 0) {
                    int c = cellNr.intValue() - 1;

                    while (c >= 0) {
                        if (titlerow.getCell((short) c).getStringCellValue().equals(dependingon)) {
                            occurs = (int)datarow.getCell((short) c).getNumericCellValue();
                            break;
                        }

                        c--;
                    }
                } else {
                    occurs = ff.getOccurs().intValue();
                }

                while (occurs-- > 0) {
                    titlerow.createCell(cellNr.shortValue())
                            .setCellValue(ff.getName());

                    Object o = data.next();

                    if (o instanceof BigDecimal) {
                        datarow.createCell(cellNr.shortValue())
                               .setCellValue(((BigDecimal) o).doubleValue());
                    } else if (o instanceof BigInteger) {
                        datarow.createCell(cellNr.shortValue())
                               .setCellValue(((BigInteger) o).intValue());
                    } else if (o instanceof Float) {
                        datarow.createCell(cellNr.shortValue())
                               .setCellValue(((Float) o).intValue());
                    } else if (o instanceof Double) {
                        datarow.createCell(cellNr.shortValue())
                               .setCellValue(((Double) o).intValue());
                    } else if (o instanceof String) {
                        datarow.createCell(cellNr.shortValue())
                               .setCellValue(o.toString());
                    }

                    cellNr.increment();
                }
            } else if (u instanceof FieldsGroup) {
                int occurs = 1;
                FieldsGroup fG = (FieldsGroup) u;
                String dependingon = fG.getDependingOn();

                if (dependingon.length() > 0) {
                    int c = cellNr.intValue() - 1;

                    while (c >= 0) {
                        if (titlerow.getCell((short) c).getStringCellValue().equals(dependingon)) {
                            occurs = (int)datarow.getCell((short) c).getNumericCellValue();
                            break;
                        }

                        c--;
                    }
                } else {
                    occurs = fG.getOccurs().intValue();
                }

                while (occurs-- > 0) {
                    createFields(data, fG, titlerow, datarow, cellNr);
                }
            }
        }
    }
}
