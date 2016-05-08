/*

Copyright (c) 2015 Eron Gjoni

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
package IK;
import sceneGraph.*;


import java.util.ArrayList;

import org.apache.commons.math3.complex.Quaternion;

/**
 * @author Eron
 *
 */
public class G {

	public static double lerp(double a, double b, double t) {
		return (1-t)*a + t*b;
	}

	public static double max(double a, double b) {
		return Math.max(a, b);  
	}

	public static double min(double a, double b) {
		return Math.min(a, b);  
	}

	public static double dist(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2)); 
	}

	public static double sqrt(double in) {
		return Math.sqrt(in);  
	}

	public static double degrees(double in) {
		return Math.toDegrees(in);  
	}

	public static double radians(double in) {
		return Math.toRadians(in);    
	}

	public static double tan(double in) {
		return Math.tan(in);  
	}

	public static double sin(double in) {
		return Math.sin(in);  
	}

	public static double cos(double in) {
		return Math.cos(in);  
	}

	public static double floor(double in) {
		return Math.floor(in);  
	}

	public static double ceil(double in) {
		return Math.ceil(in);  
	}

	public static double abs(double in) {
		return Math.abs(in);  
	}

	public static double atan2(double a, double b) {
		return Math.atan2(a, b);
	}

	public static double random(double a, double b) {
		return a+((abs(a-b))*Math.random()); 
	}

	public static double sq(double in) {
		return in*in;  
	}

	public static DVector axisRotation(DVector point, DVector axis, double angle) {
		//println("rotting");
		Rot rotation = new Rot(axis, angle);
		point = rotation.applyTo(point);
		return point;
	}

	public static DVector[] axisRotation(DVector[] points, DVector axis, double angle) {

		Rot rotation = new Rot(axis, angle);
		for(DVector point : points) {
			point = rotation.applyTo(point);
		}
		return points;
	}



	public static void rayRotation(DVector point, Ray r, double angle) {

		DVector axis = DVector.sub(r.p2, r.p1);
		point.sub(r.p1); 
		point = axisRotation(point, axis, angle);
		point.add(r.p1);

	}

	public static void rayRotation(Ray point, Ray axis, double angle) {
		DVector result = point.heading().copy();   
		result = axisRotation(result, axis.heading(), angle); 
		point.heading(result);

	}

	public static void rayRotation(DVector[] points, Ray r, double angle) {

		DVector axis = DVector.sub(r.p2, r.p1);
		for(DVector point : points) {
			point.sub(r.p1); 
		}
		points = axisRotation(points, axis, angle);
		for(DVector point : points) {
			point.add(r.p1); 
		}

	}


	public static boolean isOverTriangle(DVector p1, DVector p2, DVector origin, Ray hoverRay, DVector intersectsAt) {
		double [] uvw = new double[3];
		DVector intersectionResult = intersectTest(hoverRay, origin, p1, p2, uvw); 
		intersectsAt.x = intersectionResult.x; 
		intersectsAt.y = intersectionResult.y;
		intersectsAt.z = intersectionResult.z;

		if(uvw[0] < 0 || uvw[1] < 0 || uvw[2] < 0) {
			return false;
		} else { 
			return true;
		}
	}

	public static DVector closestPointOnGreatArc(DVector p1, DVector p2, DVector hoverPoint) {
		//returns the point on the great arc p1 <-> p2 to which hoverPoint is closest. If the point
		//does not lie on the great arc p1 <-> p2, returns whichever of p1 or p2 is closest to hoverPoint.
		DVector normal = p1.cross(p2); 
		Ray hoverRay = new Ray(hoverPoint, null); 
		hoverRay.heading(normal);
		hoverRay.elongate();  

		DVector intersectsAt = new DVector(); 
		boolean isOverTriangle = isOverTriangle(p1, p2, new DVector(0,0,0), hoverRay, intersectsAt); 

		//println("isOverTriangle = " + isOverTriangle);

		if(isOverTriangle == false) {
			if(DVector.angleBetween(intersectsAt, p1) < DVector.angleBetween(intersectsAt, p2)) {
				return p1;
			} else {
				return p2;
			}
		} else {
			return intersectsAt;   
		}

	}

	public static DVector closestPointOnGreatCircle(DVector p1, DVector p2, DVector hoverPoint) {
		//returns the point on the great circle through p1 <-> p2 to which hoverPoint is closest. 
		DVector normal = p1.cross(p2); 
		Ray hoverRay = new Ray(hoverPoint, null); 
		hoverRay.heading(normal);
		hoverRay.elongate();  

		DVector intersectsAt = new DVector(); 
		isOverTriangle(p1, p2, new DVector(0,0,0), hoverRay, intersectsAt); 

		//println("isOverTriangle = " + isOverTriangle); 
		return intersectsAt;   

	}

	public static DVector intersectTest(Ray R, DVector ta, DVector tb, DVector tc) {
		double[] uvw = new double[3];
		return DVector.add(planeIntersectTest(R.heading(), DVector.sub(ta, R.p1), DVector.sub(tb, R.p1), DVector.sub(tc, R.p1), uvw), R.p1);
		//println(uvw);
		//return DVector.add(DVector.add(DVector.mult(ta, uvw[0]), DVector.mult(tb, uvw[1])), DVector.mult(tc, uvw[2]));
	}

	public static DVector planeIntersectTestStrict(Ray R, DVector ta, DVector tb, DVector tc) {
		//will return null if the ray is too short to intersect the plane;
		double[] uvw = new double[3];
		DVector result = DVector.add(planeIntersectTest(R.heading(), DVector.sub(ta, R.p1), DVector.sub(tb, R.p1), DVector.sub(tc, R.p1), uvw), R.p1);
		Ray resultRay = new Ray(R.p1, result);
		if(resultRay.mag() > R.mag()) {/*println(resultRay.mag() + " > " + R.mag());*/  return null;} 
		else {/*println(resultRay.mag() + " < " + R.mag());*/ return result;}

		//println(uvw);
		//return DVector.add(DVector.add(DVector.mult(ta, uvw[0]), DVector.mult(tb, uvw[1])), DVector.mult(tc, uvw[2]));
	}

	public static DVector intersectTest(Ray R, DVector ta, DVector tb, DVector tc, double[] uvw) {
		return DVector.add(planeIntersectTest(R.heading(), DVector.sub(ta, R.p1), DVector.sub(tb, R.p1), DVector.sub(tc, R.p1), uvw), R.p1);
		//println(uvw);
		//return DVector.add(DVector.add(DVector.mult(ta, uvw[0]), DVector.mult(tb, uvw[1])), DVector.mult(tc, uvw[2]));
	}

	public static DVector triangleRayIntersectTest(Ray R, DVector ta, DVector tb, DVector tc) {
		double[] uvw = new double[3];
		DVector result = triangleIntersectTest(R.heading(), DVector.sub(ta, R.p1), DVector.sub(tb, R.p1), DVector.sub(tc, R.p1), uvw); 
		if(result != null) 
			return DVector.add(result, R.p1);
		else return null;
		//println(uvw);
		//return DVector.add(DVector.add(DVector.mult(ta, uvw[0]), DVector.mult(tb, uvw[1])), DVector.mult(tc, uvw[2]));
	}

	public static  DVector triangleRayIntersectTest(Ray R, DVector ta, DVector tb, DVector tc, double[] uvw) {

		DVector result = triangleIntersectTest(R.heading(), DVector.sub(ta, R.p1), DVector.sub(tb, R.p1), DVector.sub(tc, R.p1), uvw); 
		if(result != null) 
			return DVector.add(result, R.p1);
		else return null;

	}

	public static  DVector planeIntersectTest(DVector R, DVector ta, DVector tb, DVector tc, double[] uvw) {

		DVector I = new DVector();
		DVector u = new DVector(tb.x, tb.y, tb.z); 
		DVector v = new DVector(tc.x, tc.y, tc.z); 
		DVector n ;
		DVector dir = new DVector(R.x, R.y, R.z); 
		DVector w0 = new DVector(); 
		// DVector w = new DVector();
		double     r, a, b;

		//u = ta;
		DVector.sub(u, ta, u);
		//v = tc;
		DVector.sub(v, ta, v);
		n = new DVector(); // cross product
		DVector.cross(u, v, n);


		w0 = new DVector(0,0,0);
		DVector.sub(w0, ta, w0);
		a = -(new DVector(n.x, n.y, n.z).dot(w0));
		b = new DVector(n.x, n.y, n.z).dot(dir);

		r = a / b;

		I = new DVector(0,0,0);
		I.x += r * dir.x;
		I.y += r * dir.y;
		I.z += r * dir.z;

		double[] barycentric = new double[3];
		barycentric(ta, tb, tc, I, barycentric);

		uvw[0]=barycentric[0];
		uvw[1]=barycentric[1];
		uvw[2]=barycentric[2];
		return I;
	}

	public static  DVector triangleIntersectTest(DVector R, DVector ta, DVector tb, DVector tc, double[] uvw) {

		/*println("------------"+ta); 
	        println("------------"+tb+ "-------------------" + tc);*/
		DVector I = new DVector();
		DVector u = new DVector(tb.x, tb.y, tb.z); 
		DVector v = new DVector(tc.x, tc.y, tc.z); 
		DVector n ;
		DVector dir = new DVector(R.x, R.y, R.z); 
		DVector w0 = new DVector(); 
		double     r, a, b;

		DVector.sub(u, ta, u);
		DVector.sub(v, ta, v);
		n = new DVector(); // cross product
		DVector.cross(u, v, n);

		if (n.mag() == 0) {
			return null;
		}

		w0 = new DVector(0,0,0);
		DVector.sub(w0, ta, w0);
		a = -(new DVector(n.x, n.y, n.z).dot(w0));
		b = new DVector(n.x, n.y, n.z).dot(dir);

		if ((double)Math.abs(b) <  Double.MIN_VALUE) {
			return null;
		}

		r = a / b;
		if (r < 0.0) {
			return null;
		}

		I = new DVector(0,0,0);
		I.x += r * dir.x;
		I.y += r * dir.y;
		I.z += r * dir.z;
		double[] barycentric = new double[3];
		barycentric(ta, tb, tc, I, barycentric);

		if(barycentric[0] >= 0 && barycentric[1] >= 0 && barycentric[2] >= 0){ 
			uvw[0]=barycentric[0];
			uvw[1]=barycentric[1];
			uvw[2]=barycentric[2];
			return I;
		} else {
			return null;  
		}
	}

	public static DVector intersectTest(DVector R, DVector ta, DVector tb, DVector tc, double[] uvw) {

		DVector I = new DVector();
		DVector u = new DVector(tb.x, tb.y, tb.z); 
		DVector v = new DVector(tc.x, tc.y, tc.z); 
		DVector n ;
		DVector dir = new DVector(R.x, R.y, R.z); 
		DVector w0 = new DVector(); 
		//DVector w = new DVector();
		double     r, a, b;

		//u = ta;
		DVector.sub(u, ta, u);
		//v = tc;
		DVector.sub(v, ta, v);
		n = new DVector(); // cross product
		DVector.cross(u, v, n);

		if (n.mag() == 0) {
			return null;
		}

		//dir = R;
		w0 = new DVector(0,0,0);
		DVector.sub(w0, ta, w0);
		a = -(new DVector(n.x, n.y, n.z).dot(w0));
		b = new DVector(n.x, n.y, n.z).dot(dir);

		if ((double)Math.abs(b) < Double.MIN_VALUE) {
			return null;
		}

		r = a / b;
		if (r < 0.0) {
			return null;
		}

		I = new DVector(0,0,0);
		I.x += r * dir.x;
		I.y += r * dir.y;
		I.z += r * dir.z;

		double[] barycentric = new double[3];
		barycentric(ta, tb, tc, I, barycentric);

		if(barycentric[0] >= 0 && barycentric[1] >= 0 && barycentric[2] >= 0){ 
			uvw[0]=barycentric[0];
			uvw[1]=barycentric[1];
			uvw[2]=barycentric[2];
			return I;
		} else {
			return null;  
		}
	}	


	public static void barycentric(DVector a, DVector b, DVector c, DVector p, double[] uvw) {
		DVector m = new DVector();
		DVector.cross(DVector.sub(b, c), DVector.sub(c, a), m);

		double nu;
		double nv;
		double ood;

		double x = Math.abs(m.x);
		double y = Math.abs(m.y);
		double z = Math.abs(m.z);

		// compute areas of plane with largest projections
		if (x >= y && x >= z) {
			// area of PBC in yz plane
			nu = triArea2D(p.y, p.z, b.y, b.z, c.y, c.z);
			// area of PCA in yz plane
			nv = triArea2D(p.y, p.z, c.y, c.z, a.y, a.z);
			// 1/2*area of ABC in yz plane
			ood = 1.0f / m.x;
		}
		else if (y >= x && y >= z) {
			// project in xz plane
			nu = triArea2D(p.x, p.z, b.x, b.z, c.x, c.z);
			nv = triArea2D(p.x, p.z, c.x, c.z, a.x, a.z);
			ood = 1.0f / -m.y;
		}
		else {
			// project in xy plane
			nu = triArea2D(p.x, p.y, b.x, b.y, c.x, c.y);
			nv = triArea2D(p.x, p.y, c.x, c.y, a.x, a.y);
			ood = 1.0f / m.z;
		}

		uvw[0] = nu * ood;
		uvw[1] = nv * ood;
		uvw[2] = 1.0f - uvw[0] - uvw[1];
	}

	public static double triArea2D(double x1, double y1, double x2, double y2, double x3, double y3) {
		return (x1 - x2) * (y2 - y3) - (x2 - x3) * (y1 - y2);   
	}

	public static int raySphereIntersection(Ray ray, double radius, DVector S1, DVector S2) {
		DVector direction = ray.heading();
		DVector e = new DVector(direction.x, direction.y, direction.z);   // e=ray.dir
		e.normalize();                            // e=g/|g|
		DVector h = DVector.sub(new DVector(0,0,0),ray.p1);  // h=r.o-c.M
		double lf = e.dot(h);                      // lf=e.h
		double s = sq(radius)-h.dot(h)+sq(lf);   // s=r^2-h^2+lf^2
		if (s < 0.0) return 0;                    // no intersection points ?
		s = sqrt(s);                              // s=sqrt(r^2-h^2+lf^2)

		int result = 0;
		if (lf < s)                               // S1 behind A ?
		{ if (lf+s >= 0)                          // S2 before A ?}
		{ s = -s;                               // swap S1 <-> S2}
		result = 1;                           // one intersection point
		} }
		else result = 2;                          // 2 intersection points

		S1.set(DVector.mult(e, lf-s));  S1.add(ray.p1); // S1=A+e*(lf-s)
		S2.set(DVector.mult(e, lf+s));  S2.add(ray.p1); // S2=A+e*(lf+s)

		// only for testing

		return result;
	}


	public static Rot weightedAverageRotation(ArrayList<Rot> rotList, ArrayList<Double> rotWeight) {
		double addedSoFar = 0; 
		double totalWeight = 0d;
		
		for(Double rt : rotWeight) totalWeight += rt; 

		double wT = 0;
		double xT = 0; 
		double yT = 0; 
		double zT = 0;

		Rot ir = rotList.get(0);

		Quaternion initialQ = new Quaternion(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());

		for(int i = 0; i < rotList.size(); i ++) {
			Rot rt = rotList.get(i);
			Rot r = new Rot(rt.getAxis(), rt.getAngle()*(rotWeight.get(i)/totalWeight));
			Quaternion current = getSingleCoveredQuaternion(
					new Quaternion(r.rotation.getQ0(), 
									r.rotation.getQ1(),
									r.rotation.getQ2(), 
									r.rotation.getQ3()), initialQ);
			wT += current.getQ0();
			xT += current.getQ1();
			yT += current.getQ2();
			zT += current.getQ3();
			addedSoFar++;
		}

		return new Rot(wT/addedSoFar, xT/addedSoFar, yT/addedSoFar, zT/addedSoFar, true);

	}

	public static Rot averageRotation(ArrayList<Rot> rotList) {
		double addedSoFar = 0; 

		double wT = 0;
		double xT = 0; 
		double yT = 0; 
		double zT = 0;

		Rot ir = rotList.get(0);

		Quaternion initialQ = new Quaternion(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());

		for(Rot r : rotList) {
			Quaternion current = getSingleCoveredQuaternion(new Quaternion(r.rotation.getQ0(), r.rotation.getQ1(), r.rotation.getQ2(), r.rotation.getQ3()), initialQ);
			wT += current.getQ0();
			xT += current.getQ1();
			yT += current.getQ2();
			zT += current.getQ3();
			addedSoFar++;
		}

		return new Rot(wT/addedSoFar, xT/addedSoFar, yT/addedSoFar, zT/addedSoFar, true);

	}

	public static Quaternion getSingleCoveredQuaternion(Quaternion inputQ, Quaternion targetQ) {
		//targetQ is the Quaternion that exists on the target hemisphere
		if(Quaternion.dotProduct(inputQ, targetQ) < 0d) {
			return new Quaternion(-inputQ.getQ0(), -inputQ.getQ1(), -inputQ.getQ2(), -inputQ.getQ3());  
		} else {
			return inputQ;  
		}

	}

	public static Quaternion getQuaternion(Rot r) {
		double w, x, y, z; 
		w = r.rotation.getQ0();
		x = r.rotation.getQ1();
		y = r.rotation.getQ2();
		z = r.rotation.getQ3(); 
		return new Quaternion(w,x,y,z);
	}


	public static Quaternion getNormalizedQuaternion(double iw, double ix, double iy, double iz) {
		double lengthD = 1.0d / (iw*iw + ix*ix + iy*iy + iz*iz);
		return new Quaternion(iw*lengthD, ix*lengthD, iy*lengthD, iz*lengthD);
	}

}
