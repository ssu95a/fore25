package ru.inversion.fx.help.store;

import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.utils.Checks;
import ru.inversion.utils.U;
import ru.inversion.utils.dco.Dco;
import ru.inversion.utils.dco.DcoXmlWriter;
import ru.inversion.utils.dco.IDco;
import ru.inversion.xxi.invoformat.InvFmtWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class HelpSaver {

   public enum Format {
      XML,
      INV_UTF8_BOM;

      @Override
      public String toString() {

         switch(this) {
            case INV_UTF8_BOM:
               return "Invo формат UTF-8, with BOM";
            case XML:
               return "Xml";
         }
         throw new NoSuchElementException();
      }
   }

   final static private byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

   public static Writer newUtf8BomWriter(Path path) throws IOException {
      final OutputStream out = Files.newOutputStream(path);
      out.write(bom);
      return new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
   }

   /** */
   public long save( Path fileTo, Format format, Iterable<PHelp> iter, Consumer<String> listener) throws IOException {

      if( listener == null)
          listener = s -> { };

      switch (format) {
         case INV_UTF8_BOM:
            return saveToUtf8WithBom(fileTo, iter, listener);
         case XML:
            return saveToXml(fileTo, iter, listener);
      }

      throw new NoSuchElementException("Формат '" + format + "' не поддерживается для вывода help текстов");
   }

   /** */
   private static IDco toDco( PHelp h )
   {
      Checks.Require.object( h, "h");

      IDco item = new Dco("help");
      item.e( "form"   ).set  ( h.getFORM()     );
      item.e( "descr"  ).set  ( h.getDESCR()    );
      item.e( "locale" ).set  ( h.getCLOCALE()  );
      item.e( "version").set  ( h.getVER()      );
      item.e( "text"   ).cdata( h.getHTML_TEXT());

      return item;
   }

   /** */
   private long saveToXml( Path fileTo, Iterable<PHelp> iter, Consumer<String> listener )
   {
      try( final DcoXmlWriter xmlWriter = DcoXmlWriter.open( fileTo, "helps", StandardCharsets.UTF_8 ) ) {
           StreamSupport.stream( iter.spliterator(), false).map( new Function<PHelp, IDco>() {
              @Override
              public IDco apply( PHelp h ) {
                 listener.accept( U.nvl( h.getDESCR(), h.getFORM() ) );
                 return toDco(h);
              }
           })
           .forEach( xmlWriter );

           return xmlWriter.count();
      }
   }

/*
# Help Begin
LOCALE ru
DESCR ПАК КЦ. Ввод подвала заявки
FORM ru.inversion.fxku1.yzal.EditYzalController
TEXT
/* * /
/* * /
VERSION 6.05
# Help End
*/

   /** */
   private long saveToUtf8WithBom( Path fileTo, Iterable<PHelp> iter, Consumer<String> listener ) throws IOException {

      long nCount = 0L;

      try( InvFmtWriter w = InvFmtWriter.createInvFmtWriter( newUtf8BomWriter(fileTo) ) )
      {
           for( PHelp h : iter )
           {
               if( h == null )
                   continue;

               w.writeBeginElement("Help");
                  w.writeAttributeElement( "FORM",    h.getFORM()     );
                  w.writeAttributeElement( "DESCR",   h.getDESCR()    );
                  w.writeAttributeElement( "LOCALE",  h.getCLOCALE()  );
                  w.writeAttributeElement( "VERSION", h.getVER()      );
                  w.writeAttributeElement( "TEXT",    h.getHTML_TEXT());
               w.writeEndElement( );

               listener.accept( U.nvl( h.getDESCR(), h.getFORM() ) );

              nCount ++;

           }//end for
      }
      return nCount;
   }

}
