package ru.inversion.fx.app.frame.menu;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.IMnbItem;
import ru.inversion.fx.form.controls.JInvMenu;
import ru.inversion.fx.form.controls.JInvMenuBar;
import ru.inversion.fx.form.controls.JInvMenuItem;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author ssu @
 */
public class JInvMenuManager {

	private static final Logger log = LoggerFactory.getLogger(JInvMenuManager.class);

	/** */
	private JInvMenuManager() {
	}

	/** */
	private static class MenuAction implements EventHandler<ActionEvent> {

		private final IMenuItemData menuItemData;

		public MenuAction(IMenuItemData menuItemData) {
			this.menuItemData = menuItemData;
		}

		/** */
		public IMenuItemData getMenuItemData() {
			return menuItemData;
		}

		/** */
		@Override
		public void handle(ActionEvent event) {
			try {
				runMenuItem(this);
			} catch (Throwable ex) {
				JInvErrorService.handleException(null, ex);
			} // end
		}
	}

	private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

	/** */
	private static class MenuItemHelper {

		private IMenuItemData menuItemData;
		private List<MenuItemHelper> children;
		private final JInvMenuItem originalItem;

		public MenuItemHelper(IMenuItemData menuItemData) {
			this(menuItemData, null);
		}
		public MenuItemHelper(IMenuItemData menuItemData, JInvMenuItem originalItem) {
			this.menuItemData = menuItemData;
			this.originalItem = originalItem;
		}

		/** */
		public void setParent(MenuItemHelper parent) {
			if (parent != null) {
				parent.addChild(this);
			} // end if
		}

		/** */
		private void addChild(MenuItemHelper mih) {
			if (children == null) {
				children = new LinkedList<>();
			}
			children.add(mih);
		}

		/** */
		public IMenuItemData getMenuItemData() {
			return menuItemData;
		}

		/** Строит ветку меню по имеющейся информации */
		public <T extends MenuItem & IMnbItem> T toMenu()  {

			MenuItem mi = null;

			// Если это лист
			if (children == null && menuItemData.getParentID() != null)
			{
				mi = new JInvMenuItem( getMenuItemData().getName() );

				if( getOriginalItem() != null && getOriginalItem().getOnAction() != null ) {
					//если onAction уже есть, не перетираем его
					mi.setOnAction(getOriginalItem().getOnAction());
					log.debug("{} keeping existing onAction: {}", getOriginalItem().getMnbItem(), getOriginalItem().getOnAction());
				}
				else
					mi.setOnAction(new MenuAction(getMenuItemData()));

				mi.setId (
					getMenuItemData().getID() == -1 ? "imp" : getMenuItemData().getID().toString()
				);

				// Если это меню
			} else {

				JInvMenu menu = new JInvMenu( getMenuItemData().getName() );

				int lastGroupNum = -1, groupNum = 0;

				if( children != null )
				{
					for( MenuItemHelper m : children )
					{
						groupNum = m.getMenuItemData().getGroup();

						if( lastGroupNum != -1 )
						{
							if( lastGroupNum != groupNum )
								menu.getItems().add( new SeparatorMenuItem() );
						}

						lastGroupNum = groupNum;

						menu.getItems().add(m.toMenu());
					} // end for
				}

				mi = menu;
			}

			((IMnbItem) mi).setMnbItem(getMenuItemData().getMnbItem());

			return (T)mi;
		}

		public JInvMenuItem getOriginalItem() {
			return originalItem;
		}
	}

	/**
	 * Загрузка главного меню
	 */
	public static JInvMenuBar loadMainMenu(IMenuLoader menuLoader) {

		List<IMenuItemData> menuItemList = menuLoader.getMenuItemList();
		if (menuItemList == null) {
			menuItemList = new ArrayList<>();
		}
		if (menuItemList.isEmpty()) {
			String msg = fore.getString( "OSHIBKA_ZAGRUZKI_MENYU_SPISOK_EHLEMENTOV_PUST" );
			log.error( msg );
			Platform.runLater( () -> {
				Alerts.error( null, msg);
			} );
		}

		JInvMenuBar menuBar = menuLoader.getMenuBar().orElse(new JInvMenuBar());
		Map<String, JInvMenuItem> existingMnbItems = menuBar.getAllItems()
				.stream()
				.filter(item -> item instanceof JInvMenuItem)
				.map(item -> (JInvMenuItem) item)
				.collect(Collectors.toMap(JInvMenuItem::getMnbItem, item -> item));

		List<MenuItemHelper> rootList = new LinkedList<>();
		Map<Integer, MenuItemHelper> dataMap = new LinkedHashMap<>();

		MenuItemHelper mih = null;

		for (IMenuItemData mi : menuItemList) {
			if (existingMnbItems.containsKey(mi.getMnbItem())) {
				JInvMenuItem originalItem = existingMnbItems.get(mi.getMnbItem());
				mih = new MenuItemHelper(mi, originalItem);
			} else {
				mih = new MenuItemHelper(mi);
			}
			dataMap.put(mi.getID(), mih);
		} // end for

		Integer parentID = null;

		for (MenuItemHelper m : dataMap.values()) {
			parentID = m.getMenuItemData().getParentID();
			if (parentID != null && parentID != 0) {
				m.setParent(dataMap.get(parentID));
			} else {
				rootList.add(m);
			}
		} // end for

		//сносим существующее содержимое
		menuBar.getMenus().clear();

		for (MenuItemHelper m : rootList)
		{

			//log.debug("Next_(Menu/Menu_Item)= " + m.getMenuItemData().getName());
			final MenuItem m1 = m.toMenu();
			if( m1 instanceof Menu )
			{
				Menu m2 = (Menu)m1;
				if(!m2.getItems().isEmpty() )
					menuBar.getMenus().add( m2);
			}
		} // end for

		menuBar.setInitialized(true);
		log.info("initialized menuBar {}!", menuBar);

		return menuBar;
	}

