package data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import IK.doubleIK.AbstractArmature;
import IK.doubleIK.AbstractBone;
import IK.doubleIK.AbstractIKPin;
import IK.doubleIK.AbstractLimitCone;
import IK.doubleIK.Constraint;
import asj.LoadManager;
import asj.Saveable;
import asj.TypeIdentifier;
import asj.data.JSONArray;
import asj.data.JSONObject;
import asj.data.StringFuncs;
import math.doubleV.AbstractAxes;
import math.doubleV.MRotation;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.floatV.SGVec_3f;

public class DoubleBackedLoader extends LoadManager {
		
	public File currentFilePath; 

	public HashMap<String, JSONObject> 	    	axesJSONObjects 		= new HashMap<>(); 
	public HashMap<String, AbstractAxes>			axesLoadObjects 		= new HashMap<>();  

	public HashMap<String, JSONObject> 	    	armatureJSONObjects 	= new HashMap<>();
	public HashMap<String, AbstractArmature>		armatureLoadObjects 	= new HashMap<>();  

	public HashMap<String, JSONObject> 	    	boneJSONObjects 		= new HashMap<>(); 
	public HashMap<String, AbstractBone> 		boneLoadObjects 		= new HashMap<>(); 

	public HashMap<String, Constraint>		kusudamaLoadObjects 	= new HashMap<>();
	public HashMap<String, JSONObject>	    	kusudamaJSONObjects 	= new HashMap<>(); 
	
	public HashMap<String, AbstractLimitCone>	limitConeLoadObjects 	= new HashMap<>(); 
	public HashMap<String, JSONObject>	    	limitConeJSONObjects 	= new HashMap<>();

	public HashMap<String, AbstractIKPin>		IKPinLoadObjects 		= new HashMap<>();
	public HashMap<String, JSONObject>	    	IKPinJSONObjects 		= new HashMap<>(); 


	

	public boolean fileCorruptionDetected = false; 

	private String tempLoadDirectory;	
	
	private static boolean Load = false;
	
	public DoubleBackedLoader() {
		
	}

	
	/**
	 * @param selection file to import
	 * @param AxesClass the class object you've used to extend the AbstractAxes class. If null, AbstractAxes will be used. 
	 * @param BoneClass the class object you've used to extend the AbstractBone class. If null, AbstractBone will be used. 
	 * @param ArmatureClass the class object you've used to extend the AbstractArmature class. If null, AbstractArmature will be used. 
	 * @param KusudamaClass the class object you've used to extend the AbstractKusudama class. If null, AbstractKusudama will be used. 
	 * @param LimitConeClass the class object you've used to extend the AbstractLimitCone class. If null, AbstractLimitCone will be used. 
	 * @param IKPinClass the class object you've used to extend the AbstractIKPin class. If null, AbstractIKPin will be used. 
	 * 
	 * @return a list of all instantiated armatures specified by the input file. 
	 */
	public Collection<? extends AbstractArmature> importFile(File selection,
			Class<? extends AbstractAxes> AxesClass, 
			Class<? extends AbstractBone> BoneClass, 
			Class<? extends AbstractArmature> ArmatureClass, 
			Class<? extends Constraint> KusudamaClass, 
			Class<? extends AbstractLimitCone>  LimitConeClass, 
			Class<? extends AbstractIKPin> IKPinClass,
			LoadManager loader) {
		JSONObject loadFile = StringFuncs.loadJSONObject(selection);
		clearCurrentLoadObjects();
		return loadJSON(loadFile,
				AxesClass, 
				BoneClass, 
				ArmatureClass, 
				KusudamaClass, 
				LimitConeClass, 
				IKPinClass);
	}

	
/**
 * 
 * @param loadFile
 * @param AxesClass
 * @param BoneClass
 * @param ArmatureClass
 * @param KusudamaClass
 * @param LimitConeClass
 * @param IKPinClass
 * 
 * @return a list of all instantiated armatures specified by the input file. 
 */
	public Collection<? extends AbstractArmature> loadJSON(JSONObject loadFile, 
			Class<? extends AbstractAxes> AxesClass, 
			Class<? extends AbstractBone> BoneClass, 
			Class<? extends AbstractArmature> ArmatureClass, 
			Class<? extends Constraint> KusudamaClass, 
			Class<? extends AbstractLimitCone>  LimitConeClass, 
			Class<? extends AbstractIKPin> IKPinClass) {
		clearCurrentLoadObjects();
		AxesClass = AxesClass == null? AbstractAxes.class : AxesClass;
		BoneClass = BoneClass == null? AbstractBone.class : BoneClass;
		ArmatureClass = ArmatureClass == null? AbstractArmature.class : ArmatureClass;
		KusudamaClass = KusudamaClass == null? Constraint.class : KusudamaClass;
		LimitConeClass = LimitConeClass == null? AbstractLimitCone.class : LimitConeClass;
		IKPinClass = IKPinClass == null? AbstractIKPin.class : IKPinClass;
		
		createEmptyLoadMaps(axesJSONObjects, axesLoadObjects, loadFile.getJSONArray("axes"), AxesClass);		
		createEmptyLoadMaps(boneJSONObjects, boneLoadObjects, loadFile.getJSONArray("bones"),  BoneClass);		
		createEmptyLoadMaps(armatureJSONObjects, armatureLoadObjects, loadFile.getJSONArray("armatures"), ArmatureClass);
		createEmptyLoadMaps(limitConeJSONObjects, limitConeLoadObjects, loadFile.getJSONArray("limitCones"),  LimitConeClass);
		createEmptyLoadMaps(kusudamaJSONObjects, kusudamaLoadObjects, loadFile.getJSONArray("kusudamas"), KusudamaClass);		
		createEmptyLoadMaps(IKPinJSONObjects, IKPinLoadObjects, loadFile.getJSONArray("IKPins"),  IKPinClass);

		loadGenerally(axesJSONObjects, axesLoadObjects);
		loadGenerally(IKPinJSONObjects, IKPinLoadObjects);
		loadGenerally(limitConeJSONObjects, limitConeLoadObjects);
		loadGenerally(kusudamaJSONObjects, kusudamaLoadObjects);
		loadGenerally(boneJSONObjects, boneLoadObjects);
		loadGenerally(armatureJSONObjects, armatureLoadObjects);


		
		for(Saveable s: allLoadedObjects) 
			s.notifyOfLoadCompletion();
		
		updateArmatureSegments();		
		System.gc();
		return armatureLoadObjects.values();
	}

