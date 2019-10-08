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

package sceneGraph.math.floatV;


import java.util.ArrayList;

import IK.floatIK.G;
import data.JSONObject;
import data.LoadManager;
import data.SaveManager;
import data.Saveable;
import sceneGraph.math.floatV.AbstractAxes;
import sceneGraph.math.floatV.Rot;
import sceneGraph.math.floatV.SGVec_3f;

/**
 * @author Eron Gjoni
 */
public abstract class AbstractAxes implements AxisDependancy, Saveable {
	public static final int NORMAL = 0, IGNORE = 1, FORWARD = 2;
	public static final int RIGHT = 1, LEFT = -1; 
	public static final int X =0, Y=1, Z=2; 




	/**
	 * Each Axes needs to deal with an implicit stack of 
	 * transforms the result from its sequence of ancestors. 
	 * 
	 * However, some axes need to remain orthonormal, while others 
	 * need to obey their ancestor's non-orthonormal transformations. 
	 * 
	 * This creates all sort of ambiguities, and so to deal with them 
	 * each axes object keeps track of its shape/position 
	 * relative to its parent axes as if all ancestors had been orthonormal
	 * as well as its shape/position relative to its parent axes as if 
	 * all ancestors (including the parent) had not necessarily been orthonormal.  
	 * 
	 * the version that gets returned is the version that corresponds to whatever
	 * orthonormality constraint the user has set on the axes. 
	 * 
	 * in terms of computation -- getting the global coordinates of a non-orthonormal basis 
	 * is accomplished by getting its global coordinates relative to its non-orthonormal sequence of
	 * ancestors. 
	 * 
	 * getting the global coordinates of an orthonormal basis is accomplished by getting is global 
	 * coordinates relative to its orthonormal ancestors -- and then translating the basis so that 
	 * its origin aligns with the origin of its non-orthonormal doppelganger. 
	 * 
	 * This scheme is somewhat arbitrary, however, it has a few desirable properties. 
	 * 
	 * 1) There is no ambiguity as to how to orthonormalize on a non-orthonormal basis. There simply
	 * always exists an orthonormal basis, defined relative to another orthonormal basis up to the orthonormal root.
	 * 
	 * 2) Orthonormal bases will retain their position relative to non-orthonomal ancestors so as to minimize 
	 * discrepancy between the orthonormal basis and its non-orthonormal doppelganger. This should appear 
	 * to keep sibling nodes in nice relative positions to one another as ancestor nodes scale, regardless 
	 * of whether all siblings are flagged as orthonormal or not. 
	 */


	public boolean debug = false;

	public Basis localMBasis; 
	public Basis globalMBasis;
	protected AbstractAxes parent = null;

	private int slipType = 0;
	public boolean dirty = true;
	public boolean scaleDirty = false;
	//public boolean forceOrthoNormality = true; 
	public boolean forceOrthoNormality = true;
	private int globalChirality = RIGHT;
	private int localChirality = RIGHT;

	public ArrayList<AxisDependancy> dependentsRegistry = new ArrayList<AxisDependancy>(); 

	protected SGVec_3f workingVector; 

	boolean areGlobal = true;

	private sgRayf xTemp = new sgRayf(new SGVec_3f(), new SGVec_3f(1,1,1)); 
	private sgRayf yTemp = new sgRayf(new SGVec_3f(), new SGVec_3f(1,1,1)); 
	private sgRayf zTemp = new sgRayf(new SGVec_3f(), new SGVec_3f(1,1,1));
	private int flipFlag = -1; //value of -1 means the bases do not need to flip. values of 0, 1, or 2 mean the bases 
	//should flip along their X, Y, or Z axes respectively.  

	public void updateGlobal() {
		if(this.dirty || this.scaleDirty) {

			if(this.areGlobal) {
				globalMBasis.adoptValues(this.localMBasis);
			} else {
				//parent.markDirty();
				parent.updateGlobal();
				//if(this.debug) {
					//System.out.println("====== " + this + " =========");
					//System.out.println("LOCAL rotation prior:  \n" + localMBasis.rotation);
					//System.out.println("Global Rotation prior: \n" + globalMBasis.rotation);
					//}
				parent.globalMBasis.setToGlobalOf(this.localMBasis, this.globalMBasis);
				/*if(this.debug) {	
					System.out.println("Global Rotation post: \n" + globalMBasis.rotation);
				}*/

				this.globalChirality = this.globalMBasis.chirality;
				this.localChirality = this.localMBasis.chirality;
			}
		}
		globalChirality = globalMBasis.chirality;
		localChirality = localMBasis.chirality;
		dirty = false;
	} 
	
	public void createTempVars(SGVec_3f type) {
		workingVector =  type.copy(); 
		tempOrigin =  type.copy(); 
	}


	/**
	 * 
	 * @param origin the center of this axes basis. The basis vector parameters will be automatically ADDED to the origin in order to create this basis vector.
	 * @param inX the direction of the X basis vector in global coordinates, given as an offset from this base's origin in global coordinates.   
	 * @param inY the direction of the Y basis vector in global coordinates, given as an offset from this base's origin in global coordinates.
	 * @param inZ the direction of the Z basis vector in global coordinates, given as an offset from this base's origin in global coordinates.
	 * @param forceOrthoNormality
	 */
	public AbstractAxes(SGVec_3f origin, SGVec_3f inX, SGVec_3f inY, SGVec_3f inZ, boolean forceOrthoNormality, AbstractAxes parent) {

		this.forceOrthoNormality = forceOrthoNormality; 
		createTempVars(origin);

		areGlobal = true;		
		sgRayf xRay = new sgRayf(origin, origin.addCopy(inX));
		sgRayf yRay = new sgRayf(origin, origin.addCopy(inY));
		sgRayf zRay = new sgRayf(origin, origin.addCopy(inZ));

		localMBasis = new Basis(xRay, yRay, zRay);
		globalMBasis = new Basis(xRay, yRay, zRay);
		//globalNormalizedBasis = globalMBasis.copy();
		

		if(parent != null) {
			this.setParent(parent);
		} 	else {
			this.areGlobal = true;
		}
		this.updateGlobal();
		//this.updateChiralities();
	}

	public AbstractAxes getParentAxes() {
		return this.parent;
	}

	/** 
	 * @param val if set to false, axes will not be reorthonormalized on update. If set to true, axes will 
	 * be reorthogonalized. By default, this is set to true;
	 */
	public void setOrthoNormalityConstraint(boolean val) {
		forceOrthoNormality = val;
		this.markDirty();
	}

	public sgRayf lx_() {
		return this.localMBasis.getXRay();
	}

	public sgRayf ly_() {
		return this.localMBasis.getYRay();
	}

	public sgRayf lz_() {
		return this.localMBasis.getZRay();
	}

	public sgRayf lx_raw_() {
		return this.localMBasis.getXRay();
	}

	public sgRayf ly_raw_() {
		return this.localMBasis.getYRay();
	}

