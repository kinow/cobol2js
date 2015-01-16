package net.sf.cobol2j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


@SuppressWarnings("rawtypes")
public class RecordsMap extends HashMap {
	
	private static final long serialVersionUID = 4502397708683967412L;

	@SuppressWarnings("unchecked")
	public RecordsMap(FileFormat fF) {
        List<?> l = fF.getRecordFormat();
        Iterator<?> i = l.iterator();

        while (i.hasNext()) {
            RecordFormat rF = (RecordFormat) i.next();
            put(rF.getDistinguishFieldValue(), rF);
        }
    }
}
