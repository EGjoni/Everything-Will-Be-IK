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

package sceneGraph.math.floatV;

import java.io.Serializable;

import data.CanLoad;
import data.JSONArray;
import data.JSONObject;
import sceneGraph.math.floatV.Interpolation;
import sceneGraph.math.doubleV.Vec3d;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.Vec3f;

//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com */
public class SGVec_3f implements Serializable, Vec3f<SGVec_3f>, CanLoad {
	private static final long serialVersionUID = 3840054589595372522L;

	/** the x-component of this vector **/
	public float x;
	/** the y-component of this vector **/
	public float y;
	/** the z-component of this vector **/
	public float z;

	public final static int X = 0, Y= 1, Z = 2;

	private final static Matrix4f tmpMat = new Matrix4f();

	/** Constructs a vector at (0,0,0) */
	public SGVec_3f () {
	}

	/** Creates a vector with the given components
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component */
	public SGVec_3f (float x, float y, float z) {
		this.set(x, y, z);
	}
	
	public SGVec_3f(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getFloat(0);
		this.y = components.getFloat(1);
		this.z = components.getFloat(2);
	}

	/** Creates a vector from the given vector
	 * @param vector The vector */
	public SGVec_3f (final Vec3d vector) {
		this.set(vector);
	}
	
	/** Creates a vector from the given vector
	 * @param vector The vector */
	public SGVec_3f (final Vec3f vector) {
		this.set(vector);
	}

	/** Creates a vector from the given array. The array must have at least 3 elements.
	 *
	 * @param values The array */
	public SGVec_3f (final float[] values) {
		this.set(values[0], values[1], values[2]);
	}

	public SGVec_3f (JSONArray j) {
		this.x = j.getFloat(0);
		this.y = j.getFloat(1);
		this.z = j.getFloat(2);
	}


	/** Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public SGVec_3f set (float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	/** Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public SGVec_3f set (double x, double  y, double z) {
		this.x = (float)x;
		this.y = (float)y;
		this.z = (float)z;
		return this;
	}

	public SGVec_3f set (final SGVec_3f vector) {
		return this.set(vector.x, vector.y, vector.z);
	}
	
	@Override
	public SGVec_3f set (final Vec3f vector) {
		return this.set(vector.getX(), vector.getY(), vector.getZ());
	}
	
	public SGVec_3f set (final Vec3d vector) {
		return this.set(vector.getX(), vector.getY(), vector.getZ());
	}

	/** Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining */
	@Override
	public SGVec_3f set (final float[] values) {
		return this.set(values[0], values[1], values[2]);
	}

	/** Sets the components of the given vector and z-component
	 *
	 * @param vector The vector
	 * @param z The z-component
	 * @return This vector for chaining */
	public SGVec_3f set (final SGVec_2f vector, float z) {
		return this.set(vector.x, vector.y, z);
	}

	/** Sets the components from the given spherical coordinate
	 * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
	 * @param polarAngle The angle between z-axis in radians [0, pi]
	 * @return This vector for chaining */
	public SGVec_3f setFromSpherical (float azimuthalAngle, float polarAngle) {
		float cosPolar = MathUtils.cos(polarAngle);
		float sinPolar = MathUtils.sin(polarAngle);

		float cosAzim = MathUtils.cos(azimuthalAngle);
		float sinAzim = MathUtils.sin(azimuthalAngle);

		return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
	}

	

	@Override
	public SGVec_3f copy () {
		return new SGVec_3f(this);
	}

	@Override
	public SGVec_3f add (final SGVec_3f vector) {
		return this.add(vector.x, vector.y, vector.z);
	}

	/** Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining. */
	public SGVec_3f add (float x, float y, float z) {
		this.x += x; 
		this.y+= y; 
		this.z += z;
		return this;
	}

	/** Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining */
	public SGVec_3f add (float values) {
		return set(this.x + values, this.y + values, this.z + values);
	}

	@Override
	public SGVec_3f sub (final SGVec_3f a_vec) {
		return sub(a_vec.x, a_vec.y, a_vec.z);
	}

	/** Subtracts the other vector from this vector.
	 *
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public SGVec_3f sub (float x, float y, float z) {
		return this.set(this.x - x, this.y - y, this.z - z);
	}

	/** Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining */
	public SGVec_3f sub (float value) {
		return this.set(this.x - value, this.y - value, this.z - value);
	}
	
	