	public sgRayf lz_raw_() {
		return this.localMBasis.getZRay();
	}

	public sgRayf lx_norm_() {
		return this.localMBasis.getXRay();
	}

	public sgRayf ly_norm_() {
		return this.localMBasis.getYRay();
	}

	public sgRayf lz_norm_() {
		return this.localMBasis.getZRay();
	}

	/**
	 * @return a vector representing this frame's orientational X basis vector. Guaranteed to be Right-Handed and orthonormal. 
	 */

	public SGVec_3f orientation_X_() {
		this.updateGlobal();
		return  this.globalMBasis.getRotationalXHead();
	}

	/**
	 * @return a vector representing this frame's orientational Y basis vector. Guaranteed to be Right-Handed and orthonormal. 
	 */

	public SGVec_3f   orientation_Y_() {
		this.updateGlobal();
		return  this.globalMBasis.getRotationalYHead();
	}

	/**
	 * @return a vector representing this frame's orientational Z basis vector. Guaranteed to be Right-Handed and orthonormal. 
	 */

	public SGVec_3f   orientation_Z_() {
		this.updateGlobal();
		return  this.globalMBasis.getRotationalZHead();
	}

	/**
	 * @return a vector representing this frame's orthonormal X basis vector. Guaranteed to be orthonormal but not necessarily right-handed. 
	 */

	public SGVec_3f   orthonormal_X_() {
		this.updateGlobal();
		return  this.globalMBasis.getOrthonormalXHead();
	}

	/**
	 * @return a vector representing this frame's orthonormal Y basis vector. Guaranteed to be orthonormal but not necessarily right-handed. 
	 */
	public SGVec_3f   orthonormal_Y_() {
		this.updateGlobal();
		return  this.globalMBasis.getOrthonormalYHead();
	}


	/**
	 * sets the value of this AbstractAxes relative to an interpolated value between the start and end axes 
	 * relative to their parent. An amount of 0 makes it equivalent to the start axes, and amount of 1 
	 * makes it equivalent to the end axes. 
	 * 
	 * the rotational components of this axis are slerped, while its shear/scale components are lerped.
	 * 
	 * @param start
	 * @param end
	 * @param amount
	 */
	public void interpolateLocally(AbstractAxes start, AbstractAxes end, float amount) {
		start.updateGlobal(); end.updateGlobal(); this.updateGlobal();
		this.localMBasis.rotation = new Rot(amount, start.localMBasis.rotation, end.localMBasis.rotation);
		Matrix4f localShear = this.localMBasis.getShearScaleMatrix();
		Matrix4f startLocalShear = start.localMBasis.getShearScaleMatrix();
		Matrix4f endLocalShear = end.localMBasis.getShearScaleMatrix();

		localShear.val[Matrix4f.M00] = MathUtils.lerp(startLocalShear.val[Matrix4f.M00], endLocalShear.val[Matrix4f.M00], amount);
		localShear.val[Matrix4f.M10] = MathUtils.lerp(startLocalShear.val[Matrix4f.M10], endLocalShear.val[Matrix4f.M10], amount);
		localShear.val[Matrix4f.M20] = MathUtils.lerp(startLocalShear.val[Matrix4f.M20], endLocalShear.val[Matrix4f.M20], amount);

		localShear.val[Matrix4f.M01] = MathUtils.lerp(startLocalShear.val[Matrix4f.M01], endLocalShear.val[Matrix4f.M01], amount);
		localShear.val[Matrix4f.M11] = MathUtils.lerp(startLocalShear.val[Matrix4f.M11], endLocalShear.val[Matrix4f.M11], amount);
		localShear.val[Matrix4f.M21] = MathUtils.lerp(startLocalShear.val[Matrix4f.M21], endLocalShear.val[Matrix4f.M21], amount);

		localShear.val[Matrix4f.M02] = MathUtils.lerp(startLocalShear.val[Matrix4f.M02], endLocalShear.val[Matrix4f.M02], amount);
		localShear.val[Matrix4f.M12] = MathUtils.lerp(startLocalShear.val[Matrix4f.M12], endLocalShear.val[Matrix4f.M12], amount);
		localShear.val[Matrix4f.M22] = MathUtils.lerp(startLocalShear.val[Matrix4f.M22], endLocalShear.val[Matrix4f.M22], amount);

		this.localMBasis.translate = SGVec_3f.lerp(start.localMBasis.translate, end.localMBasis.translate, amount);
		this.localMBasis.refreshMatrices();
		this.markDirty();
	}


