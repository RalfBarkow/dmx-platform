package systems.dmx.workspaces;

import static systems.dmx.workspaces.Constants.*;
import systems.dmx.config.ConfigDef;
import systems.dmx.config.ConfigModRole;
import systems.dmx.config.ConfigService;
import systems.dmx.config.ConfigTarget;
import systems.dmx.facets.FacetsService;
import systems.dmx.topicmaps.TopicmapsService;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.Assoc;
import systems.dmx.core.AssocType;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RoleType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Cookies;
import systems.dmx.core.service.DirectivesResponse;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.accesscontrol.SharingMode;
import systems.dmx.core.service.event.IntroduceAssocType;
import systems.dmx.core.service.event.IntroduceRoleType;
import systems.dmx.core.service.event.IntroduceTopicType;
import systems.dmx.core.service.event.PostCreateAssoc;
import systems.dmx.core.service.event.PostCreateTopic;
import systems.dmx.core.service.event.PreDeleteTopic;

import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;



@Path("/workspaces")
@Consumes("application/json")
@Produces("application/json")
public class WorkspacesPlugin extends PluginActivator implements WorkspacesService, IntroduceTopicType,
                                                                                    IntroduceAssocType,
                                                                                    IntroduceRoleType,
                                                                                    PostCreateTopic,
                                                                                    PostCreateAssoc,
                                                                                    PreDeleteTopic {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final boolean SHARING_MODE_PRIVATE_ENABLED = Boolean.parseBoolean(
        System.getProperty("dmx.workspaces.private.enabled", "true"));
    private static final boolean SHARING_MODE_CONFIDENTIAL_ENABLED = Boolean.parseBoolean(
        System.getProperty("dmx.workspaces.confidential.enabled", "true"));
    private static final boolean SHARING_MODE_COLLABORATIVE_ENABLED = Boolean.parseBoolean(
        System.getProperty("dmx.workspaces.collaborative.enabled", "true"));
    private static final boolean SHARING_MODE_PUBLIC_ENABLED = Boolean.parseBoolean(
        System.getProperty("dmx.workspaces.public.enabled", "true"));
    private static final boolean SHARING_MODE_COMMON_ENABLED = Boolean.parseBoolean(
        System.getProperty("dmx.workspaces.common.enabled", "true"));
    // Note: the default values are required in case no config file is in effect. This applies when DM is started
    // via feature:install from Karaf. The default values must match the values defined in project POM.

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private FacetsService facetsService;

    @Inject
    private TopicmapsService topicmapsService;

    @Inject
    private ConfigService configService;

    private Messenger me = new Messenger();

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************
    // *** WorkspacesService ***
    // *************************



    @POST
    @Transactional
    @Override
    public Topic createWorkspace(@QueryParam("name") final String name, @QueryParam("uri") final String uri,
                                 @QueryParam("sharingModeUri") final SharingMode sharingMode) {
        final String operation = "Creating workspace \"" + name + "\"";
        final String info = ", uri=" + uri + ", sharingMode=" + sharingMode;
        try {
            // We suppress standard workspace assignment here as 1) a workspace itself gets no assignment at all,
            // and 2) the workspace's default topicmap requires a special assignment. See step 2) below.
            Topic workspace = dmx.getPrivilegedAccess().runInWorkspaceContext(-1, () -> {
                logger.info(operation + info);
                //
                // 1) create workspace
                Topic _workspace = dmx.createTopic(
                    mf.newTopicModel(uri, WORKSPACE, mf.newChildTopicsModel()
                        .set(WORKSPACE_NAME, name)
                        .setRef(SHARING_MODE, sharingMode.getUri())));
                //
                // 2) create default topicmap and assign to workspace
                Topic topicmap = topicmapsService.createTopicmap(
                    TopicmapsService.DEFAULT_TOPICMAP_NAME,
                    TopicmapsService.DEFAULT_TOPICMAP_TYPE_URI,
                    null    // viewProps=null
                );
                // Note: user <anonymous> has no READ access to the workspace just created as it has no owner.
                // So we must use the privileged assignToWorkspace() call here. This is to support the
                // "DMX Sign-up" 3rd-party plugin.
                dmx.getPrivilegedAccess().assignToWorkspace(topicmap, _workspace.getId());
                //
                return _workspace;
            });
            me.newWorkspace(workspace);     // FIXME: broadcast to eligible users only
            return workspace;
        } catch (Exception e) {
            throw new RuntimeException(operation + " failed" + info, e);
        }
    }

    // ---

    // Note: the "children" query parameter is handled by core's JerseyResponseFilter
    @GET
    @Path("/{uri}")
    @Override
    public Topic getWorkspace(@PathParam("uri") String uri) {
        return dmx.getPrivilegedAccess().getWorkspace(uri);
    }

    // Note: the "children" query parameter is handled by core's JerseyResponseFilter
    @GET
    @Path("/object/{id}")
    @Override
    public Topic getAssignedWorkspace(@PathParam("id") long objectId) {
        long workspaceId = getAssignedWorkspaceId(objectId);
        if (workspaceId == -1) {
            return null;
        }
        return dmx.getTopic(workspaceId);
    }

    // ---

    // Note: part of REST API, not part of OSGi service
    @PUT
    @Path("/{workspaceId}/object/{objectId}")
    @Transactional
    public DirectivesResponse assignToWorkspace(@PathParam("objectId") long objectId,
                                                @PathParam("workspaceId") long workspaceId) {
        try {
            DMXObject object = dmx.getObject(objectId);
            checkAssignmentArgs(object, workspaceId);
            __assignToWorkspace(object, workspaceId);
            return new DirectivesResponse();
        } catch (Exception e) {
            throw new RuntimeException("Assigning object " + objectId + " to workspace " + workspaceId + " failed", e);
        }
    }

    @Override
    public void assignToWorkspace(DMXObject object, long workspaceId) {
        try {
            checkAssignmentArgs(object, workspaceId);
            __assignToWorkspace(object, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + info(object) + " to workspace " + workspaceId + " failed", e);
        }
    }

    @Override
    public void assignTypeToWorkspace(DMXType type, long workspaceId) {
        try {
            checkAssignmentArgs(type, workspaceId);
            __assignToWorkspace(type, workspaceId);
            // view config topics
            for (Topic configTopic : type.getViewConfig().getConfigTopics()) {
                __assignToWorkspace(configTopic, workspaceId);
            }
            // comp defs
            for (CompDef compDef : type.getCompDefs()) {
                __assignToWorkspace(compDef, workspaceId);
                // view config topics (of comp def)
                for (Topic configTopic : compDef.getViewConfig().getConfigTopics()) {
                    __assignToWorkspace(configTopic, workspaceId);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + info(type) + " to workspace " + workspaceId + " failed", e);
        }
    }

    @Override
    public void assignRoleTypeToWorkspace(RoleType roleType, long workspaceId) {
        try {
            checkAssignmentArgs(roleType, workspaceId);
            __assignToWorkspace(roleType, workspaceId);
            // view config topics
            for (Topic configTopic : roleType.getViewConfig().getConfigTopics()) {
                __assignToWorkspace(configTopic, workspaceId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + info(roleType) + " to workspace " + workspaceId + " failed", e);
        }
    }

    // ---

    // Note: the "children" query parameter is handled by core's JerseyResponseFilter
    @GET
    @Path("/{id}/topics")
    @Override
    public List<Topic> getAssignedTopics(@PathParam("id") long workspaceId) {
        return dmx.getTopicsByProperty(PROP_WORKSPACE_ID, workspaceId);
    }

    // Note: the "children" query parameter is handled by core's JerseyResponseFilter
    @GET
    @Path("/{id}/assocs")
    @Override
    public List<Assoc> getAssignedAssocs(@PathParam("id") long workspaceId) {
        return dmx.getAssocsByProperty(PROP_WORKSPACE_ID, workspaceId);
    }

    // ---

    // Note: the "children" query parameter is handled by core's JerseyResponseFilter
    @GET
    @Path("/{id}/topics/{topicTypeUri}")
    @Override
    public List<Topic> getAssignedTopics(@PathParam("id") long workspaceId,
                                         @PathParam("topicTypeUri") String topicTypeUri) {
        // TODO: optimization. Use getRelatedTopics() by using "Workspace Assignment" type.
        List<Topic> topics = dmx.getTopicsByType(topicTypeUri);
        applyWorkspaceFilter(topics.iterator(), workspaceId);
        return topics;
    }

    // Note: the "children" query parameter is handled by core's JerseyResponseFilter
    @GET
    @Path("/{id}/assocs/{assocTypeUri}")
    @Override
    public List<Assoc> getAssignedAssocs(@PathParam("id") long workspaceId,
                                         @PathParam("assocTypeUri") String assocTypeUri) {
        List<Assoc> assocs = dmx.getAssocsByType(assocTypeUri);
        applyWorkspaceFilter(assocs.iterator(), workspaceId);
        return assocs;
    }



    // *************
    // *** Hooks ***
    // *************



    @Override
    public void preInstall() {
        configService.registerConfigDef(new ConfigDef(
            // TODO: can't use AC constants -> cyclic dependency
            // TODO: move registration to AC module?
            ConfigTarget.TYPE_INSTANCES, "dmx.accesscontrol.username",
            mf.newTopicModel("dmx.workspaces.enabled_sharing_modes", mf.newChildTopicsModel()
                .set("dmx.workspaces.private.enabled",       SHARING_MODE_PRIVATE_ENABLED)
                .set("dmx.workspaces.confidential.enabled",  SHARING_MODE_CONFIDENTIAL_ENABLED)
                .set("dmx.workspaces.collaborative.enabled", SHARING_MODE_COLLABORATIVE_ENABLED)
                .set("dmx.workspaces.public.enabled",        SHARING_MODE_PUBLIC_ENABLED)
                .set("dmx.workspaces.common.enabled",        SHARING_MODE_COMMON_ENABLED)
            ),
            ConfigModRole.ADMIN
        ));
    }

    @Override
    public void shutdown() {
        // Note 1: unregistering is crucial e.g. for redeploying the Workspaces plugin. The next register call
        // (at preInstall() time) would fail as the Config service already holds such a registration.
        // Note 2: we must check if the Config service is still available. If the Config plugin is redeployed the
        // Workspaces plugin is stopped/started as well but at shutdown() time the Config service is already gone.
        if (configService != null) {
            configService.unregisterConfigDef("dmx.workspaces.enabled_sharing_modes");
        }
    }



    // *****************
    // *** Listeners ***
    // *****************



    /**
     * Takes care the DMX standard types (and their parts) get an assignment to the DMX workspace.
     * This is important in conjunction with access control.
     * Note: type introduction is aborted if at least one of these conditions apply:
     *     - A workspace cookie is present. In this case the type gets its workspace assignment the regular way (this
     *       plugin's post-create listeners). This happens e.g. when a type is created interactively in the Webclient.
     *     - The type is not a DMX standard type. In this case the 3rd-party plugin developer is responsible
     *       for doing the workspace assignment (in case the type is created programmatically while a migration).
     *       DM can't know to which workspace a 3rd-party type belongs to. A type is regarded a DMX standard
     *       type if its URI begins with "dmx."
     */
    @Override
    public void introduceTopicType(TopicType topicType) {
        long workspaceId = workspaceIdForType(topicType);
        if (workspaceId == -1) {
            return;
        }
        //
        assignTypeToWorkspace(topicType, workspaceId);
    }

    /**
     * Takes care the DMX standard types (and their parts) get an assignment to the DMX workspace.
     * This is important in conjunction with access control.
     * Note: type introduction is aborted if at least one of these conditions apply:
     *     - A workspace cookie is present. In this case the type gets its workspace assignment the regular way (this
     *       plugin's post-create listeners). This happens e.g. when a type is created interactively in the Webclient.
     *     - The type is not a DMX standard type. In this case the 3rd-party plugin developer is responsible
     *       for doing the workspace assignment (in case the type is created programmatically while a migration).
     *       DM can't know to which workspace a 3rd-party type belongs to. A type is regarded a DMX standard
     *       type if its URI begins with "dmx."
     */
    @Override
    public void introduceAssocType(AssocType assocType) {
        long workspaceId = workspaceIdForType(assocType);
        if (workspaceId == -1) {
            return;
        }
        //
        assignTypeToWorkspace(assocType, workspaceId);
    }

    /**
     * Takes care the DMX standard types (and their parts) get an assignment to the DMX workspace.
     * This is important in conjunction with access control.
     * Note: type introduction is aborted if at least one of these conditions apply:
     *     - A workspace cookie is present. In this case the type gets its workspace assignment the regular way (this
     *       plugin's post-create listeners). This happens e.g. when a type is created interactively in the Webclient.
     *     - The type is not a DMX standard type. In this case the 3rd-party plugin developer is responsible
     *       for doing the workspace assignment (in case the type is created programmatically while a migration).
     *       DM can't know to which workspace a 3rd-party type belongs to. A type is regarded a DMX standard
     *       type if its URI begins with "dmx."
     */
    @Override
    public void introduceRoleType(RoleType roleType) {
        long workspaceId = workspaceIdForType(roleType);
        if (workspaceId == -1) {
            return;
        }
        //
        assignRoleTypeToWorkspace(roleType, workspaceId);
    }

    // ---

    /**
     * Standard workspace assignment for topics.
     */
    @Override
    public void postCreateTopic(Topic topic) {
        // Note: when editing a workspace its parts ("Workspace Name" and "Workspace Description") must not be assigned
        // to the workspace itself. This would create an endless recursion while bubbling the modification timestamp.
        if (isWorkspacePart(topic)) {
            return;
        }
        //
        long workspaceId = workspaceId(topic);
        // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
        // the DMX workspace. This would not help in gaining data consistency because the topics created
        // so far (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
        // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
        // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
        // the other hand we don't have such a mechanism (and don't want one either).
        if (workspaceId == -1) {
            return;
        }
        // Note: for an object's initial workspace assignment checking the object's WRITE permission would fail
        // as that permission is granted only by the very workspace assignment we're about to create.
        _assignToWorkspace(topic, workspaceId);
        //
        setEnabledSharingModesLabel(topic);
    }

    /**
     * Standard workspace assignment for assocs.
     */
    @Override
    public void postCreateAssoc(Assoc assoc) {
        // Note: we must avoid a vicious circle that would occur when the association is an workspace assignment.
        if (isWorkspaceAssignment(assoc)) {
            return;
        }
        //
        long workspaceId = workspaceId(assoc);
        // Note: when there is no current workspace (because no user is logged in) we do NOT fallback to assigning
        // the DMX workspace. This would not help in gaining data consistency because the associations created
        // so far (BEFORE the Workspaces plugin is activated) would still have no workspace assignment.
        // Note: for types the situation is different. The type-introduction mechanism (see introduceTopicType()
        // handler above) ensures EVERY type is catched (regardless of plugin activation order). For instances on
        // the other hand we don't have such a mechanism (and don't want one either).
        if (workspaceId == -1) {
            return;
        }
        // Note: for an object's initial workspace assignment checking the object's WRITE permission would fail
        // as that permission is granted only by the very workspace assignment we're about to create.
        _assignToWorkspace(assoc, workspaceId);
    }

    // ---

    /**
     * When a workspace is about to be deleted its entire content must be deleted.
     */
    @Override
    public void preDeleteTopic(Topic topic) {
        if (topic.getTypeUri().equals(WORKSPACE)) {
            long workspaceId = topic.getId();
            deleteWorkspaceContent(workspaceId);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private long workspaceId(DMXObject object) {
        // 1) Object model (Workspace Facet)
        TopicModel workspace = object.getModel().getChildTopics().getTopicOrNull(WORKSPACE + "#" + WORKSPACE_ASSIGNMENT
        );
        if (workspace != null) {
            logger.fine("==> " + info(object) + ": workspace " + workspace.getId() + " (from object model)");
            return workspace.getId();
        }
        // 2) Execution context
        Long workspaceId = dmx.getPrivilegedAccess().getWorkspaceContext();
        if (workspaceId != null) {
            logger.fine("==> " + info(object) + ": workspace " + workspaceId + " (from execution context)");
            return workspaceId;
        }
        // 3) Workspace cookie
        Cookies cookies = Cookies.get();
        if (cookies.has("dmx_workspace_id")) {
            workspaceId = cookies.getLong("dmx_workspace_id");
            logger.fine("--> " + info(object) + ": workspace " + workspaceId + " (from cookie)");
            return workspaceId;
        }
        //
        return -1;
    }

    /**
     * Returns the ID of the DMX workspace or -1 to signal abortion of type introduction.
     */
    private long workspaceIdForType(DMXObject object) {
        return workspaceId(object) == -1 && isDMXStandardType(object) ? getDMXWorkspaceId() : -1;
    }

    // ---

    private long getAssignedWorkspaceId(long objectId) {
        return dmx.getPrivilegedAccess().getAssignedWorkspaceId(objectId);
    }

    /**
     * Checks arguments -- except object writability --, and performs the actual workspace assignment then.
     * <p>
     * Used for *initial* workspace assignment, when object writability is created only through this very assignment.
     */
    private void _assignToWorkspace(DMXObject object, long workspaceId) {
        try {
            checkAssignmentArgs(null, workspaceId);     // object=null
            __assignToWorkspace(object, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException("Assigning " + info(object) + " to workspace " + workspaceId + " failed", e);
        }
    }

    /**
     * Performs the actual workspace assignment.
     */
    private void __assignToWorkspace(DMXObject object, long workspaceId) {
        // 1) create assignment association
        facetsService.updateFacet(object, WORKSPACE_FACET,
            mf.newFacetValueModel(WORKSPACE + "#" + WORKSPACE_ASSIGNMENT).setRef(workspaceId)
        );
        // Note: we are refering to an existing workspace. So we must set a topic *reference* (using setRef()).
        //
        // 2) store assignment property
        object.setProperty(PROP_WORKSPACE_ID, workspaceId, true);   // addToIndex=true
    }

    /**
     * Checks the args for an assign-to-workspace operation.
     * 3 checks are performed:
     *   - the workspace ID refers actually to a workspace
     *   - the workspace is writable
     *   - the object is writable
     *
     * If any check fails an exception is thrown.
     *
     * @param   object          the object to check; if null no object check is performed
     * @param   workspaceId     the workspace ID to check
     */
    private void checkAssignmentArgs(DMXObject object, long workspaceId) {
        Topic workspace = dmx.getTopic(workspaceId);
        String typeUri = workspace.getTypeUri();
        if (!typeUri.equals(WORKSPACE)) {
            throw new IllegalArgumentException("Topic " + workspaceId + " is not a workspace (but a \"" + typeUri +
                "\")");
        }
        //
        workspace.checkWriteAccess();       // throws AccessControlException
        if (object != null) {
            object.checkWriteAccess();      // throws AccessControlException
        }
    }

    // ---

    private void deleteWorkspaceContent(long workspaceId) {
        try {
            // 1) delete instances by type
            // Note: also instances assigned to other workspaces must be deleted
            for (Topic topicType : getAssignedTopics(workspaceId, TOPIC_TYPE)) {
                String typeUri = topicType.getUri();
                for (Topic topic : dmx.getTopicsByType(typeUri)) {
                    topic.delete();
                }
                dmx.getTopicType(typeUri).delete();
            }
            for (Topic assocType : getAssignedTopics(workspaceId, ASSOC_TYPE)) {
                String typeUri = assocType.getUri();
                for (Assoc assoc : dmx.getAssocsByType(typeUri)) {
                    assoc.delete();
                }
                dmx.getAssocType(typeUri).delete();
            }
            // 2) delete remaining instances
            for (Topic topic : getAssignedTopics(workspaceId)) {
                topic.delete();
            }
            for (Assoc assoc : getAssignedAssocs(workspaceId)) {
                assoc.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Deleting content of workspace " + workspaceId + " failed", e);
        }
    }

    // --- Helper ---

    private boolean isDMXStandardType(DMXObject object) {
        return object.getUri().startsWith("dmx.");
    }

    private boolean isWorkspacePart(Topic topic) {
        String typeUri = topic.getTypeUri();
        return typeUri.equals(WORKSPACE_NAME) ||
               typeUri.equals(WORKSPACE_DESCRIPTION);
    }

    private boolean isWorkspaceAssignment(Assoc assoc) {
        return assoc.getTypeUri().equals(WORKSPACE_ASSIGNMENT);
    }

    // ---

    /**
     * Returns the ID of the DMX workspace or throws an exception if it doesn't exist.
     */
    private long getDMXWorkspaceId() {
        return dmx.getPrivilegedAccess().getDMXWorkspaceId();
    }

    private void applyWorkspaceFilter(Iterator<? extends DMXObject> objects, long workspaceId) {
        while (objects.hasNext()) {
            DMXObject object = objects.next();
            if (getAssignedWorkspaceId(object.getId()) != workspaceId) {
                objects.remove();
            }
        }
    }

    private void setEnabledSharingModesLabel(Topic topic) {
        if (topic.getTypeUri().equals(ENABLED_SHARING_MODES)) {
            topic.setSimpleValue(ENABLED_SHARING_MODES_LABEL);
        }
    }

    // ---

    // ### FIXME: copied from Access Control
    // ### TODO: add shortInfo() to DMXObject interface
    private String info(DMXObject object) {
        if (object instanceof TopicType) {
            return "topic type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof AssocType) {
            return "association type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof RoleType) {
            return "role type \"" + object.getUri() + "\" (id=" + object.getId() + ")";
        } else if (object instanceof Topic) {
            return "topic " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\", uri=\"" + object.getUri() +
                "\")";
        } else if (object instanceof Assoc) {
            return "association " + object.getId() + " (typeUri=\"" + object.getTypeUri() + "\")";
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private class Messenger {

        private void newWorkspace(Topic workspace) {
            try {
                sendToReadAllowed(new JSONObject()
                    .put("type", "newWorkspace")
                    .put("args", new JSONObject()
                        .put("workspace", workspace.toJSON())
                    ), workspace.getId()
                );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while sending a \"newWorkspace\" message:", e);
            }
        }

        // ---

        private void sendToReadAllowed(JSONObject message, long objectId) {
            dmx.getWebSocketService().sendToReadAllowed(message.toString(), objectId);
        }
    }
}
