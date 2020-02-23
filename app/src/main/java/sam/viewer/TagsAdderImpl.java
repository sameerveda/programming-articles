package sam.viewer;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import programming.articles.api.StateManager;
import programming.articles.model.Tag;
import sam.api.TagsAdder;
import sam.api.Utils;
import sam.fx.helpers.FxCell;
import sam.fx.helpers.FxUtils;
import sam.myutils.Checker;
import sam.viewer.app.App;
public class TagsAdderImpl extends Stage implements TagsAdder {
	private final TextField search = new TextField();
	private final ListView<Tag> list = new ListView<>();
	private FlowPane tagsPane = new FlowPane();
	private String oldText = "";
	private volatile boolean changedInternally;
	private final Set<Tag> addedValues = Collections.newSetFromMap(new IdentityHashMap<>());
	private final StateManager tags;
	private final Stage parent;
	private final Consumer<TagView> remover = tagsPane.getChildren()::remove;

	@Inject
	public TagsAdderImpl(@Named(App.STAGE_KEY) Stage parent, StateManager tags)  {
		super(StageStyle.UNIFIED);
		this.tags = tags;
		this.parent = parent;
		
		initModality(Modality.APPLICATION_MODAL);
		initOwner(parent);

		VBox root = new VBox(5, search, list, tagsPane);
		root.setPadding(new Insets(5));
		setScene(new Scene(root));
		getScene().getStylesheets().addAll(parent.getScene().getStylesheets());
		getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), () -> hide());

		list.setCellFactory(FxCell.listCell(Tag::getName));
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		search.textProperty().addListener((p, o, n) -> {
			if (changedInternally) {
				changedInternally = false;
				return;
			}
			if (Checker.isEmptyTrimmed(n)) {
				resetList();
				oldText = "";
			} else {
				String s = n == null ? "" : n.trim().toLowerCase();
				if (!s.contains(oldText))
					resetList();

				oldText = s;
				list.getItems().removeIf(t -> !t.getLowercased().contains(s));
			}
		});
		search.setOnAction(e -> addTag(true));

		search.setOnKeyReleased(e -> {
			switch (e.getCode()) {
				case UP:
					list.getSelectionModel().selectPrevious();
					break;
				case DOWN:
					list.getSelectionModel().selectNext();
					break;
				default:

					break;
			}
		});
		list.setOnMouseClicked(e -> {
			if (e.getClickCount() > 1)
				addTag(false);
		});

		list.setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.ENTER)
				addTag(false);
		});

		list.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			changedInternally = true;
			search.setText(n == null ? null : n.getName());
		});
	}

	private void resetList() {
		List<Tag> list = this.list.getItems(); 
		list.clear();
		list.addAll(tags.allTagsNames());
		list.removeIf(addedValues::contains);
	}
	
	private void addTag(boolean usingSearch) {
		Collection<Tag> toBeAdded;

		if (usingSearch) {
			Tag t = tags.getTagByName(search.getText());
			toBeAdded = Collections.singletonList(t);
		} else {
			toBeAdded = Utils.minimumCopyList(list.getSelectionModel().getSelectedItems());
		}

		list.getSelectionModel().clearSelection();
		search.clear();
		search.requestFocus();

		list.getItems().removeAll(toBeAdded);
		toBeAdded.forEach(t -> tagsPane.getChildren().add(new TagView(t, remover)));
	}
	
	@Override
	public List<Tag> open(List<Tag> initialTags) {
		tagsPane.getChildren().clear();
		initialTags.forEach(t -> tagsPane.getChildren().add(new TagView(t, remover)));
		addedValues.clear();
		tagsPane.getChildren().forEach(t -> addedValues.add(((TagView) t).value));
		resetList();
		FxUtils.center(this, parent);
		showAndWait();
		
		return Utils.minimumCopyList(tagsPane.getChildren(), t -> ((TagView)t).value);
	}
}