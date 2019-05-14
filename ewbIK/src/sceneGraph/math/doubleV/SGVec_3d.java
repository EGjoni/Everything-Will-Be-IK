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

import data.CanLoad;
import data.JSONArray;
import data.JSONObject;
import sceneGraph.math.Interpolation;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.floatV.Vec3f;

//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com */
public class SGVec_3d implements Serializable, Vec3d<SGVec_3d>, CanLoad {
	private static final long serialVersionUID = 3840054589595372522L;

	/** the x-component of this vector **/
	public double x;
	/** the y-component of this vector **/
	public double y;
	/** the z-component of this vector **/
	public double z;

	public final static int X = 0, Y= 1, Z = 2;

	private final static Matrix4d tmpMat = new Matrix4d();

	/** Constructs a vector at (0,0,0) */
	public SGVec_3d () {
	}

	/** Creates a vector with the given components
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component */
	public SGVec_3d (double x, double y, double z) {
		this.set(x, y, z);
	}
	
	public SGVec_3d(JSONObject j) {
		JSONArray components = j.getJSONArray("vec");
		this.x = components.getDouble(0);
		this.y = components.getDouble(1);
		this.z = components.getDouble(2);
	}

	/** Creates a vector from the given vector
	 * @param vector The vector */
	public SGVec_3d (final Vec3d vector) {
		this.set(vector);
	}
	
	/** Creates a vector from the given vector
	 * @param vector The vector */
	public SGVec_3d (final Vec3f vector) {
		this.set(vector);
	}

	/** Creates a vector from the given array. The array must have at least 3 elements.
	 *
	 * @param values The array */
	public SGVec_3d (final double[] values) {
		this.set(values[0], values[1], values[2]);
	}

	public SGVec_3d (JSONArray j) {
		this.x = j.getDouble(0);
		this.y = j.getDouble(1);
		this.z = j.getDouble(2);
	}


	/** Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public SGVec_3d set (double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public SGVec_3d set (final SGVec_3d vector) {
		return this.set(vector.x, vector.y, vector.z);
	}
	
	@Override
	public SGVec_3d set (final Vec3d vector) {
		return this.set(vector.getX(), vector.getY(), vector.getZ());
	}
	
	public SGVec_3d set (final Vec3f vector) {
		return this.set(vector.getX(), vector.getY(), vector.getZ());
	}

	/** Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining */
	@Override
	public SGVec_3d set (final double[] values) {
		return this.set(values[0], values[1], values[2]);
	}

	/** Sets the components of the given vector and z-component
	 *
	 * @param vector The vector
	 * @param z The z-component
	 * @return This vector for chaining */
	public SGVec_3d set (final SGVec_2d vector, double z) {
		return this.set(vector.x, vector.y, z);
	}

	/** Sets the components from the given spherical coordinate
	 * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
	 * @param polarAngle The angle between z-axis in radians [0, pi]
	 * @return This vector for chaining */
	public SGVec_3d setFromSpherical (double azimuthalAngle, double polarAngle) {
		double cosPolar = MathUtils.cos(polarAngle);
		double sinPolar = MathUtils.sin(polarAngle);

		double cosAzim = MathUtils.cos(azimuthalAngle);
		double sinAzim = MathUtils.sin(azimuthalAngle);

		return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
	}

	

	@Override
	public SGVec_3d copy () {
		return new SGVec_3d(this);
	}

	@Override
	public SGVec_3d add (final SGVec_3d vector) {
		return this.add(vector.x, vector.y, vector.z);
	}

	/** Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining. */
	public SGVec_3d add (double x, double y, double z) {
		this.x += x; 
		this.y+= y; 
		this.z += z;
		return this;
	}

	/** Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining */
	public SGVec_3d add (double values) {
		return set(this.x + values, this.y + values, this.z + values);
	}

	@Override
	public SGVec_3d sub (final SGVec_3d a_vec) {
		return sub(a_vec.x, a_vec.y, a_vec.z);
	}

	/** Subtracts the other vector from this vector.
	 *
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public SGVec_3d sub (double x, double y, double z) {
		return this.set(this.x - x, this.y - y, this.z - z);
	}

	/** Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining */
	public SGVec_3d sub (double value) {
		return this.set(this.x - value, this.y - value, this.z - value);
	}
	
	

