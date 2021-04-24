/**
 * 
 */
package IK.floatIK;
import java.util.ArrayList;

import IK.floatIK.SegmentedArmature.WorkingBone;
import data.EWBIKLoader;
import data.EWBIKSaver;
import math.floatV.AbstractAxes;
import math.floatV.MRotation;
import math.floatV.MathUtils;
import math.floatV.Rot;
import math.floatV.SGVec_3f;
import math.floatV.Vec3f;
import math.floatV.sgRayf;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;
/**
 * @author Eron
 *
 */
public abstract class AbstractKusudama implements Constraint, Saveable {

	public static final float TAU = MathUtils.PI*2;
	public static final float PI = MathUtils.PI;
	protected AbstractAxes limitingAxes; 
	protected float painfullness; 

	/**
	 * An array containing all of the Kusudama's limitCones. The kusudama is built up
	 * with the expectation that any limitCone in the array is connected to the cone at the previous element in the array, 
	 * and the cone at the next element in the array.  
	 */
	protected ArrayList<AbstractLimitCone> limitCones = new ArrayList<AbstractLimitCone>();

	/**
	 * Defined as some Angle in radians about the limitingAxes Y axis, 0 being equivalent to the
	 * limitingAxes Z axis. 
	 */
	protected float minAxialAngle = MathUtils.PI; 
	/**
	 * Defined as some Angle in radians about the limitingAxes Y axis, 0 being equivalent to the
	 * minAxialAngle
	 */
	protected float range = MathUtils.PI*3;

	protected boolean orientationallyConstrained = false;
	protected boolean axiallyConstrained = false;

	//for IK solvers. Defines the weight ratio between the unconstrained IK solved orientation and the constrained orientation for this bone 
	//per iteration. This should help stabilize solutions somewhat by allowing for soft constraint violations. 
	protected Float strength = 1f; 

	protected AbstractBone attachedTo;  


	public AbstractKusudama() {
	}

	public AbstractKusudama(AbstractBone forBone) {
		this.attachedTo = forBone; 
		this.limitingAxes = forBone.getMajorRotationAxes();
		this.attachedTo.addConstraint(this);
		this.enable();
	}


	public void constraintUpdateNotification() {
		this.updateTangentRadii();
		this.updateRotationalFreedom();
	}

	/**
	 * This function should be called after you've set all of the Limiting Cones 
	 * for this Kusudama. It will orient the axes relative to which constrained rotations are computed 
	 * so as to minimize the potential for undesirable twist rotations due to antipodal singularities. 
	 * 
	 * In general, auto-optimization attempts to point the y-component of the constraint
	 * axes in the direction that places it within an oreintation allowed by the constraint, 
	 * and roughly as far as possible from any orientations not allowed by the constraint.  
	 */
	public void optimizeLimitingAxes() {
		AbstractAxes originalLimitingAxes = limitingAxes.getGlobalCopy();
	
			ArrayList<Vec3f<?>> directions = new ArrayList<>(); 
			if(getLimitCones().size() == 1) {
				directions.add((limitCones.get(0).getControlPoint()).copy());
			} else {
				for(int i = 0; i<getLimitCones().size()-1; i++) {
					Vec3f<?> thisC = getLimitCones().get(i).getControlPoint().copy();
					Vec3f<?> nextC = getLimitCones().get(i+1).getControlPoint().copy();
					Rot thisToNext = new Rot(thisC, nextC); 
					Rot halfThisToNext = new Rot(thisToNext.getAxis(), thisToNext.getAngle()/2f); 

					Vec3f<?> halfAngle = halfThisToNext.applyToCopy(thisC);
					halfAngle.normalize(); halfAngle.mult(thisToNext.getAngle());
					directions.add(halfAngle);
				}	
			}

			Vec3f<?> newY = new SGVec_3f();
			for(Vec3f<?> dv: directions) {
				newY.add(dv); 
			}

			newY.div(directions.size()); 
			if(newY.mag() != 0 && !Float.isNaN(newY.y)) {
				newY.normalize();
			} else {
				newY = new SGVec_3f(0,1f,0);
			}

			sgRayf newYRay = new sgRayf(new SGVec_3f(0,0,0), newY);

			Rot oldYtoNewY = new Rot(limitingAxes.y_().heading(), originalLimitingAxes.getGlobalOf(newYRay).heading());
			limitingAxes.rotateBy(oldYtoNewY);

			for(AbstractLimitCone lc : getLimitCones()) {
				originalLimitingAxes.setToGlobalOf(lc.controlPoint, lc.controlPoint);
				limitingAxes.setToLocalOf(lc.controlPoint, lc.controlPoint);
				lc.controlPoint.normalize(); 
			}

			this.updateTangentRadii();			
	}


