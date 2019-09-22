package sceneGraph.math.floatV;

import data.CanLoad;
import data.JSONArray;
import data.JSONObject;
import sceneGraph.IKVector;
import sceneGraph.math.doubleV.Vec3d;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.Vec3f;

public interface Vec3f<T extends Vec3f<T>> extends Vecf<T> {
		


	/** Sets the components of this vector to 0
	 * @return This vector for chaining */
	T setZero ();	
		
	/** Sets this vector from the given vector
	 * @param v The vector
	 * @return This vector for chaining */
	T set (Vec3f v);
	
	/** Sets this vector from the given vector
	 * @param v The vector */
	T set (float[] v);
	
	/** First scale a supplied vector, then add it to this vector.
	 * @param v addition vector
	 * @param mulVec vector by whose values the addition vector will be scaled */
	T mulAdd (T v, T mulVec);
	
	/** makes a copy of this vector and scales it by the given value, then returns that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default T multCopy (float s) {
		T cv = this.copy();
		return cv.mult(s);
	};
	
	/** Sets this vector from the given vector
	 * @param x
	 * @param y
	 * @param z
	 * @return This vector for chaining */
	T set (float x, float y, float z);
	
	T add(float x, float y, float z); 
	
	T add(float[] v);
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec3f v) {
		setX_(v.getX());
		setY_(v.getY());
		setZ_(v.getZ());
	}
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec3d v) {
		setX_((float)v.getX());
		setY_((float)v.getY());
		setZ_((float)v.getZ());
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
	
	/**
	 * sets this vector's y component to the input value
	 * @param z
	 */
	public void setZ_(float z);
	
	
	/**
	 * @return the X component of this vector. 
	 */
	public float getX();
	/**
	 * @return the Y component of this vector. 
	 */
	public float getY();
	/**
	 * @return the Z component of this vector. 
	 */
	public float getZ();
	
	/**
	 * @return a copy of this Vector cast to a single precision analog.
	 */
	public SGVec_3f toSGVec3f();

	T crs(T vector); 

	
	/** @return a copy of this vector */
	T copy ();		

	/** Adds the given vector to this vector
	 * @param v The vector
	 * @return This vector for chaining */
	T add (T v);	

	/** make a copy of this vector and add the given vector to it, then return that copy.
	 * @param v The vector
	 * @return The resulting vector */
	public default T addCopy (T v) {
		T cv = this.copy();
		return cv.add(v);
	};	
	
	
	/** makes a copy of this vector and sets it to the cross product between it and the input vector,
	 *  then returns the copy
	 * @param vector The other vector
	 * @return The copied vector for chaining */
	public default T crossCopy(T vector) {
		T  c = this.copy();
		return c.crs(vector);
	}

	/** Subtracts the given vector from this vector.
	 * @param v The vector
	 * @return This vector for chaining */
	T sub (T v);		

	/** make a copy of this vector and subtract the given vector from it, then return that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default T subCopy (T v) {
		T cv = this.copy();
		return cv.sub(v);
	};

	/** Scales this vector by another vector
	 * @return This vector for chaining */
	T mult (T v);	
	
	/** makes a copy of this vector and multiplies it componentWise by the given vector, then returns that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default T multCopy (T v) {
		T cv = this.copy();
		return cv.mult(v);
	};
	
	
	/** Normalizes this vector. Does nothing if it is zero.
	 * @return This vector for chaining */
	T normalize ();		
	
}
