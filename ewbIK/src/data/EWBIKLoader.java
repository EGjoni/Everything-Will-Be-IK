package data;

import java.beans.Visibility;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import IK.doubleIK.AbstractArmature;
import IK.doubleIK.AbstractBone;
import IK.doubleIK.AbstractIKPin;
import IK.doubleIK.AbstractKusudama;
import IK.doubleIK.AbstractLimitCone;
import data.JSONArray;
import data.JSONObject;
import data.Saveable;
import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.MRotation;
import sceneGraph.math.doubleV.Rot;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.Vec3f;

public final class EWBIKLoader {

	public static final int SINGLE = 1, DOUBLE = 2; 
	public static int currentMode = DOUBLE;
	
	
	
	public static void setMode(int mode) {
		currentMode = mode;
	}

	public static void importFile(File selection) {
		JSONObject loadFile = StringFuncs.loadJSONObject(selection);
		clearCurrentLoadObjects();
		loadJSON(loadFile);
	}

	

	public static void loadJSON(JSONObject loadFile) {
		if(currentMode == SINGLE) 
			FloatBackedLoader.loadJSON(loadFile); 
		else
			DoubleBackedLoader.loadJSON(loadFile);
	}

	private static void clearCurrentSceneObjects() {
		if(currentMode == SINGLE) 
			FloatBackedLoader.clearCurrentSceneObjects(); 
		else
			DoubleBackedLoader.clearCurrentSceneObjects();

	}

	public static void updateArmatureSegments() {
		if(currentMode == SINGLE) 
			FloatBackedLoader.updateArmatureSegments(); 
		else
			DoubleBackedLoader.updateArmatureSegments();
	}



	public static void clearCurrentLoadObjects() {
		if(currentMode == SINGLE) 
			FloatBackedLoader.clearCurrentLoadObjects(); 
		else
			DoubleBackedLoader.clearCurrentLoadObjects();
	}

	

	public String getCurrentFilePath() {
		if(currentMode == SINGLE) 
			return FloatBackedLoader.getCurrentFilePath(); 
		else
			return DoubleBackedLoader.getCurrentFilePath();
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
		if(currentMode == SINGLE) 
			return FloatBackedLoader.hashMapFromJSON(json, result, ti);
		else
			return DoubleBackedLoader.hashMapFromJSON(json, result, ti);
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
		if(currentMode == SINGLE) 
			return FloatBackedLoader.hashMapFromJSON(json, ti);
		else
			return DoubleBackedLoader.hashMapFromJSON(json, ti);
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
		if(currentMode == SINGLE) 
			return FloatBackedLoader.getObjectFromClassMaps(keyClass, identityHash);
		else
			return DoubleBackedLoader.getObjectFromClassMaps(keyClass, identityHash);
	}

	
	public static <T extends Saveable> T getObjectFor(Class objectClass, JSONObject j, String hash) {
		if(j.hasKey(hash)) {
			return (T)getObjectFromClassMaps(objectClass, j.getString(hash));
		} else return null;
	}
	
	public static TypeIdentifier getNewTypeIdentifier(Object k, Object v) {
		return new TypeIdentifier(k, v);
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
