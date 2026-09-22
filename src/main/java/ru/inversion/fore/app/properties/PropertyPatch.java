package ru.inversion.fore.app.properties;

import java.util.Map;

public record PropertyPatch( Map<String, Object> values)
{
   public PropertyPatch { values = Map.copyOf(values); }
}