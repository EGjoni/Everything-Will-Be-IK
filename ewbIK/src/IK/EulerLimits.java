package IK;


import IK.doubleIK.AbstractBone;
import IK.doubleIK.Constraint;
import IK.doubleIK.G;
import sceneGraph.IKVector;
import sceneGraph.math.Vec;
import sceneGraph.math.doubleV.AbstractAxes;
import sceneGraph.math.doubleV.MRotation;
import sceneGraph.math.doubleV.RotationOrder;
import sceneGraph.math.doubleV.SGVec_3d;

public class EulerLimits implements Constraint {
	
	AbstractAxes limitingAxes;
	AbstractBone attachedTo;
	
	double minX = -Math.PI*2;
	double maxX = Math.PI*2;
	double minY = - Math.PI*2; 
	double maxY = Math.PI*2;
	double minZ = Math.PI*2; 
	double maxZ = Math.PI*2;
	
	/**
	 * This is the worst possible constraint type -- rife with instabilities. You really shouldn't use it. 
	 * It is here more as a template than anything else. 
	 * <br> <br>
	 * All this does is clamp the bone's XZYAngles to be within the limits defined. 	 
	 * It does this using Eueler angles, and so is prone to hitting singularities.
	 * Additionally, this class does not bother noting that -1.5*PI is equivalent to 
	 * +.5*PI. So you can get some behavior which is not only weird, but also exacerbates instabilities. Your second best bet for avoiding 
	 * this is choosing a suitable orientation for the bone's majorRotationAxes given the limits you are trying to specify. 
	 * Your BEST bet for avoiding this is to use KusudamaExample constraints instead.
	 * <br><br>
	 * Feel free to fix any of this and contribute the changes back if you really need to use Euler angles though. 
	 * 
	 * @param parentBone the bone to which this constraint should be applied
	 */
	public EulerLimits(AbstractBone parentBone) {
		this.attachedTo = parentBone;
		this.limitingAxes = parentBone.getMajorRotationAxes();
	}

	@Override
	public void snapToLimits() {
		double[] angles = attachedTo.getXZYAngle();
		double x, y, z;
		
		x = Math.max(angles[0], minX);
		x = Math.min(x, maxX);
		
		z = Math.max(angles[1], minZ);
		z = Math.min(z, maxZ);
		
		y = Math.max(angles[2], minY);
		y = Math.min(y, maxY);		
		
		attachedTo.localAxes().alignLocalsTo(limitingAxes);
		attachedTo.localAxes().rotateBy(new MRotation(RotationOrder.XZY, x, z, y));
	}
	
	
	/**
	 * @param min any rotation on the x axis smaller than this value is clamped up to this value
	 * @param max any rotation on the x axis larger than this value is clamped up to this value
	 */
	public void setXLimits(double min, double max) {
		this.minX = Math.min(min, max); 
		this.maxX = Math.max(min, max);
	}
	
	/**
	 * @param min any rotation on the y axis smaller than this value is clamped up to this value
	 * @param max any rotation on the y axis larger than this value is clamped up to this value
	 */	
	public void setYLimits(double min, double max) {
		this.minY = Math.min(min, max);  
		this.maxY = Math.max(min, max);
	}
	

	/**
	 * @param min any rotation on the z axis smaller than this value is clamped up to this value
	 * @param max any rotation on the z axis larger than this value is clamped up to this value
	 */
	
	public void setZLimits(double min, double max) {
		this.minZ = Math.min(min, max); 
		this.maxZ = Math.max(min, max);
	}	
	
	/**
	 * 
	 * @param minX any rotation on the x axis smaller than this value is clamped up to this value
	 * @param maxX any rotation on the x axis larger than this value is clamped up to this value
	 * @param minY any rotation on the y axis smaller than this value is clamped up to this value
	 * @param maxY any rotation on the y axis larger than this value is clamped up to this value
	 * @param minZ any rotation on the z axis smaller than this value is clamped up to this value
	 * @param maxZ any rotation on the z axis larger than this value is clamped up to this value
	 */
	public void setXZYLimits(double minX, double maxX, double minZ, double maxZ, double minY, double maxY) {
		setXLimits(minX, maxX);
		setZLimits(minZ, maxZ);
		setYLimits(minY, maxY);		
	}
	
	public double getMinX() { return this.minX; }; 
	public double getMinY() { return this.minY; }; 
	public double getMinZ() { return this.minZ; }; 
	
	public double getMaxX() { return this.maxX; }; 
	public double getMaxY() { return this.maxY; }; 
	public double getMaxZ() { return this.maxZ; }

	@Override
	public AbstractAxes limitingAxes() {
		return this.limitingAxes;
	}

	@Override
	public void disable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getRotationalFreedom() {
		return (Math.abs(minX - maxX) / G.TAU) * (Math.abs(minY - maxY) / G.TAU) * (Math.abs(minZ - maxZ) / G.TAU);
	}


	@Override
	public boolean isInLimits_(SGVec_3d globalPoint) {
		// TODO Auto-generated method stub
		return false;
	}

	

}
