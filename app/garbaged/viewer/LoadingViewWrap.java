package sam.viewer;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import sam.fx.helpers.FxCss;

public class LoadingViewWrap extends StackPane {
	private final VBox loading = new VBox(new ProgressIndicator());
	
	public LoadingViewWrap(Node content) {
		this();
		this.getChildren().add(content);
	}
	
	public LoadingViewWrap() {
		loading.setAlignment(Pos.CENTER);
		loading.setBackground(FxCss.background(Color.rgb(255, 255, 255, 0.3)));
	}

	protected void setContent(Node...content) {
		getChildren().setAll(content);
	}
	
	protected void loading() {
		this.getChildren().remove(loading);
		this.getChildren().add(loading);
	}

	protected void notLoading() {
			this.getChildren().remove(loading);
	}
}
