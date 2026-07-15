package data.scripts.effects.bendingeffect;

import ashlib.data.plugins.misc.AshMisc;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;

public class BendingShipEffectData {

    public static class BendingEffectData {

        /*
         * Circle mode:
         *     size = diameter of the rendered effect
         *     radius = normalized inner radius, 0 to 0.5
         *
         * Cylinder mode:
         *     radius = cylinder radius in world units
         *     cylinderHeight = total cylinder height in world units
         *     size is ignored
         */
        public float size;
        public float radius;

        public float minStrength;
        public float maxStrength;

        public Vector2f coordinates;

        /*
         * Null means circle mode.
         */
        public Float cylinderHeight;

        /*
         * Angle relative to the ship sprite in degrees.
         */
        public float angle;

        public BendingEffectData(
                float size,
                float radius,
                float minStrength,
                float maxStrength,
                float x,
                float y
        ) {
            this(
                    size,
                    radius,
                    minStrength,
                    maxStrength,
                    new Vector2f(x, y),
                    null,
                    0f
            );
        }

        public BendingEffectData(
                float size,
                float radius,
                float minStrength,
                float maxStrength,
                Vector2f coordinates
        ) {
            this(
                    size,
                    radius,
                    minStrength,
                    maxStrength,
                    coordinates,
                    null,
                    0f
            );
        }

        public BendingEffectData(
                float size,
                float radius,
                float minStrength,
                float maxStrength,
                Vector2f coordinates,
                Float cylinderHeight,
                float angle
        ) {
            this.size = size;
            this.radius = radius;
            this.minStrength = minStrength;
            this.maxStrength = maxStrength;
            this.coordinates = coordinates;
            this.cylinderHeight = cylinderHeight;
            this.angle = angle;
        }

        public boolean isCylinder() {
            return cylinderHeight != null
                    && cylinderHeight > 0f;
        }
    }

    public String shipId;
    public ArrayList<BendingEffectData> effects;

    public static BendingShipEffectData getDataFromJson(
            JSONObject object
    ) throws JSONException {
        if (object == null) {
            return null;
        }

        String shipId =
                getStringValue(object, "ship_id").trim();

        if (!AshMisc.isStringValid(shipId)) {
            return null;
        }

        int amountOfInstances =
                getAmountOfInstances(object);

        if (amountOfInstances <= 0) {
            throw new JSONException(
                    "Ship \"" + shipId
                            + "\" must have at least one bending instance."
            );
        }

        /*
         * Size is optional for cylinder instances, since cylinder geometry
         * uses radius and cylinder_height.
         */
        String[] sizes = getOptionalValuesForInstances(
                object,
                "size",
                amountOfInstances,
                shipId
        );

        String[] radii = getValuesForInstances(
                object,
                "radius",
                amountOfInstances,
                shipId
        );

        String[] minStrengths = getValuesForInstances(
                object,
                "min_strength",
                amountOfInstances,
                shipId
        );

        String[] maxStrengths = getValuesForInstances(
                object,
                "max_strength",
                amountOfInstances,
                shipId
        );

        String[] coordinates = getValuesForInstances(
                object,
                "coordinates_on_ship",
                amountOfInstances,
                shipId
        );

        /*
         * Both columns are optional for backwards compatibility.
         */
        String[] cylinderHeights =
                getOptionalValuesForInstances(
                        object,
                        "cylinder_height",
                        amountOfInstances,
                        shipId
                );

        String[] angles =
                getOptionalValuesForInstances(
                        object,
                        "angle",
                        amountOfInstances,
                        shipId
                );

        BendingShipEffectData result =
                new BendingShipEffectData();

        result.shipId = shipId;
        result.effects =
                new ArrayList<>(amountOfInstances);

        for (int i = 0; i < amountOfInstances; i++) {
            Float cylinderHeight =
                    parseOptionalFloat(
                            cylinderHeights[i],
                            "cylinder_height",
                            shipId,
                            i
                    );

            boolean isCylinder =
                    cylinderHeight != null;

            Float parsedSize =
                    parseOptionalFloat(
                            sizes[i],
                            "size",
                            shipId,
                            i
                    );

            float radius = parseFloat(
                    radii[i],
                    "radius",
                    shipId,
                    i
            );

            float minStrength = parseFloat(
                    minStrengths[i],
                    "min_strength",
                    shipId,
                    i
            );

            float maxStrength = parseFloat(
                    maxStrengths[i],
                    "max_strength",
                    shipId,
                    i
            );

            float angle = parseOptionalFloatOrDefault(
                    angles[i],
                    "angle",
                    shipId,
                    i,
                    0f
            );

            Vector2f coordinate =
                    parseCoordinates(
                            coordinates[i],
                            shipId,
                            i
                    );

            float size;

            if (isCylinder) {
                validateCylinder(
                        radius,
                        cylinderHeight,
                        shipId,
                        i
                );

                /*
                 * Size is not used by cylinders, but preserve a sensible
                 * value inside the data object.
                 */
                size = parsedSize != null
                        ? parsedSize
                        : radius * 2f;
            } else {
                if (parsedSize == null) {
                    throw new JSONException(
                            "Column \"size\" is empty for circular bending "
                                    + "instance " + (i + 1)
                                    + " of ship \"" + shipId + "\"."
                    );
                }

                size = parsedSize;

                validateCircle(
                        size,
                        radius,
                        shipId,
                        i
                );
            }

            validateStrengths(
                    minStrength,
                    maxStrength,
                    shipId,
                    i
            );

            result.effects.add(
                    new BendingEffectData(
                            size,
                            radius,
                            minStrength,
                            maxStrength,
                            coordinate,
                            cylinderHeight,
                            angle
                    )
            );
        }

        return result;
    }

