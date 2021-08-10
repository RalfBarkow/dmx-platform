package systems.dmx.webclient;

import static systems.dmx.webclient.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.DMXType;
import systems.dmx.core.RoleType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.ViewConfig;
import systems.dmx.core.model.AssocTypeModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.RoleTypeModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicTypeModel;
import systems.dmx.core.model.TypeModel;
import systems.dmx.core.model.ViewConfigModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.ChangeReport;
import systems.dmx.core.service.Directive;
import systems.dmx.core.service.Directives;
import systems.dmx.core.service.event.AllPluginsActive;
import systems.dmx.core.service.event.IntroduceAssocType;
import systems.dmx.core.service.event.IntroduceRoleType;
import systems.dmx.core.service.event.IntroduceTopicType;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.service.event.PreCreateAssocType;
import systems.dmx.core.service.event.PreCreateRoleType;
import systems.dmx.core.service.event.PreCreateTopicType;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;



public class WebclientPlugin extends PluginActivator implements AllPluginsActive,
                                                                IntroduceTopicType,
                                                                IntroduceAssocType,
                                                                IntroduceRoleType,
                                                                PreCreateTopicType,
                                                                PreCreateAssocType,
                                                                PreCreateRoleType,
                                                                PostUpdateTopic {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String VIEW_CONFIG_LABEL = "View Configuration";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private boolean hasWebclientLaunched = false;
    private long time = System.currentTimeMillis();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // Listeners

    @Override
    public void allPluginsActive() {
        String webclientUrl = getWebclientUrl();
        //
        if (hasWebclientLaunched == true) {
            logger.info("### Launching DMX Webclient (" + webclientUrl + ") SKIPPED -- already launched");
            return;
        }
        //
        try {
            logger.info("DMX platform started in " + (System.currentTimeMillis() - time) / 1000f + " sec");
            logger.info("### Launching DMX Webclient: " + webclientUrl);
            Desktop.getDesktop().browse(new URI(webclientUrl));
            hasWebclientLaunched = true;
        } catch (Exception e) {
            logger.warning("### Launching DMX Webclient failed: " + e);
            logger.warning("### To launch it manually: " + webclientUrl);
        }
    }

    /**
     * Add a default view config to the type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a type (including its components like the view config) is
     * performed by the type-introduction hook of the Workspaces module. Here we use the pre-create-type hook (instead
     * of type-introduction too) as the pre-create-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateTopicType(TopicTypeModel model) {
        addDefaultViewConfig(model);
    }

    /**
     * Add a default view config to the type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a type (including its components like the view config) is
     * performed by the type-introduction hook of the Workspaces module. Here we use the pre-create-type hook (instead
     * of type-introduction too) as the pre-create-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateAssocType(AssocTypeModel model) {
        addDefaultViewConfig(model);
    }

    /**
     * Add a default view config to the role type in case no one is set.
     * <p>
     * Note: the default view config needs a workspace assignment. The default view config must be added *before* the
     * assignment can take place. Workspace assignment for a role type (including its view config) is performed by the
     * type-introduction hook of the Workspaces module. Here we use the pre-create-role-type hook (instead of type-
     * introduction too) as the pre-create-role-type hook is guaranteed to be invoked *before* type-introduction.
     * On the other hand the order of type-introduction invocations is not deterministic accross plugins.
     */
    @Override
    public void preCreateRoleType(RoleTypeModel model) {
        addDefaultViewConfigTopic(model.getViewConfig());
    }

    // ---

    /**
     * Once a view config topic is updated we must update the cached view config and inform the webclient.
     */
    @Override
    public void postUpdateTopic(Topic topic, ChangeReport report, TopicModel updateModel) {
        if (topic.getTypeUri().equals(VIEW_CONFIG)) {
            setDefaultConfigTopicLabel(topic);
            updateTypeCacheAndAddDirective(topic);
        }
    }

    // ---

    @Override
    public void introduceTopicType(TopicType topicType) {
        setViewConfigLabel(topicType);
    }

    @Override
    public void introduceAssocType(AssocType assocType) {
        setViewConfigLabel(assocType);
    }

    @Override
    public void introduceRoleType(RoleType roleType) {
        setViewConfigLabel(roleType);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === View Configuration ===

    /**
     * Updates type cache according to the given view config topic, and adds an UPDATE-TYPE directive.
     * Called once a view config topic has been updated.
     *
     * Determines the type and possibly the comp def the given view config topic belongs to.
     */
    private void updateTypeCacheAndAddDirective(Topic viewConfigTopic) {
        // type to be updated (topic type or assoc type)
        Topic type = viewConfigTopic.getRelatedTopic(COMPOSITION, CHILD, PARENT, null);
        // ID of the comp def to be updated. -1 if the update does not target an comp def (but a type).
        long compDefId = -1;
        if (type == null) {
            Assoc compDef = viewConfigTopic.getRelatedAssoc(COMPOSITION, CHILD, PARENT, COMPOSITION_DEF);
            if (compDef == null) {
                throw new RuntimeException("Orphaned view config topic: " + viewConfigTopic);
            }
            type = (Topic) compDef.getDMXObjectByRole(PARENT_TYPE);
            compDefId = compDef.getId();
        }
        //
        String typeUri = type.getTypeUri();
        if (typeUri.equals(TOPIC_TYPE) || typeUri.equals(META_TYPE)) {
            _updateTypeCacheAndAddDirective(
                dmx.getTopicType(type.getUri()),
                compDefId, viewConfigTopic, Directive.UPDATE_TOPIC_TYPE
            );
        } else if (typeUri.equals(ASSOC_TYPE)) {
            _updateTypeCacheAndAddDirective(
                dmx.getAssocType(type.getUri()),
                compDefId, viewConfigTopic, Directive.UPDATE_ASSOC_TYPE
            );
        } else if (typeUri.equals(ROLE_TYPE)) {
            Directives.get().add(Directive.UPDATE_ROLE_TYPE, dmx.getRoleType(type.getUri()));
        } else {
            throw new RuntimeException("View config " + viewConfigTopic.getId() + " is associated unexpectedly, type=" +
                type + ", compDefId=" + compDefId + ", viewConfigTopic=" + viewConfigTopic);
        }
    }

    private void _updateTypeCacheAndAddDirective(DMXType type, long compDefId, Topic viewConfigTopic, Directive dir) {
        logger.info("### Updating view config of type \"" + type.getUri() + "\", compDefId=" + compDefId);
        updateTypeCache(type.getModel(), compDefId, viewConfigTopic.getModel());
        Directives.get().add(dir, type);
    }

    /**
     * Overrides the cached view config topic for the given type/comp def with the given view config topic.
     */
    private void updateTypeCache(TypeModel type, long compDefId, TopicModel viewConfigTopic) {
        ViewConfigModel vcm;
        if (compDefId == -1) {
            vcm = type.getViewConfig();
        } else {
            vcm = getCompDef(type, compDefId).getViewConfig();
        }
        vcm.updateConfigTopic(viewConfigTopic);
    }

    // --- Label ---

    private void setViewConfigLabel(DMXType type) {
        // type
        setViewConfigLabel(type.getViewConfig());
        // comp defs
        for (String compDefUri : type) {
            setViewConfigLabel(type.getCompDef(compDefUri).getViewConfig());
        }
    }

    private void setViewConfigLabel(RoleType roleType) {
        setViewConfigLabel(roleType.getViewConfig());
    }

    private void setViewConfigLabel(ViewConfig viewConfig) {
        for (Topic configTopic : viewConfig.getConfigTopics()) {
            setDefaultConfigTopicLabel(configTopic);
        }
    }

    private void setDefaultConfigTopicLabel(Topic viewConfigTopic) {
        viewConfigTopic.setSimpleValue(VIEW_CONFIG_LABEL);
    }

    // --- Default Config Topic ---

    /**
     * Adds a default view config topic to the given type (and its comp defs) in case no one is set already.
     * <p>
     * This ensures a programmatically created type (through a migration) will
     * have a view config in any case, for being edited interactively afterwards.
     */
    private void addDefaultViewConfig(TypeModel typeModel) {
        // type
        addDefaultViewConfigTopic(typeModel.getViewConfig());
        // comp defs
        for (String compDefUri : typeModel) {
            addDefaultViewConfigTopic(typeModel.getCompDef(compDefUri).getViewConfig());
        }
    }

    private void addDefaultViewConfigTopic(ViewConfigModel viewConfig) {
        if (viewConfig.getConfigTopic(VIEW_CONFIG) == null) {
            viewConfig.addConfigTopic(mf.newTopicModel(VIEW_CONFIG));
        }
    }



    // === Webclient Start ===

    private String getWebclientUrl() {
        boolean isHttpsEnabled = Boolean.getBoolean("org.apache.felix.https.enable");
        String protocol, port;
        if (isHttpsEnabled) {
            // Note: if both protocols are enabled HTTPS takes precedence
            protocol = "https";
            port = System.getProperty("org.osgi.service.http.port.secure");
        } else {
            protocol = "http";
            port = System.getProperty("org.osgi.service.http.port");
        }
        return protocol + "://localhost:" + port + "/systems.dmx.webclient/";
    }



    // === Misc ===

    /**
     * Looks up an comp def by ID.
     */
    private CompDefModel getCompDef(TypeModel type, long compDefId) {
        for (CompDefModel compDef : type.getCompDefs()) {
            if (compDef.getId() == compDefId) {
                return compDef;
            }
        }
        throw new RuntimeException("Comp def " + compDefId + " not found in type \"" + type.getUri() + "\"");
    }
}
