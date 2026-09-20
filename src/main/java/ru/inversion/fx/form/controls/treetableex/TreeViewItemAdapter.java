package ru.inversion.fx.form.controls.treetableex;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import ru.inversion.tds.ITreeDataSet;
import ru.inversion.tds.ITreeDataSetItem;
import ru.inversion.tds.TreeDataSetItem;
import ru.inversion.tds.TreeDataSetItemEvent;

import java.util.*;

/** */
public class TreeViewItemAdapter<P> extends TreeItem<P> implements ITreeDataSetItem<P> {

    private ITreeDataSet<P> dataSet;

    /**
     * for root items only
     */
    public TreeViewItemAdapter(ITreeDataSet<P> dataSet) {
        this.dataSet = dataSet;
        init();
    }

    /**
     * for root items only
     */
    public TreeViewItemAdapter( ITreeDataSet<P> dataSet, P value )
    {
        super(value);
        this.dataSet = dataSet;
        init();
    }

    /**
     * for root items only
     */
    public TreeViewItemAdapter(
            ITreeDataSet<P> dataSet,
            P value,
            Node graphic
    ) {
        super(value, graphic);
        this.dataSet = dataSet;
        init();
    }

    /**
     *
     */
    public TreeViewItemAdapter() {
        init();
    }

    /**
     *
     */
    public TreeViewItemAdapter(P value) {
        this(value, null);
    }

    /**
     *
     */
    public TreeViewItemAdapter(
            P value,
            Node graphic
    ) {
        super(value, graphic);
        init();
    }

    /**
     *
     */
    protected void init() {
        this.valueProperty().addListener(
            (observable, oldValue, newValue) -> {
                ITreeDataSet<P> ds = getDataSet();
                if (ds != null)
                    ds.fireItemEvent( new TreeDataSetItemEvent<>( TreeViewItemAdapter.this, false, oldValue, newValue ) );
            }
        );
    }

    /**
     *
     */
    @Override
    public boolean isLeaf() {
        return super.isLeaf();
    }

    /**
     * Перекрытый метод для выявления корневого узла.
     * <p>
     * В TreeView необходимо задавать один root item,
     * а в модели может быть несколько root-узлов.
     * Поэтому скрытый JavaFX-root считаем внешним контейнером,
     * а его детей — root-узлами модели.
     */
    @Override
    public boolean isRoot() {
        return this.getParent() == null || this.getParent().getParent() == null;
    }

    /**
     *
     */
    @Override
    public ITreeDataSetItem<P> newChildItem(P value) {
        return new TreeViewItemAdapter<>(value);
    }

    /**
     *
     */
    @Override
    public void addChild(ITreeDataSetItem<P> child) {

        if (child == null) {
            return;
        }

        TreeViewItemAdapter<P> item = cast(child);

        validateChildForAttach(item);

        if (!getChildren().contains(item)) {
            getChildren().add(item);
        }
    }

    /**
     *
     */
    @Override
    public void addChildrenAt(int position, List<ITreeDataSetItem<P>> newItems) {
        if (newItems == null || newItems.isEmpty())
            return;

        int safePosition = position;

        if (safePosition < 0)
            safePosition = 0;

        if (safePosition > getChildren().size())
            safePosition = getChildren().size();

        List<TreeViewItemAdapter<P>> fxItems = new ArrayList<>(newItems.size());

        for (ITreeDataSetItem<P> child : newItems) {
            TreeViewItemAdapter<P> item = cast(child);
            validateChildForAttach(item);

            if (getChildren().contains(item) || fxItems.contains(item))
                throw new IllegalArgumentException("Child TreeItem is already attached to this parent");

            fxItems.add(item);
        }

        getChildren().addAll(safePosition, fxItems);
    }

    /**
     *
     */
    @Override
    public void removeChildren(Collection<ITreeDataSetItem<P>> items) {

        if (items == null || items.isEmpty()) {
            return;
        }

        for (ITreeDataSetItem<P> item : new ArrayList<>(items)) {
            getChildren().remove(cast(item));
        }
    }

    /**
     *
     */
    @Override
    public List<ITreeDataSetItem<P>> getChildrenList() {
        return Collections.unmodifiableList(children());
    }


    /**
     *
     */
    @Override
    public int getChildCount() {
        return super.getChildren().size();
    }

    /**
     *
     */
    @Override
    public ITreeDataSet<P> getDataSet() {

        if (dataSet != null)
            return dataSet;

        ITreeDataSetItem<P> parent = getParentItem();

        if (parent == null)
            return null;

        return parent.getDataSet();
    }

    /**
     *
     */
    @Override
    public ITreeDataSetItem<P> getParentItem() {

        TreeItem<P> parent = getParent();

        if (parent instanceof ITreeDataSetItem)
            return (ITreeDataSetItem<P>) parent;

        return null;
    }

    /**
     *
     */
    @Override
    public void executeQuery() {
        throw new UnsupportedOperationException("executeQuery");
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    protected List<ITreeDataSetItem<P>> children() {
        return (List<ITreeDataSetItem<P>>) (Object) getChildren();
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    private TreeViewItemAdapter<P> cast(ITreeDataSetItem<P> item) {
        return (TreeViewItemAdapter<P>) item;
    }

    /**
     *
     */
    private void validateChildForAttach( ITreeDataSetItem<P> child )
    {
        Objects.requireNonNull( child, "'child' is null" );

        if( child == this )
            throw new IllegalArgumentException( "Can not add item as child of itself" );

        ITreeDataSetItem<P> parentItem = getParentItem();

        while (parentItem != null)
        {
            if( parentItem == child )
                throw new IllegalArgumentException( "Can not add ancestor item as child" );

            parentItem = parentItem.getParentItem();
        }

        final ITreeDataSetItem<P> currentParent = child.getParentItem();

        if( currentParent != null && currentParent != this )
            throw new IllegalArgumentException( "Child item already has another parent" );
    }
}