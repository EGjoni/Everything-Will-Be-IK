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

package IK.doubleIK;
import math.doubleV.Rot;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;
import math.doubleV.sgRayd;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;

public abstract class AbstractLimitCone implements Saveable {

	Vec3d<?> controlPoint; 
	Vec3d<?> radialPoint; 

	//radius stored as  cosine to save on the acos call necessary for angleBetween. 
	private double radiusCosine; 
	private double radius; 
	private double cushionRadius;
	private double cushionCosine; 
	private double currentCushion = 1d;

	public AbstractKusudama parentKusudama;

	public Vec3d<?> tangentCircleCenterNext1;
	public Vec3d<?> tangentCircleCenterNext2;
	//public Vec3d<?> tangentCircleCenterPrevious1;
	//public Vec3d<?> tangentCircleCenterPrevious2;
	public double tangentCircleRadiusNext;
	public double tangentCircleRadiusNextCos;
	//public double tangentCircleRadiusPrevious;
	//public double tangentCircleRadiusPreviousCos;
	
	public Vec3d<?> cushionTangentCircleCenterNext1;
	public Vec3d<?> cushionTangentCircleCenterNext2;
	public Vec3d<?> cushionTangentCircleCenterPrevious1;
	public Vec3d<?> cushionTangentCircleCenterPrevious2;
	public double cushionTangentCircleRadiusNext;
	public double cushionTangentCircleRadiusNextCos;
	//public double cushionTangentCircleRadiusPrevious;
	//public double cushionTangentCircleRadiusPreviousCos;


	public static int BOUNDARY = 0;
	public static int CUSHION = 1; 
	


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
	public Vec3d<?>[] firstTriangleNext = new SGVec_3d[3];
	public Vec3d<?>[] secondTriangleNext = new SGVec_3d[3];

	public AbstractLimitCone(){}

	public AbstractLimitCone(Vec3d<?> direction, double rad, AbstractKusudama attachedTo) {
		setControlPoint(direction); 
		tangentCircleCenterNext1 = direction.getOrthogonal();
		tangentCircleCenterNext2 = SGVec_3d.mult(tangentCircleCenterNext1, -1);

		this.radius = Math.max(Double.MIN_VALUE, rad);
		this.radiusCosine = Math.cos(radius);
		this.cushionRadius = this.radius;
		this.cushionCosine = this.radiusCosine;
		parentKusudama = attachedTo;
	}
	
	/**
	 * 
	 * @param direction 
	 * @param rad 
	 * @param cushion range 0-1, how far toward the boundary to begin slowing down the rotation if soft constraints are enabled.
	 * Value of 1 creates a hard boundary. Value of 0 means it will always be the case that the closer a joint in the allowable region 
	 * is to the boundary, the more any further rotation in the direction of that boundary will be avoided.   
	 * @param attachedTo
	 */
	public AbstractLimitCone(Vec3d<?> direction, double rad, double cushion, AbstractKusudama attachedTo) {
		setControlPoint(direction); 
		tangentCircleCenterNext1 = direction.getOrthogonal();
		tangentCircleCenterNext2 = SGVec_3d.mult(tangentCircleCenterNext1, -1);

		this.radius = Math.max(Double.MIN_VALUE, rad);
		this.radiusCosine = Math.cos(radius);
		double adjustedCushion = Math.min(1d, Math.max(0.001d, cushion));
		this.cushionRadius = this.radius * adjustedCushion;
		this.cushionCosine = Math.cos(cushionRadius);
		parentKusudama = attachedTo;
	}



