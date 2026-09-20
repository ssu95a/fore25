package ru.inversion.fx.app.property;

import java.util.ResourceBundle;

/**
 *
 * @author ssu @
 */
public class PropertyNotFoundException extends PropertyException {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
	// bundle ??
	private static String params2Msg( PropertiesTypeEnum propertiesType, String propertyName, String note ) {
		StringBuilder sb = new StringBuilder();
		
		if( note != null && note.length() > 0 ) {
			sb.append(note);
			if( note.charAt(note.length()-1) == '.' )
				sb.append(' ');
			else
				sb.append(". ");
		}
		else
			sb.append(fore.getString("NE_NAJDENO_SVOJSTVO"));
			
		sb.append("property: "	).append( propertyName	);
		sb.append(", type: "	).append( propertiesType);
		sb.append(", techInfo: ");
		
		switch( propertiesType ) {
			case PRP:
				sb.append(fore.getString("NE_USTANOVLENO_V_FAJLE_APP_PROPERTIES_ARGS_IN_MAIN_FUCTION_SYSTEM_SETPROPERTY"));
			break;
			case SMR:
				sb.append(fore.getString("SMR_PUSTAYA_ILI_V_NEJ_NET_POLYA")).append(propertiesType);
			break;
			case DB_USER:
				sb.append("PREF.Get_Preference('").append(propertyName).append("') == null");
			break;
			case DB_GLOBAL:
				sb.append("PREF.Get_Global_Preference('").append(propertyName).append("') == null");
			break;
		}
		sb.append('.');
		return sb.toString();
	}
	/** */
	public PropertyNotFoundException( PropertiesTypeEnum propertiesType, String propertyName, String note ) {
		super( params2Msg( propertiesType, propertyName, note ) );
	}
	
}
