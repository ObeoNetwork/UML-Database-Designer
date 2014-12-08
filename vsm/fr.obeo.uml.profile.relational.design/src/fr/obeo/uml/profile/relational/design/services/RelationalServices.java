package fr.obeo.uml.profile.relational.design.services;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.DirectedRelationship;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;

public class RelationalServices {
	
	public static final String RELATIONAL_PROFILE_SCHEMA = "RelationalProfile::Schema";
	public static final String RELATIONAL_PROFILE_TABLE = "RelationalProfile::Table";
	public static final String RELATIONAL_PROFILE_VIEW = "RelationalProfile::View";
	public static final String RELATIONAL_PROFILE_COLUMN = "RelationalProfile::Column";
	public static final String RELATIONAL_PROFILE_PK = "RelationalProfile::PK";
	public static final String RELATIONAL_PROFILE_FK = "RelationalProfile::FK";
	public static final String RELATIONAL_PROFILE_FK_ELEMENT = "RelationalProfile::FKElement";
	public static final String RELATIONAL_PROFILE_INDEX = "RelationalProfile::Index";
	public static final String RELATIONAL_PROFILE_INDEX_ELEMENT = "RelationalProfile::IndexElement";

	public static final String DB_TYPE_WITH_LENGTH = "RelationalTypes-Common::DBTypeWithLength";
	public static final String DB_TYPE_WITH_PRECISION_AND_SCALE = "RelationalTypes-Common::DBTypeWithPrecisionAndScale";
	public static final String DB_SIMPLE_TYPE = "RelationalTypes-Common::DBSimpleType";
	public static final String DB_TYPE = "RelationalTypes-Common::DBType";
	
	public static final String COLUMN_LENGTH = "length";
	public static final String COLUMN_PRECISION = "precision";
	public static final String COLUMN_SCALE = "scale";
	public static final String COLUMN_IN_PK = "inPK";
	public static final String COLUMN_NULLABLE = "nullable";
	
	public static final String VIEW_QUERY = "query";
	
	public static final String INDEX_UNIQUE = "unique";
	public static final String INDEX_ELEMENT_ASC = "asc";
	
	public static boolean isUnique(Class index) {
		Object isAsc = getTaggedValue(index, RELATIONAL_PROFILE_INDEX, INDEX_UNIQUE);
		if (isAsc instanceof Boolean) {
			return (Boolean)isAsc;
		}
		return false;
	}
	
	public static boolean isAsc(Constraint indexElement) {
		Object isUnique = getTaggedValue(indexElement, RELATIONAL_PROFILE_INDEX_ELEMENT, INDEX_ELEMENT_ASC);
		if (isUnique instanceof Boolean) {
			return (Boolean)isUnique;
		}
		return false;
	}
	
	public static boolean isNullable(Property column) {
		Object inPK = getTaggedValue(column, RELATIONAL_PROFILE_COLUMN, COLUMN_NULLABLE);
		if (inPK instanceof Boolean) {
			return (Boolean)inPK;
		}
		return false;
	}
	
	public static boolean isInPKAndNotInFK(Property column) {
		return isInPK(column) && !isInFK(column);
	}
	
	public static boolean isInPKAndInFK(Property column) {
		return isInPK(column) && isInFK(column);
	}
	
	public static boolean isNotInPKAndInFK(Property column) {
		return !isInPK(column) && isInFK(column);
	}
	
	public static boolean isInPK(Property column) {
		Object inPK = getTaggedValue(column, RELATIONAL_PROFILE_COLUMN, COLUMN_IN_PK);
		if (inPK instanceof Boolean) {
			return (Boolean)inPK;
		}
		return false;
	}
	
