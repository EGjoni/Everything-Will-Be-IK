package math.floatV;

import asj.*;
import asj.*;
import asj.data.*;
import math.*;
import math.floatV.*;
import math.floatV.Vec3f;


/**
 * Minimal implementation of basis transformations. 
 * Supports only righthanded orthonormal bases (no scaling, now skewing, no reflections). 
 * @author Eron Gjoni
 *
 */
public abstract class AbstractBasis {
	public static final int LEFT = -1;
	public static final int RIGHT = 1;
	
	public int chirality = RIGHT; 
	
	public static final int NONE = -1;
	public static final int X = 0;
	public static final int Y = 1;
	public static final int Z = 2;
	
	public Rot rotation = new Rot();
	public Rot inverseRotation = new Rot();
	/**
	 * a vector respresnting the translation of this basis relative to its parent. 
	 */
	public Vec3f<?> translate;
	
	
	protected Vec3f<?> xBase; //= new SGVec_3f(1,0,0); 
	protected Vec3f<?> yBase; //= new SGVec_3f(0,1,0); 
	protected Vec3f<?> zBase;// = new SGVec_3f(0,0,1); 
	
	protected sgRayf xRay;// = new sgRayf(new SGVec_3f(0,0,0), new SGVec_3f(1,0,0)); 
	protected sgRayf yRay;// = new sgRayf(new SGVec_3f(0,0,0), new SGVec_3f(0,1,0)); 
	protected sgRayf zRay;// = new sgRayf(new SGVec_3f(0,0,0), new SGVec_3f(0,0,1)); 
	
	
	/**
	 * Initialize this basis at the origin. The basis will be righthanded by default. 
	 * @param origin
	 */
	public AbstractBasis(Vec3f<?> origin) {
			translate = origin.copy();
			xBase = origin.copy(); yBase = origin.copy(); zBase = origin.copy(); 
			xBase.set(1,0,0); yBase.set(0,1,0); zBase.set(0,0,1);
			Vec3f<?> zero = origin.copy(); zero.set(0,0,0);
			xRay = new sgRayf(zero.copy(), xBase.copy());
			yRay = new sgRayf(zero.copy(), yBase.copy());
			zRay = new sgRayf(zero.copy(), zBase.copy());
			refreshPrecomputed();
	}
	
	public <T extends AbstractBasis> AbstractBasis(T input) {
		translate = input.translate.copy();
		xBase = translate.copy(); yBase = translate.copy(); zBase = translate.copy(); 
		xBase.set(1,0,0); yBase.set(0,1,0); zBase.set(0,0,1);
		Vec3f<?> zero = translate.copy(); zero.set(0,0,0);
		xRay = new sgRayf(zero.copy(), xBase.copy());
		yRay = new sgRayf(zero.copy(), yBase.copy());
		zRay = new sgRayf(zero.copy(), zBase.copy());
		this.adoptValues(input);
		
	}
	
	
	/**
	 * Initialize this basis at the origin.
	 *  The basis will be backed by a rotation object which presumes right handed chirality. 
	 *  Therefore, the rotation object will align so its local XY plane aligns with this basis' XY plane
	 *  Afterwards, it will check chirality, and if the basis isn't righthanded, this class will assume the
	 *  z-axis is the one that's been flipped. 
	 *  
	 *  If you want to manually specify which axis has been flipped 
	 *  (so that the rotation object aligns with respect to the plane formed 
	 *  by the other two basis vectors) then use the constructor dedicated for that purpose
	 * @param origin
	 * @param x basis vector direction
	 * @param y basis vector direction
	 * @param z basis vector direction
	 */
	public <V extends Vec3f<?>> AbstractBasis(V origin, V x, V y, V z) {
		this.translate = origin.copy();
		xRay = new sgRayf(origin.copy(), origin.copy());
		yRay = new sgRayf(origin.copy(), origin.copy());
		zRay = new sgRayf(origin.copy(), origin.copy());
		this.set(x.copy(), y.copy(), z.copy());
	}
	
	/**
	 * Initialize this basis at the origin defined by the base of the @param x Ray.
	 * 
	 *  The basis will be backed by a rotation object which presumes right handed chirality. 
	 *  Therefore, the rotation object will align so its local XY plane aligns with this basis' XY plane
	 *  Afterwards, it will check chirality, and if the basis isn't righthanded, this class will assume the
	 *  z-axis is the one that's been flipped.
	 *   
	 *  If you want to manually specify which axis has been flipped 
	 *  (so that the rotation object aligns with respect to the plane formed 
	 *  by the other two basis vectors) then use the constructor dedicated for that purpose
	 *  
	 *  
	 * @param x basis Ray 
	 * @param y basis Ray 
	 * @param z basis Ray 
	 */	
	public <R extends sgRayf> AbstractBasis(R x, R y, R z) {
		this.translate = x.p1().copy();		
		xRay = x.copy(); yRay= y.copy(); zRay = z.copy();		
		Vec3f<?> xDirNew =x.heading().copy();
		Vec3f<?> yDirNew =y.heading().copy();
		Vec3f<?> zDirNew = z.heading().copy(); 		
		xDirNew.normalize(); yDirNew.normalize(); zDirNew.normalize();		
		set(xDirNew, yDirNew, zDirNew);
		
	}
	
