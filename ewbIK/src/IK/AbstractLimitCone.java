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
import sceneGraph.math.SGVec_3d;

public abstract class AbstractLimitCone {

	SGVec_3d controlPoint; 
	SGVec_3d radialPoint; 
	protected double radius;

	public AbstractKusudama parentKusudama;

	public SGVec_3d tangentCircleCenterNext1;
	public SGVec_3d tangentCircleCenterNext2;
	public double tangentCircleRadiusNext;

	public SGVec_3d tangentCircleCenterPrevious1;
	public SGVec_3d tangentCircleCenterPrevious2;
	public double tangentCircleRadiusPrevious;


	//softness of 0 means completely hard. 
	//any softness higher than 0f means that
	//as the softness value is increased 
	//the is more penalized for moving 
	//further from the center of the channel
	public double softness = 0d;

	/**
	 * a triangle where the [1] is th tangentCircleNext_n, and [0] and [2] 
	 * are the points at which the tangent circle intersects this limitCone and the
	 * next limitCone
	 */
	public SGVec_3d[] firstTriangleNext = new SGVec_3d[3];
	public SGVec_3d[] secondTriangleNext = new SGVec_3d[3];

	public AbstractLimitCone(){}

	public AbstractLimitCone(SGVec_3d location, double rad, AbstractKusudama attachedTo) {
		setControlPoint(location); 
		radialPoint = controlPoint.copy();
		tangentCircleCenterNext1 = location.getOrthogonal();
		tangentCircleCenterNext2 = SGVec_3d.mult(tangentCircleCenterNext1, -1);

		this.radius = Math.max(Double.MIN_VALUE, rad);
		parentKusudama = attachedTo;
	}

	public double getMomentScalarFromThisToNext(SGVec_3d localPoint) {
		double angle = SGVec_3d.angleBetween(localPoint, controlPoint); 		
		//1 - (x^2 / roots) ^ softness
		double multiplier = 1-Math.pow(
				Math.pow(angle, 2d)/(Math.pow(radius,2)), softness);		
		return multiplier;
	}

	/**
	 * given some number of radians which the point wishes to rotate away from this
	 * limitCone, and a startingPosition, returns the number of angles the point should
	 * actually rotate after accounting for the softness of the
	 * cone's boundary. (this function takes the integral of the intended rotation 
	 * over the cone's softness function.
	 * 
	 * 
	 * calculates how many 
	 * @param localPoint
	 * @param angularMoment
	 * @return
	 */
	public double getAdjustedRotationAmount(SGVec_3d input, double angularMoment) {
		double currentAngle = SGVec_3d.angleBetween(controlPoint, input); 
		double result = 0;
		if(currentAngle < radius) {
			double x = angularMoment-currentAngle; 
			result = x - (x*Math.pow(Math.pow(x, 2)/Math.pow(radius, 2), softness)/((2*softness) +1));
			return result; 
		}
		else return result;
	}


	/**
	 * 
	 * @param inputCurrent the current position of the input point
	 * @param inputDesired where the input point wishes to rotate to
	 * @return
	 */
	public double getAdjustRotation(SGVec_3d inputCurrent, SGVec_3d inputDesired) {
		//TODO: this currently does nothing.
		return 0f;
	}



	/**
	 * 
	 * @param input 
	 * @param triangle an array of SGVec_3ds to store the triangleArray corresponding to 
	 * the tangentCone the input was projected from. 
	 * 
	 * @return the input point projected from the appropriate tangentCone, 
	 * onto the naive path between this limit cone and the next.  
	 */
	public SGVec_3d getPointOnNaivelyInterpolatedPath(AbstractLimitCone next, SGVec_3d input, SGVec_3d[] triangleHolder){
		SGVec_3d result = null;
		triangleHolder = onNaivelyInterpolatedPath(next, input);

		if(triangleHolder != null) {
			sgRay toPathRay = new sgRay(triangleHolder[0], input);
			SGVec_3d pathIntersect = new SGVec_3d(); 
			toPathRay.intersectsPlane(new SGVec_3d(0,0,0), this.controlPoint, next.controlPoint, pathIntersect);
			pathIntersect.normalize();
			result = pathIntersect;
		}
		return result;
	}

