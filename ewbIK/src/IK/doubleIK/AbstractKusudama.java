/**
 * 
 */
package IK.doubleIK;
import java.util.ArrayList;

import data.EWBIKLoader;
import data.EWBIKSaver;
import data.JSONObject;
import data.LoadManager;
import data.SaveManager;
import data.Saveable;
import sceneGraph.*;
import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.MRotation;
import sceneGraph.math.doubleV.Rot;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.math.doubleV.Vec3d;
import sceneGraph.math.doubleV.sgRayd;
/**
 * @author Eron
 *
 */
public abstract class AbstractKusudama implements Constraint, Saveable {


	protected AbstractAxes limitingAxes; 

	/**
	 * An array containing all of the KusudamaExample's limitCones. The kusudama is built up
	 * with the expectation that any limitCone in the array is connected to the cone at the previous element in the array, 
	 * and the cone at the next element in the array.  
	 */
	protected ArrayList<AbstractLimitCone> limitCones = new ArrayList<AbstractLimitCone>();

	/**
	 * Defined as some Angle in radians about the limitingAxes Y axis, 0 being equivalent to the
	 * limitingAxes Z axis. 
	 */
	protected double minAxialAngle = G.PI; 
	/**
	 * Defined as some Angle in radians about the limitingAxes Y axis, 0 being equivalent to the
	 * minAxialAngle
	 */
	protected double range = G.PI*3;

	protected boolean orientationallyConstrained = false;
	protected boolean axiallyConstrained = false;
	
	//for IK solvers. Defines the weight ratio between the unconstrained IK solved orientation and the constrained orientation for this bone 
	//per iteration. This should help stabilize solutions somewhat by allowing for soft constraint violations. 
	protected Double strength = 1d; 

	protected AbstractBone attachedTo;  


	public AbstractKusudama() {
	}

	public AbstractKusudama(AbstractBone forBone) {
		this.attachedTo = forBone; 
		this.limitingAxes = forBone.getMajorRotationAxes();
	}


	public void constraintUpdateNotification() {
		this.updateTangentRadii();
		this.updateRotationalFreedom();
	}

	/**
	 * If true, automatically reOrients the limitingAxes 
	 * to minimize potential for degenerate rotations. 
	 */
	protected boolean autoOptimize = true;

	public void optimizeLimitingAxes() {
		AbstractAxes originalLimitingAxes = limitingAxes.getGlobalCopy();
		if(autoOptimize) {
			ArrayList<SGVec_3d> directions = new ArrayList<>(); 
			if(getLimitCones().size() == 1) {
				directions.add((limitCones.get(0).getControlPoint()).copy());
			} else {
				for(int i = 0; i<getLimitCones().size()-1; i++) {
					SGVec_3d thisC = getLimitCones().get(i).getControlPoint().copy();
					SGVec_3d nextC = getLimitCones().get(i+1).getControlPoint().copy();
					Rot thisToNext = new Rot(thisC, nextC); 
					Rot halfThisToNext = new Rot(thisToNext.getAxis(), thisToNext.getAngle()/2d); 

					SGVec_3d halfAngle = halfThisToNext.applyToCopy(thisC);
					halfAngle.normalize(); halfAngle.mult(thisToNext.getAngle());
					directions.add(halfAngle);
				}	
			}

			SGVec_3d newY = new SGVec_3d();
			for(SGVec_3d dv: directions) {
				newY.add(dv); 
			}

			newY.div(directions.size()); 
			if(newY.mag() != 0 && !Double.isNaN(newY.y)) {
				newY.normalize();
			} else {
				newY = new SGVec_3d(0,1d,0);
			}

			sgRayd newYRay = new sgRayd(new SGVec_3d(0,0,0), newY);

			Rot oldYtoNewY = new Rot(limitingAxes.y_().heading(), originalLimitingAxes.getGlobalOf(newYRay).heading());
			limitingAxes.rotateBy(oldYtoNewY);

			for(AbstractLimitCone lc : getLimitCones()) {
				originalLimitingAxes.setToOrthoNormalizedGlobalOf(lc.controlPoint, lc.controlPoint);
				originalLimitingAxes.setToOrthoNormalizedGlobalOf(lc.radialPoint, lc.radialPoint);
				limitingAxes.setToOrthoNormalLocalOf(lc.controlPoint, lc.controlPoint);
				limitingAxes.setToOrthoNormalLocalOf(lc.radialPoint, lc.radialPoint);
				lc.controlPoint.normalize(); 
				lc.radialPoint.normalize();
			}

			this.updateTangentRadii();			
			//this.recomputeBaseInverseLimitAxes();
		}
	}

	
	public void updateTangentRadii() {

		for (int i=0; i<limitCones.size(); i++) {      
			AbstractLimitCone next = i<limitCones.size() -1 ? limitCones.get(i+1) : null;
			limitCones.get(i).updateTangentHandles(next);
		}
	}


