/*
 Storybook: Open Source software for novelists and authors.
 Copyright (C) 2008 - 2012 Martin Mustun

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package storybook.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import org.hibernate.Session;
import org.miginfocom.swing.MigLayout;

import net.infonode.docking.DockingWindow;
import net.infonode.docking.DockingWindowAdapter;
import net.infonode.docking.RootWindow;
import net.infonode.docking.SplitWindow;
import net.infonode.docking.TabWindow;
import net.infonode.docking.View;
import net.infonode.docking.ViewSerializer;
import net.infonode.docking.properties.RootWindowProperties;
import net.infonode.docking.theme.DockingWindowsTheme;
import net.infonode.docking.theme.ShapedGradientDockingTheme;
import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.MixedViewHandler;
import net.infonode.docking.util.StringViewMap;
import net.infonode.util.Direction;

import org.apache.commons.io.FileUtils;

import storybook.SbApp;
import storybook.SbConstants;
import storybook.SbConstants.BookKey;
import storybook.SbConstants.Storybook;
import storybook.SbConstants.ViewName;
import storybook.SbPref;
import storybook.action.ActionHandler;
import storybook.action.SbActionManager;
import storybook.controller.BookController;
import storybook.exim.exporter.BookExporter;
import storybook.exim.exporter.ExportDlg;
import storybook.exim.exporter.ExportOptionsDlg;
import storybook.exim.exporter.TableExporter;
import storybook.exim.importer.ImportDlg;
import storybook.i18n.I18N;
import storybook.model.BlankModel;
import storybook.model.BookModel;
import storybook.model.DbFile;
import storybook.model.hbn.dao.PartDAOImpl;
import storybook.model.hbn.dao.SceneDAOImpl;
import storybook.model.hbn.entity.AbstractEntity;
import storybook.model.hbn.entity.Chapter;
import storybook.model.hbn.entity.Internal;
import storybook.model.hbn.entity.Part;
import storybook.model.hbn.entity.Scene;
import storybook.toolkit.BookUtil;
import storybook.toolkit.DockingWindowUtil;
import storybook.toolkit.EnvUtil;
import storybook.toolkit.SpellCheckerUtil;
import storybook.toolkit.swing.FontUtil;
import storybook.toolkit.swing.SwingUtil;
import storybook.ui.dialog.ChaptersOrderDlg;
import storybook.ui.dialog.ChooseFileDlg;
import storybook.ui.dialog.PropertiesDlg;
import storybook.ui.dialog.UnicodeDlg;
import storybook.ui.dialog.edit.EntityEditor;
import storybook.ui.interfaces.IPaintable;
import storybook.ui.panel.AbstractPanel;
import storybook.ui.panel.BlankPanel;
import storybook.ui.panel.typist.TypistPanel;

/**
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements IPaintable {

	private BookModel bookModel;
	private BookController bookController;
	private SbActionManager sbActionManager;
	private ViewFactory viewFactory;
	private JToolBar mainToolBar;
	private RootWindow rootWindow;
	private StatusBarPanel statusBar;
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private final HashMap<Integer, JComponent> dynamicViews = new HashMap<>();
	private DbFile dbFile;
	private Part currentPart;
	private boolean EditorModless;
	private UnicodeDlg unicodeDialog;
	private Chapter lastChapter;
	private boolean updated = false;
	public boolean showAllParts=false;
	private TypistPanel typistPanel;
	private JMenuBar mainMenuBar;

	public MainFrame() {
		FontUtil.setDefaultFont(new Font("Arial", Font.PLAIN, 12));
	}

	@Override
	public void init() {
		SbApp.trace("MainFrame.init()");
		dbFile = null;
		viewFactory = new ViewFactory(this);
		sbActionManager = new SbActionManager(this);
		sbActionManager.init();
		bookController = new BookController(this);
		BlankModel model = new BlankModel(this);
		bookController.attachModel(model);
		setIconImage(I18N.getIconImage("icon.sb"));
		addWindowListener(new MainFrameWindowAdaptor());
	}

	public void init(DbFile dbF) {
		SbApp.trace("MainFrame.init(" + dbF.getDbName() + ")");
		try {
			this.dbFile = dbF;
			viewFactory = new ViewFactory(this);
			viewFactory.setInitialisation();
			sbActionManager = new SbActionManager(this);
			sbActionManager.init();
			// model and controller
			bookController = new BookController(this);
			bookModel = new BookModel(this);
			if (!dbF.getDbName().isEmpty()) {
				bookModel.initSession(dbF.getDbName());
			}
			bookController.attachModel(bookModel);
			// listener
			addWindowListener(new MainFrameWindowAdaptor());
			// spell checker
			SpellCheckerUtil.registerDictionaries();
			viewFactory.resetInitialisation();
		} catch (Exception e) {
			SbApp.error("MainFrame.init(" + dbF.getName() + ")", e);
		}
	}

	@Override
	public void initUi() {
		SbApp.trace(">>> MainFrame.initUi()");
		setLayout(new MigLayout("flowy,fill,ins 0,gap 0", "", "[grow]"));
		setIconImage(I18N.getIconImage("icon.sb"));
		setTitle();
		restoreDimension();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		SbApp.getInstance().resetUiFont();
		sbActionManager.reloadMenuToolbar();
		initRootWindow();
		setDefaultLayout();
		add(rootWindow, "grow");
		statusBar = new StatusBarPanel(this);
		add(statusBar, "growx");
		bookController.attachView(statusBar);
		SbApp.trace("pack();");
		pack();
		setVisible(true);
		initAfterPack();
		JMenuBar menubar = getJMenuBar();
		bookController.detachView(menubar);
		bookController.attachView(menubar);
		// load last used layout
		DockingWindowUtil.loadLayout(this, BookKey.LAST_USED_LAYOUT.toString());
		// restore last used part
		try {
			Internal internal = BookUtil.get(this, BookKey.LAST_USED_PART.toString(), 1);
			Part part;
			if (internal != null && internal.getIntegerValue() != null) {
				Session session = bookModel.beginTransaction();
				PartDAOImpl dao = new PartDAOImpl(session);
				part = dao.find(internal.getIntegerValue());
				bookModel.commit();
				if (part == null) {
					part = getCurrentPart();
				}
			} else {
				part = getCurrentPart();
			}
			sbActionManager.getActionHandler().handleChangePart(part);
		} catch (Exception e) {
			SbApp.trace("exiting try in MainFrame.initUi()");
		}
		//		bookController.attachView(this);
		SwingUtilities.invokeLater(() -> {
			setTypist();
		});
		SbApp.trace("<<< MainFrame.initUi()");
	}
	
	public void setTypist() {
		if (dbFile.isOK()) {
			if (BookUtil.getBoolean(this,BookKey.TYPIST_USE, false)) {
				activateTypist();
			}
		}
	}
	
	private Scene lastUsedScene=null;
	public void setLastUsedScene(Scene s) {
		lastUsedScene=s;
	}
	
	private boolean isTypist=false;
	public void activateTypist() {
		SbApp.trace("activateTipist=>"+(isTypist?"true":"false"));
		if (isTypist) {
			remove(typistPanel);
			setJMenuBar(mainMenuBar);
			add(mainToolBar);
			add(rootWindow, "grow");
			add(statusBar, "growx");
			isTypist=false;
		} else {
			typistPanel=new TypistPanel(this,lastUsedScene);
			mainMenuBar=this.getJMenuBar();
			setJMenuBar(null);
			remove(mainToolBar);
			remove(rootWindow);
			remove(statusBar);
			add(typistPanel, "grow");
			isTypist=true;
			this.setState(Frame.MAXIMIZED_BOTH);
		}
		pack();
	}

	public void setTitle() {
		SbApp.trace("MainFrame.setTitle()");
		String prodFullTitle = Storybook.PRODUCT_FULL_NAME.toString();
		if (dbFile != null) {
			Part part = getCurrentPart();
			String partName = "";
			if (part != null) {
				partName = part.getNumberName();
			}
			String title = dbFile.getName();
			Internal internal = BookUtil.get(this, BookKey.TITLE, "");
			if (internal != null && !internal.getStringValue().isEmpty()) {
				title = internal.getStringValue();
			}
			if (isUpdated()) {
				title = "*" + title;
			}
			setTitle(title + " [" + I18N.getMsg("part") + " " + partName + "]" + " - " + prodFullTitle);
		} else {
			setTitle(prodFullTitle);
		}
		sbActionManager.getMainMenu().fileSave.setEnabled(updated);
	}

	private void initRootWindow() {
		SbApp.trace("MainFrame.initRootWindow()");
		StringViewMap viewMap = viewFactory.getViewMap();
		MixedViewHandler handler = new MixedViewHandler(viewMap, new ViewSerializer() {
			@Override
			public void writeView(View view, ObjectOutputStream out) throws IOException {
				out.writeInt(((DynamicView) view).getId());
			}

			@Override
			public View readView(ObjectInputStream in) throws IOException {
				return getDynamicView(in.readInt());
			}
		});
		rootWindow = DockingUtil.createRootWindow(viewMap, handler, true);
		rootWindow.setName("rootWindow");
		rootWindow.setPreferredSize(new Dimension(4096, 2048));
		// set theme
		DockingWindowsTheme currentTheme = new ShapedGradientDockingTheme();
		RootWindowProperties properties = new RootWindowProperties();
		properties.addSuperObject(currentTheme.getRootWindowProperties());
		// Our properties object is the super object of the root window
		// properties object, so all property values of the
		// theme and in our property object will be used by the root window
		rootWindow.getRootWindowProperties().addSuperObject(properties);
		rootWindow.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	}

	public void setDefaultLayout() {
		SbApp.trace("MainFrame.setDefaultLayout()");
		SbView scenesView = getView(ViewName.SCENES);
		SbView chaptersView = getView(ViewName.CHAPTERS);
		SbView partsView = getView(ViewName.PARTS);
		SbView locationsView = getView(ViewName.LOCATIONS);
		SbView personsView = getView(ViewName.PERSONS);
		SbView relationshipView = getView(ViewName.RELATIONSHIPS);
		SbView gendersView = getView(ViewName.GENDERS);
		SbView speciesView = getView(ViewName.SPECIES);
		SbView categoriesView = getView(ViewName.CATEGORIES);
		SbView attributesView = getView(ViewName.ATTRIBUTES);
		SbView attributesListView = getView(ViewName.ATTRIBUTESLIST);
		SbView strandsView = getView(ViewName.STRANDS);
		SbView ideasView = getView(ViewName.IDEAS);
		SbView tagsView = getView(ViewName.TAGS);
		SbView memosView = getView(ViewName.MEMOS);
		SbView itemsView = getView(ViewName.ITEMS);
		SbView tagLinksView = getView(ViewName.TAGLINKS);
		SbView itemLinksView = getView(ViewName.ITEMLINKS);
		SbView internalsView = getView(ViewName.INTERNALS);
		SbView chronoView = getView(ViewName.CHRONO);
		SbView bookView = getView(ViewName.BOOK);
		SbView manageView = getView(ViewName.MANAGE);
		SbView storyboard = getView(ViewName.STORYBOARD);
		SbView readingView = getView(ViewName.READING);
		SbView memoriaView = getView(ViewName.MEMORIA);
		SbView chartPersonsByDate = getView(ViewName.CHART_PERSONS_BY_DATE);
		SbView chartPersonsByScene = getView(ViewName.CHART_PERSONS_BY_SCENE);
		SbView chartWiWW = getView(ViewName.CHART_WiWW);
		SbView chartStrandsByDate = getView(ViewName.CHART_STRANDS_BY_DATE);
		SbView chartOccurrenceOfPersons = getView(ViewName.CHART_OCCURRENCE_OF_PERSONS);
		SbView chartOccurrenceOfLocations = getView(ViewName.CHART_OCCURRENCE_OF_LOCATIONS);
		SbView chartOccurrenceOfItems = getView(ViewName.CHART_OCCURRENCE_OF_ITEMS);
		SbView chartGantt = getView(ViewName.CHART_GANTT);
		SbView treeView = getView(ViewName.TREE);
		SbView infoView = getView(ViewName.INFO);
		SbView navigationView = getView(ViewName.NAVIGATION);
		SbView planView = getView(ViewName.PLAN);
		SbView timeEventView = getView(ViewName.TIMEEVENT);
		TabWindow tabInfoNavi = new TabWindow(new SbView[]{infoView, navigationView});
		tabInfoNavi.setName("tabInfoNaviWindow");
		SplitWindow swTreeInfo = new SplitWindow(false, 0.6f, treeView, tabInfoNavi);
		swTreeInfo.setName("swTreeInfo");
		TabWindow tabWindow = new TabWindow(new SbView[]{chronoView,
			bookView, manageView, readingView, memoriaView, scenesView,
			personsView, relationshipView, locationsView, chaptersView, gendersView, speciesView,
			categoriesView, partsView, strandsView, ideasView, tagsView,
			itemsView, tagLinksView, itemLinksView, storyboard,
			internalsView, attributesView, attributesListView,
			chartPersonsByDate, chartPersonsByScene, chartWiWW,
			chartStrandsByDate, chartOccurrenceOfPersons, chartOccurrenceOfItems,
			chartOccurrenceOfLocations, chartGantt, planView, timeEventView});
		tabWindow.setName("tabWindow");
		SplitWindow swTabWinMemo = new SplitWindow(true, 0.60f, tabWindow, memosView);
		swTabWinMemo.setName("swTabWinMemos");
		SplitWindow swMain = new SplitWindow(true, 0.20f, swTreeInfo, swTabWinMemo);
		//SplitWindow swMain = new SplitWindow(true, 0.20f, swTreeInfo, tabWindow);
		swMain.setName("swMain");
		rootWindow.setWindow(swMain);
		bookView.close();
		manageView.close();
		readingView.close();
		memoriaView.close();
		chaptersView.close();
		partsView.close();
		personsView.close();
		relationshipView.close();
		gendersView.close();
		speciesView.close();
		categoriesView.close();
		attributesView.close();
		attributesListView.close();
		strandsView.close();
		ideasView.close();
		tagsView.close();
		tagLinksView.close();
		itemsView.close();
		itemLinksView.close();
		internalsView.close();
		storyboard.close();
		chartPersonsByDate.close();
		chartPersonsByScene.close();
		chartWiWW.close();
		chartStrandsByDate.close();
		chartOccurrenceOfPersons.close();
		chartOccurrenceOfItems.close();
		chartOccurrenceOfLocations.close();
		chartGantt.close();
		planView.close();
		timeEventView.close();
		memosView.close();
		infoView.restoreFocus();
		chronoView.restoreFocus();
		rootWindow.getWindowBar(Direction.RIGHT).setEnabled(true);
		DockingWindowUtil.setRespectMinimumSize(this);
		SbApp.trace("end of MainFrame.setDefaultLayout()");
	}

	private void initAfterPack() {
		unicodeDialog = new UnicodeDlg(this);
		SbView scenesView = getView(ViewName.SCENES);
		SbView locationsView = getView(ViewName.LOCATIONS);
		SbView personsView = getView(ViewName.PERSONS);
		SbView chronoView = getView(ViewName.CHRONO);
		SbView treeView = getView(ViewName.TREE);
		SbView quickInfoView = getView(ViewName.INFO);
		SbView navigationView = getView(ViewName.NAVIGATION);
		// add docking window adapter to all views (except editor)
		MainDockingWindowAdapter dockingAdapter = new MainDockingWindowAdapter();
		for (int i = 0; i < viewFactory.getViewMap().getViewCount(); ++i) {
			View view = viewFactory.getViewMap().getViewAtIndex(i);
			/*if (view.getName().equals(ViewName.EDITOR.toString())) {
			 continue;
			 }*/
			view.addListener(dockingAdapter);
		}
		// load initially shown views here
		SbView[] views2 = {scenesView, personsView, locationsView, chronoView, treeView, quickInfoView, navigationView};
		for (SbView view : views2) {
			viewFactory.loadView(view);
			bookController.attachView(view.getComponent());
			bookModel.fireAgain(view);
		}
		quickInfoView.restoreFocus();
		chronoView.restoreFocus();
	}
	
	public SbView getView(String viewName) {
		return viewFactory.getView(viewName);
	}

	public SbView getView(ViewName viewName) {
		return viewFactory.getView(viewName);
	}

	public void showView(ViewName viewName) {
		SbApp.trace("MainFrame.showView(" + viewName.name() + ")");
//		if (viewName.equals(SbConstants.ViewName.EDITOR)) {
//			return;
//		}
		setWaitingCursor();
		SbView view = getView(viewName);
		if (view.getRootWindow() != null) {
			view.restoreFocus();
		} else {
			SbApp.trace(">>> RootWindow=null");
			DockingUtil.addWindow(view, rootWindow);
		}
		view.requestFocusInWindow();
		DockingWindowUtil.setRespectMinimumSize(this);
		setDefaultCursor();
		/*if (viewName.equals(SbConstants.ViewName.EDITOR)) {
		 showEditor();
		 }*/
	}

	public void showAndFocus(ViewName viewName) {
		SbApp.trace("MainFrame.showAndFocus(" + viewName.name() + ")");
		View view = getView(viewName);
		view.restore();
		view.restoreFocus();
	}

	public void closeView(ViewName viewName) {
		SbApp.trace("MainFrame.closeView(" + viewName.name() + ")");
		SbView view = getView(viewName);
		view.close();
	}

	public void refresh() {
		setWaitingCursor();
		for (int i = 0; i < viewFactory.getViewMap().getViewCount(); ++i) {
			SbView view = (SbView) viewFactory.getViewMap().getViewAtIndex(i);
			getBookController().refresh(view);
		}
		setDefaultCursor();
	}

	public void refreshStatusBar() {
		statusBar.refresh();
	}

	// refresh tiles of views
	public void refreshViews() {
		SbApp.trace("MainFrame.refreshViews()");
		for (int i = 0; i < viewFactory.getViewMap().getViewCount(); ++i) {
			SbView view = (SbView) viewFactory.getViewMap().getViewAtIndex(i);
			viewFactory.setViewTitle(view);
		}
	}

	public void showEditor() {
		SbApp.trace("MainFrame.showEditor()");
		/*SwingUtilities.invokeLater(new Runnable() {
		 @Override
		 public void run() {
		 SbApp.trace("MainFrame.showEditor()-->run");
		 SbView editorView = getView(ViewName.EDITOR);
		 editorView.cleverRestoreFocus();
		 }
		 });*/
		SbApp.trace("no MainFrame.showEditor()");
	}

	public void initBlankUi() {
		dbFile = null;
		setTitle(Storybook.PRODUCT_FULL_NAME.toString());
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		setLocation(screenSize.width / 2 - 450, screenSize.height / 2 - 320);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		SbApp.getInstance().resetUiFont();
		sbActionManager.reloadMenuToolbar();
		BlankPanel blankPanel = new BlankPanel(this);
		blankPanel.initAll();
		add(blankPanel);
		pack();
		setVisible(true);
	}

	public void setDefaultCursor() {
		SwingUtil.setDefaultCursor(this);
	}

	public void setWaitingCursor() {
		SwingUtil.setWaitingCursor(this);
	}

	public DbFile getDbFile() {
		return dbFile;
	}

	public boolean isBlank() {
		return dbFile == null;
	}

	public BookController getBookController() {
		return bookController;
	}

	public BookModel getBookModel() {
		return bookModel;
	}
	
	public Session getSession() {
		return(bookModel.getSession());
	}

	public RootWindow getRootWindow() {
		return rootWindow;
	}

	public SbActionManager getSbActionManager() {
		return sbActionManager;
	}

	public ActionHandler getActionController() {
		return sbActionManager.getActionController();
	}

	public ViewFactory getViewFactory() {
		return viewFactory;
	}

	private MainFrame getThis() {
		return this;
	}

	public boolean isMaximized() {
		return (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	public void setMaximized() {
		setExtendedState(Frame.MAXIMIZED_BOTH);
	}

	public void close(boolean exitIfEmpty) {
		SbApp.trace("MainFrame.close()");
		if (!isBlank()) {
			if (isTypist && typistPanel.askModified() != JOptionPane.YES_OPTION) {
				return;
			}
			/*if (isUpdated()) {
				//check if data are modified
				int n = JOptionPane.showConfirmDialog(getThis(),
						I18N.getMsg("close.confirm"),
						I18N.getMsg("close"),
						JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.NO_OPTION || n == JOptionPane.CLOSED_OPTION) {
					return;
				}
			}*/
			if (getPref().getBoolean(SbPref.Key.CONFIRM_EXIT, true)) {
				int n = JOptionPane.showConfirmDialog(getThis(),
					I18N.getMsg("ask.close"),
					I18N.getMsg("close"),
					JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.NO_OPTION || n == JOptionPane.CLOSED_OPTION) {
					return;
				}
			}
			// save dimension, location, maximized
			Dimension dim = getSize();
			getPref().setInteger(SbPref.Key.SIZE_WIDTH, dim.width);
			getPref().setInteger(SbPref.Key.SIZE_HEIGHT, dim.height);
			getPref().setInteger(SbPref.Key.POS_X, getLocationOnScreen().x);
			getPref().setInteger(SbPref.Key.POS_Y, getLocationOnScreen().y);
			getPref().setBoolean(SbPref.Key.MAXIMIZED, isMaximized());
			String s = "North";
			if (mainToolBar.getOrientation() == 0) {
				if (mainToolBar.getY() != 0) {
					s = "South";
				}
			} else if (mainToolBar.getX() != 0) {
				s = "East";
			} else {
				s = "West";
			}
			this.getPref().setString(SbPref.Key.TOOLBAR_ORIENTATION, s);
			// save layout
			DockingWindowUtil.saveLayout(this, SbConstants.BookKey.LAST_USED_LAYOUT.toString());
			// save last used part
			BookUtil.store(this, BookKey.LAST_USED_PART.toString(), (Integer) ((int) (long) getCurrentPart().getId()));
			viewFactory.saveAllTableDesign();
		}

		if (bookModel != null) {
			bookModel.closeSession();
		}
		SbApp app = SbApp.getInstance();
		app.removeMainFrame(this);
		dispose();
		if (app.getMainFrames().isEmpty()) {
			if (exitIfEmpty) {
				app.exit();
			} else { //re-create blank
				MainFrame mainFrame = new MainFrame();
				mainFrame.init();
				mainFrame.initBlankUi();
				app.addMainFrame(mainFrame);
			}
		}
	}

	private View getDynamicView(int id) {
		View view = (View) dynamicViews.get(id);
		if (view == null) {
			view = new DynamicView("Dynamic View " + id, null, createDummyViewComponent("Dynamic View " + id), id);
		}
		return view;
	}

	private static JComponent createDummyViewComponent(String text) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < 100; j++) {
			sb.append(text).append(". This is line ").append(j).append("\n");
		}
		return new JScrollPane(new JTextArea(sb.toString()));
	}

	private void restoreDimension() {
		int w = getPref().getInteger(SbPref.Key.SIZE_WIDTH, SbConstants.DEFAULT_SIZE_WIDTH);
		int h = getPref().getInteger(SbPref.Key.SIZE_HEIGHT, SbConstants.DEFAULT_SIZE_HEIGHT);
		setPreferredSize(new Dimension(w, h));
		int x = getPref().getInteger(SbPref.Key.POS_X, SbConstants.DEFAULT_POS_X);
		int y = getPref().getInteger(SbPref.Key.POS_Y, SbConstants.DEFAULT_POS_Y);

		// Get screens rectangle
		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (GraphicsDevice device : gs) {
			virtualBounds.add(device.getDefaultConfiguration().getBounds());
		}

		// Do not put frame out of screens
		x = (int) Math.max(virtualBounds.getMinX(), Math.min(virtualBounds.getMaxX() - w, x));
		y = (int) Math.max(virtualBounds.getMinY(), Math.min(virtualBounds.getMaxY() - h, y));
		setLocation(x, y);

		boolean maximized = getPref().getBoolean(SbPref.Key.MAXIMIZED, false);
		if (maximized) {
			setMaximized();
		}
	}

	public void updateStat() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public void saveAllTableDesign() {
		//viewFactory.saveAllTableDesign();
	}

	public SbPref getPref() {
		return (SbApp.getInstance().getPref());
	}

	public void setLastChapter(Chapter c) {
		lastChapter = c;
	}

	public Chapter getLastChapter() {
		return (lastChapter);
	}

	public String getLastImageDir() {
		Internal i = BookUtil.get(this, BookKey.LASTIMAGEDIR);
		return ((i.getStringValue() != null) ? i.getStringValue() : "");
	}

	public void setLastImageDir(File fc) {
		BookUtil.store(this, BookKey.LASTIMAGEDIR, fc.getAbsolutePath());
	}

	public void changeTitle(String newTitle) {
		SbApp.trace("MainFrame.changeTitle(" + newTitle + ")");
		BookUtil.store(this, BookKey.TITLE, newTitle);
		setTitle();
	}

	public void changePath(String oldPath, String newPath) {
		SbApp.trace("MainFrame.changePath(" + oldPath + ", " + newPath + ")");
		if (oldPath.equals(newPath)) {
			return;
		}
		BookModel model = this.getBookModel();
		Session session = model.beginTransaction();
		SceneDAOImpl SceneDAO = new SceneDAOImpl(session);
		List<Scene> scenes = SceneDAO.findAll();
		for (Scene scene : scenes) {
			String text = scene.getSummary();
			if (scene.getSummary().contains(oldPath)) {
				text = text.replace(oldPath, newPath);
				scene.setSummary(text);
				this.getBookController().updateScene(scene);
			}
		}
		model.commit();
	}

	public void setUpdated(boolean b) {
		updated = b;
		setTitle();
	}

	public boolean isUpdated() {
		return (updated);
	}

	private static class DynamicView extends View {

		private final int id;

		DynamicView(String title, Icon icon, Component component, int id) {
			super(title, icon, component);
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

	private class MainFrameWindowAdaptor extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent evt) {
			close(true);
		}
	}

	private class MainDockingWindowAdapter extends DockingWindowAdapter {

		@Override
		@SuppressWarnings("null")
		public void windowAdded(DockingWindow addedToWindow, DockingWindow addedWindow) {
			SbApp.trace("MainDockingWindowAdapter.windowAdded(" + addedToWindow.getName() + ", " + addedWindow.getName() + ")");
			if (addedWindow != null && addedWindow instanceof SbView) {
				SbView view = (SbView) addedWindow;
				if (!view.isLoaded()) {
					viewFactory.loadView(view);
					bookController.attachView(view.getComponent());
					bookModel.fireAgain(view);
				}
			}
		}

		@Override
		@SuppressWarnings("null")
		public void windowClosed(DockingWindow window) {
			SbApp.trace("MainDockingWindowAdapter.windowClosed(" + window.getName() + ")");
			if (window != null && window instanceof SbView) {
				SbView view = (SbView) window;
				if (!view.isLoaded()) {
					return;
				}
				bookController.detachView((AbstractPanel) view.getComponent());
				viewFactory.unloadView(view);
			}
		}
	}

	public Part getCurrentPart() {
		try {
			Session session = bookModel.beginTransaction();
			if (currentPart == null) {
				PartDAOImpl dao = new PartDAOImpl(session);
				currentPart = dao.findFirst();
			} else {
				session.refresh(currentPart);
			}
			bookModel.commit();
			return currentPart;
		} catch (NullPointerException e) {
		}
		return null;
	}

	public void setCurrentPart(Part currentPart) {
		if (currentPart != null) {
			this.currentPart = currentPart;
		}
	}

	public boolean hasCurrentPart() {
		return currentPart != null;
	}

	public void setMainToolBar(JToolBar toolBar) {
		if (mainToolBar != null) {
			//SwingUtil.unfloatToolBar(mainToolBar);
			getContentPane().remove(mainToolBar);
		}
		this.mainToolBar = toolBar;
		String bl = this.getPref().getString(SbPref.Key.TOOLBAR_ORIENTATION, "North");
		if ("East".equals(bl) || "West".equals(bl)) {
			mainToolBar.setOrientation(1);
		}
		getContentPane().add(mainToolBar, bl);
	}

	public JToolBar getMainToolBar() {
		return mainToolBar;
	}

	public void showEditorAsDialog(AbstractEntity entity) {
		JDialog dlg = new JDialog((Frame) this, true);
		EditorModless = BookUtil.isEditorModless(this);
		if (EditorModless) {
			dlg.setModalityType(Dialog.ModalityType.MODELESS);
			dlg.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		}
		EntityEditor editor = new EntityEditor(this, entity, dlg);
		dlg.setTitle(I18N.getMsg("editor"));
		Dimension pos = SwingUtil.getDlgPosition(dlg, entity);
		if (pos != null) {
			dlg.setLocation(pos.height, pos.width);
		} else {
			dlg.setSize(this.getWidth() / 2, 680);
		}
		dlg.add(editor);
		Dimension size = SwingUtil.getDlgSize(dlg, entity);
		if (size != null) {
			dlg.setSize(size.height, size.width);
		} else {
			dlg.setLocationRelativeTo(this);
		}
		//if (!EditorModless) dlg.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		dlg.setVisible(true);
		if (!EditorModless) {
			while (editor.hasEntityChanged()) {
				if (JOptionPane.showConfirmDialog(this,
					I18N.getMsg("close.confirm"),
					I18N.getMsg("edit"),
					JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
					break;
				}
				dlg.setVisible(true);
			}
		}
		SwingUtil.saveDlgPosition(dlg, entity);
	}

	public void showUnicodeDialog() {
		unicodeDialog.show();
	}
	
	public void fileNewAction(){
		SbApp.getInstance().createNewFile();
	}
	
	public void fileOpenAction() {
		setWaitingCursor();
		SbApp.getInstance().openFile();
		setDefaultCursor();
	}

	public void fileSaveAction() {
		//nothing the db is saved allways
		setUpdated(false);
	}

	public void fileSaveAsAction() {
		ChooseFileDlg dlg = new ChooseFileDlg(this, false);
		dlg.setForceDbExtension(false);
		dlg.setDefaultDBExt(this.getDbFile().getExt());
		dlg.setDefaultPath(this.getDbFile().getPath());
		dlg.setVisible(true);
		if (dlg.isCanceled()) {
			return;
		}
		File outFile = dlg.getFile();
		File inFile = this.getDbFile().getFile();
		String oldPath = this.getDbFile().getPath();
		String oldTitle = BookUtil.getTitle(this);
		this.close(false);
		try {
			FileUtils.copyFile(inFile, outFile);
		} catch (IOException ioe) {
			System.err.println("ActionHandler.handleSaveAs() IOex : " + ioe.getMessage());
		}
		DbFile newDB = new DbFile(outFile);
		String newPath = newDB.getPath();
		SbApp.getInstance().openFile(newDB, oldPath, newPath);
	}

	public void fileRenameAction() {
		ChooseFileDlg dlg = new ChooseFileDlg(this, false);
		dlg.setForceDbExtension(false);
		dlg.setDefaultDBExt(this.getDbFile().getExt());
		dlg.setVisible(true);
		if (dlg.isCanceled()) {
			return;
		}
		File outFile = dlg.getFile();
		SbApp.getInstance().renameFile(this, outFile);
	}
	
	public void filePropertiesAction() {
		PropertiesDlg dlg = new PropertiesDlg(this);
		dlg.setVisible(true);
	}

	public void fileImportAction() {
		ImportDlg.show(this);
	}

	public void newfileExportAction() {
		ExportDlg.show(this);
	}

	public void fileExportBookAction() {
		BookExporter.toFile(this, "html");
	}

	public void fileExportXmlAction() {
		TableExporter.exportDB(this);
	}

	public void fileExportOtherAction() {
		ExportDlg.show(this);
	}
	
	public void fileExportOptionsAction() {
		ExportOptionsDlg.show(this);
	}

	public void newEntity(AbstractEntity entity) {
		SbApp.trace("MainMenu.newEntity(" + entity.getClass().getName() + ")");
		showEditorAsDialog(entity);
	}
	
	public void windowSaveLayoutAction() {
		String name;
		while (true) {
			name = JOptionPane.showInputDialog(this,
				I18N.getColonMsg("enter.name"),
				I18N.getMsg("docking.save.layout"),
				JOptionPane.PLAIN_MESSAGE);
			File f = new File(EnvUtil.getPrefDir().getAbsolutePath()
				+ File.separator + name + ".layout");
			if (!f.exists()) {
				break;
			}
			//signaler que le layout existe déjà et demander le remplacement, ou si changer de nom, ou abandonner
			int ret = JOptionPane.showConfirmDialog(this,
				I18N.getMsg("warning") + ":" + I18N.getMsg("docking.save.layout.exists"),
				I18N.getMsg("docking.layout"),
				JOptionPane.YES_NO_CANCEL_OPTION);
			if (ret == JOptionPane.OK_OPTION) {
				break;
			}
			if (ret == JOptionPane.CANCEL_OPTION) {
				return;
			}
			// if NO_OPTION then continue in while
		}
		if (name != null) {
			DockingWindowUtil.saveLayout(this, name);
		}
	}
	
	public void chaptersOrderAction() {
		SwingUtil.showModalDialog(new ChaptersOrderDlg(this), this);
	}
	
}
