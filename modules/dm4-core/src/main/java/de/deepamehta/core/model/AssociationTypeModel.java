package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * Data that underlies a {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeModel extends TypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationTypeModel(String uri, String value, String dataTypeUri) {
        super(uri, "dm4.core.assoc_type", new SimpleValue(value), dataTypeUri);
    }

    public AssociationTypeModel(TopicModel topic, String dataTypeUri, List<IndexMode> indexModes,
                                List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                                ViewConfigurationModel viewConfig) {
        super(topic, dataTypeUri, indexModes, assocDefs, labelConfig, viewConfig);
    }

    public AssociationTypeModel(JSONObject assocType) throws JSONException {
        super(assocType.put("type_uri", "dm4.core.assoc_type"));
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationTypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return (AssociationTypeModel) super.addAssocDef(assocDef);
    }

    // ---

    @Override
    public String toString() {
        return "association type (" + super.toString() + ")";
    }
}
