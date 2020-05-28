package sam.book.search.app;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import sam.fx.helpers.FxCss;

class TagsListView extends FlowPane {
	private Consumer<String[]> onUpdate;

	public TagsListView() {
		super(5, 5);
	}

	public void setOnUpdate(Consumer<String[]> onUpdate) {
		this.onUpdate = onUpdate;
	}

	private class Tag extends HBox {
		private final String value;

		public Tag(String value) {
			this.value = value;
			Button btn = new Button("x");
			btn.setOnAction(e -> remove(Tag.this));
			btn.setTextFill(Color.RED);
			btn.setBackground(null);
			btn.setAlignment(Pos.CENTER);
			Label lbl = new Label(value);
			lbl.setPadding(new Insets(4,2,4,2));
			getChildren().addAll(lbl, btn);
			setAlignment(Pos.CENTER);
			setBorder(FxCss.border(Color.LIGHTGRAY));
		}
	}

	public void add(Node node) {
		this.getChildren().add(node);
	}

	private void remove(Tag tag) {
		this.getChildren().remove(tag);
		if(onUpdate != null)
			this.onUpdate.accept(getTags());
	}

	public void add(String value) {
		this.getChildren().add(new Tag(value));
	}

	public void setTags(String[] tags) {
		this.getChildren().clear();
		for (String n : tags) 
			this.add(n);
	}

	public String[] getTags() {
		return this.getChildren().stream().filter(t -> t instanceof Tag).map(t -> ((Tag)t).value).toArray(String[]::new);
	}
}
