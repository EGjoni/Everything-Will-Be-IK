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

package sceneGraph.math.doubleV;

import java.io.Serializable;

import sceneGraph.math.floatV.SGVec_2f;
import sceneGraph.math.floatV.Vec2f;

//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 * @author badlogicgames@gmail.com */
public class SGVec_2d implements Serializable, Vec2d<SGVec_2d> {
	private static final long serialVersionUID = 913902788239530931L;

	public final static SGVec_2d X = new SGVec_2d(1, 0);
	public final static SGVec_2d Y = new SGVec_2d(0, 1);
	public final static SGVec_2d Zero = new SGVec_2d(0, 0);

	/** the x-component of this vector **/
	public double x;
	/** the y-component of this vector **/
	public double y;

	/** Constructs a new vector at (0,0) */
	public SGVec_2d () {
	}

	/** Constructs a vector with the given components
	 * @param x The x-component
	 * @param y The y-component */
	public SGVec_2d (double x, double y) {
		this.x = x;
		this.y = y;
	}

	/** Constructs a vector from the given vector
	 * @param v The vector */
	public SGVec_2d (SGVec_2d v) {
		set(v);
	}

	@Override
	public SGVec_2d copy () {
		return new SGVec_2d(this);
	}

	public static double len (double x, double y) {
		return (double)Math.sqrt(x * x + y * y);
	}

	@Override
	public double mag () {
		return (double)Math.sqrt(x * x + y * y);
	}

	public static double len2 (double x, double y) {
		return x * x + y * y;
	}

	@Override
	public double magSq () {
		return x * x + y * y;
	}

	@Override
	public SGVec_2d set (SGVec_2d v) {
		x = v.x;
		y = v.y;
		return this;
	}
	

	/** Sets the components of this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining */
	public SGVec_2d set (double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}

	@Override
	public SGVec_2d sub (SGVec_2d v) {
		x -= v.x;
		y -= v.y;
		return this;
	}

	/** Substracts the other vector from this vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return This vector for chaining */
	public SGVec_2d sub (double x, double y) {
		this.x -= x;
		this.y -= y;
		return this;
	}

	@Override
	public SGVec_2d normalize () {
		double len = mag();
		if (len != 0) {
			x /= len;
			y /= len;
		}
		return this;
	}

	@Override
	public SGVec_2d add (SGVec_2d v) {
		x += v.x;
		y += v.y;
		return this;
	}

	/** Adds the given components to this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining */
	public SGVec_2d add (double x, double y) {
		this.x += x;
		this.y += y;
		return this;
	}

	public static double dot (double x1, double y1, double x2, double y2) {
		return x1 * x2 + y1 * y2;
	}

	@Override
	public double dot (SGVec_2d v) {
		return x * v.x + y * v.y;
	}

	public double dot (double ox, double oy) {
		return x * ox + y * oy;
	}

	@Override
	public SGVec_2d mult (double scalar) {
		x *= scalar;
		y *= scalar;
		return this;
	}

	/** Multiplies this vector by a scalar
	 * @return This vector for chaining */
	public SGVec_2d mult (double x, double y) {
		this.x *= x;
		this.y *= y;
		return this;
	}

	@Override
	public SGVec_2d mult (SGVec_2d v) {
		this.x *= v.x;
		this.y *= v.y;
		return this;
	}

	@Override
	public SGVec_2d mulAdd (SGVec_2d vec, double scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		return this;
	}

	@Override
	public SGVec_2d mulAdd (SGVec_2d vec, SGVec_2d mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		return this;
	}

