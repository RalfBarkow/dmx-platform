package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicDeletionModel;
import systems.dmx.core.model.TopicPlayerModel;
import systems.dmx.core.model.TopicReferenceModel;
import systems.dmx.core.model.ViewConfigModel;
import systems.dmx.core.model.facets.FacetValueModel;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.model.topicmaps.ViewProps;
import systems.dmx.core.service.ModelFactory;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



// Note: instantiated by CoreActivator
public class ModelFactoryImpl implements ModelFactory {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String REF_ID_PREFIX  = "ref_id:";
    private static final String REF_URI_PREFIX = "ref_uri:";
    private static final String DEL_ID_PREFIX  = "del_id:";
    private static final String DEL_URI_PREFIX = "del_uri:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    AccessLayer al;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === TopicModel ===

    @Override
    public TopicModelImpl newTopicModel(long id, String uri, String typeUri, SimpleValue value,
                                                                             ChildTopicsModel childTopics) {
        return new TopicModelImpl(newDMXObjectModel(id, uri, typeUri, value, childTopics));
    }

    // TODO: needed?
    @Override
    public TopicModelImpl newTopicModel(ChildTopicsModel childTopics) {
        return newTopicModel(-1, null, null, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(String typeUri) {
        return newTopicModel(-1, null, typeUri, null, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String typeUri, SimpleValue value) {
        return newTopicModel(-1, null, typeUri, value, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String typeUri, ChildTopicsModel childTopics) {
        return newTopicModel(-1, null, typeUri, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(String uri, String typeUri) {
        return newTopicModel(-1, uri, typeUri, null, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String uri, String typeUri, SimpleValue value) {
        return newTopicModel(-1, uri, typeUri, value, null);
    }

    @Override
    public TopicModelImpl newTopicModel(String uri, String typeUri, ChildTopicsModel childTopics) {
        return newTopicModel(-1, uri, typeUri, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(long id) {
        return newTopicModel(id, null, null, null, null);
    }

    @Override
    public TopicModelImpl newTopicModel(long id, SimpleValue value) {
        return newTopicModel(id, null, null, value, null);
    }

    @Override
    public TopicModelImpl newTopicModel(long id, ChildTopicsModel childTopics) {
        return newTopicModel(id, null, null, null, childTopics);
    }

    @Override
    public TopicModelImpl newTopicModel(TopicModel topic) {
        return new TopicModelImpl((TopicModelImpl) topic);
    }

    @Override
    public TopicModelImpl newTopicModel(JSONObject topic) {
        try {
            return new TopicModelImpl(newDMXObjectModel(topic));
        } catch (Exception e) {
            throw parsingFailed(topic, e, "TopicModelImpl");
        }
    }



    // === AssocModel ===

    @Override
    public AssocModelImpl newAssocModel(long id, String uri, String typeUri, PlayerModel player1, PlayerModel player2,
                                        SimpleValue value, ChildTopicsModel childTopics) {
        return new AssocModelImpl(newDMXObjectModel(id, uri, typeUri, value, childTopics),
            (PlayerModelImpl) player1, (PlayerModelImpl) player2);
    }

    @Override
    public AssocModelImpl newAssocModel(String typeUri, PlayerModel player1, PlayerModel player2) {
        return newAssocModel(-1, null, typeUri, player1, player2, null, null);
    }

    @Override
    public AssocModelImpl newAssocModel(String typeUri, PlayerModel player1, PlayerModel player2,
                                        ChildTopicsModel childTopics) {
        return newAssocModel(-1, null, typeUri, player1, player2, null, childTopics);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    // ### TODO: make internal?
    @Override
    public AssocModelImpl newAssocModel() {
        return newAssocModel(-1, null, null, null, null, null, null);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    // ### TODO: make internal?
    @Override
    public AssocModelImpl newAssocModel(ChildTopicsModel childTopics) {
        return newAssocModel(null, childTopics);
    }

    // ### TODO: don't leave the assoc uninitialized. Refactoring needed. See comment in TypeCache#put methods.
    // ### TODO: make internal?
    @Override
    public AssocModelImpl newAssocModel(String typeUri, ChildTopicsModel childTopics) {
        return newAssocModel(typeUri, null, null, childTopics);
    }

    @Override
    public AssocModelImpl newAssocModel(long id, String uri, String typeUri, PlayerModel player1, PlayerModel player2) {
        return newAssocModel(id, uri, typeUri, player1, player2, null, null);
    }

    @Override
    public AssocModelImpl newAssocModel(AssocModel assoc) {
        return new AssocModelImpl((AssocModelImpl) assoc);
    }

    @Override
    public AssocModelImpl newAssocModel(JSONObject assoc) {
        try {
            return new AssocModelImpl(newDMXObjectModel(assoc), parsePlayer1(assoc), parsePlayer2(assoc));
        } catch (Exception e) {
            throw parsingFailed(assoc, e, "AssocModelImpl");
        }
    }

    // ---

    /**
     * @return  maybe null
     */
    private PlayerModelImpl parsePlayer1(JSONObject assoc) throws JSONException {
        return parsePlayer(assoc, "player1");
    }

    /**
     * @return  maybe null
     */
    private PlayerModelImpl parsePlayer2(JSONObject assoc) throws JSONException {
        return parsePlayer(assoc, "player2");
    }

    private PlayerModelImpl parsePlayer(JSONObject assoc, String key) throws JSONException {
        return assoc.has(key) ? _parsePlayer(assoc.getJSONObject(key)) : null;
    }

    private PlayerModelImpl _parsePlayer(JSONObject playerModel) {
        if (playerModel.has("topicId") || playerModel.has("topicUri")) {
            return newTopicPlayerModel(playerModel);
        } else if (playerModel.has("assocId")) {
            return newAssocPlayerModel(playerModel);
        } else {
            throw new RuntimeException("One of \"topicId\"/\"topicUri\"/\"assocId\" is expected");
        }
    }



    // === DMXObjectModel ===

    /**
     * @param   id          Optional (-1 is a valid value and represents "not set").
     * @param   uri         Optional (<code>null</code> is a valid value).
     * @param   typeUri     Mandatory in the context of a create operation.
     *                      Optional (<code>null</code> is a valid value) in the context of an update operation.
     * @param   value       Optional (<code>null</code> is a valid value).
     * @param   childTopics Optional (<code>null</code> is a valid value and is transformed into an empty composite).
     */
    DMXObjectModelImpl newDMXObjectModel(long id, String uri, String typeUri, SimpleValue value,
                                                                                         ChildTopicsModel childTopics) {
        return new DMXObjectModelImpl(id, uri, typeUri, value, (ChildTopicsModelImpl) childTopics, al());
    }

    DMXObjectModelImpl newDMXObjectModel(JSONObject object) throws JSONException {
        return newDMXObjectModel(
            object.optLong("id", -1),
            object.optString("uri", null),
            object.optString("typeUri", null),
            object.has("value") ? new SimpleValue(object.get("value")) : null,
            object.has("children") ? newChildTopicsModel(object.getJSONObject("children")) : null
        );
    }



    // === ChildTopicsModel ===

    @Override
    public ChildTopicsModelImpl newChildTopicsModel() {
        return new ChildTopicsModelImpl(new HashMap(), this);
    }

    @Override
    public ChildTopicsModelImpl newChildTopicsModel(JSONObject values) {
        try {
            Map<String, Object> childTopics = new HashMap();
            Iterator<String> i = values.keys();
            while (i.hasNext()) {
                String compDefUri = i.next();
                Object value = values.get(compDefUri);
                if (!(value instanceof JSONArray)) {
                    childTopics.put(compDefUri, createChildTopicModel(compDefUri, value));
                } else {
                    JSONArray valueArray = (JSONArray) value;
                    List<RelatedTopicModel> topics = new ArrayList();
                    childTopics.put(compDefUri, topics);
                    for (int j = 0; j < valueArray.length(); j++) {
                        topics.add(createChildTopicModel(compDefUri, valueArray.get(j)));
                    }
                }
            }
            return new ChildTopicsModelImpl(childTopics, this);
        } catch (Exception e) {
            throw parsingFailed(values, e, "ChildTopicsModelImpl");
        }
    }

    @Override
    public String childTypeUri(String compDefUri) {
        return compDefUri.split("#")[0];
    }

    private String assocTypeUri(String compDefUri) {
        String[] s = compDefUri.split("#");
        return s.length == 2 ? s[1] : COMPOSITION;
    }

    // ---

    /**
     * Creates a child topic model from a JSON value.
     *
     * Both topic serialization formats are supported:
     * 1) canonic format -- contains entire topic models.
     * 2) simplified format -- contains the topic value only (simple or composite).
     */
    private RelatedTopicModel createChildTopicModel(String compDefUri, Object value) throws JSONException {
        String childTypeUri = childTypeUri(compDefUri);
        if (value instanceof JSONObject) {
            JSONObject val = (JSONObject) value;
            // we detect the canonic format by checking for mandatory topic properties
            if (val.has("value") || val.has("children")) {
                // canonic format (topic or topic reference)
                AssocModel relatingAssoc = null;
                if (val.has("assoc")) {
                    JSONObject assoc = val.getJSONObject("assoc");
                    initTypeUri(assoc, assocTypeUri(compDefUri));
                    relatingAssoc = newAssocModel(assoc);
                }
                if (val.has("value")) {
                    RelatedTopicModel topicRef = createReferenceModel(val.get("value"), relatingAssoc);
                    if (topicRef != null) {
                        // for updating multi-refs the original ID must be preserved
                        if (topicRef instanceof TopicReferenceModelImpl && val.has("id")) {
                            ((TopicReferenceModelImpl) topicRef).originalId = val.getLong("id");
                        }
                        return topicRef;
                    }
                }
                initTypeUri(val, childTypeUri);
                TopicModel topic = newTopicModel(val);
                if (relatingAssoc != null) {
                    return newRelatedTopicModel(topic, relatingAssoc);
                } else {
                    return newRelatedTopicModel(topic);
                }
            } else {
                // simplified format (composite topic)
                return newRelatedTopicModel(newTopicModel(childTypeUri, newChildTopicsModel(val)));
            }
        } else {
            // simplified format (simple topic or topic reference)
            RelatedTopicModel topicRef = createReferenceModel(value, null);
            if (topicRef != null) {
                return topicRef;
            }
            // simplified format (simple topic)
            return newRelatedTopicModel(newTopicModel(childTypeUri, new SimpleValue(value)));
        }
    }

    private RelatedTopicModel createReferenceModel(Object value, AssocModel relatingAssoc) {
        if (value instanceof String) {
            String val = (String) value;
            if (val.startsWith(REF_ID_PREFIX)) {
                long topicId = refTopicId(val);
                if (relatingAssoc != null) {
                    return newTopicReferenceModel(topicId, relatingAssoc);
                } else {
                    return newTopicReferenceModel(topicId);
                }
            } else if (val.startsWith(REF_URI_PREFIX)) {
                String topicUri = refTopicUri(val);
                if (relatingAssoc != null) {
                    return newTopicReferenceModel(topicUri, relatingAssoc);
                } else {
                    return newTopicReferenceModel(topicUri);
                }
            } else if (val.startsWith(DEL_ID_PREFIX)) {
                return newTopicDeletionModel(delTopicId(val));
            } else if (val.startsWith(DEL_URI_PREFIX)) {
                return newTopicDeletionModel(delTopicUri(val));
            }
        }
        return null;
    }

    /**
     * @param   object      a topic or an assoc JSON
     */
    private void initTypeUri(JSONObject object, String typeUri) throws JSONException {
        if (!object.has("typeUri")) {
            object.put("typeUri", typeUri);
            return;
        }
        // sanity check
        String _typeUri = object.getString("typeUri");
        if (!_typeUri.equals(typeUri)) {
            throw new IllegalArgumentException("A \"" + typeUri + "\" update model has type \"" + _typeUri + "\"");
        }
    }

    // ---

    private long refTopicId(String val) {
        return Long.parseLong(val.substring(REF_ID_PREFIX.length()));
    }

    private String refTopicUri(String val) {
        return val.substring(REF_URI_PREFIX.length());
    }

    private long delTopicId(String val) {
        return Long.parseLong(val.substring(DEL_ID_PREFIX.length()));
    }

    private String delTopicUri(String val) {
        return val.substring(DEL_URI_PREFIX.length());
    }



    // === TopicPlayerModel ===

    @Override
    public TopicPlayerModelImpl newTopicPlayerModel(long topicId, String roleTypeUri) {
        return new TopicPlayerModelImpl(topicId, roleTypeUri, al());
    }

    @Override
    public TopicPlayerModelImpl newTopicPlayerModel(String topicUri, String roleTypeUri) {
        return new TopicPlayerModelImpl(topicUri, roleTypeUri, al());
    }

    @Override
    public TopicPlayerModelImpl newTopicPlayerModel(long topicId, String topicUri, String roleTypeUri) {
        return new TopicPlayerModelImpl(topicId, topicUri, roleTypeUri, al());
    }

    @Override
    public TopicPlayerModelImpl newTopicPlayerModel(JSONObject topicPlayer) {
        try {
            long topicId       = topicPlayer.optLong("topicId", -1);
            String topicUri    = topicPlayer.optString("topicUri", null);
            String roleTypeUri = topicPlayer.getString("roleTypeUri");
            //
            if (topicId == -1 && topicUri == null) {
                throw new IllegalArgumentException("Neiter \"topicId\" nor \"topicUri\" is set");
            }
            return newTopicPlayerModel(topicId, topicUri, roleTypeUri);
        } catch (Exception e) {
            throw parsingFailed(topicPlayer, e, "TopicPlayerModelImpl");
        }
    }



    // === AssocPlayerModel ===

    @Override
    public AssocPlayerModelImpl newAssocPlayerModel(long assocId, String roleTypeUri) {
        return new AssocPlayerModelImpl(assocId, roleTypeUri, al());
    }    

    @Override
    public AssocPlayerModelImpl newAssocPlayerModel(JSONObject assocPlayer) {
        try {
            long assocId       = assocPlayer.getLong("assocId");
            String roleTypeUri = assocPlayer.getString("roleTypeUri");
            return newAssocPlayerModel(assocId, roleTypeUri);
        } catch (Exception e) {
            throw parsingFailed(assocPlayer, e, "AssocPlayerModelImpl");
        }
    }    



    // === RoleTypeModel ===

    @Override
    public RoleTypeModelImpl newRoleTypeModel(TopicModel roleTypeTopic, ViewConfigModel viewConfig) {
        return new RoleTypeModelImpl((TopicModelImpl) roleTypeTopic, (ViewConfigModelImpl) viewConfig);
    }

    @Override
    public RoleTypeModelImpl newRoleTypeModel(JSONObject roleType) {
        try {
            String uri = roleType.optString("uri", null);
            SimpleValue value = new SimpleValue(roleType.getString("value"));
            return newRoleTypeModel(
                newTopicModel(uri, ROLE_TYPE, value),
                newViewConfigModel(roleType.optJSONArray("viewConfigTopics"))      // optJSONArray may return null
            );
        } catch (Exception e) {
            throw parsingFailed(roleType, e, "RoleTypeModelImpl");
        }
    }



    // === RelatedTopicModel ===

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(long topicId) {
        return new RelatedTopicModelImpl(newTopicModel(topicId), newAssocModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(long topicId, AssocModel relatingAssoc) {
        return new RelatedTopicModelImpl(newTopicModel(topicId), (AssocModelImpl) relatingAssoc);
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicUri) {
        return new RelatedTopicModelImpl(newTopicModel(topicUri, (String) null), newAssocModel());
                                                                 // topicTypeUri=null
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicUri, AssocModel relatingAssoc) {
        return new RelatedTopicModelImpl(newTopicModel(topicUri, (String) null), (AssocModelImpl) relatingAssoc);
                                                                 // topicTypeUri=null
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicTypeUri, SimpleValue value) {
        return new RelatedTopicModelImpl(newTopicModel(topicTypeUri, value), newAssocModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(String topicTypeUri, ChildTopicsModel childTopics) {
        return new RelatedTopicModelImpl(newTopicModel(topicTypeUri, childTopics), newAssocModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(TopicModel topic) {
        return new RelatedTopicModelImpl((TopicModelImpl) topic, newAssocModel());
    }

    @Override
    public RelatedTopicModelImpl newRelatedTopicModel(TopicModel topic, AssocModel relatingAssoc) {
        return new RelatedTopicModelImpl((TopicModelImpl) topic, (AssocModelImpl) relatingAssoc);
    }



    // === RelatedAssocModel ===

    @Override
    public RelatedAssocModelImpl newRelatedAssocModel(AssocModel assoc, AssocModel relatingAssoc) {
        return new RelatedAssocModelImpl((AssocModelImpl) assoc, (AssocModelImpl) relatingAssoc);
    }



    // === TopicReferenceModel ===

    @Override
    public TopicReferenceModel newTopicReferenceModel(long topicId) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicId));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(long topicId, AssocModel relatingAssoc) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicId, relatingAssoc));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(String topicUri) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicUri));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(String topicUri, AssocModel relatingAssoc) {
        return new TopicReferenceModelImpl(newRelatedTopicModel(topicUri, relatingAssoc));
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(long topicId, ChildTopicsModel relatingAssocChildTopics) {
        return new TopicReferenceModelImpl(
            newRelatedTopicModel(topicId, newAssocModel(relatingAssocChildTopics))
        );
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(String topicUri, ChildTopicsModel relatingAssocChildTopics) {
        return new TopicReferenceModelImpl(
            newRelatedTopicModel(topicUri, newAssocModel(relatingAssocChildTopics))
        );
    }

    @Override
    public TopicReferenceModel newTopicReferenceModel(Object topicIdOrUri) {
        RelatedTopicModelImpl relTopic;
        if (topicIdOrUri instanceof Long) {
            relTopic = newRelatedTopicModel((Long) topicIdOrUri);
        } else if (topicIdOrUri instanceof String) {
            relTopic = newRelatedTopicModel((String) topicIdOrUri);
        } else {
            throw new IllegalArgumentException("Tried to build a TopicReferenceModel from a " +
                topicIdOrUri.getClass().getName() + " (expected are String or Long)");
        }
        return new TopicReferenceModelImpl(relTopic);
    }



    // === TopicDeletionModel ===

    @Override
    public TopicDeletionModel newTopicDeletionModel(long topicId) {
        return new TopicDeletionModelImpl(newRelatedTopicModel(topicId));
    }

    @Override
    public TopicDeletionModel newTopicDeletionModel(String topicUri) {
        return new TopicDeletionModelImpl(newRelatedTopicModel(topicUri));
    }



    // === TopicTypeModel ===

    @Override
    public TopicTypeModelImpl newTopicTypeModel(TopicModel typeTopic, String dataTypeUri, List<CompDefModel> compDefs,
                                                ViewConfigModel viewConfig) {
        return new TopicTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, compDefs, (ViewConfigModelImpl) viewConfig));
    }

    @Override
    public TopicTypeModelImpl newTopicTypeModel(String uri, String value, String dataTypeUri) {
        return new TopicTypeModelImpl(newTypeModel(uri, TOPIC_TYPE, new SimpleValue(value), dataTypeUri));
    }

    @Override
    public TopicTypeModelImpl newTopicTypeModel(JSONObject topicType) {
        try {
            return new TopicTypeModelImpl(newTypeModel(topicType.put("typeUri", TOPIC_TYPE)));
        } catch (Exception e) {
            throw parsingFailed(topicType, e, "TopicTypeModelImpl");
        }
    }



    // === AssocTypeModel ===

    @Override
    public AssocTypeModelImpl newAssocTypeModel(TopicModel typeTopic, String dataTypeUri, List<CompDefModel> compDefs,
                                                ViewConfigModel viewConfig) {
        return new AssocTypeModelImpl(newTypeModel(typeTopic, dataTypeUri, compDefs, (ViewConfigModelImpl) viewConfig));
    }

    @Override
    public AssocTypeModelImpl newAssocTypeModel(String uri, String value, String dataTypeUri) {
        return new AssocTypeModelImpl(newTypeModel(uri, ASSOC_TYPE, new SimpleValue(value), dataTypeUri));
    }

    @Override
    public AssocTypeModelImpl newAssocTypeModel(JSONObject assocType) {
        try {
            return new AssocTypeModelImpl(newTypeModel(assocType.put("typeUri", ASSOC_TYPE)));
        } catch (Exception e) {
            throw parsingFailed(assocType, e, "AssocTypeModelImpl");
        }
    }



    // === TypeModel ===

    TypeModelImpl newTypeModel(TopicModel typeTopic, String dataTypeUri, List<CompDefModel> compDefs,
                                                                         ViewConfigModelImpl viewConfig) {
        return new TypeModelImpl((TopicModelImpl) typeTopic, dataTypeUri, compDefs, viewConfig);
    }

    TypeModelImpl newTypeModel(String uri, String typeUri, SimpleValue value, String dataTypeUri) {
        return new TypeModelImpl(newTopicModel(uri, typeUri, value), dataTypeUri, new ArrayList(),
            newViewConfigModel());
    }

    TypeModelImpl newTypeModel(JSONObject typeModel) throws JSONException {
        TopicModelImpl typeTopic = newTopicModel(typeModel);
        return new TypeModelImpl(typeTopic,
            typeModel.optString("dataTypeUri", null),
            parseCompDefs(typeModel.optJSONArray("compDefs"), typeTopic.getUri()),      // optJSONArray may return null
            newViewConfigModel(typeModel.optJSONArray("viewConfigTopics")));            // optJSONArray may return null
    }

    // ---

    private List<CompDefModel> parseCompDefs(JSONArray compDefs, String parentTypeUri) throws JSONException {
        List<CompDefModel> _compDefs = new ArrayList();
        if (compDefs != null) {
            for (int i = 0; i < compDefs.length(); i++) {
                JSONObject compDef = compDefs.getJSONObject(i)
                    .put("parentTypeUri", parentTypeUri);
                _compDefs.add(newCompDefModel(compDef));
            }
        }
        return _compDefs;
    }



    // === CompDefModel ===

    @Override
    public CompDefModelImpl newCompDefModel(String parentTypeUri, String childTypeUri,
                                            String childCardinalityUri) {
        return newCompDefModel(-1, null, null, false, false, parentTypeUri, childTypeUri, childCardinalityUri, null);
    }

    @Override
    public CompDefModelImpl newCompDefModel(String parentTypeUri, String childTypeUri,
                                            String childCardinalityUri,
                                            ViewConfigModel viewConfig) {
        return newCompDefModel(-1, null, null, false, false, parentTypeUri, childTypeUri, childCardinalityUri,
            viewConfig);
    }

    @Override
    public CompDefModelImpl newCompDefModel(String customAssocTypeUri,
                                            boolean isIdentityAttr, boolean includeInLabel,
                                            String parentTypeUri, String childTypeUri,
                                            String childCardinalityUri) {
        return newCompDefModel(-1, null, customAssocTypeUri, isIdentityAttr, includeInLabel,
            parentTypeUri, childTypeUri, childCardinalityUri, null);
    }

    /**
     * @param   assoc   the underlying association.
     *                  IMPORTANT: the association must identify its players <i>by URI</i> (not by ID). ### still true?
     */
    @Override
    public CompDefModelImpl newCompDefModel(AssocModel assoc, ViewConfigModel viewConfig) {
        return new CompDefModelImpl((AssocModelImpl) assoc, (ViewConfigModelImpl) viewConfig);
    }

    @Override
    public CompDefModelImpl newCompDefModel(JSONObject compDef) {
        try {
            PlayerModel player1 = parsePlayer1(compDef);     // may be null
            PlayerModel player2 = parsePlayer2(compDef);     // may be null
            // Note: the canonic comp def JSON format does not require explicit assoc players. Comp defs declared in
            // JSON migrations support a simplified format. In contrast comp defs contained in a request may include
            // explicit assoc players already. In that case we use these ones as they contain both, the ID-ref and the
            // URI-ref. In specific situations one or the other is needed.
            return new CompDefModelImpl(
                newAssocModel(compDef.optLong("id", -1), null,      // uri=null
                    COMPOSITION_DEF,
                    player1 != null ? player1 : parentPlayer(compDef.getString("parentTypeUri")),
                    player2 != null ? player2 : childPlayer(compDef.getString("childTypeUri")),
                    null, childTopics(compDef)                      // value=null
                ),
                newViewConfigModel(compDef.optJSONArray("viewConfigTopics"))
            );
        } catch (Exception e) {
            throw parsingFailed(compDef, e, "CompDefModelImpl");
        }
    }

    /**
     * Internal.
     */
    CompDefModelImpl newCompDefModel(long id, String uri, String customAssocTypeUri,
                                     boolean isIdentityAttr, boolean includeInLabel,
                                     String parentTypeUri, String childTypeUri,
                                     String childCardinalityUri,
                                     ViewConfigModel viewConfig) {
        return new CompDefModelImpl(
            newAssocModel(id, uri, COMPOSITION_DEF, parentPlayer(parentTypeUri), childPlayer(childTypeUri),
                null, childTopics(childCardinalityUri, customAssocTypeUri, isIdentityAttr, includeInLabel) // value=null
            ),
            (ViewConfigModelImpl) viewConfig
        );
    }

    /**
     * Internal.
     */
    CompDefModelImpl newCompDefModel(ChildTopicsModel childTopics) {
        return new CompDefModelImpl(newAssocModel(COMPOSITION_DEF, childTopics));
    }

    // ---

    private TopicPlayerModel parentPlayer(String parentTypeUri) {
        return newTopicPlayerModel(parentTypeUri, PARENT_TYPE);
    }

    private TopicPlayerModel childPlayer(String childTypeUri) {
        return newTopicPlayerModel(childTypeUri, CHILD_TYPE);
    }

    // ---

    private ChildTopicsModel childTopics(JSONObject compDef) throws JSONException {
        return childTopics(
            compDef.getString("childCardinalityUri"),
            // Note: getString()/optString() on a key with JSON null value would return the string "null"
            compDef.isNull("customAssocTypeUri") ? null : compDef.getString("customAssocTypeUri"),
            compDef.optBoolean("isIdentityAttr"),
            compDef.optBoolean("includeInLabel")
        );
    }

    private ChildTopicsModel childTopics(String cardinalityUri, String customAssocTypeUri, boolean isIdentityAttr,
                                         boolean includeInLabel) {
        ChildTopicsModel childTopics = newChildTopicsModel()
            .setRef(CARDINALITY, cardinalityUri)
            .set(IDENTITY_ATTR, isIdentityAttr)
            .set(INCLUDE_IN_LABEL, includeInLabel);
        //
        if (customAssocTypeUri != null) {
            if (customAssocTypeUri.startsWith(DEL_URI_PREFIX)) {
                childTopics.setDeletionRef(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE, delTopicUri(customAssocTypeUri));
            } else {
                childTopics.setRef(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE, customAssocTypeUri);
            }
        }
        //
        return childTopics;
    }



    // === ViewConfigModel ===

    @Override
    public ViewConfigModelImpl newViewConfigModel() {
        return new ViewConfigModelImpl(new HashMap(), al());
    }    

    @Override
    public ViewConfigModelImpl newViewConfigModel(Iterable<? extends TopicModel> configTopics) {
        Map<String, TopicModelImpl> _configTopics = new HashMap();
        for (TopicModel configTopic : configTopics) {
            _configTopics.put(configTopic.getTypeUri(), (TopicModelImpl) configTopic);
        }
        return new ViewConfigModelImpl(_configTopics, al());
    }    

    /**
     * @param   configTopics    may be null
     */
    @Override
    public ViewConfigModelImpl newViewConfigModel(JSONArray configTopics) {
        try {
            Map<String, TopicModelImpl> _configTopics = new HashMap();
            if (configTopics != null) {
                for (int i = 0; i < configTopics.length(); i++) {
                    TopicModelImpl configTopic = newTopicModel(configTopics.getJSONObject(i));
                    _configTopics.put(configTopic.getTypeUri(), configTopic);
                }
            }
            return new ViewConfigModelImpl(_configTopics, al());
        } catch (Exception e) {
            throw parsingFailed(configTopics, e, "ViewConfigModelImpl");
        }
    }    



    // === Topicmaps ===

    @Override
    public ViewTopic newViewTopic(TopicModel topic, ViewProps viewProps) {
        return new ViewTopicImpl((TopicModelImpl) topic, viewProps);
    }

    @Override
    public ViewAssoc newViewAssoc(AssocModel assoc, ViewProps viewProps) {
        return new ViewAssocImpl((AssocModelImpl) assoc, viewProps);
    }

    @Override
    public ViewProps newViewProps() {
        return new ViewPropsImpl();
    }

    @Override
    public ViewProps newViewProps(int x, int y) {
        return new ViewPropsImpl(x, y);
    }

    @Override
    public ViewProps newViewProps(int x, int y, boolean visibility, boolean pinned) {
        return new ViewPropsImpl(x, y, visibility, pinned);
    }

    @Override
    public ViewProps newViewProps(boolean visibility) {
        return new ViewPropsImpl(visibility);
    }

    @Override
    public ViewProps newViewProps(boolean visibility, boolean pinned) {
        return new ViewPropsImpl(visibility, pinned);
    }

    @Override
    public ViewProps newViewProps(JSONObject viewProps) {
        return new ViewPropsImpl(viewProps);
    }



    // === Facets ===

    @Override
    public FacetValueModel newFacetValueModel(String childTypeUri) {
        return new FacetValueModelImpl(childTypeUri, this);
    }

    @Override
    public FacetValueModel newFacetValueModel(JSONObject facetValue) {
        try {
            ChildTopicsModelImpl childTopics = newChildTopicsModel(facetValue);
            if (childTopics.size() != 1) {
                throw new RuntimeException("There are " + childTopics.size() + " child topic entries (expected is 1)");
            }
            return new FacetValueModelImpl(childTopics);
        } catch (Exception e) {
            throw parsingFailed(facetValue, e, "FacetValueModelImpl");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private RuntimeException parsingFailed(JSONObject o, Exception e, String className) {
        try {
            return new RuntimeException("JSON parsing failed, " + className + " " + o.toString(4), e);
        } catch (JSONException je) {
            // fallback: no prettyprinting
            return new RuntimeException("JSON parsing failed, " + className + " " + o, e);
        }
    }

    private RuntimeException parsingFailed(JSONArray a, Exception e, String className) {
        try {
            return new RuntimeException("JSON parsing failed, " + className + " " + a.toString(4), e);
        } catch (JSONException je) {
            // fallback: no prettyprinting
            return new RuntimeException("JSON parsing failed, " + className + " " + a, e);
        }
    }

    // ---

    private AccessLayer al() {
        if (al == null) {
            throw new RuntimeException("Before using the ModelFactory an AccessLayer must be set");
        }
        return al;
    }
}
