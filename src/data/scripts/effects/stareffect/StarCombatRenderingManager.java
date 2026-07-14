package data.scripts.effects.stareffect;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StarCombatRenderingManager {

    private static final String DATA_PATH =
            "data/vfx/star_vfx_data.csv";

    private static final LinkedHashMap<String, StarCombatRenderingData>
            shipRenderingData = new LinkedHashMap<>();

    private StarCombatRenderingManager() {
    }

    public static StarCombatRenderingData getDataFromList(
            String shipId
    ) {
        if (shipId == null) {
            return null;
        }

        return shipRenderingData.get(shipId);
    }

    public static boolean hasData(String shipId) {
        return shipId != null
                && shipRenderingData.containsKey(shipId);
    }

    public static Map<String, StarCombatRenderingData> getAllData() {
        return Collections.unmodifiableMap(shipRenderingData);
    }

    public static void clearData() {
        shipRenderingData.values().forEach(data -> {
            if (data != null && data.starSlots != null) {
                data.starSlots.clear();
            }
        });

        shipRenderingData.clear();
    }

    public static void loadData() {
        clearData();

        try {
            JSONArray array =
                    Global.getSettings().getMergedSpreadsheetData(
                            "ship_id",
                            DATA_PATH
                    );

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);

                StarCombatRenderingData data =
                        StarCombatRenderingData.getDataFromJson(object);

                if (data == null) {
                    continue;
                }

                shipRenderingData.put(
                        data.shipId,
                        data
                );
            }
        } catch (IOException | JSONException exception) {
            clearData();

            throw new RuntimeException(
                    "Failed to load star combat rendering data from \"" +
                            DATA_PATH + "\".",
                    exception
            );
        }
    }
}