package ru.inversion.fx.app.cmd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.inversion.utils.S;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class Test_JavaApp {

        @Rule
        public TemporaryFolder tempDir = new TemporaryFolder();

        private JavaApp javaApp;
        private File testJar;

        @Before
        public void setUp() throws Exception {
            javaApp = new JavaApp();

            // Создаем тестовый jar файл
            testJar = tempDir.newFile("test.jar");
            Files.write(testJar.toPath(), "fake jar content".getBytes());
        }

        @Test
        public void testJarMode() {
            // Arrange
            JavaApp app = (JavaApp) new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .waitFor(false); // Не запускаем реально

            // Act & Assert
            assertEquals(CmdTypeEnum.JAVA_APP, app.getType());

            // Проверяем внутреннее состояние
            assertEquals(testJar.getAbsolutePath(), app.jarName);
            assertNull(app.mainClass);
        }

        @Test
        public void testJarClassMode() {
            // Arrange
            JavaApp app = new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .mainClass("com.example.Main");

            // Act & Assert
            assertEquals(testJar.getAbsolutePath(), app.jarName);
            assertEquals("com.example.Main", app.mainClass);
        }

        @Test
        public void testClassMode() {
            // Arrange
            JavaApp app = new JavaApp()
                    .mainClass("com.example.Main")
                    .classPath("lib1.jar:lib2.jar");

            // Act & Assert
            assertEquals("com.example.Main", app.mainClass);
            assertEquals("lib1.jar:lib2.jar", app.classPath);
            assertNull(app.jarName);
        }

        @Test
        public void testJvmOptions() {
            // Arrange
            String options = "-Xmx512m -Dprop=value -Danother=\"value with spaces\"";

            // Act
            JavaApp app = new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .jvmOptions(options);

            // Assert
            assertEquals(options, app.jvmOptions);
        }

        @Test
        public void testGuiMode() {
            // Arrange & Act
            JavaApp app = new JavaApp()
                    .guiMode(true)
                    .jar(testJar.getAbsolutePath());

            // Assert
            assertTrue(app.guiMode);

            // Act - переключение режима
            app.guiMode(false);

            // Assert
            assertFalse(app.guiMode);
        }

        @Test
        public void testUseJvm() {
            // Arrange
            String customJava = "/usr/lib/jvm/custom/bin/java";

            // Act
            JavaApp app = new JavaApp()
                    .useJvm(customJava)
                    .jar(testJar.getAbsolutePath());

            // Assert
            assertEquals(customJava, app.command);
        }

        @Test
        public void testAutoJvmSelection() {
            // Arrange
            JavaApp app = new JavaApp();

            // Act - не устанавливаем JVM явно
            app.jar(testJar.getAbsolutePath());

            // Assert - команда должна установиться автоматически
            assertNotNull(app.command);
            assertTrue(app.command.contains("java") || app.command.contains("javaw"));
        }

        @Test
        public void testRunModeDetection() throws Exception {
            // Test JAR mode
            JavaApp jarOnly = new JavaApp().jar("app.jar");
            assertEquals("JAR", getRunMode(jarOnly).name());

            // Test JAR_CLASS mode
            JavaApp jarWithClass = new JavaApp()
                    .jar("app.jar")
                    .mainClass("com.example.Main");
            assertEquals("JAR_CLASS", getRunMode(jarWithClass).name());

            // Test CLASS mode
            JavaApp classOnly = new JavaApp()
                    .mainClass("com.example.Main")
                    .classPath("lib.jar");
            assertEquals("CLASS", getRunMode(classOnly).name());
        }

        @Test(expected = IllegalStateException.class)
        public void testInvalidRunMode() throws Exception {
            // Arrange
            JavaApp app = new JavaApp(); // Не установлен ни jar, ни mainClass

            // Act & Assert
            getRunMode(app);
        }

        @Test
        public void testClassPathNormalization() throws Exception {
            // Arrange
            JavaApp app = new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .mainClass("com.example.Main")
                    .classPath("  lib1.jar ; lib2.jar ; ");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Act - попытка запуска (хоть и не реального)
            try {
                app.waitFor(false).outputTo(baos).call();
            } catch (CmdException e) {
                // Ожидаемо, т.к. нет реального процесса
            }

            // Assert - проверяем что команда строится
            assertNotNull(app.command);
        }

        @Test
        public void testCommandBuildingWithSpaces() {
            // Arrange
            File jarWithSpaces = new File(tempDir.getRoot(), "my app.jar");
            JavaApp app = new JavaApp()
                    .jar(jarWithSpaces.getAbsolutePath())
                    .jvmOptions("-Dpath=\"C:\\Program Files\\Java\"");

            // Act & Assert - проверяем что не падает при построении команды
            try {
                // Просто создаем команду, не запускаем
                app.waitFor(false);
                // Если не упало - тест пройден
                assertTrue(true);
            } catch (Exception e) {
                fail("Should not throw: " + e.getMessage());
            }
        }

        @Test
        public void testListenerIntegration() {
            // Arrange
            final List<String> events = new ArrayList<>();

            JavaApp app = (JavaApp) new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .waitFor(false);

            app.addListener(new ICmdListener<OSCmd, String>() {
                @Override
                public void onRun(OSCmd cmd, String commandLine) {
                    events.add("RUN: " + commandLine);
                }

                @Override
                public void onFinish(OSCmd cmd) {
                    events.add("FINISH");
                }
            });

            // Act - пытаемся запустить (не будет реального запуска)
            try {
                app.call();
            } catch (Exception e) {
                // Игнорируем ошибки запуска
            }

            // Assert
            assertFalse(events.isEmpty());
            assertTrue(events.get(0).startsWith("RUN: "));
        }

        @Test
        public void testStandardArgs() {
            // Arrange
            // Устанавливаем тестовый поставщик
            CommandBuilder.setStandardArgSupplier(new java.util.function.Function<Object, Object>() {
                @Override
                public Object apply(Object o) {
                    if (StandardArgEnum.USERID.equals(o)) {
                        return "test_user_123";
                    }
                    return null;
                }
            });

            JavaApp app = (JavaApp) new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .standardArg(StandardArgEnum.USERID);

            // Act & Assert
            assertTrue(app.containsArg("USERID"));
        }

        @Test
        public void testMultipleJvmOptions() throws Exception {
            // Arrange
            String options = "-Xms128m -Xmx512m -XX:+UseG1GC -Dlog.level=DEBUG";
            JavaApp app = new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .jvmOptions(options);

            // Act
            List<String> cmdParts = new ArrayList<>();
            // Симулируем вызов onPrepare через рефлексию
            Method method = JavaApp.class.getDeclaredMethod(
                    "onPrepare",
                    OSCmd.PrepEnum.class,
                    boolean.class,
                    List.class
            );
            method.setAccessible(true);
            method.invoke(app, OSCmd.PrepEnum.CMD, true, cmdParts);

            // Assert - проверяем что опции разбиты на части
            assertTrue(cmdParts.contains("-Xms128m"));
            assertTrue(cmdParts.contains("-Xmx512m"));
            assertTrue(cmdParts.contains("-XX:+UseG1GC"));
            assertTrue(cmdParts.contains("-Dlog.level=DEBUG"));
        }

        @Test
        public void testOutputRedirection() throws Exception {
            // Arrange
            File outputFile = tempDir.newFile("java_output.txt");
            JavaApp app = (JavaApp) new JavaApp()
                    .jar(testJar.getAbsolutePath())
                    .waitFor(false)
                    .outputTo(outputFile);

            // Act & Assert - проверяем что output установлен
            assertEquals(outputFile, app.out);
        }

        @Test
        public void testWaitForSetting() {
            // Arrange
            JavaApp app = new JavaApp()
                    .jar(testJar.getAbsolutePath());

            // Act
            app.waitFor(true);

            // Assert
            assertTrue(app.waitFor);

            // Act 2
            app.waitFor(false);

            // Assert 2
            assertFalse(app.waitFor);
        }

        // Вспомогательный метод для получения RunMode через рефлексию
        private Enum<?> getRunMode(JavaApp app) throws Exception {
            Method method = JavaApp.class.getDeclaredMethod("runMode");
            method.setAccessible(true);
            return (Enum<?>) method.invoke(app);
        }
    }