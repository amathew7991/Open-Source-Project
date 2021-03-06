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
package storybook.ui.panel.info;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.JButton;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.infonode.docking.View;
import org.miginfocom.swing.MigLayout;

import org.hibernate.Session;
import storybook.SbConstants.BookKey;
import storybook.controller.BookController;
import storybook.model.DbFile;
import storybook.model.BookModel;
import storybook.model.EntityUtil;
import storybook.model.hbn.entity.AbstractEntity;
import storybook.model.hbn.entity.Internal;
import storybook.toolkit.BookUtil;
import storybook.i18n.I18N;
import storybook.model.hbn.entity.Scene;
import storybook.toolkit.net.NetUtil;
import storybook.toolkit.odt.ODTUtils;
import storybook.toolkit.swing.SwingUtil;
import storybook.ui.panel.AbstractPanel;
import storybook.ui.MainFrame;

/**
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class InfoPanel extends AbstractPanel implements HyperlinkListener {

	private AbstractEntity entity;
	private JTextPane infoPane;
	private JButton externalFile;
	private boolean bExternalButton;

	public InfoPanel(MainFrame mainFrame) {
		super(mainFrame);
	}

	@Override
	public void modelPropertyChange(PropertyChangeEvent evt) {
		// Object oldValue = evt.getOldValue();
		Object newValue = evt.getNewValue();
		String propName = evt.getPropertyName();

		if (BookController.CommonProps.REFRESH.check(propName)) {
			View newView = (View) newValue;
			View view = (View) getParent().getParent();
			if (view == newView) {
				refresh();
			}
			return;
		}

		if (BookController.CommonProps.SHOW_INFO.check(propName)) {
			if (newValue instanceof AbstractEntity) {
				entity = (AbstractEntity) newValue;
				if (entity.isTransient()) {
					return;
				}
				BookModel model = mainFrame.getBookModel();
				Session session = model.beginTransaction();
				session.refresh(entity);
				model.commit();
				refreshInfo();
				return;
			}
			if (newValue instanceof DbFile) {
				StringBuilder buf = new StringBuilder();
				buf.append("<p><b>\n");
				buf.append(I18N.getColonMsg("title"));
				buf.append("</b></p><p>\n");
				Internal internal = BookUtil.get(mainFrame, BookKey.TITLE, "");
				buf.append(internal.getStringValue());
				buf.append("</p><p style='padding-top:10px'><b>\n");
				buf.append(I18N.getColonMsg("subtitle"));
				buf.append("</b></p><p>\n");
				internal = BookUtil.get(mainFrame, BookKey.SUBTITLE, "");
				buf.append(internal.getStringValue());
				buf.append("</p><p style='padding-top:10px'><b>\n");
				buf.append(I18N.getColonMsg("author_s"));
				buf.append("</b></p><p>\n");
				internal = BookUtil.get(mainFrame, BookKey.AUTHOR, "");
				buf.append(internal.getStringValue());
				buf.append("</p><p style='padding-top:10px'><b>\n");
				buf.append(I18N.getColonMsg("copyright"));
				buf.append("</b></p><p>\n");
				internal = BookUtil.get(mainFrame, BookKey.COPYRIGHT, "");
				buf.append(internal.getStringValue());
				buf.append("</p><p style='padding-top:10px'><b>\n");
				buf.append(I18N.getColonMsg("blurb"));
				buf.append("</b></p><p>\n");
				internal = BookUtil.get(mainFrame, BookKey.BLURB, "");
				buf.append(internal.getStringValue());
				buf.append("</p><p style='padding-top:10px'><b>\n");
				buf.append(I18N.getColonMsg("notes"));
				buf.append("</b></p><p>\n");
				internal = BookUtil.get(mainFrame, BookKey.NOTES, "");
				buf.append(internal.getStringValue());
				buf.append("<p>\n");
				infoPane.setText(buf.toString());
				infoPane.setCaretPosition(0);
				return;
			}
		}

		if (entity != null && newValue instanceof AbstractEntity) {
			AbstractEntity updatedEntity = (AbstractEntity) newValue;
			if (updatedEntity.getId().equals(entity.getId())) {
				refreshInfo();
			}
		}
	}

	@Override
	public void init() {
	}

	@Override
	public void initUi() {
		setLayout(new MigLayout("wrap,fill,ins 0"));

		infoPane = new JTextPane();
		infoPane.setEditable(false);
		infoPane.setOpaque(true);
		infoPane.setContentType("text/html");
		infoPane.addHyperlinkListener(this);
		JScrollPane scroller = new JScrollPane(infoPane);
		SwingUtil.setMaxPreferredSize(scroller);
		add(scroller);

		if (entity != null) {
			refreshInfo();
		}
	}

	private void refreshInfo() {
		infoPane.setText(EntityUtil.getInfo(mainFrame, entity));
		infoPane.setCaretPosition(0);
		infoPane.setComponentPopupMenu(EntityUtil.createPopupMenu(mainFrame, entity));
		for (int i=0; i < this.getComponentCount(); i++) {
			Component comp = this.getComponent(i);
			if (comp instanceof JButton) {
				if (comp.getName().equals("btExternal")) {
					this.remove(comp);
					break;
				}
			}
		}
		if ((entity instanceof Scene) && BookUtil.isUseXeditor(mainFrame)) {
			JButton btExternal = new JButton(I18N.getMsg("xeditor.launching"));
			btExternal.setName("btExternal");
			btExternal.addActionListener((ActionEvent evt) -> {
				System.out.println("launch external editor");
				ODTUtils.launchExternalEditor(mainFrame, (Scene) entity);
			});
			add(btExternal);
		}
		if (entity instanceof Scene) {
			mainFrame.setLastUsedScene((Scene)entity);
		}
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		try {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				NetUtil.openBrowser(e.getURL().toString());
			}
		} catch (Exception exc) {
			System.err.println("InfoPanel.hyperlinkUpdate(" + e.toString() + ") Exception : " + exc.getMessage());
		}
	}

	private void launchExternalFile() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
