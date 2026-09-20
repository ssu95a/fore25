package ru.inversion.fx.form.controls;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Pair;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.IViewPrefSaver;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.form.controls.skin.JInvTabPaneSkin;
import ru.inversion.utils.S;

/**
 Для вида "палитра" прописывайте в FXML side="LEFT" (см. JAVAKERNEL-1408)
 @author fomishkin on 06.12.2017. */
public class JInvTabPane extends TabPane implements IJInvControl, IViewChangeable {
    private static final AtomicLong idGenerator = new AtomicLong();
    private static final ResourceBundle FORE = ResourceBundle.getBundle( "fore" );
    private static final String HAS_DRAG_HANDLERS = "ru.inversion.has_drag_handlers";
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    private final String dragID = "TabDrag-" + idGenerator.incrementAndGet();
    private IViewPrefSaver prefSaver;
    private Tab currentDraggingTab;

    public JInvTabPane() {
        this((Tab[])null);
    }

    public JInvTabPane( final Tab... tabs ) {
        super( tabs );
        setSkin( new JInvTabPaneSkin(this) );
        addDragSupport();
    }

    public void addDragSupport() {
        getTabs().forEach( this::addDragHandlers );
        getTabs().addListener( ( ListChangeListener.Change<? extends Tab> c ) -> {
            while ( c.next() ) {
                if ( c.wasAdded() ) {
                    c.getAddedSubList().forEach( this::addDragHandlers );
                }
            }
        } );
        // Добавляем вкладку в конец, если перетянули на панель, но не на другую вкладку
        setOnDragOver( e -> {
            if ( dragID.equals( e.getDragboard().getString() ) &&
                    currentDraggingTab != null &&
                    currentDraggingTab.getTabPane() != this ) {
                e.acceptTransferModes( TransferMode.MOVE );
            }
        } );
        setOnDragDropped( e -> {
            if ( dragID.equals( e.getDragboard().getString() ) &&
                    currentDraggingTab != null &&
                    currentDraggingTab.getTabPane() != this ) {
                currentDraggingTab.getTabPane().getTabs().remove( currentDraggingTab );
                getTabs().add( currentDraggingTab );
                currentDraggingTab.getTabPane().getSelectionModel().select( currentDraggingTab );
            }
        } );
    }

    private void addDragHandlers( Tab tab ) {
        Boolean isInitialised = (Boolean) tab.getProperties().getOrDefault( HAS_DRAG_HANDLERS, false );
        if ( isInitialised ){
            return;
        }

//        logger.info( "addDragHandlers. {} tab's graphic: {}, text: {}", tab, tab.getGraphic(), tab.getText() );

        // Превращаем текст в Graphic, чтобы на него можно было повесить обработчики
        if ( tab.getText() != null && !tab.getText().isEmpty() ) {
            Label label = new Label( tab.getText(), tab.getGraphic() );
            tab.setText( null );
            tab.setGraphic( label );
        }
        Node graphic = tab.getGraphic();
        graphic.setOnDragDetected( e -> {
            //Создаём контейнер для переноски мышью
            Dragboard dragboard = graphic.startDragAndDrop( TransferMode.MOVE );
            ClipboardContent content = new ClipboardContent();
            content.putString( dragID );
            dragboard.setContent( content );
            currentDraggingTab = tab;
        } );
        graphic.setOnDragOver( e -> {
            if ( dragID.equals( e.getDragboard().getString() ) &&
                    currentDraggingTab != null &&
                    currentDraggingTab.getGraphic() != graphic ) {
                e.acceptTransferModes( TransferMode.MOVE );
            }
        } );
        graphic.setOnDragDropped( e -> {
            if ( dragID.equals( e.getDragboard().getString() ) &&
                    currentDraggingTab != null &&
                    currentDraggingTab.getGraphic() != graphic ) {
                int index = tab.getTabPane().getTabs().indexOf( tab );
                currentDraggingTab.getTabPane().getTabs().remove( currentDraggingTab );
                tab.getTabPane().getTabs().add( index, currentDraggingTab );
                currentDraggingTab.getTabPane().getSelectionModel().select( currentDraggingTab );
            }
        } );
        graphic.setOnDragDone( e -> currentDraggingTab = null );

        tab.getProperties().put( HAS_DRAG_HANDLERS, true );
    }

