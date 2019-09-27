package sceneGraph.math.doubleV;

import data.JSONObject;
import data.LoadManager;
import data.SaveManager;
import sceneGraph.math.doubleV.AbstractAxes.DependencyReference;

public class AffineAxes extends AbstractAxes {
	
	
	public boolean scaleDirty = false;
	public boolean forceOrthoNormality = true;
	private int globalChirality = RIGHT;
	private int localChirality = RIGHT;
	private sgRayd xTemp = new sgRayd(); 
	private sgRayd yTemp = new sgRayd(); 
	private sgRayd zTemp = new sgRayd();
	
	private int flipFlag = -1; //value of -1 means the bases do not need to flip. values of 0, 1, or 2 mean the bases 
	//should flip along their X, Y, or Z axes respectively.  


	public <V extends Vec3d<?>> AffineAxes(V origin, V inX, V inY, V inZ, boolean forceOrthoNormality,
			AbstractAxes parent) {
		super(origin, inX, inY, inZ, parent, true);
		this.forceOrthoNormality = forceOrthoNormality; 
		createTempVars(origin);

		areGlobal = true;		
		sgRayd xRay = new sgRayd(origin, origin.addCopy(inX));
		sgRayd yRay = new sgRayd(origin, origin.addCopy(inY));
		sgRayd zRay = new sgRayd(origin, origin.addCopy(inZ));

		localMBasis = new AffineBasis(xRay, yRay, zRay);
		globalMBasis = new AffineBasis(xRay, yRay, zRay);
		
		V o = (V)origin.copy(); o.set(0,0,0);
		V i = (V)o.copy(); i.set(1,1,1);
		xTemp = new sgRayd(o, i); 
	    yTemp = new sgRayd(o.copy(), i.copy()); 
		zTemp = new sgRayd(o.copy(), i.copy());
		//globalNormalizedBasis = getGlobalMBasis().copy();
		

		if(parent != null) {
			this.setParent(parent);
		} 	else {
			this.areGlobal = true;
		}
		this.updateGlobal();
	}


	public AffineAxes(AffineBasis globalMBasis, boolean forceOrthoNormality, AffineAxes object) {
		super(globalMBasis, object);
		this.forceOrthoNormality = forceOrthoNormality;
	}


	/**
	 * @return a copy of these axes that does not refer to any parent. 
	 * Because there is no parent, the copy's global coordinates and local coordinates will be equivalent to each other. 
	 * The copy's local coordinates will also be equivalent to the original's local coordinates. However, the copy's
	 * global coordinates will likely be drastically different from the original's global coordinates. 
	 *   
	 */
	@Override
	public AbstractAxes freeCopy() {
		AbstractAxes freeCopy = 
				new AffineAxes(
						this.getLocalMBasis().translate, 
						this.getLocalMBasis().getXHeading(), 
						this.getLocalMBasis().getYHeading(),
						this.getLocalMBasis().getZHeading(), 
						this.forceOrthoNormality,
						null);
		freeCopy.getLocalMBasis().adoptValues(this.localMBasis);
		freeCopy.markDirty();
		freeCopy.updateGlobal();
		return freeCopy;
	}
	
	
	/**
	 * Creates an exact copy of this Axes object. Attached to the same parent as this Axes object
	 * @param slipAware
	 * @return
	 */
	@Override
	public AbstractAxes attachedCopy(boolean slipAware) {
		this.updateGlobal();
		AbstractAxes copy = new AffineAxes(getGlobalMBasis(),
																this.forceOrthoNormality, 
																this.getParentAxes());  
		if(!slipAware) copy.setSlipType(IGNORE);
		copy.getLocalMBasis().adoptValues(this.localMBasis);
		copy.markDirty();
		return copy;
	}
	
	@Override
	public AbstractAxes relativeTo(AbstractAxes in) { 
		AffineAxes result = 
				new AffineAxes(workingVector, 
						this.getLocalMBasis().getXRay().heading(), 
						this.getLocalMBasis().getYRay().heading(), 
						this.getLocalMBasis().getZRay().heading(), 
						this.forceOrthoNormality, 
						null);

		result.setParent(in);
		return result;
	}
	
