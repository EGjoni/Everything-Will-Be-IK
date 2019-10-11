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
import java.util.ArrayList;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Template;

import IK.IKExceptions;
import IK.IKExceptions.NullParentForBoneException;
import IK.doubleIK.AbstractBone;
import IK.doubleIK.AbstractIKPin;
import IK.doubleIK.Constraint;
import IK.doubleIK.SegmentedArmature.WorkingBone;
import data.EWBIKLoader;
import data.EWBIKSaver;
import math.doubleV.AbstractAxes;
import math.doubleV.MRotation;
import math.doubleV.MathUtils;
import math.doubleV.Rot;
import math.doubleV.RotationOrder;
import math.doubleV.SGVec_3d;
import math.doubleV.Vec3d;
import math.doubleV.sgRayd;
import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONArray;
import asj.data.JSONObject;


/**
 * @author Eron Gjoni
 *
 */
public abstract class AbstractBone implements Saveable, Comparable<AbstractBone> {

	public static enum frameType {GLOBAL, RELATIVE};

	public AbstractArmature parentArmature;
	protected String tag;

	protected Rot lastRotation;
	protected AbstractAxes previousOrientation; 
	protected AbstractAxes localAxes;
	protected AbstractAxes majorRotationAxes;

	protected double boneHeight;
	protected AbstractBone parent;

	protected ArrayList<AbstractBone> children = new ArrayList<>();
	protected ArrayList<AbstractBone> freeChildren = new ArrayList<>();
	protected ArrayList<AbstractBone> effectoredChildren = new ArrayList<>();


	public Constraint constraints;
	protected AbstractIKPin pin = null;
	protected boolean orientationLock = false;
	protected double stiffnessScalar = 0f;
	public int ancestorCount = 0;

	/**
	 * 
	 * @param par the parent bone for this bone
	 * @param tipHeading the orienational heading of this bone (global vs relative coords specified in coordinateType)
	 * @param rollHeading axial rotation heading of the bone (it's z-axis) 
	 * @param inputTag some user specified name for the bone, if desired 
	 * @param inputBoneHeight bone length 
	 * @param coordinateType
	 * @throws NullParentForBoneException
	 */

