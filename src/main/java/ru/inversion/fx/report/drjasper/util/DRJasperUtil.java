package ru.inversion.fx.report.drjasper.util;

import javafx.scene.text.*;
import net.sf.dynamicreports.report.base.style.*;
import net.sf.dynamicreports.report.builder.style.*;
import net.sf.dynamicreports.report.builder.style.FontBuilder;
import org.slf4j.*;

import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static ru.inversion.util.TU.format;

/**
 * <p>Description: </p>
 * Date: 26.02.2021
 * Time: 12:20
 * User:  opl
 */
public class DRJasperUtil {

	protected static Logger log = LoggerFactory.getLogger( DRJasperUtil.class );
	
  /* ********************* Class Body *****************************/
  public static FontBuilder buildDefFontBuilder() {
  	FXRepUtil.dummy();
  		FontBuilder defaultFont = stl.font()
//				.setFontName("DejaVu Serif");
//				.setFontName("DejaVu Sans");
				.setFontName("DejaVu Sans Mono")
				.setFontSize(10);
  		StyleBuilder boldStyle = stl.style()
  			.bold();
  		StyleBuilder italicStyle = stl.style()
  			.italic();
  		StyleBuilder boldItalicStyle = stl.style()
  			.boldItalic();
  		log.debug( format("defaultFont= {0}", defaultFont ) );
  		return defaultFont;
  	}

	public static Font getDefaultDRJasperFont(FontBuilder fontBuilder) {
   DRFont defDRFont = fontBuilder.getFont();
   log.debug( format("defDRFont= {0}; FontName= {1}; FontSize= {2}", defDRFont, defDRFont.getFontName(), defDRFont.getFontSize() ) );
   Font font = Font.font(defDRFont.getFontName(), FontWeight.NORMAL, defDRFont.getFontSize() );
   log.debug( format("Default DR Font_Name= {0}, size= {1}", font.getName(), font.getSize()) );
   return font;
 }



  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/


}
