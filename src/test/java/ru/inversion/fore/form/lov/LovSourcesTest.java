package ru.inversion.fore.form.lov;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LovSourcesTest
{
   private JdbcConnectionPool database;

   @BeforeEach void createDatabase() throws Exception
   {
      database = JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
      database.setMaxConnections(1);
      try( var connection = database.getConnection(); var statement = connection.createStatement() )
      {
         statement.execute("create table departments(id bigint, code varchar(40), name varchar(100), company_id bigint)");
         statement.execute("insert into departments values (1,'A','Альфа',10),(2,'AB',null,10),(3,'B','Бета',20),(4,'A%_!','Спецсимволы',10)");
      }
   }

   @AfterEach void releaseDatabase() throws Exception
   {
      assertEquals(0, database.getActiveConnections(), "Соединение не возвращено в пул");
      try( var connection = database.getConnection(); var statement = connection.createStatement() )
      { statement.execute("shutdown"); }
      database.dispose();
   }

   private LovDefinition definition(int limit) throws Exception
   {
      return LovFixtures.read(LovFixtures.xml("", "max-rows=\"" + limit + "\"", """
            <sql ref="main" fetch-size="1" timeout-seconds="2">
               <query>select id as ID, code as CODE, name as NAME from departments
                  where company_id = ? and upper(code) like ? escape '!' order by id</query>
               <bind name="company"/><bind name="$pattern"/>
            </sql>
            """));
   }

   private LovDataSource.Result fetch(LovDefinition definition, String text, LovDefinition.MatchMode mode) throws Exception
   {
      return new LovSources().database("main", database).resolve(definition).fetch(
            new LovDataSource.Request(definition, text, mode, Map.of("company", 10L)));
   }

   @Test void boundedFetchDistinguishesTruncationFromExactlyFullResult() throws Exception
   {
      var limited = fetch(definition(2), "A", LovDefinition.MatchMode.PREFIX);
      assertEquals(List.of(1L, 2L), limited.rows().stream().map(row -> row.get("ID")).toList());
      assertFalse(limited.complete());
      assertNull(limited.rows().get(1).get("NAME"));
      var full = fetch(definition(3), "A", LovDefinition.MatchMode.PREFIX);
      assertEquals(3, full.rows().size());
      assertTrue(full.complete());
      assertThrows(UnsupportedOperationException.class, () -> full.rows().clear());
   }

   @Test void parametersUsePreparedStatementAndWildcardsRemainLiteral() throws Exception
   {
      var definition = definition(10);
      assertEquals(4L, fetch(definition, "a%_!", LovDefinition.MatchMode.EXACT).rows().getFirst().get("ID"));
      assertTrue(fetch(definition, "' OR 1=1 --", LovDefinition.MatchMode.PREFIX).rows().isEmpty());
      assertTrue(fetch(definition, "B", LovDefinition.MatchMode.EXACT).rows().isEmpty());
      assertEquals(2L, fetch(definition, "b", LovDefinition.MatchMode.CONTAINS).rows().getFirst().get("ID"));
   }

   @Test void missingParameterAndSqlErrorStillCloseConnection() throws Exception
   {
      var definition = definition(10);
      var source = new LovSources().database("main", database).resolve(definition);
      assertThrows(IllegalArgumentException.class, () -> source.fetch(
            new LovDataSource.Request(definition, "", LovDefinition.MatchMode.PREFIX, Map.of())));
      assertEquals(0, database.getActiveConnections());
      try( var connection = database.getConnection(); var statement = connection.createStatement() )
      { statement.execute("drop table departments"); }
      assertThrows(java.sql.SQLException.class, () -> fetch(definition, "", LovDefinition.MatchMode.PREFIX));
   }

   @Test void staticSourceUsesSameSearchAndLimitContract() throws Exception
   {
      var base = definition(1);
      var definition = new LovDefinition(base.id(), base.title(), base.comment(), base.window(), base.behavior(),
            base.search(), base.columns(), new LovDefinition.StaticSource(List.of(LovFixtures.row(1, "A"), LovFixtures.row(2, "AB"))));
      var request = new LovDataSource.Request(definition, "a", LovDefinition.MatchMode.PREFIX, Map.of());
      var loaded = new LovSources().resolve(definition).fetch(request);
      assertEquals(1, loaded.rows().size());
      assertFalse(loaded.complete());
   }
}
