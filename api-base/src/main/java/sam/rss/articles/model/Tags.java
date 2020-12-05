package sam.rss.articles.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONString;

import sam.rss.articles.utils.Ids;

public abstract class Tags implements Serializable, JSONString {
	private static final long serialVersionUID = 1L;

	private int id = Ids.ID_TAGS;
	private Map<String, String> data;
	private String type = "TAGS";
	private int version;
	private int maxId;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		if (id != Ids.ID_TAGS)
			throw new IllegalArgumentException("expected id == " + Ids.ID_TAGS);
		this.id = id;
	}

	public Map<String, String> getData() {
		return data;
	}

	public void setData(Map<String, String> data) {
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if (!type.equals("TAGS"))
			throw new IllegalArgumentException("expected type == \"TAGS\"");
		this.type = type;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getMaxId() {
		return maxId;
	}

	public void setMaxId(int maxId) {
		this.maxId = maxId;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return obj instanceof Tags && ((Tags) obj).id == this.id;
	}

	@Override
	public String toString() {
		return toJson().toString(2);
	}

	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		o.put("id", id);
		o.put("data", data);
		o.put("type", type);
		return o;
	}

	@Override
	public String toJSONString() {
		return toJson().toString();
	}

	public abstract int putNewTag(String s);
	
	protected Map<String, Integer> tagsNameIdMap;

	/**
	 * @param tag
	 * @return id if found, otherwise -1
	 */
	public int getTagIdFor(String tag) {
		if(tagsNameIdMap == null) {
			tagsNameIdMap = new HashMap<String, Integer>();
			this.getData().forEach((s,t) -> tagsNameIdMap.put(t.toLowerCase(), Integer.parseInt(s)));
		}
		
		return tagsNameIdMap.getOrDefault(tag.toLowerCase(), -1);
	}

	protected void reset(Tags tags) {
		this.data = tags.data;
		this.version = tags.version;
		this.maxId = tags.maxId;
	}
	
	private final Pattern pattern = Pattern.compile(".", Pattern.LITERAL); 

	public String[] parseTags(String tags) {
		if(tags == null || tags.length() == 0) return null;
		Map<String, String> map = getData();
		return pattern.splitAsStream(tags)
				.filter(s -> !s.isEmpty())
				.map(map::get)
				.filter(Objects::nonNull)
				.sorted()
				.toArray(String[]::new);
	}

	public String serializeTags(String[] tagNames) {
		if (tagNames == null || tagNames.length == 0)
			return null;

		return Arrays.stream(tagNames).mapToInt(s -> {
			int n = getTagIdFor(s);
			if (n < 0)
				throw new IllegalStateException("no id found for tag: " + s);
			return n;
		}).sorted().distinct().mapToObj(n -> Integer.toString(n)).collect(Collectors.joining("..", ".", "."));
	}

	public String getTag(int tagId) {
		return getData().get(Integer.toString(tagId));
	}

	protected void putTag(int id, String value) {
		this.data.put(Integer.toString(id), value);
		if(this.tagsNameIdMap != null)
			this.tagsNameIdMap.put(value.toLowerCase(), id);
	}
}
