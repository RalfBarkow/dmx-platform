package systems.dmx.core;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.SimpleValue;

import java.util.List;



/**
 * The (abstract) base class of both, {@link Topic} and {@link Assoc}.
 * <p>
 * A <code>DMXObject</code> has 5 parts: an <b>ID</b>, an <b>URI</b>, a <b>type URI</b>, a {@link SimpleValue}, and,
 * in case of a <i>composite</i> DMXObject, a (recursive) collection of {@link ChildTopics} objects.
 * <p>
 * <code>DMXObject</code> provides methods for updating, deleting, and traversal alongside associations.
 */
public interface DMXObject extends Identifiable, JSONEnabled {



    // === Model ===

    // --- ID ---

    long getId();

    // --- URI ---

    String getUri();

    void setUri(String uri);

    // --- Type URI ---

    String getTypeUri();

    void setTypeUri(String typeUri);

    // --- Simple Value ---

    SimpleValue getSimpleValue();

    void setSimpleValue(String value);
    void setSimpleValue(int value);
    void setSimpleValue(long value);
    void setSimpleValue(boolean value);
    void setSimpleValue(SimpleValue value);

    // --- Child Topics ---

    ChildTopics getChildTopics();

    // ---

    <O extends DMXObject> O loadChildTopics();
    <O extends DMXObject> O loadChildTopics(String compDefUri);

    // ---

    /**
     * Returns the type of this object.
     * <p>
     * No access control is performed as <i>Implicit READ permission</i> applies: if a user has READ access to an object
     * she has READ access to its type as well.
     * <p>
     * Note: if the user would have no READ access to this object the DMX Core would not instantiate it in the
     * first place, but throw an <code>AccessControlException</code>.
     */
    DMXType getType();

    DMXObjectModel getModel();



    // === Updating ===

    <M extends DMXObjectModel> void update(M updateModel);

    /**
     * Convenience that constructs a DMXObjectModel from a ChildTopicsModel and calls canonic update() with it.
     */
    void update(ChildTopicsModel updateModel);

    /**
     * Note: this method is meant only for facet updates.
     */
    void updateChildTopics(ChildTopicsModel updateModel, CompDef compDef);



    // === Deletion ===

    /**
     * Deletes the DMX object in its entirety, that is
     * - the object itself (the <i>parent</i>)
     * - all child topics associated via "dmx.core.composition", recusively
     * - all the remaining direct associations, e.g. "dmx.core.instantiation"
     */
    void delete();



    // === Traversal ===

    // --- Topic Retrieval ---

    /**
     * Fetches and returns a related topic or <code>null</code> if no such topic extists.
     *
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                 String othersTopicTypeUri);

    List<RelatedTopic> getRelatedTopics();

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                        String othersTopicTypeUri);

    // --- Assoc Retrieval ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    RelatedAssoc getRelatedAssoc(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                 String othersAssocTypeUri);

    List<RelatedAssoc> getRelatedAssocs();

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    List<RelatedAssoc> getRelatedAssocs(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                        String othersAssocTypeUri);

    // ---

    Assoc getAssoc(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri, long othersTopicId);

    /**
     * Fetches all associations this object is a player in.
     */
    List<Assoc> getAssocs();



    // === Properties ===

    /**
     * Returns this object's property value associated with the given property URI.
     * If there's no property value associated with the property URI an exception is thrown.
     */
    Object getProperty(String propUri);

    /**
     * Checks whether for this object a property value is associated with a given property URI.
     */
    boolean hasProperty(String propUri);

    void setProperty(String propUri, Object propValue, boolean addToIndex);

    /**
     * Removes this object's property associated with the given property URI.
     * If there's no property value associated with the property URI nothing is performed.
     */
    void removeProperty(String propUri);



    // === Permissions ===

    /**
     * Checks if current user has write access to this object.
     *
     * @throws  AccessControlException
     */
    void checkWriteAccess();



    // === Misc ===

    Object getDatabaseVendorObject();
}
