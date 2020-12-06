package sam.article.reader.view;

import static sam.article.reader.view.ClassicView.setDate;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import sam.article.reader.api.LinkOpener;
import sam.fx.clipboard.FxClipboard;
import sam.fx.helpers.FxBindings;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxCss;
import sam.fx.helpers.FxGridPane;
import sam.fx.popup.FxPopupShop;
import sam.myutils.Checker;
import sam.myutils.System2;
import sam.rss.articles.model.FeedEntry;
import sam.rss.articles.model.Tags;
import sam.rss.articles.utils.FeedEntryStatus;

class FullView extends VBox {
	protected int row = 0;
	protected final GridPane grid = FxGridPane.gridPane(5);
	protected final Text id = add("Id: ", copiable(new Text()), 1);
	protected final Label title = add("Title: ", copiable(new Label()), 2);
	protected final TextArea notes = new TextArea();
	
	private final Hyperlink source = link("Source: ");
	private final Hyperlink redirect = link("Redirect: ");
	private final ChoiceBox<FeedEntryStatus> status = add("FeedEntryStatus: ", new ChoiceBox<>(FXCollections.observableArrayList(FeedEntryStatus.values())), 1);
	private final Text publishedOn = add("Published On: ", new Text(), 1);
	private final Text updatedOn = add("Updated On: ", new Text(), 1);
	private final Text version = add("Version: ", new Text(), 1);

	protected boolean auto_copy = System2.lookupBoolean("AUTO_SOURCE_COPY");
	private final TagPillsListView tags;
	private final Button addTags = new Button("+");
	private final List<Node> rootView;
	// TODO private TagsSelector tagsAdder;
	private FeedEntry currentEntry;
	private Tags tagsService;

	@Inject
	public FullView(Tags tagsService, LinkOpener opener, TagPillsListView tags) {
		this.tagsService = tagsService;
		this.tags = tags;
		this.tags.setOrientation(Orientation.HORIZONTAL);
		/* TODO
 		 * this.tags.setOnUpdate(e -> {
		 * currentEntry.setTags(tagsService.serializeTags(e)); });
		 */
		setStyle("-fx-font-family:Consolas;-fx-background-color:white;");
		setPadding(new Insets(5));
		tags.setHgap(5);
		tags.setVgap(5);

		notes.setText("NOT YET IMPLEPLEMENTED");
		notes.setDisable(true);
		
		this.rootView = Arrays.asList(grid, new Text("Tags: "), tags, new Text("Notes: "), notes);
		
		getChildren().setAll(rootView);
		setFillWidth(true);
		this.source.setOnAction(e -> opener.openLink(this.source.getText()));
		this.redirect.setOnAction(e -> opener.openLink(this.redirect.getText()));

		VBox.setVgrow(notes, Priority.ALWAYS);

		addTags.setBackground(FxCss.background(Color.CYAN));
		addTags.setAlignment(Pos.CENTER);
		addTags.setBorder(FxCss.border(Color.BLACK));
		addTags.setOnAction(e -> addTagsView());
		status.getSelectionModel().selectedItemProperty().addListener(e -> {
			if(currentEntry != null) 
				currentEntry.setStatus(status.getSelectionModel().getSelectedItem());
		});

		this.setDisable(true);
	}
	
	private void addTagsView() {
		/* TODO 
		 * if(tagsAdder == null) tagsAdder = new TagsSelector(this.tagsService);
		 * FeedEntry c = this.currentEntry; this.currentEntry = null;
		 * getChildren().setAll(tagsAdder); tagsAdder.open(c.getTitle(), tags.getTags(),
		 * res -> { getChildren().setAll(rootView); this.currentEntry = c; if(res !=
		 * null) { tags.setTags(res); // TODO tags.add(addTags);
		 * currentEntry.setTags(tagsService.serializeTags(res)); } });
		 */
	}

	public void set(FeedEntry article) {
		this.currentEntry = null;
		this.setDisable(article == null);
		if(!rootView.equals(getChildren())) {
			System.out.println("set rootview");
			getChildren().setAll(rootView);
		}
		
		if(article == null)
			return;

		id.setText(String.valueOf(article.getId()));
		setText(title, article.getTitle());
		setText(source, article.getLink());
		setText(redirect, article.getRedirect());
		status.getSelectionModel().select(article.getStatus() == null ? FeedEntryStatus.UNREAD : article.getStatus());
		setDate(updatedOn, article.getUpdatedOn());
		setDate(publishedOn, article.getPublishedOn());
		version.setText(Integer.toString(article.getVersion()));

		this.tags.setTags(tagsService.parseTags(article.getTags()));
		// TODO this.tags.add(addTags);

		if(this.getChildren().size() < 2)
			this.getChildren().setAll(this.rootView);

		this.currentEntry = article;
		
		if(auto_copy)
			copy(article.getRedirect() == null ? article.getLink() : article.getRedirect());
	}

	public static void setText(Labeled t, String s) {
		t.setText(s);
		t.setTooltip(new Tooltip(s));
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

	protected Hyperlink link(String text) {
		Hyperlink t = new Hyperlink();
		Button btn = FxButton.button(null, e -> copy(t.getText()), FxBindings.isEmptyTrimmed(t.textProperty()));
		btn.setBackground(null);
		btn.setBorder(null);
		btn.setGraphic(new ImageView(ClassLoader.getSystemResource("clip.png").toString()));
		HBox box = new HBox(3, t, btn);
		add(text, box, 2);
		return t;
	}

	protected <T extends Node> T add(String name, T component, int rowSpan) {
		return add(new Text(name), component, rowSpan);
	}

	protected <T extends Node> T add(Node n1, T n2, int rowSpan) {
		grid.addRow(row, n1, n2);
		GridPane.setValignment(n1, VPos.TOP);
		GridPane.setColumnSpan(n2, GridPane.REMAINING);
		GridPane.setFillWidth(n2, true);
		GridPane.setHgrow(n2, Priority.ALWAYS);
		GridPane.setRowSpan(n2, rowSpan);
		row = row + rowSpan;
		return n2;
	}
}
