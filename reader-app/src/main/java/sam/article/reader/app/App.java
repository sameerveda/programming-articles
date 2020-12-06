package sam.article.reader.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.DelayQueue;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codejargon.feather.Feather;
import org.codejargon.feather.Provides;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import sam.article.reader.api.LinkOpener;
import sam.article.reader.api.LoadingIndicatorService;
import sam.article.reader.view.ClassicView;
import sam.fx.helpers.FxCss;
import sam.fx.helpers.FxUtils;
import sam.rss.articles.aws.service.AwsFeedService;
import sam.rss.articles.aws.service.SqliteFeedService;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.Tags;
import sam.rss.articles.sqlite.model.MinimalFeedEntry;
import sam.rss.articles.utils.FeedEntryStatus;
import sam.rss.articles.utils.Ids;

public class App extends Application implements LoadingIndicatorService, ChangeListener<MinimalFeedEntry>, Runnable, LinkOpener {
	private final ObservableList<MinimalFeedEntry> entriesList = FXCollections.observableArrayList();
	private final FilteredList<MinimalFeedEntry> entriesFilteredList = new FilteredList<MinimalFeedEntry>(entriesList);

	private final ListView<MinimalFeedEntry> entriesLV = new ListView<>(entriesFilteredList);
	private final SplitPane content = new SplitPane(entriesLV);
	private final VBox progressIndicator = new VBox(new ProgressIndicator());
	private final StackPane root = new StackPane(content, progressIndicator);

	private final SimpleObjectProperty<FeedEntry> currentEntry = new SimpleObjectProperty<>();

	private static final MinimalFeedEntry THREAD_STOPPER = new MinimalFeedEntry(Integer.MIN_VALUE, null);
	private final DelayQueue<DImpl<MinimalFeedEntry>> entryLoadingQueue = new DelayQueue<>();
	private final Thread entryLoadingThread = new Thread(this);

	private AwsFeedService dynamo;
	private SqliteFeedService sqlite;
	private int[] ids;
	private int page = -1;
	private int size = 100;
	private volatile int entryLoadDelay = 500; // in mills
	private ClassicView articleView;
	private File configPath;
	private Stage stage;

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		progressIndicator.setAlignment(Pos.CENTER);
		progressIndicator.setBackground(FxCss.background(new Color(0.9, 0.9, 0.9, 0.1)));
		currentEntry.addListener((p, o, n) -> {
			if (n != null) {
				articleView.set(n);
			}
		});

