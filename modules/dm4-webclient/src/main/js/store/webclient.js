import Vue from 'vue'
import Vuex from 'vuex'
import dm5 from 'dm5'

Vue.use(Vuex)

var compCount = 0

const state = {

  object: undefined,        // The selected Topic/Assoc/TopicType/AssocType.
                            // Undefined if nothing is selected.

  writable: undefined,      // True if the current user has WRITE permission for the selected object.

  detail: undefined,        // The selected tab in the detail panel: 'edit', 'related', ...
                            // If undefined the detail panel is not visible.
                            // TODO: move to separate dm5-detail-panel standard plugin

  mode: undefined,          // 'info' or 'form'
                            // TODO: move to separate dm5-detail-panel standard plugin

  objectRenderers: {},      // Registered page renderers:
                            //   {
                            //     typeUri: component
                            //   }

  quill: undefined,         // The Quill instance deployed in form mode.
                            // FIXME: support more than one Quill instance per form.

  compDefs: {}              // Registered components
}

const actions = {

  displayObject (_, object) {
    // console.log('displayObject')
    state.object = object.isType() ? object.asType() : object
    _initWritable()
    cancelEdit()    // Note: inline state is still set when inline editing was left without saving
  },

  emptyDisplay () {
    // console.log('emptyDisplay')
    state.object = undefined
  },

  /**
   * Precondition:
   * - topic/assoc data has arrived (global "object" state is up-to-date).
   */
  edit () {
    console.log('edit', state.object)
    state.mode = 'form'
  },

  submit (_, object) {
    _submit(object)
  },

  // TODO: introduce edit buffer also for inline editing?
  submitInline () {
    _submit(state.object)
  },

  editTopic ({dispatch}, id) {
    dispatch('callTopicDetailRoute', {id, detail: 'edit'})
  },

  editAssoc ({dispatch}, id) {
    dispatch('callAssocDetailRoute', {id, detail: 'edit'})
  },

  whatsRelated ({dispatch}, id) {
    dispatch('callTopicDetailRoute', {id, detail: 'related'})
  },

  selectDetail (_, detail) {
    // console.log('selectDetail', detail)
    state.detail = detail
  },

  registerObjectRenderer (_, {typeUri, component}) {
    state.objectRenderers[typeUri] = component
  },

  setQuill (_, quill) {
    state.quill = quill
  },

  createTopicLink (_, topic) {
    console.log('createTopicLink', topic)
    state.quill.format('topic-link', {
      topicId: topic.id,
      linkId: undefined   // TODO
    })
  },

  unselectIf ({dispatch}, id) {
    // console.log('unselectIf', id, isSelected(id))
    if (isSelected(id)) {
      dispatch('stripSelectionFromRoute')
    }
  },

  // ---

  registerComponent (_, compDef) {
    const compDefs = state.compDefs[compDef.mount] || (state.compDefs[compDef.mount] = [])
    compDef.id = compCount++
    compDefs.push(compDef)
  },

  /**
   * Instantiates and mounts the registered components for mount point "webclient".
   */
  mountComponents () {
    state.compDefs.webclient.forEach(compDef => {
      // Note: to manually mounted components the store must be passed explicitly
      // https://forum.vuejs.org/t/this-store-undefined-in-manually-mounted-vue-component/8756
      const Component = Vue.extend(compDef.comp)
      const comp = new Component({store}).$mount(`#mount-${compDef.id}`)
      // inject props
      if (compDef.props) {
        for (var prop in compDef.props) {
          const val = compDef.props[prop]
          if (typeof val === "function") {
            // reactive (val is getter function)
            registerPropWatcher(comp, prop, val)
          } else {
            // static (val is value)
            comp.$props[prop] = val
          }
        }
      }
    })
  },

  //

  loggedIn () {
    initWritable()
  },

  loggedOut () {
    initWritable()
    cancelEdit()
  },

  // WebSocket messages

  _processDirectives ({dispatch}, directives) {
    console.log(`Webclient: processing ${directives.length} directives`, directives)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        displayObjectIf(new dm5.Topic(dir.arg))
        break
      case "DELETE_TOPIC":
        dispatch('unselectIf', dir.arg.id)
        break
      case "UPDATE_ASSOCIATION":
        displayObjectIf(new dm5.Assoc(dir.arg))
        break
      case "DELETE_ASSOCIATION":
        dispatch('unselectIf', dir.arg.id)
        break
      }
    })
  }
}

const store = new Vuex.Store({
  state,
  actions
})

export default store

//

function displayObjectIf (object) {
  if (isSelected(object.id)) {
    store.dispatch('displayObject', object)
  }
}

function isSelected (id) {
  const object = state.object
  return object && object.id === id
}

function _submit (object) {
  object.update().then(object => {
    store.dispatch('_processDirectives', object.directives)
  })
  cancelEdit()
}

function cancelEdit () {
  state.mode = 'info'               // cancel form edit
  // state.inlineCompId = undefined ### TODO: cancel inline edit?
}

//

function initWritable() {
   state.object && _initWritable()
}

function _initWritable() {
  state.object.isWritable().then(writable => {
    state.writable = writable
  })
}

//

function registerPropWatcher (comp, prop, getter) {
  // console.log('registerPropWatcher', prop)
  store.watch(
    getter,
    val => {
      // console.log(`"${prop}" changed`, val)
      comp.$props[prop] = val
    }
  )
}
