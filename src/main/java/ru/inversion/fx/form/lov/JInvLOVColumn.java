package ru.inversion.fx.form.lov;

import javafx.beans.property.Property;
import ru.inversion.utils.U;

/**
 *
 * @author ssu @
 */
public class JInvLOVColumn {

	private String	
				columnName, 
				title;
	private Class	columnClass;
	private int	width;
	
	private Property<?> property;
	
	/** */
	public JInvLOVColumn( ) {
	}
	/** */
	public JInvLOVColumn( String columnName, Class columnClass, String title, int width ) {
		this( columnName, columnClass, title, width, null );
	}
	/** */
	public JInvLOVColumn( String columnName, Class columnClass, String title, int width, Property<?> property  ) {
		this.columnName  = columnName;
		this.title		 = title;
		this.width       = width;
		this.property	 = property;
		this.columnClass = columnClass;
	}
	/** */
	public String getColumnName() {
		return columnName;
	}
	/** */
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	/** */
	public Class getColumnClass() {
		return columnClass;
	}
	/** */
	public String getTitle() {
		return U.nvl( title, columnName );
	}
	/** */
	public void setTitle(String title) {
		this.title = title;
	}
	/** */
	public int getWidth() {
		return width;
	}
	/** */
	public void setWidth(int width) {
		this.width = width;
	}
	/** */
	public Property<?> getProperty( ) {
		return property;
	}
}
