/*

Copyright (c) 2016 Eron Gjoni

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and 
associated documentation files (the "Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

 */

package sceneGraph;
import processing.core.PVector;
import org.apache.commons.math3.geometry.euclidean.threed.*;
import org.apache.commons.math3.complex.*;

public class Rot {
	public Rotation rotation; 

	public Rot(){};
	
	public Rot(DVector iv1, DVector iv2, DVector iu1, DVector iu2) {
		Vector3D v1 = new Vector3D((double) iv1.x, (double) iv1.y, (double) iv1.z);
		Vector3D v2 = new Vector3D((double) iv2.x, (double) iv2.y, (double) iv2.z);
		Vector3D u1 = new Vector3D((double) iu1.x, (double) iu1.y, (double) iu1.z);
		Vector3D u2 = new Vector3D((double) iu2.x, (double) iu2.y, (double) iu2.z);
		try {
			rotation = new Rotation(v1, v2, u1, u2);
		} catch(Exception e) {
			rotation = new Rotation(v1, 0f);
		}
	}

	public Rot(DVector v, double t) {
		Vector3D axis = new Vector3D((double)v.x, (double) v.y, (double) v.z);
		double angle = (double) t;        
		try {
			rotation = new Rotation(axis, angle); 
		} catch(Exception e) { 
			rotation = new Rotation(new Vector3D(1,0,0), 0d);
		}
	}

	public Rot(double w, double x, double y, double z, boolean needsNormalization) {
		this.rotation = new Rotation(w, x, y, z, needsNormalization);  
	}  

	public Rot(DVector v, DVector u) {
		Vector3D begin = new Vector3D((double)v.x, (double) v.y, (double) v.z);
		Vector3D end = new Vector3D((double)u.x, (double) u.y, (double) u.z);
		try{ 
			rotation = new Rotation(begin, end);
		} catch(Exception e) { 
			rotation = new Rotation(new Vector3D(1,0,0), 0d);
		}
	}

	public DVector applyTo(DVector v) {
		Vector3D result = new Vector3D((double)v.x, (double) v.y, (double) v.z); 
		result = rotation.applyTo(result);
		return new DVector((double)result.getX(), (double)result.getY(), (double)result.getZ());
	}

	public Ray applyTo(Ray r) {
		DVector v = r.heading(); 
		DVector heading = this.applyTo(v);
		Ray result = new Ray(r.p1, null);
		result.heading(heading);
		return result;
	}

	public double getAngle() {
		return (double) rotation.getAngle();  
	}

	public DVector getAxis() {
		Vector3D v = rotation.getAxis();
		return new DVector((double) v.getX(), (double) v.getY(), (double) v.getZ());  
	}


	public Rot(PVector iv1, PVector iv2, PVector iu1, PVector iu2) {
		Vector3D v1 = new Vector3D((double) iv1.x, (double) iv1.y, (double) iv1.z);
		Vector3D v2 = new Vector3D((double) iv2.x, (double) iv2.y, (double) iv2.z);
		Vector3D u1 = new Vector3D((double) iu1.x, (double) iu1.y, (double) iu1.z);
		Vector3D u2 = new Vector3D((double) iu2.x, (double) iu2.y, (double) iu2.z);
		try {
			rotation = new Rotation(v1, v2, u1, u2);
		} catch(Exception e) {
			rotation = new Rotation(v1, 0f);
		}
	}

	public Rot(PVector v, double t) {
		Vector3D axis = new Vector3D((double)v.x, (double) v.y, (double) v.z);
		double angle = (double) t;        
		try {
			rotation = new Rotation(axis, angle); 
		} catch(Exception e) { 
			rotation = new Rotation(new Vector3D(1,0,0), 0d);
		}
	}

	public Rot(PVector v, PVector u) {
		Vector3D begin = new Vector3D((double)v.x, (double) v.y, (double) v.z);
		Vector3D end = new Vector3D((double)u.x, (double) u.y, (double) u.z);
		try{ 
			rotation = new Rotation(begin, end);
		} catch(Exception e) { 
			rotation = new Rotation(new Vector3D(1,0,0), 0d);
		}
	}

	public PVector applyTo(PVector v) {
		Vector3D result = new Vector3D((double)v.x, (double) v.y, (double) v.z); 
		result = rotation.applyTo(result);
		return new PVector((float)result.getX(), (float)result.getY(), (float)result.getZ());
	}
	
	/*
	 * interpolate between two rotations (SLERP)
	 * 
	 */
	public Rot(double amount, Rot v1, Rot v2) {
		Quaternion q = slerp(amount, 
				new Quaternion(v1.rotation.getQ0(), v1.rotation.getQ1(),v1.rotation.getQ2(), v1.rotation.getQ3()),
				new Quaternion(v2.rotation.getQ0(), v2.rotation.getQ1(),v2.rotation.getQ2(), v2.rotation.getQ3()));
		
		rotation = new Rotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), false);
	}
	
	public Quaternion slerp(double amount, Quaternion value1, Quaternion value2)
    {
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
            (value1.getQ1() * t1) + (x2 * t2),
            (value1.getQ2() * t1) + (y2 * t2),
            (value1.getQ3() * t1) + (z2 * t2),
            (value1.getQ0() * t1) + (w2 * t2));
    }
	
	
}