	/**
	 * if true (default), the axes relative to which constraints are computed will be automatically reoriented on any changes in LimitCone position 
	 * so as to minimize the potential for undesirable twist rotations. In general, auto-optimization attempts to point the y-component of the constraint
	 * axes in the direction that places it within in an oreintation allowed by the constraint, and roughly as far as possible from any orientations not allowed by the constraint.  
	 * @param b
	 */
	public void setAutoOptimizeConstraintOrientationBasis(boolean b) {
		autoOptimize = b;
	}


	sgRayd boneRay = new sgRayd(new SGVec_3d(), new SGVec_3d());
	sgRayd constrainedRay = new sgRayd(new SGVec_3d(), new SGVec_3d());
	Rot rectifiedRot = new Rot(MRotation.IDENTITY);
	/**
	 * Snaps the bone this KusudamaExample is constraining to be within the KusudamaExample's orientational and axial limits. 
	 */
	public void snapToLimits() {
		//System.out.println("snapping to limits");
		if(orientationallyConstrained) {
			setAxesToOrientationSnap(attachedTo().localAxes(), limitingAxes, strength);
		}
		if(axiallyConstrained) {
			snapToTwistLimits(attachedTo().localAxes(), limitingAxes);
		}
	}


	public void snapToSoftLimits(AbstractAxes currentBoneAxes, AbstractAxes targetBoneAxes, AbstractAxes toSet) {
		//System.out.println("snapping to limits");
		if(orientationallyConstrained) {
			setAxesToSoftOrientationSnap(currentBoneAxes, targetBoneAxes, limitingAxes, toSet);
			//setAxesToSnapped(toSet, limitingAxes);
		}
		if(axiallyConstrained) {
			//snapToTwistLimits(attachedTo().localAxes(), limitingAxes);
		}
	}

	/**
	 * Presumes the input axes are the bone's localAxes, and rotates 
	 * them to satisfy the snap limits. 
	 * 
	 * @param toSet
	 */
	public void setAxesToSnapped(AbstractAxes toSet, AbstractAxes limitingAxes) {
		if(limitingAxes != null) {					
			if(orientationallyConstrained) {
				setAxesToOrientationSnap(toSet, limitingAxes, strength);
			} 		
			if(axiallyConstrained) {
				snapToTwistLimits(toSet, limitingAxes);
			}		
		}

	}

	@Override
	public boolean isInLimits_(SGVec_3d globalPoint) {
		double[] inBounds = {1d}; 
		//boneRay.p1.set(toSet.origin()); boneRay.p2.set(toSet.y().getScaledTo(attachedTo.boneHeight));    
		SGVec_3d inLimits = this.pointInLimits(globalPoint, inBounds, limitingAxes);    
		return inBounds[0] > 0d;
	}


	/**
	 * Presumes the input axes are the bone's localAxes, and rotates 
	 * them to satisfy the snap limits. 
	 * 
	 * @param toSet
	 */
	public void setAxesToOrientationSnap(AbstractAxes toSet, AbstractAxes limitingAxes, Double snapStrength) {
		double[] inBounds = {1d}; 
		boneRay.p1().set(toSet.origin_()); boneRay.p2().set(toSet.y_().getScaledTo(attachedTo.boneHeight));    
		SGVec_3d inLimits = this.pointInLimits(boneRay.p2(), inBounds, limitingAxes);
		/*if(inBounds[0] == -1) {
			this.pointInLimits(boneRay.p2, inBounds, limitingAxes);   
		}*/


		if (inBounds[0] == -1 && inLimits != null) {     
			constrainedRay.p1().set(boneRay.p1()); constrainedRay.p2().set(inLimits); 
			rectifiedRot.set(boneRay.heading(), constrainedRay.heading());      
			
			//if( snapStrength >= 1 || snapStrength == null || snapStrength <=0) {
				toSet.rotateBy(rectifiedRot);
				toSet.updateGlobal();			
			/*} else { 
				toSet.rotateBy(new Rot(Rot.slerp(snapStrength, MRotation.IDENTITY, rectifiedRot.rotation)));
				toSet.updateGlobal();			
			}*/
		}
		
	}


