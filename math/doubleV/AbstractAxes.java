package math.doubleV;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

import asj.LoadManager;
import asj.SaveManager;
import asj.Saveable;
import asj.data.JSONObject;

/**
 * @author Eron Gjoni
 */
public abstract class AbstractAxes implements AxisDependency, Saveable {
	public static final int NORMAL = 0, IGNORE = 1, FORWARD = 2;
	public static final int RIGHT = 1, LEFT = -1; 
	public static final int X =0, Y=1, Z=2; 


	public boolean debug = false;
	//protected int globalChirality = RIGHT;
	//protected int localChirality = RIGHT;

	public AbstractBasis localMBasis; 
	public AbstractBasis globalMBasis;
	private DependencyReference<AbstractAxes> parent = null;

	private int slipType = 0;
	public boolean dirty = true;
	
	//public boolean forceOrthoNormality = true; 
	

	public LinkedList<DependencyReference<AxisDependency>> dependentsRegistry = new LinkedList<DependencyReference<AxisDependency>>(); 

	protected Vec3d<?> workingVector; 

	protected boolean areGlobal = true;
	
	public <V extends Vec3d<?>> void createTempVars(V type) {
		workingVector =  type.copy(); 
		tempOrigin =  type.copy(); 
	}
	
	
	
	/**
	 * @param globalMBasis a Basis object for this Axes to adopt the vakues of
	 * @param customBases set to true if you intend to use a custom Bases class, in which case, this constructor will not initialize them.
	 */
	public <V extends Vec3d<?>> AbstractAxes(AbstractBasis globalBasis, AbstractAxes parent) {
		this.globalMBasis = globalBasis.copy(); 
		createTempVars(globalBasis.getOrigin());
		if(this.getParentAxes() != null)
			setParent(parent);
		else {
			this.areGlobal = true;
			this.localMBasis = globalBasis.copy();
		}
		
		this.updateGlobal();				
		//this.updateChiralities();
	}
	
	/**
	 * @param origin the center of this axes basis. The basis vector parameters will be automatically ADDED to the origin in order to create this basis vector.
	 * @param inX the direction of the X basis vector in global coordinates, given as an offset from this base's origin in global coordinates.   
	 * @param inY the direction of the Y basis vector in global coordinates, given as an offset from this base's origin in global coordinates.
	 * @param inZ the direction of the Z basis vector in global coordinates, given as an offset from this base's origin in global coordinates.
	 * @param forceOrthoNormality
	 * @param customBases set to true if you intend to use a custom Bases class, in which case, this constructor will not initialize them.
	 */
	public AbstractAxes(Vec3d<?> origin, Vec3d<?> inX, Vec3d<?> inY, Vec3d<?> inZ, AbstractAxes parent, boolean customBases) {
		if(!customBases) {
			globalMBasis = parent != null ? parent.getGlobalMBasis().copy() : new CartesianBasis(origin);
			localMBasis = parent != null ? parent.getLocalMBasis().copy() : new CartesianBasis(origin);
			globalMBasis.setIdentity();
			localMBasis.setIdentity();
		}		
		if(parent == null) 
			this.areGlobal = true;
		
		//this.updateChiralities();
	}

	


	public AbstractAxes getParentAxes() {
		if(this.parent == null) 
			return null;
		else 
			return this.parent.get();
	}
	
	public void updateGlobal() {
		if(this.dirty) {
			if(this.areGlobal) {
				globalMBasis.adoptValues(this.localMBasis);
			} else {
				getParentAxes().updateGlobal();				
				getParentAxes().getGlobalMBasis().setToGlobalOf(this.localMBasis, this.globalMBasis);			
			}
		}
		dirty = false;
	}

	public void debugCall() {};


	Vec3d<?> tempOrigin;
	public Vec3d<?> origin_() {
		this.updateGlobal();  
		tempOrigin.set(this.getGlobalMBasis().getOrigin());
		return tempOrigin;
	}

