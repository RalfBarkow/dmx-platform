package de.deepamehta.core;

import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * Specification of an association -- A n-ary connection between topics and other associations.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Association extends DeepaMehtaObject {

    Role getRole1();

    Role getRole2();

    // ---

    Role getRole(long objectId);



    // === Traversal ===

    Topic getTopic(String roleTypeUri);

    Set<Topic> getTopics(String roleTypeUri);

    // ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                            String othersTopicTypeUri,
                                                                            boolean fetchComposite);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                  String othersTopicTypeUri,
                                                                                  boolean fetchComposite);
}