	public void setAxesToSoftOrientationSnap(AbstractAxes currentBoneAxes, AbstractAxes targetBoneAxes, AbstractAxes limitingAxes, AbstractAxes resultAxes) {
		boneRay.p1().set(currentBoneAxes.origin_()); boneRay.p2().set(currentBoneAxes.y_().getScaledTo(attachedTo.boneHeight));   
		constrainedRay.p1().set(targetBoneAxes.origin_()); constrainedRay.p2().set(targetBoneAxes.y_().getScaledTo(attachedTo.boneHeight));   

		Rot naiveRot = new Rot(boneRay.heading(), constrainedRay.heading());

		SGVec_3d pointInSoftBound = pointInSoftBound(boneRay.p2(), constrainedRay.p2(), limitingAxes);
		sgRayd resultRay = new sgRayd(limitingAxes.origin_(), pointInSoftBound); 
		resultRay.setP2(resultRay.getScaledTo(attachedTo.getBoneHeight()));
		Rot rectifiedRot = new Rot(boneRay.heading(), resultRay.heading());
		//if(resultAxes != targetBoneAxes) 
		resultAxes.alignGlobalsTo(currentBoneAxes);
		resultAxes.rotateBy(rectifiedRot);
		targetBoneAxes.updateGlobal();

	} 


	/**
	 * KusudamaExample constraints decompose the bone orientation into a swing component, and a twist component. 
	 * The "Swing" component is the final direction of the bone. The "Twist" component represents how much 
	 * the bone is rotated about its own final direction. Where limit cones allow you to constrain the "Swing" 
	 * component, this method lets you constrain the "twist" component. 
	 * 
	 * @param minAnlge some angle in radians about the major rotation frame's y-axis to serve as the first angle within the range that the bone is allowed to twist. 
	 * @param inRange some angle in radians added to the minAngle. if the bone's local Z goes maxAngle radians beyond the minAngle, it is considered past the limit. 
	 * This value is always interpreted as being in the positive direction. For example, if this value is -PI/2, the entire range from minAngle to minAngle + 3PI/4 is
	 * considered valid.  
	 */
	public void setAxialLimits(double minAngle, double inRange) {
		minAxialAngle = minAngle;
		range = toTau(inRange);
		constraintUpdateNotification();
	}

	//protected AbstractAxes limitLocalAxes;


	//FIXME: Implement a "localOfOrientation" function in Basis and have this function utilize it to determine axial offset
	/**
	 * 
	 * @param toSet
	 * @param limitingAxes
	 * @return radians of twist required to snap bone into twist limits (0 if bone is already in twist limits)
	 */
	public double snapToTwistLimits(AbstractAxes toSet, AbstractAxes limitingAxes) {

		if(!axiallyConstrained) return 0d;

		limitingAxes.updateGlobal();
		//this.limitingAxes.globalMBasis.setToLocalOf(this.attachedTo().localAxes().globalMBasis, limitLocalAxes.localMBasis);
		//this.limitingAxes.globalMBasis.setToGlobalOf(limitLocalAxes.localMBasis, limitLocalAxes.globalMBasis);
		Rot alignRot = limitingAxes.globalMBasis.rotation.applyInverseTo(toSet.globalMBasis.rotation);
		Rot[] decomposition = alignRot.getSwingTwist(new SGVec_3d(0,1,0));
		/*limitLocalAxes.alignToParent();
        limitLocalAxes.rotateTo(decomposition[1]);
        limitLocalAxes.markDirty(); limitLocalAxes.updateGlobal();*/
		double angleDelta = decomposition[1].getAngle() * decomposition[1].getAxis().y*-1 * limitingAxes.getGlobalChirality()*
				(limitingAxes.globalMBasis.flippedAxes[AbstractAxes.Y] ? -1 : 1);

		angleDelta = toTau(angleDelta);
		double fromMinToAngleDelta = toTau(signedAngleDifference(angleDelta, G.TAU - this.minAxialAngle())); 

		if(fromMinToAngleDelta <  G.TAU - range ) {                          
			double distToMin = Math.abs(signedAngleDifference(angleDelta, G.TAU-this.minAxialAngle()));
			double distToMax =  Math.abs(signedAngleDifference(angleDelta, G.TAU-(this.minAxialAngle()+range)));
			double turnDiff = limitingAxes.getGlobalChirality(); 
			if(distToMin < distToMax){
				turnDiff = turnDiff*(fromMinToAngleDelta);
				toSet.rotateAboutY(turnDiff, true);                                                                                                         
			} else {                              
				turnDiff = turnDiff*(range - (G.TAU-fromMinToAngleDelta));
				toSet.rotateAboutY(turnDiff, true);
			}
			return turnDiff < 0 ? turnDiff*-1 : turnDiff;
		} else {
			return 0;
		}
	}

