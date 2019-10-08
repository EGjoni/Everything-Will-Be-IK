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
import math.floatV.AbstractAxes;
import math.floatV.MathUtils;
import math.floatV.Rot;
import math.floatV.SGVec_3f;
import math.floatV.Vec3f;
import math.floatV.sgRayf;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;

public abstract class AbstractLimitCone implements Saveable {

	Vec3f<?> controlPoint; 
	Vec3f<?> radialPoint; 

	//radius stored as  cosine to save on the acos call necessary for angleBetween. 
	private float radiusCosine; 
	private float radius; 

	public AbstractKusudama parentKusudama;

	public Vec3f<?> tangentCircleCenterNext1;
	public Vec3f<?> tangentCircleCenterNext2;
	public float tangentCircleRadiusNext;
	public float tangentCircleRadiusNextCos;

	public Vec3f<?> tangentCircleCenterPrevious1;
	public Vec3f<?> tangentCircleCenterPrevious2;
	public float tangentCircleRadiusPrevious;
	public float tangentCircleRadiusPreviousCos;


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
	public Vec3f<?>[] firstTriangleNext = new SGVec_3f[3];
	public Vec3f<?>[] secondTriangleNext = new SGVec_3f[3];

	public AbstractLimitCone(){}

	public AbstractLimitCone(Vec3f<?> location, float rad, AbstractKusudama attachedTo) {
		setControlPoint(location); 
		radialPoint = controlPoint.copy();
		tangentCircleCenterNext1 = location.getOrthogonal();
		tangentCircleCenterNext2 = SGVec_3f.mult(tangentCircleCenterNext1, -1);

		this.radius = MathUtils.max(Float.MIN_VALUE, rad);
		this.radiusCosine = MathUtils.cos(radius);
		parentKusudama = attachedTo;
	}



	/**
	 * 
	 * @param next
	 * @param input
	 * @param collisionPoint will be set to the rectified (if necessary) position of the input after accounting for collisions
	 * @return
	 */
	public boolean inBoundsFromThisToNext(AbstractLimitCone next, Vec3f<?> input, Vec3f<?> collisionPoint) {
		boolean isInBounds = false;//determineIfInBounds(next, input);
		//if(!isInBounds) {
		Vec3f<?> closestCollision = getClosestCollision(next, input);
		if(closestCollision == null) {
			/**
			 * getClosestCollision returns null if the point is already in bounds,
			 * so we set isInBounds to true.  
			 */
			isInBounds = true;
			collisionPoint.x = input.x; 
			collisionPoint.y = input.y; 
			collisionPoint.z = input.z;			
		} else {
			collisionPoint.x = closestCollision.x; 
			collisionPoint.y = closestCollision.y; 
			collisionPoint.z = closestCollision.z;
		}
		/*} else {
			collisionPoint.x = input.x; 
			collisionPoint.y = input.y; 
			collisionPoint.z = input.z;
		}*/
		return isInBounds;
	}

	/**
	 * 
	 * @param next
	 * @param input
	 * @return null if the input point is already in bounds, or the point's rectified position 
	 * if the point was out of bounds. 
	 */
	public <V extends Vec3f<?>> Vec3f<?> getClosestCollision(AbstractLimitCone next, V input) {
		Vec3f<?> result = getOnGreatTangentTriangle(next, input);
		if(result == null) {
			boolean[] inBounds = {false};
			result = closestPointOnClosestCone(next, input, inBounds);
		}
		return result;
	}

