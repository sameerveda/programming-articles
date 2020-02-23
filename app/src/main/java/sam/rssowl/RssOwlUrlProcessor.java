
package sam.rssowl;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static programming.articles.model.dynamo.ConstantDataItem.ADDED_ON;
import static programming.articles.model.dynamo.ConstantDataItem.FAVICON;
import static programming.articles.model.dynamo.ConstantDataItem.ID;
import static programming.articles.model.dynamo.ConstantDataItem.REDIRECT;
import static programming.articles.model.dynamo.ConstantDataItem.SOURCE;
import static programming.articles.model.dynamo.ConstantDataItem.TABLE_NAME;
import static programming.articles.model.dynamo.ConstantDataItem.TITLE;
import static programming.articles.model.dynamo.DataItem.TAGS;
import static sam.full.access.dynamodb.DynamoConnection.value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ShortHashSet;
import com.carrotsearch.hppc.cursors.ShortObjectCursor;

import programming.articles.model.Tag;
import programming.articles.model.dynamo.DataItem;
import programming.articles.model.dynamo.LoadedMetas;
import programming.articles.model.dynamo.SettableLoadedMetas;
import sam.collection.ArraysUtils;
import sam.console.ANSI;
import sam.full.access.dynamodb.DynamoConnection;
import sam.io.GZipUtils;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.myutils.Checker;
import sam.myutils.MyUtilsPath;
import sam.myutils.System2;