	public boolean inTwistLimits(AbstractAxes boneAxes, AbstractAxes limitingAxes) {

		limitingAxes.updateGlobal();
		Rot alignRot = limitingAxes.globalMBasis.rotation.applyInverseTo(boneAxes.globalMBasis.rotation);
		Rot[] decomposition = alignRot.getSwingTwist(new SGVec_3d(0,1,0));

		double angleDelta = decomposition[1].getAngle() * decomposition[1].getAxis().y*-1 * limitingAxes.getGlobalChirality()*
				(limitingAxes.globalMBasis.flippedAxes[AbstractAxes.Y] ? -1 : 1);;

				angleDelta = toTau(angleDelta);
				double fromMinToAngleDelta = toTau(signedAngleDifference(angleDelta, G.TAU - this.minAxialAngle())); 

				if(fromMinToAngleDelta <  G.TAU - range ) {                          
					double distToMin = Math.abs(signedAngleDifference(angleDelta, G.TAU-this.minAxialAngle()));
					double distToMax =  Math.abs(signedAngleDifference(angleDelta, G.TAU-(this.minAxialAngle()+range)));
					if(distToMin < distToMax){ 
						return false;                                                                                                        
					} else {                                                                                                                                                                                                           
						return false;   
					}
				}
				return true;
	}

	public double signedAngleDifference(double minAngle, double base) {
		double d = Math.abs(minAngle - base) % G.TAU; 
		double r = d > G.PI ? G.TAU - d : d;

		double sign = (minAngle - base >= 0 && minAngle - base <=G.PI) || (minAngle - base <=-G.PI && minAngle - base>= -G.TAU) ? 1f : -1f; 
		r *= sign;
		return r;
	}

	/**
	 * Given a point (in global coordinates), checks to see if a ray can be extended from the KusudamaExample's
	 * origin to that point, such that the ray in the KusudamaExample's reference frame is within the range allowed by the KusudamaExample's
	 * coneLimits.
	 * If such a ray exists, the original point is returned (the point is within the limits). 
	 * If it cannot exist, the tip of the ray within the kusudama's limits that would require the least rotation
	 * to arrive at the input point is returned.
	 * @param inPoint the point to test.
	 * @param returns a number from -1 to 1 representing the point's distance from the boundary, 0 means the point is right on 
	 * the boundary, 1 means the point is within the boundary and on the path furthest from the boundary. any negative number means 
	 * the point is outside of the boundary, but does not signify anything about how far from the boundary the point is.  
	 * @return the original point, if it's in limits, or the closest point which is in limits.
	 */
	public SGVec_3d pointInLimits(SGVec_3d inPoint, double[] inBounds, AbstractAxes limitingAxes) {

		SGVec_3d point = new SGVec_3d(); 
		limitingAxes.setToLocalOf(inPoint, point);
		point.normalize(); 
		//point.mult(attachedTo.boneHeight);

		inBounds[0] = -1;

		SGVec_3d closestCollisionPoint = null;
		if (limitCones.size() > 1 && this.orientationallyConstrained) {
			for (int i =0; i<limitCones.size() -1; i++) {
				SGVec_3d collisionPoint = new SGVec_3d(0,0,0);
				AbstractLimitCone nextCone = limitCones.get(i+1);				
				boolean inSegBounds = limitCones.get(i).inBoundsFromThisToNext(nextCone, point, collisionPoint);
				
				if(inSegBounds == false && (closestCollisionPoint == null 
						|| SGVec_3d.angleBetween(collisionPoint, point) < SGVec_3d.angleBetween(point,closestCollisionPoint))) {
					closestCollisionPoint = collisionPoint.copy();     
				} else if( inSegBounds == true) {
					inBounds[0] = 1;  
				}
			}   

			if (inBounds[0] == -1) { 
				return limitingAxes.getGlobalOf(closestCollisionPoint);
			} else { 
				return inPoint;
			}
		} else if(orientationallyConstrained){
			if(SGVec_3d.angleBetween(point, limitCones.get(0).getControlPoint()) < limitCones.get(0).getRadius()) {
				inBounds[0] = 1;
				return inPoint;
			} else {
				Rot toLimit = new Rot(limitCones.get(0).getControlPoint(), point);
				toLimit = new Rot(toLimit.getAxis(), limitCones.get(0).getRadius());
				return limitingAxes.getGlobalOf(toLimit.applyToCopy(limitCones.get(0).getControlPoint()));
			}
		} else {
			inBounds[0] = 1;
			return inPoint;
		}
	}

