/**
 * 
 */
package IK;
import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;

import ga.Axes;
import sceneGraph.*;
/**
 * @author Eron
 *
 */
public abstract class AbstractKusudama implements Constraint{


	protected AbstractAxes limitingAxes; 

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
	protected double minAxialAngle = 0; 
	/**
	 * Defined as some Angle in radians about the limitingAxes Y axis, 0 being equivalent to the
	 * minAxialAngle
	 */
	protected double range = G.TAU;

	protected boolean orientationallyConstrained = false;
	protected boolean axiallyConstrained = false; 

	protected AbstractBone attachedTo;  

	public AbstractKusudama() {}
	
	public AbstractKusudama(AbstractBone forBone) {
		this.attachedTo = forBone; 
		this.limitingAxes = forBone.getMajorRotationAxes();
	}

	public abstract void generateAxes(Ray x, Ray y, Ray z);


	public void updateTangentRadii() {

		for (int i=0; i<limitCones.size()-1; i++) {      
			limitCones.get(i).updateTangentHandles(limitCones.get(i+1));
		}
	}

	/**
	 * Snaps the bone this Kusudama is constraining to the Kusudama's orientational and axial limits. 
	 */
	public void snapToLimits() {
		//System.out.println("snapping to limits");
		if(orientationallyConstrained) {
			boolean[] inBounds = {true}; 
			//System.out.println("orientationalSnap ");
			Ray boneRay = new Ray(attachedTo.getBase(), attachedTo.getTip());    
			DVector inLimits = this.pointInLimits(attachedTo.getTip(), inBounds);    

			if (inBounds[0] == false && inLimits != null) {      
				Ray constrainedRay = new Ray(attachedTo.getBase(), inLimits); 
				Rot rotation = new Rot(boneRay.heading(), constrainedRay.heading());      
				attachedTo.localAxes.rotateTo(rotation);      
			}
		}
		if(axiallyConstrained) {
			snapToTwistLimits();
		}
	}

	private DVector minVector = new DVector(1,0,0);
	private DVector maxVector = new DVector(1,0,0);
	
	/**
	 * Kusudama constraints decompose the bone orientation into a swing component, and a twist component. 
	 * The "Swing" component is the final direction of the bone. The "Twist" component represents how much 
	 * the bone is rotated about its own final direction. Where limit cones allow you to constrain the "Swing" 
	 * component, this method lets you constrain the "twist" component. 
	 * 
	 * @param minAnlge some angle in radians about the major rotation frame's y-axis to serve as the first angle within the range that the bone is allowed to twist. 
	 * @param maxAngle some angle in radians added to the minAngle. if the bone's local Z goes maxAngle radians beyond the minAngle, it is considered past the limit. 
	 * This value is always interpreted as being in the positive direction. For example, if this value is -PI/2, the entire range from minAngle to minAngle + 3PI/4 is
	 * considered valid.  
	 */
	
	public void setAxialLimits(double minAngle, double inRange) {
		minAxialAngle = minAngle;
		range = inRange;
	}
	
	public void snapToTwistLimits() {
		
		AbstractAxes limitLocalAxes = limitingAxes.getLocalOf(attachedTo.localAxes);
		Rot localAxestoLimitingAxes = new Rot(limitLocalAxes.lx.p2, limitLocalAxes.lz.p2, new DVector(1,0,0), new DVector(0,0,1)); 
		Rot[] decomposition = localAxestoLimitingAxes.getSwingTwist(new DVector(0,1,0));
		
		
		double angleDelta = decomposition[1].getAngle() * decomposition[1].getAxis().y *-1;
					
		angleDelta = toTau(angleDelta);
		
		double fromMinToAngleDelta = toTau(signedAngleDifference(angleDelta, minAxialAngle()));
				
		if(fromMinToAngleDelta > range ) {
			if(fromMinToAngleDelta - range < G.TAU - fromMinToAngleDelta) {
				attachedTo().localAxes().rotateAboutY(-1*(fromMinToAngleDelta - range)); 
			} else {
				attachedTo().localAxes().rotateAboutY(G.TAU-fromMinToAngleDelta);
			}
		} else {
		}
		
		
	}
	
	public double signedAngleDifference(double minAngle, double base) {
		double d = Math.abs(minAngle - base) % G.TAU; 
		double r = d > G.PI ? G.TAU - d : d;

		double sign = (minAngle - base >= 0 && minAngle - base <=G.PI) || (minAngle - base <=-G.PI && minAngle - base>= -G.TAU) ? 1f : -1f; 
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
	 * @param inBounds just used as a pass by reference. Will populate the the [0] element with false if the point was not in bounds, 
	 * and true if it was in bounds.
	 * @return the original point, if it's in limits, or a the closest point which is in limits.
	 */
	public DVector pointInLimits(DVector inPoint, boolean[] inBounds) {
		
		DVector point = limitingAxes.getLocalOf(inPoint);
		point.normalize(); 
		point.mult(attachedTo.boneHeight);
		
		inBounds[0] = false;
		
		DVector closestCollisionPoint = null;
		if (limitCones.size() > 1 && this.orientationallyConstrained) {
			for (int i =0; i<limitCones.size() -1; i++) {
				DVector collisionPoint = new DVector(0,0,0);
				AbstractLimitCone nextCone = (limitCones.size() > 1)? limitCones.get(i+1) : null;
				boolean inSegBounds = limitCones.get(i).inBoundsFromThisToNext(nextCone, point, collisionPoint);

				if(inSegBounds == false && (closestCollisionPoint == null 
				  || DVector.angleBetween(collisionPoint, point) < DVector.angleBetween(point,closestCollisionPoint))) {
					closestCollisionPoint = collisionPoint.copy();     
				} else if( inSegBounds == true) {
					inBounds[0] = true;  
				}
			}   

			if (inBounds[0] == false) { 
				return limitingAxes.getGlobalOf(closestCollisionPoint);
			} else { 
				return inPoint;
			}
		} else {
			if(DVector.angleBetween(point, limitCones.get(0).controlPoint) < limitCones.get(0).radius) {
				return inPoint;
			} else {
				Rot toLimit = new Rot(limitCones.get(0).controlPoint, point);
				toLimit = new Rot(toLimit.getAxis(), limitCones.get(0).radius);
				return limitingAxes.getGlobalOf(toLimit.applyTo(limitCones.get(0).controlPoint));
			}
		}
	}
	
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
	public void addLimitCone(DVector newPoint, double radius, AbstractLimitCone previous, AbstractLimitCone next) {
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
	public void addLimitConeAtIndex(int insertAt, DVector newPoint, double radius) {
		this.updateTangentRadii();
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
	 * @return the limitingAxes of this Kusudama (actually, these are just its parentBone's majorRotationAxes)
	 */
	public AbstractAxes limitingAxes() {
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
		return signedAngleDifference(range+minAxialAngle, 0);
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
	
	
	/**
	 * attaches the Kusudama to the Bone. If the 
	 * kusudama has its own limiting axes specified,
	 * replaces the bone's major rotation 
	 * axes with the Kusudamas limiting axes. 
	 * 
	 * otherwise, this function will set the kusudama's
	 * limiting axes to the major rotation axes specified by the bone.
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
}


