package sam.book.search.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import sam.fx.helpers.FxCss;

public class TagsListView extends FlowPane {
	
	private class Tag extends HBox {
		private final String value;

		public Tag(String value) {
			this.value = value;
			Button btn = new Button("x");
			btn.setBackground(FxCss.background(Color.RED));
			btn.setTextFill(Color.WHITE);
			btn.setAlignment(Pos.CENTER);
			Label lbl = new Label(value);
			lbl.setPadding(new Insets(2));
			getChildren().addAll(lbl, btn);
			setAlignment(Pos.CENTER);
			setBorder(FxCss.border(Color.LIGHTGRAY));
		}
	}
	
	public void add(Node node) {
		this.getChildren().add(node);
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
		return this.getChildren().stream().map(t -> ((Tag)t).value).toArray(String[]::new);
	}
}
