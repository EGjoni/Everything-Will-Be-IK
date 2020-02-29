package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.WeakHashMap;

import IK.floatIK.AbstractArmature;
import IK.floatIK.AbstractBone;
import IK.floatIK.AbstractIKPin;
import IK.floatIK.AbstractKusudama;
import IK.floatIK.AbstractLimitCone;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONArray;
import asj.data.JSONObject;
import asj.data.StringFuncs;
import math.floatV.AbstractAxes;


public class FloatBackedSaver extends SaveManager{

	WeakHashMap<Saveable, Boolean> saveables = new WeakHashMap<Saveable, Boolean>();

	public static String currentFilePath;
	public static String tempDir;
	
	public FloatBackedSaver() {

	}

	public void saveArmature(AbstractArmature toSave, String absolutePath)  {
		try {
			File tempFile = File.createTempFile("GiftedApprentice", ".tmp");
			System.out.println(tempFile.getParent());
			System.out.println(tempFile.getParent() + File.separator);
			System.out.println(tempFile.getParent() + File.separator + "GiftedApprentice"+System.currentTimeMillis());
			tempDir = tempFile.getParent()+ File.separator + "GiftedApprentice"+System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		//p.println("tempDir = " + tempDir);
	}

	public  void addToSaveState(Saveable saveObj) {
		saveables.put(saveObj, true);
	}



	public  void removeFromSaveState(Saveable saveObj) {
		saveables.remove(saveObj);
	}

	public  void clearSaveState() {
		saveables.clear();
	}


	public  void notifyCurrentSaveablesOfSaveCompletion() {
		ArrayList<Saveable> sarr = new ArrayList<>(saveables.keySet());
		for(Saveable s : sarr) {
			s.notifyOfSaveCompletion(this);
		}
	}

	public  JSONObject getSaveObject() {	

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
				if(AbstractAxes.class.isAssignableFrom(s.getClass())) 
					axesJSON.append(jsonObj);
				if(AbstractArmature.class.isAssignableFrom(s.getClass())) 
					armaturesJSON.append(jsonObj); 
				if(AbstractBone.class.isAssignableFrom(s.getClass())) 
					bonesJSON.append(jsonObj);
				if(AbstractKusudama.class.isAssignableFrom(s.getClass())) 
					kusudamaJSON.append(jsonObj);
				if(AbstractLimitCone.class.isAssignableFrom(s.getClass()))
					limitConeJSON.append(jsonObj);
				if(AbstractIKPin.class.isAssignableFrom(s.getClass())) 
					IKPinsJSON.append(jsonObj);
			}
		}
		
		saveObject.setJSONArray("axes", axesJSON);
		saveObject.setJSONArray("armatures", armaturesJSON);
		saveObject.setJSONArray("bones", bonesJSON);
		saveObject.setJSONArray("kusudamas", kusudamaJSON);
		saveObject.setJSONArray("limitCones", limitConeJSON);
		saveObject.setJSONArray("IKPins", IKPinsJSON);		

		notifyCurrentSaveablesOfSaveCompletion();
		return saveObject;
	}
	public String getSaveSring() {
		String resultString = getSaveObject().toString();
		return resultString;
	}

	public  void saveAs(String savePath) {
		//File saveFile = p.saveFile("Save", currentFilePath, ".ga");//p.selectOutput("Save", "saveFileSelected");
		currentFilePath = savePath;
		//p.saveFileSelected(new File(currentFilePath));	
		save();
	}

	public  void save() {

		JSONObject fileContent = getSaveObject();
		//p.println(fileContent.toString());
		StringFuncs.saveJSONObject(fileContent, tempDir+File.separator+"structure"); 
		try {
			File cFile = new File(currentFilePath);
			if(cFile != null) {
				cFile.getParentFile().mkdirs();
			}
		} catch (Exception e) {
			System.out.println("failed to save");
		}

		//p.saveJSONObject(fileContent, location);
	}
}
