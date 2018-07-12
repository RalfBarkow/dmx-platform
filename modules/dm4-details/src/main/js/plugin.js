import Vue from 'vue'

export default store => ({

  storeModule: {
    name: 'details',
    module: require('./details').default
  },

  storeWatcher: [{
    getter: state => state.details.visible,
    callback: () => {
      Vue.nextTick(() => {
        store.dispatch('resizeTopicmapRenderer')
      })
    }
  }],

  components: [
    {
      comp: require('dm5-detail-panel').default,
      mount: 'webclient',
      props: {
        object:          (_, getters) => getters && getters.object,   // TODO: why is getters undefined on 1st call?
        writable:        state => state.writable,
        visible:         state => state.details.visible,
        tab:             state => state.details.tab,
        mode:            state => state.details.mode,
        objectRenderers: state => state.objectRenderers,
        quillConfig:     state => state.quillConfig
      },
      listeners: {
        'tab-click':           tab      => {store.dispatch('callDetailRoute', tab)},
        'edit':                ()       => {store.dispatch('callDetailRoute', 'edit')},
        'submit':              object   => {store.dispatch('submit', object)
                                            store.dispatch('callDetailRoute', 'info')},
        'submit-inline':       object   => {store.dispatch('submit', object)},
        'child-topic-reveal':  relTopic => {store.dispatch('revealRelatedTopic', relTopic)},
        'related-topic-click': relTopic => {store.dispatch('revealRelatedTopic', relTopic)}
      }
    },
    {
      comp: require('./components/dm5-detail-panel-toggle').default,
      mount: 'toolbar-right'
    }
  ]
})