	/**
	 * Make a GlobalCopy of these Axes. 
	 * @return
	 */
	public abstract <A extends AbstractAxes> A getGlobalCopy();
	
	
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
		/*for(DependencyReference<AxisDependency> ad : this.dependentsRegistry) {
			ad.get().parentChangeWarning(this, oldParent, intendedParent, requestedBy);
		}*/
		forEachDependent(
				(ad) -> ad.get().parentChangeWarning(this, oldParent, intendedParent, requestedBy));
		
		
		if(intendedParent != null && intendedParent != this) {
			intendedParent.updateGlobal(); 
			intendedParent.getGlobalMBasis().setToLocalOf(globalMBasis, localMBasis);			

			if(oldParent != null) oldParent.disown(this);
			this.parent = new DependencyReference<AbstractAxes>(intendedParent);

			this.getParentAxes().registerDependent(this);
			this.areGlobal = false;
		} else {
			if(oldParent != null) oldParent.disown(this);
			this.parent = new DependencyReference<AbstractAxes>(null);
			this.areGlobal = true;
		}
		this.markDirty();
		this.updateGlobal();
		
		forEachDependent(
				(ad) -> ad.get().parentChangeCompletionNotice(this, oldParent, intendedParent, requestedBy));
		/*for(DependencyReference<AxisDependency> ad : this.dependentsRegistry) {
			ad.get().parentChangeCompletionNotice(this, oldParent, intendedParent, requestedBy);
		}*/
	}
	
	/**
	 * runs the given runnable on each dependent axis,
	 * taking advantage of the call to remove entirely any 
	 * weakreferences to elements that have been cleaned up by the garbage collector. 
	 * @param r
	 */
	public void forEachDependent(Consumer<DependencyReference<AxisDependency>> action) {		
		Iterator<DependencyReference<AxisDependency>> i = dependentsRegistry.iterator();
		while (i.hasNext()) {
			DependencyReference<AxisDependency> dr = i.next();
			if(dr.get() != null) {
				action.accept(dr);
			} else {
				i.remove();
			}		    
		}
	}
	
	public int getGlobalChirality() {
		this.updateGlobal();
		return this.getGlobalMBasis().chirality;
	}

	public int getLocalChirality() {
		this.updateGlobal();
		return this.getLocalMBasis().chirality;
	}	

	/**
	 * True if the input axis of this Axes object in global coordinates should be multiplied by negative one after rotation. 
	 * By default, this always returns false. But can be overriden for more advanced implementations
	 * allowing for reflection transformations. 
	 * @param axis
	 * @return true if axis should be flipped, false otherwise. Default is false. 
	 */
	public boolean isGlobalAxisFlipped(int axis) {
		this.updateGlobal();
		return globalMBasis.isAxisFlipped(axis);
	}
	
	/**
	 * True if the input axis of this Axes object in local coordinates should be multiplied by negative one after rotation. 
	 * By default, this always returns false. But can be overriden for more advanced implementations
	 * allowing for reflection transformations. 
	 * @param axis
	 * @return true if axis should be flipped, false otherwise. Default is false. 
	 */
	public boolean isLocalAxisFlipped(int axis) {
		return localMBasis.isAxisFlipped(axis);
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
		if(this.getParentAxes() != null) this.getParentAxes().disown(this);
		this.parent = new DependencyReference<AbstractAxes>(par);
		this.areGlobal = false;
		this.getParentAxes().registerDependent(this);
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
	public <V extends Vec3d<?>> V getGlobalOf(V in) {
		V result =  (V) in.copy();
		setToGlobalOf(in, result);
		return  result;
	}



	/**
	 *  Given a vector in this axes local coordinates, modifies the vector's values to represent its position global coordinates.
	 * @param in
	 * @return a reference to this the @param in object.
	 */
	public Vec3d<?> setToGlobalOf(Vec3d<?> in) {
		this.updateGlobal();
		getGlobalMBasis().setToGlobalOf(in, in);
		return in;
	}

	/**
	 *  Given an input vector in this axes local coordinates, modifies the output vector's values to represent the input's position in global coordinates.
	 * @param in
	 */
	public <V extends Vec3d<?>> void setToGlobalOf(V input, V output) {
		this.updateGlobal();
		getGlobalMBasis().setToGlobalOf(input, output);		
	}

	

	/**
	 *  Given an input sgRay in this axes local coordinates, modifies the output Rays's values to represent the input's in global coordinates.
	 * @param in
	 */
	public void setToGlobalOf(sgRayd input, sgRayd output) {
		this.updateGlobal();
		this.setToGlobalOf(input.p1(), output.p1());
		this.setToGlobalOf(input.p2(), output.p2());
	}

	public sgRayd getGlobalOf(sgRayd in) { 
		return new sgRayd(this.getGlobalOf( in.p1()), this.getGlobalOf( in.p2()));
	}
	
	
	

	/**
	 * returns an axis representing the global location of this axis if the input axis were its parent.
	 * @param in
	 * @return
	 */
	public abstract AbstractAxes relativeTo(AbstractAxes in);


	


	public <V extends Vec3d<?>> V getLocalOf(V in) {		
		this.updateGlobal();
		return getGlobalMBasis().getLocalOf(in);
	}


	/**
	 *  Given a vector in global coordinates, modifies the vector's values to represent its position in theseAxes local coordinates.
	 * @param in
	 * @return a reference to the @param in object. 
	 */

	public <V extends Vec3d<?>> V  setToLocalOf(V  in) {
		this.updateGlobal();
		V result = (V)in.copy();
		this.getGlobalMBasis().setToLocalOf(in, result);
		in.set(result);
		return result;
	}

	/**
	 *  Given a vector in global coordinates, modifies the vector's values to represent its position in theseAxes local coordinates.
	 * @param in
	 */

	public <V extends Vec3d<?>> void setToLocalOf(V in, V out) {
		this.updateGlobal();
		this.getGlobalMBasis().setToLocalOf(in, out);
	}

	/**
	 *  Given a sgRay in global coordinates, modifies the sgRay's values to represent its position in theseAxes local coordinates.
	 * @param in
	 */

	public void setToLocalOf(sgRayd in, sgRayd out) {
		this.setToLocalOf(in.p1(), out.p1());
		this.setToLocalOf(in.p2(), out.p2());
	}

	public void setToLocalOf(AbstractBasis input, AbstractBasis output) {
		this.updateGlobal();
		this.getGlobalMBasis().setToLocalOf(input, output);
	}

	public <R extends sgRayd> R getLocalOf(R in) {
		R result = (R) in.copy();
		result.p1.set(this.getLocalOf(in.p1())); 
		result.p2.set(this.getLocalOf(in.p2()));
		return result;
	}


	public abstract AbstractAxes getLocalOf(AbstractAxes input);	

	public abstract <B extends AbstractBasis> B getLocalOf(B input);

	public <V extends Vec3d<?>> void translateByLocal(V translate) {    
		this.updateGlobal();
		getLocalMBasis().translateBy(translate);
		this.markDirty();

	} 
	public void translateByGlobal(Vec3d<?> translate) {		
		if(this.getParentAxes() != null ) {
			this.updateGlobal();		
			this.translateTo(translate.addCopy(this.origin_()));
		} else {
			getLocalMBasis().translateBy(translate);
		}

		this.markDirty();
	}



	public void translateTo(Vec3d<?> translate, boolean slip) {
		this.updateGlobal();
		if(slip) {
			AbstractAxes tempAbstractAxes = this.getGlobalCopy();
			tempAbstractAxes.translateTo(translate);
			this.slipTo(tempAbstractAxes);
		} else {
			this.translateTo(translate);  
		}
	}

	public void translateTo(Vec3d<?> translate) {
		if(this.getParentAxes() != null ) {
			this.updateGlobal();
			getLocalMBasis().translateTo(getParentAxes().getGlobalMBasis().getLocalOf(translate));			
			this.markDirty();
		} else {
			this.updateGlobal();
			getLocalMBasis().translateTo(translate);
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
	public abstract AbstractAxes freeCopy();

	
	/**
	 *  return a ray / segment representing this Axes global x basis position and direction and magnitude
	 * @return a ray / segment representing this Axes global x basis position and direction and magnitude
	 */
	public abstract sgRayd x_();


	/**
	 *  return a ray / segment representing this Axes global y basis position and direction and magnitude
	 * @return a ray / segment representing this Axes global y basis position and direction and magnitude
	 */
	public abstract sgRayd y_();

	/**
	 *  return a ray / segment representing this Axes global z basis position and direction and magnitude
	 * @return a ray / segment representing this Axes global z basis position and direction and magnitude
	 */
	public abstract sgRayd z_();

/**
 * Creates an exact copy of this Axes object. Attached to the same parent as this Axes object
 * @param slipAware
 * @return
 */
	public abstract AbstractAxes attachedCopy(boolean slipAware);

	public void setSlipType(int type) {
		if(this.getParentAxes() != null) {
			if(type == IGNORE) {
				this.getParentAxes().dependentsRegistry.remove(this);
			} else if(type == NORMAL || type == FORWARD) {
				this.getParentAxes().registerDependent(this);
			} 
		}
		this.slipType = type;
	}

	public int getSlipType() {
		return this.slipType;
	}
	
	public void rotateAboutX(double angle, boolean orthonormalized) {
		this.updateGlobal();
		Rot xRot = new Rot(getGlobalMBasis().getXHeading(), angle);		
		this.rotateBy(xRot);
		this.markDirty();
	}

	public void rotateAboutY(double angle, boolean orthonormalized) {
		this.updateGlobal();	
		Rot yRot = new Rot(getGlobalMBasis().getYHeading(), angle); 
		this.rotateBy(yRot);
		this.markDirty();
	}

	public void rotateAboutZ(double angle, boolean orthonormalized) {
		this.updateGlobal();
		Rot zRot = new Rot(getGlobalMBasis().getZHeading(), angle);
		this.rotateBy(zRot);
		this.markDirty();
	}

	public void rotateBy(MRotation apply) {
		this.updateGlobal();		
		if(parent != null) {		
			Rot newRot = this.getParentAxes().getGlobalMBasis().getLocalOfRotation(new Rot(apply));
			this.getLocalMBasis().rotateBy(newRot);
		} else {
			this.getLocalMBasis().rotateBy(new Rot(apply));
		}		
		this.markDirty(); 
	}


	/**
	 * Rotates the bases around their origin in global coordinates
	 * @param rotation
	 */
	public void rotateBy(Rot apply) {

		this.updateGlobal();		
		if(this.getParentAxes() != null) {
			Rot newRot = this.getParentAxes().getGlobalMBasis().getLocalOfRotation(apply);
			this.getLocalMBasis().rotateBy(newRot);
		} else {
			this.getLocalMBasis().rotateBy(apply);
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
			this.getLocalMBasis().rotateBy(apply);
		}
		this.markDirty(); 
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
		this.getLocalMBasis().adoptValues(targetAxes.localMBasis);
		this.markDirty();
	}



	/**
	 * sets the bases to the Identity basis and Identity rotation relative to its parent, and translates 
	 * its origin to the parent's origin. 
	 * 
	 * be careful calling this method, as it destroys any shear / scale information. 
	 */
	public void alignToParent() {
		this.getLocalMBasis().setIdentity();
		this.markDirty();
	}

	/**
	 * rotates and translates the axes back to its parent, but maintains 
	 * its shear, translate and scale attributes.
	 */
	public void rotateToParent() {
		this.getLocalMBasis().rotateTo(new Rot(MRotation.IDENTITY));
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
		if(this.getParentAxes() != null) {
			getParentAxes().getGlobalMBasis().setToLocalOf(targetAxes.globalMBasis, localMBasis);	
		} else {
			this.getLocalMBasis().adoptValues(targetAxes.globalMBasis);
		}
		this.markDirty();
		this.updateGlobal();
	}

	public void alignOrientationTo(AbstractAxes targetAxes) {
		targetAxes.updateGlobal();
		this.updateGlobal();
		if(this.getParentAxes() != null) {
			this.getGlobalMBasis().rotateTo(targetAxes.getGlobalMBasis().rotation);
			getParentAxes().getGlobalMBasis().setToLocalOf(this.globalMBasis, this.localMBasis);
		} else {
			this.getLocalMBasis().rotateTo(targetAxes.getGlobalMBasis().rotation);
		}
		this.markDirty();
	}

	/**
	 * updates the axes object such that its global orientation 
	 * matches the given Rot object. 
	 * @param rotation
	 */
	public void setGlobalOrientationTo(Rot rotation) {
		this.updateGlobal();
		if(this.getParentAxes() != null) {
			this.getGlobalMBasis().rotateTo(rotation);
			getParentAxes().getGlobalMBasis().setToLocalOf(this.globalMBasis, this.localMBasis);
		} else {
			this.getLocalMBasis().rotateTo(rotation);
		}
		this.markDirty();
	}


	public void registerDependent(AxisDependency newDependent) {
		//Make sure we don't hit a dependency loop
		if(AbstractAxes.class.isAssignableFrom(newDependent.getClass())) {
			if(((AbstractAxes)newDependent).isAncestorOf(this)) {
				this.transferToParent(((AxisDependency)newDependent).getParentAxes());
			}			
		} 
		if(dependentsRegistry.indexOf(newDependent) == -1){
			dependentsRegistry.add(new DependencyReference<AxisDependency>(newDependent));
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
		if(this.getParentAxes() != null) {
			this.updateGlobal();
			AbstractAxes oldParent = this.getParentAxes();
			for(DependencyReference<AxisDependency> ad: this.dependentsRegistry) {
				ad.get().parentChangeWarning(this, this.getParentAxes(), null, null);
			}
			this.getLocalMBasis().adoptValues(this.globalMBasis);
			this.getParentAxes().disown(this);
			this.parent = new DependencyReference<AbstractAxes>(null);
			this.areGlobal = true;
			this.markDirty();
			this.updateGlobal();
			for(DependencyReference<AxisDependency> ad: this.dependentsRegistry) {
				ad.get().parentChangeCompletionNotice(this, oldParent, null, null);
			}
		}
	}

	public void disown(AxisDependency child) {
		dependentsRegistry.remove(child);
	}
	
	public AbstractBasis getGlobalMBasis() {
		this.updateGlobal();
		return globalMBasis;
	}
	
	public AbstractBasis getLocalMBasis() {
		return localMBasis;
	}
	
	@Override
	public JSONObject getSaveJSON(SaveManager saveManager) {
		this.updateGlobal();
		JSONObject thisAxes = new JSONObject(); 
		JSONObject shearScale = new JSONObject();
		SGVec_3d xShear = new SGVec_3d(); 
		SGVec_3d yShear = new SGVec_3d(); 
		SGVec_3d zShear = new SGVec_3d(); 

		this.getLocalMBasis().setToShearXBase(xShear);
		this.getLocalMBasis().setToShearYBase(yShear);
		this.getLocalMBasis().setToShearZBase(zShear);

		shearScale.setJSONArray("x", xShear.toJSONArray());
		shearScale.setJSONArray("y", yShear.toJSONArray());
		shearScale.setJSONArray("z", zShear.toJSONArray());
		
		thisAxes.setJSONArray("translation", (new SGVec_3d(getLocalMBasis().translate)).toJSONArray());
		thisAxes.setJSONArray("rotation", getLocalMBasis().rotation.toJsonArray());
		thisAxes.setJSONObject("bases", shearScale);

		//thisAxes.setJSONArray("flippedAxes", saveManager.primitiveArrayToJSONArray(this.getLocalMBasis().flippedAxes));
		String parentHash = "-1"; 
		if(getParentAxes() != null) parentHash = ((Saveable)getParentAxes()).getIdentityHash();
		thisAxes.setString("parent",  parentHash);
		thisAxes.setInt("slipType", this.getSlipType());
		thisAxes.setString("identityHash",  this.getIdentityHash());
		return thisAxes;
	}

	@Override
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		SGVec_3d origin = new SGVec_3d(j.getJSONArray("translation"));		
		Rot rotation = new Rot(j.getJSONArray("rotation"));
		this.getLocalMBasis().translate = origin;
		this.getLocalMBasis().rotation = rotation;
		this.getLocalMBasis().refreshPrecomputed();
		AbstractAxes par;
		try {
			par = (AbstractAxes) l.getObjectFor(AbstractAxes.class, j, "parent");
			if(par != null)
				this.setRelativeToParent(par);
			this.setSlipType(j.getInt("slipType"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


	public void axisSlipWarning(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes actualAxis, ArrayList<Object>dontWarn) {
		this.updateGlobal();
		if(this.slipType == NORMAL ) {
			if(this.getParentAxes() != null) {
				AbstractAxes globalVals = this.relativeTo(globalPriorToSlipping);
				globalVals = globalPriorToSlipping.getLocalOf(globalVals); 
				this.getLocalMBasis().adoptValues(globalMBasis);
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
	public abstract <A extends AbstractAxes> boolean equals(A ax);

	public void axisSlipWarning(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes actualAxis) {

	}

	public void axisSlipCompletionNotice(AbstractAxes globalPriorToSlipping, AbstractAxes globalAfterSlipping, AbstractAxes thisAxis) {

	}

	public void slipTo(AbstractAxes newAxisGlobal) {
		this.updateGlobal();
		AbstractAxes originalGlobal = this.getGlobalCopy();
		notifyDependentsOfSlip(newAxisGlobal); 
		AbstractAxes newVals = newAxisGlobal.freeCopy();

		if(this.getParentAxes() != null) {
			newVals = getParentAxes().getLocalOf(newVals);
		}
		this.getLocalMBasis().adoptValues(newVals.globalMBasis);
		this.dirty = true;	
		this.updateGlobal();

		notifyDependentsOfSlipCompletion(originalGlobal);
	}
	
	/**
	 * You probably shouldn't touch this unless you're implementing i/o or undo/redo. 
	 * @return
	 */
	protected DependencyReference<AbstractAxes> getWeakRefToParent() {
		return this.parent;
	}
	
	/**
	 * You probably shouldn't touch this unless you're implementing i/o or undo/redo. 
	 * @return
	 */
	protected void setWeakRefToParent(DependencyReference<AbstractAxes> parentRef) {
		this.parent = parentRef;
	}

	public void slipTo(AbstractAxes newAxisGlobal, ArrayList<Object> dontWarn) {
		this.updateGlobal();
		AbstractAxes originalGlobal = this.getGlobalCopy();
		notifyDependentsOfSlip(newAxisGlobal, dontWarn); 
		AbstractAxes newVals = newAxisGlobal.getGlobalCopy();

		if(this.getParentAxes() != null) {
			newVals = getParentAxes().getLocalOf(newAxisGlobal);
		}
		this.alignGlobalsTo(newAxisGlobal);
		this.markDirty();
		this.updateGlobal();
		
		notifyDependentsOfSlipCompletion(originalGlobal, dontWarn);
	}

	public void notifyDependentsOfSlip(AbstractAxes newAxisGlobal, ArrayList<Object> dontWarn) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {
			if(!dontWarn.contains(dependentsRegistry.get(i))) {
				AxisDependency dependant = dependentsRegistry.get(i).get();

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
				dependentsRegistry.get(i).get().axisSlipCompletionNotice(globalAxisPriorToSlipping, this.getGlobalCopy(), this);
			else 
				System.out.println("skipping: " + dependentsRegistry.get(i));
		}
	}


	public void notifyDependentsOfSlip(AbstractAxes newAxisGlobal) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {
			dependentsRegistry.get(i).get().axisSlipWarning(this.getGlobalCopy(), newAxisGlobal, this);
		}
	}

	public void notifyDependentsOfSlipCompletion(AbstractAxes globalAxisPriorToSlipping) {
		for(int i = 0; i<dependentsRegistry.size(); i++) {//AxisDependancy dependent : dependentsRegistry) {
			dependentsRegistry.get(i).get().axisSlipCompletionNotice(globalAxisPriorToSlipping, this.getGlobalCopy(), this);
		}
	}

	

	public void markDirty() {
		
		if(!this.dirty) {			
			this.dirty = true;
			this.markDependentsDirty();			
		}
		
	}

	public void markDependentsDirty() {
		forEachDependent((a) -> a.get().markDirty());
	}

	public String toString() {
		String global = "Global: " + getGlobalMBasis().toString();
		String local = "Local: " + getLocalMBasis().toString();
		return global + "\n" + local;
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
	public void setLoading(boolean loading) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isLoading() {
		
		return false;
	}
	@Override
	public void makeSaveable(SaveManager saveManager) {
		saveManager.addToSaveState(this);
		forEachDependent(				  
				(ad) -> {
					if(Saveable.class.isAssignableFrom(ad.get().getClass()))
							((Saveable)ad.get()).makeSaveable(saveManager);
				});
	}
	
	/**
	 * custom Weakreference extension for garbage collection
	 */
	public class DependencyReference<E> extends WeakReference<E> {
		public DependencyReference(E referent) {
			super(referent);
		}
		
		@Override 
		public boolean equals(Object o) {
			if(o == this) return true; 
			if(o == this.get()) return true; 
			else return false;
		}
	}
}
