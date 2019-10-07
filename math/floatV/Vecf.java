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

/** Encapsulates a general vector. Allows chaining operations by returning a reference to itself in all modification methods. See
 * {@link SGVec_2f} and {@link SGVec_3f} for specific implementations.
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
	float dot (T v);

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
	T div(float n);

	
	
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
	boolean epsilonEquals (T other, float epsilon);

	/** First scale a supplied vector, then add it to this vector.
	 * @param v addition vector
	 * @param scalar for scaling the addition vector */
	T mulAdd (T v, float scalar);



}
