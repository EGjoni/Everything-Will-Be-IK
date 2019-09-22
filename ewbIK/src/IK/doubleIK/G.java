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
package IK.doubleIK;
import sceneGraph.*;
import sceneGraph.math.doubleV.Quaternion;
import sceneGraph.math.doubleV.Rot;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.doubleV.sgRayd;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.sgRayf;

import java.util.ArrayList;


/**
 * @author Eron
 *
 */
public class G {
	
	public static double PI = Math.PI;
	public static double TAU = 2*PI;
	
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

	public static SGVec_3d axisRotation(SGVec_3d point, SGVec_3d axis, double angle) {
		//println("rotting");
		Rot rotation = new Rot(axis, angle);
		rotation.applyTo(point, point);
		return point;
	}


	public static boolean isOverTriangle(SGVec_3d p1, SGVec_3d p2, SGVec_3d origin, sgRayd hoverRay, SGVec_3d intersectsAt) {
		double [] uvw = new double[3];
		SGVec_3d intersectionResult = intersectTest(hoverRay, origin, p1, p2, uvw); 
		intersectsAt.x = intersectionResult.x; 
		intersectsAt.y = intersectionResult.y;
		intersectsAt.z = intersectionResult.z;

		if(uvw[0] < 0 || uvw[1] < 0 || uvw[2] < 0) {
			return false;
		} else { 
			return true;
		}
	}

	/**
	 * returns the point on the great arc p1 <-> p2 to which hoverPoint is closest. If the point
	 * does not lie on the great arc p1 <-> p2, returns whichever of p1 or p2 is closest to hoverPoint.
	 * @param p1
	 * @param p2
	 * @param hoverPoint
	 * @return
	 */
	public static SGVec_3d closestPointOnGreatArc(SGVec_3d p1, SGVec_3d p2, SGVec_3d hoverPoint) {		
		SGVec_3d normal = p1.crossCopy(p2); 
		sgRayd hoverRay = new sgRayd(hoverPoint, null); 
		hoverRay.heading(normal);
		hoverRay.elongate();  

		SGVec_3d intersectsAt = new SGVec_3d(); 
		boolean isOverTriangle = isOverTriangle(p1, p2, new SGVec_3d(0,0,0), hoverRay, intersectsAt); 

		//println("isOverTriangle = " + isOverTriangle);

		if(isOverTriangle == false) {
			if(SGVec_3d.angleBetween(intersectsAt, p1) < SGVec_3d.angleBetween(intersectsAt, p2)) {
				return p1;
			} else {
				return p2;
			}
		} else {
			return intersectsAt;   
		}

	}

	/**
	 * returns the point on the great circle through p1 <-> p2 to which hoverPoint is closest. 
	 * @param p1
	 * @param p2
	 * @param hoverPoint
	 * @return
	 */
	public static SGVec_3d closestPointOnGreatCircle(SGVec_3d p1, SGVec_3d p2, SGVec_3d hoverPoint) {
		
		SGVec_3d normal = p1.crossCopy(p2); 
		sgRayd hoverRay = new sgRayd(hoverPoint, null); 
		hoverRay.heading(normal);
		hoverRay.elongate();  

		SGVec_3d intersectsAt = new SGVec_3d(); 
		isOverTriangle(p1, p2, new SGVec_3d(0,0,0), hoverRay, intersectsAt); 

		//println("isOverTriangle = " + isOverTriangle); 
		return intersectsAt;   

	}

	public static SGVec_3d intersectTest(sgRayd R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc) {
		double[] uvw = new double[3];
		return SGVec_3d.add(planeIntersectTest(R.heading(), SGVec_3d.sub(ta, R.p1()), SGVec_3d.sub(tb, R.p1()), SGVec_3d.sub(tc, R.p1()), uvw), R.p1());
		//println(uvw);
		//return SGVec_3d.add(SGVec_3d.add(SGVec_3d.mult(ta, uvw[0]), SGVec_3d.mult(tb, uvw[1])), SGVec_3d.mult(tc, uvw[2]));
	}

