# GUI-002 review evidence

Commit 74fa142a5af7722809e6ce842be0581d0fe7cd8c, parent 60e70689f7eeed990c64f19669b35b817d3f82b8.

## Complete commit diff

```diff
diff --git a/src/test/probes/Test-LegacyGuiSmoke.ps1 b/src/test/probes/Test-LegacyGuiSmoke.ps1
new file mode 100644
index 0000000..a788dbe
--- /dev/null
+++ b/src/test/probes/Test-LegacyGuiSmoke.ps1
@@ -0,0 +1,54 @@
+$ErrorActionPreference = 'Stop'
+$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
+if ($env:JAVA_HOME) {
+    $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
+    $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
+    if (!(Test-Path -LiteralPath $java -PathType Leaf) -or !(Test-Path -LiteralPath $javac -PathType Leaf)) {
+        throw "JAVA_HOME must point to a JDK 25: $env:JAVA_HOME"
+    }
+} else {
+    $java = (Get-Command java -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
+    $javac = (Get-Command javac -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
+}
+$javaVersion = & $java '--version'
+if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25(?:[. +\-]|$)') {
+    throw "GUI-001 requires Java 25. $java reported: $javaVersion"
+}
+$javacVersion = & $javac '--version'
+if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
+    throw "GUI-001 requires javac 25. $javac reported: $javacVersion"
+}
+
+$maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
+if (!$maven) {
+    $maven = Join-Path $root 'mvnw.cmd'
+    if (!(Test-Path -LiteralPath $maven -PathType Leaf)) { throw 'GUI-001 requires mvn or mvnw.cmd' }
+}
+
+$output = Join-Path $root ('target/gui-001-' + [Guid]::NewGuid().ToString('N'))
+New-Item -ItemType Directory -Path $output | Out-Null
+$dependencyFile = Join-Path $output 'dependencies.txt'
+Push-Location $root
+try {
+    & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
+    if ($LASTEXITCODE -ne 0) { throw 'GUI-001 Maven project compile/classpath failed' }
+} finally {
+    Pop-Location
+}
+if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw 'GUI-001 Maven dependency classpath missing' }
+$dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
+if (!$dependencies) { throw 'GUI-001 Maven dependency classpath empty' }
+
+$classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
+$probeSource = Join-Path $PSScriptRoot 'gui/LegacyGuiSmoke.java'
+$compileArgs = Join-Path $output 'javac.args'
+$runArgs = Join-Path $output 'java.args'
+$runClasspath = $output + [IO.Path]::PathSeparator + $classpath
+[IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
+    '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
+[IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
+    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), 'LegacyGuiSmoke'))
+& $javac "@$compileArgs"
+if ($LASTEXITCODE -ne 0) { throw 'GUI-001 smoke source compilation failed' }
+& $java "@$runArgs"
+if ($LASTEXITCODE -ne 0) { throw 'GUI-001 JavaFX runtime/assertion failed' }
diff --git a/src/test/probes/gui/LegacyGuiSmoke.java b/src/test/probes/gui/LegacyGuiSmoke.java
new file mode 100644
index 0000000..b5b363f
--- /dev/null
+++ b/src/test/probes/gui/LegacyGuiSmoke.java
@@ -0,0 +1,134 @@
+import java.io.ByteArrayInputStream;
+import java.nio.charset.StandardCharsets;
+import java.util.Objects;
+import java.util.concurrent.CompletableFuture;
+import java.util.concurrent.TimeUnit;
+import javafx.application.Platform;
+import javafx.beans.property.ReadOnlyStringWrapper;
+import javafx.fxml.FXMLLoader;
+import javafx.scene.Scene;
+import javafx.scene.control.TableCell;
+import javafx.scene.layout.VBox;
+import javafx.stage.Stage;
+import ru.inversion.fx.form.controls.JInvTable;
+import ru.inversion.fx.form.controls.JInvTableColumn;
+import ru.inversion.fx.form.controls.JInvTextField;
+import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;
+import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
+
+public final class LegacyGuiSmoke {
+    private static final String FXML = """
+            <?import javafx.scene.layout.VBox?>
+            <?import ru.inversion.fx.form.controls.JInvTable?>
+            <?import ru.inversion.fx.form.controls.JInvTextField?>
+            <VBox xmlns:fx="http://javafx.com/fxml/1" spacing="8">
+                <JInvTable fx:id="table" VBox.vgrow="ALWAYS"/>
+                <JInvTextField fx:id="editor" text="legacy"/>
+            </VBox>
+            """;
+
+    public static void main(String[] args) {
+        var verdict = new CompletableFuture<Void>();
+        int exitCode = 1;
+        try {
+            require(Runtime.version().feature() == 25, "Java 25 required");
+            Platform.startup(() -> {
+                Thread.currentThread().setUncaughtExceptionHandler((thread, failure) ->
+                        verdict.completeExceptionally(failure));
+                try {
+                    show(verdict);
+                } catch (Throwable failure) {
+                    verdict.completeExceptionally(failure);
+                }
+            });
+            verdict.get(30, TimeUnit.SECONDS);
+            System.out.println("GUI-001 PASS");
+            exitCode = 0;
+        } catch (Throwable failure) {
+            if (failure instanceof java.util.concurrent.TimeoutException) {
+                System.err.println("GUI-001 timed out; run in an interactive desktop and focus the smoke window.");
+            }
+            failure.printStackTrace(System.err);
+        } finally {
+            Platform.exit();
+        }
+        System.exit(exitCode);
+    }
+
+    @SuppressWarnings("unchecked")
+    private static void show(CompletableFuture<Void> verdict) throws Exception {
+        String javafxVersion = System.getProperty("javafx.version", "UNKNOWN");
+        require(javafxVersion.equals("25") || javafxVersion.startsWith("25."), "JavaFX 25 required: " + javafxVersion);
+        System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafxVersion);
+        System.out.println("Legacy controls=" + JInvTable.class.getProtectionDomain().getCodeSource().getLocation());
+
+        var loader = new FXMLLoader();
+        VBox root = loader.load(new ByteArrayInputStream(FXML.getBytes(StandardCharsets.UTF_8)));
+        var table = (JInvTable<String>) loader.getNamespace().get("table");
+        var editor = (JInvTextField) loader.getNamespace().get("editor");
+        var name = new JInvTableColumn<String, String>("Name");
+        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
+        var code = new JInvTableColumn<String, String>("Code");
+        code.setCellValueFactory(cell -> new ReadOnlyStringWrapper("Code " + cell.getValue()));
+        code.setPrefWidth(130);
+        table.getColumns().add(name);
+        table.getColumns().add(code);
+        table.getItems().setAll("A", "B");
+        table.getSelectionModel().setCellSelectionEnabled(true);
+
+        var scene = new Scene(root, 460, 270);
+        scene.getStylesheets().add(Objects.requireNonNull(LegacyGuiSmoke.class.getResource("/css/general.css"),
+                "Production css/general.css missing").toExternalForm());
+        var stage = new Stage();
+        stage.setTitle("GUI-001 Legacy controls smoke");
+        stage.setScene(scene);
+        stage.focusedProperty().addListener((observable, previous, focused) -> {
+            if (focused) {
+                Platform.runLater(() -> {
+                    if (stage.isFocused() && !verdict.isDone()) {
+                        check(stage, root, table, code, editor);
+                        Platform.runLater(() -> {
+                            stage.close();
+                            verdict.complete(null);
+                        });
+                    }
+                });
+            }
+        });
+        stage.show();
+        stage.requestFocus();
+    }
+
+    private static void check(Stage stage, VBox root, JInvTable<String> table,
+                              JInvTableColumn<String, String> code, JInvTextField editor) {
+        require(stage.isShowing(), "Stage is not showing");
+        root.applyCss();
+        root.layout();
+        require(table.getSkin() instanceof JInvTableViewSkin<?>, "Legacy table skin missing");
+        require(editor.getSkin() instanceof JInvTextFieldButtonSkin, "Legacy editor skin missing");
+        require(code.getWidth() > 0 && table.lookupAll(".table-cell").stream().anyMatch(node ->
+                node instanceof TableCell<?, ?> cell && "Code B".equals(cell.getItem())),
+                "Legacy column/cell not laid out");
+
+        table.requestFocus();
+        require(table.isFocused(), "Table did not receive focus");
+        table.getSelectionModel().clearAndSelect(1, code);
+        require(table.getSelectionModel().getSelectedIndex() == 1
+                        && "B".equals(table.getSelectionModel().getSelectedItem())
+                        && table.indexClickedColumnProperty().get() == 1,
+                "Legacy table did not select row B / column Code");
+
+        editor.requestFocus();
+        require(editor.isFocused() && !table.isFocused(), "Focus did not move to the Legacy editor");
+        editor.selectRange(0, 6);
+        editor.replaceSelection("edited");
+        require("edited".equals(editor.getText()), "Legacy editor rejected the selection replacement");
+        System.out.println("Stage shown; FXML/CSS loaded; Legacy table skin, layout, cell selection and editor focus/edit passed");
+    }
+
+    private static void require(boolean condition, String message) {
+        if (!condition) {
+            throw new AssertionError(message);
+        }
+    }
+}
```

