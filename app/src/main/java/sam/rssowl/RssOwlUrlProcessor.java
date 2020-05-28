
package sam.rssowl;

import static java.nio.file.StandardOpenOption.CREATE;
import static programming.articles.model.DataItemMeta.DYNAMO_TABLE_NAME;
import static sam.full.access.dynamodb.DynamoConnection.value;
import static sam.sql.JDBCHelper.placeholders;
import static sam.sql.sqlite.Sqlite4javaHelper.STRING_BINDER;
import static sam.sql.sqlite.Sqlite4javaHelper.getInt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.almworks.sqlite4java.SQLiteException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectScatterMap;
import com.carrotsearch.hppc.ObjectIntScatterMap;
import com.carrotsearch.hppc.ShortArrayList;
import com.carrotsearch.hppc.ShortByteMap;
import com.carrotsearch.hppc.ShortByteScatterMap;
import com.carrotsearch.hppc.ShortContainer;
import com.carrotsearch.hppc.ShortIntScatterMap;
import com.carrotsearch.hppc.ShortObjectMap;
import com.carrotsearch.hppc.ShortObjectScatterMap;
import com.carrotsearch.hppc.ShortScatterSet;

import programming.articles.model.DataStatus;
import programming.articles.model.LoadedMetas;
import programming.articles.model.SettableLoadedMetas;
import programming.articles.model.Tag;
import sam.collection.ArraysUtils;
import sam.console.ANSI;
import sam.full.access.dynamodb.DynamoConnection;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;
import sam.sql.sqlite.Sqlite4javaHelper;

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

	private File sql_db;
	private Path loadedMetasFile;

	private void init() throws FileNotFoundException {
		if (this.sql_db != null)
			return;
		String sql_db = System2.lookup("SQL_DB_PATH");
		if (sql_db == null)
			throw new IllegalStateException("\"SQL_DB_PATH\" env not set");
		this.sql_db = new File(sql_db);
		if (!this.sql_db.exists())
			throw new FileNotFoundException("\"SQL_DB_PATH\" not found at: " + sql_db);

		this.loadedMetasFile = selfDir.resolve("RssOwlUrlProcessor.meta.backup");
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

	public void sync() throws SQLiteException, ClassNotFoundException, IOException {
		init();
		try (Sqlite4javaHelper sqlite = new Sqlite4javaHelper(sql_db, false);
				DynamoConnection con = new DynamoConnection()) {
			sync(con, sqlite);
		}
	}

	private SettableLoadedMetas sync(DynamoConnection dynamoDb, Sqlite4javaHelper sqliteDb)
			throws SQLiteException, IOException, ClassNotFoundException {
		SettableLoadedMetas metas = SettableLoadedMetas.load(dynamoDb, Arrays.asList(LoadedMetas.IDS, LoadedMetas.TAGS, LoadedMetas.IDS_STATUS), loadOldMetas());
		boolean updated = false;
		metas.setConnection(dynamoDb);
		{
			ShortObjectMap<String> db = new ShortObjectScatterMap<>();
			db.putAll(metas.tags());
			ShortObjectMap<String> sqlite = new ShortObjectScatterMap<>();

			sqliteDb.iterate("SELECT id, name FROM Tags", st -> {
				short id = (short) st.columnInt(0);
				if (db.get(id).equals(st.columnString(1)))
					db.remove(id);
				else
					sqlite.put(id, st.columnString(1));
			});

			if (!db.isEmpty() || !sqlite.isEmpty()) {
				System.out.println(ANSI.yellow("NEW TAGS ------------------"));
				printTags("DYNAMO", db);
				printTags("SQLITE", sqlite);

				boolean[] conflicts = { false };
				HPPCUtils.forEach(db, (s, t) -> {
					if (sqlite.get(s) != null) {
						if (!conflicts[0]) {
							conflicts[0] = true;
							System.out.println(ANSI.red("Found Tags conflicts"));
						}
						System.out.printf("%5s %s <-> %s\n", s, t, sqlite.get(s));
					}
				});
				if (conflicts[0])
					throw new IllegalStateException("tags conflicts found");

				if (!sqlite.isEmpty()) {
					System.out.println("PUSH TO: Dynamo, Tags: " + sqlite);
					metas.addTags(sqlite);
					updated = true;
				}
				if (!db.isEmpty()) {
					System.out.println("PULL TO: Sqlite, Tags: " + db);
					sqliteDb.batch("INSERT INTO Tags(id, name) VALUES(?,?);", db, (st, item) -> {
						st.bind(1, item.key);
						st.bind(2, item.value);
					});
				}
			}
		}

		{
			ShortScatterSet dynamo = new ShortScatterSet();
			dynamo.addAll(metas.ids());
			ShortScatterSet sqlite = new ShortScatterSet();

			sqliteDb.iterate("SELECT id from Data", st -> {
				if (!dynamo.remove((short) st.columnInt(0)))
					sqlite.add((short) st.columnInt(0));
			});

			pullItemsToSqlite(dynamo, dynamoDb, sqliteDb);
			if (pushItemsToDynamo(sqlite, sqliteDb, dynamoDb)) {
				updated = true;
				updatedAllIds(metas, sqliteDb);
			}
		}

		{
			ScanRequest req = new ScanRequest(DYNAMO_TABLE_NAME).withProjectionExpression("id, version")
					.withFilterExpression("attribute_exists(version) AND version > :version")
					.addExpressionAttributeValuesEntry(":version", value(0));

			ShortIntScatterMap dynamo = new ShortIntScatterMap();
			dynamoDb.collect(req).forEach(
					m -> dynamo.put(Short.parseShort(m.get("id").getN()), Integer.parseInt(m.get("version").getN())));

			ShortArrayList sqlite = new ShortArrayList();

			sqliteDb.iterate("SELECT id, version FROM Data WHERE id IN" + ArraysUtils.toString(dynamo.keys().toArray()),
					st -> {
						short id = (short)st.columnInt(0);
						int version = st.columnInt(1);
						if(dynamo.get(id) > version)
							sqlite.add(id);
						if (dynamo.get(id) >= version)
							dynamo.remove(id);
						else 
							sqlite.add(id);
					});

			if (!sqlite.isEmpty()) {
				System.out.println("PULL TO SQLITE: version changed: " + sqlite.size() + ": " + sqlite);
				sqliteDb.execute("DELETE FROM Data WHERE id IN" + ArraysUtils.toString(sqlite.toArray()), null);
				pullItemsToSqlite(sqlite, dynamoDb, sqliteDb);
				updated = true;
			}

			if(!dynamo.isEmpty()) {
				System.out.println("PUSH TO DYNAMO: version changed: " + dynamo.size() + ": " + dynamo.keys().toString());
				pushItemsToDynamo(dynamo.keys(), sqliteDb, dynamoDb);
				ShortByteMap statues = new ShortByteScatterMap();
				sqliteDb.iterate("SELECT id, status FROM Data WHERE id IN"+ArraysUtils.toString(dynamo.keys().toArray()), rs -> statues.put((short)rs.columnInt(0), DataStatus.parse(rs.columnString(1)).byteValue()));
				if(!statues.isEmpty()) {
					System.out.println("Update Status: "+statues);
					metas.addStatusIds(statues);					
				}
				updated = true;
			}
		}

		if (updated)
			updateMetasFile(metas);
		else {
			System.out.println(ANSI.createBanner("UPDATE TO DATE"));
		}

		return metas;
	}

	private static void updatedAllIds(SettableLoadedMetas metas, Sqlite4javaHelper sqliteDb) throws SQLiteException {
		ShortArrayList set = new ShortArrayList();
		sqliteDb.iterate("SELECT id from Data", st -> set.add((short) st.columnInt(0)));
		short[] array = set.toArray();
		Arrays.sort(array);
		metas.setAllIds(array);
		System.out.println(ANSI.yellow("aws-push-all-ids: ") + array.length);
	}

	private static boolean pushItemsToDynamo(ShortContainer ids, Sqlite4javaHelper sqlite, DynamoConnection db)
			throws SQLiteException {
		if (ids.isEmpty())
			return false;
		System.out.println("PUSH TO: Dynamo, Data: " + ids.size() + ": " + ids);
		IntObjectScatterMap<String> dates = new IntObjectScatterMap<>();
		sqlite.iterate("SELECT id, _date  Dates", rs -> dates.put(rs.columnInt(0), rs.columnString(1)));
		IntObjectScatterMap<String> favicons = new IntObjectScatterMap<>();
		sqlite.iterate("SELECT id, url Favicons", rs -> favicons.put(rs.columnInt(0), rs.columnString(1)));
		pushDataItems(sqlite.collectToList("SELECT * FROM Data WHERE id IN" + ArraysUtils.toString(ids.toArray()),
				rs -> new Mutableitem(rs, dates, favicons)), db);
		return true;
	}

	private static void pullItemsToSqlite(ShortContainer ids, DynamoConnection db, Sqlite4javaHelper sqlite)
			throws SQLiteException {
		if (ids.isEmpty())
			return;
		System.out.println("PULL TO: Sqlite, Data: " + ids.size() + ": " + ids);
		insertDataItems(getItems(ids.toArray(), db), sqlite);
	}

	private static void printTags(String title, ShortObjectMap<String> map) {
		System.out.println(ANSI.green(title));
		HPPCUtils.forEach(map, (s, t) -> System.out.printf("%5s %s\n", s, t));
		System.out.println();
	}

	private static String join(Object... args) {
		synchronized (_join_sb) {
			_join_sb.setLength(0);

			for (Object s : args)
				_join_sb.append(s);

			return _join_sb.toString();
		}
	}

	public void extract(final Path filePath)
			throws IOException, InterruptedException, ClassNotFoundException, SQLiteException {
		if (Files.notExists(filePath)) {
			System.out.println("file not found: " + filePath);
			return;
		}

		Set<String> sourceUrls = Files.lines(filePath).filter(Checker::isNotEmptyTrimmed).collect(Collectors.toSet());

		if (sourceUrls.isEmpty()) {
			System.out.println(ANSI.green("no urls found in: ") + filePath);
			return;
		}

		extract0(sourceUrls, filePath);
	}

	public void extract(Collection<String> sourceUrls)
			throws IOException, InterruptedException, ClassNotFoundException, SQLiteException {
		extract0(sourceUrls, null);
	}

	private void extract0(Collection<String> sourceUrls, Path filePath)
			throws IOException, InterruptedException, ClassNotFoundException, SQLiteException {
		if (sourceUrls.isEmpty()) {
			System.out.println(ANSI.red("no urls specified"));
			return;
		}

		init();
		System.out.println("size: " + sourceUrls.size());
		Set<String> skipStrings = new HashSet<>();
		final TempTag[] allTags;
		SettableLoadedMetas metas;

		try (Sqlite4javaHelper sqlite = new Sqlite4javaHelper(sql_db, false);
				DynamoConnection db = new DynamoConnection();) {
			metas = sync(db, sqlite);
			sqlite.iterate("SELECT title, source, redirect FROM Data", st -> {
				skipStrings.add(st.columnString(0));
				skipStrings.add(st.columnString(1));
				skipStrings.add(st.columnString(2));
			});

			if (removeExisting(sourceUrls, skipStrings))
				return;

			skipStrings.remove(null);
			allTags = sqlite.stream("SELECT id, name FROM Tags", st -> new TempTag(st.columnString(1), st.columnInt(0))).toArray(TempTag[]::new);
		}

		List<String> failed = Collections.synchronizedList(new ArrayList<>(sourceUrls));
		List<Mutableitem> data = startExtract(sourceUrls, Collections.synchronizedSet(skipStrings), allTags, failed);

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

		try (Sqlite4javaHelper sqlite = new Sqlite4javaHelper(sql_db, false)) {

			final String addedOn = LocalDate.now().toString();
			int maxId = sqlite.getFirst("SELECT max(id) FROM Data", null, st -> st.columnInt(0)) + 1;

			System.out.println("maxId: " + (maxId - 1));
			System.out.println("\n--------------------new item: ");

			for (Mutableitem item : data) {
				item.setAddedOn(addedOn);
				int id = maxId++;
				if (id >= Short.MAX_VALUE)
					throw new IllegalStateException(String
							.format("short id limit exceeded id(%s) >= Short.MAX_VALUE(%s)", id, Short.MAX_VALUE));

				item.setId((short) id);
				System.out.println(id + ": " + item.getTitle());
			}
			System.out.println("--------------------\n");

			insertDataItems(data, sqlite);

			try (DynamoConnection db = new DynamoConnection();) {
				pushDataItems(data, db);
				System.out.println("newIds: (" + data.size() + ")"
						+ Arrays.toString(data.stream().mapToInt(m -> m.getId()).toArray()));
				metas.setConnection(db);
				updatedAllIds(metas, sqlite);
				updateMetasFile(metas);
			}
		}
	}

	private List<Mutableitem> startExtract(Collection<String> sourceUrls, Set<String> existing, TempTag[] allTags,
			List<String> failed) throws InterruptedException {
		int progressCount = 1;
		final int size = sourceUrls.size();

		int tn = Math.min(Integer.parseInt(System2.lookup("THREAD_COUNT", "4").trim()), sourceUrls.size());
		System.out.println(ANSI.yellow("THREAD_COUNT: ") + tn);
		ExecutorService executor = Executors.newFixedThreadPool(tn);

		List<Callable<Mutableitem>> callables = new ArrayList<>();
		final String FAILED = ANSI.red("FAILED: ");
		final String SUCCESS = ANSI.green("SUCCESS: ");

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

		return tasks.stream().map(f -> {
			try {
				return f.get();
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private static void pushDataItems(List<Mutableitem> data, DynamoConnection db) {
		List<Map<String, List<WriteRequest>>> temp = db.mapper.batchSave(data).stream()
				.map(FailedBatch::getUnprocessedItems).collect(Collectors.toList());
		int failedCount[] = { 1 };
		if (!temp.isEmpty()) {
			System.out.println(
					ANSI.red("retry push: ") + "(" + failedCount[0] + ") " + temp.stream().mapToInt(Map::size).sum());
			db.write(temp, list -> {
				System.out.println(ANSI.red("retry push: ") + "(" + failedCount[0] + ") "
						+ list.stream().mapToInt(Map::size).sum());
				failedCount[0]++;
				if (failedCount[0] == 20)
					throw new RuntimeException("exceet retry count: " + 20);
				return true;
			});
		}
	}

	private void updateMetasFile(SettableLoadedMetas metas) throws IOException {
		ObjectWriter.write(this.loadedMetasFile, metas);
		System.out.println("updated caches");
	}

	private SettableLoadedMetas loadOldMetas() throws ClassNotFoundException, IOException {
		return Files.exists(this.loadedMetasFile) ? ObjectReader.read(this.loadedMetasFile) : null;
	}

	private static void insertDataItems(List<Mutableitem> data, Sqlite4javaHelper sqlite) throws SQLiteException {
		ToIntFunction<String> dates = collect(data, sqlite, s -> Stream.of(s.getAddedOn(), s.getUpdatedOn()), "Dates",
				"_date");
		ToIntFunction<String> favicons = collect(data, sqlite, s -> Stream.of(s.getFavicon()), "Favicons", "url");
		int count = sqlite.batch(Mutableitem.INSERT_SQL, data, (st, d) -> d.insert(st, dates, favicons));
		System.out.println("commit Data: " + count);
	}

	private static ToIntFunction<String> collect(List<Mutableitem> data, Sqlite4javaHelper sqlite,
			Function<Mutableitem, Stream<String>> mapper, String tableName, String valueField) throws SQLiteException {
		Set<String> find = data.stream().flatMap(mapper).filter(Checker::isNotEmptyTrimmed).collect(Collectors.toSet());
		ObjectIntScatterMap<String> foundMap = new ObjectIntScatterMap<>();
		Sqlite4javaHelper.iterate(
				sqlite.bindAll("SELECT id, " + valueField + " FROM " + tableName + " where " + valueField + " in("
						+ placeholders(find.size()) + ");", find, STRING_BINDER),
				st -> foundMap.put(st.columnString(1), st.columnInt(0)));
		if (find.size() != foundMap.size()) {
			int[] max = { sqlite.getFirst("SELECT max(id) FROM " + tableName, null, getInt(0)) + 1 };
			find.removeIf(foundMap::containsKey);
			sqlite.batch("INSERT INTO " + tableName + "(id, " + valueField + ") VALUES(?,?)", find, (st, e) -> {
				int id = max[0]++;
				st.bind(1, id);
				st.bind(2, e);
				foundMap.put(e, id);
			});
			System.out.println("insert " + tableName + ": " + find.size());
		}
		return foundMap::get;
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

	public void resetLoadedMetasFile() throws IOException {
		init();
		try (DynamoConnection db = new DynamoConnection()) {
			LoadedMetas metas = SettableLoadedMetas.load(db, Arrays.asList(LoadedMetas.IDS, LoadedMetas.TAGS), null);
			ObjectWriter.write(this.loadedMetasFile, metas);
		}
		System.out.println("saved: " + this.loadedMetasFile);
	}

	public static List<Mutableitem> getItems2(String[] ids) throws IOException {
		short[] array = new short[ids.length];
		for (int i = 0; i < array.length; i++)
			array[i] = Short.parseShort(ids[i]);
		try (DynamoConnection db = new DynamoConnection()) {
			return getItems(array, db);
		}
	}

	private static List<Mutableitem> getItems(short[] ids, DynamoConnection db) {
		return db
				.getBatch(DYNAMO_TABLE_NAME, "id",
						ArraysUtils.stream(ids).mapToObj(s -> new AttributeValue().withN(String.valueOf(s))), null)
				.stream().map(Mutableitem::new).collect(Collectors.toList());
	}
}
