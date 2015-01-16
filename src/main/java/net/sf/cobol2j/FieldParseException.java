package net.sf.cobol2j;

public class FieldParseException extends Exception {
	private static final long serialVersionUID = -8396675981476932664L;
	private byte[] oryginalData;
    private FieldFormat fieldFormat;

    public FieldParseException(byte[] oD, FieldFormat fF, Throwable cause) {
        this("", oD, fF, cause);
    }

    public FieldParseException(String msg, byte[] oD, FieldFormat fF,
        Throwable cause) {
        super(msg, cause);
        oryginalData = oD;
        fieldFormat = fF;
    }

    public byte[] getOryginalData() {
        return oryginalData;
    }

    public FieldFormat getFieldFormat() {
        return fieldFormat;
    }
}
