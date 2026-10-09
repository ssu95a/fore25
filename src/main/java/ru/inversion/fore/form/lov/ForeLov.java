package ru.inversion.fore.form.lov;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import ru.inversion.fore.form.FormTools;

import java.text.MessageFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * Неблокирующее окно одиночного выбора. Один экземпляр обслуживает один вызов за раз.
 * Источник выполняется в рабочем потоке, состояние окна принадлежит потоку JavaFX.
 */
public final class ForeLov implements AutoCloseable
{
   private static final ResourceBundle TEXT = ResourceBundle.getBundle("ru.inversion.fore.form.lov.lov");
   private static final Executor WORKER = command -> Thread.ofVirtual().name("fore-lov").start(command);
   private LovDefinition definition;
   private LovDataSource source;
   private final String designTitle;
   private final Map<String, String> designColumnTitles;
   private final Executor executor;
   private final ReadOnlyObjectWrapper<Throwable> error = new ReadOnlyObjectWrapper<>();
   private FutureTask<LovDataSource.Result> task;
   private CompletableFuture<LovDataSource.Result> pending;
   private long generation;
   private LovDataSource.Request cachedRequest;
   private LovDataSource.Result cachedResult;
   private View view;
   private boolean closed;

   public ForeLov(LovDefinition definition, LovDataSource source)
   {
      this(definition, source, WORKER);
   }

   /** Исполнитель обязан выполнять запросы вне потока JavaFX. */
   public ForeLov(LovDefinition definition, LovDataSource source, Executor executor)
   {
      this.definition = Objects.requireNonNull(definition);
      this.source = Objects.requireNonNull(source);
      this.designTitle = definition.title();
      this.designColumnTitles = new HashMap<>();
      definition.columns().forEach(column -> designColumnTitles.put(column.name(), column.title()));
      this.executor = Objects.requireNonNull(executor);
   }

   public LovDefinition getDefinition() { return definition; }
   public ReadOnlyObjectProperty<Throwable> errorProperty() { return error.getReadOnlyProperty(); }
   public Throwable getError() { return error.get(); }
   public boolean isShowing() { return view != null; }

   /** Панель существует только во время показа; предназначена для CSS и интеграции интерфейса. */
   public DialogPane getDialogPane() { return view == null ? null : view.dialog.getDialogPane(); }

   /** Изменяет свойства, соответствующие SET_LOV_PROPERTY, на экземпляре LOV. */
   public void setTitle(String title)
   {
      checkOpen();
      replace(definitionWith(definition, title == null ? designTitle : title, definition.window(), definition.behavior(), definition.columns()), false);
   }

   public void setAutoRefresh(boolean value)
   {
      checkOpen();
      var old = definition.behavior();
      var behavior = new LovDefinition.Behavior(old.autoDisplay(), value, old.autoSelect(), old.autoSkip(),
            old.filterBeforeDisplay(), old.validateFromList(), old.key());
      replace(definitionWith(definition, definition.title(), definition.window(), behavior, definition.columns()), true);
   }

   /** Размер в логических пикселях JavaFX. */
   public void setSize(double width, double height)
   {
      checkOpen();
      var old = definition.window();
      var window = new LovDefinition.Window(width, height, old.x(), old.y(), old.automaticPosition(),
            old.automaticColumnWidth(), old.direction(), old.styleClass());
      replace(definitionWith(definition, definition.title(), window, definition.behavior(), definition.columns()), false);
   }

   /** Координаты окна; null/null возвращают автоматическое размещение по умолчанию. */
   public void setPosition(Double x, Double y)
   {
      checkOpen();
      var old = definition.window();
      var window = new LovDefinition.Window(old.width(), old.height(), x, y, old.automaticPosition(),
            old.automaticColumnWidth(), old.direction(), old.styleClass());
      replace(definitionWith(definition, definition.title(), window, definition.behavior(), definition.columns()), false);
   }

   /** Изменяет TITLE или WIDTH колонки, как SET_LOV_COLUMN_PROPERTY. Ноль скрывает колонку. */
   public void setColumnTitle(String name, String title)
   {
      setColumn(name, title, null, true);
   }

