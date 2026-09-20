package ru.inversion.fx.form.controls;

import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.apache.commons.io.input.BOMInputStream;
import org.controlsfx.control.StatusBar;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.form.property.BooleanProperties;
import ru.inversion.utils.S;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.app.frame.menu.PropertyItemEnum.XML_STYLE_BACKGROUND;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.DB_USER;
import static ru.inversion.fx.form.ActionFactory.createButton;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.icons.RowIconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;

/**
 * XML-viewer
 * @author lukichev
 */
public class JInvXMLViewer extends VBox implements IJInvControl {

    private static final String VALID_CODEPAGE = "[\\w\\s\\p{Punct}а-яёА-ЯЁ№]+";
    private static final Pattern XML_TAG = Pattern.compile("(?<ELEMENT>(<\\/?\\h*)(\\w+[^<>\\s]*\\w+)(>?)((<?[^<>]*)(\\/>)|(<?[^<>]*)(>)))|(?<COMMENT><!--[\\s\\S\\n]*?-->)");
    private static final Pattern ATTRIBUTES = Pattern.compile("(?<ATTRIBUTES>(?<!:)\\b((?!xmlns)\\w+)(\\h*=\\h*)(\"[^\"]*\"))|" +
            "(?<NAMESPACE>(xmlns[^<>\\s]*\\w*)(\\h*=\\h*)(\"[^\"]+\"))|" +
            "(?<CDATA>(<!\\[)(CDATA)(\\[)([^\"]+)(]]))");
    private final static Logger logger = getLogger(MethodHandles.lookup().lookupClass());

    final private static ResourceBundle xml_bundle = ResourceBundle.getBundle("fore");

    static final private ObservableList<Charset> charsets = FXCollections.observableArrayList(
            StandardCharsets.UTF_8,
            Charset.forName("cp1251"),
            StandardCharsets.UTF_16,
            StandardCharsets.ISO_8859_1
    );

    private static final int GROUP_OPEN_BRACKET = 2;
    private static final int GROUP_ELEMENT_NAME = 3;
    private static final int GROUP_OPTIONAL_CLOSE_BRACKET = 4;
    private static final int GROUP_ATTRIBUTES_SECTION = 6;
    private static final int GROUP_CLOSE_BRACKET = 7;

    private static final int GROUP_ATTRIBUTE_NAME = 2;
    private static final int GROUP_ATTRIBUTE_EQUAL_SYMBOL = 3;
    private static final int GROUP_ATTRIBUTE_VALUE = 4;

    private static final int GROUP_NAMESPACE_NAME = 6;
    private static final int GROUP_NAMESPACE_EQUAL_SYMBOL = 7;
    private static final int GROUP_NAMESPACE_VALUE = 8;

    private static final int GROUP_CDATA_FIRST_OPEN_BRACKET = 10;
    private static final int GROUP_CDATA_WORD = 11;
    private static final int GROUP_CDATA_SECOND_OPEN_BRACKET = 12;
    private static final int GROUP_CDATA_TEXT = 13;
    private static final int GROUP_CDATA_CLOSE_BRACKET = 14;

    private int totalMatches;
    private int indexOfParagraphsMatches;
    private List<Integer> listOfParagraphs;
    private final CodeArea codeArea = new CodeArea();
    private final ComboBox<Charset> comboOpenCodePage = new ComboBox<>();
    private TextField searchField = new TextField();
    private TextField countMatches = new TextField();
    private Button searchButton = new Button(xml_bundle.getString("XML_SEARCH_AGAIN"));
    private CheckBox chMatchCase = new CheckBox(xml_bundle.getString("XML_WORD_CASE"));
    private CheckBox chWord = new CheckBox(xml_bundle.getString("XML_SEARCH_WORD"));
    private CheckBox chRegex = new CheckBox(xml_bundle.getString("XML_SEARCH_REGEX"));
    private CheckBox chWrap = new CheckBox(xml_bundle.getString("XML_WRAP_TEXT"));
    private StatusBar statusBar;
    private Charset currentCharset;