	public static SGVec_3d planeIntersectTestStrict(sgRayd R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc) {
		//will return null if the ray is too short to intersect the plane;
		double[] uvw = new double[3];
		SGVec_3d result = SGVec_3d.add(planeIntersectTest(R.heading(), SGVec_3d.sub(ta, R.p1()), SGVec_3d.sub(tb, R.p1()), SGVec_3d.sub(tc, R.p1()), uvw), R.p1());
		sgRayd resultRay = new sgRayd(R.p1(), result);
		if(resultRay.mag() > R.mag()) {/*println(resultRay.mag() + " > " + R.mag());*/  return null;} 
		else {/*println(resultRay.mag() + " < " + R.mag());*/ return result;}

		//println(uvw);
		//return SGVec_3d.add(SGVec_3d.add(SGVec_3d.mult(ta, uvw[0]), SGVec_3d.mult(tb, uvw[1])), SGVec_3d.mult(tc, uvw[2]));
	}

	public static SGVec_3d intersectTest(sgRayd R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc, double[] uvw) {
		return SGVec_3d.add(planeIntersectTest(R.heading(), SGVec_3d.sub(ta, R.p1()), SGVec_3d.sub(tb, R.p1()), SGVec_3d.sub(tc, R.p1()), uvw), R.p1());
		//println(uvw);
		//return SGVec_3d.add(SGVec_3d.add(SGVec_3d.mult(ta, uvw[0]), SGVec_3d.mult(tb, uvw[1])), SGVec_3d.mult(tc, uvw[2]));
	}

	public static SGVec_3d triangleRayIntersectTest(sgRayd R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc) {
		double[] uvw = new double[3];
		SGVec_3d result = triangleIntersectTest(R.heading(), SGVec_3d.sub(ta, R.p1()), SGVec_3d.sub(tb, R.p1()), SGVec_3d.sub(tc, R.p1()), uvw); 
		if(result != null) 
			return SGVec_3d.add(result, R.p1());
		else return null;
		//println(uvw);
		//return SGVec_3d.add(SGVec_3d.add(SGVec_3d.mult(ta, uvw[0]), SGVec_3d.mult(tb, uvw[1])), SGVec_3d.mult(tc, uvw[2]));
	}

	public static  SGVec_3d triangleRayIntersectTest(sgRayd R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc, double[] uvw) {

		SGVec_3d result = triangleIntersectTest(R.heading(), SGVec_3d.sub(ta, R.p1()), SGVec_3d.sub(tb, R.p1()), SGVec_3d.sub(tc, R.p1()), uvw); 
		if(result != null) 
			return SGVec_3d.add(result, R.p1());
		else return null;

	}

