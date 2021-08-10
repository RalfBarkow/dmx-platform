import Vue from 'vue'
import dmx from 'dmx-api'

window.addEventListener('focus', updateWorkspaceCookie)

const state = {

  workspaceId: undefined,           // ID of selected workspace (number)

  isWritable: undefined,            // true if selected workspace is writable

  workspaceTopics: undefined,       // All workspace topics readable by current user (array of dmx.Topic)

  workspaceCommands: {},            // Registered workspace commands:
                                    //   {
                                    //      topicmapTypeUri: [comp]
                                    //   }

  ready: fetchWorkspaceTopics()     // A promise resolved once the workspace topics are loaded
}

const actions = {

  createWorkspace ({dispatch}, {name, sharingModeUri}) {
    // console.log('createWorkspace', name, sharingModeUri)
    dmx.rpc.createWorkspace(name, undefined, sharingModeUri).then(topic => {     // uri=undefined
      // console.log('Workspace', topic)
      state.workspaceTopics.push(topic)
      selectWorkspace(topic.id, dispatch)
    })
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectWorkspace ({dispatch}, id) {
    selectWorkspace(id, dispatch)
  },

  /**
   * Dispatched for initial navigation (see router.js), after workspace deletion,
   * and after logout (see loggedOut() below).
   *
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectFirstWorkspace ({dispatch}) {
    selectFirstWorkspace(dispatch)
  },

  /**
   * Sets the workspace state ("workspaceId" and cookie), and fetches the workspace's topicmap topics
   * if not done already.
   *
   * Low-level action (dispatched by router) that sets the workspace state *without* selecting a topicmap.
   *
   * Postconditions:
   * - "workspaceId" state is up-to-date
   * - "dmx_workspace_id" cookie is up-to-date.
   *
   * @return  a promise resolved once the workspace's topicmap topics are available.
   *          At this time the "topicmapTopics" state is up-to-date (see topicmaps module).
   */
  _selectWorkspace ({dispatch}, id) {
    return _selectWorkspace(id, dispatch)
  },

  _initWorkspaceIsWritable () {
    // workspaceId might be uninitialized. Accesscontrol "username" state is inited *before* workspaceId state. TODO?
    state.workspaceId && dmx.permCache.isWritable(state.workspaceId).then(
      writable => {
        // console.log('_initWorkspaceIsWritable', state.workspaceId, writable)
        state.isWritable = writable
      }
    )
  },

  registerWorkspaceCommand (_, command) {
    const c = state.workspaceCommands
    const uri = command.topicmapTypeUri
    const commands = c[uri] || (c[uri] = [])
    commands.push(command.comp)
  },

  //

  loggedIn () {
    fetchWorkspaceTopics()
  },

  loggedOut ({dispatch}) {
    fetchWorkspaceTopics()
      .then(() => dispatch('clearTopicmap'))
      .then(() => dispatch('initTypeCache'))
      .then(() => {
        if (isWorkspaceReadable()) {
          // Note: 'clearTopicmapCache' is dispatched inside 'reloadTopicmap'
          dispatch('reloadTopicmap')
        } else {
          dispatch('clearTopicmapCache')
          selectFirstWorkspace(dispatch)
        }
      })
  },

  // WebSocket messages

  _newWorkspace (_, {workspace}) {
    state.workspaceTopics.push(workspace)
  },

  _processDirectives ({dispatch}, directives) {
    // console.log(`Workspaces: processing ${directives.length} directives`)
    directives.forEach(dir => {
      let topic
      switch (dir.type) {
      case 'UPDATE_TOPIC':
        topic = new dmx.Topic(dir.arg)
        if (topic.typeUri === 'dmx.workspaces.workspace') {
          updateWorkspace(topic)
        }
        break
      case 'DELETE_TOPIC':
        topic = new dmx.Topic(dir.arg)
        if (topic.typeUri === 'dmx.workspaces.workspace') {
          deleteWorkspace(topic, dispatch)
        }
        break
      }
    })
  }
}

export default {
  state,
  actions
}

// Actions helper

function selectFirstWorkspace (dispatch) {
  selectWorkspace(state.workspaceTopics[0].id, dispatch)
}

function selectWorkspace (id, dispatch) {
  // console.log('selectWorkspace', id)
  _selectWorkspace(id, dispatch).then(() => {
    // the workspace's topicmap topics are now available
    dispatch('selectTopicmapForWorkspace')
  })
}

function _selectWorkspace (id, dispatch) {
  // console.log('_selectWorkspace', id)
  state.workspaceId = id
  updateWorkspaceCookie()
  return dispatch('fetchTopicmapTopics')     // data for topicmap selector
}

// State helper

function fetchWorkspaceTopics () {
  return dmx.rpc.getTopicsByType('dmx.workspaces.workspace').then(topics => {
    // console.log('### Workspaces ready!')
    state.workspaceTopics = topics
  })
}

function findWorkspaceTopic (id, callback) {
  const i = state.workspaceTopics.findIndex(topic => topic.id === id)
  if (i !== -1) {
    callback(state.workspaceTopics, i)
  }
}

function isWorkspaceReadable () {
  return state.workspaceTopics.find(workspace => workspace.id === state.workspaceId)
}

function updateWorkspaceCookie () {
  // console.log('dmx_workspace_id', state.workspaceId)
  dmx.utils.setCookie('dmx_workspace_id', state.workspaceId)
}

// Process directives

/**
 * Processes an UPDATE_TOPIC directive.
 * Updates the workspace menu when a workspace is renamed.
 */
function updateWorkspace (topic) {
  // update state
  findWorkspaceTopic(topic.id, (topics, i) => Vue.set(topics, i, topic))
}

/**
 * Processes a DELETE_TOPIC directive.
 */
function deleteWorkspace (topic, dispatch) {
  // update state
  findWorkspaceTopic(topic.id, (topics, i) => topics.splice(i, 1))
  // redirect
  if (topic.id === state.workspaceId) {
    selectFirstWorkspace(dispatch)
  }
}