public class RssOwlUrlProcessor {
	private static final Path selfDir = MyUtilsPath.selfDir().resolve("scrapper");
	static {
		try {
			if(Files.notExists(selfDir))
				Files.createDirectory(selfDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final StringBuilder _join_sb = new StringBuilder();

	private static String join(Object... args) {
		synchronized (_join_sb) {
			_join_sb.setLength(0);

			for (Object s : args)
				_join_sb.append(s);

			return _join_sb.toString();	
		}
	}

	public void invoke(final Path filePath) throws IOException, InterruptedException, ClassNotFoundException {
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

	public void invoke(Collection<String> sourceUrls) throws  IOException, InterruptedException, ClassNotFoundException {
		invoke0(sourceUrls, null);
	}

	private void invoke0(Collection<String> sourceUrls, Path filePath)
			throws  IOException, InterruptedException, ClassNotFoundException {
		if (sourceUrls.isEmpty()) {
			System.out.println(ANSI.red("no urls specified"));
			return;
		}

		System.out.println("size: " + sourceUrls.size());

		Path cached_skip_path = selfDir.resolve("cached_skip");
		Set<String> skipStrings = Files.exists(cached_skip_path) ? GZipUtils.lines(cached_skip_path).collect(Collectors.toSet()) : new HashSet<>();

		if (removeExisting(sourceUrls, skipStrings))
			return;

		Path oldMetasPath = selfDir.resolve("RssOwlUrlProcessor.meta.backup");
		SettableLoadedMetas oldMetas = Files.exists(oldMetasPath) ? ObjectReader.read(oldMetasPath) : null;

		final String FAILED = ANSI.red("FAILED: ");
		final String SUCCESS = ANSI.green("SUCCESS: ");
		boolean updated = false;
		Tag[] allTags;

		SettableLoadedMetas metas;
		try (DynamoConnection db = new DynamoConnection()) {
			metas = SettableLoadedMetas.load(db, Arrays.asList(LoadedMetas.IDS, LoadedMetas.TAGS), oldMetas);
			short[] aws_ids = metas.ids();
			Arrays.sort(aws_ids);

			allTags = new Tag[metas.tags().size()];
			int index = 0;
			for (ShortObjectCursor<String> t : metas.tags())
				allTags[index++] = new Tag(t.key, t.value);
			
			short[] cached_ids = oldMetas.ids() == null ? new short[0] : oldMetas.ids();

			if (!Arrays.equals(aws_ids, cached_ids)) {
				System.out.println(ANSI.yellow("updating meta"));
				List<AttributeValue> missing = new ArrayList<>();

				for (short n : aws_ids) {
					if(Arrays.binarySearch(cached_ids, n) < 0)
						missing.add(value(n));
				}

				if (!missing.isEmpty()) {
					updated = true;
					cached_ids = aws_ids;
					System.out.println(ANSI.yellow("aws-pull-missing: ") + missing.size());
					int n = skipStrings.size();
					List<Map<String, AttributeValue>> result = db.getBatch(TABLE_NAME, ID, missing.stream(), Arrays.asList(TITLE, SOURCE, REDIRECT));
					result.stream()
					.flatMap(m -> m.values().stream())
					.map(attr -> attr.getS())
					.forEach(skipStrings::add);
					
					System.out.println("skip strings added: "+(n - skipStrings.size()));
				}
			}
		}

		skipStrings.remove(null);

		if (removeExisting(sourceUrls, skipStrings)) {
			if(updated) {
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

		List<Callable<Map<String, AttributeValue>>> callables = new ArrayList<>();
		List<String> failed = Collections.synchronizedList(new ArrayList<>(size));

		for (String source : sourceUrls) {
			String progress = ANSI.cyan((progressCount++) + "/" + size + ": ");

			Callable<Map<String, AttributeValue>> task = () -> {
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

					Map<String, AttributeValue> item = new HashMap<>();
					put(item, SOURCE, source);
					put(item, REDIRECT, redirect);
					put(item, TITLE, title);

					Element el = doc.selectFirst("link[rel=\"icon\"]");
					put(item, FAVICON, el == null ? null : el.absUrl("href"));

					IntArrayList tags = new IntArrayList();

					if (Checker.isNotEmptyTrimmed(title)) {
						String titleLowercase = title.toLowerCase();
						for (Tag t : allTags) {
							if (titleLowercase.contains(t.getLowercased()))
								tags.add(t.getId());
						}
					}

					if (!tags.isEmpty())
						put(item, TAGS, Tag.serialize(Arrays.stream(tags.buffer, 0, tags.size())));

					System.out.println(join(progress, SUCCESS, "\n  ", source, (redirect == null ? "" : "\n  " + redirect) + "\n  ", title));

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

		List<Future<Map<String, AttributeValue>>> tasks = executor.invokeAll(callables);
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.DAYS);

		List<Map<String, AttributeValue>> data = new ArrayList<>(size);

		for (Future<Map<String, AttributeValue>> future : tasks) {
			try {
				Map<String, AttributeValue> d = future.get();
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
		System.out.println("push to aws: "+data.size());

		final String date = LocalDate.now().toString();
		short[] cached_ids = metas.ids();
		int maxId = ArraysUtils.max(cached_ids);

		System.out.println("maxId: "+maxId);
		System.out.println("\n--------------------new item: ");
		
		ShortHashSet allIds = new ShortHashSet(cached_ids.length + data.size());

		for (Map<String, AttributeValue> item : data) {
			put(item, ADDED_ON, date); 
			int id = ++maxId;
			if(id >= Short.MAX_VALUE)
				throw new IllegalStateException(String.format("short id limit exceeded id(%s) >= Short.MAX_VALUE(%s)", id, Short.MAX_VALUE));
			
			item.put(ID, value(id));
			allIds.add((short)id);
			System.out.println(id+": "+item.get(TITLE).getS());
		}

		System.out.println("--------------------\n");

		try(DynamoConnection db = new DynamoConnection()) {
			if(data.size() == 1) {
				db.putItem(DataItem.TABLE_NAME, data.get(0));
			} else {
				int failedCount[] = {0}; 
				db.putItems(DataItem.TABLE_NAME, data, list -> {
					System.out.println(ANSI.red("retry push: ")+"("+failedCount[0]+") " + list.stream().mapToInt(Map::size).sum());
					failedCount[0]++;
					if(failedCount[0] == 20)
						throw new RuntimeException("exceet retry count: "+20);
					return true;
				});
			}

			System.out.println("newIds: ("+allIds.size()+")"+allIds.toString());
			allIds.addAll(cached_ids);
			System.out.println(ANSI.yellow("aws-pull-all-ids: ")+allIds.size());
			cached_ids = allIds.toArray();
			Arrays.sort(cached_ids);
			metas.setConnection(db);
			metas.setAllIds(cached_ids);

			ObjectWriter.write(oldMetasPath, metas);
			GZipUtils.write(cached_skip_path, skipStrings, CREATE, TRUNCATE_EXISTING);
			System.out.println("updated caches");
		}
	}

	private void put(Map<String, AttributeValue> item, String key, String value) {
		if(Checker.isEmpty(value))
			return ;
		item.put(key, value(value));
		
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



}