   public void setColumnWidth(String name, double width)
   {
      setColumn(name, null, width, false);
   }

   /** Меняет источник вместе с его уже разрешённым Java-адаптером. */
   public void setSource(LovDefinition.Source source, LovDataSource adapter)
   {
      checkOpen();
      Objects.requireNonNull(source, "source");
      this.source = Objects.requireNonNull(adapter, "adapter");
      this.definition = new LovDefinition(definition.id(), definition.title(), definition.comment(), definition.window(),
            definition.behavior(), definition.search(), definition.columns(), source,
            definition.binding(), definition.appearance());
      invalidate();
      if( view != null ) view.search(true);
   }

   private void setColumn(String name, String title, Double width, boolean titleChanged)
   {
      checkOpen();
      var columns = new java.util.ArrayList<LovDefinition.Column>(definition.columns());
      int index = -1;
      for( int i = 0; i < columns.size(); i++ ) if( columns.get(i).name().equals(name) ) { index = i; break; }
      if( index < 0 ) throw new IllegalArgumentException("Неизвестная колонка LOV: " + name);
      var old = columns.get(index);
      columns.set(index, new LovDefinition.Column(old.name(), !titleChanged ? old.title() : title == null ? designColumnTitles.get(old.name()) : title, old.type(),
            width == null ? old.width() : width, old.returnTo(), old.length()));
      replace(definitionWith(definition, definition.title(), definition.window(), definition.behavior(), columns), true);
   }

   private void replace(LovDefinition value, boolean reload)
   {
      this.definition = value;
      invalidate();
      if( view != null )
      {
         view.refreshMetadata();
         if( reload ) view.search(true);
      }
   }

   private static LovDefinition definitionWith(LovDefinition old, String title, LovDefinition.Window window,
                                                LovDefinition.Behavior behavior, java.util.List<LovDefinition.Column> columns)
   {
      return new LovDefinition(old.id(), title, old.comment(), window, behavior, old.search(), columns, old.source(),
            old.binding(), old.appearance());
   }

   public CompletableFuture<Optional<LovRow>> show(Node invoker, Map<String, Object> parameters, String initialText)
   { return show(invoker, parameters, initialText, null); }

   /** Показывает LOV с разовым аналогом List X/Y Position на уровне вызова. */
   public CompletableFuture<Optional<LovRow>> show(Node invoker, Map<String, Object> parameters, String initialText,
                                                   LovDefinition.Position position)
   {
      checkOpen();
      if( view != null ) throw new IllegalStateException("LOV уже открыт");
      if( invoker == null || invoker.getScene() == null || invoker.getScene().getWindow() == null
            || !invoker.getScene().getWindow().isShowing() )
         throw new IllegalArgumentException("LOV требует видимый элемент вызывающей формы");
      cancelQuery();
      View current = new View(invoker, parameters, initialText == null ? "" : initialText, position);
      view = current;
      try
      {
         current.dialog.show();
         current.position(invoker);
         if( !definition.behavior().filterBeforeDisplay() || !current.query.getText().isEmpty() )
            current.search(false);
      }
      catch( RuntimeException ex )
      {
         current.result.completeExceptionally(ex);
         current.dialog.close();
         view = null;
      }
      return current.result;
   }

   public void refresh()
   {
      checkOpen();
      invalidate();
      if( view != null ) view.search(true);
   }

   /** Кэш содержит только последнюю выборку с теми же параметрами и условием поиска. */
   public void invalidate()
   {
      FormTools.requireFxThread();
      cachedRequest = null;
      cachedResult = null;
   }

