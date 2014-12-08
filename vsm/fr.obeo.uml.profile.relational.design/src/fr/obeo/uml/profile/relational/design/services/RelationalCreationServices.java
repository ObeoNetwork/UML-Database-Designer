package fr.obeo.uml.profile.relational.design.services;

import java.util.Collection;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;

public class RelationalCreationServices {
	
	public Constraint createPrimaryKey(Class table) {
		Constraint newPK = UMLFactory.eINSTANCE.createConstraint();
		table.getOwnedRules().add(newPK);
		newPK.setName("PK");
		applyStereotype(newPK, RelationalServices.RELATIONAL_PROFILE_PK);
		LiteralString literal = UMLFactory.eINSTANCE.createLiteralString();
		newPK.setSpecification(literal);
		return newPK;
	}

	public Class createTable(Package pkg) {
		Class newTable = UMLFactory.eINSTANCE.createClass();
		pkg.getPackagedElements().add(newTable);
		applyStereotype(newTable, RelationalServices.RELATIONAL_PROFILE_TABLE);
		newTable.setName("Table" + RelationalServices.getTables(pkg).size());
		return newTable;
	}
	
	public Class createView(Package pkg) {
		Class newView = UMLFactory.eINSTANCE.createClass();
		pkg.getPackagedElements().add(newView);
		Stereotype stereotype = applyStereotype(newView, RelationalServices.RELATIONAL_PROFILE_VIEW);
		newView.setValue(stereotype, RelationalServices.VIEW_QUERY, "SELECT * FROM XXX;");
		newView.setName("View" + RelationalServices.getViews(pkg).size());
		return newView;
	}
	
	public Property createColumn(Class table) {
		return internalCreateColumn(table, false);
	}
	
	public Property createPKColumn(Class table) {
		// Ensure there is a PK on table
		if (RelationalServices.getPrimaryKey(table) == null) {
			createPrimaryKey(table);
		}
		return internalCreateColumn(table, true);
	}
	
	public Class createForeignKey(Class sourceTable, Class targetTable) {
		Class fk = UMLFactory.eINSTANCE.createClass();
		sourceTable.getNestedClassifiers().add(fk);
		applyStereotype(fk, RelationalServices.RELATIONAL_PROFILE_FK);
		fk.setName("fk" + RelationalServices.getForeignKeys(sourceTable).size());
		
		// Create FK columns
		for (Property targetTableColumn : RelationalServices.getColumns(targetTable)) {
			if (RelationalServices.isInPK(targetTableColumn)) {
				// Create a FK column and reference the PK column
				Property fkColumn = createColumn(sourceTable);
				fkColumn.setName(targetTableColumn.getName());
				Type targetType = targetTableColumn.getType();
				fkColumn.setType(targetType);
				// Copy tagged values
				if (RelationalServices.isTypeWithLength(targetType)) {
					RelationalServices.setColumnLength(fkColumn, new Integer(RelationalServices.getColumnLength(targetTableColumn)));
				} else if (RelationalServices.isTypeWithPrecisionAndScale(targetType)) {
					RelationalServices.setColumnPrecision(fkColumn, new Integer(RelationalServices.getColumnPrecision(targetTableColumn)));
					RelationalServices.setColumnScale(fkColumn, new Integer(RelationalServices.getColumnScale(targetTableColumn)));
				}
				
				Association association = UMLFactory.eINSTANCE.createAssociation();
				fk.getNestedClassifiers().add(association);
				association.getMemberEnds().add(targetTableColumn);
				association.getMemberEnds().add(fkColumn);
				applyStereotype(association, RelationalServices.RELATIONAL_PROFILE_FK_ELEMENT);
			}
		}
		
		return fk;
	}
	
	public Class createIndex(Class table) {
		Class index = null;
		
		Collection<Property> columns = RelationalServices.getColumns(table);
		if (columns.isEmpty()) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Select Index columns", "The table contains no column.");
			return table;
		}
		ListSelectionDialog dlg = new ListSelectionDialog(Display.getDefault().getActiveShell(),
				columns,
				new ArrayContentProvider(),
				new LabelProvider() {
					@Override
					public String getText(Object element) {
						return new RelationalLabelServices().getRelationalElementLabel((Property)element);
					}
				},
				"Select Index column(s)");
		int btn = dlg.open();
		if (btn == Window.OK) {
			index = UMLFactory.eINSTANCE.createClass();
			table.getNestedClassifiers().add(index);
			applyStereotype(index, RelationalServices.RELATIONAL_PROFILE_INDEX);
			index.setName("index" + RelationalServices.getIndices(table).size());
			
			// Add selected columns to PK
			for (Object selectedElt : dlg.getResult()) {
				Property column = (Property)selectedElt;
				Constraint indexElt = UMLFactory.eINSTANCE.createConstraint();
				index.getOwnedRules().add(indexElt);
				applyStereotype(indexElt, RelationalServices.RELATIONAL_PROFILE_INDEX_ELEMENT);
				indexElt.getConstrainedElements().add(column);
				RelationalServices.setTaggedValue(indexElt, RelationalServices.RELATIONAL_PROFILE_INDEX_ELEMENT, RelationalServices.INDEX_ELEMENT_ASC, true);
				LiteralString literal = UMLFactory.eINSTANCE.createLiteralString();
				indexElt.setSpecification(literal);
			}
			dlg.getResult();
		}
		
		return index;
	}
	
	private Property internalCreateColumn(Class table, boolean inPK) {
		Property newColumn = UMLFactory.eINSTANCE.createProperty();
		table.getOwnedAttributes().add(newColumn);
		Stereotype appliedStereotype = applyStereotype(newColumn, RelationalServices.RELATIONAL_PROFILE_COLUMN);
		newColumn.setValue(appliedStereotype, RelationalServices.COLUMN_IN_PK, inPK);
		newColumn.setName("col" + RelationalServices.getColumns(table).size());
		return newColumn;
	}
	
	private Stereotype applyStereotype(Element element, String stereotypeName) {
		Stereotype applicableStereotype = element.getApplicableStereotype(stereotypeName);
		if (applicableStereotype != null) {
			element.applyStereotype(applicableStereotype);
		}
		return applicableStereotype;
	}
	
}
