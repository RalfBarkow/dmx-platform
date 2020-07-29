package systems.dmx.core;

import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.ViewConfigModel;



/**
 * A container for config topics.
 * <p>
 * Config topics can be accessed by their type URI.
 * A view config can contain only one config topic with a certain type URI.
 */
public interface ViewConfig {

    Iterable<Topic> getConfigTopics();

    /**
     * @return  the config topic for the given type URI, or <code>null</code> if there is none.
     */
    Topic getConfigTopic(String configTypeUri);

    /**
     * Adds a config topic to this view config.
     *
     * @return  the (instantiated) config topic.
     *
     * @throws  RuntimeException    if this view config already contains a config topic with the same type URI.
     */
    Topic addConfigTopic(TopicModel configTopic);

    /**
     * Sets a single value of a certain config topic.
     * If no such config topic exists in this view config it is created.
     *
     * @param   configTypeUri   The type URI of the config topic, e.g. "dmx.webclient.view_config"
     * @param   childTypeUri    The child type URI of the config value to set, e.g. "dmx.webclient.icon"
     * @param   value           The config value (String, Integer, Long, Double, or Boolean)
     */
    ViewConfig setConfigValue(String configTypeUri, String childTypeUri, Object value);

    ViewConfig setConfigValueRef(String configTypeUri, String childTypeUri, Object topicIdOrUri);

    // ---

    ViewConfigModel getModel();
}
