package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeData extends TopicData implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long id;

    private String dataTypeUri;

    private Map<String, AssociationDefinition> assocDefs;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeData(TopicTypeData topicTypeData) {
        super(topicTypeData);
        this.dataTypeUri = topicTypeData.getDataTypeUri();
        this.assocDefs = topicTypeData.getAssocDefs();
    }

    public TopicTypeData(Topic typeTopic, String dataTypeUri) {
        super(typeTopic.getUri(), typeTopic.getValue(), typeTopic.getTypeUri(), null);  // composite=null
        this.dataTypeUri = dataTypeUri;
        this.assocDefs = new HashMap();
    }

    public TopicTypeData(String uri, String value, String dataTypeUri) {
        super(uri, new TopicValue(value), "dm3.core.topic_type", null);                 // composite=null
        this.dataTypeUri = dataTypeUri;
        this.assocDefs = new HashMap();
    }

    public TopicTypeData(JSONObject topicType) {
        try {
            this.uri = topicType.getString("uri");
            this.value = new TopicValue(topicType.get("value"));
            this.typeUri = "dm3.core.topic_type";
            this.dataTypeUri = topicType.getString("data_type_uri");
            this.assocDefs = new HashMap();
            parseAssocDefs(topicType);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicTypeData failed (JSONObject=" + topicType + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getDataTypeUri() {
        return dataTypeUri;
    }

    @Override
    public Map<String, AssociationDefinition> getAssocDefs() {
        return assocDefs;
    }

    // ---

    @Override
    public AssociationDefinition getAssocDef(String assocDefUri) {
        AssociationDefinition assocDef = assocDefs.get(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Association definition \"" + assocDefUri + "\" not found (in " + this + ")");
        }
        return assocDef;
    }

    // FIXME: abstraction. Adding should be the factory's resposibility
    @Override
    public void addAssocDef(AssociationDefinition assocDef) {
        String assocDefUri = assocDef.getUri();
        AssociationDefinition existing = assocDefs.get(assocDefUri);
        if (existing != null) {
            throw new RuntimeException("Ambiguity: topic type definition \"" + uri + "\" has more than " +
                "one association definitions with uri \"" + assocDefUri + "\" -- Use distinct part role types or " +
                "specifiy an unique uri");
        }
        assocDefs.put(assocDefUri, assocDef);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            o.put("data_type_uri", dataTypeUri);
            //
            List assocDefs = new ArrayList();
            for (AssociationDefinition assocDef : this.assocDefs.values()) {
                assocDefs.add(assocDef.toJSON());
            }
            o.put("assoc_defs", assocDefs);
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "topic type data (uri=\"" + uri + "\", value=" + value + ", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + dataTypeUri + "\", assocDefs=" + assocDefs + ")";
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void parseAssocDefs(JSONObject topicType) throws Exception {
        JSONArray assocDefs = topicType.optJSONArray("assoc_defs");
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                addAssocDef(new AssociationDefinition(assocDefs.getJSONObject(i), this.uri));
            }
        }
    }
}