    private static void validateCircle(
            float size,
            float radius,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        if (size <= 0f) {
            throw new JSONException(
                    "Circle size must be greater than 0 for bending instance "
                            + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }

        if (radius < 0f || radius > 0.5f) {
            throw new JSONException(
                    "Circular radius must be between 0 and 0.5 for bending "
                            + "instance " + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }
    }

    private static void validateCylinder(
            float radius,
            float cylinderHeight,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        if (radius <= 0f) {
            throw new JSONException(
                    "Cylinder radius must be greater than 0 for bending "
                            + "instance " + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }

        if (cylinderHeight <= 0f) {
            throw new JSONException(
                    "Cylinder height must be greater than 0 for bending "
                            + "instance " + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }

        float cylinderWidth = radius * 2f;

        if (cylinderHeight < cylinderWidth) {
            throw new JSONException(
                    "Cylinder height must be at least radius * 2. Instance "
                            + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\" has radius "
                            + radius + ", width " + cylinderWidth
                            + " and height " + cylinderHeight + "."
            );
        }
    }

    private static void validateStrengths(
            float minStrength,
            float maxStrength,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        if (minStrength < 0f || maxStrength < 0f) {
            throw new JSONException(
                    "Bending strengths cannot be negative for instance "
                            + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }

        if (minStrength > maxStrength) {
            throw new JSONException(
                    "min_strength cannot be greater than max_strength for "
                            + "bending instance " + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }
    }

    private static int getAmountOfInstances(
            JSONObject object
    ) throws JSONException {
        String columnName;

        if (object.has("amount_of_bending_instances")) {
            columnName = "amount_of_bending_instances";
        } else if (object.has("amount_of_instances")) {
            columnName = "amount_of_instances";
        } else {
            throw new JSONException(
                    "Missing \"amount_of_bending_instances\" column."
            );
        }

        String value =
                getStringValue(object, columnName).trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new JSONException(
                    "Invalid value for \"" + columnName
                            + "\": \"" + value + "\"."
            );
        }
    }

    private static String[] getValuesForInstances(
            JSONObject object,
            String columnName,
            int amountOfInstances,
            String shipId
    ) throws JSONException {
        String rawValue =
                getStringValue(object, columnName).trim();

        if (!AshMisc.isStringValid(rawValue)) {
            throw new JSONException(
                    "Column \"" + columnName
                            + "\" is empty for ship \"" + shipId + "\"."
            );
        }

        if (!rawValue.contains(";")) {
            String[] values =
                    new String[amountOfInstances];

            Arrays.fill(values, rawValue);
            return values;
        }

        String[] values =
                rawValue.split(";", -1);

        if (values.length != amountOfInstances) {
            throw new JSONException(
                    "Column \"" + columnName
                            + "\" for ship \"" + shipId
                            + "\" contains " + values.length
                            + " values, but " + amountOfInstances
                            + " bending instances were declared."
            );
        }

        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();

            if (!AshMisc.isStringValid(values[i])) {
                throw new JSONException(
                        "Column \"" + columnName
                                + "\" for ship \"" + shipId
                                + "\" has an empty value for bending instance "
                                + (i + 1) + "."
                );
            }
        }

        return values;
    }

    /**
     * Optional columns support:
     *
     * Empty entire cell:
     *     every instance receives null
     *
     * One value:
     *     applied to every instance
     *
     * Semicolon-separated values:
     *     empty individual entries receive null
     *
     * Example:
     *     ;180
     *
     * First instance is a circle, second has cylinder height 180.
     */
    private static String[] getOptionalValuesForInstances(
            JSONObject object,
            String columnName,
            int amountOfInstances,
            String shipId
    ) throws JSONException {
        String[] emptyValues =
                new String[amountOfInstances];

        if (!object.has(columnName)) {
            return emptyValues;
        }

        Object rawObject =
                object.get(columnName);

        if (rawObject == null
                || rawObject == JSONObject.NULL) {
            return emptyValues;
        }

        String rawValue =
                String.valueOf(rawObject).trim();

        if (!AshMisc.isStringValid(rawValue)) {
            return emptyValues;
        }

        if (!rawValue.contains(";")) {
            String[] values =
                    new String[amountOfInstances];

            Arrays.fill(values, rawValue);
            return values;
        }

        String[] values =
                rawValue.split(";", -1);

        if (values.length != amountOfInstances) {
            throw new JSONException(
                    "Optional column \"" + columnName
                            + "\" for ship \"" + shipId
                            + "\" contains " + values.length
                            + " values, but " + amountOfInstances
                            + " bending instances were declared."
            );
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();

            values[i] = AshMisc.isStringValid(value)
                    ? value
                    : null;
        }

        return values;
    }

    private static Vector2f parseCoordinates(
            String value,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        String[] split =
                value.split(",", -1);

        if (split.length != 2) {
            throw new JSONException(
                    "Invalid coordinates \"" + value
                            + "\" for bending instance "
                            + (instanceIndex + 1)
                            + " of ship \"" + shipId
                            + "\". Expected format: x,y"
            );
        }

        float x = parseFloat(
                split[0],
                "coordinates_on_ship.x",
                shipId,
                instanceIndex
        );

        float y = parseFloat(
                split[1],
                "coordinates_on_ship.y",
                shipId,
                instanceIndex
        );

        return new Vector2f(x, y);
    }

    private static float parseOptionalFloatOrDefault(
            String value,
            String columnName,
            String shipId,
            int instanceIndex,
            float defaultValue
    ) throws JSONException {
        Float parsed = parseOptionalFloat(
                value,
                columnName,
                shipId,
                instanceIndex
        );

        return parsed != null
                ? parsed
                : defaultValue;
    }

    private static Float parseOptionalFloat(
            String value,
            String columnName,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        if (!AshMisc.isStringValid(value)) {
            return null;
        }

        return parseFloat(
                value,
                columnName,
                shipId,
                instanceIndex
        );
    }

    private static float parseFloat(
            String value,
            String columnName,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            throw new JSONException(
                    "Invalid number \"" + value
                            + "\" in column \"" + columnName
                            + "\" for bending instance "
                            + (instanceIndex + 1)
                            + " of ship \"" + shipId + "\"."
            );
        }
    }

    private static String getStringValue(
            JSONObject object,
            String columnName
    ) throws JSONException {
        if (!object.has(columnName)) {
            throw new JSONException(
                    "Missing required column \""
                            + columnName + "\"."
            );
        }

        Object value =
                object.get(columnName);

        if (value == null
                || value == JSONObject.NULL) {
            throw new JSONException(
                    "Column \"" + columnName
                            + "\" contains no value."
            );
        }

        return String.valueOf(value);
    }
}