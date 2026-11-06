package ru.inversion.fx.form.controls.filter.impl;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvLabel;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterFull;
import ru.inversion.fx.form.controls.progress.ProgressCallback;
import ru.inversion.fx.form.controls.progress.ProgressTaskExecutor;
import ru.inversion.fx.form.lov.JInvFileChooserLov;


public final class FilterExportController extends JInvFXFormController<PFrmFilterFull> {

    private static final OpenOption[] WRITE_OPTIONS = { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };
    private static final String HOME_DIR = System.getProperty("user.home");
    private static final String DEFAULT_FILENAME = "filters.inv";
    private static final ProgressTaskExecutor<Void, Void> PROGRESS_TASK_EXECUTOR = new ProgressTaskExecutor<>();

    @FXML private AnchorPane rootPane, progressPane;

    @FXML private GridPane gridPane;

    @FXML private JInvTextField exportPath;

    @FXML private JInvCheckBox openAfterExport;

    private XXIDataSet<PFrmFilterFull> dsFilter;

    private long markedCount;

    private long currentRowId;

    @Override
    protected void init() throws Exception {
        setTitle(getBundleString("FILTER_EXPORT_TITLE"));

        dsFilter = (XXIDataSet<PFrmFilterFull>) initProperties.get("dsFilter");

        markedCount = dsFilter.computeNumberMarkedRows();
        currentRowId = dsFilter.getCurrentRow().getID();

        // Если экспортируем методом маркировки - показываем кол-во экспортируемых фильтров
        if (markedCount > 0) {
            JInvLabel exportLabel = new JInvLabel();
            exportLabel.setTextAlignment(TextAlignment.CENTER);
            String exportCount = String.valueOf(dsFilter.computeNumberMarkedRows());
            exportLabel.setText(String.format(getBundleString("FILTER_EXPORT_COUNT.TEXT"), exportCount));

            // Добавляем на grid панель
            insertRows(gridPane, 1); // Смещаем позицию каждой строки в gridPane, для добавления новой строки
            gridPane.getRowConstraints().get(0).setMaxHeight(15); // Высота первой строки в gridPane
            gridPane.add(exportLabel, 0, 0);
            GridPane.setConstraints(exportLabel, 0, 0, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
        }

        JInvFileChooserLov jInvFileChooserLov = new JInvFileChooserLov();
        jInvFileChooserLov.showSaveProperty().set(true);
        jInvFileChooserLov.getExtensionFilters().add(new FileChooser.ExtensionFilter("Invo (*.inv)", "*.inv"));

        exportPath.setLOV(jInvFileChooserLov);

        // Заполняем путь для экспорта (по умолчанию)
        try {
            exportPath.setText(Paths.get(HOME_DIR).resolve(DEFAULT_FILENAME).toString());
        } catch (Exception e) {
            JInvErrorService.handleException(getViewContext(), e);
        }

        getViewContext().getStage().sizeToScene();
    }

    @Override
    protected boolean onOK() {
        // Экспортируем по маркеру или текущую строку таблицы
        Map<String, Object> params = new HashMap<>();
        if (markedCount > 0) {
            //Коммитим маркер, чтобы было видно из другой сессии
            dsFilter.getTaskContext().commit();
            params.put("markerId", dsFilter.getMarkerID());
            params.put("clearMrk", "Y");
            execute("exportByMark", params);
        } else if (currentRowId > 0) {
            params.put("filterId", currentRowId);
            execute("exportByCurrentRow", params);
        }

        return false;
    }

    private void execute(String procedureName, Map<String, Object> params) {
        if (!PROGRESS_TASK_EXECUTOR.isDone()) return;

        PROGRESS_TASK_EXECUTOR.callback((ProgressCallback<Void, Void>) (progress, parameter) -> {
            progress.before(getBundleString("FILTER_EXPORT_STATUS_START"));
            try {
                progress.updateMessage(getBundleString("FILTER_EXPORT_STATUS_SELECT"));
                FilterWork.createBufferAndExecute( procedureName, params ); // Создаём запрос

                progress.updateMessage(getBundleString("FILTER_EXPORT_STATUS_GET.RESULT"));
                Optional<String> result = FilterWork.getBufferTab(); // Получаем результат экспорта
                FilterWork.cleanAllBuffer(); // Чистим за собой

                if (result.isPresent() && !result.get().isEmpty()) {
                    try {
                        progress.updateMessage(getBundleString("FILTER_EXPORT_STATUS_SAVE.TO.FILE"));
                        Path filterFilePath = (Paths.get(exportPath.getText()));
                        saveFilter(filterFilePath, result.get().getBytes());
                        Platform.runLater(() -> {
                            // Открыть файл после экспорта
                            if (openAfterExport.isSelected()) {
                                // Если указана программа по умолчанию (для открытия такого рода файла) -
                                // открываем используя эту программу
                                // Если программа не указана - перехватываем exception и открываем папку с файлом
                                try {
                                    Desktop.getDesktop().open(filterFilePath.toFile());
                                } catch (IOException e) {
                                    try {
                                        Desktop.getDesktop().browse(filterFilePath.getParent().toUri());
                                    } catch (IOException ignored) {}
                                }
                            }
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            JInvErrorService.handleException(getViewContext(), e);
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        Alerts.error(getViewContext(), getBundleString("ERROR_MSG"), getBundleString("FILTER_EXPORT_EMPTY_RESULT"));
                    });
                }

            } catch (SQLException e) {
                Platform.runLater(() -> {
                    JInvErrorService.handleException(getViewContext(), e);
                });
            }
            progress.end(getBundleString("FILTER_EXPORT_STATUS_FINISH"));
            return null;
        });
        PROGRESS_TASK_EXECUTOR.pane(progressPane);
        PROGRESS_TASK_EXECUTOR.execute();
    }

    private void saveFilter(Path file, byte[] bytes) throws IOException {
        Path parent = file.getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(file, bytes, WRITE_OPTIONS);
    }

    private void insertRows(GridPane gridPane, int count) {
        for (Node child : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(child);
            GridPane.setRowIndex(child, rowIndex == null ? count : count + rowIndex);
        }
    }
}