   CompletableFuture<LovDataSource.Result> query(String text, LovDefinition.MatchMode mode,
                                                Map<String, Object> parameters, boolean force)
   {
      checkOpen();
      cancelQuery();
      error.set(null);
      LovDefinition requestDefinition = definition;
      LovDataSource requestSource = source;
      var request = new LovDataSource.Request(requestDefinition, text, mode, parameters);
      if( !force && !requestDefinition.behavior().autoRefresh() && request.equals(cachedRequest) )
         return CompletableFuture.completedFuture(cachedResult);
      invalidate();
      long ticket = generation;
      var result = new CompletableFuture<LovDataSource.Result>();
      pending = result;
      task = new FutureTask<>(() -> {
         if( Platform.isFxApplicationThread() )
            throw new IllegalStateException("Источник LOV не должен выполняться в потоке JavaFX");
         LovDataSource.Result loaded = Objects.requireNonNull(requestSource.fetch(request), "Источник вернул null");
         if( loaded.rows().size() > request.maxRows() )
            throw new IllegalArgumentException("Источник превысил max-rows");
         for( LovRow row : loaded.rows() )
         {
            for( var column : request.definition().columns() ) column.convert(row.get(column.name()));
            if( !request.matches(row) )
               throw new IllegalArgumentException("Источник вернул строку, не соответствующую условию поиска");
         }
         return loaded;
      }) {
         @Override protected void done()
         {
            Platform.runLater(() -> {
               if( ticket != generation ) return;
               task = null;
               pending = null;
               try
               {
                  LovDataSource.Result loaded = get();
                  if( !requestDefinition.behavior().autoRefresh() )
                  {
                     cachedRequest = request;
                     cachedResult = loaded;
                  }
                  result.complete(loaded);
               }
               catch( Exception ex )
               {
                  Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                  error.set(cause);
                  result.completeExceptionally(cause);
               }
            });
         }
      };
      try { executor.execute(task); }
      catch( RuntimeException ex )
      {
         task = null;
         pending = null;
         error.set(ex);
         result.completeExceptionally(ex);
      }
      return result;
   }

   void cancelQuery()
   {
      FormTools.requireFxThread();
      generation++;
      FutureTask<?> oldTask = task;
      CompletableFuture<?> oldResult = pending;
      task = null;
      pending = null;
      if( oldTask != null ) oldTask.cancel(true);
      if( oldResult != null ) oldResult.cancel(false);
   }

   private void checkOpen()
   {
      FormTools.requireFxThread();
      if( closed ) throw new IllegalStateException("LOV закрыт");
   }

   @Override public void close()
   {
      FormTools.requireFxThread();
      if( closed ) return;
      closed = true;
      cancelQuery();
      if( view != null ) view.dialog.close();
      invalidate();
   }

   private static String text(String key, Object... arguments)
   {
      return MessageFormat.format(TEXT.getString(key), arguments);
   }

   private final class View
   {
      private final Dialog<LovRow> dialog = new Dialog<>();
      private final TableView<LovRow> table = new TableView<>();
      private final TextField query = new TextField();
      private final Label status = new Label(text("enterFilter"));
      private final ProgressIndicator progress = new ProgressIndicator();
      private final CompletableFuture<Optional<LovRow>> result = new CompletableFuture<>();
      private final Map<String, Object> parameters;
      private final Button accept;
      private final LovDefinition.Position invocationPosition;
      private boolean loading;
      private long requestNumber;

