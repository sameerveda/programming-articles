package sam.viewer.app;
import java.util.concurrent.CompletableFuture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import sam.di.FeatherInjector;
import sam.di.Injector;
import sam.di.InjectorProvider;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.helpers.FxUtils;
import sam.fx.helpers.StageAttr;
import sam.fx.popup.FxPopupShop;
import sam.myutils.MyUtilsException;
import sam.viewer.DataDetailsView;
import sam.viewer.LeftPane;

public class App extends Application implements StageAttr {
	public static final String STAGE_KEY = "STAGE";
	// private final Logger logger = LoggerFactory.getLogger(getClass());

	private LeftPane leftPane;
	private DataDetailsView detailsView;
	// private Properties2 config;
	private Injector injector;
	private Stage stage;
	private SplitPane root;
	private Providers providers;
	
	@Override
	public void start(Stage stage) throws Exception {
		// this.config = loadConfig();
		this.stage = stage;
		stage.setScene(new Scene(new VBox(new ProgressIndicator(), new Text("LOADING"))));
		stage.show();
		
		CompletableFuture.supplyAsync(() -> MyUtilsException.toUnchecked(Providers::new))
		.handle((result, error) -> {
			Platform.runLater(() -> {
				try {
					if(error != null)
						start2(error);
					else 
						start2(result);	
				} catch (Throwable e) {
					start2(e);
				}
			});
			return null;
		});
	}

	private void start2(Throwable error) {
		stage.hide();
		FxUtils.setErrorTa(stage, "failed to load app", null, error);
		stage.show();
		error.printStackTrace();
	}

	private void start2(Providers providers) {
		this.providers = providers;
		stage.hide();
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);
		
		providers.setHostServices(getHostServices());
		providers.setStage(stage);
		this.injector = Injector.init(new FeatherInjector(InjectorProvider.detectAndAdd(providers)));
		detailsView = injector.instance(DataDetailsView.class);
		leftPane = injector.instance(LeftPane.class);
		leftPane.setOnSelect(detailsView::update);
		detailsView.setOnNext(leftPane::next); 

		ScrollPane sp = new ScrollPane(detailsView);
		sp.setFitToWidth(true);

		root = new SplitPane(leftPane, sp);
		root.setDividerPositions(0.3, 0.7);

		stage.setScene(new Scene(root));
		stage.getScene().getStylesheets().add("styles.css");
		setStageAttrs(400, 400);
		stage.show();
	}

/*	TODO
 * private Properties2 loadConfig() throws JSONException, IOException {
		Path p = Optional.ofNullable(System2.lookup("config.file"))
				.map(Paths::get)
				.filter(Files::exists)
				.orElseGet(() -> MyUtilsPath.selfDir().resolve("config.properties"));

		if(Files.exists(p))
			return new Properties2(Files.newInputStream(p));
		else 
			return new Properties2();
	}*/

	@Override
	public void stop() throws Exception {
		detailsView.close();
		updateStageAttrs();
		providers.close();
	}

	@Override
	public Stage stage() {
		return stage;
	}

	public static void copyToCliboard(String text) {
		if (text == null)
			return;
		FxClipboard.setString(text);
		FxPopupShop.showHidePopup("copied: " + text, 1500);
	}
}
