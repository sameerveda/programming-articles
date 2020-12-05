package sam.rss.articles.model;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import sam.rss.articles.utils.Ids;

public class FeedEntryStatuses implements Serializable, JSONString {
	private static final long serialVersionUID = 1L;

	private int id = Ids.ID_STATUS;
	private String type = "STATUS";
	private int[] read, unread, later, deleted, favorite;
	private int version;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		if (id != Ids.ID_STATUS)
			throw new IllegalArgumentException("expected id == " + Ids.ID_STATUS);
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if (!type.equals("STATUS"))
			throw new IllegalArgumentException("expected type == \"STATUS\"");
		this.type = type;
	}
	
	public void setUnread(int[] unread) {
		this.unread = unread;
	}
	
	public int[] getUnread() {
		return unread;
	}

	public int[] getRead() {
		return read;
	}

	public void setRead(int[] read) {
		this.read = read;
	}

	public int[] getLater() {
		return later;
	}

	public void setLater(int[] later) {
		this.later = later;
	}

	public int[] getDeleted() {
		return deleted;
	}

	public void setDeleted(int[] deleted) {
		this.deleted = deleted;
	}

	public int[] getFavorite() {
		return favorite;
	}

	public void setFavorite(int[] favorite) {
		this.favorite = favorite;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return toJson().toString(2);
	}

	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		o.put("id", id);
		o.put("type", type);
		put(o, "read", read);
		put(o, "later", later);
		put(o, "deleted", deleted);
		put(o, "favorite", favorite);
		return o;
	}

	private static void put(JSONObject o, String field, int[] data) {
		JSONArray array = data == null ? null : new JSONArray();
		if (data != null) {
			for (int i : data)
				array.put(i);
		}
		o.put(field, array);
	}

	@Override
	public String toJSONString() {
		return toJson().toString();
	}

}
