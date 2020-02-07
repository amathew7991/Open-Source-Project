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

package storybook.ui.table;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import org.hibernate.Session;
import storybook.SbApp;
import storybook.controller.BookController;
import storybook.exim.exporter.TableExporter;
import storybook.model.BookModel;
import storybook.model.hbn.dao.SpeciesDAOImpl;
import storybook.model.hbn.entity.AbstractEntity;
import storybook.model.hbn.entity.Species;
import storybook.ui.MainFrame;
import storybook.ui.SbView;

/**
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class SpeciesTable extends AbstractTable {

	public SpeciesTable(MainFrame mainFrame) {
		super(mainFrame);
	}

	@Override
	public void init() {
		columns = SbColumnFactory.getInstance().getSpeciesColumns();
	}

	@Override
	protected void modelPropertyChangeLocal(PropertyChangeEvent evt) {
		try {
			String propName = evt.getPropertyName();
			if (BookController.SpeciesProps.INIT.check(propName)) {
				initTableModel(evt);
			} else if (BookController.SpeciesProps.UPDATE.check(propName)) {
				updateEntity(evt);
			} else if (BookController.SpeciesProps.NEW.check(propName)) {
				newEntity(evt);
			} else if (BookController.SpeciesProps.DELETE.check(propName)) {
				deleteEntity(evt);
			} else if (BookController.CommonProps.EXPORT.check(propName) 
				&& ((SbView)evt.getNewValue()).getName().equals("Species")) {
				TableExporter.exportTable(mainFrame,(SbView)evt.getNewValue());
			}
		} catch (Exception e) {
		}
	}

	@Override
	protected void sendSetEntityToEdit(int row) {
		if (row == -1) {
			return;
		}
		Species species = (Species) getEntityFromRow(row);
//		ctrl.setSpeciesToEdit(species);
//		mainFrame.showView(ViewName.EDITOR);
		mainFrame.showEditorAsDialog(species);
	}

	@Override
	protected void sendSetNewEntityToEdit(AbstractEntity entity) {
//		ctrl.setSpeciesToEdit((Species) entity);
//		mainFrame.showView(ViewName.EDITOR);
		mainFrame.showEditorAsDialog(entity);
	}

	@Override
	protected synchronized void sendDeleteEntity(int row) {
		Species species = (Species) getEntityFromRow(row);
		ctrl.deleteSpecies(species);
	}

	@Override
	protected synchronized void sendDeleteEntities(int[] rows) {
		ArrayList<Long> ids = new ArrayList<>();
		for (int row : rows) {
			Species species = (Species) getEntityFromRow(row);
			ids.add(species.getId());
		}
		ctrl.deleteMultiSpecies(ids);
	}

	@Override
	protected AbstractEntity getEntity(Long id) {
		BookModel model = mainFrame.getBookModel();
		Session session = model.beginTransaction();
		SpeciesDAOImpl dao = new SpeciesDAOImpl(session);
		Species species = dao.find(id);
		model.commit();
		return species;
	}

	@Override
	protected AbstractEntity getNewEntity() {
		return new Species();
	}

	@Override
	public String getTableName() {
		return("Species");
	}
}