	private <V extends Vec3f<?>> void set(V x, V y, V z) {
		xBase = translate.copy(); yBase = translate.copy(); zBase = translate.copy(); 
		xBase.set(1,0,0); yBase.set(0,1,0); zBase.set(0,0,1);
		Vec3f<?> zero = translate.copy(); zero.set(0,0,0);
		xRay.setP1(zero.copy()); xRay.setP2(xBase.copy());
		yRay.setP1(zero.copy()); yRay.setP2(yBase.copy());
		zRay.setP1(zero.copy()); zRay.setP2(zBase.copy());
		this.rotation = createPrioritzedRotation(x, y, z);
		this.refreshPrecomputed();
	}
	
	/**
	 * takes on the same values (not references) as the input basis. 
	 * @param in
	 */
	public <T extends AbstractBasis> void adoptValues(T in) {
		this.translate.set(in.translate);
		this.rotation.set(in.rotation);
		xBase = translate.copy(); yBase = translate.copy(); zBase = translate.copy(); 
		xBase.set(1,0,0); yBase.set(0,1,0); zBase.set(0,0,1);
		xRay = in.xRay.copy(); yRay= in.yRay.copy(); zRay = in.zRay.copy();	
		this.refreshPrecomputed();
	}
	
	public void setIdentity() {
		this.translate.set(0,0,0);
		xBase.set(1,0,0); yBase.set(0,1,0); zBase.set(0,0,1);
		this.xRay.p1.set(this.translate); this.xRay.p2.set(xBase);
		this.yRay.p1.set(this.translate); this.yRay.p2.set(yBase); 
		this.zRay.p1.set(this.translate); this.zRay.p2.set(zBase);
		this.rotation = new Rot();
		refreshPrecomputed();
	}

	
	
	private Rot createPrioritzedRotation(Vec3f<?> xHeading, Vec3f<?> yHeading, Vec3f<?> zHeading) {		
	
			Vec3f<?> tempV = zHeading.copy(); tempV.set(0,0,0);
			Rot toYZ = new Rot(yBase, zBase, yHeading, zHeading); 
			toYZ.applyTo(yBase, tempV);
			Rot toY = new Rot(tempV, yHeading);

			return toY.applyTo(toYZ);
		/*Vec3f<?> xidt = xBase.copy(); Vec3f<?> yidt = yBase.copy();  Vec3f<?> zidt = zBase.copy();
		Vec3f<?> origin = xBase.copy(); origin.set(0,0,0);  
		
		Vec3f<?>[] from = {origin, xidt, yidt, zidt};
		Vec3f<?>[] to = {origin.copy(), xHeading, yHeading, zHeading};
		QCP alignHeads = new QCP(MathUtils.DOUBLE_ROUNDING_ERROR, MathUtils.DOUBLE_ROUNDING_ERROR);
		alignHeads.setMaxIterations(50);
		Rot rotation = alignHeads.weightedSuperpose(from, to, null, false);
		return rotation;*/
	}
	
	
	public Rot getLocalOfRotation(Rot inRot) {		
			Rot resultNew =  inverseRotation.applyTo(inRot).applyTo(rotation);						
			return resultNew;			
	}
	
	public <B extends AbstractBasis> void setToLocalOf(B global_input, B local_output) {
		local_output.translate = this.getLocalOf(global_input.translate);
		inverseRotation.applyTo(global_input.rotation, local_output.rotation); 
		
		local_output.refreshPrecomputed();
	}
	
	public void refreshPrecomputed() {
		this.rotation.setToReversion(inverseRotation);
		this.updateRays();		
	}
	
	public <V extends Vec3f<?>> V getLocalOf(V v) {
		V result = (V) v.copy(); 
		setToLocalOf(v, result);
		return result;
	}

	
	public <V extends Vec3f<?>> void setToLocalOf(V input, V output) {
		output.set(input);
		output.sub(this.translate); 
		inverseRotation.applyTo(output, output);  	
	}

	public void rotateTo(Rot newRotation) {				
		this.rotation.set(newRotation); 
		this.refreshPrecomputed();
	}

	public void rotateBy(Rot addRotation) {		
		addRotation.applyTo(this.rotation, this.rotation);
		this.refreshPrecomputed();
	}
	
	
	/**
	 * the default Basis implementation is orthonormal, 
	 * so by default this function will just set @param vec to (1,0,0),
	 * but extending (affine) classes can override this to represent the direction and magnitude of the x axis prior to rotation. 
	 */
	public <V extends Vec3f<?>> void setToShearXBase(V vec){
		vec.set(xBase);
	}

