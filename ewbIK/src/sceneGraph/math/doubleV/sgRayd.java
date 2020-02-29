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


package math.doubleV;

import asj.CanLoad;
import asj.data.JSONObject;
import math.floatV.SGVec_3f;
import math.floatV.Vec3f;

/**
 * @author Eron Gjoni
 *
 */
public class sgRayd implements CanLoad {
	public static final int X=0, Y=1, Z=2;
	protected Vec3d<?>  p1;
	protected Vec3d<?> p2; 

	public sgRayd() {
		workingVector = new SGVec_3d();
		//this.p1 = new SGVec_3d();
	}
	
	public sgRayd(Vec3d<?> origin) {
		this.workingVector =  origin.copy();
		this.p1 =  origin.copy();
	}

	public sgRayd(Vec3d<?> p1, Vec3d<?> p2) {
		this.workingVector =  p1.copy();
		this.p1 =  p1.copy();
		if(p2 != null)
			this.p2 =  p2.copy();
	}	


	public <V extends Vec3d<?>> double distTo(V point) { 

		Vec3d<?> inPoint =  point.copy();
		inPoint.sub(this.p1); 
		Vec3d<?> heading =  this.heading();
		double scale = (inPoint.dot(heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());

		return point.dist(this.getRayScaledBy(scale).p2); 
	}

	/**
	 * returns the distance between the input point and the point on this ray (treated as a lineSegment) to which the input is closest.
	 * @param point
	 * @return
	 */
	public <V extends Vec3d<?>> double distToStrict(V point) { 

		Vec3d<?> inPoint =  point.copy();
		inPoint.sub(this.p1); 
		Vec3d<?> heading = this.heading();
		double scale = (inPoint.dot(heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());
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
	public double distTo(sgRayd r) {
		Vec3d<?> closestOnThis = this.closestPointToRay3D(r);
		return r.distTo(closestOnThis);
	}

	/**
	 * returns the distance between this ray as a line segment, and the input ray treated as a line segment
	 */	
	public double distToStrict(sgRayd r) {
		Vec3d<?> closestOnThis = this.closestPointToSegment3D(r);
		return closestOnThis.dist(r.closestPointToStrict(closestOnThis));
	}

	/**
	 * returns the point on this sgRay which is closest to the input point
	 * @param point
	 * @return
	 */
	public <V extends Vec3d<?>> V closestPointTo(V point) { 

		workingVector.set(point);
		workingVector.sub(this.p1); 
		Vec3d<?> heading = this.heading();
		heading.mag();
		workingVector.mag();
		//workingVector.normalize();
		heading.normalize();
		double scale = workingVector.dot(heading);


		return (V) this.getScaledTo(scale);
	}

	public <V extends Vec3d<?>> Vec3d<?> closestPointToStrict(V point) {
		V inPoint =  (V) point.copy();
		inPoint.sub(this.p1); 
		V heading = (V) this.heading();
		double scale = (inPoint.dot(heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());

		if(scale <= 0) 
			return this.p1;
		else if (scale >= 1) 
			return this.p2;
		else 
			return this.getMultipledBy(scale); 
	}

	public Vec3d<?> heading(){
		if(this.p2 == null) {
			if(p1 == null) p1 = new SGVec_3d();
			p2 =   p1.copy();
			p2.set(0d,0d,0d);
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
	public void alignTo(sgRayd target) {
		p1.set(target.p1);
		p2.set(target.p2);
	}
	
	public void heading(double[] newHead){
		if(p2 == null) p2 =  p1.copy();
		p2.set(newHead);
		p2.set(p1);		
	}

	public  <V extends Vec3d<?>> void heading(V newHead){
		if(p2 == null) p2 =  p1.copy();
		p2.set(p1);
		p2.add(newHead);
	}
	public void heading(SGVec_3f newHead){
		if(p2 == null) p2 =  p1.copy();
		p2.set(p1);
		p2.add(newHead);
	}

	


	/**
	 * sets the input vector equal to this sgRay's heading.
	 * @param setTo
	 */
	public void getHeading(SGVec_3d setTo){
		setTo.set(p2);
		setTo.sub(this.p1);
	}


	/**
	 * @return a copy of this ray with its z-component set to 0;
	 */
	public sgRayd get2DCopy() {
		return this.get2DCopy(sgRayd.Z);
	}

	/**
	 * gets a copy of this ray, with the component specified by
	 * collapseOnAxis set to 0. 
	 * @param collapseOnAxis the axis on which to collapse the ray.
	 * @return
	 */
	public sgRayd get2DCopy(int collapseOnAxis) {
		sgRayd result = this.copy(); 
		if(collapseOnAxis == sgRayd.X) {
			result.p1.setX_(0); 
			result.p2.setX_(0);
		}
		if(collapseOnAxis == sgRayd.Y) {
			result.p1.setY_(0);
			result.p2.setY_(0);
		}
		if(collapseOnAxis == sgRayd.Z) {
			result.p1.setZ_(0);
			result.p2.setZ_(0);
		}

		return result;
	}

	public Vec3d<?> origin(){
		return  p1.copy();
	}

	public double mag() {
		workingVector.set(p2);
		return  (workingVector.sub(p1)).mag();   
	}

	public void mag(double newMag) {
		workingVector.set(p2);
		Vec3d<?> dir =  workingVector.sub(p1);
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
	public double scaledProjection(SGVec_3d input) {
		workingVector.set(input);
		workingVector.sub(this.p1); 
		 Vec3d<?> heading = this.heading();
		double headingMag = heading.mag();
		double workingVectorMag = workingVector.mag();
		if(workingVectorMag == 0 || headingMag == 0) 
			return 0;
		else 
			return (workingVector.dot(heading)/(headingMag*workingVectorMag))*(workingVectorMag/headingMag);
	}


	protected Vec3d<?> workingVector; 



	/**
	 * divides the ray by the amount specified by divisor, such that the 
	 * base of the ray remains where it is, and the tip
	 * is scaled accordinly.  
	 * @param divisor
	 */
	public void div(double divisor) {
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
	public void mult(double scalar) {
		p2.sub(p1); 
		p2.mult(scalar);
		p2.add(p1);
	}


	/**
	 * Returns a SGVec_3d representing where the tip
	 * of this ray would be if mult() was called on the ray
	 * with scalar as the parameter. 
	 * @param scalar
	 * @return
	 */
	public Vec3d<?> getMultipledBy(double scalar) {
		 Vec3d<?> result = this.heading();
		result.mult(scalar);
		result.add(p1); 
		return result;
	}


	/**
	 * Returns a SGVec_3d representing where the tip
	 * of this ray would be if div() was called on the ray
	 * with scalar as the parameter. 
	 * @param scalar
	 * @return
	 */
	public Vec3d<?> getDivideddBy(double divisor) {
		 Vec3d<?> result =  this.heading().copy();
		result.mult(divisor);
		result.add(p1); 
		return result;
	}


	/**
	 * Returns a SGVec_3d representing where the tip
	 * of this ray would be if mag(scale) was called on the ray
	 * with scalar as the parameter. 
	 * @param scalar
	 * @return
	 */
	public Vec3d<?> getScaledTo(double scale) {
		Vec3d<?> result =  this.heading().copy();
		result.normalize(); 
		result.mult(scale);
		result.add(p1); 
		return result;
	}


	/**
	 *  adds the specified length to the ray in both directions.
	 */
		public void elongate(float amt) {
			Vec3d midPoint = p1.addCopy(p2).multCopy(0.5d);
			Vec3d p1Heading = p1.subCopy(midPoint);
			Vec3d p2Heading = p2.subCopy(midPoint);
			Vec3d p1Add = (Vec3d) p1Heading.copy().normalize().mult(amt);
			Vec3d p2Add = (Vec3d) p2Heading.copy().normalize().mult(amt);
			
			this.p1.set((Vec3d)p1Heading.addCopy(p1Add).addCopy(midPoint)); 
			this.p2.set((Vec3d)p2Heading.addCopy(p2Add).addCopy(midPoint));
		}


	public sgRayd copy() {
		return new sgRayd(this.p1, this.p2);  
	}

	public void reverse() {
		 Vec3d<?> temp = this.p1; 
		this.p1 = this.p2;
		this.p2 = temp; 
	}

	public sgRayd getReversed() {
		return new sgRayd(this.p2, this.p1);
	}

	public sgRayd getRayScaledTo(double scalar) {
		return new sgRayd(p1, this.getScaledTo(scalar));
	}

	/*
	 * reverses this ray's direction so that it 
	 * has a positive dot product with the heading of r
	 * if dot product is already positive, does nothing.
	 */
	public void pointWith(sgRayd r) {
		if(this.heading().dot(r.heading()) < 0) this.reverse(); 
	}

	public void pointWith(SGVec_3d heading) {
		if(this.heading().dot(heading) < 0) this.reverse(); 
	}
	public sgRayd getRayScaledBy(double scalar) {
		return new sgRayd(p1, this.getMultipledBy(scalar));
	}
	
	/**
	 * sets the values of the given vector to where the 
	 * tip of this Ray would be if the ray were inverted
	 * @param vec
	 * @return the vector that was passed in after modification (for chaining) 
	 */
	public Vec3d<?> setToInvertedTip(Vec3d<?> vec) {
		vec.x = (p1.x - p2.x)+p1.x; 
		vec.y = (p1.y - p2.y)+p1.y; 
		vec.z = (p1.z - p2.z)+p1.z; 
		return vec;
	}

	public void contractTo(double percent) {
		//contracts both ends of a ray toward its center such that the total length of the ray
		//is the percent % of its current length; 
		double halfPercent = 1-((1-percent)/2f);

		p1 =  p1.lerp(p2, halfPercent);//)new SGVec_3d(p1Tempx, p1Tempy, p1Tempz);
		p2 =  p2.lerp(p1, halfPercent);//new SGVec_3d(p2Tempx, p2Tempy, p2Tempz);
	}

	public void translateTo(SGVec_3d newLocation) {

		workingVector.set(p2);
		workingVector.sub(p1);
		workingVector.add(newLocation);
		p2.set(workingVector);
		p1.set(newLocation);
	}

	public void translateTipTo(SGVec_3d newLocation) {
		workingVector.set(newLocation);
		 Vec3d<?> transBy =  workingVector.sub(p2); 
		this.translateBy(transBy); 
	}

	public  <V extends Vec3d<?>> void translateBy(V toAdd) {
		p1.add(toAdd); 
		p2.add(toAdd);
	}


	public void normalize() {
		this.mag(1);  
	}

	public Vec3d<?> intercepts2D (sgRayd r) {
		Vec3d<?> result =  p1.copy();
		
		double p0_x = this.p1.x;
		double p0_y = this.p1.y; 
		double p1_x = this.p2.x;
		double p1_y = this.p2.y;

		double p2_x = r.p1.x;
		double p2_y = r.p1.y;
		double p3_x = r.p2.x; 
		double p3_y = r.p2.y;

		double s1_x, s1_y, s2_x, s2_y;
		s1_x = p1_x - p0_x;     s1_y = p1_y - p0_y;
		s2_x = p3_x - p2_x;     s2_y = p3_y - p2_y;

		double t;
		t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

		//if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
		// Collision detected
		return  result.set(p0_x + (t * s1_x), p0_y + (t * s1_y), 0d);
		// }

		//return null; // No collision
	}

	/*public Vec3d<?> intercepts2D(sgRay r) {
		SGVec_3d result = new SGVec_3d();

		double a1 = p2.y - p1.y;
		double b1 = p1.x - p2.x;
		double c1 = a1*p1.x + b1*p1.y;

		double a2 = r.p2.y - r.p1.y;
		double b2 = r.p1.x - r.p2.y;
		double c2 = a2* + b2* r.p1.y;

		double det = a1*b2 - a2*b1;
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
	public Vec3d<?> closestPointToSegment3D(sgRayd r) {
		Vec3d<?> closestToThis =  r.closestPointToRay3DStrict(this); 
		return this.closestPointTo(closestToThis);
	}

	/*public Vec3d<?> closestPointToSegment3DStrict(sgRay r) {

	}*/

	/**
	 * returns the point on this ray which is closest to the input ray
	 * @param r
	 * @return
	 */

	public Vec3d<?> closestPointToRay3D(sgRayd r) {
		Vec3d<?> result = null;
		
		workingVector.set(p2);
		Vec3d<?>   u =  workingVector.sub(this.p1);
		workingVector.set(r.p2);
		Vec3d<?>   v =  workingVector.sub(r.p1);
		workingVector.set(this.p1);
		Vec3d<?>   w =  workingVector.sub(r.p1);
		double    a = u.dot(u);         // always >= 0
		double    b = u.dot(v);
		double    c = v.dot(v);         // always >= 0
		double    d = u.dot(w);
		double    e = v.dot(w);
		double    D = a*c - b*b;        // always >= 0
		double    sc; //tc

		// compute the line parameters of the two closest points
		if (D < Double.MIN_VALUE) {          // the lines are almost parallel
			sc = 0.0;
			//tc = (b>c ? d/b : e/c);    // use the largest denominator
		}
		else {
			sc = (b*e - c*d) / D;
			//tc = (a*e - b*d) / D;
		}

		result =  this.getRayScaledBy(sc).p2;
		return result;
	}

	public Vec3d<?> closestPointToRay3DStrict(sgRayd r) {
		Vec3d<?> result = null;

		workingVector.set(p2);
		Vec3d<?>   u =  workingVector.sub(this.p1);
		workingVector.set(r.p2);
		Vec3d<?>   v =  workingVector.sub(r.p1);
		workingVector.set(this.p1);
		Vec3d<?>   w =  workingVector.sub(r.p1);
		double    a = u.dot(u);         // always >= 0
		double    b = u.dot(v);
		double    c = v.dot(v);         // always >= 0
		double    d = u.dot(w);
		double    e = v.dot(w);
		double    D = a*c - b*b;        // always >= 0
		double    sc; //tc

		// compute the line parameters of the two closest points
		if (D < Double.MIN_VALUE) {          // the lines are almost parallel
			sc = 0.0;
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
	public Vec3d<?> closestPointToRay3DBounded(sgRayd r) {
		Vec3d<?> result = null;

		workingVector.set(p2);
		Vec3d<?>   u =  workingVector.sub(this.p1);
		workingVector.set(r.p2);
		Vec3d<?>   v =  workingVector.sub(r.p1);
		workingVector.set(this.p1);
		Vec3d<?>   w =  workingVector.sub(r.p1);
		double    a = u.dot(u);         // always >= 0
		double    b = u.dot(v);
		double    c = v.dot(v);         // always >= 0
		double    d = u.dot(w);
		double    e = v.dot(w);
		double    D = a*c - b*b;        // always >= 0
		double    sc; //tc

		// compute the line parameters of the two closest points
		if (D < Double.MIN_VALUE) {          // the lines are almost parallel
			sc = 0.0;
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
	public sgRayd getPerpendicular2D() {
		Vec3d<?> heading = this.heading(); 
		workingVector.set(heading.x-1d, heading.x, 0d);
		return new sgRayd(this.p1,  workingVector.add(this.p1));
	}

	public Vec3d<?> intercepts2DStrict(sgRayd r) { 
		//will also return null if the intersection does not occur on the 
		//line segment specified by the ray.
		Vec3d<?> result =  p1.copy();

		//boolean over = false;
		double a1 = p2.y - p1.y;
		double b1 = p1.x - p2.x;
		double c1 = a1*p1.x + b1*p1.y;

		double a2 = r.p2.y - r.p1.y;
		double b2 = r.p1.x - r.p2.y;
		double c2 = a2* + b2* r.p1.y;

		double det = a1*b2 - a2*b1;
		if(det == 0){
			// Lines are parallel
			return null;
		}
		else {
			result.setX_((b2*c1 - b1*c2)/det);
			result.setY_((a1*c2 - a2*c1)/det);

		}   

		double position = result.dot(this.heading()); 
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
	public static <V extends Vec3d<?>> sgRayd planePlaneIntersect(V a1, V  a2, V  a3, V  b1, V  b2, V  b3) {
		sgRayd a1a2 = new sgRayd(a1,a2);
		sgRayd a1a3 = new sgRayd(a1,a3); 
		sgRayd a2a3 = new sgRayd(a2,a3);

		Vec3d<?> interceptsa1a2 =  a1a2.intersectsPlane(b1, b2, b3);
		Vec3d<?> interceptsa1a3 =  a1a3.intersectsPlane(b1, b2, b3);
		Vec3d<?> interceptsa2a3 =  a2a3.intersectsPlane(b1, b2, b3);

		Vec3d<?>[] notNullCandidates = {interceptsa1a2, interceptsa1a3, interceptsa2a3};
		Vec3d<?> notNull1 = null;  
		Vec3d<?> notNull2 = null; 

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
			return new sgRayd(notNull1, notNull2);
		else 
			return null;
	}

	/**
	 * @param ta the first vertex of a triangle on the plane
	 * @param tb the second vertex of a triangle on the plane 
	 * @param tc the third vertex of a triangle on the plane
	 * @return the point where this ray intersects the plane specified by the triangle ta,tb,tc. 
	 */
	public  <V extends Vec3d<?>> Vec3d<?> intersectsPlane(V  ta, V tb, V tc) {
		double[] uvw = new double[3]; 
		return intersectsPlane(ta, tb, tc, uvw);
	}

	
	Vec3d<?> tta, ttb, ttc;
	public  <V extends Vec3d<?>> Vec3d<?>  intersectsPlane(V ta, V tb, V tc, double[] uvw) {
		if(tta == null) {
			tta =  ta.copy(); ttb =  tb.copy(); ttc =  tc.copy(); 
		} else {
			tta.set(ta); ttb.set(tb); ttc.set(tc); 
		}
		tta.sub(p1); 
		ttb.sub(p1); 
		ttc.sub(p1);
		
		Vec3d<?> result =  (V) planeIntersectTest(tta, ttb, ttc, uvw).copy();
		return  result.add(this.p1);
	}
	
	/**
	 * @param ta the first vertex of a triangle on the plane
	 * @param tb the second vertex of a triangle on the plane 
	 * @param tc the third vertex of a triangle on the plane
	 * @param result the variable in which to hold the result
	 */
	public void intersectsPlane(Vec3d<?> ta, Vec3d<?> tb, Vec3d<?> tc, Vec3d<?> result) {
		double[] uvw = new double[3]; 
		result.set(intersectsPlane(ta, tb, tc, uvw));
	}
	
	
	/**
	 * Similar to intersectsPlane, but returns false if intersection does not occur on the triangle strictly defined by ta, tb, and tc
	 * @param ta the first vertex of a triangle on the plane
	 * @param tb the second vertex of a triangle on the plane 
	 * @param tc the third vertex of a triangle on the plane
	 * @param result the variable in which to hold the result
	 */
	public <V extends Vec3d<?>>boolean intersectsTriangle(V ta, V tb, V tc, V result) {
		double[] uvw = new double[3]; 
		result.set(intersectsPlane(ta, tb, tc, uvw));
		if(Double.isNaN(uvw[0]) || Double.isNaN(uvw[1]) || Double.isNaN(uvw[2]) || uvw[0] < 0 || uvw[1] < 0 || uvw[2] < 0) 
			return false; 
		else 
			return true;
	}

	Vec3d<?> I,u,v,n,dir,w0; 
	boolean inUse = false;
	
	private <V extends Vec3d<?>> V planeIntersectTest(V ta, V tb, V tc, double[] uvw) {
		
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
		//SGVec_3d w = new SGVec_3d();
		double  r, a, b;
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
		//double[] barycentric = new double[3]; 
		barycentric(ta, tb, tc, I, uvw);
	
		return (V) I.copy();		
	}

	
	/* Find where this ray intersects a sphere
	 * @param SGVec_3d the center of the sphere to test against.
	 * @param radius radius of the sphere
	 * @param S1 reference to variable in which the first intersection will be placed
	 * @param S2 reference to variable in which the second intersection will be placed
	 * @return number of intersections found;
	 */
	public <V extends Vec3d<?>> int intersectsSphere(V sphereCenter, double radius, V S1, V S2) {
		Vec3d<?> tp1 =  p1.subCopy(sphereCenter);
		Vec3d<?> tp2 =  p2.subCopy(sphereCenter);
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
	public <V extends Vec3d<?>> int intersectsSphere(V rp1, V rp2, double radius, V S1, V S2) {
		V direction =  (V) rp2.subCopy(rp1);
		V e =  (V) direction.copy();   // e=ray.dir
		e.normalize();                            // e=g/|g|
		V h =  (V) p1.copy();
		h.set(0d,0d,0d);
		h =  (V) h.sub(rp1);  // h=r.o-c.M
		double lf = e.dot(h);                      // lf=e.h
		double radpow = radius*radius;
		double hdh = h.magSq(); 
		double lfpow = lf*lf;		
		double s = radpow-hdh+lfpow;   // s=r^2-h^2+lf^2
		if (s < 0.0) return 0;                    // no intersection points ?
		s = Math.sqrt(s);                              // s=sqrt(r^2-h^2+lf^2)

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

    Vec3d<?> m, at, bt, ct, pt;;
    Vec3d<?> bc, ca, ac; 
	public <V extends Vec3d<?>> void barycentric(V a, V b, V c, V p, double[] uvw) {
		if(m == null) {
			//m=a.copy();
			//m.set(0d,0d,0d);
			bc =  b.copy();
			ca =  c.copy();
			at = a.copy();
			bt = b.copy();
			ct = c.copy();
			pt = p.copy();
		} else {
			bc.set(b);
			ca.set(a);
			at.set(a);
			bt.set(b);
			ct.set(c);
			pt.set(p);
		}
		
		m = new SGVec_3d(((SGVec_3d)bc.subCopy(ct)).crossCopy((SGVec_3d)ca.subCopy(at)));

		double nu;
		double nv;
		double ood;

		double x = Math.abs(m.x);
		double y = Math.abs(m.y);
		double z = Math.abs(m.z);

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

	public static double triArea2D(double x1, double y1, double x2, double y2, double x3, double y3) {
		return (x1 - x2) * (y2 - y3) - (x2 - x3) * (y1 - y2);   
	}


	public <V extends Vec3d<?>> void p1(V in) {
		this.p1 =  in.copy();
	}

	public <V extends Vec3d<?>> void p2(V in) {
		this.p2 =  in.copy();
	}
	


	public double lerp(double a, double b, double t) {
		return (1-t)*a + t*b;
	}


	public Vec3d<?> p2() {
		return p2;
	}
	
	public <R extends sgRayd>void set(R r) {
		this.p1.set(r.p1);
		this.p2.set(r.p2);
	}

	public <V extends Vec3d<?>> void setP2(V p2) {
		this.p2 = p2;
	}

	public Vec3d<?> p1() {
		return p1;
	}

	public <V extends Vec3d<?>> void setP1(V p1) {
		this.p1 = p1;
	}

	@Override
	public CanLoad populateSelfFromJSON(JSONObject j) {
		if(this.p1 != null) this.p2 = this.p1.copy();
		if(this.p2 != null) this.p1 = this.p2.copy();
		
		if(this.p1 == null)
			this.p1 = new SGVec_3d(j.getJSONObject("p1"));
		else { 
			this.p1.set(new SGVec_3d(j.getJSONObject("p1")));
		}
		
		if(this.p2 == null) 
			this.p2 = new SGVec_3d(j.getJSONObject("p2"));
		else 
			this.p2.set(new SGVec_3d(j.getJSONObject("p2")));
		
		return this;
	}

	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		result.setJSONObject("p1", p1.toJSONObject());
		result.setJSONObject("p2", p2.toJSONObject());
		return result;
	}


}


