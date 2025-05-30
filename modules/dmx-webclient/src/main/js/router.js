/**
 * The router.
 * - Initializes app state according to start URL.
 * - Adapts app state when URL changes.
 */

import { nextTick } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import Webclient from './components/dmx-webclient'
import store from './store/webclient'
import app from './app'
import dmx from 'dmx-api'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/topicmap/:topicmapId',
      name: 'topicmap',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/topic/:topicId',
      name: 'topic',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/assoc/:assocId',
      name: 'assoc',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/topic/:topicId/:detail',
      name: 'topicDetail',
      component: Webclient
    },
    {
      path: '/topicmap/:topicmapId/assoc/:assocId/:detail',
      name: 'assocDetail',
      component: Webclient
    }
  ]
})

// global guard
/* ### TODO: eliminate "__vue__"
router.beforeEach((to, from, next) => {
  // console.log('### beforeEach', to, from)
  performDirtyCheck(to, from).then(abort => {
    if (abort) {
      next(false)
    } else {
      next()
    }
  })
})
*/

export default router

// store module
store.registerModule('routerModule', {

  state: {
    router
  },

  actions: {

    initialNavigation () {
      navigate(router.currentRoute.value, {params: {}})
      registerRouteWatcher()
    },

    callRoute (_, location) {
      router.push(location)
    },

    callTopicmapRoute (_, id) {
      router.push({
        name: 'topicmap',
        params: {topicmapId: id}
      })
    },

    callTopicRoute (_, id) {
      callObjectRoute(id, 'topicId', 'topic', 'topicDetail')
    },

    callAssocRoute (_, id) {
      callObjectRoute(id, 'assocId', 'assoc', 'assocDetail')
    },

    stripSelectionFromRoute () {
      router.push({
        name: 'topicmap'
      })
    },

    /**
     * Redirects to "topicDetail" or "assocDetail" route, depending on current selection.
     *
     * Prerequisite: a single selection
     *
     * @param   detail    "info", "related", "meta", "config" or "edit"
     */
    callDetailRoute ({rootState}, detail) {
      const object = rootState.object
      if (!object) {
        throw Error('callDetailRoute() when there is no single selection')
      }
      router.push({
        name: object.isTopic ? 'topicDetail' : 'assocDetail',
        params: {detail}
      })
    },

    /**
     * Redirects to "topicDetail" route.
     *
     * @param   id        a topic ID
     * @param   detail    "info", "related", "meta", "config" or "edit"
     */
    callTopicDetailRoute (_, {id, detail}) {
      router.push({
        name: 'topicDetail',
        params: {topicId: id, detail}
      })
    },

    /**
     * Redirects to "assocDetail" route.
     *
     * @param   id        an assoc ID
     * @param   detail    "info", "related", "meta", "config" or "edit"
     */
    callAssocDetailRoute (_, {id, detail}) {
      router.push({
        name: 'assocDetail',
        params: {assocId: id, detail}
      })
    },

    stripDetailFromRoute ({rootState}) {
      const object = rootState.object
      if (!object) {
        throw Error('stripDetailFromRoute() when there is no single selection')
      }
      router.push({
        name: object.isTopic ? 'topic' : 'assoc'
      })
    }
  }
})

function registerRouteWatcher () {
  store.watch(
    state => state.routerModule.router.currentRoute,
    (to, from) => {
      // console.log('### Route watcher', to, from)
      navigate(to, from)
    }
  )
}

/**
 * @return  Promise which resolves to a boolean: true if user wants abort navigation (to continue editing),
 *          false otherwise.
 */
function performDirtyCheck (to, from) {
  // Perform a dirty check if all conditions apply:
  //   - the detail panel is visible (in-map details are NOT dirty checked; TODO?)
  //   - there is a selection (the detail panel is not empty)
  //   - in the route there is a topicmap change or a selection change (or both)
  // Note: at router instantiation time the details plugin is not yet initialized ### TODO: still true?
  // TODO: rethink router instantiation time
  return new Promise(resolve => {
    if (store.state.details && store.state.details.visible && store.state.object &&
        (topicmapId(to) !== topicmapId(from) || objectId(to) !== objectId(from))) {
      const detailPanel = document.querySelector('.dmx-detail-panel').__vue__.$parent     // $parent <transition>
      const isDirty = detailPanel.isDirty()
      // console.log('isDirty', isDirty, store.state.object.id)
      if (isDirty) {
        app.config.globalProperties.$confirm('There are unsaved changes', 'Warning', {
          type: 'warning',
          confirmButtonText: 'Save',
          cancelButtonText: 'Discard Changes',
          distinguishCancelAndClose: true
        }).then(action => {   // -> Save
          detailPanel.save()
          resolve(false)
        }).catch(action => {
          switch (action) {
          case 'cancel':      // -> Discard changes
            resolve(false)
            break
          case 'close':       // -> Continue editing, abort navigation
            restoreSelection(from)
            resolve(true)
            break
          default:
            throw Error(`unexpected MessageBox action: "${action}"`)
          }
        })
      } else {
        resolve(false)
      }
    } else {
      resolve(false)
    }
  })
}