		stage.setScene(new Scene(root));
		stage.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), Platform::exit);
		stage.getIcons().add(new Image(ClassLoader.getSystemResourceAsStream("stage-icon.png")));
		stage.getScene().getStylesheets().add(ClassLoader.getSystemResource("css/style.css").toExternalForm());

		try {
			Feather feather = Feather.with(this);
			configPath = new File(Objects.requireNonNull(getString("CONFIG_PATH")));
			File dbFile = new File(getString("SQL_DB_PATH"));
			
			if (!dbFile.exists())
				throw new FileNotFoundException("file not found: " + dbFile);

			dynamo = new AwsFeedService(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build());
			sqlite = new SqliteFeedService(new SQLiteConnection(dbFile), true);
			this.updateStatus();
			this.ids = sqlite.idsFor(FeedEntryStatus.UNREAD, true);
			this.entriesLV.getSelectionModel().selectedItemProperty().addListener(this);
			this.articleView = feather.instance(ClassicView.class);
			this.content.getItems().add(this.articleView);
			setPage(0);
			entryLoadingThread.setDaemon(true);
			entryLoadingThread.start();
			hideLoading();
			if(!entriesLV.getItems().isEmpty())
				Platform.runLater(() -> entriesLV.getSelectionModel().select(0));
		} catch (Exception e) {
			FxUtils.setErrorTa(stage, "failed to init", e.getMessage(), e);
		}

		stage.setTitle("Expand Your Knowledge");
		try {
			if(configPath.exists()) {
				JSONObject json = new JSONObject(new JSONTokener(new FileInputStream(configPath)));
				stage.setX(json.getDouble("x"));
				stage.setY(json.getDouble("y"));
				stage.setWidth(json.getDouble("width"));
				stage.setHeight(json.getDouble("height"));
				JSONArray dividersArray = json.getJSONArray("dividers");
				double[] dividers = new double[dividersArray.length()];
				for (int i = 0; i < dividersArray.length(); i++)
					dividers[i] = dividersArray.getDouble(i);
				Platform.runLater(() -> Platform.runLater(() -> content.setDividerPositions(dividers)));
			}
		} catch (Exception e) {
			e.printStackTrace();	
		}
		stage.show();
		stage.toFront();
	}

	private String getString(String key) {
		return Objects.requireNonNull(Optional.ofNullable(System.getProperty(key)).orElseGet(() -> System.getenv("READER_APP_"+key)));
	}

	private void setPage(int page) {
		this.page = page;
		this.entriesList.setAll(this.sqlite
				.getMinimalEntries(Arrays.copyOfRange(ids, page * size, Math.min(page * size + size, ids.length))));
	}

	private void updateStatus() throws SQLiteException {
		if (dynamo.versionOf(Ids.ID_STATUS) == sqlite.versionOf(Ids.ID_STATUS))
			return;
		System.out.println("Update: STATUSES");
		sqlite.updateStatuses(dynamo.getStatueses());
	}

	// entry loader
	@Override
	public void run() {
		while (true) {
			DImpl<MinimalFeedEntry> d = entryLoadingQueue.poll();
			if(d != null && d.value == THREAD_STOPPER) 
				return;
			if (d == null || !entryLoadingQueue.isEmpty())
				continue;

			System.out.println("LOADING: " + d.value.id + ": " + d.value.title);
			FeedEntry e = dynamo.getEntry(d.value.id);
			Platform.runLater(() -> currentEntry.set(e));
		}
	}

	@Override
	public void changed(ObservableValue<? extends MinimalFeedEntry> observable, MinimalFeedEntry oldValue,
			MinimalFeedEntry newValue) {
		if (newValue != null) {
			entryLoadingQueue.add(new DImpl<>(newValue, entryLoadDelay));
			articleView.set(newValue);
		}

	}

	@Override
	public void stop() throws Exception {
		if (entryLoadingThread != null) {
			entryLoadingQueue.add(new DImpl<MinimalFeedEntry>(THREAD_STOPPER, 100));
			entryLoadingThread.interrupt();
			entryLoadingThread.join();
		}

		if (sqlite != null)
			sqlite.close();
		
		JSONObject config = new JSONObject();
		config.put("x", this.stage.getX());
		config.put("y", this.stage.getY());
		config.put("width", this.stage.getWidth());
		config.put("height", this.stage.getHeight());
		double[] d = this.content.getDividerPositions();
		config.put("dividers", Arrays.asList(d[0], 1 - d[0]));
		try(Writer w = new FileWriter(configPath)) {
			config.write(w);
		}
		System.out.println("STOP");
	}

	@Override
	public void showLoading() {
		if (!root.getChildren().contains(progressIndicator))
			root.getChildren().add(progressIndicator);
	}

	@Override
	public void hideLoading() {
		if (root.getChildren().contains(progressIndicator))
			root.getChildren().remove(progressIndicator);
	}

	@Provides
	@Named("root-view")
	public StackPane root() {
		return root;
	}

	@Provides
	@Singleton
	public Tags tags() {
		System.out.println("get tags");
		return dynamo.getTags();
	}

	@Provides
	public LoadingIndicatorService loadingIndicatorService() {
		return this;
	}

	@Provides
	public LinkOpener linkOpener() {
		return this;
	}

	@Override
	public void openLink(String link) {
		getHostServices().showDocument(link);
	}

}
