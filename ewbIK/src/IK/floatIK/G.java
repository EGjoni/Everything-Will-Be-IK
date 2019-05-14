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
package IK.floatIK;
import sceneGraph.*;
import sceneGraph.math.floatV.MathUtils;
import sceneGraph.math.floatV.Quaternionf;
import sceneGraph.math.floatV.Rot;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.sgRayf;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.sgRayf;

import java.util.ArrayList;


/**
 * @author Eron
 *
 */
public class G {
	
	public static float PI = MathUtils.PI;
	public static float TAU = 2*PI;
	
	public static float lerp(float a, float b, float t) {
		return (1-t)*a + t*b;
	}

	public static float max(float a, float b) {
		return Math.max(a, b);  
	}

	public static float min(float a, float b) {
		return Math.min(a, b);  
	}

	public static float dist(float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2)); 
	}

	public static float sqrt(float in) {
		return (float)Math.sqrt(in);  
	}

	public static float degrees(float in) {
		return (float)Math.toDegrees(in);  
	}

	public static float radians(float in) {
		return (float)Math.toRadians(in);    
	}

	public static float tan(float in) {
		return (float)Math.tan(in);  
	}

	public static float sin(float in) {
		return MathUtils.sin(in);  
	}

	public static float cos(float in) {
		return MathUtils.cos(in);  
	}

	public static float floor(float in) {
		return MathUtils.floor(in);  
	}

	public static float ceil(float in) {
		return MathUtils.ceil(in);  
	}

	public static float abs(float in) {
		return Math.abs(in);  
	}

	public static float atan2(float a, float b) {
		return MathUtils.atan2(a, b);
	}

	public static float random(float a, float b) {
		return (float) (a+((abs(a-b))*Math.random())); 
	}

	public static float sq(float in) {
		return in*in;  
	}

	public static SGVec_3f axisRotation(SGVec_3f point, SGVec_3f axis, float angle) {
		//println("rotting");
		Rot rotation = new Rot(axis, angle);
		rotation.applyTo(point, point);
		return point;
	}


	public static boolean isOverTriangle(SGVec_3f p1, SGVec_3f p2, SGVec_3f origin, sgRayf hoverRay, SGVec_3f intersectsAt) {
		float [] uvw = new float[3];
		SGVec_3f intersectionResult = intersectTest(hoverRay, origin, p1, p2, uvw); 
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
	public static SGVec_3f closestPointOnGreatArc(SGVec_3f p1, SGVec_3f p2, SGVec_3f hoverPoint) {		
		SGVec_3f normal = p1.crossCopy(p2); 
		sgRayf hoverRay = new sgRayf(hoverPoint, null); 
		hoverRay.heading(normal);
		hoverRay.elongate();  

		SGVec_3f intersectsAt = new SGVec_3f(); 
		boolean isOverTriangle = isOverTriangle(p1, p2, new SGVec_3f(0,0,0), hoverRay, intersectsAt); 

		//println("isOverTriangle = " + isOverTriangle);

		if(isOverTriangle == false) {
			if(SGVec_3f.angleBetween(intersectsAt, p1) < SGVec_3f.angleBetween(intersectsAt, p2)) {
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
	public static SGVec_3f closestPointOnGreatCircle(SGVec_3f p1, SGVec_3f p2, SGVec_3f hoverPoint) {
		
		SGVec_3f normal = p1.crossCopy(p2); 
		sgRayf hoverRay = new sgRayf(hoverPoint, null); 
		hoverRay.heading(normal);
		hoverRay.elongate();  

		SGVec_3f intersectsAt = new SGVec_3f(); 
		isOverTriangle(p1, p2, new SGVec_3f(0,0,0), hoverRay, intersectsAt); 

		//println("isOverTriangle = " + isOverTriangle); 
		return intersectsAt;   

	}

	public static SGVec_3f intersectTest(sgRayf R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc) {
		float[] uvw = new float[3];
		return SGVec_3f.add(planeIntersectTest(R.heading(), SGVec_3f.sub(ta, R.p1()), SGVec_3f.sub(tb, R.p1()), SGVec_3f.sub(tc, R.p1()), uvw), R.p1());
		//println(uvw);
		//return SGVec_3f.add(SGVec_3f.add(SGVec_3f.mult(ta, uvw[0]), SGVec_3f.mult(tb, uvw[1])), SGVec_3f.mult(tc, uvw[2]));
	}

	public static SGVec_3f planeIntersectTestStrict(sgRayf R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc) {
		//will return null if the ray is too short to intersect the plane;
		float[] uvw = new float[3];
		SGVec_3f result = SGVec_3f.add(planeIntersectTest(R.heading(), SGVec_3f.sub(ta, R.p1()), SGVec_3f.sub(tb, R.p1()), SGVec_3f.sub(tc, R.p1()), uvw), R.p1());
		sgRayf resultRay = new sgRayf(R.p1(), result);
		if(resultRay.mag() > R.mag()) {/*println(resultRay.mag() + " > " + R.mag());*/  return null;} 
		else {/*println(resultRay.mag() + " < " + R.mag());*/ return result;}

		//println(uvw);
		//return SGVec_3f.add(SGVec_3f.add(SGVec_3f.mult(ta, uvw[0]), SGVec_3f.mult(tb, uvw[1])), SGVec_3f.mult(tc, uvw[2]));
	}

	public static SGVec_3f intersectTest(sgRayf R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {
		return SGVec_3f.add(planeIntersectTest(R.heading(), SGVec_3f.sub(ta, R.p1()), SGVec_3f.sub(tb, R.p1()), SGVec_3f.sub(tc, R.p1()), uvw), R.p1());
		//println(uvw);
		//return SGVec_3f.add(SGVec_3f.add(SGVec_3f.mult(ta, uvw[0]), SGVec_3f.mult(tb, uvw[1])), SGVec_3f.mult(tc, uvw[2]));
	}

	public static SGVec_3f triangleRayIntersectTest(sgRayf R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc) {
		float[] uvw = new float[3];
		SGVec_3f result = triangleIntersectTest(R.heading(), SGVec_3f.sub(ta, R.p1()), SGVec_3f.sub(tb, R.p1()), SGVec_3f.sub(tc, R.p1()), uvw); 
		if(result != null) 
			return SGVec_3f.add(result, R.p1());
		else return null;
		//println(uvw);
		//return SGVec_3f.add(SGVec_3f.add(SGVec_3f.mult(ta, uvw[0]), SGVec_3f.mult(tb, uvw[1])), SGVec_3f.mult(tc, uvw[2]));
	}

	public static  SGVec_3f triangleRayIntersectTest(sgRayf R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {

		SGVec_3f result = triangleIntersectTest(R.heading(), SGVec_3f.sub(ta, R.p1()), SGVec_3f.sub(tb, R.p1()), SGVec_3f.sub(tc, R.p1()), uvw); 
		if(result != null) 
			return SGVec_3f.add(result, R.p1());
		else return null;

	}

	public static  SGVec_3f planeIntersectTest(SGVec_3f R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {
		SGVec_3f I = new SGVec_3f();
		SGVec_3f u = new SGVec_3f(tb.x, tb.y, tb.z); 
		SGVec_3f v = new SGVec_3f(tc.x, tc.y, tc.z); 
		SGVec_3f n ;
		SGVec_3f dir = new SGVec_3f(R.x, R.y, R.z); 
		SGVec_3f w0 = new SGVec_3f(); 
		// SGVec_3f w = new SGVec_3f();
		float     r, a, b;

		//u = ta;
		SGVec_3f.sub(u, ta, u);
		//v = tc;
		SGVec_3f.sub(v, ta, v);
		n = new SGVec_3f(); // cross product
		SGVec_3f.cross(u, v, n);


		w0 = new SGVec_3f(0,0,0);
		SGVec_3f.sub(w0, ta, w0);
		a = -(new SGVec_3f(n.x, n.y, n.z).dot(w0));
		b = new SGVec_3f(n.x, n.y, n.z).dot(dir);

		r = a / b;

		I = new SGVec_3f(0,0,0);
		I.x += r * dir.x;
		I.y += r * dir.y;
		I.z += r * dir.z;

		float[] barycentric = new float[3];
		barycentric(ta, tb, tc, I, barycentric);

		uvw[0]=barycentric[0];
		uvw[1]=barycentric[1];
		uvw[2]=barycentric[2];
		return I;
	}

	public static  SGVec_3f triangleIntersectTest(SGVec_3f R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {

		/*println("------------"+ta); 
	        println("------------"+tb+ "-------------------" + tc);*/
		SGVec_3f I = new SGVec_3f();
		SGVec_3f u = new SGVec_3f(tb.x, tb.y, tb.z); 
		SGVec_3f v = new SGVec_3f(tc.x, tc.y, tc.z); 
		SGVec_3f n ;
		SGVec_3f dir = new SGVec_3f(R.x, R.y, R.z); 
		SGVec_3f w0 = new SGVec_3f(); 
		float     r, a, b;

		SGVec_3f.sub(u, ta, u);
		SGVec_3f.sub(v, ta, v);
		n = new SGVec_3f(); // cross product
		SGVec_3f.cross(u, v, n);

		if (n.mag() == 0) {
			return null;
		}

		w0 = new SGVec_3f(0,0,0);
		SGVec_3f.sub(w0, ta, w0);
		a = -(new SGVec_3f(n.x, n.y, n.z).dot(w0));
		b = new SGVec_3f(n.x, n.y, n.z).dot(dir);

		if ((float)Math.abs(b) <  Double.MIN_VALUE) {
			return null;
		}

		r = a / b;
		if (r < 0.0) {
			return null;
		}

		I = new SGVec_3f(0,0,0);
		I.x += r * dir.x;
		I.y += r * dir.y;
		I.z += r * dir.z;
		float[] barycentric = new float[3];
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

	public static SGVec_3f intersectTest(SGVec_3f R, SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {

		SGVec_3f I = new SGVec_3f();
		SGVec_3f u = new SGVec_3f(tb.x, tb.y, tb.z); 
		SGVec_3f v = new SGVec_3f(tc.x, tc.y, tc.z); 
		SGVec_3f n ;
		SGVec_3f dir = new SGVec_3f(R.x, R.y, R.z); 
		SGVec_3f w0 = new SGVec_3f(); 
		float     r, a, b;

		SGVec_3f.sub(u, ta, u);
		SGVec_3f.sub(v, ta, v);
		n = new SGVec_3f(); // cross product
		SGVec_3f.cross(u, v, n);

		if (n.mag() == 0) {
			return null;
		}

		w0 = new SGVec_3f(0,0,0);
		SGVec_3f.sub(w0, ta, w0);
		a = -(new SGVec_3f(n.x, n.y, n.z).dot(w0));
		b = new SGVec_3f(n.x, n.y, n.z).dot(dir);

		if ((float)Math.abs(b) < Double.MIN_VALUE) {
			return null;
		}

		r = a / b;

		I = new SGVec_3f(0,0,0);
		I.x += r * dir.x;
		I.y += r * dir.y;
		I.z += r * dir.z;

		float[] barycentric = new float[3];
		barycentric(ta, tb, tc, I, barycentric);

		uvw[0]=barycentric[0];
		uvw[1]=barycentric[1];
		uvw[2]=barycentric[2];
		return I;
		
	}	


	public static void barycentric(SGVec_3f a, SGVec_3f b, SGVec_3f c, SGVec_3f p, float[] uvw) {
		SGVec_3f m = new SGVec_3f();
		SGVec_3f.cross(SGVec_3f.sub(b, c), SGVec_3f.sub(c, a), m);

		float nu;
		float nv;
		float ood;

		float x = Math.abs(m.x);
		float y = Math.abs(m.y);
		float z = Math.abs(m.z);

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
	
	public static SGVec_3f barycentricToCartesian(SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {
		SGVec_3f result = SGVec_3f.mult(ta, uvw[0]);
		result.add(SGVec_3f.mult(tb, uvw[1]));
		result.add(SGVec_3f.mult(tc, uvw[2]));

		return result;
	}

	public static float triArea2D(float x1, float y1, float x2, float y2, float x3, float y3) {
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
	public static int raySphereIntersection(sgRayf ray, float radius, SGVec_3f S1, SGVec_3f S2) {
		SGVec_3f direction = ray.heading();
		SGVec_3f e = new SGVec_3f(direction.x, direction.y, direction.z);   // e=ray.dir
		e.normalize();                            // e=g/|g|
		SGVec_3f h = SGVec_3f.sub(new SGVec_3f(0,0,0),ray.p1());  // h=r.o-c.M
		float lf = e.dot(h);                      // lf=e.h
		float s = sq(radius)-h.dot(h)+sq(lf);   // s=r^2-h^2+lf^2
		if (s < 0.0) return 0;                    // no intersection points ?
		s = sqrt(s);                              // s=sqrt(r^2-h^2+lf^2)

		int result = 0;
		if (lf < s)                               // S1 behind A ?
		{ if (lf+s >= 0)                          // S2 before A ?}
		{ s = -s;                               // swap S1 <-> S2}
		result = 1;                           // one intersection point
		} }
		else result = 2;                          // 2 intersection points

		S1.set(SGVec_3f.mult(e, lf-s));  S1.add(ray.p1()); // S1=A+e*(lf-s)
		S2.set(SGVec_3f.mult(e, lf+s));  S2.add(ray.p1()); // S2=A+e*(lf+s)

		// only for testing

		return result;
	}


	public static Rot weightedAverageRotation(ArrayList<Rot> rotList, ArrayList<Float> rotWeight) {
		float addedSoFar = 0; 
		float totalWeight = 0f;
		
		for(Float rt : rotWeight) totalWeight += rt; 

		float wT = 0;
		float xT = 0; 
		float yT = 0; 
		float zT = 0;

		Rot ir = rotList.get(0);

		Quaternionf initialQ = new Quaternionf(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());

		for(int i = 0; i < rotList.size(); i ++) {
			Rot rt = rotList.get(i);
			Rot r = new Rot(rt.getAxis(), rt.getAngle()*(rotWeight.get(i)/totalWeight));
			Quaternionf current = getSingleCoveredQuaternionf(
					new Quaternionf(r.rotation.getQ0(), 
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
		float addedSoFar = 0; 

		float wT = 0;
		float xT = 0; 
		float yT = 0; 
		float zT = 0;

		Rot ir = rotList.get(0);

		Quaternionf initialQ = new Quaternionf(ir.rotation.getQ0(), ir.rotation.getQ1(), ir.rotation.getQ2(), ir.rotation.getQ3());

		for(Rot r : rotList) {
			Quaternionf current = getSingleCoveredQuaternionf(new Quaternionf(r.rotation.getQ0(), r.rotation.getQ1(), r.rotation.getQ2(), r.rotation.getQ3()), initialQ);
			wT += current.getQ0();
			xT += current.getQ1();
			yT += current.getQ2();
			zT += current.getQ3();
			addedSoFar++;
		}

		return new Rot(wT/addedSoFar, xT/addedSoFar, yT/addedSoFar, zT/addedSoFar, true);

	}

	public static Quaternionf getSingleCoveredQuaternionf(Quaternionf inputQ, Quaternionf targetQ) {
		//targetQ is the Quaternionf that exists on the target hemisphere
		if(inputQ.dot(targetQ) < 0f) {
			return new Quaternionf(-inputQ.getQ0(), -inputQ.getQ1(), -inputQ.getQ2(), -inputQ.getQ3());  
		} else {
			return inputQ;  
		}

	}

	public static Quaternionf getQuaternionf(Rot r) {
		float w, x, y, z; 
		w = r.rotation.getQ0();
		x = r.rotation.getQ1();
		y = r.rotation.getQ2();
		z = r.rotation.getQ3(); 
		return new Quaternionf(w,x,y,z);
	}


	public static Quaternionf getNormalizedQuaternionf(float iw, float ix, float iy, float iz) {
		float lengthD = 1.0f / (iw*iw + ix*ix + iy*iy + iz*iz);
		return new Quaternionf(iw*lengthD, ix*lengthD, iy*lengthD, iz*lengthD);
	}
	
	public static float smoothstep(float edge0, float edge1, float x) {
		float t = (float) Math.min(Math.max((x - edge0) / (edge1 - edge0), 0.0), 1.0);
		return (float) (t * t * (3.0 - 2.0 * t));
	}


}
