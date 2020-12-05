package sam.article.reader.view;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import sam.article.reader.api.LinkOpener;
import sam.fx.helpers.FxCss;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.Tags;
import sam.rss.articles.sqlite.model.MinimalFeedEntry;
import sam.rss.articles.utils.FeedEntryStatus;

public class ClassicView extends VBox {
	private final Label title = new Label();
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
		
		setFillWidth(true);
		setBackground(FxCss.background(Color.WHITE));
		tagsView.setOnSave(res -> this.entry.setTags(tagsService.serializeTags(res)));

		ProgressBar webProgress = new ProgressBar(0);
		webProgress.setId("webview-progress");
		webProgress.setMaxWidth(Double.MAX_VALUE);
		webProgress.progressProperty().bind(browser.getEngine().getLoadWorker().progressProperty());

		HBox dates = new HBox(5, bold("Published On:"), published, bold("Updated On:"), updated, bold("Status: "), statusChoice);
		dates.setAlignment(Pos.CENTER_LEFT);
		
		getChildren().addAll(
				title, 
				link, 
				dates,
				bold("Tags:"),
				tagsView,
				webProgress,
				browser
				);
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
			System.out.println("    Status Change: " + entry.getStatus() + " -> " + f);
			entry.setStatus(f);
			setDate(updated, entry.getUpdatedOn());
		});
	}
	
	public void set(MinimalFeedEntry m) {
		this.entry = null;
		title.setText(""+ m.id + ": "+ m.title);
		link.setText(null);
		published.setText(null);
		updated.setText(null);
		tagsPills.clear();
		statusChoice.getSelectionModel().select(null);
	}
	
	public void set(FeedEntry m) {
		this.entry = null;
		title.setText(""+ m.getId() + ": "+ m.getTitle());
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
		text.setText(epochSeconds == 0 ? "NEVER" : LocalDateTime.ofEpochSecond(epochSeconds, 0, zoneId).toString());
	}
}
