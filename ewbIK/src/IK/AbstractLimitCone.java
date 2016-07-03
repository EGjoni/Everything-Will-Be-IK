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

public abstract class AbstractLimitCone {

	public DVector controlPoint; 
	public double radius;

	public AbstractKusudama parentKusudama;

	public DVector tangentCircleCenterNext1;
	public DVector tangentCircleCenterNext2;
	public double tangentCircleRadiusNext;

	public DVector tangentCircleCenterPrevious1;
	public DVector tangentCircleCenterPrevious2;
	public double tangentCircleRadiusPrevious;

	public boolean isSelected = false;

	/**
	 * a triangle where the [1] is th tangentCircleNext_n, and [0] and [2] 
	 * are the points at which the tangent circle intersects this limitCone and the
	 * next limitCone
	 */
	public DVector[] firstTriangleNext = new DVector[3];
	public DVector[] secondTriangleNext = new DVector[3];

	public AbstractLimitCone(){}

	public AbstractLimitCone(DVector location, double rad, AbstractKusudama attachedTo) {
		controlPoint = location; 
		radius = Math.max(Double.MIN_VALUE, rad);
		parentKusudama = attachedTo;
	}


	public boolean inBoundsFromThisToNext(AbstractLimitCone next, DVector input, DVector collisionPoint) {
		boolean isInBounds = determineIfInBounds(next, input);
		if(!isInBounds) {
			DVector closestCollision = getClosestCollision(next, input); 
			collisionPoint.x = closestCollision.x; collisionPoint.y = closestCollision.y; collisionPoint.z = closestCollision.z; 
		} else {
			collisionPoint.x = input.x; 
			collisionPoint.y = input.y; 
			collisionPoint.z = input.z;
		}
		return isInBounds;
	}
	
	public DVector getClosestCollision(AbstractLimitCone next, DVector input) {
		DVector result = getOnGreatTangentTriangleSnap(next, input);
		if(result == null) {
			boolean[] inBounds = {false};
			result = closestPointOnClosestCone(next, input, inBounds);
		}
		return result;
	}
	
	public boolean determineIfInBounds(AbstractLimitCone next, DVector input) {
		boolean[] inBounds = {false};
		if(next == null) {
			DVector result = closestToCone(input, inBounds);
			return inBounds[0];
		} else {
			if(!onTangentRadii(input)) {
				if(!onNaivelyInterpolatedPath(next, input)) {
					closestPointOnClosestCone(next, input, inBounds);
					return inBounds[0];
				} else {
					return true;
				}
			} else {	
				return false;
			}
		}
	}

	public DVector getOnGreatTangentTriangleSnap(AbstractLimitCone next, DVector input) {
		DVector result = null;

		double[] onTriangle1 = new double[3];
		DVector tri1Intersect = G.intersectTest(input, this.controlPoint, this.tangentCircleCenterNext1, next.controlPoint, onTriangle1);

		if(tri1Intersect != null 
		&& tri1Intersect.dot(input) > 0 
		&& onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0) {
		
			Rot tan1ToBorder = new Rot(tangentCircleCenterNext1, input); 
			result = new Rot(tan1ToBorder.getAxis(), tangentCircleRadiusNext).applyTo(tangentCircleCenterNext1);
		} else {
			double[] onTriangle2 = new double[3];
			DVector tri2Intersect = G.intersectTest(input, this.controlPoint, this.tangentCircleCenterNext2, next.controlPoint, onTriangle2);

			if(tri2Intersect != null 
			&& tri2Intersect.dot(input) > 0 
			&& onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0) {
			
				Rot tan2ToBorder = new Rot(tangentCircleCenterNext2, input); 
				result = new Rot(tan2ToBorder.getAxis(), tangentCircleRadiusNext).applyTo(tangentCircleCenterNext2);
			}
		}
		
		return result;
	}

