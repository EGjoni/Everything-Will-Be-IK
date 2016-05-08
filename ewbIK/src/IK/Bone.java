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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import IK.AbstractBone.NullParentForBoneException;
import sceneGraph.*;

public class Bone extends AbstractBone {
	
	public Bone (Bone par, //parent bone
			DVector tipHeading, //the orienational heading of this bone (global vs relative coords specified in coordinateType)
			DVector rollHeading, //axial rotation heading of the bone (it's z-axis) 
			String inputTag,	 //some user specified name for the bone, if desired 
			double inputBoneHeight, //bone length 
			frameType coordinateType							
			) throws NullParentForBoneException {
		super(par, tipHeading, rollHeading, inputTag, inputBoneHeight, coordinateType);
	}

	public Bone(AbstractArmature armature, DVector tipHeading, DVector rollHeading, String inputTag, double boneHeight,
			frameType coordinateType) {
		super(armature, tipHeading, rollHeading, inputTag, boneHeight, coordinateType);
	}
	
	/** 
	 * Creates a new bone of specified length emerging from the parentBone. 
	 * The new bone extends in the same exact direction as the parentBone. 
	 * You can then manipulate its orientation using something like 
	 * rotAboutFrameX(), rotAboutFrameY(), or rotAboutFrameZ().
	 * You can also change its frame of rotation using setFrameOfRotation(Axes rotationFrame);
	 * 
	 * @param par the parent bone to which this bone is attached. 
	 * @param inputTag some user specified name for the bone, if desired
	 * @param inputBoneHeight bone length 
	 */ 
	public Bone (AbstractBone par, //parent bone
			String inputTag, //some user specified name for the bone, if desired
			double inputBoneHeight //bone length 
			) {
		super(par, 0,0,0, inputTag, inputBoneHeight);		
	}
	
	/**
	 * 
	 * @param par the parent bone to which this bone is attached. 
	 * @param xAngle how much the bone should be pitched relative to its parent bone
	 * @param yAngle how much the bone should be rolled relative to its parent bone
	 * @param zAngle how much the bone should be yawed relative to its parent bone
	 * @param inputTag some user specified name for the bone, if desired
	 * @param inputBoneHeight bone length 
	 */ 
	
	public Bone (AbstractBone par, //parent bone
			double xAngle, //how much the bone should be pitched relative to its parent bone
			double yAngle, //how much the bone should be rolled relative to its parent bone
			double zAngle, //how much the bone should be yawed relative to its parent bone
			String inputTag, //some user specified name for the bone, if desired
			double inputBoneHeight //bone length 
			) {
		super(par, xAngle, yAngle, zAngle,inputTag, inputBoneHeight);		
	}
	

	@Override
	protected void generateAxes(DVector origin, DVector x, DVector y, DVector z) {
		this.localAxes = new Axes(origin, x, y, z);
		this.localAxes.orthogonalize();
	}

	@Override
	protected IKPin createAndReturnPinAtOrigin(DVector origin) {
		// TODO Auto-generated method stub
		return new IKPin(new Axes(
				new Ray((DVector) this.getTip(), new DVector(1,0,0)), 
				new Ray((DVector) this.getTip(),new DVector(0,1,0)), 
				new Ray((DVector) this.getTip(),new DVector(0,0,1)), 
				parentArmature.localAxes()), false, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFrameofRotation(AbstractAxes rotationFrameCoordinates) {
		super.setFrameofRotation(rotationFrameCoordinates);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void rotAboutFrameX(double amt) {
		super.rotAboutFrameX(amt);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void rotAboutFrameY(double amt) {
		super.rotAboutFrameY(amt);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void rotAboutFrameZ(double amt) {
		super.rotAboutFrameZ(amt);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void rotateAxially(double amt) {
		super.rotateAxially(amt);
	}
	
	/** 
	 {@inheritDoc}
	 */
	@Override
	public double[] getXZYAngle() {
		return super.getXZYAngle();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Rotation getRotation() {
		return super.getRotation();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disablePin() {
		super.disablePin();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enablePin() {
		super.enablePin();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enablePin(DVector pin) {
		super.enablePin(pin);
	}
	
	/**
	 * @param pin new position for the pin (in global coordinates).
	 */
	@Override
	public void setPin(DVector pin) {
		super.setPin(pin);
	}

	 
	/**
	 * @return In the case of this out-of-the-box class, getPin() returns a DVector indicating
	 * the spatial target of the pin. 
	 */
	public DVector getPin() {
		if(pin == null) return null;
		else return pin.axes.origin();
	}
	
	
	
	

}
