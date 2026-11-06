package ru.inversion.fx.dialog;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.app.es.JInvErrorService;

import java.util.concurrent.Callable;

/**
 *
 * @author perov
 * @param <V>
 */
public abstract class JInvAbstractProgressDialog<V> extends Task<V> implements IProgressDialog<V>{
    
    private final Stage stage;    
    private final ProgressBar progressBar;
    private final TextArea textArea;
    private Callable<V> action = () -> { return null; };
   
    public JInvAbstractProgressDialog() {
        this(Modality.APPLICATION_MODAL);   
    }
    
    public JInvAbstractProgressDialog( Modality modality )
    {
        super();
       
        this.stage = new Stage();
        if (modality == null) {
            this.stage.initModality(Modality.APPLICATION_MODAL);
        } else {
            this.stage.initModality(modality);
        }        
        this.stage.setResizable(false);
        
        this.progressBar = new ProgressBar();
        this.progressBar.setMaxWidth(Double.MAX_VALUE);
        
        this.textArea = new TextArea();
        this.textArea.setWrapText(true );
        this.textArea.setEditable(false);
        this.textArea.setPrefRowCount(2);
        this.textArea.setMaxWidth(Double.MAX_VALUE);

        VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(5, 5, 5, 5));
        vbox.setSpacing(5);
        vbox.getChildren().addAll(this.progressBar, this.textArea);

        Scene lScene = new Scene(vbox);
        this.stage.setScene(lScene);
        
        //Стартуем при показе формы
        stage.setOnShowing((WindowEvent event) -> {
            ThreadPoolManager.getInstance().executeShortTask( JInvAbstractProgressDialog.this );
//            Thread thread = new Thread(JInvAbstractProgressDialog.this);
//            thread.setName("FX Task Progress Dialog Thread");
//            thread.start();
        });
        
        //Отменяем выполнение при закрытии формы
        stage.setOnCloseRequest((WindowEvent event) -> {
            JInvAbstractProgressDialog.this.cancel();
        });
        
        //Обработчик для исключения
        this.setOnFailed((WorkerStateEvent event) -> {
            try {
                handleFailed(event);
            } catch (Exception ex) {
                JInvErrorService.handleException("", ex);
            }
        });
        
        //Обработчик для успешного выполнения
        this.setOnSucceeded((WorkerStateEvent event) -> {
            try {
                handleSuccess(event);
            } catch (Exception ex) {
                JInvErrorService.handleException("", ex);
            }
        });

        this.progressBar.progressProperty().bind(this.progressProperty());
        this.textArea   .textProperty().bind(this.messageProperty());
    }
    @Override
    public void setAction(Callable<V> clb) {
        this.action = clb;
    }
    
    protected  Callable<V> getAction(){
        return action;
    }
    
    public void showDialog( String title, int rowCount ) {
        this.textArea.setPrefRowCount(rowCount);
        this.stage.setTitle(title);
        this.stage.show();
    }

    @Override
    public void showDialog( String title ) {
        this.stage.setTitle(title);
        this.stage.show();
    }

    @Override
    public void setText(String msg){    
        this.updateMessage(msg);
    }
    
    @Override
    public void setProgress(long workDone, long max){
        updateProgress(workDone, max);
    }

    @Override
    public void closeDialog() {
        stage.close();
    }
    
    protected Stage getStage() {
        return stage;
    }
    
    @Override
    protected abstract V call() throws Exception;
    
    protected abstract void handleSuccess(WorkerStateEvent event) throws Exception;
    protected abstract void handleFailed(WorkerStateEvent event) throws Exception;

    }
