package data;

import java.util.ArrayList;

import IK.floatIK.AbstractBone;

public interface LoadManager {

	default <T extends Saveable> T getObjectFor(Class objectClass, JSONObject j, String hash) {
		if(j.hasKey(hash)) {
			return (T)getObjectFromClassMaps(objectClass, j.getString(hash));
		} else return null;
	}
	
	public Saveable getObjectFromClassMaps(Class keyClass, String identityHash);
	
	public static TypeIdentifier getNewTypeIdentifier(Object k, Object v) {
		return new TypeIdentifier(k, v);
	}

	public <T extends Object> void arrayListFromJSONArray(JSONArray jsonArray, ArrayList<T> list, Class c);
}