      private View(Node invoker, Map<String, Object> parameters, String initialText, LovDefinition.Position invocationPosition)
      {
         this.invocationPosition = invocationPosition;
         this.parameters = new LovDataSource.Request(definition, "", definition.search().mode(), parameters).parameters();
         dialog.initOwner(invoker.getScene().getWindow());
         dialog.initModality(Modality.WINDOW_MODAL);
         dialog.setTitle(definition.title());
         dialog.setResizable(true);
         var pane = dialog.getDialogPane();
         pane.setId("fore-lov");
         pane.getStyleClass().add("fore-lov");
         if( !definition.window().styleClass().isBlank() )
            pane.getStyleClass().addAll(definition.window().styleClass().strip().split("\\s+"));
         pane.getStylesheets().addAll(invoker.getScene().getStylesheets());
         pane.setNodeOrientation(switch( definition.window().direction() )
         {
            case INHERIT -> invoker.getEffectiveNodeOrientation();
            case LEFT_TO_RIGHT -> NodeOrientation.LEFT_TO_RIGHT;
            case RIGHT_TO_LEFT -> NodeOrientation.RIGHT_TO_LEFT;
         });
         var ok = new ButtonType(text("select"), ButtonBar.ButtonData.OK_DONE);
         pane.getButtonTypes().addAll(ok, new ButtonType(text("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
         accept = (Button) pane.lookupButton(ok);
         accept.setDefaultButton(false);
         accept.setDisable(true);
         table.setId("lov-table");
         table.setEditable(false);
         table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
         table.setItems(FXCollections.emptyObservableList());
         table.setPlaceholder(new Label(text("empty")));
         rebuildColumns();
         table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, value) -> updateAccept());
         table.setRowFactory(unused -> {
            var row = new TableRow<LovRow>();
            if( !definition.appearance().rowLineColor().isEmpty() )
               row.setStyle("-fx-border-color: transparent transparent " + definition.appearance().rowLineColor() + " transparent; -fx-border-width: 0 0 1 0;");
            row.setOnMouseClicked(event -> {
               if( event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty() )
                  accept.fire();
            });
            return row;
         });
         table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if( event.getCode() == KeyCode.ENTER ) { accept.fire(); event.consume(); }
         });
         query.setId("lov-query");
         query.setText(initialText);
         query.setPromptText(text("searchBy", definition.searchColumn().title()));
         query.setAccessibleText(query.getPromptText());
         var find = new Button(text("find"));
         find.setId("lov-search");
         find.setOnAction(event -> search(false));
         query.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if( event.getCode() == KeyCode.ENTER ) { search(false); event.consume(); }
         });
         query.textProperty().addListener((observable, oldValue, value) -> {
            requestNumber++;
            cancelQuery();
            clear();
            status.setText(text("enterFilter"));
         });
         progress.setPrefSize(20, 20);
         progress.setVisible(false);
         progress.setManaged(false);
         var searchBar = new HBox(8, query, find, progress);
         HBox.setHgrow(query, Priority.ALWAYS);
         status.setId("lov-status");
         status.setWrapText(true);
         var content = new VBox(10, searchBar, table, status);
         content.setPadding(new Insets(8));
         VBox.setVgrow(table, Priority.ALWAYS);
         pane.setContent(content);
         pane.setPrefSize(definition.window().width(), definition.window().height());
         applyAppearance();
         dialog.setResultConverter(button -> button == ok ? table.getSelectionModel().getSelectedItem() : null);
         dialog.setOnHidden(event -> {
            requestNumber++;
            cancelQuery();
            clear();
            view = null;
            if( definition.behavior().autoRefresh() ) invalidate();
            result.complete(Optional.ofNullable(dialog.getResult()));
         });
      }

      private void updateAccept()
      {
         accept.setDisable(loading || table.getSelectionModel().getSelectedItem() == null);
      }

      private void rebuildColumns()
      {
         table.getColumns().clear();
         for( var column : definition.columns() )
         {
            if( !column.visible() ) continue;
            var displayed = new TableColumn<LovRow, Object>(column.title());
            displayed.setId(column.name());
            displayed.setSortable(false);
            displayed.setReorderable(false);
            displayed.setPrefWidth(column.width());
            displayed.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get(column.name())));
            displayed.setCellFactory(unused -> new TableCell<>() {
               @Override protected void updateItem(Object value, boolean empty)
               {
                  super.updateItem(value, empty);
                  setText(empty ? null : LovDefinition.ValueType.text(value));
                  setAlignment(column.type() == LovDefinition.ValueType.STRING || column.type() == LovDefinition.ValueType.BOOLEAN
                        ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
               }
            });
            table.getColumns().add(displayed);
         }
      }

      private void applyAppearance()
      {
         LovDefinition.Appearance appearance = definition.appearance();
         var style = new StringBuilder();
         if( !appearance.fontName().isEmpty() ) style.append("-fx-font-family: '").append(css(appearance.fontName())).append("';");
         if( appearance.fontSize() > 0 ) style.append("-fx-font-size: ").append(appearance.fontSize()).append("px;");
         if( !appearance.fontWeight().isEmpty() ) style.append("-fx-font-weight: ").append(appearance.fontWeight()).append(';');
         if( !appearance.fontStyle().isEmpty() ) style.append("-fx-font-style: ").append(appearance.fontStyle()).append(';');
         if( !appearance.foregroundColor().isEmpty() ) style.append("-fx-text-fill: ").append(appearance.foregroundColor()).append(';');
         if( !appearance.backgroundColor().isEmpty() ) style.append("-fx-background-color: ").append(appearance.backgroundColor()).append(';');
         dialog.getDialogPane().setStyle(style.toString());
         var tableStyle = new StringBuilder();
         if( !appearance.backgroundColor().isEmpty() ) tableStyle.append("-fx-background-color: ").append(appearance.backgroundColor()).append(';');
         if( !appearance.foregroundColor().isEmpty() ) tableStyle.append("-fx-text-fill: ").append(appearance.foregroundColor()).append(';');
         table.setStyle(tableStyle.toString());
      }

      private void refreshMetadata()
      {
         dialog.setTitle(definition.title());
         dialog.getDialogPane().setPrefSize(definition.window().width(), definition.window().height());
         query.setPromptText(text("searchBy", definition.searchColumn().title()));
         rebuildColumns();
         applyAppearance();
         clear();
      }

      private static String css(String value) { return value.replace("'", "\\'"); }

      private void clear()
      {
         table.setItems(FXCollections.emptyObservableList());
         loading = false;
         progress.setVisible(false);
         progress.setManaged(false);
         updateAccept();
      }

      private void search(boolean force)
      {
         long number = ++requestNumber;
         cancelQuery();
         clear();
         if( query.getText().length() < definition.search().minLength() )
         {
            status.setText(text("minLength", definition.search().minLength()));
            return;
         }
         loading = true;
         progress.setVisible(true);
         progress.setManaged(true);
         status.setText(text("loading"));
         ForeLov.this.query(query.getText(), definition.search().mode(), parameters, force)
               .whenComplete((loaded, failure) -> {
                  if( view != this || number != requestNumber ) return;
                  clear();
                  if( failure != null )
                  {
                     if( !(failure instanceof CancellationException) ) status.setText(text("error"));
                     return;
                  }
                  // Два представления над тем же списком; массив строк не копируется.
                  table.setItems(FXCollections.unmodifiableObservableList(FXCollections.observableList(loaded.rows())));
                  status.setText(text(loaded.complete() ? "count" : "limited", loaded.rows().size()));
                  if( !loaded.rows().isEmpty() ) table.getSelectionModel().selectFirst();
                  if( definition.behavior().autoSelect() && loaded.complete() && loaded.rows().size() == 1 )
                     accept.fire();
               });
      }

      private void position(Node invoker)
      {
         dialog.getDialogPane().applyCss();
         if( definition.window().automaticColumnWidth() )
            for( var column : table.getColumns() )
            {
               var heading = new Text(column.getText());
               heading.setFont(query.getFont());
               for( Node header : table.lookupAll(".column-header") )
                  if( header instanceof TableColumnHeader actual && actual.getTableColumn() == column
                        && actual.lookup(".label") instanceof Label label )
                     heading.setFont(label.getFont());
               column.setPrefWidth(Math.max(column.getPrefWidth(), heading.getLayoutBounds().getWidth() + 28));
            }
         Bounds anchor = invoker.localToScreen(invoker.getBoundsInLocal());
         Rectangle2D screen = anchor == null ? Screen.getPrimary().getVisualBounds() :
               Screen.getScreensForRectangle(anchor.getMinX(), anchor.getMinY(), anchor.getWidth(), anchor.getHeight())
                     .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
         dialog.setWidth(Math.min(definition.window().width(), screen.getWidth()));
         dialog.setHeight(Math.min(definition.window().height(), screen.getHeight()));
         LovDefinition.Position configured = invocationPosition != null ? invocationPosition : definition.binding().position();
         double x = configured != null ? configured.x() : definition.window().x() == null ? dialog.getX() : definition.window().x();
         double y = configured != null ? configured.y() : definition.window().y() == null ? dialog.getY() : definition.window().y();
         if( configured == null && definition.window().automaticPosition() && anchor != null )
         {
            x = anchor.getMinX();
            y = anchor.getMaxY();
         }
         dialog.setX(Math.max(screen.getMinX(), Math.min(x, screen.getMaxX() - dialog.getWidth())));
         dialog.setY(Math.max(screen.getMinY(), Math.min(y, screen.getMaxY() - dialog.getHeight())));
         query.requestFocus();
      }
   }
}
