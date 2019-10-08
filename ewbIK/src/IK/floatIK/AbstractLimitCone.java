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
import data.EWBIKLoader;
import data.EWBIKSaver;
import data.JSONObject;
import data.LoadManager;
import data.SaveManager;
import data.Saveable;
import sceneGraph.*;
import sceneGraph.math.floatV.MathUtils;
import sceneGraph.math.floatV.Rot;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.sgRayf;

public abstract class AbstractLimitCone implements Saveable {

	SGVec_3f controlPoint; 
	SGVec_3f radialPoint; 
	
	//stored internally as  cosine to save on the acos call necessary for vec.angleBetween. 
	protected float radius; 

	public AbstractKusudama parentKusudama;

	public SGVec_3f tangentCircleCenterNext1;
	public SGVec_3f tangentCircleCenterNext2;
	public float tangentCircleRadiusNext;

	public SGVec_3f tangentCircleCenterPrevious1;
	public SGVec_3f tangentCircleCenterPrevious2;
	public float tangentCircleRadiusPrevious;


	//softness of 0 means completely hard. 
	//any softness higher than 0f means that
	//as the softness value is increased 
	//the is more penalized for moving 
	//further from the center of the channel
	public float softness = 0f;

	/**
	 * a triangle where the [1] is th tangentCircleNext_n, and [0] and [2] 
	 * are the points at which the tangent circle intersects this limitCone and the
	 * next limitCone
	 */
	public SGVec_3f[] firstTriangleNext = new SGVec_3f[3];
	public SGVec_3f[] secondTriangleNext = new SGVec_3f[3];

	public AbstractLimitCone(){}

	public AbstractLimitCone(SGVec_3f location, float rad, AbstractKusudama attachedTo) {
		setControlPoint(location); 
		radialPoint = controlPoint.copy();
		tangentCircleCenterNext1 = location.getOrthogonal();
		tangentCircleCenterNext2 = SGVec_3f.mult(tangentCircleCenterNext1, -1);

		this.radius = MathUtils.max(Float.MIN_VALUE, rad);
		parentKusudama = attachedTo;
	}

