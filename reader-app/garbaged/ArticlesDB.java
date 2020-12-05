package sam.article.reader.model;

import static sam.article.reader.app.FilterOptionsImpl.SORT_BY;
import static sam.article.reader.app.FilterOptionsImpl.SORT_DIR;
import static sam.article.reader.app.FilterOptionsImpl.STATUS;
import static sam.dblist.viewer.FilterOptions.SHOW_ALL;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.carrotsearch.hppc.IntObjectScatterMap;
import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.ObjectIntScatterMap;

import sam.dblist.viewer.Connector.Result;
import sam.dblist.viewer.DB;
import sam.dblist.viewer.FilterOptions;
import sam.dblist.viewer.model.Title;
import sam.functions.ConsumerWithException;
import sam.fx.alert.FxAlert;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;
import sam.sql.sqlite.Sqlite4JavaDB;
import sam.string.StringSplitIterator;

public class ArticlesDB extends Sqlite4JavaDB implements DB<Article> {
	public static final String BOOKMARK = "Bookmark";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final IntObjectScatterMap<String> datesMap = new IntObjectScatterMap<>();
	private final IntObjectScatterMap<String> faviconsMap = new IntObjectScatterMap<>();
	private IntObjectScatterMap<String> tagsIdMap;
	private ObjectIntScatterMap<String> tagsNameMap;
	private int today = -1;

	public ArticlesDB(File dbpath) throws SQLiteException {
		super(dbpath, false);
	}

	public Result queryIds(String text, int offset, int pageSize, List<FilterOptions> filterOptions) throws SQLiteException {
		String nameLike = Checker.isEmptyTrimmed(text) ? null : text;
		Status status = STATUS.getValue() == SHOW_ALL ? null : (Status)STATUS.getValue();
		SortBy sort = SORT_BY.getValue();
		SortDir sortDir = SORT_DIR.getValue();
		System.out.println("query: "+text+", "+filterOptions.stream().map(f -> f.getTitle() +"="+f.getValue()).collect(Collectors.joining(", ")));

		final StringBuilder sql = new StringBuilder(" FROM Data ");
		ConsumerWithException<SQLiteStatement, SQLiteException> binder = null;

		if (!(status == null && nameLike == null)) {
			sql.append(" WHERE ");

			if (status != null) {
				sql.append("(");
				if (Status.UNREAD.equals(status))
					sql.append("status IS NULL OR ");
				sql.append("status = '").append(status).append("') ");
			}

			if (nameLike != null) {
				if (status != null)
					sql.append(" AND ");
				sql.append(" title LIKE ? ");
			}

			if (nameLike != null) {
				String nl = "%" + nameLike + "%";
				binder = (st -> st.bind(1, nl));
			}
		}

		int count = getFirst("SELECT count(id)" + sql, binder, rs -> rs.columnInt(0));

		if (sort != null)
			sql.append(" ORDER BY ").append(sort.field).append(' ').append(sortDir == null ? sort.dir : sortDir).append(" ");

		sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
		sql.insert(0, "SELECT id, title");

		SQLiteStatement st = prepare(sql.toString(), false, binder);
		return new Result(count, collectToList(st, rs -> new Title(st.columnInt(0), st.columnString(1))));
	}

	public String getDate(int id) {
		return datesMap.get(id);
	}

	public String getIcon(int id) {
		return faviconsMap.get(id);
	}