	public DVector closestPointOnClosestCone(AbstractLimitCone next, DVector input, boolean[] inBounds) {
		DVector closestToFirst = this.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToFirst; 
		}
		DVector closestToSecond = next.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToSecond; 
		}
		
		double angleToFirst = DVector.angleBetween(input, closestToFirst); 
		double angleToSecond = DVector.angleBetween(input, closestToSecond);

		if(Math.abs(angleToFirst) < Math.abs(angleToSecond)) {
			return closestToFirst;
		} else {
			return closestToSecond;
		}

	}
	
	public DVector closestToCone(DVector input, boolean[] inBounds) {
		Rot pointDiff = new Rot(this.controlPoint, input);
		DVector result = new DVector(0,0,0);
		if(pointDiff.getAngle() < this.radius) {
			inBounds[0] = true;
			return input;
		} else {
			Rot rotTo = new Rot(pointDiff.getAxis(), this.radius); 
			result = rotTo.applyTo(this.controlPoint);
			inBounds[0] = false;
			return result;	
		}
	}


	private boolean onTangentRadii(DVector input) {
		if(Math.abs(DVector.angleBetween(input, tangentCircleCenterNext1)) < tangentCircleRadiusNext) {
			return true; 
		} else if(Math.abs(DVector.angleBetween(input, tangentCircleCenterNext2)) < tangentCircleRadiusNext) {
			return true;
		} else {
			return false;
		}
	}


	private boolean onNaivelyInterpolatedPath(AbstractLimitCone next, DVector input) {
		double[] alongPath = new double[3];
		/*Rot thisToNext = new Rot(this.controlPoint, next.controlPoint); 
		DVector midPoint = new Rot(thisToNext.getAxis(), thisToNext.getAngle()/2d).applyTo(next.controlPoint);*/

		DVector intersectPoint = G.intersectTest(input, firstTriangleNext[0], firstTriangleNext[2], secondTriangleNext[0], alongPath);

		if(intersectPoint != null && 
				alongPath[0] >= 0 && alongPath[1] >= 0 && alongPath[2] >= 0 
				&& intersectPoint.dot(input) > 0) {
			return true;
		}

		intersectPoint = G.intersectTest(input, secondTriangleNext[0], secondTriangleNext[2], firstTriangleNext[2], alongPath);

		if(intersectPoint != null &&
				alongPath[0] >= 0 && alongPath[1] >= 0 && alongPath[2] >= 0 
				&& intersectPoint.dot(input) >0 ) {
			return true;
		} else {
			return false;
		}		
	}

	public void updateTangentHandles(AbstractLimitCone next) {    
		double radA = this.radius;
		double radB = next.radius;

		DVector A = this.controlPoint.copy();
		DVector B = next.controlPoint.copy(); 

		DVector arcNormal = A.cross(B); 
		Rot aToARadian = new Rot(A, arcNormal); 
		DVector aToARadianAxis = aToARadian.getAxis();
		Rot bToBRadian = new Rot(B, arcNormal); 
		DVector bToBRadianAxis = bToBRadian.getAxis();
		aToARadian = new Rot(aToARadianAxis, radA);
		bToBRadian = new Rot(bToBRadianAxis, radB);

		double tRadius = ((Math.PI)-(radA+radB))/2f; 

		double minorAppoloniusRadiusA = radA + tRadius;
		DVector minorAppoloniusAxisA = A.copy().normalize(); 
		double minorAppoloniusRadiusB = radB + tRadius;
		DVector minorAppoloniusAxisB  = B.copy().normalize();

		DVector minorAppoloniusP1A = DVector.mult(minorAppoloniusAxisA, G.cos(minorAppoloniusRadiusA));
		DVector minorAppoloniusP2A = new Rot(arcNormal, minorAppoloniusRadiusA).applyTo(minorAppoloniusAxisA);
		DVector minorAppoloniusP3A = new Rot(minorAppoloniusAxisA, Math.PI/2d).applyTo(minorAppoloniusP2A);

		DVector minorAppoloniusP1B = DVector.mult(minorAppoloniusAxisB, G.cos(minorAppoloniusRadiusB));
		DVector minorAppoloniusP2B = new Rot(arcNormal, minorAppoloniusRadiusB).applyTo(minorAppoloniusAxisB);
		DVector minorAppoloniusP3B = new Rot(minorAppoloniusAxisB, Math.PI/2d).applyTo(minorAppoloniusP2B);      

		Ray r1B = new Ray(minorAppoloniusP2B, minorAppoloniusP1B); r1B.elongate();
		Ray r2B = new Ray(minorAppoloniusP2B, minorAppoloniusP3B); r2B.elongate();

		DVector intersection1 = G.intersectTest(r1B, minorAppoloniusP1A, minorAppoloniusP2A, minorAppoloniusP3A);
		DVector intersection2 = G.intersectTest(r2B, minorAppoloniusP1A, minorAppoloniusP2A, minorAppoloniusP3A);

		Ray intersectionRay = new Ray(DVector.mult(intersection1, parentKusudama.attachedTo.boneHeight), DVector.mult(intersection2, parentKusudama.attachedTo.boneHeight));
		intersectionRay.elongate();

		DVector sphereIntersect1 = new DVector(); 
		DVector sphereIntersect2 = new DVector();
		G.raySphereIntersection(intersectionRay, parentKusudama.attachedTo.boneHeight, sphereIntersect1, sphereIntersect2);  


		this.tangentCircleCenterNext1 = sphereIntersect1; 
		this.tangentCircleCenterNext2 = sphereIntersect2; 
		this.tangentCircleRadiusNext = tRadius;

		next.tangentCircleCenterPrevious1 = sphereIntersect1; 
		next.tangentCircleCenterPrevious2 = sphereIntersect2;
		next.tangentCircleRadiusPrevious = tRadius;

		computeTriangles(next); 
	}

	private void computeTriangles(AbstractLimitCone next) {
		firstTriangleNext[1] = this.tangentCircleCenterNext1;		
		Rot tangentNext1ToThisLimitConeCenter = new Rot(tangentCircleCenterNext1, this.controlPoint);
		Rot tangentNext1ToThisLimitConeBoundary = new Rot(tangentNext1ToThisLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		firstTriangleNext[0] = tangentNext1ToThisLimitConeBoundary.applyTo(tangentCircleCenterNext1);

		Rot tangentNext1ToNextLimitConeCenter = new Rot(tangentCircleCenterNext1, next.controlPoint);
		Rot tangentNext1ToNextLimitConeBoundary = new Rot(tangentNext1ToNextLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		firstTriangleNext[2] = tangentNext1ToNextLimitConeBoundary.applyTo(tangentCircleCenterNext1);

		secondTriangleNext[1] = this.tangentCircleCenterNext2;

		Rot tangentNext2ToThisLimitConeCenter = new Rot(tangentCircleCenterNext2, this.controlPoint);
		Rot tangentNext2ToThisLimitConeBoundary = new Rot(tangentNext2ToThisLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		secondTriangleNext[0] = tangentNext2ToThisLimitConeBoundary.applyTo(tangentCircleCenterNext2);

		Rot tangentNext2ToNextLimitConeCenter = new Rot(tangentCircleCenterNext2, next.controlPoint);
		Rot tangentNext2ToNextLimitConeBoundary = new Rot(tangentNext2ToNextLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		secondTriangleNext[2] = tangentNext2ToNextLimitConeBoundary.applyTo(tangentCircleCenterNext2);		
	}
}
