package systems.dmx.topicmaps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systems.dmx.core.Assoc;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static systems.dmx.topicmaps.Constants.PINNED;
import static systems.dmx.topicmaps.Constants.VISIBILITY;
import static systems.dmx.topicmaps.Constants.X;
import static systems.dmx.topicmaps.Constants.Y;



class TopicmapContextGuardTest {

    @Test
    @DisplayName("healthy topic view context keeps long-form props only and passes unchanged")
    void healthyTopicViewContextPasses() {
        Assoc topicmapContext = topicmapContext(921503, true, true, true, true, false, false, false, false);

        assertThatCode(() -> TopicmapContextGuard.requireTopicViewProps(topicmapContext))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("healthy assoc view context is unaffected when only long-form visibility props exist")
    void healthyAssocViewContextPasses() {
        Assoc topicmapContext = topicmapContext(921342, false, false, true, true, false, false, false, false);

        assertThatCode(() -> TopicmapContextGuard.requireAssocViewProps(topicmapContext))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("short-key-only malformed topic context names offending assoc and missing long-form keys")
    void malformedTopicViewContextNamesAssocAndMissingKeys() {
        Assoc topicmapContext = topicmapContext(921471, false, false, false, false, true, true, true, true);

        assertThatThrownBy(() -> TopicmapContextGuard.requireTopicViewProps(topicmapContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("921471")
            .hasMessageContaining(X)
            .hasMessageContaining(Y)
            .hasMessageContaining(VISIBILITY)
            .hasMessageContaining(PINNED)
            .hasMessageContaining("x")
            .hasMessageContaining("y")
            .hasMessageContaining("visibility")
            .hasMessageContaining("pinned")
            .hasMessageContaining("safe copy-forward shape");
    }

    @Test
    @DisplayName("visibility lookup on malformed short-key-only context surfaces the offending assoc")
    void malformedVisibilityLookupNamesAssocAndMissingVisibility() {
        Assoc topicmapContext = topicmapContext(921404, false, false, false, false, true, true, true, true);

        assertThatThrownBy(() -> TopicmapContextGuard.requireProperty(
            topicmapContext, VISIBILITY, "topicmap_context visibility lookup"
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("921404")
            .hasMessageContaining(VISIBILITY)
            .hasMessageContaining(X)
            .hasMessageContaining(Y)
            .hasMessageContaining(PINNED);
    }

    @Test
    @DisplayName("safe copy-forward marker only applies to the exact short-key-only topic shape")
    void safeCopyForwardMarkerStaysNarrow() {
        Assoc exactShape = topicmapContext(921471, false, false, false, false, true, true, true, true);
        Assoc partialShape = topicmapContext(921426, false, false, false, false, false, false, true, true);
        Assoc mixedLongAndShortShape = topicmapContext(921420, true, false, false, false, true, true, true, true);

        assertThatCode(() -> {
            if (!TopicmapContextGuard.matchesExactSafeCopyForwardShape(exactShape)) {
                throw new IllegalStateException("expected exact malformed topic shape");
            }
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            if (TopicmapContextGuard.matchesExactSafeCopyForwardShape(partialShape)) {
                throw new IllegalStateException("partial malformed shape must stay outside exact copy-forward marker");
            }
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            if (TopicmapContextGuard.matchesExactSafeCopyForwardShape(mixedLongAndShortShape)) {
                throw new IllegalStateException("mixed long-and-short shape must stay outside exact copy-forward marker");
            }
        }).doesNotThrowAnyException();
    }

    private Assoc topicmapContext(long id, boolean hasLongX, boolean hasLongY, boolean hasLongVisibility,
                                  boolean hasLongPinned, boolean hasShortX, boolean hasShortY,
                                  boolean hasShortVisibility, boolean hasShortPinned) {
        Assoc topicmapContext = mock(Assoc.class);
        when(topicmapContext.getId()).thenReturn(id);
        when(topicmapContext.hasProperty(X)).thenReturn(hasLongX);
        when(topicmapContext.hasProperty(Y)).thenReturn(hasLongY);
        when(topicmapContext.hasProperty(VISIBILITY)).thenReturn(hasLongVisibility);
        when(topicmapContext.hasProperty(PINNED)).thenReturn(hasLongPinned);
        when(topicmapContext.hasProperty("x")).thenReturn(hasShortX);
        when(topicmapContext.hasProperty("y")).thenReturn(hasShortY);
        when(topicmapContext.hasProperty("visibility")).thenReturn(hasShortVisibility);
        when(topicmapContext.hasProperty("pinned")).thenReturn(hasShortPinned);
        return topicmapContext;
    }
}
