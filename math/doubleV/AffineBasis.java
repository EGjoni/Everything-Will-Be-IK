package sceneGraph.math.doubleV;



import sceneGraph.math.floatV.Vec3f;

public class AffineBasis extends AbstractBasis {

	/**
	 * FIXME:  magnitudes should always remain positive! 
	 * Use determinants to determine handedness.
	 */

	/**
	 * xHeading, yHeading, and zHeading represent the direction of this vector's bases
	 * relative to their parent. The orthogonality of these is dependent on the ancestor's
	 * scaling. If all ancestors have uniform scaling, these are guaranteed to remain orthogonal. 
	 * 
	 * if orthogonality is not assured due to non-uniform parent scaling, and the user 
	 * wants to operate on these bases as if they were orthogonal, it is prudent to work
	 * with respect to the rotation of this basis instead of its headings. 
	 * 
	 * note that orthogonality should generally be assured unless this is a world-space matrix or 
	 * has been derived from a world space matrix in which orthogonality was not assured. 
	 */
	/*private SGVec_3d xHeading  = new SGVec_3d(1,0,0); 
	private SGVec_3d yHeading  = new SGVec_3d(0,1,0); 
	private SGVec_3d zHeading  = new SGVec_3d(0,0,1);*/ 

	//public boolean forceOrthonormal = false;
	public Vec3d<?> scale;

	public static final int NONE = -1;
	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;

	public boolean[] flippedAxes = new boolean[3];
	public Matrix4d reflectionMatrix = new Matrix4d();
	public Matrix4d inverseReflectionMatrix = new Matrix4d();

	protected int chirality = RIGHT;

	private Vec3d<?> scaledXHeading; 
	private Vec3d<?> scaledYHeading; 
	private Vec3d<?> scaledZHeading; 

	//private Transform3D inverseComposedTransform = new Transform3D();

	private Matrix4d composedMatrix = new Matrix4d(); 
	private Matrix4d inverseComposedMatrix = new Matrix4d();
	private Matrix4d shearScaleMatrix = new Matrix4d(); 


	private boolean reflectionInversesDirty = true;
	private boolean composedInversesDirty = true;
	private boolean orthoNormalInversesDirty = true;
	//Matrix4d tempMat = new Matrix4d();

	/**
	 * A basis is a collection of linearly independent rays + their origin. 
	 * Bases can be affine -- supporting uniform scaling, non-uniform scaling
	 * shearing, rotation, and reflection. 
	 * 
	 * rotation is handled behind the scenes as if the basis were of righthanded 
	 * chirality, even if the basis is of left-handed chirality. 
	 * 
	 * when the basis is instantiated, the initial orientation attempts to line up
	 * with the input y-axis, and x-axis (for historical reasons having mostly to do with
	 * this library's original purpose) regardless of chirality. 
	 * 
	 * if it cannot align with both y and z due to non-orthogonality, the orientaiton
	 * aligns its x to the xy plane, and its y precisely to the yheading.
	 *
	 * @param x the x ray. it's p1 value is assumed to be the base's origin. 
	 * @param y the y ray. it's p1 value is ignored for any purpose other than determining direction.
	 * @param z the z ray. it's p1 value is ignored for any purpose other than determining direction.
	 */
	public AffineBasis(sgRayd x, sgRayd y, sgRayd z) {
		super(x.p1);
		this.shearScaleMatrix.idt();
		this.translate.set((SGVec_3d) x.p1().copy());

		scale = translate.copy(); 
		this.scale.x = x.mag();
		this.scale.y = y.mag();
		this.scale.z = z.mag();
		this.scaledXHeading = xBase.copy();
		this.scaledYHeading = yBase.copy(); 
		this.scaledZHeading = zBase.copy();

		SGVec_3d xDirNew =new SGVec_3d((SGVec_3d) x.heading());
		SGVec_3d yDirNew =new SGVec_3d((SGVec_3d) y.heading()); 
		SGVec_3d zDirNew = new SGVec_3d((SGVec_3d) z.heading());   		

		this.rotation = createPrioritzedRotation(xDirNew, yDirNew, zDirNew);

		//if(shearScaleMatrix.determinant() < 0) {		
		//this.rotation = createIdealRotation(xDirNew, yDirNew, zDirNew);
		//} else {
		//this.rotation = new Rot(xBase, yBase, yDirNew, zDirNew);
		//}

		Rot inverseRot = new Rot(this.rotation.rotation.revert());

		inverseRot.applyTo(xDirNew, xDirNew);
		inverseRot.applyTo(yDirNew, yDirNew);
		inverseRot.applyTo(zDirNew, zDirNew);

		setShearXBaseTo(xDirNew, false);
		setShearYBaseTo(yDirNew, false);
		setShearZBaseTo(zDirNew, true);

	}

	public AffineBasis(Vec3d<?> origin) {
		super(origin);
		scale = translate.copy(); 
		this.scale.set(1,1,1);
		this.rotation = new Rot();
		this.shearScaleMatrix.idt();
		this.scaledXHeading = xBase.copy();
		this.scaledYHeading = yBase.copy(); 
		this.scaledZHeading = zBase.copy();
		this.refreshPrecomputed();
	}

