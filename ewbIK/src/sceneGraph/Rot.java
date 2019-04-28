/*

Copyright (c) 2016 Eron Gjoni

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
associated documentation files (the "Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOU SGVec_3d WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BU SGVec_3d NO SGVec_3d LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVEN SGVec_3d SHALL THE AUTHORS OR COPYRIGH SGVec_3d HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TOR SGVec_3d OR OTHERWISE, ARISING FROM, OU SGVec_3d OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

 */

package sceneGraph;
//import org.apache.commons.math3.geometry.euclidean.threed.*;

import org.apache.commons.math3.complex.*;

import data.JSONArray;
import sceneGraph.math.SGVec_3d;
import sceneGraph.math.RotationOrder;
import sceneGraph.math.SGVec_3d;
import sceneGraph.math.floatV.SGVec_3f;

public class Rot {
	public MRotation rotation; 
	private double[] workingInput = new double[3];
	private double[] workingOutput = new double[3];

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
	public Rot(Quaternion q) {
		this.rotation = new MRotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), false);
	}

	
	/**
	 * 
	 * @param q
	 * @param normalize
	 */
	public Rot(Quaternion q, boolean normalize) {
		this.rotation = new MRotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), normalize);
	}
	

	
	public Rot(MRotation r) {
		this.rotation = new MRotation(r.getQ0(), r.getQ1(), r.getQ2(), r.getQ3());
	}

	public Rot( SGVec_3d v1, SGVec_3d v2, SGVec_3d u1, SGVec_3d u2) {
		try {
			rotation = new MRotation(
					new SGVec_3d(v1), 
					new SGVec_3d(v2), 
					new SGVec_3d(u1), 
					new SGVec_3d(u2));
		} catch(Exception e) {
			rotation = new MRotation(v1, 0f);
		}
	}

	public Rot( SGVec_3d  axis, double angle) {		
		try {
			rotation = new MRotation(axis, angle); 
		} catch(Exception e) { 
			rotation = new MRotation(RotationOrder.X, 0d);
		}
	}

	public Rot(double w, double x, double y, double z, boolean needsNormalization) {
		this.rotation = new MRotation(w, x, y, z, needsNormalization);  
	}  
	
	public Rot( SGVec_3d begin, SGVec_3d end) {
		try{ 
			rotation = new MRotation(new SGVec_3d(begin), new SGVec_3d(end));
		} catch(Exception e) { 
			rotation = new MRotation(RotationOrder.X, 0d);
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
	public void set(SGVec_3d axis, double angle) {
		this.rotation.set(axis, angle);
	}
	
	/**
	 * sets the value of this rotation to what is represented 
	 * by the input startVector targetVector parameters
	 * @param axis
	 * @param angle
	 */
	public void set(SGVec_3d startVec, SGVec_3d targetVec) {
		this.rotation.set(startVec, targetVec);
	}
	

	public void applyTo( SGVec_3d v, SGVec_3d output) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyTo(workingInput, workingOutput);
		output.set(workingOutput);
	}


	public void applyInverseTo( SGVec_3d v, SGVec_3d output) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyInverseTo(workingInput, workingOutput);
		output.set(workingOutput);
	}


	/**
	 * applies the rotation to a copy of the input vector
	 * @param v
	 * @return
	 */
	
	public SGVec_3d applyToCopy( SGVec_3d v) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyTo(workingInput, workingOutput);
		 SGVec_3d copy =  v.copy();		
		return copy.set(workingOutput[0], workingOutput[1], workingOutput[2]);
	}


	public SGVec_3d  applyInverseToCopy( SGVec_3d v) {
		workingInput[0] = v.x; workingInput[1] = v.y; workingInput[2]=v.z; 
		rotation.applyInverseTo(workingInput, workingOutput);
		 SGVec_3d copy =  v.copy();		
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

	public sgRay applyToCopy(sgRay rIn) {
		workingInput[0] = rIn.p2().x - rIn.p1().x;
		workingInput[1] = rIn.p2().y - rIn.p1().y; 
		workingInput[2] = rIn.p2().z - rIn.p1().z;
		
		this.rotation.applyTo(workingInput, workingOutput);
		sgRay result = rIn.copy();
		result.heading(workingOutput);
		return result;
	}

	public sgRay applyInverseTo(sgRay rIn) {
		workingInput[0] = rIn.p2().x - rIn.p1().x;
		workingInput[1] = rIn.p2().y - rIn.p1().y; 
		workingInput[2] = rIn.p2().z - rIn.p1().z;		

		this.rotation.applyInverseTo(workingInput, workingOutput);
		sgRay result = rIn.copy();
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
	
	public double getAngle() {
		return (double) rotation.getAngle();  
	}

	public SGVec_3d getAxis() {
		return rotation.getAxis();
	}
	
	public void getAxis( SGVec_3d output) {
		output.set(rotation.getAxis());
	}


	public Rot revert() {
		return new Rot(this.rotation.revert());
	}


	/*
	 * interpolate between two rotations (SLERP)
	 * 
	 */
	public Rot(double amount, Rot v1, Rot v2) {
		Quaternion q = slerp(amount, 
				new Quaternion(v1.rotation.getQ0(), v1.rotation.getQ1(),v1.rotation.getQ2(), v1.rotation.getQ3()),
				new Quaternion(v2.rotation.getQ0(), v2.rotation.getQ1(),v2.rotation.getQ2(), v2.rotation.getQ3()));

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
	public Rot[] getSwingTwist ( SGVec_3d axis) {
		Quaternion thisQ = new Quaternion(rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3());
		 SGVec_3d temp =  axis.copy();
		double d = temp.set(thisQ.getQ1(), thisQ.getQ2(), thisQ.getQ3()).dot(axis);
		Quaternion twist = new Quaternion(rotation.getQ0(), axis.x * d, axis.y * d, axis.z * d).normalize();
		if (d < 0) twist.multiply(-1f);
		Quaternion swing = twist.getConjugate();
		swing = Quaternion.multiply(thisQ, swing);

		Rot[] result = new Rot[2]; 

		result[0] = new Rot(new MRotation(swing.getQ0(), swing.getQ1(), swing.getQ2(), swing.getQ3(), true));
		result[1] = new Rot(new MRotation(twist.getQ0(), twist.getQ1(), twist.getQ2(), twist.getQ3(), true));		

		return result;
	}


	public static Quaternion slerp(double amount, Quaternion value1, Quaternion value2)
	{
		if(Double.isNaN(amount)) {
			return new Quaternion(value1.getQ0(), value1.getQ1(), value1.getQ2(), value1.getQ3());
		}
		
		if (amount < 0.0)
			return value1;
		else if (amount > 1.0)
			return value2;

		double dot = value1.dotProduct(value2);
		double x2, y2, z2, w2;
		if (dot < 0.0)
		{
			dot = 0.0 - dot;
			x2 = 0.0 - value2.getQ1();
			y2 = 0.0 - value2.getQ2();
			z2 = 0.0 - value2.getQ3();
			w2 = 0.0 - value2.getQ0();
		}
		else
		{
			x2 = value2.getQ1();
			y2 = value2.getQ2();
			z2 = value2.getQ3();
			w2 = value2.getQ0();
		}

		double t1, t2;

		final double EPSILON = 0.0001;
		if ((1.0 - dot) > EPSILON) // standard case (slerp)
		{
			double angle = Math.acos(dot);
			double sinAngle = Math.sin(angle);
			t1 = Math.sin((1.0 - amount) * angle) / sinAngle;
			t2 = Math.sin(amount * angle) / sinAngle;
		}
		else // just lerp
		{
			t1 = 1.0 - amount;
			t2 = amount;
		}

		return new Quaternion(
				(value1.getQ0() * t1) + (w2 * t2),
				(value1.getQ1() * t1) + (x2 * t2),
				(value1.getQ2() * t1) + (y2 * t2),
				(value1.getQ3() * t1) + (z2 * t2));
	}
	
	public static MRotation slerp(double amount, MRotation value1, MRotation value2)
	{
		
		if(Double.isNaN(amount)) {
			return new MRotation(value1.getQ0(), value1.getQ1(), value1.getQ2(), value1.getQ3());
		}
		if (amount < 0.0)
			return value1;
		else if (amount > 1.0)
			return value2;

		double dot = value1.dotProduct(value2);
		double x2, y2, z2, w2;
		if (dot < 0.0)
		{
			dot = 0.0 - dot;
			x2 = 0.0 - value2.getQ1();
			y2 = 0.0 - value2.getQ2();
			z2 = 0.0 - value2.getQ3();
			w2 = 0.0 - value2.getQ0();
		}
		else
		{
			x2 = value2.getQ1();
			y2 = value2.getQ2();
			z2 = value2.getQ3();
			w2 = value2.getQ0();
		}

		double t1, t2;

		final double EPSILON = 0.0001;
		if ((1.0 - dot) > EPSILON) // standard case (slerp)
		{
			double angle = Math.acos(dot);
			double sinAngle = Math.sin(angle);
			t1 = Math.sin((1.0 - amount) * angle) / sinAngle;
			t2 = Math.sin(amount * angle) / sinAngle;
		}
		else // just lerp
		{
			t1 = 1.0 - amount;
			t2 = amount;
		}

		return new MRotation(
				(value1.getQ0() * t1) + (w2 * t2),
				(value1.getQ1() * t1) + (x2 * t2),
				(value1.getQ2() * t1) + (y2 * t2),
				(value1.getQ3() * t1) + (z2 * t2));
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
		rotation = new MRotation(jarray.getDouble(0), jarray.getDouble(1), jarray.getDouble(2), jarray.getDouble(3), true);
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