	/**
	 * Determines if a ray emanating from the origin to given point in local space 
	 * lies withing the path from this cone to the next cone. This function relies on 
	 * an optimization trick for a performance boost, but the trick ruins everything 
	 * if the input isn't normalized. So it is ABSOLUTELY VITAL 
	 * that @param input have unit length in order for this function to work correctly.  
	 * @param next
	 * @param input
	 * @return
	 */
	public boolean determineIfInBounds(AbstractLimitCone next, Vec3f<?> input) {
		/**
		 * Procedure : Check if input is contained in this cone, or the next cone 
		 * 	if it is, then we're finished and in bounds. otherwise, 
		 * check if the point  is contained within the tangent radii, 
		 * 	if it is, then we're out of bounds and finished, otherwise  
		 * in the tangent triangles while still remaining outside of the tangent radii 
		 * if it is, then we're finished and in bounds. otherwise, we're out of bounds. 
		 */

		if(controlPoint.dot(input) >= radiusCosine || next.controlPoint.dot(input) >= next.radiusCosine ) {
			return true; 
		} else {
			boolean inTan1Rad = tangentCircleCenterNext1.dot(input)	> tangentCircleRadiusNextCos; 
			if(inTan1Rad)
				return false; 
			boolean inTan2Rad = tangentCircleCenterNext2.dot(input)	> tangentCircleRadiusNextCos;
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

			Vec3f<?> planeNormal = controlPoint.crossCopy(tangentCircleCenterNext1);
			if(input.dot(planeNormal) < 0)
				return false; 			
			planeNormal = Vec3f.cross(tangentCircleCenterNext2, controlPoint, planeNormal);
			if(input.dot(planeNormal) < 0)
				return false; 
			planeNormal = Vec3f.cross(next.controlPoint, tangentCircleCenterNext2, planeNormal);
			if(input.dot(planeNormal) < 0)
				return false; 
			planeNormal = Vec3f.cross(tangentCircleCenterNext1, next.controlPoint, planeNormal);
			if(input.dot(planeNormal) < 0)
				return false; 


			return true;
		}

	}	


	public <V extends Vec3f<?>> Vec3f<?> getOnGreatTangentTriangle(AbstractLimitCone next, V input) {

		//first, we check to see if we're even out of bounds.
		//-1 means we're within the region of tangentCircleCenterNext1
		//0 means we are within neither region, 
		// 1 means wer'e within the region of tangentCircleCenterNext2

		Vec3f<?> planeNormal = controlPoint.crossCopy(tangentCircleCenterNext1);

		/*if eany of the following four if statements fail, it means 
		 * we're outside the confines of the great triangle formed 
		 * by {this.controlPoint, tangentCircleCenterNext1, next.controlPoint}
		 * and the adjacent triangle formed by 
		 * {next.controlPoint, tangentCirclenteNext2, this.controlPoint}. 
		 * 
		 * So we send null to signal that point determination should be handled by some other function.
		 **/
		if(input.dot(planeNormal) < 0) 
			return null;
		planeNormal.set(tangentCircleCenterNext1).crs(next.controlPoint);
		if(input.dot(planeNormal) < 0)
			return null; 
		planeNormal.set(tangentCircleCenterNext2).crs(controlPoint);
		if(input.dot(planeNormal) < 0)
			return null; 
		planeNormal.set(next.controlPoint).crs(tangentCircleCenterNext2);
		if(input.dot(planeNormal) < 0)
			return null; 



		if(input.dot(tangentCircleCenterNext1) > tangentCircleRadiusNextCos) 
		{
			/*
			 * If we reach this point in the code, we're within the confines of tangentCircleCenterNext1, and the 
			 * great triangle formed by {this.controlPoint, tangentCircleCenterNext1, next.controlPoint}.
			 * so we have to calculate the rotation that rotates the input away from the tangentCenter enough
			 * to bring it back outside of its radius. 
			 * 
			 * We return the result (which is not null) and thereby signals no futher action is required. 
			 */
			planeNormal = Vec3f.cross(tangentCircleCenterNext1, input, planeNormal); 
			Rot rotateAboutBy = new Rot(planeNormal, tangentCircleRadiusNext);
			return rotateAboutBy.applyToCopy(tangentCircleCenterNext1);
		} 
		else if(input.dot(tangentCircleCenterNext2) > tangentCircleRadiusNextCos) 
		{
			/*
			 * If we reach this point in the code, we're within the confines of tangentCircleCenterNext2, and the 
			 * great triangle formed by {next.controlPoint, tangentCircleCenterNext2, this.controlPoint}.
			 * so we have to calculate the rotation that rotates the input away from the tangentCenter enough
			 * to bring it back outside of its radius. 
			 * 
			 * We return the result (which is not null) and thereby signals no futher action is required. 
			 */		
			planeNormal = Vec3f.cross(tangentCircleCenterNext2, input, planeNormal); 
			Rot rotateAboutBy = new Rot(planeNormal, tangentCircleRadiusNext);
			return rotateAboutBy.applyToCopy(tangentCircleCenterNext2);	
		} 
		else 
		{
			/**
			 * if both of the above conditions failed, it means also that we are 
			 * neither in the impermissible regions around tangentCircleCenterNext1 
			 * nor that of tangentCircleCenterNext2.
			 * This means implicitly that we're in an acceptable region (because we know we're between 
			 * this.controlPoint and next.controlPoint).
			 * 
			 *  So we set the result = to the input, to signal that no further action is required. 
			 */
			return input.copy();
		}






		/*
		 * if we're at this point in the code we know  
		 */


		//Vec3f<?> result = input.copy(); 
		/*sgRayf inRay = new sgRayf(new SGVec_3f(0,0,0), input);
		Vec3f<?> intersectionResult = new SGVec_3f();
		boolean tri1Intersects = inRay.intersectsTriangle(this.getControlPoint(),this.tangentCircleCenterNext1,next.getControlPoint(), intersectionResult);

		if(tri1Intersects) {
			Rot tan1ToBorder = new Rot(tangentCircleCenterNext1, input); 
			result.set(new Rot(tan1ToBorder.getAxis(), tangentCircleRadiusNext).applyToCopy(tangentCircleCenterNext1));
		} else {
			boolean tri2Intersect = inRay.intersectsTriangle(this.getControlPoint(), this.tangentCircleCenterNext2, next.getControlPoint(), intersectionResult);
			if(tri2Intersect) {
				Rot tan2ToBorder = new Rot(tangentCircleCenterNext2, input); 
				result.set(new Rot(tan2ToBorder.getAxis(), tangentCircleRadiusNext).applyToCopy(tangentCircleCenterNext2));
			} else {
				result = null;
			}
		}

		return  result;*/
	}