	/**
	 * Given a start and end point (in local coordinates), build a rotation to take the start point to the end point,
	 * then computes a penalty on the rotation such that the end point does not exceed the kusudama's limits
	 * and avoids rotation toward those limits in proportion to the point's distance to those limits.  
	 * If such a ray exists, the original point is returned (the point is within the limits). 
	 * If it cannot exist, the tip of the ray within the kusudama's limits that would require the least rotation
	 * to arrive at the input point is returned.
	 * @param inPoint the point to test.
	 * @param returns a number from -1 to 1 representing the point's distance from the boundary, 0 means the point is right on 
	 * the boundary, 1 means the point is within the boundary and on the path furthest from the boundary. any negative number means 
	 * the point is outside of the boundary, but does not signify anything about how far from the boundary the point is.  
	 * @return the original point, if it's in limits, or the closest point which is in limits.
	 */
	public void getPenaltyRotationAt(SGVec_3d startPoint, SGVec_3d goalPoint) {
		Rot rotation = new Rot(startPoint, goalPoint); 



	}

	/**
	 * Given a point (in local coordinates), checks to see if a ray can be extended from the KusudamaExample's
	 * origin to that point, such that the ray in the KusudamaExample's reference frame is within the range allowed by the KusudamaExample's
	 * coneLimits.
	 * If such a ray exists, the original point is returned (the point is within the limits). 
	 * If it cannot exist, the tip of the ray within the kusudama's limits that would require the least rotation
	 * to arrive at the input point is returned.
	 * @param inPoint the point to test.
	 * @param returns a number from -1 to 1 representing the point's distance from the boundary, 0 means the point is right on 
	 * the boundary, 1 means the point is within the boundary and on the path furthest from the boundary. any negative number means 
	 * the point is outside of the boundary, but does not signify anything about how far from the boundary the point is.  
	 * @return the original point, if it's in limits, or the closest point which is in limits.
	 */
	public SGVec_3d pointInLocalLimits(SGVec_3d inPoint, double[] inBounds) {

		SGVec_3d point = inPoint.copy();
		point.normalize(); 
		//point.mult(attachedTo.boneHeight);

		inBounds[0] = -1;

		SGVec_3d closestCollisionPoint = null;
		if (limitCones.size() > 1 && this.orientationallyConstrained) {
			for (int i =0; i<limitCones.size() -1; i++) {
				SGVec_3d collisionPoint = new SGVec_3d(0,0,0);
				AbstractLimitCone nextCone = (limitCones.size() > 1)? limitCones.get(i+1) : null;
				boolean inSegBounds = limitCones.get(i).inBoundsFromThisToNext(nextCone, point, collisionPoint);

				if(inSegBounds == false && (closestCollisionPoint == null 
						|| SGVec_3d.angleBetween(collisionPoint, point) < SGVec_3d.angleBetween(point,closestCollisionPoint))) {
					closestCollisionPoint = collisionPoint.copy();     
				} else if( inSegBounds == true) {
					inBounds[0] = 1;  
				}
			}   

			if (inBounds[0] == -1) { 
				return closestCollisionPoint.mult(attachedTo.boneHeight);
			} else { 
				return inPoint.mult(attachedTo.boneHeight);
			}  
		} else {
			if(SGVec_3d.angleBetween(point, limitCones.get(0).getControlPoint()) < limitCones.get(0).getRadius()) {
				inBounds[0] = 1;
				return inPoint.mult(attachedTo.boneHeight);
			} else {
				Rot toLimit = new Rot(limitCones.get(0).getControlPoint(), point);
				toLimit = new Rot(toLimit.getAxis(), limitCones.get(0).getRadius());
				return toLimit.applyToCopy(limitCones.get(0).getControlPoint()).mult(attachedTo.boneHeight);
			}
		}
	}



