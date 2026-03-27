package systems.dmx.topicmaps;

import systems.dmx.core.Assoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static systems.dmx.topicmaps.Constants.PINNED;
import static systems.dmx.topicmaps.Constants.TOPICMAP_CONTEXT;
import static systems.dmx.topicmaps.Constants.VISIBILITY;
import static systems.dmx.topicmaps.Constants.X;
import static systems.dmx.topicmaps.Constants.Y;



final class TopicmapContextGuard {

    private static final String SHORT_X = "x";
    private static final String SHORT_Y = "y";
    private static final String SHORT_VISIBILITY = "visibility";
    private static final String SHORT_PINNED = "pinned";

    private static final List<String> TOPIC_VIEW_KEYS = Collections.unmodifiableList(
        Arrays.asList(X, Y, VISIBILITY, PINNED)
    );

    private static final List<String> ASSOC_VIEW_KEYS = Collections.unmodifiableList(
        Arrays.asList(VISIBILITY, PINNED)
    );

    private static final List<String> LEGACY_SHORT_KEYS = Collections.unmodifiableList(
        Arrays.asList(SHORT_X, SHORT_Y, SHORT_VISIBILITY, SHORT_PINNED)
    );

    private TopicmapContextGuard() {
    }

    static void requireTopicViewProps(Assoc topicmapContext) {
        requireLongFormProps(topicmapContext, TOPIC_VIEW_KEYS, "topic rendering");
    }

    static void requireAssocViewProps(Assoc topicmapContext) {
        requireLongFormProps(topicmapContext, ASSOC_VIEW_KEYS, "association rendering");
    }

    static void requireProperty(Assoc topicmapContext, String propUri, String accessMode) {
        List<String> missingRequiredKeys = topicmapContext.hasProperty(propUri)
            ? Collections.emptyList()
            : Collections.singletonList(propUri);
        if (!missingRequiredKeys.isEmpty()) {
            throw new IllegalStateException(malformedMessage(topicmapContext, missingRequiredKeys, accessMode));
        }
    }

    static boolean matchesExactSafeCopyForwardShape(Assoc topicmapContext) {
        return hasAllProps(topicmapContext, LEGACY_SHORT_KEYS) && missingAny(topicmapContext, TOPIC_VIEW_KEYS);
    }

    private static void requireLongFormProps(Assoc topicmapContext, List<String> requiredKeys, String accessMode) {
        List<String> missingRequiredKeys = missingKeys(topicmapContext, requiredKeys);
        if (!missingRequiredKeys.isEmpty()) {
            throw new IllegalStateException(malformedMessage(topicmapContext, missingRequiredKeys, accessMode));
        }
    }

    private static String malformedMessage(Assoc topicmapContext, List<String> missingRequiredKeys, String accessMode) {
        StringBuilder message = new StringBuilder();
        message.append("Malformed ")
            .append(TOPICMAP_CONTEXT)
            .append(" assoc ")
            .append(topicmapContext.getId())
            .append(": missing required long-form view props ")
            .append(missingRequiredKeys)
            .append(" during ")
            .append(accessMode)
            .append(".");
        List<String> missingKnownLongFormKeys = missingKeys(topicmapContext, TOPIC_VIEW_KEYS);
        if (!missingKnownLongFormKeys.isEmpty()) {
            message.append(" Known missing topicmap_context view props on this assoc: ")
                .append(missingKnownLongFormKeys)
                .append(".");
        }
        List<String> legacyShortKeysPresent = presentShortKeys(topicmapContext);
        if (!legacyShortKeysPresent.isEmpty()) {
            message.append(" Legacy short-form props present: ")
                .append(legacyShortKeysPresent)
                .append(".");
        }
        if (matchesExactSafeCopyForwardShape(topicmapContext)) {
            message.append(" This assoc matches the known short-key-only safe copy-forward shape.");
        }
        return message.toString();
    }

    private static boolean hasAllProps(Assoc topicmapContext, List<String> propUris) {
        for (String propUri : propUris) {
            if (!topicmapContext.hasProperty(propUri)) {
                return false;
            }
        }
        return true;
    }

    private static boolean missingAny(Assoc topicmapContext, List<String> propUris) {
        for (String propUri : propUris) {
            if (!topicmapContext.hasProperty(propUri)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> missingKeys(Assoc topicmapContext, List<String> propUris) {
        List<String> missingKeys = new ArrayList<>();
        for (String propUri : propUris) {
            if (!topicmapContext.hasProperty(propUri)) {
                missingKeys.add(propUri);
            }
        }
        return missingKeys;
    }

    private static List<String> presentShortKeys(Assoc topicmapContext) {
        List<String> presentShortKeys = new ArrayList<>();
        for (String propUri : LEGACY_SHORT_KEYS) {
            if (topicmapContext.hasProperty(propUri)) {
                presentShortKeys.add(propUri);
            }
        }
        return presentShortKeys;
    }
}
