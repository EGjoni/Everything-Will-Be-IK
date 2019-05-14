package IK.floatIK;

import sceneGraph.math.Vec;
import sceneGraph.math.floatV.AbstractAxes;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.Vec3f;

public interface Constraint {
	
	public void snapToLimits();
	public void disable(); 
	public void enable(); 
	public boolean isEnabled();
	
	//returns true if the ray from the constraint origin to the globalPoint is within the constraint's limits
	//false otherwise.
	public boolean isInLimits_(SGVec_3f globalPoint);
	public AbstractAxes limitingAxes();
	
	/**
	 * @return a measure of the rotational freedom afforded by this constraint. 
	 * with 0 meaning no rotational freedom (the bone is essentially stationary in relation to its parent)
	 * and 1 meaning full rotational freedom (the bone is completely unconstrained). 
	 * 
	 * This should be computed as ratio between orientations a bone can be in and orientations 
	 * a bone cannot be in as defined by its representation as a point on the surface of a hypersphere. 
	 */
	public float getRotationalFreedom();

}
