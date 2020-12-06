package sam.rss.articles.aws.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.FeedEntryIds;
import sam.rss.articles.model.FeedEntryStatuses;
import sam.rss.articles.model.Tags;
import sam.rss.articles.service.FeedsService;
import sam.rss.articles.sqlite.model.MinimalFeedEntry;
import sam.rss.articles.sqlite.model.SqliteFeedEntry;
import sam.rss.articles.utils.FeedEntryStatus;
import static sam.rss.articles.utils.FeedEntryStatus.*;

@Singleton
public class SqliteFeedService implements FeedsService {
	private final static String SELECT_SQL = "SELECT id,title,link,redirect,tags,status,summary,notes,updated_on,published_on,version FROM FeedEntries";
	private final static String SELECT_SQL_BY_ID = SELECT_SQL + " WHERE id = ?";

	private final SQLiteConnection connection;

	@Inject
	public SqliteFeedService(SQLiteConnection connection, boolean open) throws SQLiteException {
		this.connection = Objects.requireNonNull(connection);
		if(open)
			this.connection.open(false);
	}

	@Override
	public FeedEntry getEntry(int id) {
		if (id < 100)
			throw new IllegalArgumentException("bad id, id should be < 100, but was: " + id);

		try {
			SQLiteStatement rs = connection.prepare(SELECT_SQL_BY_ID);
			rs.bind(1, id);

			while (rs.step())
				return feedEntry(rs);

			return null;
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<FeedEntry> getEntries(int[] ids) {
		if (ids.length == 0)
			return Collections.emptyList();
		for (int id : ids) {
			if (id < 100)
				throw new IllegalArgumentException("bad id, id should be > 100, but was: " + id + "in: " + ids);
		}

		try {
			SQLiteStatement rs = connection.prepare(Arrays.stream(ids).mapToObj(id -> Integer.toString(id))
					.collect(Collectors.joining(",", SELECT_SQL + " WHERE id IN(", ")")), false);
			List<FeedEntry> list = new ArrayList<FeedEntry>();
			while (rs.step())
				list.add(feedEntry(rs));

			return list;
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	public List<MinimalFeedEntry> getMinimalEntries(FeedEntryStatus status, int offset, int limit, boolean reverseOrder) throws SQLiteException {
		String sql = (status == UNREAD ? "SELECT id,title FROM FeedEntries WHERE status IS NULL OR status='UNREAD'" : "SELECT id,title FROM FeedEntries WHERE status='"+status+"'")
				.concat(reverseOrder ? " ORDER BY id DESC" : "")
				.concat(" LIMIT ? OFFSET ?");

		SQLiteStatement rs = connection.prepare(sql, false).bind(1, limit).bind(2, offset);
		List<MinimalFeedEntry> list = new ArrayList<>();
		while (rs.step())
			list.add(new MinimalFeedEntry(rs.columnInt(0), rs.columnString(1)));

		return list;
	}

	private FeedEntry feedEntry(SQLiteStatement rs) throws SQLiteException {
		SqliteFeedEntry s = new SqliteFeedEntry(rs.columnInt(0), // id
				rs.columnString(1), // title
				rs.columnString(2), // link
				rs.columnString(3), // redirect
				rs.columnString(4), // tags
				FeedEntryStatus.parse(rs.columnString(5), true), // status
				rs.columnBlob(6), // summary
				rs.columnString(7), // notes
				rs.columnInt(8), // updatedOn
				rs.columnInt(9), // publishedOn
				rs.columnInt(10) // version
				);
		s.setService(this);
		return s;
	}

	private int[] idList(String sql, FeedEntryStatus status) throws SQLiteException {
		SQLiteStatement rs = connection.prepare(sql, false);
		if (status != null)
			rs.bind(1, status.toString());
		IntStream.Builder builder = IntStream.builder();
		while (rs.step())
			builder.add(rs.columnInt(0));

		int[] data = builder.build().toArray();
		Arrays.sort(data);
		return data;
	}

	@Override
	public FeedEntryIds getIds() {
		try {
			int[] data = idList("SELECT id FROM FeedEntries", null);
			FeedEntryIds res = new FeedEntryIds();
			res.setData(data);
			res.setMaxId(data[data.length - 1]);

			return res;
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public FeedEntryStatuses getStatueses() {
		try {
			FeedEntryStatuses res = new FeedEntryStatuses();
			res.setDeleted(idList("SELECT id FROM FeedEntries WHERE status = ?", FeedEntryStatus.DELETED));
			res.setFavorite(idList("SELECT id FROM FeedEntries WHERE status = ?", FeedEntryStatus.FAVORITE));
			res.setLater(idList("SELECT id FROM FeedEntries WHERE status = ?", FeedEntryStatus.LATER));
			res.setRead(idList("SELECT id FROM FeedEntries WHERE status = ?", FeedEntryStatus.READ));
			return res;
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Tags getTags() {
		try {
			Map<String, String> map = new HashMap<>();
			SQLiteStatement rs = connection.prepare("SELECT cast(id as text), name FROM Tags");
			while (rs.step())
				map.put(rs.columnString(0), rs.columnString(1));
			Tags t = new Tags() {
				private static final long serialVersionUID = 1L;

				@Override
				public int putNewTag(String s) {
					throw new IllegalAccessError();
				}
			};
			t.setData(map);
			return t;
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	public void executeUpdate(int id, String field, String value) {
		try {
			SQLiteStatement st = connection.prepare("UPDATE FeedEntries SET version = version + 1, " + field + "=? WHERE id=?;");
			st.bind(1, value);
			st.bind(2, id);
			st.step();
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	public void executeUpdate(int id, String field, long value) {
		try {
			SQLiteStatement st = connection.prepare("UPDATE FeedEntries SET version = version + 1, " + field + "=? WHERE id=?;");
			st.bind(1, value);
			st.bind(2, id);
			st.step();
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int versionOf(int id) {
		try {
			SQLiteStatement s = id < 100 ? connection.prepare("SELECT version FROM MetaVersions WHERE id = ?;") : connection.prepare("SELECT version FROM FeedEntries WHERE id = ?;");
			s.bind(1, id);
			return s.step() ? s.columnInt(0) : -1;
		} catch (SQLiteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		if(connection != null)
			connection.dispose();
	}

	public void updateStatuses(FeedEntryStatuses statueses) throws SQLiteException {
		updateStatus(statueses.getUnread(), UNREAD);
		updateStatus(statueses.getRead(), READ);
		updateStatus(statueses.getLater(), LATER);
		updateStatus(statueses.getDeleted(), DELETED);
		updateStatus(statueses.getFavorite(), FAVORITE);
		connection.exec("UPDATE MetaVersions SET version="+statueses.getVersion()+" WHERE id="+statueses.getId());
	}

	private void updateStatus(int[] ids, FeedEntryStatus status) throws SQLiteException {
		if(ids == null || ids.length == 0)
			return;

		connection.exec(Arrays.stream(ids).mapToObj(Integer::toString).collect(Collectors.joining(",", "UPDATE FeedEntries SET status='"+status+"' WHERE NOT status IS '"+status+"' AND id IN(", ")")));
		System.out.println("    Update Status to: "+status + ", for: "+connection.getChanges());
	}

}
