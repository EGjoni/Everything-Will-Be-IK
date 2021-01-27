package math.floatV;

public class CartesianBasis extends AbstractBasis {

	public CartesianBasis(CartesianBasis cartesianBasis) {
		super(cartesianBasis);
	}

	public CartesianBasis(Vec3f<?> origin) {
		super(origin);
	}

	public <V extends Vec3f<?>> CartesianBasis(V origin, V inX, V inY, V inZ) {
		super(origin, inX, inY, inZ);
	}

	@Override
	public AbstractBasis copy() {
		return  new CartesianBasis(this); 
	}

}
