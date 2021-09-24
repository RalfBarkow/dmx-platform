package systems.dmx.core.impl;

import static systems.dmx.contacts.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedAssoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.storage.spi.DMXTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class CoreServiceTest extends CoreServiceTestEnvironment {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Test
    public void compositeModel() {
        ChildTopicsModel person = mf.newChildTopicsModel()
            .set("dmx.core.name", "Karl Blum")
            .set(HOME_ADDRESS, mf.newChildTopicsModel()
                .set(POSTAL_CODE, 13206)
                .set(CITY, "Berlin"))
            .set("dmx.contacts.office_address", mf.newChildTopicsModel()
                .set(POSTAL_CODE, 14345)
                .set(CITY, "Berlin"));
        //
        assertEquals("Karl Blum", person.getString("dmx.core.name"));
        //
        ChildTopicsModel address = person.getChildTopics(HOME_ADDRESS);
        assertEquals("Berlin", address.getString(CITY));
        //
        Object code = address.getValue(POSTAL_CODE);
        assertSame(Integer.class, code.getClass());
        assertEquals(13206, code);  // autoboxing
    }

    // ---

    @Test
    public void typeDefinition() {
        TopicType topicType = dmx.getTopicType(PLUGIN);
        assertEquals(PLUGIN,     topicType.getUri());
        assertEquals(TOPIC_TYPE, topicType.getTypeUri());
        assertEquals(ENTITY,     topicType.getDataTypeUri());
        assertEquals(3,          topicType.getCompDefs().size());
        CompDef compDef = topicType.getCompDef(PLUGIN_MIGRATION_NR);
        assertEquals(COMPOSITION_DEF,     compDef.getTypeUri());
        assertEquals(PLUGIN,              compDef.getParentTypeUri());
        assertEquals(PLUGIN_MIGRATION_NR, compDef.getChildTypeUri());
        assertEquals(ONE,                 compDef.getChildCardinalityUri());
        DMXObject t1 = compDef.getDMXObjectByRole(PARENT_TYPE);
        DMXObject t2 = compDef.getDMXObjectByRole(CHILD_TYPE);
        assertEquals(PLUGIN,              t1.getUri());
        assertEquals(TOPIC_TYPE,          t1.getTypeUri());
        assertEquals(PLUGIN_MIGRATION_NR, t2.getUri());
        assertEquals(TOPIC_TYPE,          t2.getTypeUri());
    }

    @Test
    public void createWithoutComposite() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic = dmx.createTopic(mf.newTopicModel("systems.dmx.notes", PLUGIN,
                new SimpleValue("DMX Notes")));
            //
            topic.update(mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 23));
            //
            int nr = topic.getChildTopics().getTopic(PLUGIN_MIGRATION_NR).getSimpleValue().intValue();
            assertEquals(23, nr);
            //
            topic.update(mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 42));
            //
            nr = topic.getChildTopics().getTopic(PLUGIN_MIGRATION_NR).getSimpleValue().intValue();
            assertEquals(42, nr);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void createWithComposite() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // Note: has() is internal API, so we need a TopicImpl here
            TopicImpl topic = dmx.createTopic(mf.newTopicModel("systems.dmx.notes", PLUGIN,
                mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 23)
            ));
            //
            assertTrue(topic.getChildTopics().has(PLUGIN_MIGRATION_NR));
            //
            int nr = topic.getChildTopics().getTopic(PLUGIN_MIGRATION_NR).getSimpleValue().intValue();
            assertEquals(23, nr);
            //
            topic.update(mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 42));
            //
            nr = topic.getChildTopics().getTopic(PLUGIN_MIGRATION_NR).getSimpleValue().intValue();
            assertEquals(42, nr);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void onDemandChildTopicLoading() {
        DMXTransaction tx = dmx.beginTx();
        try {
            dmx.createTopic(mf.newTopicModel("systems.dmx.notes", PLUGIN,
                mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 23)
            ));
            // Note: has() is internal API, so we need a TopicImpl here
            TopicImpl topic = (TopicImpl) dmx.getTopicByUri("systems.dmx.notes");
            ChildTopicsImpl comp = topic.getChildTopics();
            assertFalse(comp.has(PLUGIN_MIGRATION_NR));                 // child topic is not yet loaded
            //
            Topic childTopic = comp.getTopic(PLUGIN_MIGRATION_NR);
            assertEquals(23, childTopic.getSimpleValue().intValue());   // child topic is loaded on-demand
            assertTrue(comp.has(PLUGIN_MIGRATION_NR));                  // child topic is now loaded
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void onDemandChildTopicLoadingWithConvenienceAccessor() {
        DMXTransaction tx = dmx.beginTx();
        try {
            dmx.createTopic(mf.newTopicModel("systems.dmx.notes", PLUGIN,
                mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 23)
            ));
            // Note: has() is internal API, so we need a TopicImpl here
            TopicImpl topic = (TopicImpl) dmx.getTopicByUri("systems.dmx.notes");
            ChildTopicsImpl comp = topic.getChildTopics();
            assertFalse(comp.has(PLUGIN_MIGRATION_NR));              // child topic is not yet loaded
            //
            assertEquals(23, comp.getInt(PLUGIN_MIGRATION_NR));      // child topic is loaded on-demand
            assertTrue(comp.has(PLUGIN_MIGRATION_NR));               // child topic is now loaded
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void changeLabelWithUpdateChildTopics() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic = dmx.createTopic(mf.newTopicModel(PLUGIN));
            assertEquals("", topic.getSimpleValue().toString());
            //
            topic.update(mf.newChildTopicsModel().set(PLUGIN_NAME, "My Plugin"));
            assertEquals("My Plugin", topic.getChildTopics().getString(PLUGIN_NAME));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            Topic fetchedTopic = dmx.getTopic(topic.getId());
            assertEquals("My Plugin", fetchedTopic.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void changeLabelWithChildTopicsSet() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic = dmx.createTopic(mf.newTopicModel(PLUGIN));
            assertEquals("", topic.getSimpleValue().toString());
            //
            topic.update(mf.newChildTopicsModel().set(PLUGIN_NAME, "My Plugin"));
            assertEquals("My Plugin", topic.getChildTopics().getString(PLUGIN_NAME));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            Topic fetchedTopic = dmx.getTopic(topic.getId());
            assertEquals("My Plugin", fetchedTopic.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void setLabelChildWhileChildrenAreNotLoaded() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic = dmx.createTopic(mf.newTopicModel(PLUGIN, mf.newChildTopicsModel()
                .set(PLUGIN_NAME, "My Plugin")
                .set(PLUGIN_SYMBOLIC_NAME, "dmx.test.my_plugin")
                .set(PLUGIN_MIGRATION_NR, 1)
            ));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            topic = dmx.getTopic(topic.getId());                                // Note: the children are not loaded
            assertEquals("My Plugin", topic.getSimpleValue().toString());       // the label is intact
            topic.update(mf.newChildTopicsModel().set(PLUGIN_NAME, "HuHu"));    // setting child used for labeling
            assertEquals("HuHu", topic.getSimpleValue().toString());            // the label is recalculated
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void setNonlabelChildWhileChildrenAreNotLoaded() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic = dmx.createTopic(mf.newTopicModel(PLUGIN, mf.newChildTopicsModel()
                .set(PLUGIN_NAME, "My Plugin")
                .set(PLUGIN_SYMBOLIC_NAME, "dmx.test.my_plugin")
                .set(PLUGIN_MIGRATION_NR, 1)
            ));
            assertEquals("My Plugin", topic.getSimpleValue().toString());
            //
            topic = dmx.getTopic(topic.getId());                                // Note: the children are not loaded
            assertEquals("My Plugin", topic.getSimpleValue().toString());       // the label is intact
            topic.update(mf.newChildTopicsModel().set(PLUGIN_MIGRATION_NR, 3)); // setting child NOT used for labeling
            assertEquals("My Plugin", topic.getSimpleValue().toString());       // the label is still intact
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void changeLabelWithSetRefSimple() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) define model
            // "Person Name" (simple)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.person_name", "Person Name", TEXT));
            // "Comment" (composite)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.comment", "Comment", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.comment", "dmx.test.person_name", ONE
                ))
            );
            // 2) create instances
            // "Person Name"
            Topic karl = dmx.createTopic(mf.newTopicModel("dmx.test.person_name", new SimpleValue("Karl Albrecht")));
            //
            assertEquals("Karl Albrecht", karl.getSimpleValue().toString());
            //
            // "Comment"
            Topic comment = dmx.createTopic(mf.newTopicModel("dmx.test.comment"));
            comment.update(mf.newChildTopicsModel().setRef("dmx.test.person_name", karl.getId()));
            //
            assertEquals(karl.getId(), comment.getChildTopics().getTopic("dmx.test.person_name").getId());
            assertEquals("Karl Albrecht", comment.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void changeLabelWithSetRefComposite() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) define model
            // "First Name", "Last Name" (simple)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.first_name", "First Name", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.last_name",  "Last Name",  TEXT));
            // "Person Name" (composite)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.person_name", "Person Name", VALUE)
                .addCompDef(mf.newCompDefModel(null, false, true,
                    "dmx.test.person_name", "dmx.test.first_name", ONE
                ))
                .addCompDef(mf.newCompDefModel(null, false, true,
                    "dmx.test.person_name", "dmx.test.last_name", ONE
                ))
            );
            // "Comment" (composite)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.comment", "Comment", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.comment", "dmx.test.person_name", ONE
                ))
            );
            // 2) create instances
            // "Person Name"
            Topic karl = dmx.createTopic(mf.newTopicModel("dmx.test.person_name", mf.newChildTopicsModel()
                .set("dmx.test.first_name", "Karl")
                .set("dmx.test.last_name", "Albrecht")
            ));
            //
            assertEquals("Karl Albrecht", karl.getSimpleValue().toString());
            //
            // "Comment"
            Topic comment = dmx.createTopic(mf.newTopicModel("dmx.test.comment"));
            comment.update(mf.newChildTopicsModel().setRef("dmx.test.person_name", karl.getId()));
            //
            assertEquals(karl.getId(), comment.getChildTopics().getTopic("dmx.test.person_name").getId());
            assertEquals("Karl Albrecht", comment.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void changeLabelWithSetComposite() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) define model
            // "First Name", "Last Name" (simple)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.first_name", "First Name", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.last_name",  "Last Name",  TEXT));
            // "Person Name" (composite)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.person_name", "Person Name", VALUE)
                .addCompDef(mf.newCompDefModel(null, false, true,
                    "dmx.test.person_name", "dmx.test.first_name", ONE
                ))
                .addCompDef(mf.newCompDefModel(null, false, true,
                    "dmx.test.person_name", "dmx.test.last_name", ONE
                ))
            );
            // "Comment" (composite)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.comment", "Comment", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.comment", "dmx.test.person_name", ONE
                ))
            );
            // 2) create instances
            // "Comment"
            Topic comment = dmx.createTopic(mf.newTopicModel("dmx.test.comment"));
            comment.update(mf.newChildTopicsModel().set("dmx.test.person_name", mf.newChildTopicsModel()
                .set("dmx.test.first_name", "Karl")
                .set("dmx.test.last_name", "Albrecht")
            ));
            //
            assertEquals("Karl Albrecht", comment.getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void hasIncludeInLabel() {
        // Note: the comp def is created while migration
        RelatedTopic includeInLabel = dmx.getTopicType(PLUGIN)
            .getCompDef(PLUGIN_NAME).getChildTopics().getTopicOrNull(INCLUDE_IN_LABEL);
        assertNotNull(includeInLabel);
        assertEquals(false, includeInLabel.getSimpleValue().booleanValue());
    }

    @Test
    public void hasIncludeInLabelForAddedCompDef() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // add comp def programmatically
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.date", "Date", TEXT));
            dmx.getTopicType(PLUGIN).addCompDef(
                mf.newCompDefModel(
                    PLUGIN, "dmx.test.date", ONE
                ));
            //
            // Note: the topic type must be re-get as getTopicType() creates
            // a cloned model that doesn't contain the added comp def
            RelatedTopic includeInLabel = dmx.getTopicType(PLUGIN)
                .getCompDef("dmx.test.date").getChildTopics().getTopicOrNull(INCLUDE_IN_LABEL);
            assertNotNull(includeInLabel);
            assertEquals(false, includeInLabel.getSimpleValue().booleanValue());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void setIncludeInLabel() {
        DMXTransaction tx = dmx.beginTx();
        try {
            TopicTypeImpl tt = dmx.getTopicType(PLUGIN);
            //
            // set "Include in Label" flag
            CompDef cd = tt.getCompDef(PLUGIN_NAME);
            ChildTopics ct = cd.getChildTopics();
            cd.update(mf.newChildTopicsModel().set(INCLUDE_IN_LABEL, true));
            //
            assertEquals(true, ct.getBoolean(INCLUDE_IN_LABEL));
            //
            List<String> lc = tt.getLabelConfig();
            assertEquals(1, lc.size());
            assertEquals(PLUGIN_NAME, lc.get(0));
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void setIncludeInLabelWhenCustomAssocTypeIsSet() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) create composite type, set a custom assoc type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.date", "Date", TEXT));
            dmx.createAssocType(mf.newAssocTypeModel("dmx.test.birthday", "Birthday", TEXT));
            TopicTypeImpl tt = dmx.createTopicType(
                mf.newTopicTypeModel("dmx.test.person", "Person", ENTITY).addCompDef(
                    mf.newCompDefModel("dmx.test.birthday", false, false,
                        "dmx.test.person", "dmx.test.date", ONE)));
            // test comp def children *before* set
            CompDef cd = tt.getCompDef("dmx.test.date#dmx.test.birthday");
            ChildTopics ct = cd.getChildTopics();
            assertEquals(false, ct.getBoolean(INCLUDE_IN_LABEL));
            assertEquals("dmx.test.birthday", ct.getTopic(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE).getUri());
            //
            // 2) set "Include in Label" flag
            cd.update(mf.newChildTopicsModel().set(INCLUDE_IN_LABEL, true));
            //
            // test comp def children *after* set (custom assoc type must not change)
            assertEquals(true, ct.getBoolean(INCLUDE_IN_LABEL));
            assertEquals("dmx.test.birthday", ct.getTopic(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE).getUri());
            //
            List<String> lc = tt.getLabelConfig();
            assertEquals(1, lc.size());
            assertEquals("dmx.test.date#dmx.test.birthday", lc.get(0));
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void editCompDefViaAssoc() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // set "Include in Label" flag
            long compDefId = dmx.getTopicType(PLUGIN).getCompDef(PLUGIN_NAME).getId();
            dmx.getAssoc(compDefId).update(mf.newChildTopicsModel().set(INCLUDE_IN_LABEL, false));
            //
            // comp def order must not have changed
            Collection<CompDef> compDefs = dmx.getTopicType(PLUGIN).getCompDefs();
            // Note: the topic type must be re-get as getTopicType() creates
            // a cloned model that doesn't contain the manipulated comp defs
            assertEquals(3, compDefs.size());
            Iterator<CompDef> i = compDefs.iterator();
            assertEquals(PLUGIN_NAME,          i.next().getCompDefUri());
            assertEquals(PLUGIN_SYMBOLIC_NAME, i.next().getCompDefUri());
            assertEquals(PLUGIN_MIGRATION_NR,  i.next().getCompDefUri());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void editCompDefSetCustomAssocType() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // set Custom Assoc Type (via comp def)
            dmx.getTopicType(PLUGIN).getCompDef(PLUGIN_NAME).update(mf.newChildTopicsModel().setRef(
                ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE, ASSOCIATION
            ));
            //
            // get Custom Assoc Type
            Topic assocType = dmx.getTopicType(PLUGIN)
                .getCompDef("dmx.core.plugin_name#dmx.core.association").getChildTopics()
                .getTopic(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE);
            // Note: the topic type must be re-get as getTopicType() creates
            // a cloned model that doesn't contain the manipulated comp defs
            assertEquals(ASSOCIATION, assocType.getUri());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void editCompDefViaAssocSetCustomAssocType() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // set Custom Assoc Type (via association)
            long compDefId = dmx.getTopicType(PLUGIN).getCompDef(PLUGIN_NAME).getId();
            dmx.getAssoc(compDefId).update(mf.newChildTopicsModel().setRef(
                ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE, ASSOCIATION
            ));
            //
            // get Custom Assoc Type
            Topic assocType = dmx.getTopicType(PLUGIN)
                .getCompDef("dmx.core.plugin_name#dmx.core.association").getChildTopics()
                .getTopic(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE);
            // Note: the topic type must be re-get as getTopicType() creates
            // a cloned model that doesn't contain the manipulated comp defs
            assertEquals(ASSOCIATION, assocType.getUri());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void uriUniquenessCreateTopic() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic = dmx.createTopic(mf.newTopicModel("dmx.my.uri", PLUGIN));
            assertEquals("dmx.my.uri", topic.getUri());
            //
            dmx.createTopic(mf.newTopicModel("dmx.my.uri", PLUGIN));
            fail("\"URI already taken\" exception not thrown");
            //
            tx.success();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause.getMessage().startsWith("Value integration failed"));
            cause = cause.getCause();
            assertTrue(cause.getMessage().startsWith("Creating single topic failed"));
            cause = cause.getCause();
            assertEquals("URI \"dmx.my.uri\" is already taken", cause.getMessage());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void uriUniquenessSetUri() {
        DMXTransaction tx = dmx.beginTx();
        try {
            Topic topic1 = dmx.createTopic(mf.newTopicModel("dmx.my.uri", PLUGIN));
            assertEquals("dmx.my.uri", topic1.getUri());
            //
            Topic topic2 = dmx.createTopic(mf.newTopicModel(PLUGIN));
            assertEquals("", topic2.getUri());
            //
            topic2.setUri("dmx.my.uri");
            fail("\"URI already taken\" exception not thrown");
            //
            tx.success();
        } catch (Exception e) {
            assertEquals("URI \"dmx.my.uri\" is already taken", e.getMessage());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void uriUniquenessUpdate() {
        DMXTransaction tx = dmx.beginTx();
        long topic2Id = -1;
        try {
            Topic topic1 = dmx.createTopic(mf.newTopicModel("dmx.my.uri", PLUGIN));
            assertEquals("dmx.my.uri", topic1.getUri());
            //
            Topic topic2 = dmx.createTopic(mf.newTopicModel(PLUGIN));
            topic2Id = topic2.getId();
            assertEquals("", topic2.getUri());
            //
            topic2.update(mf.newTopicModel("dmx.my.uri", PLUGIN));
            fail("\"URI already taken\" exception not thrown");
            //
            tx.success();
        } catch (Exception e) {
            // logger.log(Level.WARNING, "Exception thrown:", e);
            assertEquals("Updating topic " + topic2Id + " failed", e.getMessage());
            assertEquals("URI \"dmx.my.uri\" is already taken", e.getCause().getCause().getMessage());
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void compDefSequence() {
        DMXType type = dmx.getTopicType(PLUGIN);
        //
        // find comp def 1/3
        RelatedAssoc compDef = type.getRelatedAssoc(COMPOSITION, TYPE, SEQUENCE_START, COMPOSITION_DEF);
        logger.info("### comp def ID 1/3 = " + compDef.getId() + ", relating assoc ID = " +
            compDef.getRelatingAssoc().getId());
        assertNotNull(compDef);
        //
        // find comp def 2/3
        compDef = compDef.getRelatedAssoc(SEQUENCE, PREDECESSOR, SUCCESSOR, COMPOSITION_DEF);
        logger.info("### comp def ID 2/3 = " + compDef.getId() + ", relating assoc ID = " +
            compDef.getRelatingAssoc().getId());
        assertNotNull(compDef);
        //
        // find comp def 3/3
        compDef = compDef.getRelatedAssoc(SEQUENCE, PREDECESSOR, SUCCESSOR, COMPOSITION_DEF);
        logger.info("### comp def ID 3/3 = " + compDef.getId() + ", relating assoc ID = " +
            compDef.getRelatingAssoc().getId());
        assertNotNull(compDef);
        //
        // there is no other
        compDef = compDef.getRelatedAssoc(SEQUENCE, PREDECESSOR, SUCCESSOR, COMPOSITION_DEF);
        assertNull(compDef);
    }

    // ---

    @Test
    public void insertCompDefAtPos0() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // create child type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.name", "Name", TEXT));
            // insert comp def at pos 0
            dmx.getTopicType(PLUGIN).addCompDefBefore(
                mf.newCompDefModel(PLUGIN, "dmx.test.name", ONE),
                PLUGIN_NAME
            );
            //
            // Note: the type manipulators (here: addCompDefBefore()) operate on the *kernel* type model, while the
            // accessors (here: getCompDefs()) operate on the *userland* type model, which is a cloned (and filtered)
            // kernel type model. The manipulation is not immediately visible in the userland type model. To see the
            // change we must re-get the userland type model (by getTopicType()).
            Collection<CompDef> compDefs = dmx.getTopicType(PLUGIN).getCompDefs();
            assertSame(4, compDefs.size());
            //
            Iterator<CompDef> i = compDefs.iterator();
            assertEquals("dmx.test.name", i.next().getChildTypeUri());      // new comp def is at pos 0
            assertEquals(PLUGIN_NAME, i.next().getChildTypeUri());          // former pos 0 is now at pos 1
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void insertCompDefAtPos1() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // create child type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.name", "Name", TEXT));
            // insert comp def at pos 1
            dmx.getTopicType(PLUGIN).addCompDefBefore(
                mf.newCompDefModel(PLUGIN, "dmx.test.name", ONE),
                PLUGIN_SYMBOLIC_NAME
            );
            //
            // Note: the type manipulators (here: addCompDefBefore()) operate on the *kernel* type model, while the
            // accessors (here: getCompDefs()) operate on the *userland* type model, which is a cloned (and filtered)
            // kernel type model. The manipulation is not immediately visible in the userland type model. To see the
            // change we must re-get the userland type model (by getTopicType()).
            Collection<CompDef> compDefs = dmx.getTopicType(PLUGIN).getCompDefs();
            assertSame(4, compDefs.size());
            //
            Iterator<CompDef> i = compDefs.iterator();
            assertEquals(PLUGIN_NAME, i.next().getChildTypeUri());              // pos 0 is unchanged
            assertEquals("dmx.test.name", i.next().getChildTypeUri());          // new comp def is at pos 1
            assertEquals(PLUGIN_SYMBOLIC_NAME, i.next().getChildTypeUri());     // former pos 1 is now at pos 2
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void getTopicsByType() {
        Topic type = dmx.getTopicByUri(DATA_TYPE);
        List<RelatedTopic> topics1 = getTopicInstancesByTraversal(type);
        assertEquals(7, topics1.size());
        List<Topic> topics2 = getTopicInstances(DATA_TYPE);
        assertEquals(7, topics2.size());
    }

    // Note: when the meta model changes the values might need adjustment
    @Test
    public void getAssocsByType() {
        List<RelatedAssoc> assocs;
        //
        assocs = getAssocInstancesByTraversal(INSTANTIATION);
        assertEquals(39, assocs.size());
        //
        assocs = getAssocInstancesByTraversal(COMPOSITION_DEF);
        assertEquals(7, assocs.size());
    }

    // ---

    @Test
    public void retypeAssoc() {
        DMXTransaction tx = dmx.beginTx();
        Topic type;
        List<RelatedTopic> childTypes;
        try {
            type = dmx.getTopicByUri(PLUGIN);
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.size());
            //
            // retype assoc
            Assoc assoc = childTypes.get(0).getRelatingAssoc();
            assertEquals(COMPOSITION_DEF, assoc.getTypeUri());
            assoc.setTypeUri(ASSOCIATION);
            assertEquals(ASSOCIATION, assoc.getTypeUri());
            assoc = dmx.getAssoc(assoc.getId());
            assertEquals(ASSOCIATION, assoc.getTypeUri());
            //
            // re-execute query
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.size());
            // ### Note: the Lucene index update is not visible within the transaction!
            // ### That's contradictory to the Neo4j documentation!
            // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        childTypes = getChildTypes(type);
        assertEquals(2, childTypes.size());
        // ### Note: the Lucene index update is only visible once the transaction is committed!
        // ### That's contradictory to the Neo4j documentation!
        // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
    }

    @Test
    public void retypeAssocRoles() {
        DMXTransaction tx = dmx.beginTx();
        Topic type;
        List<RelatedTopic> childTypes;
        try {
            type = dmx.getTopicByUri(PLUGIN);
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.size());
            //
            // retype assoc players
            Assoc assoc = childTypes.get(0).getRelatingAssoc();
            assoc.getPlayer1().setRoleTypeUri(DEFAULT);
            assoc.getPlayer2().setRoleTypeUri(DEFAULT);
            //
            // re-execute query
            childTypes = getChildTypes(type);
            assertEquals(3, childTypes.size());
            // ### Note: the Lucene index update is not visible within the transaction!
            // ### That's contradictory to the Neo4j documentation!
            // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        childTypes = getChildTypes(type);
        assertEquals(2, childTypes.size());
        // ### Note: the Lucene index update is only visible once the transaction is committed!
        // ### That's contradictory to the Neo4j documentation!
        // ### It states that QueryContext's tradeCorrectnessForSpeed behavior is off by default.
    }

    @Test
    public void retypeTopicAndTraverse() {
        DMXTransaction tx = dmx.beginTx();
        Topic t0;
        List<RelatedTopic> topics;
        try {
            setupTestTopics();
            //
            t0 = dmx.getTopicByUri("dmx.test.t0");
            //
            // execute query
            topics = getTestTopics(t0);
            assertEquals(3, topics.size());  // we have 3 topics
            //
            // retype the first topic
            Topic topic = topics.get(0);
            assertEquals(PLUGIN, topic.getTypeUri());
            topic.setTypeUri(DATA_TYPE);
            assertEquals(DATA_TYPE, topic.getTypeUri());
            topic = dmx.getTopic(topic.getId());
            assertEquals(DATA_TYPE, topic.getTypeUri());
            //
            // re-execute query
            topics = getTestTopics(t0);
            assertEquals(2, topics.size());  // now we have 2 topics
            // ### Note: the Lucene index update *is* visible within the transaction *if* the test content is
            // ### created within the same transaction!
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        topics = getTestTopics(t0);
        assertEquals(2, topics.size());      // we still have 2 topics
    }

    @Test
    public void retypeAssocAndTraverse() {
        DMXTransaction tx = dmx.beginTx();
        Topic t0;
        List<RelatedAssoc> assocs;
        try {
            setupTestAssocs();
            //
            t0 = dmx.getTopicByUri("dmx.test.t0");
            //
            // execute query
            assocs = getTestAssocs(t0);
            assertEquals(3, assocs.size());  // we have 3 associations
            //
            // retype the first association
            Assoc assoc = assocs.get(0);
            assertEquals(ASSOCIATION, assoc.getTypeUri());
            assoc.setTypeUri(COMPOSITION);
            assertEquals(COMPOSITION, assoc.getTypeUri());
            assoc = dmx.getAssoc(assoc.getId());
            assertEquals(COMPOSITION, assoc.getTypeUri());
            //
            // re-execute query
            assocs = getTestAssocs(t0);
            assertEquals(2, assocs.size());  // now we have 2 associations
            // ### Note: the Lucene index update *is* visible within the transaction *if* the test content is
            // ### created within the same transaction!
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        assocs = getTestAssocs(t0);
        assertEquals(2, assocs.size());      // we still have 2 associations
    }

    @Test
    public void retypeTopicAndTraverseInstantiations() {
        DMXTransaction tx = dmx.beginTx();
        Topic type;
        List<RelatedTopic> topics;
        try {
            type = dmx.getTopicByUri(DATA_TYPE);
            topics = getTopicInstancesByTraversal(type);
            assertEquals(7, topics.size());
            //
            // retype topic
            Topic topic = topics.get(0);
            assertEquals(DATA_TYPE, topic.getTypeUri());
            topic.setTypeUri(PLUGIN_NAME);
            assertEquals(PLUGIN_NAME, topic.getTypeUri());
            topic = dmx.getTopic(topic.getId());
            assertEquals(PLUGIN_NAME, topic.getTypeUri());
            //
            // re-execute query
            topics = getTopicInstancesByTraversal(type);
            assertEquals(6, topics.size());
            // ### Note: in contrast to the above 4 tests this time the Lucene index update *is* visible
            // ### within the transaction! This suggests the following hypothesis:
            // ###     index.remove(entity) operation *is* visible within the transaction
            // ###     index.remove(entity, key) operation is *not* visible within the transaction
            // ### For the moment this seems to be a Neo4j oddity. Needs to be confirmed.
            //
            // ### Update: meanwhile that hypothesis is falsified.
            // ### Actually the latter 3 test are in contrast to the former 2 ones.
            // ### One possible difference is whether the test content is created in the same transaction or not.
            //
            tx.success();
        } finally {
            tx.finish();
        }
        // re-execute query
        topics = getTopicInstancesByTraversal(type);
        assertEquals(6, topics.size());
        // ### Note: the Lucene index update was already visible within the transaction!
    }

    // ---

    @Test
    public void updateAggregationOne() {
        DMXTransaction tx = dmx.beginTx();
        TopicImpl comp1;    // Note: has() is internal API, so we need a TopicImpl here
        Topic item1, item2;
        try {
            // 1) define composite type
            // child types
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.name", "Name", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.item", "Item", TEXT));
            // parent type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.composite", "Composite", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.composite", "dmx.test.name", ONE
                ))
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.composite", "dmx.test.item", ONE
                ))
            );
            // 2) create example child instances
            item1 = dmx.createTopic(mf.newTopicModel("dmx.test.item", new SimpleValue("Item 1")));
            item2 = dmx.createTopic(mf.newTopicModel("dmx.test.item", new SimpleValue("Item 2")));
            // 3) create composite instance
            comp1 = dmx.createTopic(mf.newTopicModel("dmx.test.composite", mf.newChildTopicsModel()
                .set("dmx.test.name", "Composite 1")
                // ### .setRef("dmx.test.item", item1.getId())
            ));
            tx.success();
        } finally {
            tx.finish();
        }
        // check memory
        assertEquals("Composite 1", comp1.getChildTopics().getString("dmx.test.name"));
        assertFalse(                comp1.getChildTopics().has("dmx.test.item"));
        comp1.loadChildTopics();
        assertFalse(                comp1.getChildTopics().has("dmx.test.item"));
        assertEquals(2, dmx.getTopicsByType("dmx.test.item").size());
        //
        // update and check again
        tx = dmx.beginTx();
        try {
            comp1.update(mf.newTopicModel(mf.newChildTopicsModel()
                .setRef("dmx.test.item", item2.getId())
            ));
            // Note: this would be more easy and have the same effect:
            // comp1.getChildTopics().setRef("dmx.test.item", item2.getId());
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertEquals("Composite 1", comp1.getChildTopics().getString("dmx.test.name"));
        assertTrue(                 comp1.getChildTopics().has("dmx.test.item"));
        assertEquals("Item 2",      comp1.getChildTopics().getString("dmx.test.item"));
        assertEquals(item2.getId(), comp1.getChildTopics().getTopic("dmx.test.item").getId());
        assertEquals(2, dmx.getTopicsByType("dmx.test.item").size());
        //
        // update and check again
        tx = dmx.beginTx();
        try {
            comp1.update(mf.newTopicModel(mf.newChildTopicsModel()
                .setRef("dmx.test.item", item1.getId())
            ));
            // Note: this would be more easy and have the same effect:
            // comp1.getChildTopics().setRef("dmx.test.item", item1.getId());
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertEquals("Composite 1", comp1.getChildTopics().getString("dmx.test.name"));
        assertTrue(                 comp1.getChildTopics().has("dmx.test.item"));
        assertEquals("Item 1",      comp1.getChildTopics().getString("dmx.test.item"));
        assertEquals(item1.getId(), comp1.getChildTopics().getTopic("dmx.test.item").getId());
        assertEquals(2, dmx.getTopicsByType("dmx.test.item").size());
    }

    @Test
    public void updateAggregationOneFacet() {
        DMXTransaction tx = dmx.beginTx();
        TopicImpl name;     // Note: has() is internal API, so we need a TopicImpl here
        Topic item1, item2;
        try {
            // 1) define facet
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.item", "Item", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.item_facet", "Item Facet", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.item_facet", "dmx.test.item", ONE
                ))
            );
            // 2) create example facet values
            item1 = dmx.createTopic(mf.newTopicModel("dmx.test.item", new SimpleValue("Item 1")));
            item2 = dmx.createTopic(mf.newTopicModel("dmx.test.item", new SimpleValue("Item 2")));
            // 3) define simple type + instance
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.name", "Name", TEXT));
            name = dmx.createTopic(mf.newTopicModel("dmx.test.name", new SimpleValue("Name 1")));
            //
            tx.success();
        } finally {
            tx.finish();
        }
        //
        CompDef compDef = dmx.getTopicType("dmx.test.item_facet").getCompDef("dmx.test.item");
        //
        // update facet
        tx = dmx.beginTx();
        try {
            name.updateChildTopics(
                mf.newChildTopicsModel().setRef("dmx.test.item", item1.getId()),
                compDef
            );
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertTrue(                 name.getChildTopics().has("dmx.test.item"));
        Topic facetValue = (Topic)  name.getChildTopics().get("dmx.test.item");
        assertEquals("Item 1",      facetValue.getSimpleValue().toString());
        assertEquals(item1.getId(), facetValue.getId());
        assertEquals(2, dmx.getTopicsByType("dmx.test.item").size());
        //
        // update facet again
        tx = dmx.beginTx();
        try {
            name.updateChildTopics(
                mf.newChildTopicsModel().setRef("dmx.test.item", item2.getId()),
                compDef
            );
            tx.success();
        } finally {
            tx.finish();
        }
        //
        assertTrue(                 name.getChildTopics().has("dmx.test.item"));
        facetValue = (Topic)        name.getChildTopics().get("dmx.test.item");
        assertEquals("Item 2",      facetValue.getSimpleValue().toString());
        assertEquals(item2.getId(), facetValue.getId());
        assertEquals(2, dmx.getTopicsByType("dmx.test.item").size());
    }

    // ---

    @Test
    public void createManyChildRefViaModel() {
        DMXTransaction tx = dmx.beginTx();
        Topic parent1, child1;
        try {
            // 1) define composite type
            // child type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.child", "Child", TEXT));
            // parent type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.parent", "Parent", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.parent", "dmx.test.child", MANY
                ))
            );
            // 2) create example child instance
            child1 = dmx.createTopic(mf.newTopicModel("dmx.test.child", new SimpleValue("Child 1")));
            // 3) create composite instance
            // Note: addRef() must be used (instead of setRef()) as child is defined as "many".
            parent1 = dmx.createTopic(mf.newTopicModel("dmx.test.parent", mf.newChildTopicsModel()
                .addRef("dmx.test.child", child1.getId())
            ));
            tx.success();
        } finally {
            tx.finish();
        }
        List<RelatedTopic> children = parent1.getChildTopics().getTopics("dmx.test.child");
        assertEquals(1, children.size());
        assertEquals(child1.getId(), children.get(0).getId());
        assertEquals("Child 1", children.get(0).getSimpleValue().toString());
    }

    @Test
    public void createManyChildRefViaObject() {
        DMXTransaction tx = dmx.beginTx();
        Topic parent1, child1;
        try {
            // 1) define parent type (with Aggregation-Many child definition)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.child", "Child", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.parent", "Parent", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.parent", "dmx.test.child", MANY
                ))
            );
            // 2) create child instance
            child1 = dmx.createTopic(mf.newTopicModel("dmx.test.child", new SimpleValue("Child 1")));
            // 3) create composite instance
            parent1 = dmx.createTopic(mf.newTopicModel("dmx.test.parent"));
            parent1.update(mf.newChildTopicsModel().addRef("dmx.test.child", child1.getId()));
            tx.success();
        } finally {
            tx.finish();
        }
        List<RelatedTopic> children = parent1.getChildTopics().getTopics("dmx.test.child");
        assertEquals(1, children.size());
        assertEquals(child1.getId(), children.get(0).getId());
        assertEquals("Child 1", children.get(0).getSimpleValue().toString());
    }

    @Test
    public void createManyChildViaObject() {
        DMXTransaction tx = dmx.beginTx();
        Topic parent1;
        try {
            // 1) define composite type
            // child type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.child", "Child", TEXT));
            // parent type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.parent", "Parent", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.parent", "dmx.test.child", MANY
                ))
            );
            // 2) create composite instance
            parent1 = dmx.createTopic(mf.newTopicModel("dmx.test.parent"));
            parent1.update(mf.newChildTopicsModel().add("dmx.test.child", "Child 1"));
            tx.success();
        } finally {
            tx.finish();
        }
        List<RelatedTopic> children = parent1.getChildTopics().getTopics("dmx.test.child");
        assertEquals(1, children.size());
        assertEquals("Child 1", children.get(0).getSimpleValue().toString());
    }

    // ---

    @Test
    public void createAndUpdateAggregationOne() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) define parent type (with Aggregation-One child definition)
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.child", "Child", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.parent", "Parent", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.parent", "dmx.test.child", ONE
                ))
            );
            // 2) create parent instance
            Topic parent1 = dmx.createTopic(mf.newTopicModel("dmx.test.parent", mf.newChildTopicsModel()
                .set("dmx.test.child", "Child 1")
            ));
            //
            assertEquals("Child 1", parent1.getChildTopics().getTopic("dmx.test.child").getSimpleValue().toString());
            // 3) update child topics
            parent1.update(mf.newChildTopicsModel().set("dmx.test.child", "Child 2"));
            //
            assertEquals("Child 2", parent1.getChildTopics().getTopic("dmx.test.child").getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void createCompositionWithChildRef() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) define composite type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.child", "Child", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.parent", "Parent", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.parent", "dmx.test.child", ONE
                ))
            );
            // 2) create child instance
            Topic child1 = dmx.createTopic(mf.newTopicModel("dmx.test.child", new SimpleValue("Child 1")));
            // 3) create parent instance
            Topic parent1 = dmx.createTopic(mf.newTopicModel("dmx.test.parent", mf.newChildTopicsModel()
                .setRef("dmx.test.child", child1.getId())
            ));
            //
            assertEquals("Child 1", parent1.getChildTopics().getTopic("dmx.test.child").getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void createAggregationWithChildRef() {
        DMXTransaction tx = dmx.beginTx();
        try {
            // 1) define composite type
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.child", "Child", TEXT));
            dmx.createTopicType(mf.newTopicTypeModel("dmx.test.parent", "Parent", ENTITY)
                .addCompDef(mf.newCompDefModel(
                    "dmx.test.parent", "dmx.test.child", ONE
                ))
            );
            // 2) create child instance
            Topic child1 = dmx.createTopic(mf.newTopicModel("dmx.test.child", new SimpleValue("Child 1")));
            // 3) create parent instance
            Topic parent1 = dmx.createTopic(mf.newTopicModel("dmx.test.parent", mf.newChildTopicsModel()
                .setRef("dmx.test.child", child1.getId())
            ));
            //
            assertEquals("Child 1", parent1.getChildTopics().getTopic("dmx.test.child").getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void deleteTopic() {
        DMXTransaction tx = dmx.beginTx();
        try {
            dmx.createTopic(mf.newTopicModel("dmx.test.t0", PLUGIN));
            //
            Topic t0 = dmx.getTopicByUri("dmx.test.t0");
            assertNotNull(t0);
            //
            t0.delete();
            t0 = dmx.getTopicByUri("dmx.test.t0");
            assertNull(t0);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void privilegedAccessAssignTopicToWorkspace() {
        DMXTransaction tx = dmx.beginTx();
        try {
            setupWorkspacesModel();
            //
            Topic t1 = dmx.createTopic(mf.newTopicModel(PLUGIN));
            Topic ws = dmx.createTopic(mf.newTopicModel(PLUGIN));
            //
            dmx.getPrivilegedAccess().assignToWorkspace(t1, ws.getId());
            //
            long wsId = (Long) t1.getProperty("dmx.workspaces.workspace_id");
            assertEquals(ws.getId(), wsId);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void privilegedAccessAssignAssocToWorkspace() {
        DMXTransaction tx = dmx.beginTx();
        try {
            setupWorkspacesModel();
            //
            Topic t1 = dmx.createTopic(mf.newTopicModel(PLUGIN));
            Topic t2 = dmx.createTopic(mf.newTopicModel(PLUGIN));
            Topic ws = dmx.createTopic(mf.newTopicModel(PLUGIN));
            Assoc assoc = createAssoc(t1, t2);
            //
            dmx.getPrivilegedAccess().assignToWorkspace(assoc, ws.getId());
            //
            long wsId = (Long) assoc.getProperty("dmx.workspaces.workspace_id");
            assertEquals(ws.getId(), wsId);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<Topic> getTopicInstances(String topicTypeUri) {
        return dmx.getTopicsByValue("typeUri", new SimpleValue(topicTypeUri));
    }

    private List<RelatedTopic> getTopicInstancesByTraversal(Topic type) {
        return type.getRelatedTopics(INSTANTIATION, TYPE, INSTANCE, type.getUri());
    }

    private List<RelatedAssoc> getAssocInstancesByTraversal(String assocTypeUri) {
        return dmx.getTopicByUri(assocTypeUri).getRelatedAssocs(INSTANTIATION, TYPE, INSTANCE, assocTypeUri);
    }

    private List<RelatedTopic> getChildTypes(Topic type) {
        return type.getRelatedTopics(COMPOSITION_DEF, PARENT_TYPE, CHILD_TYPE, TOPIC_TYPE);
    }

    // ---

    private void setupTestTopics() {
        Topic t0 = dmx.createTopic(mf.newTopicModel("dmx.test.t0", PLUGIN));
        Topic t1 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        Topic t2 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        Topic t3 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        createAssoc(t0, t1);
        createAssoc(t0, t2);
        createAssoc(t0, t3);
    }

    private void setupTestAssocs() {
        Topic t0 = dmx.createTopic(mf.newTopicModel("dmx.test.t0", PLUGIN));
        Topic t1 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        Topic t2 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        Topic t3 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        Topic t4 = dmx.createTopic(mf.newTopicModel(PLUGIN));
        Assoc a1 = createAssoc(t1, t2);
        Assoc a2 = createAssoc(t2, t3);
        Assoc a3 = createAssoc(t3, t4);
        createAssoc(t0, a1);
        createAssoc(t0, a2);
        createAssoc(t0, a3);
    }

    private void setupWorkspacesModel() {
        dmx.createAssocType(mf.newAssocTypeModel("dmx.workspaces.workspace_assignment", "Workspace Assignment", TEXT));
    }

    // ---

    private Assoc createAssoc(Topic topic1, Topic topic2) {
        return dmx.createAssoc(mf.newAssocModel(ASSOCIATION,
            mf.newTopicPlayerModel(topic1.getId(), DEFAULT),
            mf.newTopicPlayerModel(topic2.getId(), DEFAULT)
        ));
    }

    private Assoc createAssoc(Topic topic, Assoc assoc) {
        return dmx.createAssoc(mf.newAssocModel(ASSOCIATION,
            mf.newTopicPlayerModel(topic.getId(), DEFAULT),
            mf.newAssocPlayerModel(assoc.getId(), DEFAULT)
        ));
    }

    // ---

    private List<RelatedTopic> getTestTopics(Topic topic) {
        return topic.getRelatedTopics(ASSOCIATION, DEFAULT, DEFAULT, PLUGIN);
    }

    private List<RelatedAssoc> getTestAssocs(Topic topic) {
        return topic.getRelatedAssocs(ASSOCIATION, DEFAULT, DEFAULT, ASSOCIATION);
    }
}
