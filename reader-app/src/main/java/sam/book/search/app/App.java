package sam.book.search.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumMap;
import java.util.Map;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sam.book.search.model.Article;
import sam.book.search.model.ArticleStatus;
import sam.book.search.model.ArticlesDB;
import sam.book.search.model.SortBy;
import sam.book.search.model.SortDir;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.components.LazyListView;
import sam.fx.helpers.FxFxml;
import sam.fx.helpers.FxUtils;
import sam.fx.helpers.IconButton;
import sam.fx.popup.FxPopupShop;
import sam.myutils.System2;
import sam.sql.sqlite.Sqlite4javaHelper;

public class App extends Application {
	public static final String SHOW_ALL = "All";
	public static final String BOOKMARK = "bookmark";
	private ArticlesDB con;

	@FXML private Scene mainScene;
	@FXML private SplitPane mainRoot;
	@FXML private Text searchLabel;
	@FXML private Text countText;
	@FXML private TextField searchField;
	@FXML private IconButton dirFilter;
	@FXML private LazyListView<Integer, Article> booklist;
	@FXML private Hyperlink typeChoice;
	@FXML private Hyperlink statusChoice;
	@FXML private Hyperlink sortByChoice;

	private Map<Choices, Object> selected = new EnumMap<>(Choices.class);
	private SortDir sortDir;
	private Stage stage;
	private FullView fullview;
	
	private static HostServices hostservice;

	@Override
	public void start(Stage stage) throws Exception {
		hostservice = getHostServices();
		this.stage = stage;
		try {
			String dbPath = System2.lookup("db_path");
			if (dbPath == null) {
				throw new IllegalStateException("env \"db_path\" not set");
			}
			if (!new File(dbPath.trim()).exists()) {
				throw new FileNotFoundException("db not found, db_path: " + dbPath);
			}

			FxFxml.load(ClassLoader.getSystemResource("fxml/App.fxml"), stage, this);
			FxPopupShop.setParent(stage);
			FxAlert.setParent(stage);
			searchField.setText(null);
			countText.textProperty().bind(Bindings.size(booklist.getItems()).asString());

			for (Choices c : Choices.values())
				selected.put(c, c.defaultValue);

			typeChoice.setText(selected.get(Choices.TYPE).toString());
			statusChoice.setText(selected.get(Choices.STATUS).toString());
			sortByChoice.setText(selected.get(Choices.SORT_BY).toString());
			sortDir = ((SortBy) selected.get(Choices.SORT_BY)).dir;

			con = new ArticlesDB(new File(dbPath.trim()));
			
			fullview = new FullView(con, booklist.getSelectionModel()::selectPrevious, booklist.getSelectionModel()::selectNext);
			mainRoot.getItems().add(fullview);
			mainRoot.setDividerPositions(0.4, 0.6);
			
			booklist.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
				if(n instanceof Article)
					fullview.set((Article)n);
			});
			update(null);
		} catch (Throwable e) {
			FxUtils.setErrorTa(stage, "failed to init", "failed to init", e);
		}
		stage.show();
	}

	private void updateType(Object n, String type) {
		if (selected.get(Choices.TYPE) != SHOW_ALL)
			return;
		try {
			int changes = con.execute("UPDATE reader_app_data SET last_mod=? WHERE type=? AND _id=?", st -> {
				st.bind(1, System.currentTimeMillis());
				st.bind(2, type);
				st.bind(3, id(n));
			});

			if (changes == 0) {
				changes = con.execute("INSERT INTO reader_app_data(_id, type, last_mod) VALUES(?,?,?)", st -> {
					st.bind(1, id(n));
					st.bind(2, type);
					st.bind(3, System.currentTimeMillis());
				});
			}
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}

	private int id(Object n) {
		return n instanceof Integer ? (int) n : ((Article) n).id;
	}

	@FXML
	private void update(Event event) {
		try {
			SQLiteStatement st;
			if (selected.get(Choices.TYPE) != SHOW_ALL) {
				st = con.prepare("SELECT _id FROM reader_app_data WHERE type = ? ORDER BY last_mod DESC");
				st.bind(1, selected.get(Choices.TYPE).toString());
			} else {
				st = con.queryIds(
						selected.get(Choices.STATUS) == SHOW_ALL ? null : (ArticleStatus) selected.get(Choices.STATUS),
						(SortBy) selected.get(Choices.SORT_BY), sortDir, searchField.getText(), null);
			}
			this.booklist.getItems().setAll(Sqlite4javaHelper.collectToList(st, rs -> rs.columnInt(0)));
			if (!booklist.isRunning())
				booklist.start(Integer.class, con::loadData);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() throws Exception {
		booklist.close();
		try {
			if (con != null) {
				con.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// LOGGER.debug("run time: {} ({}). loops: {}, loaded-smallbooks: {}",
		// System.currentTimeMillis() - START_TIME,
		// Duration.ofMillis(System.currentTimeMillis() - START_TIME), loops.get(),
		// loaded.size());
		super.stop();
	}

	@FXML
	private void saveAsHtml(Event e) {
		// TODO auto-generated
		FxPopupShop.showHidePopup("not working: ", 2000);
	}

	@FXML
	private void saveFilter(Event e) {
		// TODO
		FxPopupShop.showHidePopup("not working: ", 2000);
	}

	@FXML
	private void copyCombinedAction(Event e) {
		// TODO auto-generated
		FxPopupShop.showHidePopup("not working: ", 2000);
	}

	@FXML
	private void openFileAction(Event e) {
		// TODO auto-generated
		FxPopupShop.showHidePopup("not working: ", 2000);
	}

	@FXML
	private void reloadResoueces(Event e) {
		// TODO auto-generated
		FxPopupShop.showHidePopup("not working: ", 2000);
	}

	@FXML
	private void dirFilterAction(Event e) {
		// TODO auto-generated
		FxPopupShop.showHidePopup("not working: ", 2000);
	}

	@FXML
	private void updateState(Event e) {
		Hyperlink link = (Hyperlink) e.getSource();
		Choices choice = (Choices) link.getUserData();
		ChoiceDialog<Object> d = new ChoiceDialog<Object>(selected.get(choice), choice.allValues);
		d.initModality(Modality.APPLICATION_MODAL);
		d.initOwner(stage);
		d.setContentText(choice.toString());
		d.setHeaderText("Select");
		d.showAndWait().ifPresent(o -> {
			if (selected.get(choice) != o || choice == Choices.SORT_BY) {
				selected.put(choice, o);
				link.setText(o.toString());

				statusChoice.setDisable(choice == Choices.TYPE && o != SHOW_ALL);
				sortByChoice.setDisable(choice == Choices.TYPE && o != SHOW_ALL);
				if (choice == Choices.SORT_BY)
					sortDir = selected.get(choice) != o ? ((SortBy) o).dir
							: sortDir == SortDir.DESC ? SortDir.ASC : SortDir.DESC;
				update(null);
			}
		});
	}

	public static void copyToClipboard(String text) {
		FxClipboard.setString(text);
		FxPopupShop.showHidePopup("copied: " + text, 1500);
	}

	public static void browse(String url) {
		hostservice.showDocument(url);
	}
}
