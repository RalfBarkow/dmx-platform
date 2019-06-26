package systems.dmx.core.impl;

import systems.dmx.core.Assoc;
import systems.dmx.core.AssocPlayer;



/**
 * An association player that is attached to the {@link AccessLayer}.
 */
class AssocPlayerImpl extends PlayerImpl implements AssocPlayer {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssocPlayerImpl(AssocPlayerModelImpl model, AssocModelImpl assoc) {
        super(model, assoc);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === AssocPlayer Implementation ===

    @Override
    public Assoc getAssoc() {
        return (Assoc) getDMXObject();
    }



    // === PlayerImpl Overrides ===

    @Override
    public AssocPlayerModelImpl getModel() {
        return (AssocPlayerModelImpl) model;
    }
}
