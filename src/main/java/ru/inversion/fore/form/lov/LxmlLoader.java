package ru.inversion.fore.form.lov;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

/** Строгий интерпретатор LXML: наследование разрешается только через явно зарегистрированные ресурсы. */
public final class LxmlLoader
{
   private static final int MAX_BYTES = 1024 * 1024;
   private static final int MAX_TEMPLATE_DEPTH = 16;
   private static final Schema SCHEMA = schema();
   private final ResourceBundle bundle;
   private final Map<String, URL> templates = new HashMap<>();
   private final Map<String, LovDefinition.Appearance> visualAttributes = new HashMap<>();

   public LxmlLoader() { this(null); }
   /** Ссылки %ключ разрешаются только в заголовках; %% обозначает обычный знак %. */
   public LxmlLoader(ResourceBundle bundle) { this.bundle = bundle; }

   /** Регистрирует разрешённый ресурс для extends и property-class. XML не может открыть произвольный URL. */
   public LxmlLoader template(String name, URL resource)
   {
      LovDefinition.requireName(name, "template");
      templates.put(name, Objects.requireNonNull(resource, "resource"));
      return this;
   }

   public LxmlLoader registerTemplate(String name, URL resource) { return template(name, resource); }

   /** Синоним template для деклараций, использующих термин Forms Property Class. */
   public LxmlLoader propertyClass(String name, URL resource) { return template(name, resource); }

   public LxmlLoader registerPropertyClass(String name, URL resource) { return template(name, resource); }

   /** Именованный аналог Visual Attribute Group; локальные атрибуты appearance имеют приоритет. */
   public LxmlLoader visualAttribute(String name, LovDefinition.Appearance appearance)
   {
      LovDefinition.requireName(name, "visual-attribute");
      visualAttributes.put(name, Objects.requireNonNull(appearance));
      return this;
   }

   public ForeLov load(URL resource, LovSources sources) throws IOException
   {
      LovDefinition definition = read(resource);
      return new ForeLov(definition, sources.resolve(definition));
   }

   public LovDefinition read(URL resource) throws IOException
   {
      Objects.requireNonNull(resource, "Не найден ресурс LXML");
      try { return toDefinition(readResolved(resource, new HashSet<>(), 0)); }
      catch( IOException ex ) { throw ex; }
      catch( Exception ex ) { throw new IOException("Некорректный LXML: " + ex.getMessage(), ex); }
   }

   /** Поток принадлежит вызывающему коду. Вызов не запускает JavaFX. */
   public LovDefinition read(InputStream input) throws IOException
   {
      byte[] xml = input.readNBytes(MAX_BYTES + 1);
      if( xml.length > MAX_BYTES ) throw new IOException("LXML превышает 1 МиБ");
      try { return toDefinition(merge(null, partial(parse(new ByteArrayInputStream(xml)), false))); }
      catch( IOException ex ) { throw ex; }
      catch( Exception ex ) { throw new IOException("Некорректный LXML: " + ex.getMessage(), ex); }
   }

   private TemplatePart readResolved(URL resource, Set<String> stack, int depth) throws IOException
   {
      String identity = resource.toExternalForm();
      if( depth > MAX_TEMPLATE_DEPTH || !stack.add(identity) )
         throw new IOException("Циклическое или слишком глубокое наследование LXML: " + identity);
      try( InputStream input = resource.openStream() )
      {
         byte[] xml = input.readNBytes(MAX_BYTES + 1);
         if( xml.length > MAX_BYTES ) throw new IOException("LXML превышает 1 МиБ");
         return resolvePart(parse(new ByteArrayInputStream(xml)), stack, depth);
      }
      finally { stack.remove(identity); }
   }

   private LovDefinition resolve(Element root, Set<String> stack, int depth) throws IOException
   {
      return toDefinition(resolvePart(root, stack, depth));
   }

   private TemplatePart resolvePart(Element root, Set<String> stack, int depth) throws IOException
   {
      try
      {
         TemplatePart base = null;
         String parentName = optional(root, "extends");
         String className = optional(root, "property-class");
         if( parentName != null ) base = template(parentName, stack, depth);
         if( className != null ) base = merge(base, template(className, stack, depth).asPartial());
         return merge(base, partial(root, false));
      }
      catch( IOException ex ) { throw ex; }
      catch( Exception ex ) { throw new IOException("Некорректный LXML: " + ex.getMessage(), ex); }
   }