## src/test/probes/gui/LegacyGuiSmoke.java

```java
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

public final class LegacyGuiSmoke {
    private static final String FXML = """
            <?import javafx.scene.layout.VBox?>
            <?import ru.inversion.fx.form.controls.JInvTable?>
            <?import ru.inversion.fx.form.controls.JInvTextField?>
            <VBox xmlns:fx="http://javafx.com/fxml/1" spacing="8">
                <JInvTable fx:id="table" VBox.vgrow="ALWAYS"/>
                <JInvTextField fx:id="editor" text="legacy"/>
            </VBox>
            """;

    public static void main(String[] args) {
        var verdict = new CompletableFuture<Void>();
        int exitCode = 1;
        try {
            require(Runtime.version().feature() == 25, "Java 25 required");
            Platform.startup(() -> {
                Thread.currentThread().setUncaughtExceptionHandler((thread, failure) ->
                        verdict.completeExceptionally(failure));
                try {
                    show(verdict);
                } catch (Throwable failure) {
                    verdict.completeExceptionally(failure);
                }
            });
            verdict.get(30, TimeUnit.SECONDS);
            System.out.println("GUI-001 PASS");
            exitCode = 0;
        } catch (Throwable failure) {
            if (failure instanceof java.util.concurrent.TimeoutException) {
                System.err.println("GUI-001 timed out; run in an interactive desktop and focus the smoke window.");
            }
            failure.printStackTrace(System.err);
        } finally {
            Platform.exit();
        }
        System.exit(exitCode);
    }

    @SuppressWarnings("unchecked")
    private static void show(CompletableFuture<Void> verdict) throws Exception {
        String javafxVersion = System.getProperty("javafx.version", "UNKNOWN");
        require(javafxVersion.equals("25") || javafxVersion.startsWith("25."), "JavaFX 25 required: " + javafxVersion);
        System.out.println("Java=" + Runtime.version() + " JavaFX=" + javafxVersion);
        System.out.println("Legacy controls=" + JInvTable.class.getProtectionDomain().getCodeSource().getLocation());

        var loader = new FXMLLoader();
        VBox root = loader.load(new ByteArrayInputStream(FXML.getBytes(StandardCharsets.UTF_8)));
        var table = (JInvTable<String>) loader.getNamespace().get("table");
        var editor = (JInvTextField) loader.getNamespace().get("editor");
        var name = new JInvTableColumn<String, String>("Name");
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
        var code = new JInvTableColumn<String, String>("Code");
        code.setCellValueFactory(cell -> new ReadOnlyStringWrapper("Code " + cell.getValue()));
        code.setPrefWidth(130);
        table.getColumns().add(name);
        table.getColumns().add(code);
        table.getItems().setAll("A", "B");
        table.getSelectionModel().setCellSelectionEnabled(true);

        var scene = new Scene(root, 460, 270);
        scene.getStylesheets().add(Objects.requireNonNull(LegacyGuiSmoke.class.getResource("/css/general.css"),
                "Production css/general.css missing").toExternalForm());
        var stage = new Stage();
        stage.setTitle("GUI-001 Legacy controls smoke");
        stage.setScene(scene);
        stage.focusedProperty().addListener((observable, previous, focused) -> {
            if (focused) {
                Platform.runLater(() -> {
                    if (stage.isFocused() && !verdict.isDone()) {
                        check(stage, root, table, code, editor);
                        Platform.runLater(() -> {
                            stage.close();
                            verdict.complete(null);
                        });
                    }
                });
            }
        });
        stage.show();
        stage.requestFocus();
    }

    private static void check(Stage stage, VBox root, JInvTable<String> table,
                              JInvTableColumn<String, String> code, JInvTextField editor) {
        require(stage.isShowing(), "Stage is not showing");
        root.applyCss();
        root.layout();
        require(table.getSkin() instanceof JInvTableViewSkin<?>, "Legacy table skin missing");
        require(editor.getSkin() instanceof JInvTextFieldButtonSkin, "Legacy editor skin missing");
        require(code.getWidth() > 0 && table.lookupAll(".table-cell").stream().anyMatch(node ->
                node instanceof TableCell<?, ?> cell && "Code B".equals(cell.getItem())),
                "Legacy column/cell not laid out");

        table.requestFocus();
        require(table.isFocused(), "Table did not receive focus");
        table.getSelectionModel().clearAndSelect(1, code);
        require(table.getSelectionModel().getSelectedIndex() == 1
                        && "B".equals(table.getSelectionModel().getSelectedItem())
                        && table.indexClickedColumnProperty().get() == 1,
                "Legacy table did not select row B / column Code");

        editor.requestFocus();
        require(editor.isFocused() && !table.isFocused(), "Focus did not move to the Legacy editor");
        editor.selectRange(0, 6);
        editor.replaceSelection("edited");
        require("edited".equals(editor.getText()), "Legacy editor rejected the selection replacement");
        System.out.println("Stage shown; FXML/CSS loaded; Legacy table skin, layout, cell selection and editor focus/edit passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
```

