package data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import IK.floatIK.AbstractArmature;
import IK.floatIK.AbstractBone;
import IK.floatIK.AbstractIKPin;
import IK.floatIK.AbstractKusudama;
import IK.floatIK.AbstractLimitCone;
import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.MRotation;
import sceneGraph.math.doubleV.Rot;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.doubleV.sgRayd;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.sgRayf;

public class DoubleBackedLoader {
		
	public static File currentFilePath; 

	public static HashMap<String, JSONObject> 	    	axesJSONObjects 		= new HashMap<>();
	public static HashMap<String, AbstractAxes>			axesLoadObjects 		= new HashMap<>();  

	public static HashMap<String, JSONObject> 	    	armatureJSONObjects 	= new HashMap<>();
	public static HashMap<String, AbstractArmature>		armatureLoadObjects 	= new HashMap<>();  

	public static HashMap<String, JSONObject> 	    	boneJSONObjects 		= new HashMap<>(); 
	public static HashMap<String, AbstractBone> 		boneLoadObjects 		= new HashMap<>(); 

	public static HashMap<String, AbstractKusudama>		kusudamaLoadObjects 	= new HashMap<>();
	public static HashMap<String, JSONObject>	    	kusudamaJSONObjects 	= new HashMap<>(); 
	
	public static HashMap<String, AbstractLimitCone>	limitConeLoadObjects 	= new HashMap<>(); 
	public static HashMap<String, JSONObject>	    	limitConeJSONObjects 	= new HashMap<>();

	public static HashMap<String, AbstractIKPin>		IKPinLoadObjects 		= new HashMap<>();
	public static HashMap<String, JSONObject>	    	IKPinJSONObjects 		= new HashMap<>(); 


	public static ArrayList<Saveable> allLoadedObjects = new ArrayList<>();

	public boolean fileCorruptionDetected = false; 

	private String tempLoadDirectory;	
	
	private static boolean Load = false;
	
	public DoubleBackedLoader() {
		
	}

	public static void importFile(File selection) {
		JSONObject loadFile = StringFuncs.loadJSONObject(selection);
		clearCurrentLoadObjects();
		loadJSON(loadFile);
	}

	

	public static void loadJSON(JSONObject loadFile) {
		clearCurrentLoadObjects();		
		createEmptyLoadMaps(axesJSONObjects, axesLoadObjects, loadFile.getJSONArray("axes"), AbstractAxes.class);		
		createEmptyLoadMaps(boneJSONObjects, boneLoadObjects, loadFile.getJSONArray("bones"), AbstractBone.class);		
		createEmptyLoadMaps(armatureJSONObjects, armatureLoadObjects, loadFile.getJSONArray("armatures"), AbstractArmature.class);
		createEmptyLoadMaps(kusudamaJSONObjects, kusudamaLoadObjects, loadFile.getJSONArray("kusudamas"), AbstractKusudama.class);
		createEmptyLoadMaps(limitConeJSONObjects, limitConeLoadObjects, loadFile.getJSONArray("limitCones"), AbstractLimitCone.class);
		createEmptyLoadMaps(IKPinJSONObjects, IKPinLoadObjects, loadFile.getJSONArray("KeyableIKPins"), AbstractIKPin.class);

		loadGenerally(axesJSONObjects, axesLoadObjects);
		loadGenerally(IKPinJSONObjects, IKPinLoadObjects);
		loadGenerally(limitConeJSONObjects, limitConeLoadObjects);
		loadGenerally(kusudamaJSONObjects, kusudamaLoadObjects);
		loadGenerally(boneJSONObjects, boneLoadObjects);
		loadGenerally(armatureJSONObjects, armatureLoadObjects);


		
		for(Saveable s: allLoadedObjects) 
			s.notifyOfLoadCompletion();
		
		updateArmatureSegments();
		clearCurrentLoadObjects();

		
		System.gc();
	}

	static void clearCurrentSceneObjects() {
		// TODO Auto-generated method stub

	}

	public static  void updateArmatureSegments() {
		Collection<AbstractArmature> armatures = armatureLoadObjects.values();		
		for(AbstractArmature a : armatures) {
			a.refreshArmaturePins();			
		}
	}