	public static boolean isInFK(Property column) {
		Association association = column.getAssociation();
		if (association != null) {
			if (isForeignKeyElement(association)) {
				Element owner = association.getOwner();
				if (isForeignKey(owner)) {
					Element superOwner = owner.getOwner();
					// This should be the table containing the column
					if (superOwner == column.getOwner()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static Collection<Class> getTables(Package pkg) {
		Collection<Class> tables = new ArrayList<Class>();
		for (Type ownedType : pkg.getOwnedTypes()) {
			if (isTable(ownedType)) {
				tables.add((Class)ownedType);
			}
		}
		return tables;
	}
	
	public static Collection<Class> getViews(Package pkg) {
		Collection<Class> tables = new ArrayList<Class>();
		for (Type ownedType : pkg.getOwnedTypes()) {
			if (isView(ownedType)) {
				tables.add((Class)ownedType);
			}
		}
		return tables;
	}
	
	public static Collection<Property> getColumns(Class table) {
		Collection<Property> columns = new ArrayList<Property>();
		for (Property ownedAttribute : table.getOwnedAttributes()) {
			if (isColumn(ownedAttribute)) {
				columns.add((Property)ownedAttribute);
			}
		}
		return columns;
	}
	
	public static Constraint getPrimaryKey(Class table) {
		for (Constraint constraint : table.getOwnedRules()) {
			if (isPrimaryKey(constraint)) {
				return constraint;
			}
		}
		return null;
	}
	
	public static Collection<Class> getAllForeignKeys(Package schema) {
		Collection<Class> fks = new ArrayList<Class>();
		for (Class table : getTables(schema)) {
			fks.addAll(getForeignKeys(table));
		}
		return fks;
	}
	
	public static Collection<Class> getForeignKeys(Class table) {
		Collection<Class> fks = new ArrayList<Class>();
		for (Classifier nestedClassifier : table.getNestedClassifiers()) {
			if (isForeignKey(nestedClassifier)) {
				fks.add((Class)nestedClassifier);
			}
		}
		return fks;
	}
	
	public static Class getTargetTableForFK(Class fk) {
		for (Classifier classifier : fk.getNestedClassifiers()) {
			if (classifier instanceof Association) {
				Association association = (Association)classifier;
				if (isForeignKeyElement(association)) {
					for (Property end : association.getMemberEnds()) {
						Element table = end.getOwner();
						if (table != fk.getOwner() && isTable(table)) {
							return (Class)table;
						}
					}
				}
			}
		}
		return null;
	}
	
	public static Collection<Class> getIndices(Class table) {
		Collection<Class> indices = new ArrayList<Class>();
		for (Classifier nestedClassifier : table.getNestedClassifiers()) {
			if (isIndex(nestedClassifier)) {
				indices.add((Class)nestedClassifier);
			}
		}
		return indices;
	}
	
	public static boolean isTable(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_TABLE);
	}
	
	public static boolean isView(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_VIEW);
	}
	
	public static boolean isColumn(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_COLUMN);
	}
	
	public static boolean isSchema(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_SCHEMA);
	}
	
	public static boolean isPrimaryKey(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_PK);
	}
	
