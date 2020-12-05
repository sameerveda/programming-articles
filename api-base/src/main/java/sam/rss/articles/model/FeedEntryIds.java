package sam.rss.articles.model;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import sam.rss.articles.utils.Ids;

public class FeedEntryIds implements Serializable, JSONString {
	private static final long serialVersionUID = 1L;

	private int id = Ids.ID_IDS;
	private int[] data;
	private int maxId;
	private String type = "IDS";
	private int version;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		if (id != Ids.ID_IDS)
			throw new IllegalArgumentException("expected id == " + Ids.ID_IDS);
		this.id = id;
	}

	public int[] getData() {
		return data;
	}

	public void setData(int[] data) {
		this.data = data;
	}

	public int getMaxId() {
		return maxId;
	}

	public void setMaxId(int maxId) {
		this.maxId = maxId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if (!type.equals("IDS"))
			throw new IllegalArgumentException("expected type == \"IDS\"");
		this.type = type;
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
		JSONArray array = data == null ? null : new JSONArray();
		if (data != null) {
			for (int i : data)
				array.put(i);
		}
		o.put("data", array);
		o.put("maxId", maxId);
		o.put("type", type);
		return o;
	}

	@Override
	public String toJSONString() {
		return toJson().toString();
	}
}