	/**
	 * Given a point representing the tip of this bone in local coordinates prior to rotation
	 * and a point representing the tip of this bone in local coordinates after rotation
	 * returns the point where it would be after penalizing the portion of the rotation
	 * moving toward the nearest boundary.
	 * @param previousPosition
	 * @param currentPosition
	 * @return
	 */
	public SGVec_3d pointInSoftBound(SGVec_3d currentPosition_global, SGVec_3d targetPosition_global, AbstractAxes limitingAxes) {
		boolean inBounds = false;
		SGVec_3d vPos = limitingAxes.getLocalOf(currentPosition_global).normalize();
		SGVec_3d targetPosition = limitingAxes.getLocalOf(targetPosition_global).normalize();
		int coneCount = limitCones.size();
		boolean discardAttempted = false; 
		boolean coneBorderDetected = false; 
		SGVec_3d totalPenaltyAxis = new SGVec_3d(0.0,0.0,0.0);
		double highestLimit = 1.0;				
		double prevPathHeight = 0.0f;
		boolean inPrevPath = false;
		boolean inPathTri = false; 
		double pathHeight= 0.0;


		SGVec_3d pathPointClosestToOriging =   limitCones.get(0).getControlPoint();  
		SGVec_3d pathPointClosestToTarget = limitCones.get(0).getControlPoint();  

		Rot toTarg = null;

		if(coneCount == 1) {
			SGVec_3d controlPointPos = limitCones.get(0).getControlPoint(); 
			highestLimit = SGVec_3d.angleBetween(limitCones.get(0).getControlPoint(), vPos)/limitCones.get(0).radius; 
			//totalPenaltyAxis.add(limitCones.get(0).getControlPoint().cross(vPos));
			toTarg = new Rot(vPos, targetPosition);
			totalPenaltyAxis.add(toTarg.getAxis());
		}

		for(int i=1; i < coneCount; i++) {
			boolean isInTan = false; 
			boolean isInCone = false;
			double[] distToBorder = {0.0};
			AbstractLimitCone prevCone = limitCones.get(i-1);
			AbstractLimitCone thisCone =  limitCones.get(i);
			SGVec_3d tripletDirection = new SGVec_3d(); 	
			int subtractionType_prevToThis = thisCone.getScaledPenaltyRotationFromThisToAdjacentConeifInBounds(prevCone, vPos, -1, distToBorder, tripletDirection);
			double tripletSubtractionAmount = distToBorder[0]; 					
			//boolean newSmallest = highestLimit > max(0.0, 1.0 - tripletSubtractionAmount) ? true : false;
			System.out.println("triplet: " + (tripletSubtractionAmount));
			highestLimit -=  Math.max(0.0, tripletSubtractionAmount);
			totalPenaltyAxis.add(tripletDirection.mult(1.0 - tripletSubtractionAmount));
		}
		System.out.println("PreSmoothPenalty coeffecient: " + (highestLimit));
		highestLimit = G.smoothstep(0.0, 1.0, highestLimit);
		System.out.println("Penalty coeffecient: " + (highestLimit));
		totalPenaltyAxis.add(totalPenaltyAxis.normalize());  

		/**
		 * we want to get the rotation that takes us from the original position to the target position. 
		 * then we want to remove from that rotation as much of it as is penalized by our limit cone
		 */

		Rot desiredRotation = new Rot(vPos, targetPosition);
		Rot[] aboutAxis = desiredRotation.getSwingTwist(totalPenaltyAxis);
		double desiredAngleAboutAxis  =  aboutAxis[1].getAngle(); 
		double penalizedAngle = desiredAngleAboutAxis * (1.0-highestLimit);
		System.out.println("---- total desired angle: " + Math.toDegrees(desiredRotation.getAngle()));
		System.out.println("------DesiredAngle About Axis: " +Math.toDegrees(desiredAngleAboutAxis));
		System.out.println("------PenalizedAngle: " +Math.toDegrees(penalizedAngle));
		System.out.println("---Ratio of penalized to desired: " + penalizedAngle/desiredAngleAboutAxis ); 
		//Rot penalizedRotation = new Rot(aboutAxis[1].getAxis(), penalizedAngle).applyTo(aboutAxis[0]);
		Rot unPenalizedRotation = new Rot(new Rot(totalPenaltyAxis, desiredAngleAboutAxis).rotation.multiply(aboutAxis[0].rotation));
		Rot penalizedRotation = new Rot(aboutAxis[0].rotation.multiply(new Rot(totalPenaltyAxis, penalizedAngle).rotation));
		Rot naivelyPenalizedRotation = new Rot(desiredRotation.getAxis(), desiredRotation.getAngle()*(1-highestLimit));

		//SGVec_3d result = targetPosition;
		SGVec_3d result = unPenalizedRotation.applyToCopy(vPos);
		return limitingAxes.getGlobalOf(result);
	}


	//public double softLimit

	public AbstractBone attachedTo() {
		return this.attachedTo;
	}