	private Rot createPrioritzedRotation(SGVec_3d xHeading, SGVec_3d yHeading, SGVec_3d zHeading) {
		SGVec_3d tempV = new SGVec_3d();
		Rot toYX = new Rot(yBase, xBase, yHeading, xHeading); 
		toYX.applyTo(yBase, tempV);
		Rot toY = new Rot(tempV, yHeading);

		return toY.applyTo(toYX);
	}



	public AffineBasis(AffineBasis input) {
		super(input.translate);
		scale = translate.copy(); 
		this.adoptValues(input);
	}

	/**
	 * takes on the same values (not references) as the input basis. 
	 * @param in
	 */
	public void adoptValues(AffineBasis in) {
		super.adoptValues(in);
		this.shearScaleMatrix.set(in.getShearScaleMatrix());
		this.composedMatrix.set(in.getComposedMatrix());
		this.reflectionMatrix.set(in.reflectionMatrix);
		scale.set(in.scale);
		this.flippedAxes[X] = in.flippedAxes[X];
		this.flippedAxes[Y] = in.flippedAxes[Y];
		this.flippedAxes[Z] = in.flippedAxes[Z];

		this.reflectionInversesDirty = true;
		this.composedInversesDirty = true;
		this.orthoNormalInversesDirty = true;
		refreshPrecomputed();
	}

	public void orthoNormalize() {
		if(flippedAxes[X]) shearScaleMatrix.setColumn(X, -xBase.x, -xBase.y, -xBase.z, 0);
		else shearScaleMatrix.setColumn(X, xBase.x, xBase.y, xBase.z, 0);
		if(flippedAxes[Y]) shearScaleMatrix.setColumn(Y, -yBase.x, -yBase.y, -yBase.z, 0);
		else shearScaleMatrix.setColumn(Y, yBase.x, yBase.y, yBase.z, 0);
		if(flippedAxes[Z]) shearScaleMatrix.setColumn(Z, -zBase.x, -zBase.y, -zBase.z, 0);
		else shearScaleMatrix.setColumn(Z, zBase.x, zBase.y, zBase.z, 0);

		refreshPrecomputed();
	}

	@Override
	public AffineBasis copy() {
		return new AffineBasis(this);
	}
	Matrix4d tempMatrix = new Matrix4d();

	/**
	 * sets the values of local_output such that
	 * 
	 * this.getGlobalOf(local_output) == global_input.  
	 *  
	 * @param input
	 */

	public void setToLocalOf(AffineBasis global_input, AffineBasis local_output) {

		///if a matrix is inverted, reflection should be computed by Reflection *Matrix. 
		//if a matrix is NOT inverted, reflection should be computed by Matrix * Reflection.	

		this.rotation.applyInverseTo(global_input.rotation, local_output.rotation); 
		local_output.composedMatrix.setToMulOf(this.getInverseComposedMatrix(), global_input.composedMatrix);
		this.setToChiralityModifiedRotationOf(local_output.rotation, local_output.rotation);
		//local_output.rotation.set(currentRot);
		//Rot postModRot = this.rotation.applyTo(currentRot);

		local_output.composedMatrix.getColumn(X, arrVec1);		
		local_output.rotation.rotation.applyInverseTo(arrVec1, arrVec2);
		SGVec_3d arrV = new SGVec_3d(); arrV.set(arrVec2);
		//arrV.add(this.translate);
		//SGVec_3d orthonormalVer = new SGVec_3d();  this.setToOrthoNormalLocalOf(arrV, orthonormalVer);
		local_output.shearScaleMatrix.setColumn(X, arrVec2);	

		local_output.composedMatrix.getColumn(Y, arrVec1);		
		local_output.rotation.rotation.applyInverseTo(arrVec1, arrVec2);
		local_output.shearScaleMatrix.setColumn(Y, arrVec2);

		local_output.composedMatrix.getColumn(Z, arrVec1);		
		local_output.rotation.rotation.applyInverseTo(arrVec1, arrVec2);
		local_output.shearScaleMatrix.setColumn(Z, arrVec2);
		//tempMatrix.mul(this.reflectionMatrix, global_input.reflectionMatrix);
		//local_output.shearScaleMatrix.mul(local_output.shearScaleMatrix, tempMatrix);		
		local_output.translate = this.getLocalOf(global_input.translate);

		local_output.refreshPrecomputed();
	}

	/**
	 * like set to OrientationalLocalOf, but acknowledge chirality.
	 * @param globalInput
	 * @param local_output
	 */
	public void setToOrthoNormalLocalOf(AffineBasis global_input, AffineBasis local_output) {
		this.rotation.applyInverseTo(global_input.rotation, local_output.rotation);//global_input.rotation.applyToInverseOf(this.rotation);
		local_output.shearScaleMatrix.set(global_input.shearScaleMatrix);
		local_output.applyInverseRotTo(this.rotation, global_input.shearScaleMatrix, local_output.shearScaleMatrix);
		local_output.translate = this.getLocalOf(global_input.translate);
		local_output.shearScaleMatrix.setToMulOf(local_output.shearScaleMatrix, this.reflectionMatrix);
		local_output.refreshPrecomputed();		
	}

	public boolean debug = false;

