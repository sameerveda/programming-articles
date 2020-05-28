package sam.viewer.app;

import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.isDirectory;
import static java.util.Optional.ofNullable;
import static sam.myutils.MyUtilsPath.SELF_DIR;
import static sam.myutils.System2.lookup;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.inject.Named;

import org.codejargon.feather.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.HostServices;
import javafx.stage.Stage;
import programming.articles.app.providers.DefaultProviders;
import sam.api.TagsAdder;
import sam.api.UrlOpener;
import sam.di.Injector;
import sam.full.access.dynamodb.DynamoConnection;
import sam.nopkg.EnsureSingleton;
import sam.reference.WeakAndLazy;
import sam.viewer.TagsAdderImpl;

class Providers extends DefaultProviders implements UrlOpener {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	private static final Logger logger = LoggerFactory.getLogger(Providers.class);

	{ SINGLETON.init(); }
	
	private static final Path CACHE_DIR;
	
	static {
		CACHE_DIR = ofNullable(lookup("sam.cache.dir")).map(Paths::get).orElseGet(() -> ofNullable(SELF_DIR).map(s -> s.resolve("cache")).orElse(null));
		logger.debug("cacheDir: {}", CACHE_DIR);

		try {
			if(!isDirectory(CACHE_DIR))
				createDirectory(CACHE_DIR);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private HostServices hostServices;
	private Stage stage;
	
	public Providers() throws Exception {
		super();
	}
	
	@Override
	protected DynamoConnection openConnection() throws Exception {
		return new DynamoConnection();
	}

	public void setHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}
	
	@Override
	protected void cacheInit() throws IOException {
	}
	
	@Override
	protected <E extends Comparable<?>, F> Map<E, F> cacheFor(String name) {
		return cache.fileDb.treeMap(name);
	}
	
	@Override
	protected Path loadedMetasPath() {
		return CACHE_DIR.resolve("app.loadedmeta");
	}
	
	@Override
	public void close() throws Exception {
		super.close();
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void openUrl(String url) {
		if (url != null)
			hostServices.showDocument(url);
	}

	@Provides
	UrlOpener urlOpener() {
		return this;
	}

	@Provides
	@Named(App.STAGE_KEY)
	public Stage stage() {
		return stage;
	}

	private final WeakAndLazy<TagsAdderImpl> tagsAdder = new WeakAndLazy<TagsAdderImpl>(
			() -> Injector.getInstance().instance(TagsAdderImpl.class));

	@Provides
	public TagsAdder tagsAdder() {
		return tagsAdder.get();
	}
}
