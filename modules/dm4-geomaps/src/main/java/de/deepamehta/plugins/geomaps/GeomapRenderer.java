package de.deepamehta.plugins.geomaps;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.plugins.topicmaps.TopicmapRenderer;



class GeomapRenderer implements TopicmapRenderer {

    @Override
    public String getUri() {
        return "dm4.geomaps.geomap_renderer";
    }

    @Override
    public ChildTopicsModel initialTopicmapState() {
        return new ChildTopicsModel()
            .put("dm4.topicmaps.translation", new ChildTopicsModel()
                .put("dm4.topicmaps.translation_x", 11.0)     // default region is "Germany"
                .put("dm4.topicmaps.translation_y", 51.0)
            )
            .put("dm4.topicmaps.zoom_level", 6);
    }
}
