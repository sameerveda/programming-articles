package sam.article.reader.view;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import sam.fx.helpers.FxCss;
import sam.myutils.Checker;
import sam.rss.articles.model.Tags;

class TagPillsListView extends FlowPane {
	private static final String[] DISABLE = new String[0];
	private final TextField search = new TextField();
	private final ObservableList<String> fxAllTags = FXCollections.observableArrayList();
	private final FilteredList<String> filtered = new FilteredList<String>(fxAllTags);
	private final ListView<String> allTagsLV = new ListView<String>(filtered);
	private final Tags tagsService;
	private boolean textSelected = false;
	private final Button save = new Button("SAVE");
	private Consumer<String[]> onSave;

	@Inject
	public TagPillsListView(Tags tagsService) {
		super(5, 5);
		this.tagsService = tagsService;
		this.fxAllTags.addAll(this.tagsService.getData().values());
		Pane pane = new Pane(allTagsLV);
		pane.setManaged(false);
		
		InvalidationListener boundListener = i -> {
			Bounds b = localToParent(search.getBoundsInParent());
			pane.setLayoutX(b.getMinX());
			pane.setLayoutY(b.getMaxY());			
		};
		
		InvalidationListener focusListener = i -> {
			if (this.search.isFocused() || allTagsLV.isFocused()) {
				if (((VBox) getParent()).getChildren().contains(pane))
					return;
				search.boundsInParentProperty().addListener(boundListener);
				((VBox) getParent()).getChildren().add(pane);
			} else {
				((VBox) getParent()).getChildren().remove(pane);
				search.boundsInParentProperty().removeListener(boundListener);
			}	
		};
		
		this.search.focusedProperty().addListener(focusListener);
		this.allTagsLV.focusedProperty().addListener(focusListener);

		allTagsLV.setOnMouseClicked(e -> {
			if(e.getClickCount() > 1)
				Platform.runLater(() -> addTag(allTagsLV.getSelectionModel().getSelectedItem()));
		});

		search.textProperty().addListener((p, o, n) -> filtered.setPredicate(computePredicate(n)));
		search.setOnKeyReleased(this::handleKeyEvent);
		save.setOnAction(e -> {
			onSave.accept(getTags());
			getChildren().remove(save);
			this.getChildren().stream().filter(t -> t instanceof TagPill).forEach(f -> ((TagPill)f).setIsNew(false));
		});
	}

	private class TagPill extends HBox {
		private final String value;

		public TagPill(String value, boolean isNew) {
			this.value = value;
			Button btn = new Button("x");
			btn.setOnAction(e -> remove(TagPill.this));
			btn.setTextFill(Color.RED);
			btn.setBackground(null);
			btn.setAlignment(Pos.CENTER);
			Label lbl = new Label(value);
			lbl.setPadding(new Insets(4, 2, 4, 2));
			getChildren().addAll(lbl, btn);
			setAlignment(Pos.CENTER);
			setIsNew(isNew);
		}

		private void setIsNew(boolean isNew) {
			setBorder(FxCss.border(isNew ? Color.LIGHTBLUE : Color.LIGHTGRAY));
		}
	}

	public void setOnSave(Consumer<String[]> onSave) {
		this.onSave = onSave;
	}

	private void remove(TagPill tag) {
		this.getChildren().remove(tag);
		getChildren().add(save);
	}

	public void clear() {
		this.setTags(DISABLE);
	}

	public void setTags(String[] tags) {
		this.getChildren().clear();
		if (tags == DISABLE)
			return;
		if (tags != null) {
			Arrays.sort(tags, String.CASE_INSENSITIVE_ORDER);
			for (String n : tags)
				this.getChildren().add(new TagPill(n, false));
		}
		filtered.setPredicate(null);
		this.getChildren().add(search);
	}

	public String[] getTags() {
		return this.getChildren().stream().filter(t -> t instanceof TagPill).map(t -> ((TagPill) t).value)
				.toArray(String[]::new);
	}

	private Predicate<String> computePredicate(String text) {
		this.textSelected = true;
		if (Checker.isEmptyTrimmed(text))
			return null;
		else {
			String s = text.toLowerCase().trim();
			if (s.indexOf(' ') > 0) {
				String[] ss = s.split("\\s+");
				return (t -> {
					for (String k : ss) {
						if (t.toLowerCase().contains(k))
							return true;
					}
					return false;
				});
			} else {
				return (t -> t.toLowerCase().contains(s));
			}
		}
	}

	private void handleKeyEvent(KeyEvent e) {
		switch (e.getCode()) {
		case UP:
			this.textSelected = false;
			allTagsLV.getSelectionModel().selectPrevious();
			break;
		case DOWN:
			this.textSelected = false;
			allTagsLV.getSelectionModel().selectNext();
			break;
		case ENTER:
			addTag(this.textSelected ? getTag(this.search.getText())
					: this.allTagsLV.getSelectionModel().getSelectedItem());
			break;
		default:
			break;
		}
	}

	private String getTag(String s) {
		if (Checker.isEmptyTrimmed(s))
			return null;

		s = s.trim();
		int found = this.tagsService.getTagIdFor(s);
		if (found >= 0)
			return Objects.requireNonNull(this.tagsService.getTag(found));

		this.tagsService.putNewTag(s);
		return s;
	}

	private void addTag(String tag) {
		if (tag == null)
			return;

		this.getChildren().add(this.getChildren().indexOf(search), new TagPill(tag, true));
		// this.fxAllTags.remove(tag);
		// this.fxAllTags.add(0, tag);
		this.search.setText(null);
		if (!getChildren().contains(save))
			getChildren().add(save);
	}

}
