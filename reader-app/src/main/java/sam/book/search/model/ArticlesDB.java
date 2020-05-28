package sam.book.search.model;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.carrotsearch.hppc.IntObjectScatterMap;
import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.ObjectIntScatterMap;

import sam.collection.ArraysUtils;
import sam.fx.alert.FxAlert;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;
import sam.sql.sqlite.Sqlite4javaHelper;
import sam.string.StringSplitIterator;

public class ArticlesDB extends Sqlite4javaHelper {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	public static final List<String> ALL_STATUS = Collections.unmodifiableList(Arrays.asList("UNREAD", "READ", "LATER", "DELETED", "FAVORITE"));

	private final IntObjectScatterMap<String> datesMap = new IntObjectScatterMap<>();
	private final IntObjectScatterMap<String> faviconsMap = new IntObjectScatterMap<>();
	private IntObjectScatterMap<String> tagsIdMap;
	private ObjectIntScatterMap<String> tagsNameMap;
	private int today = -1;

	public ArticlesDB(File dbpath) throws SQLiteException {
		super(dbpath, false);
	}

	public List<Integer>  queryIds(String status, SortBy sort, SortDir sortDir, String nameLike, String additionalWhere) throws SQLiteException {
		if (Checker.isEmptyTrimmed(nameLike))
			nameLike = null;
		if (Checker.isEmptyTrimmed(additionalWhere))
			additionalWhere = null;

		if (status == null && sort == null && nameLike == null && additionalWhere == null) 
			return collectToList("SELECT id FROM Data;", getInt(0));

		if (status == null && nameLike == null && additionalWhere == null) 
			return collectToList(String.format("SELECT id FROM Data ORDER BY %s %s;", sort.field, sortDir == null ? sort.dir : sortDir), getInt(0));

		StringBuilder sql = new StringBuilder("SELECT id FROM Data WHERE ");

		if (status != null) {
			sql.append("(");
			if ("UNREAD".equals(status))
				sql.append("status IS NULL OR ");
			sql.append("status = '").append(status).append("') ");
		}

		if (nameLike != null) {
			if (status != null)
				sql.append(" AND ");
			sql.append(" name LIKE ? ");
		}

		if (additionalWhere != null) {
			if (status != null || nameLike != null)
				sql.append(" AND ");
			sql.append(additionalWhere);
		}

		if (sort != null) {
			sql.append(" ORDER BY ").append(sort.field).append(" ").append(sortDir == null ? sort.dir : sortDir);
		}

		SQLiteStatement st = con.prepare(sql.toString());
		if (nameLike != null)
			st.bind(1, "%" + nameLike + "%");

		logger.debug("{}", sql.append(nameLike == null ? "" : "%" + nameLike + "%"));
		return collectToList(st, getInt(0));
	}

	public String getDate(int id) {
		return datesMap.get(id);
	}

	public String getIcon(int id) {
		return faviconsMap.get(id);
	}

	public Map<Integer, Title> loadTitles(Stream<Integer> ids) {
		int[] array = ids.mapToInt(Integer::intValue).toArray();
		if(array.length == 0)
			return Collections.emptyMap();

		try {
			return collectToMap("SELECT id, title FROM Data WHERE id IN" + ArraysUtils.toString(array), rs -> rs.columnInt(0), rs -> new Title(rs.columnInt(0), rs.columnString(1)));
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog("SELECT id, title FROM Data WHERE id IN" + ArraysUtils.toString(array), "failed to load", e);
		}
		return Collections.emptyMap();
	}

	public String[] parseTags(String tags) {
		this.initTags();

		if(Checker.isEmptyTrimmed(tags))
			return  new String[0];

		IntScatterSet set = new IntScatterSet();
		new StringSplitIterator(tags, '.')
		.forEachRemaining(s -> {
			if(Checker.isNotEmptyTrimmed(s))
				set.add(Integer.parseInt(s));
		});

		int[] array = set.toArray();
		Arrays.sort(array);
		String[] res = new String[array.length];

		for (int i = 0; i < res.length; i++) 
			res[i] = this.tagsIdMap.get(array[i]);

		return res;
	}
	