   private TemplatePart template(String name, Set<String> stack, int depth) throws IOException
   {
      URL resource = templates.get(name);
      if( resource == null ) throw new IOException("Не зарегистрирован шаблон LOV: " + name);
      return readResolved(resource, stack, depth + 1);
   }

   private Partial partial(Element root, boolean completeDefaults)
   {
      List<ColumnPart> columns = null;
      Element columnsElement = child(root, "columns");
      if( columnsElement != null )
      {
         columns = new ArrayList<>();
         for( Element column : children(columnsElement) )
            columns.add(new ColumnPart(column.getAttribute("name"),
                  has(column, "title") ? caption(column.getAttribute("title")) : completeDefaults ? column.getAttribute("name") : null,
                  has(column, "type") ? LovDefinition.ValueType.valueOf(column.getAttribute("type").toUpperCase(Locale.ROOT)) : completeDefaults ? LovDefinition.ValueType.STRING : null,
                  has(column, "width") ? number(column, "width") : completeDefaults ? 160 : Double.NaN,
                  has(column, "return-to") ? column.getAttribute("return-to") : completeDefaults ? "" : null,
                  has(column, "length") ? integer(column, "length", 0) : completeDefaults ? 0 : -1));
      }
      Element sourceElement = child(root, "source");
      return new Partial(root.getAttribute("id"), has(root, "title") ? caption(root.getAttribute("title")) : null,
            child(root, "comment") == null ? null : child(root, "comment").getTextContent(),
            partialWindow(child(root, "window")), partialBehavior(child(root, "behavior")),
            partialSearch(child(root, "search")), partialBinding(child(root, "binding")),
            partialAppearance(child(root, "appearance")), columns,
            sourceElement);
   }

   private TemplatePart merge(TemplatePart base, Partial local)
   {
      String id = local.id == null || local.id.isBlank() ? base == null ? null : base.id() : local.id;
      String title = local.title == null ? base == null ? null : base.title() : local.title;
      LovDefinition.Window window = mergeWindow(base == null ? null : base.window, local.window);
      LovDefinition.Behavior behavior = mergeBehavior(base == null ? null : base.behavior, local.behavior);
      LovDefinition.Search search = mergeSearch(base == null ? null : base.search, local.search);
      LovDefinition.Binding binding = mergeBinding(base == null ? null : base.binding, local.binding);
      LovDefinition.Appearance appearance = mergeAppearance(base == null ? null : base.appearance,
            local.appearance == null ? null : visualAttributes.get(local.appearance.visualAttribute()));
      appearance = mergeAppearance(appearance, local.appearance);
      List<LovDefinition.Column> columns = mergeColumns(base == null ? null : base.columns, local.columns);
      Element source = local.source == null ? base == null ? null : base.source : local.source;
      return new TemplatePart(id, title, local.comment == null ? base == null ? null : base.comment : local.comment,
            window, behavior, search, binding, appearance, columns, source, local);
   }

   private TemplatePart merge(TemplatePart base, TemplatePart overlay)
   {
      if( base == null ) return overlay;
      return merge(base, overlay.asPartial());
   }

   private LovDefinition toDefinition(TemplatePart part)
   {
      if( part == null || part.id == null || part.id.isBlank() || part.title == null || part.columns == null || part.source == null )
         throw new IllegalArgumentException("Для LOV обязательны id, title, columns и source");
      LovDefinition.Source source = source(part.source, part.columns);
      return new LovDefinition(part.id, part.title, part.comment == null ? "" : part.comment,
            part.window, part.behavior, part.search, part.columns, source, part.binding, part.appearance);
   }

   private static List<LovDefinition.Column> mergeColumns(List<LovDefinition.Column> base,
                                                            List<ColumnPart> local)
   {
      if( local == null ) return base;
      if( base == null ) return normalizeColumns(local);
      var merged = new LinkedHashMap<String, LovDefinition.Column>();
      base.forEach(column -> merged.put(column.name(), column));
      for( ColumnPart part : local )
      {
         LovDefinition.Column old = merged.get(part.name());
         if( old == null )
         {
            if( part.title == null || part.type == null || Double.isNaN(part.width) || part.length < 0 )
               throw new IllegalArgumentException("Новая наследуемая колонка должна иметь все свойства: " + part.name);
            merged.put(part.name, part.column());
         }
         else merged.put(part.name, new LovDefinition.Column(part.name,
               part.title == null ? old.title() : part.title, part.type == null ? old.type() : part.type,
               Double.isNaN(part.width) ? old.width() : part.width,
               part.returnTo == null ? old.returnTo() : part.returnTo,
               part.length < 0 ? old.length() : part.length));
      }
      return List.copyOf(merged.values());
   }

