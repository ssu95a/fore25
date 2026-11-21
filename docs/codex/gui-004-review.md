# GUI-004 exact commit diff

22d2dffff9d48558193a3c95184fe1ed94b51244 against 504f94d6fa4331ce3fe5e561afb8a96e510ebacd.

```diff
diff --git a/src/test/probes/Test-LegacyGuiSmoke.ps1 b/src/test/probes/Test-LegacyGuiSmoke.ps1
index a788dbe..7de4105 100644
--- a/src/test/probes/Test-LegacyGuiSmoke.ps1
+++ b/src/test/probes/Test-LegacyGuiSmoke.ps1
@@ -1,3 +1,11 @@
+param([switch] $Editors)
+
+$probeId = 'GUI-001'
+$probeClass = 'LegacyGuiSmoke'
+if ($Editors) {
+    $probeId = 'GUI-004'
+    $probeClass = 'LegacyEditorsSmoke'
+}
 $ErrorActionPreference = 'Stop'
 $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
 if ($env:JAVA_HOME) {
@@ -12,43 +20,43 @@ if ($env:JAVA_HOME) {
 }
 $javaVersion = & $java '--version'
 if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25(?:[. +\-]|$)') {
-    throw "GUI-001 requires Java 25. $java reported: $javaVersion"
+    throw "$probeId requires Java 25. $java reported: $javaVersion"
 }
 $javacVersion = & $javac '--version'
 if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
-    throw "GUI-001 requires javac 25. $javac reported: $javacVersion"
+    throw "$probeId requires javac 25. $javac reported: $javacVersion"
 }
 
 $maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
 if (!$maven) {
     $maven = Join-Path $root 'mvnw.cmd'
-    if (!(Test-Path -LiteralPath $maven -PathType Leaf)) { throw 'GUI-001 requires mvn or mvnw.cmd' }
+    if (!(Test-Path -LiteralPath $maven -PathType Leaf)) { throw "$probeId requires mvn or mvnw.cmd" }
 }
 
-$output = Join-Path $root ('target/gui-001-' + [Guid]::NewGuid().ToString('N'))
+$output = Join-Path $root ("target/$($probeId.ToLowerInvariant())-" + [Guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $output | Out-Null
 $dependencyFile = Join-Path $output 'dependencies.txt'
 Push-Location $root
 try {
     & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
-    if ($LASTEXITCODE -ne 0) { throw 'GUI-001 Maven project compile/classpath failed' }
+    if ($LASTEXITCODE -ne 0) { throw "$probeId Maven project compile/classpath failed" }
 } finally {
     Pop-Location
 }
-if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw 'GUI-001 Maven dependency classpath missing' }
+if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw "$probeId Maven dependency classpath missing" }
 $dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
-if (!$dependencies) { throw 'GUI-001 Maven dependency classpath empty' }
+if (!$dependencies) { throw "$probeId Maven dependency classpath empty" }
 
 $classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
-$probeSource = Join-Path $PSScriptRoot 'gui/LegacyGuiSmoke.java'
+$probeSource = Join-Path $PSScriptRoot "gui/$probeClass.java"
 $compileArgs = Join-Path $output 'javac.args'
 $runArgs = Join-Path $output 'java.args'
 $runClasspath = $output + [IO.Path]::PathSeparator + $classpath
 [IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
     '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
 [IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
-    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), 'LegacyGuiSmoke'))
+    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), $probeClass))
 & $javac "@$compileArgs"
-if ($LASTEXITCODE -ne 0) { throw 'GUI-001 smoke source compilation failed' }
+if ($LASTEXITCODE -ne 0) { throw "$probeId smoke source compilation failed" }
 & $java "@$runArgs"
-if ($LASTEXITCODE -ne 0) { throw 'GUI-001 JavaFX runtime/assertion failed' }
+if ($LASTEXITCODE -ne 0) { throw "$probeId JavaFX runtime/assertion failed" }
diff --git a/src/test/probes/gui/LegacyEditorsSmoke.java b/src/test/probes/gui/LegacyEditorsSmoke.java
new file mode 100644
index 0000000..0553573
--- /dev/null
+++ b/src/test/probes/gui/LegacyEditorsSmoke.java
@@ -0,0 +1,183 @@
+import java.util.Locale;
+import java.util.Objects;
+import java.util.concurrent.CompletableFuture;
+import java.util.concurrent.TimeUnit;
+import java.util.concurrent.TimeoutException;
+import javafx.animation.AnimationTimer;
+import javafx.application.Platform;
+import javafx.scene.Scene;
+import javafx.scene.control.ContextMenu;
+import javafx.scene.control.MenuItem;
+import javafx.scene.layout.VBox;
+import javafx.stage.PopupWindow;
+import javafx.stage.Stage;
+import javafx.util.StringConverter;
+import ru.inversion.fx.form.controls.JInvComboBox;
+import ru.inversion.fx.form.controls.JInvTextArea;
+import ru.inversion.fx.form.controls.skin.JInvComboBoxListViewSkin;
+import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;
+
+public final class LegacyEditorsSmoke {
+    private final CompletableFuture<Void> verdict = new CompletableFuture<>();
+    private Stage stage;
+    private JInvTextArea area;
+    private JInvComboBox<String, String> combo;
+    private boolean started;
+
+    public static void main(String[] args) {
+        var smoke = new LegacyEditorsSmoke();
+        int exit = 1;
+        try {
+            require(Runtime.version().feature() == 25, "setup: Java 25 required");
+            Platform.startup(() -> smoke.runChecked(smoke::show));
+            smoke.verdict.get(30, TimeUnit.SECONDS);
+            System.out.println("GUI-004 PASS");
+            exit = 0;
+        } catch (TimeoutException failure) {
+            System.err.println("GUI-004 UNKNOWN: timed out waiting for focus or FX pulses. Use an interactive desktop.");
+        } catch (Exception | AssertionError failure) {
+            System.err.println("GUI-004 failed; see the setup, focus, or control diagnostic below.");
+            failure.printStackTrace(System.err);
+        } finally {
+            Platform.exit();
+        }
+        System.exit(exit);
+    }
+
+    private void show() {
+        Platform.setImplicitExit(false);
+        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
+        String version = System.getProperty("javafx.version", "UNKNOWN");
+        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required: " + version);
+        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
+        System.out.println("Legacy controls=" + JInvTextArea.class.getProtectionDomain().getCodeSource().getLocation());
+
+        area = new JInvTextArea("first\nsecond");
+        combo = new JInvComboBox<>();
+        combo.setEditable(true);
+        combo.getItems().setAll("ALPHA", "BETA");
+        combo.setConverter(new StringConverter<>() {
+            @Override public String toString(String value) { return value == null ? "" : value; }
+            @Override public String fromString(String text) { return text.trim().toUpperCase(Locale.ROOT); }
+        });
+
+        var scene = new Scene(new VBox(8, area, combo), 420, 250);
+        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
+                "setup: production CSS missing").toExternalForm());
+        stage = new Stage();
+        stage.setTitle("GUI-004 Legacy editors smoke");
+        stage.setScene(scene);
+        stage.focusedProperty().addListener((property, oldValue, focused) -> {
+            if (focused && !started) {
+                afterPulse(() -> {
+                    if (stage.isFocused() && !started) {
+                        started = true;
+                        checkEditors();
+                    }
+                });
+            }
+        });
+        stage.show();
+        stage.requestFocus();
+    }
+
+    private void checkEditors() {
+        require(stage.isShowing() && area.getWidth() > 0 && area.getHeight() > 0
+                && combo.getWidth() > 0 && combo.getHeight() > 0, "layout: controls are not laid out");
+        require(area.getSkin() instanceof JInvTextAreaSkin, "setup: Legacy TextArea skin missing");
+        require(combo.getSkin() instanceof JInvComboBoxListViewSkin<?>, "setup: Legacy ComboBox skin missing");
+
+        area.requestFocus();
+        require(area.isFocused(), "focus: TextArea did not receive focus");
+        area.selectRange(6, 12);
+        require("second".equals(area.getSelectedText()), "TextArea: unexpected selection");
+        area.replaceSelection("edited");
+        require("first\nedited".equals(area.getText()), "TextArea: text replacement failed");
+
+        ContextMenu skinMenu = area.getContextMenu();
+        require(skinMenu != null && !skinMenu.getItems().isEmpty(), "TextArea menu: skin menu unavailable");
+        area.setOnContextMenuRequested(event -> event.consume());
+        require(area.getContextMenu() == null, "TextArea menu: skin menu did not yield to request handler");
+        var customMenu = new ContextMenu(new MenuItem("Custom"));
+        area.setContextMenu(customMenu);
+        area.setOnContextMenuRequested(null);
+        require(area.getContextMenu() == customMenu, "TextArea menu: custom menu was replaced");
+        area.setContextMenu(null);
+        require(area.getContextMenu() == skinMenu, "TextArea menu: skin menu was not restored");
+
+        combo.requestFocus();
+        require(comboHasFocus() && !area.isFocused(), "focus: focus did not move to ComboBox");
+        combo.getSelectionModel().select(0);
+        requireComboValue(0, "ALPHA");
+        combo.getEditor().setText(" beta ");
+        combo.forceCommitValue();
+        requireComboValue(1, "BETA");
+        combo.getEditor().setText(" beta ");
+        combo.forceCommitValue();
+        requireComboValue(1, "BETA");
+        System.out.println("TextArea edit/menu ownership and ComboBox selection, commit and same-value normalization passed");
+
+        var skin = (JInvComboBoxListViewSkin<?>) combo.getSkin();
+        combo.show();
+        afterPulse(() -> checkPopup(skin));
+    }
+
+    private void checkPopup(JInvComboBoxListViewSkin<?> skin) {
+        var content = skin.getPopupContent();
+        require(combo.isShowing() && content.getScene() != null
+                        && content.getScene().getWindow() instanceof PopupWindow popup
+                        && popup.isShowing() && popup.getOwnerWindow() == stage,
+                "ComboBox popup: no actual showing popup owned by smoke Stage after pulse");
+        var popup = content.getScene().getWindow();
+        combo.hide();
+        afterPulse(() -> {
+            require(!combo.isShowing() && !popup.isShowing(), "ComboBox popup: window did not hide");
+            require(stage.isFocused() && comboHasFocus(), "focus: ComboBox did not retain focus after hide");
+            area.requestFocus();
+            require(area.isFocused() && !comboHasFocus(), "focus: could not return to TextArea after popup");
+            System.out.println("ComboBox popup window shown and hidden; focus returned to TextArea");
+            stage.close();
+            verdict.complete(null);
+        });
+    }
+
+    private boolean comboHasFocus() {
+        var owner = stage.getScene().getFocusOwner();
+        return owner == combo || owner == combo.getEditor();
+    }
+
+    private void requireComboValue(int index, String expected) {
+        var selection = combo.getSelectionModel();
+        require(selection.getSelectedIndex() == index && expected.equals(selection.getSelectedItem())
+                        && expected.equals(combo.getValue()) && expected.equals(combo.getEditor().getText()),
+                "ComboBox: expected index=" + index + ", value/item/editor=" + expected + "; got "
+                        + selection.getSelectedIndex() + "/" + combo.getValue() + "/"
+                        + selection.getSelectedItem() + "/" + combo.getEditor().getText());
+    }
+
+    private void afterPulse(Runnable action) {
+        new AnimationTimer() {
+            private boolean firstFrame = true;
+            @Override public void handle(long now) {
+                if (firstFrame) {
+                    firstFrame = false;
+                    return;
+                }
+                stop();
+                if (!verdict.isDone()) runChecked(action);
+            }
+        }.start();
+    }
+
+    private void runChecked(Runnable action) {
+        try {
+            action.run();
+        } catch (Throwable failure) {
+            verdict.completeExceptionally(failure);
+        }
+    }
+
+    private static void require(boolean condition, String message) {
+        if (!condition) throw new AssertionError(message);
+    }
+}
```
