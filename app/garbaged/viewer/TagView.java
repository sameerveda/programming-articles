package sam.viewer;

import java.util.function.Consumer;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import programming.articles.model.Tag;
import sam.fx.helpers.FxButton;

public class TagView extends HBox {
	public final Tag value;

	public TagView(Tag value, Consumer<TagView> onRemove) {
		this.value = value;
		getStyleClass().add("tag");
		Label t = new Label(value.getName());
		t.setTooltip(new Tooltip(Integer.toString(value.getId())));
		getChildren().addAll(t, FxButton.button("x", e -> onRemove.accept(this)));
	}
}