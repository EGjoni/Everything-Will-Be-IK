/*

Copyright (c) 2016 Eron Gjoni

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

package sceneGraph;


import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
/**
 * @author Eron Gjoni
 */
public abstract class AbstractAxes implements AxisDependancy {
public static final int NORMAL = 0, IGNORE = 1, FORWARD = 2;


public Ray lx, ly, lz; //Relative To Parent
public DVector lorigin; //Relative To Parent
public AbstractAxes parent = null;
public AbstractAxes globalCoords = null; 

private int slipType = 0;
public boolean dirty = true;

public ArrayList<AxisDependancy> dependentsRegistry = new ArrayList<AxisDependancy>(); 

public AbstractAxes(Ray inX, Ray inY, Ray inZ) {
	lx = new Ray(inX.p1, inX.p2); 
	ly = new Ray(inY.p1, inY.p2);
	lz = new Ray(inZ.p1, inZ.p2);
	lorigin = lx.origin();  
}

public AbstractAxes(Ray inX, Ray inY, Ray inZ, AbstractAxes parent) {
	if(parent != null) {
		lx = new Ray(parent.getLocalOf(inX.p1), parent.getLocalOf(inX.p2)); 
		ly = new Ray(parent.getLocalOf(inY.p1), parent.getLocalOf(inY.p2));
		lz = new Ray(parent.getLocalOf(inZ.p1), parent.getLocalOf(inZ.p2));
		lorigin = parent.getLocalOf(lx.origin());  
		this.parent = parent;
		this.parent.registerDependent(this); 
	} else {
		lx = inX.copy();
		ly = inY.copy();
		lz = inZ.copy();
		lorigin = lx.origin();
	}
	this.updateGlobal();
}


public AbstractAxes(DVector origin, DVector inX, DVector inY, DVector inZ) {
	lx = new Ray(origin, DVector.add(inX, origin));
	ly = new Ray(origin, DVector.add(inY, origin));
	lz = new Ray(origin, DVector.add(inZ, origin)); 
	this.lorigin = origin.copy();
}

public AbstractAxes(DVector origin, DVector inX, DVector inY, DVector inZ,  AbstractAxes parent) { //<>//
	lx = new Ray(parent.getLocalOf(origin), parent.getLocalOf(DVector.add(inX, origin)));
	ly = new Ray(parent.getLocalOf(origin), parent.getLocalOf(DVector.add(inY, origin)));
	lz = new Ray(parent.getLocalOf(origin), parent.getLocalOf(DVector.add(inZ, origin))); 
	this.lorigin = parent.getLocalOf(origin);
	this.parent = parent;
	this.parent.registerDependent(this); 
	this.updateGlobal();
}

public Ray x() {
	this.updateGlobal();  
	return this.globalCoords.lx;
}

public Ray y() {
	this.updateGlobal();  
	return this.globalCoords.ly;
}

public Ray z() {
	this.updateGlobal();  
	return this.globalCoords.lz;
}

public DVector origin() {
	this.updateGlobal();  
	return this.globalCoords.lorigin;
}
public void updateGlobal() {
	this.getAbsolute();  
	dirty = false;
}  

public AbstractAxes getAbsoluteCopy() {
	return this.getAbsolute().freeCopy();  
}

public AbstractAxes getAbsolute() {
	if (this.parent == null) { 
		globalCoords = this;
		return this;      
	} else if (this.needsUpdate()) { 
		globalCoords = this.relativeTo(parent.getAbsolute());
		return globalCoords;
	} else {
		return globalCoords;  
	}
}

/**
 * Sets the parentAxes for this axis globally.  
 * in other words, globalX, globalY, and globalZ remain unchanged, but lx, ly, and lz 
 * change.  
 **/

public void setParent(AbstractAxes par) {
	AbstractAxes parRelCop = par.getLocalOf(this.getAbsoluteCopy());
	this.slipTo(parRelCop);
	this.parent = par;
	this.parent.registerDependent(this); 
	this.markDirty();
	this.updateGlobal();
}


/**
 * Sets the parentAxes for this axis locally. 
 * in other words, lx,ly,lz remain unchanged, but globalX, globalY, and globalZ 
 * change.  
 **/
public void setRelativeToParent(AbstractAxes par) {
	this.parent = par;
	this.parent.registerDependent(this);
	markDirty();
	this.updateGlobal();
}

public boolean needsUpdate() {
	if(this.dirty) return true;  
	else return false;
}

/**
 * Given a vector in this axes local coordinates, returns the vector's position in global coordinates.
 * @param in
 * @return
 */
public DVector getGlobalOf(DVector in) {
	this.updateGlobal();
	return DVector.add(globalCoords.lorigin, DVector.add(
			DVector.add(DVector.mult(globalCoords.lx.heading(), in.x), 
					DVector.mult(globalCoords.ly.heading(), in.y)), 
			DVector.mult(globalCoords.lz.heading(), in.z))); 

}

public Ray getGlobalOf(Ray in) { 
	return new Ray(this.getGlobalOf(in.p1), this.getGlobalOf(in.p2));
}

public abstract AbstractAxes instantiate(Ray x, Ray y, Ray z);
public abstract AbstractAxes instantiate(DVector origin, DVector x, DVector y, DVector z);
public abstract AbstractAxes instantiate(Ray x, Ray y, Ray z, AbstractAxes par);


/**
 * returns an axis representing the global location of this axis if the input axis were its parent.
 * @param in
 * @return
 */
public AbstractAxes relativeTo(AbstractAxes in) { 
	return instantiate(new Ray (in.getGlobalOf(lx.p1), in.getGlobalOf(lx.p2)), new Ray (in.getGlobalOf(ly.p1), in.getGlobalOf(ly.p2)), new Ray (in.getGlobalOf(lz.p1), in.getGlobalOf(lz.p2)));         
}


public DVector getLocalOf(DVector in) {
	this.updateGlobal();

	DVector inTemp = DVector.sub(in, globalCoords.lorigin);
	double xScale = DVector.dot(inTemp, globalCoords.lx.heading());
	double yScale = DVector.dot(inTemp, globalCoords.ly.heading());
	double zScale = DVector.dot(inTemp, globalCoords.lz.heading());
	DVector result = new DVector(xScale, yScale, zScale);


	return result;
}

public Ray getLocalOf(Ray in) {
	return new Ray(this.getLocalOf(in.p1), this.getLocalOf(in.p2));  
}

public AbstractAxes getLocalOf(AbstractAxes input) {
	return instantiate(this.getLocalOf(input.x()), this.getLocalOf(input.y()), this.getLocalOf(input.z())); 
}

public AbstractAxes getRelativeCopy() {
	return instantiate(this.lx, this.ly, this.lz);
}


public void translateByLocal(DVector translate) {    
	this.updateGlobal();
	lx.translateBy(translate); 
	ly.translateBy(translate); 
	lz.translateBy(translate);
	lorigin.add(translate);
	this.markDirty();
	this.updateGlobal();

} 

public void translateByGlobal(DVector translate, boolean slip) {
	if(!slip) {
		translateByGlobal(translate);   
	} else {
		this.slipTo(this.getGlobalTranslatedBy(translate));  
	}

}

public void translateByGlobal(DVector translate) {

	if(this.parent != null ) {
		this.updateGlobal();
		AbstractAxes globalTemp = globalCoords.freeCopy();
		globalTemp.lx.translateBy(translate); 
		globalTemp.ly.translateBy(translate); 
		globalTemp.lz.translateBy(translate);
		globalTemp.lorigin.add(translate);
		AbstractAxes localResult = this.parent.getLocalOf(globalTemp);

		this.lx.p1 = localResult.lx.p1.copy();
		this.lx.p2 = localResult.lx.p2.copy();
		this.ly.p1 = localResult.ly.p1.copy();
		this.ly.p2 = localResult.ly.p2.copy();
		this.lz.p1 = localResult.lz.p1.copy();
		this.lz.p2 = localResult.lz.p2.copy();
		this.lorigin = localResult.lorigin.copy();

		this.markDirty();
		this.updateGlobal();
	} else {
		this.translateByLocal(translate);  

	}
}

public AbstractAxes getLocalTranslatedBy(DVector translate) {
	this.updateGlobal();
	return instantiate(DVector.add(this.lorigin, translate), lx.heading(), ly.heading(), lz.heading());
} 

public AbstractAxes getGlobalTranslatedBy(DVector translate) {
	this.updateGlobal();
	if(this.parent != null ) {      
		return instantiate(DVector.add(globalCoords.lorigin, translate), globalCoords.lx.heading(), globalCoords.ly.heading(), globalCoords.lz.heading());
	} else {
		return getLocalTranslatedBy(translate); 
	}
}

public void translateTo(DVector translate, boolean slip) {
	this.updateGlobal();
	if(slip) {
		AbstractAxes tempAbstractAxes = this.getAbsoluteCopy();
		tempAbstractAxes.translateTo(translate);
		this.slipTo(tempAbstractAxes);
	} else {
		this.translateTo(translate);  
	}
}

public void translateTo(DVector translate) {
	if(this.parent != null ) {
		this.updateGlobal();
		AbstractAxes globalTemp = globalCoords.freeCopy();
		globalTemp.lx.translateTo(translate); 
		globalTemp.ly.translateTo(translate); 
		globalTemp.lz.translateTo(translate);
		globalTemp.lorigin = translate.copy();

		AbstractAxes localResult = this.parent.getLocalOf(globalTemp);

		this.lx.p1 = localResult.lx.p1.copy();
		this.lx.p2 = localResult.lx.p2.copy();
		this.ly.p1 = localResult.ly.p1.copy();
		this.ly.p2 = localResult.ly.p2.copy();
		this.lz.p1 = localResult.lz.p1.copy();
		this.lz.p2 = localResult.lz.p2.copy();
		this.lorigin = localResult.lorigin.copy();

		this.markDirty();
		this.updateGlobal();
	} else {
		this.updateGlobal();
		this.lx.translateTo(translate); 
		this.ly.translateTo(translate); 
		this.lz.translateTo(translate);
		this.lorigin = translate.copy();
		this.markDirty();
		this.updateGlobal();
	}


}

public AbstractAxes getLocalTranslatedTo(DVector translate) {
	this.updateGlobal();
	return instantiate(translate, lx.heading(), ly.heading(), lz.heading()); 
}

public AbstractAxes getGlobalTranslatedTo(DVector translate) {
	if(this.parent != null ) {
		this.updateGlobal();
		return instantiate(translate, globalCoords.lx.heading(), globalCoords.ly.heading(), globalCoords.lz.heading());
	} else {
		return getLocalTranslatedTo(translate); 
	} 
}

public AbstractAxes freeCopy() {
	return instantiate(this.lx, this.ly, this.lz);  
}

public AbstractAxes attachedCopy(boolean slipAware) {
	AbstractAxes copy = instantiate(this.x(), this.y(), this.z(), this.parent);  
	if(!slipAware) copy.setSlipType(IGNORE);
	return copy;
}

public void setSlipType(int type) {
	if(this.parent != null) {
		if(type == IGNORE) {
			this.parent.dependentsRegistry.remove(this);
		} else if(type == NORMAL || type == NORMAL) {
			this.parent.registerDependent(this);
		} 
	}
	this.slipType = type;
}

public int getSlipType() {
	return this.slipType;
}

public void rotateAboutX(double angle, boolean slip) {
	if(slip) {
		AbstractAxes absCopy = this.getAbsoluteCopy();  
		absCopy.rotateAboutX(angle);
		this.slipTo(absCopy);
	} else { 
		this.rotateAboutX(angle);  
	}
}

public void rotateAboutY(double angle, boolean slip) {
	if(slip) {
		AbstractAxes absCopy = this.getAbsoluteCopy();  
		absCopy.rotateAboutY(angle);
		this.slipTo(absCopy);
	} else { 
		this.rotateAboutY(angle);  
	}
}

public void rotateAboutZ(double angle, boolean slip) {
	if(slip) {
		AbstractAxes absCopy = this.getAbsoluteCopy();  
		absCopy.rotateAboutZ(angle);
		this.slipTo(absCopy);
	} else { 
		this.rotateAboutZ(angle);  
	}
}

public void rotateAboutX(double angle) {
	this.updateGlobal();
	rayRotation(lz, lx, angle); 
	rayRotation(ly, lx, angle);
	orthogonalize();
	this.markDirty();
	this.updateGlobal();
}

public void rotateAboutY(double angle) {
	this.updateGlobal();
	rayRotation(lz, ly, angle); 
	rayRotation(lx, ly, angle);
	orthogonalize();
	this.markDirty();
	this.updateGlobal();
}

public void rotateAboutZ(double angle) {
	this.updateGlobal();
	rayRotation(lx, lz, angle); 
	rayRotation(ly, lz, angle);
	orthogonalize();
	this.markDirty();
	this.updateGlobal();
}


public void rotateAboutCustomLocal(DVector axis, double angle) {
	this.updateGlobal();
	Ray axisRay = new Ray(new DVector(0,0,0), axis);
	rayRotation(lx, axisRay, angle); 
	rayRotation(ly, axisRay, angle); 
	rayRotation(lz, axisRay, angle);
	orthogonalize();
	this.markDirty();
	this.updateGlobal();
}

public void rotateAboutCustomGlobal(DVector axis, double angle) {
	Ray axisRay = new Ray(new DVector(0,0,0), axis);
	this.rotateAboutCustomGlobal(axisRay, angle);
}

public void rotateAboutCustomLocal(Ray axis, double angle) {
	this.updateGlobal();
	rayRotation(lx, axis, angle); 
	rayRotation(ly, axis, angle); 
	rayRotation(lz, axis, angle);
	orthogonalize();
	this.markDirty();
	this.updateGlobal();
}

public void rotateAboutCustomGlobal(Ray axis, double angle) {
	if(this.parent != null) {
		this.updateGlobal(); 
		rayRotation(globalCoords.lx, axis, angle); 
		rayRotation(globalCoords.ly, axis, angle); 
		rayRotation(globalCoords.lz, axis, angle);
		globalCoords.orthogonalize();

		AbstractAxes tempLocal = this.parent.getLocalOf(globalCoords);
		this.lorigin = tempLocal.lorigin;
		this.lx.p1 = tempLocal.lx.p1;
		this.lx.p2 = tempLocal.lx.p2;
		this.ly.p1 = tempLocal.ly.p1; 
		this.ly.p2 = tempLocal.ly.p2; 
		this.lz.p1 = tempLocal.lz.p1;
		this.lz.p2 = tempLocal.lz.p2;
		this.markDirty();
		this.updateGlobal();
	} {
		this.updateGlobal();
		rayRotation(this.lx, axis, angle); 
		rayRotation(this.ly, axis, angle); 
		rayRotation(this.lz, axis, angle);
		this.orthogonalize();
		this.markDirty();
		this.updateGlobal();
	}
}


/**
 * rotate about this axes parent frame
 * (first translates to the frames origin,
 * then rotates, then translates back to the 
 * frame's origin)
 * @param rotation
 */
public void locallyRotateTo(Rot rotation) {
	this.lx.p2 = rotation.applyTo(lx.heading());
	this.ly.p2 = rotation.applyTo(ly.heading());
	this.lz.p2 = rotation.applyTo(lz.heading());
	lx.heading(lx.p2);
	ly.heading(ly.p2); 
	lz.heading(lz.p2);
	this.markDirty();
	this.updateGlobal();
}


public void rotateTo(Rot rotation, boolean slip) {
	this.rotateTo(rotation.rotation, slip);
}

public void rotateTo(Rotation rotation, boolean slip) {
	if(slip) {
		this.updateGlobal();
		AbstractAxes absCopy = this.getAbsoluteCopy();  
		absCopy.rotateTo(rotation);
		this.slipTo(absCopy);
	} else { 
		this.updateGlobal();
		this.rotateTo(rotation);  
	}
}

public void rotateTo(Rot rotation,  DVector aboutOrigin, boolean slip) {
	if(slip) {
		this.updateGlobal();
		AbstractAxes absCopy = this.getAbsoluteCopy();  
		absCopy.rotateTo(rotation, aboutOrigin);
		this.slipTo(absCopy);
	} else { 
		this.updateGlobal();
		this.rotateTo(rotation, aboutOrigin);  
	}
}


public void rotateTo(Rotation rotation) {
	this.updateGlobal();
	Vector3D xHeading = x().heading().toVector3D(); 
	Vector3D yHeading = y().heading().toVector3D(); 
	Vector3D zHeading = z().heading().toVector3D(); 

	xHeading = rotation.applyTo(xHeading); 
	yHeading = rotation.applyTo(yHeading);
	zHeading = rotation.applyTo(zHeading);


	AbstractAxes rotated = instantiate(this.origin(), new DVector(xHeading), new DVector(yHeading), new DVector(zHeading));
	if(this.parent != null) rotated = parent.getLocalOf(rotated);
	alignLocalsTo(rotated);	
}

public void rotateTo(Rot rotation) {
	this.updateGlobal();
	DVector xHeading = x().heading(); 
	DVector yHeading = y().heading(); 
	DVector zHeading = z().heading(); 

	xHeading = rotation.applyTo(xHeading); 
	yHeading = rotation.applyTo(yHeading);
	zHeading = rotation.applyTo(zHeading);


	AbstractAxes rotated = instantiate(this.origin(), xHeading, yHeading, zHeading);
	if(this.parent != null) rotated = parent.getLocalOf(rotated);
	alignLocalsTo(rotated);	
}

/**
 * sets these axes to have the same orientation and location relative to their parent
 * axes as the input's axes do to the input's parent axes.
 * 
 * This function normalizes and orthogonalizes the axes.
 * 
 * @param targetAxes the Axes to make this Axis identical to
 */
public void alignLocalsTo(AbstractAxes targetAxes ) {
	this.lx.p1 = targetAxes.lx.p1;
	this.lx.p2 = targetAxes.lx.p2; 
	this.ly.p1 = targetAxes.ly.p1; 
	this.ly.p2 = targetAxes.ly.p2;
	this.lz.p1 = targetAxes.lz.p1; 
	this.lz.p2 = targetAxes.lz.p2;
	this.lorigin = this.lx.p1;
	this.orthogonalize();
	this.markDirty();
	this.updateGlobal();
}

/**
 * just sets the axes to (1,0,0), (0,1,0), and (0,0,1)
 */
public void alignToParent() {
	this.lx.p1.x = 0;
	this.lx.p1.y = 0;
	this.lx.p1.z = 0;
	
	this.lx.p2.x = 1; 
	this.lx.p2.y = 0;
	this.lx.p2.z = 0;
	
	this.ly.p2.x = 0; 
	this.ly.p2.y = 0;
	this.ly.p2.z = 0;
	
	this.ly.p2.x = 0; 
	this.ly.p2.y = 1;
	this.ly.p2.z = 0;
	
	this.lz.p2.x = 0; 
	this.lz.p2.y = 0;
	this.lz.p2.z = 0;
	
	this.lz.p2.x = 0; 
	this.lz.p2.y = 0;
	this.lz.p2.z = 1;
		
	this.lorigin = this.lx.p1;
	this.orthogonalize();
	this.markDirty();
	this.updateGlobal();
}

public void rotateTo(Rot rotation, DVector aboutOrigin) {
	this.updateGlobal();
	DVector thisOrigin = DVector.sub(globalCoords.lx.p1, aboutOrigin); 

	thisOrigin = rotation.applyTo(thisOrigin);
	thisOrigin.add(aboutOrigin); 



	if(this.parent == null) {

		this.rotateTo(rotation); 
		this.translateTo(thisOrigin);
		this.orthogonalize();

	} else {
		globalCoords.rotateTo(rotation); 
		globalCoords.translateTo(thisOrigin);
		globalCoords.orthogonalize();
		AbstractAxes tempLocal = this.parent.getLocalOf(globalCoords); 
		this.lorigin = tempLocal.lorigin;
		this.lx.p1 = tempLocal.lx.p1;
		this.lx.p2 = tempLocal.lx.p2;
		this.ly.p1 = tempLocal.ly.p1; 
		this.ly.p2 = tempLocal.ly.p2; 
		this.lz.p1 = tempLocal.lz.p1;
		this.lz.p2 = tempLocal.lz.p2;

		this.markDirty();
		this.updateGlobal();
	}
}

public void orthogonalize() {
	this.updateGlobal();
	DVector xHeading = lx.heading(); 
	DVector yHeading = ly.heading(); 

	DVector zHeading = xHeading.cross(yHeading); 
	if(zHeading.dot(lz.heading()) < 0) zHeading.mult(-1); 

	xHeading = yHeading.cross(zHeading); 
	if(xHeading.dot(lx.heading()) < 0) xHeading.mult(-1); 

	lx.heading(xHeading);
	lz.heading(zHeading); 
	lx.mag(1);
	ly.mag(1);
	lz.mag(1);

	this.markDirty();
	this.updateGlobal();
}


public void registerDependent(AxisDependancy newDependent) {
	if(dependentsRegistry.indexOf(newDependent) == -1){ 
		dependentsRegistry.add(newDependent);
	}
}

/**
 * unregisters this AbstractAxes from its current parent and 
 * registers it to a new parent without changing its global position or orientation 
 * when doing so.
 * @param newParent
 */

public void transferToParent(AbstractAxes newParent) {
	this.emancipate();
	this.setParent(newParent);
}

/*
 * unregisters this AbstractAxes from its parent, 
 * but keeps its global position the same.
 */
public void emancipate() {
	if(this.parent != null) {
		this.updateGlobal();
		this.lx = globalCoords.lx.copy();
		this.ly = globalCoords.ly.copy();
		this.lz = globalCoords.lz.copy();
		this.lorigin = globalCoords.origin();
		this.parent.disown(this);
	}
}

public void disown(AbstractAxes child) {
	if(dependentsRegistry.remove(child));
}

public void axisSlipWarning(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes actualAxis, ArrayList<Object>dontWarn) {
	this.updateGlobal();
	if(this.slipType == NORMAL ) {
		if(this.parent != null) {
			AbstractAxes globalVals = this.relativeTo(globalPriorToSlipping);
			globalVals = globalPriorToSlipping.getLocalOf(globalVals); 
			this.lx.p1 = globalVals.lx.p1;
			this.lx.p2 = globalVals.lx.p2; 
			this.ly.p1 = globalVals.ly.p1; 
			this.ly.p2 = globalVals.ly.p2;
			this.lz.p1 = globalVals.lz.p1; 
			this.lz.p2 = globalVals.lz.p2;
			this.lorigin = this.lx.p1;
		}
	} else if(this.slipType == FORWARD) {
			AbstractAxes globalAfterVals = this.relativeTo(globalAfterSlipping);
			this.notifyDependentsOfSlip(globalAfterVals, dontWarn);
	}
}


public void axisSlipWarning(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes actualAxis) {
	
}

public void axisSlipCompletionNotice(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes thisAxis) {

}

public void slipTo(AbstractAxes newAxisGlobal) {
	this.updateGlobal();
	AbstractAxes originalGlobal = this.getAbsoluteCopy();
	notifyDependentsOfSlip(newAxisGlobal); 
	AbstractAxes newVals = newAxisGlobal.freeCopy();

	if(this.parent != null) {
		newVals = parent.getLocalOf(newVals);
	}
	this.lx.p1 = newVals.lx.p1;
	this.lx.p2 = newVals.lx.p2; 
	this.ly.p1 = newVals.ly.p1; 
	this.ly.p2 = newVals.ly.p2;
	this.lz.p1 = newVals.lz.p1; 
	this.lz.p2 = newVals.lz.p2;
	this.lorigin = this.lx.p1;
	this.dirty = true;
	this.updateGlobal();
	//this.globalCoords = newAxisGlobal.getAbsoluteCopy();
	/*System.out.println("#######");
	System.out.println("Notifying dependents");
	System.out.println("originalZ " + originalGlobal.z().heading());
	System.out.println("newZ " + newAxisGlobal.z().heading());
	System.out.println("confirmation " + this.getAbsoluteCopy().z().heading());*/
	
	
	notifyDependentsOfSlipCompletion(originalGlobal);
}

public void slipTo(AbstractAxes newAxisGlobal, ArrayList<Object> dontWarn) {
	this.updateGlobal();
	AbstractAxes originalGlobal = this.getAbsoluteCopy();
	notifyDependentsOfSlip(newAxisGlobal, dontWarn); 
	AbstractAxes newVals = newAxisGlobal.freeCopy();

	if(this.parent != null) {
		newVals = parent.getLocalOf(newVals);
	}
	this.lx.p1 = newVals.lx.p1;
	this.lx.p2 = newVals.lx.p2; 
	this.ly.p1 = newVals.ly.p1; 
	this.ly.p2 = newVals.ly.p2;
	this.lz.p1 = newVals.lz.p1; 
	this.lz.p2 = newVals.lz.p2;
	this.lorigin = this.lx.p1;
	this.dirty = true;
	this.updateGlobal();
	//this.globalCoords = newAxisGlobal.getAbsoluteCopy();
	/*System.out.println("#######");
	System.out.println("Notifying dependents");
	System.out.println("originalZ " + originalGlobal.z().heading());
	System.out.println("newZ " + newAxisGlobal.z().heading());
	System.out.println("confirmation " + this.getAbsoluteCopy().z().heading());*/
	notifyDependentsOfSlipCompletion(originalGlobal, dontWarn);
}

public void notifyDependentsOfSlip(AbstractAxes newAxisGlobal, ArrayList<Object> dontWarn) {
	for(int i = 0; i<dependentsRegistry.size(); i++) {
		if(!dontWarn.contains(dependentsRegistry.get(i))) {
			AxisDependancy dependant = dependentsRegistry.get(i);
			
			//First we check if the dependent extends AbstractAxes
			//so we know whether or not to pass the dontWarn list
			if(this.getClass().isAssignableFrom(dependant.getClass())) { 
				((AbstractAxes)dependant).axisSlipWarning(this.getAbsoluteCopy(), newAxisGlobal, this, dontWarn);
			} else {
				dependant.axisSlipWarning(this.getAbsoluteCopy(), newAxisGlobal, this);
			}
		} else {
			System.out.println("skipping: " + dependentsRegistry.get(i));
		}
	}
}

public void notifyDependentsOfSlipCompletion(AbstractAxes globalAxisPriorToSlipping, ArrayList<Object> dontWarn) {
	for(int i = 0; i<dependentsRegistry.size(); i++) {
		if(!dontWarn.contains(dependentsRegistry.get(i)))
			dependentsRegistry.get(i).axisSlipCompletionNotice(globalAxisPriorToSlipping, this.getAbsoluteCopy(), this);
		else 
			System.out.println("skipping: " + dependentsRegistry.get(i));
	}
}


public void notifyDependentsOfSlip(AbstractAxes newAxisGlobal) {
	for(int i = 0; i<dependentsRegistry.size(); i++) {
		dependentsRegistry.get(i).axisSlipWarning(this.getAbsoluteCopy(), newAxisGlobal, this);
	}
}

public void notifyDependentsOfSlipCompletion(AbstractAxes globalAxisPriorToSlipping) {
	for(int i = 0; i<dependentsRegistry.size(); i++) {//AxisDependancy dependent : dependentsRegistry) {
		dependentsRegistry.get(i).axisSlipCompletionNotice(globalAxisPriorToSlipping, this.getAbsoluteCopy(), this);
	}
}

public static DVector axisRotation(DVector point, DVector axis, double angle) {
	Rot rotation = new Rot(axis, angle);
	point = rotation.applyTo(point);
	return point;
}

public static DVector[] axisRotation(DVector[] points, DVector axis, double angle) {

	Rot rotation = new Rot(axis, angle);
	for(DVector point : points) {
		point = rotation.applyTo(point);
	}
	return points;
}



public static void rayRotation(DVector point, Ray r, double angle) {

	DVector axis = DVector.sub(r.p2, r.p1);
	point.sub(r.p1); 
	point = axisRotation(point, axis, angle);
	point.add(r.p1);

}

public static void rayRotation(Ray point, Ray axis, double angle) {
	DVector result = point.heading().copy();   
	result = axisRotation(result, axis.heading(), angle); 
	point.heading(result);

}

public static void rayRotation(DVector[] points, Ray r, double angle) {

	DVector axis = DVector.sub(r.p2, r.p1);
	for(DVector point : points) {
		point.sub(r.p1); 
	}
	points = axisRotation(points, axis, angle);
	for(DVector point : points) {
		point.add(r.p1); 
	}

}

public void markDirty() {
	this.dirty = true;
	this.markDependentsDirty();

}

public void markDependentsDirty() {
	for(AxisDependancy ad : dependentsRegistry) {
		ad.markDirty();
	}
}
}
