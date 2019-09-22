/*

Copyright (c) 2016 Eron Gjoni

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
associated documentation files (the "Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOU SGVec_3f WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BU SGVec_3f NO SGVec_3f LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVEN SGVec_3f SHALL THE AUTHORS OR COPYRIGH SGVec_3f HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TOR SGVec_3f OR OTHERWISE, ARISING FROM, OU SGVec_3f OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

 */

package sceneGraph.math.floatV;
//import org.apache.commons.math3.geometry.euclidean.threed.*;


import data.JSONArray;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.numerical.Precision.MathArithmeticException;
import sceneGraph.numerical.Precision.MathIllegalArgumentException;

public class Rot {
	public MRotation rotation; 
	private float[] workingInput = new float[3];
	private float[] workingOutput = new float[3];

	public Rot(){
		this.rotation = new MRotation(
				MRotation.IDENTITY.getQ0(), 
				MRotation.IDENTITY.getQ1(), 
				MRotation.IDENTITY.getQ2(), 
				MRotation.IDENTITY.getQ3(), false);
	};
	
	/**
	 * assume no normalization is needed
	 * 
	 * @param q
	 */
	public Rot(Quaternionf q) {
		this.rotation = new MRotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), false);
	}

	
	/**
	 * 
	 * @param q
	 * @param normalize
	 */
	public Rot(Quaternionf q, boolean normalize) {
		this.rotation = new MRotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), normalize);
	}
	

	
	public Rot(MRotation r) {
		this.rotation = new MRotation(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3());
	}

	public Rot( SGVec_3f v1, SGVec_3f v2, SGVec_3f u1, SGVec_3f u2) {
		try {
			rotation = new MRotation(
					new SGVec_3f(v1), 
					new SGVec_3f(v2), 
					new SGVec_3f(u1), 
					new SGVec_3f(u2));
		} catch(Exception e) {
			rotation = new MRotation(v1, 0f);
		}
	}

	public Rot( SGVec_3f  axis, float angle) {		
		try {
			rotation = new MRotation(axis, angle); 
		} catch(Exception e) { 
			rotation = new MRotation(RotationOrder.X, 0f);
		}
	}

	public Rot(float w, float x, float y, float z, boolean needsNormalization) {
		this.rotation = new MRotation(w, x, y, z, needsNormalization);  
	}  
	
	public Rot( SGVec_3f begin, SGVec_3f end) {
		try{ 
			rotation = new MRotation(begin, end);
		} catch(Exception e) { 
			rotation = new MRotation(RotationOrder.X, 0f);
		}
	}

	
	public Rot copy() {
		return new Rot(new MRotation(rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3(), false));
	}

	/**
	 * sets the value of this rotation to r
	 * @param r a rotation to make this rotation equivalent to
	 */
	public void set(MRotation r) {
		if(r != null)
			this.rotation.set(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3(), false);
		else 
			this.set(MRotation.IDENTITY);
	}
	

	
	/**
	 * sets the value of this rotation to r
	 * @param r a rotation to make this rotation equivalent to
	 */
	public void set(Rot r) {
		if(r != null)
			this.set(r.rotation);
		else 
			this.set(MRotation.IDENTITY);
	}
	
	/**
	 * sets the value of this rotation to what is represented 
	 * by the input axis angle parameters
	 * @param axis
	 * @param angle
	 */
	public void set(SGVec_3f axis, float angle) {
		try {
			this.rotation.set(axis, angle);
		} catch (MathIllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * sets the value of this rotation to what is represented 
	 * by the input startVector targetVector parameters
	 * @param axis
	 * @param angle
	 */
	public void set(SGVec_3f startVec, SGVec_3f targetVec) {
		try {
			this.rotation.set(startVec, targetVec);
		} catch (MathArithmeticException e) {
			e.printStackTrace();
		}
	}
	

	public void applyTo( SGVec_3f v, SGVec_3f output) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyTo(workingInput, workingOutput);
		output.set(workingOutput);
	}


	public void applyInverseTo( SGVec_3f v, SGVec_3f output) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyInverseTo(workingInput, workingOutput);
		output.set(workingOutput);
	}


	/**
	 * applies the rotation to a copy of the input vector
	 * @param v
	 * @return
	 */
	
	public SGVec_3f applyToCopy( SGVec_3f v) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyTo(workingInput, workingOutput);
		 SGVec_3f copy =  v.copy();		
		return copy.set(workingOutput[0], workingOutput[1], workingOutput[2]);
	}


	public SGVec_3f  applyInverseToCopy( SGVec_3f v) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyInverseTo(workingInput, workingOutput);
		 SGVec_3f copy =  v.copy();		
		return copy.set(workingOutput[0], workingOutput[1], workingOutput[2]);
	}


	/**
	 * Given a rotation r, this function returns a rotation L such that this.applyTo(L) = r. 
	 * @param r
	 * @return
	 *
	public Rot getLocalOfRotation(Rot r) {
		Rotation composedRot = this.rotation.composeInverse(r.rotation, RotationConvention.VECTOR_OPERATOR);


		return new Rot(composedRot);
	}*/

	private Rot getNormalized(MRotation r) {
		return new Rot(new MRotation(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3(), true));
	}

	public sgRayf applyToCopy(sgRayf rIn) {
		workingInput[0] = rIn.p2().x - rIn.p1().x;
		workingInput[1] = rIn.p2().y - rIn.p1().y; 
		workingInput[2] = rIn.p2().z - rIn.p1().z;
		
		this.rotation.applyTo(workingInput, workingOutput);
		sgRayf result = rIn.copy();
		result.heading(workingOutput);
		return result;
	}

	public sgRayf applyInverseTo(sgRayf rIn) {
		workingInput[0] = rIn.p2().x - rIn.p1().x;
		workingInput[1] = rIn.p2().y - rIn.p1().y; 
		workingInput[2] = rIn.p2().z - rIn.p1().z;		

		this.rotation.applyInverseTo(workingInput, workingOutput);
		sgRayf result = rIn.copy();
		result.p2().add(workingOutput);
		return result;
	}


	public void applyTo(Rot rot, Rot storeIn) {                                                                                                  
		MRotation r = rot.rotation;                                                                                                 
		MRotation tr = this.rotation;                                                                                               
		storeIn.rotation.set(                                                                                            
				r.getQ0() * tr.getQ0() -(r.getQ1() * tr.getQ1() +  r.getQ2() * tr.getQ2() + r.getQ3() * tr.getQ3()),   
				r.getQ1() * tr.getQ0() + r.getQ0() * tr.getQ1() + (r.getQ2() * tr.getQ3() - r.getQ3() * tr.getQ2()),   
				r.getQ2() * tr.getQ0() + r.getQ0() * tr.getQ2() + (r.getQ3() * tr.getQ1() - r.getQ1() * tr.getQ3()),   
				r.getQ3() * tr.getQ0() + r.getQ0() * tr.getQ3() + (r.getQ1() * tr.getQ2() - r.getQ2() * tr.getQ1()),   
				true);                                                                                                                                                                                          
	}                                                             

	public void applyInverseTo(Rot rot, Rot storeIn) {                                                                                           
		MRotation r = rot.rotation;                                                                                                 
		MRotation tr = this.rotation;                                                                                               
		storeIn.rotation.set(                                                                                          
				-r.getQ0() * tr.getQ0() -(r.getQ1() * tr.getQ1() +  r.getQ2() * tr.getQ2() + r.getQ3() * tr.getQ3()),    
				-r.getQ1() * tr.getQ0() + r.getQ0() * tr.getQ1() + (r.getQ2() * tr.getQ3() - r.getQ3() * tr.getQ2()),    
				-r.getQ2() * tr.getQ0() + r.getQ0() * tr.getQ2() + (r.getQ3() * tr.getQ1() - r.getQ1() * tr.getQ3()),    
				-r.getQ3() * tr.getQ0() + r.getQ0() * tr.getQ3() + (r.getQ1() * tr.getQ2() - r.getQ2() * tr.getQ1()),    
				true);                                                                                                             
	}      
	
	
	public Rot applyTo(Rot rot) {                                                                                                  
		MRotation r = rot.rotation;                                                                                                 
		MRotation tr = this.rotation;                                                                                               
		MRotation result = new MRotation(                                                                                            
				r.getQ0() * tr.getQ0() -(r.getQ1() * tr.getQ1() +  r.getQ2() * tr.getQ2() + r.getQ3() * tr.getQ3()),   
				r.getQ1() * tr.getQ0() + r.getQ0() * tr.getQ1() + (r.getQ2() * tr.getQ3() - r.getQ3() * tr.getQ2()),   
				r.getQ2() * tr.getQ0() + r.getQ0() * tr.getQ2() + (r.getQ3() * tr.getQ1() - r.getQ1() * tr.getQ3()),   
				r.getQ3() * tr.getQ0() + r.getQ0() * tr.getQ3() + (r.getQ1() * tr.getQ2() - r.getQ2() * tr.getQ1()),   
				true);                                                                                      
		return new Rot(result);                                                                                                    
	}                                                             

	public Rot applyInverseTo(Rot rot) {                                                                                           
		MRotation r = rot.rotation;                                                                                                 
		MRotation tr = this.rotation;                                                                                               
		MRotation result = new MRotation(                                                                                            
				-r.getQ0() * tr.getQ0() -(r.getQ1() * tr.getQ1() +  r.getQ2() * tr.getQ2() + r.getQ3() * tr.getQ3()),    
				-r.getQ1() * tr.getQ0() + r.getQ0() * tr.getQ1() + (r.getQ2() * tr.getQ3() - r.getQ3() * tr.getQ2()),    
				-r.getQ2() * tr.getQ0() + r.getQ0() * tr.getQ2() + (r.getQ3() * tr.getQ1() - r.getQ1() * tr.getQ3()),    
				-r.getQ3() * tr.getQ0() + r.getQ0() * tr.getQ3() + (r.getQ1() * tr.getQ2() - r.getQ2() * tr.getQ1()),    
				true);                                                                                                   
		return new Rot(result);                                                                                                    
	}   
	
	public float getAngle() {
		return (float) rotation.getAngle();  
	}

	public SGVec_3f getAxis() {
		return rotation.getAxis();
	}
	
	public void getAxis( SGVec_3f output) {
		output.set(rotation.getAxis());
	}


	public Rot revert() {
		return new Rot(this.rotation.revert());
	}


	/*
	 * interpolate between two rotations (SLERP)
	 * 
	 */
	public Rot(float amount, Rot v1, Rot v2) {
		Quaternionf q = slerp(amount, 
				new Quaternionf(v1.rotation.getQ0(), v1.rotation.getQ1(),v1.rotation.getQ2(), v1.rotation.getQ3()),
				new Quaternionf(v2.rotation.getQ0(), v2.rotation.getQ1(),v2.rotation.getQ2(), v2.rotation.getQ3()));

		rotation = new MRotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), false);
	}

	/** Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis. The swing and twist rotation can be used to reconstruct the original
	 * quaternion: this = swing * twist
	 * 
	 * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
	 * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
	 * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
	 * @return an Array of Rot objects. With the first element representing the swing, and the second representing the twist
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a> */
	public Rot[] getSwingTwist ( SGVec_3f axis) {
		Quaternionf swing = new Quaternionf(); 
		Quaternionf twist = new Quaternionf(); 
		Quaternionf thisRot = new Quaternionf(rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3());
		thisRot.getSwingTwist(axis, swing, twist);
		Rot[] result = new Rot[2];
		result[0] = new Rot(swing);
		result[1] = new Rot(twist);
		return result;
	}


	public static Quaternionf slerp(float amount, Quaternionf value1, Quaternionf value2)
	{
		if(Float.isNaN(amount)) {
			return new Quaternionf(value1.getQ0(), value1.getQ1(), value1.getQ2(), value1.getQ3());
		}
		
		if (amount < 0.0)
			return value1;
		else if (amount > 1.0)
			return value2;

		float dot = value1.dot(value2);
		float x2, y2, z2, w2;
		if (dot < 0.0)
		{
			dot = 0.0f - dot;
			x2 = 0.0f - value2.getQ1();
			y2 = 0.0f - value2.getQ2();
			z2 = 0.0f - value2.getQ3();
			w2 = 0.0f - value2.getQ0();
		}
		else
		{
			x2 = value2.getQ1();
			y2 = value2.getQ2();
			z2 = value2.getQ3();
			w2 = value2.getQ0();
		}

		float t1, t2;

		final float EPSILON = 0.0001f;
		if ((1.0 - dot) > EPSILON) // standard case (slerp)
		{
			float angle = MathUtils.acos(dot);
			float sinAngle = MathUtils.sin(angle);
			t1 = MathUtils.sin((1.0f - amount) * angle) / sinAngle;
			t2 = MathUtils.sin(amount * angle) / sinAngle;
		}
		else // just lerp
		{
			t1 = 1.0f - amount;
			t2 = amount;
		}

		return new Quaternionf(
				(value1.getQ0() * t1) + (w2 * t2),
				(value1.getQ1() * t1) + (x2 * t2),
				(value1.getQ2() * t1) + (y2 * t2),
				(value1.getQ3() * t1) + (z2 * t2));
	}
	
	public static MRotation slerp(float amount, MRotation value1, MRotation value2)
	{
		
		if(Float.isNaN(amount)) {
			return new MRotation(value1.getQ0(), value1.getQ1(), value1.getQ2(), value1.getQ3());
		}
		if (amount < 0.0)
			return value1;
		else if (amount > 1.0)
			return value2;

		float dot = value1.dotProduct(value2);
		float x2, y2, z2, w2;
		if (dot < 0.0)
		{
			dot = 0.0f - dot;
			x2 = 0.0f - value2.getQ1();
			y2 = 0.0f - value2.getQ2();
			z2 = 0.0f - value2.getQ3();
			w2 = 0.0f - value2.getQ0();
		}
		else
		{
			x2 = value2.getQ1();
			y2 = value2.getQ2();
			z2 = value2.getQ3();
			w2 = value2.getQ0();
		}

		float t1, t2;

		final float EPSILON = 0.0001f;
		if ((1.0 - dot) > EPSILON) // standard case (slerp)
		{
			float angle = MathUtils.acos(dot);
			float sinAngle = MathUtils.sin(angle);
			t1 = MathUtils.sin((1.0f - amount) * angle) / sinAngle;
			t2 = MathUtils.sin(amount * angle) / sinAngle;
		}
		else // just lerp
		{
			t1 = 1.0f - amount;
			t2 = amount;
		}

		return new MRotation(
				(value1.getQ0() * t1) + (w2 * t2),
				(value1.getQ1() * t1) + (x2 * t2),
				(value1.getQ2() * t1) + (y2 * t2),
				(value1.getQ3() * t1) + (z2 * t2));
	}

	
	public static Rot nlerp(Rot[] rotations, float[] weights) {
	
		if(weights == null) {		
			return nlerp(rotations);
		} else {
			float q0 = 0f; 
			float q1 = 0f;
			float q2 = 0f;
			float q3 = 0f; 
			float total = 0f; 
			
			for(int i = 0; i<rotations.length; i++) {
				MRotation r = rotations[i].rotation;
				float weight = weights[i];
				q0 += r.getQ0() * weight;
				q1 += r.getQ1() * weight;
				q2 += r.getQ2() * weight;
				q3 += r.getQ3() * weight;
				total += weight;
			}			
			
			q0 /= total;
			q1 /= total;
			q2 /= total;
			q3 /= total;
			
			return new Rot(q0, q1, q2, q3, true);
		}
	}
	
	public static Rot nlerp(Rot[] rotations) {
		float q0 = 0f; 
		float q1 = 0f;
		float q2 = 0f;
		float q3 = 0f; 
		float total = rotations.length; 
		
		for(int i = 0; i<rotations.length; i++) {
			MRotation r = rotations[i].rotation;			
			q0 += r.getQ0();
			q1 += r.getQ1();
			q2 += r.getQ2();
			q3 += r.getQ3();
		}	
		q0 /= total;
		q1 /= total;
		q2 /= total;
		q3 /= total;
		
		return new Rot(q0, q1, q2, q3, true);
	}
	
	/**
	 * finds the instantaneous (optionally weighted) average of the given rotations 
	 * in an order-agnostic manner. Conceptually, this is equivalent to breaking down 
	 * each rotation into an infinitesimal sequence of rotations, and then applying the rotations 
	 * in alternation.
	 * @param rots
	 * @param weights if this parameter receives null, equal weights are assumed for all rotations. Otherwise, 
	 * every element in this array is treated as a weight on the corresponding element of the rots array.  
	 * @return the weighted average Rotation. If the total weights are 0, then returns null. 
	 */
	public static Rot instantaneousAvg(Rot[] rots, float[] weights) {
		SGVec_3f accumulatedAxisAngle = new SGVec_3f(); 
		float totalWeight = rots.length; 
		if(weights != null) {
			totalWeight = 0f; 
			for(int i=0; i<weights.length; i++) {
				totalWeight += weights[i];
			}
		}
		
		if(totalWeight == 0) {
			return null;
		}
		
		for(int i=0; i<rots.length; i++) {
			SGVec_3f axis = rots[i].getAxis(); 
			float angle = rots[i].getAngle(); 
			angle /= totalWeight;
			if(weights != null) {				
				angle *= weights[i]; 
			}
			axis.mult(angle);
			accumulatedAxisAngle.add(axis);
		}
		float extractAngle = accumulatedAxisAngle.mag(); 
		accumulatedAxisAngle.div(extractAngle); 
		return new Rot(accumulatedAxisAngle, extractAngle);
	}

	public String toString() {
		return "\n axis: "+ this.getAxis().toSGVec3f() +", \n angle: "+((float)Math.toDegrees(this.getAngle()));
	}
	
	/**
	 * loads a rotation from a JSON array of quaternion values. 
	 * 
	 * where 
	 * jarray[0] = q0 = w;
	 * jarray[1] = q1 = x; 
	 * jarray[2] = q2 = y; 
	 * jarray[3] = q3 = z;
	 * @param jarray
	 */
	public Rot(JSONArray jarray) {
		rotation = new MRotation(jarray.getFloat(0), jarray.getFloat(1), jarray.getFloat(2), jarray.getFloat(3), true);
	}
	
	public JSONArray toJsonArray() {
		JSONArray result = new JSONArray(); 
		result.append(this.rotation.getQ0());
		result.append(this.rotation.getQ1());
		result.append(this.rotation.getQ2());
		result.append(this.rotation.getQ3());
		return result;
	}


}