	/**
	 * sets globalOutput such that the result of 
	 * this.getLocalOf(globalOutput) == localInput. 
	 * 
	 * @param localInput
	 * @param globalOutput
	 */
	public void setToGlobalOf(AffineBasis localInput, AffineBasis globalOutput) {		
		this.setToGlobalOf(localInput.translate, globalOutput.translate);
		tempMatrix.setToMulOf(this.shearScaleMatrix, localInput.composedMatrix);		
		setToChiralityModifiedRotationOf(localInput.rotation, globalOutput.rotation);
		applyInverseRotTo(globalOutput.rotation, tempMatrix, globalOutput.shearScaleMatrix);
		this.rotation.applyTo(globalOutput.rotation, globalOutput.rotation);
		globalOutput.refreshPrecomputed();
	}

	@Override
	public Rot getLocalOfRotation(Rot inRot) {	
			SGVec_3d tempV = new SGVec_3d();
			inRot.getAxis(tempV);
			this.getInverseComposedOrthoNormalMatrix().transform(tempV, tempV);		
			return new Rot(tempV, inRot.getAngle()*this.chirality);		
	}
	public Matrix4d composedOrthoNormalMatrix = new Matrix4d();
	private Matrix4d inverseComposedOrthoNormalMatrix = new Matrix4d();


	private void setToChiralityModifiedRotationOf(Rot localRot, Rot outputRot) {
		SGVec_3d tempV = new SGVec_3d();
		localRot.getAxis(tempV);
		this.reflectionMatrix.transform(tempV, tempV);
		double angle = localRot.getAngle();
		if(this.chirality == LEFT) angle *=-1;
		outputRot.set(tempV, angle);
	}


	//SGVec_3d tempV_3 = new SGVec_3d(0,0,0);

	/**
	 * @return the x-heading of the orthonormal rotation matrix of this basis.
	 * guaranteed to be orthonormal and right-handed. 
	 */
	public Vec3d<?> getRotationalXHead() {
		return this.rotation.applyToCopy(xBase);
	}

	/**
	 * @return the y-heading of the orthonormal rotation matrix of this basis.
	 * guaranteed to be orthonormal and right-handed.
	 */
	public Vec3d<?> getRotationalYHead() {
		return this.rotation.applyToCopy(yBase);
	}

	/**
	 * @return the z-heading of the orthonormal rotation matrix of this basis.
	 * guaranteed to be orthonormal and right-handed.
	 */
	public Vec3d<?> getRotationalZHead() {
		return this.rotation.applyToCopy(zBase);
	}


	/**
	 * @return the x-heading of the orthonormal rotation matrix of this basis.
	 * guaranteed to be orthonormal but not necessarily right-handed.
	 */
	public Vec3d<?> getOrthonormalXHead() {
		//setToShearXBase(workingVector);
		if(!flippedAxes[X])  
			return this.rotation.applyToCopy(xBase);
		else 
			return this.rotation.applyToCopy(SGVec_3d.mult(xBase, -1d));
	}

	/**
	 * @return the y-heading of the orthonormal rotation matrix of this basis.
	 * guaranteed to be orthonormal but not necessarily right-handed.
	 */
	public Vec3d<?> getOrthonormalYHead() {
		if(!flippedAxes[Y])  
			return this.rotation.applyToCopy(yBase);
		else 
			return this.rotation.applyToCopy(SGVec_3d.mult(yBase, -1d));
	}

	/**
	 * @return the z-heading of the orthonormal rotation matrix of this basis.
	 * guaranteed to be orthonormal but not necessarily right-handed.
	 */
	public Vec3d<?> getOrthonormalZHead() {

		if(!flippedAxes[Z])  
			return this.rotation.applyToCopy(zBase);
		else 
			return this.rotation.applyToCopy(SGVec_3d.mult(zBase, -1d));
	}

	//FIXME: Or, at least make sure I work. 

	/**
	 * like set setToOrientationalGlobalOf, but acknowledges chirality. 
	 * @param input
	 * @param output
	 */
	public <V extends Vec3d<?>> void setToOrthoNormalGlobalOf(V input, V output) {	
		if(input != null) {
			V tempV = output == input ? (V) input.copy() : output;
			reflectionMatrix.transform(tempV, tempV);
			this.rotation.applyTo(tempV, tempV);
			output.setX_(tempV.x+translate.x); 
			output.setY_(tempV.y+translate.y); 
			output.setZ_(tempV.z+translate.z);  		
		}
	}

	public void setToOrientationalGlobalOf(SGVec_3d input, SGVec_3d output) {	
		SGVec_3d tempV = new SGVec_3d();
		this.rotation.applyTo(input, tempV);
		output.setX_(tempV.x+translate.x); 
		output.setY_(tempV.y+translate.y); 
		output.setZ_(tempV.z+translate.z);  		
	}

	public <V extends Vec3d<?>> void setToGlobalOf(V input, V output) {
		V tempV = (V) output.copy();
		this.composedMatrix.transform(tempV, tempV);		
		output.set(tempV);
		output.add(this.translate); 	
	}


	public <V extends Vec3d<?>> void setToGlobalOf(V input) {
		this.setToGlobalOf(input, input);
	}

	public <V extends Vec3d<?>> V getGlobalOf(V input) {
		V result = (V) input.copy();
		this.setToGlobalOf(input, result);
		return result;
	}