   private static List<LovDefinition.Column> normalizeColumns(List<ColumnPart> columns)
   {
      return columns.stream().map(column -> {
         return new LovDefinition.Column(column.name, column.title == null ? column.name : column.title,
               column.type == null ? LovDefinition.ValueType.STRING : column.type,
               Double.isNaN(column.width) ? 160 : column.width, column.returnTo == null ? "" : column.returnTo,
               column.length < 0 ? 0 : column.length);
      }).toList();
   }

   private static LovDefinition.Window mergeWindow(LovDefinition.Window old, WindowPart part)
   {
      if( part == null ) return old == null ? new LovDefinition.Window(640, 420, null, null, false, false,
            LovDefinition.Direction.INHERIT, "") : old;
      return new LovDefinition.Window(part.width == null ? old == null ? 640 : old.width() : part.width,
            part.height == null ? old == null ? 420 : old.height() : part.height,
            part.x == null ? old == null ? null : old.x() : part.x,
            part.y == null ? old == null ? null : old.y() : part.y,
            part.autoPosition == null ? old != null && old.automaticPosition() : part.autoPosition,
            part.autoColumnWidth == null ? old != null && old.automaticColumnWidth() : part.autoColumnWidth,
            part.direction == null ? old == null ? LovDefinition.Direction.INHERIT : old.direction() : part.direction,
            part.styleClass == null ? old == null ? "" : old.styleClass() : part.styleClass);
   }

   private static LovDefinition.Behavior mergeBehavior(LovDefinition.Behavior old, BehaviorPart part)
   {
      if( part == null ) return old == null ? new LovDefinition.Behavior(false, true, false, false, false, false, "F9") : old;
      return new LovDefinition.Behavior(part.autoDisplay == null ? old != null && old.autoDisplay() : part.autoDisplay,
            part.autoRefresh == null ? old == null || old.autoRefresh() : part.autoRefresh,
            part.autoSelect == null ? old != null && old.autoSelect() : part.autoSelect,
            part.autoSkip == null ? old != null && old.autoSkip() : part.autoSkip,
            part.filterBeforeDisplay == null ? old != null && old.filterBeforeDisplay() : part.filterBeforeDisplay,
            part.validateFromList == null ? old != null && old.validateFromList() : part.validateFromList,
            part.key == null ? old == null ? "F9" : old.key() : part.key);
   }

   private static LovDefinition.Search mergeSearch(LovDefinition.Search old, SearchPart part)
   {
      if( part == null ) return old == null ? new LovDefinition.Search(LovDefinition.MatchMode.PREFIX, false, 0, 200) : old;
      return new LovDefinition.Search(part.mode == null ? old == null ? LovDefinition.MatchMode.PREFIX : old.mode() : part.mode,
            part.caseSensitive == null ? old != null && old.caseSensitive() : part.caseSensitive,
            part.minLength == null ? old == null ? 0 : old.minLength() : part.minLength,
            part.maxRows == null ? old == null ? 200 : old.maxRows() : part.maxRows);
   }

   private static LovDefinition.Binding mergeBinding(LovDefinition.Binding old, BindingPart part)
   {
      if( part == null ) return old == null ? LovDefinition.Binding.defaults() : old;
      if( (part.x == null) != (part.y == null) )
         throw new IllegalArgumentException("Координаты binding x и y задаются парой");
      LovDefinition.Position position = part.x == null && part.y == null ? old == null ? null : old.position() :
            new LovDefinition.Position(part.x, part.y);
      return new LovDefinition.Binding(position, part.lovButton == null ? old != null && old.lovButton() : part.lovButton);
   }

   private static LovDefinition.Appearance mergeAppearance(LovDefinition.Appearance old,
                                                            LovDefinition.Appearance part)
   {
      if( part == null ) return old == null ? LovDefinition.Appearance.defaults() : old;
      return new LovDefinition.Appearance(part.visualAttribute().isEmpty() && old != null ? old.visualAttribute() : part.visualAttribute(),
            part.fontName().isEmpty() && old != null ? old.fontName() : part.fontName(),
            part.fontSize() == 0 && old != null ? old.fontSize() : part.fontSize(),
            part.fontWeight().isEmpty() && old != null ? old.fontWeight() : part.fontWeight(),
            part.fontStyle().isEmpty() && old != null ? old.fontStyle() : part.fontStyle(),
            part.foregroundColor().isEmpty() && old != null ? old.foregroundColor() : part.foregroundColor(),
            part.backgroundColor().isEmpty() && old != null ? old.backgroundColor() : part.backgroundColor(),
            part.rowLineColor().isEmpty() && old != null ? old.rowLineColor() : part.rowLineColor());
   }

