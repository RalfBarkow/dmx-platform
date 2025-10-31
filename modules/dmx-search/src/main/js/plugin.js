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

  function getTextType(store) {
    return (
      store.state.model.getTopicTypeByUri('dmx.core.note') ||
      store.state.model.getTopicTypeByUri('dmx.core.text') ||
      null
    )
  }

  function makeOpLog(action, context) {
    const lines = []
    const now = () => new Date().toISOString()
    const safe = (v) => {
      try { return JSON.stringify(v) } catch (_) { return String(v) }
    }
    return {
      info: (m, d) => lines.push(`${now()} [info] ${m}${d ? ' ' + safe(d) : ''}`),
      warn: (m, d) => lines.push(`${now()} [warn] ${m}${d ? ' ' + safe(d) : ''}`),
      error: (m, d) => lines.push(`${now()} [error] ${m}${d ? ' ' + safe(d) : ''}`),
      toText: (status, err) => {
        const header = `${action} — ${status.toUpperCase()}`
        const ctx = context ? `\n\nContext: ${safe(context)}` : ''
        const errPart = err ? `\n\nError: ${err.message || String(err)}${err.stack ? `\nStack:\n${err.stack}` : ''}` : ''
        return `${header}${ctx}\n\nLog:\n${lines.join('\n')}${errPart}`
      }
    }
  }

  async function reportFailureAsTextTopic({ store, revealTopic, title, logText }) {
    const type = getTextType(store)
    if (!type) {
      console.error('[dmx-search] No dmx.core.text/note type available to report error')
      return
    }
    const model = type.newTopicModel({ value: `${title}\n\n${logText}` })
    const topic = await dmx.rpc.createTopic(model)
    revealTopic(topic)
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

  function createTopic({ topicType, value }) {
    const action = 'Create FedWiki Sitemap' // or derive from topicType
    const ctx = {
      topicTypeUri: topicType?.uri,
      topicTypeLabel: topicType?.value,
      inputPreview: typeof value === 'string' ? value.slice(0, 200) : value
    }
    const log = makeOpLog(action, ctx)

    try {
      if (!topicType || typeof topicType.newTopicModel !== 'function') {
        throw new Error('Invalid topicType')
      }

      const isSimple = !!topicType.isSimple
      const payload = isSimple
        ? (value == null ? { value: '' } : (typeof value === 'object' ? value : { value }))
        : { children: {} } // minimal MVP for composites; refine later if needed

      log.info('building topicModel', { isSimple })

      const topicModel = topicType.newTopicModel(payload)

      dmx.rpc.createTopic(topicModel)
        .then(topic => {
          log.info('createTopic success', { id: topic.id, typeUri: topic.typeUri })
          revealTopic(topic)
          store.dispatch('_processDirectives', topic.directives)
        })
        .catch(async (err) => {
          log.error('createTopic RPC failed')
          await reportFailureAsTextTopic({
            store,
            revealTopic,
            title: `${action} — FAILED`,
            logText: log.toText('failure', err)
          })
        })
    } catch (err) {
      // synchronous errors (before RPC)
      log.error('createTopic threw before RPC')
      reportFailureAsTextTopic({
        store,
        revealTopic,
        title: `${action} — FAILED (client)`,
        logText: log.toText('failure', err)
      })
    }
  }

  function createExtra ({extraItem, value, optionsData}) {
    extraItem.create(value, optionsData, store.state.search.pos.model)
  }

  function createTopicmap ({name, topicmapTypeUri, viewProps}) {
    store.dispatch('createTopicmap', {name, topicmapTypeUri, viewProps})
  }
}
