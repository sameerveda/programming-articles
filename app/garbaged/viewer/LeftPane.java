package sam.viewer;

import static sam.fx.helpers.FxHBox.buttonBox;
import static sam.fx.helpers.FxHBox.maxPane;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.carrotsearch.hppc.predicates.ShortPredicate;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.Callback;
import programming.articles.api.DataItemPagination;
import programming.articles.api.IconManager;
import programming.articles.model.DataItem;
import programming.articles.model.DataStatus;
import sam.fx.alert.FxAlert;
import sam.fx.components.LazyListView;
import sam.fx.helpers.FxButton;
import sam.nopkg.EnsureSingleton;

public class LeftPane extends BorderPane implements Callback<ListView<Object>, ListCell<Object>> {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{ SINGLETON.init(); }

	private final static String ALL = "ALL";

	private final LazyListView<Short, DataItem> listview = new LazyListView<>(true);
	private final LoadingViewWrap loading = new LoadingViewWrap(listview);
	private final ChoiceBox<Object> statusFilter = new ChoiceBox<>();
	private final Text pageLabel = new Text();
	private final IconManager iconManager;
	
	private DataItemPagination pagination;
	private Button prev, next;
	private Consumer<DataItem> onSelect = d -> {};

	@Inject
	public LeftPane(DataItemPagination itemPagination, IconManager iconManager) {
		this.pagination = itemPagination;
		this.iconManager = iconManager;
		this.listview.setPlaceholder(new Text("NO ITEMS"));

		statusFilter.getItems().add(ALL);
		statusFilter.getItems().addAll(Arrays.asList(DataStatus.values()));
		statusFilter.getSelectionModel().selectedItemProperty().addListener(i -> updateFilter());

		setTop(buttonBox(Pos.CENTER_LEFT, new Text("status"), maxPane(statusFilter), pageLabel));
		setCenter(loading);
		prev = FxButton.button("prev", e -> changePage(-1, e));
		prev.setDisable(true);
		next = FxButton.button("next", e -> changePage(1, e));
		setBottom(buttonBox(prev, next));

		listview.setCellFactory(this);
		listview.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			if(o != null && !pagination.getFilter().apply(id(o)))
				listview.getItems().remove(o);
			onSelect.accept((DataItem)n);
		});
		listview.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		Platform.runLater(() -> statusFilter.getSelectionModel().select(DataStatus.UNREAD));
	}
	
	private static short id(Object o) {
		return o instanceof Number ? (short)o : ((DataItem)o).getId();
	}

	public void setOnSelect(Consumer<DataItem> onSelect) {
		this.onSelect = onSelect;
	}

	private void changePage(int i, ActionEvent e) {
		pagination.setPage(pagination.getPage() + i);
		prev.setDisable(pagination.getPage() == 0);
		updateData();
	}

	private void updateFilter() {
		Object status = this.statusFilter.getSelectionModel().getSelectedItem();
		this.pagination.setStatus(status == ALL ? null : (DataStatus)status);
		updateData();
	}

	private void updateData() {
		loading.loading();
		pagination.getData((list, error) -> Platform.runLater(() -> {
			loading.notLoading();
			if(error != null)
				FxAlert.showErrorDialog(null, "failed to load data for skip: "+ (pagination.getPage() * pagination.getPageSize()), error);
			else {
				ShortPredicate filter = pagination.getFilter();
				list.removeIf(d -> d ==  null || !filter.apply(d.getId()));
				this.listview.getItems().setAll(list);
				pageLabel.setText(pagination.skip()+"-"+Math.min(pagination.skip() + list.size(), pagination.size()) + "/"+pagination.size());
				this.setDisable(pagination.skip() + pagination.getPageSize() >= pagination.size()); 
			}
		}));
	}
	
	public void next() {
		listview.getSelectionModel().selectNext();
	} 

	public DataItem selected() {
		return (DataItem) listview.getSelectionModel().getSelectedItem();
	}

	@Override
	public ListCell<Object> call(ListView<Object> param) {
		return new ListCell<Object>() {
			private final ImageView graphic = new ImageView();
			private final AtomicInteger mod = new AtomicInteger();

			{
				graphic.setFitHeight(15);
				graphic.setFitWidth(15);
				graphic.setPreserveRatio(true);
				setGraphic(graphic);
				setGraphicTextGap(10);
			}

			@Override
			protected void updateItem(Object item, boolean empty) {
				super.updateItem(item, empty);
				int mod = this.mod.incrementAndGet();

				if (empty || item == null) {
					setText(null);
					graphic.setImage(null);
				} else if(item instanceof Number) {
					listview.queue(this, (Short)item);
				} else {
					DataItem d = (DataItem)item;
					Image m = iconManager.getIcon(d.getFavicon());
					graphic.setImage(m);
					if (m == null) {
						iconManager.loadIcon(d.getFavicon(), (img, error) -> {
							if(error == null && mod == this.mod.get())
								graphic.setImage(img);
						});
					}

					setText(d.getTitle());
				}
			}
		};
	}
}