   private LovDefinition.Source source(Element element, List<LovDefinition.Column> columns)
   {
      Element child = children(element).stream().findFirst().orElse(null);
      return switch( child == null ? "" : child.getLocalName() )
      {
         case "provider" -> new LovDefinition.ProviderSource(child.getAttribute("ref"));
         case "sql" -> new LovDefinition.SqlSource(child.getAttribute("ref"), child(child, "query").getTextContent().strip(),
               children(child).stream().filter(e -> e.getLocalName().equals("bind"))
                     .map(e -> e.getAttribute("name")).toList(), integer(child, "fetch-size", 100), integer(child, "timeout-seconds", 30));
         case "static" -> {
            if( columns == null ) throw new IllegalArgumentException("Статический источник требует columns");
            var rows = new ArrayList<LovRow>();
            var types = new LinkedHashMap<String, LovDefinition.Column>();
            columns.forEach(column -> types.put(column.name(), column));
            for( Element row : children(child) )
            {
               var values = new LinkedHashMap<String, Object>();
               for( Element value : children(row) )
               {
                  String name = value.getAttribute("column");
                  LovDefinition.Column column = types.get(name);
                  if( column == null || values.containsKey(name) ) throw new IllegalArgumentException("Неизвестная или повторная колонка строки: " + name);
                  boolean isNull = bool(value, "null", false);
                  if( isNull && !value.getTextContent().isBlank() ) throw new IllegalArgumentException("NULL не может иметь текст: " + name);
                  values.put(name, isNull ? null : column.convert(value.getTextContent()));
               }
               if( !values.keySet().equals(types.keySet()) ) throw new IllegalArgumentException("В строке должны присутствовать все объявленные колонки");
               rows.add(new LovRow(values));
            }
            yield new LovDefinition.StaticSource(rows);
         }
         default -> throw new IllegalArgumentException("Неизвестный источник: " + element.getTextContent());
      };
   }

   private WindowPart partialWindow(Element element)
   {
      if( element == null ) return null;
      return new WindowPart(optionalDouble(element, "width"), optionalDouble(element, "height"), coordinate(element, "x"), coordinate(element, "y"),
            optionalBool(element, "automatic-position"), optionalBool(element, "automatic-column-width"), element.hasAttribute("direction") ?
            LovDefinition.Direction.valueOf(element.getAttribute("direction").toUpperCase(Locale.ROOT).replace('-', '_')) : null, optional(element, "style-class"));
   }

   private BehaviorPart partialBehavior(Element element)
   {
      if( element == null ) return null;
      return new BehaviorPart(optionalBool(element, "auto-display"), optionalBool(element, "auto-refresh"), optionalBool(element, "auto-select"), optionalBool(element, "auto-skip"),
            optionalBool(element, "filter-before-display"), optionalBool(element, "validate-from-list"), optional(element, "key"));
   }

   private SearchPart partialSearch(Element element)
   {
      if( element == null ) return null;
      return new SearchPart(element.hasAttribute("mode") ? LovDefinition.MatchMode.valueOf(element.getAttribute("mode").toUpperCase(Locale.ROOT)) : null,
            optionalBool(element, "case-sensitive"), optionalInt(element, "min-length"), optionalInt(element, "max-rows"));
   }

   private BindingPart partialBinding(Element element)
   { return element == null ? null : new BindingPart(coordinate(element, "x"), coordinate(element, "y"), optionalBool(element, "lov-button")); }

   private LovDefinition.Appearance partialAppearance(Element element)
   {
      if( element == null ) return null;
      Double size = optionalDouble(element, "font-size");
      return new LovDefinition.Appearance(optional(element, "visual-attribute"), optional(element, "font-name"), size == null ? 0 : size,
            optional(element, "font-weight"), optional(element, "font-style"), optional(element, "foreground-color"), optional(element, "background-color"), optional(element, "row-line-color"));
   }

