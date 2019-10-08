package sceneGraph.math.floatV;

import sceneGraph.IKVector;
import sceneGraph.math.floatV.SGVec_2f;
import sceneGraph.math.floatV.Vec2f;
import sceneGraph.math.floatV.Vec3f;

public interface Vec2f<T extends Vec2f<T>> extends Vecf<T>{
	
	/** @return a copy of this vector */
	T copy ();
	
	/** Subtracts the given vector from this vector.
	 * @param v The vector
	 * @return This vector for chaining */
	T sub (T v);		

	/** Normalizes this vector. Does nothing if it is zero.
	 * @return This vector for chaining */
	T normalize ();

	/** Adds the given vector to this vector
	 * @param v The vector
	 * @return This vector for chaining */
	T add (T v);	

	/** Scales this vector by another vector
	 * @return This vector for chaining */
	T mult (T v);	

	/** First scale a supplied vector, then add it to this vector.
	 * @param v addition vector
	 * @param mulVec vector by whose values the addition vector will be scaled */
	T mulAdd (T v, T mulVec);

	/** Sets the components of this vector to 0
	 * @return This vector for chaining */
	T setZero ();	
	
	/** Sets this vector from the given vector
	 * @param v The vector
	 * @return This vector for chaining */
	T set (T v);
	
	/** Sets this vector from the given vector
	 * @param v The vector*/
	T set (float[] v);
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec2f v) {
		setX_(v.getX_());
		setY_(v.getY_());
	}
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec3f v) {
		setX_(v.getX());
		setY_(v.getY());
	}	
	
	/**
	 * sets this vector's x component to the input value
	 * @param x
	 */
	public void setX_(float x);
	
	/**
	 * sets this vector's y component to the input value
	 * @param y
	 */
	public void setY_(float y);
	


	/** make a copy of this vector and add the given vector to it, then return that copy.
	 * @param v The vector
	 * @return The resulting vector */
	default T addCopy (T v) {
		T cv = v.copy();
		return cv.add(v);
	};	


	/** make a copy of this vector and subtract the given vector from it, then return that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default T subCopy (T v) {
		T cv = v.copy();
		return cv.sub(v);
	};


	
	/** makes a copy of this vector and multiplies it componentWise by the given vector, then returns that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default T multCopy (T v) {
		T cv = v.copy();
		return cv.mult(v);
	};
	
	
	
	/**
	 * @return the X component of this vector. 
	 */
	public float getX_();
	/**
	 * @return the Y component of this vector. 
	 */
	public float getY_();
	
	/**
	 * @return a copy of this Vector cast to a single precision analog.
	 */
	public <V extends Vec2f> V toSGVec2f(); 
	
	
}
