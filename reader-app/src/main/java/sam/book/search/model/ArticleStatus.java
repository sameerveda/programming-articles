package sam.book.search.model;

public enum ArticleStatus {
	UNREAD, READ, LATER, DELETED, FAVORITE;
	
	public static ArticleStatus parse(String value) {
		if(value == null || value.length() == 0)
			return ArticleStatus.UNREAD;
		
		switch (value.toUpperCase()) {
			case "UNREAD":  return UNREAD;
			case "READ":    return READ;
			case "LATER":   return LATER;
			case "DELETED": return DELETED;
			case "FAVORITE": return FAVORITE;
			default: throw new IllegalArgumentException("bad ArticleStatus: "+value);
		}
	}

	public byte byteValue() {
		return (byte)ordinal();
	} 
}