	static void clearCurrentSceneObjects() {
		// TODO Auto-generated method stub

	}

	public  void updateArmatureSegments() {
		Collection<AbstractArmature> armatures = armatureLoadObjects.values();		
		for(AbstractArmature a : armatures) {
			a.refreshArmaturePins();			
		}
	}



	public void clearCurrentLoadObjects() {

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
		
		IKPinJSONObjects.clear();
		IKPinLoadObjects.clear();

		allLoadedObjects.clear();

	}



	public String getCurrentFilePath() {
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
	public <T extends Object, V extends Object> HashMap<T, V> hashMapFromJSON(JSONObject json, HashMap<T,V> result, TypeIdentifier ti) {
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
	public <T extends Object, V extends Object> HashMap<T, V> hashMapFromJSON(JSONObject json, TypeIdentifier ti) {
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
	
	public Saveable getObjectFromClassMaps(Class keyClass, String identityHash) {
		Saveable result = null; 
	
		if(AbstractAxes.class.isAssignableFrom(keyClass)) 				result = axesLoadObjects.get(identityHash);
		else if(AbstractArmature.class.isAssignableFrom(keyClass))		result = armatureLoadObjects.get(identityHash);
		else if(AbstractBone.class.isAssignableFrom(keyClass))			result = boneLoadObjects.get(identityHash);
		else if(Constraint.class.isAssignableFrom(keyClass))		result = kusudamaLoadObjects.get(identityHash);
		else if(AbstractLimitCone.class.isAssignableFrom(keyClass))	result = limitConeLoadObjects.get(identityHash);
		else if(AbstractIKPin.class.isAssignableFrom(keyClass))		result = IKPinLoadObjects.get(identityHash);

		return result;
	}

	
	public <T extends Saveable> T getObjectFor(Class objectClass, JSONObject j, String hash) {
		if(j.hasKey(hash)) {
			return (T)getObjectFromClassMaps(objectClass, j.getString(hash));
		} else return null;
	}
	
	public static void setTempLoadDirectory(String tempLoadDirectory) {
		tempLoadDirectory = tempLoadDirectory;
	}

	


	public <T extends Object> void arrayListFromJSONArray(JSONArray jsonArray, ArrayList<T> list, Class c) {	

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
