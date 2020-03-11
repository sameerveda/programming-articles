
package sam.rssowl;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static programming.articles.model.ConstantDataItem.ID;
import static programming.articles.model.ConstantDataItem.REDIRECT;
import static programming.articles.model.ConstantDataItem.SOURCE;
import static programming.articles.model.ConstantDataItem.TABLE_NAME;
import static programming.articles.model.ConstantDataItem.TITLE;
import static sam.full.access.dynamodb.DynamoConnection.value;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONWriter;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sqlite.JDBC;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ShortHashSet;
import com.carrotsearch.hppc.cursors.ShortObjectCursor;

import programming.articles.model.LoadedMetas;
import programming.articles.model.SettableLoadedMetas;
import programming.articles.model.Tag;
import sam.collection.ArraysUtils;
import sam.console.ANSI;
import sam.full.access.dynamodb.DynamoConnection;
import sam.io.GZipUtils;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;
import sam.sql.JDBCHelper;

public class RssOwlUrlProcessor {
	private static final Path selfDir = MyUtilsPath.selfDir().resolve("scrapper");
	static {
		try {
			if (Files.notExists(selfDir))
				Files.createDirectory(selfDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final StringBuilder _join_sb = new StringBuilder();

	private static final class TempTag {
		final Pattern pattern;
		final int id;

		public TempTag(String pattern, int id) {
			this.pattern = Pattern.compile("(?i)\\b\\Q" + pattern + "\\E\\b");
			this.id = id;
		}
	}

	private static String join(Object... args) {
		synchronized (_join_sb) {
			_join_sb.setLength(0);

			for (Object s : args)
				_join_sb.append(s);

			return _join_sb.toString();
		}
	}

	public void invoke(final Path filePath)
			throws IOException, InterruptedException, ClassNotFoundException, SQLException {
		if (Files.notExists(filePath)) {
			System.out.println("file not found: " + filePath);
			return;
		}

		Set<String> sourceUrls = Files.lines(filePath).filter(Checker::isNotEmptyTrimmed).collect(Collectors.toSet());

		if (sourceUrls.isEmpty()) {
			System.out.println(ANSI.green("no urls found in: ") + filePath);
			return;
		}

		invoke0(sourceUrls, filePath);
	}

	public void invoke(Collection<String> sourceUrls)
			throws IOException, InterruptedException, ClassNotFoundException, SQLException {
		invoke0(sourceUrls, null);
	}

	private void invoke0(Collection<String> sourceUrls, Path filePath)
			throws IOException, InterruptedException, ClassNotFoundException, SQLException {
		if (sourceUrls.isEmpty()) {
			System.out.println(ANSI.red("no urls specified"));
			return;
		}

		final String sql_db = System2.lookup("SQL_DB_PATH");
		if (sql_db == null)
			throw new IllegalStateException("\"SQL_DB_PATH\" env not set");
		if (!new File(sql_db).exists())
			throw new FileNotFoundException("\"SQL_DB_PATH\" not found at: " + sql_db);

		System.out.println("size: " + sourceUrls.size());

		Path cached_skip_path = selfDir.resolve("cached_skip");
		Set<String> skipStrings = Files.exists(cached_skip_path)
				? GZipUtils.lines(cached_skip_path).collect(Collectors.toSet())
				: new HashSet<>();

		if (removeExisting(sourceUrls, skipStrings))
			return;

		Path oldMetasPath = loadedMetasFile();
		SettableLoadedMetas oldMetas = Files.exists(oldMetasPath) ? ObjectReader.read(oldMetasPath) : null;

		final String FAILED = ANSI.red("FAILED: ");
		final String SUCCESS = ANSI.green("SUCCESS: ");
		boolean updated = false;
		TempTag[] allTags;

		SettableLoadedMetas metas;
		try (DynamoConnection db = new DynamoConnection()) {
			metas = SettableLoadedMetas.load(db, Arrays.asList(LoadedMetas.IDS, LoadedMetas.TAGS), oldMetas);
			short[] aws_ids = metas.ids();
			Arrays.sort(aws_ids);

			TempTag[] tempArray = new TempTag[metas.tags().size()];
			int index = 0;
			Set<String> temp = new HashSet<>();
			for (ShortObjectCursor<String> t : metas.tags()) {
				if (temp.add(t.value.toLowerCase().trim()))
					tempArray[index++] = new TempTag(t.value, t.key);
			}
			allTags = Arrays.copyOf(tempArray, index);

			short[] cached_ids = oldMetas.ids() == null ? new short[0] : oldMetas.ids();

			if (!Arrays.equals(aws_ids, cached_ids)) {
				System.out.println(ANSI.yellow("updating meta"));
				List<AttributeValue> missing = new ArrayList<>();

				for (short n : aws_ids) {
					if (Arrays.binarySearch(cached_ids, n) < 0)
						missing.add(value(n));
				}

				if (!missing.isEmpty()) {
					updated = true;
					cached_ids = aws_ids;
					System.out.println(ANSI.yellow("aws-pull-missing: ") + missing.size());
					int n = skipStrings.size();
					List<Map<String, AttributeValue>> result = db.getBatch(TABLE_NAME, ID, missing.stream(),
							Arrays.asList(TITLE, SOURCE, REDIRECT));
					result.stream().flatMap(m -> m.values().stream()).map(attr -> attr.getS())
							.forEach(skipStrings::add);

					System.out.println("skip strings added: " + (n - skipStrings.size()));
				}
			}
		}

		skipStrings.remove(null);

		if (removeExisting(sourceUrls, skipStrings)) {
			if (updated) {
				ObjectWriter.write(oldMetasPath, metas);
				GZipUtils.write(cached_skip_path, skipStrings, CREATE, TRUNCATE_EXISTING);
				System.out.println("updated caches");
			}
			return;
		}

		int progressCount = 1;
		final int size = sourceUrls.size();
		final Set<String> existing = Collections.synchronizedSet(skipStrings);

		int tn = Math.min(Integer.parseInt(System2.lookup("THREAD_COUNT", "4").trim()), sourceUrls.size());
		System.out.println(ANSI.yellow("THREAD_COUNT: ") + tn);
		ExecutorService executor = Executors.newFixedThreadPool(tn);

		List<Callable<Mutableitem>> callables = new ArrayList<>();
		List<String> failed = Collections.synchronizedList(new ArrayList<>(size));

		for (String source : sourceUrls) {
			String progress = ANSI.cyan((progressCount++) + "/" + size + ": ");

			Callable<Mutableitem> task = () -> {
				try {
					Connection con = HttpConnection.connect(source);
					con.timeout(60000);
					con.followRedirects(true);
					con.method(Method.GET);

					Response res = con.execute();
					String redirect = res.url().toString();

					if (source.equals(redirect))
						redirect = null;
					else if (existing.contains(redirect)) {
						System.out.println(join(progress, SKIPPED, "\n  ", source, "\n  ", redirect));
						return null;
					}

					Document doc = res.parse();
					String title = doc.title();

					if (Checker.isEmptyTrimmed(title)) {
						System.out.println(join(progress, FAILED, "(extract title) ", "\n  ", source,
								(redirect == null ? "" : "\n  " + redirect)));
						return null;
					}

					if (existing.contains(title)) {
						existing.add(title);
						if (redirect != null)
							existing.add(redirect);

						System.out.println(join(progress, SKIPPED, "\n  ", source,
								(redirect == null ? "" : "\n  " + redirect), "\n  ", title));
						return null;
					}

					existing.add(source);
					if (redirect != null)
						existing.add(redirect);

					existing.add(title);

					Mutableitem item = new Mutableitem();
					item.setSource(source);
					item.setRedirect(redirect);
					item.setTitle(title);

					Element el = doc.selectFirst("link[rel=\"icon\"]");
					item.setFavicon(el == null ? null : el.absUrl("href"));

					IntArrayList tags = new IntArrayList();

					if (Checker.isNotEmptyTrimmed(title)) {
						for (TempTag t : allTags) {
							if (t.pattern.matcher(title).find())
								tags.add(t.id);
						}
					}

					if (!tags.isEmpty())
						item.setTags(Tag.serialize(Arrays.stream(tags.buffer, 0, tags.size())));
					System.out.println(join(progress, SUCCESS, "\n  ", source,
							(redirect == null ? "" : "\n  " + redirect) + "\n  ", title));

					return item;
				} catch (Throwable e) {
					existing.remove(source);
					System.out.println(join(ANSI.red("failed: "), source, "\n", e));
					failed.add(source);
				}
				return null;
			};

			callables.add(task);
		}

		List<Future<Mutableitem>> tasks = executor.invokeAll(callables);
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.DAYS);

		List<Mutableitem> data = new ArrayList<>(size);
		for (Future<Mutableitem> future : tasks) {
			try {
				Mutableitem d = future.get();
				if (d != null)
					data.add(d);
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		if (!failed.isEmpty()) {
			Path p = (filePath != null ? filePath : Paths.get("."))
					.resolveSibling((filePath != null ? filePath.getFileName() : "") + "-" + System.currentTimeMillis()
							+ "-rssowl-failed.txt");
			Files.write(p, failed, CREATE);
			System.out.println(ANSI.red("failed: ") + failed.size() + ANSI.yellow(" saved: ") + p.toAbsolutePath());
		}

		if (data.isEmpty()) {
			System.out.println(ANSI.red("no data extracted"));
			return;
		}
		System.out.println("\n\n--------------------------------------\n");
		System.out.println("push to aws: " + data.size());

		final String date = LocalDate.now().toString();
		short[] cached_ids = metas.ids();
		int maxId = ArraysUtils.max(cached_ids);
		final int maxIdFixed = maxId;

		System.out.println("maxId: " + maxId);
		System.out.println("\n--------------------new item: ");

		ShortHashSet allIds = new ShortHashSet(cached_ids.length + data.size());

		for (Mutableitem item : data) {
			item.setAddedOn(date);
			int id = ++maxId;
			if (id >= Short.MAX_VALUE)
				throw new IllegalStateException(
						String.format("short id limit exceeded id(%s) >= Short.MAX_VALUE(%s)", id, Short.MAX_VALUE));

			item.setId((short) id);
			allIds.add(item.getId());
			System.out.println(id + ": " + item.getTitle());
		}
		System.out.println("--------------------\n");
		final String sqldb_url = JDBC.PREFIX.concat(sql_db);

		try (java.sql.Connection con = JDBC.createConnection(sqldb_url, new Properties());
				JDBCHelper helper = new JDBCHelper(con);) {
			con.setAutoCommit(false);

			helper.executeUpdate("DELETE FROM "+Mutableitem.SQL_TABLE_NAME+" WHERE id > "+maxIdFixed+";");
			con.commit();
			insert(data, helper);

			helper.prepareStatementBlock("INSERT INTO Tags(id, name) VALUES(?,?);", ps -> {
				ShortHashSet tagsIds = new ShortHashSet();
				helper.iterate("SELECT id FROM Tags", rs -> tagsIds.add(rs.getShort(1)));
				for (ShortObjectCursor<String> t : metas.tags()) {
					if (!tagsIds.contains(t.key)) {
						ps.setInt(1, t.key);
						ps.setString(2, t.value);
						ps.addBatch();
					}
				}
				System.out.println("(sql) updated tags: " + ps.executeBatch().length);
				return null;
			});
			
			ShortHashSet dbIds = new ShortHashSet(cached_ids.length);
			dbIds.addAll(cached_ids);
			helper.iterate("SELECT id FROM Data", rs -> dbIds.remove(rs.getShort(1)));

			try (DynamoConnection db = new DynamoConnection();) {
				if(!dbIds.isEmpty()) {
					System.out.printf("missing from sql: (%s): %s", dbIds.size(), dbIds.toString());
					String[] ids = new String[dbIds.size()];
					int index[] = {0};
					HPPCUtils.forEach(dbIds, n -> {
						ids[index[0]++] = Short.toString(n);
					});
					for (int i = 0; i < ids.length; i+=25) 
						insert(getItems(db, Arrays.copyOfRange(ids, i, Math.min(i + 25, ids.length))), helper);	
				}
				
				List<Map<String, List<WriteRequest>>> temp = db.mapper.batchSave(data).stream()
						.map(FailedBatch::getUnprocessedItems).collect(Collectors.toList());
				int failedCount[] = { 1 };
				if (!temp.isEmpty()) {
					System.out.println(ANSI.red("retry push: ") + "(" + failedCount[0] + ") "
							+ temp.stream().mapToInt(Map::size).sum());
					db.write(temp, list -> {
						System.out.println(ANSI.red("retry push: ") + "(" + failedCount[0] + ") "
								+ list.stream().mapToInt(Map::size).sum());
						failedCount[0]++;
						if (failedCount[0] == 20)
							throw new RuntimeException("exceet retry count: " + 20);
						return true;
					});
				}

				System.out.println("newIds: (" + allIds.size() + ")" + allIds.toString());
				allIds.addAll(cached_ids);
				cached_ids = allIds.toArray();
				Arrays.sort(cached_ids);
				metas.setConnection(db);
				System.out.println(ANSI.yellow("aws-push-all-ids: ") + cached_ids.length);
				long t = System.currentTimeMillis();
				metas.setAllIds(cached_ids);
				System.out.println("DONE: " + (System.currentTimeMillis() - t));

				ObjectWriter.write(oldMetasPath, metas);
				GZipUtils.write(cached_skip_path, skipStrings, CREATE, TRUNCATE_EXISTING);
				System.out.println("updated caches");
				
				con.commit();
			}
		}
	}

	private void insert(List<Mutableitem> data, JDBCHelper helper) throws SQLException {
		helper.prepareStatementBlock(Mutableitem.INSERT_SQL, ps -> {
			for (Mutableitem d : data)
				d.insert(ps);
			System.out.println("(sql) insert Items: " + ps.executeBatch().length);
			return null;
		});
	}

	static final String SKIPPED = ANSI.yellow("SKIPPED: ");

	private static boolean removeExisting(Collection<String> sourceUrls, Set<String> target) {
		if (target.isEmpty())
			return sourceUrls.isEmpty();

		sourceUrls.removeIf(s -> {
			if (target.contains(s)) {
				System.out.println(SKIPPED.concat(s));
				return true;
			}
			return false;
		});

		if (sourceUrls.isEmpty())
			System.out.println(ANSI.green("no urls left to process"));

		return sourceUrls.isEmpty();
	}

	public static void resetLoadedMetasFile() throws IOException {
		Path oldMetasPath = loadedMetasFile();
		try (DynamoConnection db = new DynamoConnection()) {
			LoadedMetas metas = SettableLoadedMetas.load(db, Arrays.asList(LoadedMetas.IDS, LoadedMetas.TAGS), null);
			ObjectWriter.write(oldMetasPath, metas);
		}
		System.out.println("saved: " + oldMetasPath);
	}

	private static Path loadedMetasFile() {
		return selfDir.resolve("RssOwlUrlProcessor.meta.backup");
	}

	public static void getItems(String[] ids) throws IOException {
		if (ids.length == 0)
			throw new IllegalArgumentException("no ids specified");
		try (DynamoConnection db = new DynamoConnection();) {
			List<Mutableitem> list = getItems(db, ids);
			if (list.isEmpty()) {
				System.out.println("no items found");
				return;
			}
			System.out.println("items found: " + ids.length + " -> " + list.size());
			Path p = Paths.get("items-" + System.currentTimeMillis() + ".json");
			try (Writer sink = Files.newBufferedWriter(p, StandardOpenOption.CREATE_NEW)) {
				JSONWriter w = new JSONWriter(sink);
				w.array();
				list.forEach(t -> {
					w.object();
					t.write(w);
					w.endObject();
				});
				w.endArray();
			}
			System.out.println("saved: " + p);
		}
	}

	private static List<Mutableitem> getItems(DynamoConnection db, String[] ids) {
		return db.getBatch(Mutableitem.TABLE_NAME, "id", Arrays.stream(ids).map(s -> new AttributeValue().withN(s)),
				null).stream().map(Mutableitem::new).collect(Collectors.toList());
	}
}
