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

import java.io.Serializable;

import math.floatV.Interpolation;
import math.floatV.SGVec_2f;
import math.floatV.Vec2f;

//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 * @author badlogicgames@gmail.com */
public class SGVec_2f implements Serializable, Vec2f<SGVec_2f> {
	private static final long serialVersionUID = 913902788239530931L;

	public final static SGVec_2f X = new SGVec_2f(1, 0);
	public final static SGVec_2f Y = new SGVec_2f(0, 1);
	public final static SGVec_2f Zero = new SGVec_2f(0, 0);

	/** the x-component of this vector **/
	public float x;
	/** the y-component of this vector **/
	public float y;

	/** Constructs a new vector at (0,0) */
	public SGVec_2f () {
	}

	/** Constructs a vector with the given components
	 * @param x The x-component
	 * @param y The y-component */
	public SGVec_2f (float x, float y) {
		this.x = x;
		this.y = y;
	}

	/** Constructs a vector from the given vector
	 * @param v The vector */
	public SGVec_2f (SGVec_2f v) {
		set(v);
	}

	@Override
	public SGVec_2f copy () {
		return new SGVec_2f(this);
	}

	public static float len (float x, float y) {
		return (float)MathUtils.sqrt(x * x + y * y);
	}

	@Override
	public float mag () {
		return (float)MathUtils.sqrt(x * x + y * y);
	}

	public static float len2 (float x, float y) {
		return x * x + y * y;
	}

	@Override
	public float magSq () {
		return x * x + y * y;
	}

	@Override
	public SGVec_2f set (SGVec_2f v) {
		x = v.x;
		y = v.y;
		return this;
	}
	