	public <V extends Vec3d<?>> void setToLocalOf(V input, V output) {
		V tempV = (V) input.copy();
		tempV.set(input);
		tempV.x -= translate.x; tempV.y -= translate.y; tempV.z -= translate.z; 		

		this.getInverseComposedMatrix().transform(tempV, tempV);
		output.setX_(tempV.x); 
		output.setY_(tempV.y); 
		output.setZ_(tempV.z);
	}

	public <V extends Vec3d<?>> V getLocalOf(V global_input) {
		V result = (V) global_input.copy();
		setToLocalOf(global_input, result);
		return result;
	}

	public void setToOrientationalLocalOf(AffineBasis global_input, AffineBasis local_output) {
		this.rotation.applyInverseTo(global_input.rotation, local_output.rotation);
		local_output.shearScaleMatrix.set(global_input.shearScaleMatrix);
		local_output.applyInverseRotTo(this.rotation, local_output.shearScaleMatrix, local_output.composedMatrix);
		local_output.translate = this.getLocalOf(global_input.translate);
		local_output.refreshPrecomputed();		
	}

	/**
	 * sets output to the value of input in terms of the right-handed
	 * orthonormal basis representing this affine's rotation. 
	 * @param input
	 * @param output
	 */
	public <V extends Vec3d<?>> void setToOrientationalLocalOf(V input, V output) {
		V tempV = output == input ? (V) input.copy() : output;
		tempV.set(input).sub(this.translate);
		this.rotation.applyInverseTo(tempV, tempV);
		output.set(tempV);
	}

	/*public void setToOrthoNormalLocalOf(SGVec_3d input, SGVec_3d output) {		
		this.setToOrientationalLocalOf(input, output);		
		setTupleFromDVec(output, workingPoint);
		tempMatrix.invert(this.reflectionMatrix);
		tempMatrix.transform(this.workingPoint);
		this.setDVecFromTuple(output, workingPoint);		
	}*/

	public <V extends Vec3d<?>> void setToOrthoNormalLocalOf(V input, V output) {		
		V tempV = output == input ? (V) input.copy() : output;
		tempV.set(input); 
		this.setToOrientationalLocalOf(tempV, tempV);		
		this.getInverseReflectionMatrix().transform(tempV, tempV);
		output.set(tempV);
	}


	/*public void orthoNormalize() {
		this.scaleRotationMatrix.setAutoNormalize(true);
		this.scaleRotationMatrix.normalize();
		this.inverseScaleRotation.normalize();		
	}*/


	

	public String toString() {
		SGVec_3d tempV = new SGVec_3d();
		setToComposedXBase(tempV);
		Vec3f xh = tempV.toVec3f();

		setToComposedYBase(tempV);
		Vec3f yh = tempV.toVec3f();

		setToComposedZBase(tempV);
		Vec3f zh = tempV.toVec3f();

		float xMag = xh.mag();	
		float yMag = yh.mag();
		float zMag = zh.mag();
		//this.chirality = this.composedMatrix. ? RIGHT : LEFT;
		String chirality = this.chirality == LEFT ? "LEFT" : "RIGHT";
		String result = "-----------\n"  
				+chirality + " handed \n"
				+"origin: " + this.translate + "\n"
				+"rot Axis: " + this.rotation.getAxis().toVec3f() + ", "
				+"Angle: " + (float)Math.toDegrees(this.rotation.getAngle()) + "\n"
				+"xHead: " + xh + ", mag: " + xMag + "\n"
				+"yHead: " + yh + ", mag: " + yMag + "\n"
				+"zHead: " + zh + ", mag: " + zMag + "\n";

		return result;
	}	

	public void scaleXTo(double scale) {
		SGVec_3d shearX = new SGVec_3d(); 
		this.setToShearXBase(shearX); 
		shearX.normalize();
		this.setShearXBaseTo(shearX, true);
		scaleXBy(scale);
	}

	public void scaleYTo(double scale) {
		SGVec_3d shearY = new SGVec_3d(); 
		this.setToShearYBase(shearY); 
		shearY.normalize();
		this.setShearYBaseTo(shearY, true);
		scaleYBy(scale);
	}

	public void scaleZTo(double scale) {
		SGVec_3d shearZ = new SGVec_3d(); 
		this.setToShearZBase(shearZ); 
		shearZ.normalize();
		this.setShearZBaseTo(shearZ, true);
		scaleZBy(scale);
	}

	public void scaleXBy(double scale) {
		SGVec_3d shearX = new SGVec_3d(); 
		setToShearXBase(shearX); 
		double clampedScale = clamp(shearX.mag()*scale);
		shearX.normalize();
		shearX.mult(clampedScale);
		this.setShearXBaseTo(shearX, true);
		this.updateHeadings();

	}

	public void scaleYBy(double scale) {
		SGVec_3d shearY = new SGVec_3d(); 
		setToShearYBase(shearY); 
		double clampedScale = clamp(shearY.mag()*scale);
		shearY.normalize();
		shearY.mult(clampedScale);
		this.setShearYBaseTo(shearY, true);
		this.updateHeadings();
	}

	public void scaleZBy(double scale) {
		SGVec_3d shearZ = new SGVec_3d(); 
		setToShearZBase(shearZ); 
		double clampedScale = clamp(shearZ.mag()*scale);
		shearZ.normalize();
		shearZ.mult(clampedScale);
		this.setShearZBaseTo(shearZ, true);
		this.updateHeadings();
	}


