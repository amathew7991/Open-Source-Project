/*
 * Created on Aug 18, 2006
 */
package shef.ui.text;

import java.lang.ref.WeakReference;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import shef.ui.ShefUtils;
import storybook.i18n.I18N;

/**
 * Manages an application-wide popup menu for JTextComponents. Any JTextComponent registered with the manager will have
 * a right-click invokable popup menu, which provides options to undo, redo, cut, copy, paste, and select-all. The popup
 * manager is a singleton and must be retrieved with the getInstance() method:
 *
 * <pre><code>
 * JTextField textField = new JTextField(20);
 * TextEditPopupManager.getInstance().registerJTextComponent(textField);
 * </code></pre>
 *
 * @author Bob Tantlinger
 */
public class TextEditPopupManager {

	private static TextEditPopupManager singleton = null;

	public static final String CUT = "cut";
	public static final String COPY = "copy";
	public static final String PASTE = "paste";
	public static final String SELECT_ALL = "selectAll";
	public static final String UNDO = "undo";
	public static final String REDO = "redo";

	private final HashMap actions = new HashMap();

	// The actions we add to the popup menu
	private final Action cut = new DefaultEditorKit.CutAction();
	private final Action copy = new DefaultEditorKit.CopyAction();
	private final Action paste = new DefaultEditorKit.PasteAction();
	private final Action selectAll = new NSelectAllAction();
	private final Action undo = new UndoAction();
	private final Action redo = new RedoAction();

	// maintains a list of the currently registered JTextComponents
	private final List textComps = new Vector();
	private final List undoers = new Vector();

	private JTextComponent focusedComp;// the registered JTextComponent that is
	// focused
	private UndoManager undoer; // The undomanager for the focused
	// JTextComponent

	// Listeners for the JTextComponents
	private final FocusListener focusHandler = new PopupFocusHandler();
	private final MouseListener popupHandler = new PopupHandler();
	private final UndoListener undoHandler = new UndoListener();
	private final CaretListener caretHandler = new CaretHandler();
	private final JPopupMenu popup = new JPopupMenu();// The one and only popup menu

	@SuppressWarnings("unchecked")
	private TextEditPopupManager() {
		cut.putValue(Action.NAME, I18N.getMsg("shef.cut"));
		cut.putValue(Action.SMALL_ICON, ShefUtils.getIconX16("cut"));
		copy.putValue(Action.NAME, I18N.getMsg("shef.copy"));
		copy.putValue(Action.SMALL_ICON, ShefUtils.getIconX16("copy"));
		paste.putValue(Action.NAME, I18N.getMsg("shef.paste"));
		paste.putValue(Action.SMALL_ICON, ShefUtils.getIconX16("paste"));
		selectAll.putValue(Action.ACCELERATOR_KEY, null);

		popup.add(undo);
		popup.add(redo);
		popup.addSeparator();
		popup.add(cut);
		popup.add(copy);
		popup.add(paste);
		popup.addSeparator();
		popup.add(selectAll);

		actions.put(CUT, cut);
		actions.put(COPY, copy);
		actions.put(PASTE, paste);
		actions.put(SELECT_ALL, selectAll);
		actions.put(UNDO, undo);
		actions.put(REDO, redo);

	}

	public static TextEditPopupManager getInstance() {
		if (singleton == null) {
			singleton = new TextEditPopupManager();
		}
		return singleton;
	}

	public Action getAction(String name) {
		return (Action) actions.get(name);
	}

	public void registerJTextComponent(JTextComponent tc) throws IllegalArgumentException {
		registerJTextComponent(tc, new UndoManager());
	}

	@SuppressWarnings("unchecked")
	public void registerJTextComponent(JTextComponent tc, UndoManager um) throws IllegalArgumentException {
		if (tc == null || um == null) {
			throw new IllegalArgumentException("null arguments aren't allowed");
		}

		if (getIndexOfJTextComponent(tc) != -1) {
			throw new IllegalArgumentException("Component already registered");
		}

		tc.addFocusListener(focusHandler);
		tc.addCaretListener(caretHandler);
		tc.addMouseListener(popupHandler);
		tc.getDocument().addUndoableEditListener(undoHandler);

		textComps.add(new WeakReference(tc));
		undoers.add(um);
	}