/**
 * Adapts app state when route changes.
 */
function navigate (to, from) {
  const topicmapId = id(to.params.topicmapId)
  if (!topicmapId) {
    defaultNavigation()
    return
  }
  //
  let p     // a promise resolved once the topicmap rendering is complete
  // 1) topicmap
  const topicmapChanged = topicmapId !== id(from.params.topicmapId)
  // Note: route params read from URL are strings. Route params set by push() are numbers.
  if (topicmapChanged) {
    // Note: the workspace must be set *before* the topicmap is displayed.
    // See preconditions at "displayTopicmap".
    p = checkTopicmapId(topicmapId)
          .then(() => dmx.rpc.getAssignedWorkspace(topicmapId))
          .then(workspace => store.dispatch('_selectWorkspace', workspace.id))
          .then(() => store.dispatch('displayTopicmap', topicmapId))
          .catch(e => {
            console.warn(`Route ${to.path} failed (${e})`)
            redirectToFirstWorkspace()
          })
  } else {
    p = Promise.resolve()
  }
  // 2) topic/assoc selection
  const topicId = id(to.params.topicId)
  const assocId = id(to.params.assocId)
  const oldTopicId = id(from.params.topicId)
  const oldAssocId = id(from.params.assocId)
  const oldId = oldAssocId || oldTopicId        // Note: oldAssocId is checked first as oldId must be a number
  const newId = assocId    || topicId           // Note: assocId    is checked first as newId must be a number
  const topicChanged = topicId !== oldTopicId
  const assocChanged = assocId !== oldAssocId
  let p2
  if ((topicChanged || topicmapChanged) && topicId !== undefined) {                     // Note: 0 is a valid topic ID
    p2 = fetchAndDisplayTopic(topicId, p)
  } else if ((assocChanged || topicmapChanged) && assocId) {
    p2 = fetchAndDisplayAssoc(assocId, p)
  } else if ((topicChanged || assocChanged) && topicId === undefined && !assocId) {     // Note: 0 is a valid topic ID
    store.dispatch('emptyDisplay')                              // -> dmx-webclient
    if (!topicmapChanged) {
      p.then(() => store.dispatch('unsetSelection', oldId))     // -> dmx-topicmaps
    }
  }
  p2?.catch(e => {
    console.warn(`Route ${to.path} failed (${e})`)
    // give detail panel time to appear (before it closes again), it's DOM is needed for the imminent dirty check
    nextTick(() => {
      redirectToTopicmap(topicmapId)      // strip selection
    })
  })
  // 3) detail panel tab
  const detail = to.params.detail
  const oldDetail = from.params.detail
  if (detail !== oldDetail) {
    store.dispatch('setDetailPanelVisibility', detail !== undefined || store.state.details.pinned)
    if (detail) {
      store.dispatch('selectDetail', detail)
      if (!oldDetail && oldId === newId) {
        store.dispatch('_removeDetail')
      }
    }
  }
}

function defaultNavigation () {
  // console.log('### defaultNavigation')
  const topicmapId = id(dmx.utils.getCookie('dmx_topicmap_id'))
  if (topicmapId) {
    checkTopicmapId(topicmapId)
      .then(() => redirectToTopicmap(topicmapId))
      .catch(e => {
        console.warn(`Invalid dmx_topicmap_id cookie (value: ${topicmapId}). ${e}`)
        redirectToFirstWorkspace()
      })
  } else {
    // console.log('No dmx_topicmap_id cookie present')
    redirectToFirstWorkspace()
  }
}

function redirectToFirstWorkspace () {
  const workspaceId = store.state.workspaces.workspaceTopics[0].id
  // console.log('redirectToFirstWorkspace', workspaceId)
  dmx.rpc.getAssignedTopics(workspaceId, 'dmx.topicmaps.topicmap').then(topics => {
    if (!topics.length) {
      throw Error(`workspace ${workspaceId} has no topicmap`)
    }
    const topicmapId = topics[0].id
    redirectToTopicmap(topicmapId)
  })
}

function redirectToTopicmap (topicmapId) {
  // console.log('redirectToTopicmap', topicmapId)
  store.dispatch('callTopicmapRoute', topicmapId)
}

