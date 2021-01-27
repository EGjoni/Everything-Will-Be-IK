package math.floatV;

import asj.LoadManager;
import asj.data.JSONObject;

public class CartesianAxes extends AbstractAxes {

	
	public CartesianAxes(AbstractBasis globalBasis, AbstractAxes parent) {
		super(globalBasis, parent);
	}
	public CartesianAxes(Vec3f<?> origin, Vec3f<?> inX, Vec3f<?> inY, Vec3f<?> inZ,
			AbstractAxes parent) {
		super(origin, inX, inY, inZ, parent, true);
		createTempVars(origin);

		areGlobal = true;		

		localMBasis = new CartesianBasis(origin, inX, inY, inZ);
		globalMBasis = new CartesianBasis(origin, inX, inY, inZ);
		
		Vec3f<?> o = origin.copy(); o.set(0,0,0);
		Vec3f<?> i = o.copy(); i.set(1,1,1);	

		if(parent != null) {
			this.setParent(parent);
		} 	else {
			this.areGlobal = true;
		}
		this.markDirty();
		this.updateGlobal();
	}
	
	public sgRayf x_() {
		this.updateGlobal();
		return this.getGlobalMBasis().getXRay();
	}


	public sgRayf y_() {
		this.updateGlobal();
		return this.getGlobalMBasis().getYRay();
	}

	public sgRayf z_() {
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

	@Override
	public <B extends AbstractBasis> B getLocalOf(B input) {
		CartesianBasis newBasis = new CartesianBasis((CartesianBasis)input);
		getGlobalMBasis().setToLocalOf(input, newBasis);
		return (B)newBasis;
	}
	
}
