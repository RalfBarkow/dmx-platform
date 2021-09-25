package systems.dmx.core.util;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Identifiable;
import systems.dmx.core.JSONEnabled;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.model.RelatedObjectModel;
import systems.dmx.core.model.TopicPlayerModel;
import systems.dmx.core.osgi.CoreActivator;
import systems.dmx.core.service.CoreService;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;



public class DMXUtils {

    private static final Logger logger = Logger.getLogger(DMXUtils.class.getName());

    private static final String DMX_HOST_URL = System.getProperty("dmx.host.url");  // ### TODO: default value (#734)
    static {
        logger.info("Host config:\n  dmx.host.url = \"" + DMX_HOST_URL + "\"");
    }



    // ************
    // *** URLs ***
    // ************



    /**
     * Checks if an URL refers to this DMX installation.
     * The check relies on the "dmx.host.url" system property.
     */
    public static boolean isDMXURL(URL url) {
        try {
            return url.toString().startsWith(DMX_HOST_URL);
        } catch (Exception e) {
            throw new RuntimeException("Checking for DMX URL failed (url=\"" + url + "\")", e);
        }
    }



    // *******************
    // *** Collections ***
    // *******************



    public static List<Long> idList(Iterable<? extends Identifiable> items) {
        List<Long> ids = new ArrayList();
        for (Identifiable item : items) {
            ids.add(item.getId());
        }
        return ids;
    }