    private void removeDragHandlers( Tab tab ) {
        tab.getGraphic().setOnDragDetected( null );
        tab.getGraphic().setOnDragOver( null );
        tab.getGraphic().setOnDragDropped( null );
        tab.getGraphic().setOnDragDone( null );
    }

    @Override
    public void setViewPrefSaver( final IViewPrefSaver saver ) {
        this.prefSaver = saver;
    }

    @Override
    public void applyViewPrefs() {
        if ( prefSaver != null && !getTabs().isEmpty() && getFormName().isPresent() && getComponentName().isPresent() ) {
            //Следующая конструкция позволяет избежать коллизий/пропаданя вкладок в частных случаях:
            //например, если один контроллер открывается в разных режимах (с разными вкладками),
            //и у нескольких вкладок сохраняется нулевой индекс
            TreeSet<Pair<Integer, Tab>> tabOrder = new TreeSet<>(
            Comparator.comparing( (Function<Pair<Integer, Tab>, Integer>) Pair::getKey )
                      .thenComparing( Object::toString ) );

            Set<PPrefComponent> savedTabPrefs = prefSaver.getInitialPrefs()
                    .stream()
                    .filter( ( PPrefComponent pref ) -> pref.getFORM_NAME().equals( getFormName().get() ) )
                    .filter( ( PPrefComponent pref ) -> pref.getCOMPONENT() != null &&
                            pref.getCOMPONENT().equals( getComponentName().get() ) )
                    .collect( Collectors.toSet() );
            final ObservableList<Tab> tabs = getTabs();
            for ( int i = tabs.size(); i-- > 0; ) {
                final Tab tab = tabs.get( i );//name/id
                String identificator = "";
                if ( tabHasGraphicText( tab ) ) {
                    identificator = ( (Label) tab.getGraphic() ).getText();
                } else if ( S.isNotNullOrEmpty( tab.getId() ) ) {
                    identificator = tab.getId();
                } else {
                    continue;
                }
                final String finalIdentificator = identificator;
                final PPrefComponent prefComponent = savedTabPrefs.stream()
                        .filter( pojo -> pojo.getELEMENT().equals( finalIdentificator ) )
                        .findFirst()
                        .orElse( null );
                //Получаем индекс и удаляем вкладку
                if ( prefComponent != null && prefComponent.getORDBY() != null ) {
                    getTabs().remove( tab );
                    tabOrder.add( new Pair<>( prefComponent.getORDBY(), tab ) );
                }
            }
            //Восстанавливаем вкладки в нужном порядке
            tabOrder.forEach( pair -> getTabs().add( pair.getValue() ) );
            getSelectionModel().select( 0 );
        }
    }

    private Optional<String> getComponentName() {
        if ( this.getId() != null && !this.getId().isEmpty() ) {
            return Optional.of(this.getId());
        }
        return Optional.empty();
    }

    private Optional<String> getAppId() {
        final String appID = BaseApp.APP().getAppID();
        if ( appID != null && !appID.isEmpty() ) {
            return Optional.of(appID);
        }
        return Optional.empty();
    }

    private Optional<String> getFormName() {
        String formName = getController().getViewContext().getFormName();
        if ( S.isNotNullOrEmpty( formName ) ) {
            return Optional.of(formName);
        }
        return Optional.empty();
    }

    public static boolean tabHasGraphicText( final Tab tab ) {
        final Node graphic = tab.getGraphic();
        return graphic instanceof Label && !( (Label) graphic ).getText().isEmpty();
    }
}



