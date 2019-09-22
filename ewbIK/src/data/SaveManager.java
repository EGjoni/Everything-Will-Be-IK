package data;

import java.util.ArrayList;
import java.util.HashMap;

public interface SaveManager {
		public void addToSaveState(Saveable saveble);
		public void removeFromSaveState(Saveable saveObj);
		public void clearSaveState();
		public JSONObject hashMapToJSON(HashMap<?, ?> hm);
		public JSONArray arrayListToJSONArray(ArrayList<?> al);
		public JSONArray primitiveArrayToJSONArray(Object a);
}
