import java.util.ArrayList;

import data.JSONArray;
import data.JSONObject;

public interface LoadManager {

	default <T extends Saveable> T getObjectFor(Class objectClass, JSONObject j, String hashKey) {
		if(j.hasKey(hashKey)) {
			return (T)getObjectFromClassMaps(objectClass, j.getString(hashKey));
		} else return null;
	}
	
	default <T extends Saveable> T getObjectFor(Class objectClass, String hash) {
			return (T)getObjectFromClassMaps(objectClass, hash);
	}
	
	public Saveable getObjectFromClassMaps(Class keyClass, String identityHash);
	
	public static TypeIdentifier getNewTypeIdentifier(Object k, Object v) {
		return new TypeIdentifier(k, v);
	}

	public <T extends Object> void arrayListFromJSONArray(JSONArray jsonArray, ArrayList<T> list, Class c);
}
