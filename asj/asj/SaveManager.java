package asj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.WeakHashMap;

import asj.data.FloatList;
import asj.data.IntList;
import asj.data.JSONArray;
import asj.data.JSONObject;
import asj.data.StringList;



public abstract class SaveManager {
	
		protected HashMap<Class, JSONArray>  classes = new HashMap<>();
		WeakHashMap<Saveable, Boolean> saveables = new WeakHashMap<Saveable, Boolean>();
	
		public void registerSaveableClass(Class c) throws ClassNotSaveableException {
			if(!Saveable.class.isAssignableFrom(c)) {
				throw new ClassNotSaveableException(c);
			} else if(classes.get(c) == null) {
				classes.put(c, new JSONArray());
			}
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
		
		public JSONObject hashMapToJSON(HashMap<?, ?> hm) {
			Collection<?> keys = hm.keySet();
			Iterator<?> keyI = keys.iterator();
			JSONObject result = new JSONObject();

			while(keyI.hasNext()) {
				Object k = keyI.next();
				try {
					if(k != null) {
						String ks = null;
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
							else if(vc.isAssignableFrom(CanLoad.class)) {
								result.setJSONObject(ks, ((CanLoad)v).toJSONObject());
							}
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
				} catch(Exception e) {
					e.printStackTrace(System.out);
				}
			}

			return result;		
		}
		
		public JSONArray arrayListToJSONArray(ArrayList<?> al) { 
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
					else if(c.isAssignableFrom(CanLoad.class)) {
						result.append(((CanLoad)o).toJSONObject());
					}
					else if(Saveable.class.isAssignableFrom(c))
						result.append(((Saveable)o).getIdentityHash());
					else
						result.append(System.identityHashCode(o));
				}
			}
			return result;		
		}
		
		public JSONArray primitiveArrayToJSONArray(Object a) {
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
		
		
		public class ClassNotSaveableException extends Exception {
			public ClassNotSaveableException(Class c) {
				super(c.toString() + " does not implement the Saveable interface and therefore cannot be registered with Saveable.");
			}
		}		
}
