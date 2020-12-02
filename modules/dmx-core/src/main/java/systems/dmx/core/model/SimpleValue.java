package systems.dmx.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A wrapper for the topic/assoc value (atomic, non-null). Supported value types are string, int, long, double, boolean.
 */
public class SimpleValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The (auto-boxed) wrapped value.
     * Either String, Integer, Long, Double, or Boolean.
     */
    private Object value;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Called by JAX-RS container to create a SimpleValue from a @PathParam or @QueryParam
     */
    public SimpleValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to build a SimpleValue from a null String");
        }
        this.value = value;
    }

    public SimpleValue(int value) {
        this.value = value;
    }

    public SimpleValue(long value) {
        this.value = value;
    }

    public SimpleValue(double value) {
        this.value = value;
    }

    public SimpleValue(boolean value) {
        this.value = value;
    }

    public SimpleValue(Object value) {
        // check argument
        if (value == null) {
            throw new IllegalArgumentException("Tried to build a SimpleValue from a null Object");
        }
        if (!(value instanceof String || value instanceof Integer || value instanceof Long ||
              value instanceof Double || value instanceof Boolean)) {
            throw new IllegalArgumentException("Tried to build a SimpleValue from a " + value.getClass().getName() +
                " (expected are String, Integer, Long, Double, or Boolean)");
        }
        //
        this.value = value;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public int intValue() {
        return (Integer) value;
    }

    public long longValue() {
        return (Long) value;
    }

    public double doubleValue() {
        return (Double) value;
    }

    public boolean booleanValue() {
        return (Boolean) value;
    }

    public Object value() {
        return value;
    }

    /**
     * Trims this value in-place, provided it is of type String. Does nothing otherwise.
     *
     * @return  this object
     */
    public SimpleValue trim() {
        if (value instanceof String) {
            value = ((String) value).trim();
        }
        return this;
    }

    // ---

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SimpleValue)) {
            return false;
        }
        return ((SimpleValue) o).value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