	public void updateTangentRadii() {

		for (int i=0; i<limitCones.size(); i++) {      
			AbstractLimitCone next = i<limitCones.size() -1 ? limitCones.get(i+1) : null;
			limitCones.get(i).updateTangentHandles(next);
		}
	}


	
	sgRayf boneRay = new sgRayf(new SGVec_3f(), new SGVec_3f());
	sgRayf constrainedRay = new sgRayf(new SGVec_3f(), new SGVec_3f());
	
	/**
	 * Snaps the bone this Kusudama is constraining to be within the Kusudama's orientational and axial limits. 
	 */
	public void snapToLimits() {
		//System.out.println("snapping to limits");
		if(orientationallyConstrained) {
			setAxesToOrientationSnap(attachedTo().localAxes(), limitingAxes, 0);
		}
		if(axiallyConstrained) {
			snapToTwistLimits(attachedTo().localAxes(), limitingAxes);
		}
	}

	/**
	 * Presumes the input axes are the bone's localAxes, and rotates 
	 * them to satisfy the snap limits. 
	 * 
	 * @param toSet
	 */
	public void setAxesToSnapped(AbstractAxes toSet, AbstractAxes limitingAxes, float cosHalfAngleDampen) {
		if(limitingAxes != null) {					
			if(orientationallyConstrained) {
				setAxesToOrientationSnap(toSet, limitingAxes, cosHalfAngleDampen);
			} 		
			if(axiallyConstrained) {
				snapToTwistLimits(toSet, limitingAxes);
			}		
		}
	}
	
	
	public void setAxesToReturnfulled(AbstractAxes toSet, AbstractAxes limitingAxes, float cosHalfReturnfullness, float angleReturnfullness) {
		if(limitingAxes != null && painfullness >0f) {
			if(orientationallyConstrained) {				
				Vec3f<?> origin = toSet.origin_();
				Vec3f<?> inPoint = toSet.y_().p2().copy();
				Vec3f<?> pathPoint = pointOnPathSequence(inPoint, limitingAxes);
				inPoint.sub(origin);
				pathPoint.sub(origin);				
				Rot toClamp = new Rot(inPoint, pathPoint);				
				toClamp.rotation.clampToQuadranceAngle(cosHalfReturnfullness);
				toSet.rotateBy(toClamp);
			}
			if(axiallyConstrained) {
				float angleToTwistMid = angleToTwistCenter(toSet, limitingAxes);
				float clampedAngle = MathUtils.clamp(angleToTwistMid, -angleReturnfullness, angleReturnfullness);
				toSet.rotateAboutY(clampedAngle, false);
			}
		}
	}
	
	/**
	 * A value between (ideally between 0 and 1) dictating 
	 * how much the bone to which this kusudama belongs 
	 * prefers to be away from the edges of the kusudama  
	 * if it can. This is useful for avoiding unnatural poses,
	 * as the kusudama will push bones back into their more
	 * "comfortable" regions. Leave this value at its default of 
	 * 0 unless you empircal observations show you need it. 
	 * Setting this value to anything higher than 0.4 is probably overkill
	 * in most situations.
	 *  
	 * @param amt
	 */
	public void setPainfullness(float amt) {
		painfullness = amt;
		if(attachedTo() != null && attachedTo().parentArmature != null) {
			SegmentedArmature s =  attachedTo().parentArmature.boneSegmentMap.get(this.attachedTo());
			if(s != null ) {
				WorkingBone wb = s.simulatedBones.get(this.attachedTo());
				if(wb != null) {
					wb.updateCosDampening();
				}
			}
		}
	}
	
	
	/**
	 * @return A value between (ideally between 0 and 1) dictating 
	 * how much the bone to which this kusudama belongs 
	 * prefers to be away from the edges of the kusudama  
	 * if it can. 
	 */
	public float getPainfullness() {
		return painfullness;
	}

	@Override
	public <V extends Vec3f<?>> boolean isInLimits_(V globalPoint) {
		float[] inBounds = {1f}; 
		//boneRay.p1.set(toSet.origin()); boneRay.p2.set(toSet.y().getScaledTo(attachedTo.boneHeight));    
		Vec3f<?> inLimits = this.pointInLimits(limitingAxes.getLocalOf(globalPoint), inBounds);    
		return inBounds[0] > 0f;
	}