	public Vec3d<?> getXHeading() {
		this.setToComposedXBase(scaledXHeading);
		return scaledXHeading;
	}

	public Vec3d<?> getYHeading() {
		this.setToComposedYBase(scaledYHeading);
		return scaledYHeading;
	}

	public Vec3d<?> getZHeading() {
		this.setToComposedZBase(scaledZHeading);
		return scaledZHeading;
	}

	public <V extends Vec3d<?>> void setXHeading(V newXHeading, boolean refreshMatrices) {
		double xHeadingMag = newXHeading.mag();
		xHeadingMag = clamp(xHeadingMag); 
		V modifiedXHeading = (V) newXHeading.copy();
		modifiedXHeading.normalize(); modifiedXHeading.mult(xHeadingMag);
		rotation.applyInverseTo(modifiedXHeading, modifiedXHeading);
		this.setShearXBaseTo(modifiedXHeading, refreshMatrices);
	}

	public <V extends Vec3d<?>> void setYHeading(V newYHeading, boolean refreshMatrices) {
		double yHeadingMag = newYHeading.mag();
		yHeadingMag = clamp(yHeadingMag); 
		V modifiedYHeading = (V) newYHeading.copy();
		modifiedYHeading.normalize(); modifiedYHeading.mult(yHeadingMag);
		rotation.applyInverseTo(modifiedYHeading, modifiedYHeading);	 		
		this.setShearYBaseTo(modifiedYHeading, refreshMatrices);
	}

	public <V extends Vec3d<?>> void setZHeading(V newZHeading, boolean refreshMatrices) {
		double zHeadingMag = newZHeading.mag();
		zHeadingMag = clamp(zHeadingMag); 
		V modifiedZHeading = (V) newZHeading.copy();
		modifiedZHeading.normalize(); modifiedZHeading.mult(zHeadingMag);
		rotation.applyInverseTo(modifiedZHeading, modifiedZHeading);	 		
		this.setShearZBaseTo(modifiedZHeading, refreshMatrices);
	}

	public <V extends Vec3d<?>> void setXHeading(V newXHeading) { 
		setXHeading(newXHeading, true);
	}

	public <V extends Vec3d<?>> void setYHeading(V newYHeading) { 
		setXHeading(newYHeading, true);  
	}


	public<V extends Vec3d<?>>  void setZHeading(V newZHeading) { 
		setXHeading(newZHeading, true);
	}

	/*public void updateScaledHeadings() {
	setToXBase(scaledXHeading);  		
	setToYBase(scaledYHeading);		
	setToZBase(scaledZHeading);
}*/



	protected double clamp(double val) {
		if(val>= 0)
			return Math.max(val, 0.0000000000001d);
		else 
			return Math.min(val, -0.0000000000001d);
	}

	public sgRayd getInverseXRay() {
		SGVec_3d inverseX = new SGVec_3d();
		Matrix4d updatedInverseComposed = this.getInverseComposedMatrix();
		inverseX.setX_(updatedInverseComposed.val[M00]); 
		inverseX.setY_(updatedInverseComposed.val[M10]); 
		inverseX.setZ_(updatedInverseComposed.val[M20]);

		sgRayd inverseXRay = new sgRayd(SGVec_3d.mult(this.translate, -1), null); 
		inverseXRay.heading(inverseX);

		return inverseXRay; 
	}

	public sgRayd getInverseYRay() {
		SGVec_3d inverseY = new SGVec_3d();
		Matrix4d updatedInverseComposed = this.getInverseComposedMatrix();
		inverseY.setX_(updatedInverseComposed.val[M01]); 
		inverseY.setY_(updatedInverseComposed.val[M11]); 
		inverseY.setZ_(updatedInverseComposed.val[M21]);

		sgRayd inverseYRay = new sgRayd(SGVec_3d.mult(this.translate, -1), null); 
		inverseYRay.heading(inverseY);
		return inverseYRay; 
	}

	public sgRayd getInverseZRay() {
		SGVec_3d inverseZ = new SGVec_3d();
		Matrix4d updatedInverseComposed = this.getInverseComposedMatrix();
		inverseZ.setX_(updatedInverseComposed.val[M02]); 
		inverseZ.setY_(updatedInverseComposed.val[M12]); 
		inverseZ.setZ_(updatedInverseComposed.val[M22]);
		sgRayd inverseZRay = new sgRayd(SGVec_3d.mult(this.translate, -1), null); 
		inverseZRay.heading(inverseZ);
		return inverseZRay; 
	}
	


	/**sets the input Tuple3d to have the values
	 * of this matrix's xbasis 
	 */
	public void setToComposedXBase(Vec3d<?> vec){
		vec.setX_(composedMatrix.val[M00]);
		vec.setY_(composedMatrix.val[M10]); 
		vec.setZ_(composedMatrix.val[M20]);
	}

	/**sets the input Tuple3d to have the values
	 * of this matrix's pre-rotation ybasis 
	 */
	public void setToComposedYBase(Vec3d<?> vec){
		vec.setX_(composedMatrix.val[M01]); 
		vec.setY_(composedMatrix.val[M11]); 
		vec.setZ_(composedMatrix.val[M21]);
	}

