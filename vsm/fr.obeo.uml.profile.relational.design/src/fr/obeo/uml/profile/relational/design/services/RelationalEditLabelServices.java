package fr.obeo.uml.profile.relational.design.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.util.UMLSwitch;

public class RelationalEditLabelServices extends UMLSwitch<Element>{
	
	private static final String TYPEDEF_PATTERN_STRING = "\\(\\s*([0-9]*)\\s*(?:,\\s*([0-9]*)\\s*)?\\)?";
	private static final Pattern TYPEDEF_PATTERN = Pattern.compile(TYPEDEF_PATTERN_STRING);

	private String editedLabelContent;
	
	public Element editRelationalElementLabel(Element element, String editedLabelContent) {
		this.editedLabelContent = editedLabelContent;
		return doSwitch(element);
	}
	
	@Override
	public Element caseProperty(Property property) {
		if (!RelationalServices.isColumn(property)) {
			return super.caseProperty(property);			
		} else {
			// the label can be in the form "columnName : typeName [precision, length]"
			int pos = editedLabelContent.indexOf(':');
			if (pos != -1) {
				String columnName = editedLabelContent.substring(0, pos).trim();
				property.setName(columnName);
				
				String typeDef = editedLabelContent.substring(pos + 1).trim();
				setType(property, typeDef, getRelationalTypes(property));

			} else {
				// there is only a name
				return super.caseNamedElement(property);
			}
		}
		return property;
	}
	
	private Collection<PrimitiveType> getRelationalTypes(Element element) {
		Collection<PrimitiveType> types = new ArrayList<PrimitiveType>();

		ResourceSet set = element.eResource().getResourceSet();
		for (Resource resource : set.getResources()) {
			for (EObject object : resource.getContents()) {
				if (object instanceof Model) {
					Model model = (Model)object;
					if ("RelationalTypes-MySQL-5".equals(model.getName())) {
						for (Type ownedType : model.getOwnedTypes()) {
							if (ownedType instanceof PrimitiveType) {
								if (RelationalServices.isDBType(ownedType)) {
									types.add((PrimitiveType)ownedType);
								}
							}
						}
					}
				}
			}
		}
		
		return types;
	}
	
	private void setType(Property column, String typeDef, Collection<PrimitiveType> types) {
		// Extract type name
		String typeName = null;
		Integer value1 = null;
		Integer value2 = null;
		int pos = typeDef.indexOf("(");
		if (pos != -1) {
			typeName = typeDef.substring(0, pos).trim();
			String attributes = typeDef.substring(pos).trim();
			Matcher matcher = TYPEDEF_PATTERN.matcher(attributes);
			if (matcher.matches()) {
				value1 = getIntegerFromStringValue(matcher.group(1));
				value2 = getIntegerFromStringValue(matcher.group(2));
			}
		} else {
			typeName = typeDef.trim();
		}
		if (typeName != null) {
			for (PrimitiveType type : types) {
				if (typeName.equalsIgnoreCase(type.getName())) {
					column.setType(type);
					if (RelationalServices.isTypeWithLength(type)) {
						RelationalServices.setColumnLength(column, value1);
					} else if (RelationalServices.isTypeWithPrecisionAndScale(type)) {
						RelationalServices.setColumnPrecision(column, value1);
						RelationalServices.setColumnScale(column, value2);
					}
					return;
				}
			}
		}
	}
	
	private Integer getIntegerFromStringValue(String valueAsString) {
		if (valueAsString == null) {
			return null;
		}
		try {
			Integer valueAsInteger = new Integer(valueAsString);
			return valueAsInteger;
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Override
	public Element caseNamedElement(NamedElement namedElement) {
		namedElement.setName(editedLabelContent);
		return namedElement;
	}
	
	public Class editViewQuery(Class view, String query) {
		if (RelationalServices.isView(view)) {
			Stereotype appliedStereotype = view.getAppliedStereotype(RelationalServices.RELATIONAL_PROFILE_VIEW);
			view.setValue(appliedStereotype, RelationalServices.VIEW_QUERY, query);
		}
		return view;
	}
}
