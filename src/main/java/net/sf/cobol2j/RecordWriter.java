package net.sf.cobol2j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;


public class RecordWriter {
    OutputStream oS;
    String charset;
    Map<?, ?> recdefs;

    public RecordWriter(OutputStream os, FileFormat ff)
        throws UnsupportedEncodingException {
        oS = os;
        charset = ff.getConversionTable();
        recdefs = new RecordsMap(ff);
    }

    public void writeRecord(List<?> fields) throws Exception {
        RecordFormat rF = (RecordFormat) recdefs.get("0");

        if (recdefs.size() > 1) {
            String first1 = fields.get(0).toString();
            rF = (RecordFormat) recdefs.get(first1);
        }

        Iterator<?> data = fields.listIterator();
        createFields(data, rF);
    }

    private void createFields(Iterator<?> data, FieldsList deflist)
        throws Exception {
        HashMap<String, BigDecimal> potentialDepOn = new HashMap<String, BigDecimal>();
        Iterator<?> def = deflist.getFieldFormatOrFieldsGroup().listIterator();

        while (def.hasNext()) {
            Object u = def.next();

            if (u instanceof FieldFormat) {
                int occurs = 1;
                FieldFormat ff = (FieldFormat) u;
                String dependingon = ff.getDependingOn();

                if (dependingon.length() > 0) {
                    BigDecimal bd = potentialDepOn.get(dependingon);
                    occurs = bd.intValue();
                } else {
                    occurs = ff.getOccurs().intValue();
                }

                while (occurs-- > 0) {
                    Object o = data.next();
                    writeField(o, ff, potentialDepOn);
                }
            } else if (u instanceof FieldsGroup) {
                int occurs = 1;
                FieldsGroup fG = (FieldsGroup) u;
                String dependingon = fG.getDependingOn();

                if (dependingon.length() > 0) {
                    BigDecimal bd = potentialDepOn.get(dependingon);
                    occurs = bd.intValue();
                } else {
                    occurs = fG.getOccurs().intValue();
                }

                while (occurs-- > 0) {
                    createFields(data, fG);
                }
            }
        }
    }

	private void writeField(Object o, FieldFormat format, HashMap<String, BigDecimal> potDepOn)
        throws IOException, Exception {
        char fType = format.getType().charAt(0);

        switch (fType) {
        case 'X':

            if (o instanceof String) {
                String value = (String) o;

                if (value.length() != format.getSize().intValue()) {
                    throw new Exception(
                        "Value size must be equal to field length.");
                }

                oS.write(value.getBytes(charset));
            } else {
                throw new Exception("ValueTypeMismatch");
            }

            break;

        case '9':

            if (o instanceof BigDecimal) {
                BigDecimal value = (BigDecimal) o;

                if (format.getDecimal().intValue() != value.scale()) {
                    BigDecimal tmp = value.setScale(format.getDecimal()
                                                          .intValue(),
                            BigDecimal.ROUND_FLOOR);
                    value = tmp;
                }

                String str = value.toPlainString();
                StringBuffer buf = new StringBuffer(str);

                // Modify byte with sign ...
                if (format.isSigned()) {
                    int lastidx = buf.length() - 1;
                    char lastbyte = buf.charAt(lastidx);
                    char repl;

                    if (value.signum() > 0) {
                        repl = "{ABCDEFGHI".charAt(Integer.parseInt(
                                    String.valueOf(lastbyte)));
                    } else {
                        repl = "}JKLMNOPQR".charAt(Integer.parseInt(
                                    String.valueOf(lastbyte)));
                    }

                    buf.setCharAt(lastidx, repl);

                    // Delete sign char
                    String tmp = buf.toString();
                    String tmp2 = tmp.replace("-", "");
                    buf = new StringBuffer(tmp2);
                }

                // Delete decimal point...
                if (format.isImpliedDecimal()) {
                    String tmp = buf.toString();
                    String tmp2 = tmp.replace(".", "");
                    buf = new StringBuffer(tmp2);
                }

                // Adding some leading zeros ...
                while (format.getSize().intValue() > buf.length()) {
                    buf.insert(0, "0");
                }

                oS.write(buf.toString().getBytes(charset));
                potDepOn.put(format.getName(), value);
            } else {
                throw new Exception("ValueTypeMismatch");
            }

            break;

        case '3':

            if (o instanceof BigDecimal) {
                BigDecimal value = (BigDecimal) o;

                if (format.getDecimal().intValue() != value.scale()) {
                    BigDecimal tmp = value.setScale(format.getDecimal()
                                                          .intValue(),
                            BigDecimal.ROUND_FLOOR);
                    value = tmp;
                }

                String str = value.toPlainString();
                int len = str.length();
                int fieldSize = RecordSet.getByteSize(format);
                byte[] buf = new byte[fieldSize];
                MutableInt k = new MutableInt(len);
                byte even; // left and right nibble ( we go from right to left )
                byte odd;

                for (int i = fieldSize - 1; i >= 0; i--) {
                    // Last byte needs sign nibble
                    if (i == (fieldSize - 1)) {
                        even = getNextByte(str, k);

                        if (format.isSigned()) {
                            if (value.signum() >= 0) {
                                odd = 0x0C;
                            } else {
                                odd = 0x0D;
                            }
                        } else {
                            odd = (byte) 0x0F;
                        }
                    } else {
                        // Packing rest of the digits...
                        // Get even digit if exist or zero
                        odd = getNextByte(str, k);
                        even = getNextByte(str, k);
                    }

                    buf[i] = (byte) ((even << 4) | odd);
                }

                // TODO: Check if str ">" buf and eventually throw an Exc.
                oS.write(buf);
            } else {
                throw new Exception("ValueTypeMismatch");
            }

            break;

        case 'B':

            if (o instanceof BigDecimal) {
                BigDecimal value = (BigDecimal) o;

                if (format.getDecimal().intValue() != value.scale()) {
                    BigDecimal tmp = value.setScale(format.getDecimal()
                                                          .intValue(),
                            BigDecimal.ROUND_FLOOR);
                    value = tmp;
                }

                BigInteger unscaled = value.unscaledValue();

                // TODO: Requires trimming to environment dependent bytesize
                oS.write(unscaled.toByteArray());
            } else {
                throw new Exception("ValueTypeMismatch");
            }

            break;

        case 'T':

            if (o instanceof byte[]) {
                byte[] value = (byte[]) o;

                if (value.length == format.getSize().intValue()) {
                    oS.write(value);
                } else {
                    throw new Exception(
                        "Value size must be equal to field length.");
                }
            } else {
                throw new Exception("ValueTypeMismatch");
            }

            break;

        default:
            throw new FileFormatException("Field type not supported: " + fType);
        }
    }

    private byte getNextByte(String number, MutableInt recentlyReturned) {
        MutableInt zero = new MutableInt(0);
        recentlyReturned.decrement();

        if (recentlyReturned.compareTo(zero) >= 0) {
            while (!"0123456789".contains(String.valueOf(number.charAt(
                                recentlyReturned.intValue())))) {
                recentlyReturned.decrement();

                if (recentlyReturned.compareTo(zero) < 0) {
                    return 0;
                }
            }

            return (byte) (Character.getNumericValue(number.charAt(
                    recentlyReturned.intValue())));
        } else {
            return 0;
        }
    }
}
