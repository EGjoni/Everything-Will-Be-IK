package data;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.WeakHashMap;

import IK.doubleIK.AbstractArmature;
import IK.doubleIK.AbstractBone;
import IK.doubleIK.AbstractIKPin;
import IK.doubleIK.AbstractKusudama;
import IK.doubleIK.AbstractLimitCone;
import asj.Saveable;
import asj.data.JSONArray;
import asj.data.JSONObject;
import asj.data.StringFuncs;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.floatV.SGVec_3f;
import asj.SaveManager;


public class EWBIKSaver extends SaveManager {


	WeakHashMap<Saveable, Boolean> saveables = new WeakHashMap<Saveable, Boolean>();

	public static String currentFilePath;
	public static String tempDir;

	public void saveArmature(IK.doubleIK.AbstractArmature toSave, String path)  {
		clearSaveState();
		((IK.doubleIK.AbstractArmature)toSave).notifyOfSaveIntent(this);
		saveAs(path);
		notifyCurrentSaveablesOfSaveCompletion();
	}
	
	public void saveArmature(IK.floatIK.AbstractArmature toSave, String path)  {
		clearSaveState();
		((IK.floatIK.AbstractArmature)toSave).notifyOfSaveIntent(this);
		saveAs(path);
		notifyCurrentSaveablesOfSaveCompletion();
	}

	public void addToSaveState(Saveable saveObj) {
		saveables.put(saveObj, true);
	}

	public void removeFromSaveState(Saveable saveObj) {
		saveables.remove(saveObj);
	}

	public void clearSaveState() {
		saveables.clear();
	}


	public void notifyCurrentSaveablesOfSaveCompletion() {
		ArrayList<Saveable> sarr = new ArrayList<>(saveables.keySet());
		for(Saveable s : sarr) {
			s.notifyOfSaveCompletion(this);
		}
		clearSaveState();
	}

	public JSONObject getSaveObject() {	

		JSONArray axesJSON = new JSONArray();	  
		JSONArray armaturesJSON = new JSONArray();
		JSONArray bonesJSON = new JSONArray();    
		JSONArray kusudamaJSON = new JSONArray(); 
		JSONArray limitConeJSON = new JSONArray(); 
		JSONArray IKPinsJSON = new JSONArray();

		Collection<Saveable> sk = saveables.keySet();

		JSONObject saveObject = new JSONObject();

		for(Saveable s: sk) {
			JSONObject jsonObj = s.getSaveJSON(this); 
			if(jsonObj != null) {
				if(math.doubleV.AbstractAxes.class.isAssignableFrom(s.getClass())) 
					axesJSON.append(jsonObj);
				if(IK.doubleIK.AbstractArmature.class.isAssignableFrom(s.getClass())) 
					armaturesJSON.append(jsonObj); 
				if(IK.doubleIK.AbstractBone.class.isAssignableFrom(s.getClass())) 
					bonesJSON.append(jsonObj);
				if(IK.doubleIK.AbstractKusudama.class.isAssignableFrom(s.getClass())) 
					kusudamaJSON.append(jsonObj);
				if(IK.doubleIK.AbstractLimitCone.class.isAssignableFrom(s.getClass()))
					limitConeJSON.append(jsonObj);
				if(IK.doubleIK.AbstractIKPin.class.isAssignableFrom(s.getClass())) 
					IKPinsJSON.append(jsonObj);
				if(math.floatV.AbstractAxes.class.isAssignableFrom(s.getClass())) 
					axesJSON.append(jsonObj);
				if(IK.floatIK.AbstractArmature.class.isAssignableFrom(s.getClass())) 
					armaturesJSON.append(jsonObj); 
				if(IK.floatIK.AbstractBone.class.isAssignableFrom(s.getClass())) 
					bonesJSON.append(jsonObj);
				if(IK.floatIK.AbstractKusudama.class.isAssignableFrom(s.getClass())) 
					kusudamaJSON.append(jsonObj);
				if(IK.floatIK.AbstractLimitCone.class.isAssignableFrom(s.getClass()))
					limitConeJSON.append(jsonObj);
				if(IK.floatIK.AbstractIKPin.class.isAssignableFrom(s.getClass())) 
					IKPinsJSON.append(jsonObj);
			}
		}
		
		saveObject.setJSONArray("axes", axesJSON);
		saveObject.setJSONArray("armatures", armaturesJSON);
		saveObject.setJSONArray("bones", bonesJSON);
		saveObject.setJSONArray("kusudamas", kusudamaJSON);
		saveObject.setJSONArray("limitCones", limitConeJSON);
		saveObject.setJSONArray("IKPins", IKPinsJSON);				
		return saveObject;
	}
	public String getSaveString() {
		String resultString = getSaveObject().toString();
		return resultString;
	}

	private void saveAs(String savePath) {
		save(savePath);
	}

	public void save(String savePath) {
		JSONObject fileContent = getSaveObject();
		StringFuncs.saveJSONObject(fileContent, savePath); 
	}
	
	

}