	/**
	 * returns null if no rectification is required.
	 * @param next
	 * @param input
	 * @param inBounds
	 * @return
	 */
	public <V extends Vec3f<?>> Vec3f<?> closestPointOnClosestCone(AbstractLimitCone next, V input, boolean[] inBounds) {
		Vec3f<?> closestToFirst = this.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToFirst; 
		}
		Vec3f<?> closestToSecond = next.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToSecond; 
		}

		float cosToFirst = input.dot(closestToFirst); 
		float cosToSecond = input.dot(closestToSecond);

		if(cosToFirst > cosToSecond) {
			return closestToFirst;
		} else {
			return closestToSecond;
		}

	}

	/**
	 * returns null if no rectification is required.
	 * @param input
	 * @param inBounds
	 * @return
	 */
	public <V extends Vec3f<?>> Vec3f<?> closestToCone(V input, boolean[] inBounds) {

		if(input.dot(this.getControlPoint()) > this.getRadiusCosine()) {
			inBounds[0] = true;
			return null;//input.copy();
		} else {
			Vec3f<?> axis = this.getControlPoint().crossCopy(input);
			//axis.normalize();
			//Rot pointDiff = new Rot(this.getControlPoint(), input);
			Rot rotTo = new Rot(axis, this.getRadius());
			//Rot rot2To = new Rot(pointDiff.getAxis(), this.getRadius());
			Vec3f<?>result = rotTo.applyToCopy(this.getControlPoint());
			inBounds[0] = false;
			return result;	
		}
	}


	public void updateTangentHandles(AbstractLimitCone next) {    
		this.controlPoint.normalize();
		if(next !=null) {
			float radA = this.getRadius();
			float radB = next.getRadius();

			Vec3f<?> A = this.getControlPoint().copy();
			Vec3f<?> B = next.getControlPoint().copy(); 

			Vec3f<?> arcNormal = A.crossCopy(B); 
			Rot aToARadian = new Rot(A, arcNormal); 
			Vec3f<?> aToARadianAxis = aToARadian.getAxis();
			Rot bToBRadian = new Rot(B, arcNormal); 
			Vec3f<?> bToBRadianAxis = bToBRadian.getAxis();
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

			float tRadius = ((MathUtils.PI)-(radA+radB))/2f;


			/**
			 * Once we have the desired radius for our tangent circle, we may find the solution for its
			 * centers (usually, there are two).
			 */

			float minorAppoloniusRadiusA = radA + tRadius;
			Vec3f<?> minorAppoloniusAxisA = A.copy().normalize(); 
			float minorAppoloniusRadiusB = radB + tRadius;
			Vec3f<?> minorAppoloniusAxisB  = B.copy().normalize();

			//the point on the radius of this cone + half the arcdistance to the circumference of the next cone along the arc path to the next cone 
			Vec3f<?> minorAppoloniusP1A = new Rot(arcNormal, minorAppoloniusRadiusA).applyToCopy(minorAppoloniusAxisA);
			//the point on the radius of this cone + half the arcdistance to the circumference of the next cone, rotated 90 degrees along the axis of this cone
			Vec3f<?> minorAppoloniusP2A = new Rot(minorAppoloniusAxisA, MathUtils.PI/2f).applyToCopy(minorAppoloniusP1A);
			//the axis of this cone, scaled to minimize its distance to the previous two points. 
			Vec3f<?> minorAppoloniusP3A =  SGVec_3f.mult(minorAppoloniusAxisA, MathUtils.cos(minorAppoloniusRadiusA));

			Vec3f<?> minorAppoloniusP1B = new Rot(arcNormal, minorAppoloniusRadiusB).applyToCopy(minorAppoloniusAxisB);
			Vec3f<?> minorAppoloniusP2B = new Rot(minorAppoloniusAxisB, MathUtils.PI/2f).applyToCopy(minorAppoloniusP1B);      
			Vec3f<?> minorAppoloniusP3B = Vec3f.mult(minorAppoloniusAxisB, MathUtils.cos(minorAppoloniusRadiusB));

			// ray from scaled center of next cone to half way point between the circumference of this cone and the next cone. 
			sgRayf r1B = new sgRayf(minorAppoloniusP1B, minorAppoloniusP3B); r1B.elongate();
			sgRayf r2B = new sgRayf(minorAppoloniusP1B, minorAppoloniusP2B); r2B.elongate();

			Vec3f<?> intersection1 = r1B.intersectsPlane(minorAppoloniusP3A, minorAppoloniusP1A, minorAppoloniusP2A);
			Vec3f<?> intersection2 = r2B.intersectsPlane( minorAppoloniusP3A, minorAppoloniusP1A, minorAppoloniusP2A);

			sgRayf intersectionRay = new sgRayf(intersection1, intersection2);
			intersectionRay.elongate();

			Vec3f<?> sphereIntersect1 = new SGVec_3f(); 
			Vec3f<?> sphereIntersect2 = new SGVec_3f();
			Vec3f<?> sphereCenter = new SGVec_3f();
			intersectionRay.intersectsSphere(sphereCenter, 1f, sphereIntersect1, sphereIntersect2);  

			this.tangentCircleCenterNext1 = sphereIntersect1; 
			this.tangentCircleCenterNext2 = sphereIntersect2; 
			this.tangentCircleRadiusNext = tRadius;

			next.tangentCircleCenterPrevious1 = sphereIntersect1; 
			next.tangentCircleCenterPrevious2 = sphereIntersect2;
			next.tangentCircleRadiusPrevious = tRadius;
		}

		this.tangentCircleRadiusNextCos = MathUtils.cos(tangentCircleRadiusNext);
		this.tangentCircleRadiusPreviousCos = MathUtils.cos(tangentCircleRadiusPrevious);

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


	public Vec3f<?> getRadialPoint() {
		if(radialPoint == null) { 
			radialPoint = controlPoint.getOrthogonal();
			Vec3f<?> radialAxis = radialPoint.crossCopy(controlPoint);
			Rot rotateToRadial = new Rot(radialAxis, radius);
			radialPoint = rotateToRadial.applyToCopy(controlPoint);
		}
		return radialPoint;
	}
	public void setRadialPoint(SGVec_3f radialPoint) {
		this.radialPoint = radialPoint;
	}

	public Vec3f<?> getControlPoint() {
		return controlPoint;
	}

	public void setControlPoint(Vec3f<?> controlPoint) {
		this.controlPoint = controlPoint.copy();
		this.controlPoint.normalize();
		if(this.parentKusudama != null)
			this.parentKusudama.constraintUpdateNotification();
	}

	public float getRadius() {
		return radius;
	}

	public float getRadiusCosine() {
		return this.radiusCosine;
	}

	public void setRadius(float radius) {
		this.radius = radius;
		this.radiusCosine = MathUtils.cos(radius);
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
