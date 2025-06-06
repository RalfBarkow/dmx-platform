import { nextTick } from 'vue'

export default ({store}) => {
  return {

    init () {
      store.dispatch('initConfigDefs')
    },

    storeModule: {
      name: 'details',
      module: require('./details').default
    },

    storeWatcher: [{
      getter: state => state.details.visible,
      callback: (visible) => {
        nextTick(() => {
          if (visible) {
            store.dispatch('adaptPanelsWidth')
          }
        })
      }
    }],

    components: [
      {
        comp: require('dmx-detail-panel').default,
        mount: 'webclient',
        props: () => ({
          object:          store.getters.object,
          writable:        store.state.writable,
          visible:         store.state.details.visible,
          pinned:          store.state.details.pinned,
          tab:             store.state.details.tab,
          mode:            store.state.details.mode,
          configDefs:      store.state.details.configDefs,
          markerTopicIds:  store.getters.visibleTopicIds,
          detailRenderers: store.state.detailRenderers,
          extraButtons:    store.state.detailPanelButtons,
          types:           {
                             assocTypes: store.state.typeCache.assocTypes,
                             roleTypes:  store.state.typeCache.roleTypes
                           },                                                                /* eslint indent: "off" */
          quillConfig:     store.state.quillConfig,
          transX:          store.state.resizerPos
        }),
        listeners: {
          'tab-click':           tabClick,
          edit:                  ()              => store.dispatch('callDetailRoute', 'edit'),
          submit:                object          => {
                                                      store.dispatch('submit', object)
                                                      store.dispatch('callDetailRoute', 'info')
                                                    },
          'submit-inline':       object          => store.dispatch('submit', object),
          'submit-view-config':  viewConfigTopic => store.dispatch('submit', viewConfigTopic),
          'submit-config-topic': configTopic     => store.dispatch('updateConfigTopic', configTopic),
          'child-topic-reveal':  relTopic        => store.dispatch('revealRelatedTopic', {relTopic}),
          'related-topic-click': relTopic        => store.dispatch('revealRelatedTopic', {relTopic}),
          'related-icon-click':  relTopic        => store.dispatch('revealRelatedTopic', {relTopic, noSelect: true}),
          'object-id-click':     object          => window.open(url(object), '_blank'),
          pin:                   pinned          => store.dispatch('setDetailPanelPinned', pinned)
        }
      }
    ]
  }

  function tabClick (tab) {
    const details = store.state.details
    // clicking 1st tab while in form mode
    if (tab === 'info' && details.mode === 'form') {
      // 1st tab is selected already -> no-op
      if (details.tab === 'info') {
        return
      }
      // another tab is currently selected -> resume editing
      tab = 'edit'
    }
    //
    store.dispatch('callDetailRoute', tab)
  }

  function url (object) {
    if (object.typeUri === 'dmx.core.topic_type') {
      return `/core/topic-type/${object.uri}`
    } else if (object.typeUri === 'dmx.core.assoc_type') {
      return `/core/assoc-type/${object.uri}`
    } else if (object.isTopic) {
      return `/core/topic/${object.id}?children=true&assocChildren=true`
    } else if (object.isAssoc) {
      return `/core/assoc/${object.id}?children=true&assocChildren=true`
    }
    throw Error('unexpected object')
  }
}
