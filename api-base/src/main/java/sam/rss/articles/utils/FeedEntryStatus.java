package sam.rss.articles.utils;

public enum FeedEntryStatus {
	UNREAD, READ, LATER, DELETED, FAVORITE;

	public static FeedEntryStatus parse(String value) {
		return parse(value, false);
	}
	public static FeedEntryStatus parse(String value, boolean nullable) {
		if(value == null && nullable) return null;
		if(value == null || value.length() == 0)
			return FeedEntryStatus.UNREAD;

		switch (value.toUpperCase()) {
			case "UNREAD":  return UNREAD;
			case "READ":    return READ;
			case "LATER":   return LATER;
			case "DELETED": return DELETED;
			case "FAVORITE": return FAVORITE;
			default: throw new IllegalArgumentException("bad FeedEntryStatus: "+value);
		}
	}

	public byte byteValue() {
		return (byte)ordinal();
	}
}