	/**
	 * Add a LimitCone to the KusudamaExample. 
	 * @param newPoint where on the KusudamaExample to add the LimitCone (in KusudamaExample's local coordinate frame defined by its bone's majorRotationAxes))
	 * @param radius the radius of the limitCone
	 * @param previous the LimitCone adjacent to this one (may be null if LimitCone is not supposed to be between two existing LimitCones)
	 * @param next the other LimitCone adjacent to this one (may be null if LimitCone is not supposed to be between two existing LimitCones)
	 */
	public void addLimitCone(SGVec_3d newPoint, double radius, AbstractLimitCone previous, AbstractLimitCone next) {
		int insertAt = 0;

		if(next == null || limitCones.size() == 0) {
			addLimitConeAtIndex(-1, newPoint, radius);
		} else if(previous != null) {
			insertAt = limitCones.indexOf(previous)+1;
		} else {
			insertAt = (int)G.max(0, limitCones.indexOf(next));
		}
		addLimitConeAtIndex(insertAt, newPoint, radius);		
	}

	public void removeLimitCone(AbstractLimitCone limitCone) {
		this.limitCones.remove(limitCone);
		this.updateTangentRadii();
		this.updateRotationalFreedom();
	}

	public abstract AbstractLimitCone createLimitConeForIndex(int insertAt, SGVec_3d newPoint, double radius);
	
	/**
	 * Adds a LimitCone to the KusudamaExample. LimitCones are reach cones which can be arranged sequentially. The KusudamaExample will infer
	 * a smooth path leading from one LimitCone to the next. 
	 * 
	 * Using a single LimitCone is functionally equivalent to a classic reachCone constraint. 
	 * 
	 * @param insertAt the intended index for this LimitCone in the sequence of LimitCones from which the KusudamaExample will infer a path. @see IK.AbstractKusudama.limitCones limitCones array. 
	 * @param newPoint where on the KusudamaExample to add the LimitCone (in KusudamaExample's local coordinate frame defined by its bone's majorRotationAxes))
	 * @param radius the radius of the limitCone
	 */
	public void addLimitConeAtIndex(int insertAt, SGVec_3d newPoint, double radius) {
		AbstractLimitCone newCone = createLimitConeForIndex(insertAt, newPoint, radius);
		if(insertAt == -1) {
			limitCones.add(newCone);
		} else {
			limitCones.add(insertAt, newCone);
		}		
		this.updateTangentRadii();
		this.updateRotationalFreedom();
	}


	public double toTau(double angle) {
		double result = angle; 
		if(angle<0) {
			result = (2*Math.PI) + angle;
		}
		result = result%(Math.PI*2);
		return result;
	}



	/**
	 * @return the limitingAxes of this KusudamaExample (these are just its parentBone's majorRotationAxes)
	 */
	public AbstractAxes limitingAxes() {
		//if(inverted) return inverseLimitingAxes; 
		 return limitingAxes;
	}

	/**
	 * 
	 * @return the lower bound on the axial constraint
	 */
	public double minAxialAngle(){
		return minAxialAngle;
	} 

	public double maxAxialAngle(){
		return range;
	}

	/**
	 * the upper bound on the axial constraint in absolute terms
	 * @return
	 */
	public double absoluteMaxAxialAngle(){
		return (minAxialAngle + range)%(Math.PI*2d);
		//return signedAngleDifference(range+minAxialAngle, 0);
	}

	public boolean isAxiallyConstrained() {
		return axiallyConstrained;
	}

	public boolean isOrientationallyConstrained() {
		return orientationallyConstrained;
	}

	public void disableOrientationalLimits() {
		this.orientationallyConstrained = false;
	}

	public void enableOrientationalLimits() {
		this.orientationallyConstrained = true;
	}

	public void toggleOrientationalLimits() {
		this.orientationallyConstrained = !this.orientationallyConstrained;
	}

	public void disableAxialLimits() {
		this.axiallyConstrained = false;
	}

	public void enableAxialLimits() {
		this.axiallyConstrained = true;
	}

	public void toggleAxialLimits() {
		axiallyConstrained = !axiallyConstrained;  
	}

	public boolean isEnabled() {
		return axiallyConstrained || orientationallyConstrained;
	}

	public void disable() {
		this.axiallyConstrained = false; 
		this.orientationallyConstrained = false;
	}

	public void enable() {
		this.axiallyConstrained = true; 
		this.orientationallyConstrained = true; 
	}


	double unitHyperArea = 2*Math.pow(Math.PI, 2);
	double unitArea = 4*Math.PI;

