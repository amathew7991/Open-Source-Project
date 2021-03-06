/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package storybook.ui.table;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import org.hibernate.Session;
import storybook.SbApp;

import storybook.controller.BookController;
import storybook.exim.exporter.TableExporter;
import storybook.model.BookModel;
import storybook.model.hbn.dao.AttributeDAOImpl;
import storybook.model.hbn.entity.AbstractEntity;
import storybook.model.hbn.entity.Attribute;
import storybook.ui.MainFrame;
import storybook.ui.SbView;

/**
 *
 * @author favdb
 */
public class AttributeTable extends AbstractTable {
	
	public AttributeTable(MainFrame main) {
		super(main);
	}

	@Override
	protected void sendSetEntityToEdit(int row) {
		if (row == -1) {
			return;
		}
		Attribute attribute = (Attribute) getEntityFromRow(row);
		mainFrame.showEditorAsDialog(attribute);
	}

	@Override
	protected void sendSetNewEntityToEdit(AbstractEntity entity) {
		mainFrame.showEditorAsDialog(entity);
	}

	@Override
	protected void sendDeleteEntity(int row) {
		Attribute entity = (Attribute) getEntityFromRow(row);
		ctrl.deleteAttribute(entity);
	}

	@Override
	protected void sendDeleteEntities(int[] rows) {
		ArrayList<Long> ids = new ArrayList<Long>();
		for (int row : rows) {
			Attribute entity = (Attribute) getEntityFromRow(row);
			ids.add(entity.getId());
		}
		ctrl.deleteMultiGenders(ids);
		ctrl.deleteMultiSpecies(ids);
	}

	@Override
	protected void modelPropertyChangeLocal(PropertyChangeEvent evt) {
		try {
			String propName = evt.getPropertyName();
			if (BookController.AttributeProps.INIT.check(propName)) {
				initTableModel(evt);
			} else if (BookController.AttributeProps.UPDATE.check(propName)) {
				updateEntity(evt);
			} else if (BookController.AttributeProps.NEW.check(propName)) {
				newEntity(evt);
			} else if (BookController.AttributeProps.DELETE.check(propName)) {
				deleteEntity(evt);
			} else if (BookController.CommonProps.EXPORT.check(propName) 
				&& ((SbView)evt.getNewValue()).getName().equals("Attributes")) {
				TableExporter.exportTable(mainFrame,(SbView)evt.getNewValue());
			}
		} catch (Exception e) {
		}

	}

	@Override
	public void init() {
		SbApp.trace("AttributeTable.init()");
		columns = SbColumnFactory.getInstance().getAttributeColumns();
	}
	
	@Override
	protected AbstractEntity getEntity(Long id) {
		BookModel model = mainFrame.getBookModel();
		Session session = model.beginTransaction();
		AttributeDAOImpl dao = new AttributeDAOImpl(session);
		Attribute entity = dao.find(id);
		model.commit();
		return entity;
	}

	@Override
	protected AbstractEntity getNewEntity() {
		return new Attribute();
	}

	@Override
	public String getTableName() {
		return("Attribute");
	}
}
