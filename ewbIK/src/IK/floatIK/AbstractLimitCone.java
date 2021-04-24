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
import math.floatV.CartesianAxes;
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
		tangentCircleCenterNext1 = location.getOrthogonal();
		tangentCircleCenterNext2 = SGVec_3f.mult(tangentCircleCenterNext1, -1);
		this.setRadius(rad);
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
	
	public <V extends Vec3f<?>> Vec3f<?> getClosestPathPoint(AbstractLimitCone next, V input) {
		Vec3f<?> result = getOnPathSequence(next, input);
		if(result == null) {
			result = closestCone(next, input);
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
			 *	as it didn't allow for early termination. . 
			 */

			//Vec3f<?> planeNormal = controlPoint.crossCopy(tangentCircleCenterNext1);
			Vec3f c1xc2 = controlPoint.crossCopy(next.controlPoint);		
			float c1c2fir = input.dot(c1xc2);

			if(c1c2fir < 0.0) { 
				Vec3f c1xt1 = controlPoint.crossCopy(tangentCircleCenterNext1); 
				Vec3f t1xc2 = tangentCircleCenterNext1.crossCopy(next.controlPoint);	
				return  input.dot(c1xt1) > 0 && input.dot(t1xc2) > 0; 	
			} else {
				Vec3f t2xc1 = tangentCircleCenterNext2.crossCopy(controlPoint);	
				Vec3f c2xt2 = next.controlPoint.crossCopy(tangentCircleCenterNext2);
				return input.dot(t2xc1) > 0 && input.dot(c2xt2) > 0;
			}	
		}
	}	
	
	public <V extends Vec3f<?>> Vec3f<?> closestCone(AbstractLimitCone next, V input) {
		if(input.dot(controlPoint) > input.dot(next.controlPoint)) 
			return this.controlPoint.copy();
		else 
			return next.controlPoint.copy();
	}

	public <V extends Vec3f<?>> Vec3f<?> getOnPathSequence(AbstractLimitCone next, V input) {
		Vec3f c1xc2 = controlPoint.crossCopy(next.controlPoint);		
		float c1c2fir = input.dot(c1xc2);
		if(c1c2fir < 0.0) { 
			Vec3f c1xt1 = controlPoint.crossCopy(tangentCircleCenterNext1); 
			Vec3f t1xc2 = tangentCircleCenterNext1.crossCopy(next.controlPoint);	
			if(input.dot(c1xt1) > 0 && input.dot(t1xc2) > 0) {
					sgRayf tan1ToInput = new sgRayf(tangentCircleCenterNext1, input);
					SGVec_3f result = new SGVec_3f();
					tan1ToInput.intersectsPlane(new SGVec_3f(0,0,0), controlPoint, next.controlPoint, result);
					return result.normalize();
			}	else {
				return null;
			}
		} else {
			Vec3f t2xc1 = tangentCircleCenterNext2.crossCopy(controlPoint);	
			Vec3f c2xt2 = next.controlPoint.crossCopy(tangentCircleCenterNext2);
			if(input.dot(t2xc1) > 0 && input.dot(c2xt2) > 0) {				
				sgRayf tan2ToInput = new sgRayf(tangentCircleCenterNext2, input);
				SGVec_3f result = new SGVec_3f();
				tan2ToInput.intersectsPlane(new SGVec_3f(0,0,0), controlPoint, next.controlPoint, result);
				return result.normalize();
			}else {
				return null;
			}
		}	

	}
	
	
	public <V extends Vec3f<?>> Vec3f<?> getOnGreatTangentTriangle(AbstractLimitCone next, V input) {
		Vec3f c1xc2 = controlPoint.crossCopy(next.controlPoint);		
		float c1c2fir = input.dot(c1xc2);
		if(c1c2fir < 0.0) { 
			Vec3f c1xt1 = controlPoint.crossCopy(tangentCircleCenterNext1); 
			Vec3f t1xc2 = tangentCircleCenterNext1.crossCopy(next.controlPoint);	
			if(input.dot(c1xt1) > 0 && input.dot(t1xc2) > 0) {
				if(input.dot(tangentCircleCenterNext1) > tangentCircleRadiusNextCos) {
					Vec3f<?> planeNormal = tangentCircleCenterNext1.crossCopy(input); 
					Rot rotateAboutBy = new Rot(planeNormal, tangentCircleRadiusNext);
					return rotateAboutBy.applyToCopy(tangentCircleCenterNext1);
				}  else {
					return input.copy();
				}
			} else {
				return null;
			}			
		} else {
			Vec3f t2xc1 = tangentCircleCenterNext2.crossCopy(controlPoint);	
			Vec3f c2xt2 = next.controlPoint.crossCopy(tangentCircleCenterNext2);
			if(input.dot(t2xc1) > 0 && input.dot(c2xt2) > 0) {
				if(input.dot(tangentCircleCenterNext2) > tangentCircleRadiusNextCos) {
					Vec3f<?> planeNormal = tangentCircleCenterNext2.crossCopy(input); 
					Rot rotateAboutBy = new Rot(planeNormal, tangentCircleRadiusNext);
					return rotateAboutBy.applyToCopy(tangentCircleCenterNext2);	
				} else {
					return input.copy();
				} 
			}
			else {
				return null;
			}
		}	

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
		float controlPointDotProduct = input.dot(this.getControlPoint()); 
		float radiusCosine = this.getRadiusCosine();
		if(controlPointDotProduct > radiusCosine) {
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
			float boundaryPlusTangentRadiusA = radA + tRadius;
			float boundaryPlusTangentRadiusB = radB + tRadius;
			
			//the axis of this cone, scaled to minimize its distance to the tangent  contact points. 
			Vec3f<?> scaledAxisA =  SGVec_3f.mult(A, MathUtils.cos(boundaryPlusTangentRadiusA));
			//a point on the plane running through the tangent contact points
			Vec3f<?> planeDir1A = new Rot(arcNormal, boundaryPlusTangentRadiusA).applyToCopy(A);
			//another poiint on the same plane
			Vec3f<?> planeDir2A = new Rot(A, MathUtils.PI/2f).applyToCopy(planeDir1A);			
			
			Vec3f<?> scaledAxisB = Vec3f.mult(B, MathUtils.cos(boundaryPlusTangentRadiusB));
			//a point on the plane running through the tangent contact points
			Vec3f<?> planeDir1B = new Rot(arcNormal, boundaryPlusTangentRadiusB).applyToCopy(B);
			//another poiint on the same plane
			Vec3f<?> planeDir2B = new Rot(B, MathUtils.PI/2f).applyToCopy(planeDir1B);      
			

			// ray from scaled center of next cone to half way point between the circumference of this cone and the next cone. 
			sgRayf r1B = new sgRayf(planeDir1B, scaledAxisB); 
			sgRayf r2B = new sgRayf(planeDir1B, planeDir2B);
			
			
			r1B.elongate(99);
			 r2B.elongate(99);
			 
			Vec3f<?> intersection1 = r1B.intersectsPlane(scaledAxisA, planeDir1A, planeDir2A);
			Vec3f<?> intersection2 = r2B.intersectsPlane( scaledAxisA, planeDir1A, planeDir2A);

			sgRayf intersectionRay = new sgRayf(intersection1, intersection2);
			intersectionRay.elongate(99);

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
		this.radius = MathUtils.max(Float.MIN_VALUE, radius);
		this.radiusCosine = MathUtils.cos(this.radius);
		if(this.parentKusudama != null)
			this.parentKusudama.constraintUpdateNotification();
	}

	public AbstractKusudama getParentKusudama() {
		return parentKusudama;
	}

	@Override
	public void makeSaveable(SaveManager saveManager) {
		saveManager.addToSaveState(this);
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
		SGVec_3f controlPointJ = null;
		try {
			controlPointJ = new SGVec_3f(j.getJSONObject("controlPoint"));
		} catch(Exception e) {
			controlPointJ = new SGVec_3f(j.getJSONArray("controlPoint"));
		}
		
		controlPointJ.normalize();
		
		this.controlPoint = controlPointJ;
		this.setRadius(j.getFloat("radius"));
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
