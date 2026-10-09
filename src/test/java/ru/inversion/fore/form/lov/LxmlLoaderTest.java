package ru.inversion.fore.form.lov;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LxmlLoaderTest
{
   @org.junit.jupiter.api.Test
   void columnLengthUsesUnicodeCharactersAndNewBindingProperties() throws Exception
   {
      String xml = LovFixtures.xml("", "", """
            <static><row><value column="ID">1</value><value column="CODE">😀</value><value column="NAME">Имя</value></row></static>
            """).replace("name=\"CODE\" title=\"Код\"", "name=\"CODE\" title=\"Код\" length=\"1\"")
            .replace("<columns>", "<binding x=\"11\" y=\"22\" lov-button=\"true\"/><appearance row-line-color=\"#aabbcc\"/><columns>");
      var definition = LovFixtures.read(xml);
      assertEquals(1, definition.columns().get(1).length());
      assertEquals(new LovDefinition.Position(11, 22), definition.binding().position());
      assertTrue(definition.binding().lovButton());
      assertEquals("#aabbcc", definition.appearance().rowLineColor());
   }

   @org.junit.jupiter.api.Test
   void inheritedAndOverriddenPropertiesAreResolvedFromRegisteredResource(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception
   {
      Path base = directory.resolve("base.lxml");
      Files.writeString(base, LovFixtures.xml("auto-refresh=\"false\"", "max-rows=\"17\"", "<provider ref=\"test\"/>"));
      Path child = directory.resolve("child.lxml");
      Files.writeString(child, """
            <lov xmlns="urn:inversion:fore:lov:1" version="1" id="child" extends="base" title="Переопределённый">
               <behavior auto-select="true"/>
               <columns><column name="CODE" width="240"/></columns>
            </lov>
            """);
      var loader = new LxmlLoader().template("base", base.toUri().toURL());
      var definition = loader.read(child.toUri().toURL());
      assertEquals("Переопределённый", definition.title());
      assertFalse(definition.behavior().autoRefresh());
      assertTrue(definition.behavior().autoSelect());
      assertEquals(17, definition.search().maxRows());
      assertEquals(240, definition.columns().stream().filter(c -> c.name().equals("CODE")).findFirst().orElseThrow().width());
      assertEquals(3, definition.columns().size());
   }

   @org.junit.jupiter.api.Test
   void unregisteredInheritanceIsRejectedWithoutOpeningArbitraryResource() throws Exception
   {
      String xml = """
            <lov xmlns="urn:inversion:fore:lov:1" version="1" id="child" extends="not-registered" title="X"/>
            """;
      assertThrows(IOException.class, () -> LovFixtures.read(xml));
   }

   @org.junit.jupiter.api.Test
   void propertyClassMayContainOnlyCommonBehavior(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception
   {
      Path property = directory.resolve("property.lxml");
      Files.writeString(property, """
            <lov xmlns="urn:inversion:fore:lov:1" version="1" id="common">
               <behavior auto-refresh="false"/>
            </lov>
            """);
      Path lov = directory.resolve("local.lxml");
      Files.writeString(lov, LovFixtures.xml("", "", "<provider ref=\"test\"/>")
            .replace("id=\"test\"", "id=\"local\" property-class=\"common\""));
      var definition = new LxmlLoader().propertyClass("common", property.toUri().toURL()).read(lov.toUri().toURL());
      assertFalse(definition.behavior().autoRefresh());
   }

   @org.junit.jupiter.api.Test
   void propertyClassDoesNotEraseInheritedWindow(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception
   {
      Path base = directory.resolve("base.lxml");
      Files.writeString(base, LovFixtures.xml("", "", "<provider ref=\"test\"/>")
            .replace("<behavior", "<window width=\"900\" height=\"600\"/><behavior"));
      Path property = directory.resolve("property.lxml");
      Files.writeString(property, """
            <lov xmlns="urn:inversion:fore:lov:1" version="1" id="common">
               <behavior auto-refresh="false"/>
            </lov>
            """);
      Path child = directory.resolve("child.lxml");
      Files.writeString(child, LovFixtures.xml("", "", "<provider ref=\"test\"/>")
            .replace("id=\"test\"", "id=\"child\" extends=\"base\" property-class=\"common\""));
      var definition = new LxmlLoader().template("base", base.toUri().toURL())
            .propertyClass("common", property.toUri().toURL()).read(child.toUri().toURL());
      assertEquals(900, definition.window().width());
      assertFalse(definition.behavior().autoRefresh());
   }

   @Test void defaultsAndHiddenReturnColumn() throws Exception
   {
      var definition = LovFixtures.definition("", "");
      assertEquals("CODE", definition.searchColumn().name());
      assertTrue(definition.behavior().autoRefresh());
      assertFalse(definition.behavior().autoSelect());
      assertEquals(200, definition.search().maxRows());
      assertEquals(7L, LovFixtures.row(7, "A").returns(definition).get("id"));
      assertThrows(UnsupportedOperationException.class, () -> definition.columns().clear());
   }

   @Test void shippedExamplesAreValid() throws Exception
   {
      for( String name : new String[]{"departments.lxml", "departments-sql.lxml"} )
         assertNotNull(new LxmlLoader().read(getClass().getResource("/ru/inversion/fore/demo/lov/" + name)));
   }

   @Test void staticValuesPreserveNullAndExactNumericTypes() throws Exception
   {
      var definition = LovFixtures.read(LovFixtures.xml("", "", """
            <static><row><value column="ID">9007199254740993</value>
            <value column="CODE">A</value><value column="NAME" null="true"/></row></static>
            """));
      var row = ((LovDefinition.StaticSource) definition.source()).rows().getFirst();
      assertEquals(9007199254740993L, row.get("ID"));
      assertNull(row.get("NAME"));
      assertThrows(IllegalArgumentException.class, () -> row.get("UNKNOWN"));
      assertThrows(UnsupportedOperationException.class, () -> row.values().clear());
      assertEquals(new BigDecimal("1.20"), LovDefinition.ValueType.DECIMAL.convert("1.20"));
      assertThrows(ArithmeticException.class, () -> LovDefinition.ValueType.LONG.convert("1.5"));
   }

   @Test void explicitBundleResolvesOnlyCaptions() throws Exception
   {
      var bundle = new ListResourceBundle() {
         @Override protected Object[][] getContents() { return new Object[][]{{"caption", "Подразделения"}}; }
      };
      String xml = LovFixtures.xml("", "", "<provider ref=\"test\"/>").replace("title=\"Справочник\"", "title=\"%caption\"");
      var definition = new LxmlLoader(bundle).read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
      assertEquals("Подразделения", definition.title());
      assertThrows(IOException.class, () -> LovFixtures.read(xml));
   }

   static Stream<String> invalidDocuments()
   {
      String base = LovFixtures.xml("", "", "<provider ref=\"test\"/>");
      return Stream.of(
            base.replace("id=\"test\"", "id=\"test\" typo=\"true\""),
            base.replace("version=\"1\"", "version=\"2\""),
            base.replace("urn:inversion:fore:lov:1", "urn:wrong"),
            base.replace("<behavior", "<unknown/><behavior"),
            base.replace("<behavior ", "<behavior auto-select=\"maybe\" "),
            base.replace("<behavior ", "<behavior validate-from-list=\"true\" ").replace("return-to=\"code\"", ""),
            base.replace("name=\"NAME\"", "name=\"CODE\""),
            base.replace("return-to=\"name\"", "return-to=\"code\""),
            base.replace("title=\"Код\"", "title=\"Код\" width=\"0\"").replace("title=\"Название\"", "title=\"Название\" width=\"0\""),
            base.replace("width=\"0\"", "width=\"-1\""),
            base.replace("<behavior", "<window x=\"1\"/><behavior"),
            base.replace("<search ", "<search max-rows=\"0\" "),
            base.replace("<search ", "<search max-rows=\"10001\" "),
            base.replace("<source>", "<source><provider ref=\"extra\"/>"),
            base.replace("<provider ref=\"test\"/>", "<static><row><value column=\"UNKNOWN\">A</value></row></static>"),
            base.replace("<provider ref=\"test\"/>", "<static><row><value column=\"ID\">1.5</value></row></static>"),
            base.replace("<provider ref=\"test\"/>", "<static><row><value column=\"ID\" null=\"true\">1</value></row></static>")
      );
   }

   @ParameterizedTest @MethodSource("invalidDocuments")
   void malformedOrInconsistentDefinitionFailsBeforeUse(String xml)
   {
      assertThrows(IOException.class, () -> LovFixtures.read(xml));
   }

   @Test void externalEntitiesAndOversizedDocumentsAreRejected()
   {
      String xml = "<!DOCTYPE lov [<!ENTITY xxe SYSTEM 'file:///not-to-read'>]>" +
            LovFixtures.xml("", "", "<provider ref=\"test\"/>").replace("Справочник", "&xxe;");
      assertThrows(IOException.class, () -> LovFixtures.read(xml));
      assertThrows(IOException.class, () -> LovFixtures.read(" ".repeat(1024 * 1024 + 1)));
   }

   @Test void registryRejectsUnknownOrDuplicateProvider() throws Exception
   {
      var sources = new LovSources();
      var definition = LovFixtures.definition("", "");
      assertThrows(IllegalArgumentException.class, () -> sources.resolve(definition));
      LovDataSource provider = request -> new LovDataSource.Result(java.util.List.of(), true);
      assertSame(provider, sources.provider("test", provider).resolve(definition));
      assertThrows(IllegalArgumentException.class, () -> sources.provider("test", provider));
      assertEquals(Map.of("id", 1L, "code", "A", "name", "Имя A"), LovFixtures.row(1, "A").returns(definition));
   }
}
