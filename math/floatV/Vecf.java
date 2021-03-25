/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package math.floatV;

import math.Vec;
import math.floatV.Vec3f;

/** Encapsulates a general vector. Allows chaining operations by returning a reference to itself in all modification methods. See
 * {@link SGVec_2f} and {@link SGVec_3f} for specific imlementations.
 * @author Xoppa */
public interface Vecf<T extends Vecf<T>> extends Vec<T> {	

	

	/** @return The euclidean length */
	float mag ();

	/** This method is faster than {@link Vecf#mag()} because it avoids calculating a square root. It is useful for comparisons,
	 * but not for getting exact lengths, as the return value is the square of the actual length.
	 * @return The squared euclidean length */
	float magSq ();

	/** Limits the length of this vector, based on the desired maximum length.
	 * @param limit desired maximum length for this vector
	 * @return this vector for chaining */
	 T limit (float limit);

	/** Limits the length of this vector, based on the desired maximum length squared.
	 * <p />
	 * This method is slightly faster than limit().
	 * @param limit2 squared desired maximum length for this vector
	 * @return this vector for chaining
	 * @see #magSq() */
	 T limitSq (float limit2);

	/** Sets the length of this vector. Does nothing is this vector is zero.
	 * @param len desired length for this vector
	 * @return this vector for chaining */
	 T setMag (float len);

	/** Sets the length of this vector, based on the square of the desired length. Does nothing is this vector is zero.
	 * <p />
	 * This method is slightly faster than setLength().
	 * @param len2 desired square of the length for this vector
	 * @return this vector for chaining
	 * @see #magSq() */
	 T setMagSq (float len2);

	/** Clamps this vector's length to given min and max values
	 * @param min Min length
	 * @param max Max length
	 * @return This vector for chaining */
	 T clamp (float min, float max);



	/** @param v The other vector
	 * @return The dot product between this and the other vector */
	 <V extends Vecf<?>> float dot (V v);

	/**
	 * ( begin auto-generated from SGVec_3f_div.xml )
	 *
	 * Divides a vector by a scalar or divides one vector by another.
	 *
	 * ( end auto-generated )
	 *
	 * @webref Vecf:method
	 * @usage web_application
	 * @brief Divide a vector by a scalar
	 * @param n the number by which to divide the vector
	 */
	 T  div(float n);

	
	
	/** Scales this vector by a scalar
	 * @param scalar The scalar
	 * @return This vector for chaining */
	 T mult (float scalar);


	/** @param v The other vector
	 * @return the distance between this and the other vector */
	 float dist (T v);

	/** This method is faster than {@link Vecf#dist(Vecf)} because it avoids calculating a square root. It is useful for
	 * comparisons, but not for getting accurate distances, as the return value is the square of the actual distance.
	 * @param v The other vector
	 * @return the squared distance between this and the other vector */
	 float distSq (T v);

	/** Linearly interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is stored
	 * in this vector.
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	 T lerp (T target, float alpha);


	/*/** Sets this vector to the unit vector with a random direction
	 * @return This vector for chaining 
	T setToRandomDirection ();*/

	/** @return Whether this vector is a unit length vector */
	boolean isUnit ();

	/** @return Whether this vector is a unit length vector within the given margin. */
	boolean isUnit (final float margin);

	/** @return Whether this vector is a zero vector */
	boolean isZero ();

	/** @return Whether the length of this vector is smaller than the given margin */
	boolean isZero (final float margin);

	/** @return true if this vector is in line with the other vector (either in the same or the opposite direction) */
	 boolean isOnLine (T other, float epsilon);

	/** @return true if this vector is in line with the other vector (either in the same or the opposite direction) */
	 boolean isOnLine (T other);

	/** @return true if this vector is collinear with the other vector ({@link #isOnLine(Vecf, float)} &&
	 *         {@link #hasSameDirection(Vecf)}). */
	 boolean isCollinear (T other, float epsilon);

	/** @return true if this vector is collinear with the other vector ({@link #isOnLine(Vecf)} &&
	 *         {@link #hasSameDirection(Vecf)}). */
	 boolean isCollinear (T other);

	/** @return true if this vector is opposite collinear with the other vector ({@link #isOnLine(Vecf, float)} &&
	 *         {@link #hasOppositeDirection(Vecf)}). */
	 boolean isCollinearOpposite (T other, float epsilon);