	@Override
	public SGVec_3f mult (float scalar) {
		return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	@Override
	public SGVec_3f mult (final SGVec_3f other) {
		return this.set(x * other.x, y * other.y, z * other.z);
	}

	/** Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining */
	public SGVec_3f mult (float vx, float vy, float vz) {
		return this.set(this.x * vx, this.y * vy, this.z * vz);
	}
	

	@Override
	public SGVec_3f div(float n) {
		x /= n;
		y /= n;
		z /= n;
		return this;
	}

	

	@Override
	public SGVec_3f mulAdd (SGVec_3f vec, float scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		this.z += vec.z * scalar;
		return this;
	}

	@Override
	public SGVec_3f mulAdd (SGVec_3f vec, SGVec_3f mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		this.z += vec.z * mulVec.z;
		return this;
	}

	/** @return The euclidean length */
	public static float mag (final float x, final float y, final float z) {
		return MathUtils.sqrt(x * x + y * y + z * z);
	}

	@Override
	public float mag () {
		return MathUtils.sqrt(x * x + y * y + z * z);
	}

	/** @return The squared euclidean length */
	public static float magSq (final float x, final float y, final float z) {
		return x * x + y * y + z * z;
	}

	@Override
	public float magSq () {
		return x * x + y * y + z * z;
	}

	/** @param vector The other vector
	 * @return Whether this and the other vector are equal */
	public boolean idt (final SGVec_3f vector) {
		return x == vector.x && y == vector.y && z == vector.z;
	}

	/** @return The euclidean distance between the two specified vectors */
	public static float dst (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		return MathUtils.sqrt(a * a + b * b + c * c);
	}

	@Override
	public float dist (final SGVec_3f vector) {
		final float a = vector.x - x;
		final float b = vector.y - y;
		final float c = vector.z - z;
		return MathUtils.sqrt(a * a + b * b + c * c);
	}

	/** @return the distance between this point and the given point */
	public float dst (float x, float y, float z) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		return MathUtils.sqrt(a * a + b * b + c * c);
	}

	/** @return the squared distance between the given points */
	public static float dst2 (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		return a * a + b * b + c * c;
	}

	@Override
	public float distSq (SGVec_3f point) {
		final float a = point.x - x;
		final float b = point.y - y;
		final float c = point.z - z;
		return a * a + b * b + c * c;
	}