	/**
	 * Presumes the input axes are the bone's localAxes, and rotates 
	 * them to satisfy the snap limits. 
	 * 
	 * @param toSet
	 */
	public void setAxesToOrientationSnap(AbstractAxes toSet, AbstractAxes limitingAxes, float cosHalfAngleDampen) {
		float[] inBounds = {1f}; 
		limitingAxes.updateGlobal();
		boneRay.p1().set(limitingAxes.origin_()); boneRay.p2().set(toSet.y_().p2());
		Vec3f<?> bonetip = limitingAxes.getLocalOf(toSet.y_().p2());
		Vec3f<?> inLimits = this.pointInLimits(bonetip, inBounds);

		if (inBounds[0] == -1 && inLimits != null) {     
			constrainedRay.p1().set(boneRay.p1()); constrainedRay.p2().set(limitingAxes.getGlobalOf(inLimits)); 
			Rot rectifiedRot = new Rot(boneRay.heading(), constrainedRay.heading());
			toSet.rotateBy(rectifiedRot);
			toSet.updateGlobal();			
		}		
	}

	public boolean isInOrientationLimits(AbstractAxes globalAxes, AbstractAxes limitingAxes) {
		float[] inBounds = {1f}; 		
		//boneRay.p1().set(globalAxes.origin_()); boneRay.p2().set(globalAxes.y_().getScaledTo(attachedTo.boneHeight));    
		Vec3f<?> inLimits = this.pointInLimits(limitingAxes.getLocalOf(globalAxes.y_().p2()), inBounds);
		if(inBounds[0] == -1l) {
			return false;
		} else {
			return true;
		}
	}


	/**
	 * Kusudama constraints decompose the bone orientation into a swing component, and a twist component. 
	 * The "Swing" component is the final direction of the bone. The "Twist" component represents how much 
	 * the bone is rotated about its own final direction. Where limit cones allow you to constrain the "Swing" 
	 * component, this method lets you constrain the "twist" component. 
	 * 
	 * @param minAnlge some angle in radians about the major rotation frame's y-axis to serve as the first angle within the range that the bone is allowed to twist. 
	 * @param inRange some angle in radians added to the minAngle. if the bone's local Z goes maxAngle radians beyond the minAngle, it is considered past the limit. 
	 * This value is always interpreted as being in the positive direction. For example, if this value is -PI/2, the entire range from minAngle to minAngle + 3PI/4 is
	 * considered valid.  
	 */
	public void setAxialLimits(float minAngle, float inRange) {
		minAxialAngle = minAngle;
		range = toTau(inRange);
		constraintUpdateNotification();
	}

	//protected CartesianAxes limitLocalAxes;


	
	/**
	 * 
	 * @param toSet
	 * @param limitingAxes
	 * @return radians of twist required to snap bone into twist limits (0 if bone is already in twist limits)
	 */
	public float snapToTwistLimits(AbstractAxes toSet, AbstractAxes limitingAxes) {

		if(!axiallyConstrained) return 0f;

		Rot alignRot = limitingAxes.getGlobalMBasis().getInverseRotation().applyTo(toSet.getGlobalMBasis().rotation);
		Rot[] decomposition = alignRot.getSwingTwist(new SGVec_3f(0,1,0));
		float angleDelta2 = decomposition[1].getAngle() * decomposition[1].getAxis().y*-1f; 
		angleDelta2 = toTau(angleDelta2);
		float fromMinToAngleDelta = toTau(signedAngleDifference(angleDelta2, TAU - this.minAxialAngle())); 

		if(fromMinToAngleDelta <  TAU - range ) {                          
			float distToMin = MathUtils.abs(signedAngleDifference(angleDelta2, TAU-this.minAxialAngle()));
			float distToMax =  MathUtils.abs(signedAngleDifference(angleDelta2, TAU-(this.minAxialAngle()+range)));
			float turnDiff = 1f; 
			//uncomment the next line for reflectable axis  support (removed for performance reasons) 
			turnDiff *= limitingAxes.getGlobalChirality(); 
			if(distToMin < distToMax){
				turnDiff = turnDiff*(fromMinToAngleDelta);
				toSet.rotateAboutY(turnDiff, true);                                                                                                         
			} else {                              
				turnDiff = turnDiff*(range - (TAU-fromMinToAngleDelta));
				toSet.rotateAboutY(turnDiff, true);
			}
			return turnDiff < 0 ? turnDiff*-1 : turnDiff;
		} else {
			return 0;
		}
		//return 0;
	}
	
	
	public float angleToTwistCenter(AbstractAxes toSet, AbstractAxes limitingAxes) {

		if(!axiallyConstrained) return 0f;

		Rot alignRot = limitingAxes.getGlobalMBasis().getInverseRotation().applyTo(toSet.getGlobalMBasis().rotation);
		Rot[] decomposition = alignRot.getSwingTwist(new SGVec_3f(0,1,0));
		float angleDelta2 = decomposition[1].getAngle() * decomposition[1].getAxis().y*-1f; 
		angleDelta2 = toTau(angleDelta2);
		              
		float distToMid =  signedAngleDifference(angleDelta2, TAU-(this.minAxialAngle()+(range/2f)));
		return distToMid;	
		
	}

