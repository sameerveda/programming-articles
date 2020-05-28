package sam.book.search.app;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import sam.book.search.model.ArticlesDB;
import sam.fx.helpers.FxButton;
import sam.fx.helpers.FxHBox;
import sam.myutils.Checker;

class TagsAdder extends VBox {
	private final TextField search = new TextField();
	private final ObservableList<String> list;
	private final FilteredList<String> filtered;
	private final ListView<String> listView;
	private final TagsListView tags = new TagsListView();

	private final ArticlesDB db;

	private Consumer<String[]> onResult;
	private boolean textSelected = false;

	public TagsAdder(ArticlesDB db) {
		super(5);
		this.db = db;
		String[] strs = db.allTags();
		Arrays.sort(strs);
		list = FXCollections.observableArrayList(strs);
		filtered = new FilteredList<>(list);
		listView = new ListView<>(filtered);
		search.textProperty().addListener((p, o, n) -> filtered.setPredicate(computePredicate(n)));
		listView.setOnMouseClicked(e -> {
			if(e.getClickCount() > 1)
				addTag(listView.getSelectionModel().getSelectedItem());
		});

		setFillWidth(true);

		VBox.setVgrow(listView, Priority.ALWAYS);
		search.setOnKeyReleased(e -> handleKeyEvent(e));

		getChildren().addAll(search, listView, tags, FxHBox.buttonBox(
				FxButton.button("CANCEL", e -> result(null)),
				FxButton.button("OK", e -> result(tags.getTags()))
				));
	}

	private void result(String[] tags) {
		this.onResult.accept(tags);
		this.onResult = null;
	}

	public void open(String[] input, Consumer<String[]> onResult) {
		this.tags.setTags(input);
		this.onResult = onResult; 
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
				listView.getSelectionModel().selectPrevious();			
				break;
			case DOWN:
				this.textSelected = false;
				listView.getSelectionModel().selectNext();			
				break;
			case ENTER:
				addTag(this.textSelected ? this.db.getTag(this.search.getText()) : this.listView.getSelectionModel().getSelectedItem());
				break;
			default:
				break;
		}
	}

	private void addTag(String tag) {
		if(tag == null)
			return;

		this.tags.add(tag);
		this.list.remove(tag);
		this.list.add(0, tag);
		this.search.setText(null);
	}
}