## src/test/probes/Test-LegacyGuiSmoke.ps1

```powershell
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
    $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
    if (!(Test-Path -LiteralPath $java -PathType Leaf) -or !(Test-Path -LiteralPath $javac -PathType Leaf)) {
        throw "JAVA_HOME must point to a JDK 25: $env:JAVA_HOME"
    }
} else {
    $java = (Get-Command java -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $javac = (Get-Command javac -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
}
$javaVersion = & $java '--version'
if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25(?:[. +\-]|$)') {
    throw "GUI-001 requires Java 25. $java reported: $javaVersion"
}
$javacVersion = & $javac '--version'
if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
    throw "GUI-001 requires javac 25. $javac reported: $javacVersion"
}

$maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
if (!$maven) {
    $maven = Join-Path $root 'mvnw.cmd'
    if (!(Test-Path -LiteralPath $maven -PathType Leaf)) { throw 'GUI-001 requires mvn or mvnw.cmd' }
}

$output = Join-Path $root ('target/gui-001-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $output | Out-Null
$dependencyFile = Join-Path $output 'dependencies.txt'
Push-Location $root
try {
    & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
    if ($LASTEXITCODE -ne 0) { throw 'GUI-001 Maven project compile/classpath failed' }
} finally {
    Pop-Location
}
if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) { throw 'GUI-001 Maven dependency classpath missing' }
$dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
if (!$dependencies) { throw 'GUI-001 Maven dependency classpath empty' }

$classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
$probeSource = Join-Path $PSScriptRoot 'gui/LegacyGuiSmoke.java'
$compileArgs = Join-Path $output 'javac.args'
$runArgs = Join-Path $output 'java.args'
$runClasspath = $output + [IO.Path]::PathSeparator + $classpath
[IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
    '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
[IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
    '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), 'LegacyGuiSmoke'))
& $javac "@$compileArgs"
if ($LASTEXITCODE -ne 0) { throw 'GUI-001 smoke source compilation failed' }
& $java "@$runArgs"
if ($LASTEXITCODE -ne 0) { throw 'GUI-001 JavaFX runtime/assertion failed' }
```
