package sam.book.search.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import sam.book.search.model.Article;
import sam.book.search.model.ArticlesDB;
import sam.book.search.model.Title;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxCss;
import sam.fx.helpers.FxGridPane;
import sam.fx.helpers.FxHBox;
import sam.internetutils.InternetUtils;
import sam.myutils.Checker;
import sam.reference.WeakMap;

class FullView extends VBox implements Runnable, EventHandler<ActionEvent> {
	private int row = 0;
	private final GridPane grid = FxGridPane.gridPane(5);
	private final Text id = add("Id: ", new Text());
	private final Label title = add("Title: ", wrap(new Label()));
	private final Hyperlink source = add("Source: ", wrap(new Hyperlink()));
	private final Hyperlink redirect = add("Redirect: ", wrap(new Hyperlink()));
	private final Text addedOn = add("Added On: ", new Text());
	private final ChoiceBox<String> status = add("Status: ", new ChoiceBox<>(FXCollections.observableArrayList(ArticlesDB.ALL_STATUS)));
	private final Text updatedOn = add("Updated On: ", new Text());
	private final Text version = add("Version: ", new Text());

	private final TagsListView tags = new TagsListView();
	private final ImageView icon = new ImageView();
	private final TextArea notes = new TextArea();
	private final Button addTags = new Button("+");
	private final Node[] rootView;
	private TagsAdder tagsAdder;

	private final ArticlesDB db;
	private final WeakMap<String, Image> icons = new WeakMap<>(new ConcurrentHashMap<>());
	private final AtomicReference<String> iconToLoad = new AtomicReference<>();
	private Thread thread;
	private Article article;
	
	private SimpleIntegerProperty onUpdate = new SimpleIntegerProperty();

	public FullView(ArticlesDB db, Runnable previous, Runnable next) {
		super(5);
		this.db = db;
		this.tags.setOrientation(Orientation.HORIZONTAL);
		this.tags.setOnUpdate(e -> {
			article.setTags(db.serializeTags(e));
			update();
		});
		setStyle("-fx-font-family:Consolas;-fx-background-color:white;");
		setPadding(new Insets(5));
		tags.setHgap(5);
		tags.setVgap(5);
		
		notes.setText("NOT YET IMPLEPLEMENTED");
		notes.setDisable(true);

		this.rootView = new Node[] {icon, grid, new Text("Tags: "), tags, new Text("Notes: "), notes,  FxHBox.buttonBox(
				FxButton.button("PREVIOUS", e -> previous.run()),
				FxButton.button("NEXT", e -> next.run()),
				FxHBox.maxPane(),
				FxButton.button("SAVE", e -> save(), Bindings.createBooleanBinding(() -> article == null || !article.isChanged(), onUpdate))
				)};

		getChildren().setAll(rootView);
		setFillWidth(true);
		this.source.setOnAction(this);
		this.redirect.setOnAction(this);
		
		VBox.setVgrow(notes, Priority.ALWAYS);

		addTags.setBackground(FxCss.background(Color.CYAN));
		addTags.setAlignment(Pos.CENTER);
		addTags.setBorder(FxCss.border(Color.BLACK));
		addTags.setOnAction(e -> addTagsView());
		status.getSelectionModel().selectedItemProperty().addListener(e -> {
			if(article != null) {
				article.setStatus(status.getSelectionModel().getSelectedItem());
				update();				
			}
		});
		
		this.setDisable(true);
	}

	private void update() {
		onUpdate.set(onUpdate.get() + 1);
	}

	private void save() {
		this.set(db.update(article));
	}

	private void addTagsView() {
		if(tagsAdder == null) 
			tagsAdder = new TagsAdder(db);
		getChildren().setAll(tagsAdder);
		tagsAdder.open(tags.getTags(), res -> {
			getChildren().setAll(rootView);
			if(res != null) {
				tags.setTags(res);
				tags.add(addTags);
				article.setTags(db.serializeTags(res));
				update();
			}
		});
	}

	@Override
	public void handle(ActionEvent e) {
		if (e.getSource() instanceof Hyperlink) {
			Hyperlink h = (Hyperlink) e.getSource();
			if (Checker.isNotEmptyTrimmed(h.getText()))
				App.browse(h.getText());
		}
	}
	
	public void set(Title title) {
		set(db.getArticle(title.id));
	}
	
	public void set(Article article) {
		this.article = null;
		this.setDisable(article == null);
		
		id.setText(String.valueOf(article.id));
		title.setText(article.title);
		source.setText(article.source);
		redirect.setText(article.redirect);
		addedOn.setText(db.getDate(article.addedOn));
		status.getSelectionModel().select(article.status == null ? "UNREAD" : article.status);
		updatedOn.setText(article.updatedOn == 0 ? "NEVER" : db.getDate(article.updatedOn));
		version.setText(String.valueOf(article.version));
		
		this.tags.setTags(db.parseTags(article.tags));
		this.tags.add(addTags);
		
		if(this.getChildren().size() < 2)
			this.getChildren().setAll(this.rootView);

		String url = db.getIcon(article.favicon);
		Image m = this.icons.get(url);
		if (m != null) {
			this.icon.setImage(m);
		} else {
			this.icon.setImage(null);
			if (Checker.isNotEmptyTrimmed(url)) {
				if (thread == null) {
					thread = new Thread(this);
					thread.setDaemon(true);
					thread.start();
				}
				this.iconToLoad.set(url);
			}
		}
		
		this.article = article;
		update();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
				String url = this.iconToLoad.getAndSet(null);
				if (url == null)
					continue;
				Image m = new Image(InternetUtils.connection(url).getInputStream());
				this.icons.put(url, m);
				Platform.runLater(() -> {
					if (this.iconToLoad.get() == null)
						this.icon.setImage(m);
				});
			} catch (Exception e) {
				if (e instanceof InterruptedException)
					break;
			}
		}
	}

	private <T extends Labeled> T wrap(T t) {
		t.setWrapText(true);
		return t;
	}

	private <T extends Node> T add(String name, T component) {
		return add(new Text(name), component);
	}

	private <T extends Node> T add(Node n1, T n2) {
		grid.addRow(row++, n1, n2);
		GridPane.setValignment(n1, VPos.TOP);
		GridPane.setColumnSpan(n2, GridPane.REMAINING);
		GridPane.setFillWidth(n2, true);
		GridPane.setHgrow(n2, Priority.ALWAYS);
		return n2;
	}
}
