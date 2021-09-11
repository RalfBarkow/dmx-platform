package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * Maintains the sequence of a parent topic's child topics.
 */
class ChildTopicsSequence implements Iterable<RelatedAssocModelImpl> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long parentTopicId;
    private String childTypeUri;
    private String assocTypeUri;

    private AccessLayer al;
    private ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    ChildTopicsSequence(long parentTopicId, String childTypeUri, String assocTypeUri, AccessLayer al) {
        this.parentTopicId = parentTopicId;
        this.childTypeUri = childTypeUri;
        this.assocTypeUri = assocTypeUri;
        this.al = al;
        this.mf = al.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @return  an iterator of the parent topic's child topics (<code>RelatedTopic</code>s).
     *          Their "relating association" is the respective parent connection. ### FIXDOC
     */
    @Override
    public Iterator<RelatedAssocModelImpl> iterator() {

        return new Iterator() {

            private RelatedAssocModelImpl assoc = getFirstAssoc();

            private List assocIds = new ArrayList();

            @Override
            public boolean hasNext() {
                return assoc != null;
            }

            @Override
            public RelatedAssocModelImpl next() {
                RelatedAssocModelImpl _assoc = assoc;
                assoc = getSuccessorAssoc(assoc);
                //
                if (assocIds.contains(_assoc.id)) {
                    throw new RuntimeException("Cycle detected: assoc " + _assoc.id + " already in " + assocIds);
                }
                assocIds.add(_assoc.id);
                //
                return _assoc;
            }
        };
    }

    // ---

    AssocModelImpl insert(AssocModelImpl assoc, AssocModelImpl predAssoc) {
        try {
            if (predAssoc != null) {
                RelatedAssocModelImpl succAssoc = getSuccessorAssoc(predAssoc);
                if (succAssoc != null) {
                    insertInBetween(assoc.id, predAssoc, succAssoc);
                } else {
                    createSequenceSegment(predAssoc.id, assoc.id);
                }
            } else {
                RelatedAssocModelImpl first = getFirstAssoc();
                if (first != null) {
                    insertAtBegin(assoc.id, first);
                } else {
                    createSequenceStart(assoc.id);
                }
            }
            return assoc;
        } catch (Exception e) {
            throw new RuntimeException("Inserting association " + assoc.id + " into sequence of parent topic " +
                parentTopicId + " failed, predAssoc=" + predAssoc, e);
        }
    }

    /**
     * Removes the given child topic (actually the association that connects it to parent) from this sequence.
     * The child topic itself is not deleted, nor the association that connects it to parent.
     * <p>
     * Must be called <i>before</i> the child topic (or the association that connects it to parent) is deleted
     * (if at all).
     *
     * @param   childTopicId    the child topic to remove ### FIXDOC
     *
     * @return  the <code>RelatedTopic</code> representation of the child topic.
     */
    RelatedTopicModelImpl remove(AssocModelImpl assoc) {
        try {
            RelatedAssocModelImpl pred = getPredecessorAssoc(assoc.id);
            RelatedAssocModelImpl succ = getSuccessorAssoc(assoc);
            if (succ != null) {
                succ.getRelatingAssoc().delete();
            }
            if (pred != null) {
                pred.getRelatingAssoc().delete();
                if (succ != null) {
                    createSequenceSegment(pred.getId(), succ.getId());
                }
            } else {
                getFirstAssoc().getRelatingAssoc().delete();
                if (succ != null) {
                    createSequenceStart(succ.getId());
                }
            }
            return childTopic(assoc);
        } catch (Exception e) {
            throw new RuntimeException("Removing association " + assoc.getId() + " from sequence of parent topic " +
                parentTopicId + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * @param   firstChildAssoc     the association that connects the parent topic with its first child topic.
     *                              This association's "relating association" is its parent connection.
     */
    private void insertAtBegin(long assocId, RelatedAssocModelImpl firstChildAssoc) {
        firstChildAssoc.getRelatingAssoc().delete();
        createSequenceStart(assocId);
        createSequenceSegment(assocId, firstChildAssoc.getId());
    }

    private void insertInBetween(long assocId, AssocModelImpl predAssoc, RelatedAssocModelImpl succAssoc) {
        succAssoc.getRelatingAssoc().delete();
        createSequenceSegment(predAssoc.getId(), assocId);
        createSequenceSegment(assocId, succAssoc.getId());
    }

    // ---

    private void createSequenceStart(long assocId) {
        al.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newTopicPlayerModel(parentTopicId, DEFAULT),
            mf.newAssocPlayerModel(assocId, SEQUENCE_START)
        ));
    }

    private void createSequenceSegment(long predAssocId, long succAssocId) {
        al.createAssoc(mf.newAssocModel(SEQUENCE,
            mf.newAssocPlayerModel(predAssocId, PREDECESSOR),
            mf.newAssocPlayerModel(succAssocId, SUCCESSOR)
        ));
    }

    // ---

    /* private RelatedAssocModelImpl getPredecessorAssoc(RelatedTopicModelImpl childTopic) {
        return getPredecessorAssoc(childTopic.getRelatingAssoc());
    } */

    /**
     * @return      possibly null
     */
    private RelatedAssocModelImpl getPredecessorAssoc(long assocId) {
        return al.getAssocRelatedAssoc(assocId, SEQUENCE, SUCCESSOR, PREDECESSOR, assocTypeUri);
    }

    // ---

    /**
     * @return  the association that connects the parent topic with its first child topic, or <code>null</code> if there
     *          is no child topic. The returned association's "relating association" is of type "Assoc" and
     *          connects the returned association (role type is "Sequence Start") with the parent topic.
     */
    private RelatedAssocModelImpl getFirstAssoc() {
        return al.getTopicRelatedAssoc(parentTopicId, SEQUENCE, DEFAULT, SEQUENCE_START, assocTypeUri);
    }

    /**
     * Convenience.
     */
    private RelatedAssocModelImpl getSuccessorAssoc(RelatedTopicModelImpl childTopic) {
        return getSuccessorAssoc(childTopic.getRelatingAssoc());
    }

    /**
     * @param   assoc   expected to connect the parent topic with a child topic.
     *
     * @return  the given association's successor association, or <code>null</code> if there is no successor.
     *          The returned association's "relating association" is of type "Sequence".
     */
    private RelatedAssocModelImpl getSuccessorAssoc(AssocModelImpl assoc) {
        List<RelatedAssocModelImpl> succAssocs = al.getAssocRelatedAssocs(assoc.id, SEQUENCE, PREDECESSOR, SUCCESSOR,
            assocTypeUri);
        RelatedAssocModelImpl succAssoc = null;
        int size = succAssocs.size();
        if (size >= 1) {
            succAssoc = succAssocs.get(0);
            // ambiguity detection
            if (size > 1) {
                throw new RuntimeException("Ambiguity detected: assoc " + assoc.id + " has " + size + " successors: " +
                    succAssocs);
            }
        }
        return succAssoc;
    }

    // ---

    /**
     * Returns the <code>RelatedTopic</code> representation of the specified child topic. ### TODO: drop it
     *
     * @param   childTopicId    ID of the child topic to return.
     *
     * @return  a <code>RelatedTopic</code> that represents the specified child topic.
     *          Its "relating association" is the one that connects this sequence's parent topic with the child topic.
     *
     * @throws  RuntimeException    if the specified ID is not one of this sequence's child topics.
     */
    private RelatedTopicModelImpl getChildTopic(long childTopicId) {
        AssocModelImpl assoc = al.getAssocBetweenTopicAndTopic(assocTypeUri, parentTopicId, childTopicId, PARENT,
            CHILD);
        if (assoc == null) {
            throw new RuntimeException("Topic " + childTopicId + " is not a child of topic " + parentTopicId);
        }
        RelatedTopicModelImpl childTopic = childTopic(assoc);
        checkTopic(childTopic);
        return childTopic;
    }

    private RelatedTopicModelImpl childTopic(AssocModelImpl assoc) {
        return (RelatedTopicModelImpl) assoc.getDMXObjectByRole(CHILD);
    }

    private long assocId(RelatedTopicModelImpl childTopic) {
        return childTopic.getRelatingAssoc().getId();
    }

    private void checkTopic(TopicModelImpl topic) {
        String typeUri = topic.getTypeUri();
        if (!typeUri.equals(childTypeUri)) {
            throw new RuntimeException("Topic " + topic.getId() + " is of type \"" + typeUri +
                "\" but expected is \"" + childTypeUri + "\"");
        }
    }
}
