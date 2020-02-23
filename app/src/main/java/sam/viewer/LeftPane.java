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
import programming.articles.api.ConstantDataItemPagination;
import programming.articles.api.IconManager;
import programming.articles.model.DataStatus;
import programming.articles.model.dynamo.ConstantDataItem;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxButton;
import sam.nopkg.EnsureSingleton;

public class LeftPane extends BorderPane {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{ SINGLETON.init(); }

	private final static String ALL = "ALL";

	private final ListView<ConstantDataItem> listview = new ListView<>();
	private final LoadingViewWrap loading = new LoadingViewWrap(listview);
	// private final TextField searchField = new TextField();
	private final ChoiceBox<Object> statusFilter = new ChoiceBox<>();
	private final Text pageLabel = new Text();
	private ConstantDataItemPagination pagination;
	private Button prev, next;
	private Consumer<ConstantDataItem> onSelect = d -> {};

	@Inject
	public LeftPane(ConstantDataItemPagination itemPagination, IconManager iconManager) {
		this.pagination = itemPagination;
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
		// setBottom(buttonBox(Pos.CENTER_LEFT, maxPane(searchField), FxButton.button("search", e -> updateFilter())));
		// searchField.setOnAction(e -> updateFilter());

		listview.setCellFactory(callback -> new ListCell<ConstantDataItem>() {
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
			protected void updateItem(ConstantDataItem item, boolean empty) {
				super.updateItem(item, empty);
				int mod = this.mod.incrementAndGet();

				if (empty || item == null) {
					setText(null);
					graphic.setImage(null);
				} else {
					Image m = iconManager.getIcon(item.getFavicon());
					graphic.setImage(m);
					if (m == null) {
						iconManager.loadIcon(item.getFavicon(), (img, error) -> {
							if(error == null && mod == this.mod.get())
								graphic.setImage(img);
						});
					}

					setText(item.getTitle());
				}
			}
		});

		listview.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			if(o != null && !pagination.getFilter().apply(o.getId()))
				listview.getItems().remove(o);
			onSelect.accept(n);
		});
		listview.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		Platform.runLater(() -> statusFilter.getSelectionModel().select(DataStatus.UNREAD));
	}
	
	public void setOnSelect(Consumer<ConstantDataItem> onSelect) {
		this.onSelect = onSelect;
	}

	/* TODO with search
	 * 	private void updateFilter() {
		Object status = this.statusFilter.getSelectionModel().getSelectedItem();
		String text = this.searchField.getText();

		Predicate<ConstantDataItem> filter = null;
		if (status != ALL)
			filter = (d -> d.getStatus() == status);
		if (Checker.isNotEmptyTrimmed(text)) {
			String s = text.toLowerCase();
			Predicate<ConstantDataItem> f = (d -> lowercasedTitle.computeIfAbsent(d.getTitle(), t -> t.toLowerCase()).contains(s));
			filter = filter == null ? f : filter.and(f);
		}
		this.filter = filter;
		applyFilter();
	}
		private List<ConstantDataItem> tempFiltered = new ArrayList<>();

	private void applyFilter() {
		tempFiltered.clear();
		if (filter == null)
			this.listview.getItems().setAll(allData);
		else {
			allData.forEach(f -> {
				if (filter.test(f))
					tempFiltered.add(f);
			});
			this.listview.getItems().setAll(tempFiltered);
		}
	}

	public void delete(ConstantDataItem data) {
		int index = allData.indexOf(data);
		getSelectionModel().selectNext();
		removedItems.add(new RemovedItem(index, allData.remove(index)));
	}

	public void update(ConstantDataItem data) {
		allData.set(allData.indexOf(data), data);
		int n = this.listview.getItems().indexOf(data);
		if (n >= 0)
			this.listview.getItems().set(n, data);
	}

	public ObservableList<RemovedItem> getRemovedItems() {
		return removedItems;
	}

	 */

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

	public ConstantDataItem selected() {
		return listview.getSelectionModel().getSelectedItem();
	}
}