    private String  inputXML;
    private String  transformedStringXML;
    private Charset correctCodePage;
    private boolean badXML = false;

    //Флаг переключения мода работы компонента
    //private boolean editableMode = false;

    /**
     * Определять ли кодировку из заголовка
     */
    private BooleanProperty autoDetectEncoding = new SimpleBooleanProperty(true);
    /**
     * Сохранять ли оригинальный заголовок при сохранении
     */
    private BooleanProperty saveOriginalHeader = new SimpleBooleanProperty(false);
    /**
     * Текущая кодировка
     */
    private ObjectProperty<Charset> encoding = new SimpleObjectProperty<>(charsets.get(0));

    private StringProperty savingfilename = new SimpleStringProperty("name");

    public void setSavingfilename(String savingfilename) {
        this.savingfilename.set(savingfilename);
    }

    /** */
    public JInvXMLViewer( )
    {
        this(false);
    }
    
    /**
     * Создание компонента с выбором режима работы
     * @param editable true - появляется возможность редактировать текст, false - только просмотр
     */
    public JInvXMLViewer(boolean editable)
    {
        initJInvXMLViewer(editable);
    }

    /** */
    private void initJInvXMLViewer( boolean editable ) {

        //editableMode = editable;
        readOnlyProperty().set( !editable) ;

        setFillWidth (true);

        initToolBar  (this);
        initCodeArea (this);
        initStatusBar(this);

        comboOpenCodePage.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (!codeArea.getText().isEmpty()) {
                currentCharset = newValue;
                if (newValue == correctCodePage) {
                    codeArea.replaceText(transformedStringXML);
                } else {
                    codeArea.replaceText(new String(codeArea.getText().getBytes(oldValue), newValue));
                }
            }
        });

        searchButton.setDisable(true);
        searchButton.setOnMouseClicked(event -> searchAgain());

        chRegex.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                chWord.setSelected(false);
                chWord.setDisable(true);
                chMatchCase.setSelected(true);
            } else {
                chWord.setDisable(false);
                chMatchCase.setSelected(false);
            }
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            searchButton.setDisable(true);
            countMatches.setText("");
            if (newValue.isEmpty()) {
                if(!badXML) {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                } else {
                    codeArea.replaceText(inputXML);
                    codeArea.showParagraphAtTop(0);
                    codeArea.moveTo(0, 0);
                }
                countMatches.setText("");
                searchButton.setDisable(true);
            }
        });

        chWrap.selectedProperty().bindBidirectional(codeArea.wrapTextProperty());

        searchField.addEventHandler(KeyEvent.KEY_PRESSED, KE -> {
            if (!codeArea.getText().isEmpty()) {
                if (KE.getCode() == KeyCode.ENTER) {
                    String value = searchField.getText();
                    if(!badXML) {
                        codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                    } else {
                        codeArea.replaceText(inputXML);
                    }
                    if (chRegex.isSelected()) {
                        if (chMatchCase.isSelected())
                            searchByRegex(value, true);
                        else
                            searchByRegex(value, false);
                    } else {
                        if (chMatchCase.isSelected()) {
                            if (chWord.isSelected())
                                searchByWord(value, true);
                            else
                                search(value, true);
                        } else {
                            if (chWord.isSelected())
                                searchByWord(value, false);
                            else
                                search(value, false);
                            }
                        }
                    if (!value.isEmpty())
                        countMatches.setText(totalMatches + " " + xml_bundle.getString("XML_COUNT_MATCHES"));
                    else
                        countMatches.setText("");
                    }
                }
        });
        comboOpenCodePage.getSelectionModel().select(0);

        if( !isReadOnly() ) {
            codeArea.setOnKeyReleased((event) -> {
                validXML();
            });
        }
    }
    
    private void initToolBar(VBox vBox) {
        ButtonBase btnSave = createButton(
                new ActionBuilder()
                        .handler(e -> saveFile())
                        .toolTipText(xml_bundle.getString("XML_BTN_SAVE"))
                        .icon(new RowIconDescriptorBuilder().padding(1).add(FontAwesome.fa_floppy_o).build())
                        .build());
        ButtonBase btnCopy = createButton(
                new ActionBuilder()
                        .handler(e -> copyToClipBoard())
                        .toolTipText(xml_bundle.getString("XML_BTN_COPY"))
                        .icon(new RowIconDescriptorBuilder().padding(1).add(FontAwesome.fa_copy).build())
                        .build());
        ButtonBase btnLoad = createButton(
                new ActionBuilder()
                        .handler(e -> loadXml())
                        .toolTipText(xml_bundle.getString("XML_BTN_LOAD"))
                        .icon(new RowIconDescriptorBuilder().padding(1).add(FontAwesome.fa_folder_open_o).build())
                        .build());
        ButtonBase btnValid = createButton(
                new ActionBuilder()
                        .handler(e -> openValidFileXML())
                        .toolTipText(xml_bundle.getString("XML_BTN_VALID"))
                        .icon(new RowIconDescriptorBuilder().padding(1).add(FontAwesome.fa_check_square_o).build())
                        .build());
        Label searchLabel = new Label(xml_bundle.getString("XML_SEARCH"));
        ToolBar tb = new JInvToolBar();
        countMatches.setStyle("-fx-background-color:transparent");
        countMatches.setPrefWidth(130.0);

        comboOpenCodePage.setItems(charsets);

        tb.getItems().addAll( comboOpenCodePage, new Separator(), btnCopy, btnSave );

        //Проверяем мод, если доступно редактирование, то добавляем кнопку загрузки
        if(!isReadOnly() ) {
            tb.getItems().addAll(btnLoad);
            tb.getItems().addAll(btnValid);
        }

        tb.getItems().addAll(
                new Separator(),
                chWrap,
                new Separator(),
                searchLabel,
                searchField,
                searchButton,
                chMatchCase,
                chWord,
                chRegex,
                countMatches
        );
        vBox.getChildren().addAll(tb);
    }

    private void initStatusBar(VBox vbox) {
        statusBar = new StatusBar();
        vbox.getChildren().addAll(statusBar);
    }

    private void initCodeArea(VBox vBox) {
        StackPane stackPane = new StackPane();
        stackPane.setStyle("-fx-border-color: #7F7679;");
        stackPane.getChildren().add(new VirtualizedScrollPane(codeArea));
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if(!badXML)
                codeArea.setStyleSpans(0, computeHighlighting(newText));
        });
        codeArea.setWrapText(false);

        codeArea.editableProperty().bindBidirectional( BooleanProperties.not( readOnlyProperty() )); // (editableMode);

        final IAppProperties ap = BaseApp.APP().getProperties(DB_USER);

        final Function<Color, String> toRGBCode = new Function<Color, String>() {
            @Override
            public String apply(Color color) {
                return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
            }
        };
        codeArea.setStyle(
                "-fx-background-color: "
                        +
                        ap.getStringProperty(XML_STYLE_BACKGROUND.name(), toRGBCode.apply((Color) XML_STYLE_BACKGROUND.getValue()))
                        +
                        ";"
        );
        vBox.getChildren().addAll(stackPane);
        vBox.setVgrow(stackPane, Priority.ALWAYS);
    }

    /**
     * Поиск по символам
     */
    private void search(String searchText, boolean mCase) {
        int caret = 0;
        totalMatches = 0;
        listOfParagraphs = new ArrayList<>();
        indexOfParagraphsMatches = 1;
        int indexOfSearchText;
        for (int i = 0; i < codeArea.getParagraphs().size(); i++) {
            String text = codeArea.getText(i);
            int lengthOfLine = text.length();

            if (!searchText.isEmpty()) {
                int lengthOfSearchText = searchText.length();
                if (mCase) {
                    indexOfSearchText = text.indexOf(searchText);
                } else {
                    indexOfSearchText = text.toLowerCase().indexOf(searchText.toLowerCase());
                }
                if (indexOfSearchText != -1) {
                    int indexForHighlighting = caret + indexOfSearchText;
                    codeArea.moveTo(caret);
                    listOfParagraphs.add(codeArea.getCurrentParagraph());
                    while (indexOfSearchText != -1) {
                        codeArea.setStyleClass(indexForHighlighting, indexForHighlighting + lengthOfSearchText, "highlight");
                        totalMatches++;
                        text = text.substring(indexOfSearchText + lengthOfSearchText);
                        if (mCase) {
                            indexOfSearchText = text.indexOf(searchText);
                        } else {
                            indexOfSearchText = text.toLowerCase().indexOf(searchText.toLowerCase());
                        }
                        indexForHighlighting += indexOfSearchText + lengthOfSearchText;
                    }
                }
            }
            caret = caret + lengthOfLine + 1;
        }
        if (listOfParagraphs.size() > 0) {
            codeArea.showParagraphAtTop(listOfParagraphs.get(0));
            searchButton.setDisable(false);
        } else {
            searchButton.setDisable(true);
        }
    }

    /**
     * Поиск по слову
     */
    private void searchByWord(String searchText, boolean mCase) {
        Pattern findword;
        Matcher matcher;
        int caret = 0;
        listOfParagraphs = new ArrayList<>();
        indexOfParagraphsMatches = 1;
        totalMatches = 0;
        for (int i = 0; i < codeArea.getParagraphs().size(); i++) {
            String text = codeArea.getText(i);
            int lengthOfLine = text.length();
            if (mCase) {
                findword = Pattern.compile("(?:^|[^a-zA-Zа-яА-ЯёЁ])(" + searchText + ")(?![a-zA-Zа-яА-ЯёЁ])");
                matcher = findword.matcher(text);
            } else {
                findword = Pattern.compile("(?:^|[^a-zA-Zа-яА-ЯёЁ])(" + searchText.toLowerCase() + ")(?![a-zA-Zа-яА-ЯёЁ])");
                matcher = findword.matcher(text.toLowerCase());
            }
            while (matcher.find()) {
                codeArea.setStyleClass(caret + matcher.start(1), caret + matcher.end(1), "highlight");
                codeArea.moveTo(caret);
                int par = codeArea.getCurrentParagraph();
                if (!listOfParagraphs.contains(par))
                    listOfParagraphs.add(par);
                totalMatches++;
            }
            caret = caret + lengthOfLine + 1;
        }
        if (listOfParagraphs.size() > 0) {
            codeArea.showParagraphAtTop(listOfParagraphs.get(0));
            searchButton.setDisable(false);
        } else {
            searchButton.setDisable(true);
        }
    }

    /**
     * Поиск по регулярному выражению
     */
    private void searchByRegex(String searchText, boolean mCase) {
        int caret = 0;
        listOfParagraphs = new ArrayList<>();
        indexOfParagraphsMatches = 1;
        totalMatches = 0;
        Pattern pattern;
        Matcher matcher;
        for (int i = 0; i < codeArea.getParagraphs().size(); i++) {
            String text = codeArea.getText(i);
            int lengthOfLine = text.length();
            if (!searchText.isEmpty()) {
                try {
                    if (mCase) {
                        pattern = Pattern.compile(searchText);
                        matcher = pattern.matcher(text);
                    } else {
                        pattern = Pattern.compile(searchText.toLowerCase());
                        matcher = pattern.matcher(text.toLowerCase());
                    }
                    while (matcher.find()) {
                        codeArea.moveTo(caret);
                        int par = codeArea.getCurrentParagraph();
                        if (!listOfParagraphs.contains(par))
                            listOfParagraphs.add(par);
                        codeArea.setStyleClass(caret + matcher.start(), caret + matcher.end(), "highlight");
                        totalMatches++;
                    }
                } catch (PatternSyntaxException e) {
                    if(!badXML) {
                        codeArea.setStyleSpans(caret, computeHighlighting(text));
                    } else {
                        codeArea.replaceText(inputXML);
                    }
                }
            }
            caret = caret + lengthOfLine + 1;
        }
        if (listOfParagraphs.size() > 0) {
            codeArea.showParagraphAtTop(listOfParagraphs.get(0));
            searchButton.setDisable(false);
        } else {
            searchButton.setDisable(true);
        }
    }

    /**
     * Следущий результат поиска
     */
    private void searchAgain() {
        if (indexOfParagraphsMatches == listOfParagraphs.size())
            indexOfParagraphsMatches = 0;
        codeArea.showParagraphAtTop(listOfParagraphs.get(indexOfParagraphsMatches));
        indexOfParagraphsMatches++;
    }

    /**
     * Скопировать в буфер обмена
     */
    private void copyToClipBoard() {
        ClipboardContent content = new ClipboardContent();
        String textToClipboard = codeArea.getSelectedText();
        content.putString(textToClipboard);
        Clipboard.getSystemClipboard().setContent(content);
    }
    
    /**Загрузка xml из файла*/
    private void loadXml()
    {
        openFile();
    }
    
    /**
     * Выбор схемы для проверки
     */
    private void openValidFileXML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XSD");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Files (*.xsd,*dtd)", "*.xsd", "*.dtd");
        fileChooser.getExtensionFilters().add(extFilter);
        XSDFile = (fileChooser.showOpenDialog(getScene().getWindow()));
        validXML();
    }
    
    private File XSDFile = null;
    
    /**Проверка xml с использованием схем */
    private void validXML() {
        if(S.isNullOrEmpty(this.getText()))
            return;
        if (XSDFile != null) {
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                Schema schema = factory.newSchema(XSDFile);
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(new ByteArrayInputStream(this.getText().getBytes(StandardCharsets.UTF_8))));
                statusBar.setText(xml_bundle.getString("XML_FORMAT_GOOD"));
            } catch (IOException ex) {
            } catch (SAXException e) {
                statusBar.setText(e.getMessage());
            }
        }
    }
    
    /**Проверка xml с использованием схем
     * @param xml 
     * @param shema Файл (схема) по которому будет происходить проверка
     * @return  true - xml прошел проверку, false - xml не соответствует схеме*/
    public boolean validXML(String xml, File shema) throws SAXException, IOException, Exception {
        if (shema == null) {
            throw new Exception("File is null");
        }
        if (S.isNullOrEmpty(xml)) {
            throw new Exception("xml is null or empty");
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            Schema schema = factory.newSchema(shema);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
            return true;
        } catch (IOException ex) {
            throw new IOException(ex);
        } catch (SAXException e) {
            throw new SAXException(e); 
        }

    }

    /**
     *
     */
    static final private XMLInputFactory factory = XMLInputFactory.newInstance();

    private void doTransform(String s){
        inputXML = s.trim();
        chooseValidCodePage(s);
    }

    private void noXMLData(){
        codeArea.clear();
        statusBar.setText(xml_bundle.getString("XML_NO_DATA"));
    }

    /**
     * XML из строки
     */
    public void loadString(String s) throws IOException {
        if (s == null || s.length() == 0) {
            noXMLData();
            return;
        }
        doTransform(s);
    }

    /**
     * XML из ридера
     */
    public void loadReader(Reader reader) {
        if (reader == null) {
            noXMLData();
            return;
        }
        doTransform(S.toString(reader));
    }

    /**
     * XML из потока
     */
    public void loadStream(InputStream is) throws IOException {
        if (is == null) {
            noXMLData();
            return;
        }
        doTransform(getStringFromStream(is));
    }

    /**
     * XML из файла
     */
    public void loadFile(File file) throws IOException {
        if (file == null || !file.isFile() || !file.exists()) {
            noXMLData();
            throw new FileNotFoundException(file == null ? "'file' param is null" : file.getAbsolutePath());
        }
        doTransform(getStringFromStream(new FileInputStream(file)));
    }

    /**
     * Получение строки из потока
     */
    private String getStringFromStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

    /**
     * Выбор правильной кодировки для отображения
     */
    private void chooseValidCodePage(String str) {
        String outString = "";
        boolean isValidCp = false;
        for (Charset ch : charsets) {
            outString = transformXML(str, ch);
            if (outString != null) {
                if (Pattern.matches(VALID_CODEPAGE, outString)) {
                    isValidCp = true;
                    currentCharset = ch;
                    comboOpenCodePage.getSelectionModel().select(currentCharset);
                    transformedStringXML = outString;
                    correctCodePage = ch;
                    break;
                }
            }
        }
        if (!isValidCp) {
            currentCharset = charsets.get(0);
            comboOpenCodePage.getSelectionModel().select(currentCharset);
            outString = transformXML(str, currentCharset);
        }
        if (!badXML) {
            if(outString==null)
                setTextToCodeArea(str);else
            setTextToCodeArea(outString);
        } else {
            setTextToCodeArea(inputXML);
        }
    }

    /**
     * Форматирование в структуру XML-документа
     */
    private String transformXML(String xml, Charset ch) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(ch)));
            OutputFormat format = new OutputFormat(doc);
            format.setIndenting(true);
            format.setIndent(2);
            format.setLineWidth(2000);
            if(!inputXML.startsWith("<?xml version=")) {
                format.setOmitXMLDeclaration(true);
            }
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(doc);
            String formatedXMLString = out.toString();
