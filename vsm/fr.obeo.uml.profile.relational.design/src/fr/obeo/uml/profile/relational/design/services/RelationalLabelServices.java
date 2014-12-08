package fr.obeo.uml.profile.relational.design.services;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.util.UMLSwitch;

public class RelationalLabelServices extends UMLSwitch<String> {

	public String getRelationalElementLabel(Element element) {
		return doSwitch(element);
	}
	
	@Override
	public String caseClass(Class object) {
		if (RelationalServices.isIndex(object)) {
			String label = caseNamedElement(object);
			label += "(";
			label += getIndexColumnsLabel(object);
			label += ")";
			return label;
		}
		return super.caseClass(object);
	}
	
	public String getIndexColumnsLabel(Class index) {
		String label = "";
		for (Constraint indexElt : RelationalServices.getIndexElements(index)) {
			Property column = RelationalServices.getColumnForIndexElement(indexElt);
			if (column != null) {
				label += column.getName();
				boolean asc = RelationalServices.isAsc(indexElt);
				if (asc) {
					label += " ASC, ";
				} else {
					label += " DESC, ";
				}
			} else {
				label += "?, ";
			}
		}
		// Remove last ", " sequence
		if (label.endsWith(", ")) {
			label = label.substring(0, label.length() - 2);
		}
		return label;
	}
	
	public String caseProperty(Property property) {
		if (RelationalServices.isColumn(property)) {
			String label = caseNamedElement(property);
			label += " : ";
			label += getColumnTypeLabel(property);
			return label;
		} else {
			return super.caseProperty(property);
		}
	};
	
	public String getColumnTypeLabel(Property property) {
		String typeLabel = "undefined";
		Type propertyType = property.getType();
		if (propertyType != null) {
			typeLabel = caseNamedElement(propertyType);
			// Add length, precision or scale
			if (RelationalServices.isTypeWithLength(propertyType)) {
				String columnLength = RelationalServices.getColumnLength(property);
				if (columnLength == null) {
					columnLength = "?";
				}
				typeLabel += "(" + columnLength + ")";
			} else if (RelationalServices.isTypeWithPrecisionAndScale(propertyType)) {
				String columnPrecision = RelationalServices.getColumnPrecision(property);
				if (columnPrecision == null) {
					columnPrecision = "?";
				}
				String columnScale = RelationalServices.getColumnScale(property);
				if (columnScale == null) {
					columnScale = "?";
				}
				typeLabel += "(" + columnPrecision + "," + columnScale + ")";
			}
		}
		return typeLabel;
	}
	
	@Override
	public String caseNamedElement(NamedElement namedElement) {
		return namedElement.getName();
	}	
}