	public boolean inBoundsFromThisToNext(AbstractLimitCone next, SGVec_3d input, SGVec_3d collisionPoint) {
		boolean isInBounds = determineIfInBounds(next, input);
		if(!isInBounds) {
			SGVec_3d closestCollision = getClosestCollision(next, input); 
			collisionPoint.x = closestCollision.x; collisionPoint.y = closestCollision.y; collisionPoint.z = closestCollision.z; 
		} else {
			collisionPoint.x = input.x; 
			collisionPoint.y = input.y; 
			collisionPoint.z = input.z;
		}
		return isInBounds;
	}

	public SGVec_3d getClosestCollision(AbstractLimitCone next, SGVec_3d input) {
		SGVec_3d result = getOnGreatTangentTriangleSnap(next, input);
		if(result == null) {
			boolean[] inBounds = {false};
			result = closestPointOnClosestCone(next, input, inBounds);
		}
		return result;
	}

	public boolean determineIfInBounds(AbstractLimitCone next, SGVec_3d input) {
		boolean[] inBounds = {false};
		/**
		 * Procedure : Check if input is contained in this cone, or the next cone 
		 * if it is, then we're finished and in bounds. otherwise, check if the point  is contained 
		 * in the tangent triangles while still remaining outside of the tangent radii 
		 * if it is, then we're finished and in bounds. otherwise, we're out of bounds. 
		 */

		if(SGVec_3d.angleBetween(controlPoint, input) <=radius || SGVec_3d.angleBetween(next.controlPoint, input) <= next.radius ) {
			return true; 
		} else {
			double[] onTriangle1 = new double[3];
			SGVec_3d tri1Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext1, next.getControlPoint(), onTriangle1);
			double[] onTriangle2 = new double[3];
			SGVec_3d tri2Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext2, next.getControlPoint(), onTriangle2);
			boolean onTri1 = tri1Intersect != null 
					&& tri1Intersect.dot(input) > 0 && onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0 && onTriangle1[0] <= 1 && onTriangle1[1] <= 1 && onTriangle1[2] <=1; 
					boolean onTri2 = tri2Intersect != null 
							&& tri2Intersect.dot(input) > 0 && onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0 && onTriangle2[0] <= 1 && onTriangle2[1] <= 1 && onTriangle2[2] <=1; 				
							if(onTri1 || onTri2) {
								onTri1 = SGVec_3d.angleBetween(tangentCircleCenterNext1, input)	 < tangentCircleRadiusNext && onTri1; 	
								onTri2 = SGVec_3d.angleBetween(tangentCircleCenterNext2, input)	  < tangentCircleRadiusNext && onTri2; 
								if(!onTri1 && !onTri2) return true;
								else return false;
							}
		}
		return false;
	}
	
	/**
	 * if the point is in bounds, returns the rotation penalty incurred by this point 
	 * as a Rot object in which the axis represents the direction of rotation being penalized 
	 * and the angle represents the magnitude of the penalty (with pi being full penalty, and 0 beig no penalty)
	 * 
	 * @return the type of intersection between this point and its furthest applicable boundary. 
	 *0 = outside of limit 
	* 1 = in cone1 exclusively. 
	*2 = in cone2 exclusively
	*3 = in cone 1 and cone 2 exclusively
	*4 = in path exclusively
	*5 = in cone1 and path 
	*6 = in cone2 and path
	*7 = in cone1, cone2, and path
	*
	* the values of distanceToBorder[0] will be populated with the distance to the furthest applicable boundary, and penaltyAxis with the axis of rotation for that penalty. 
	 */
	public int getScaledPenaltyRotationFromThisToAdjacentConeifInBounds(AbstractLimitCone next, SGVec_3d point, int direction, double[] distanceToBorder, SGVec_3d penaltyDir) {
		int type = 0;
		SGVec_3d leftTan = null, rightTan = null;
		double leftTanRadius = 0, rightTanRadius = 0;
		if(direction == -1) {
			leftTan = tangentCircleCenterPrevious1;
			rightTan = tangentCircleCenterPrevious2; 
			rightTanRadius = tangentCircleRadiusPrevious; 
			leftTanRadius = tangentCircleRadiusPrevious; 
		} else {
			leftTan = tangentCircleCenterNext1;
			rightTan = tangentCircleCenterNext2; 
			rightTanRadius = tangentCircleRadiusNext; 
			leftTanRadius = tangentCircleRadiusNext; 
		}
		double arcDistToLeft = SGVec_3d.angleBetween(point, leftTan);
		double arcDistToRight = SGVec_3d.angleBetween(point, rightTan);
		double arcDistToCone1 = SGVec_3d.angleBetween(point, controlPoint);
			
		double leftTanBoundDist = (arcDistToLeft - leftTanRadius); 
		double rightTanBoundDist =(arcDistToRight - rightTanRadius);
		
		
		double arcDistTocone2 = SGVec_3d.angleBetween(point, next.controlPoint);
		boolean incone2 = arcDistTocone2 < next.radius;	
		boolean inCone1 = arcDistToCone1 < radius; 
		double cone1Height =  Math.max(0.0, 1.0 - (arcDistToCone1 / radius)); 
		double cone2Height = Math.max(0.0, 1.0 -(arcDistTocone2 / next.radius)); 
			
		boolean inLeftTan = arcDistToLeft < leftTanRadius; 	
		boolean inRightTan = arcDistToRight < rightTanRadius;	
		
		SGVec_3d cone1PenaltyAxis = new SGVec_3d(0.0,0.0,0.0);
		SGVec_3d pathPenaltyAxis = new SGVec_3d(0.0,0.0,0.0);
		SGVec_3d cone2PenaltyAxis = new SGVec_3d(0.0,0.0,0.0);
		
				
		double[] onTriangle1 = new double[3];
		SGVec_3d tri1Intersect = G.intersectTest(point, this.getControlPoint(), leftTan, next.getControlPoint(), onTriangle1);
		double[] onTriangle2 = new double[3];
		SGVec_3d tri2Intersect = G.intersectTest(point, this.getControlPoint(), rightTan, next.getControlPoint(), onTriangle2);
		boolean onTri1 = tri1Intersect != null 
				&& tri1Intersect.dot(point) > 0 && onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0 && onTriangle1[0] <= 1 && onTriangle1[1] <= 1 && onTriangle1[2] <=1; 
		boolean onTri2 = tri2Intersect != null 
						&& tri2Intersect.dot(point) > 0 && onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0 && onTriangle2[0] <= 1 && onTriangle2[1] <= 1 && onTriangle2[2] <=1; 
		
		onTri1 = onTri1 && !inLeftTan; 
		onTri2 = onTri2 && !inRightTan;
		double distToPath = 0.0; 
		double lrRatio = 6.0; 
		
		if( (onTri1 || onTri2)) { 
			
			if(onTri1) {
				distToPath =	distFromPathCenterTowardTanCone(point, leftTan, controlPoint, next.controlPoint);  
				lrRatio = (distToPath /  ( distToPath + leftTanBoundDist));
				pathPenaltyAxis = leftTan.crossCopy(point);  
				type += 4;
			} else if(onTri2) {
				distToPath =	distFromPathCenterTowardTanCone(point, rightTan, controlPoint, next.controlPoint);  
				lrRatio = (distToPath /  ( distToPath + rightTanBoundDist));	
				pathPenaltyAxis = rightTan.crossCopy(point);  
				type += 4;
			}
			distToPath = 1.0-lrRatio;
		}		
		
		if(inCone1) {
			cone1PenaltyAxis = point.crossCopy(controlPoint); 
			type += 1;
		}
		if(incone2) {
			cone2PenaltyAxis = point.crossCopy(next.controlPoint);
			type +=2; 
		}
		
		//0 - path, 1= cone1, 2= cone2
		int biggestSub = 0;
		double furthestApplicable = distToPath; 
		if(type ==1) { 
			biggestSub = 1; 
			furthestApplicable =  cone1Height;
		}
		if(type == 2) { 
			biggestSub = 2;
			furthestApplicable =  cone2Height;
	   }
	   if(type > 2) { 
			if(cone1Height > cone2Height) {
				biggestSub = 1;
				furthestApplicable = cone1Height;
			} else {
				biggestSub = 2; 
				furthestApplicable = cone2Height; 
			}
			if(type >= 3) {
				if(distToPath > furthestApplicable) {
					biggestSub = 0; //istToPath < furthestApplicable ? 0 : biggestSub;
					furthestApplicable = distToPath;
				}
			}
		}  
		if(biggestSub == 1) penaltyDir.set(cone1PenaltyAxis); 
		if(biggestSub == 2) penaltyDir.set(cone2PenaltyAxis);
		if(biggestSub == 0) penaltyDir.set(pathPenaltyAxis);
		distanceToBorder[0] = furthestApplicable; 
		return type;
	}
	
	double distFromPathCenterTowardTanCone(SGVec_3d pos, SGVec_3d tanCone, SGVec_3d cone1, SGVec_3d cone2) {
		SGVec_3d ro = new SGVec_3d(0.0, 0.0, 0.0);
		sgRay tanToPos = new sgRay(tanCone, pos);
		SGVec_3d intersectsGreatArcAt = tanToPos.intersectsPlane(ro, cone2, cone1);
		intersectsGreatArcAt = intersectsGreatArcAt.normalize(); 
		return SGVec_3d.angleBetween(pos, intersectsGreatArcAt);
	} 

	/**
	 * checks to see if this cone is encompassed by or encompasses the input cone
	 * @param next
	 * @return the encompassing cone if encompassment is detected, null otherwise.
	 */
	public AbstractLimitCone coneEncompassmentCheck(AbstractLimitCone next) {
		AbstractLimitCone result = null; 
		if(next.getRadius() > this.getRadius()) {
			double radBetween =SGVec_3d.angleBetween(next.getControlPoint(), this.getControlPoint());
			double radTotal = radBetween + this.getRadius();
			if((radTotal )< next.getRadius()) 
				result = next;
		} else {
			double radBetween =SGVec_3d.angleBetween(next.getControlPoint(), this.getControlPoint());
			double radTotal = radBetween + next.getRadius();
			if(radTotal < this.getRadius())
				result = this; 
		}

		return result;
	}

	public SGVec_3d getOnGreatTangentTriangleSnap(AbstractLimitCone next, SGVec_3d input) {
		SGVec_3d result = null;

		double[] onTriangle1 = new double[3];
		SGVec_3d tri1Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext1, next.getControlPoint(), onTriangle1);

		if(tri1Intersect != null 
				&& tri1Intersect.dot(input) > 0 
				&& onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0) {

			Rot tan1ToBorder = new Rot(tangentCircleCenterNext1, input); 
			result = new Rot(tan1ToBorder.getAxis(), tangentCircleRadiusNext).applyToCopy(tangentCircleCenterNext1);
		} else {
			double[] onTriangle2 = new double[3];
			SGVec_3d tri2Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext2, next.getControlPoint(), onTriangle2);

			if(tri2Intersect != null 
					&& tri2Intersect.dot(input) > 0 
					&& onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0) {

				Rot tan2ToBorder = new Rot(tangentCircleCenterNext2, input); 
				result = new Rot(tan2ToBorder.getAxis(), tangentCircleRadiusNext).applyToCopy(tangentCircleCenterNext2);
			}
		}

		return result;
	}

	public SGVec_3d closestPointOnClosestCone(AbstractLimitCone next, SGVec_3d input, boolean[] inBounds) {
		SGVec_3d closestToFirst = this.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToFirst; 
		}
		SGVec_3d closestToSecond = next.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToSecond; 
		}

		double angleToFirst = SGVec_3d.angleBetween(input, closestToFirst); 
		double angleToSecond = SGVec_3d.angleBetween(input, closestToSecond);

		if(Math.abs(angleToFirst) < Math.abs(angleToSecond)) {
			return closestToFirst;
		} else {
			return closestToSecond;
		}

	}

	public SGVec_3d closestToCone(SGVec_3d input, boolean[] inBounds) {
		Rot pointDiff = new Rot(this.getControlPoint(), input);
		SGVec_3d result = new SGVec_3d(0,0,0);
		if(pointDiff.getAngle() < this.getRadius()) {
			inBounds[0] = true;
			return input;
		} else {
			Rot rotTo = new Rot(pointDiff.getAxis(), this.getRadius()); 
			result = rotTo.applyToCopy(this.getControlPoint());
			inBounds[0] = false;
			return result;	
		}
	}


	private boolean onTangentRadii(SGVec_3d input) {
		if(Math.abs(SGVec_3d.angleBetween(input, tangentCircleCenterNext1)) < tangentCircleRadiusNext) {
			return true; 
		} else if(Math.abs(SGVec_3d.angleBetween(input, tangentCircleCenterNext2)) < tangentCircleRadiusNext) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param next
	 * @param input
	 * @return the tangentCone intersectionPoint triangle representing the tangentCone the
	 * input point is in the relevance region of, or null if it is not in the relevance region. 
	 * (the relevance region is that region on the path which is not best 
	 * handled by the limitCones themselves)
	 */
	private SGVec_3d[] onNaivelyInterpolatedPath(AbstractLimitCone next, SGVec_3d input) {
		double[] alongPath = new double[3];

		SGVec_3d intersectPoint = G.intersectTest(input, firstTriangleNext[0], firstTriangleNext[1], firstTriangleNext[2], alongPath);

		if(intersectPoint != null && 
				alongPath[0] >= 0 && alongPath[1] >= 0 && alongPath[2] >= 0
				&& intersectPoint.dot(input) >0 ) {
			if(SGVec_3d.angleBetween(intersectPoint, tangentCircleCenterNext1) > tangentCircleRadiusNext)
				return firstTriangleNext;
		}

		intersectPoint = G.intersectTest(input, secondTriangleNext[0], secondTriangleNext[1], secondTriangleNext[2], alongPath);

		if(intersectPoint != null &&
				alongPath[0] >= 0 && alongPath[1] >= 0 && alongPath[2] >= 0 
				&& intersectPoint.dot(input) >0 ) {
			if(SGVec_3d.angleBetween(intersectPoint, tangentCircleCenterNext2) > tangentCircleRadiusNext)
				return secondTriangleNext;
			else return null;
		} else {
			return null;
		}		
	}

	public void updateTangentHandles(AbstractLimitCone next) {    
		if(next !=null) {
			double radA = this.getRadius();
			double radB = next.getRadius();

			SGVec_3d A = this.getControlPoint().copy();
			SGVec_3d B = next.getControlPoint().copy(); 

			SGVec_3d arcNormal = A.crossCopy(B); 
			Rot aToARadian = new Rot(A, arcNormal); 
			SGVec_3d aToARadianAxis = aToARadian.getAxis();
			Rot bToBRadian = new Rot(B, arcNormal); 
			SGVec_3d bToBRadianAxis = bToBRadian.getAxis();
			aToARadian = new Rot(aToARadianAxis, radA);
			bToBRadian = new Rot(bToBRadianAxis, radB);

			/**
			 * There are an infinite number of circles co-tangent with A and B, every other
			 * one of which had a unique radius.  
			 * 
			 * However, we want the radius of our tangent circles to obey the following properties: 
			 *   1) When the radius of A + B == 0, our tangent circle's radius should = 90.
			 *   	In other words, the tangent circle should span a hemisphere. 
			 *   2) When the radius of A + B == 180, our tangent circle's radius should = 0. 
			 *   	In other words, when A + B combined are capable of spanning the entire sphere, 
			 *   	our tangentCircle should be nothing.   
			 *   
			 * Another way to think of this is -- whatever the maximum distance can be between the
			 * borders of A and B (presuming their centers are free to move about the circle
			 * but their radii remain constant), we want our tangentCircle's diameter to be precisely that distance, 
			 * and so, our tangent circles radius should be precisely half of that distance. 
			 */

			double tRadius = ((Math.PI)-(radA+radB))/2f;


			/**
			 * Once we have the desired radius for our tangent circle, we may find the solution for its
			 * centers (usually, there are two).
			 */

			double minorAppoloniusRadiusA = radA + tRadius;
			SGVec_3d minorAppoloniusAxisA = A.copy().normalize(); 
			double minorAppoloniusRadiusB = radB + tRadius;
			SGVec_3d minorAppoloniusAxisB  = B.copy().normalize();

			//the point on the radius of this cone + half the arcdistance to the circumference of the next cone along the arc path to the next cone 
			SGVec_3d minorAppoloniusP1A = new Rot(arcNormal, minorAppoloniusRadiusA).applyToCopy(minorAppoloniusAxisA);
			//the point on the radius of this cone + half the arcdistance to the circumference of the next cone, rotated 90 degrees along the axis of this cone
			SGVec_3d minorAppoloniusP2A = new Rot(minorAppoloniusAxisA, Math.PI/2d).applyToCopy(minorAppoloniusP1A);
			//the axis of this cone, scaled to minimize its distance to the previous two points. 
			SGVec_3d minorAppoloniusP3A =  SGVec_3d.mult(minorAppoloniusAxisA, G.cos(minorAppoloniusRadiusA));
			
			SGVec_3d minorAppoloniusP1B = new Rot(arcNormal, minorAppoloniusRadiusB).applyToCopy(minorAppoloniusAxisB);
			SGVec_3d minorAppoloniusP2B = new Rot(minorAppoloniusAxisB, Math.PI/2d).applyToCopy(minorAppoloniusP1B);      
			SGVec_3d minorAppoloniusP3B = SGVec_3d.mult(minorAppoloniusAxisB, G.cos(minorAppoloniusRadiusB));

			// ray from scaled center of next cone to half way point between the circumference of this cone and the next cone. 
			sgRay r1B = new sgRay(minorAppoloniusP1B, minorAppoloniusP3B); r1B.elongate();
			sgRay r2B = new sgRay(minorAppoloniusP1B, minorAppoloniusP2B); r2B.elongate();

			SGVec_3d intersection1 = G.intersectTest(r1B, minorAppoloniusP3A, minorAppoloniusP1A, minorAppoloniusP2A);
			SGVec_3d intersection2 = G.intersectTest(r2B, minorAppoloniusP3A, minorAppoloniusP1A, minorAppoloniusP2A);

			sgRay intersectionRay = new sgRay(intersection1, intersection2);
			intersectionRay.elongate();

			SGVec_3d sphereIntersect1 = new SGVec_3d(); 
			SGVec_3d sphereIntersect2 = new SGVec_3d();
			G.raySphereIntersection(intersectionRay, 1f, sphereIntersect1, sphereIntersect2);  


			this.tangentCircleCenterNext1 = sphereIntersect1; 
			this.tangentCircleCenterNext2 = sphereIntersect2; 
			this.tangentCircleRadiusNext = tRadius;


			next.tangentCircleCenterPrevious1 = sphereIntersect1; 
			next.tangentCircleCenterPrevious2 = sphereIntersect2;
			next.tangentCircleRadiusPrevious = tRadius;


			/*if(SGVec_3d.angleBetween(this.tangentCircleCenterNext1, this.getControlPoint()) < this.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(this.tangentCircleCenterNext2, this.getControlPoint()) < this.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(this.tangentCircleCenterNext1, next.getControlPoint()) < next.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(this.tangentCircleCenterNext2, next.getControlPoint()) < next.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(next.tangentCircleCenterPrevious1, this.getControlPoint()) < this.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(next.tangentCircleCenterPrevious2, this.getControlPoint()) < this.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(next.tangentCircleCenterPrevious1, next.getControlPoint()) < next.getRadius()) tRadius = 0d; 
			if(SGVec_3d.angleBetween(next.tangentCircleCenterPrevious2, next.getControlPoint()) < next.getRadius()) tRadius = 0d; */
		}
		/*if(this.tangentCircleCenterNext1.mag() == 0 && this.tangentCircleCenterNext2.mag() == 0) {
			AbstractLimitCone encompassingCone = coneEncompassmentCheck(next);

			//if(encompassingCone == this) {
				Rot radialCreation = new Rot(this.controlPoint.getOrthogonal(), this.radius); 
				SGVec_3d radialPoint = radialCreation.applyTo(this.controlPoint);
				this.tangentCircleCenterNext1 =  radialPoint; 
				this.tangentCircleCenterNext2 = radialPoint.copy();
				this.tangentCircleRadiusNext = this.radius;
				next.tangentCircleCenterPrevious1 =  radialPoint.copy(); 
				next.tangentCircleCenterPrevious2 = radialPoint.copy();
				next.tangentCircleRadiusPrevious = this.radius;

		}*/

		if(tangentCircleCenterNext1 == null) 
			tangentCircleCenterNext1 = controlPoint.getOrthogonal();
		if(tangentCircleCenterNext2 == null)
			tangentCircleCenterNext2 = SGVec_3d.mult(tangentCircleCenterNext1, -1);
		if(next != null)
			computeTriangles(next); 
	}

	private void computeTriangles(AbstractLimitCone next) {
		firstTriangleNext[1] = this.tangentCircleCenterNext1;		
		firstTriangleNext[0] = this.getControlPoint(); 
		firstTriangleNext[2] = next.getControlPoint();

		secondTriangleNext[1] = this.tangentCircleCenterNext2;		
		secondTriangleNext[0] = this.getControlPoint(); 
		secondTriangleNext[2] = next.getControlPoint();


		/*firstTriangleNext[1] = this.tangentCircleCenterNext1;	
		Rot tangentNext1ToThisLimitConeCenter = new Rot(tangentCircleCenterNext1, this.getControlPoint());
		Rot tangentNext1ToThisLimitConeBoundary = new Rot(tangentNext1ToThisLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		firstTriangleNext[0] = tangentNext1ToThisLimitConeBoundary.applyTo(tangentCircleCenterNext1);

		Rot tangentNext1ToNextLimitConeCenter = new Rot(tangentCircleCenterNext1, next.getControlPoint());
		Rot tangentNext1ToNextLimitConeBoundary = new Rot(tangentNext1ToNextLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		firstTriangleNext[2] = tangentNext1ToNextLimitConeBoundary.applyTo(tangentCircleCenterNext1);

		secondTriangleNext[1] = this.tangentCircleCenterNext2;

		Rot tangentNext2ToThisLimitConeCenter = new Rot(tangentCircleCenterNext2, this.getControlPoint());
		Rot tangentNext2ToThisLimitConeBoundary = new Rot(tangentNext2ToThisLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		secondTriangleNext[0] = tangentNext2ToThisLimitConeBoundary.applyTo(tangentCircleCenterNext2);

		Rot tangentNext2ToNextLimitConeCenter = new Rot(tangentCircleCenterNext2, next.getControlPoint());
		Rot tangentNext2ToNextLimitConeBoundary = new Rot(tangentNext2ToNextLimitConeCenter.getAxis(), tangentCircleRadiusNext);
		secondTriangleNext[2] = tangentNext2ToNextLimitConeBoundary.applyTo(tangentCircleCenterNext2);*/		
	}


	
	
	public SGVec_3d getRadialPoint() {
		if(radialPoint == null) { 
			radialPoint = controlPoint.getOrthogonal();
			SGVec_3d radialAxis = radialPoint.crossCopy(controlPoint);
			Rot rotateToRadial = new Rot(radialAxis, radius);
			radialPoint = rotateToRadial.applyToCopy(controlPoint);
		}
		return radialPoint;
	}
	public void setRadialPoint(SGVec_3d radialPoint) {
		this.radialPoint = radialPoint;
	}

	public SGVec_3d getControlPoint() {
		return controlPoint;
	}
	
	public void setControlPoint(SGVec_3d controlPoint) {
		this.controlPoint = controlPoint.copy();
		this.controlPoint.normalize();
		if(this.parentKusudama != null)
			this.parentKusudama.constraintUpdateNotification();
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
		this.parentKusudama.constraintUpdateNotification();
	}
	
	public AbstractKusudama getParentKusudama() {
		return parentKusudama;
	}
}