	/**sets the input Tuple3d to have the values
	 * of this matrix's pre-rotation zbasis 
	 */
	public void setToComposedZBase(Vec3d<?> vec){
		vec.setX_(composedMatrix.val[M02]); 
		vec.setY_(composedMatrix.val[M12]); 
		vec.setZ_(composedMatrix.val[M22]);
	}


	/**
	 *sets @param vec to the direction and magnitude of the x axis prior to rotation.
	 */
	public <V extends Vec3d<?>> void setToShearXBase(V vec){
		vec.x = shearScaleMatrix.val[M00];
		vec.y = shearScaleMatrix.val[M10]; 
		vec.z = shearScaleMatrix.val[M20];
	}

	/**
	 *sets @param vec to the direction and magnitude of the y axis prior to rotation.
	 */
	public <V extends Vec3d<?>> void setToShearYBase(V vec){
		vec.x = shearScaleMatrix.val[M01]; 
		vec.y = shearScaleMatrix.val[M11]; 
		vec.z = shearScaleMatrix.val[M21];
	}

	/**
	 *sets @param vec to the direction and magnitude of the y axis prior to rotation.
	 */
	public <V extends Vec3d<?>> void setToShearZBase(V vec){
		vec.x = shearScaleMatrix.val[M02]; 
		vec.y = shearScaleMatrix.val[M12]; 
		vec.z = shearScaleMatrix.val[M22];
	}

	/**sets the matrix's xbasis according to this vector. 
	 * @param compose if true, the cached data for this Basis is recomputed after setting the matrix.  
	 */
	public <V extends Vec3d<?>> void setShearXBaseTo(V vec, boolean compose){
		shearScaleMatrix.val[M00] = vec.x; 
		shearScaleMatrix.val[M10] = vec.y; 
		shearScaleMatrix.val[M20] = vec.z;
		if(compose) {
			refreshPrecomputed();
		}
	}

	/**sets the matrix's ybasis according to this vector. 
	 * @param compose if true, the cached data for this Basis is recomputed after setting the matrix.  
	 */
	public <V extends Vec3d<?>> void setShearYBaseTo(V vec, boolean compose){
		shearScaleMatrix.val[M01] = vec.x; 
		shearScaleMatrix.val[M11] = vec.y; 
		shearScaleMatrix.val[M21] = vec.z;
		if(compose) {
			refreshPrecomputed();
		}
	}

	/**sets the matrix's zbasis according to this vector. 
	 * @param compose if true, the cached data for this Basis is recomputed after setting the matrix.  
	 */
	public <V extends Vec3d<?>> void setShearZBaseTo(V vec, boolean compose){
		shearScaleMatrix.val[M02] = vec.x; 
		shearScaleMatrix.val[M12] = vec.y; 
		shearScaleMatrix.val[M22] = vec.z;
		if(compose) {
			refreshPrecomputed();
		}
	}


	public void updateHeadings() {
		scaledXHeading.x = composedMatrix.val[M00];
		scaledXHeading.y = composedMatrix.val[M10];
		scaledXHeading.z = composedMatrix.val[M20];

		scaledYHeading.x = composedMatrix.val[M01];
		scaledYHeading.y = composedMatrix.val[M11];
		scaledYHeading.z = composedMatrix.val[M21];

		scaledZHeading.x = composedMatrix.val[M02];
		scaledZHeading.y = composedMatrix.val[M12];
		scaledZHeading.z = composedMatrix.val[M22];

		/*setToShearXBase(workingVector);
	if(xBase.dot(workingVector) < 0) flippedAxes[X] = true;
	else flippedAxes[X] = false;

	setToShearYBase(workingVector);
	if(yBase.dot(workingVector) < 0) flippedAxes[Y] = true;
	else flippedAxes[Y] = false;

	setToShearZBase(workingVector);
	if(zBase.dot(workingVector) < 0) flippedAxes[Z] = true;
	else flippedAxes[Z] = false;*/

	}


	Quaternion tempQuat = new Quaternion();
	public void refreshPrecomputed() {

		//this.shearScaleTransform.set(shearScaleMatrix);
		applyRotTo(this.rotation, this.shearScaleMatrix, this.composedMatrix);
		//this.composedTransform.set(composedMatrix);
		//int oldDeterminant = this.chirality;
		
				
		if(this.composedMatrix.determinant() > 0)
			this.chirality = RIGHT; 
		else 
			this.chirality = LEFT;
		this.updateHeadings();
		this.updateRays();
		//if(oldDeterminant != this.chirality)
		this.updateChirality();
		applyRotTo(this.rotation, this.reflectionMatrix, this.composedOrthoNormalMatrix);
		//this.composedOrthonormalTransform.set(composedOrthoNormalMatrix);
		orthoNormalInversesDirty = true;
		composedInversesDirty = true;
		reflectionInversesDirty = true;
	}

	public void applyRotTo(Rot rotation, Matrix4d inputMatrix, Matrix4d outputMatrix) {		

		inputMatrix.getColumn(X, arrVec1); 
		rotation.rotation.applyTo(arrVec1, arrVec2);
		outputMatrix.setColumn(X, arrVec2);

		inputMatrix.getColumn(Y, arrVec1); 
		rotation.rotation.applyTo(arrVec1, arrVec2);
		outputMatrix.setColumn(Y, arrVec2);

		inputMatrix.getColumn(Z, arrVec1); 
		rotation.rotation.applyTo(arrVec1, arrVec2);
		outputMatrix.setColumn(Z, arrVec2);

		outputMatrix.val[M33] = 1;
	}


