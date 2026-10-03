package ru.inversion.fore.form.action;

import javafx.scene.text.Font;

import java.net.URL;

/**
 * Vector fonts already bundled with Fore25. No Oracle Forms compatible icons.
 * Fonts are loaded lazily so merely opening a class in Scene Builder does not
 * initialize every font family.
 */
public enum IconFont
{
   FONT_AWESOME_4          ("/style/fontawesome-webfont.ttf"),
   FONT_AWESOME_SOLID      ("/style/fa-solid-900.ttf"),
   FONT_AWESOME_REGULAR    ("/style/fa-regular-400.ttf"),
   ENTYPO                 ("/style/entypo.ttf"),
   IONICONS               ("/style/ionicons.ttf"),
   MATERIAL_DESIGN_ICONS  ("/style/materialdesignicons-webfont.ttf");

   private static final double ICON_SIZE = 16;

   private final String resourcePath;
   private Font loadedFont;

   IconFont(String resourcePath)
   {
      this.resourcePath = resourcePath;
   }

   String resourcePath()
   {
      return resourcePath;
   }

   /** Cache the actual font face: Solid and Regular may share a family name. */
   Font font()
   {
      if( loadedFont != null )
         return loadedFont;

      final URL url = IconFont.class.getResource(resourcePath);
      if( url == null )
         throw new IllegalStateException("Bundled icon font not found: " + resourcePath);

      final Font font = Font.loadFont(url.toExternalForm(), ICON_SIZE);
      if( font == null )
         throw new IllegalStateException("Unable to load icon font: " + resourcePath);

      loadedFont = font;
      return font;
   }
}