	public String[] parseTags(String tags) {
		this.initTags();

		if (Checker.isEmptyTrimmed(tags))
			return new String[0];

		IntScatterSet set = new IntScatterSet();
		new StringSplitIterator(tags, '.').forEachRemaining(s -> {
			if (Checker.isNotEmptyTrimmed(s))
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
		if (Checker.isEmpty(tagNames))
			return null;

		initTags();
		StringBuilder sb = new StringBuilder(tagNames.length * 3);

		Arrays.stream(tagNames).mapToInt(s -> {
			int n = this.tagsNameMap.getOrDefault(s.toLowerCase(), -1);
			if (n < 0)
				throw new IllegalStateException("no id found for tag: " + s);
			return n;
		}).sorted().distinct().forEach(n -> sb.append('.').append(n).append('.'));

		return sb.toString();
	}

	public String[] allTags() {
		this.initTags();
		return tagsIdMap.values().toArray(String.class);
	}

	private void initTags() {
		if (this.tagsIdMap == null) {
			tagsIdMap = new IntObjectScatterMap<>();
			try {
				iterate("SELECT id, name from Tags", st -> tagsIdMap.put(st.columnInt(0), st.columnString(1)));
			} catch (SQLiteException e) {
				throw new RuntimeException(e);
			}
			tagsNameMap = new ObjectIntScatterMap<>();
			HPPCUtils.forEach(tagsIdMap, (s, t) -> tagsNameMap.put(t.toLowerCase(), s));
		}
	}

	public String getTag(String s) {
		if (Checker.isEmptyTrimmed(s))
			return null;

		s = s.trim();
		int id = this.tagsNameMap.getOrDefault(s, -1);
		if (id == -1) {
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

	private static final String SELECT_BY_ID_SQL = Article.SELECT_SQL + " WHERE id is ?";

	@Override
	public Article getFullItem(int id) {
		try {
			Article c = getFirstByInt(SELECT_BY_ID_SQL, id, Article::new);

			if (!this.faviconsMap.containsKey(c.favicon))
				this.faviconsMap.put(c.favicon,
						getFirstByInt("SELECT url FROM Favicons WHERE id is ?", c.favicon, getString(0)));
			if (!this.datesMap.containsKey(c.addedOn))
				this.datesMap.put(c.addedOn,
						getFirstByInt("SELECT _date FROM Dates WHERE id is ?", c.addedOn, getString(0)));
			if (!this.datesMap.containsKey(c.updatedOn))
				this.datesMap.put(c.updatedOn,
						getFirstByInt("SELECT _date FROM Dates WHERE id is ?", c.updatedOn, getString(0)));
			return c;
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog(SELECT_BY_ID_SQL + " = " + id, "failed to load", e);
		}
		return null;
	}

	public Article update(Article c) {
		try {
			initToday();
			execute("UPDATE Data SET status=?, tags=?, updatedOn=?, version=version+1 WHERE id=?;", st -> {
				st.bind(1, c.getStatus());
				st.bind(2, c.getTags());
				st.bind(3, today);
				st.bind(4, c.id);
			});
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog(c, "failed to update article", e);
		}
		return getFullItem(c.id);
	}

	private void initToday() throws SQLiteException {
		if (today != -1)
			return;

		String now = LocalDate.now().toString();
		Integer n = getFirst("SELECT id FROM Dates WHERE _date=?", st -> st.bind(1, now), getInt(0));
		if (n == null) {
			today = getFirst("SELECT max(id) FROM Dates", null, getInt(0)) + 1;
			execute("INSERT INTO Dates(id, _date) VALUES(?,?);", st -> {
				st.bind(1, today);
				st.bind(2, now);
			});
		} else {
			today = n;
		}
		this.datesMap.put(today, now);
	}

	private static final String FIND_SQL = Article.SELECT_SQL
			+ " WHERE lower(title)=lower(?) OR lower(source)=lower(?) OR lower(redirect)=lower(?);";

	public Article addNew(String title, String url) {
		List<Article> articles;
		logger.debug("INSERT: title='{}', url='{}'", title, url);

		try {
			articles = collectToList(bindAll(FIND_SQL, new String[] { title, url, url }, STRING_BINDER), Article::new);
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog("failed to find", "failed to find", e);
			return null;
		}
		if (!articles.isEmpty()) {
			logger.info("articles found for title: '{}', url: '{}', articles: {}", title, url, articles);
			return articles.get(0);
		}

		try {
			initToday();
			execute("INSERT INTO Data(title, source, addedOn) VALUES(?,?,?);", st -> {
				st.bind(1, title);
				st.bind(2, url);
				st.bind(3, today);
			});
			return getFirst(Article.SELECT_SQL + " WHERE title=?;", st -> st.bind(1, title), Article::new);
		} catch (SQLiteException e) {
			FxAlert.showErrorDialog("failed to insert", "failed to insert", e);
		}

		return null;
	}
}