	public static double dst (double x1, double y1, double x2, double y2) {
		final double x_d = x2 - x1;
		final double y_d = y2 - y1;
		return (double)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	@Override
	public double dist (SGVec_2d v) {
		final double x_d = v.x - x;
		final double y_d = v.y - y;
		return (double)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	/** @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector */
	public double dst (double x, double y) {
		final double x_d = x - this.x;
		final double y_d = y - this.y;
		return (double)Math.sqrt(x_d * x_d + y_d * y_d);
	}

	public static double dst2 (double x1, double y1, double x2, double y2) {
		final double x_d = x2 - x1;
		final double y_d = y2 - y1;
		return x_d * x_d + y_d * y_d;
	}

	@Override
	public double distSq (SGVec_2d v) {
		final double x_d = v.x - x;
		final double y_d = v.y - y;
		return x_d * x_d + y_d * y_d;
	}

	/** @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector */
	public double dst2 (double x, double y) {
		final double x_d = x - this.x;
		final double y_d = y - this.y;
		return x_d * x_d + y_d * y_d;
	}

	@Override
	public SGVec_2d limit (double limit) {
		return limitSq(limit * limit);
	}

	@Override
	public SGVec_2d limitSq (double limit2) {
		double len2 = magSq();
		if (len2 > limit2) {
			return mult((double)Math.sqrt(limit2 / len2));
		}
		return this;
	}

	@Override
	public SGVec_2d clamp (double min, double max) {
		final double len2 = magSq();
		if (len2 == 0f) return this;
		double max2 = max * max;
		if (len2 > max2) return mult((double)Math.sqrt(max2 / len2));
		double min2 = min * min;
		if (len2 < min2) return mult((double)Math.sqrt(min2 / len2));
		return this;
	}

	@Override
	public SGVec_2d setMag (double len) {
		return setMagSq(len * len);
	}

	@Override
	public SGVec_2d setMagSq (double len2) {
		double oldLen2 = magSq();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : mult((double)Math.sqrt(len2 / oldLen2));
	}

	/** Converts this {@code Vector2} to a string in the format {@code (x,y)}.
	 * @return a string representation of this object. */
	@Override
	public String toString () {
		return "(" + x + "," + y + ")";
	}


	/** Left-multiplies this vector by the given matrix
	 * @param mat the matrix
	 * @return this vector */
	public SGVec_2d mul (Matrix3d mat) {
		double x = this.x * mat.val[0] + this.y * mat.val[3] + mat.val[6];
		double y = this.x * mat.val[1] + this.y * mat.val[4] + mat.val[7];
		this.x = x;
		this.y = y;
		return this;
	}

	/** Calculates the 2D cross product between this and the given vector.
	 * @param v the other vector
	 * @return the cross product */
	public double crs (SGVec_2d v) {
		return this.x * v.y - this.y * v.x;
	}

	/** Calculates the 2D cross product between this and the given vector.
	 * @param x the x-coordinate of the other vector
	 * @param y the y-coordinate of the other vector
	 * @return the cross product */
	public double crs (double x, double y) {
		return this.x * y - this.y * x;
	}


	/** @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise) */
	public double angleRad () {
		return (double)Math.atan2(y, x);
	}

	/** @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.) */
	public double angleRad (SGVec_2d reference) {
		return (double)Math.atan2(crs(reference), dot(reference));
	}

	
	/** Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param radians The angle in radians to set. */
	public SGVec_2d setAngleRad (double radians) {
		this.set(mag(), 0f);
		this.rotateRad(radians);

		return this;
	}

	
	/** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param radians the angle in radians */
	public SGVec_2d rotateRad (double radians) {
		double cos = (double)Math.cos(radians);
		double sin = (double)Math.sin(radians);

		double newX = this.x * cos - this.y * sin;
		double newY = this.x * sin + this.y * cos;

		this.x = newX;
		this.y = newY;

		return this;
	}

	/** Rotates the Vector2 by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise. */
	public SGVec_2d rotate90 (int dir) {
		double x = this.x;
		if (dir >= 0) {
			this.x = -y;
			y = x;
		} else {
			this.x = y;
			y = -x;
		}
		return this;
	}

	@Override
	public SGVec_2d lerp (SGVec_2d target, double alpha) {
		final double invAlpha = 1.0f - alpha;
		this.x = (x * invAlpha) + (target.x * alpha);
		this.y = (y * invAlpha) + (target.y * alpha);
		return this;
	}

	@Override
	public SGVec_2d interpolate (SGVec_2d target, double alpha, Interpolation interpolation) {
		return lerp(target, interpolation.apply(alpha));
	}

/*	@Override
	public Vector2 setToRandomDirection () {
		double theta = MathUtils.random(0f, MathUtils.PI2);
		return this.set(MathUtils.cos(theta), MathUtils.sin(theta));
	}*/

	/*@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + NumberUtils.doubleToIntBits(x);
		result = prime * result + NumberUtils.doubleToIntBits(y);
		return result;
	}*/

/*	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Vector2 other = (Vector2)obj;
		if (NumberUtils.doubleToIntBits(x) != NumberUtils.doubleToIntBits(other.x)) return false;
		if (NumberUtils.doubleToIntBits(y) != NumberUtils.doubleToIntBits(other.y)) return false;
		return true;
	}*/

	@Override
	public boolean epsilonEquals (SGVec_2d other, double epsilon) {
		if (other == null) return false;
		if (Math.abs(other.x - x) > epsilon) return false;
		if (Math.abs(other.y - y) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (double x, double y, double epsilon) {
		if (Math.abs(x - this.x) > epsilon) return false;
		if (Math.abs(y - this.y) > epsilon) return false;
		return true;
	}

	@Override
	public boolean isUnit () {
		return isUnit(0.000000001f);
	}

	@Override
	public boolean isUnit (final double margin) {
		return Math.abs(magSq() - 1f) < margin;
	}

	@Override
	public boolean isZero () {
		return x == 0 && y == 0;
	}

	@Override
	public boolean isZero (final double margin) {
		return magSq() < margin;
	}

	@Override
	public boolean isOnLine (SGVec_2d other) {
		return MathUtils.isZero(x * other.y - y * other.x);
	}

	@Override
	public boolean isOnLine (SGVec_2d other, double epsilon) {
		return MathUtils.isZero(x * other.y - y * other.x, epsilon);
	}

	@Override
	public boolean isCollinear (SGVec_2d other, double epsilon) {
		return isOnLine(other, epsilon) && dot(other) > 0f;
	}

	@Override
	public boolean isCollinear (SGVec_2d other) {
		return isOnLine(other) && dot(other) > 0f;
	}

	@Override
	public boolean isCollinearOpposite (SGVec_2d other, double epsilon) {
		return isOnLine(other, epsilon) && dot(other) < 0f;
	}

	@Override
	public boolean isCollinearOpposite (SGVec_2d other) {
		return isOnLine(other) && dot(other) < 0f;
	}

	@Override
	public boolean isPerpendicular (SGVec_2d vector) {
		return MathUtils.isZero(dot(vector));
	}

	@Override
	public boolean isPerpendicular (SGVec_2d vector, double epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	@Override
	public boolean hasSameDirection (SGVec_2d vector) {
		return dot(vector) > 0;
	}

	@Override
	public boolean hasOppositeDirection (SGVec_2d vector) {
		return dot(vector) < 0;
	}

	@Override
	public SGVec_2d setZero () {
		this.x = 0;
		this.y = 0;
		return this;
	}

	@Override
	public double getX_() {
		return this.x;
	}

	@Override
	public double getY_() {
		return this.y; 
	}
	

	@Override
	public void setX_(double x) {
		this.x = x;
	}

	@Override
	public void setY_(double y) {
		this.y = y;
	}

	public SGVec_2f toSGVec2f() {
		return new SGVec_2f((float)x,(float) y);
	}

	@Override
	public SGVec_2d div(double n) {
		x /= n;
		y /= n;
		return this;
	}

	@Override
	public SGVec_2d set(double[] v) {
		this.x = v[0];
		this.y = v[1];		
		return this;
	}

}
