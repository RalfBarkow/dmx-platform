package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.SequencedHashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



abstract class TypeModelImpl extends TopicModelImpl implements TypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String dataTypeUri;
    private List<IndexMode> indexModes;
    private SequencedHashMap<String, AssociationDefinitionModel> assocDefs; // is never null, may be empty
    private List<String> labelConfig;                                       // is never null, may be empty
    private ViewConfigurationModel viewConfig;                              // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    TypeModelImpl(String uri, String topicTypeUri, SimpleValue value, String dataTypeUri) {
        super(uri, topicTypeUri, value);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = new ArrayList();
        this.assocDefs = new SequencedHashMap();
        this.labelConfig = new ArrayList();
        this.viewConfig = new ViewConfigurationModel();
    }

    TypeModelImpl(TopicModel topic, String dataTypeUri, List<IndexMode> indexModes,
                  List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                  ViewConfigurationModel viewConfig) {
        super(topic);
        this.dataTypeUri = dataTypeUri;
        this.indexModes = indexModes;
        this.assocDefs = new SequencedHashMap();
        for (AssociationDefinitionModel assocDef : assocDefs) {
            addAssocDef(assocDef);
        }
        this.labelConfig = labelConfig;
        this.viewConfig = viewConfig;
    }

    TypeModelImpl(JSONObject typeModel) {
        super(typeModel);
        try {
            this.dataTypeUri = typeModel.getString("data_type_uri");
            this.indexModes = IndexMode.parse(typeModel);
            this.assocDefs = new SequencedHashMap();
            this.labelConfig = parseLabelConfig(typeModel);
            this.viewConfig = new ViewConfigurationModel(typeModel);
            parseAssocDefs(typeModel);
        } catch (Exception e) {
            throw new RuntimeException("Parsing TypeModel failed (JSONObject=" + typeModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Data Type ===

    @Override
    public String getDataTypeUri() {
        return dataTypeUri;
    }

    @Override
    public void setDataTypeUri(String dataTypeUri) {
        this.dataTypeUri = dataTypeUri;
    }



    // === Index Modes ===

    @Override
    public List<IndexMode> getIndexModes() {
        return indexModes;
    }

    @Override
    public void addIndexMode(IndexMode indexMode) {
        indexModes.add(indexMode);
    }



    // === Association Definitions ===

    @Override
    public Collection<AssociationDefinitionModel> getAssocDefs() {
        return assocDefs.values();
    }

    @Override
    public AssociationDefinitionModel getAssocDef(String assocDefUri) {
        return getAssocDefOrThrow(assocDefUri);
    }

    /**
     * @param   assocDef    the assoc def to add.
     *                      Note: its ID might be uninitialized (-1).
     */
    @Override
    public TypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return addAssocDefBefore(assocDef, null);   // beforeAssocDefUri=null
    }

    /**
     * @param   assocDef            the assoc def to add.
     *                              Note: its ID might be uninitialized (-1).
     * @param   beforeAssocDefUri   the URI of the assoc def <i>before</i> the given assoc def is inserted.
     *                              If <code>null</code> the assoc def is appended at the end.
     */
    @Override
    public TypeModel addAssocDefBefore(AssociationDefinitionModel assocDef, String beforeAssocDefUri) {
        try {
            // error check
            String assocDefUri = assocDef.getAssocDefUri();
            AssociationDefinitionModel existing = _getAssocDef(assocDefUri);
            if (existing != null) {
                throw new RuntimeException("Ambiguity: type \"" + getUri() + "\" has more than one \"" + assocDefUri +
                    "\" association definitions");
            }
            //
            assocDefs.putBefore(assocDefUri, assocDef, beforeAssocDefUri);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Adding an association definition to type \"" + getUri() + "\" before \"" +
                beforeAssocDefUri + "\" failed" + assocDef, e);
        }
    }

    @Override
    public AssociationDefinitionModel removeAssocDef(String assocDefUri) {
        try {
            AssociationDefinitionModel assocDef = assocDefs.remove(assocDefUri);
            if (assocDef == null) {
                throw new RuntimeException("Association definition \"" + assocDefUri + "\" not found in " +
                    assocDefs.keySet());
            }
            return assocDef;
        } catch (Exception e) {
            throw new RuntimeException("Removing association definition \"" + assocDefUri + "\" from type \"" +
                getUri() + "\" failed", e);
        }
    }

    // ---

    /**
     * Finds an assoc def by ID and returns its URI (at index 0). Returns the URI of the next-in-sequence
     * assoc def as well (at index 1), or null if the found assoc def is the last one.
     *
     * ### TODO: remove from public API
     */
    @Override
    public String[] findAssocDefUris(long assocDefId) {
        if (assocDefId == -1) {
            throw new IllegalArgumentException("findAssocDefUris() called with assocDefId=-1");
        }
        String[] assocDefUris = new String[2];
        Iterator<String> i = iterator();
        while (i.hasNext()) {
            String assocDefUri = i.next();
            long _assocDefId = checkAssocDefId(_getAssocDef(assocDefUri));
            if (_assocDefId == assocDefId) {
                assocDefUris[0] = assocDefUri;
                if (i.hasNext()) {
                    assocDefUris[1] = i.next();
                }
                break;
            }
        }
        if (assocDefUris[0] == null) {
            throw new RuntimeException("Assoc def with ID " + assocDefId + " not found in assoc defs of type \"" +
                getUri() + "\" (" + assocDefs.keySet() + ")");
        }
        return assocDefUris;
    }

    // ### TODO: remove from public API
    @Override
    public boolean hasSameAssocDefSequence(Collection<AssociationDefinitionModel> assocDefs) {
        Collection<AssociationDefinitionModel> _assocDefs = getAssocDefs();
        if (assocDefs.size() != _assocDefs.size()) {
            return false;
        }
        //
        Iterator<AssociationDefinitionModel> i = assocDefs.iterator();
        for (AssociationDefinitionModel _assocDef : _assocDefs) {
            AssociationDefinitionModel assocDef = i.next();
            // Note: if the assoc def's custom association type changedes the assoc def URI changes as well.
            // So we must identify the assoc defs to compare **by ID**.
            long assocDefId  = checkAssocDefId(assocDef);
            long _assocDefId = checkAssocDefId(_assocDef);
            if (assocDefId != _assocDefId) {
                return false;
            }
        }
        //
        return true;
    }

    // ### TODO: remove from public API
    @Override
    public void rehashAssocDef(String assocDefUri, String beforeAssocDefUri) {
        AssociationDefinitionModel assocDef = removeAssocDef(assocDefUri);
        logger.info("### Rehashing assoc def \"" + assocDefUri + "\" -> \"" + assocDef.getAssocDefUri() +
            "\" (put " + (beforeAssocDefUri != null ? "before \"" + beforeAssocDefUri + "\"" : "at end") + ")");
        addAssocDefBefore(assocDef, beforeAssocDefUri);
    }

    // ### TODO: remove from public API
    @Override
    public void rehashAssocDefs(Collection<AssociationDefinitionModel> newAssocDefs) {
        for (AssociationDefinitionModel assocDef : newAssocDefs) {
            rehashAssocDef(assocDef.getAssocDefUri(), null);
        }
    }

    // ### TODO: remove from public API
    @Override
    public void replaceAssocDef(AssociationDefinitionModel assocDef) {
        replaceAssocDef(assocDef, assocDef.getAssocDefUri(), null);
    }

    // ### TODO: remove from public API
    @Override
    public void replaceAssocDef(AssociationDefinitionModel assocDef, String oldAssocDefUri, String beforeAssocDefUri) {
        removeAssocDef(oldAssocDefUri);
        addAssocDefBefore(assocDef, beforeAssocDefUri);
    }



    // === Label Configuration ===

    @Override
    public List<String> getLabelConfig() {
        return labelConfig;
    }

    @Override
    public void setLabelConfig(List<String> labelConfig) {
        this.labelConfig = labelConfig;
    }

    // ---

    // ### TODO: remove from public API
    @Override
    public void replaceInLabelConfig(String newAssocDefUri, String oldAssocDefUri) {
        List<String> labelConfig = getLabelConfig();
        int i = labelConfig.indexOf(oldAssocDefUri);
        if (i != -1) {
            logger.info("### Label config: replacing \"" + oldAssocDefUri + "\" -> \"" + newAssocDefUri +
                "\" (position " + i + ")");
            labelConfig.set(i, newAssocDefUri);
        }
    }

    // ### TODO: remove from public API
    @Override
    public void removeFromLabelConfig(String assocDefUri) {
        List<String> labelConfig = getLabelConfig();
        int i = labelConfig.indexOf(assocDefUri);
        if (i != -1) {
            logger.info("### Label config: removing \"" + assocDefUri + "\" (position " + i + ")");
            labelConfig.remove(i);
        }
    }



    // === View Configuration ===

    @Override
    public ViewConfigurationModel getViewConfigModel() {
        return viewConfig;
    }

    // FIXME: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the topic type model. Possibly this method should be dropped.
    @Override
    public Object getViewConfig(String typeUri, String settingUri) {
        return viewConfig.getSetting(typeUri, settingUri);
    }

    @Override
    public void setViewConfig(ViewConfigurationModel viewConfig) {
        this.viewConfig = viewConfig;
    }



    // === Iterable Implementation ===

    /**
     * Returns an interator which iterates this TypeModel's assoc def URIs.
     */
    @Override
    public Iterator<String> iterator() {
        return assocDefs.keySet().iterator();
    }



    // ****************************
    // *** TopicModel Overrides ***
    // ****************************



    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            //
            o.put("data_type_uri", getDataTypeUri());
            IndexMode.toJSON(indexModes, o);
            AssociationDefinitionModel.toJSON(assocDefs.values(), o);
            o.put("label_config", new JSONArray(getLabelConfig()));
            getViewConfigModel().toJSON(o);
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public String toString() {
        return "id=" + id + ", uri=\"" + uri + "\", value=\"" + value + "\", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + getDataTypeUri() + "\", indexModes=" + getIndexModes() + ", assocDefs=" +
            getAssocDefs() + ", labelConfig=" + getLabelConfig() + ", " + getViewConfigModel();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<String> parseLabelConfig(JSONObject typeModel) throws JSONException {
        if (typeModel.has("label_config")) {
            return DeepaMehtaUtils.toList(typeModel.getJSONArray("label_config"));
        }
        return new ArrayList();
    }

    private void parseAssocDefs(JSONObject typeModel) throws JSONException {
        JSONArray assocDefs = typeModel.optJSONArray("assoc_defs");
        if (assocDefs != null) {
            for (int i = 0; i < assocDefs.length(); i++) {
                JSONObject assocDef = assocDefs.getJSONObject(i);
                assocDef.put("parent_type_uri", this.uri);
                addAssocDef(new AssociationDefinitionModel(assocDef));
            }
        }
    }

    // ---

    private AssociationDefinitionModel getAssocDefOrThrow(String assocDefUri) {
        AssociationDefinitionModel assocDef = _getAssocDef(assocDefUri);
        if (assocDef == null) {
            throw new RuntimeException("Association definition \"" + assocDefUri + "\" not found in " +
                assocDefs.keySet());
        }
        return assocDef;
    }

    private AssociationDefinitionModel _getAssocDef(String assocDefUri) {
        return assocDefs.get(assocDefUri);
    }

    // ---

    private long checkAssocDefId(AssociationDefinitionModel assocDef) {
        long assocDefId = assocDef.getId();
        if (assocDefId == -1) {
            throw new RuntimeException("The assoc def ID is uninitialized (-1): " + assocDef);
        }
        return assocDefId;
    }
}