//            formatedXMLString = formatedXMLString.replace("&quot;", "'");
            codeArea.setWrapText(false);
            statusBar.setText(xml_bundle.getString("XML_FORMAT_GOOD"));
            badXML = false;
            return formatedXMLString.substring(0, formatedXMLString.length() - 1);
        } catch (MalformedByteSequenceException e) {
            statusBar.setText(xml_bundle.getString("XML_UNABLE_DETECT_CODEPAGE"));
            codeArea.setWrapText(true);
            return null;
        } catch (SAXException e) {
            badXML = true;
            statusBar.setText(xml_bundle.getString("XML_FORMAT_BAD"));
            codeArea.setWrapText(true);
            return null;
        } catch (IOException | ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Загрузка текста в CodeArea
     */
    private void setTextToCodeArea(String text) {
        searchField.setText(S.EMPTY_STRING);
        codeArea.clear();
        codeArea.appendText(text);
        Pattern pattern = Pattern.compile("<[?].+[?]>");
        Matcher matcher = pattern.matcher(codeArea.getParagraph(0).getText());
        String headerOfXML = "<?xml version=\"1.0\" encoding=\"" + currentCharset.displayName() + "\"?>";
        while (matcher.find()) {
            codeArea.replaceText(matcher.start(), matcher.end(), headerOfXML);
        }
        codeArea.showParagraphAtTop(0);
        codeArea.moveTo(0, 0);
    }

    /**
     * Запись в файл
     */
    private void saveFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(new File(".").getAbsolutePath()));
        fileChooser.setInitialFileName(savingfilename.get());
        fileChooser.setTitle("Save Document");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        
        if(currentCharset==null)
                currentCharset=StandardCharsets.UTF_8;
        
        if (file != null) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(file), currentCharset.displayName()))) {
                bufferedWriter.write(codeArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Раскраска текста
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = XML_TAG.matcher(text);
        int changeAttributesSection;
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else {
                if (matcher.group("ELEMENT") != null) {
                        if (matcher.group(GROUP_ATTRIBUTES_SECTION) != null)
                        changeAttributesSection = 0;
                        else
                        changeAttributesSection = 2;

                    String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION + changeAttributesSection);

                    spansBuilder.add(Collections.singleton("bracket"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
                    spansBuilder.add(Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.start(GROUP_ELEMENT_NAME));
                    spansBuilder.add(Collections.singleton("bracket"), matcher.end(GROUP_OPTIONAL_CLOSE_BRACKET) - matcher.start(GROUP_OPTIONAL_CLOSE_BRACKET));

                    if (!attributesText.isEmpty()) {
                        lastKwEnd = 0;
                        Matcher amatcher = ATTRIBUTES.matcher(attributesText);
                        while (amatcher.find()) {
                            spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);
                            if (amatcher.group("ATTRIBUTES") != null) {
                                spansBuilder.add(Collections.singleton("attribute_name"), amatcher.end(GROUP_ATTRIBUTE_NAME) - amatcher.start(GROUP_ATTRIBUTE_NAME));
                                spansBuilder.add(Collections.singleton("equal"), amatcher.end(GROUP_ATTRIBUTE_EQUAL_SYMBOL) - amatcher.start(GROUP_ATTRIBUTE_EQUAL_SYMBOL));
                                spansBuilder.add(Collections.singleton("attribute_value"), amatcher.end(GROUP_ATTRIBUTE_VALUE) - amatcher.start(GROUP_ATTRIBUTE_VALUE));
                            }
                            if (amatcher.group("NAMESPACE") != null) {
                                spansBuilder.add(Collections.singleton("ns_name"), amatcher.end(GROUP_NAMESPACE_NAME) - amatcher.start(GROUP_NAMESPACE_NAME));
                                spansBuilder.add(Collections.singleton("equal"), amatcher.end(GROUP_NAMESPACE_EQUAL_SYMBOL) - amatcher.start(GROUP_NAMESPACE_EQUAL_SYMBOL));
                                spansBuilder.add(Collections.singleton("ns_value"), amatcher.end(GROUP_NAMESPACE_VALUE) - amatcher.start(GROUP_NAMESPACE_VALUE));
                            }
                            if (amatcher.group("CDATA") != null) {
                                spansBuilder.add(Collections.singleton("bracket"), amatcher.end(GROUP_CDATA_FIRST_OPEN_BRACKET) - amatcher.start(GROUP_CDATA_FIRST_OPEN_BRACKET));
                                spansBuilder.add(Collections.singleton("cdata_word"), amatcher.end(GROUP_CDATA_WORD) - amatcher.start(GROUP_CDATA_WORD));
                                spansBuilder.add(Collections.singleton("bracket"), amatcher.end(GROUP_CDATA_SECOND_OPEN_BRACKET) - amatcher.start(GROUP_CDATA_SECOND_OPEN_BRACKET));
                                spansBuilder.add(Collections.singleton("cdata_value"), amatcher.end(GROUP_CDATA_TEXT) - amatcher.start(GROUP_CDATA_TEXT));
                                spansBuilder.add(Collections.singleton("bracket"), amatcher.end(GROUP_CDATA_CLOSE_BRACKET) - amatcher.start(GROUP_CDATA_CLOSE_BRACKET));
                            }
                            lastKwEnd = amatcher.end();
                        }
                            if (attributesText.length() > lastKwEnd)
                            spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
                        }

                    lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION + changeAttributesSection);

                    spansBuilder.add(Collections.singleton("bracket"), matcher.end(GROUP_CLOSE_BRACKET + changeAttributesSection) - lastKwEnd);
                }
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    /**
     *
     */
    private void applyColors() {
        String bracket = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String anytag = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String attribute_name = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String attribute_value = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String ns_name = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String ns_value = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String cdata_word = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String cdata_value = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String equal = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String comment = toRGBCode(Color.color(Math.random(), Math.random(), Math.random()));
        String cssBody = ".bracket {-fx-fill:" + bracket + ";}\n" +
                ".anytag {-fx-fill:" + anytag + ";}\n" +
                ".attribute_name {-fx-fill:" + attribute_name + ";}\n" +
                ".ns_name {-fx-fill:" + ns_name + ";}\n" +
                ".ns_value {-fx-fill:" + ns_value + ";}\n" +
                ".attribute_value {-fx-fill:" + attribute_value + ";}\n" +
                ".cdata_word {-fx-fill:" + cdata_word + ";}\n" +
                ".cdata_value {-fx-fill:" + cdata_value + ";}\n" +
                ".comment {-fx-fill:" + comment + ";}\n" +
                ".equal {-fx-fill:" + equal + ";}\n" +
                ".highlight {-rtfx-background-color: yellow;}";
        try {
//            cssFile.delete();
//            cssFile.createNewFile();
//            BufferedWriter bw = new BufferedWriter(new FileWriter(cssFile));
//            bw.write(cssBody);
//            bw.close();
//            String pathToCss = cssFile.toURI().toString();
//            if (codeArea.getStylesheets().contains(pathToCss))
//                codeArea.getStylesheets().remove(pathToCss);
//            codeArea.getStylesheets().add(pathToCss);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     *
     */
    private void loadStream(InputStream is, boolean isChangeEncoding) throws IOException {

        String encoding = null;
        Charset charset = null;

        if (!isChangeEncoding) {
            final XMLEventReader xmlReader;

            try {

                xmlReader = factory.createXMLEventReader(new BOMInputStream(is));

                while (xmlReader.hasNext()) {
                    final XMLEvent xmlEvent = xmlReader.nextEvent();

                    if (xmlEvent.isStartDocument()) {
                        StartDocument startDocument = (StartDocument) xmlEvent;

                        if (isAutoDetectEncoding() && startDocument.encodingSet()) {
                            encoding = startDocument.getCharacterEncodingScheme();

                            break;
                        }
                    }

                    if (xmlEvent.isStartElement())
                        break;
                    }

                xmlReader.close();

            } catch (XMLStreamException e) {
                throw new IOException(Tags.PRODUCT_LABEL + "Error on parse xml input stream", e);
            }

            if (encoding == null)
                encoding = getEncoding().toString();

//        Charset charset = null;

            try {
                charset = Charset.forName(encoding);
            } catch (Throwable ignored) {
            }

            if (charset != null) {
                if (!charsets.contains(charset))
                    charsets.add(charset);
            } else
                charset = charsets.get(0);

            comboOpenCodePage.getSelectionModel().select(charset);

        } else {
            charset = Charset.forName(getEncoding().toString());
        }

        is.reset();

        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, charset))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.trim().length() != 0)
                    sb.append(line).append('\n');
                }
            }
        setTextToCodeArea(sb.toString());
    }

    /**
     *
     */
    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML Document");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
             try {
                String res = "";
                int ts = 8192;
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), ts);
                String str;
                while ((str = br.readLine()) != null) {
                    res += str;
                }
                doTransform(res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Определять ли кодировку из заголовка
     */
    public BooleanProperty autoDetectEncodingProperty() {
        return autoDetectEncoding;
    }

    /**
     * Определять ли кодировку из заголовка
     */
    public boolean isAutoDetectEncoding() {
        return autoDetectEncoding.get();
    }

    /**
     * Определять ли кодировку из заголовка
     */
    public void setAutoDetectEncoding(boolean autoDetectEncoding) {
        this.autoDetectEncoding.set(autoDetectEncoding);
    }

    /**
     * Текущая кодировка
     */
    public Charset getEncoding() {
        return encoding.get();
    }

    /**
     * Текущая кодировка
     */
    public Property<Charset> encodingProperty() {
        return encoding;
    }

    /**
     * Текущая кодировка
     */
    public void setEncoding(Charset encoding) {
        this.encoding.set(encoding);
    }

    /**
     * Сохранять ли оригинальный заголовок при сохранении
     */
    public boolean isSaveOriginalHeader() {
        return saveOriginalHeader.get();
    }

    /**
     * Сохранять ли оригинальный заголовок при сохранении
     */
    public BooleanProperty saveOriginalHeaderProperty() {
        return saveOriginalHeader;
    }

    /**
     * Сохранять ли оригинальный заголовок при сохранении
     */
    public void setSaveOriginalHeader(boolean saveOriginalHeader) {
        this.saveOriginalHeader.set(saveOriginalHeader);
    }

    /**
     * Получение текста
     */
    public String getText() {
        return codeArea.getText();
    }
}
