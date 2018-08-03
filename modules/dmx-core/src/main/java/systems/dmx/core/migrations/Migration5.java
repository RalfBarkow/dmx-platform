package systems.dmx.core.migrations;

import systems.dmx.core.service.Migration;



/**
 * Adds child type "Association Type" to association type "Composition Definition".
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 */
public class Migration5 extends Migration {

    @Override
    public void run() {
        dmx.getAssociationType("dmx.core.composition_def").addAssocDefBefore(
            mf.newAssociationDefinitionModel("dmx.core.composition_def", "dmx.core.custom_assoc_type", false, false,
                "dmx.core.composition_def", "dmx.core.assoc_type", "dmx.core.many", "dmx.core.one"
            ),
            "dmx.core.identity_attr"
        );
    }
}
