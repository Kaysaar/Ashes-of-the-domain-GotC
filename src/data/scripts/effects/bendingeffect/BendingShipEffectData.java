package data.scripts.effects.bendingeffect;

import ashlib.data.plugins.misc.AshMisc;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;

public class BendingShipEffectData {

    public static class BendingEffectData {
        public float size;
        public float radius;
        public float minStrength;
        public float maxStrength;
        public Vector2f coordinates;

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
                    new Vector2f(x, y)
            );
        }

        public BendingEffectData(
                float size,
                float radius,
                float minStrength,
                float maxStrength,
                Vector2f coordinates
        ) {
            this.size = size;
            this.radius = radius;
            this.minStrength = minStrength;
            this.maxStrength = maxStrength;
            this.coordinates = coordinates;
        }
    }

    public String shipId;
    public ArrayList<BendingEffectData> effects;

    public static BendingShipEffectData getDataFromJson(JSONObject object) throws JSONException {
        if (object == null) {
            return null;
        }

        String shipId = getStringValue(object, "ship_id").trim();

        if (!AshMisc.isStringValid(shipId)) {
            return null;
        }

        int amountOfInstances = getAmountOfInstances(object);

        if (amountOfInstances <= 0) {
            throw new JSONException(
                    "Ship \"" + shipId + "\" must have at least one bending instance."
            );
        }

        String[] sizes = getValuesForInstances(
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

        BendingShipEffectData result = new BendingShipEffectData();
        result.shipId = shipId;
        result.effects = new ArrayList<>(amountOfInstances);

        for (int i = 0; i < amountOfInstances; i++) {
            float size = parseFloat(
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

            Vector2f coordinate = parseCoordinates(
                    coordinates[i],
                    shipId,
                    i
            );

            result.effects.add(
                    new BendingEffectData(
                            size,
                            radius,
                            minStrength,
                            maxStrength,
                            coordinate
                    )
            );
        }

        return result;
    }

    private static int getAmountOfInstances(JSONObject object) throws JSONException {
        String columnName;

        if (object.has("amount_of_bending_instances")) {
            columnName = "amount_of_bending_instances";
        } else if (object.has("amount_of_instances")) {
            // Compatibility with the name used by the older parser.
            columnName = "amount_of_instances";
        } else {
            throw new JSONException(
                    "Missing \"amount_of_bending_instances\" column."
            );
        }

        String value = getStringValue(object, columnName).trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new JSONException(
                    "Invalid value for \"" + columnName + "\": \"" + value + "\"."
            );
        }
    }


    private static String[] getValuesForInstances(
            JSONObject object,
            String columnName,
            int amountOfInstances,
            String shipId
    ) throws JSONException {
        String rawValue = getStringValue(object, columnName).trim();

        if (!AshMisc.isStringValid(rawValue)) {
            throw new JSONException(
                    "Column \"" + columnName + "\" is empty for ship \"" + shipId + "\"."
            );
        }

        if (!rawValue.contains(";")) {
            String[] values = new String[amountOfInstances];
            Arrays.fill(values, rawValue);
            return values;
        }

        String[] values = rawValue.split(";", -1);

        if (values.length != amountOfInstances) {
            throw new JSONException(
                    "Column \"" + columnName + "\" for ship \"" + shipId +
                            "\" contains " + values.length + " values, but " +
                            amountOfInstances + " bending instances were declared."
            );
        }

        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();

            if (!AshMisc.isStringValid(values[i])) {
                throw new JSONException(
                        "Column \"" + columnName + "\" for ship \"" + shipId +
                                "\" has an empty value for bending instance " + (i + 1) + "."
                );
            }
        }

        return values;
    }

    private static Vector2f parseCoordinates(
            String value,
            String shipId,
            int instanceIndex
    ) throws JSONException {
        String[] split = value.split(",", -1);

        if (split.length != 2) {
            throw new JSONException(
                    "Invalid coordinates \"" + value + "\" for bending instance " +
                            (instanceIndex + 1) + " of ship \"" + shipId +
                            "\". Expected format: x,y"
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
                    "Invalid number \"" + value + "\" in column \"" + columnName +
                            "\" for bending instance " + (instanceIndex + 1) +
                            " of ship \"" + shipId + "\"."
            );
        }
    }


    private static String getStringValue(
            JSONObject object,
            String columnName
    ) throws JSONException {
        if (!object.has(columnName)) {
            throw new JSONException(
                    "Missing required column \"" + columnName + "\"."
            );
        }

        Object value = object.get(columnName);

        if (value == null || value == JSONObject.NULL) {
            throw new JSONException(
                    "Column \"" + columnName + "\" contains no value."
            );
        }

        return String.valueOf(value);
    }
}