	public static boolean isForeignKey(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_FK);
	}
	
	public static boolean isForeignKeyElement(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_FK_ELEMENT);
	}
	
	public static boolean isIndex(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_INDEX);
	}
	
	public static boolean isIndexElement(EObject object) {
		return hasStereotype(object, RELATIONAL_PROFILE_INDEX_ELEMENT);
	}
	
	public static boolean hasStereotype(EObject object, String stereotypeName) {
		if (object instanceof Element) {
			return ((Element)object).getAppliedStereotype(stereotypeName) != null;
		}
		return false;
	}
	
	public static String getColumnLength(Property property) {
		return getTaggedValueAsString(property, RELATIONAL_PROFILE_COLUMN, COLUMN_LENGTH);
	}
	
	public static String getColumnPrecision(Property property) {
		return getTaggedValueAsString(property, RELATIONAL_PROFILE_COLUMN, COLUMN_PRECISION);
	}
	
	public static String getColumnScale(Property property) {
		return getTaggedValueAsString(property, RELATIONAL_PROFILE_COLUMN, COLUMN_SCALE);
	}
	
	public static String getViewQuery(Class view) {
		return getTaggedValueAsString(view, RELATIONAL_PROFILE_VIEW, VIEW_QUERY);
	}
	
	public static Object getTaggedValue(Element element, String stereotypeName, String valueName) {
		Stereotype appliedStereotype = element.getAppliedStereotype(stereotypeName);
		if (appliedStereotype != null) {
			return element.getValue(appliedStereotype, valueName);
		}
		return null;
	}
	
	public static void setColumnLength(Property property, Integer length) {
		setTaggedValue(property, RELATIONAL_PROFILE_COLUMN, COLUMN_LENGTH, length);
	}
	
	public static void setColumnPrecision(Property property, Integer precision) {
		setTaggedValue(property, RELATIONAL_PROFILE_COLUMN, COLUMN_PRECISION, precision);
	}
	
	public static void setColumnScale(Property property, Integer scale) {
		setTaggedValue(property, RELATIONAL_PROFILE_COLUMN, COLUMN_SCALE, scale);
	}
	
	public static void setTaggedValue(Element element, String stereotypeName, String valueName, Object value) {
		Stereotype appliedStereotype = element.getAppliedStereotype(stereotypeName);
		if (appliedStereotype != null) {
			element.setValue(appliedStereotype, valueName, value);
		}
	}
	
	public static String getTaggedValueAsString(Element element, String stereotypeName, String valueName) {
		Object taggedValue = getTaggedValue(element, stereotypeName, valueName);
		if (taggedValue != null) {
			return taggedValue.toString();
		}
		return null;
	}
	
	public static boolean isSimpleType(Type propertyType) {
		return inheritsFromType(propertyType, DB_SIMPLE_TYPE);
	}
	
	public static boolean isTypeWithLength(Type propertyType) {
		return inheritsFromType(propertyType, DB_TYPE_WITH_LENGTH);
	}
	
	public static boolean isTypeWithPrecisionAndScale(Type propertyType) {
		return inheritsFromType(propertyType, DB_TYPE_WITH_PRECISION_AND_SCALE);
	}	
	
	public static boolean isDBType(Type propertyType) {
		return isSimpleType(propertyType) || isTypeWithLength(propertyType) || isTypeWithPrecisionAndScale(propertyType);
	}
	
	public static boolean inheritsFromType(Type propertyType, String parentTypeName) {
		for (DirectedRelationship relationship : propertyType.getSourceDirectedRelationships()) {
			if (relationship instanceof Generalization) {
				Generalization generalization = (Generalization)relationship;
				if (parentTypeName.equals(generalization.getGeneral().getQualifiedName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean validatePKAndPKColumns(Class table) {
		if (isTable(table)) {
			Constraint primaryKey = getPrimaryKey(table);
			if (primaryKey != null) {
				for (Property column : getColumns(table)) {
					if (isInPK(column)) {
						// At least one PK column, everything is Ok
						return true;
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public static Class selectPKColumns(Class table) {
		Collection<Property> columns = getColumns(table);
		if (columns.isEmpty()) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Select PK columns", "The table contains no column.");
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
				"Select PK column(s)");
		int btn = dlg.open();
		if (btn == Window.OK) {
			// Add selected columns to PK
			for (Object selectedElt : dlg.getResult()) {
				Property column = (Property)selectedElt;
				Stereotype appliedStereotype = column.getAppliedStereotype(RelationalServices.RELATIONAL_PROFILE_COLUMN);
				column.setValue(appliedStereotype, RelationalServices.COLUMN_IN_PK, true);
			}
			dlg.getResult();
		}
		return table;
	}
	
	public static Collection<Property> getIndexColumns(Class index) {
		Collection<Property> columns = new ArrayList<Property>();
		for (Constraint indexElt : getIndexElements(index)) {
			Property column = getColumnForIndexElement(indexElt);
			if (column != null) {
				columns.add(column);
			}
		}
		return columns;
	}
	
	public static Property getColumnForIndexElement(Constraint indexElement) {
		if (isIndexElement(indexElement)) {
			if (!indexElement.getConstrainedElements().isEmpty()) {
				Element element = indexElement.getConstrainedElements().get(0);
				if (element instanceof Property && isColumn(element)) {
					return (Property)element;
				}
			}
		}
		return null;
	}
	
	public static Collection<Constraint> getIndexElements(Class index) {
		Collection<Constraint> constraints = new ArrayList<Constraint>();
		for (Constraint constraint : index.getOwnedRules()) {
			if (isIndexElement(constraint)) {
				constraints.add(constraint);
			}
		}
		return constraints;
	}
	
	public static boolean isIndexForPK(Class index) {
		Element owner = index.getOwner();
		if (owner instanceof Class) {
			Class table = (Class)owner;
			Collection<Property> indexColumns = getIndexColumns(index);
			Collection<Property> tableColumns = getColumns(table);
			Collection<Property> pkColumns = new ArrayList<Property>();
			for (Property tableColumn : tableColumns) {
				if (isInPK(tableColumn)) {
					pkColumns.add(tableColumn);
				}
			}
			
			// We have to compare indexColumns and pkColumns
			if (pkColumns.containsAll(indexColumns) && indexColumns.containsAll(pkColumns)) {
				return true;
			}
		}
		return false;
	}
}