	public boolean inTwistLimits(AbstractAxes boneAxes, AbstractAxes limitingAxes) {

		limitingAxes.updateGlobal();
		Rot alignRot = limitingAxes.getGlobalMBasis().getInverseRotation().applyTo(boneAxes.globalMBasis.rotation);
		Rot[] decomposition = alignRot.getSwingTwist(new SGVec_3f(0,1,0));

		float angleDelta = decomposition[1].getAngle() * decomposition[1].getAxis().y*-1;
		//uncomment the next line for reflectable axis  support (removed for performance reasons) 
		angleDelta*= limitingAxes.getGlobalChirality()*(limitingAxes.isGlobalAxisFlipped(AbstractAxes.Y) ? -1 : 1);;

		angleDelta = toTau(angleDelta);
		float fromMinToAngleDelta = toTau(signedAngleDifference(angleDelta, TAU - this.minAxialAngle())); 

		if(fromMinToAngleDelta <  TAU - range ) {                          
			float distToMin = MathUtils.abs(signedAngleDifference(angleDelta, TAU-this.minAxialAngle()));
			float distToMax =  MathUtils.abs(signedAngleDifference(angleDelta, TAU-(this.minAxialAngle()+range)));
			if(distToMin < distToMax){ 
				return false;                                                                                                        
			} else {                                                                                                                                                                                                           
				return false;   
			}
		}
		return true;
	}

	public float signedAngleDifference(float minAngle, float base) {
		float d = MathUtils.abs(minAngle - base) % TAU; 
		float r = d > PI ? TAU - d : d;

		float sign = (minAngle - base >= 0 && minAngle - base <=PI) || (minAngle - base <=-PI && minAngle - base>= -TAU) ? 1f : -1f; 
		r *= sign;
		return r;
	}