	public float getMomentScalarFromThisToNext(SGVec_3f localPoint) {
		float angle = SGVec_3f.angleBetween(localPoint, controlPoint); 		
		//1 - (x^2 / roots) ^ softness
		float multiplier = 1-MathUtils.pow(
				MathUtils.pow(angle, 2f)/(MathUtils.pow(radius,2)), softness);		
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
	public float getAdjustedRotationAmount(SGVec_3f input, float angularMoment) {
		float currentAngle = SGVec_3f.angleBetween(controlPoint, input); 
		float result = 0;
		if(currentAngle < radius) {
			float x = angularMoment-currentAngle; 
			result = x - (x*MathUtils.pow(MathUtils.pow(x, 2)/MathUtils.pow(radius, 2), softness)/((2*softness) +1));
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
	public float getAdjustRotation(SGVec_3f inputCurrent, SGVec_3f inputDesired) {
		//TODO: this currently does nothing.
		return 0f;
	}



	/**
	 * 
	 * @param input 
	 * @param triangle an array of SGVec_3fs to store the triangleArray corresponding to 
	 * the tangentCone the input was projected from. 
	 * 
	 * @return the input point projected from the appropriate tangentCone, 
	 * onto the naive path between this limit cone and the next.  
	 */
	public SGVec_3f getPointOnNaivelyInterpolatedPath(AbstractLimitCone next, SGVec_3f input, SGVec_3f[] triangleHolder){
		SGVec_3f result = null;
		triangleHolder = onNaivelyInterpolatedPath(next, input);

		if(triangleHolder != null) {
			sgRayf toPathRay = new sgRayf(triangleHolder[0], input);
			SGVec_3f pathIntersect = new SGVec_3f(); 
			toPathRay.intersectsPlane(new SGVec_3f(0,0,0), this.controlPoint, next.controlPoint, pathIntersect);
			pathIntersect.normalize();
			result = pathIntersect;
		}
		return result;
	}

	public boolean inBoundsFromThisToNext(AbstractLimitCone next, SGVec_3f input, SGVec_3f collisionPoint) {
		boolean isInBounds = determineIfInBounds(next, input);
		if(!isInBounds) {
			SGVec_3f closestCollision = getClosestCollision(next, input); 
			collisionPoint.x = closestCollision.x; collisionPoint.y = closestCollision.y; collisionPoint.z = closestCollision.z; 
		} else {
			collisionPoint.x = input.x; 
			collisionPoint.y = input.y; 
			collisionPoint.z = input.z;
		}
		return isInBounds;
	}

	public SGVec_3f getClosestCollision(AbstractLimitCone next, SGVec_3f input) {
		SGVec_3f result = getOnGreatTangentTriangleSnap(next, input);
		if(result == null) {
			boolean[] inBounds = {false};
			result = closestPointOnClosestCone(next, input, inBounds);
		}
		return result;
	}

	public boolean determineIfInBounds(AbstractLimitCone next, SGVec_3f input) {
		/**
		 * Procedure : Check if input is contained in this cone, or the next cone 
		 * 	if it is, then we're finished and in bounds. otherwise, 
		 * check if the point  is contained within the tangent radii, 
		 * 	if it is, then we're out of bounds and finished, otherwise  
		 * in the tangent triangles while still remaining outside of the tangent radii 
		 * if it is, then we're finished and in bounds. otherwise, we're out of bounds. 
		 */

		if(SGVec_3f.angleBetween(controlPoint, input) <=radius || SGVec_3f.angleBetween(next.controlPoint, input) <= next.radius ) {
			return true; 
		} else {
			boolean inTan1Rad = SGVec_3f.angleBetween(tangentCircleCenterNext1, input)	 < tangentCircleRadiusNext; 
			if(inTan1Rad)
				return false; 
			boolean inTan2Rad = SGVec_3f.angleBetween(tangentCircleCenterNext2, input)	< tangentCircleRadiusNext;
			if(inTan2Rad) 
				return false; 
			
			/*if we reach this point in the code, we are either on the path between two limitCones, or on the path extending out from between them
			 * but outside of their radii. 
			 * 	To determine which , we take the cross product of each control point with each tangent center. 
			 * 		The direction of each of the resultant vectors will represent the normal of a plane. 
			 * 		Each of these four planes define part of a boundary which determines if our point is in bounds. 
			 * 		If the dot product of our point with the normal of any of these planes is negative, we must be out 
			 * 		of bounds. 
			 *
			 *	Older version of this code relied on a triangle intersection algorithm here, which I think is slightly less efficient on average
			 *	ass it didn't allow for early termination. . 
			 */
			
			SGVec_3f planeNormal = controlPoint.crossCopy(tangentCircleCenterNext1);
				if(input.dot(planeNormal) < 0)
					return false; 			
			planeNormal = SGVec_3f.cross(tangentCircleCenterNext2, controlPoint, planeNormal);
			if(input.dot(planeNormal) < 0)
				return false; 
			planeNormal = SGVec_3f.cross(next.controlPoint, tangentCircleCenterNext2, planeNormal);
			if(input.dot(planeNormal) < 0)
				return false; 
			planeNormal = SGVec_3f.cross(tangentCircleCenterNext1, next.controlPoint, planeNormal);
			if(input.dot(planeNormal) < 0)
				return false; 
			
			
			return true;
		}
			
			/*float[] onTriangle1 = new float[3];
			SGVec_3f tri1Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext1, next.getControlPoint(), onTriangle1);
			float[] onTriangle2 = new float[3];
			SGVec_3f tri2Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext2, next.getControlPoint(), onTriangle2);
			boolean onTri1 = tri1Intersect != null 
					&& tri1Intersect.dot(input) > 0 && onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0 && onTriangle1[0] <= 1 && onTriangle1[1] <= 1 && onTriangle1[2] <=1; 
					boolean onTri2 = tri2Intersect != null 
							&& tri2Intersect.dot(input) > 0 && onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0 && onTriangle2[0] <= 1 && onTriangle2[1] <= 1 && onTriangle2[2] <=1; 				
							if(onTri1 || onTri2) {
								onTri1 = SGVec_3f.angleBetween(tangentCircleCenterNext1, input)	 < tangentCircleRadiusNext && onTri1; 	
								onTri2 = SGVec_3f.angleBetween(tangentCircleCenterNext2, input)	  < tangentCircleRadiusNext && onTri2; 
								if(!onTri1 && !onTri2) return true;
								else return false;
							}
		}*/
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
	public int getScaledPenaltyRotationFromThisToAdjacentConeifInBounds(AbstractLimitCone next, SGVec_3f point, int direction, float[] distanceToBorder, SGVec_3f penaltyDir) {
		int type = 0;
		SGVec_3f leftTan = null, rightTan = null;
		float leftTanRadius = 0, rightTanRadius = 0;
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
		float arcDistToLeft = SGVec_3f.angleBetween(point, leftTan);
		float arcDistToRight = SGVec_3f.angleBetween(point, rightTan);
		float arcDistToCone1 = SGVec_3f.angleBetween(point, controlPoint);
			
		float leftTanBoundDist = (arcDistToLeft - leftTanRadius); 
		float rightTanBoundDist =(arcDistToRight - rightTanRadius);
		
		
		float arcDistTocone2 = SGVec_3f.angleBetween(point, next.controlPoint);
		boolean incone2 = arcDistTocone2 < next.radius;	
		boolean inCone1 = arcDistToCone1 < radius; 
		float cone1Height =  MathUtils.max(0.0f, 1.0f - (arcDistToCone1 / radius)); 
		float cone2Height = MathUtils.max(0.0f, 1.0f -(arcDistTocone2 / next.radius)); 
			
		boolean inLeftTan = arcDistToLeft < leftTanRadius; 	
		boolean inRightTan = arcDistToRight < rightTanRadius;	
		
		SGVec_3f cone1PenaltyAxis = new SGVec_3f(0.0f,0.0f,0.0f);
		SGVec_3f pathPenaltyAxis = new SGVec_3f(0.0f,0.0f,0.0f);
		SGVec_3f cone2PenaltyAxis = new SGVec_3f(0.0f,0.0f,0.0f);
		
				
		float[] onTriangle1 = new float[3];
		SGVec_3f tri1Intersect = G.intersectTest(point, this.getControlPoint(), leftTan, next.getControlPoint(), onTriangle1);
		float[] onTriangle2 = new float[3];
		SGVec_3f tri2Intersect = G.intersectTest(point, this.getControlPoint(), rightTan, next.getControlPoint(), onTriangle2);
		boolean onTri1 = tri1Intersect != null 
				&& tri1Intersect.dot(point) > 0 && onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0 && onTriangle1[0] <= 1 && onTriangle1[1] <= 1 && onTriangle1[2] <=1; 
		boolean onTri2 = tri2Intersect != null 
						&& tri2Intersect.dot(point) > 0 && onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0 && onTriangle2[0] <= 1 && onTriangle2[1] <= 1 && onTriangle2[2] <=1; 
		
		onTri1 = onTri1 && !inLeftTan; 
		onTri2 = onTri2 && !inRightTan;
		float distToPath = 0.0f; 
		float lrRatio = 6.0f; 
		
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
			distToPath = 1.0f-lrRatio;
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
		float furthestApplicable = distToPath; 
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
	
	float distFromPathCenterTowardTanCone(SGVec_3f pos, SGVec_3f tanCone, SGVec_3f cone1, SGVec_3f cone2) {
		SGVec_3f ro = new SGVec_3f(0.0f, 0.0f, 0.0f);
		sgRayf tanToPos = new sgRayf(tanCone, pos);
		SGVec_3f intersectsGreatArcAt = tanToPos.intersectsPlane(ro, cone2, cone1);
		intersectsGreatArcAt = intersectsGreatArcAt.normalize(); 
		return SGVec_3f.angleBetween(pos, intersectsGreatArcAt);
	} 

	/**
	 * checks to see if this cone is encompassed by or encompasses the input cone
	 * @param next
	 * @return the encompassing cone if encompassment is detected, null otherwise.
	 */
	public AbstractLimitCone coneEncompassmentCheck(AbstractLimitCone next) {
		AbstractLimitCone result = null; 
		if(next.getRadius() > this.getRadius()) {
			float radBetween =SGVec_3f.angleBetween(next.getControlPoint(), this.getControlPoint());
			float radTotal = radBetween + this.getRadius();
			if((radTotal )< next.getRadius()) 
				result = next;
		} else {
			float radBetween =SGVec_3f.angleBetween(next.getControlPoint(), this.getControlPoint());
			float radTotal = radBetween + next.getRadius();
			if(radTotal < this.getRadius())
				result = this; 
		}

		return result;
	}

	public SGVec_3f getOnGreatTangentTriangleSnap(AbstractLimitCone next, SGVec_3f input) {
		SGVec_3f result = null;

		float[] onTriangle1 = new float[3];
		SGVec_3f tri1Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext1, next.getControlPoint(), onTriangle1);

		if(tri1Intersect != null 
				&& tri1Intersect.dot(input) > 0 
				&& onTriangle1[0] >= 0 && onTriangle1[1] >= 0 && onTriangle1[2] >= 0) {

			Rot tan1ToBorder = new Rot(tangentCircleCenterNext1, input); 
			result = new Rot(tan1ToBorder.getAxis(), tangentCircleRadiusNext).applyToCopy(tangentCircleCenterNext1);
		} else {
			float[] onTriangle2 = new float[3];
			SGVec_3f tri2Intersect = G.intersectTest(input, this.getControlPoint(), this.tangentCircleCenterNext2, next.getControlPoint(), onTriangle2);

			if(tri2Intersect != null 
					&& tri2Intersect.dot(input) > 0 
					&& onTriangle2[0] >= 0 && onTriangle2[1] >= 0 && onTriangle2[2] >= 0) {

				Rot tan2ToBorder = new Rot(tangentCircleCenterNext2, input); 
				result = new Rot(tan2ToBorder.getAxis(), tangentCircleRadiusNext).applyToCopy(tangentCircleCenterNext2);
			}
		}

		return result;
	}

	public SGVec_3f closestPointOnClosestCone(AbstractLimitCone next, SGVec_3f input, boolean[] inBounds) {
		SGVec_3f closestToFirst = this.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToFirst; 
		}
		SGVec_3f closestToSecond = next.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToSecond; 
		}

		float angleToFirst = SGVec_3f.angleBetween(input, closestToFirst); 
		float angleToSecond = SGVec_3f.angleBetween(input, closestToSecond);

		if(MathUtils.abs(angleToFirst) < MathUtils.abs(angleToSecond)) {
			return closestToFirst;
		} else {
			return closestToSecond;
		}

	}

	public SGVec_3f closestToCone(SGVec_3f input, boolean[] inBounds) {
		Rot pointDiff = new Rot(this.getControlPoint(), input);
		SGVec_3f result = new SGVec_3f(0,0,0);
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


	/**
	 * 
	 * @param next
	 * @param input
	 * @return the tangentCone intersectionPoint triangle representing the tangentCone the
	 * input point is in the relevance region of, or null if it is not in the relevance region. 
	 * (the relevance region is that region on the path which is not best 
	 * handled by the limitCones themselves)
	 */
	private SGVec_3f[] onNaivelyInterpolatedPath(AbstractLimitCone next, SGVec_3f input) {
		float[] alongPath = new float[3];

		SGVec_3f intersectPoint = G.intersectTest(input, firstTriangleNext[0], firstTriangleNext[1], firstTriangleNext[2], alongPath);

		if(intersectPoint != null && 
				alongPath[0] >= 0 && alongPath[1] >= 0 && alongPath[2] >= 0
				&& intersectPoint.dot(input) >0 ) {
			if(SGVec_3f.angleBetween(intersectPoint, tangentCircleCenterNext1) > tangentCircleRadiusNext)
				return firstTriangleNext;
		}

		intersectPoint = G.intersectTest(input, secondTriangleNext[0], secondTriangleNext[1], secondTriangleNext[2], alongPath);

		if(intersectPoint != null &&
				alongPath[0] >= 0 && alongPath[1] >= 0 && alongPath[2] >= 0 
				&& intersectPoint.dot(input) >0 ) {
			if(SGVec_3f.angleBetween(intersectPoint, tangentCircleCenterNext2) > tangentCircleRadiusNext)
				return secondTriangleNext;
			else return null;
		} else {
			return null;
		}		
	}

	public void updateTangentHandles(AbstractLimitCone next) {    
		if(next !=null) {
			float radA = this.getRadius();
			float radB = next.getRadius();

			SGVec_3f A = this.getControlPoint().copy();
			SGVec_3f B = next.getControlPoint().copy(); 

			SGVec_3f arcNormal = A.crossCopy(B); 
			Rot aToARadian = new Rot(A, arcNormal); 
			SGVec_3f aToARadianAxis = aToARadian.getAxis();
			Rot bToBRadian = new Rot(B, arcNormal); 
			SGVec_3f bToBRadianAxis = bToBRadian.getAxis();
			aToARadian = new Rot(aToARadianAxis, radA);
			bToBRadian = new Rot(bToBRadianAxis, radB);

			/**
			 * There are an infinite number of circles co-tangent with A and B, every other
			 * one of which has a unique radius.  
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

			float tRadius = ((MathUtils.PI)-(radA+radB))/2f;


			/**
			 * Once we have the desired radius for our tangent circle, we may find the solution for its
			 * centers (usually, there are two).
			 */

			float minorAppoloniusRadiusA = radA + tRadius;
			SGVec_3f minorAppoloniusAxisA = A.copy().normalize(); 
			float minorAppoloniusRadiusB = radB + tRadius;
			SGVec_3f minorAppoloniusAxisB  = B.copy().normalize();

			//the point on the radius of this cone + half the arcdistance to the circumference of the next cone along the arc path to the next cone 
			SGVec_3f minorAppoloniusP1A = new Rot(arcNormal, minorAppoloniusRadiusA).applyToCopy(minorAppoloniusAxisA);
			//the point on the radius of this cone + half the arcdistance to the circumference of the next cone, rotated 90 degrees along the axis of this cone
			SGVec_3f minorAppoloniusP2A = new Rot(minorAppoloniusAxisA, MathUtils.PI/2f).applyToCopy(minorAppoloniusP1A);
			//the axis of this cone, scaled to minimize its distance to the previous two points. 
			SGVec_3f minorAppoloniusP3A =  SGVec_3f.mult(minorAppoloniusAxisA, MathUtils.cos(minorAppoloniusRadiusA));
			
			SGVec_3f minorAppoloniusP1B = new Rot(arcNormal, minorAppoloniusRadiusB).applyToCopy(minorAppoloniusAxisB);
			SGVec_3f minorAppoloniusP2B = new Rot(minorAppoloniusAxisB, MathUtils.PI/2f).applyToCopy(minorAppoloniusP1B);      
			SGVec_3f minorAppoloniusP3B = SGVec_3f.mult(minorAppoloniusAxisB, MathUtils.cos(minorAppoloniusRadiusB));

			// ray from scaled center of next cone to half way point between the circumference of this cone and the next cone. 
			sgRayf r1B = new sgRayf(minorAppoloniusP1B, minorAppoloniusP3B); r1B.elongate();
			sgRayf r2B = new sgRayf(minorAppoloniusP1B, minorAppoloniusP2B); r2B.elongate();

			SGVec_3f intersection1 = G.intersectTest(r1B, minorAppoloniusP3A, minorAppoloniusP1A, minorAppoloniusP2A);
			SGVec_3f intersection2 = G.intersectTest(r2B, minorAppoloniusP3A, minorAppoloniusP1A, minorAppoloniusP2A);

			sgRayf intersectionRay = new sgRayf(intersection1, intersection2);
			intersectionRay.elongate();

			SGVec_3f sphereIntersect1 = new SGVec_3f(); 
			SGVec_3f sphereIntersect2 = new SGVec_3f();
			G.raySphereIntersection(intersectionRay, 1f, sphereIntersect1, sphereIntersect2);  

			this.tangentCircleCenterNext1 = sphereIntersect1; 
			this.tangentCircleCenterNext2 = sphereIntersect2; 
			this.tangentCircleRadiusNext = tRadius;


			next.tangentCircleCenterPrevious1 = sphereIntersect1; 
			next.tangentCircleCenterPrevious2 = sphereIntersect2;
			next.tangentCircleRadiusPrevious = tRadius;

		}

		if(tangentCircleCenterNext1 == null) 
			tangentCircleCenterNext1 = controlPoint.getOrthogonal().normalize();
		if(tangentCircleCenterNext2 == null)
			tangentCircleCenterNext2 = SGVec_3f.mult(tangentCircleCenterNext1, -1).normalize();
		if(next != null)
			computeTriangles(next); 
	}

	private void computeTriangles(AbstractLimitCone next) {
		firstTriangleNext[1] = this.tangentCircleCenterNext1.normalize();		
		firstTriangleNext[0] = this.getControlPoint().normalize(); 
		firstTriangleNext[2] = next.getControlPoint().normalize();

		secondTriangleNext[1] = this.tangentCircleCenterNext2.normalize();		
		secondTriangleNext[0] = this.getControlPoint().normalize(); 
		secondTriangleNext[2] = next.getControlPoint().normalize();
	}


	
	
	public SGVec_3f getRadialPoint() {
		if(radialPoint == null) { 
			radialPoint = controlPoint.getOrthogonal();
			SGVec_3f radialAxis = radialPoint.crossCopy(controlPoint);
			Rot rotateToRadial = new Rot(radialAxis, radius);
			radialPoint = rotateToRadial.applyToCopy(controlPoint);
		}
		return radialPoint;
	}
	public void setRadialPoint(SGVec_3f radialPoint) {
		this.radialPoint = radialPoint;
	}

	public SGVec_3f getControlPoint() {
		return controlPoint;
	}
	
	public void setControlPoint(SGVec_3f controlPoint) {
		this.controlPoint = controlPoint.copy();
		this.controlPoint.normalize();
		if(this.parentKusudama != null)
			this.parentKusudama.constraintUpdateNotification();
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
		this.parentKusudama.constraintUpdateNotification();
	}
	
	public AbstractKusudama getParentKusudama() {
		return parentKusudama;
	}
	
	@Override
	public void makeSaveable(SaveManager saveManager) {
	}
	
	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		JSONObject saveJSON = new JSONObject(); 
		saveJSON.setString("identityHash", this.getIdentityHash());
		saveJSON.setString("parentKusudama", this.getParentKusudama().getIdentityHash()); 
		saveJSON.setJSONObject("controlPoint", this.controlPoint.toJSONObject());
		saveJSON.setFloat("radius", this.radius);
		return saveJSON;
	}
	
	
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		this.parentKusudama = (AbstractKusudama) l.getObjectFromClassMaps(AbstractKusudama.class, j.getString("parentKusudama")); 
		SGVec_3f controlPointJ = new SGVec_3f(j.getJSONArray("controlPoint"));
		this.setControlPoint(controlPointJ);
		this.radius = j.getFloat("radius");
	}
	
	
	@Override
	public void notifyOfSaveIntent(SaveManager saveManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyOfSaveCompletion(SaveManager saveManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLoading() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLoading(boolean loading) {
		// TODO Auto-generated method stub
		
	}
}
