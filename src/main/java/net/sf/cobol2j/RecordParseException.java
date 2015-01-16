package net.sf.cobol2j;

import java.util.List;


public class RecordParseException extends Exception {
	private static final long serialVersionUID = 1431246669429020554L;
	private List<?> recordDef;
    private List<?> partialRecord;

    public RecordParseException(String msg, List<?> rDef, List<?> pRec,
        Throwable cause) {
        super(msg, cause);
        recordDef = rDef;
        partialRecord = pRec;
    }

    public List<?> getPartialRecord() {
        return partialRecord;
    }

    public List<?> getRecordDef() {
        return recordDef;
    }

    public byte[] getFieldData() {
        return ((FieldParseException) getCause()).getOryginalData();
    }
}