	/**
	 * the default Basis implementation is orthonormal, 
	 * so by default this function will just set @param vec to (0,1,0),
	 * but extending (affine) classes can override this to represent the direction and magnitude of the y axis prior to rotation. 
	 */
	public <V extends Vec3f<?>>void setToShearYBase(V vec){
		vec.set(yBase);
	}

	/**
	 * the default Basis implementation is orthonormal, 
	 * so by default this function will just set @param vec to (0,0,1),
	 * but extending (affine) classes can override this to represent the direction and magnitude of the z axis prior to rotation. 
	 */
	public <V extends Vec3f<?>> void setToShearZBase(V vec){
		vec.set(zBase);
	}
	
	
	/**
	 * sets globalOutput such that the result of 
	 * this.getLocalOf(globalOutput) == localInput. 
	 * 
	 * @param localInput
	 * @param globalOutput
	 */
	public <T extends AbstractBasis> void setToGlobalOf(T localInput, T globalOutput) {		
		this.rotation.applyTo(localInput.rotation, globalOutput.rotation);
		this.setToGlobalOf(localInput.translate, globalOutput.translate);		
		globalOutput.refreshPrecomputed();
	}
	
	
	public <V extends Vec3f<?>> void setToGlobalOf(V input, V output) {
		try {
		rotation.applyTo(input, output);
		output.add(this.translate);
		} catch(Exception e) {
			e.printStackTrace();
		}
 	}
	
	
	public <V extends Vec3f<?>> void translateBy(V transBy) {
		this.translate.x += transBy.x; 
		this.translate.y += transBy.y;
		this.translate.z += transBy.z;
		updateRays();
	}
	
	public <V extends Vec3f<?>> void translateTo(V newOrigin) {
		this.translate.x = newOrigin.x;
		this.translate.y = newOrigin.y;
		this.translate.z = newOrigin.z; 
		updateRays();
	}
	
	public sgRayf getXRay() {
		return xRay; 
	}

	public sgRayf getYRay() {
		return yRay; 
	}

	public sgRayf getZRay() {
		return zRay; 
	}
	
	public Vec3f<?> getXHeading() {
		return this.xRay.heading();
	}

	public Vec3f<?> getYHeading() {
		return this.yRay.heading();
	}

	public Vec3f<?> getZHeading() {
		return this.zRay.heading();
	}

	public Vec3f<?> getOrigin() {
		return translate;
	}
	
	/**
	 * true if the input axis should be multiplied by negative one after rotation. 
	 * By default, this always returns false. But can be overriden for more advanced implementations
	 * allowing for reflection transformations. 
	 * @param axis
	 * @return true if axis should be flipped, false otherwise. Default is false. 
	 */
	public boolean isAxisFlipped(int axis) {
		return false; 
	}
	
	/**
	 * @return a precomputed inverse of the rotation represented by this basis object.
	 */
	public Rot getInverseRotation() {
		return this.inverseRotation;
	}
	

	
	private void updateRays() {		
		xRay.setP1(this.translate); xRay.p2.set(xBase); 		
		yRay.setP1(this.translate); yRay.p2.set(yBase);			
		zRay.setP1(this.translate); zRay.p2.set(zBase);

		rotation.applyTo(xRay.p2, xRay.p2);
		rotation.applyTo(yRay.p2, yRay.p2);
		rotation.applyTo(zRay.p2, zRay.p2);
		
		xRay.p2.add(this.translate);
		yRay.p2.add(this.translate);
		zRay.p2.add(this.translate);	
	}
		
	public abstract AbstractBasis copy();
	
	public String toString() {
		Vec3f xh = xRay.heading().toVec3f();

		Vec3f yh = yRay.heading().toVec3f();

		Vec3f zh = zRay.heading().toVec3f();

		float xMag = xh.mag();	
		float yMag = yh.mag();
		float zMag = zh.mag();
		//this.chirality = this.composedMatrix. ? RIGHT : LEFT;
		String chirality = this.chirality == LEFT ? "LEFT" : "RIGHT";
		String result = "-----------\n"  
				+chirality + " handed \n"
				+"origin: " + this.translate.toVec3f() + "\n"
				+"rot Axis: " + this.rotation.getAxis().toVec3f() + ", "
				+"Angle: " + (float)MathUtils.toDegrees(this.rotation.getAngle()) + "\n"
				+"xHead: " + xh + ", mag: " + xMag + "\n"
				+"yHead: " + yh + ", mag: " + yMag + "\n"
				+"zHead: " + zh + ", mag: " + zMag + "\n";

		return result;
	}	
	
}