    public static <T extends Identifiable> T findById(long id, Iterable<T> items) {
        for (T item : items) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public static <T extends RelatedObjectModel> T findByAssoc(long assocId, Iterable<T> objects) {
        for (T object : objects) {
            if (object.getRelatingAssoc().getId() == assocId) {
                return object;
            }
        }
        return null;
    }

    public static int indexOfAssoc(long assocId, Iterable<? extends RelatedObjectModel> objects) {
        int i = 0;
        for (RelatedObjectModel object : objects) {
            if (object.getRelatingAssoc().getId() == assocId) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static <M> List<M> toModelList(Iterable<? extends DMXObject> objects) {
        List<M> modelList = new ArrayList();
        for (DMXObject object : objects) {
            modelList.add((M) object.getModel());
        }
        return modelList;
    }

    public static String topicNames(Iterable<? extends Topic> topics) {
        StringBuilder names = new StringBuilder();
        Iterator<? extends Topic> i = topics.iterator();
        while (i.hasNext()) {
            Topic topic = i.next();
            names.append('"').append(topic.getSimpleValue()).append('"');
            if (i.hasNext()) {
                names.append(", ");
            }
        }
        return names.toString();
    }

    public static <T extends DMXObject> List<T> loadChildTopics(List<T> objects) {
        for (DMXObject object : objects) {
            object.loadChildTopics();
        }
        return objects;
    }



    // ************
    // *** JSON ***
    // ************



    // === Generic ===

    public static Map toMap(JSONObject o) {
        return toMap(o, new HashMap());
    }

    public static Map toMap(JSONObject o, Map map) {
        try {
            Iterator<String> i = o.keys();
            while (i.hasNext()) {
                String key = i.next();
                map.put(key, o.get(key));   // throws JSONException
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Converting JSONObject to Map failed", e);
        }
    }

    // ---

    public static List toList(JSONArray o) {
        try {
            List list = new ArrayList();
            for (int i = 0; i < o.length(); i++) {
                list.add(o.get(i));         // throws JSONException
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Converting JSONArray to List failed", e);
        }
    }

    // === DMX specific ===

    public static JSONArray toJSONArray(Iterable<? extends JSONEnabled> items) {
        JSONArray array = new JSONArray();
        for (JSONEnabled item : items) {
            array.put(item.toJSON());
        }
        return array;
    }



    // *****************
    // *** Traversal ***
    // *****************



    /**
     * Finds all parent topics of the given topic by traversing along the child->parent relationship.
     * Only the leaves are returned.
     * <p>
     * If the given topic has no parent topics the returned list contains only the given topic.
     */
    public static List<Topic> getParentTopics(Topic topic) {
        List<Topic> parentTopics = new ArrayList();
        List<RelatedTopic> _parentTopics = topic.getRelatedTopics((String) null, CHILD, PARENT, null);
        if (_parentTopics.isEmpty()) {
            parentTopics.add(topic);
        } else {
            for (Topic _topic : _parentTopics) {
                parentTopics.addAll(getParentTopics(_topic));
            }
        }
        return parentTopics;
    }



    // *******************************
    // *** Association Auto-Typing ***
    // *******************************



    public static PlayerModel[] assocAutoTyping(AssocModel assoc, String topicTypeUri1, String topicTypeUri2,
                                                String assocTypeUri, String roleTypeUri1, String roleTypeUri2) {
        return assocAutoTyping(assoc, topicTypeUri1, topicTypeUri2, assocTypeUri, roleTypeUri1, roleTypeUri2, null);
    }

    /**
     * Retypes the given association if its players match the given topic types. The given assoc model is modified
     * in-place. Typically called from a plugin's {@link systems.dmx.core.service.event.PreCreateAssoc}.
     *
     * <p>Read the parameters as follows: if "assoc" connects a "topicTypeUri1" with a "topicTypeUri2" (regardless of
     * 1,2 position) then retype it to "assocTypeUri" and use the role types "roleTypeUri1" and "roleTypeUri2".
     * "roleTypeUri1" is used for the "topicTypeUri1" player, "roleTypeUri2" is used for the "topicTypeUri2" player.
     *
     * <p>Auto-typing takes place only if the given assoc is of type "Association" (that is a generic assoc without any
     * semantics). If the given assoc is not a generic one, no retyping takes place (null is returned).
     *
     * <p>Auto-typing is supported only for topic players, and only if they are identified by-ID. If the given assoc has
     * at least one assoc player, or if a topic player is identfied by-URI, no retyping takes place (null is returned).
     *
     * @param   playerTest      optional: a predicate tested after a type-match is detected for both players, but
     *                          before actual auto-typing takes place. You can prohibit auto-typing by returning
     *                          <code>false</code>. An array of 2 assoc players is passed. [0] is the player that
     *                          matches "topicTypeUri1", [1] is the player that matches "topicTypeUri2".
     *                          If no predicate is given (null) no additional test is performed before auto-typing.
     *
     * @return  a 2-element {@link systems.dmx.core.model.PlayerModel} array if auto-typing took place,
     *          <code>null</code> otherwise. Convenience to access the assoc's players after retyping.
     *          Element 0 is the player of "topicTypeUri1", Element 1 is the player of "topicTypeUri2".
     */
    public static PlayerModel[] assocAutoTyping(AssocModel assoc, String topicTypeUri1, String topicTypeUri2,
                                                String assocTypeUri, String roleTypeUri1, String roleTypeUri2,
                                                Predicate<PlayerModel[]> playerTest) {
        if (!assoc.getTypeUri().equals(ASSOCIATION)) {
            return null;
        }
        PlayerModel[] players = getPlayerModels(assoc, topicTypeUri1, topicTypeUri2);
        if (players != null) {
            if (playerTest != null && !playerTest.test(players)) {
                logger.info("### Auto typing association into \"" + assocTypeUri + "\" ABORTED by player test");
                return null;
            }
            logger.info("### Auto typing association into \"" + assocTypeUri +
                "\" (\"" + topicTypeUri1 + "\" <-> \"" + topicTypeUri2 + "\")");
            assoc.setTypeUri(assocTypeUri);
            setAssocValue(assoc);
            players[0].setRoleTypeUri(roleTypeUri1);
            players[1].setRoleTypeUri(roleTypeUri2);
        }
        return players;
    }

    public static PlayerModel[] getPlayerModels(AssocModel assoc, String topicTypeUri1, String topicTypeUri2) {
        PlayerModel r1 = assoc.getPlayer1();
        PlayerModel r2 = assoc.getPlayer2();
        // ### FIXME: auto-typing is supported only for topic players, and if they are identified by-ID.
        if (!(r1 instanceof TopicPlayerModel) || !(r2 instanceof TopicPlayerModel)) {
            return null;
        }
        // Note: we can't call playerModel.getDMXObject() as this would build an entire object model, but its "value"
        // is not yet available in case the association is part of the player's composite structure.
        // Compare to AssocModelImpl.duplicateCheck()
        String t1 = r1.getTypeUri();
        String t2 = r2.getTypeUri();
        PlayerModel player1 = getPlayer(r1, r2, t1, t2, topicTypeUri1, 1);
        PlayerModel player2 = getPlayer(r1, r2, t1, t2, topicTypeUri2, 2);
        // Note: if topicTypeUri1 equals topicTypeUri2 and in the assoc only *one* player matches this type
        // both getPlayer() calls return the *same* player. Auto-typing must not be performed.
        if (player1 != null && player2 != null && player1 != player2) {
            return new PlayerModel[] {player1, player2};
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private static PlayerModel getPlayer(PlayerModel r1, PlayerModel r2, String t1, String t2, String topicTypeUri,
                                         int nr) {
        boolean m1 = t1.equals(topicTypeUri);
        boolean m2 = t2.equals(topicTypeUri);
        if (m1 && m2) {
            return nr == 1 ? r1 : r2;
        }
        return m1 ? r1 : m2 ? r2 : null;
    }

    // Note: this is experimental
    // ### TODO: do it only for simple assoc types? Drop it, and let the application do this?
    private static void setAssocValue(AssocModel assoc) {
        AssocType assocType = CoreActivator.getCoreService().getAssocType(assoc.getTypeUri());
        assoc.setSimpleValue(assocType.getSimpleValue());
    }
}
