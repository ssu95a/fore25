package ru.inversion.fore.form.lov;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class LovFixtures
{
   static String xml(String behavior, String search, String source)
   {
      return """
            <lov xmlns="urn:inversion:fore:lov:1" version="1" id="test" title="Справочник">
               <behavior %s/>
               <search %s/>
               <columns>
                  <column name="ID" type="long" width="0" return-to="id"/>
                  <column name="CODE" title="Код" return-to="code"/>
                  <column name="NAME" title="Название" return-to="name"/>
               </columns>
               <source>%s</source>
            </lov>
            """.formatted(behavior, search, source);
   }

   static LovDefinition definition(String behavior, String search) throws Exception
   {
      return read(xml(behavior, search, "<provider ref=\"test\"/>"));
   }

   static LovDefinition read(String xml) throws Exception
   {
      return new LxmlLoader().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
   }

   static LovRow row(long id, String code)
   {
      return new LovRow(Map.of("ID", id, "CODE", code, "NAME", "Имя " + code));
   }

   static LovDataSource rows(LovRow... values)
   {
      return request -> {
         List<LovRow> found = List.of(values).stream().filter(request::matches).limit(request.maxRows() + 1L).toList();
         return new LovDataSource.Result(found.subList(0, Math.min(found.size(), request.maxRows())), found.size() <= request.maxRows());
      };
   }
}