	/** @return true if this vector is opposite collinear with the other vector ({@link #isOnLine(Vecf)} &&
	 *         {@link #hasOppositeDirection(Vecf)}). */
	 boolean isCollinearOpposite (T other);

	/** @return Whether this vector is perpendicular with the other vector. True if the dot product is 0. */
	 boolean isPerpendicular (T other);

	/** @return Whether this vector is perpendicular with the other vector. True if the dot product is 0.
	 * @param epsilon a positive small number close to zero */
	 boolean isPerpendicular (T other, float epsilon);

	/** @return Whether this vector has similar direction compared to the other vector. True if the normalized dot product is > 0. */
	 boolean hasSameDirection (T other);

	/** @return Whether this vector has opposite direction compared to the other vector. True if the normalized dot product is < 0. */
	 boolean hasOppositeDirection (T other);

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @param other
	 * @param epsilon
	 * @return whether the vectors have fuzzy equality. */
	 <V extends Vecf<?>>boolean epsilonEquals (V other, float epsilon);

	/** First scale a supplied vector, then add it to this vector.
	 * @param v addition vector
	 * @param scalar for scaling the addition vector */
	<V extends Vecf<?>> T mulAdd (V v, float scalar);


	/** Sets the components of this vector to 0
	 * @return This vector for chaining */
	 T setZero ();	
		

	
	/** Sets this vector from the given vector
	 * @param v The vector */
	 T set (float[] v);
	
	/** First scale a supplied vector, then add it to this vector.
	 * @param v addition vector
	 * @param mulVec vector by whose values the addition vector will be scaled */
	 <V extends Vecf<?>> T mulAdd (V v, V mulVec);
	/** makes a copy of this vector and scales it by the given value, then returns that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default  T multCopy (float s) {
		T cv = this.copy();
		return cv.mult(s);
	}
	
	/** Sets this vector from the given vector
	 * @param x
	 * @param y
	 * @param z
	 * @return This vector for chaining */
	 T set (float x, float y, float z);
	
	 T add(float x, float y, float z); 
	
	 T add(float[] v);
	
	
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
	 * @return the X component of this vector. 
	 */
	public float getX();
	/**
	 * @return the Y component of this vector. 
	 */
	public float getY();
	
	/**
	 * @return the Y component of this vector. 
	 */
	public float getZ();
	
	
	/**
	 * @return a copy of this Vector cast to a single precision analog.
	 */
	public <V extends Vec3f<?>>V toVec3f();

	 <V extends Vecf<?>> T crs(V vector); 

	
	/** @return a copy of this vector */
	 T copy ();		
	
	/** Adds the given vector to this vector
	 * @param v The vector
	 * @return This vector for chaining */
	 <V extends Vecf<?>> T add(V v);	

	/** make a copy of this vector and add the given vector to it, then return that copy.
	 * @param v The vector
	 * @return The resulting vector */
	public default  <V extends Vecf<?>>  T addCopy (V v) {
		T cv = this.copy();
		return cv.add(v);
	}
	
	
	/** makes a copy of this vector and sets it to the cross product between it and the input vector,
	 *  then returns the copy
	 * @param vector The other vector
	 * @return The copied vector for chaining */
	public default  <V extends Vecf<?>>  T crossCopy(V vector) {
		T  c = this.copy();
		return c.crs(vector);
	}

	/** Subtracts the given vector from this vector.
	 * @param v The vector
	 * @return This vector for chaining */
	 <V extends Vecf<?>> T  sub (V v);		

	/** make a copy of this vector and subtract the given vector from it, then return that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default  <V extends Vecf<?>> T subCopy (V v) {
		T cv = this.copy();
		return cv.sub(v);
	}

	/** Scales this vector by another vector
	 * @return This vector for chaining */
	 <V extends Vecf<?>> T mult (V v);	
	
	/** makes a copy of this vector and multiplies it componentWise by the given vector, then returns that copy.
	 * @param v The vector
	 * @return  the resulting vector */
	default  <V extends Vecf<?>> T multCopy (V v) {
		T cv = this.copy();
		return  cv.mult(v);
	}
	
	/** Normalizes this vector. Does nothing if it is zero.
	 * @return This vector for chaining */
	public T normalize ();		
	
	public  <V extends Vec3f<?>> T add (V vector);

}