	/**
	 * 
	 * @param next
	 * @param input
	 * @param collisionPoint will be set to the rectified (if necessary) position of the input after accounting for collisions
	 * @return
	 */
	public boolean inBoundsFromThisToNext(AbstractLimitCone next, Vec3d<?> input, Vec3d<?> collisionPoint) {
		boolean isInBounds = false;//determineIfInBounds(next, input);
		//if(!isInBounds) {
		Vec3d<?> closestCollision = getClosestCollision(next, input);
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
	public <V extends Vec3d<?>> Vec3d<?> getClosestCollision(AbstractLimitCone next, V input) {
		Vec3d<?> result = getOnGreatTangentTriangle(next, input);
		if(result == null) {
			boolean[] inBounds = {false};
			result = closestPointOnClosestCone(next, input, inBounds);
		}
		return result;
	}
	
	public <V extends Vec3d<?>> Vec3d<?> getClosestPathPoint(AbstractLimitCone next, V input) {
		Vec3d<?> result = getOnPathSequence(next, input);
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
	public boolean determineIfInBounds(AbstractLimitCone next, Vec3d<?> input) {

		/**
		 * Procedure : Check if input is contained in this cone, or the next cone 
		 * 	if it is, then we're finished and in bounds. otherwise, 
		 * check if the point  is contained within the tangent radii, 
		 * 	if it is, then we're out of bounds and finished, otherwise  
		 * in the tangent triangles while still remaining outside of the tangent radii 
		 * if it is, then we're finished and in bounds. otherwise, we're out of bounds. 
		 */

		if(controlPoint.dot(input) >= radiusCosine)
			return true;
		else if (next != null && next.controlPoint.dot(input) >= next.radiusCosine ) 
			return true; 
		else {
			if(next == null) 
				return false;
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

			//Vec3d<?> planeNormal = controlPoint.crossCopy(tangentCircleCenterNext1);
			Vec3d c1xc2 = controlPoint.crossCopy(next.controlPoint);		
			double c1c2dir = input.dot(c1xc2);

			if(c1c2dir < 0.0) { 
				Vec3d c1xt1 = controlPoint.crossCopy(tangentCircleCenterNext1); 
				Vec3d t1xc2 = tangentCircleCenterNext1.crossCopy(next.controlPoint);	
				return  input.dot(c1xt1) > 0 && input.dot(t1xc2) > 0; 	
			} else {
				Vec3d t2xc1 = tangentCircleCenterNext2.crossCopy(controlPoint);	
				Vec3d c2xt2 = next.controlPoint.crossCopy(tangentCircleCenterNext2);
				return input.dot(t2xc1) > 0 && input.dot(c2xt2) > 0;
			}	
		}
	}	
	

	public <V extends Vec3d<?>> Vec3d<?> getOnPathSequence(AbstractLimitCone next, V input) {
		Vec3d c1xc2 = controlPoint.crossCopy(next.controlPoint);		
		double c1c2dir = input.dot(c1xc2);
		if(c1c2dir < 0.0) { 
			Vec3d c1xt1 = controlPoint.crossCopy(tangentCircleCenterNext1); 
			Vec3d t1xc2 = tangentCircleCenterNext1.crossCopy(next.controlPoint);	
			if(input.dot(c1xt1) > 0 && input.dot(t1xc2) > 0) {
					sgRayd tan1ToInput = new sgRayd(tangentCircleCenterNext1, input);
					SGVec_3d result = new SGVec_3d();
					tan1ToInput.intersectsPlane(new SGVec_3d(0,0,0), controlPoint, next.controlPoint, result);
					return result.normalize();
			}	else {
				return null;
			}
		} else {
			Vec3d t2xc1 = tangentCircleCenterNext2.crossCopy(controlPoint);	
			Vec3d c2xt2 = next.controlPoint.crossCopy(tangentCircleCenterNext2);
			if(input.dot(t2xc1) > 0 && input.dot(c2xt2) > 0) {				
				sgRayd tan2ToInput = new sgRayd(tangentCircleCenterNext2, input);
				SGVec_3d result = new SGVec_3d();
				tan2ToInput.intersectsPlane(new SGVec_3d(0,0,0), controlPoint, next.controlPoint, result);
				return result.normalize();
			}else {
				return null;
			}
		}	

	}
	
	
	/**
	 *
	 * @param next
	 * @param input
	 * @return null if inapplicable for rectification. the original point if in bounds, or the point rectified to the closest boundary on the path sequence
	 * between two cones if the point is out of bounds and applicable for rectification.
	 */
	public <V extends Vec3d<?>> Vec3d<?> getOnGreatTangentTriangle(AbstractLimitCone next, V input) {
		Vec3d c1xc2 = controlPoint.crossCopy(next.controlPoint);		
		double c1c2dir = input.dot(c1xc2);
		if(c1c2dir < 0.0) { 
			Vec3d c1xt1 = controlPoint.crossCopy(tangentCircleCenterNext1); 
			Vec3d t1xc2 = tangentCircleCenterNext1.crossCopy(next.controlPoint);	
			if( input.dot(c1xt1)> 0 &&  input.dot(t1xc2) > 0) {
				double toNextCos = input.dot(tangentCircleCenterNext1) ;
				if(toNextCos > tangentCircleRadiusNextCos) {
					Vec3d<?> planeNormal = tangentCircleCenterNext1.crossCopy(input); 
					Rot rotateAboutBy = new Rot(planeNormal, tangentCircleRadiusNext);
					return rotateAboutBy.applyToCopy(tangentCircleCenterNext1);
				}  else {
					return input;
				}
			} else {
				return null;
			}			
		} else {
			Vec3d t2xc1 = tangentCircleCenterNext2.crossCopy(controlPoint);	
			Vec3d c2xt2 = next.controlPoint.crossCopy(tangentCircleCenterNext2);
			if(input.dot(t2xc1) > 0 && input.dot(c2xt2) > 0) {
				if(input.dot(tangentCircleCenterNext2) > tangentCircleRadiusNextCos) {
					Vec3d<?> planeNormal = tangentCircleCenterNext2.crossCopy(input); 
					Rot rotateAboutBy = new Rot(planeNormal, tangentCircleRadiusNext);
					return rotateAboutBy.applyToCopy(tangentCircleCenterNext2);	
				} else {
					return input;
				} 
			}
			else {
				return null;
			}
		}
	}
	

	public <V extends Vec3d<?>> Vec3d<?> closestCone(AbstractLimitCone next, V input) {
		if(input.dot(controlPoint) > input.dot(next.controlPoint)) 
			return this.controlPoint.copy();
		else 
			return next.controlPoint.copy();
	}

	/**
	 * returns null if no rectification is required.
	 * @param next
	 * @param input
	 * @param inBounds
	 * @return
	 */
	public <V extends Vec3d<?>> Vec3d<?> closestPointOnClosestCone(AbstractLimitCone next, V input, boolean[] inBounds) {
		Vec3d<?> closestToFirst = this.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToFirst; 
		}
		Vec3d<?> closestToSecond = next.closestToCone(input, inBounds); 
		if(inBounds[0]) {
			return closestToSecond; 
		}

		double cosToFirst = input.dot(closestToFirst); 
		double cosToSecond = input.dot(closestToSecond);

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
	public <V extends Vec3d<?>> Vec3d<?> closestToCone(V input, boolean[] inBounds) {

		if(input.dot(this.getControlPoint()) > this.getRadiusCosine()) {
			inBounds[0] = true;
			return null;//input.copy();
		} else {
			Vec3d<?> axis = this.getControlPoint().crossCopy(input);
			//axis.normalize();
			//Rot pointDiff = new Rot(this.getControlPoint(), input);
			Rot rotTo = new Rot(axis, this.getRadius());
			//Rot rot2To = new Rot(pointDiff.getAxis(), this.getRadius());
			Vec3d<?>result = rotTo.applyToCopy(this.getControlPoint());
			inBounds[0] = false;
			return result;	
		}
	}


	public void updateTangentHandles(AbstractLimitCone next) {    
		this.controlPoint.normalize();
		updateTangentAndCushionHandles(next, BOUNDARY);
		updateTangentAndCushionHandles(next, CUSHION);
	}
	
	private void updateTangentAndCushionHandles(AbstractLimitCone next, int mode) {
		if(next !=null) {
			double radA = this._getRadius(mode);
			double radB = next._getRadius(mode);

			Vec3d<?> A = this.getControlPoint().copy();
			Vec3d<?> B = next.getControlPoint().copy(); 

			Vec3d<?> arcNormal = A.crossCopy(B); 
			
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
			double tRadius = ((Math.PI)-(radA+radB))/2d;

			/**
			 * Once we have the desired radius for our tangent circle, we may find the solution for its
			 * centers (usually, there are two).
			 */
			double boundaryPlusTangentRadiusA = radA + tRadius;
			double boundaryPlusTangentRadiusB = radB + tRadius;
			
			//the axis of this cone, scaled to minimize its distance to the tangent  contact points. 
			Vec3d<?> scaledAxisA =  SGVec_3d.mult(A, Math.cos(boundaryPlusTangentRadiusA));
			//a point on the plane running through the tangent contact points
			Vec3d<?> planeDir1A = new Rot(arcNormal, boundaryPlusTangentRadiusA).applyToCopy(A);
			//another poiint on the same plane
			Vec3d<?> planeDir2A = new Rot(A, Math.PI/2d).applyToCopy(planeDir1A);			
			
			Vec3d<?> scaledAxisB = Vec3d.mult(B, Math.cos(boundaryPlusTangentRadiusB));
			//a point on the plane running through the tangent contact points
			Vec3d<?> planeDir1B = new Rot(arcNormal, boundaryPlusTangentRadiusB).applyToCopy(B);
			//another poiint on the same plane
			Vec3d<?> planeDir2B = new Rot(B, Math.PI/2d).applyToCopy(planeDir1B);      
			

			// ray from scaled center of next cone to half way point between the circumference of this cone and the next cone. 
			sgRayd r1B = new sgRayd(planeDir1B, scaledAxisB); 
			sgRayd r2B = new sgRayd(planeDir1B, planeDir2B);			
			
			r1B.elongate(99);
			r2B.elongate(99);
			 
			Vec3d<?> intersection1 = r1B.intersectsPlane(scaledAxisA, planeDir1A, planeDir2A);
			Vec3d<?> intersection2 = r2B.intersectsPlane( scaledAxisA, planeDir1A, planeDir2A);

			sgRayd intersectionRay = new sgRayd(intersection1, intersection2);
			intersectionRay.elongate(99);

			Vec3d<?> sphereIntersect1 = new SGVec_3d(); 
			Vec3d<?> sphereIntersect2 = new SGVec_3d();
			Vec3d<?> sphereCenter = new SGVec_3d();
			intersectionRay.intersectsSphere(sphereCenter, 1f, sphereIntersect1, sphereIntersect2);  

			this.setTangentCircleCenterNext1(sphereIntersect1, mode); 
			this.setTangentCircleCenterNext2(sphereIntersect2, mode); 
			this.setTangentCircleRadiusNext(tRadius, mode);
		}

		
		//this.tangentCircleRadiusPreviousCos = Math.cos(tangentCircleRadiusPrevious);

		if(this.tangentCircleCenterNext1 == null) { 
			this.tangentCircleCenterNext1 = controlPoint.getOrthogonal().normalize();
			this.cushionTangentCircleCenterNext1 = controlPoint.getOrthogonal().normalize();
		}
		if(tangentCircleCenterNext2 == null) {
			tangentCircleCenterNext2 = SGVec_3d.mult(tangentCircleCenterNext1, -1).normalize();
			cushionTangentCircleCenterNext2 = SGVec_3d.mult(cushionTangentCircleCenterNext1, -1).normalize();
		}
		if(next != null)
			computeTriangles(next); 
	}
	
	
	private void setTangentCircleCenterNext1(Vec3d<?> point, int mode) {
		if(mode == CUSHION) {
			this.cushionTangentCircleCenterNext1 = point; 
		} else {
			this.tangentCircleCenterNext1 = point;
		}
	}
	private void setTangentCircleCenterNext2(Vec3d<?> point, int mode) {
		if(mode == CUSHION) {
			this.cushionTangentCircleCenterNext2 = point; 
		} else {
			this.tangentCircleCenterNext2 = point;
		}
	}	
	
	
	private void setTangentCircleRadiusNext(double rad, int mode ) {
		if(mode==CUSHION) {
			this.cushionTangentCircleRadiusNext = rad; 
			this.cushionTangentCircleRadiusNext = Math.cos(cushionTangentCircleRadiusNextCos);
		} else {
				this.tangentCircleRadiusNext = rad;
				this.tangentCircleRadiusNextCos = Math.cos(tangentCircleRadiusNext);
		}
	}	
	/**
	 * for internal and rendering use only. Avoid modifying any values in the resulting object, 
	 * which is returned by reference. 
	 * @param mode
	 * @return
	 */
	protected Vec3d<?> getTangentCircleCenterNext1(int mode) {
		if(mode == CUSHION)
				return cushionTangentCircleCenterNext1;
		return tangentCircleCenterNext1;
	}	
	
	protected double getTangentCircleRadiusNext(int mode) {
		if(mode == CUSHION) 
				return cushionTangentCircleRadiusNext; 
		return tangentCircleRadiusNext;
	}
	
	protected double getTangentCircleRadiusNextCos(int mode) {
		if(mode == CUSHION) 
				return cushionTangentCircleRadiusNextCos; 
		return tangentCircleRadiusNextCos;
	}

	/**
	 * for internal and rendering use only. Avoid modifying any values in the resulting object, 
	 * which is returned by reference. 
	 * @param mode
	 * @return
	 */
	protected Vec3d<?> getTangentCircleCenterNext2(int mode) {
		if(mode == CUSHION)
			return cushionTangentCircleCenterNext2;
		return tangentCircleCenterNext2;
	}

	protected double _getRadius(int mode) {
		if(mode == CUSHION)
			return cushionRadius;
		return radius;
	}
	
	protected double _getRadiusCosine(int mode) {
		if(mode == CUSHION)
			return cushionCosine;
		else return radiusCosine; 
	}

	private void computeTriangles(AbstractLimitCone next) {
		firstTriangleNext[1] = this.tangentCircleCenterNext1.normalize();		
		firstTriangleNext[0] = this.getControlPoint().normalize(); 
		firstTriangleNext[2] = next.getControlPoint().normalize();

		secondTriangleNext[1] = this.tangentCircleCenterNext2.normalize();		
		secondTriangleNext[0] = this.getControlPoint().normalize(); 
		secondTriangleNext[2] = next.getControlPoint().normalize();
	}


	public Vec3d<?> getControlPoint() {
		return controlPoint;
	}

	public void setControlPoint(Vec3d<?> controlPoint) {
		this.controlPoint = controlPoint.copy();
		this.controlPoint.normalize();
		if(this.parentKusudama != null)
			this.parentKusudama.constraintUpdateNotification();
	}

	public double getRadius() {
		return this.radius;
	}

	public double getRadiusCosine() {
		return this.radiusCosine;
	}

	public void setRadius(double radius) {
		this.radius = radius;
		this.radiusCosine = Math.cos(radius);
		this.parentKusudama.constraintUpdateNotification();
	}
	
	public double getCushionRadius() {
		return this.cushionRadius;
	}
	
	public double getCushionCosine() {
		return this.cushionCosine;
	}
	
	/**
	 * @param cushion range 0-1, how far toward the boundary to begin slowing down the rotation if soft constraints are enabled.
	 * Value of 1 creates a hard boundary. Value of 0 means it will always be the case that the closer a joint in the allowable region 
	 * is to the boundary, the more any further rotation in the direction of that boundary will be avoided.   
	 */
	public void setCushionRadius(double cushion) {
		double adjustedCushion = Math.min(1d, Math.max(0.001d, cushion));
		this.cushionRadius = this.radius * adjustedCushion;
		this.cushionCosine = Math.cos(cushionRadius);
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
		saveJSON.setDouble("radius", this.radius);
		return saveJSON;
	}


	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		this.parentKusudama = (AbstractKusudama) l.getObjectFromClassMaps(AbstractKusudama.class, j.getString("parentKusudama"));
		SGVec_3d controlPointJ = null;
		try {
			controlPointJ = new SGVec_3d(j.getJSONObject("controlPoint"));
		} catch(Exception e) {
			controlPointJ = new SGVec_3d(j.getJSONArray("controlPoint"));
		}
		
		controlPointJ.normalize();
		
		this.controlPoint = controlPointJ;
		this.radius = j.getDouble("radius");
		this.radiusCosine = Math.cos(radius);
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