	@Override
	public SGVec_3d mult (double scalar) {
		return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	@Override
	public SGVec_3d mult (final SGVec_3d other) {
		return this.set(x * other.x, y * other.y, z * other.z);
	}

	/** Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining */
	public SGVec_3d mult (double vx, double vy, double vz) {
		return this.set(this.x * vx, this.y * vy, this.z * vz);
	}
	

	@Override
	public SGVec_3d div(double n) {
		x /= n;
		y /= n;
		z /= n;
		return this;
	}

	

	@Override
	public SGVec_3d mulAdd (SGVec_3d vec, double scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		this.z += vec.z * scalar;
		return this;
	}

	@Override
	public SGVec_3d mulAdd (SGVec_3d vec, SGVec_3d mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		this.z += vec.z * mulVec.z;
		return this;
	}

	/** @return The euclidean length */
	public static double mag (final double x, final double y, final double z) {
		return (double)Math.sqrt(x * x + y * y + z * z);
	}

	@Override
	public double mag () {
		return (double)Math.sqrt(x * x + y * y + z * z);
	}

	/** @return The squared euclidean length */
	public static double magSq (final double x, final double y, final double z) {
		return x * x + y * y + z * z;
	}

	@Override
	public double magSq () {
		return x * x + y * y + z * z;
	}

	/** @param vector The other vector
	 * @return Whether this and the other vector are equal */
	public boolean idt (final SGVec_3d vector) {
		return x == vector.x && y == vector.y && z == vector.z;
	}

	/** @return The euclidean distance between the two specified vectors */
	public static double dst (final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		final double a = x2 - x1;
		final double b = y2 - y1;
		final double c = z2 - z1;
		return (double)Math.sqrt(a * a + b * b + c * c);
	}

	@Override
	public double dist (final SGVec_3d vector) {
		final double a = vector.x - x;
		final double b = vector.y - y;
		final double c = vector.z - z;
		return (double)Math.sqrt(a * a + b * b + c * c);
	}

	/** @return the distance between this point and the given point */
	public double dst (double x, double y, double z) {
		final double a = x - this.x;
		final double b = y - this.y;
		final double c = z - this.z;
		return (double)Math.sqrt(a * a + b * b + c * c);
	}

	/** @return the squared distance between the given points */
	public static double dst2 (final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		final double a = x2 - x1;
		final double b = y2 - y1;
		final double c = z2 - z1;
		return a * a + b * b + c * c;
	}

	@Override
	public double distSq (SGVec_3d point) {
		final double a = point.x - x;
		final double b = point.y - y;
		final double c = point.z - z;
		return a * a + b * b + c * c;
	}

	/** Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance */
	public double dst2 (double x, double y, double z) {
		final double a = x - this.x;
		final double b = y - this.y;
		final double c = z - this.z;
		return a * a + b * b + c * c;
	}

	@Override
	public SGVec_3d normalize () {
		final double len2 = this.mag();
		if (len2 == 0d || len2 == 1d) return this;
		return this.mult(1d / (double)len2);
	}

	/** @return The dot product between the two vectors */
	public static double dot (double x1, double y1, double z1, double x2, double y2, double z2) {
		return x1 * x2 + y1 * y2 + z1 * z2;
	}

	@Override
	public double dot (final SGVec_3d vector) {
		return x * vector.x + y * vector.y + z * vector.z;
	}

	/** Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product */
	public double dot (double x, double y, double z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	@Override
	public SGVec_3d crs (final SGVec_3d vector) {
		return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
	}

	/** Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public SGVec_3d crs (double x, double y, double z) {
		return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/** Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
	 * rotation and scale plus a 1x3 matrix representing the translation.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3d mul4x3 (double[] matrix) {
		return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7]
			+ matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11]);
	}

	/** Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3d mul (final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M01] + z * l_mat[Matrix4d.M02] + l_mat[Matrix4d.M03], x
			* l_mat[Matrix4d.M10] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M12] + l_mat[Matrix4d.M13], x * l_mat[Matrix4d.M20] + y
			* l_mat[Matrix4d.M21] + z * l_mat[Matrix4d.M22] + l_mat[Matrix4d.M23]);
	}

	/** Multiplies the vector by the transpose of the given matrix, assuming the fourth (w) component of the vector is 1.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3d traMul (final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M10] + z * l_mat[Matrix4d.M20] + l_mat[Matrix4d.M30], x
			* l_mat[Matrix4d.M01] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M21] + l_mat[Matrix4d.M31], x * l_mat[Matrix4d.M02] + y
			* l_mat[Matrix4d.M12] + z * l_mat[Matrix4d.M22] + l_mat[Matrix4d.M32]);
	}

	/** Left-multiplies the vector by the given matrix.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3d mul (Matrix3d matrix) {
		final double l_mat[] = matrix.val;
		return set(x * l_mat[Matrix3d.M00] + y * l_mat[Matrix3d.M01] + z * l_mat[Matrix3d.M02], x * l_mat[Matrix3d.M10] + y
			* l_mat[Matrix3d.M11] + z * l_mat[Matrix3d.M12], x * l_mat[Matrix3d.M20] + y * l_mat[Matrix3d.M21] + z * l_mat[Matrix3d.M22]);
	}

	/** Multiplies the vector by the transpose of the given matrix.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3d traMul (Matrix3d matrix) {
		final double l_mat[] = matrix.val;
		return set(x * l_mat[Matrix3d.M00] + y * l_mat[Matrix3d.M10] + z * l_mat[Matrix3d.M20], x * l_mat[Matrix3d.M01] + y
			* l_mat[Matrix3d.M11] + z * l_mat[Matrix3d.M21], x * l_mat[Matrix3d.M02] + y * l_mat[Matrix3d.M12] + z * l_mat[Matrix3d.M22]);
	}

	/** Multiplies the vector by the given {@link Quaternionf}.
	 * @return This vector for chaining */
	public SGVec_3d mul (final Quaternion quat) {
		return quat.transform(this);
	}

	/** Multiplies this vector by the given matrix dividing by w, assuming the fourth (w) component of the vector is 1. This is
	 * mostly used to project/unproject vectors via a perspective projection matrix.
	 *
	 * @param matrix The matrix.
	 * @return This vector for chaining */
	public SGVec_3d prj (final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		final double l_w = 1f / (x * l_mat[Matrix4d.M30] + y * l_mat[Matrix4d.M31] + z * l_mat[Matrix4d.M32] + l_mat[Matrix4d.M33]);
		return this.set((x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M01] + z * l_mat[Matrix4d.M02] + l_mat[Matrix4d.M03]) * l_w, (x
			* l_mat[Matrix4d.M10] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M12] + l_mat[Matrix4d.M13])
			* l_w, (x * l_mat[Matrix4d.M20] + y * l_mat[Matrix4d.M21] + z * l_mat[Matrix4d.M22] + l_mat[Matrix4d.M23]) * l_w);
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
	public SGVec_3d getPlaneProjectionOf(SGVec_3d p1, SGVec_3d p2) {
		return this.getPlaneProjectionOf(p1.crossCopy(p2));
	}
	
	
	/**
	 * Takes a vector representing the normal of a plane, and returns 
	 * the value of this vector projected onto that plane
	 * @param norm
	 * @return
	 */
	public SGVec_3d getPlaneProjectionOf(SGVec_3d rawNorm) {
		SGVec_3d norm = rawNorm.copy().normalize();
		SGVec_3d normProj = norm.multCopy(this.dot(norm));
		normProj.mult(-1);
		
		return normProj.addCopy(this);
	}

	/** Multiplies this vector by the first three columns of the matrix, essentially only applying rotation and scaling.
	 *
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public SGVec_3d rot (final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M01] + z * l_mat[Matrix4d.M02], x * l_mat[Matrix4d.M10] + y
			* l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M12], x * l_mat[Matrix4d.M20] + y * l_mat[Matrix4d.M21] + z * l_mat[Matrix4d.M22]);
	}

	/** Multiplies this vector by the transpose of the first three columns of the matrix. Note: only works for translation and
	 * rotation, does not work for scaling. For those, use {@link #rot(Matrix4d)} with {@link Matrix4d#inv()}.
	 * @param matrix The transformation matrix
	 * @return The vector for chaining */
	public SGVec_3d unrotate (final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M10] + z * l_mat[Matrix4d.M20], x * l_mat[Matrix4d.M01] + y
			* l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M21], x * l_mat[Matrix4d.M02] + y * l_mat[Matrix4d.M12] + z * l_mat[Matrix4d.M22]);
	}

	/** Translates this vector in the direction opposite to the translation of the matrix and the multiplies this vector by the
	 * transpose of the first three columns of the matrix. Note: only works for translation and rotation, does not work for
	 * scaling. For those, use {@link #mul(Matrix4d)} with {@link Matrix4d#inv()}.
	 * @param matrix The transformation matrix
	 * @return The vector for chaining */
	public SGVec_3d untransform (final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		x -= l_mat[Matrix4d.M03];
		y -= l_mat[Matrix4d.M03];
		z -= l_mat[Matrix4d.M03];
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M10] + z * l_mat[Matrix4d.M20], x * l_mat[Matrix4d.M01] + y
			* l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M21], x * l_mat[Matrix4d.M02] + y * l_mat[Matrix4d.M12] + z * l_mat[Matrix4d.M22]);
	}

	/** Rotates this vector by the given angle in degrees around the given axis.
	 *
	 * @param degrees the angle in degrees
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 * @return This vector for chaining */
	public SGVec_3d rotate (double degrees, double axisX, double axisY, double axisZ) {
		return this.mul(tmpMat.setToRotation(axisX, axisY, axisZ, degrees));
	}

	/** Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param radians the angle in radians
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 * @return This vector for chaining */
	public SGVec_3d rotateRad (double radians, double axisX, double axisY, double axisZ) {
		return this.mul(tmpMat.setToRotationRad(axisX, axisY, axisZ, radians));
	}

	/** Rotates this vector by the given angle in degrees around the given axis.
	 *
	 * @param axis the axis
	 * @param degrees the angle in degrees
	 * @return This vector for chaining */
	public SGVec_3d rotate (final SGVec_3d axis, double degrees) {
		tmpMat.setToRotation(axis, degrees);
		return this.mul(tmpMat);
	}

	/** Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param axis the axis
	 * @param radians the angle in radians
	 * @return This vector for chaining */
	public SGVec_3d rotateRad (final SGVec_3d axis, double radians) {
		tmpMat.setToRotationRad(axis, radians);
		return this.mul(tmpMat);
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
		return x == 0 && y == 0 && z == 0;
	}

	@Override
	public boolean isZero (final double margin) {
		return magSq() < margin;
	}

	@Override
	public boolean isOnLine (SGVec_3d other, double epsilon) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon;
	}

	@Override
	public boolean isOnLine (SGVec_3d other) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.DOUBLE_ROUNDING_ERROR;
	}

	@Override
	public boolean isCollinear (SGVec_3d other, double epsilon) {
		return isOnLine(other, epsilon) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinear (SGVec_3d other) {
		return isOnLine(other) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (SGVec_3d other, double epsilon) {
		return isOnLine(other, epsilon) && hasOppositeDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (SGVec_3d other) {
		return isOnLine(other) && hasOppositeDirection(other);
	}

	@Override
	public boolean isPerpendicular (SGVec_3d vector) {
		return MathUtils.isZero(dot(vector));
	}

	@Override
	public boolean isPerpendicular (SGVec_3d vector, double epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	@Override
	public boolean hasSameDirection (SGVec_3d vector) {
		return dot(vector) > 0;
	}

	@Override
	public boolean hasOppositeDirection (SGVec_3d vector) {
		return dot(vector) < 0;
	}

	@Override
	public SGVec_3d lerp (final SGVec_3d target, double alpha) {
		x += alpha * (target.x - x);
		y += alpha * (target.y - y);
		z += alpha * (target.z - z);
		return this;
	}

	@Override
	public SGVec_3d interpolate (SGVec_3d target, double alpha, Interpolation interpolator) {
		return lerp(target, interpolator.apply(0f, 1f, alpha));
	}

	/** Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
	 * stored in this vector.
	 *
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	public SGVec_3d slerp (final SGVec_3d target, double alpha) {
		final double dot = dot(target);
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

		// theta0 = angle between input vectors
		final double theta0 = (double)Math.acos(dot);
		// theta = angle between this vector and result
		final double theta = theta0 * alpha;

		final double st = (double)Math.sin(theta);
		final double tx = target.x - x * dot;
		final double ty = target.y - y * dot;
		final double tz = target.z - z * dot;
		final double l2 = tx * tx + ty * ty + tz * tz;
		final double dl = st * ((l2 < 0.0001f) ? 1f : 1f / (double)Math.sqrt(l2));

		return mult((double)Math.cos(theta)).add(tx * dl, ty * dl, tz * dl).normalize();
	}

	/** Converts this {@code Vector3} to a string in the format {@code (x,y,z)}.
	 * @return a string representation of this object. */
	@Override
	public String toString () {
		return "(" +(float) x + "," + (float)y + "," + (float)z + ")";
	}


	@Override
	public SGVec_3d limit (double limit) {
		return limitSq(limit * limit);
	}

	@Override
	public SGVec_3d limitSq (double limit2) {
		double len2 = magSq();
		if (len2 > limit2) {
			mult((double)Math.sqrt(limit2 / len2));
		}
		return this;
	}

	@Override
	public SGVec_3d setMag (double len) {
		return setMagSq(len * len);
	}

	@Override
	public SGVec_3d setMagSq (double len2) {
		double oldLen2 = magSq();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : mult((double)Math.sqrt(len2 / oldLen2));
	}

	@Override
	public SGVec_3d clamp (double min, double max) {
		final double len2 = magSq();
		if (len2 == 0f) return this;
		double max2 = max * max;
		if (len2 > max2) return mult((double)Math.sqrt(max2 / len2));
		double min2 = min * min;
		if (len2 < min2) return mult((double)Math.sqrt(min2 / len2));
		return this;
	}


	@Override
	public boolean epsilonEquals (final SGVec_3d other, double epsilon) {
		if (other == null) return false;
		if (Math.abs(other.x - x) > epsilon) return false;
		if (Math.abs(other.y - y) > epsilon) return false;
		if (Math.abs(other.z - z) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (double x, double y, double z, double epsilon) {
		if (Math.abs(x - this.x) > epsilon) return false;
		if (Math.abs(y - this.y) > epsilon) return false;
		if (Math.abs(z - this.z) > epsilon) return false;
		return true;
	}

	@Override
	public SGVec_3d setZero () {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		return this;
	}

	
	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y; 
	}

	@Override
	public double getZ() {
		return this.z;
	}


	@Override
	public void setX_(double x) {
		this.x = x;		
	}

	@Override
	public void setY_(double y) {
		this.y = y;
		
	}

	@Override
	public void setZ_(double z) {
		this.z = z;
	}
	public SGVec_3d getOrthogonal() {
		SGVec_3d result = new SGVec_3d(0,0,0);
		
		double threshold = this.mag() * 0.6;
		if(threshold > 0) {
			if (Math.abs(x) <= threshold) {
				double inverse  = 1 / Math.sqrt(y * y + z * z);
				return new SGVec_3d(0, inverse * z, -inverse * y);
			} else if (Math.abs(y) <= threshold) {
				double inverse  = 1 / Math.sqrt(x * x + z * z);
				return new SGVec_3d(-inverse * z, 0, inverse * x);
			}
			double inverse  = 1 / Math.sqrt(x * x + y * y);
			return new SGVec_3d(inverse * y, -inverse * x, 0);
		}

		return result; 
	}

	@Override
	public SGVec_3f toSGVec3f() {
		return new SGVec_3f((float)x,(float)y,(float)z);
	}
	
	public static double dot(SGVec_3d u, SGVec_3d v) {
		return u.dot(v);
	}
	
	public static SGVec_3d add(SGVec_3d v1, SGVec_3d v2) {
		return SGVec_3d.add(v1, v2, null);
	}
	
	public static SGVec_3d add(SGVec_3d v1, SGVec_3d v2, SGVec_3d target) {
		if (target == null) {
			target = new SGVec_3d(
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
	 * @param target SGVec_3d in which to store the result
	 */
	static public SGVec_3d sub(SGVec_3d v1, SGVec_3d v2) {
		return SGVec_3d.sub(v1, v2, null);
	}	
	
	static public SGVec_3d mult(SGVec_3d  v, double n) {
		return mult(v, n, null);
	}

	static public SGVec_3d mult(SGVec_3d v, double n, SGVec_3d target) {
		if (target == null) {
			target = new SGVec_3d(v.x*n, v.y*n, v.z*n);
		} else {
			target.set(v.x*n, v.y*n, v.z*n);
		}
		return target;
	}
	
	static public SGVec_3d div(SGVec_3d  v, double n) {
		return div(v, n, null);
	}

	static public SGVec_3d div(SGVec_3d v, double n, SGVec_3d target) {
		if (target == null) {
			target = new SGVec_3d(v.x/n, v.y/n, v.z/n);
		} else {
			target.set(v.x/n, v.y/n, v.z/n);
		}
		return target;
	}

	/**
	 * Subtract one vector from another and store in another vector
	 * @param target SGVec_3d in which to store the result
	 * @return 
	 */
	static public SGVec_3d sub(SGVec_3d v1, SGVec_3d v2, SGVec_3d target) {
		if (target == null) {
			target = new SGVec_3d(
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
	 * @param v1 any variable of type SGVec_3d
	 * @param v2 any variable of type SGVec_3d
	 * @param target SGVec_3d to store the result
	 */
	public static SGVec_3d cross(SGVec_3d v1, SGVec_3d v2, SGVec_3d target) {
		double crossX = v1.y * v2.z - v2.y * v1.z;
		double crossY = v1.z * v2.x - v2.z * v1.x;
		double crossZ = v1.x * v2.y - v2.x * v1.y;

		if (target == null) {
			target = new SGVec_3d(crossX, crossY, crossZ);
		} else {
			target.set(crossX, crossY, crossZ);
		}
		return target;
	}	
	
	
	/**
	 * Linear interpolate between two vectors (returns a new SGVec_3d object)
	 * @param v1 the vector to start from
	 * @param v2 the vector to lerp to
	 */
	public static SGVec_3d lerp(SGVec_3d v1, SGVec_3d v2, double amt) {
		SGVec_3d v = v1.copy();
		v.lerp(v2, amt);
		return v;
	}
	
	/**
	 * ( begin auto-generated from SGVec_3d_angleBetween.xml )
	 *
	 * Calculates and returns the angle (in radians) between two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref SGVec_3d:method
	 * @usage web_application
	 * @param v1 the x, y, and z components of a SGVec_3d
	 * @param v2 the x, y, and z components of a SGVec_3d
	 * @brief Calculate and return the angle between two vectors
	 */
	static public double angleBetween(SGVec_3d v1, SGVec_3d v2) {

		// We get NaN if we pass in a zero vector which can cause problems
		// Zero seems like a reasonable angle between a (0,0,0) vector and something else
		if (v1.x == 0 && v1.y == 0 && v1.z == 0 ) return 0.0f;
		if (v2.x == 0 && v2.y == 0 && v2.z == 0 ) return 0.0f;

		double dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
		double v1mag = Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
		double v2mag = Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);
		// This should be a number between -1 and 1, since it's "normalized"
		double amt = dot / (v1mag * v2mag);
		// But if it's not due to rounding error, then we need to fix it
		// http://code.google.com/p/processing/issues/detail?id=340
		// Otherwise if outside the range, acos() will return NaN
		// http://www.cppreference.com/wiki/c/math/acos
		if (amt <= -1) {
			return Math.PI;
		} else if (amt >= 1) {
			return 0;
		}
		return (double) Math.acos(amt);
	}

	@Override
	public SGVec_3d add(double[] v) {
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
		this.x = components.getDouble(0);
		this.y = components.getDouble(1);
		this.z = components.getDouble(2);
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
