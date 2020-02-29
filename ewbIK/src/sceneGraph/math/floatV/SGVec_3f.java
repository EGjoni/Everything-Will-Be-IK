/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package math.floatV;

import java.io.Serializable;

import asj.CanLoad;
import asj.data.JSONArray;
import asj.data.JSONObject;
import math.floatV.SGVec_3f;


//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com */
public class SGVec_3f extends Vec3f<SGVec_3f> implements CanLoad {
	

	public SGVec_3f(SGVec_3f sgVec_3f) {
		super(sgVec_3f);
	}	
	
	public <V extends Vec3f<?>> SGVec_3f(V v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public SGVec_3f() {
		super();
	}

	public SGVec_3f(float i, float j, float k) {
		super(i, j,k);
	}


	@Override
	public CanLoad populateSelfFromJSON(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getFloat(0);
		this.y = components.getFloat(1);
		this.z = components.getFloat(2);
		return this;
	}

	public SGVec_3f(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getFloat(0);
		this.y = components.getFloat(1);
		this.z = components.getFloat(2);
	}	

	public SGVec_3f (JSONArray j) {
		this.x = j.getFloat(0);
		this.y = j.getFloat(1);
		this.z = j.getFloat(2);
	}	

	@Override
	public SGVec_3f copy() {
		return new SGVec_3f(this);
	}


	@Override
	public SGVec_3f toVec3f() {
		return new SGVec_3f((float)x,(float)y,(float)z);
	}



	public JSONArray toJSONArray() {
		JSONArray vec = new JSONArray();
		vec.append(this.x); vec.append(this.y); vec.append(this.z);
		return vec;
	}
	 
	@Override
	public JSONObject toJSONObject() {
		JSONObject j = new JSONObject(); 
		JSONArray components = new JSONArray(); 
		components.append(this.x);
		components.append(this.y);
		components.append(this.z);
		j.setJSONArray("vec", components); 
		return j;
	}




}