	/**
	 * Given a point (in global coordinates), checks to see if a ray can be extended from the Kusudama's
	 * origin to that point, such that the ray in the Kusudama's reference frame is within the range allowed by the Kusudama's
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
	public <V extends Vec3f<?>> Vec3f<?> pointInLimits(V inPoint, float[] inBounds) {

		Vec3f<?> point = inPoint.copy(); 
		point.normalize(); 		
		//point.mult(attachedTo.boneHeight);

		inBounds[0] = -1;

		Vec3f<?> closestCollisionPoint = null; 
		float closestCos = -2f;
		if (limitCones.size() > 1 && this.orientationallyConstrained) {
			for (int i =0; i<limitCones.size() -1; i++) {
				Vec3f<?> collisionPoint = inPoint.copy(); collisionPoint.set(0,0,0);
				AbstractLimitCone nextCone = limitCones.get(i+1);				
				boolean inSegBounds = limitCones.get(i).inBoundsFromThisToNext(nextCone, point, collisionPoint);				
				if( inSegBounds == true) {
					inBounds[0] = 1;  
				} else {
					float thisCos =  collisionPoint.dot(point); 
					if(closestCollisionPoint == null || thisCos > closestCos) {
						closestCollisionPoint = collisionPoint.copy();
						closestCos = thisCos;
					}
				} 
			}   
			if (inBounds[0] == -1) { 
				return closestCollisionPoint;
			} else { 
				return point;
			}
		} else if(orientationallyConstrained) {
			float pointdot = point.dot(limitCones.get(0).getControlPoint()); 
			float radcos = limitCones.get(0).getRadiusCosine();
			if(pointdot > radcos) {
				inBounds[0] = 1;
				return inPoint;
			} else {
				Vec3f<?> axis = limitCones.get(0).getControlPoint().crossCopy(point);
				//Rot toLimit = new Rot(limitCones.get(0).getControlPoint(), point);
				Rot toLimit = new Rot(axis, limitCones.get(0).getRadius());
				Vec3f<?> newPoint = toLimit.applyToCopy(limitCones.get(0).getControlPoint());
				return newPoint;
			}
		} else {
			inBounds[0] = 1;
			return inPoint;
		}
	}
	
	
	public <V extends Vec3f<?>> Vec3f<?> pointOnPathSequence(V inPoint, AbstractAxes limitingAxes) {
		float closestPointDot = 0f; 
		Vec3f point = limitingAxes.getLocalOf(inPoint);
		point.normalize();
		Vec3f result = (Vec3f) point.copy();
				
		if(limitCones.size() == 1) {
			result.set(limitCones.get(0).controlPoint);
		} else {
			for (int i =0; i<limitCones.size() -1; i++) {
				AbstractLimitCone nextCone = limitCones.get(i+1);
				Vec3f<?> closestPathPoint = limitCones.get(i).getClosestPathPoint(nextCone, point);
				float closeDot = closestPathPoint.dot(point);
				if(closeDot > closestPointDot) {
					result.set(closestPathPoint);
					closestPointDot = closeDot;
				}
			}
		}		
		
		return limitingAxes.getGlobalOf(result); 
	}

	//public float softLimit

	public AbstractBone attachedTo() {
		return this.attachedTo;
	}

	/**
	 * Add a LimitCone to the Kusudama. 
	 * @param newPoint where on the Kusudama to add the LimitCone (in Kusudama's local coordinate frame defined by its bone's majorRotationAxes))
	 * @param radius the radius of the limitCone
	 * @param previous the LimitCone adjacent to this one (may be null if LimitCone is not supposed to be between two existing LimitCones)
	 * @param next the other LimitCone adjacent to this one (may be null if LimitCone is not supposed to be between two existing LimitCones)
	 */
	public void addLimitCone(SGVec_3f newPoint, float radius, AbstractLimitCone previous, AbstractLimitCone next) {
		int insertAt = 0;

		if(next == null || limitCones.size() == 0) {
			addLimitConeAtIndex(-1, newPoint, radius);
		} else if(previous != null) {
			insertAt = limitCones.indexOf(previous)+1;
		} else {
			insertAt = (int)MathUtils.max(0, limitCones.indexOf(next));
		}
		addLimitConeAtIndex(insertAt, newPoint, radius);		
	}

	public void removeLimitCone(AbstractLimitCone limitCone) {
		this.limitCones.remove(limitCone);
		this.updateTangentRadii();
		this.updateRotationalFreedom();
	}

	public abstract AbstractLimitCone createLimitConeForIndex(int insertAt, Vec3f<?> newPoint, float radius);

	/**
	 * Adds a LimitCone to the Kusudama. LimitCones are reach cones which can be arranged sequentially. The Kusudama will infer
	 * a smooth path leading from one LimitCone to the next. 
	 * 
	 * Using a single LimitCone is functionally equivalent to a classic reachCone constraint. 
	 * 
	 * @param insertAt the intended index for this LimitCone in the sequence of LimitCones from which the Kusudama will infer a path. @see IK.AbstractKusudama.limitCones limitCones array. 
	 * @param newPoint where on the Kusudama to add the LimitCone (in Kusudama's local coordinate frame defined by its bone's majorRotationAxes))
	 * @param radius the radius of the limitCone
	 */
	public void addLimitConeAtIndex(int insertAt, SGVec_3f newPoint, float radius) {
		AbstractLimitCone newCone = createLimitConeForIndex(insertAt, newPoint, radius);
		if(insertAt == -1) {
			limitCones.add(newCone);
		} else {
			limitCones.add(insertAt, newCone);
		}		
		this.updateTangentRadii();
		this.updateRotationalFreedom();
		
	}


	public float toTau(float angle) {
		float result = angle; 
		if(angle<0) {
			result = (2*MathUtils.PI) + angle;
		}
		result = result%(MathUtils.PI*2);
		return result;
	}

	public float mod(float x, float y) {
		if(y!=0 && x!= 0) {
			float result = x % y;
			if (result < 0) 
				result += y;
			return result;
		} else return 0;
	}


