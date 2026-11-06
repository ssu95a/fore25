package ru.inversion.fx.help.controller;

import javafx.scene.effect.BlendMode;
import javafx.scene.web.WebView;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.help.controller.search.WebViewSearchPane;
import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.tc.TaskContext;

import java.util.ResourceBundle;

import static ru.inversion.fx.help.controller.HelpController.HELP_COLOR_TINT;

/**
 * Внутренний класс. Реализация Tab - отображения справочной информации.
 *
 * @author perov
 * @version 1.0.0
 */
class ViewTab extends AbstractTab {
    //    private final JInvToolBar toolBar = new JInvToolBar();
    private final WebViewDecorator webViewDecorator;
    private final WebView webView;
    private final TaskContext tc;
//    private final LinkedList<String> listHistory = new LinkedList<>();
    private final XXIDataSet<PHelp> dsHelp = new XXIDataSet<>();
    private String formName;
//    private Button btnHistoryPrev;

    public ViewTab(String title, JInvFXFormController contoller, TaskContext tc, ResourceBundle bundle) {
        super(title, contoller, bundle);
        this.tc = tc;
        webViewDecorator = new WebViewDecorator(new WebView());
        webView = webViewDecorator.getWebView();
        init();
        initWebViewDisabler(webView);
    }

    @Override
    public final void init() {
        webView.setBlendMode(BlendMode.MULTIPLY);
        final WebViewSearchPane webViewSearchPane = new WebViewSearchPane(webViewDecorator);
        webViewSearchPane.getRootBox().setStyle("-fx-background-color:" + HELP_COLOR_TINT + ";");
        this.setContent(webViewSearchPane);
//        initToolBar();
        initDS();

    }

    @Override
    public void draw(String formName) {
        this.formName = formName;
        refreshDS();
//        disableBtnHistory(formName);
        setTitle(getBundle().getString("HELP_TITLE_VIEW"));

        if (dsHelp.getCurrentRow() != null) {

//            if (!listHistory.contains(formName)) {
//                listHistory.add(formName);
//            }
            if (dsHelp.getCurrentRow().getHTML_TEXT() != null) {
                webView.getEngine().loadContent(dsHelp.getCurrentRow().getHTML_TEXT());
            }
        } else {
            webView.getEngine().loadContent(getBundle().getString("NET_SPRAVOCHNOJ_INFORMACII_DLYA_FORMY")+ " " +
                    formName);
        }
    }

//    private void disableBtnHistory(String formName) {
//        if (listHistory.isEmpty() || listHistory.indexOf(formName) == 0) {
//            btnHistoryPrev.setDisable(true);
//        } else {
//            btnHistoryPrev.setDisable(false);
//        }
//    }

    private void initDS() {
        dsHelp.setTaskContext(tc);
        dsHelp.setWherePredicat("form = ? and CLOCALE = ?");
        dsHelp.setRowClass(PHelp.class);
    }

    private void refreshDS() {
        try {
            dsHelp.setParameter(0, formName);
            dsHelp.setParameter(1, BaseApp.APP().getLocale().toString());
            dsHelp.executeQuery(false);
        } catch (DataSetException ex) {
            JInvErrorService.handleException(null, ex);
        }
    }
//
//    private void initToolBar() {
//
//        btnHistoryPrev = ActionFactory.createButton(ActionFactory.IconEnum.fa_arrow_left, (ActionEvent a) -> {
//            ListIterator<String> li = getIterator(formName);
//            if (li.hasPrevious()) {
//                draw(li.previous());
//            }
//        });
//        toolBar.getItems().add(btnHistoryPrev);
//    }

//    private ListIterator<String> getIterator(String formName) {
//        return listHistory.listIterator(listHistory.indexOf(formName));
//    }

}