	/** */
	private static void removeItems( Menu menu, Map<String, IMenuItemData> itemMap ) {

		menu.getItems().removeIf (
			mi -> mi instanceof MenuItem && mi instanceof IMnbItem && !itemMap.containsKey(((IMnbItem) mi).getMnbItem())
		);

		menu.getItems().stream().filter( mi->mi instanceof Menu).forEach( mi-> removeItems((Menu)mi,itemMap) );
	}

	/** */
	private static void removeEmpty( Menu menu ) {
		menu.getItems().removeIf(mi -> mi instanceof Menu && ((Menu) mi).getItems().isEmpty());
		menu.getItems().stream().filter(mi -> mi instanceof Menu ).forEach( mi->removeEmpty((Menu)mi) );
	}

	/** */
	public static void checkMenuBar(JInvMenuBar menuBar, IMenuLoader menuLoader) {

		final Map<String, IMenuItemData> itemMap = new HashMap<>();
		menuLoader.getMenuItemList().forEach((i) -> itemMap.put(i.getMnbItem(), i));

//		menuBar.getAllItems().removeIf(mi -> mi instanceof IMnbItem && !itemMap.containsKey(((IMnbItem) mi).getMnbItem()));
//		menuBar.getAllItems().removeIf(mi -> mi instanceof Menu && ((Menu) mi).getItems().isEmpty());

		menuBar.getMenus().forEach ( (mi)->removeItems( mi, itemMap ) );
		menuBar.getMenus().removeIf( (mi)->mi.getItems().isEmpty()	  );
		menuBar.getMenus().forEach ( (mi)->removeEmpty( mi ) );

		menuBar.getAllItems().forEach(new Consumer< MenuItem>() {
			@Override
			public void accept(MenuItem menuItem) {
				if (menuItem instanceof IMnbItem) {
					IMnbItem mnb = (IMnbItem) menuItem;
					final IMenuItemData itemData = itemMap.get(mnb.getMnbItem());
					if (itemData != null) {
						menuItem.setText(itemData.getName());
						if (!(menuItem instanceof Menu)) {
							if (menuItem.getOnAction() == null) {
								menuItem.setOnAction(new MenuAction(itemData));
							}
						}
					}
				}
			}
		});
		menuBar.setInitialized(true);
	}

/*

	public static TreeView loadTreeMainMenu(IMenuLoader menuLoader) {

		List<IMenuItemData> menuItemList = menuLoader.getMenuItemList();
		if (menuItemList == null) {
			menuItemList = new ArrayList<>();
		}
		if (menuItemList.isEmpty()) {
			String msg = fore.getString( "OSHIBKA_ZAGRUZKI_MENYU_SPISOK_EHLEMENTOV_PUST" );
			log.error( msg );
			Platform.runLater( () -> {
				Alerts.error( null, msg);
			} );
		}

		List<MenuItemHelper> rootList = new LinkedList<>();
		Map<Integer, MenuItemHelper> dataMap = new LinkedHashMap<>();

		MenuItemHelper mih;

		for (IMenuItemData mi : menuItemList) {
			mih = new MenuItemHelper(mi);
			dataMap.put(mi.getID(), mih);
		}

		Integer parentID;

		for (MenuItemHelper m : dataMap.values()) {
			parentID = m.getMenuItemData().getParentID();
			if (parentID != null && parentID != 0) {
				m.setParent(dataMap.get(parentID));
			} else {
				rootList.add(m);
			}
		}

		TreeItem<String> rootItem = new TreeItem("root");
		TreeView<String> treeViewMenu = new TreeView(rootItem);
		treeViewMenu.setShowRoot(false);

		for (MenuItemHelper m : rootList) {
			log.debug("Next_(Menu/Menu_Item)= " + m.getMenuItemData().getName());
			rootItem.getChildren().add(new TreeItem<>(m.getMenuItemData().getName()));
		}
		return treeViewMenu;
	}
*/

	public static void runMenuItem(MenuAction action) throws Exception {
		BaseApp.APP().runMenuItem(action.getMenuItemData());
	}
}
