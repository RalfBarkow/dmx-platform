package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.storage.spi.DMXTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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



public class PersonTest extends CoreServiceTestEnvironment {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------------- Tests

    @Test
    public void labelRules() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            Topic person = createPerson();
            // one Person and one Address exists
            List<Topic> persons   = dmx.getTopicsByType("dmx.contacts.person");
            List<Topic> addresses = dmx.getTopicsByType("dmx.contacts.address");
            assertEquals(1, persons.size());
            assertEquals(1, addresses.size());
            // labels are concatenated
            ChildTopics children = persons.get(0).getChildTopics();
            assertEquals("Jörg Richter", children.getString("dmx.contacts.person_name"));
            assertEquals("Parkstr. 3 13187 Berlin Germany",
                                         children.getTopics("dmx.contacts.address#dmx.contacts.address_entry")
                                            .get(0).getSimpleValue().toString());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void immutability() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            Topic person = createPerson();
            Topic address = person.getChildTopics().getTopics("dmx.contacts.address#dmx.contacts.address_entry").get(0);
            // this looks like we override "Berlin" with "Hamburg"
            address.getChildTopics().set("dmx.contacts.city", "Hamburg");
            // ... BUT the original address is unchanged
            assertEquals("Berlin", address.getChildTopics().getString("dmx.contacts.city"));
            // ... and another Address topic has been created
            List<Topic> addrs = dmx.getTopicsByType("dmx.contacts.address");
            assertEquals(2, addrs.size());
            Topic address2 = addrs.get(0).getId() == address.getId() ? addrs.get(1) : addrs.get(0);
            assertNotEquals(address.getId(), address2.getId());
            // there are 2 City topics now
            assertEquals(2, dmx.getTopicsByType("dmx.contacts.city").size());
            //
            assertEquals("Hamburg", address2.getChildTopics().getString("dmx.contacts.city"));
            assertEquals(1, address2.getChildTopics().size());
            assertEquals(4, address.getChildTopics().size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void mutateEntity() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            assertEquals("<p>Software Developer</p>", children.getString("dmx.contacts.person_description"));
            // change description in-place
            children.set("dmx.contacts.person_description", "<p>Cook</p>");
            assertEquals("<p>Cook</p>", children.getString("dmx.contacts.person_description"));
            //
            // there is still only 1 person in the DB (it was mutated in-place), refetch ...
            List<Topic> persons = dmx.getTopicsByType("dmx.contacts.person");
            assertEquals(1, persons.size());
            // no children are loaded yet
            person = persons.get(0);
            children = person.getChildTopics();
            assertEquals(0, children.size());
            // the description has changed in-place
            assertEquals("<p>Cook</p>", children.getString("dmx.contacts.person_description"));
            assertEquals(1, children.size());
            // person name and address are still there
            person.loadChildTopics();
            assertEquals(3, children.size());
            // now there are 2 description topics in the DB (the original one is not mutated/deleted)
            List<Topic> descriptions = dmx.getTopicsByType("dmx.contacts.person_description");
            assertEquals(2, descriptions.size());
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Test
    public void mutateEntityComposite() {
        DMXTransaction tx = dmx.beginTx();
        try {
            definePersonModel();
            // create Person
            Topic person = createPerson();
            ChildTopics children = person.getChildTopics();
            // change Last Name -- DON'T DO IT THIS WAY (it produces unexpected result)
            children.getChildTopics("dmx.contacts.person_name").set("dmx.contacts.last_name", "Damm");
            // last name is still unchanged
            assertEquals("Richter", children.getChildTopics("dmx.contacts.person_name").getString("dmx.contacts.last_name"));
            //
            // now there are 2 Person Name topics in the DB, the 2nd has only Last Name (no First Name)
            assertEquals(2, dmx.getTopicsByType("dmx.contacts.person_name").size());
            assertEquals(2, dmx.getTopicsByType("dmx.contacts.last_name").size());
            assertEquals(1, dmx.getTopicsByType("dmx.contacts.first_name").size());
            // Last Name "Damm" is assigned to a Person Name parent
            Topic lastName = dmx.getTopicByValue("dmx.contacts.last_name", new SimpleValue("Damm"));
            Topic personName = lastName.getRelatedTopic(COMPOSITION, CHILD, PARENT, "dmx.contacts.person_name");
            assertNotNull(personName);
            // ... which has no First Name (but only a Last Name)
            assertNull(personName.getChildTopics().getString("dmx.contacts.first_name", null));
            assertEquals("Damm", personName.getChildTopics().getString("dmx.contacts.last_name"));
            // ... and is not assigned to any Person
            Topic person2 = personName.getRelatedTopic(COMPOSITION, CHILD, PARENT, "dmx.contacts.person");
            assertNull(person2);
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void definePersonModel() {
        defineAddressModel();
        // Person Name
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.first_name",  "First Name",  TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.last_name",   "Last Name",   TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.person_name", "Person Name", VALUE)
          .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.person_name", "dmx.contacts.first_name", ONE))
          .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.person_name", "dmx.contacts.last_name",  ONE))
        );
        // Person
        dmx.createAssocType(mf.newAssocTypeModel("dmx.contacts.address_entry",      "Address Entry",      TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.person_description", "Person Description", HTML));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.person",             "Person",             IDENTITY)
          .addCompDef(mf.newCompDefModel(null, true, false, "dmx.contacts.person", "dmx.contacts.person_name", ONE))
          .addCompDef(mf.newCompDefModel("dmx.contacts.address_entry", false, false,
                                         "dmx.contacts.person", "dmx.contacts.address",            MANY))
          .addCompDef(mf.newCompDefModel("dmx.contacts.person", "dmx.contacts.person_description", ONE))
        );
    }

    private void defineAddressModel() {
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.street",      "Street",      TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.postal_code", "Postal Code", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.city",        "City",        TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.country",     "Country",     TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.contacts.address", "Address", VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.street",      ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.postal_code", ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.city",        ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.contacts.address", "dmx.contacts.country",     ONE))
        );
    }

    private Topic createPerson() {
        return dmx.createTopic(mf.newTopicModel("dmx.contacts.person", mf.newChildTopicsModel()
            .set("dmx.contacts.person_name", mf.newChildTopicsModel()
                .set("dmx.contacts.first_name", "Jörg")
                .set("dmx.contacts.last_name",  "Richter"))
            .add("dmx.contacts.address#dmx.contacts.address_entry", mf.newChildTopicsModel()
                .set("dmx.contacts.street",      "Parkstr. 3")
                .set("dmx.contacts.postal_code", "13187")
                .set("dmx.contacts.city",        "Berlin")
                .set("dmx.contacts.country",     "Germany"))
            .set("dmx.contacts.person_description", "<p>Software Developer</p>")
        ));
    }
}