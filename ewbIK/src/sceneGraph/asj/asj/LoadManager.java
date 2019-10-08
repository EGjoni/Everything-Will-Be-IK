package asj;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import asj.data.JSONArray;
import asj.data.JSONObject;
import jdk.internal.dynalink.support.ClassMap;

public abstract class LoadManager {
	
	public HashMap<Class, HashMap<String, JSONObject>> jsonObjects = new HashMap<>();
	public HashMap<Class, HashMap<String, Saveable>>  classObjects = new HashMap<>();
	public ArrayList<Saveable> allLoadedObjects = new ArrayList<>();

	public <T extends Saveable> T getObjectFor(Class objectClass, JSONObject j, String hashKey)  {
		if(j.hasKey(hashKey)) {
			return (T)getObjectFromClassMaps(objectClass, j.getString(hashKey));
		} else return null;
	}
	
	public <T extends Saveable> T getObjectFor(Class objectClass, String hash) throws Exception {
			return (T)getObjectFromClassMaps(objectClass, hash);
	}
	
	public <T> void createEmptyLoadMaps (Map<String, JSONObject> jMap, Map<String, ? super T>oMap, JSONArray jArr, Class<T> c) {
		try {
			for(int i=0; i < jArr.size(); i++) {
				JSONObject jo = jArr.getJSONObject(i);
				String id = jo.getString("identityHash");

				jMap.put(id, jo);
				Object created = c.newInstance();
				oMap.put(id, (T) created);			
				allLoadedObjects.add((Saveable)created);

			}
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This function should be called when initializing the loader object so that it knows 
	 * which keys in the JSON file correspond to which classes you intend to load.
	 * 
	 * @param classMap a map of Class objects and their corresponding 
	 * JSON key names.
	 */
	public void initializeClassMaps(HashMap<String, Class> classMap) {
		for(String k : classMap.keySet()) {
			Class c = classMap.get(k);
			if(jsonObjects.get(k) == null) {
				jsonObjects.put(c, new HashMap<>());
			}
			if(classObjects.get(c) == null) {
				classObjects.put(c, new HashMap<>());
			}
		}
	}
	
	public Saveable getObjectFromClassMaps(Class keyClass, String identityHash) {
		HashMap<String, Saveable> objectMap = classObjects.get(keyClass);
		if(objectMap != null) {
			return objectMap.get(identityHash);
		} else {
			try {
			throw new Exception("Class not found in object maps. Either define the class using the InitializeClassMaps " +
					"method in LoadManager, or override the getObjectFromClassMaps method to handle whatever edgecase you've encountered.");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static TypeIdentifier getNewTypeIdentifier(Object k, Object v) {
		return new TypeIdentifier(k, v);
	}
	
	/**
	 * general loader for when nothing fancy is required (I should make pretty much everything use this eventually)
	 * @param jsonForm
	 * @param saveableForm
	 */
	public void loadGenerally(HashMap<String, JSONObject> jsonForm, HashMap<String, ? extends Saveable> saveableForm) {
		Collection<String> K =  jsonForm.keySet();
		for(String k : K) {
			JSONObject kj = jsonForm.get(k);
			Saveable si = saveableForm.get(k);
			si.loadFromJSONObject(kj, this);
		}
	}

	public abstract <T extends Object> void arrayListFromJSONArray(JSONArray jsonArray, ArrayList<T> list, Class c);
}
