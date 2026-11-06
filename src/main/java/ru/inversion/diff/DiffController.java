package ru.inversion.diff;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.treetable.FilterTreeItem;
import ru.inversion.fx.form.controls.treetable.ITreeItemPredicate;
import ru.inversion.fx.form.controls.treetable.JInvTreeTable;
import ru.inversion.fx.form.controls.treetable.JInvTreeTableColumn;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.MaterialDesign;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DiffController extends JInvFXFormController<DiffParamObject> {

    @FXML
    private VBox rootVBox;

    @FXML
    private JInvToolBar toolBar;

    @FXML
    private JInvTreeTable<DiffNodeElement> diffTreeTable;

    @FXML
    private JInvTreeTableColumn<DiffNodeElement, String> attributeColumn;

    @FXML
    private JInvTreeTableColumn<DiffNodeElement, String> oldValueColumn;

    @FXML
    private JInvTreeTableColumn<DiffNodeElement, String> newValueColumn;

    @FXML
    private JInvTreeTableColumn<DiffNodeElement, Boolean> changeValueColumn;

    private JInvTextField searchField;

    private Collection<DiffItemResult> diffItemCollection;

    @Override
    protected void init() throws Exception {
        this.diffItemCollection = Collections.unmodifiableCollection(
                Objects.requireNonNull(getDataObject().getDiffItemCollection(), "diffItemCollection can't be null"));
        initTableFactory();
        initToolbar();
        initDiffTable();
    }

    @Override
    protected boolean onOK() {
        final List<DiffItemResult> diffItemResultList = collectAllItemsOnChange(diffTreeTable.getRoot());
        final List<DiffItem> diffItemList = diffItemResultList.stream().map(DiffItemResult::getItem).collect(Collectors.toList());
        getDataObject().getResultItemCollection().accept(diffItemList);

        // Commit изменения
//        try {
//            for (DiffItemResult diffItemHolder : changeItemList) {
//                for (PropertyAccessor propertyAccessor : diffItemHolder.getItemInfo().getAccessors()) {
//                    if (!propertyAccessor.getPropertyName().equals("oldValue")) continue;
//                    final boolean set = propertyAccessor.set(diffItemHolder.getItem(), diffItemHolder.getItem().getNewValue());
//                    if (!set) {
//                        throw new DiffAttributeAccessException(diffItemHolder.getItem().getName());
//                    }
//                }
//            }
//        } catch (DiffAttributeAccessException e) {
//            JInvErrorService.handleException(viewContext, e);
//        }
        return true;
    }

    private void initTableFactory() {
        attributeColumn.setCellFactory(param -> {
            return new TreeTableCell<DiffNodeElement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    final DiffNodeElement rowItem = getTreeTableRow().getItem();
                    if (rowItem == null || empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    if (rowItem.isCategory()) {
                        String categoryName = rowItem.getValue().getItem().getCategory();
                        if (categoryName == null || categoryName.isEmpty()) {
                            categoryName = "Default";
                        }
                        setText(categoryName);
                    } else {
                        String attributeName = rowItem.getValue().getItem().getName();
                        if (attributeName == null || attributeName.isEmpty()) {
                            attributeName = "Unknown";
                        }
                        setText(attributeName);
                    }
                }
            };
        });
        oldValueColumn.setCellFactory(param -> {
            final TreeTableCell<DiffNodeElement, String> cell = new TreeTableCell<DiffNodeElement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    final DiffNodeElement rowItem = getTreeTableRow().getItem();
                    if (rowItem == null || rowItem.isCategory() || empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    if (rowItem.getValue().getItem().getOldValue() == null) {
                        setText(null);
                    } else {
                        setText(rowItem.getValue().getItem().getOldValue().toString());
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        newValueColumn.setCellFactory(param -> {
            final TreeTableCell<DiffNodeElement, String> cell = new TreeTableCell<DiffNodeElement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    final DiffNodeElement rowItem = getTreeTableRow().getItem();
                    if (rowItem == null || rowItem.isCategory() || empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    if (rowItem.getValue().getItem().getNewValue() == null) {
                        setText(null);
                    } else {
                        setText(rowItem.getValue().getItem().getNewValue().toString());
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        changeValueColumn.setCellFactory(p -> {
            final CheckBoxTreeTableCell<DiffNodeElement, Boolean> cell = new CheckBoxTreeTableCell<DiffNodeElement, Boolean>() {
                @Override
                public void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    final DiffNodeElement rowItem = getTreeTableRow().getItem();
                    if (rowItem == null || empty || rowItem.getValue().getState() == DiffState.UNCHANGED) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    if (item != null && rowItem.isCategory()) {
                        // Если это категория, то меняем checkbox'ы у потомков
                        for (TreeItem<DiffNodeElement> diffNodeElementTreeItem : getTreeTableRow().getTreeItem().getChildren()) {
                            if (diffNodeElementTreeItem.getValue().getValue().getState() == DiffState.CHANGED) {
                                diffNodeElementTreeItem.getValue().setChangeValue(item);
                            }
                        }
                    } else {
                        final boolean allSelected = checkIsAllSelectedInCategory(getTreeTableRow().getTreeItem().getParent());
                        if (allSelected) {
                            getTreeTableRow().getTreeItem().getParent().getValue().setChangeValue(true);
                        }
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        // Row color
//        diffTreeTable.setRowFactory(param -> {
//            return new TreeTableRow<DiffNodeElement>(){
//                @Override
//                protected void updateItem(DiffNodeElement item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (item != null && !item.isCategory()) {
//                        if (item.getValue().getState() == DiffState.CHANGED) {
//                            this.setStyle("-fx-background-color:rgba(134,182,230,0.7)");
//                        }
//                    } else {
//                        this.setStyle("");
//                    }
//                }
//            };
//        });
        changeValueColumn.setCellValueFactory(param -> param.getValue().getValue().changeValueProperty());
        diffTreeTable.setEditable(true);
    }

    private void initToolbar() {
        final JInvButton collapseBtn = new JInvButton();
        collapseBtn.setGraphic(IconFactory.getLabel(FontAwesome.fa_angle_double_up));
        collapseBtn.setOnAction(event -> {
            diffTreeTable.setUnExpandedAll(true);
        });
        final JInvButton expandBtn = new JInvButton();
        expandBtn.setGraphic(IconFactory.getLabel(FontAwesome.fa_angle_double_down));
        expandBtn.setOnAction(event -> {
            diffTreeTable.setExpandedAll();
        });
        final JInvButton changeAllBtn = new JInvButton();
        changeAllBtn.setGraphic(IconFactory.getLabel(MaterialDesign.mdi_check_all));
        changeAllBtn.setOnAction(event -> {
            if (checkIsAllSelected(diffTreeTable.getRoot())) {
                changeAll(diffTreeTable.getRoot(), false);
            } else {
                changeAll(diffTreeTable.getRoot(), true);
            }
        });
        final JInvButton showBtn = new JInvButton();
        showBtn.setGraphic(IconFactory.getLabel(FontAwesome.fa_eye));
        showBtn.setOnAction(event -> {
            final Stage stage = new Stage();
            final DiffItem selectedItem = diffTreeTable.getSelectionModel().getSelectedItem().getValue().getValue().getItem();
            final DifferencePane differencePane = new DifferencePane(selectedItem.getOldValue().toString(),
                    selectedItem.getNewValue().toString());
            final Scene scene = new Scene(differencePane, 600, 250);
            stage.setScene(scene);
            stage.show();
        });
        showBtn.disableProperty().bind(Bindings.isNull(diffTreeTable.getSelectionModel().selectedItemProperty()));

        final Pane rightSpace = new Pane();
        HBox.setHgrow(rightSpace, Priority.ALWAYS);

        searchField = new JInvTextField();
        searchField.setPromptText(getBundleString("SEARCH") + "...");

        toolBar.getItems().addAll(collapseBtn, expandBtn, new Separator(), changeAllBtn, showBtn, rightSpace, searchField);
    }

    private void initDiffTable() {
        initLongOperation(() -> {
            final FilterTreeItem<DiffNodeElement> rootItem = new FilterTreeItem<>(null);
            rootItem.predicateProperty().bind(Bindings.createObjectBinding(() -> {
                if (searchField.getText() == null || searchField.getText().isEmpty())
                    return null;
                return ITreeItemPredicate.create(new Predicate<DiffNodeElement>() {
                    @Override
                    public boolean test(DiffNodeElement nodeElement) {
                        final String searchText = searchField.getText().toLowerCase();
                        return nodeElement.getValue().getItem().getName().toLowerCase().contains(searchText) ||
                                nodeElement.getValue().getItem().getCategory().toLowerCase().contains(searchText);
                    }
                });
            }, searchField.textProperty()));
            final List<FilterTreeItem<DiffNodeElement>> categoryItem = createCategoryItem(diffItemCollection);
            // Добавляем item'ы. Каждый в свою категорию
            for (DiffItemResult diffItemResult : diffItemCollection) {
                categoryItem.stream()
                        .filter(diffNodeElementTreeItem -> {
                            return diffNodeElementTreeItem.getValue().getValue().getItem().getCategory()
                                    .equals(diffItemResult.getItem().getCategory());
                        })
                        .findFirst()
                        .ifPresent(diffNodeElementTreeItem -> {
                            diffNodeElementTreeItem.getSourceList().add(new FilterTreeItem<>(new DiffNodeElement(diffItemResult, false)));
                        });
            }
            rootItem.getSourceList().addAll(categoryItem);
            Platform.runLater(() -> {
                diffTreeTable.setRoot(rootItem);
                diffTreeTable.setShowRoot(false);
                diffTreeTable.setExpandedAll();
            });
        });
    }

    /**
     * Находим и создаем список категорий для item'ов
     *
     * @return Список категорий
     */
    private List<FilterTreeItem<DiffNodeElement>> createCategoryItem(Collection<DiffItemResult> diffItemCollection) {
        final List<FilterTreeItem<DiffNodeElement>> categoryItemList = new ArrayList<>();
        for (DiffItemResult diffItemResult : diffItemCollection) {
            final FilterTreeItem<DiffNodeElement> categoryItem = new FilterTreeItem<>(new DiffNodeElement(diffItemResult, true));
            final boolean anyMatch = categoryItemList.stream().anyMatch(item -> {
                return item.getValue().getValue().getItem().getCategory()
                        .equals(categoryItem.getValue().getValue().getItem().getCategory());
            });
            if (!anyMatch) {
                categoryItemList.add(categoryItem);
            }
        }
        return categoryItemList;
    }

    private void changeAll(TreeItem<DiffNodeElement> item, boolean val) {
        if (item != null) {
            if (item.getValue() != null) {
                if (item.getValue().getValue().getState() != DiffState.UNCHANGED) {
                    item.getValue().setChangeValue(val);
                }
            }
            for (TreeItem<DiffNodeElement> diffNodeElementTreeItem : item.getChildren()) {
                changeAll(diffNodeElementTreeItem, val);
            }
        }
    }

    private boolean checkIsAllSelected(TreeItem<DiffNodeElement> item) {
        boolean allSelected = true;
        if (item != null) {
            if (item.getValue() != null && !item.getValue().isCategory()
                    && item.getValue().getValue().getState() != DiffState.UNCHANGED) {
                if (!item.getValue().isChangeValue()) {
                    allSelected = false;
                }
            }
            if (allSelected) {
                for (TreeItem<DiffNodeElement> diffNodeElementTreeItem : item.getChildren()) {
                    allSelected = checkIsAllSelected(diffNodeElementTreeItem);
                    if (!allSelected) break;
                }
            }
        }
        return allSelected;
    }

    /**
     * Проверяет у всех ли item'ов в категории стоит changeValue == true
     *
     * @param item категория у которой нужно проверить item'ы
     * @return true если у всех item'ов данной категории стоит changeValue
     */
    private boolean checkIsAllSelectedInCategory(TreeItem<DiffNodeElement> item) {
        boolean allSelected = true;
        if (item != null && item.getValue() != null) {
            if (!item.getValue().isCategory() &&
                    item.getValue().getValue().getState() == DiffState.CHANGED && !item.getValue().isChangeValue()) {
                allSelected = false;
            }
            if (allSelected) {
                for (TreeItem<DiffNodeElement> diffNodeElementTreeItem : item.getChildren()) {
                    allSelected = checkIsAllSelectedInCategory(diffNodeElementTreeItem);
                    if (!allSelected) break;
                }
            }
        }
        return allSelected;
    }

    private List<DiffItemResult> collectAllItemsOnChange(TreeItem<DiffNodeElement> item) {
        return collectAllItemsOnChange(item, null);
    }

    /**
     * Собираем все item'ы у которых стоит changeValue == true
     *
     * @param item            откуда начинаем собирать
     * @param nodeElementList список куда будут помещены найденные item'ы
     */
    private List<DiffItemResult> collectAllItemsOnChange(TreeItem<DiffNodeElement> item, List<DiffItemResult> nodeElementList) {
        if (nodeElementList == null) {
            nodeElementList = new ArrayList<>();
        }
        if (item != null) {
            if (item.getValue() != null && !item.getValue().isCategory() &&
                    item.getValue().getValue().getState() == DiffState.CHANGED && item.getValue().isChangeValue()) {
                nodeElementList.add(item.getValue().getValue());
            }
            for (TreeItem<DiffNodeElement> diffNodeElementTreeItem : item.getChildren()) {
                nodeElementList = collectAllItemsOnChange(diffNodeElementTreeItem, nodeElementList);
            }
        }
        return nodeElementList;
    }

}
