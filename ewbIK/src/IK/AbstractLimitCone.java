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

	public AbstractLimitCone(DVector location, double rad, AbstractKusudama attachedTo) {
		controlPoint = location; 
		radius = Math.max(Double.MIN_VALUE, rad);
		parentKusudama = attachedTo;
	}


	public boolean inBounds(DVector input, DVector output) {
		Rot pointDiff = new Rot(this.controlPoint, input);
		if(pointDiff.getAngle() < this.radius) {
			output = input; 
			return true;
		} else {
			Rot rotTo = new Rot(pointDiff.getAxis(), this.radius); 
			output = rotTo.applyTo(input);
			return false;	
		}
	}

	public boolean inBoundsFromThisToNext(AbstractLimitCone next, DVector input, DVector collisionPoint) {

		if(next == null) {
			return inBounds(input, collisionPoint);
		} else {
			DVector tangentThisBisector = G.closestPointOnGreatCircle(this.controlPoint, 
					next.controlPoint, 
					new Rot(
							new Rot(
									this.controlPoint, this.tangentCircleCenterNext1
									).getAxis(), 
							this.radius
							).applyTo(this.controlPoint)
					).normalize();
			
			DVector tangentNextBisector = G.closestPointOnGreatCircle(next.controlPoint, this.controlPoint, new Rot(new Rot(next.controlPoint, this.tangentCircleCenterNext1).getAxis(), next.radius).applyTo(next.controlPoint)).normalize();
			DVector closestPointOnSegment = G.closestPointOnGreatCircle(this.controlPoint, next.controlPoint, input).normalize();


			if((DVector.angleBetween(input, this.controlPoint) < this.radius || DVector.angleBetween(input, next.controlPoint) < next.radius)) {
				collisionPoint = input;
				return true;  
			} else {

				boolean withinBisectorBounds = false; 

				if(DVector.angleBetween(closestPointOnSegment,  tangentThisBisector) < DVector.angleBetween(tangentNextBisector, tangentThisBisector) 
						&& DVector.angleBetween(closestPointOnSegment, tangentNextBisector) < DVector.angleBetween(tangentNextBisector, tangentThisBisector)) {
					Rot dirCheckThis = new Rot(this.controlPoint, closestPointOnSegment);
					Rot dirCheckNext = new Rot(this.controlPoint, next.controlPoint); 
					if(dirCheckThis.getAxis().dot(dirCheckNext.getAxis()) > 0) {
						withinBisectorBounds = true;  
					}

				}
				//double segmentRadius = DVector.angleBetween(this.controlPoint, next.controlPoint);

				double distFromThis = DVector.angleBetween(closestPointOnSegment, this.controlPoint);
				double distFromNext = DVector.angleBetween(closestPointOnSegment, next.controlPoint);

				if(DVector.angleBetween(this.tangentCircleCenterNext1, input) > tangentCircleRadiusNext 
						&& DVector.angleBetween(this.tangentCircleCenterNext2, input) > tangentCircleRadiusNext
						&& withinBisectorBounds) {
					return true;      
				} else if (withinBisectorBounds) {
					double distFromTangent1 = DVector.angleBetween(this.tangentCircleCenterNext1, input);
					double distFromTangent2 = DVector.angleBetween(this.tangentCircleCenterNext2, input);
					if(distFromTangent1 < distFromTangent2) {
						collisionPoint.set(new Rot(new Rot(this.tangentCircleCenterNext1, input).getAxis(), this.tangentCircleRadiusNext).applyTo(tangentCircleCenterNext1));
					} else {
						collisionPoint.set(new Rot(new Rot(this.tangentCircleCenterNext2, input).getAxis(), this.tangentCircleRadiusNext).applyTo(tangentCircleCenterNext2));
					}

					return false;  
				} else  {
					if(distFromNext < distFromThis) { 
						collisionPoint.set(new Rot(new Rot(next.controlPoint, input).getAxis(), next.radius).applyTo(next.controlPoint));
					} else {
						collisionPoint.set(new Rot(new Rot(this.controlPoint, input).getAxis(), this.radius).applyTo(this.controlPoint));
					}
					return false;  
				}
			}
		}

	}

	public void updateTangentHandles(AbstractLimitCone next) {    
		DVector inA = this.controlPoint;
		double radA = this.radius;
		DVector inB = next.controlPoint;
		double radB = next.radius;


		DVector aProjected = DVector.mult(inA, G.cos(radA));
		DVector bProjected = DVector.mult(inB, G.cos(radB));

		DVector A = inA.copy();
		DVector B = inB.copy(); 

		DVector arcNormal = A.cross(B); 
		Rot aToARadian = new Rot(A, arcNormal); 
		DVector aToARadianAxis = aToARadian.getAxis();
		Rot bToBRadian = new Rot(B, arcNormal); 
		DVector bToBRadianAxis = bToBRadian.getAxis();
		aToARadian = new Rot(aToARadianAxis, radA);
		bToBRadian = new Rot(bToBRadianAxis, radB);

		DVector aRadP = aToARadian.applyTo(A.copy());

		double aRadianProjectedLength = aProjected.dist(aRadP); 

		double aToBEuclidianLength = aProjected.dist(bProjected);

		double tangentLength = G.sqrt((aToBEuclidianLength*aToBEuclidianLength) - (aRadianProjectedLength*aRadianProjectedLength));

		Ray projectedAToBRay = new Ray(aProjected, bProjected);
		Ray projectedAToARadRay = new Ray(aProjected, aRadP);


		DVector euclidianPlaneNormal = projectedAToBRay.heading().cross(projectedAToARadRay.heading());
		DVector tangentHeading = euclidianPlaneNormal.cross(projectedAToARadRay.heading());
		if (tangentHeading.dot(projectedAToBRay.heading()) < 0) tangentHeading.mult(-1); 
		tangentHeading.normalize();
		tangentHeading.mult(tangentLength); 
		Ray tangentRay = new Ray(aRadP, null);
		tangentRay.heading(tangentHeading);


		Ray aProjectedCenterToATangentTip = new Ray(aProjected, tangentRay.p2);
		Rot rotateTangent = new Rot(aProjectedCenterToATangentTip.heading(), projectedAToBRay.heading()); 

		tangentRay.p1 = rotateTangent.applyTo(DVector.sub(tangentRay.p1, aProjected));
		tangentRay.p2 = rotateTangent.applyTo(DVector.sub(tangentRay.p2, aProjected));

		tangentRay.translateBy(A);

		Rot aToTangentBase = new Rot(A, tangentRay.p1);
		DVector aFullRad = new Rot(aToTangentBase.getAxis(), radB).applyTo(tangentRay.p1);
		aFullRad.normalize();
		aFullRad.mult(A.mag());

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


		Ray r1B = new Ray(minorAppoloniusP2B, minorAppoloniusP1B);
		r1B.elongate();
		Ray r2B = new Ray(minorAppoloniusP2B, minorAppoloniusP3B);
		r2B.elongate();
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

	}
}
