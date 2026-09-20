package ru.inversion.fx.dialog;

import javafx.concurrent.WorkerStateEvent;
import javafx.stage.Modality;
import ru.inversion.fx.app.es.JInvErrorService;

/**
 *
 * @author perov
 */
public class JInvProgressDialog extends JInvAbstractProgressDialog<Void>{

    public JInvProgressDialog() {
        super();
    }

    public JInvProgressDialog(Modality modality) {
        super(modality);
    }            
        
    @Override
    public Void call() throws Exception {
        return getAction().call();
    }

    @Override
    protected void handleSuccess(WorkerStateEvent event) throws Exception {}

    @Override
    protected void handleFailed(WorkerStateEvent event) throws Exception {
        JInvErrorService.handleException(null, event.getSource().getException());
        getStage().close();       
    }
    
    
}