	public static void clearCurrentLoadObjects() {

		axesJSONObjects.clear(); 			
		axesLoadObjects.clear();			

		armatureJSONObjects.clear();		
		armatureLoadObjects.clear(); 		

		boneJSONObjects.clear(); 			
		boneLoadObjects.clear(); 				

		kusudamaLoadObjects.clear(); 		
		kusudamaJSONObjects.clear(); 		
		limitConeLoadObjects.clear(); 		
		limitConeJSONObjects.clear(); 		

		allLoadedObjects.clear();

	}


	public static <T> void createEmptyLoadMaps (Map<String, JSONObject> jMap, Map<String, ? super T>oMap, JSONArray jArr, Class<T> c) {

		//Class cls[] = new Class[] {GiftedApprentice.class};

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
	 * general loader for when nothing fancy is required (I should make pretty much everything use this eventually)
	 * @param jsonForm
	 * @param saveableForm
	 */
	public static void loadGenerally(HashMap<String, JSONObject> jsonForm, HashMap<String, ? extends Saveable> saveableForm) {
		Collection<String> K =  jsonForm.keySet();
		for(String k : K) {
			JSONObject kj = jsonForm.get(k);
			Saveable si = saveableForm.get(k);
			si.loadFromJSONObject(kj);
		}
	}
	

	public static String getCurrentFilePath() {
		if(currentFilePath == null) {
			return "";
		} else {
			return currentFilePath.getAbsolutePath();
		}
	}


	/**
	 * takes a JSONObject and parses it into the format specified by the TypeIdentifier. 
	 * The Value parameter can be another hashmap, and this
	 * will nest hashmaps from jsonObjects accordingly.
	 * 
	 * @param json
	 * @param result
	 */
	public static <T extends Object, V extends Object> HashMap<T, V> hashMapFromJSON(JSONObject json, HashMap<T,V> result, TypeIdentifier ti) {
		Class keyClass = null;
		if(ti.key.getClass() == Class.class) {
			keyClass = (Class)ti.key;
		}
		Class valueClass = ti.value.getClass();
		if(valueClass == TypeIdentifier.class && keyClass != null) {
			Collection<String> jKeys = json.keys();
			for(String jk : jKeys) {
				JSONObject jValue = json.getJSONObject(jk);
				T keyObject = (T)getObjectFromClassMaps(keyClass, jk);

				HashMap<?, ?> innerHash = new HashMap<>();
				hashMapFromJSON(jValue, innerHash, (TypeIdentifier)ti.value);

				result.put(keyObject, (V)innerHash);
				return result;
			}
		} else {
			valueClass = (Class)ti.value;
			Collection<String> jKeys = json.keys();
			for(String jk : jKeys) {

				boolean javaClass = keyClass.getName().startsWith("java.lang"); 
				Object keyObject = javaClass ? parsePrimitive(keyClass, jk) : getObjectFromClassMaps(keyClass, jk);
				Object valueObject = null;
				if(valueClass == SGVec_3d.class) {
					valueObject = new SGVec_3d();
					((SGVec_3d)valueObject).populateSelfFromJSON(json);
					result.put((T)keyObject, (V)valueObject);
				} else {				
					Object obj = json.get(jk);
					valueObject = 
							valueClass.getName().startsWith("java.lang") ?  
									parsePrimitive(valueClass, ""+obj)
									: getObjectFromClassMaps(valueClass, json.getString(jk));
									result.put((T)keyObject, (V)valueObject);					
				}

			}
			return result;
		}
		return result;
	}


	/**
	 * takes a JSONObject and parses it into the format specified by the TypeIdentifier. 
	 * The Value parameter can be another hashmap, and this
	 * will nest hashmaps from jsonObjects accordingly.
	 * 
	 * @param json
	 * @param result
	 */
	public static <T extends Object, V extends Object> HashMap<T, V> hashMapFromJSON(JSONObject json, TypeIdentifier ti) {
		Class keyClass = null;
		if(ti.key.getClass() == Class.class) {
			keyClass = (Class)ti.key;
		}
		Class valueClass = ti.value.getClass();
		HashMap<T, V> result = new HashMap<>();
		if(valueClass == TypeIdentifier.class && keyClass != null) {
			Collection<String> jKeys = json.keys();
			for(String jk : jKeys) {
				JSONObject jValue = json.getJSONObject(jk);
				T keyObject = (T)getObjectFromClassMaps(keyClass, jk);
				HashMap<?, ?> innerHash = new HashMap<>();
				hashMapFromJSON(jValue, innerHash, (TypeIdentifier)ti.value);
				result.put(keyObject, (V)innerHash);
				return result;
			}
		} else {
			valueClass = (Class)ti.value;
			Collection<String> jKeys = json.keys();
			for(String jk : jKeys) {

				boolean javaClass = keyClass.getName().startsWith("java.lang"); 
				Object keyObject = javaClass ? parsePrimitive(keyClass, jk) : getObjectFromClassMaps(keyClass, jk);
				Object valueObject = null;
				if(valueClass == SGVec_3d.class) {
					valueObject = new SGVec_3d();
					((SGVec_3d)valueObject).populateSelfFromJSON(json);
					result.put((T)keyObject, (V)valueObject);
				}  else {				
					String hash = json.getString(jk);
					valueObject = getObjectFromClassMaps(valueClass, hash);
					result.put((T)keyObject, (V)valueObject);					
				}
			}
			return result;
		}
		return result;
	}



	public static Object parsePrimitive(Class keyClass, String toParse) {
		if(keyClass == String.class) return toParse;
		if(keyClass == Float.class) return Float.parseFloat(toParse);
		if(keyClass == Double.class) return Double.parseDouble(toParse);
		if(keyClass == Long.class) return Long.parseLong(toParse);
		if(keyClass == Boolean.class) return Boolean.parseBoolean(toParse);
		if(keyClass == Integer.class) return Integer.parseInt(toParse);
		if(keyClass == Byte.class) return Byte.parseByte(toParse);
		else return null;
	}

	/*public Object parsePrimitive(Class keyClass, String toParse) {
		if(keyClass == String.class) return toParse;
		if(keyClass == Float.class) return Float.parseFloat(toParse);
		if(keyClass == Double.class) return Double.parseDouble(toParse);
		if(keyClass == Long.class) return Long.parseLong(toParse);
		if(keyClass == Boolean.class) return Boolean.parseBoolean(toParse);
		if(keyClass == Integer.class) return Integer.parseInt(toParse);
		if(keyClass == Byte.class) return Byte.parseByte(toParse);
		else return null;
	}*/

	/**
	 * returns the appropriate object from the load hashmaps based on the identityHash and keyClass. 
	 * if the object is not found, returns null
	 * @param keyClass
	 * @param identityHash
	 * @return
	 */

	public static Saveable getObjectFromClassMaps(Class keyClass, String identityHash) {
		Saveable result = null; 
	
			if(keyClass == AbstractAxes.class) 				result = axesLoadObjects.get(identityHash);
			else if(keyClass == AbstractArmature.class)		result = armatureLoadObjects.get(identityHash);
			else if(keyClass == AbstractBone.class)			result = boneLoadObjects.get(identityHash);
			else if(keyClass == AbstractKusudama.class)		result = kusudamaLoadObjects.get(identityHash);
			else if(keyClass == AbstractLimitCone.class)	result = limitConeLoadObjects.get(identityHash);
			else if(keyClass == AbstractIKPin.class)		result = IKPinLoadObjects.get(identityHash);

		return result;
	}

	
	public static <T extends Saveable> T getObjectFor(Class objectClass, JSONObject j, String hash) {
		if(j.hasKey(hash)) {
			return (T)getObjectFromClassMaps(objectClass, j.getString(hash));
		} else return null;
	}
	
	public TypeIdentifier getNewTypeIdentifier(Object k, Object v) {
		return EWBIKLoader.getNewTypeIdentifier(k, v);
	}

	

	public static void setTempLoadDirectory(String tempLoadDirectory) {
		tempLoadDirectory = tempLoadDirectory;
	}

	


	public static<T extends Object> void arrayListFromJSONArray(JSONArray jsonArray, ArrayList<T> list, Class c) {	

		for(int i =0 ; i< jsonArray.size(); i++ ) {
			Object item = jsonArray.get(i);

			if(c==SGVec_3d.class) list.add((T) new SGVec_3d(jsonArray.getJSONArray(i))); 
			else if(c == SGVec_3f.class) list.add((T) new SGVec_3f(jsonArray.getJSONArray(i)));
			else if(c == Rot.class) list.add((T) new Rot(jsonArray.getJSONArray(i)));
			else if(c == MRotation.class) list.add((T) new Rot(jsonArray.getJSONArray(i)).rotation);
			else if(c.getName().startsWith("java.lang")) list.add((T)parsePrimitive(c, ""+jsonArray.get(i)));
			else {
				String sitem = Number.class.isAssignableFrom(item.getClass()) ? ""+item : (String) item;
				list.add((T) getObjectFromClassMaps(c, sitem));
			}
		}
	}


}