	public String serializeTags(String[] tagNames) {
		if(Checker.isEmpty(tagNames))
			return null;
		
		initTags();
		StringBuilder sb = new StringBuilder(tagNames.length * 3);
		
		Arrays.stream(tagNames)
		.mapToInt(s -> {
			int n = this.tagsNameMap.getOrDefault(s.toLowerCase(), -1);
			if(n < 0)
				throw new IllegalStateException("no id found for tag: "+s);
			return n;
		})
		.sorted()
		.distinct()
		.forEach(n -> sb.append('.').append(n).append('.'));
		
		return sb.toString();
	}

	public String[] allTags() {
		this.initTags();
		return tagsIdMap.values().toArray(String.class);
	}

	private void initTags() {
		if(this.tagsIdMap == null) {
			tagsIdMap = new IntObjectScatterMap<>();
			try {
				iterate("SELECT id, name from Tags", st -> tagsIdMap.put(st.columnInt(0), st.columnString(1)));
			} catch (SQLiteException e) {
				throw new RuntimeException(e);
			}
			tagsNameMap = new ObjectIntScatterMap<>();
			HPPCUtils.forEach(tagsIdMap, (s,t) -> tagsNameMap.put(t.toLowerCase(), s));
		}
	}

	public String getTag(String s) {
		if(Checker.isEmptyTrimmed(s))
			return null;

		s = s.trim();
		int id = this.tagsNameMap.getOrDefault(s, -1);
		if(id == -1) {
			id = Arrays.stream(this.tagsIdMap.keys).max().getAsInt() + 1;
			int id2 = id;
			String s2 = s;
			try {
				execute("INSERT INTO Tags(id, name) VALUES(?,?)", st -> {
					st.bind(1, id2);
					st.bind(2, s2);
				});
			} catch (SQLiteException e) {
				throw new RuntimeException(e);
			}
			this.tagsIdMap.put(id, s);
			this.tagsNameMap.put(s.toLowerCase(), id);
			return s;
		} else {
			return tagsIdMap.get(id);
		}
	}

	public Article getArticle(int id) {
		try {
			Article c = getFirstByInt("SELECT * FROM Data WHERE id is ?", id, Article::new);

			if(!this.faviconsMap.containsKey(c.favicon)) 
				this.faviconsMap.put(c.favicon, getFirstByInt("SELECT url FROM Favicons WHERE id is ?", c.favicon, getString(0)));
			if(!this.datesMap.containsKey(c.addedOn)) 
				this.datesMap.put(c.addedOn, getFirstByInt("SELECT _date FROM Dates WHERE id is ?", c.addedOn, getString(0)));
			if(!this.datesMap.containsKey(c.updatedOn)) 
				this.datesMap.put(c.updatedOn, getFirstByInt("SELECT _date FROM Dates WHERE id is ?", c.updatedOn, getString(0)));
			return c;
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog("SELECT * FROM Data WHERE id is "+id, "failed to load", e);
		}
		return null;
	}

	public Article update(Article c) {
		try {
			if(today == -1) {
				String now = LocalDate.now().toString();
				Integer n = getFirst("SELECT id FROM Dates WHERE _date=?", st -> st.bind(1, now), getInt(0));
				if(n == null) {
					today = getFirst("SELECT max(id) FROM Dates", null, getInt(0)) + 1;
					execute("INSERT INTO Dates(id, _date) VALUES(?,?);", st -> {
						st.bind(1, today);
						st.bind(2, now);
					});
				} else {
					today = n;
				}
			}
			
			execute("UPDATE Data SET status=?, tags=?, updatedOn=?, version=version+1 WHERE id=?;", st -> {
				st.bind(1, c.getStatus());
				st.bind(2, c.getTags());
				st.bind(3, today);
				st.bind(4, c.id);
			});
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog(c, "failed to update article", e);
		}
		return getArticle(c.id);
	}
}
