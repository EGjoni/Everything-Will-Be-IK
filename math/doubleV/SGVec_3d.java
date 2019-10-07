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

package math.doubleV;

import java.io.Serializable;

import asj.CanLoad;
import asj.data.JSONArray;
import asj.data.JSONObject;
import math.floatV.SGVec_3f;


//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com */
public class SGVec_3d extends Vec3d<SGVec_3d> implements CanLoad {
	

	public SGVec_3d(SGVec_3d sgVec_3d) {
		super(sgVec_3d);
	}	
	
	public <V extends Vec3d<?>> SGVec_3d(V v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public SGVec_3d() {
		super();
	}

	public SGVec_3d(double i, double j, double k) {
		super(i, j,k);
	}


	@Override
	public CanLoad populateSelfFromJSON(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getDouble(0);
		this.y = components.getDouble(1);
		this.z = components.getDouble(2);
		return this;
	}

	public SGVec_3d(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getDouble(0);
		this.y = components.getDouble(1);
		this.z = components.getDouble(2);
	}	

	public SGVec_3d (JSONArray j) {
		this.x = j.getDouble(0);
		this.y = j.getDouble(1);
		this.z = j.getDouble(2);
	}	

	@Override
	public SGVec_3d copy() {
		return new SGVec_3d(this);
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
