package sceneGraph.math;

import data.CanLoad;
import data.JSONArray;
import data.JSONObject;
import sceneGraph.IKVector;

public interface Vec3d<T extends Vec3d<T>> extends Vecd<T> {
		


	/** Sets the components of this vector to 0
	 * @return This vector for chaining */
	T setZero ();	
		
	/** Sets this vector from the given vector
	 * @param v The vector
	 * @return This vector for chaining */
	T set (Vec3d v);
	
	/** Sets this vector from the given vector
	 * @param v The vector */
	T set (double[] v);
	
	/** First scale a supplied vector, then add it to this vector.
	 * @param v addition vector
	 * @param mulVec vector by whose values the addition vector will be scaled */
	T mulAdd (T v, T mulVec);
	
	/** makes a copy of this vector and scales it by the given value, then returns that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default T multCopy (double s) {
		T cv = this.copy();
		return cv.mult(s);
	};
	
	/** Sets this vector from the given vector
	 * @param x
	 * @param y
	 * @param z
	 * @return This vector for chaining */
	T set (double x, double y, double z);
	
	T add(double x, double y, double z); 
	
	T add(double[] v);
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec3d v) {
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
	public default void adoptValuesOf(Vec3f v) {
		setX_(v.getX());
		setY_(v.getY());
		setZ_(v.getZ());
	}
	
	
	/**
	 * sets this vector's x component to the input value
	 * @param x
	 */
	public void setX_(double x);
	
	/**
	 * sets this vector's y component to the input value
	 * @param y
	 */
	public void setY_(double y);
	
	/**
	 * sets this vector's y component to the input value
	 * @param z
	 */
	public void setZ_(double z);
	
	
	/**
	 * @return the X component of this vector. 
	 */
	public double getX();
	/**
	 * @return the Y component of this vector. 
	 */
	public double getY();
	/**
	 * @return the Z component of this vector. 
	 */
	public double getZ();
	
	/**
	 * @return a copy of this Vector cast to a single precision analog.
	 */
	public Vec3f toSGVec3f();

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
