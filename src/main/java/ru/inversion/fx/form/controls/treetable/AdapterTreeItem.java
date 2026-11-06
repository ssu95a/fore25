/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.treetable;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.IParameters;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Адаптер для получения TreeItem из списка элементов реализующих интерфейс
 * ITreeElem. ITreeElem содержит методы: getId() - ID элемента getParentId() -
 * ID родительского элемента
 *
 * @author perov
 * @param <T>
 * @param <P>
 */
public class AdapterTreeItem<T extends ITreeElem, P extends Comparable<P>> {

    public static final String PROPERTY_ADAPTER_TREE_ITEM = "ru.inversion.adaptertreeitem";

    private boolean isFisrtSql = true;
    private TaskContext tc;
    private Class entityClass;
    private String rootSql;
    private IParameters rootParam;
    private String childSql;
    private IParameters childParam;
    private final XXIDataSet<T> rootDataSet = new XXIDataSet<>();
    private final XXIDataSet<T> childDataSet = new XXIDataSet<>();
    private JInvTreeTable<T> table;
    private T item;

    public TreeItem<T> getRootItem(List<T> list) {

        final Map<P, Pair<T, TreeItem<T>>> map = new HashMap<>();
        final TreeItem<T> root = new TreeItem();
        //1. Заполняем map. Ключ  - ID элемента. Значение - пара Entity,TreeItem<Entity>
        list.stream().forEach((T p) -> {
            map.put((P) p.getId(), new Pair<>(p, new TreeItem<>(p)));
        });

        System.out.println(map.size());
        //Перебираем элементы map и формируем TreeItem
        map.values().forEach(new Consumer<Pair<T, TreeItem<T>>>() {
            @Override
            public void accept(Pair<T, TreeItem<T>> t) {
                System.out.println(t);
                P pid = (P) t.getFirst().getParentId();
                if (pid != null) {
                    Pair<T, TreeItem<T>> parent = map.get(pid);
                    if (parent != null) {
                        TreeItem<T> parentItem = parent.getSecond();
                        if (parentItem == null) {
                            parentItem = new TreeItem<>(parent.getFirst());
                            parent.second = parentItem;
                        }
                        parentItem.getChildren().add(t.getSecond());
                    }
                } else {
                    root.getChildren().add(t.getSecond());
                }
            }
        });

        return root;
    }

    public AdapterTreeItem() {
    }

    public AdapterTreeItem(JInvTreeTable<T> table, Class entityClass) {
        this.table = table;
        this.table.getProperties().put(PROPERTY_ADAPTER_TREE_ITEM, this);
        this.entityClass = entityClass;
    }

    /**
     * Построение дерева на основе запроса к БД
     *
     * @param item - екземпляр Entity класса которым типизирован TreeItem
     * @param tc - TaskContext
     * @param rootSql - SQL для построения стартового дерева
     * @param rootParam - параметры если есть    
     * @param childSql - SQL для поиска дочерних элементов
     * @param childParam - параметры если есть
     */
    public void bind(final T item, TaskContext tc, String rootSql, IParameters rootParam, String childSql, IParameters childParam) {

        this.tc = tc;
        this.item = item;

        this.rootSql = rootSql;
        this.rootParam = rootParam;
        this.rootDataSet.setTaskContext(tc);
        this.rootDataSet.setRowClass(entityClass);
        this.rootDataSet.setSQL(rootSql);
        this.rootDataSet.setCallbackParameters(rootParam);

        this.childSql = childSql;
        this.childParam = childParam;
        this.childDataSet.setTaskContext(tc);
        this.childDataSet.setRowClass(entityClass);
        this.childDataSet.setSQL(childSql);
        this.childDataSet.setCallbackParameters(childParam);

        beforeCreateNode();

        table.setRoot(createNode(item));
        table.setShowRoot(false);
    }

    /**
     * перед создание дерева
     */
    private void beforeCreateNode() {
        if (table == null) {
            throw new IllegalArgumentException("table is empty");
        }

        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass is empty");
        }

        if (tc == null) {
            throw new IllegalArgumentException("tc is empty");
        }

        if (item == null) {
            throw new IllegalArgumentException("item is empty");
        }

        if (rootSql == null) {
            throw new IllegalArgumentException("rootSql is empty");
        }

        if (childSql == null) {
            throw new IllegalArgumentException("childSql is empty");
        }

        isFisrtSql = true;
    }

    /**
     * Создание дерева
     *
     * @param item
     * @return
     */
    private TreeItem<T> createNode(final T item) {

        return new TreeItem<T>(item) {

            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override
            public ObservableList<TreeItem<T>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    try {
                        super.getChildren().setAll(buildChildren(this));
                    } catch (DataSetException ex) {
                        JInvErrorService.handleException(null, ex);
                    }
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {

                    isFirstTimeLeaf = false;
                    T t = (T) getValue();
                    try {
                        if (!hasChildren(t.getId())) {
                            isLeaf = true;
                        }
                    } catch (DataSetException ex) {
                        JInvErrorService.handleException(null, ex);
                    }
                }

                return isLeaf;
            }

            private ObservableList<TreeItem<T>> buildChildren(TreeItem<T> TreeItem) throws DataSetException {
                T f = TreeItem.getValue();

                if (f != null) {
                    Iterable<T> itr = null;
                    if (isFisrtSql) {
                        rootDataSet.executeQuery(true);
                        itr = () -> rootDataSet.getRowIterator(null);
                        isFisrtSql = false;
                    } else {
                        childDataSet.setParameter("id", f.getId());
                        childDataSet.executeQuery(true);
                        itr = () -> childDataSet.getRowIterator(null);

                    }

                    if (itr.iterator().hasNext()) {
                        ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
                        itr.forEach(new Consumer<T>() {
                            @Override
                            public void accept(T t) {
                                if (!t.getId().equals(f.getId())) {
                                    children.add(createNode(t));
                                }
                            }

                        });
                        return children;
                    }

                }

                return FXCollections.emptyObservableList();
            }

            private boolean hasChildren(Comparable id) throws DataSetException {

                Iterable<T> itr = null;

                if (isFisrtSql) {
                    rootDataSet.executeQuery(true);
                    itr = () -> rootDataSet.getRowIterator(null);

                } else {
                    childDataSet.setParameter("id", id);
                    childDataSet.executeQuery(true);
                    itr = () -> childDataSet.getRowIterator(null);
                }

                if (id == null) {
                    return itr.iterator().hasNext();
                } else {
                    while (itr.iterator().hasNext()) {

                        if (!itr.iterator().next().getId().equals(id)) {
                            return true;
                        }

                    }

                }

                return false;
            }

        };

    }

    /**
     * Установка фильтра*/
    public void showFilterDialog() {
//        beforeCreateNode();
//
//        JInvTreeTableFilterDialog jInvTreeTableFilterDialog = new JInvTreeTableFilterDialog(table, entityClass);
//        Optional<Map<String, String>> result = jInvTreeTableFilterDialog.show();
//        result.ifPresent((Map<String, String> map) -> {
//
//            try {
//                rootDataSet.applyFilterMap(map);
//            } catch (DataSetException ex) {
//                JInvErrorService.handleException(null, ex);
//            }
//
//            table.setRoot(createNode(item));
//
//        });
    }
    

    /**
     * Перестройка дерева
     */
    public void refreshTree() {
        if (table != null && entityClass != null) {
            beforeCreateNode();
            rootDataSet.clearFilter(true);
            table.setRoot(createNode(item));
        }
    }

}
