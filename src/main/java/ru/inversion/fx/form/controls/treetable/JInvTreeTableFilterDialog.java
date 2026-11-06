/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.treetable;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import ru.inversion.fx.form.controls.JInvTextField;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author perov
 */
public class JInvTreeTableFilterDialog {

    private final JInvTreeTable table;
    private final Map<String, Node> map = new HashMap<>();
    private final Class clazz;

    public JInvTreeTableFilterDialog(JInvTreeTable table, Class clazz) {
        this.table = table;
        this.clazz = clazz;
    }

    public Optional<Map<String, String>> show() {

        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Фильтр");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setVgap(5.0);
        grid.setHgap(5.0);
        
        for (int i = 0; i < table.getColumns().size(); i++) {
            if (table.getColumns().get(i) instanceof JInvTreeTableColumn) {
                JInvTreeTableColumn col = (JInvTreeTableColumn) table.getColumns().get(i);
                if (col.getFieldName() != null) {
                    String labelText = col.getFieldName();
                    if (!col.getText().isEmpty()) {
                        labelText = col.getText();
                    }
                    grid.add(new Label(labelText), 0, i);
                    JInvTextField textField = new JInvTextField();
                    map.put(col.getFieldName(), textField);
                    grid.add(textField, 1, i);
                }
            }
        }
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(new Callback<ButtonType, Map<String, String>>() {
            @Override
            public Map<String, String> call(ButtonType param) {

                if (param == ButtonType.OK) {

                    return map.entrySet().stream()
                            .filter(m -> m.getValue() != null)
                            .filter(m -> m.getValue() instanceof JInvTextField)
                            .filter(m -> ((JInvTextField) m.getValue()).getText() != null)
                            .collect(Collectors.toMap(p -> p.getKey(), p -> ((JInvTextField) p.getValue()).getText()));
                }
                return null;
            }

        });
        return dialog.showAndWait();
    }
//
//    public Optional<ButtonType> show2() {
//
//        Dialog<ButtonType> dialog = new Dialog<>();
//        dialog.setTitle("Title");
//        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
//
//        GridPane grid = new GridPane();
//
//        for (int i = 0; i < table.getColumns().size(); i++) {
//            if (table.getColumns().get(i) instanceof JInvTreeTableColumn) {
//                JInvTreeTableColumn col = (JInvTreeTableColumn) table.getColumns().get(i);
//                if (col.getFieldName() != null) {
//                    String labelText = col.getFieldName();
//                    if (!col.getText().isEmpty()) {
//                        labelText = col.getText();
//                    }
//                    grid.add(new Label(labelText), 0, i);
//                    JInvTextField textField = new JInvTextField();
//                    map.put(col.getFieldName(), textField);
//                    grid.add(textField, 1, i);
//                }
//            }
//        }
//        dialog.getDialogPane().setContent(grid);
//        dialog.setResultConverter(new Callback<ButtonType, ButtonType>() {
//            @Override
//            public ButtonType call(ButtonType param) {
//                if (param == ButtonType.OK) {
//                    setFilter();
//                }
//                return param;
//            }
//
//        });
//        return dialog.showAndWait();
//    }
//
//    List<Pair<Method, String>> listMethodArgument = new LinkedList();
//
//    private void setFilter() {
//
//        System.out.println(clazz.getName());
//
//        //заполняем map -> метод + значение
//        map.entrySet().stream().forEach((Entry<String, node> e) -> {
//
//            String key = e.getKey();
//            String value = ((JInvTextField) e.getValue()).getText();
//
//            if (key != null && value != null) {
//                System.out.println(key + " " + value);
//
//                for (Method m : clazz.getMethods()) {
//
//                    if (m.getName().toUpperCase().equals("GET" + key.toUpperCase())) {
//                        listMethodArgument.add(new Pair<>(m, value));
//                        break;
//                    }
//
//                }
//            }
//        });
//
//        filtred(table.getRoot());
//
//    }
//
//    private boolean accept(TreeItem item) {
//        boolean ret = false;
//        for (Pair<Method, String> p : listMethodArgument) {
//            Object[] args = new Object[]{};            
//            try {
//                String invoke = p.first.invoke(item.getValue(), args).toString();
//                System.out.println(invoke + " = " + p.second);
//                if (invoke.equals(p.second)) {
//                    ret = true;
//                } else {
//                    return false;
//                }
//            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
//                JInvErrorService.handleException(null, ex);
//            }
//        }
//        return ret;
//    }
//
//    private Map<String, String> getValueFilterMap() {
//        return map.entrySet().stream()
//                .filter(m -> m.getValue() != null)
//                .filter(m -> m.getValue() instanceof JInvTextField)
//                .filter(m -> ((JInvTextField) m.getValue()).getText() != null)
//                .collect(Collectors.toMap(p -> p.getKey(), p -> ((JInvTextField) p.getValue()).getText()));
//    }
//
//    private void filtred(TreeItem item) {
//        if (item != null) {
//            if (!item.getChildren().isEmpty()) {
//                item.getChildren().stream().forEach((i) -> {
//                    filtred((TreeItem) i);
//                });
//            }
//            //если показываем элемент root 
//            if (table.showRootProperty().get()) {
//
//            } //если не показываем элемент root 
//            else if (item.getParent() != null) {  
//                
//                if (accept(item)){                    
//                    System.out.println( item.getValue().toString());
//                    item.setExpanded(true);
//                }
//                else {
//                    item.setExpanded(false);
//                }
//                
//            }
//        }
//    }

}
