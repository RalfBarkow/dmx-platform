package systems.dmx.core.model;

import systems.dmx.core.JSONEnabled;

import java.util.List;



/**
 * The data that underly a {@link ChildTopics} object.
 */
public interface ChildTopicsModel extends JSONEnabled, Iterable<String> {



    // === Accessors ===

    /**
     * Accesses a single-valued child.
     * Throws if there is no such child.
     */
    RelatedTopicModel getTopic(String compDefUri);

    /**
     * Accesses a single-valued child.
     * Returns <code>null</code> if there is no such child.
     */
    RelatedTopicModel getTopicOrNull(String compDefUri);

    /**
     * Accesses a multiple-valued child.
     * Throws if there is no such child. ### TODO: explain why not return an empty list instead
     */
    List<? extends RelatedTopicModel> getTopics(String compDefUri);

    /**
     * Accesses a multiple-valued child.
     * Returns <code>null</code> if there is no such child. ### TODO: explain why not return an empty list instead
     */
    List<? extends RelatedTopicModel> getTopicsOrNull(String compDefUri);

    // ---

    /**
     * Accesses a child generically, regardless of single-valued or multiple-valued.
     * Returns null if there is no such child.
     *
     * @return  A RelatedTopicModel or List<RelatedTopicModel>, or null if there is no such child.
     */
    Object get(String compDefUri);

    /**
     * Checks if a child is contained in this ChildTopicsModel.
     */
    boolean has(String compDefUri);

    /**
     * Returns the number of children contained in this ChildTopicsModel.
     * Multiple-valued children count as one.
     */
    int size();



    // === Convenience Accessors ===

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    String getString(String compDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    String getString(String compDefUri, String defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    int getInt(String compDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    int getInt(String compDefUri, int defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    long getLong(String compDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    long getLong(String compDefUri, long defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    double getDouble(String compDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    double getDouble(String compDefUri, double defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    boolean getBoolean(String compDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    boolean getBoolean(String compDefUri, boolean defaultValue);

    // ---

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Throws if the child doesn't exist.
     *
     * @return  String, Integer, Long, Double, or Boolean. Never null.
     */
    Object getValue(String compDefUri);

    /**
     * Convenience accessor for the *simple* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     *
     * @return  String, Integer, Long, Double, or Boolean. Never null.
     */
    Object getValue(String compDefUri, Object defaultValue);

    // ---

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Throws if the child doesn't exist.
     */
    ChildTopicsModel getChildTopics(String compDefUri);

    /**
     * Convenience accessor for the *composite* value of a single-valued child.
     * Returns a default value if the child doesn't exist.
     */
    ChildTopicsModel getChildTopics(String compDefUri, ChildTopicsModel defaultValue);

    // Note: there are no convenience accessors for a multiple-valued child.



    // === Manipulators ===

    // --- Single-valued Children ---

    /**
     * Puts a value in a single-valued child.
     * An existing value is overwritten.
     */
    ChildTopicsModel set(String compDefUri, RelatedTopicModel value);

    ChildTopicsModel set(String compDefUri, TopicModel value);

    /**
     * Convenience method to put a *simple* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @param   value   the simple value: a String, Integer, Long, Double, or a Boolean.
     *                  Primitive values are auto-boxed.
     *
     * @return  this ChildTopicsModel.
     */
    ChildTopicsModel set(String compDefUri, Object value);

    /**
     * Convenience method to put a *composite* value in a single-valued child.
     * An existing value is overwritten.
     *
     * @return  this ChildTopicsModel.
     */
    ChildTopicsModel set(String compDefUri, ChildTopicsModel value);

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    ChildTopicsModel setRef(String compDefUri, long refTopicId);

    /**
     * Puts a by-URI topic reference in a single-valued child.
     * An existing reference is overwritten.
     */
    ChildTopicsModel setRef(String compDefUri, String refTopicUri);

    // ---

    /**
     * Puts a by-ID topic deletion reference in a single-valued child.
     * An existing value is overwritten.
     */
    ChildTopicsModel setDeletionRef(String compDefUri, long refTopicId);

    /**
     * Puts a by-URI topic deletion reference in a single-valued child.
     * An existing value is overwritten.
     */
    ChildTopicsModel setDeletionRef(String compDefUri, String refTopicUri);

    // ---

    /**
     * Removes a single-valued child.
     */
    ChildTopicsModel remove(String compDefUri);

    // --- Multiple-valued Children ---

    /**
     * Adds a value to a multiple-valued child.
     */
    ChildTopicsModel add(String compDefUri, RelatedTopicModel value);

    ChildTopicsModel add(String compDefUri, TopicModel value);

    /**
     * Convenience method to add a *simple* value to a multiple-valued child.
     *
     * @param   value   the simple value: a String, Integer, Long, Double, or a Boolean.
     *                  Primitive values are auto-boxed.
     *
     * @return  this ChildTopicsModel.
     */
    ChildTopicsModel add(String compDefUri, Object value);

    /**
     * Convenience method to add a *composite* value to a multiple-valued child.
     *
     * @return  this ChildTopicsModel.
     */
    ChildTopicsModel add(String compDefUri, ChildTopicsModel value);

    /**
     * Sets the values of a multiple-valued child.
     * Existing values are overwritten.
     */
    ChildTopicsModel set(String compDefUri, List<RelatedTopicModel> values);

    /**
     * Removes a value from a multiple-valued child.
     */
    ChildTopicsModel remove(String compDefUri, TopicModel value);

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued child.
     */
    ChildTopicsModel addRef(String compDefUri, long refTopicId);

    /**
     * Adds a by-URI topic reference to a multiple-valued child.
     */
    ChildTopicsModel addRef(String compDefUri, String refTopicUri);

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued child.
     */
    ChildTopicsModel addDeletionRef(String compDefUri, long refTopicId);

    /**
     * Adds a by-URI topic deletion reference to a multiple-valued child.
     */
    ChildTopicsModel addDeletionRef(String compDefUri, String refTopicUri);



    // ===

    ChildTopicsModel clone();
}
