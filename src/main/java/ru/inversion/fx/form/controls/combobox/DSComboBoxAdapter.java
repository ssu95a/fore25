package ru.inversion.fx.form.controls.combobox;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import ru.inversion.dataset.*;
import ru.inversion.fx.form.IFXEntity;
import ru.inversion.fx.form.controls.JInvComboBox;

import java.util.function.Function;

/**
 *
 * @author ssu @
 */
public class DSComboBoxAdapter<T, P> extends ObservableListBase<T> implements ObservableList<T>, IDataSetNavigationListener, IDataSetRowListener {

    final private IDataSet<T> dataSet;
    final private JInvComboBox<T, P> comboBox;

    private boolean insideInSetCurrentRow = false;

    /**
     *
     */
    public DSComboBoxAdapter(IDataSet<T> dataSet, JInvComboBox<T, P> comboBox, IFXEntity fxEntity, Function<T, P> valueGetter) {

        this.dataSet = dataSet;
        this.comboBox = comboBox;

//        if( S.isNullOrEmpty(entityProperty) ) 
//        {
//           entityProperty = Controls.getFieldNameFromControl(comboBox);
//        }
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

//        if( !U.containsNull( valueGetter, entityProperty, fxEntity ) ) {
//        
//            final String propertyName = entityProperty; 
//
//            this.comboBox.valueProperty().addListener( new InvalidationListener() {
//                @Override
//                public void invalidated(Observable observable) {
//                    fxEntity.getProperty(propertyName).setValue( valueGetter.apply( (T)((ObjectProperty)observable).get() ) );
//                }
//            });
//        }
        this.dataSet.addRowListener(this);
        this.dataSet.addNavigationListener(this);
    }

    /**
     *
     */
    @Override
    public T get(int index) {
        return dataSet.getRow(index);
    }

    /**
     *
     */
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
        Platform.runLater(() -> {

            if (!insideInSetCurrentRow) {
                comboBox.getSelectionModel().select(e.getNewRowIndex());
            }
        });

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