	public static  SGVec_3d planeIntersectTest(SGVec_3d R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc, double[] uvw) {
		SGVec_3d I = new SGVec_3d();
		SGVec_3d u = new SGVec_3d(tb.x, tb.y, tb.z); 
		SGVec_3d v = new SGVec_3d(tc.x, tc.y, tc.z); 
		SGVec_3d n ;
		SGVec_3d dir = new SGVec_3d(R.x, R.y, R.z); 
		SGVec_3d w0 = new SGVec_3d(); 
		// SGVec_3d w = new SGVec_3d();
		double     r, a, b;

		//u = ta;
		SGVec_3d.sub(u, ta, u);
		//v = tc;
		SGVec_3d.sub(v, ta, v);
		n = new SGVec_3d(); // cross product
		SGVec_3d.cross(u, v, n);


		w0 = new SGVec_3d(0,0,0);
		SGVec_3d.sub(w0, ta, w0);
		a = -(new SGVec_3d(n.x, n.y, n.z).dot(w0));
		b = new SGVec_3d(n.x, n.y, n.z).dot(dir);

		r = a / b;

		I = new SGVec_3d(0,0,0);
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

	public static  SGVec_3d triangleIntersectTest(SGVec_3d R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc, double[] uvw) {

		/*println("------------"+ta); 
	        println("------------"+tb+ "-------------------" + tc);*/
		SGVec_3d I = new SGVec_3d();
		SGVec_3d u = new SGVec_3d(tb.x, tb.y, tb.z); 
		SGVec_3d v = new SGVec_3d(tc.x, tc.y, tc.z); 
		SGVec_3d n ;
		SGVec_3d dir = new SGVec_3d(R.x, R.y, R.z); 
		SGVec_3d w0 = new SGVec_3d(); 
		double     r, a, b;

		SGVec_3d.sub(u, ta, u);
		SGVec_3d.sub(v, ta, v);
		n = new SGVec_3d(); // cross product
		SGVec_3d.cross(u, v, n);

		if (n.mag() == 0) {
			return null;
		}

		w0 = new SGVec_3d(0,0,0);
		SGVec_3d.sub(w0, ta, w0);
		a = -(new SGVec_3d(n.x, n.y, n.z).dot(w0));
		b = new SGVec_3d(n.x, n.y, n.z).dot(dir);

		if ((double)Math.abs(b) <  Double.MIN_VALUE) {
			return null;
		}

		r = a / b;
		if (r < 0.0) {
			return null;
		}

		I = new SGVec_3d(0,0,0);
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

	public static SGVec_3d intersectTest(SGVec_3d R, SGVec_3d ta, SGVec_3d tb, SGVec_3d tc, double[] uvw) {

		SGVec_3d I = new SGVec_3d();
		SGVec_3d u = new SGVec_3d(tb.x, tb.y, tb.z); 
		SGVec_3d v = new SGVec_3d(tc.x, tc.y, tc.z); 
		SGVec_3d n ;
		SGVec_3d dir = new SGVec_3d(R.x, R.y, R.z); 
		SGVec_3d w0 = new SGVec_3d(); 
		double     r, a, b;

		SGVec_3d.sub(u, ta, u);
		SGVec_3d.sub(v, ta, v);
		n = new SGVec_3d(); // cross product
		SGVec_3d.cross(u, v, n);

		if (n.mag() == 0) {
			return null;
		}

		w0 = new SGVec_3d(0,0,0);
		SGVec_3d.sub(w0, ta, w0);
		a = -(new SGVec_3d(n.x, n.y, n.z).dot(w0));
		b = new SGVec_3d(n.x, n.y, n.z).dot(dir);

		if ((double)Math.abs(b) < Double.MIN_VALUE) {
			return null;
		}

		r = a / b;

		I = new SGVec_3d(0,0,0);
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


	public static void barycentric(SGVec_3d a, SGVec_3d b, SGVec_3d c, SGVec_3d p, double[] uvw) {
		SGVec_3d m = new SGVec_3d();
		SGVec_3d.cross(SGVec_3d.sub(b, c), SGVec_3d.sub(c, a), m);

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
	
	public static SGVec_3d barycentricToCartesian(SGVec_3d ta, SGVec_3d tb, SGVec_3d tc, double[] uvw) {
		SGVec_3d result = SGVec_3d.mult(ta, uvw[0]);
		result.add(SGVec_3d.mult(tb, uvw[1]));
		result.add(SGVec_3d.mult(tc, uvw[2]));

		return result;
	}

	public static double triArea2D(double x1, double y1, double x2, double y2, double x3, double y3) {
		return (x1 - x2) * (y2 - y3) - (x2 - x3) * (y1 - y2);   
	}

	
	/**
	 * Find where a ray intersects a sphere (centered about the origin)
	 * @param ray to test against sphere 
	 * @param radius radius of the sphere
	 * @param S1 reference to variable in which the first intersection will be placed
	 * @param S2 reference to variable in which the second intersection will be placed
	 * @return number of intersections found;
	 */
	public static int raySphereIntersection(sgRayd ray, double radius, SGVec_3d S1, SGVec_3d S2) {
		SGVec_3d direction = ray.heading();
		SGVec_3d e = new SGVec_3d(direction.x, direction.y, direction.z);   // e=ray.dir
		e.normalize();                            // e=g/|g|
		SGVec_3d h = SGVec_3d.sub(new SGVec_3d(0,0,0),ray.p1());  // h=r.o-c.M
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

		S1.set(SGVec_3d.mult(e, lf-s));  S1.add(ray.p1()); // S1=A+e*(lf-s)
		S2.set(SGVec_3d.mult(e, lf+s));  S2.add(ray.p1()); // S2=A+e*(lf+s)

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
		double dot = inputQ.dot(targetQ);
		if(dot <0d) {
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
	
	public static double smoothstep(double edge0, double edge1, double x) {
		double t = Math.min(Math.max((x - edge0) / (edge1 - edge0), 0.0), 1.0);
		return t * t * (3.0 - 2.0 * t);
	}

	

}