	private void applyInverseRotTo(Rot rotation, Matrix4d inputMatrix, Matrix4d outputMatrix) {
		inputMatrix.getColumn(X, arrVec1); 
		rotation.rotation.applyInverseTo(arrVec1, arrVec2);
		outputMatrix.setColumn(X, arrVec2);

		inputMatrix.getColumn(Y, arrVec1); 
		rotation.rotation.applyInverseTo(arrVec1, arrVec2);
		outputMatrix.setColumn(Y, arrVec2);

		inputMatrix.getColumn(Z, arrVec1); 
		rotation.rotation.applyInverseTo(arrVec1, arrVec2);
		outputMatrix.setColumn(Z, arrVec2);

		outputMatrix.val[M33] = 1;
	}

	double [] arrVec1 = new double[4];
	double [] arrVec2 = new double[4];

	private void updateChirality() {
		setFlipArrayForMatrix(this.composedMatrix, this.flippedAxes, this.rotation);
		arrVec1[X] = flippedAxes[X] ? -1 : 1; arrVec1[Y] = 0; arrVec1[Z] = 0; arrVec1[3] = 0;
		reflectionMatrix.setColumn(0, arrVec1);

		arrVec1[X] = 0; arrVec1[Y] = flippedAxes[Y] ? -1 : 1; arrVec1[Z] = 0;
		reflectionMatrix.setColumn(1, arrVec1);

		arrVec1[X] = 0; arrVec1[Y] = 0; arrVec1[Z]= flippedAxes[Z] ? -1 : 1;
		reflectionMatrix.setColumn(2, arrVec1);

		reflectionMatrix.val[M33] = 1;	
	}

	public void setFlipArrayForMatrix(Matrix4d forMatrix, boolean[] flipArray, Rot rotation) {
		SGVec_3d tempV = new SGVec_3d();
		double[] vecArr = new double[4]; 
		forMatrix.getColumn(Z, vecArr); 
		tempV.x = vecArr[X]; tempV.y = vecArr[Y]; tempV.z =  vecArr[Z];
		forMatrix.getColumn(Y, vecArr); 
		tempV.x = vecArr[X]; tempV.y = vecArr[Y]; tempV.z =  vecArr[Z];
		SGVec_3d tempVec = new SGVec_3d(); 
		rotation.applyTo(xBase, tempVec);		
		forMatrix.getColumn(X, vecArr); 
		tempV.x = vecArr[X]; tempV.y = vecArr[Y]; tempV.z =  vecArr[Z];		

		double dot = tempVec.dot(tempV);
		if( dot < 0) {		
			flipArray[X] = true;
		} else {
			flipArray[X] = false;
		}

		forMatrix.getColumn(Y, vecArr); 
		tempV.x = vecArr[X]; tempV.y = vecArr[Y]; tempV.z =  vecArr[Z];
		rotation.applyTo(yBase, tempVec);
		if(tempVec.dot(tempV) < 0) {
			flipArray[Y] = true;
		}
		else flipArray[Y] = false;

		forMatrix.getColumn(Z, vecArr); 
		tempV.x = vecArr[X]; tempV.y = vecArr[Y]; tempV.z =  vecArr[Z];
		rotation.applyTo(zBase, tempVec);
		if(tempVec.dot(tempV) < 0) {
			flipArray[Z] = true;
		}
		else flipArray[Z] = false;		
	}


	private void updateRays() {		
		SGVec_3d tempV = new SGVec_3d();
		xRay.setP1(this.translate); 		
		yRay.setP1(this.translate);			
		zRay.setP1(this.translate);

		setToComposedXBase(tempV);
		xRay.heading(tempV);
		setToComposedYBase(tempV);
		yRay.heading(tempV);
		setToComposedZBase(tempV);
		zRay.heading(tempV);		
	}


	/*public String typeString(Transform3D input) {
	if(input == null) input = composedTransform;
	int type = input.getBestType();
	String result = "";
	if ((type & Transform3D.ZERO)             	  > 0 ) result += " ZERO";
	if ((type & Transform3D.IDENTITY)             > 0 ) result +=" IDENTITY";
	if ((type & Transform3D.SCALE)                > 0 ) result +=" SCALE";
	if ((type & Transform3D.TRANSLATION)          > 0 ) result +=" TRANSLATION";
	if ((type & Transform3D.ORTHOGONAL)           > 0 ) result +=" ORTHOGONAL";
	if ((type & Transform3D.RIGID)                > 0 ) result +=" RIGID";
	if ((type & Transform3D.CONGRUENT)            > 0 ) result +=" CONGRUENT";
	if ((type & Transform3D.AFFINE)               > 0 ) result +=" AFFINE";
	if ((type & Transform3D.NEGATIVE_DETERMINANT) > 0 ) result +=" NEGATIVE_DETERMINANT";

	return result;
}*/