	/**
	 * @return the limitingAxes of this Kusudama (these are just its parentBone's majorRotationAxes)
	 */
	@Override
	public <A extends AbstractAxes> A limitingAxes() {
		//if(inverted) return inverseLimitingAxes; 
		return (A) limitingAxes;
	}

	/**
	 * 
	 * @return the lower bound on the axial constraint
	 */
	public float minAxialAngle(){
		return minAxialAngle;
	} 

	public float maxAxialAngle(){
		return range;
	}

	/**
	 * the upper bound on the axial constraint in absolute terms
	 * @return
	 */
	public float absoluteMaxAxialAngle(){
		//return mod((minAxialAngle + range),(MathUtils.PI*2f));
		return signedAngleDifference(range+minAxialAngle, MathUtils.PI*2f);
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


	float unitHyperArea = 2*MathUtils.pow(MathUtils.PI, 2);
	float unitArea = 4*MathUtils.PI;

	/**
	 * TODO: // this functionality is not yet fully implemented It always returns an overly simplistic representation 
	 * not in line with what is described below.
	 *  
	 * @return an (approximate) measure of the amount of rotational 
	 * freedom afforded by this kusudama, with 0 meaning no rotational 
	 * freedom, and 1 meaning total unconstrained freedom. 
	 * 
	 * This is approximately computed as a ratio between the orientations the bone can be in 
	 * vs the orientations it cannot be in. Note that unfortunately this function float counts 
	 * the orientations a bone can be in between two limit cones in a sequence if those limit
	 * cones intersect with a previous sequence.   
	 */
	public float getRotationalFreedom() {

		//computation cached from updateRotationalFreedom 
		//feel free to override that method if you want your own more correct result. 
		//please contribute back a better solution if you write one.
		return rotationalFreedom;
	}

	float rotationalFreedom = 1f; 

	protected void updateRotationalFreedom() {
		float axialConstrainedHyperArea = isAxiallyConstrained() ? (range/TAU) : 1f;
		// quick and dirty solution (should revisit);
		float totalLimitConeSurfaceAreaRatio = 0f;
		for(AbstractLimitCone l : limitCones) {
			totalLimitConeSurfaceAreaRatio += (l.getRadius()*2f)/ TAU; 
		}		
		rotationalFreedom = axialConstrainedHyperArea * ( isOrientationallyConstrained() ?  MathUtils.min(totalLimitConeSurfaceAreaRatio, 1f) : 1f); 
		//System.out.println("rotational freedom: " + rotationalFreedom);
	}

	/**
	 * attaches the Kusudama to the BoneExample. If the 
	 * kusudama has its own limiting axes specified,
	 * replaces the bone's major rotation 
	 * axes with the Kusudamas limiting axes. 
	 * 
	 * otherwise, this function will set the kusudama's
	 * limiting axes to the major rotation axes specified by the bone.
	 * 
	 * @param forBone the bone to which to attach this Kusudama.
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
	public void setStrength(float newStrength) {
		this.strength = MathUtils.max(0f, MathUtils.min(1f, newStrength));
	}

	/**for IK solvers. Defines the weight ratio between the unconstrained IK solved orientation and the constrained orientation for this bone 
	 per iteration. This should help stabilize solutions somewhat by allowing for soft constraint violations.**/ 
	public float getStrength() {
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
		saveJSON.setFloat("minAxialAngle", minAxialAngle); 
		saveJSON.setFloat("axialRange", range);
		saveJSON.setBoolean("axiallyConstrained", this.axiallyConstrained);
		saveJSON.setBoolean("orientationallyConstrained", this.orientationallyConstrained);
		saveJSON.setFloat("painfulness", this.painfullness);
		return saveJSON;
	}


	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		this.attachedTo = l.getObjectFor(AbstractBone.class, j, "attachedTo");
		this.limitingAxes = l.getObjectFor(AbstractAxes.class, j, "limitAxes");
		limitCones = new ArrayList<>();
		l.arrayListFromJSONArray(j.getJSONArray("limitCones"), limitCones, AbstractLimitCone.class);
		this.minAxialAngle = j.getFloat("minAxialAngle");
		this.range = j.getFloat("axialRange");
		this.axiallyConstrained = j.getBoolean("axiallyConstrained"); 
		this.orientationallyConstrained = j.getBoolean("orientationallyConstrained");
		this.painfullness = j.getFloat("painfulness");
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
		this.optimizeLimitingAxes();
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


