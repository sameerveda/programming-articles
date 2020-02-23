package programming.articles.model;

public enum DataStatus {
	UNREAD, READ, LATER, DELETED, FAVORITE;
	
	public static DataStatus parse(String value) {
		if(value == null || value.length() == 0)
			return DataStatus.UNREAD;
		
		switch (value.toUpperCase()) {
			case "UNREAD":  return UNREAD;
			case "READ":    return READ;
			case "LATER":   return LATER;
			case "DELETED": return DELETED;
			case "FAVORITE": return FAVORITE;
			default: throw new IllegalArgumentException("bad DataStatus: "+value);
		}
	}

	public byte byteValue() {
		return (byte)ordinal();
	} 
}
