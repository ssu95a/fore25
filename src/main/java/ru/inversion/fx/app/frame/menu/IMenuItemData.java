package ru.inversion.fx.app.frame.menu;

/**
 *
 * @author ssu @
 */
public interface IMenuItemData {

	/** ID menu */
	Integer getID( );
	
	/** Наименование пункта */
	String getName( );
	
	/** Номер группы куда входит пункт */
	Integer getGroup( );
	
	/** Порядок в группе */
	Integer getOrder( );
	
	/** Всплывающая подсказка */
	String getTooltip();
	
	/** Java класс */
	String getJavaClass();
	
	/** статический метод вызываемы из класса */
	String getJavaMethod();

	/** Параметры передаваемые в метод
	 *	пока notImpl
	 */
	String getParameters( );
	
	/** код родителя*/
	Integer getParentID( );

	/** Пункт меню MNB*/
	String getMnbItem( );

}
