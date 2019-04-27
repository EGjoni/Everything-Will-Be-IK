package sceneGraph.math;

import sceneGraph.IKVector;

public interface  Vec3f<T extends Vec3f<T>> extends Vecf<T>{
	
	
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
	 * @param v The vector */
	T set (float[] v);
	
	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	public T crs (final T vector);
	

	/** Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public T crs (float x, float y, float z);
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec3f v) {
		setX(v.getX());
		setY(v.getY());
		setZ(v.getZ());
	}
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public default void adoptValuesOf(Vec3d v) {
		setX((float)v.getX());
		setY((float)v.getY());
		setZ((float)v.getZ());
	}
	
	
	/**
	 * sets this vector's x component to the input value
	 * @param x
	 */
	public void setX(float x);
	
	/**
	 * sets this vector's y component to the input value
	 * @param y
	 */
	public void setY(float y);
	
	/**
	 * sets this vector's y component to the input value
	 * @param z
	 */
	public void setZ(float z);
	
	
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
	
	

	/** make a copy of this vector and add the given vector to it, then return that copy.
	 * @param v The vector
	 * @return The resulting vector */
	default T addCopy (T v) {
		T cv = v.copy();
		return cv.add(v);
	};	

	/** makes a copy of this vector and sets it to the cross product between it and the input vector,
	 *  then returns the copy
	 * @param vector The other vector
	 * @return The copied vector for chaining */
	default T crossCopy(T vector) {
		T  c = this.copy();
		return c.crs(vector);
	}

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
	 * @return a copy of this Vector cast to a double precision analog.
	 */
	public Vec3d toSGVec3d(); 
	
	
}
