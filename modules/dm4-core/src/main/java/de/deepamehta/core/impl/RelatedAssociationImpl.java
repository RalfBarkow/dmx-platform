package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.model.RelatedAssociationModel;



/**
 * An Association-Association pair that is attached to the {@link DeepaMehtaService}.
 */
class RelatedAssociationImpl extends AssociationImpl implements RelatedAssociation {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Association relatingAssoc;      // Attached object cache

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedAssociationImpl(RelatedAssociationModel model, EmbeddedService dms) {
        super(model, dms);
        this.relatingAssoc = new AssociationImpl(model.getRelatingAssociation(), dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Association getRelatingAssociation() {
        return relatingAssoc;
    }

    @Override
    public RelatedAssociationModel getModel() {
        return (RelatedAssociationModel) super.getModel();
    }
}
