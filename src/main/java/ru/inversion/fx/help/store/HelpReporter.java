/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.help.store;

import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.utils.converter.TypeConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * Сущность для создания и записи протокола загрузки хелпов
 *
 * @author antonovdi
 */
public class HelpReporter {

    // Имя отчета
    private File helpFile;
    // Количество обработанных документов
    private int countDocuments;
    // Имя пользователя
    private String userName;
    // Количество успешных документов
    private int countDoneDocuments;
    // Протокол ошибок
    private StringBuilder errorProtocol;
    // Время начала обработки
    private LocalTime startProcessing;
    // Время обработки 
    private LocalTime totalTimeProcessing;

    public LocalTime getTotalTimeProcessing() {
        return totalTimeProcessing;
    }

    public void setTotalTimeProcessing(LocalTime totalTimeProcessing) {
        this.totalTimeProcessing = totalTimeProcessing;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public StringBuilder getErrorProtocol() {
        return errorProtocol;
    }

    public void setErrorProtocol(StringBuilder errorProtocol) {
        this.errorProtocol = errorProtocol;
    }

    public LocalTime getStartProcessing() {
        return startProcessing;
    }

    public void setStartProcessing(LocalTime startProcessing) {
        this.startProcessing = startProcessing;
    }

    public int getCountDoneDocuments() {
        return countDoneDocuments;
    }

    public void setCountDoneDocuments(int countDoneDocuments) {
        this.countDoneDocuments = countDoneDocuments;
    }

    public int getCountDocuments() {
        return countDocuments;
    }

    public void setCountDocuments(int countDocument) {
        this.countDocuments = countDocument;
    }

    public File getHelpFile() {
        return helpFile;
    }

    public void setHelpFile(File reportName) {
        this.helpFile = reportName;
    }

    public void createAndWriteReport() throws IOException {

        if (helpFile != null) {

            Path pathHelp = helpFile.toPath();
            String fileName = helpFile.getName();

            if (fileName.contains(".")) {
                fileName = fileName.split("\\.")[0];
            }
            Path pathProtocol = pathHelp.resolveSibling(fileName + ".log");
            Path pathError = pathHelp.resolveSibling(fileName + ".err");

            LocalTime timeProtocol = LocalTime.now();
            setTotalTimeProcessing(LocalTime.ofSecondOfDay(
                Duration.between(startProcessing, timeProtocol).getSeconds()));

            StringBuilder sb = new StringBuilder();
            sb.append("--------------------------------------------------------\n");
            sb.append("        П Р О Т О К О Л  О Б Р А Б О Т К И              \n");
            sb.append("--------------------------------------------------------\n");
            sb.append(" Пользователь                  : ").append(BaseApp.APP().
                getProperties(PropertiesTypeEnum.PRP).
                getStringProperty("ru.inversion.app.user_full_name")).append("\n");
            sb.append(" Алиас                         : ").append(userName).append("\n");
            sb.append(" Дата обработки                : ").
                append(TypeConverter.convertToString(LocalDate.now(), null)).append("\n");
            sb.append(" Время обработки               : ").
                append(TypeConverter.convertToString(startProcessing, null)).append("\n");
            sb.append(" Файл данных                   : ").
                append(helpFile.getAbsolutePath()).append("\n");
            sb.append(" Протокол обработки            : ").
                append(pathProtocol).append("\n");
            sb.append("--------------------------------------------------------\n");
            sb.append("--------------------------------------------------------\n");
            sb.append(" Всего обработано документов   : "
                + "").append(countDocuments).append("\n");
            sb.append(" Успешно обработано документов : ").
                append(countDoneDocuments).append("\n");
            sb.append(" Отбраковано документов        : ").
                append(countDocuments - countDoneDocuments).append("\n");
            sb.append(" Окончание процесса загрузки   : ").
                append(TypeConverter.convertToString(timeProtocol, null)).append("\n");
            sb.append(" Время процесса                : ").
                append(TypeConverter.convertToString(totalTimeProcessing, null)).append("\n");
            sb.append("--------------------------------------------------------\n");
            sb.append(errorProtocol);

            Iterable<? extends CharSequence> linesProtocol = Arrays.asList(sb.toString().split("\\n"));
            Files.write(pathProtocol, linesProtocol, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            if (errorProtocol == null || errorProtocol.length() != 0) {
                Iterable<? extends CharSequence> linesError = Arrays.asList(errorProtocol.toString().split("\\n"));
                Files.write(pathError, linesError, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

        }
    }

}