function checkTopicmapId (id) {
  return dmx.rpc.getTopic(id).then(topic => {
    if (topic.typeUri !== 'dmx.topicmaps.topicmap') {
      throw Error(`${id} is not a topicmap, but a ${topic.typeUri}`)
    }
  })
}

/**
 * Redirects to "topic"/"assoc" route while retaining the selected detail tab (if any).
 *
 * Falls back to "info" tab in 2 cases: when we're coming from ...
 *   - form mode (we don't want stay in form mode when user selects another topic/assoc)
 *   - empty (pinned) detail panel (we don't want restore the tab that was selected before emptying the panel)
 */
function callObjectRoute (id, paramName, routeName, detailRouteName) {
  const location = {
    params: {[paramName]: id}
  }
  // Note: normally the route is the source of truth. But there is an app state not represented in the route: an empty
  // (pinned) detail panel. We detect that by inspecting both, the "details.visible" app state AND the route's "detail"
  // segment.
  if (store.state.details.visible) {
    location.name = detailRouteName
    // fallback to "info" tab
    // Note: when the detail panel is empty there is no "detail" route segment
    const detail = router.currentRoute.value.params.detail
    if (!detail || detail === 'edit') {
      location.params.detail = 'info'
    }
  } else {
    location.name = routeName
  }
  router.push(location)
}

/**
 * Fetches the given topic, displays it in the detail panel, and renders it as selected in the topicmap panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 * @return      a promise resolved once the topicmap rendering is complete and the selection is done.
 */
function fetchAndDisplayTopic (id, p) {
  const p2 = dmx.rpc.getTopic(id, true, true).then(topic => {  // includeChildren=true, includeAssocChildren=true
    // Note: the topicmap panel manually syncs the selected object with the topicmap renderer.
    // The "object" state must not be set before a topicmap renderer is instantiated.
    p.then(() => {
      store.dispatch('displayObject', topic)              // -> dmx-webclient
    })
  }).catch(e => {
    console.warn(`${e}`)
  })
  return p.then(() => {
    store.dispatch('setTopicSelection', {id, p: p2})      // -> dmx-topicmaps
  })
}

/**
 * Fetches the given assoc, displays it in the detail panel, and renders it as selected in the topicmap panel.
 *
 * @param   p   a promise resolved once the topicmap rendering is complete.
 * @return      a promise resolved once the topicmap rendering is complete and the selection is done.
 */
function fetchAndDisplayAssoc (id, p) {
  const p2 = dmx.rpc.getAssoc(id, true, true).then(assoc => {  // includeChildren=true, includeAssocChildren=true
    // Note: the topicmap panel manually syncs the selected object with the topicmap renderer.
    // The "object" state must not be set before a topicmap renderer is instantiated.
    p.then(() => {
      store.dispatch('displayObject', assoc)              // -> dmx-webclient
    })
  }).catch(e => {
    console.warn(`${e}`)
  })
  return p.then(() => {
    store.dispatch('setAssocSelection', {id, p: p2})      // -> dmx-topicmaps
  })
}

const SETTER = {
  topicDetail: 'setTopic',
  assocDetail: 'setAssoc'
}

/**
 * Restores the selection according to the given route
 */
function restoreSelection (route) {
  const setter = SETTER[route.name]
  if (!setter) {
    throw Error(`unexpected route: "${route.name}"`)
  }
  const id = objectId(route)
  const selection = store.getters.selection
  // update view
  selection.forEachId(id => {
    store.dispatch('_renderAsUnselected', id)
  })
  store.dispatch('_renderAsSelected', id)
  // update model
  selection[setter](id)
}

/**
 * @return  an ID (type Number) or undefined
 */
function objectId (route) {
  return id(route.params.assocId || route.params.topicId)     // Note: 0 is a valid topic ID; check assoc ID first
}

function topicmapId (route) {
  return id(route.params.topicmapId)
}

/**
 * Converts the given value into Number.
 *
 * @return  the number, or undefined if `undefined` or `null` is given.
 *          Never returns `null`.
 *
 * @throws  if the given value is not one of Number/String/undefined/null.
 */
function id (v) {
  // Note: Number(undefined) is NaN, and NaN != NaN is true!
  // Note: dmx.utils.getCookie may return null, and Number(null) is 0 (and typeof null is 'object')
  if (typeof v === 'number') {
    return v
  } else if (typeof v === 'string') {
    return Number(v)
  } else if (v !== undefined && v !== null) {
    throw Error(`id() expects one of [number|string|undefined|null], got ${v}`)
  }
}
