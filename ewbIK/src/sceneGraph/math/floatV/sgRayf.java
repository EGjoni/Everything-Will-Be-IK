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


package sceneGraph.math.floatV;

import data.JSONObject;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.floatV.SGVec_3f;

/**
 * @author Eron Gjoni
 *
 */
public class sgRayf {
	public static final int X=0, Y=1, Z=2;
	protected SGVec_3f  p1;
	protected SGVec_3f p2; 

	public sgRayf() {
		workingVector = new SGVec_3f();
		//this.p1 = new SGVec_3f();
	}
	
	public sgRayf(SGVec_3f origin) {
		this.workingVector =  origin.copy();
		this.p1 =  origin.copy();
	}

	public sgRayf(SGVec_3f p1, SGVec_3f p2) {
		this.workingVector =  p1.copy();
		this.p1 =  p1.copy();
		if(p2 != null)
			this.p2 =  p2.copy();
	}	


	public float distTo(SGVec_3f point) { 

		SGVec_3f inPoint =  point.copy();
		inPoint.sub(this.p1); 
		SGVec_3f heading =  this.heading();
		float scale = (inPoint.dot(heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());

		return point.dist(this.getRayScaledBy(scale).p2); 
	}

	/**
	 * returns the distance between the input point and the point on this ray (treated as a lineSegment) to which the input is closest.
	 * @param point
	 * @return
	 */
	public float distToStrict(SGVec_3f point) { 

		SGVec_3f inPoint =  point.copy();
		inPoint.sub(this.p1); 
		SGVec_3f heading = this.heading();
		float scale = (inPoint.dot(heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());
		if(scale < 0) {
			return point.dist(this.p1);   
		} else if (scale > 1) {
			return point.dist(this.p2);   
		} else {
			return point.dist(this.getRayScaledBy(scale).p2); 
		}    

	}


	/**
	 * returns the distance between this ray treated as a line and the input ray treated as a line. 
	 * @param r
	 * @return
	 */
	public float distTo(sgRayf r) {
		SGVec_3f closestOnThis = this.closestPointToRay3D(r);
		return r.distTo(closestOnThis);
	}

	/**
	 * returns the distance between this ray as a line segment, and the input ray treated as a line segment
	 */	
	public float distToStrict(sgRayf r) {
		SGVec_3f closestOnThis = this.closestPointToSegment3D(r);
		return closestOnThis.dist(r.closestPointToStrict(closestOnThis));
	}

	/**
	 * returns the point on this sgRay which is closest to the input point
	 * @param point
	 * @return
	 */
	public SGVec_3f closestPointTo(SGVec_3f point) { 

		workingVector.set(point);
		workingVector.sub(this.p1); 
		SGVec_3f heading = this.heading();
		heading.mag();
		workingVector.mag();
		//workingVector.normalize();
		heading.normalize();
		float scale = workingVector.dot(heading);


		return this.getScaledTo(scale);
	}

	public SGVec_3f closestPointToStrict(SGVec_3f point) {
		SGVec_3f inPoint =  point.copy();
		inPoint.sub(this.p1); 
		SGVec_3f heading = this.heading();
		float scale = (inPoint.dot(heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());

		if(scale <= 0) 
			return this.p1;
		else if (scale >= 1) 
			return this.p2;
		else 
			return this.getMultipledBy(scale); 
	}

	public SGVec_3f heading(){
		if(this.p2 == null) {
			if(p1 == null) p1 = new SGVec_3f();
			p2 =  p1.copy();
			p2.set(0f,0f,0f);
			return p2;
		}
		else {
			workingVector.set(p2);
			return  workingVector.subCopy(p1);
		}
	}

	/**
	 * manually sets the raw variables of this
	 * ray to be equivalent to the raw variables of the 
	 * target ray. Such that the two rays align without 
	 * creating a new variable. 
	 * @param target
	 */
	public void alignTo(sgRayf target) {
		p1.set(target.p1);
		p2.set(target.p2);
	}
	
	public void heading(float[] newHead){
		if(p2 == null) p2 =  p1.copy();
		p2.set(newHead);
		p2.set(p1);		
	}

	public void heading(SGVec_3f newHead){
		if(p2 == null) p2 =  p1.copy();
		p2.set(p1);
		p2.add(newHead);
	}
	public void heading(SGVec_3d newHead){
		if(p2 == null) p2 =  p1.copy();
		p2.set(p1);
		p2.add(new SGVec_3f(newHead));
	}

	


	/**
	 * sets the input vector equal to this sgRay's heading.
	 * @param setTo
	 */
	public void getHeading(SGVec_3f setTo){
		setTo.set(p2);
		setTo.sub(this.p1);
	}


	/**
	 * @return a copy of this ray with its z-component set to 0;
	 */
	public sgRayf get2DCopy() {
		return this.get2DCopy(sgRayf.Z);
	}

	/**
	 * gets a copy of this ray, with the component specified by
	 * collapseOnAxis set to 0. 
	 * @param collapseOnAxis the axis on which to collapse the ray.
	 * @return
	 */
	public sgRayf get2DCopy(int collapseOnAxis) {
		sgRayf result = this.copy(); 
		if(collapseOnAxis == sgRayf.X) {
			result.p1.setX_(0); 
			result.p2.setX_(0);
		}
		if(collapseOnAxis == sgRayf.Y) {
			result.p1.setY_(0);
			result.p2.setY_(0);
		}
		if(collapseOnAxis == sgRayf.Z) {
			result.p1.setZ_(0);
			result.p2.setZ_(0);
		}

		return result;
	}

	public SGVec_3f origin(){
		return  p1.copy();
	}

	public float mag() {
		workingVector.set(p2);
		return  (workingVector.sub(p1)).mag();   
	}

	public void mag(float newMag) {
		workingVector.set(p2);
		SGVec_3f dir =  workingVector.sub(p1);
		dir.setMag(newMag);
		this.heading(dir);   
	}


	/**
	 * Returns the scalar projection of the input vector on this 
	 * ray. In other words, if this ray goes from (5, 0) to (10, 0), 
	 * and the input vector is (7.5, 7), this function 
	 * would output 0.5. Because that is amount the ray would need 
	 * to be scaled by so that its tip is where the vector would project onto
	 * this ray. 
	 * 
	 * Due to floating point errors, the intended properties of this function might 
	 * not be entirely consistent with its output under summation. 
	 * 
	 * To help spare programmer cognitive cycles debugging in such circumstances, the intended properties 
	 * are listed for reference here (despite their being easily inferred). 
	 * 
	 * 1. calling scaledProjection(someVector) should return the same value as calling 
	 * scaledProjection(closestPointTo(someVector).
	 * 2. calling getMultipliedBy(scaledProjection(someVector)) should return the same 
	 * vector as calling closestPointTo(someVector)
	 * 
	 * 
	 * @param input a vector to project onto this ray  
	 */
	public float scaledProjection(SGVec_3f input) {
		workingVector.set(input);
		workingVector.sub(this.p1); 
		SGVec_3f heading = this.heading();
		float headingMag = heading.mag();
		float workingVectorMag = workingVector.mag();
		if(workingVectorMag == 0 || headingMag == 0) 
			return 0;
		else 
			return (workingVector.dot(heading)/(headingMag*workingVectorMag))*(workingVectorMag/headingMag);
	}


	protected SGVec_3f workingVector; 



	/**
	 * divides the ray by the amount specified by divisor, such that the 
	 * base of the ray remains where it is, and the tip
	 * is scaled accordinly.  
	 * @param divisor
	 */
	public void div(float divisor) {
		p2.sub(p1); 
		p2.div(divisor);
		p2.add(p1);
	}


	/**
	 * multiples the ray by the amount specified by scalar, such that the 
	 * base of the ray remains where it is, and the tip
	 * is scaled accordinly.  
	 * @param divisor
	 */
	public void mult(float scalar) {
		p2.sub(p1); 
		p2.mult(scalar);
		p2.add(p1);
	}


	/**
	 * Returns a SGVec_3f representing where the tip
	 * of this ray would be if mult() was called on the ray
	 * with scalar as the parameter. 
	 * @param scalar
	 * @return
	 */
	public SGVec_3f getMultipledBy(float scalar) {
		SGVec_3f result = this.heading();
		result.mult(scalar);
		result.add(p1); 
		return result;
	}


	/**
	 * Returns a SGVec_3f representing where the tip
	 * of this ray would be if div() was called on the ray
	 * with scalar as the parameter. 
	 * @param scalar
	 * @return
	 */
	public SGVec_3f getDivideddBy(float divisor) {
		SGVec_3f result =  this.heading().copy();
		result.mult(divisor);
		result.add(p1); 
		return result;
	}


	/**
	 * Returns a SGVec_3f representing where the tip
	 * of this ray would be if mag(scale) was called on the ray
	 * with scalar as the parameter. 
	 * @param scalar
	 * @return
	 */
	public SGVec_3f getScaledTo(float scale) {
		SGVec_3f result =  this.heading().copy();
		result.normalize(); 
		result.mult(scale);
		result.add(p1); 
		return result;
	}


/**
 * scale the ray outward in both directions by a large amount. (900)
 */
	public void elongate() {
		sgRayf reverseRay = new sgRayf(this.p2.copy(), this.p1.copy()); 
		sgRayf result = this.getRayScaledBy(900); 
		reverseRay = reverseRay.getRayScaledBy(900);
		result.p1 = reverseRay.p2.copy();
		this.p1.set(result.p1); 
		this.p2.set(result.p2);
	}

	public sgRayf copy() {
		return new sgRayf(this.p1, this.p2);  
	}

	public void reverse() {
		SGVec_3f temp = this.p1; 
		this.p1 = this.p2;
		this.p2 = temp; 
	}

	public sgRayf getReversed() {
		return new sgRayf(this.p2, this.p1);
	}

	public sgRayf getRayScaledTo(float scalar) {
		return new sgRayf(p1, this.getScaledTo(scalar));
	}

	/*
	 * reverses this ray's direction so that it 
	 * has a positive dot product with the heading of r
	 * if dot product is already positive, does nothing.
	 */
	public void pointWith(sgRayf r) {
		if(this.heading().dot(r.heading()) < 0) this.reverse(); 
	}

	public void pointWith(SGVec_3f heading) {
		if(this.heading().dot(heading) < 0) this.reverse(); 
	}
	public sgRayf getRayScaledBy(float scalar) {

		return new sgRayf(p1, this.getMultipledBy(scalar));
	}

	public void contractTo(float percent) {
		//contracts both ends of a ray toward its center such that the total length of the ray
		//is the percent % of its current length; 
		float halfPercent = 1-((1-percent)/2f);

		p1 =  p1.lerp(p2, halfPercent);//)new SGVec_3f(p1Tempx, p1Tempy, p1Tempz);
		p2 =  p2.lerp(p1, halfPercent);//new SGVec_3f(p2Tempx, p2Tempy, p2Tempz);
	}

	public void translateTo(SGVec_3f newLocation) {

		workingVector.set(p2);
		workingVector.sub(p1);
		workingVector.add(newLocation);
		p2.set(workingVector);
		p1.set(newLocation);
	}

	public void translateTipTo(SGVec_3f newLocation) {
		workingVector.set(newLocation);
		SGVec_3f transBy =  workingVector.sub(p2); 
		this.translateBy(transBy); 
	}

	public void translateBy(SGVec_3f toAdd) {
		p1.add(toAdd); 
		p2.add(toAdd);
	}


	public void normalize() {
		this.mag(1);  
	}

	public SGVec_3f intercepts2D (sgRayf r) {
		SGVec_3f result =  p1.copy();
		
		float p0_x = this.p1.x;
		float p0_y = this.p1.y; 
		float p1_x = this.p2.x;
		float p1_y = this.p2.y;

		float p2_x = r.p1.x;
		float p2_y = r.p1.y;
		float p3_x = r.p2.x; 
		float p3_y = r.p2.y;

		float s1_x, s1_y, s2_x, s2_y;
		s1_x = p1_x - p0_x;     s1_y = p1_y - p0_y;
		s2_x = p3_x - p2_x;     s2_y = p3_y - p2_y;

		float t;
		t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

		//if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
		// Collision detected
		return  result.set(p0_x + (t * s1_x), p0_y + (t * s1_y), 0f);
		// }

		//return null; // No collision
	}

	/*public SGVec_3f intercepts2D(sgRay r) {
		SGVec_3f result = new SGVec_3f();

		float a1 = p2.y - p1.y;
		float b1 = p1.x - p2.x;
		float c1 = a1*p1.x + b1*p1.y;

		float a2 = r.p2.y - r.p1.y;
		float b2 = r.p1.x - r.p2.y;
		float c2 = a2* + b2* r.p1.y;

		float det = a1*b2 - a2*b1;
		if(det == 0){
			// Lines are parallel
			return null;
		}
		else {
			result.x = (b2*c1 - b1*c2)/det;
			result.y = (a1*c2 - a2*c1)/det;
		}   
		return result;
	}*/

	/**
	 * If the closest point to this sgRay on the input sgRay lies
	 * beyond the bounds of that input sgRay, this returns closest point
	 * to the input Rays bound; 	
	 * @param r
	 * @return
	 */
	public SGVec_3f closestPointToSegment3D(sgRayf r) {
		SGVec_3f closestToThis =  r.closestPointToRay3DStrict(this); 
		return this.closestPointTo(closestToThis);
	}

	/*public SGVec_3f closestPointToSegment3DStrict(sgRay r) {

	}*/

	/**
	 * returns the point on this ray which is closest to the input ray
	 * @param r
	 * @return
	 */

	public SGVec_3f closestPointToRay3D(sgRayf r) {
		SGVec_3f result = null;
		
		workingVector.set(p2);
		SGVec_3f   u =  workingVector.sub(this.p1);
		workingVector.set(r.p2);
		SGVec_3f   v =  workingVector.sub(r.p1);
		workingVector.set(this.p1);
		SGVec_3f   w =  workingVector.sub(r.p1);
		float    a = u.dot(u);         // always >= 0
		float    b = u.dot(v);
		float    c = v.dot(v);         // always >= 0
		float    d = u.dot(w);
		float    e = v.dot(w);
		float    D = a*c - b*b;        // always >= 0
		float    sc; //tc

		// compute the line parameters of the two closest points
		if (D < Float.MIN_VALUE) {          // the lines are almost parallel
			sc = 0.0f;
			//tc = (b>c ? d/b : e/c);    // use the largest denominator
		}
		else {
			sc = (b*e - c*d) / D;
			//tc = (a*e - b*d) / D;
		}

		result =  this.getRayScaledBy(sc).p2;
		return result;
	}

	public SGVec_3f closestPointToRay3DStrict(sgRayf r) {
		SGVec_3f result = null;

		workingVector.set(p2);
		SGVec_3f   u =  workingVector.sub(this.p1);
		workingVector.set(r.p2);
		SGVec_3f   v =  workingVector.sub(r.p1);
		workingVector.set(this.p1);
		SGVec_3f   w =  workingVector.sub(r.p1);
		float    a = u.dot(u);         // always >= 0
		float    b = u.dot(v);
		float    c = v.dot(v);         // always >= 0
		float    d = u.dot(w);
		float    e = v.dot(w);
		float    D = a*c - b*b;        // always >= 0
		float    sc; //tc

		// compute the line parameters of the two closest points
		if (D < Float.MIN_VALUE) {          // the lines are almost parallel
			sc = 0f;
			//tc = (b>c ? d/b : e/c);    // use the largest denominator
		}
		else {
			sc = (b*e - c*d) / D;
			//tc = (a*e - b*d) / D;
		}

		if(sc < 0 ) result = this.p1;
		else if (sc > 1) result = this.p2;
		else result =  this.getRayScaledBy(sc).p2;	

		return result;
	}

	/**
	 * returns the point on this ray which is closest to 
	 * the input sgRay. If that point lies outside of the bounds
	 * of this ray, returns null. 
	 * @param r
	 * @return
	 */
	public SGVec_3f closestPointToRay3DBounded(sgRayf r) {
		SGVec_3f result = null;

		workingVector.set(p2);
		SGVec_3f   u =  workingVector.sub(this.p1);
		workingVector.set(r.p2);
		SGVec_3f   v =  workingVector.sub(r.p1);
		workingVector.set(this.p1);
		SGVec_3f   w =  workingVector.sub(r.p1);
		float    a = u.dot(u);         // always >= 0
		float    b = u.dot(v);
		float    c = v.dot(v);         // always >= 0
		float    d = u.dot(w);
		float    e = v.dot(w);
		float    D = a*c - b*b;        // always >= 0
		float    sc; //tc

		// compute the line parameters of the two closest points
		if (D < Float.MIN_VALUE) {          // the lines are almost parallel
			sc = 0f;
			//tc = (b>c ? d/b : e/c);    // use the largest denominator
		}
		else {
			sc = (b*e - c*d) / D;
			//tc = (a*e - b*d) / D;
		}

		if(sc < 0 ) result = null;
		else if (sc > 1) result = null;
		else result =  this.getRayScaledBy(sc).p2;	

		return result;
	}

	//returns a ray perpendicular to this ray on the XY plane;
	public sgRayf getPerpendicular2D() {
		SGVec_3f heading = this.heading(); 
		workingVector.set(heading.x-1f, heading.x, 0f);
		SGVec_3f perpHeading = workingVector;
		return new sgRayf(this.p1,  workingVector.add(this.p1));
	}

	public SGVec_3f intercepts2DStrict(sgRayf r) { 
		//will also return null if the intersection does not occur on the 
		//line segment specified by the ray.
		SGVec_3f result =  p1.copy();

		//boolean over = false;
		float a1 = p2.y - p1.y;
		float b1 = p1.x - p2.x;
		float c1 = a1*p1.x + b1*p1.y;

		float a2 = r.p2.y - r.p1.y;
		float b2 = r.p1.x - r.p2.y;
		float c2 = a2* + b2* r.p1.y;

		float det = a1*b2 - a2*b1;
		if(det == 0){
			// Lines are parallel
			return null;
		}
		else {
			result.setX_((b2*c1 - b1*c2)/det);
			result.setY_((a1*c2 - a2*c1)/det);

		}   

		float position = result.dot(this.heading()); 
		if (position > 1 || position < 0) return null;

		return result;
	}

	/**
	 * Given two planes specified by a1,a2,a3 and b1,b2,b3 returns a
	 * ray representing the line along which the two planes intersect
	 * 
	 * @param a1 the first vertex of a triangle on the first plane
	 * @param a2 the second vertex of a triangle on the first plane
	 * @param a3 the third vertex od a triangle on the first plane
	 * @param b1 the first vertex of a triangle on the second plane
	 * @param b2 the second vertex of a triangle on the second plane
	 * @param b3 the third vertex od a triangle on the second plane
	 * @return a sgRay along the line of intersection of these two planes, or null if inputs are coplanar
	 */
	public static sgRayf planePlaneIntersect(SGVec_3f a1, SGVec_3f  a2, SGVec_3f  a3, SGVec_3f  b1, SGVec_3f  b2, SGVec_3f  b3) {
		sgRayf a1a2 = new sgRayf(a1,a2);
		sgRayf a1a3 = new sgRayf(a1,a3); 
		sgRayf a2a3 = new sgRayf(a2,a3);

		SGVec_3f interceptsa1a2 =  a1a2.intersectsPlane(b1, b2, b3);
		SGVec_3f interceptsa1a3 =  a1a3.intersectsPlane(b1, b2, b3);
		SGVec_3f interceptsa2a3 =  a2a3.intersectsPlane(b1, b2, b3);

		SGVec_3f[] notNullCandidates = {interceptsa1a2, interceptsa1a3, interceptsa2a3};
		SGVec_3f notNull1 = null;  
		SGVec_3f notNull2 = null; 

		for(int i=0; i<notNullCandidates.length; i++) {
			if(notNullCandidates[i] != null) {
				if(notNull1 == null) 
					notNull1 =  notNullCandidates[i]; 
				else {
					notNull2 =  notNullCandidates[i];
					break;
				}
			}
		}		
		if(notNull1 != null && notNull2 != null) 
			return new sgRayf(notNull1, notNull2);
		else 
			return null;
	}

	/**
	 * @param ta the first vertex of a triangle on the plane
	 * @param tb the second vertex of a triangle on the plane 
	 * @param tc the third vertex of a triangle on the plane
	 * @return the point where this ray intersects the plane specified by the triangle ta,tb,tc. 
	 */
	public SGVec_3f intersectsPlane(SGVec_3f  ta, SGVec_3f tb, SGVec_3f tc) {
		float[] uvw = new float[3]; 
		return intersectsPlane(ta, tb, tc, uvw);
	}

	
	SGVec_3f tta, ttb, ttc;
	public SGVec_3f intersectsPlane(SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {
		if(tta == null) {
			tta =  ta.copy(); ttb =  tb.copy(); ttc =  tc.copy(); 
		} else {
			tta.set(ta); ttb.set(tb); ttc.set(tc); 
		}
		tta.sub(p1); 
		ttb.sub(p1); 
		ttc.sub(p1);
		
		SGVec_3f result =  planeIntersectTest(tta, ttb, ttc, uvw).copy();
		return  result.add(this.p1);
	}
	
	/**
	 * @param ta the first vertex of a triangle on the plane
	 * @param tb the second vertex of a triangle on the plane 
	 * @param tc the third vertex of a triangle on the plane
	 * @param result the variable in which to hold the result
	 */
	public void intersectsPlane(SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, SGVec_3f result) {
		float[] uvw = new float[3]; 
		result.set(intersectsPlane(ta, tb, tc, uvw));
	}
	
	
	/**
	 * Similar to intersectsPlane, but returns false if intersection does not occur on the triangle strictly defined by ta, tb, and tc
	 * @param ta the first vertex of a triangle on the plane
	 * @param tb the second vertex of a triangle on the plane 
	 * @param tc the third vertex of a triangle on the plane
	 * @param result the variable in which to hold the result
	 */
	public boolean intersectsTriangle(SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, SGVec_3f result) {
		float[] uvw = new float[3]; 
		result.set(intersectsPlane(ta, tb, tc, uvw));
		if(Float.isNaN(uvw[0]) || Float.isNaN(uvw[1]) || Float.isNaN(uvw[2]) || uvw[0] < 0 || uvw[1] < 0 || uvw[2] < 0) 
			return false; 
		else 
			return true;
	}

	SGVec_3f I,u,v,n,dir,w0; 
	boolean inUse = false;
	
	private SGVec_3f planeIntersectTest(SGVec_3f ta, SGVec_3f tb, SGVec_3f tc, float[] uvw) {
		
		if(u== null) {
			u =  tb.copy();
			v =  tc.copy();
			dir = this.heading();
			w0 =  p1.copy(); w0.set(0,0,0);
			I = p1.copy();
		} else {
			u.set(tb); 
			v.set(tc); 
			n.set(0,0,0);
			dir.set(this.heading()); 
			w0.set(0,0,0);
		}
		//SGVec_3f w = new SGVec_3f();
		float  r, a, b;
		u.sub(ta);
		v.sub(ta);
		
		n =  u.crossCopy(v);

		w0.sub(ta);
		a = -(n.dot(w0));
		b = n.dot(dir);
		r = a / b;
		I.set(0,0,0);
		I.set(dir); 
		I.mult(r);
		//float[] barycentric = new float[3]; 
		barycentric(ta, tb, tc, I, uvw);

		
		return I.copy();		
	}

	
	/* Find where this ray intersects a sphere
	 * @param SGVec_3f the center of the sphere to test against.
	 * @param radius radius of the sphere
	 * @param S1 reference to variable in which the first intersection will be placed
	 * @param S2 reference to variable in which the second intersection will be placed
	 * @return number of intersections found;
	 */
	public int intersectsSphere(SGVec_3f sphereCenter, float radius, SGVec_3f S1, SGVec_3f S2) {
		SGVec_3f tp1 =  p1.subCopy(sphereCenter);
		SGVec_3f tp2 =  p2.subCopy(sphereCenter);
		int result = intersectsSphere(tp1, tp2, radius, S1, S2);
		S1.add(sphereCenter); S2.add(sphereCenter);
		return result;
	}
	/* Find where this ray intersects a sphere
	 * @param radius radius of the sphere
	 * @param S1 reference to variable in which the first intersection will be placed
	 * @param S2 reference to variable in which the second intersection will be placed
	 * @return number of intersections found;
	 */
	public int intersectsSphere(SGVec_3f rp1, SGVec_3f rp2, float radius, SGVec_3f S1, SGVec_3f S2) {
		SGVec_3f direction =  rp2.subCopy(rp1);
		SGVec_3f e =  direction.copy();   // e=ray.dir
		e.normalize();                            // e=g/|g|
		SGVec_3f h =  p1.copy();
		h.set(0f,0f,0f);
		h =  h.sub(rp1);  // h=r.o-c.M
		float lf = e.dot(h);                      // lf=e.h
		float s = MathUtils.pow(radius, 2)-h.dot(h)+MathUtils.pow(lf, 2);   // s=r^2-h^2+lf^2
		if (s < 0.0) return 0;                    // no intersection points ?
		s = MathUtils.sqrt(s);                              // s=sqrt(r^2-h^2+lf^2)

		int result = 0;
		if (lf < s) {                               // S1 behind A ?
			if (lf+s >= 0) {                         // S2 before A ?}
				s = -s;                               // swap S1 <-> S2}
				result = 1;                           // one intersection point
			} 
		}else result = 2;                          // 2 intersection points

		S1.set(e.multCopy(lf-s));  
		S1.add(rp1); // S1=A+e*(lf-s)
		S2.set(e.multCopy(lf+s));  
		S2.add(rp1); // S2=A+e*(lf+s)

		// only for testing

		return result;
	}

	SGVec_3f m, at, bt, ct, pt;;
	SGVec_3f bc, ca, ac; 
	public void barycentric(SGVec_3f a, SGVec_3f b, SGVec_3f c, SGVec_3f p, float[] uvw) {
		if(m == null) {
			//m=a.copy();
			//m.set(0f,0f,0f);
			bc =  b.copy();
			ca =  c.copy();
			at = new SGVec_3f(a);
			bt = new SGVec_3f(b);
			ct = new SGVec_3f(c);
			pt = new SGVec_3f(p);
		} else {
			bc.set(b);
			ca.set(a);
			at.set(a);
			bt.set(b);
			ct.set(c);
			pt.set(p);
		}
		
		m = new SGVec_3f(((SGVec_3f)bc.sub(ct)).crossCopy((SGVec_3f)ca.subCopy(at)));

		float nu;
		float nv;
		float ood;

		float x = MathUtils.abs(m.x);
		float y = MathUtils.abs(m.y);
		float z = MathUtils.abs(m.z);

		if (x >= y && x >= z) {
			nu = triArea2D(pt.y, pt.z, bt.y, bt.z, ct.y, ct.z);
			nv = triArea2D(pt.y, pt.z, ct.y, ct.z, at.y, at.z);
			ood = 1.0f / m.x;
		}
		else if (y >= x && y >= z) {
			nu = triArea2D(pt.x, pt.z, bt.x, bt.z, ct.x, ct.z);
			nv = triArea2D(pt.x, pt.z, ct.x, ct.z, at.x, at.z);
			ood = 1.0f / -m.y;
		}
		else {
			nu = triArea2D(pt.x, pt.y, bt.x, bt.y, ct.x, ct.y);
			nv = triArea2D(pt.x, pt.y, ct.x, ct.y, at.x, at.y);
			ood = 1.0f / m.z;
		}
		uvw[0] = nu * ood;
		uvw[1] = nv * ood;
		uvw[2] = 1.0f - uvw[0] - uvw[1];
	}

	@Override
	public String toString() {
		String result = "sgRay " + System.identityHashCode(this) + "\n"
					+"("+(float)this.p1.x +" ->  " +(float)this.p2.x + ") \n " 
					+"("+(float)this.p1.y +" ->  " +(float)this.p2.y + ") \n "
					+"("+(float)this.p1.z +" ->  " +(float)this.p2.z+ ") \n ";
		return result;		
	}

	public static float triArea2D(float x1, float y1, float x2, float y2, float x3, float y3) {
		return (x1 - x2) * (y2 - y3) - (x2 - x3) * (y1 - y2);   
	}


	public void p1(SGVec_3f in) {
		this.p1 =  in.copy();
	}

	public void p2(SGVec_3f in) {
		this.p2 =  in.copy();
	}
	


	public float lerp(float a, float b, float t) {
		return (1-t)*a + t*b;
	}

	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.setJSONArray("p1", p1.toJSONArray());
		result.setJSONArray("p2", p2.toJSONArray());
		return result;
	}

	public SGVec_3f p2() {
		return p2;
	}

	public void setP2(SGVec_3f p2) {
		this.p2 = p2;
	}

	public SGVec_3f p1() {
		return p1;
	}

	public void setP1(SGVec_3f p1) {
		this.p1 = p1;
	}


}


