/*
 * Created on Feb 28, 2005
 *
 */
package shef.ui.text.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledEditorKit;

import shef.ui.ShefUtils;
import shef.ui.text.HTMLUtils;
import storybook.i18n.I18N;

/**
 * Action which edits HTML font color
 *
 * @author Bob Tantlinger
 *
 */
public class HTMLFontColorAction extends HTMLTextEditAction {

	private static final long serialVersionUID = 1L;

	public HTMLFontColorAction() {
		super(I18N.getMsg("shef.color_"));
		putValue(MNEMONIC_KEY, (int) I18N.getMnemonic("shef.color_"));
		this.putValue(SMALL_ICON, ShefUtils.getIconX16("color"));
	}

	@Override
	protected void sourceEditPerformed(ActionEvent e, JEditorPane editor) {
		Color c = getColorFromUser(editor);
		if (c == null) {
			return;
		}

		String prefix = "<font color=" + HTMLUtils.colorToHex(c) + ">";
		String postfix = "</font>";
		String sel = editor.getSelectedText();
		if (sel == null) {
			editor.replaceSelection(prefix + postfix);

			int pos = editor.getCaretPosition() - postfix.length();
			if (pos >= 0) {
				editor.setCaretPosition(pos);
			}
		} else {
			sel = prefix + sel + postfix;
			editor.replaceSelection(sel);
		}
	}

	@Override
	protected void wysiwygEditPerformed(ActionEvent e, JEditorPane editor) {
		Color color = getColorFromUser(editor);
		if (color != null) {
			Action a = new StyledEditorKit.ForegroundAction("Color", color);
			a.actionPerformed(e);
		}
	}

	private Color getColorFromUser(Component c) {
		Window win = SwingUtilities.getWindowAncestor(c);
		if (win != null) {
			c = win;
		}
		Color color
				= JColorChooser.showDialog(c, "Color", Color.black);	 //$NON-NLS-1$
		return color;
	}

}
