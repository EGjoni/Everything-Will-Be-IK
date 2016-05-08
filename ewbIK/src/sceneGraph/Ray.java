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
import processing.data.JSONArray;
import processing.data.JSONObject;

/**
 * @author Eron Gjoni
 *
 */
public class Ray {
	public static final int X=0, Y=1, Z=2;
	public DVector p1, p2; 

	public Ray(DVector p1, DVector p2) {
		this.p1 = p1.copy();
		if(p2 != null)
			this.p2 = p2.copy();
	}
	
	public Ray(PVector p1, PVector p2) {
		this.p1 = new DVector(p1);
		if(p2 != null)
			this.p2 = new DVector(p2);
	}
	
	public Ray(JSONObject rayObj) {
		this.p1 = new DVector(rayObj.getJSONArray("p1"));
		this.p2 = new DVector(rayObj.getJSONArray("p2"));
	}
	
	public Ray(JSONArray rayObj) {
		this.p1 = new DVector(rayObj.getJSONArray(0));
		this.p2 = new DVector(rayObj.getJSONArray(1));
	}


	public double distTo(DVector point) { 
		//returns the distance between the input point and the point on this ray to which the input is closest.
		DVector inPoint = point.copy();
		inPoint.sub(this.p1); 
		DVector heading = this.heading();
		double scale = (DVector.dot(inPoint, heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());
		if(scale < 0) {
			return point.dist(this.p1);   
		} else if (scale > 1) {
			return point.dist(this.p2);   
		} else {
			return point.dist(this.getRayScaledBy(scale).p2); 
		}    

	}
	

	public DVector closestPointTo(DVector point) { 
		//returns the distance between the input point and the point on this ray to which the input is closest.
		DVector inPoint = point.copy();
		inPoint.sub(this.p1); 
		DVector heading = this.heading();
		double scale = (DVector.dot(inPoint, heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());

		return this.getRayScaledBy(scale).p2; 
	}
	
	public DVector closestPointToStrict(DVector point) {
		DVector inPoint = point.copy();
		inPoint.sub(this.p1); 
		DVector heading = this.heading();
		double scale = (DVector.dot(inPoint, heading)/(heading.mag()*inPoint.mag()))*(inPoint.mag()/heading.mag());
		
		if(scale <= 0) 
			return this.p1;
		else if (scale >= 1) 
			return this.p2;
		else 
			return this.getRayScaledBy(scale).p2; 
	}

	public DVector heading(){
		if(this.p2 == null) return new DVector(0,0,0);
		else return DVector.sub(p2, p1);
	}

	public void heading(DVector newHead){
		p2 = DVector.add(p1, newHead);
	}
	public void heading(PVector newHead){
		p2 = DVector.add(p1, new DVector(newHead));
	}
	
	/**
	 * @return a copy of this ray with its z-component set to 0;
	 */
	public Ray get2DCopy() {
		return this.get2DCopy(Ray.Z);
	}
	
	/**
	 * gets a copy of this ray, with the component specified by
	 * collapseOnAxis set to 0. 
	 * @param collapseOnAxis the axis on which to collapse the ray.
	 * @return
	 */
	public Ray get2DCopy(int collapseOnAxis) {
		Ray result = this.copy(); 
		if(collapseOnAxis == Ray.X) {
			result.p1.x = 0; 
			result.p2.x = 0;
		}
		if(collapseOnAxis == Ray.Y) {
			result.p1.y = 0; 
			result.p2.y = 0;
		}
		if(collapseOnAxis == Ray.Z) {
			result.p1.z = 0; 
			result.p2.z = 0;
		}
		
		return result;
	}
	
	public DVector origin(){
		return p1.copy();
	}

	public double mag() {
		return  DVector.sub(p2, p1).mag();   
	}

	public void mag(double newMag) {
		DVector dir = DVector.sub(p2, p1);
		dir.setMag(newMag);
		this.heading(dir);   
	}

	public void scale(double scalar) {
		p2.sub(p1); 
		p2.mult(scalar);
		p2.add(p1);
	}

	public DVector getScaledTo(double scalar) {
		DVector result = this.heading().copy();
		result.normalize(); 
		result.mult(scalar);
		result.add(p1); 
		return result;
	}

	public DVector getScaledBy(double scalar) {
		DVector result = this.heading();
		result.mult(scalar);
		result.add(p1); 
		return result;
	}

	public void elongate() {
		Ray reverseRay = new Ray(this.p2.copy(), this.p1.copy()); 
		Ray result = this.getRayScaledTo(900000); 
		reverseRay = reverseRay.getRayScaledTo(900000);
		result.p1 = reverseRay.p2.copy();
		this.p1 = result.p1; 
		this.p2 = result.p2;
	}

	public Ray copy() {
		return new Ray(this.p1, this.p2);  
	}
	
	public void reverse() {
		DVector temp = this.p1; 
		this.p1 = this.p2;
		this.p2 = temp; 
	}
	
	public Ray getReversed() {
		return new Ray(this.p2, this.p1);
	}

	public Ray getRayScaledTo(double scalar) {
		return new Ray(p1, this.getScaledTo(scalar));
	}
	
	/*
	 * reverses this ray's direction so that it 
	 * has a positive dot product with the heading of r
	 * if dot product is already positive, does nothing.
	 */
	public void pointWith(Ray r) {
		if(this.heading().dot(r.heading()) < 0) this.reverse(); 
	}

	public void pointWith(DVector heading) {
		if(this.heading().dot(heading) < 0) this.reverse(); 
	}
	public Ray getRayScaledBy(double scalar) {

		return new Ray(p1, this.getScaledBy(scalar));
	}

	public void contractTo(double percent) {
		//contracts both ends of a ray toward its center such that the total length of the ray
		//is the percent % of its current length; 
		double halfPercent = 1-((1-percent)/2f);
		double p1Tempx = lerp(p1.x, p2.x, halfPercent); 
		double p1Tempy = lerp(p1.y, p2.y, halfPercent);
		double p1Tempz = lerp(p1.z, p2.z, halfPercent);

		double p2Tempx = lerp(p2.x, p1.x, halfPercent);
		double p2Tempy = lerp(p2.y, p1.y, halfPercent);
		double p2Tempz = lerp(p2.z, p1.z, halfPercent);

		p1 = new DVector(p1Tempx, p1Tempy, p1Tempz);
		p2 = new DVector(p2Tempx, p2Tempy, p2Tempz);
	}

	public void translateTo(DVector newLocation) {
		DVector tempHeading = this.heading();
		p1 = newLocation.copy(); 
		this.heading(tempHeading); 
	}

	public void translateTipTo(DVector newLocation) {
		DVector transBy = DVector.sub(newLocation, this.p2); 
		this.translateBy(transBy); 
	}

	public void translateBy(DVector toAdd) {
		p1.add(toAdd); 
		p2.add(toAdd);
	}


	public void normalize() {
		this.mag(1);  
	}
	
	public DVector intercepts2D (Ray r) {
			
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

		    double s, t;
		    s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
		    t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

		    //if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
		       // Collision detected
		       return new DVector(p0_x + (t * s1_x), p0_y + (t * s1_y));
		   // }

		    //return null; // No collision
		}
	
	/*public DVector intercepts2D(Ray r) {
		DVector result = new DVector();

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
	 * If the closest point to this Ray on the input Ray lies
	 * beyond the bounds of that input Ray, this returns closest point
	 * to the input Rays bound; 	
	 * @param r
	 * @return
	 */
	public DVector closestPointToSegment3D(Ray r) {
		DVector closestToThis = r.closestPointToRay3DStrict(this); 
		return this.closestPointTo(closestToThis);
	}
	
	/*public DVector closestPointToSegment3DStrict(Ray r) {
		
	}*/
	
	/**
	 * returns the point on this ray which is closest to the input ray
	 * @param r
	 * @return
	 */

	public DVector closestPointToRay3D(Ray r) {
		DVector result = null;

		DVector   u = DVector.sub(this.p2, this.p1);
		DVector   v = DVector.sub(r.p2, r.p1);
		DVector   w = DVector.sub(this.p1, r.p1);
		double    a = DVector.dot(u,u);         // always >= 0
		double    b = DVector.dot(u,v);
		double    c = DVector.dot(v,v);         // always >= 0
		double    d = DVector.dot(u,w);
		double    e = DVector.dot(v,w);
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

		result = this.getRayScaledBy(sc).p2;
		return result;
	}
	
	public DVector closestPointToRay3DStrict(Ray r) {
		DVector result = null;

		DVector   u = DVector.sub(this.p2, this.p1);
		DVector   v = DVector.sub(r.p2, r.p1);
		DVector   w = DVector.sub(this.p1, r.p1);
		double    a = DVector.dot(u,u);         // always >= 0
		double    b = DVector.dot(u,v);
		double    c = DVector.dot(v,v);         // always >= 0
		double    d = DVector.dot(u,w);
		double    e = DVector.dot(v,w);
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
		else result = this.getRayScaledBy(sc).p2;	
		
		return result;
	}
	
	//returns a ray perpendicular to this ray on the XY plane;
	public Ray getPerpendicular2D() {
		DVector heading = this.heading(); 
		DVector perpHeading = new DVector(heading.y*-1, heading.x);
		return new Ray(this.p1, DVector.add(perpHeading, this.p1));
	}

	public DVector intercepts2DStrict(Ray r) { 
		//will also return null if the intersection does not occur on the 
		//line segment specified by the ray.
		DVector result = new DVector();

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
			result.x = (b2*c1 - b1*c2)/det;
			result.y = (a1*c2 - a2*c1)/det;

		}   

		double position = DVector.dot(result, this.heading()); 
		if (position > 1 || position < 0) return null;

		return result;
	}
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.setJSONArray("p1", p1.toJSONArray());
		result.setJSONArray("p2", p2.toJSONArray());
		return result;
	}
	
	public void p1(PVector in) {
		this.p1 = new DVector(in);
	}
	
	public void p2(PVector in) {
		this.p2 = new DVector(in);
	}
	
	public double lerp(double a, double b, double t) {
		return (1-t)*a + t*b;
	}

	


}


