package systems.dmx.topicmaps;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systems.dmx.core.Assoc;
import systems.dmx.core.impl.ModelFactoryImpl;
import systems.dmx.core.model.topicmaps.ViewProps;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static systems.dmx.topicmaps.Constants.PINNED;
import static systems.dmx.topicmaps.Constants.VISIBILITY;
import static systems.dmx.topicmaps.Constants.X;
import static systems.dmx.topicmaps.Constants.Y;



class TopicmapContextMutationPathTest {

    private final ModelFactoryImpl modelFactory = new ModelFactoryImpl();

    @Test
    @DisplayName("JSON view props body stores healthy long-form topicmap props unchanged")
    void storesHealthyLongFormPropsUnchanged() throws Exception {
        Assoc topicmapContext = mock(Assoc.class);

        ViewProps viewProps = modelFactory.newViewProps(new JSONObject()
            .put(X, 160)
            .put(Y, 120)
            .put(VISIBILITY, true)
            .put(PINNED, false)
        );

        assertThatCode(() -> viewProps.store(topicmapContext))
            .doesNotThrowAnyException();

        verify(topicmapContext).setProperty(X, 160, false);
        verify(topicmapContext).setProperty(Y, 120, false);
        verify(topicmapContext).setProperty(VISIBILITY, true, false);
        verify(topicmapContext).setProperty(PINNED, false, false);
        verify(topicmapContext, never()).setProperty(eq("x"), any(), eq(false));
        verify(topicmapContext, never()).setProperty(eq("y"), any(), eq(false));
        verify(topicmapContext, never()).setProperty(eq("visibility"), any(), eq(false));
        verify(topicmapContext, never()).setProperty(eq("pinned"), any(), eq(false));
    }

    @Test
    @DisplayName("JSON view props body stores malformed short-key-only topicmap props unchanged")
    void storesMalformedShortKeyOnlyPropsUnchanged() throws Exception {
        Assoc topicmapContext = mock(Assoc.class);

        ViewProps viewProps = modelFactory.newViewProps(new JSONObject()
            .put("x", 160)
            .put("y", 120)
            .put("visibility", true)
            .put("pinned", false)
        );

        assertThatCode(() -> viewProps.store(topicmapContext))
            .doesNotThrowAnyException();

        verify(topicmapContext).setProperty("x", 160, false);
        verify(topicmapContext).setProperty("y", 120, false);
        verify(topicmapContext).setProperty("visibility", true, false);
        verify(topicmapContext).setProperty("pinned", false, false);
        verify(topicmapContext, never()).setProperty(eq(X), any(), eq(false));
        verify(topicmapContext, never()).setProperty(eq(Y), any(), eq(false));
        verify(topicmapContext, never()).setProperty(eq(VISIBILITY), any(), eq(false));
        verify(topicmapContext, never()).setProperty(eq(PINNED), any(), eq(false));
    }

    @Test
    @DisplayName("partial short-key JSON update does not normalize or remove topicmap props")
    void partialShortKeyUpdateDoesNotNormalizeOrRemoveProps() throws Exception {
        Assoc topicmapContext = mock(Assoc.class);

        ViewProps viewProps = modelFactory.newViewProps(new JSONObject()
            .put("visibility", true)
        );

        assertThatCode(() -> viewProps.store(topicmapContext))
            .doesNotThrowAnyException();

        verify(topicmapContext).setProperty("visibility", true, false);
        verify(topicmapContext, never()).setProperty(eq(VISIBILITY), any(), eq(false));
        verify(topicmapContext, never()).removeProperty(anyString());
    }
}
