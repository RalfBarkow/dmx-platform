package de.deepamehta.core.impl;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.AssociationType;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;

import java.util.List;
import java.util.logging.Logger;



/**
 * An association type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociationType extends AttachedType implements AssociationType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociationType(AssociationTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** AssociationType Implementation ***
    // **************************************



    @Override
    public AssociationTypeModel getModel() {
        return (AssociationTypeModel) super.getModel();
    }

    @Override
    public void update(AssociationTypeModel model) {
        logger.info("Updating association type \"" + getUri() + "\" (new " + model + ")");
        // Note: the UPDATE_ASSOCIATION_TYPE directive must be added *before* a possible UPDATE_TOPIC directive (added
        // by super.update()). In case of a changed type URI the webclient's type cache must be updated *before*
        // the AssociationTypeRenderer can render the type.
        Directives.get().add(Directive.UPDATE_ASSOCIATION_TYPE, this);
        //
        super.update(model);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods



    // === AttachedTopic Overrides ===

    @Override
    final String className() {
        return "association type";
    }



    // === Implementation of abstract AttachedType methods ===

    @Override
    final void putInTypeCache() {
        dms.typeCache.putAssociationType(this);
    }

    @Override
    final void removeFromTypeCache() {
        dms.typeCache.removeAssociationType(getUri());
    }

    // ---

    @Override
    final Directive getDeleteTypeDirective() {
        return Directive.DELETE_ASSOCIATION_TYPE;
    }

    @Override
    final List<? extends DeepaMehtaObject> getAllInstances() {
        return dms.getAssociations(getUri()).getItems();
    }
}
