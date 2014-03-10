package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.CompositeValue;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A composite value model that is attached to the DB.
 */
class AttachedCompositeValue implements CompositeValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private CompositeValueModel model;                          // underlying model

    private AttachedDeepaMehtaObject parent;                    // attached object cache

    /**
     * Attached object cache.
     * Key: child type URI (String), value: AttachedTopic or List<AttachedTopic>
     */
    private Map<String, Object> childTopics = new HashMap();    // attached object cache

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedCompositeValue(CompositeValueModel model, AttachedDeepaMehtaObject parent, EmbeddedService dms) {
        this.model = model;
        this.parent = parent;
        this.dms = dms;
        initAttachedObjectCache();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************************
    // *** CompositeValue Implementation ***
    // *************************************



    // === Accessors ===

    @Override
    public Topic getTopic(String childTypeUri) {
        loadChildTopics(childTypeUri);
        return _getTopic(childTypeUri);
    }

    @Override
    public Topic getTopic(String childTypeUri, Topic defaultTopic) {
        loadChildTopics(childTypeUri);
        return _getTopic(childTypeUri, (AttachedTopic) defaultTopic);
    }

    // ---

    @Override
    public List<Topic> getTopics(String childTypeUri) {
        loadChildTopics(childTypeUri);
        return _getTopics(childTypeUri);
    }

    @Override
    public List<Topic> getTopics(String childTypeUri, List<Topic> defaultValue) {
        loadChildTopics(childTypeUri);
        return _getTopics(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public Object get(String childTypeUri) {
        return childTopics.get(childTypeUri);
    }

    @Override
    public boolean has(String childTypeUri) {
        return childTopics.containsKey(childTypeUri);
    }

    @Override
    public Iterable<String> childTypeUris() {
        return childTopics.keySet();
    }

    @Override
    public int size() {
        return childTopics.size();
    }

    // ---

    @Override
    public CompositeValueModel getModel() {
        return model;
    }



    // === Convenience Accessors ===

    @Override
    public String getString(String childTypeUri) {
        return getModel().getString(childTypeUri);
    }

    @Override
    public String getString(String childTypeUri, String defaultValue) {
        return getModel().getString(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public int getInt(String childTypeUri) {
        return getModel().getInt(childTypeUri);
    }

    @Override
    public int getInt(String childTypeUri, int defaultValue) {
        return getModel().getInt(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public long getLong(String childTypeUri) {
        return getModel().getLong(childTypeUri);
    }

    @Override
    public long getLong(String childTypeUri, long defaultValue) {
        return getModel().getLong(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public double getDouble(String childTypeUri) {
        return getModel().getDouble(childTypeUri);
    }

    @Override
    public double getDouble(String childTypeUri, double defaultValue) {
        return getModel().getDouble(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public boolean getBoolean(String childTypeUri) {
        return getModel().getBoolean(childTypeUri);
    }

    @Override
    public boolean getBoolean(String childTypeUri, boolean defaultValue) {
        return getModel().getBoolean(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public Object getObject(String childTypeUri) {
        return getModel().getObject(childTypeUri);
    }

    @Override
    public Object getObject(String childTypeUri, Object defaultValue) {
        return getModel().getObject(childTypeUri, defaultValue);
    }

    // ---

    @Override
    public CompositeValue getCompositeValue(String childTypeUri) {
        return getTopic(childTypeUri).getCompositeValue();
    }

    @Override
    public CompositeValue getCompositeValue(String childTypeUri, CompositeValue defaultValue) {
        Topic topic = getTopic(childTypeUri, null);
        return topic != null ? topic.getCompositeValue() : defaultValue;
    }

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    @Override
    public CompositeValue set(String childTypeUri, TopicModel value, ClientState clientState, Directives directives) {
        return _update(childTypeUri, value, clientState, directives);
    }

    @Override
    public CompositeValue set(String childTypeUri, Object value, ClientState clientState, Directives directives) {
        return _update(childTypeUri, new TopicModel(childTypeUri, new SimpleValue(value)), clientState, directives);
    }

    @Override
    public CompositeValue set(String childTypeUri, CompositeValueModel value, ClientState clientState,
                                                                              Directives directives) {
        return _update(childTypeUri, new TopicModel(childTypeUri, value), clientState, directives);
    }

    // ---

    @Override
    public CompositeValue setRef(String childTypeUri, long refTopicId, ClientState clientState, Directives directives) {
        return _update(childTypeUri, new TopicReferenceModel(refTopicId), clientState, directives);
    }

    @Override
    public CompositeValue setRef(String childTypeUri, String refTopicUri, ClientState clientState,
                                                                          Directives directives) {
        return _update(childTypeUri, new TopicReferenceModel(refTopicUri), clientState, directives);
    }

    // ---

    @Override
    public CompositeValue remove(String childTypeUri, long topicId, ClientState clientState, Directives directives) {
        return _update(childTypeUri, new TopicDeletionModel(topicId), clientState, directives);
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void update(CompositeValueModel newComp, ClientState clientState, Directives directives) {
        try {
            for (AssociationDefinition assocDef : parent.getType().getAssocDefs()) {
                String childTypeUri   = assocDef.getChildTypeUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                TopicModel newChildTopic        = null;     // only used for "one"
                List<TopicModel> newChildTopics = null;     // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = newComp.getTopic(childTypeUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = newComp.getTopics(childTypeUri, null);      // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                updateChildTopics(newChildTopic, newChildTopics, assocDef, clientState, directives);
            }
            //
            dms.valueStorage.refreshLabel(parent.getModel());
            //
        } catch (Exception e) {
            throw new RuntimeException("Updating composite value of " + parent.className() + " " + parent.getId() +
                " failed (newComp=" + newComp + ")", e);
        }
    }

    void updateChildTopics(TopicModel newChildTopic, List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                                      ClientState clientState, Directives directives) {
        // Note: updating the child topics requires them to be loaded
        loadChildTopics(assocDef);
        //
        String assocTypeUri = assocDef.getTypeUri();
        boolean one = newChildTopic != null;
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                updateCompositionOne(newChildTopic, assocDef, clientState, directives);
            } else {
                updateCompositionMany(newChildTopics, assocDef, clientState, directives);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                updateAggregationOne(newChildTopic, assocDef, clientState, directives);
            } else {
                updateAggregationMany(newChildTopics, assocDef, clientState, directives);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    // ---

    void loadChildTopics(String childTypeUri) {
        loadChildTopics(getAssocDef(childTypeUri));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    // --- Access this attached object cache ---

    private Topic _getTopic(String childTypeUri) {
        Topic topic = (Topic) childTopics.get(childTypeUri);
        // error check
        if (topic == null) {
            throw new RuntimeException("Child topic of type \"" + childTypeUri + "\" not found in " + childTopics);
        }
        //
        return topic;
    }

    private AttachedTopic _getTopic(String childTypeUri, AttachedTopic defaultTopic) {
        AttachedTopic topic = (AttachedTopic) childTopics.get(childTypeUri);
        return topic != null ? topic : defaultTopic;
    }

    // ---

    private List<Topic> _getTopics(String childTypeUri) {
        try {
            List<Topic> topics = (List<Topic>) childTopics.get(childTypeUri);
            // error check
            if (topics == null) {
                throw new RuntimeException("Child topics of type \"" + childTypeUri + "\" not found in " + childTopics);
            }
            //
            return topics;
        } catch (ClassCastException e) {
            getModel().throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    private List<Topic> _getTopics(String childTypeUri, List<Topic> defaultValue) {
        try {
            List<Topic> topics = (List<Topic>) childTopics.get(childTypeUri);
            return topics != null ? topics : defaultValue;
        } catch (ClassCastException e) {
            getModel().throwInvalidAccess(childTypeUri, e);
            return null;    // never reached
        }
    }

    // ---

    private CompositeValue _update(String childTypeUri, TopicModel newChildTopic, ClientState clientState,
                                                                                  Directives directives) {
        // regard parent object as updated
        parent.addUpdateDirective(directives);
        //
        updateChildTopics(newChildTopic, null, getAssocDef(childTypeUri), clientState, directives);
        dms.valueStorage.refreshLabel(parent.getModel());                         // newChildTopics=null
        return this;
    }

    // --- Composition ---

    private void updateCompositionOne(TopicModel newChildTopic, AssociationDefinition assocDef,
                                                                ClientState clientState, Directives directives) {
        AttachedTopic childTopic = _getTopic(assocDef.getChildTypeUri(), null);
        // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
        // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
        if (childTopic != null) {
            // == update child ==
            // update DB
            childTopic._update(newChildTopic, clientState, directives);
            // Note: memory is already up-to-date. The child topic is updated in-place of parent.
        } else {
            // == create child ==
            createChildTopicOne(newChildTopic, assocDef, clientState, directives);
        }
    }

    private void updateCompositionMany(List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                        ClientState clientState, Directives directives) {
        for (TopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                Topic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    // Note: "delete child" is an idempotent operation. A delete request for an child which has been
                    // deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                    continue;
                }
                // == delete child ==
                // update DB
                childTopic.delete(directives);
                // update memory
                removeFromCompositeValue(childTopic, assocDef);
            } else if (childTopicId != -1) {
                // == update child ==
                updateChildTopicMany(newChildTopic, assocDef, clientState, directives);
            } else {
                // == create child ==
                createChildTopicMany(newChildTopic, assocDef, clientState, directives);
            }
        }
    }

    // --- Aggregation ---

    private void updateAggregationOne(TopicModel newChildTopic, AssociationDefinition assocDef,
                                                                ClientState clientState, Directives directives) {
        RelatedTopic childTopic = (RelatedTopic) _getTopic(assocDef.getChildTypeUri(), null);
        if (newChildTopic instanceof TopicReferenceModel) {
            if (childTopic != null) {
                if (((TopicReferenceModel) newChildTopic).isReferingTo(childTopic)) {
                    return;
                }
                // == update assignment ==
                // update DB
                childTopic.getRelatingAssociation().delete(directives);
            } else {
                // == create assignment ==
            }
            // update DB
            Topic topic = dms.valueStorage.associateReferencedChildTopic(parent.getModel(),
                (TopicReferenceModel) newChildTopic, assocDef, clientState);
            // update memory
            putInCompositeValue(topic, assocDef);
        } else if (newChildTopic.getId() != -1) {
            // == update child ==
            updateChildTopicOne(newChildTopic, assocDef, clientState, directives);
        } else {
            // == create child ==
            // update DB
            if (childTopic != null) {
                childTopic.getRelatingAssociation().delete(directives);
            }
            createChildTopicOne(newChildTopic, assocDef, clientState, directives);
        }
    }

    private void updateAggregationMany(List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                        ClientState clientState, Directives directives) {
        for (TopicModel newChildTopic : newChildTopics) {
            long childTopicId = newChildTopic.getId();
            if (newChildTopic instanceof TopicDeletionModel) {
                RelatedTopic childTopic = findChildTopic(childTopicId, assocDef);
                if (childTopic == null) {
                    // Note: "delete assignment" is an idempotent operation. A delete request for an assignment which
                    // has been deleted already (resp. is non-existing) is not an error. Instead, nothing is performed.
                    continue;
                }
                // == delete assignment ==
                // update DB
                childTopic.getRelatingAssociation().delete(directives);
                // update memory
                removeFromCompositeValue(childTopic, assocDef);
            } else if (newChildTopic instanceof TopicReferenceModel) {
                if (isReferingToAny((TopicReferenceModel) newChildTopic, assocDef)) {
                    // Note: "create assignment" is an idempotent operation. A create request for an assignment which
                    // exists already is not an error. Instead, nothing is performed.
                    continue;
                }
                // == create assignment ==
                // update DB
                Topic topic = dms.valueStorage.associateReferencedChildTopic(parent.getModel(),
                    (TopicReferenceModel) newChildTopic, assocDef, clientState);
                // update memory
                addToCompositeValue(topic, assocDef);
            } else if (childTopicId != -1) {
                // == update child ==
                updateChildTopicMany(newChildTopic, assocDef, clientState, directives);
            } else {
                // == create child ==
                createChildTopicMany(newChildTopic, assocDef, clientState, directives);
            }
        }
    }

    // ---

    private void updateChildTopicOne(TopicModel newChildTopic, AssociationDefinition assocDef, ClientState clientState,
                                                                                               Directives directives) {
        AttachedTopic childTopic = _getTopic(assocDef.getChildTypeUri(), null);
        if (childTopic != null && childTopic.getId() == newChildTopic.getId()) {
            // update DB
            childTopic._update(newChildTopic, clientState, directives);
            // Note: memory is already up-to-date. The child topic is updated in-place of parent.
        } else {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parent.className() + " " + parent.getId() + " according to " + assocDef);
        }
    }

    private void updateChildTopicMany(TopicModel newChildTopic, AssociationDefinition assocDef, ClientState clientState,
                                                                                                Directives directives) {
        AttachedTopic childTopic = findChildTopic(newChildTopic.getId(), assocDef);
        if (childTopic != null) {
            // update DB
            childTopic._update(newChildTopic, clientState, directives);
            // Note: memory is already up-to-date. The child topic is updated in-place of parent.
        } else {
            throw new RuntimeException("Topic " + newChildTopic.getId() + " is not a child of " +
                parent.className() + " " + parent.getId() + " according to " + assocDef);
        }
    }

    // ---

    private void createChildTopicOne(TopicModel newChildTopic, AssociationDefinition assocDef, ClientState clientState,
                                                                                               Directives directives) {
        // update DB
        Topic childTopic = dms.createTopic(newChildTopic, clientState);
        dms.valueStorage.associateChildTopic(parent.getModel(), childTopic.getId(), assocDef, clientState);
        // update memory
        putInCompositeValue(childTopic, assocDef);
    }

    private void createChildTopicMany(TopicModel newChildTopic, AssociationDefinition assocDef, ClientState clientState,
                                                                                               Directives directives) {
        // update DB
        Topic childTopic = dms.createTopic(newChildTopic, clientState);
        dms.valueStorage.associateChildTopic(parent.getModel(), childTopic.getId(), assocDef, clientState);
        // update memory
        addToCompositeValue(childTopic, assocDef);
    }



    // ===

    void loadChildTopics() {
        dms.valueStorage.fetchCompositeValue(parent.getModel());
        initAttachedObjectCache();
    }

    /**
     * Recursively loads child topics (model) and updates this attached object cache accordingly.
     * If the child topics are loaded already nothing is performed.
     *
     * @param   assocDef    the child topics according to this association definition are loaded.
     *                      <p>
     *                      Note: the association definition must not necessarily originate from this object's
     *                      type definition. It may originate from a facet definition as well.
     */
    private void loadChildTopics(AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        if (!has(childTypeUri)) {
            logger.fine("### Lazy-loading \"" + childTypeUri + "\" child topic(s) of " + parent.className() + " " +
                parent.getId());
            dms.valueStorage.fetchChildTopics(parent.getModel(), assocDef);
            initAttachedObjectCache(childTypeUri);
        }
    }



    // === Attached Object Cache Initialization ===

    /**
     * Initializes this attached object cache. For all childs contained in the underlying model attached topics are
     * created and put in the attached object cache (recursively).
     */
    private void initAttachedObjectCache() {
        for (String childTypeUri : model) {
            initAttachedObjectCache(childTypeUri);
        }
    }

    /**
     * Initializes this attached object cache selectively (and recursively).
     */
    private void initAttachedObjectCache(String childTypeUri) {
        Object value = model.get(childTypeUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return;
        }
        // Note: no direct recursion takes place here. Recursion is indirect: attached topics are created here, this
        // implies creating further attached composite values, which in turn calls this method again but for the next
        // child-level. Finally attached topics are created for all child-levels.
        if (value instanceof TopicModel) {
            TopicModel childTopic = (TopicModel) value;
            childTopics.put(childTypeUri, createAttachedObject(childTopic));
        } else if (value instanceof List) {
            List<Topic> topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
            for (TopicModel childTopic : (List<TopicModel>) value) {
                topics.add(createAttachedObject(childTopic));
            }
        } else {
            throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
        }
    }

    /**
     * Creates an attached topic to be put in this attached object cache.
     */
    private Topic createAttachedObject(TopicModel model) {
        if (model instanceof RelatedTopicModel) {
            // Note: composite value models obtained through *fetching* contain *related topic models*.
            // We exploit the related topics when updating assignments (in conjunction with aggregations).
            // See updateAggregationOne() and updateAggregationMany().
            return new AttachedRelatedTopic((RelatedTopicModel) model, dms);
        } else {
            // Note: composite value models for *new topics* to be created contain sole *topic models*.
            return new AttachedTopic(model, dms);
        }
    }



    // === Update ===

    // --- Update this attached object cache + underlying model ---

    /**
     * For single-valued childs
     */
    private void putInCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        put(childTypeUri, childTopic);                              // attached object cache
        getModel().put(childTypeUri, childTopic.getModel());        // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void addToCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        add(childTypeUri, childTopic);                              // attached object cache
        getModel().add(childTypeUri, childTopic.getModel());        // underlying model
    }

    /**
     * For multiple-valued childs
     */
    private void removeFromCompositeValue(Topic childTopic, AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getChildTypeUri();
        remove(childTypeUri, childTopic);                           // attached object cache
        getModel().remove(childTypeUri, childTopic.getModel());     // underlying model
    }

    // --- Update this attached object cache ---

    /**
     * Puts a single-valued child. An existing value is overwritten.
     */
    private void put(String childTypeUri, Topic topic) {
        childTopics.put(childTypeUri, topic);
    }

    /**
     * Adds a value to a multiple-valued child.
     */
    private void add(String childTypeUri, Topic topic) {
        List<Topic> topics = _getTopics(childTypeUri, null);        // defaultValue=null
        // Note: topics just created have no child topics yet
        if (topics == null) {
            topics = new ArrayList();
            childTopics.put(childTypeUri, topics);
        }
        topics.add(topic);
    }

    /**
     * Removes a value from a multiple-valued child.
     */
    private void remove(String childTypeUri, Topic topic) {
        List<Topic> topics = _getTopics(childTypeUri, null);        // defaultValue=null
        if (topics != null) {
            topics.remove(topic);
        }
    }



    // === Helper ===

    private AttachedRelatedTopic findChildTopic(long childTopicId, AssociationDefinition assocDef) {
        List<Topic> childTopics = _getTopics(assocDef.getChildTypeUri(), new ArrayList());
        for (Topic childTopic : childTopics) {
            if (childTopic.getId() == childTopicId) {
                return (AttachedRelatedTopic) childTopic;
            }
        }
        return null;
    }

    /**
     * Checks weather the given topic reference refers to any of the child topics.
     *
     * @param   assocDef    the child topics according to this association definition are considered.
     */
    private boolean isReferingToAny(TopicReferenceModel topicRef, AssociationDefinition assocDef) {
        return topicRef.isReferingToAny(_getTopics(assocDef.getChildTypeUri(), new ArrayList()));
    }

    private AssociationDefinition getAssocDef(String childTypeUri) {
        // Note: doesn't work for facets
        return parent.getType().getAssocDef(childTypeUri);
    }
}