	public void setIdentity() {
		this.scale.x = 1; this.scale.y = 1; this.scale.z = 1; 
		this.scaledXHeading = xBase.copy();
		this.scaledYHeading = yBase.copy(); 
		this.scaledZHeading = zBase.copy();
		this.rotation = new Rot();
		this.translate.x = 0; this.translate.y = 0; this.translate.z = 0;
		this.xRay.p1.set(this.translate); this.xRay.p1(xBase);
		this.yRay.p1.set(this.translate); this.yRay.p1(yBase); 
		this.zRay.p1.set(this.translate); this.zRay.p1(zBase);
		this.composedMatrix.idt();
		this.shearScaleMatrix.idt();
		refreshPrecomputed();
	}



	public void createMultiDimMatrixFromMat3d(Matrix3d mat3d, double[][] outputMultiDimMatrix) {
		outputMultiDimMatrix[0][0] = mat3d.val[M00];   outputMultiDimMatrix[1][0] = mat3d.val[M01];  outputMultiDimMatrix[2][0] = mat3d.val[M02];
		outputMultiDimMatrix[0][1] = mat3d.val[M10];   outputMultiDimMatrix[1][1] = mat3d.val[M11];  outputMultiDimMatrix[2][1] = mat3d.val[M12];
		outputMultiDimMatrix[0][2] = mat3d.val[M20];   outputMultiDimMatrix[1][2] = mat3d.val[M21];  outputMultiDimMatrix[2][2] = mat3d.val[M22];		
	}

	public void createMultiDimMatrixFromMat4d(Matrix4d mat4d, double[][] outputMultiDimMatrix) {
		outputMultiDimMatrix[0][0] = mat4d.val[M00];   outputMultiDimMatrix[1][0] = mat4d.val[M01];  outputMultiDimMatrix[2][0] = mat4d.val[M02];
		outputMultiDimMatrix[0][1] = mat4d.val[M10];   outputMultiDimMatrix[1][1] = mat4d.val[M11];  outputMultiDimMatrix[2][1] = mat4d.val[M12];
		outputMultiDimMatrix[0][2] = mat4d.val[M20];   outputMultiDimMatrix[1][2] = mat4d.val[M21];  outputMultiDimMatrix[2][2] = mat4d.val[M22];		
	}

	public void createMat3dFromMultiDimMatrix(double[][] multiDimMatrix, Matrix3d outputMat3d) {

		outputMat3d.val[M00] = multiDimMatrix[0][0];  outputMat3d.val[M01] = multiDimMatrix[1][0];  outputMat3d.val[M02] = multiDimMatrix[2][0]; 
		outputMat3d.val[M10] = multiDimMatrix[0][1];  outputMat3d.val[M11] = multiDimMatrix[1][1];  outputMat3d.val[M12] = multiDimMatrix[2][1]; 
		outputMat3d.val[M20] = multiDimMatrix[0][2];  outputMat3d.val[M21] = multiDimMatrix[1][2];  outputMat3d.val[M22] = multiDimMatrix[2][2]; 

	}

	public void createMat4dFromMultiDimMatrix(double[][] multiDimMatrix, Matrix4d outputMat4d) {

		outputMat4d.val[M00] = multiDimMatrix[0][0];  outputMat4d.val[M01] = multiDimMatrix[1][0];  outputMat4d.val[M02] = multiDimMatrix[2][0]; 
		outputMat4d.val[M10] = multiDimMatrix[0][1];  outputMat4d.val[M11] = multiDimMatrix[1][1];  outputMat4d.val[M12] = multiDimMatrix[2][1]; 
		outputMat4d.val[M20] = multiDimMatrix[0][2];  outputMat4d.val[M21] = multiDimMatrix[1][2];  outputMat4d.val[M22] = multiDimMatrix[2][2]; 
		outputMat4d.val[M33] = 1;

	}

	public Matrix4d getComposedMatrix() {
		return composedMatrix;
	}

	public Matrix4d getInverseComposedMatrix() {
		if(composedInversesDirty) {
			this.inverseComposedMatrix.toInverseOf(composedMatrix);
			composedInversesDirty = false;			
		}
		return this.inverseComposedMatrix;
	}

	public Matrix4d getInverseReflectionMatrix() {
		if(reflectionInversesDirty) {
			this.inverseReflectionMatrix.toInverseOf(this.reflectionMatrix);
			reflectionInversesDirty = false;			
		}
		return this.inverseReflectionMatrix;
	}


	private Matrix4d getInverseComposedOrthoNormalMatrix() {
		if(orthoNormalInversesDirty) {
			this.inverseComposedOrthoNormalMatrix.toInverseOf(composedOrthoNormalMatrix);
			orthoNormalInversesDirty = false;			
		}
		return this.inverseComposedOrthoNormalMatrix;
	}


	public Matrix4d getShearScaleMatrix() {
		return this.shearScaleMatrix;
	}

	public static final int 
	M00 = 0, M01 = 4, M02 = 8, M03 = 1, 
	M10 = 1, M11 = 5, M12 = 9, M13 = 13,
	M20 = 2, M21 = 6, M22 = 10, M23 = 14, 
	M30 = 3, M31 = 7, M32 = 11, M33 = 15;


	public static final int 
	mM00 = 0, mM01 = 3, mM02 = 6,
	mM10 = 1, mM11 = 4, mM12 = 7, 
	mM20 = 2, mM21 = 5, mM22 = 8; 

}