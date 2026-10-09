package ru.inversion.fore.form.lov;

import javafx.scene.input.KeyCombination;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/** Интерпретатор LXML версии 1: строгая схема, типы и проверка ссылок до открытия окна. */
public final class LxmlLoader
{
   private static final int MAX_BYTES = 1024 * 1024;
   private static final Schema SCHEMA = schema();
   private final ResourceBundle bundle;

   public LxmlLoader() { this(null); }

   /** Ссылки %ключ разрешаются только в заголовках; %% обозначает обычный знак %. */
   public LxmlLoader(ResourceBundle bundle) { this.bundle = bundle; }

   public ForeLov load(URL resource, LovSources sources) throws IOException
   {
      LovDefinition definition = read(resource);
      return new ForeLov(definition, sources.resolve(definition));
   }

   public LovDefinition read(URL resource) throws IOException
   {
      Objects.requireNonNull(resource, "Не найден ресурс LXML");
      try( InputStream input = resource.openStream() ) { return read(input); }
   }

   /** Поток принадлежит вызывающему коду. Чтение определения не запускает JavaFX. */
   public LovDefinition read(InputStream input) throws IOException
   {
      byte[] xml = input.readNBytes(MAX_BYTES + 1);
      if( xml.length > MAX_BYTES ) throw new IOException("LXML превышает 1 МиБ");
      try
      {
         var factory = DocumentBuilderFactory.newInstance();
         factory.setNamespaceAware(true);
         factory.setXIncludeAware(false);
         factory.setExpandEntityReferences(false);
         factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
         factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
         factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
         factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
         factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
         factory.setSchema(SCHEMA);
         var builder = factory.newDocumentBuilder();
         builder.setEntityResolver((publicId, systemId) -> { throw new SAXException("Внешние сущности запрещены"); });
         builder.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException ex) throws SAXException { throw ex; }
            @Override public void error(SAXParseException ex) throws SAXException { throw ex; }
            @Override public void fatalError(SAXParseException ex) throws SAXException { throw ex; }
         });
         Element root = builder.parse(new ByteArrayInputStream(xml)).getDocumentElement();
         Element window = child(root, "window");
         Element behavior = child(root, "behavior");
         Element search = child(root, "search");
         var columns = new ArrayList<LovDefinition.Column>();
         for( Element column : children(child(root, "columns")) )
            columns.add(new LovDefinition.Column(column.getAttribute("name"),
                  caption(attr(column, "title", column.getAttribute("name"))),
                  LovDefinition.ValueType.valueOf(attr(column, "type", "string").toUpperCase(Locale.ROOT)),
                  number(column, "width", 160), attr(column, "return-to", "")));
         String key = attr(behavior, "key", "F9");
         KeyCombination.valueOf(key);
         return new LovDefinition(root.getAttribute("id"), caption(root.getAttribute("title")),
               child(root, "comment") == null ? "" : child(root, "comment").getTextContent(),
               new LovDefinition.Window(number(window, "width", 640), number(window, "height", 420),
                     coordinate(window, "x"), coordinate(window, "y"),
                     bool(window, "automatic-position", false), bool(window, "automatic-column-width", false),
                     LovDefinition.Direction.valueOf(attr(window, "direction", "inherit")
                           .toUpperCase(Locale.ROOT).replace('-', '_')), attr(window, "style-class", "")),
               new LovDefinition.Behavior(bool(behavior, "auto-display", false),
                     bool(behavior, "auto-refresh", true), bool(behavior, "auto-select", false),
                     bool(behavior, "auto-skip", false), bool(behavior, "filter-before-display", false),
                     bool(behavior, "validate-from-list", false), key),
               new LovDefinition.Search(LovDefinition.MatchMode.valueOf(attr(search, "mode", "prefix").toUpperCase(Locale.ROOT)),
                     bool(search, "case-sensitive", false), integer(search, "min-length", 0), integer(search, "max-rows", 200)),
               columns, source(children(child(root, "source")).getFirst(), columns));
      }
      catch( Exception ex )
      {
         throw new IOException("Некорректный LXML: " + ex.getMessage(), ex);
      }
   }

   private LovDefinition.Source source(Element element, List<LovDefinition.Column> columns)
   {
      return switch( element.getLocalName() )
      {
         case "provider" -> new LovDefinition.ProviderSource(element.getAttribute("ref"));
         case "sql" -> new LovDefinition.SqlSource(element.getAttribute("ref"), child(element, "query").getTextContent().strip(),
               children(element).stream().filter(e -> e.getLocalName().equals("bind"))
                     .map(e -> e.getAttribute("name")).toList(),
               integer(element, "fetch-size", 100), integer(element, "timeout-seconds", 30));
         case "static" -> {
            var rows = new ArrayList<LovRow>();
            var types = new LinkedHashMap<String, LovDefinition.ValueType>();
            columns.forEach(column -> types.put(column.name(), column.type()));
            for( Element row : children(element) )
            {
               var values = new LinkedHashMap<String, Object>();
               for( Element value : children(row) )
               {
                  String name = value.getAttribute("column");
                  if( !types.containsKey(name) || values.containsKey(name) )
                     throw new IllegalArgumentException("Неизвестная или повторная колонка строки: " + name);
                  if( bool(value, "null", false) && !value.getTextContent().isBlank() )
                     throw new IllegalArgumentException("NULL не может иметь текст: " + name);
                  values.put(name, bool(value, "null", false) ? null : types.get(name).convert(value.getTextContent()));
               }
               if( !values.keySet().equals(types.keySet()) )
                  throw new IllegalArgumentException("В строке должны присутствовать все объявленные колонки");
               rows.add(new LovRow(values));
            }
            yield new LovDefinition.StaticSource(rows);
         }
         default -> throw new IllegalArgumentException("Неизвестный источник: " + element.getLocalName());
      };
   }

   private String caption(String value)
   {
      if( value.startsWith("%%") ) return value.substring(1);
      if( !value.startsWith("%") ) return value;
      if( bundle == null ) throw new IllegalArgumentException("Для заголовка " + value + " не задан ResourceBundle");
      return bundle.getString(value.substring(1));
   }

   private static List<Element> children(Element element)
   {
      var result = new ArrayList<Element>();
      if( element != null )
         for( var node = element.getFirstChild(); node != null; node = node.getNextSibling() )
            if( node instanceof Element child ) result.add(child);
      return result;
   }

   private static Element child(Element element, String name)
   {
      return children(element).stream().filter(e -> name.equals(e.getLocalName())).findFirst().orElse(null);
   }

   private static String attr(Element element, String name, String fallback)
   {
      return element == null || !element.hasAttribute(name) ? fallback : element.getAttribute(name);
   }

   private static boolean bool(Element element, String name, boolean fallback)
   {
      String value = attr(element, name, Boolean.toString(fallback));
      return "true".equals(value) || "1".equals(value);
   }

   private static double number(Element element, String name, double fallback)
   {
      return Double.parseDouble(attr(element, name, Double.toString(fallback)));
   }

   private static int integer(Element element, String name, int fallback)
   {
      return Integer.parseInt(attr(element, name, Integer.toString(fallback)));
   }

   private static Double coordinate(Element element, String name)
   {
      return element != null && element.hasAttribute(name) ? Double.valueOf(element.getAttribute(name)) : null;
   }

   private static Schema schema()
   {
      try( var input = LxmlLoader.class.getResourceAsStream("lov.xsd") )
      {
         var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
         factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
         factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
         return factory.newSchema(new StreamSource(Objects.requireNonNull(input, "Не найден lov.xsd")));
      }
      catch( Exception ex ) { throw new ExceptionInInitializerError(ex); }
   }
}