	public void unregisterJTextComponent(JTextComponent tc) {
		int index = getIndexOfJTextComponent(tc);
		if (index != -1) {
			tc.removeFocusListener(focusHandler);
			tc.removeCaretListener(caretHandler);
			tc.removeMouseListener(popupHandler);
			tc.getDocument().removeUndoableEditListener(undoHandler);

			textComps.remove(index);
			undoers.remove(index);
		}
	}

	protected int getIndexOfJTextComponent(JTextComponent tc) {
		for (int i = 0; i < textComps.size(); i++) {
			WeakReference wr = (WeakReference) textComps.get(i);
			if (wr.get() == tc) {
				return i;
			}
		}

		return -1;
	}

	@SuppressWarnings("unchecked")
	private void clearEmptyReferences() {
		for (int i = 0; i < textComps.size(); i++) {
			WeakReference wr = (WeakReference) textComps.get(i);
			if (wr.get() == null) {
				undoers.set(i, null);
			}
		}

		for (Iterator it = textComps.iterator(); it.hasNext();) {
			WeakReference w = (WeakReference) it.next();
			if (w.get() == null) {
				it.remove();
			}
		}

		for (Iterator it = undoers.iterator(); it.hasNext();) {
			if (it.next() == null) {
				it.remove();
			}
		}
	}

	private void updateActions() {
		if (focusedComp != null && focusedComp.hasFocus()) {
			undo.setEnabled(undoer.canUndo());
			redo.setEnabled(undoer.canRedo());
			boolean hasSel = focusedComp.getSelectedText() != null;
			copy.setEnabled(hasSel);
			cut.setEnabled(hasSel);
		}
	}

	private class UndoListener implements UndoableEditListener {

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			UndoableEdit edit = e.getEdit();
			if (undoer != null) {
				undoer.addEdit(edit);
				updateActions();
			}
		}
	}

	private class RedoAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public RedoAction() {
			super(I18N.getMsg("shef.redo"), ShefUtils.getIconX16("redo"));
			putValue(MNEMONIC_KEY, (int) I18N.getMnemonic("shef.redo"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (undoer != null) {
					undoer.redo();
					updateActions();
				}
			} catch (Exception ex) {
				System.out.println("Cannot Redo");
			}
		}
	}

	private class UndoAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public UndoAction() {
			super(I18N.getMsg("shef.undo"), ShefUtils.getIconX16("undo"));
			putValue(MNEMONIC_KEY, (int) I18N.getMnemonic("shef.undo"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (undoer != null) {
					undoer.undo();
					updateActions();
				}
			} catch (Exception ex) {
				System.out.println("Cannot Undo");
			}
		}
	}

	private class NSelectAllAction extends TextAction {

		private static final long serialVersionUID = 1L;

		public NSelectAllAction() {
			super(I18N.getMsg("shef.select_all"));
			putValue(MNEMONIC_KEY, (int) I18N.getMnemonic("shef.select_all"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			getTextComponent(e).selectAll();
		}
	}

	private class PopupFocusHandler implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {
			if (!e.isTemporary()) {
				JTextComponent tc = (JTextComponent) e.getComponent();
				int index = getIndexOfJTextComponent(tc);
				if (index != -1) {
					// set the current UndoManager for the currently focused
					// JTextComponent
					undoer = (UndoManager) undoers.get(index);
					focusedComp = tc;
					updateActions();
				}

				// clean up any dead refs that have been garbage collected
				clearEmptyReferences();
			}
		}

		@Override
		public void focusLost(FocusEvent e) {
		}
	}

	private class CaretHandler implements CaretListener {

		@Override
		public void caretUpdate(CaretEvent e) {
			updateActions();
		}
	}

	private class PopupHandler extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			checkForPopupTrigger(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			checkForPopupTrigger(e);
		}

		private void checkForPopupTrigger(MouseEvent e) {
			JTextComponent tc = (JTextComponent) e.getComponent();
			if (e.isPopupTrigger() && tc.isEditable()) {
				if (!tc.isFocusOwner()) {
					tc.requestFocusInWindow();
				}

				popup.show(tc, e.getX(), e.getY());
			}
		}
	}
}
