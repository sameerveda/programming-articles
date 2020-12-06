package sam.article.reader.view;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import sam.article.reader.api.LinkOpener;
import sam.fx.clipboard.FxClipboard;
import sam.fx.helpers.FxCss;
import sam.fx.popup.FxPopupShop;
import sam.myutils.Checker;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.Tags;
import sam.rss.articles.sqlite.model.MinimalFeedEntry;
import sam.rss.articles.utils.FeedEntryStatus;

public class ClassicView extends VBox implements Closeable {
	private final Text id = copiable(new Text());
	private final Text title = copiable(new Text());
	private final Hyperlink link = new Hyperlink();
	private final Text published = new Text();
	private final Text updated = new Text();
	private final ChoiceBox<FeedEntryStatus> statusChoice = new ChoiceBox<>(FXCollections.observableArrayList(FeedEntryStatus.values()));
	
	private final TagPillsListView tagsPills;
	private final Tags tagsService;
	private final WebView browser = new WebView();
	
	private FeedEntry entry;
	
	@Inject
	public ClassicView(LinkOpener opener, Tags tagsService, TagPillsListView tagsView) {
		super(4);
		this.tagsService = tagsService;
		this.tagsPills = tagsView;
		setId("classic-view");
		
		setFillWidth(true);
		setBackground(FxCss.background(Color.WHITE));
		tagsView.setOnSave(res -> {
			this.entry.setTags(tagsService.serializeTags(res));
			updateUpdateTime();
		});

		ProgressBar webProgress = new ProgressBar(0);
		webProgress.setId("webview-progress");
		webProgress.setMaxWidth(Double.MAX_VALUE);
		webProgress.progressProperty().bind(browser.getEngine().getLoadWorker().progressProperty());
		
		getChildren().addAll(
				hBox(5, id, new Text(":"), title), 
				link, 
				hBox(5, bold("Published On:"), published, bold("Updated On:"), updated, bold("Status: "), statusChoice),
				bold("Tags:"),
				tagsView,
				webProgress,
				browser
				);
		HBox.setHgrow(title, Priority.ALWAYS);
		getChildren().subList(0, getChildren().indexOf(webProgress)).forEach(n -> VBox.setMargin(n, new Insets(0, 5, 0, 5)));
		setPadding(new Insets(5, 0, 0, 0));
		
		VBox.setVgrow(browser, Priority.ALWAYS);
		
		title.setStyle("-fx-font-size: 1.2em");
		link.setOnAction(e -> {
			opener.openLink(link.getText());
			link.setVisited(false);
		});
		
		statusChoice.setOnAction(e -> {
			FeedEntryStatus f = statusChoice.getSelectionModel().getSelectedItem();
			if(entry == null || f == null || entry.getStatusNonNull() == f) 
				return;
			entry.setStatus(f);
			updateUpdateTime();
		});
	}
	
	private void updateUpdateTime() {
		setDate(updated, entry.getUpdatedOn());
	}

	private Node hBox(int spacing, Node...nodes) {
		HBox h = new HBox(spacing, nodes);
		h.setAlignment(Pos.CENTER_LEFT);
		return h;
	}

	public static <T extends Node> T copiable(T t) {
		t.setOnMouseClicked(e -> {
			if (e.getClickCount() > 1) {
				String s = t instanceof Text ? ((Text) t).getText() : ((Labeled) t).getText();
				if (Checker.isNotEmptyTrimmed(s))
					copy(s);
			}
		});
		return t;
	}
	
	public static void copy(String s) {
		FxClipboard.setString(s);
		FxPopupShop.showHidePopup("copied: " + s, 1500);
	}
	
	public void set(MinimalFeedEntry m) {
		this.entry = null;
		id.setText(Integer.toString(m.id));
		title.setText(m.title);
		link.setText(null);
		published.setText(null);
		updated.setText(null);
		tagsPills.clear();
		statusChoice.getSelectionModel().select(null);
		browser.getEngine().load(null);
	}
	
	public void set(FeedEntry m) {
		this.entry = null;
		id.setText(Integer.toString(m.getId()));
		title.setText(m.getTitle());
		link.setText(m.getLink());
		statusChoice.getSelectionModel().select(m.getStatusNonNull());
		setDate(published, m.getPublishedOn());
		setDate(updated, m.getUpdatedOn());
		
		String[] tags = tagsService.parseTags(m.getTags());
		tagsPills.setTags(tags);
		browser.getEngine().load(m.getLink());
		this.entry = m;
	}
	
	private Node bold(String string) {
		Text t = new Text(string);
		t.setStyle("-fx-font-weight: bold");
		return t;
	}

	public static final ZoneOffset zoneId = ZoneOffset.of("+05:30");
	public static void setDate(Text text, long epochSeconds) {
		text.setText(epochSeconds == 0 ? "UNKNOWN" : LocalDateTime.ofEpochSecond(epochSeconds, 0, zoneId).toString());
	}
	
	@Override
	public void close() throws IOException {
		browser.getEngine().load(null);
	}
}
