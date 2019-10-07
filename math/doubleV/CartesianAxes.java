package math.doubleV;

import asj.LoadManager;
import asj.data.JSONObject;

public class CartesianAxes extends AbstractAxes {

	
	public CartesianAxes(AbstractBasis globalBasis, AbstractAxes parent) {
		super(globalBasis, parent);
	}
	public CartesianAxes(Vec3d<?> origin, Vec3d<?> inX, Vec3d<?> inY, Vec3d<?> inZ,
			AbstractAxes parent) {
		super(origin, inX, inY, inZ, parent, true);
		createTempVars(origin);

		areGlobal = true;		

		localMBasis = new CartesianBasis(origin, inX, inY, inZ);
		globalMBasis = new CartesianBasis(origin, inX, inY, inZ);
		
		Vec3d<?> o = origin.copy(); o.set(0,0,0);
		Vec3d<?> i = o.copy(); i.set(1,1,1);	

		if(parent != null) {
			this.setParent(parent);
		} 	else {
			this.areGlobal = true;
		}
		this.markDirty();
		this.updateGlobal();
	}
	
	public sgRayd x_() {
		this.updateGlobal();
		return this.getGlobalMBasis().getXRay();
	}


	public sgRayd y_() {
		this.updateGlobal();
		return this.getGlobalMBasis().getYRay();
	}

	public sgRayd z_() {
		this.updateGlobal();
		return this.getGlobalMBasis().getZRay();
	}
	
	@Override
	public  <A extends AbstractAxes> boolean equals(A ax) {
		this.updateGlobal();
		ax.updateGlobal();

		boolean composedRotationsAreEquivalent = getGlobalMBasis().rotation.equals(ax.globalMBasis.rotation);
		boolean originsAreEquivalent = getGlobalMBasis().getOrigin().equals(ax.origin_());

		return composedRotationsAreEquivalent && originsAreEquivalent;
	}

	@Override
	public CartesianAxes getGlobalCopy() {
		return new CartesianAxes(getGlobalMBasis(), this.getParentAxes());
	}


	@Override
	public AbstractAxes relativeTo(AbstractAxes in) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractAxes getLocalOf(AbstractAxes input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractAxes freeCopy() {
		AbstractAxes freeCopy = 
				new CartesianAxes(this.getLocalMBasis(),
						null);
		freeCopy.getLocalMBasis().adoptValues(this.localMBasis);
		freeCopy.markDirty();
		freeCopy.updateGlobal();
		return freeCopy;
	}

	
	@Override 
	public void loadFromJSONObject(JSONObject j, LoadManager l) {
		super.loadFromJSONObject(j, l);
		/*SGVec_3d origin = new SGVec_3d(j.getJSONArray("translation"));
		SGVec_3d x = new SGVec_3d(j.getJSONObject("bases").getJSONArray("x"));
		SGVec_3d y = new SGVec_3d(j.getJSONObject("bases").getJSONArray("y"));
		SGVec_3d z =  new SGVec_3d(j.getJSONObject("bases").getJSONArray("z"));
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
		super.loadFromJSONObject(j, l);*/
	} 
	
	/**
	 * Creates an exact copy of this Axes object. Attached to the same parent as this Axes object
	 * @param slipAware
	 * @return
	 */
	@Override
	public CartesianAxes attachedCopy(boolean slipAware) {
		this.updateGlobal();
		CartesianAxes copy = new CartesianAxes(getGlobalMBasis(), 
																this.getParentAxes());  
		if(!slipAware) copy.setSlipType(IGNORE);
		copy.getLocalMBasis().adoptValues(this.localMBasis);
		copy.markDirty();
		return copy;
	}
	
}
