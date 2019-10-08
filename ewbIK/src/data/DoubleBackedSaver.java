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
import asj.data.FloatList;
import asj.data.IntList;
import asj.data.JSONArray;
import asj.data.JSONObject;
import asj.data.StringFuncs;
import asj.data.StringList;
import math.doubleV.AbstractAxes;
import math.doubleV.MRotation;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.sgRayd;
import math.floatV.SGVec_3f;
import math.floatV.sgRayf;


public class DoubleBackedSaver extends SaveManager{

	 WeakHashMap<Saveable, Boolean> saveables = new WeakHashMap<Saveable, Boolean>();

	public static String currentFilePath;
	public static String tempDir;
	
	public DoubleBackedSaver() {

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

	
	public  JSONObject hashMapToJSON(HashMap<?, ?> hm) {

		Collection<?> keys = hm.keySet();
		Iterator<?> keyI = keys.iterator();
		JSONObject result = new JSONObject();

		while(keyI.hasNext()) {
			Object k = keyI.next();
			try {
				if(k != null) {
					String ks = null;
					//JSONObject kj = null;
					//JSONArray ka = null;

					Class<?> kc = k.getClass();

					if (kc == Integer.class)
						ks = ((Integer)k).toString();
					else if (kc == Long.class)
						ks = ((Long)k).toString();
					else if (kc == Float.class)
						ks = ((Float)k).toString();
					else if(kc == Double.class) 
						ks = ((Double)k).toString();				
					else if (kc == Boolean.class)
						ks = ((Boolean)k).toString();
					else if(Saveable.class.isAssignableFrom(kc))
						ks = ((Saveable)k).getIdentityHash();
					else if(String.class.isAssignableFrom(kc)) 
						ks = (String)k;
					else
						ks = Integer.toString(System.identityHashCode(k));

					if(hm.get(k) != null) {
						Class<?> vc = hm.get(k).getClass();		 
						Object v = hm.get(k);

						if(vc == ArrayList.class)
							result.setJSONArray(ks, arrayListToJSONArray((ArrayList<?>)v));
						else if (vc.isArray()) 
							result.setJSONArray(ks, primitiveArrayToJSONArray(v));
						else if(vc.isAssignableFrom(SGVec_3d.class))
							result.setJSONArray(ks, ((SGVec_3f)v).toJSONArray());
						else if(vc.isAssignableFrom(SGVec_3d.class))
							result.setJSONArray(ks, ((SGVec_3f)v).toJSONArray());
						else if(vc == Rot.class) 
							result.setJSONArray(ks, ((Rot)v).toJsonArray());
						else if(vc == MRotation.class) 
							result.setJSONArray(ks, (new Rot((MRotation)v)).toJsonArray());						
						else if(vc.isAssignableFrom(sgRayd.class)) 
							result.setJSONObject(ks, ((sgRayd)v).toJSONObject());			
						else if(vc == Integer.class)
							result.setInt(ks, (Integer)hm.get(k));
						else if(vc == Long.class)
							result.setLong(ks, (Long)hm.get(k));
						else if(vc == Float.class)
							result.setFloat(ks, (Float)hm.get(k));
						else if(vc == Double.class) 
							result.setDouble(ks, (Double)hm.get(k));					
						else if(vc == Boolean.class)
							result.setBoolean(ks, (Boolean)hm.get(k));
						else if(vc == String.class)
							result.setString(ks, (String)hm.get(k));						
						else if(vc == HashMap.class) {
							result.setJSONObject(ks, hashMapToJSON(((HashMap)v)));					
						}
						else 
							result.setString(ks, ((Saveable)hm.get(k)).getIdentityHash());
					}
				}
			}catch(Exception e) {
				e.printStackTrace(System.out);
				int debug =0; 

			}


		}

		return result;

	}


	public  JSONArray arrayListToJSONArray(ArrayList<?> al) {
		JSONArray result = new JSONArray(); 

		for(int i = 0; i<al.size(); i++) {
			Object o = al.get(i);
			Class c = o.getClass();
			if(o!= null) {

				if(c == Integer.class)
					result.append((Integer)o);
				else if (c == Float.class) 
					result.append((Float)o);
				else if (c == Double.class) 
					result.append((Double)o);
				else if(c == Boolean.class)
					result.append((Boolean)o);
				else if(c.isAssignableFrom(SGVec_3d.class)) 
					result.append(((SGVec_3d)o).toJSONArray());
				else if(c.isAssignableFrom(SGVec_3f.class)) 
					result.append(((SGVec_3f)o).toJSONArray());
				else if(c.isAssignableFrom(sgRayd.class)) 
					result.append(((sgRayd)o).toJSONObject());
				else if(c.isAssignableFrom(sgRayf.class)) 
					result.append(((sgRayf)o).toJSONObject());
				else if(c == Rot.class) 
					result.append(((Rot)o).toJsonArray());
				else if(c == MRotation.class) 
					result.append((new Rot((MRotation)o)).toJsonArray());				
				else if(Saveable.class.isAssignableFrom(c))
					result.append(((Saveable)o).getIdentityHash());
				else
					result.append(System.identityHashCode(o));
			}
		}

		return result;
	}

	public  JSONArray primitiveArrayToJSONArray(Object a) {
		JSONArray result = null; 

		if(a instanceof int[] || a instanceof Integer[]) 
			result = new JSONArray(new IntList((int[])a));
		else if (a instanceof float[] || a instanceof Float[])
			result = new JSONArray(new FloatList((float[]) a)); 
		else if(a instanceof String[]) 
			result = new JSONArray(new StringList((String[]) a));
		else if(a instanceof boolean[]) {
			boolean[] array = (boolean[])a;
			result = new JSONArray();
			for(int i =0; i< array.length; i++) {
				result.append(array[i]);
			}
		}


		return result;
	}

}
