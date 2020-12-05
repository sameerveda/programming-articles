import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.almworks.sqlite4java.SQLiteException;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.ShortArrayList;

import programming.articles.model.DataStatus;
import programming.articles.model.LoadedMetas;
import programming.articles.model.MetaReset;
import sam.collection.ArraysUtils;
import sam.console.ANSI;
import sam.full.access.dynamodb.DynamoConnection;
import sam.myutils.Checker;
import sam.myutils.HPPCUtils;
import sam.myutils.LoggerUtils;
import sam.myutils.System2;
import sam.rssowl.RssOwlUrlProcessor;

public class Main {

	public static void main(String[] args) throws Exception {
		LoggerUtils.lookupEnableSlf4jSimple();
		
		if (args.length == 0) {
			System.out.println(ANSI.red("no command specified"));
			printUsage();
			return;
		}

		switch (args[0].toLowerCase()) {
			case "-f":
			case "--file":
				byFiles(Arrays.copyOfRange(args, 1, args.length));
				break;
			case "-c":
			case "--clipboard":
				byClipboard();
				break;
			case "view":
				throw new RuntimeException("no implemented");
				// TODO Application.launch(App.class, args);
				// break;
			case "meta-reset":
				try (DynamoConnection con = new DynamoConnection()) {
					new MetaReset(con).run();
				}
				break;
			case "sync": 
				new RssOwlUrlProcessor().sync();
				break;
			case "meta-print":
				printMeta(System2.lookupInt("sam.line.width", 80));
				break;
			case "get-items":
				RssOwlUrlProcessor.getItems2(Arrays.copyOfRange(args, 1, args.length));
				break;
			default:
				System.out.println(ANSI.red("failed  command: ") + Arrays.toString(args));
				printUsage();
				break;
		}
	}

	private static void printMeta(final int width) throws IOException {
		try (DynamoConnection con = new DynamoConnection()) {
			System.out.println("loading");
			LoadedMetas m = LoadedMetas.load(con, null);
			StringBuilder sb = new StringBuilder();
			Formatter fm = new Formatter(sb);
			fm.format("%" + width / 2 + "s\n", "------------- IDS -------------");
			int min = ArraysUtils.min(m.ids());
			int max = ArraysUtils.max(m.ids());

			sb.append("min: ").append(min).append('\n').append("max: ").append(max).append('\n').append("holes: ");

			Arrays.sort(m.ids());
			IntStream.range(min, max + 1)
			.filter(i -> Arrays.binarySearch(m.ids(), (short) i) < 0)
			.sorted()
			.forEach(i -> sb.append(i).append(", "));

			sb.append("\n\n");
			fm.format("%" + width / 2 + "s\n", "------------- ID STATUS -------------");
			IntObjectHashMap<ShortArrayList> statuses = new IntObjectHashMap<>();
			DataStatus[] statuesArray = DataStatus.values();
			ArraysUtils.forEach(statuesArray, d -> statuses.put(d.ordinal(), new ShortArrayList()));

			HPPCUtils.forEach(m.idStatusMap(), (s, t) -> statuses.get(t).add(s));
			HPPCUtils.forEach(statuses, (status, list) -> {

				if (!list.isEmpty()) {
					Arrays.sort(list.buffer, 0, list.elementsCount);
					sb.append(statuesArray[status]).append('(').append(list.size()).append("): ")
					.append(list.toString()).append('\n');
				}

			});

			sb.append("\n\n------------------------ TAGS ------------------------\n");
			int[] size = { 0 };
			HPPCUtils.forEach(m.tags(), (s, t) -> size[0] = Math.max(size[0], t.length()));
			String format2 = "%-" + (size[0] + 3) + "s";

			short[] keys = m.tags().keys().toArray();
			Arrays.sort(keys);

			int len = sb.length();			
			for (short s : keys) {
				int n = sb.length();
				fm.format(format2, s+"="+m.tags().get(s));
				if (sb.length() - len > width) {
					sb.setLength(n);
					sb.append('\n');
					len = sb.length();
					fm.format(format2, s+"="+m.tags().get(s));
				}	
			}
			System.out.println(sb);
			fm.close();
		}
	}

	private static void printUsage() {
		System.out.println(ANSI.yellow("usage"));
		System.out.println("-f, --file [FILE] start extractor load urls from FILE");
		System.out.println("-c, --clipboard   start extractor load urls from CLIPBOARD");
		System.out.println("view              start app");
		System.out.println("meta-reset        reset meta");
		System.out.println("meta-print        print meta");
	}

	private static void byClipboard()
			throws SQLiteException, IOException, UnsupportedFlavorException, InterruptedException, ClassNotFoundException {
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String s = (String) clip.getData(DataFlavor.stringFlavor);
		if (s == null)
			System.out.println(ANSI.red("no data found in clipboard"));
		else {
			List<String> array = Pattern.compile("\r?\n").splitAsStream(s).filter(Checker::isNotEmptyTrimmed)
					.collect(Collectors.toList());

			new RssOwlUrlProcessor().extract(array);
		}
	}

	private static void byFiles(String[] files) throws IOException, SQLiteException, InterruptedException, ClassNotFoundException {
		if (files.length == 0)
			System.out.println(ANSI.red("no files specified"));
		else {
			for (String f : files)
				new RssOwlUrlProcessor().extract(Paths.get(f));
		}
	}

}
