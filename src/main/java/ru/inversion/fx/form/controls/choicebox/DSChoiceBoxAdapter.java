package ru.inversion.fx.form.controls.choicebox;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import ru.inversion.dataset.*;
import ru.inversion.fx.form.IFXEntity;
import ru.inversion.fx.form.controls.JInvChoiceBox;

import java.util.function.Function;


/**
 *
 * @author ssu @
 * @param <T>
 * @param <P>
 */
public class DSChoiceBoxAdapter<T, P> extends ObservableListBase<T> implements ObservableList<T>, IDataSetNavigationListener, IDataSetRowListener {

    final private IDataSet<T> dataSet;
    final private JInvChoiceBox<T, P> comboBox;

    private boolean insideInSetCurrentRow = false;

    /**
     *
     * @param dataSet
     * @param comboBox
     * @param fxEntity
     * @param valueGetter */
    public DSChoiceBoxAdapter(IDataSet<T> dataSet, JInvChoiceBox<T, P> comboBox, IFXEntity fxEntity, Function<T, P> valueGetter) {

        this.dataSet = dataSet;
        this.comboBox = comboBox;

       
        this.comboBox.setValueFactory(valueGetter);
        this.comboBox.setItems(this);
         
        this.comboBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                insideInSetCurrentRow = true;
                dataSet.setCurrentRowNum(newValue.intValue());
                insideInSetCurrentRow = false;
            }
        });

        this.dataSet.addRowListener(this);
        this.dataSet.addNavigationListener(this);


        
        this.dataSet.addRowListener(this);
        this.dataSet.addNavigationListener(this);
    }

    /**
     *      */
    @Override
    public T get(int index) {
        return dataSet.getRow(index);
    }

    /**
     *      */
    @Override
    public int size() {
        return dataSet.getLoadedRowCount();
    }

    /**
     */
    public void elementsAdded(int index, int count) {
        beginChange();
        try {
            nextAdd(index, index + count);
        } finally {
            endChange();
        }
    }

    /**
     */
    @Override
    public void navigated(DataSetNavigationEvent e) {
        if (!insideInSetCurrentRow) {
            comboBox.getSelectionModel().select(e.getNewRowIndex());
        }
    }

    /**
     */
    @Override
    public void rowOperation(DataSetRowEvent event) {
        switch (event.getRowOperation()) {
            case INSERT:
                elementsAdded(event.getRowIndex(), event.getRowCount());
                break;
        }
    }
}
