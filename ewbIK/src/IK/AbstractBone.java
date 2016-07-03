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
import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;

import processing.data.JSONObject;
import sceneGraph.*;


/**
 * @author Eron Gjoni
 *
 */
public abstract class AbstractBone {
	
	public static enum frameType {GLOBAL, RELATIVE};

	public AbstractArmature parentArmature;
	protected String tag;

	protected Rotation lastRotation;
	protected AbstractAxes previousOrientation; 
	protected AbstractAxes localAxes;
	protected AbstractAxes majorRotationAxes;

	protected double boneHeight;
	protected AbstractBone parent;

	protected ArrayList<AbstractBone> children = new ArrayList<AbstractBone>();
	protected ArrayList<AbstractBone> freeChildren = new ArrayList<AbstractBone>();
	protected ArrayList<AbstractBone> effectoredChildren = new ArrayList<AbstractBone>();


	public Constraint constraints;
	protected AbstractIKPin pin = null;
	
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

	public AbstractBone (AbstractBone par, //parent bone
			DVector tipHeading, //the orienational heading of this bone (global vs relative coords specified in coordinateType)
			DVector rollHeading, //axial rotation heading of the bone (it's z-axis) 
			String inputTag,	 //some user specified name for the bone, if desired 
			double inputBoneHeight, //bone length 
			frameType coordinateType							
			) throws NullParentForBoneException {
		if(par != null) {
			if(this.tag == null || this.tag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;
			this.boneHeight = inputBoneHeight;

			Ray tipHeadingRay = new Ray(par.getTip(), tipHeading);
			Ray rollHeadingRay = new Ray(par.getTip(), rollHeading);
			DVector tempTip = new DVector(); 
			DVector tempRoll = new DVector(); 
			DVector tempX = new DVector(); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = par.localAxes().getGlobalOf(tipHeadingRay.heading());
				tempRoll = par.localAxes().getGlobalOf(rollHeadingRay.heading());
			} else { 

				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempRoll.cross(tempTip);
			tempRoll = tempX.cross(tempTip); 


			this.parent = par;
			this.parentArmature = this.parent.parentArmature;
			parentArmature.addToBoneList(this);

			generateAxes(parent.getTip(), tempX, tempTip, tempRoll);
			this.localAxes.orthogonalize();
			localAxes.setParent(parent.localAxes);
			
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parent.localAxes().getAbsoluteCopy(); 
			majorRotationAxes.translateTo(parent.getTip());
			majorRotationAxes.setParent(parent.localAxes);

			this.parent.addFreeChild(this);
			this.parent.addChild(this);

			this.updateSegmentedArmature();	
		} else {
			throw new NullParentForBoneException();
		}

	}

	public AbstractBone (AbstractBone par, //parent bone
			double xAngle, //how much the bone should be pitched relative to its parent bone
			double yAngle, //how much the bone should be rolled relative to its parent bone
			double zAngle, //how much the bone should be yawed relative to its parent bone
			String inputTag, //some user specified name for the bone, if desired
			double inputBoneHeight //bone length 
			) throws NullParentForBoneException {

		if(par != null) {
			if(this.tag == null || this.tag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;
			this.boneHeight = inputBoneHeight;

			AbstractAxes tempAxes = par.localAxes().getAbsoluteCopy();
			Rotation toRot = new Rotation(RotationOrder.XZY, xAngle, yAngle, zAngle);
			Rot newRot = new Rot();
			newRot.rotation = toRot;
			tempAxes.rotateTo(newRot);			

			this.parent = par;
			this.parentArmature = this.parent.parentArmature;
			parentArmature.addToBoneList(this);

			generateAxes(parent.getTip(), tempAxes.x().heading(), tempAxes.y().heading(), tempAxes.z().heading());
			this.localAxes.orthogonalize();
			localAxes.setParent(parent.localAxes);			
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parent.localAxes().getAbsoluteCopy(); 
			majorRotationAxes.translateTo(parent.getTip());
			majorRotationAxes.setParent(parent.localAxes);

			this.parent.addFreeChild(this);
			this.parent.addChild(this);

			this.updateSegmentedArmature();	
		} else {
			throw new NullParentForBoneException();
		}

	}

	public AbstractBone (AbstractBone par, 
			DVector tipHeading, 
			DVector rollHeading, 
			String inputTag, 
			frameType coordinateType) 
					throws NullParentForBoneException {
		if(par != null) {
			if(this.tag == null || this.tag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;

			Ray tipHeadingRay = new Ray(par.getTip(), tipHeading);
			Ray rollHeadingRay = new Ray(par.getTip(), rollHeading);
			DVector tempTip = new DVector(); 
			DVector tempRoll = new DVector(); 
			DVector tempX = new DVector(); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = par.localAxes().getGlobalOf(tipHeadingRay.heading());
				tempRoll = par.localAxes().getGlobalOf(rollHeadingRay.heading());
			} else {
				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempRoll.cross(tempTip);
			tempRoll = tempX.cross(tempTip); 

			this.boneHeight = tipHeadingRay.mag();
			this.parent = par;
			this.parentArmature = this.parent.parentArmature;
			parentArmature.addToBoneList(this);

			generateAxes(parent.getTip(), tempX, tempTip, tempRoll);
			this.localAxes.orthogonalize();
			localAxes.setParent(parent.localAxes);
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parent.localAxes().getAbsoluteCopy(); 
			majorRotationAxes.translateTo(parent.getTip());
			majorRotationAxes.setParent(parent.localAxes);


			this.parent.addChild(this);
			this.parent.addFreeChild(this);
			this.updateSegmentedArmature();	
		} else {
			throw new NullParentForBoneException();
		}
	}


	protected abstract void generateAxes(DVector origin, DVector x, DVector y, DVector z);

	public AbstractBone (AbstractArmature parArma, 
			DVector tipHeading, 
			DVector rollHeading, 
			String inputTag, 
			double inputBoneHeight, 
			frameType coordinateType) 
					throws NullParentForBoneException {

		if(parArma != null) {
			if(this.tag == null || this.tag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;

			Ray tipHeadingRay = new Ray(parArma.localAxes.origin(), tipHeading);
			tipHeadingRay.getRayScaledTo(inputBoneHeight);
			Ray rollHeadingRay = new Ray(parArma.localAxes.origin(), rollHeading);
			DVector tempTip = new DVector(); 
			DVector tempRoll = new DVector(); 
			DVector tempX = new DVector(); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = parArma.localAxes.getGlobalOf(tipHeadingRay.heading());
				tempRoll = parArma.localAxes.getGlobalOf(rollHeadingRay.heading());
			} else {
				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempRoll.cross(tempTip);
			tempRoll = tempX.cross(tempTip); 

			this.parentArmature = parArma;
			parentArmature.addToBoneList(this);	

			generateAxes(parentArmature.localAxes.origin(), tempX, tempTip, tempRoll);
			this.localAxes.orthogonalize();
			localAxes.setParent(parentArmature.localAxes);
			previousOrientation = localAxes.attachedCopy(true);
			
			majorRotationAxes = parentArmature.localAxes().getAbsoluteCopy(); 
			majorRotationAxes.setParent(parentArmature.localAxes());



			this.boneHeight = inputBoneHeight;
			//this.updateSegmentedArmature();	


		} else {
			throw new NullParentForBoneException();
		}

	}

	public AbstractBone (AbstractArmature parArma, 
			DVector tipHeading, 
			DVector rollHeading, 
			String inputTag, 
			frameType coordinateType) 
					throws NullParentForBoneException {

		if(parArma != null) {
			if(this.tag == null || this.tag == "") {
				this.tag = Integer.toString(System.identityHashCode(this));			
			} else this.tag = inputTag;

			Ray tipHeadingRay = new Ray(parArma.localAxes.origin(), tipHeading);
			Ray rollHeadingRay = new Ray(parArma.localAxes.origin(), rollHeading);
			DVector tempTip = new DVector(); 
			DVector tempRoll = new DVector(); 
			DVector tempX = new DVector(); 

			if(coordinateType == frameType.GLOBAL) {
				tempTip = tipHeadingRay.heading();
				tempRoll = rollHeadingRay.heading();
			} else if(coordinateType == frameType.RELATIVE ) {
				tempTip = parArma.localAxes.getGlobalOf(tipHeadingRay.heading());
				tempRoll = parArma.localAxes.getGlobalOf(rollHeadingRay.heading());
			} else {
				System.out.println("WOAH WOAH WOAH");
			}

			tempX = tempRoll.cross(tempTip);
			tempRoll = tempX.cross(tempTip); 

			this.boneHeight = tipHeading.mag();
			this.parentArmature = parArma;

			generateAxes(parentArmature.localAxes.origin(), tempX, tempTip, tempRoll);
			this.localAxes.orthogonalize();
			localAxes.setParent(parentArmature.localAxes);	
			previousOrientation = localAxes.attachedCopy(true);

			majorRotationAxes = parentArmature.localAxes().getAbsoluteCopy(); 
			majorRotationAxes.setParent(parent.localAxes);
			
			parentArmature.addToBoneList(this);
			//this.updateSegmentedArmature();	

		} else {
			throw new NullParentForBoneException();
		}

	}
	
	public AbstractBone() {
		
	}

	public AbstractBone(AbstractArmature parArma, JSONObject boneJSON, AbstractAxes attachedAxes) {
		this.localAxes = attachedAxes;
		previousOrientation = localAxes.attachedCopy(true);
		this.boneHeight = boneJSON.getDouble("boneHeight");
		this.tag = boneJSON.getString("tag");

		
		this.parentArmature = parArma;
		parentArmature.addToBoneList(this);
	}

	public AbstractBone getParent() {
		return this.parent;
	}
	
	public void attachToParent(AbstractBone inputParent) {
		inputParent.addChildBone(this);
		this.parent = inputParent;
	}

	public void addChildBone(AbstractBone newChild) {
		if(!this.children.contains(newChild)) this.children.add(newChild);
		this.parentArmature.addToBoneList(newChild);
	}
	
	public void solveIKFromHere() {
		this.parentArmature.IKSolver(this);
	}

	public void ambitiouslySolveIKFromHere(double dampening, int iterations) {
		this.parentArmature.ambitiousIKSolver(this, dampening, iterations);
	}
	
	public void tranquillySolveIKFromHere(double dampening, int iterations) {
		this.parentArmature.tranquilIKSolver(this, dampening, iterations);
	}

	public void snapToConstraints() {
		if(constraints != null) {
			constraints.snapToLimits();  
		}
	}	
	
	public void setPin(DVector pin) {
		if(this.pin == null) {
			this.enablePin(pin);
		} else {
			this.pin.translateTo(pin);
		}
		//parentArmature.updateArmatureSegments();
	}
	

	public DVector getPinPosition() {
		if(pin == null) return null;
		else return pin.getLocation();
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
	 * axes relative to its parent bone. If the bone has no parent, this method 
	 * throws an exception.
	 * @throws NullParentForBoneException
	 */
	public double[] getXZYAngle() throws NullParentForBoneException {
		if(this.parent != null) {
			Rot boneOffset = new Rot(this.majorRotationAxes.x().heading(), 
					this.majorRotationAxes.y().heading(), 
					this.localAxes().x().heading(), 
					this.localAxes().y().heading());
			return boneOffset.rotation.getAngles(RotationOrder.XZY);
		} else {
			return null;
		}
	}
	
	/**
	 * @return An Apache Commons Rotation object representing the rotation of this bone relative to its 
	 * reference frame. The Rotation object is more versatile and robust than an array of angles. 
	 * And allows you to treat rotation in a wide variety of conventions.
	 */
	public Rotation getRotation() {
		return (new Rot(this.majorRotationAxes.x().heading(), this.majorRotationAxes.y().heading(), 
				this.localAxes().x().heading(), this.localAxes().y().heading())).rotation;
	}
	
	/**
	 * @return An Apache Commons Rotation object representing the rotation which transforms this 
	 * Bone from its previous orientation to its current orientation. 
	 */
	
	public Rotation getRotationFromPrevious() {
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
	public void rotateBy(Rotation rot) {
		this.previousOrientation.alignLocalsTo(localAxes);
		this.localAxes.rotateTo(rot);
		
		this.lastRotation = rot;
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
		previousOrientation.alignLocalsTo(localAxes);		
		
		Rot xRot = new Rot(majorRotationAxes.x().heading(), amt); 
		localAxes.rotateTo(xRot);

		lastRotation = xRot.rotation;
		
		if(obeyConstraints) this.snapToConstraints();
	}

	/**
	 * rotates the bone about the major Y axis of rotation 
	 * @param amt number of degrees to rotate by
	 * @param obeyConstrants  whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotAboutFrameY(double amt, boolean obeyConstraints) {
		previousOrientation.alignLocalsTo(localAxes);	
		
		Rot yRot = new Rot(majorRotationAxes.y().heading(), amt); 
		localAxes.rotateTo(yRot);

		lastRotation = yRot.rotation;
		
		if(obeyConstraints) this.snapToConstraints();
	}


	/**
	 * rotates the bone about the major Z axis of rotation 
	 * @param amt number of degrees to rotate by
	 * @param obeyConstrants  whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotAboutFrameZ(double amt, boolean obeyConstraints) {
		previousOrientation.alignLocalsTo(localAxes);	
		
		Rot zRot = new Rot(majorRotationAxes.z().heading(), amt); 
		localAxes.rotateTo(zRot);
		
		lastRotation = zRot.rotation;

		if(obeyConstraints) this.snapToConstraints();
	}

	/**
	 * rotates the bone about its own length. (Like a drill, or The Queen waving). 
	 * @param number of degrees to rotate by
	 * @param whether or not this functions should obey constraints when rotating the bone
	 */
	public void rotateAxially(double amt, boolean obeyConstraints) {
		localAxes.rotateAboutY(amt);

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
	 * and the limitCones and axial twist limits of Kusudama are expected relative to these Axes.<br><br>
	 * 
	 * Changing these axes is essentially the equivalent of rotating the joint on which this bone rests, 
	 * while keeping the bone in globally in place.<br><br>
	 * 
	 * You don't need to change this unless you start wishing you could change this.
	 */
	public void setFrameofRotation(AbstractAxes rotationFrameCoordinates) {
		majorRotationAxes.alignLocalsTo(rotationFrameCoordinates);
		if(parent != null) {
			majorRotationAxes.translateTo(parent.getTip());
		}
	}
	
	public AbstractAxes getMajorRotationAxes() {
		return this.majorRotationAxes;
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

	protected abstract AbstractIKPin createAndReturnPinAtOrigin(DVector origin);
	
	
	/**
	 * Enables an existing pin for this Bone. Or creates a pin for this bone at the bone's tip. 
	 */
	public void enablePin() {
		System.out.println("pinning");
		if(pin == null) pin = createAndReturnPinAtOrigin(this.getTip());
		pin.enable();
		System.out.println("clearing children");
		freeChildren.clear(); 
		System.out.println("adding children");
		for (AbstractBone child : children) {
			if (child.pin !=null && !child.pin.isEnabled()) {	    	  
				addFreeChild(child);
				System.out.println("childAdd");
			}
		}
		System.out.println("notifying ancestors");
		notifyAncestorsOfPin();
		System.out.println("updating segment armature");
		this.updateSegmentedArmature();	
		System.out.println("segment armature updated");
	}
	
	/**
	 * Creates a pin for this bone
	 * @param pinTo the position of the pin in the coordinateFrame of the parentArmature.
	 */

	public void enablePin(DVector pinTo) {
		if(pin == null) pin = createAndReturnPinAtOrigin(pinTo);
		else pin.translateTo(pinTo);
		pin.enable();
		freeChildren.clear(); 
		for (AbstractBone child : children) {
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
		if(pin == null || !pin.isEnabled) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Creates / enables a pin of there no pin is active, disables the pin if it is active.
	 */
	public void togglePin() {
		if(this.pin == null) this.enablePin();
		this.pin.toggle();
		updateSegmentedArmature();
	}


	public ArrayList<AbstractBone> returnChildrenWithPinnedDescendants() {
		ArrayList<AbstractBone> childrenWithPinned = new ArrayList<AbstractBone>();
		for(AbstractBone c : children) {
			if(c.hasPinnedDescendant()) 
				childrenWithPinned.add(c);
		}
		return childrenWithPinned;
	}
	
	
	public ArrayList<AbstractBone> getMostImmediatelyPinnedDescendants() {
		ArrayList<AbstractBone> mostImmediatePinnedDescendants = new ArrayList<AbstractBone>();
		this.addSelfIfPinned(mostImmediatePinnedDescendants);
		return mostImmediatePinnedDescendants;
	}

	public DVector pinnedTo() {
		return pin.getLocation();
	}

	public AbstractAxes getPinnedAxes() {
		return this.pin.getAxes();
	}

	public void addSelfIfPinned(ArrayList<AbstractBone> pinnedBones2) {
		if (this.isPinned()) {
			pinnedBones2.add(this);
		} else {
			for (AbstractBone child : children) {
				child.addSelfIfPinned(pinnedBones2);
			}
		}
	}

	public void notifyAncestorsOfPin() { 
		if (this.parent != null) {
			parent.addToEffectored(this);
		}
		parentArmature.updateArmatureSegments();
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
	public DVector getBase() {
		return localAxes.origin();
	}

	public DVector getTip() { 		
		return localAxes.y().getScaledTo(boneHeight);
	}

	public void setBoneHeight(double inBoneHeight) {
		this.boneHeight = inBoneHeight;
		for(AbstractBone child : this.children) {
			child.localAxes().translateTo(this.getTip());
			child.majorRotationAxes.translateTo(this.getTip());
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
			for(AbstractBone c : children) {
				if(c.hasPinnedDescendant()) {
					result = true;
					break;
				}
			}
			return result;
		}
		
	}

	public void addChild(AbstractBone bone) {		
		if(this.children.indexOf(bone) == -1) {
			children.add(bone);
		}
		parentArmature.updateArmatureSegments();
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
		for(AbstractBone b : children) {
			parentArmature.addToBoneList(b);
			b.addDescendantsToArmature();
		}
	}

	public class NullParentForBoneException extends NullPointerException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 24957056215695010L;
	}

}
