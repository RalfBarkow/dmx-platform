package systems.dmx.dita;

import systems.dmx.core.impl.CoreServiceTestEnvironment;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DitaProcessorPayloadTest extends CoreServiceTestEnvironment {

    @Test
    public void processorPayloadRejectsNestedValueObject() throws Exception {
        JSONObject payload = new JSONObject()
            .put("typeUri", "dmx.dita.processor")
            .put("children", new JSONObject()
                .put("dmx.dita.processor_name", new JSONObject()
                    .put("value", new JSONObject().put("children", new JSONObject()))
                )
                .put("dmx.dita.output_format", new JSONObject()
                    .put("value", "html5")
                )
            );

        try {
            mf.newTopicModel(payload);
            fail("Expected payload with nested value object to be rejected");
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            assertTrue("Expected IllegalArgumentException, got " + root,
                root instanceof IllegalArgumentException);
        }
    }
}