	/**
	 * TODO: // this functionality is not yet fully implemented It always returns an overly simplistic representation 
	 * not in line with what is described below.
	 *  
	 * @return an (approximate) measure of the amount of rotational 
	 * freedom afforded by this kusudama, with 0 meaning no rotational 
	 * freedom, and 1 meaning total unconstrained freedom. 
	 * 
	 * This is approximately computed as a ratio between the orientations the bone can be in 
	 * vs the orientations it cannot be in. Note that unfortunately this function double counts 
	 * the orientations a bone can be in between two limit cones in a sequence if those limit
	 * cones intersect with a previous sequence.   
	 */
	public double getRotationalFreedom() {

		//computation cached from updateRotationalFreedom 
		//feel free to override that method if you want your own more correct result. 
		//please contribute back a better solution if you write one.
		return rotationalFreedom;
	}

	double rotationalFreedom = 1d; 

	protected void updateRotationalFreedom() {
		double axialConstrainedHyperArea = isAxiallyConstrained() ? (range/G.TAU) : 1d;
		// quick and dirty solution (should revisit);
		double totalLimitConeSurfaceAreaRatio = 0d;
		for(AbstractLimitCone l : limitCones) {
			totalLimitConeSurfaceAreaRatio += (l.radius*2d)/ G.TAU; 
		}		
		rotationalFreedom = axialConstrainedHyperArea * ( isOrientationallyConstrained() ?  Math.min(totalLimitConeSurfaceAreaRatio, 1d) : 1d); 
		//System.out.println("rotational freedom: " + rotationalFreedom);
	}

	/**
	 * attaches the KusudamaExample to the BoneExample. If the 
	 * kusudama has its own limiting axes specified,
	 * replaces the bone's major rotation 
	 * axes with the Kusudamas limiting axes. 
	 * 
	 * otherwise, this function will set the kusudama's
	 * limiting axes to the major rotation axes specified by the bone.
	 * 
	 * @param forBone the bone to which to attach this KusudamaExample.
	 */
	public void attachTo(AbstractBone forBone) {
		this.attachedTo = forBone; 
		if(this.limitingAxes == null)
			this.limitingAxes = forBone.getMajorRotationAxes();
		else {
			forBone.setFrameofRotation(this.limitingAxes);
			this.limitingAxes = forBone.getMajorRotationAxes();
		}
	}
	
	/**for IK solvers. Defines the weight ratio between the unconstrained IK solved orientation and the constrained orientation for this bone 
	 per iteration. This should help stabilize solutions somewhat by allowing for soft constraint violations.
	 @param strength a value between 0 and 1. Any other value will be clamped to this range. 
	 **/ 
	public void setStrength(double newStrength) {
		this.strength = Math.max(0d, Math.min(1d, newStrength));
	}
	
	/**for IK solvers. Defines the weight ratio between the unconstrained IK solved orientation and the constrained orientation for this bone 
	 per iteration. This should help stabilize solutions somewhat by allowing for soft constraint violations.**/ 
	public double getStrength() {
		return this.strength;
	}


	public ArrayList<? extends AbstractLimitCone> getLimitCones() {
		return this.limitCones;
	}
	
	@Override
	public void makeSaveable(SaveManager saveManager) {
		saveManager.addToSaveState(this);
		for(AbstractLimitCone lc : limitCones) {
			lc.makeSaveable(saveManager);
		}
	}
	
	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		JSONObject saveJSON = new JSONObject(); 
		saveJSON.setString("identityHash", this.getIdentityHash());
		saveJSON.setString("limitAxes", limitingAxes().getIdentityHash()); 
		saveJSON.setString("attachedTo", attachedTo().getIdentityHash());
		saveJSON.setJSONArray("limitCones", saveManager.arrayListToJSONArray(limitCones));
		saveJSON.setDouble("minAxialAngle", minAxialAngle); 
		saveJSON.setDouble("axialRange", range);
		saveJSON.setBoolean("axiallyConstrained", this.axiallyConstrained);
		saveJSON.setBoolean("orientationallyConstrained", this.orientationallyConstrained);
		return saveJSON;
	}
	
	
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		this.attachedTo = l.getObjectFor(AbstractBone.class, j, "attachedTo");
		
		limitCones = new ArrayList<>();
		l.arrayListFromJSONArray(j.getJSONArray("limitCones"), limitCones, AbstractLimitCone.class);
		
		this.minAxialAngle = j.getDouble("minAxialAngle");
		this.range = j.getDouble("axialRange");
		this.axiallyConstrained = j.getBoolean("axiallyConstrained"); 
		this.orientationallyConstrained = j.getBoolean("orientationallyConstrained");
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
	public void notifyOfLoadCompletion() {
		this.constraintUpdateNotification();
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