	/** Sets the components of this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining */
	public SGVec_2f set (float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	@Override
	public SGVec_2f sub (SGVec_2f v) {
		x -= v.x;
		y -= v.y;
		return this;
	}

	/** Substracts the other vector from this vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return This vector for chaining */
	public SGVec_2f sub (float x, float y) {
		this.x -= x;
		this.y -= y;
		return this;
	}

	@Override
	public SGVec_2f normalize () {
		float len = mag();
		if (len != 0) {
			x /= len;
			y /= len;
		}
		return this;
	}

	@Override
	public SGVec_2f add (SGVec_2f v) {
		x += v.x;
		y += v.y;
		return this;
	}

	/** Adds the given components to this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining */
	public SGVec_2f add (float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	public static float dot (float x1, float y1, float x2, float y2) {
		return x1 * x2 + y1 * y2;
	}

	@Override
	public float dot (SGVec_2f v) {
		return x * v.x + y * v.y;
	}

	public float dot (float ox, float oy) {
		return x * ox + y * oy;
	}

	@Override
	public SGVec_2f mult (float scalar) {
		x *= scalar;
		y *= scalar;
		return this;
	}

	/** Multiplies this vector by a scalar
	 * @return This vector for chaining */
	public SGVec_2f mult (float x, float y) {
		this.x *= x;
		this.y *= y;
		return this;
	}

	@Override
	public SGVec_2f mult (SGVec_2f v) {
		this.x *= v.x;
		this.y *= v.y;
		return this;
	}

	@Override
	public SGVec_2f mulAdd (SGVec_2f vec, float scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		return this;
	}

	@Override
	public SGVec_2f mulAdd (SGVec_2f vec, SGVec_2f mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		return this;
	}

	public static float dst (float x1, float y1, float x2, float y2) {
		final float x_d = x2 - x1;
		final float y_d = y2 - y1;
		return (float)MathUtils.sqrt(x_d * x_d + y_d * y_d);
	}

	@Override
	public float dist (SGVec_2f v) {
		final float x_d = v.x - x;
		final float y_d = v.y - y;
		return (float)MathUtils.sqrt(x_d * x_d + y_d * y_d);
	}

	/** @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector */
	public float dst (float x, float y) {
		final float x_d = x - this.x;
		final float y_d = y - this.y;
		return (float)MathUtils.sqrt(x_d * x_d + y_d * y_d);
	}

	public static float dst2 (float x1, float y1, float x2, float y2) {
		final float x_d = x2 - x1;
		final float y_d = y2 - y1;
		return x_d * x_d + y_d * y_d;
	}

	@Override
	public float distSq (SGVec_2f v) {
		final float x_d = v.x - x;
		final float y_d = v.y - y;
		return x_d * x_d + y_d * y_d;
	}

	/** @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector */
	public float dst2 (float x, float y) {
		final float x_d = x - this.x;
		final float y_d = y - this.y;
		return x_d * x_d + y_d * y_d;
	}

	@Override
	public SGVec_2f limit (float limit) {
		return limitSq(limit * limit);
	}

	@Override
	public SGVec_2f limitSq (float limit2) {
		float len2 = magSq();
		if (len2 > limit2) {
			return mult((float)MathUtils.sqrt(limit2 / len2));
		}
		return this;
	}

	@Override
	public SGVec_2f clamp (float min, float max) {
		final float len2 = magSq();
		if (len2 == 0f) return this;
		float max2 = max * max;
		if (len2 > max2) return mult((float)MathUtils.sqrt(max2 / len2));
		float min2 = min * min;
		if (len2 < min2) return mult((float)MathUtils.sqrt(min2 / len2));
		return this;
	}

	@Override
	public SGVec_2f setMag (float len) {
		return setMagSq(len * len);
	}

	@Override
	public SGVec_2f setMagSq (float len2) {
		float oldLen2 = magSq();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : mult((float)MathUtils.sqrt(len2 / oldLen2));
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
	public SGVec_2f mul (Matrix3f mat) {
		float x = this.x * mat.val[0] + this.y * mat.val[3] + mat.val[6];
		float y = this.x * mat.val[1] + this.y * mat.val[4] + mat.val[7];
		this.x = x;
		this.y = y;
		return this;
	}

	/** Calculates the 2D cross product between this and the given vector.
	 * @param v the other vector
	 * @return the cross product */
	public float crs (SGVec_2f v) {
		return this.x * v.y - this.y * v.x;
	}

	/** Calculates the 2D cross product between this and the given vector.
	 * @param x the x-coordinate of the other vector
	 * @param y the y-coordinate of the other vector
	 * @return the cross product */
	public float crs (float x, float y) {
		return this.x * y - this.y * x;
	}

	/** @return the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis (typically
	 *         counter-clockwise) and between 0 and 360. */
	public float angle () {
		float angle = (float)MathUtils.atan2(y, x) * MathUtils.radiansToDegrees;
		if (angle < 0) angle += 360;
		return angle;
	}

	/** @return the angle in degrees of this vector (point) relative to the given vector. Angles are towards the positive y-axis
	 *         (typically counter-clockwise.) between -180 and +180 */
	public float angle (SGVec_2f reference) {
		return (float)MathUtils.atan2(crs(reference), dot(reference)) * MathUtils.radiansToDegrees;
	}

	/** @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise) */
	public float angleRad () {
		return (float)MathUtils.atan2(y, x);
	}

	/** @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.) */
	public float angleRad (SGVec_2f reference) {
		return (float)MathUtils.atan2(crs(reference), dot(reference));
	}

	/** Sets the angle of the vector in degrees relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param degrees The angle in degrees to set. */
	public SGVec_2f setAngle (float degrees) {
		return setAngleRad(degrees * MathUtils.degreesToRadians);
	}

	/** Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param radians The angle in radians to set. */
	public SGVec_2f setAngleRad (float radians) {
		this.set(mag(), 0f);
		this.rotateRad(radians);

		return this;
	}

	/** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param degrees the angle in degrees */
	public SGVec_2f rotate (float degrees) {
		return rotateRad(degrees * MathUtils.degreesToRadians);
	}

	/** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param radians the angle in radians */
	public SGVec_2f rotateRad (float radians) {
		float cos = (float)MathUtils.cos(radians);
		float sin = (float)MathUtils.sin(radians);

		float newX = this.x * cos - this.y * sin;
		float newY = this.x * sin + this.y * cos;

		this.x = newX;
		this.y = newY;

		return this;
	}

	/** Rotates the Vector2 by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise. */
	public SGVec_2f rotate90 (int dir) {
		float x = this.x;
		if (dir >= 0) {
			this.x = -y;
			y = x;
		} else {
			this.x = y;
			y = -x;
		}
		return this;
	}


	
/*	@Override
	public Vector2 setToRandomDirection () {
		float theta = MathUtils.random(0f, MathUtils.PI2);
		return this.set(MathUtils.cos(theta), MathUtils.sin(theta));
	}*/

	/*@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + NumberUtils.floatToIntBits(x);
		result = prime * result + NumberUtils.floatToIntBits(y);
		return result;
	}*/

/*	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Vector2 other = (Vector2)obj;
		if (NumberUtils.floatToIntBits(x) != NumberUtils.floatToIntBits(other.x)) return false;
		if (NumberUtils.floatToIntBits(y) != NumberUtils.floatToIntBits(other.y)) return false;
		return true;
	}*/

	@Override
	public boolean epsilonEquals (SGVec_2f other, float epsilon) {
		if (other == null) return false;
		if (MathUtils.abs(other.x - x) > epsilon) return false;
		if (MathUtils.abs(other.y - y) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (float x, float y, float epsilon) {
		if (MathUtils.abs(x - this.x) > epsilon) return false;
		if (MathUtils.abs(y - this.y) > epsilon) return false;
		return true;
	}

	@Override
	public boolean isUnit () {
		return isUnit(0.000000001f);
	}

	@Override
	public boolean isUnit (final float margin) {
		return MathUtils.abs(magSq() - 1f) < margin;
	}

	@Override
	public boolean isZero () {
		return x == 0 && y == 0;
	}

	@Override
	public boolean isZero (final float margin) {
		return magSq() < margin;
	}

	@Override
	public boolean isOnLine (SGVec_2f other) {
		return MathUtils.isZero(x * other.y - y * other.x);
	}

	@Override
	public boolean isOnLine (SGVec_2f other, float epsilon) {
		return MathUtils.isZero(x * other.y - y * other.x, epsilon);
	}

	@Override
	public boolean isCollinear (SGVec_2f other, float epsilon) {
		return isOnLine(other, epsilon) && dot(other) > 0f;
	}

	@Override
	public boolean isCollinear (SGVec_2f other) {
		return isOnLine(other) && dot(other) > 0f;
	}

	@Override
	public boolean isCollinearOpposite (SGVec_2f other, float epsilon) {
		return isOnLine(other, epsilon) && dot(other) < 0f;
	}

	@Override
	public boolean isCollinearOpposite (SGVec_2f other) {
		return isOnLine(other) && dot(other) < 0f;
	}

	@Override
	public boolean isPerpendicular (SGVec_2f vector) {
		return MathUtils.isZero(dot(vector));
	}

	@Override
	public boolean isPerpendicular (SGVec_2f vector, float epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	@Override
	public boolean hasSameDirection (SGVec_2f vector) {
		return dot(vector) > 0;
	}

	@Override
	public boolean hasOppositeDirection (SGVec_2f vector) {
		return dot(vector) < 0;
	}

	@Override
	public SGVec_2f setZero () {
		this.x = 0;
		this.y = 0;
		return this;
	}

	@Override
	public float getX_() {
		return this.x;
	}

	@Override
	public float getY_() {
		return this.y; 
	}
	

	@Override
	public void setX_(float x) {
		this.x = x;
	}

	@Override
	public void setY_(float y) {
		this.y = y;
	}

	public SGVec_2f toSGVec2f() {
		return new SGVec_2f((float)x,(float) y);
	}

	@Override
	public SGVec_2f div(float n) {
		x /= n;
		y /= n;
		return this;
	}

	@Override
	public SGVec_2f set(float[] v) {
		this.x = v[0];
		this.y = v[1];		
		return this;
	}

}