	public <V extends Vec3d<?>> AbstractBone (
			AbstractBone par, //parent bone
			V tipHeading, //the orienational heading of this bone (global vs relative coords specified in coordinateType)
			V rollHeading, //axial rotation heading of the bone (it's z-axis) 
			String inputTag,	 //some user specified name for the bone, if desired 
			double inputBoneHeight, //bone length 
			frameType coordinateType							
			) throws NullParentForBoneException {

		this.lastRotation = new Rot(MRotation.IDENTITY);
		if(par != null) {
			if(inputTag == null || inputTag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;
			this.boneHeight = inputBoneHeight;

			sgRayd tipHeadingRay = new sgRayd(par.getTip_(), tipHeading);
			sgRayd rollHeadingRay = new sgRayd(par.getTip_(), rollHeading);
			Vec3d<?> tempTip = tipHeading.copy(); tempTip.set(0,0,0); 
			Vec3d<?> tempRoll =rollHeading.copy(); tempRoll.set(0,0,0); 
			Vec3d<?>tempX = tempRoll.copy(); tempX.set(0,0,0); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = par.localAxes().getGlobalOf(tipHeadingRay.heading());
				tempRoll = par.localAxes().getGlobalOf(rollHeadingRay.heading());
			} else { 

				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempTip.crossCopy(tempRoll);
			//TODO: this commented out one is correct. Using old version test chirality code. 
			tempRoll = tempX.crossCopy(tempTip); 
			//tempRoll = tempTip.crossCopy(tempX);

			tempX.normalize();
			tempTip.normalize();
			tempRoll.normalize();


			this.parent = par;
			this.parentArmature = this.parent.parentArmature;
			parentArmature.addToBoneList(this);

			generateAxes(parent.getTip_(), tempX, tempTip, tempRoll);
			//this.localAxes.orthoNormalize(true);
			localAxes.setParent(parent.localAxes);

			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parent.localAxes().getGlobalCopy(); 
			majorRotationAxes.translateTo(parent.getTip_());
			majorRotationAxes.setParent(parent.localAxes);

			this.parent.addFreeChild(this);
			this.parent.addChild(this);
			this.updateAncestorCount();
			//this.updateSegmentedArmature();	
		} else {
			throw IKExceptions.NullParentForBoneException();
		}

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
	public AbstractBone (AbstractBone par, //parent bone
			double xAngle, //how much the bone should be pitched relative to its parent bone
			double yAngle, //how much the bone should be rolled relative to its parent bone
			double zAngle, //how much the bone should be yawed relative to its parent bone
			String inputTag, //some user specified name for the bone, if desired
			double inputBoneHeight //bone length 
			) throws NullParentForBoneException {

		this.lastRotation = new Rot(MRotation.IDENTITY);
		if(par != null) {
			if(inputTag == null || inputTag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else {
				this.tag = inputTag;
			}
			this.boneHeight = inputBoneHeight;

			AbstractAxes tempAxes = par.localAxes().getGlobalCopy();
			Rot newRot = new Rot(new MRotation(RotationOrder.XZY, xAngle, yAngle, zAngle));
			tempAxes.rotateBy(newRot);			

			this.parent = par;
			this.parentArmature = this.parent.parentArmature;
			parentArmature.addToBoneList(this);

			generateAxes(parent.getTip_(), tempAxes.x_().heading(), tempAxes.y_().heading(), tempAxes.z_().heading());
			//this.localAxes.orthoNormalize(true);
			localAxes.setParent(parent.localAxes);			
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parent.localAxes().getGlobalCopy(); 
			majorRotationAxes.translateTo(parent.getTip_());
			majorRotationAxes.setParent(parent.localAxes);

			this.parent.addFreeChild(this);
			this.parent.addChild(this);
			this.updateAncestorCount();
			//this.updateSegmentedArmature();	
		} else {
			throw IKExceptions.NullParentForBoneException();
		}

	}


	private void updateAncestorCount() {
		int countedAncestors = 0;
		AbstractBone currentBone = this.parent;
		while(currentBone != null) {
			countedAncestors++; 
			currentBone = currentBone.parent;
		}
		setAncestorCount(countedAncestors);
	}

	/**updates the ancestor count for this bone, and 
	 * sets the ancestor count of all child bones 
	 * to this bone's ancestor count +1;
	 * @param count
	 */
	private void setAncestorCount(int count) {
		this.ancestorCount = count; 
		for(AbstractBone b : this.children) {
			b.setAncestorCount(this.ancestorCount +1);
		}
	}



	public AbstractBone (AbstractBone par, 
			Vec3d<?> tipHeading, 
			Vec3d<?> rollHeading, 
			String inputTag, 
			frameType coordinateType) 
					throws NullParentForBoneException {

		this.lastRotation = new Rot(MRotation.IDENTITY);
		if(par != null) {
			if(inputTag == null || inputTag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;

			sgRayd tipHeadingRay = new sgRayd(par.getTip_(), tipHeading);
			sgRayd rollHeadingRay = new sgRayd(par.getTip_(), rollHeading);
			Vec3d<?> tempTip = tipHeading.copy(); 
			Vec3d<?> tempRoll = rollHeading.copy();
			Vec3d<?> tempX = new SGVec_3d(); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = par.localAxes().getGlobalOf(tipHeadingRay.heading());
				tempRoll = par.localAxes().getGlobalOf(rollHeadingRay.heading());
			} else {
				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempTip.crossCopy(tempRoll);
			//TODO: this commented out one is correct. Using old version test chirality code. 
			tempRoll = tempX.crossCopy(tempTip); 
			//tempRoll = tempTip.crossCopy(tempX); 		

			tempX.normalize();
			tempTip.normalize();
			tempRoll.normalize();

			this.boneHeight = tipHeadingRay.mag();
			this.parent = par;
			this.parentArmature = this.parent.parentArmature;
			parentArmature.addToBoneList(this);

			generateAxes(parent.getTip_(), tempX, tempTip, tempRoll);

			localAxes.setParent(parent.localAxes);			
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parent.localAxes().getGlobalCopy(); 
			majorRotationAxes.translateTo(parent.getTip_());
			majorRotationAxes.setParent(parent.localAxes);


			this.parent.addChild(this);
			this.parent.addFreeChild(this);
			this.updateAncestorCount();
			//this.updateSegmentedArmature();	
		} else {
			throw new NullParentForBoneException();
		}
	}


	protected abstract void generateAxes(Vec3d<?> origin, Vec3d<?> x, Vec3d<?> y, Vec3d<?> z);

	public AbstractBone (
			AbstractArmature parArma, 
			Vec3d<?> tipHeading, 
			Vec3d<?> rollHeading, 
			String inputTag, 
			double inputBoneHeight, 
			frameType coordinateType) 
					throws NullParentForBoneException {

		this.lastRotation = new Rot(MRotation.IDENTITY);
		if(parArma != null) {
			if(inputTag == null || inputTag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;

			sgRayd tipHeadingRay = new sgRayd(parArma.localAxes.origin_(), tipHeading);
			tipHeadingRay.getRayScaledTo(inputBoneHeight);
			sgRayd rollHeadingRay = new sgRayd(parArma.localAxes.origin_(), rollHeading);
			Vec3d<?> tempTip = tipHeading.copy(); 
			Vec3d<?> tempRoll = rollHeading.copy(); 
			Vec3d<?> tempX = tempTip.copy(); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = parArma.localAxes.getGlobalOf(tipHeadingRay.heading());
				tempRoll = parArma.localAxes.getGlobalOf(rollHeadingRay.heading());
			} else {
				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempTip.crossCopy(tempRoll);
			//TODO: this commented out one is correct. Using old version test chirality code. 
			tempRoll = tempX.crossCopy(tempTip); 
			//tempRoll = tempTip.crossCopy(tempX);  

			tempX.normalize();
			tempTip.normalize();
			tempRoll.normalize();

			this.parentArmature = parArma;
			parentArmature.addToBoneList(this);	

			generateAxes(parentArmature.localAxes.origin_(), tempX, tempTip, tempRoll);
			//this.localAxes.orthoNormalize(true);
			localAxes.setParent(parentArmature.localAxes);
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parentArmature.localAxes().getGlobalCopy(); 
			majorRotationAxes.setParent(parentArmature.localAxes());



			this.boneHeight = inputBoneHeight;
			this.updateAncestorCount();
			//this.updateSegmentedArmature();	


		} else {
			throw new NullParentForBoneException();
		}

	}

	public AbstractBone (AbstractArmature parArma, 
			Vec3d<?> tipHeading, 
			Vec3d<?> rollHeading, 
			String inputTag, 
			frameType coordinateType) 
					throws NullParentForBoneException {

		this.lastRotation = new Rot(MRotation.IDENTITY);
		if(parArma != null) {
			if(inputTag == null || inputTag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;

			sgRayd tipHeadingRay = new sgRayd(parArma.localAxes.origin_(), tipHeading);
			sgRayd rollHeadingRay = new sgRayd(parArma.localAxes.origin_(), rollHeading);
			Vec3d<?> tempTip = tipHeading.copy(); 
			Vec3d<?> tempRoll = rollHeading.copy(); 
			Vec3d<?> tempX = rollHeading.copy();

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = parArma.localAxes.getGlobalOf(tipHeadingRay.heading());
				tempRoll = parArma.localAxes.getGlobalOf(rollHeadingRay.heading());
			} else {
				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempTip.crossCopy(tempRoll);
			//TODO: this commented out one is correct. Using old version test chirality code. 
			tempRoll = tempX.crossCopy(tempTip); 
			//tempRoll = tempTip.crossCopy(tempX);

			tempX.normalize();
			tempTip.normalize();
			tempRoll.normalize();

			this.boneHeight = tipHeading.mag();
			this.parentArmature = parArma;

			generateAxes(parentArmature.localAxes.origin_(), tempX, tempTip, tempRoll);
			//this.localAxes.orthoNormalize(true);
			localAxes.setParent(parentArmature.localAxes);	
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parentArmature.localAxes().getGlobalCopy(); 
			majorRotationAxes.setParent(parent.localAxes);

			parentArmature.addToBoneList(this);
			this.updateAncestorCount();
			this.updateSegmentedArmature();

		} else {
			throw new NullParentForBoneException();
		}

	}

	public AbstractBone() {
		this.lastRotation = new Rot(MRotation.IDENTITY);
	}

	/*public AbstractBone(AbstractArmature parArma, JSONObject boneJSON, AbstractAxes attachedAxes) {
		this.lastRotation = new Rot(MRotation.IDENTITY);
		this.localAxes = attachedAxes;
		previousOrientation = localAxes.attachedCopy(true);
		this.boneHeight = boneJSON.getDouble("boneHeight");
		this.tag = boneJSON.getString("tag");


		this.parentArmature = parArma;
		parentArmature.addToBoneList(this);
	}*/

	public AbstractBone getParent() {
		return this.parent;
	}

	public void attachToParent(AbstractBone inputParent) {
		inputParent.addChild(this);
		this.parent = inputParent;
		this.updateAncestorCount();
	}



	public void solveIKFromHere() {
		this.parentArmature.IKSolver(this);
	}


	public void snapToConstraints() {
		if(constraints != null) {		
			constraints.snapToLimits();  
		}
	}	



	/**
	 * Called whenever this bone's orientation has changed due to an Inverse Kinematics computation. 
	 * This function is called only once per IK solve, not per iteration. Meaning one call to Solve IK **SHOULD NOT** 
	 * results in  more than one call to each affected bone. 
	 */
	public void IKUpdateNotification() {


	}

	/**same as snapToConstraints, but operates on user
	 * supplied axes meant to correspond to the bone's 
	 * localAxes and majorRotationAxes 
	 * 
	 * you are unlikely to need to use this, and at the moment 
	 * it presumes KusudamaExample constraints 
	 */
	public void setAxesToSnapped(AbstractAxes toSet, AbstractAxes limitingAxes, double cosHalfAngleDampen) {
		if(constraints != null && AbstractKusudama.class.isAssignableFrom(constraints.getClass())) {
			((AbstractKusudama)constraints).setAxesToSnapped(toSet, limitingAxes, cosHalfAngleDampen);
		}
	}
	
	public void setAxesToReturnfulled(AbstractAxes toSet, AbstractAxes limitingAxes, double cosHalfAngleDampen) {
		if(constraints != null && AbstractKusudama.class.isAssignableFrom(constraints.getClass())) {
			((AbstractKusudama)constraints).setAxesToReturnfulled(toSet, limitingAxes, cosHalfAngleDampen);
		}
	}


	public void setPin_(SGVec_3d pin) {
		if(this.pin == null) {
			this.enablePin_(pin);
		} else {
			this.pin.translateTo_(pin);
		}
		//parentArmature.updateArmatureSegments();
	}


	/**
	 * @param newConstraint a constraint Object to add to this bone
	 * @return the constraintObject that was just added
	 */
	public Constraint addConstraint(Constraint newConstraint) {
		constraints = newConstraint;
		return constraints;
	}

	/**
	 * 
	 * @return this bone's constraint object.
	 */
	public Constraint getConstraint() {
		return constraints;
	}

	/** 
	 * @return an array where each element indicated how much this bone is rotated on the X,Y,Z (in that order)
	 * axes relative to its constraint Axes. 
	 * If there are no constraint Axes, the result is relative to the parent bone. 
	 * If the bone has no parent, this method throws an exception.
	 * @throws NullParentForBoneException
	 */
	public double[] getXYZAngle() throws NullParentForBoneException {
		if(this.parent != null) {
			this.localAxes().markDirty(); this.localAxes().updateGlobal();
			AbstractAxes result =this.localAxes().getGlobalCopy();
			this.getMajorRotationAxes().updateGlobal();
			this.getMajorRotationAxes().globalMBasis.setToLocalOf(result.globalMBasis, result.globalMBasis);
			return result.globalMBasis.rotation.rotation.getAngles(RotationOrder.XYZ);
		} else {
			return null;
		}
	}

	/**
	 * @return An Apache Commons Rotation object representing the rotation of this bone relative to its 
	 * reference frame. The Rotation object is more versatile and robust than an array of angles. 
	 * And allows you to treat rotation in a wide variety of conventions.
	 */
	public Rot getRotation() {
		return (new Rot(this.majorRotationAxes.x_().heading(), this.majorRotationAxes.y_().heading(), 
				this.localAxes().x_().heading(), this.localAxes().y_().heading()));
	}

	/**
	 * @return An Apache Commons Rotation object representing the rotation which transforms this 
	 * BoneExample from its previous orientation to its current orientation. 
	 */

	public Rot getRotationFromPrevious() {
		return lastRotation;
	}

	/** 
	 * @return the reference frame representing this bone's previous orientation relative to
	 * its parent.
	 */
	public AbstractAxes getPreviousOrientation() {
		return previousOrientation;
	}
	/**
	 * Rotate the bone about its frame of reference by a custom Apache Commons Rotation object
	 * @param rot
	 */
	public void rotateBy(Rot rot) {
		this.previousOrientation.alignLocalsTo(localAxes);
		this.localAxes.rotateBy(rot);

		this.lastRotation.set(rot);
	}

	/**
	 * rotates the bone about the major X axis of rotation, 
	 * obeying constraints by default
	 * @param amt number of degrees to rotate by
	 */
	public void rotAboutFrameX(double amt) {
		rotAboutFrameX(amt, true);
	}

	/**
	 * rotates the bone about the major Y axis of rotation, 
	 * obeying constraints by default
	 * @param amt number of degrees to rotate by
	 */
	public void rotAboutFrameY(double amt) {
		rotAboutFrameY(amt, true);
	}

	/**
	 * rotates the bone about the major Z axis of rotation, 
	 * obeying constraints by default
	 * @param amt number of degrees to rotate by
	 */
	public void rotAboutFrameZ(double amt) {
		rotAboutFrameZ(amt, true);
	}

	public void rotateAxially(double amt) {
		rotateAxially(amt, true);
	}	

	/**
	 * rotates the bone about the major X axis of rotation 
	 * @param amt number of degrees to rotate by
	 * @param obeyConstrants  whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotAboutFrameX(double amt, boolean obeyConstraints) {
		//localAxes().alignLocalsTo(previousOrientation);
		previousOrientation.alignLocalsTo(localAxes);		


		Rot xRot = new Rot(majorRotationAxes.x_().heading(), amt); 
		localAxes.rotateBy(xRot);

		lastRotation.set(xRot);

		if(obeyConstraints) this.snapToConstraints();
	}

	/**
	 * rotates the bone about the major Y axis of rotation 
	 * @param amt number of degrees to rotate by
	 * @param obeyConstrants  whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotAboutFrameY(double amt, boolean obeyConstraints) {
		previousOrientation.alignLocalsTo(localAxes);	

		Rot yRot = new Rot(majorRotationAxes.y_().heading(), amt); 
		localAxes.rotateBy(yRot);

		lastRotation.set(yRot);

		if(obeyConstraints) this.snapToConstraints();
	}


	/**
	 * rotates the bone about the major Z axis of rotation 
	 * @param amt number of degrees to rotate by
	 * @param obeyConstrants  whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotAboutFrameZ(double amt, boolean obeyConstraints) {
		previousOrientation.alignLocalsTo(localAxes);	

		Rot zRot = new Rot(majorRotationAxes.z_().heading(), amt); 
		localAxes.rotateBy(zRot);

		lastRotation.set(zRot);

		if(obeyConstraints) this.snapToConstraints();
	}

	/**
	 * rotates the bone about its own length. (Like a drill, or The Queen waving). 
	 * @param number of degrees to rotate by
	 * @param whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotateAxially(double amt, boolean obeyConstraints) {
		localAxes.rotateAboutY(amt, true);

		if(obeyConstraints) this.snapToConstraints();
	}


	/**
	 * @param rotationFrameCoordinates the Axes around which rotAboutFrameX, rotAboutFrameY, and rotAboutFrameZ will rotate, 
	 * and against which getXZYAngle() will be computed. 
	 * The input is expected to be in RELATIVE coordinates. 
	 * so specifying these axes as having an <br>
	 * x component heading of (1,0,0) and <br>
	 * y component heading of (0,1,0) and<br>
	 * z component heading of (0,0,1) <br>
	 * <br>
	 * is equivalent to specifying the frameOfRotation of this bone as being perfectly aligned 
	 * with the localAxes of its parent bone.<br><br>
	 * 
	 * The physical intuition of this is maybe something like 
	 * "at what angle did I place the servos on this joint"<br><br>
	 * 
	 * It doesn't necessarily determine where the bone can rotate, but it does determine *how*
	 * the bone would rotate to get there.<br><br>
	 * 
	 * This is also used to set constraints. For example, euler constraints are computed against these axes
	 * and the limitCones and axial twist limits of KusudamaExample are specified relative to these Axes.<br><br>
	 * 
	 * Changing these axes is essentially the equivalent of rotating the joint on which this bone rests, 
	 * while keeping the bone in globally in place.<br><br>
	 * 
	 * You don't need to change this unless you start wishing you could change this.
	 */
	public void setFrameofRotation(AbstractAxes rotationFrameCoordinates) {
		majorRotationAxes.alignLocalsTo(rotationFrameCoordinates);
		if(parent != null) {
			majorRotationAxes.translateTo(parent.getTip_());
		}
	}

	/**
	 * Get the Axes relative to which this bone's rotations are defined. (If the bone has constraints, this will be the 
	 * constraint Axes)
	 * @return
	 */	
	public <A extends AbstractAxes> A getMajorRotationAxes() {
		return (A) this.majorRotationAxes;
	}

	/**
	 * Disables the pin for this bone so that it no longer interests the IK Solver. 
	 * However, all information abut the pin is maintained, so the pin can be turned 
	 * on again with enablePin(). 
	 */
	public void disablePin() {
		pin.disable();
		if (this.effectoredChildren.size() == 0) {
			notifyAncestorsOfUnpin();
		}
		this.updateSegmentedArmature();	
	}	

	/**
	 * Entirely removes the pin from this bone. Any child pins attached to it are reparented to this
	 * pin's parent.  
	 */
	public void removePin() {
		pin.disable();
		if (this.effectoredChildren.size() == 0) {
			notifyAncestorsOfUnpin();
		}
		pin.removalNotification(); 
		this.updateSegmentedArmature();	
	}

	protected abstract AbstractIKPin createAndReturnPinOnAxes(AbstractAxes a);


	/**
	 * Enables an existing pin for this BoneExample. Or creates a pin for this bone at the bone's tip. 
	 */
	public void enablePin() {
		//System.out.println("pinning");
		if(pin == null) {
			AbstractAxes pinAxes = this.localAxes().getGlobalCopy();
			pinAxes.setParent(this.parentArmature.localAxes().getParentAxes());
			pin = createAndReturnPinOnAxes(pinAxes);
		}
		pin.enable();
		//System.out.println("clearing children");
		freeChildren.clear(); 
		//System.out.println("adding children");
		for (AbstractBone child : getChildren()) {
			if (child.pin !=null && !child.pin.isEnabled()) {	    	  
				addFreeChild(child);
				//System.out.println("childAdd");
			}
		}
		//System.out.println("notifying ancestors");
		notifyAncestorsOfPin(false);
		//System.out.println("updating segment armature");
		this.updateSegmentedArmature();	
		//System.out.println("segment armature updated");
	}

	/**
	 * Creates a pin for this bone
	 * @param pinTo the position of the pin in the coordinateFrame of the parentArmature.
	 */

	public void enablePin_(Vec3d<?> pinTo) {
		if(pin == null) {
			AbstractAxes pinAxes = this.localAxes().getGlobalCopy();
			pinAxes.setParent(this.parentArmature.localAxes().getParentAxes());
			pinAxes.translateTo(pinTo);
			pin = createAndReturnPinOnAxes(pinAxes);
		}
		else pin.translateTo_(pinTo);
		pin.enable();
		freeChildren.clear(); 
		for (AbstractBone child : getChildren()) {
			if (!child.pin.isEnabled()) {
				addFreeChild(child);
			}
		}
		notifyAncestorsOfPin();
		//this.updateSegmentedArmature();	
	}


	/**
	 * @return true if the bone has a pin enabled, false otherwise.
	 */
	public boolean isPinned() {
		if(pin == null || !pin.isEnabled()) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Creates / enables a pin if there no pin is active, disables the pin if it is active.
	 */
	public void togglePin() {
		if(this.pin == null) this.enablePin();
		this.pin.toggle();
		updateSegmentedArmature();
	}


	public ArrayList<AbstractBone> returnChildrenWithPinnedDescendants() {
		ArrayList<AbstractBone> childrenWithPinned = new ArrayList<AbstractBone>();
		for(AbstractBone c : getChildren()) {
			if(c.hasPinnedDescendant()) 
				childrenWithPinned.add(c);
		}
		return childrenWithPinned;
	}


	public ArrayList<? extends AbstractBone> getMostImmediatelyPinnedDescendants() {
		ArrayList<AbstractBone> mostImmediatePinnedDescendants = new ArrayList<AbstractBone>();
		this.addSelfIfPinned(mostImmediatePinnedDescendants);
		return mostImmediatePinnedDescendants;
	}


	public AbstractAxes getPinnedAxes() {
		if(this.pin == null) return null;
		return this.pin.getAxes();
	}

	public void addSelfIfPinned(ArrayList<AbstractBone> pinnedBones2) {
		if (this.isPinned()) {
			pinnedBones2.add(this);
		} else {
			for (AbstractBone child : getChildren()) {
				child.addSelfIfPinned(pinnedBones2);
			}
		}
	}

	void notifyAncestorsOfPin(boolean updateSegments) {
		if (this.parent != null) {
			parent.addToEffectored(this);
		}		
		if(updateSegments) parentArmature.updateArmatureSegments(); 
	}

	public void notifyAncestorsOfPin() { 
		notifyAncestorsOfPin(true);		
	}

	public void notifyAncestorsOfUnpin() {
		if (this.parent != null) {
			parent.removeFromEffectored(this);
		}
		parentArmature.updateArmatureSegments();
	}

	public void addToEffectored(AbstractBone abstractBone) {
		int freeIndex  = freeChildren.indexOf(abstractBone);
		if ( freeIndex != -1) freeChildren.remove(freeIndex);

		if (effectoredChildren.contains(abstractBone)) {
		} else {
			effectoredChildren.add(abstractBone);
		}
		if (this.parent != null) {
			parent.addToEffectored(this);
		}
	}

	public void removeFromEffectored(AbstractBone abstractBone) {
		int effectoredIndex  = effectoredChildren.indexOf(abstractBone);
		if ( effectoredIndex != -1) effectoredChildren.remove(effectoredIndex);

		if (freeChildren.contains(abstractBone)) {
		} else {
			addFreeChild(abstractBone);
		}
		if (this.parent != null && this.effectoredChildren.size() == 0 && this.pin != null && this.pin.isEnabled()) {
			parent.removeFromEffectored(this);
		}
	}

	public AbstractBone getPinnedRootBone() {
		AbstractBone rootBone = this;
		while (rootBone.parent != null && !rootBone.parent.pin.isEnabled()) {
			rootBone = rootBone.parent;
		} 
		return rootBone;
	}

	public void  updateSegmentedArmature() {
		this.parentArmature.updateArmatureSegments();
	}


	public void setTag(String newTag) {
		parentArmature.updateBoneTag(this, this.tag, newTag);
		this.tag = newTag;
	}

	public String getTag() {
		return this.tag;
	}
	public Vec3d<?> getBase_() {
		return localAxes.origin_().copy();
	}

	public Vec3d<?> getTip_() { 		
		return localAxes.y_().getScaledTo(boneHeight);
	}

	public void setBoneHeight(double inBoneHeight) {
		this.boneHeight = inBoneHeight;
		for(AbstractBone child : this.getChildren()) {
			child.localAxes().translateTo(this.getTip_());
			child.majorRotationAxes.translateTo(this.getTip_());
		}
	}  


	/**
	 * removes this BoneExample and any of its children from the armature.
	 */
	public void deleteBone() {
		ArrayList<AbstractBone> bones = new ArrayList<>();
		AbstractBone root = parentArmature.rootBone;
		root.hasChild(bones, this);
		for(AbstractBone p : bones) {
			System.out.println("removing from" + p);
			p.removeFromEffectored(this);
			for(AbstractBone ab : this.effectoredChildren) {
				p.removeFromEffectored(ab);
			}
			p.getChildren().remove(this);
			p.freeChildren.remove(this);
		}
		this.parentArmature.removeFromBoneList(this);
	}

	/*adds this bone to the arrayList if inputBone is among its children*/
	private void hasChild(ArrayList<AbstractBone> list, AbstractBone query) {
		if(getChildren().contains(query)) 
			list.add(this); 
		for(AbstractBone c : getChildren()) {
			c.hasChild(list, query);
		}
	}

	public AbstractAxes localAxes() { 
		return this.localAxes;
	}

	public double getBoneHeight() {
		return this.boneHeight;
	}

	public boolean hasPinnedDescendant() {
		if(this.isPinned()) return true; 
		else {
			boolean result = false;
			for(AbstractBone c : getChildren()) {
				if(c.hasPinnedDescendant()) {
					result = true;
					break;
				}
			}
			return result;
		}

	}

	public AbstractIKPin getIKPin() {
		return this.pin;
	}


	/**
	 * if set to true, the IK system will not rotate this bone
	 * as it solves the IK chain. 
	 * @param val
	 */
	public void setIKOrientationLock(boolean val) {
		this.orientationLock = val;
	}

	public boolean getIKOrientationLock() {
		return this.orientationLock;
	}

	public void addChild(AbstractBone bone) {		
		if(this.getChildren().indexOf(bone) == -1) {
			((ArrayList<AbstractBone>)getChildren()).add(bone);
		}
		//parentArmature.updateArmatureSegments();
	}


	public void addFreeChild(AbstractBone bone) {		
		if(this.freeChildren.indexOf(bone) == -1) {
			freeChildren.add(bone);
		}
		parentArmature.updateArmatureSegments();
	}

	public void addEffectoredChild(AbstractBone bone) {		
		if(this.effectoredChildren.indexOf(bone) == -1) {
			this.effectoredChildren.add(bone);
		}

	}

	public void addDescendantsToArmature() {
		for(AbstractBone b : getChildren()) {
			parentArmature.addToBoneList(b);
			b.addDescendantsToArmature();
		}
	}

	public ArrayList<? extends AbstractBone> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<? extends AbstractBone> children) {
		this.children = (ArrayList<AbstractBone>) children;
	}

	/**
	 * The stiffness of a bone determines how much the IK solver should 
	 * prefer to avoid rotating it if it can. A value of 0  means the solver will 
	 * rotate this bone as much as the overall dampening parameter will 
	 * allow it to per iteration. A value of 0.5 means the solver will 
	 * rotate it half as much as the dampening parameter will allow,
	 * and a value of 1 effectively means the solver is not allowed 
	 * to rotate this bone at all. 
	 * @return a value between 1 and 0. 
	 */
	public double getStiffness() {
		return stiffnessScalar; 
	}

	/**
	 * The stiffness of a bone determines how much the IK solver should 
	 * prefer to avoid rotating it if it can. A value of 0  means the solver will 
	 * rotate this bone as much as the overall dampening parameter will 
	 * allow it to per iteration. A value of 0.5 means the solver will 
	 * rotate it half as much as the dampening parameter will allow,
	 * and a value of 1 effectively means the solver is not allowed 
	 * to rotate this bone at all. 
	 */
	public void setStiffness(double stiffness) {
		stiffnessScalar = stiffness;
		if(parentArmature != null) {
			SegmentedArmature s = parentArmature.boneSegmentMap.get(this);
			if(s != null ) {
				WorkingBone wb = s.simulatedBones.get(this);
				if(wb != null) {
					wb.updateCosDampening();
				}
			}
		}
	}

	@Override
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		this.localAxes = (AbstractAxes) l.getObjectFromClassMaps(AbstractAxes.class, j.getString("localAxes"));
		this.majorRotationAxes = (AbstractAxes) l.getObjectFromClassMaps(AbstractAxes.class, j.getString("majorRotationAxes"));
		this.parentArmature = (AbstractArmature) l.getObjectFromClassMaps(AbstractArmature.class, j.getString("parentArmature")); 
		l.arrayListFromJSONArray(j.getJSONArray("children"), this.children, this.getClass()); 
		this.boneHeight = j.getDouble("boneHeight");
		if(j.hasKey("stiffness")) 
			this.setStiffness(j.getDouble("stiffness"));			
		if(j.hasKey("constraints")) 
			this.constraints = (Constraint) l.getObjectFromClassMaps(Constraint.class, j.getString("constraints"));
		if(j.hasKey("IKPin")) 
			this.pin = (AbstractIKPin) l.getObjectFromClassMaps(AbstractIKPin.class, j.getString("IKPin"));
		this.tag = j.getString("tag");
	}

	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		JSONObject thisBone = new JSONObject();
		thisBone.setString("identityHash", this.getIdentityHash());
		thisBone.setString("localAxes", this.localAxes.getIdentityHash());
		thisBone.setString("majorRotationAxes", majorRotationAxes.getIdentityHash());
		thisBone.setString("parentArmature",  ((Saveable) parentArmature).getIdentityHash());
		JSONArray children = saveManager.arrayListToJSONArray(getChildren());
		thisBone.setJSONArray("children", children);
		if(constraints != null) {
			thisBone.setString("constraints", constraints.getIdentityHash());
		}
		if(pin != null) 
			thisBone.setString("IKPin", pin.getIdentityHash());

		thisBone.setDouble("boneHeight", this.getBoneHeight());
		thisBone.setDouble("stiffness", this.getStiffness());

		return thisBone; 
	}



	@Override
	public void makeSaveable(SaveManager saveManager) {
		saveManager.addToSaveState(this);
		if(this.getIKPin() != null) {
			this.getIKPin().makeSaveable(saveManager);
		}
		for(AbstractBone b : this.children) {
			b.makeSaveable(saveManager);
		}
		if(this.getConstraint() != null) {
			this.getConstraint().makeSaveable(saveManager);
		}
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
		this.setBoneHeight(boneHeight);
		for(AbstractBone b : this.children) {
			b.attachToParent(this);
		}
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

	@Override 
	public int compareTo(AbstractBone i) {
		return this.ancestorCount - i.ancestorCount;
	}

	public String toString() {
		return this.getTag();
	}
}
