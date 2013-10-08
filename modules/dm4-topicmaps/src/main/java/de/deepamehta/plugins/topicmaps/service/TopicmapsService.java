package de.deepamehta.plugins.topicmaps.service;

import de.deepamehta.plugins.topicmaps.TopicmapRenderer;
import de.deepamehta.plugins.topicmaps.ViewmodelCustomizer;
import de.deepamehta.plugins.topicmaps.model.ClusterCoords;
import de.deepamehta.plugins.topicmaps.model.TopicmapViewmodel;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;



public interface TopicmapsService extends PluginService {

    Topic createTopicmap(String name,             String topicmapRendererUri, ClientState clientState);
    Topic createTopicmap(String name, String uri, String topicmapRendererUri, ClientState clientState);

    // ---

    TopicmapViewmodel getTopicmap(long topicmapId);

    // ---

    void addTopicToTopicmap(long topicmapId, long topicId, int x, int y);

    void addAssociationToTopicmap(long topicmapId, long assocId);

    void moveTopic(long topicmapId, long topicId, int x, int y);

    void setTopicVisibility(long topicmapId, long topicId, boolean visibility);

    void removeAssociationFromTopicmap(long topicmapId, long assocId);

    void moveCluster(long topicmapId, ClusterCoords coords);

    void setTopicmapTranslation(long topicmapId, int trans_x, int trans_y);

    // ---

    void registerTopicmapRenderer(TopicmapRenderer renderer);

    void registerViewmodelCustomizer(ViewmodelCustomizer customizer);
}
