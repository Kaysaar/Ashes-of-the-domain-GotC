package data.scripts.effects.bendingeffect;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;

public class BendingEffectDataHandler {
   public static LinkedHashMap<String,BendingShipEffectData>shipEffectData = new LinkedHashMap<>();

   public  static BendingShipEffectData getDataFromList(String id){
       return shipEffectData.getOrDefault(id,null);
   }
   public static void loadData(){
       shipEffectData.values().forEach(x->x.effects.clear());
       shipEffectData.clear();
       try {
           JSONArray array = Global.getSettings().getMergedSpreadsheetData("ship_id","data/vfx/bending_vfx_data.csv");
           for (int i = 0; i < array.length(); i++) {
               JSONObject object = array.getJSONObject(i);
               BendingShipEffectData data = BendingShipEffectData.getDataFromJson(object);
               shipEffectData.put(data.shipId,data);
           }
       } catch (IOException e) {
           throw new RuntimeException(e);
       } catch (JSONException e) {
           throw new RuntimeException(e);
       }
   }
}
