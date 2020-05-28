package sam.book.search.model;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.carrotsearch.hppc.IntObjectScatterMap;
import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.ObjectIntScatterMap;

import sam.fx.alert.FxAlert;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;
import sam.sql.sqlite.Sqlite4javaHelper;
import sam.string.StringSplitIterator;

public class ArticlesDB extends Sqlite4javaHelper {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final IntObjectScatterMap<String> datesMap = new IntObjectScatterMap<>();
	private final IntObjectScatterMap<String> iconMap = new IntObjectScatterMap<>();
	private IntObjectScatterMap<String> tagsIdMap;
	private ObjectIntScatterMap<String> tagsNameMap;

	public ArticlesDB(File dbpath) throws SQLiteException {
		super(dbpath, false);
	}

	public SQLiteStatement queryIds(ArticleStatus status, SortBy sort, SortDir sortDir, String nameLike,
			String additionalWhere) throws SQLiteException {
		if (Checker.isEmptyTrimmed(nameLike))
			nameLike = null;
		if (Checker.isEmptyTrimmed(additionalWhere))
			additionalWhere = null;

		if (status == null && sort == null && nameLike == null && additionalWhere == null) {
			return con.prepare("SELECT id FROM Data;");
		}

		if (status == null && nameLike == null && additionalWhere == null) {
			return con.prepare(String.format("SELECT id FROM Data ORDER BY %s %s;", sort.field,
					sortDir == null ? sort.dir : sortDir));
		}

		StringBuilder sql = new StringBuilder("SELECT id FROM Data WHERE ");

		if (status != null) {
			sql.append("(");
			if (status == ArticleStatus.UNREAD)
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
		return st;
	}

	public String getDate(int id) {
		return datesMap.get(id);
	}

	public String getIcon(int id) {
		return iconMap.get(id);
	}

	private final StringBuilder select_sql = new StringBuilder(Article.SELECT_SQL).append(" WHERE id IN(");
	private final int length = select_sql.length();

	public Map<Integer, Article> loadData(Stream<Integer> ids) {
		select_sql.setLength(length);
		ids.forEach(s -> select_sql.append(s).append(','));
		select_sql.setCharAt(select_sql.length() - 1, ')');
		select_sql.append(';');

		try {
			Map<Integer, Article> map = stream(select_sql.toString(), Article::new)
					.collect(Collectors.toMap(a -> a.id, a -> a));
			fill(datesMap, "Dates", "_date", map.values().stream().flatMapToInt(a -> IntStream.of(a.addedOn, a.updatedOn)));
			fill(iconMap, "Favicons", "url", map.values().stream().mapToInt(a -> a.favicon));
			return map;
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog(select_sql, "failed to load", e);
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

	private void fill(IntObjectScatterMap<String> map, String tableName, String field, IntStream stream) throws SQLiteException {
		int[] array = stream.filter(t -> !map.containsKey(t)).distinct().toArray();
		if(array.length == 0)
			return;
		iterate(String.format("SELECT id, %s FROM %s WHERE id IN %s", field, tableName, Arrays.toString(array).replace('[', '(').replace(']', ')')), rs -> map.put(rs.columnInt(0), rs.columnString(1)));
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
}