   private String caption(String value)
   {
      if( value.startsWith("%%") ) return value.substring(1);
      if( !value.startsWith("%") ) return value;
      if( bundle == null ) throw new IllegalArgumentException("Для заголовка " + value + " не задан ResourceBundle");
      return bundle.getString(value.substring(1));
   }

   private Element parse(InputStream input) throws IOException
   {
      try
      {
         var factory = DocumentBuilderFactory.newInstance();
         factory.setNamespaceAware(true); factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
         factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
         factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
         factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
         factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
         factory.setSchema(SCHEMA);
         var builder = factory.newDocumentBuilder();
         builder.setEntityResolver((publicId, systemId) -> { throw new SAXException("Внешние сущности запрещены"); });
         builder.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException ex) throws SAXException { throw ex; }
            @Override public void error(SAXParseException ex) throws SAXException { throw ex; }
            @Override public void fatalError(SAXParseException ex) throws SAXException { throw ex; }
         });
         Element root = builder.parse(input).getDocumentElement();
         if( !"lov".equals(root.getLocalName()) ) throw new IllegalArgumentException("Корневой элемент должен быть lov");
         return root;
      }
      catch( Exception ex ) { throw new IOException("Некорректный LXML: " + ex.getMessage(), ex); }
   }

   private static List<Element> children(Element element)
   {
      var result = new ArrayList<Element>();
      if( element != null ) for( var node = element.getFirstChild(); node != null; node = node.getNextSibling() ) if( node instanceof Element child ) result.add(child);
      return result;
   }
   private static Element child(Element element, String name) { return children(element).stream().filter(e -> name.equals(e.getLocalName())).findFirst().orElse(null); }
   private static boolean has(Element element, String name) { return element != null && element.hasAttribute(name); }
   private static String optional(Element element, String name) { return has(element, name) ? element.getAttribute(name) : null; }
   private static boolean bool(Element element, String name, boolean fallback) { return has(element, name) ? Boolean.parseBoolean(element.getAttribute(name)) : fallback; }
   private static Boolean optionalBool(Element element, String name) { return has(element, name) ? Boolean.valueOf(element.getAttribute(name)) : null; }
   private static double number(Element element, String name) { return Double.parseDouble(element.getAttribute(name)); }
   private static Double optionalDouble(Element element, String name) { return has(element, name) ? Double.valueOf(element.getAttribute(name)) : null; }
   private static int integer(Element element, String name, int fallback) { return has(element, name) ? Integer.parseInt(element.getAttribute(name)) : fallback; }
   private static Integer optionalInt(Element element, String name) { return has(element, name) ? Integer.valueOf(element.getAttribute(name)) : null; }
   private static Double coordinate(Element element, String name) { return optionalDouble(element, name); }

   private static Schema schema()
   {
      try( var input = LxmlLoader.class.getResourceAsStream("lov.xsd") )
      {
         var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
         factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
         return factory.newSchema(new StreamSource(Objects.requireNonNull(input, "Не найден lov.xsd")));
      }
      catch( Exception ex ) { throw new ExceptionInInitializerError(ex); }
   }

   private record TemplatePart(String id, String title, String comment, LovDefinition.Window window,
                               LovDefinition.Behavior behavior, LovDefinition.Search search, LovDefinition.Binding binding,
                               LovDefinition.Appearance appearance, List<LovDefinition.Column> columns, Element source,
                               Partial declared)
   {
      private Partial asPartial() { return declared; }
   }
   private record Partial(String id, String title, String comment, WindowPart window, BehaviorPart behavior, SearchPart search,
                          BindingPart binding, LovDefinition.Appearance appearance, List<ColumnPart> columns, Element source) { }
   private record ColumnPart(String name, String title, LovDefinition.ValueType type, double width, String returnTo, int length)
   {
      private LovDefinition.Column column() { return new LovDefinition.Column(name, title, type, width, returnTo, length); }
   }
   private record WindowPart(Double width, Double height, Double x, Double y, Boolean autoPosition, Boolean autoColumnWidth,
                             LovDefinition.Direction direction, String styleClass) { }
   private record BehaviorPart(Boolean autoDisplay, Boolean autoRefresh, Boolean autoSelect, Boolean autoSkip,
                               Boolean filterBeforeDisplay, Boolean validateFromList, String key) { }
   private record SearchPart(LovDefinition.MatchMode mode, Boolean caseSensitive, Integer minLength, Integer maxRows) { }
   private record BindingPart(Double x, Double y, Boolean lovButton) { }
}