	/**
	 * Make a GlobalCopy of these Axes. 
	 * @return
	 */
	@Override
	public AffineAxes getGlobalCopy() {
		this.updateGlobal();
		AffineAxes globalCopy = new AffineAxes( this.getGlobalMBasis(), this.forceOrthoNormality, null);
		globalCopy.getLocalMBasis().adoptValues(this.globalMBasis);
		globalCopy.markDirty();
		globalCopy.updateGlobal();
		return globalCopy;
	}

	public AffineAxes getOrthoNormalizedGlobalCopy() {
		this.updateGlobal();		
		AffineAxes orthoNormalizedCopy =
				new AffineAxes(
						this.origin_(), 
						this.x_norm_().heading(), 
						this.y_norm_().heading(),
						this.z_norm_().heading(), 
						true,
						null);
		orthoNormalizedCopy.getLocalMBasis().rotation = new Rot(this.getGlobalMBasis().rotation.rotation);
		orthoNormalizedCopy.getLocalMBasis().setShearXBaseTo(AffineBasis.xBase.mult(getGlobalMBasis().flippedAxes[AffineBasis.X] ? -1 : 1), false);
		orthoNormalizedCopy.getLocalMBasis().setShearYBaseTo(AffineBasis.yBase.mult(getGlobalMBasis().flippedAxes[AffineBasis.Y] ? -1 : 1), false);
		orthoNormalizedCopy.getLocalMBasis().setShearZBaseTo(AffineBasis.zBase.mult(getGlobalMBasis().flippedAxes[AffineBasis.Z] ? -1 : 1), false);
		orthoNormalizedCopy.getLocalMBasis().rotation = new Rot(this.getGlobalMBasis().rotation.rotation);
		orthoNormalizedCopy.markDirty();
		orthoNormalizedCopy.updateGlobal();
		return orthoNormalizedCopy;
	}
	
	
	public boolean hasNonOrthonormalAncestor() {
		if(this.getParentAxes() == null) return false; 
		else if (!this.getParentAxes().forceOrthoNormality || !this.forceOrthoNormality) 
			return true; 
		else 
			return this.getParentAxes().hasNonOrthonormalAncestor();
	}

	public <V extends Vec3d<?>> V getRawGlobalOf(V input) {
		V result =  (V) input.copy();
		setToRawGlobalOf(input, result);
		return  result;
	}

	public sgRayd getRawGlobalOf(sgRayd input) {
		return new sgRayd(getRawGlobalOf(input.p1()), getRawGlobalOf(input.p2()));
	}


	public sgRayd getRawLocalOf(sgRayd input) {
		return new sgRayd(getRawLocalOf(input.p1()), getRawLocalOf(input.p2()));
	}



	public sgRayd getOrthoNormalizedLocalOf(sgRayd input) {
		return new sgRayd(getOrthoNormalizedLocalOf(input.p1()), getOrthoNormalizedLocalOf(input.p2()));
	}

	public <V extends Vec3d<?>> V   getOrthoNormalizedLocalOf(V in) {
		V result =  (V) in.copy(); 
		setToOrthoNormalLocalOf(in, result);
		return  result;
	}


	/**
	 * like getLocalOf, except uses the axes non-orthonormal doppelganger. 
	 * @param in
	 * @return
	 */
	public <V extends Vec3d<?>> V  getRawLocalOf(V in) {
		V result =  (V) in.copy(); 
		setToRawLocalOf(in, result);
		return  result;
	}
	
