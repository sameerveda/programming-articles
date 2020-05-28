package sam.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Provider;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import programming.articles.api.IconManager;
import programming.articles.api.StateManager;
import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;
import programming.articles.model.Tag;
import sam.api.TagsAdder;
import sam.api.UrlOpener;
import sam.api.Utils;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxHBox;
import sam.myutils.Checker;
import sam.nopkg.EnsureSingleton;
import sam.viewer.app.App;

public class DataDetailsView extends VBox implements AutoCloseable {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{ singleton.init(); }
	private final ImageView icon = new ImageView();
	private final Label id = label();
	private final Label title = label();
	private final Hyperlink source = hyperlink();
	private final Hyperlink redirect = hyperlink();
	private final Button addTags = new Button();
	private final FlowPane tagsPane = new FlowPane(2, 2, new Text("Tags"), addTags);
	private final Text added_on = text();
	private final ChoiceBox<DataStatus> status = new ChoiceBox<>(FXCollections.observableArrayList(DataStatus.values()));
	private final TextArea notes = new TextArea();
	private boolean tagsFailed = false;
	private DataItem current;
	private final Button next = button("next url", "right-arrow.png");
	private final Button delete = button("delete", "delete.png");
	private final HBox buttonBox = FxHBox.buttonBox(icon, delete, FxHBox.maxPane(), id, next);
	private final Provider<TagsAdder> tagsAdder;
	private final UrlOpener urlOpener;
	private final StateManager stateManager;
	private final IconManager iconManager;

	private final Consumer<TagView> tagRemover = tag -> {
		if(tagsPane.getChildren().isEmpty())
			return;

		tagsPane.getChildren().remove(tag);
		current.setTags(current.getTags().replace("."+tag.value.getId()+".", ""));
	};

	@Inject
	public DataDetailsView(StateManager stateManager, IconManager iconManager, Provider<TagsAdder> tagsAdder, UrlOpener opener) {
		this.tagsAdder = tagsAdder;
		this.stateManager = stateManager;
		this.urlOpener = opener;
		this.iconManager = iconManager;

		getStyleClass().add("data-details-view");

		VBox secondBox = new VBox(5, new Text("source"), linkBox(source), new Text("redirect"), linkBox(redirect),
				box(new Text("added on: "), added_on), box(new Text("status: "), status), tagsPane, new Text("Notes"),
				notes,
				FxHBox.buttonBox(FxButton.button("save notes", e -> current.setNotes(notes.getText()))));

		secondBox.getStyleClass().add("second-box");
		secondBox.setFillWidth(true);
		secondBox.setMaxHeight(Double.MAX_VALUE);

		getChildren().addAll(buttonBox,
				box("title", title, copyBtn(title)),
				secondBox);
		VBox.setVgrow(secondBox, Priority.ALWAYS);

		notes.maxHeight(Double.MAX_VALUE);
		VBox.setVgrow(notes, Priority.ALWAYS);
		setVisible(false);

		addTags.getStyleClass().add("tags_add_btn");
		addTags.setTooltip(new Tooltip("Add Tags"));
		addTags.setOnAction(e -> addTags());
		FlowPane.setMargin(addTags, new Insets(0, 0, 0, 10));

		status.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> current.setStatus(n));
	}

	private static Text text() {
		return new Text();
	}

	private static Hyperlink hyperlink() {
		Hyperlink l = new Hyperlink();
		l.setWrapText(false);
		return l;
	}

	private static Label label() {
		Label l = new Label();
		l.setWrapText(true);
		return l;
	}

	public void setOnNext(Runnable action) {
		next.setOnAction(action == null ? null : e -> action.run());
	}

	private Button button(String tooltip, String icon) {
		Button btn = new Button();
		btn.visibleProperty().bind(btn.onActionProperty().isNotNull());
		btn.setTooltip(new Tooltip(tooltip));
		btn.getStyleClass().clear();
		btn.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream(icon))));
		return btn;
	}

	/*public void setOnDelete(Consumer<DataItem> onDelete) {
		delete.setOnAction(onDelete == null ? null : e -> onDelete.accept(current));
	}*/

	private final StringBuilder sb = new StringBuilder();

	private void addTags() {
		List<TagView> list = tagsSubList();
		List<Tag> result = tagsAdder.get().open(Utils.minimumCopyList(list, t -> t.value));
		list.clear();
		result.forEach(t -> list.add(new TagView(t, tagRemover)));

		if (list.isEmpty()) {
			current.setTags(null);
		} else {
			sb.setLength(0);
			Tag.appendTo(list.stream().mapToInt(t -> ((TagView) t).value.getId()), sb);
			current.setTags(sb.toString());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<TagView> tagsSubList() {
		List list = tagsPane.getChildren().subList(1, tagsPane.getChildren().size() - 1);
		return list;
	}

	private Node linkBox(Hyperlink link) {
		link.setOnAction(e -> urlOpener.openUrl(link.getText()));
		return box(link, copyBtn(link));
	}

	private HBox box(String cls, Node... nodes) {
		HBox h = box(nodes);
		h.getStyleClass().add(cls);
		return h;
	}

	private HBox box(Node... nodes) {
		HBox h = new HBox(5, nodes);
		h.getStyleClass().add("box");
		return h;
	}

	private Button copyBtn(Labeled source) {
		Button btn = new Button();
		btn.getStyleClass().add("copy-btn");
		btn.visibleProperty().bind(source.textProperty().isNotEmpty());
		btn.setOnAction(e -> App.copyToCliboard(source.getText()));
		return btn;
	}

	private List<TagView> tempTags = new ArrayList<>();
	private static final DataItem EMPTY_ITEM = new DataItem(); // to discard any changes during update method

	public void update(DataItem data) {
		close();
		this.current = data;
		this.icon.setImage(data == null ? null : iconManager.getIcon(data.getFavicon()));
		setVisible(false);
		if(data != null)
			update0(data);
	}

	private void update0(DataItem data) {
		setVisible(true);
		id.setText(Integer.toString(data.getId()));
		title.setText(data.getTitle());
		source.setText(data.getSource());
		redirect.setText(data.getRedirect());
		added_on.setText(data.getAddedOn());
		status.getSelectionModel().select(data.getStatus());
		notes.setText(data.getNotes());

		tagsSubList().clear();

		if (!tagsFailed && Checker.isNotEmptyTrimmed(data.getTags())) {
			tempTags.clear();
			Tag.parse(data.getTags(), tagId -> {
				Tag t = stateManager.getTagById(tagId);
				if(t != null)
					tempTags.add(new TagView(t, tagRemover));
			});
			tagsPane.getChildren().addAll(1, tempTags);
			tempTags.clear();
		}

		this.current = data;
	}

	@Override
	public void close() {
		if(this.current != null && this.current != EMPTY_ITEM)
			stateManager.commit(this.current, (b, e) -> FxAlert.showErrorDialog(null, null, e));
	}
}
