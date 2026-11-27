package ru.inversion.fx.form.controls;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.ViewPrefAppService;

public final class ButtonTargetAssociationSmoke {
    static class App extends BaseApp {
        static void install() { g_app = new App(); }
        @Override public ViewPrefAppService getViewPrefService() { return new ViewPrefAppService(); }
    }

    static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }

    static void legacy() throws Exception {
        var a = new JInvTextField();
        var b = new JInvTextField();
        a.setButtonToolTipText("target-a");
        b.setButtonToolTipText("target-b");
        var xml = "<?import ru.inversion.fx.form.controls.JInvButton?>"
                + "<JInvButton idTextField='a' xmlns:fx='http://javafx.com/fxml/1'/>";
        JInvButton external = new FXMLLoader().load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        require(external.getIdTextField().equals("a"), "FXML id link");
        require(external.getParent() == null, "pre-parent");
        external.setTextField(a);
        require(external.getTextField() == a && a.externalButtonProperty().get() == null
                && external.getTooltip().getText().equals("target-a"), "pre-parent config");
        var parent = new VBox(a, b, external);
        external.setTextField(a);
        require(a.externalButtonProperty().get() == external, "legacy parented external");
        external.setTextField(b);
        external.setTextField(null);
        require(external.getTextField() == b && b.externalButtonProperty().get() == external
                && external.getTooltip().getText().equals("target-b"), "legacy reassociation and null");

        var fe = new JInvFEButton();
        var provided = new Tooltip("custom");
        fe.tooltipProperty().bind(new SimpleObjectProperty<>(provided));
        a.setEditable(false);
        a.setInnerButton(fe);
        require(fe.getTextField() == a && fe.getTooltip() == provided && provided.getText().equals("target-a")
                && fe.isDisabled(), "inner FE target and existing bound tooltip identity");
        a.setEditable(true);
        require(!fe.isDisabled(), "inner FE follows editability");
        a.setReadOnly(true);
        require(fe.isDisabled(), "inner FE follows readOnly");
        a.setReadOnly(false);
        a.removeInnerButton();
        fe.disableProperty().unbind();

        var legacy = new JInvFEButton();
        var block = new SimpleBooleanProperty(false);
        legacy.disableProperty().bind(block);
        try {
            legacy.setTextField(b);
            throw new AssertionError("bound disable was accepted");
        } catch (RuntimeException expected) {
            require(legacy.getTextField() == b && legacy.getTooltip().getText().equals("target-b")
                    && legacy.disableProperty().isBound(), "legacy partial failure ordering");
        }
        require(parent.getChildren().size() == 3, "no node reparented");
    }

    static void targetOnly() {
        var a = new JInvTextField();
        var b = new JInvTextField();
        var button = new JInvFEButton();
        button.setTooltip(null);
        button.disableProperty().bind(new SimpleBooleanProperty(true));
        var parent = new VBox(button);
        button.associateTextField(a);
        require(button.getTextField() == a && button.getTooltip() == null && button.isDisabled()
                && a.externalButtonProperty().get() == null && button.getParent() == parent, "associate only");
        button.associateTextField(b);
        require(button.getTextField() == b && b.externalButtonProperty().get() == null, "reassociate only");
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        for (int i = 0; i < 10000; i++) button.associateTextField((i & 1) == 0 ? a : b);
        long before = bean.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < 10000; i++) button.associateTextField((i & 1) == 0 ? a : b);
        System.out.println("target reassociations FX bytes=" + (bean.getCurrentThreadAllocatedBytes() - before));
        button.associateTextField(null);
        require(button.getTextField() == null && button.getTooltip() == null && button.isDisabled()
                && a.externalButtonProperty().get() == null && b.externalButtonProperty().get() == null, "detach only");
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> {
            App.install();
            legacy();
            targetOnly();
            return null;
        });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(task); });
        try {
            task.get(35, TimeUnit.SECONDS);
            System.out.println("ARCH-002 PASS (target association and legacy configuration)");
        } finally {
            Platform.exit();
        }
    }
}