	/**
	 * sets the global values of this AbstractAxes relative to an interpolated value between the start and end axes 
	 * in global space. An amount of 0 makes it equivalent to the start axes, and amount of 1 
	 * makes it equivalent to the end axes. 
	 * 
	 * the rotational components of this axis are slerped, while its shear/scale components are lerped.
	 * 
	 * @param start
	 * @param end
	 * @param amount
	 */
	public void interpolateGlobally(AbstractAxes start, AbstractAxes end, float amount) {
		start.updateGlobal(); end.updateGlobal(); this.updateGlobal();
		this.globalMBasis.rotation = new Rot(amount, start.globalMBasis.rotation, end.globalMBasis.rotation);
		Matrix4f globalShear = this.globalMBasis.getShearScaleMatrix();
		Matrix4f startGlobalShear = start.globalMBasis.getShearScaleMatrix();
		Matrix4f endGlobalShear = end.globalMBasis.getShearScaleMatrix();

		globalShear.val[Matrix4f.M00] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M00], endGlobalShear.val[Matrix4f.M00], amount);
		globalShear.val[Matrix4f.M10] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M10], endGlobalShear.val[Matrix4f.M10], amount);
		globalShear.val[Matrix4f.M20] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M20], endGlobalShear.val[Matrix4f.M20], amount);

		globalShear.val[Matrix4f.M01] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M01], endGlobalShear.val[Matrix4f.M01], amount);
		globalShear.val[Matrix4f.M11] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M11], endGlobalShear.val[Matrix4f.M11], amount);
		globalShear.val[Matrix4f.M21] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M21], endGlobalShear.val[Matrix4f.M21], amount);

		globalShear.val[Matrix4f.M02] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M02], endGlobalShear.val[Matrix4f.M02], amount);
		globalShear.val[Matrix4f.M12] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M12], endGlobalShear.val[Matrix4f.M12], amount);
		globalShear.val[Matrix4f.M22] = MathUtils.lerp(startGlobalShear.val[Matrix4f.M22], endGlobalShear.val[Matrix4f.M22], amount);

		this.globalMBasis.translate = SGVec_3f.lerp(start.globalMBasis.translate, end.globalMBasis.translate, amount);
		this.globalMBasis.refreshMatrices();

		this.parent.globalMBasis.setToLocalOf(this.globalMBasis, this.localMBasis);
		this.markDirty();
	}

	/**
	 * 
	 * @param xHeading new global xHeading
	 * @param yHeading new global yHeading
	 * @param zHeading new gloabl zHeading
	 * @param flipOn axis to ignore on rotation adjustment if chirality changes. 0 = x, 1= y, 2 =z;
	 */
	public void setHeadings(SGVec_3f xHeading, SGVec_3f yHeading, SGVec_3f zHeading, int autoFlip) {
		this.markDirty();
		this.updateGlobal();
		SGVec_3f localX = new SGVec_3f(); SGVec_3f localY = new SGVec_3f(); SGVec_3f localZ = new SGVec_3f(0,0,0);
		SGVec_3f tempX = new SGVec_3f(xHeading); 
		SGVec_3f tempY = new SGVec_3f(yHeading); 
		SGVec_3f tempZ = new SGVec_3f(zHeading); 
		if(this.parent != null) {
			this.parent.globalMBasis.setToLocalOf(tempX.add(this.parent.globalMBasis.translate), localX); 
			this.parent.globalMBasis.setToLocalOf(tempY.add(this.parent.globalMBasis.translate), localY);
			this.parent.globalMBasis.setToLocalOf(tempZ.add(this.parent.globalMBasis.translate), localZ);
		}
		if(autoFlip >= 0) {
			this.flipFlag = autoFlip;

			if(autoFlip == 0) {

				Rot newRot = new Rot(this.parent.globalMBasis.getOrthonormalYHead(), this.parent.globalMBasis.getOrthonormalZHead(), yHeading, zHeading);//new Rot(this.globalMBasis.yBase, this.globalMBasis.zBase, localY, localZ);/
				this.localMBasis.rotation = this.parent.globalMBasis.getLocalOfRotation(newRot);//this.parent.globalMBasis.rotation.applyInverseTo(newRot.applyTo(this.parent.globalMBasis.rotation));
			} else if( autoFlip == 1) {
				Rot newRot = new Rot(this.parent.globalMBasis.getOrthonormalXHead(), this.parent.globalMBasis.getOrthonormalZHead(), xHeading, zHeading);
				this.localMBasis.rotation = this.parent.globalMBasis.getLocalOfRotation(newRot);
			} else{// if(autoFlip == 2){
				Rot newRot = new Rot(this.parent.globalMBasis.getOrthonormalXHead(), this.parent.globalMBasis.getOrthonormalYHead(), xHeading, yHeading);
				this.localMBasis.rotation = this.parent.globalMBasis.getLocalOfRotation(newRot);
			}

		}

		this.localMBasis.setXHeading(localX, false);
		this.localMBasis.setYHeading(localY, false);
		this.localMBasis.setZHeading(localZ, true);
		this.markDirty();
		this.updateGlobal();	
	}


	/**
	 *TODO: implement this.
	 * used by neighborhoodRotateBy and neighborhoodRotateTo. 
	 * @param rotationNeighborhood a desired orientation to treat as the origin
	 */
	public void setLocalRotationNeighborhood(Rot rotationNeighborhood) {

	}


	/**
	 *TODO: Implement this.
	 * like rotateBy, but rotates through the path which is closest 
	 * to the neighborhood. This is useful for things like armatures. 
	 * Where most rigs have a relaxed and natural pose, rotations near 
	 * which are less likely to represent hyperextions of any given joint. 	  
	 */
	public void neighborhoodRotateBy() {

	}

	public void debugCall() {};

	public SGVec_3f   orthonormal_Z_() {
		this.updateGlobal();
		return  this.globalMBasis.getOrthonormalZHead();
	}

	public sgRayf x_() {
		this.updateGlobal();
		if(this.forceOrthoNormality) {
			return x_norm_();
		} else 
			return this.globalMBasis.getXRay();
	}


	public sgRayf y_() {
		this.updateGlobal();  	
		if(this.forceOrthoNormality) {
			return y_norm_();
		} else 
			return this.globalMBasis.getYRay();
	}

	public sgRayf z_() {
		this.updateGlobal();  
		if(this.forceOrthoNormality) {
			return z_norm_();
		} else 
			return this.globalMBasis.getZRay();
	}

	public sgRayf x_norm_() {
		this.updateGlobal();  
		xTemp.p1().set(this.globalMBasis.getOrigin()); xTemp.heading(this.globalMBasis.getOrthonormalXHead());
		return xTemp;
	}

	public sgRayf y_norm_() {
		this.updateGlobal();  
		yTemp.p1().set(this.globalMBasis.getOrigin()); yTemp.heading(this.globalMBasis.getOrthonormalYHead());
		return yTemp;
	}

	public sgRayf z_norm_() {
		this.updateGlobal();  
		zTemp.p1().set(this.globalMBasis.getOrigin()); zTemp.heading(this.globalMBasis.getOrthonormalZHead());
		return zTemp;
	}

	public sgRayf x_raw_() {
		this.updateGlobal();  
		return this.globalMBasis.getXRay();
	}

	public sgRayf y_raw_() {
		this.updateGlobal();  
		return this.globalMBasis.getYRay();
	}

	public sgRayf z_raw_() {
		this.updateGlobal();  
		return this.globalMBasis.getZRay();
	}


	SGVec_3f tempOrigin;
	public SGVec_3f origin_() {
		this.updateGlobal();  
		tempOrigin.set(this.globalMBasis.getOrigin());
		return tempOrigin;
	}


	public void scaleXBy(float scale) {
		this.localMBasis.scaleXBy(scale);
		this.markDirty();
		this.updateGlobal();
	}

	public void scaleYBy(float scale) {
		this.localMBasis.scaleYBy(scale);
		this.markDirty();
		this.updateGlobal();
		//}
	}	

	public void scaleZBy(float scale) {
		this.updateGlobal();
		this.localMBasis.scaleZBy(scale);
		this.markDirty();
		this.updateGlobal();
	}


	public void scaleXTo(float scale) {		
		if(!this.forceOrthoNormality) {
			this.updateGlobal();
			this.globalMBasis.scaleXTo(scale);
			if(this.parent != null)
				this.parent.setToLocalOf(this.globalMBasis, this.localMBasis);
			else
				this.localMBasis.adoptValues(globalMBasis);
			this.markDirty();
			this.updateGlobal();
		}
	}

	public void scaleYTo(float scale) {		
		if(!this.forceOrthoNormality) {
			this.updateGlobal();
			this.globalMBasis.scaleYTo(scale);
			if(this.parent != null)
				this.parent.setToLocalOf(this.globalMBasis, this.localMBasis);
			else
				this.localMBasis.adoptValues(globalMBasis);
			this.markDirty();
			this.updateGlobal();
		}
	}	

	public void scaleZTo(float scale) {
		if(!this.forceOrthoNormality) {
			this.updateGlobal();
			this.globalMBasis.scaleZTo(scale);
			if(this.parent != null)
				this.parent.setToLocalOf(this.globalMBasis, this.localMBasis);
			else
				this.localMBasis.adoptValues(globalMBasis);
			this.markDirty();
			this.updateGlobal();
		}
	}

	public void markChildScalesDirty() {
		for(AxisDependancy axes : dependentsRegistry) {
			if(AbstractAxes.class.isAssignableFrom(axes.getClass()))
				((AbstractAxes)axes).markScaleDirty();
		}
	}

	public void markChildReflectionDirty(int flipFlag) {
		for(AxisDependancy axes : dependentsRegistry) {
			if(AbstractAxes.class.isAssignableFrom(axes.getClass()))
				((AbstractAxes)axes).markReflectionDirty(flipFlag);
		}
	}

	public void markScaleDirty() {
		this.scaleDirty = true;
		markChildScalesDirty();
	}

	public void markReflectionDirty(int flipFlag) {
		this.flipFlag = flipFlag;
		markChildReflectionDirty(this.flipFlag);
	}


	public AbstractAxes getGlobalCopy() {
		this.updateGlobal();
		AbstractAxes globalCopy = 
				instantiate(
						this.origin_(), 
						this.x_().heading(), 
						this.y_().heading(), 
						this.z_().heading(), 
						this.forceOrthoNormality,
						null);
		globalCopy.localMBasis.adoptValues(this.globalMBasis);
		globalCopy.markDirty();
		globalCopy.updateGlobal();
		return globalCopy;
	}

	public AbstractAxes getRawGlobalCopy() {
		this.updateGlobal();
		AbstractAxes rawGlobalCopy = 
				instantiate(
						this.origin_(), 
						this.globalMBasis.getXRay().heading(), 
						this.globalMBasis.getYRay().heading(), 
						this.globalMBasis.getZRay().heading(), 
						false,
						null);
		rawGlobalCopy.localMBasis.adoptValues(this.globalMBasis);
		rawGlobalCopy.markDirty();
		rawGlobalCopy.updateGlobal();
		return rawGlobalCopy;
	}

	public AbstractAxes getOrthoNormalizedGlobalCopy() {
		this.updateGlobal();		
		AbstractAxes orthoNormalizedCopy =
				instantiate(
						this.origin_(), 
						this.x_norm_().heading(), 
						this.y_norm_().heading(),
						this.z_norm_().heading(), 
						true,
						null);
		orthoNormalizedCopy.localMBasis.rotation = new Rot(this.globalMBasis.rotation.rotation);
		orthoNormalizedCopy.localMBasis.setShearXBaseTo(Basis.xBase.mult(globalMBasis.flippedAxes[Basis.X] ? -1 : 1), false);
		orthoNormalizedCopy.localMBasis.setShearYBaseTo(Basis.yBase.mult(globalMBasis.flippedAxes[Basis.Y] ? -1 : 1), false);
		orthoNormalizedCopy.localMBasis.setShearZBaseTo(Basis.zBase.mult(globalMBasis.flippedAxes[Basis.Z] ? -1 : 1), false);
		orthoNormalizedCopy.localMBasis.rotation = new Rot(this.globalMBasis.rotation.rotation);
		orthoNormalizedCopy.markDirty();
		orthoNormalizedCopy.updateGlobal();
		return orthoNormalizedCopy;
	}
	
	
	/**
	 * Sets the parentAxes for this axis globally.  
	 * in other words, globalX, globalY, and globalZ remain unchanged, but lx, ly, and lz 
	 * change.
	 * 
	 *   @param par the new parent Axes
	 **/
	public void setParent(AbstractAxes par) {
		setParent(par, null);
	}
	

	/**
	 * Sets the parentAxes for this axis globally.  
	 * in other words, globalX, globalY, and globalZ remain unchanged, but lx, ly, and lz 
	 * change.
	 * 
	 *   @param intendedParent the new parent Axes
	 *   @param requestedBy the object making thisRequest, will be passed on to parentChangeWarning 
	 *   for any AxisDependancy objects registered with this AbstractAxes  (can be null if not important)
	 **/	
	public void setParent(AbstractAxes intendedParent, Object requestedBy) {	
		this.updateGlobal();
		AbstractAxes oldParent = this.getParentAxes();
		for(AxisDependancy ad : this.dependentsRegistry) {
			ad.parentChangeWarning(this, oldParent, intendedParent, requestedBy);
		}
		if(intendedParent != null && intendedParent != this) {
			intendedParent.updateGlobal(); 
			intendedParent.globalMBasis.setToLocalOf(globalMBasis, localMBasis);			

			if(this.parent != null) this.parent.disown(this);
			this.parent = intendedParent;

			this.parent.registerDependent(this);
			this.areGlobal = false;
		} else {
			this.parent = null;
			this.areGlobal = true;
		}
		this.markDirty();
		this.updateGlobal();
		for(AxisDependancy ad : this.dependentsRegistry) {
			ad.parentChangeCompletionNotice(this, oldParent, intendedParent, requestedBy);
		}
	}


	/**
	 * Sets the parentAxes for this axis locally. 
	 * in other words, lx,ly,lz remain unchanged, but globalX, globalY, and globalZ 
	 * change.  
	 * 
	 * if setting this parent would result in a dependency loop, then the input Axes 
	 * parent is set to this Axes' parent, prior to this axes setting the input axes
	 * as its parent.   
	 **/
	public void setRelativeToParent(AbstractAxes par) {
		if(this.parent != null) this.parent.disown(this);
		this.parent = par;
		this.areGlobal = false;
		this.parent.registerDependent(this);
		this.markDirty();
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
	public SGVec_3f getGlobalOf(SGVec_3f in) {
		this.updateGlobal();
		SGVec_3f result =  in.copy();
		setToGlobalOf(in, result);
		return  result;
	}

	public SGVec_3f getOrthoNormalizedGlobalOf(SGVec_3f in) {
		SGVec_3f result =  workingVector.copy();
		setToOrthoNormalizedGlobalOf(in, result);
		return  result;
	}

	public sgRayf getOrthoNormalizedGlobalOf(sgRayf in) {
		sgRayf result = new sgRayf(new SGVec_3f(0,0,0), new SGVec_3f(1,1,1));
		setToOrthoNormalizedGlobalOf(in, result);
		return result;
	}


	/**
	 *  Given a vector in this axes local coordinates, modifies the vector's values to represent its position global coordinates.
	 * @param in
	 * @return a reference to this the @param in object.
	 */
	public SGVec_3f setToGlobalOf(SGVec_3f in) {
		this.updateGlobal();
		if(this.forceOrthoNormality)
			this.setToOrthoNormalizedGlobalOf(in, in);
		else 
			globalMBasis.setToGlobalOf(in, in);
		return in;
	}

	/**
	 *  Given an input vector in this axes local coordinates, modifies the output vector's values to represent the input's position in global coordinates.
	 * @param in
	 */
	public void setToGlobalOf(SGVec_3f input, SGVec_3f output) {
		this.updateGlobal();
		if(this.forceOrthoNormality)
			this.setToOrthoNormalizedGlobalOf(input, output);
		else 
			globalMBasis.setToGlobalOf(input, output);		
	}

	/** 
	 * like setToGlobalOf, but operates on the axes non-orthonormaldoppelganger
	 * @param input
	 * @param output
	 * @return a reference to these Axes, for method chaining.
	 */
	public void setToRawGlobalOf(SGVec_3f input, SGVec_3f output) {
		this.updateGlobal();
		globalMBasis.setToGlobalOf(input, output);
	}

	public void setToOrthoNormalizedGlobalOf(SGVec_3f input, SGVec_3f output) {
		this.updateGlobal();		
		globalMBasis.setToOrthoNormalGlobalOf(input, output);
	}

	public void setToOrthoNormalizedGlobalOf(sgRayf input, sgRayf output) {
		this.updateGlobal();
		this.setToOrthoNormalizedGlobalOf(input.p1(), output.p1());
		this.setToOrthoNormalizedGlobalOf(input.p2(), output.p2());
	}

	public void setToRawGlobalOf(sgRayf input, sgRayf output) {
		this.updateGlobal();
		this.setToRawGlobalOf(input.p1(), output.p1());
		this.setToRawGlobalOf(input.p2(), output.p2());
	}

	/**
	 *  Given an input sgRay in this axes local coordinates, modifies the output Rays's values to represent the input's in global coordinates.
	 * @param in
	 */
	public void setToGlobalOf(sgRayf input, sgRayf output) {
		this.updateGlobal();
		this.setToGlobalOf(input.p1(), output.p1());
		this.setToGlobalOf(input.p2(), output.p2());
	}

	public sgRayf getGlobalOf(sgRayf in) { 
		return new sgRayf(this.getGlobalOf( in.p1()), this.getGlobalOf( in.p2()));
	}
	
	/**
	 * instantiate an instance of this Axes object.
	 * Because Abstract classes cannot be instantiated, the user is expected to 
	 * extend this method to do whatever preprocessing might be required 
	 * and then to call the appropriate constructor of the extending class 
	 * @param add
	 * @param gXHeading
	 * @param gYHeading
	 * @param gZHeading
	 * @param forceOrthoNormality
	 * @param par
	 * @return
	 */
	protected abstract AbstractAxes instantiate(
			SGVec_3f add, 
			SGVec_3f gXHeading, 
			SGVec_3f gYHeading, 
			SGVec_3f gZHeading, 
			boolean forceOrthoNormality, 
			AbstractAxes par);
	


	/**
	 * returns an axis representing the global location of this axis if the input axis were its parent.
	 * @param in
	 * @return
	 */
	public AbstractAxes relativeTo(AbstractAxes in) { 
		AbstractAxes result = 
				instantiate(workingVector, 
						this.localMBasis.getXRay().heading(), 
						this.localMBasis.getYRay().heading(), 
						this.localMBasis.getZRay().heading(), 
						this.forceOrthoNormality, 
						null);

		result.setParent(in);
		return result;
	}


	public boolean hasNonOrthonormalAncestor() {
		if(this.parent == null) return false; 
		else if (!this.parent.forceOrthoNormality || !this.forceOrthoNormality) 
			return true; 
		else 
			return this.parent.hasNonOrthonormalAncestor();
	}

	public SGVec_3f getRawGlobalOf(SGVec_3f input) {
		SGVec_3f result =  input.copy();
		setToRawGlobalOf(input, result);
		return  result;
	}

	public sgRayf getRawGlobalOf(sgRayf input) {
		return new sgRayf(getRawGlobalOf(input.p1()), getRawGlobalOf(input.p2()));
	}


	public sgRayf getRawLocalOf(sgRayf input) {
		return new sgRayf(getRawLocalOf(input.p1()), getRawLocalOf(input.p2()));
	}



	public sgRayf getOrthoNormalizedLocalOf(sgRayf input) {
		return new sgRayf(getOrthoNormalizedLocalOf(input.p1()), getOrthoNormalizedLocalOf(input.p2()));
	}

	public SGVec_3f   getOrthoNormalizedLocalOf(SGVec_3f in) {
		SGVec_3f result =  in.copy(); 
		setToOrthoNormalLocalOf(in, result);
		return  result;
	}


	/**
	 * like getLocalOf, except uses the axes non-orthonormal doppelganger. 
	 * @param in
	 * @return
	 */
	public SGVec_3f   getRawLocalOf(SGVec_3f in) {
		SGVec_3f result =  in.copy(); 
		setToRawLocalOf(in, result);
		return  result;
	}

	/**
	 * like setToLocalOf, except uses the axes non-orthonormal doppelganger. 
	 * @param in
	 * @return
	 */
	public void setToRawLocalOf(SGVec_3f in, SGVec_3f out) {
		this.updateGlobal();
		this.globalMBasis.setToLocalOf(in, out);
	}

	public void setToRawLocalOf(sgRayf input, sgRayf output) {
		this.setToRawLocalOf(input.p1(), output.p1());
		this.setToRawLocalOf(input.p2(), output.p2());
	}

	public void setToRawLocalOf(Basis input, Basis output) {
		this.updateGlobal();
		this.globalMBasis.setToLocalOf(input, output);
	}

	public void setToOrthoNormalizedLocalOf(Basis input, Basis output) {
		this.updateGlobal();
		this.globalMBasis.setToOrthoNormalLocalOf(input, output);
	}


	public SGVec_3f getLocalOf(SGVec_3f in) {
		SGVec_3f result =  in.copy();
		setToLocalOf(in, result);
		return  result;
	}


	/**
	 *  Given a vector in global coordinates, modifies the vector's values to represent its position in theseAxes local coordinates.
	 * @param in
	 * @return a reference to the @param in object. 
	 */

	public SGVec_3f setToLocalOf(SGVec_3f  in) {
		if(forceOrthoNormality)
			setToOrthoNormalLocalOf(in, in);
		else 
			setToRawLocalOf(in, in);
		return in;
	}

	/**
	 *  Given a vector in global coordinates, modifies the vector's values to represent its position in theseAxes local coordinates.
	 * @param in
	 */

	public void setToLocalOf(SGVec_3f in, SGVec_3f out) {
		if(forceOrthoNormality)
			setToOrthoNormalLocalOf(in, out);
		else 
			setToRawLocalOf(in, out);
	}

	/**
	 *  Given a sgRay in global coordinates, modifies the sgRay's values to represent its position in theseAxes local coordinates.
	 * @param in
	 */

	public void setToLocalOf(sgRayf in, sgRayf out) {
		this.setToLocalOf(in.p1(), out.p1());
		this.setToLocalOf(in.p2(), out.p2());
	}

	public void setToLocalOf(Basis input, Basis output) {
		this.globalMBasis.setToLocalOf(input, output);
	}

	public sgRayf getLocalOf(sgRayf in) {
		return new sgRayf(this.getLocalOf(in.p1()), this.getLocalOf(in.p2()));  
	}


	public AbstractAxes getLocalOf(AbstractAxes input) {
		return instantiate(
				this.getLocalOf(input.origin_()), 
				this.getLocalOf(input.x_()).heading(), 
				this.getLocalOf(input.y_()).heading(), 
				this.getLocalOf(input.z_()).heading(), 
				input.forceOrthoNormality,
				null); 
	}	

	public Basis getLocalOf(Basis input) {
		return new Basis(this.getLocalOf(input.getXRay()), this.getLocalOf(input.getYRay()), this.getLocalOf(input.getZRay()));
	}

	public void translateByLocal(SGVec_3f translate) {    
		this.updateGlobal();
		localMBasis.translateBy(translate);
		this.markDirty();

	} 
	public void translateByGlobal(SGVec_3f translate) {		
		if(this.parent != null ) {
			this.updateGlobal();		
			this.translateTo(translate.addCopy(this.origin_()));
		} else {
			localMBasis.translateBy(translate);
		}

		this.markDirty();
	}



	public void translateTo(SGVec_3f translate, boolean slip) {
		this.updateGlobal();
		if(slip) {
			AbstractAxes tempAbstractAxes = this.getGlobalCopy();
			tempAbstractAxes.translateTo(translate);
			this.slipTo(tempAbstractAxes);
		} else {
			this.translateTo(translate);  
		}
	}

	public void translateTo(SGVec_3f translate) {
		if(this.parent != null ) {
			this.updateGlobal();
			localMBasis.translateTo(parent.globalMBasis.getLocalOf(translate));
			this.markDirty();
		} else {
			this.updateGlobal();
			localMBasis.translateTo(translate);
			this.markDirty();
		}


	}

	/**
	 * @return a copy of these axes that does not refer to any parent. 
	 * Because there is no parent, the copy's global coordinates and local coordinates will be equivalent to each other. 
	 * The copy's local coordinates will also be equivalent to the original's local coordinates. However, the copy's
	 * global coordinates will likely be drastically different from the original's global coordinates. 
	 *   
	 */
	public AbstractAxes freeCopy() {
		AbstractAxes freeCopy = 
				instantiate(
						this.localMBasis.translate, 
						this.localMBasis.getXHeading(), 
						this.localMBasis.getYHeading(),
						this.localMBasis.getZHeading(), 
						this.forceOrthoNormality,
						null);
		freeCopy.localMBasis.adoptValues(this.localMBasis);
		freeCopy.markDirty();
		freeCopy.updateGlobal();
		return freeCopy;
	}



	public AbstractAxes attachedCopy(boolean slipAware) {
		AbstractAxes copy = 
				instantiate(
						this.origin_(), 
						this.x_().heading(), 
						this.y_().heading(), 
						this.z_().heading(), 
						this.forceOrthoNormality, 
						this.parent);  
		if(!slipAware) copy.setSlipType(IGNORE);
		copy.localMBasis.adoptValues(this.localMBasis);
		copy.markDirty();
		return copy;
	}

	public void setSlipType(int type) {
		if(this.parent != null) {
			if(type == IGNORE) {
				this.parent.dependentsRegistry.remove(this);
			} else if(type == NORMAL || type == FORWARD) {
				this.parent.registerDependent(this);
			} 
		}
		this.slipType = type;
	}

	public int getSlipType() {
		return this.slipType;
	}


	public void scaleBy(float scaleX, float scaleY, float scaleZ) {
		this.updateGlobal();
		this.localMBasis.scaleXBy(scaleX);
		this.localMBasis.scaleYBy(scaleY);
		this.localMBasis.scaleZBy(scaleZ);
		this.markDirty();
		this.updateGlobal();
	}

	public void rotateAboutX(float angle, boolean orthonormalized) {
		this.updateGlobal();
		Rot xRot = new Rot(globalMBasis.getOrthonormalXHead(), angle);		
		this.rotateBy(xRot);
		//this.markDirty();
	}

	public void rotateAboutY(float angle, boolean orthonormalized) {
		this.updateGlobal();	
		Rot yRot = new Rot(globalMBasis.getOrthonormalYHead(), angle); 
		this.rotateBy(yRot);
		//this.markDirty();
	}

	public void rotateAboutZ(float angle, boolean orthonormalized) {
		this.updateGlobal();
		Rot zRot = new Rot(globalMBasis.getOrthonormalZHead(), angle);
		this.rotateBy(zRot);
		//this.markDirty();
	}


	public int getGlobalChirality() {
		this.updateGlobal();
		return this.globalChirality;
	}

	public int getLocalChirality() {
		this.updateGlobal();
		return this.localChirality;
	}

	public void rotateBy(MRotation apply) {
		this.updateGlobal();		
		if(parent != null) {		
			Rot newRot = this.parent.globalMBasis.getLocalOfRotation(new Rot(apply));
			this.localMBasis.rotateBy(newRot);
		} else {
			this.localMBasis.rotateBy(new Rot(apply));
		}		
		this.markDirty(); 
	}


	/**
	 * Rotates the bases around their origin in global coordinates
	 * @param rotation
	 */
	public void rotateBy(Rot apply) {

		this.updateGlobal();		
		if(parent != null) {
			Rot newRot = this.parent.globalMBasis.getLocalOfRotation(apply);
			this.localMBasis.rotateBy(newRot);
		} else {
			this.localMBasis.rotateBy(apply);
		}

		this.markDirty(); 
	}

	/**
	 * rotates the bases around their origin in Local coordinates
	 * @param rotation
	 */
	public void rotateByLocal(Rot apply) {
		this.updateGlobal();		
		if(parent != null) {
			this.localMBasis.rotateBy(apply);
		}
		this.markDirty(); 
	}
	
	/**
	 * @param input_global a ray in global space
	 * @param local_output will be updated to represent that   
	 * with respect to an orthonormal version of this axes. (shear is not applied, but reflections are) 
	 */	

	public void setToOrthonormalLocalOf(sgRayf global_input, sgRayf local_output) {
		this.setToOrthoNormalLocalOf(global_input.p1(), local_output.p1());
		this.setToOrthoNormalLocalOf(global_input.p2(), local_output.p2());
	}
	
	/**
	 * @param input_global a point in global space
	 * @param local_output will be updated to represent that point  
	 * with respect to an orthonormal version of this axes. (shear is not applied, but reflections are) 
	 */	

	public void setToOrthoNormalLocalOf(SGVec_3f input_global, SGVec_3f output_local_normalized) {
		this.updateGlobal();
		this.globalMBasis.setToOrthoNormalLocalOf(input_global, output_local_normalized);
	}
	
	/**
	 * @param input_global a point in global space
	 * @param output_local_orthonormal_chiral will be updated to the same point 
	 * with respect to a righthanded orthonormal version of this axes.
	 */	
	public void setToOrientationalLocalOf(SGVec_3f input_global, SGVec_3f output_local_orthonormal_chiral) {
		this.updateGlobal();
		this.globalMBasis.setToOrientationalLocalOf(input_global, output_local_orthonormal_chiral);
	}
	
	/**
	 * @param input_global a point in global space
	 * @return a copy of that point with respect to a righthanded orthonormal version of this axes.
	 */	
	public SGVec_3f   getOrientationalLocalOf(SGVec_3f input_global) {
		SGVec_3f result =  input_global.copy();
		setToOrientationalLocalOf(input_global, result);
		return  result;
	}

	/**
	 * @param input_local a point with respect to this axes
	 * @param output_local_orthonormal_chiral will be updated to a point 
	 * equivalent to the point which would result if transforming
	 * only by this axes position and orientation (scale, shear, and reflection are not applied)
	 */	
	public void setToOrientationalGlobalOf(SGVec_3f input_local, SGVec_3f output_global_orthonormal_chiral) {
		this.updateGlobal();
		this.globalMBasis.setToOrientationalGlobalOf(input_local, output_global_orthonormal_chiral);
	}

	/**
	 * @param input_local a point with respect to this axes
	 * @return a point equivalent to the point which would result if transforming
	 * only by this axes position and orientation (scale, shear, and reflection are not applied)
	 */	
	public SGVec_3f getOrientationalGlobalOf(SGVec_3f input_local) {
		SGVec_3f result =  input_local.copy();
		setToOrientationalGlobalOf(input_local, result);
		return result;
	}

	/**
	 * sets these axes to have the same orientation and location relative to their parent
	 * axes as the input's axes do to the input's parent axes.
	 * 
	 * If the axes on which this function is called are orthonormal, 
	 * this function normalizes and orthogonalizes them regardless of whether the targetAxes are orthonormal.
	 * 
	 * @param targetAxes the Axes to make this Axis identical to
	 */
	public void alignLocalsTo(AbstractAxes targetAxes ) {
		this.localMBasis.adoptValues(targetAxes.localMBasis);
		this.markDirty();
	}



	/**
	 * sets the bases to the Identity basis and Identity rotation relative to its parent, and translates 
	 * its origin to the parent's origin. 
	 * 
	 * be careful calling this method, as it destroys any shear / scale information. 
	 */
	public void alignToParent() {
		this.localMBasis.setIdentity();
		this.markDirty();
	}

	/**
	 * rotates and translates the axes back to its parent, but maintains 
	 * its shear, translate and scale attributes.
	 */
	public void rotateToParent() {
		this.localMBasis.rotateTo(new Rot(MRotation.IDENTITY));
		this.markDirty();
	}

	/**
	 * sets these axes to have the same global orientation as the input Axes. 
	 * these Axes lx, ly, and lz headings will differ from the target ages,
	 * but its gx, gy, and gz headings should be identical unless this 
	 * axis is orthonormalized and the target axes are not.
	 * @param targetAxes
	 */
	public void alignGlobalsTo(AbstractAxes targetAxes ) {
		targetAxes.updateGlobal();
		this.updateGlobal();
		if(this.parent != null) {
			parent.globalMBasis.setToLocalOf(targetAxes.globalMBasis, localMBasis);	
		} else {
			this.localMBasis.adoptValues(targetAxes.globalMBasis);
		}
		this.markDirty();
		this.updateGlobal();
	}

	public void alignOrientationTo(AbstractAxes targetAxes) {
		targetAxes.updateGlobal();
		this.updateGlobal();
		if(this.parent != null) {
			this.globalMBasis.rotateTo(targetAxes.globalMBasis.rotation);
			parent.globalMBasis.setToLocalOf(this.globalMBasis, this.localMBasis);
		} else {
			this.localMBasis.rotateTo(targetAxes.globalMBasis.rotation);
		}
		this.markDirty();
	}


	/**
	 * calling this function orthogonalizes and normalizes the bases in global space. 
	 */
	public void orthoNormalize() {
		this.updateGlobal();
		if(!this.areGlobal) {
			this.globalMBasis.orthoNormalize();
			parent.setToLocalOf(this.globalMBasis, this.localMBasis);//this.local_rawBasis.orthoNormalize();
		} else {
			this.localMBasis.orthoNormalize();
		}
		this.markDirty();
	}

	public void registerDependent(AxisDependancy newDependent) {
		//Make sure we don't hit a dependency loop
		if(AbstractAxes.class.isAssignableFrom(newDependent.getClass())) {
			if(((AbstractAxes)newDependent).isAncestorOf(this)) {
				this.transferToParent(((AbstractAxes)newDependent).getParentAxes());
			}			
		} 
		if(dependentsRegistry.indexOf(newDependent) == -1){
			dependentsRegistry.add(newDependent);
		}
	}

	public boolean isAncestorOf(AbstractAxes potentialDescendent) {
		boolean result = false;
		AbstractAxes cursor = potentialDescendent.getParentAxes();
		while(cursor != null) {
			if(cursor == this) {
				result = true; 
				break; 
			} else {
				cursor = cursor.getParentAxes(); 
			}
		}		
		return result;
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

	/**
	 * unregisters this AbstractAxes from its parent, 
	 * but keeps its global position the same.
	 */
	public void emancipate() {
		if(this.parent != null) {
			this.updateGlobal();
			AbstractAxes oldParent = this.parent;
			for(AxisDependancy ad: this.dependentsRegistry) {
				ad.parentChangeWarning(this, this.parent, null, null);
			}
			this.localMBasis.adoptValues(this.globalMBasis);
			this.parent.disown(this);
			this.parent = null;
			this.areGlobal = true;
			this.markDirty();
			this.updateGlobal();
			for(AxisDependancy ad: this.dependentsRegistry) {
				ad.parentChangeCompletionNotice(this, oldParent, null, null);
			}
		}
	}

	public void disown(AxisDependancy child) {
		dependentsRegistry.remove(child);
	}

	public void axisSlipWarning(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes actualAxis, ArrayList<Object>dontWarn) {
		this.updateGlobal();
		if(this.slipType == NORMAL ) {
			if(this.parent != null) {
				AbstractAxes globalVals = this.relativeTo(globalPriorToSlipping);
				globalVals = globalPriorToSlipping.getLocalOf(globalVals); 
				this.localMBasis.adoptValues(globalMBasis);
				this.markDirty();
			}
		} else if(this.slipType == FORWARD) {
			AbstractAxes globalAfterVals = this.relativeTo(globalAfterSlipping);
			this.notifyDependentsOfSlip(globalAfterVals, dontWarn);
		}
	}

	/**
	 * if the input axes have have the same global
	 * values as these axes, returns true, otherwise, returns false.
	 * 
	 * This function is orthonormality aware. Meaning, if the orthonormality 
	 * constraint is enabled on either axes, that axes' orthonormal version
	 * will be used in the comparison. 
	 * @param ax
	 */
	public boolean equals(AbstractAxes ax) {
		this.updateGlobal();
		ax.updateGlobal();
		Matrix4f thisGlobal = forceOrthoNormality ? globalMBasis.composedOrthoNormalMatrix : globalMBasis.getComposedMatrix();
		Matrix4f axGlobal = ax.forceOrthoNormality ? ax.globalMBasis.composedOrthoNormalMatrix : ax.globalMBasis.getComposedMatrix();

		boolean composedMatricesAreEquivalent = thisGlobal.equals(axGlobal);
		boolean originsAreEquivalent = globalMBasis.getOrigin().equals(ax.origin_());

		return composedMatricesAreEquivalent && originsAreEquivalent;
	}

	public void axisSlipWarning(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes actualAxis) {

	}

	public void axisSlipCompletionNotice(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes thisAxis) {

	}

	public void slipTo(AbstractAxes newAxisGlobal) {
		this.updateGlobal();
		AbstractAxes originalGlobal = this.getGlobalCopy();
		notifyDependentsOfSlip(newAxisGlobal); 
		AbstractAxes newVals = newAxisGlobal.freeCopy();

		if(this.parent != null) {
			newVals = parent.getLocalOf(newVals);
		}
		this.localMBasis.adoptValues(newVals.globalMBasis);
		this.dirty = true;	
		this.updateGlobal();

		notifyDependentsOfSlipCompletion(originalGlobal);
	}

	public void slipTo(AbstractAxes newAxisGlobal, ArrayList<Object> dontWarn) {
		this.updateGlobal();
		AbstractAxes originalGlobal = this.getGlobalCopy();
		notifyDependentsOfSlip(newAxisGlobal, dontWarn); 
		AbstractAxes newVals = newAxisGlobal.getGlobalCopy();

		if(this.parent != null) {
			newVals = parent.getLocalOf(newAxisGlobal);
		}
		this.alignGlobalsTo(newAxisGlobal);
		this.markDirty();
		this.updateGlobal();
		
		notifyDependentsOfSlipCompletion(originalGlobal, dontWarn);
	}

	public void notifyDependentsOfSlip(AbstractAxes newAxisGlobal, ArrayList<Object> dontWarn) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {
			if(!dontWarn.contains(dependentsRegistry.get(i))) {
				AxisDependancy dependant = dependentsRegistry.get(i);

				//First we check if the dependent extends AbstractAxes
				//so we know whether or not to pass the dontWarn list
				if(this.getClass().isAssignableFrom(dependant.getClass())) { 
					((AbstractAxes)dependant).axisSlipWarning(this.getGlobalCopy(), newAxisGlobal, this, dontWarn);
				} else {
					dependant.axisSlipWarning(this.getGlobalCopy(), newAxisGlobal, this);
				}
			} else {
				System.out.println("skipping: " + dependentsRegistry.get(i));
			}
		}
	}

	public void notifyDependentsOfSlipCompletion(AbstractAxes globalAxisPriorToSlipping, ArrayList<Object> dontWarn) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {
			if(!dontWarn.contains(dependentsRegistry.get(i)))
				dependentsRegistry.get(i).axisSlipCompletionNotice(globalAxisPriorToSlipping, this.getGlobalCopy(), this);
			else 
				System.out.println("skipping: " + dependentsRegistry.get(i));
		}
	}


	public void notifyDependentsOfSlip(AbstractAxes newAxisGlobal) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {
			dependentsRegistry.get(i).axisSlipWarning(this.getGlobalCopy(), newAxisGlobal, this);
		}
	}

	public void notifyDependentsOfSlipCompletion(AbstractAxes globalAxisPriorToSlipping) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {//AxisDependancy dependent : dependentsRegistry) {
			dependentsRegistry.get(i).axisSlipCompletionNotice(globalAxisPriorToSlipping, this.getGlobalCopy(), this);
		}
	}

	

	public void markDirty() {
		if(!this.dirty) {
			this.dirty = true;
			this.markDependentsDirty();
		}
	}

	public void markDependentsDirty() {
		for(AxisDependancy ad : dependentsRegistry) {
			ad.markDirty();
		}
	}

	public String toString() {
		String global = "Global: " + globalMBasis.toString();
		String local = "Local: " + localMBasis.toString();
		return global + "\n" + local;
	}
	
	
	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		this.updateGlobal();
		JSONObject thisAxes = new JSONObject(); 
		JSONObject shearScale = new JSONObject();
		SGVec_3f xShear = new SGVec_3f(); 
		SGVec_3f yShear = new SGVec_3f(); 
		SGVec_3f zShear = new SGVec_3f(); 

		this.localMBasis.setToShearXBase(xShear);
		this.localMBasis.setToShearYBase(yShear);
		this.localMBasis.setToShearZBase(zShear);

		shearScale.setJSONArray("x", xShear.toJSONArray());
		shearScale.setJSONArray("y", yShear.toJSONArray());
		shearScale.setJSONArray("z", zShear.toJSONArray());

		thisAxes.setJSONArray("translation", localMBasis.translate.toJSONArray());
		thisAxes.setJSONArray("rotation", localMBasis.rotation.toJsonArray());
		thisAxes.setJSONObject("bases", shearScale);

		//thisAxes.setJSONArray("flippedAxes", saveManager.primitiveArrayToJSONArray(this.localMBasis.flippedAxes));
		String parentHash = "-1"; 
		if(parent != null) parentHash = ((AbstractAxes) parent).getIdentityHash();
		thisAxes.setString("parent",  parentHash);
		thisAxes.setInt("slipType", this.getSlipType());
		thisAxes.setString("identityHash",  this.getIdentityHash());
		thisAxes.setBoolean("forceOrthoNormality", this.forceOrthoNormality);

		return thisAxes;
	}

	@Override
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		SGVec_3f origin = new SGVec_3f(j.getJSONArray("translation"));
		SGVec_3f x = new SGVec_3f(j.getJSONObject("bases").getJSONArray("x"));
		SGVec_3f y = new SGVec_3f(j.getJSONObject("bases").getJSONArray("y"));
		SGVec_3f z =  new SGVec_3f(j.getJSONObject("bases").getJSONArray("z"));
		Rot rotation = new Rot(j.getJSONArray("rotation"));
		this.forceOrthoNormality = j.getBoolean("forceOrthoNormality");
		this.localMBasis.setShearXBaseTo(x, false);
		this.localMBasis.setShearYBaseTo(y, false);
		this.localMBasis.setShearZBaseTo(z, false);
		this.localMBasis.translate = origin;
		this.localMBasis.rotation = rotation;
		this.localMBasis.refreshMatrices();
		AbstractAxes par = (AbstractAxes) l.getObjectFor(AbstractAxes.class, j, "parent");
		if(par != null)
			this.setRelativeToParent(par);
		this.setSlipType(j.getInt("slipType"));
	}
	
	@Override
	public void notifyOfSaveIntent(SaveManager saveManager) {}	
	
	@Override
	public void notifyOfLoadCompletion() {
		this.markDirty();
	}

	boolean isLoading = false;
	@Override
	public void setLoading(boolean loading) {isLoading = loading;}
	@Override
	public boolean isLoading() {return isLoading;}
	@Override
	public void notifyOfSaveCompletion(SaveManager saveManager) {}
	public void makeSaveable(SaveManager saveManager) {
		if(this.parent != null) 
			((AbstractAxes)parent).makeSaveable(saveManager);
		saveManager.addToSaveState(this);
	}
}