	/**
	 *  Given a vector in global coordinates, modifies the vector's values to represent its position in theseAxes local coordinates.
	 * @param in
	 * @return a reference to the @param in object. 
	 */
	public SGVec_3d setToLocalOf(SGVec_3d  in) {
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
	public void setToLocalOf(SGVec_3d in, SGVec_3d out) {
		if(forceOrthoNormality)
			setToOrthoNormalLocalOf(in, out);
		else 
			setToRawLocalOf(in, out);
	}

	/**
	 * like setToLocalOf, except uses the axes non-orthonormal doppelganger. 
	 * @param in
	 * @return
	 */
	public <V extends Vec3d<?>> void setToRawLocalOf(V in, V out) {
		this.updateGlobal();
		this.getGlobalMBasis().setToLocalOf(in, out);
	}

	public void setToRawLocalOf(sgRayd input, sgRayd output) {
		this.setToRawLocalOf(input.p1(), output.p1());
		this.setToRawLocalOf(input.p2(), output.p2());
	}

	public void setToRawLocalOf(AffineBasis input, AffineBasis output) {
		this.updateGlobal();
		this.getGlobalMBasis().setToLocalOf(input, output);
	}

	public void setToOrthoNormalizedLocalOf(AffineBasis input, AffineBasis output) {
		this.updateGlobal();
		this.getGlobalMBasis().setToOrthoNormalLocalOf(input, output);
	}
	
	/**
	 * @param input_global a ray in global space
	 * @param local_output will be updated to represent that   
	 * with respect to an orthonormal version of this axes. (shear is not applied, but reflections are) 
	 */	

	public void setToOrthonormalLocalOf(sgRayd global_input, sgRayd local_output) {
		this.setToOrthoNormalLocalOf(global_input.p1(), local_output.p1());
		this.setToOrthoNormalLocalOf(global_input.p2(), local_output.p2());
	}
	
	/**
	 * @param input_global a point in global space
	 * @param local_output will be updated to represent that point  
	 * with respect to an orthonormal version of this axes. (shear is not applied, but reflections are) 
	 */	

	public <V extends Vec3d<?>> void setToOrthoNormalLocalOf(V input_global, V output_local_normalized) {
		this.updateGlobal();
		this.getGlobalMBasis().setToOrthoNormalLocalOf(input_global, output_local_normalized);
	}
	
	/**
	 * @param input_global a point in global space
	 * @param output_local_orthonormal_chiral will be updated to the same point 
	 * with respect to a righthanded orthonormal version of this axes.
	 */	
	public <V extends Vec3d<?>> void setToOrientationalLocalOf(V input_global, V output_local_orthonormal_chiral) {
		this.updateGlobal();
		this.getGlobalMBasis().setToOrientationalLocalOf(input_global, output_local_orthonormal_chiral);
	}
	
	/**
	 * @param input_global a point in global space
	 * @return a copy of that point with respect to a righthanded orthonormal version of this axes.
	 */	
	public SGVec_3d   getOrientationalLocalOf(SGVec_3d input_global) {
		SGVec_3d result =  input_global.copy();
		setToOrientationalLocalOf(input_global, result);
		return  result;
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
	@Override
	public <A extends AbstractAxes> boolean equals(A ax) {
		this.updateGlobal();
		ax.updateGlobal();
		boolean composedMatricesAreEquivalent = false; 
		Matrix4d thisGlobal = forceOrthoNormality ? getGlobalMBasis().composedOrthoNormalMatrix : getGlobalMBasis().getComposedMatrix();
		if(this.getClass().isAssignableFrom(ax.getClass())) {
			Matrix4d axGlobal = ((AffineAxes)ax).forceOrthoNormality ? ((AffineAxes)ax).getGlobalMBasis().composedOrthoNormalMatrix : ((AffineAxes)ax).getGlobalMBasis().getComposedMatrix();
			composedMatricesAreEquivalent = thisGlobal.equals(axGlobal);
		}		
		boolean originsAreEquivalent = getGlobalMBasis().getOrigin().equals(ax.origin_());

		return composedMatricesAreEquivalent && originsAreEquivalent;
	}

	/**
	 * @param input_local a point with respect to this axes
	 * @param output_local_orthonormal_chiral will be updated to a point 
	 * equivalent to the point which would result if transforming
	 * only by this axes position and orientation (scale, shear, and reflection are not applied)
	 */	
	public void setToOrientationalGlobalOf(SGVec_3d input_local, SGVec_3d output_global_orthonormal_chiral) {
		this.updateGlobal();
		this.getGlobalMBasis().setToOrientationalGlobalOf(input_local, output_global_orthonormal_chiral);
	}

	/**
	 * @param input_local a point with respect to this axes
	 * @return a point equivalent to the point which would result if transforming
	 * only by this axes position and orientation (scale, shear, and reflection are not applied)
	 */	
	public SGVec_3d getOrientationalGlobalOf(SGVec_3d input_local) {
		SGVec_3d result =  input_local.copy();
		setToOrientationalGlobalOf(input_local, result);
		return result;
	}
	
	public void updateGlobal() {
		if(this.dirty || this.scaleDirty) {

			if(this.areGlobal) {
				getGlobalMBasis().adoptValues(this.getLocalMBasis());
			} else {
				//parent.markDirty();
				getParentAxes().updateGlobal();
				getParentAxes().getGlobalMBasis().setToGlobalOf(this.localMBasis, this.globalMBasis);
				/*if(this.debug) {	
					System.out.println("Global Rotation post: \n" + getGlobalMBasis().rotation);
				}*/

				this.globalChirality = this.getGlobalMBasis().chirality;
				this.localChirality = this.getLocalMBasis().chirality;
			}
		}
		globalChirality = getGlobalMBasis().chirality;
		localChirality = getLocalMBasis().chirality;
		dirty = false;
	} 
	
	public void markChildScalesDirty() {
		for(DependencyReference<AxisDependency> axesRef : dependentsRegistry) {
			Object axes = axesRef.get(); 
			if(axes != null) {
				if(AffineAxes.class.isAssignableFrom(axes.getClass()))
					((AffineAxes)axes).markScaleDirty();
			}
		}
	}

	public void markChildReflectionDirty(int flipFlag) {
		for(DependencyReference<AxisDependency> axesRef : dependentsRegistry) {
			Object axes = axesRef.get(); 
			if(axes != null) {
				if(AffineAxes.class.isAssignableFrom(axes.getClass()))
					((AffineAxes)axes).markChildScalesDirty();
			}
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
	
	/** 
	 * @param val if set to false, axes will not be reorthonormalized on update. If set to true, axes will 
	 * be reorthogonalized. By default, this is set to true;
	 */
	public void setOrthoNormalityConstraint(boolean val) {
		forceOrthoNormality = val;
		this.markDirty();
	}
	
	@Override
	public AffineAxes getLocalOf(AbstractAxes input) {
		this.updateGlobal();
			AffineBasis newBasis = new AffineBasis();
			this.getGlobalMBasis().setToLocalOf(input.getGlobalMBasis(), newBasis);
			return new AffineAxes(
				newBasis, 
				((AffineAxes)input).forceOrthoNormality,
				null); 		
	}	
	
	
	
	/**
	 *  Given an input vector in this axes local coordinates, modifies the output vector's values to represent the input's position in global coordinates.
	 * @param in
	 */
	public void setToGlobalOf(SGVec_3d input, SGVec_3d output) {
		this.updateGlobal();
		if(this.forceOrthoNormality)
			this.setToOrthoNormalizedGlobalOf(input, output);
		else 
			getGlobalMBasis().setToGlobalOf(input, output);		
	}
	
	
	/**
	 *  Given a vector in this axes local coordinates, modifies the vector's values to represent its position global coordinates.
	 * @param in
	 * @return a reference to this the @param in object.
	 */
	public SGVec_3d setToGlobalOf(SGVec_3d in) {
		this.updateGlobal();
		if(this.forceOrthoNormality)
			this.setToOrthoNormalizedGlobalOf(in, in);
		else 
			getGlobalMBasis().setToGlobalOf(in, in);
		return in;
	}
	
	/** 
	 * like setToGlobalOf, but operates on the axes non-orthonormaldoppelganger
	 * @param input
	 * @param output
	 * @return a reference to these Axes, for method chaining.
	 */
	public <V extends Vec3d<?>> void setToRawGlobalOf(V input, V output) {
		this.updateGlobal();
		getGlobalMBasis().setToGlobalOf(input, output);
	}
	
	public  <V extends Vec3d<?>> V getOrthoNormalizedGlobalOf(V in) {
		V result =  (V) workingVector.copy();
		setToOrthoNormalizedGlobalOf(in, result);
		return  result;
	}
	
	public  <V extends Vec3d<?>> void setToOrthoNormalizedGlobalOf(V input, V output) {
		this.updateGlobal();		
		getGlobalMBasis().setToOrthoNormalGlobalOf(input, output);
	}

	public void setToOrthoNormalizedGlobalOf(sgRayd input, sgRayd output) {
		this.updateGlobal();
		this.setToOrthoNormalizedGlobalOf(input.p1(), output.p1());
		this.setToOrthoNormalizedGlobalOf(input.p2(), output.p2());
	}

	public void setToRawGlobalOf(sgRayd input, sgRayd output) {
		this.updateGlobal();
		this.setToRawGlobalOf(input.p1(), output.p1());
		this.setToRawGlobalOf(input.p2(), output.p2());
	}
	
	/**
	 * 
	 * @param xHeading new global xHeading
	 * @param yHeading new global yHeading
	 * @param zHeading new gloabl zHeading
	 * @param flipOn axis to ignore on rotation adjustment if chirality changes. 0 = x, 1= y, 2 =z;
	 */
	public  <V extends Vec3d<?>> void setHeadings(V xHeading, V yHeading, V zHeading, int autoFlip) {
		this.markDirty();
		this.updateGlobal();
		V localX = (V) xHeading.copy(); localX.set(0,0,0);
		 V localY = (V) localX.copy(); V localZ = (V)localX.copy();
		V tempX =(V) xHeading.copy(); 
		V tempY = (V) yHeading.copy(); 
		V tempZ = (V) zHeading.copy(); 
		if(this.getParentAxes() != null) {
			this.getParentAxes().getGlobalMBasis().setToLocalOf(tempX.add(this.getParentAxes().getGlobalMBasis().translate), localX); 
			this.getParentAxes().getGlobalMBasis().setToLocalOf(tempY.add(this.getParentAxes().getGlobalMBasis().translate), localY);
			this.getParentAxes().getGlobalMBasis().setToLocalOf(tempZ.add(this.getParentAxes().getGlobalMBasis().translate), localZ);
		}
		if(autoFlip >= 0) {
			this.flipFlag = autoFlip;

			if(autoFlip == 0) {

				Rot newRot = new Rot(this.getParentAxes().getGlobalMBasis().getOrthonormalYHead(), this.getParentAxes().getGlobalMBasis().getOrthonormalZHead(), yHeading, zHeading);//new Rot(this.getGlobalMBasis().yBase, this.getGlobalMBasis().zBase, localY, localZ);/
				this.getLocalMBasis().rotation = this.getParentAxes().getGlobalMBasis().getLocalOfRotation(newRot);//this.getParentAxes().getGlobalMBasis().rotation.applyInverseTo(newRot.applyTo(this.getParentAxes().getGlobalMBasis().rotation));
			} else if( autoFlip == 1) {
				Rot newRot = new Rot(this.getParentAxes().getGlobalMBasis().getOrthonormalXHead(), this.getParentAxes().getGlobalMBasis().getOrthonormalZHead(), xHeading, zHeading);
				this.getLocalMBasis().rotation = this.getParentAxes().getGlobalMBasis().getLocalOfRotation(newRot);
			} else{// if(autoFlip == 2){
				Rot newRot = new Rot(this.getParentAxes().getGlobalMBasis().getOrthonormalXHead(), this.getParentAxes().getGlobalMBasis().getOrthonormalYHead(), xHeading, yHeading);
				this.getLocalMBasis().rotation = this.getParentAxes().getGlobalMBasis().getLocalOfRotation(newRot);
			}
		}
		this.getLocalMBasis().setXHeading(localX, false);
		this.getLocalMBasis().setYHeading(localY, false);
		this.getLocalMBasis().setZHeading(localZ, true);
		this.markDirty();
		this.updateGlobal();	
	}
	
	public AffineAxes getRawGlobalCopy() {
		this.updateGlobal();
		AffineAxes rawGlobalCopy = 
				new AffineAxes(
						this.getGlobalMBasis(), 
						false,
						null);
		rawGlobalCopy.getLocalMBasis().adoptValues(this.globalMBasis);
		rawGlobalCopy.markDirty();
		rawGlobalCopy.updateGlobal();
		return rawGlobalCopy;
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
		
	
	public sgRayd x_() {
		this.updateGlobal();
		if(this.forceOrthoNormality) {
			return x_norm_();
		} else 
			return this.getGlobalMBasis().getXRay();
	}


	public sgRayd y_() {
		this.updateGlobal();  	
		if(this.forceOrthoNormality) {
			return y_norm_();
		} else 
			return this.getGlobalMBasis().getYRay();
	}

	public sgRayd z_() {
		this.updateGlobal();  
		if(this.forceOrthoNormality) {
			return z_norm_();
		} else 
			return this.getGlobalMBasis().getZRay();
	}

	public sgRayd x_norm_() {
		this.updateGlobal();  
		xTemp.p1().set(this.getGlobalMBasis().getOrigin()); xTemp.heading(this.getGlobalMBasis().getOrthonormalXHead());
		return xTemp;
	}

	public sgRayd y_norm_() {
		this.updateGlobal();  
		yTemp.p1().set(this.getGlobalMBasis().getOrigin()); yTemp.heading(this.getGlobalMBasis().getOrthonormalYHead());
		return yTemp;
	}

	public sgRayd z_norm_() {
		this.updateGlobal();  
		zTemp.p1().set(this.getGlobalMBasis().getOrigin()); zTemp.heading(this.getGlobalMBasis().getOrthonormalZHead());
		return zTemp;
	}

	public sgRayd x_raw_() {
		this.updateGlobal();  
		return this.getGlobalMBasis().getXRay();
	}

	public sgRayd y_raw_() {
		this.updateGlobal();  
		return this.getGlobalMBasis().getYRay();
	}

	public sgRayd z_raw_() {
		this.updateGlobal();  
		return this.getGlobalMBasis().getZRay();
	}
	
	public sgRayd ly_raw_() {
		return this.getLocalMBasis().getYRay();
	}

	public sgRayd lz_raw_() {
		return this.getLocalMBasis().getZRay();
	}

	public sgRayd lx_norm_() {
		return this.getLocalMBasis().getXRay();
	}

	public sgRayd ly_norm_() {
		return this.getLocalMBasis().getYRay();
	}

	public sgRayd lz_norm_() {
		return this.getLocalMBasis().getZRay();
	}
	

	public sgRayd lx_raw_() {
		return this.getLocalMBasis().getXRay();
	}

	/**
	 * @return a vector representing this frame's orientational X basis vector. Guaranteed to be Right-Handed and orthonormal. 
	 */

	public SGVec_3d orientation_X_() {
		this.updateGlobal();
		return  this.getGlobalMBasis().getRotationalXHead();
	}

	/**
	 * @return a vector representing this frame's orientational Y basis vector. Guaranteed to be Right-Handed and orthonormal. 
	 */

	public SGVec_3d   orientation_Y_() {
		this.updateGlobal();
		return  this.getGlobalMBasis().getRotationalYHead();
	}

	/**
	 * @return a vector representing this frame's orientational Z basis vector. Guaranteed to be Right-Handed and orthonormal. 
	 */

	public SGVec_3d   orientation_Z_() {
		this.updateGlobal();
		return  this.getGlobalMBasis().getRotationalZHead();
	}

	/**
	 * @return a vector representing this frame's orthonormal X basis vector. Guaranteed to be orthonormal but not necessarily right-handed. 
	 */

	public SGVec_3d   orthonormal_X_() {
		this.updateGlobal();
		return  this.getGlobalMBasis().getOrthonormalXHead();
	}
	

	/**
	 * @return a vector representing this frame's orthonormal Y basis vector. Guaranteed to be orthonormal but not necessarily right-handed. 
	 */
	public SGVec_3d   orthonormal_Y_() {
		this.updateGlobal();
		return  this.getGlobalMBasis().getOrthonormalYHead();
	}
	
	/**
	 * @return a vector representing this frame's orthonormal Z basis vector. Guaranteed to be orthonormal but not necessarily right-handed. 
	 */
	public SGVec_3d   orthonormal_Z_() {
		this.updateGlobal();
		return  this.getGlobalMBasis().getOrthonormalZHead();
	}
	

	public sgRayd getOrthoNormalizedGlobalOf(sgRayd in) {
		sgRayd result = new sgRayd(new SGVec_3d(0,0,0), new SGVec_3d(1,1,1));
		setToOrthoNormalizedGlobalOf(in, result);
		return result;
	}
	
	public int getGlobalChirality() {
		this.updateGlobal();
		return this.globalChirality;
	}

	public int getLocalChirality() {
		this.updateGlobal();
		return this.localChirality;
	}	
	
	public void rotateAboutX(double angle, boolean orthonormalized) {
		this.updateGlobal();
		Rot xRot = new Rot(getGlobalMBasis().getOrthonormalXHead(), angle);		
		this.rotateBy(xRot);
		this.markDirty();
	}

	public void rotateAboutY(double angle, boolean orthonormalized) {
		this.updateGlobal();	
		Rot yRot = new Rot(getGlobalMBasis().getOrthonormalYHead(), angle); 
		this.rotateBy(yRot);
		this.markDirty();
	}

	public void rotateAboutZ(double angle, boolean orthonormalized) {
		this.updateGlobal();
		Rot zRot = new Rot(getGlobalMBasis().getOrthonormalZHead(), angle);
		this.rotateBy(zRot);
		this.markDirty();
	}
	
	public void scaleBy(double scaleX, double scaleY, double scaleZ) {
		this.updateGlobal();
		this.getLocalMBasis().scaleXBy(scaleX);
		this.getLocalMBasis().scaleYBy(scaleY);
		this.getLocalMBasis().scaleZBy(scaleZ);
		this.markDirty();
		this.updateGlobal();
	}
	
	public void scaleXBy(double scale) {
		this.getLocalMBasis().scaleXBy(scale);
		this.markDirty();
		this.updateGlobal();
	}

	public void scaleYBy(double scale) {
		this.getLocalMBasis().scaleYBy(scale);
		this.markDirty();
		this.updateGlobal();
		//}
	}	

	public void scaleZBy(double scale) {
		this.updateGlobal();
		this.getLocalMBasis().scaleZBy(scale);
		this.markDirty();
		this.updateGlobal();
	}


	public void scaleXTo(double scale) {		
		if(!this.forceOrthoNormality) {
			this.updateGlobal();
			this.getGlobalMBasis().scaleXTo(scale);
			if(this.getParentAxes() != null)
				this.getParentAxes().setToLocalOf(this.getGlobalMBasis(), this.getLocalMBasis());
			else
				this.getLocalMBasis().adoptValues(getGlobalMBasis());
			this.markDirty();
			this.updateGlobal();
		}
	}

	public void scaleYTo(double scale) {		
		if(!this.forceOrthoNormality) {
			this.updateGlobal();
			this.getGlobalMBasis().scaleYTo(scale);
			if(this.getParentAxes() != null)
				this.getParentAxes().setToLocalOf(this.getGlobalMBasis(), this.getLocalMBasis());
			else
				this.getLocalMBasis().adoptValues(getGlobalMBasis());
			this.markDirty();
			this.updateGlobal();
		}
	}	

	public void scaleZTo(double scale) {
		if(!this.forceOrthoNormality) {
			this.updateGlobal();
			this.getGlobalMBasis().scaleZTo(scale);
			if(this.getParentAxes() != null)
				this.getParentAxes().setToLocalOf(this.getGlobalMBasis(), this.getLocalMBasis());
			else
				this.getLocalMBasis().adoptValues(getGlobalMBasis());
			this.markDirty();
			this.updateGlobal();
		}
	}
	
	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		JSONObject thisAxes = super.getSaveJSON(saveManager);
		thisAxes.setBoolean("forceOrthoNormality", this.forceOrthoNormality);
		return thisAxes;
	}

	
	@Override
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		SGVec_3d x = new SGVec_3d(j.getJSONObject("bases").getJSONArray("x"));
		SGVec_3d y = new SGVec_3d(j.getJSONObject("bases").getJSONArray("y"));
		SGVec_3d z =  new SGVec_3d(j.getJSONObject("bases").getJSONArray("z"));
		
		this.forceOrthoNormality = j.getBoolean("forceOrthoNormality");
		this.getLocalMBasis().setShearXBaseTo(x, false);
		this.getLocalMBasis().setShearYBaseTo(y, false);
		this.getLocalMBasis().setShearZBaseTo(z, false);
		super.loadFromJSONObject(j,l);
	}

	
	public AffineBasis getGlobalMBasis() {
		return (AffineBasis)globalMBasis;
	}
	
	public AffineBasis getLocalMBasis() {
		return (AffineBasis)localMBasis;
	}
	
	public AffineAxes getParentAxes() {
		return (AffineAxes)super.getParentAxes();
	}

}
