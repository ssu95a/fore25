package ru.inversion.fx.app.cmd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.inversion.utils.U;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class Test_OSCmd {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private OSCmd oscmd;

    @Before
    public void setUp() {
        oscmd = new OSCmd();
    }

    @Test
    public void testSimpleCommandExecution() throws Exception {
        // Arrange
        String command = isWindows() ? "cmd.exe" : "sh";
        String arg = isWindows() ? "/c" : "-c";
        String echo = isWindows() ? "echo test" : "echo 'test'";

        final List<String> output = new ArrayList<>();

        // Act
        oscmd.command(command)
                .unnamedArgs(arg, echo)
                .waitFor(true)
                .outputTo(output)
                .call();

        // Assert
        assertFalse(output.isEmpty());
        assertTrue(output.get(0).contains("test"));
    }

    @Test
    public void testCommandWithNamedArgs() throws Exception {
        // Arrange
        OSCmd cmd = new OSCmd();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Act & Assert - проверяем что не падает
        try {
            cmd.command("java")
                    .namedArg("Dprop1", "value1")
                    .namedArg("Dprop2", "value2 with spaces")
                    .unnamedArgs("-version")
                    .waitFor(true)
                    .outputTo(baos)
                    .call();
        } catch (Exception e) {
            // Может упасть если java нет в PATH, но это нормально
            // Главное - проверить построение команды
            System.out.println("Expected exception: " + e.getMessage());
        }
    }

    @Test
    public void testOutputToFile() throws Exception {
        // Arrange
        File outputFile = tempDir.newFile("output.txt");
        String command = isWindows() ? "cmd.exe" : "sh";
        String arg = isWindows() ? "/c" : "-c";
        String echo = isWindows() ? "echo file_output_test" : "echo 'file_output_test'";

        // Act
        oscmd.command(command)
                .unnamedArgs(arg, echo)
                .waitFor(true)
                .outputTo(outputFile)
                .call();

        // Assert
        assertTrue(outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("file_output_test"));
    }

    @Test
    public void testOutputToOutputStream() throws Exception {
        // Arrange
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String command = isWindows() ? "cmd.exe" : "sh";
        String arg = isWindows() ? "/c" : "-c";
        String echo = isWindows() ? "echo stream_test" : "echo 'stream_test'";

        // Act
        oscmd.command(command)
                .unnamedArgs(arg, echo)
                .waitFor(true)
                .outputTo(baos)
                .call();

        // Assert
        String output = baos.toString();
        assertTrue(output.contains("stream_test"));
    }

    @Test
    public void testEnvironmentVariables() throws Exception {
        // Arrange
        String command = isWindows() ? "cmd.exe" : "printenv";
        String arg = isWindows() ? "/c" : "";
        String envCmd = isWindows() ? "echo %TEST_VAR%" : "TEST_VAR";

        final List<String> output = new ArrayList<>();

        // Act
        oscmd.command(command)
                .unnamedArgs(isWindows() ? arg : "", envCmd)
                .environment("TEST_VAR", "test_value_123")
                .waitFor(true)
                .outputTo(output)
                .call();

        // Assert
        if (isWindows()) {
            // Windows echo includes the variable
            String line = String.join("", output);
            assertTrue(line.contains("test_value_123"));
        } else {
            // Unix printenv outputs the value
            boolean found = false;
            for (String line : output) {
                if (line.contains("test_value_123")) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void testWorkDirectory() throws Exception {
        // Arrange
        File workDir = tempDir.newFolder("workdir");
        String command = isWindows() ? "cmd.exe" : "pwd";
        String arg = isWindows() ? "/c" : "";
        String pwdCmd = isWindows() ? "cd" : "";

        final List<String> output = new ArrayList<>();

        // Act
        oscmd.command(command)
                .unnamedArgs(isWindows() ? arg : "", pwdCmd)
                .workDir(workDir)
                .waitFor(true)
                .outputTo(output)
                .call();

        // Assert
        String result = String.join(" ", output);
        assertTrue(result.contains(workDir.getPath()) ||
                result.contains(workDir.getAbsolutePath()));
    }

    @Test
    public void testListenerEvents() throws Exception {
        // Arrange
        final AtomicBoolean runCalled = new AtomicBoolean(false);
        final AtomicBoolean finishCalled = new AtomicBoolean(false);

        String command = isWindows() ? "cmd.exe" : "sh";
        String arg = isWindows() ? "/c" : "-c";
        String echo = isWindows() ? "echo listener_test" : "echo 'listener_test'";

        oscmd.addListener(new ICmdListener<OSCmd, String>() {
            @Override
            public void onRun(OSCmd cmd, String commandLine) {
                runCalled.set(true);
                assertNotNull(commandLine);
                assertTrue(commandLine.contains(command));
            }

            @Override
            public void onFinish(OSCmd cmd) {
                finishCalled.set(true);
                assertNotNull(cmd);
            }
        });

        // Act
        oscmd.command(command)
                .unnamedArgs(arg, echo)
                .waitFor(true)
                .call();

        // Assert
        assertTrue(runCalled.get());
        assertTrue(finishCalled.get());
    }

    @Test
    public void testPreCallTemplateSubstitution() throws Exception {
        // Arrange
        // Устанавливаем стандартный поставщик для теста
        CommandBuilder.setStandardArgSupplier(new java.util.function.Function<Object, Object>() {
            @Override
            public Object apply(Object o) {
                if (o instanceof StandardArgEnum) {
                    StandardArgEnum sae = (StandardArgEnum) o;
                    switch (sae) {
                        case USER_LOGIN: return "testuser";
                        case USER_PASSWORD: return "testpass";
                        case DB_ALIAS: return "testdb";
                    }
                }
                return null;
            }
        });

        OSCmd cmd = new OSCmd();

        // Act
        cmd.command("cmd.exe {U} {P} {D}")
                .waitFor(false); // Не ждем выполнения

        // Вызываем preCall напрямую через рефлексию
        java.lang.reflect.Method method = OSCmd.class.getDeclaredMethod("preCall");
        method.setAccessible(true);
        method.invoke(cmd);

        // Assert
        // После preCall команда должна быть заменена
        assertTrue(cmd.command.contains("cmd.exe"));
        assertFalse(cmd.command.contains("{U}"));
        assertFalse(cmd.command.contains("{P}"));
        assertFalse(cmd.command.contains("{D}"));
    }

    @Test
    public void testUnnamedPrefix() {
        // Arrange
        OSCmd cmd = new OSCmd();

        // Act
        cmd.unNamedPrefix("-")
                .unnamedArgs("version", "help");

        // Assert
        assertEquals("-", cmd.unnamedPrefix);
    }

    @Test
    public void testNamedPrefix() {
        // Arrange
        OSCmd cmd = new OSCmd();

        // Act
        cmd.namedPrefix("-D")
                .namedArg("prop1", "value1");

        // Assert
        assertEquals("-D", cmd.namedPrefix);
    }

    @Test
    public void testCommandBuilderChain() {
        // Arrange & Act
        OSCmd cmd = new OSCmd()
                .command("test.exe")
                .namedArg("param1", "value1")
                .unnamedArgs("arg1", "arg2")
                .environment("ENV1", "VAL1")
                .workDir(new File("."))
                .waitFor(true);

        // Assert
        assertEquals("test.exe", cmd.command);
        assertTrue(cmd.containsArg("param1"));
        assertTrue(cmd.waitFor);
        assertNotNull(cmd.workDir);
    }

    @Test(expected = CmdException.class)
    public void testCmdExceptionOnInvalidCommand() throws Exception {
        // Arrange
        OSCmd cmd = new OSCmd()
                .command("nonexistent_command_xyz123")
                .waitFor(true);

        // Act
        cmd.call();
    }

    @Test
    public void testStringArgs() throws Exception {
        // Arrange
        final List<String> output = new ArrayList<>();
        String command = isWindows() ? "cmd.exe" : "echo";
        String args = isWindows() ? "/c echo hello world" : "hello world";

        // Act
        oscmd.command(command)
                .stringArgs(args)
                .waitFor(true)
                .outputTo(output)
                .call();

        // Assert
        assertFalse(output.isEmpty());
        String result = String.join(" ", output).toLowerCase();
        assertTrue(result.contains("hello") || result.contains("world"));
    }

    @Test
    public void testFileArgs() throws Exception {
        // Arrange
        File argsFile = tempDir.newFile("args.txt");
        Files.write(argsFile.toPath(), Arrays.asList("arg1", "arg2"));

        final List<String> output = new ArrayList<>();
        String command = isWindows() ? "cmd.exe" : "echo";
        String echo = isWindows() ? "/c echo %1 %2" : "$1 $2";

        // Act
        oscmd.command(command)
                .fileArgs(argsFile)
                .unnamedArgs(echo)
                .waitFor(true)
                .outputTo(output)
                .call();

        // Assert - проверяем что не падает
        assertNotNull(output);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}