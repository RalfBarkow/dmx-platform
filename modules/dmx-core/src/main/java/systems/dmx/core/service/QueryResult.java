package systems.dmx.core.service;

import systems.dmx.core.DMXObject;
import systems.dmx.core.JSONEnabled;
import systems.dmx.core.util.DMXUtils;
import org.codehaus.jettison.json.JSONObject;
import java.util.List;



public class QueryResult implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public String topicQuery,           assocQuery;
    public String topicTypeUri,         assocTypeUri;
    public boolean searchTopicChildren, searchAssocChildren;

    public List<? extends DMXObject> objects;       // result objects

    // ---------------------------------------------------------------------------------------------------- Constructors

    public QueryResult(String topicQuery, String topicTypeUri, boolean searchTopicChildren,
                       String assocQuery, String assocTypeUri, boolean searchAssocChildren,
                       List<? extends DMXObject> objects) {
        this.topicQuery = topicQuery;
        this.topicTypeUri = topicTypeUri;
        this.searchTopicChildren = searchTopicChildren;
        this.assocQuery = assocQuery;
        this.assocTypeUri = assocTypeUri;
        this.searchAssocChildren = searchAssocChildren;
        this.objects = objects;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("topicQuery", topicQuery)
                .put("topicTypeUri", topicTypeUri)
                .put("searchTopicChildren", searchTopicChildren)
                .put("assocQuery", assocQuery)
                .put("assocTypeUri", assocTypeUri)
                .put("searchAssocChildren", searchAssocChildren)
                .put("objects", DMXUtils.toJSONArray(objects));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
