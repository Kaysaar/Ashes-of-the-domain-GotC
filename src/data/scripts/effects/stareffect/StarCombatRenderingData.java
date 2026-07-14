package data.scripts.effects.stareffect;

import ashlib.data.plugins.misc.AshMisc;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StarCombatRenderingData {

    public static class StarSlotData {
        public String slotId;
        public float starSize;

        public StarSlotData(String slotId, float starSize) {
            this.slotId = slotId;
            this.starSize = starSize;
        }
    }

    public String shipId;
    public ArrayList<StarSlotData> starSlots = new ArrayList<>();

    public StarSlotData getStarSlotData(String slotId) {
        for (StarSlotData slotData : starSlots) {
            if (slotData.slotId.equals(slotId)) {
                return slotData;
            }
        }
        return null;
    }
    public static StarCombatRenderingData getDataFromJson(
            JSONObject object
    ) throws JSONException {
        if (object == null) {
            return null;
        }

        String shipId = getStringValue(object, "ship_id").trim();

        if (!AshMisc.isStringValid(shipId)) {
            return null;
        }

        String rawSlotIds = getStringValue(object, "slot_id").trim();
        String rawStarSizes = getStringValue(object, "star_size").trim();

        if (!AshMisc.isStringValid(rawSlotIds)) {
            throw new JSONException(
                    "Column \"slot_id\" is empty for ship \"" + shipId + "\"."
            );
        }

        if (!AshMisc.isStringValid(rawStarSizes)) {
            throw new JSONException(
                    "Column \"star_size\" is empty for ship \"" + shipId + "\"."
            );
        }

        String[] slotIds = splitValues(
                rawSlotIds,
                "slot_id",
                shipId
        );

        String[] starSizes = getValuesForSlots(
                rawStarSizes,
                slotIds.length,
                shipId
        );

        StarCombatRenderingData result =
                new StarCombatRenderingData();

        result.shipId = shipId;
        result.starSlots = new ArrayList<>(slotIds.length);

        for (int i = 0; i < slotIds.length; i++) {
            String slotId = slotIds[i].trim();

            if (!AshMisc.isStringValid(slotId)) {
                throw new JSONException(
                        "Empty slot ID at index " + i +
                                " for ship \"" + shipId + "\"."
                );
            }

            float starSize = parseFloat(
                    starSizes[i],
                    "star_size",
                    shipId,
                    i
            );

            result.starSlots.add(
                    new StarSlotData(
                            slotId,
                            starSize
                    )
            );
        }

        return result;
    }

    private static String[] getValuesForSlots(
            String rawValue,
            int slotAmount,
            String shipId
    ) throws JSONException {
        /*
         * A single star size applies to every slot.
         *
         * Example:
         * slot_id:   SUN1;SUN2;SUN3
         * star_size: 20
         *
         * Result:
         * SUN1 = 20
         * SUN2 = 20
         * SUN3 = 20
         */
        if (!rawValue.contains(";")) {
            String[] values = new String[slotAmount];
            Arrays.fill(values, rawValue.trim());

            return values;
        }

        String[] values = splitValues(
                rawValue,
                "star_size",
                shipId
        );

        if (values.length != slotAmount) {
            throw new JSONException(
                    "Column \"star_size\" for ship \"" + shipId +
                            "\" contains " + values.length +
                            " values, but \"slot_id\" contains " +
                            slotAmount + " slots."
            );
        }

        return values;
    }

    private static String[] splitValues(
            String rawValue,
            String columnName,
            String shipId
    ) throws JSONException {
        String[] values = rawValue.split(";", -1);

        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();

            if (!AshMisc.isStringValid(values[i])) {
                throw new JSONException(
                        "Column \"" + columnName +
                                "\" contains an empty value at index " +
                                i + " for ship \"" + shipId + "\"."
                );
            }
        }

        return values;
    }

    private static float parseFloat(
            String value,
            String columnName,
            String shipId,
            int index
    ) throws JSONException {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            throw new JSONException(
                    "Invalid number \"" + value +
                            "\" in column \"" + columnName +
                            "\" at index " + index +
                            " for ship \"" + shipId + "\"."
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