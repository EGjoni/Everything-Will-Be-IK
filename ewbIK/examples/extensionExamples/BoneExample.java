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

package extensionExamples;

import IK.AbstractArmature;
import IK.AbstractBone;
import IK.IKExceptions;
import IK.AbstractBone.frameType;
import IK.IKExceptions.NullParentForBoneException;
import sceneGraph.*;
import sceneGraph.math.SGVec_3d;
import sceneGraph.math.Vec3d;

public class BoneExample extends AbstractBone {
	
	
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
	public BoneExample (BoneExample par, //parent bone
			ExampleVector tipHeading, //the orienational heading of this bone (global vs relative coords specified in coordinateType)
			ExampleVector  rollHeading, //axial rotation heading of the bone (it's z-axis) 
			String inputTag,	 //some user specified name for the bone, if desired 
			double inputBoneHeight, //bone length 
			frameType coordinateType							
			) throws NullParentForBoneException {
		super(
				par, 
				AxesExample.toSGVec(tipHeading), 
				AxesExample.toSGVec(rollHeading),
				inputTag, 
				inputBoneHeight, 
				coordinateType);
	}

	public BoneExample(
			ArmatureExample armature, 
			ExampleVector  tipHeading, 
			ExampleVector  rollHeading, 
			String inputTag, 
			double boneHeight,
			frameType coordinateType) {
		super(
				armature, 
				AxesExample.toSGVec(tipHeading), 
				AxesExample.toSGVec(rollHeading),
				inputTag, 
				boneHeight, 
				coordinateType);
	}
	
	
	public BoneExample (AbstractBone par, //parent bone
			String inputTag, //some user specified name for the bone, if desired
			double inputBoneHeight //bone length 
			) {
		super(par, 0,0,0, inputTag, inputBoneHeight);		
	}
	
	
	
	public BoneExample (AbstractBone par, //parent bone
			double xAngle, //how much the bone should be pitched relative to its parent bone
			double yAngle, //how much the bone should be rolled relative to its parent bone
			double zAngle, //how much the bone should be yawed relative to its parent bone
			String inputTag, //some user specified name for the bone, if desired
			double inputBoneHeight //bone length 
			) {
		super(par, xAngle, yAngle, zAngle,inputTag, inputBoneHeight);		
	}
	

	@Override
	protected void generateAxes(SGVec_3d origin, SGVec_3d x, SGVec_3d y, SGVec_3d z) {
		this.localAxes = new AxesExample(origin, x, y, z);
	}
	
	public ExampleVector getBase() {
		return AxesExample.toExampleVector(super.getBase_());
	}

	public ExampleVector getTip() { 		
		return AxesExample.toExampleVector(super.getTip_());
	}

	@Override
	protected IKPinExample createAndReturnPinAtOrigin(SGVec_3d origin) {
		// TODO Auto-generated method stub
		AbstractAxes thisBoneAxes = localAxes().getGlobalCopy(); 
		thisBoneAxes.setOrthoNormalityConstraint(true);
		thisBoneAxes.translateTo(origin);
		return new IKPinExample(
						thisBoneAxes, 
						true, 
						this
				);
	}
	
	public SGVec_3d getBase_() {
		return localAxes.origin_().copy();
	}

	public SGVec_3d getTip_() { 		
		return localAxes.y_().getScaledTo(boneHeight);
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
	public Rot getRotation() {
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
	
	
	public void enablePin(ExampleVector pin) {
		super.enablePin_(AxesExample.toSGVec(pin));
	}
	
	
	public void setPin(ExampleVector pin) {
		super.setPin_(AxesExample.toSGVec(pin));
	}

	 
	/**
	 * @return In the case of this out-of-the-box class, getPin() returns a IKVector indicating
	 * the spatial target of the pin. 
	 */
	public ExampleVector getPinLocation() {
		if(pin == null) return null;
		else return AxesExample.toExampleVector(pin.getLocation_());
	}
	
	

	
	
	

}