	/** Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance */
	public float dst2 (float x, float y, float z) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		return a * a + b * b + c * c;
	}

	@Override
	public SGVec_3f normalize () {
		final float len2 = this.mag();
		if (len2 == 0f || len2 == 1f) return this;
		return this.mult(1f / (float)len2);
	}

	/** @return The dot product between the two vectors */
	public static float dot (float x1, float y1, float z1, float x2, float y2, float z2) {
		return x1 * x2 + y1 * y2 + z1 * z2;
	}

	@Override
	public float dot (final SGVec_3f vector) {
		return x * vector.x + y * vector.y + z * vector.z;
	}

	/** Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product */
	public float dot (float x, float y, float z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	@Override
	public SGVec_3f crs (final SGVec_3f vector) {
		return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
	}

	/** Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public SGVec_3f crs (float x, float y, float z) {
		return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/** Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
	 * rotation and scale plus a 1x3 matrix representing the translation.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3f mul4x3 (float[] matrix) {
		return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7]
			+ matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11]);
	}

	/** Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3f mul (final Matrix4f matrix) {
		final float l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M01] + z * l_mat[Matrix4f.M02] + l_mat[Matrix4f.M03], x
			* l_mat[Matrix4f.M10] + y * l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M12] + l_mat[Matrix4f.M13], x * l_mat[Matrix4f.M20] + y
			* l_mat[Matrix4f.M21] + z * l_mat[Matrix4f.M22] + l_mat[Matrix4f.M23]);
	}

	/** Multiplies the vector by the transpose of the given matrix, assuming the fourth (w) component of the vector is 1.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3f traMul (final Matrix4f matrix) {
		final float l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M10] + z * l_mat[Matrix4f.M20] + l_mat[Matrix4f.M30], x
			* l_mat[Matrix4f.M01] + y * l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M21] + l_mat[Matrix4f.M31], x * l_mat[Matrix4f.M02] + y
			* l_mat[Matrix4f.M12] + z * l_mat[Matrix4f.M22] + l_mat[Matrix4f.M32]);
	}

	/** Left-multiplies the vector by the given matrix.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3f mul (Matrix3f matrix) {
		final float l_mat[] = matrix.val;
		return set(x * l_mat[Matrix3f.M00] + y * l_mat[Matrix3f.M01] + z * l_mat[Matrix3f.M02], x * l_mat[Matrix3f.M10] + y
			* l_mat[Matrix3f.M11] + z * l_mat[Matrix3f.M12], x * l_mat[Matrix3f.M20] + y * l_mat[Matrix3f.M21] + z * l_mat[Matrix3f.M22]);
	}

	/** Multiplies the vector by the transpose of the given matrix.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3f traMul (Matrix3f matrix) {
		final float l_mat[] = matrix.val;
		return set(x * l_mat[Matrix3f.M00] + y * l_mat[Matrix3f.M10] + z * l_mat[Matrix3f.M20], x * l_mat[Matrix3f.M01] + y
			* l_mat[Matrix3f.M11] + z * l_mat[Matrix3f.M21], x * l_mat[Matrix3f.M02] + y * l_mat[Matrix3f.M12] + z * l_mat[Matrix3f.M22]);
	}

	/** Multiplies the vector by the given {@link Quaternionff}.
	 * @return This vector for chaining */
	public SGVec_3f mul (final Quaternionf quat) {
		return quat.transform(this);
	}

	/** Multiplies this vector by the given matrix dividing by w, assuming the fourth (w) component of the vector is 1. This is
	 * mostly used to project/unproject vectors via a perspective projection matrix.
	 *
	 * @param matrix The matrix.
	 * @return This vector for chaining */
	public SGVec_3f prj (final Matrix4f matrix) {
		final float l_mat[] = matrix.val;
		final float l_w = 1f / (x * l_mat[Matrix4f.M30] + y * l_mat[Matrix4f.M31] + z * l_mat[Matrix4f.M32] + l_mat[Matrix4f.M33]);
		return this.set((x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M01] + z * l_mat[Matrix4f.M02] + l_mat[Matrix4f.M03]) * l_w, (x
			* l_mat[Matrix4f.M10] + y * l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M12] + l_mat[Matrix4f.M13])
			* l_w, (x * l_mat[Matrix4f.M20] + y * l_mat[Matrix4f.M21] + z * l_mat[Matrix4f.M22] + l_mat[Matrix4f.M23]) * l_w);
	}
	
	
	/**
	 * Takes two vectors representing a plane 
	 * and returns the projection of this vector onto 
	 * that plane.
	 * 
	 * @param p1 vector representing first edge of plane 
	 * @param p2 vector representing second edge of plane
	 * @return
	 */
	public SGVec_3f getPlaneProjectionOf(SGVec_3f p1, SGVec_3f p2) {
		return this.getPlaneProjectionOf(p1.crossCopy(p2));
	}
	
	
	/**
	 * Takes a vector representing the normal of a plane, and returns 
	 * the value of this vector projected onto that plane
	 * @param norm
	 * @return
	 */
	public SGVec_3f getPlaneProjectionOf(SGVec_3f rawNorm) {
		SGVec_3f norm = rawNorm.copy().normalize();
		SGVec_3f normProj = norm.multCopy(this.dot(norm));
		normProj.mult(-1);
		
		return normProj.addCopy(this);
	}

	/** Multiplies this vector by the first three columns of the matrix, essentially only applying rotation and scaling.
	 *
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3f rot (final Matrix4f matrix) {
		final float l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M01] + z * l_mat[Matrix4f.M02], x * l_mat[Matrix4f.M10] + y
			* l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M12], x * l_mat[Matrix4f.M20] + y * l_mat[Matrix4f.M21] + z * l_mat[Matrix4f.M22]);
	}

	/** Multiplies this vector by the transpose of the first three columns of the matrix. Note: only works for translation and
	 * rotation, does not work for scaling. For those, use {@link #rot(Matrix4f)} with {@link Matrix4f#inv()}.
	 * @param matrix The transformation matrix
	 * @return The vector for chaining */
	public SGVec_3f unrotate (final Matrix4f matrix) {
		final float l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M10] + z * l_mat[Matrix4f.M20], x * l_mat[Matrix4f.M01] + y
			* l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M21], x * l_mat[Matrix4f.M02] + y * l_mat[Matrix4f.M12] + z * l_mat[Matrix4f.M22]);
	}

	/** Translates this vector in the direction opposite to the translation of the matrix and the multiplies this vector by the
	 * transpose of the first three columns of the matrix. Note: only works for translation and rotation, does not work for
	 * scaling. For those, use {@link #mul(Matrix4f)} with {@link Matrix4f#inv()}.
	 * @param matrix The transformation matrix
	 * @return The vector for chaining */
	public SGVec_3f untransform (final Matrix4f matrix) {
		final float l_mat[] = matrix.val;
		x -= l_mat[Matrix4f.M03];
		y -= l_mat[Matrix4f.M03];
		z -= l_mat[Matrix4f.M03];
		return this.set(x * l_mat[Matrix4f.M00] + y * l_mat[Matrix4f.M10] + z * l_mat[Matrix4f.M20], x * l_mat[Matrix4f.M01] + y
			* l_mat[Matrix4f.M11] + z * l_mat[Matrix4f.M21], x * l_mat[Matrix4f.M02] + y * l_mat[Matrix4f.M12] + z * l_mat[Matrix4f.M22]);
	}

	/** Rotates this vector by the given angle in degrees around the given axis.
	 *
	 * @param degrees the angle in degrees
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 * @return This vector for chaining */
	public SGVec_3f rotate (float degrees, float axisX, float axisY, float axisZ) {
		return this.mul(tmpMat.setToRotation(axisX, axisY, axisZ, degrees));
	}

	/** Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param radians the angle in radians
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 * @return This vector for chaining */
	public SGVec_3f rotateRad (float radians, float axisX, float axisY, float axisZ) {
		return this.mul(tmpMat.setToRotationRad(axisX, axisY, axisZ, radians));
	}

	/** Rotates this vector by the given angle in degrees around the given axis.
	 *
	 * @param axis the axis
	 * @param degrees the angle in degrees
	 * @return This vector for chaining */
	public SGVec_3f rotate (final SGVec_3f axis, float degrees) {
		tmpMat.setToRotation(axis, degrees);
		return this.mul(tmpMat);
	}

	/** Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param axis the axis
	 * @param radians the angle in radians
	 * @return This vector for chaining */
	public SGVec_3f rotateRad (final SGVec_3f axis, float radians) {
		tmpMat.setToRotationRad(axis, radians);
		return this.mul(tmpMat);
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
		return x == 0 && y == 0 && z == 0;
	}

	@Override
	public boolean isZero (final float margin) {
		return magSq() < margin;
	}

	@Override
	public boolean isOnLine (SGVec_3f other, float epsilon) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon;
	}

	@Override
	public boolean isOnLine (SGVec_3f other) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.DOUBLE_ROUNDING_ERROR;
	}

	@Override
	public boolean isCollinear (SGVec_3f other, float epsilon) {
		return isOnLine(other, epsilon) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinear (SGVec_3f other) {
		return isOnLine(other) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (SGVec_3f other, float epsilon) {
		return isOnLine(other, epsilon) && hasOppositeDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (SGVec_3f other) {
		return isOnLine(other) && hasOppositeDirection(other);
	}

	@Override
	public boolean isPerpendicular (SGVec_3f vector) {
		return MathUtils.isZero(dot(vector));
	}

	@Override
	public boolean isPerpendicular (SGVec_3f vector, float epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	@Override
	public boolean hasSameDirection (SGVec_3f vector) {
		return dot(vector) > 0;
	}

	@Override
	public boolean hasOppositeDirection (SGVec_3f vector) {
		return dot(vector) < 0;
	}


	public SGVec_3f lerp (final SGVec_3f target, float alpha) {
		x += alpha * (target.x - x);
		y += alpha * (target.y - y);
		z += alpha * (target.z - z);
		return this;
	}

	/** Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
	 * stored in this vector.
	 *
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	public SGVec_3f slerp (final SGVec_3f target, float alpha) {
		final float dot = dot(target);
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

		// theta0 = angle between input vectors
		final float theta0 = (float)MathUtils.acos(dot);
		// theta = angle between this vector and result
		final float theta = theta0 * alpha;

		final float st = (float)MathUtils.sin(theta);
		final float tx = target.x - x * dot;
		final float ty = target.y - y * dot;
		final float tz = target.z - z * dot;
		final float l2 = tx * tx + ty * ty + tz * tz;
		final float dl = st * ((l2 < 0.0001f) ? 1f : 1f / MathUtils.sqrt(l2));

		return mult(MathUtils.cos(theta)).add(tx * dl, ty * dl, tz * dl).normalize();
	}

	/** Converts this {@code Vector3} to a string in the format {@code (x,y,z)}.
	 * @return a string representation of this object. */
	@Override
	public String toString () {
		return "(" +(float) x + "," + (float)y + "," + (float)z + ")";
	}


	@Override
	public SGVec_3f limit (float limit) {
		return limitSq(limit * limit);
	}

	@Override
	public SGVec_3f limitSq (float limit2) {
		float len2 = magSq();
		if (len2 > limit2) {
			mult(MathUtils.sqrt(limit2 / len2));
		}
		return this;
	}

	@Override
	public SGVec_3f setMag (float len) {
		return setMagSq(len * len);
	}

	@Override
	public SGVec_3f setMagSq (float len2) {
		float oldLen2 = magSq();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : mult(MathUtils.sqrt(len2 / oldLen2));
	}

	@Override
	public SGVec_3f clamp (float min, float max) {
		final float len2 = magSq();
		if (len2 == 0f) return this;
		float max2 = max * max;
		if (len2 > max2) return mult(MathUtils.sqrt(max2 / len2));
		float min2 = min * min;
		if (len2 < min2) return mult(MathUtils.sqrt(min2 / len2));
		return this;
	}


	@Override
	public boolean epsilonEquals (final SGVec_3f other, float epsilon) {
		if (other == null) return false;
		if (MathUtils.abs(other.x - x) > epsilon) return false;
		if (MathUtils.abs(other.y - y) > epsilon) return false;
		if (MathUtils.abs(other.z - z) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (float x, float y, float z, float epsilon) {
		if (MathUtils.abs(x - this.x) > epsilon) return false;
		if (MathUtils.abs(y - this.y) > epsilon) return false;
		if (MathUtils.abs(z - this.z) > epsilon) return false;
		return true;
	}

	@Override
	public SGVec_3f setZero () {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		return this;
	}

	
	@Override
	public float getX() {
		return this.x;
	}

	@Override
	public float getY() {
		return this.y; 
	}

	@Override
	public float getZ() {
		return this.z;
	}


	@Override
	public void setX_(float x) {
		this.x = x;		
	}

	@Override
	public void setY_(float y) {
		this.y = y;
		
	}

	@Override
	public void setZ_(float z) {
		this.z = z;
	}
	public SGVec_3f getOrthogonal() {
		SGVec_3f result = new SGVec_3f(0,0,0);
		
		float threshold = this.mag() * 0.6f;
		if(threshold > 0) {
			if (MathUtils.abs(x) <= threshold) {
				float inverse  = 1 / MathUtils.sqrt(y * y + z * z);
				return new SGVec_3f(0, inverse * z, -inverse * y);
			} else if (MathUtils.abs(y) <= threshold) {
				float inverse  = 1 / MathUtils.sqrt(x * x + z * z);
				return new SGVec_3f(-inverse * z, 0, inverse * x);
			}
			float inverse  = 1 / MathUtils.sqrt(x * x + y * y);
			return new SGVec_3f(inverse * y, -inverse * x, 0);
		}

		return result; 
	}

	@Override
	public SGVec_3f toSGVec3f() {
		return new SGVec_3f((float)x,(float)y,(float)z);
	}
	
	public static float dot(SGVec_3f u, SGVec_3f v) {
		return u.dot(v);
	}
	
	public static SGVec_3f add(SGVec_3f v1, SGVec_3f v2) {
		return SGVec_3f.add(v1, v2, null);
	}
	
	public static SGVec_3f add(SGVec_3f v1, SGVec_3f v2, SGVec_3f target) {
		if (target == null) {
			target = new SGVec_3f(
					v1.x + v2.x, 
					v1.y + v2.y, 
					v1.z + v2.z);
		} else {
			target.set(v1.x + v2.x, 
					v1.y+ v2.y, 
					v1.z + v2.z);
		}
		return target;
	}
	
	

	
	/**
	 * Subtract one vector from another and store in another vector
	 * @param target SGVec_3f in which to store the result
	 */
	static public SGVec_3f sub(SGVec_3f v1, SGVec_3f v2) {
		return SGVec_3f.sub(v1, v2, null);
	}	
	
	static public SGVec_3f mult(SGVec_3f  v, float n) {
		return mult(v, n, null);
	}

	static public SGVec_3f mult(SGVec_3f v, float n, SGVec_3f target) {
		if (target == null) {
			target = new SGVec_3f(v.x*n, v.y*n, v.z*n);
		} else {
			target.set(v.x*n, v.y*n, v.z*n);
		}
		return target;
	}
	
	static public SGVec_3f div(SGVec_3f  v, float n) {
		return div(v, n, null);
	}

	static public SGVec_3f div(SGVec_3f v, float n, SGVec_3f target) {
		if (target == null) {
			target = new SGVec_3f(v.x/n, v.y/n, v.z/n);
		} else {
			target.set(v.x/n, v.y/n, v.z/n);
		}
		return target;
	}

	/**
	 * Subtract v3 from v1 and store in target
	 * @param target SGVec_3f in which to store the result
	 * @return 
	 */
	static public SGVec_3f sub(SGVec_3f v1, SGVec_3f v2, SGVec_3f target) {
		if (target == null) {
			target = new SGVec_3f(
					v1.x - v2.x, 
					v1.y- v2.y, 
					v1.z - v2.z);
		} else {
			target.set(v1.x - v2.x, 
					v1.y- v2.y, 
					v1.z - v2.z);
		}
		return target;
	}
	
	/**
	 * @param v1 any variable of type SGVec_3f
	 * @param v2 any variable of type SGVec_3f
	 * @param target SGVec_3f to store the result
	 */
	public static SGVec_3f cross(SGVec_3f v1, SGVec_3f v2, SGVec_3f target) {
		float crossX = v1.y * v2.z - v2.y * v1.z;
		float crossY = v1.z * v2.x - v2.z * v1.x;
		float crossZ = v1.x * v2.y - v2.x * v1.y;

		if (target == null) {
			target = new SGVec_3f(crossX, crossY, crossZ);
		} else {
			target.set(crossX, crossY, crossZ);
		}
		return target;
	}	
	
	
	/**
	 * Linear interpolate between two vectors (returns a new SGVec_3f object)
	 * @param v1 the vector to start from
	 * @param v2 the vector to lerp to
	 */
	public static SGVec_3f lerp(SGVec_3f v1, SGVec_3f v2, float amt) {
		SGVec_3f v = v1.copy();
		v.lerp(v2, amt);
		return v;
	}
	
	/**
	 * ( begin auto-generated from SGVec_3f_angleBetween.xml )
	 *
	 * Calculates and returns the angle (in radians) between two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref SGVec_3f:method
	 * @usage web_application
	 * @param v1 the x, y, and z components of a SGVec_3f
	 * @param v2 the x, y, and z components of a SGVec_3f
	 * @brief Calculate and return the angle between two vectors
	 */
	static public float angleBetween(SGVec_3f v1, SGVec_3f v2) {

		// We get NaN if we pass in a zero vector which can cause problems
		// Zero seems like a reasonable angle between a (0,0,0) vector and something else
		if (v1.x == 0 && v1.y == 0 && v1.z == 0 ) return 0.0f;
		if (v2.x == 0 && v2.y == 0 && v2.z == 0 ) return 0.0f;

		float dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
		float v1mag = MathUtils.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
		float v2mag = MathUtils.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);
		// This should be a number between -1 and 1, since it's "normalized"
		float amt = dot / (v1mag * v2mag);
		// But if it's not due to rounding error, then we need to fix it
		// http://code.google.com/p/processing/issues/detail?id=340
		// Otherwise if outside the range, acos() will return NaN
		// http://www.cppreference.com/wiki/c/math/acos
		if (amt <= -1) {
			return MathUtils.PI;
		} else if (amt >= 1) {
			return 0;
		}
		return (float) MathUtils.acos(amt);
	}

	@Override
	public SGVec_3f add(float[] v) {
		// TODO Auto-generated method stub
		return null;
	}

	public JSONArray toJSONArray() {
		JSONArray vec = new JSONArray();
		vec.append(this.x); vec.append(this.y); vec.append(this.z);
		return vec;
	}

	@Override
	public CanLoad populateSelfFromJSON(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getFloat(0);
		this.y = components.getFloat(1);
		this.z = components.getFloat(2);
		return this;
	}

	@Override
	public JSONObject toJSONObject() {
		JSONObject j = new JSONObject(); 
		JSONArray components = new JSONArray(); 
		components.append(this.x);
		components.append(this.y);
		components.append(this.z);
		j.setJSONArray("vec", components); 
		return j;
	}

}
