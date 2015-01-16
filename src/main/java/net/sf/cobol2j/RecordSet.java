package net.sf.cobol2j;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RecordSet {
	private static Log log = LogFactory.getLog(RecordSet.class);
	private InputStream stream;
	private long bytesProcessed = 0L;
	private String charset;
	private FileFormat fileFormat;
	private long recNr = 0L;

	public RecordSet(InputStream is, FileFormat ff) {
		stream = is;
		fileFormat = ff;
		charset = ff.getConversionTable();
	}

	public void setInputStream(InputStream byteStream) {
		stream = byteStream;
	}

	public void setFileFormat(FileFormat fFormat) {
		fileFormat = fFormat;
	}

	public boolean hasNext() throws IOException {
		return (stream.available() == 0) ? false : true;
	}

	/**
	 * @return List of next record's fields values. Elements may contain String,
	 *         BigDecimal, BigInteger, Double or Float.
	 * @throws IOException
	 * @throws FileFormatException
	 */
	public List next() throws IOException, FileFormatException,
			RecordParseException {
		String dfv = "";
		RecordFormat rf;
		ArrayList fields = new ArrayList();
		int dfsz = fileFormat.getDistinguishFieldSize().intValue();
		Map recDef = new RecordsMap(fileFormat);

		if (dfsz < 0) {
			throw new FileFormatException(
					"Negative distinguish field size is invalid.");
		}

		recNr++;

		if (dfsz > 0) {
			try {
				dfv = readText(dfsz);
			} catch (FieldParseException ex) {
				log.error("Total bytes processed before error: "
						+ bytesProcessed);
				throw new RecordParseException(
						"Unexpected EOF when reading distinguished field of record nr: "
								+ recNr + ".", null, fields, ex);
			}

			rf = (RecordFormat) recDef.get(dfv);
		} else {
			rf = (RecordFormat) recDef.get("0");
		}

		if (rf == null) {
			throw new FileFormatException("No such record format : " + dfv);
		}

		try {
			getFieldsValues(rf.getFieldFormatOrFieldsGroup(), fields, dfv);
		} catch (FieldParseException ex) {
			String printable = new String(ex.getOryginalData()).replaceAll(
					"\\p{Cntrl}", ".");
			FieldFormat ff = ex.getFieldFormat();
			log.error("Cannot parse field: " + ff.getName() + ". Data: '"
					+ printable + "', Picture: " + ff.getPicture() + ", Type: "
					+ ff.getType() + ", Size: " + ff.getSize());
			log.error("Total bytes processed before error: " + bytesProcessed);

			String msg;

			if (ex.getOryginalData().length == ff.getSize().intValue()) {
				msg = "Couldn't parse record nr: ";
			} else {
				msg = "Unexpected EOF while reading record nr: ";
			}

			throw new RecordParseException(msg + recNr + ".",
					rf.getFieldFormatOrFieldsGroup(), fields, ex);
		}

		// Skip new line byte(s) if any
		try {
			readText(fileFormat.getNewLineSize().intValue());
		} catch (FieldParseException ex) {
			log.error("Total bytes processed before error: " + bytesProcessed);
			throw new RecordParseException(
					"Unexpected EOF while reading new line separator of record nr: "
							+ recNr + ".", rf.getFieldFormatOrFieldsGroup(),
					fields, ex);
		}

		return fields;
	}

	private List getFieldsValues(List fieldsList, List values, String dfv)
			throws IOException, FileFormatException, FieldParseException {
		HashMap potentialDepOn = new HashMap();
		Iterator i = fieldsList.iterator();

		while (i.hasNext()) {
			Object o = i.next();

			if (o instanceof FieldFormat) {
				int occurs = 1;
				FieldFormat fF = (FieldFormat) o;
				String dependingon = fF.getDependingOn();

				if (dependingon.length() > 0) {
					BigDecimal bd = (BigDecimal) potentialDepOn
							.get(dependingon);
					occurs = bd.intValue();
				} else {
					occurs = fF.getOccurs().intValue();
				}

				char fType = fF.getType().charAt(0);
				while (occurs-- > 0) {
					BigDecimal v;

					switch (fType) {
					case 'X':

						if (dfv.length() > 0) {
							values.add(dfv);
							dfv = "";
						} else {
							values.add(readText(fF));
						}

						break;

					case '1':
						values.add(readComp1(fF));

						break;

					case '2':
						values.add(readComp2(fF));

						break;

					case '3':
						v = readPacked(fF);
						values.add(v);
						potentialDepOn.put(fF.getName(), v);

						break;

					case '7':
						values.add(readComp7(fF));

						break;

					case '8':
						values.add(readComp8(fF));

						break;

					case '9':
						v = readZoned(fF);
						values.add(v);
						potentialDepOn.put(fF.getName(), v);

						break;

					case 'D':

						/*
						 * if (!fF.getPacked()) { values.add(readDateZoned(fF));
						 * } else { values.add(readDatePacked(fF)); }
						 */
						break;

					case 'B':

						BigDecimal bd = readBinary(fF);
						values.add(bd);
						potentialDepOn.put(fF.getName(), bd);

						break;

					case 'T':
						values.add(readTransparent(fF));

						break;

					case 'H':
						values.add(readByteAsHex(fF));

						break;

					default:
						throw new FileFormatException(
								"Invalid field type definition: " + fType);
					}
				} // while end
			}

			if (o instanceof FieldsGroup) {
				int occurs = 1;
				FieldsGroup fG = (FieldsGroup) o;
				String dependingon = fG.getDependingOn();

				if (dependingon.length() > 0) {
					BigDecimal bd = (BigDecimal) potentialDepOn
							.get(dependingon);
					occurs = bd.intValue();
				} else {
					occurs = fG.getOccurs().intValue();
				}

				while (occurs-- > 0) {
					getFieldsValues(fG.getFieldFormatOrFieldsGroup(), values,
							dfv);
				}
			}
		}

		return values;
	}

	String readText(int len) throws IOException, FieldParseException {
		byte[] buf = new byte[len];
		int actuallyRead = stream.read(buf);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			if (actuallyRead < 0) {
				actuallyRead = 0;
			}

			byte[] buf2 = new byte[actuallyRead];

			for (int i = 0; i < actuallyRead; i++)
				buf2[i] = buf[i];

			throw new FieldParseException(buf2, null, null);
		}

		return new String(buf, charset);
	}

	String readText(FieldFormat ff) throws IOException, FieldParseException {
		int len = getByteSize(ff);
		String retValue;

		try {
			retValue = readText(len);
		} catch (FieldParseException ex) {
			throw new FieldParseException(ex.getOryginalData(), ff, ex);
		}

		return retValue;
	}

	Float readComp1(FieldFormat ff) throws IOException, FieldParseException {
		int len = getByteSize(ff);
		byte[] b = new byte[len];
		float f;
		int actuallyRead = stream.read(b);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(b, ff, null);
		}

		try {
			f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
		} catch (Exception ex) {
			throw new FieldParseException(b, ff, ex);
		}

		return new Float(f);
	}

	Double readComp2(FieldFormat ff) throws IOException, FieldParseException {
		int len = getByteSize(ff);
		byte[] b = new byte[len];
		double d;
		int actuallyRead = stream.read(b);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(b, ff, null);
		}

		try {
			d = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getDouble();
		} catch (Exception ex) {
			throw new FieldParseException(b, ff, ex);
		}

		return new Double(d);
	}

	Float readComp7(FieldFormat ff) throws IOException, FieldParseException {
		int len = getByteSize(ff);
		byte[] b = new byte[len];
		float f;
		int actuallyRead = stream.read(b);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(b, ff, null);
		}

		try {
			f = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
		} catch (Exception ex) {
			throw new FieldParseException(b, ff, ex);
		}

		return new Float(f);
	}

	Double readComp8(FieldFormat ff) throws IOException, FieldParseException {
		int len = getByteSize(ff);
		byte[] b = new byte[len];
		double d;
		int actuallyRead = stream.read(b);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(b, ff, null);
		}

		try {
			d = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble();
		} catch (Exception ex) {
			throw new FieldParseException(b, ff, ex);
		}

		return new Double(d);
	}

	BigDecimal readZoned(FieldFormat ff) throws IOException,
			FieldParseException {
		int len = getByteSize(ff);
		int dec = ff.getDecimal().intValue();
		byte[] b = new byte[len];
		byte c;
		int idx;
		char[] pos = { 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8,
				0xC9 };
		char[] neg = { 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8,
				0xD9 };
		String IBMpositives = new String(pos);
		String IBMnegatives = new String(neg);
		String failoverpositives = "{ABCDEFGHI";
		String failovernegatives = "}JKLMNOPQR";
		String MFnegatives = "pqrstuvwxy";
		String CAnegatives = " !\"#$%&'()";

		int actuallyRead = stream.read(b);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(b, ff, null);
		}

		StringBuffer buf = new StringBuffer(new String(b, charset));
		int lastidx = buf.length() - 1;
		char lastbyte = buf.charAt(lastidx);
		boolean positive = true;

		/*
		 * // In case most significant digit is over punched with sign idx =
		 * failoverpositives.indexOf(buf.charAt(0)); if (idx > -1) {
		 * buf.replace(0, 1, new Integer(idx).toString()); positive = true; }
		 * idx = failovernegatives.indexOf(buf.charAt(0)); if (idx > -1) {
		 * buf.replace(0, 1, new Integer(idx).toString()); positive = false; }
		 */
		idx = IBMpositives.indexOf(lastbyte);
		if (idx > -1) {
			buf.replace(lastidx, lastidx + 1, new Integer(idx).toString());
			positive = true;
		}

		idx = IBMnegatives.indexOf(lastbyte);
		if (idx > -1) {
			buf.replace(lastidx, lastidx + 1, new Integer(idx).toString());
			positive = false;
		}

		idx = failoverpositives.indexOf(lastbyte);
		if (idx > -1) {
			buf.replace(lastidx, lastidx + 1, new Integer(idx).toString());
			positive = true;
		}

		idx = failovernegatives.indexOf(lastbyte);
		if (idx > -1) {
			buf.replace(lastidx, lastidx + 1, new Integer(idx).toString());
			positive = false;

		}

		idx = MFnegatives.indexOf(lastbyte);
		if (idx > -1) {
			buf.replace(lastidx, lastidx + 1, new Integer(idx).toString());
			positive = false;
		}

		idx = CAnegatives.indexOf(lastbyte);
		if (idx > -1) {
			buf.replace(lastidx, lastidx + 1, new Integer(idx).toString());
			positive = false;
		}

		if ((dec > 0) && ff.isImpliedDecimal()) {
			buf.insert(buf.length() - dec, '.');
		}

		/*
		 * second time ?! if ((dec > 0) && ff.isImpliedDecimal()) {
		 * buf.insert(buf.length() - dec, '.'); }
		 */

		BigDecimal retVal;

		try {
			retVal = new BigDecimal(buf.toString().trim());
		} catch (NumberFormatException ex) {
			throw new FieldParseException(buf.toString().getBytes(), ff, ex);
		}

		if (!positive) {
			retVal = retVal.negate();
		}

		return retVal;
	}

	/**
	 * TODO Need to check if nibble is 0-9 and throw exception if A-F.
	 * 
	 * @return
	 * @throws IOException
	 */
	BigDecimal readPacked(FieldFormat ff) throws IOException,
			FieldParseException {
		int len = getByteSize(ff);
		int dec = ff.getDecimal().intValue();
		byte[] buf = new byte[len];
		StringBuffer strbuf = new StringBuffer();
		int tmp;
		int tmp1;
		int tmp2;
		int actuallyRead = stream.read(buf);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(buf, ff, null);
		}

		for (int i = 0; i < len; i++) {
			tmp = buf[i];
			tmp1 = tmp & 0xF0;
			tmp2 = tmp1 >> 4;
			strbuf.append(tmp2);

			if (i < (len - 1)) {
				tmp = buf[i];
				tmp1 = tmp & 0x0F;
				strbuf.append(tmp1);
			}
		}

		if ((dec > 0) && ff.isImpliedDecimal()) {
			strbuf.insert(strbuf.length() - dec, '.');
		}

		BigDecimal retVal;

		try {
			retVal = new BigDecimal(strbuf.toString());
		} catch (NumberFormatException ex) {
			throw new FieldParseException(strbuf.toString().getBytes(), ff, ex);
		}

		tmp = buf[len - 1];
		tmp1 = tmp & 0x0F;

		if ((tmp1 == 0x0F) || (tmp1 == 0x0C)) {
			return retVal;
		} else if (tmp1 == 0x0D) {
			return retVal.negate();
		} else {
			if (log.isDebugEnabled())
				log.debug("Packed field sign nibble auto-correction. Field parsed but sign nibble not equal to 0xC, 0xD or 0xF. Assuming NOT negative. Field name: "
						+ ff.getName());

			return retVal;
		}
	}

	String readDateZoned(FieldFormat ff) throws IOException,
			FieldParseException {
		StringBuffer buf = new StringBuffer(readText(ff));
		char lastbyte = buf.charAt(buf.length() - 1);

		if (lastbyte == 0xC6) {
			;
		}

		return buf.toString();
	}

	/**
	 * TODO: Check if no scientific notation.
	 * 
	 * @return String representation of date.
	 * @throws IOException
	 */
	String readDatePacked(FieldFormat ff) throws IOException,
			FieldParseException {
		return readPacked(ff).toString();
	}

	BigDecimal readBinary(FieldFormat ff) throws IOException,
			FieldParseException {
		// TODO size should be rather computed here depending on cobol impl.
		// cb2xml2cobol2j xsl assumes IBM mainframe binary type rep.
		int len = getByteSize(ff);
		byte[] buf;
		buf = new byte[len];

		BigDecimal retVal;
		int actuallyRead = stream.read(buf);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(buf, ff, null);
		}

		try {
			retVal = new BigDecimal(new BigInteger(buf), ff.getDecimal()
					.intValue());
		} catch (NumberFormatException ex) {
			throw new FieldParseException(buf, ff, ex);
		}

		return retVal;
	}

	byte[] readTransparent(FieldFormat ff) throws IOException,
			FieldParseException {
		int len = getByteSize(ff);
		byte[] buf;
		buf = new byte[len];

		int actuallyRead = stream.read(buf);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(buf, ff, null);
		}

		return buf;
	}

	String readByteAsHex(FieldFormat ff) throws IOException,
			FieldParseException {
		int len = getByteSize(ff);
		byte[] buf;
		buf = new byte[len];

		int actuallyRead = stream.read(buf);

		if (actuallyRead == len) {
			bytesProcessed += actuallyRead;
		} else {
			throw new FieldParseException(buf, ff, null);
		}

		return byteArrayToHexString(buf);
	}

	public static int getByteSize(FieldFormat fieldF) {
		int sz = fieldF.getSize().intValue();

		if (fieldF.getType().equals("3")) {
			sz++;
			if ((sz % 2) != 0) {
				return (sz / 2) + 1;
			} else {
				return sz / 2;
			}
		} else {
			return sz;
		}
	}

	/**
	 * @deprecated Use next() and combine with commas,tabs,etc yourselve.
	 * @see next()
	 * @return
	 */
	public String getNextRecordAsPlainString(String fieldSeparator)
			throws IOException, FileFormatException, RecordParseException,
			FieldParseException {
		StringBuffer row = new StringBuffer();
		Iterator i = next().iterator();
		boolean afterfirst = false;

		while (i.hasNext()) {
			if (afterfirst) {
				row.append(fieldSeparator);
			}

			row.append(i.next().toString());
			afterfirst = true;
		}

		return row.toString();
	}

	/**
	 * Convert a byte[] array to readable string format. This makes the "hex"
	 * readable!
	 * 
	 * @return result String buffer in String format
	 * @param in
	 *            byte[] buffer to convert to string format
	 */
	private String byteArrayToHexString(byte[] in) {
		byte ch = 0x00;

		if ((in == null) || (in.length <= 0)) {
			return null;
		}

		String[] pseudo = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"A", "B", "C", "D", "E", "F" };
		StringBuffer out = new StringBuffer(in.length * 2);

		for (int i = 0; i < in.length; i++) {
			ch = (byte) (in[i] & 0xF0);

			// Strip off high nibble
			ch = (byte) (ch >>> 4);

			// shift the bits down
			ch = (byte) (ch & 0x0F);

			// must do this is high order bit is on!
			out.append(pseudo[(int) ch]);

			// convert the nibble to a String Character
			ch = (byte) (in[i] & 0x0F);

			// Strip off low nibble
			out.append(pseudo[(int) ch]);

			// convert the nibble to a String Character
		}

		String rslt = new String(out);

		return rslt;
	}
}
