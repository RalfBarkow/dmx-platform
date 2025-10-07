import dmx from 'dmx-api'
import '../style/style.css'

export default ({store}) => {
  return {

    storeModule: {
      name: 'search',
      module: require('./search').default
    },

    components: [
      {
        comp: require('dmx-search-widget').default,
        mount: 'webclient',
        props: {
          visible:          state => state.search.visible,
          extraMenuItems:   state => state.search.extraMenuItems,
          createEnabled:    state => state.workspaces.isWritable,
          markerTopicIds:   (_, getters) => getters?.visibleTopicIds,
          markerAssocIds:   (_, getters) => getters?.visibleAssocIds,
          createTopicTypes: (_, getters) => getters?.createTopicTypes,    // TODO: getters is undefined at startup
          searchAssocTypes: () => dmx.typeCache.getAllAssocTypes(),
          topicmapTypes:    state => Object.values(state.topicmaps.topicmapTypes)
        },
        listeners: {
          'topic-click':     revealTopic,
          'icon-click':      revealTopicNoSelect,
          'assoc-click':     revealAssoc,
          'topic-create':    createTopic,
          'extra-create':    createExtra,
          'topicmap-create': createTopicmap,
          close: _ => store.dispatch('closeSearchWidget')
        }
      }
    ]
  }

  function revealTopicNoSelect (topic) {
    revealTopic(topic, true)    // noSelect=true
  }

  function revealTopic (topic, noSelect) {
    const state = store.state.search
    store.dispatch('revealTopic', {
      topic,
      pos: state.pos.model,
      noSelect: noSelect || state.options.noSelect
    })
    state.options.topicHandler && state.options.topicHandler(topic)
  }

  function revealAssoc (assoc) {
    const pos = store.state.search.pos.model
    store.dispatch('revealTopic', {topic: assoc.player1.topic, pos,                                  noSelect: true})
    store.dispatch('revealTopic', {topic: assoc.player2.topic, pos: {x: pos.x + 340, y: pos.y - 40}, noSelect: true})
    store.dispatch('revealAssoc', {assoc})
  }

  function createTopic ({ topicType, value }) {
    if (!topicType || typeof topicType.newTopicModel !== 'function') {
      console.warn('[createTopic] invalid topicType', topicType)
      return
    }

    const isSimple = !!topicType.isSimple

    const payload = isSimple
      // Simple-valued: ensure { value: ... }
      ? (value == null
          ? { value: '' }
          : (typeof value === 'object' ? value : { value }))
      // Composite: put the typed string into a sensible simple child
      : (() => {
          const p = { children: {} }
          if (value != null) {
            const defs = topicType.compDefs || []
            // prefer a “name/label/title” child if available, else first simple child
            const nameDef =
              defs.find(d =>
                d.childType?.isSimple &&
                (
                  /name|label|title/i.test(d.childType?.value || '') ||
                  d.childType?.uri === 'dmx.core.name' ||
                  d.childType?.uri === 'dmx.core.text'
                )
              ) || defs.find(d => d.childType?.isSimple)

            if (nameDef) {
              const v = (typeof value === 'object' && 'value' in value) ? value : { value }
              p.children[nameDef.compDefUri] = nameDef.isOne ? v : [v]
            }
          }
          return p
        })()

    console.debug('[createTopic]', { isSimple, payload, topicType })

    const topicModel = topicType.newTopicModel(payload)
    dmx.rpc.createTopic(topicModel).then(topic => {
      revealTopic(topic)
      store.dispatch('_processDirectives', topic.directives)
    })
  }

  function createExtra ({extraItem, value, optionsData}) {
    extraItem.create(value, optionsData, store.state.search.pos.model)
  }

  function createTopicmap ({name, topicmapTypeUri, viewProps}) {
    store.dispatch('createTopicmap', {name, topicmapTypeUri, viewProps})
  }
}
