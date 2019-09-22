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

//import com.badlogic.gdx.utils.NumberUtils;

/** A simple quaternion class.
 * @see <a href="http://en.wikipedia.org/wiki/Quaternion">http://en.wikipedia.org/wiki/Quaternion</a>
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa */
public class Quaternion implements Serializable {
	private static final long serialVersionUID = -7661875440674897168L;
	private static Quaternion tmp1 = new Quaternion(0, 0, 0, 0);
	private static Quaternion tmp2 = new Quaternion(0, 0, 0, 0);

	private double q1;
	private double q2;
	private double q3;
	private double q0;

	/** Constructor, sets the four components of the quaternion.
	 * @param w The w-component
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 *  */
	public Quaternion ( double w, double x, double y, double z) {
		this.set(w, x, y, z);
	}

	public Quaternion () {
		idt();
	}

	/** Constructor, sets the quaternion components from the given quaternion.
	 * 
	 * @param quaternion The quaternion to copy. */
	public Quaternion (Quaternion quaternion) {
		this.set(quaternion);
	}

	/** Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
	 * 
	 * @param axis The axis
	 * @param angle The angle in radians. */
	public Quaternion (SGVec_3d axis, double angle) {
		this.set(axis, angle);
	}

	/** Sets the components of the quaternion
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @param w The w-component
	 * @return This quaternion for chaining */
	public Quaternion set (double w, double x, double y, double z) {
		this.q0 = w;
		this.q1 = x;
		this.q2 = y;
		this.q3 = z;		
		return this;
	}

	/** Sets the quaternion components from the given quaternion.
	 * @param quaternion The quaternion.
	 * @return This quaternion for chaining. */
	public Quaternion set (Quaternion quaternion) {
		return this.set(quaternion.q0, quaternion.q1, quaternion.q2, quaternion.q3);
	}

	/** Sets the quaternion components from the given axis and angle around that axis.
	 * 
	 * @param axis The axis
	 * @param angle The angle in degrees
	 * @return This quaternion for chaining. */
	public Quaternion set (SGVec_3d axis, double angle) {
		return setFromAxisRad(axis.x, axis.y, axis.z, angle);
	}

	/** @return a copy of this quaternion */
	public Quaternion copy () {
		return new Quaternion(this);
	}

	/** @return the euclidean length of the specified quaternion */
	public final static double len (final double x, final double y, final double z, final double w) {
		return (double)Math.sqrt(x * x + y * y + z * z + w * w);
	}

	/** @return the euclidean length of this quaternion */
	public double len () {
		return (double)Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q0 * q0);
	}

	@Override
	public String toString () {
		return "["+ q0 +" " + q1 + " " + q2 + " " + q3 +  "]";
	}

	
	/** Sets the quaternion to the given euler angles in radians.
	 * @param yaw the rotation around the y axis in radians
	 * @param pitch the rotation around the x axis in radians
	 * @param roll the rotation around the z axis in radians
	 * @return this quaternion */
	public Quaternion setEulerAnglesRad (double yaw, double pitch, double roll) {
		final double hr = roll * 0.5d;
		final double shr = (double)Math.sin(hr);
		final double chr = (double)Math.cos(hr);
		final double hp = pitch * 0.5d;
		final double shp = (double)Math.sin(hp);
		final double chp = (double)Math.cos(hp);
		final double hy = yaw * 0.5d;
		final double shy = (double)Math.sin(hy);
		final double chy = (double)Math.cos(hy);
		final double chy_shp = chy * shp;
		final double shy_chp = shy * chp;
		final double chy_chp = chy * chp;
		final double shy_shp = shy * shp;

		q1 = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
		q2 = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
		q3 = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
		q0 = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
		return this;
	}

	/** Get the pole of the gimbal lock, if any.
	 * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock */
	public int getGimbalPole () {
		final double t = q2 * q1 + q3 * q0;
		return t > 0.499d ? 1 : (t < -0.499d ? -1 : 0);
	}

	

	public final static double len2 (final double x, final double y, final double z, final double w) {
		return x * x + y * y + z * z + w * w;
	}

	/** @return the length of this quaternion without square root */
	public double len2 () {
		return q1 * q1 + q2 * q2 + q3 * q3 + q0 * q0;
	}

	/** Normalizes this quaternion to unit length
	 * @return the quaternion for chaining */
	public Quaternion nor () {
		double len = len2();
		if (len != 0.d && !MathUtils.isEqual(len, 1d)) {
			len = (double)Math.sqrt(len);
			q0 /= len;
			q1 /= len;
			q2 /= len;
			q3 /= len;
		}
		return this;
	}

	/** Conjugate the quaternion.
	 * 
	 * @return This quaternion for chaining */
	public Quaternion conjugate () {
		q1 = -q1;
		q2 = -q2;
		q3 = -q3;
		return this;
	}
	
	
	/** get the Conjugate of the quaternion.
	 * 
	 * @return This quaternion for chaining */
	public Quaternion getConjugate () {
		return this.copy().conjugate();
	}

	// TODO : this would better fit into the vector3 class
	/** Transforms the given vector using this quaternion
	 * 
	 * @param v Vector to transform */
	public SGVec_3d transform (SGVec_3d v) {
		tmp2.set(this);
		tmp2.conjugate();
		tmp2.mulLeft(tmp1.set(0, v.x, v.y, v.z)).mulLeft(this);

		v.x = tmp2.q1;
		v.y = tmp2.q2;
		v.z = tmp2.q3;
		return v;
	}

	/** Multiplies this quaternion with another one in the form of this = this * other
	 * 
	 * @param other Quaternion to multiply with
	 * @return This quaternion for chaining */
	public Quaternion mul (final Quaternion other) {
		final double newX = this.q0 * other.q1 + this.q1 * other.q0 + this.q2 * other.q3 - this.q3 * other.q2;
		final double newY = this.q0 * other.q2 + this.q2 * other.q0 + this.q3 * other.q1 - this.q1 * other.q3;
		final double newZ = this.q0 * other.q3 + this.q3 * other.q0 + this.q1 * other.q2 - this.q2 * other.q1;
		final double newW = this.q0 * other.q0 - this.q1 * other.q1 - this.q2 * other.q2 - this.q3 * other.q3;
		this.q1 = newX;
		this.q2 = newY;
		this.q3 = newZ;
		this.q0 = newW;
		return this;
	}



	/** Multiplies this quaternion with another one in the form of this = other * this
	 * 
	 * @param other Quaternion to multiply with
	 * @return This quaternion for chaining */
	public Quaternion mulLeft (Quaternion other) {
		final double newX = other.q0 * this.q1 + other.q1 * this.q0 + other.q2 * this.q3 - other.q3 * q2;
		final double newY = other.q0 * this.q2 + other.q2 * this.q0 + other.q3 * this.q1 - other.q1 * q3;
		final double newZ = other.q0 * this.q3 + other.q3 * this.q0 + other.q1 * this.q2 - other.q2 * q1;
		final double newW = other.q0 * this.q0 - other.q1 * this.q1 - other.q2 * this.q2 - other.q3 * q3;
		this.q1 = newX;
		this.q2 = newY;
		this.q3 = newZ;
		this.q0 = newW;
		return this;
	}

	

	/** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion */
	public Quaternion add (Quaternion quaternion) {
		this.q1 += quaternion.q1;
		this.q2 += quaternion.q2;
		this.q3 += quaternion.q3;
		this.q0 += quaternion.q0;
		return this;
	}


	// TODO : the matrix4 set(quaternion) doesnt set the last row+col of the matrix to 0,0,0,1 so... that's why there is this
// method
	/** Fills a 4x4 matrix with the rotation matrix represented by this quaternion.
	 * 
	 * @param matrix Matrix to fill */
	public void toMatrix (final double[] matrix) {
		final double xx = q1 * q1;
		final double xy = q1 * q2;
		final double xz = q1 * q3;
		final double xw = q1 * q0;
		final double yy = q2 * q2;
		final double yz = q2 * q3;
		final double yw = q2 * q0;
		final double zz = q3 * q3;
		final double zw = q3 * q0;
		// Set matrix from quaternion
		matrix[Matrix4d.M00] = 1 - 2 * (yy + zz);
		matrix[Matrix4d.M01] = 2 * (xy - zw);
		matrix[Matrix4d.M02] = 2 * (xz + yw);
		matrix[Matrix4d.M03] = 0;
		matrix[Matrix4d.M10] = 2 * (xy + zw);
		matrix[Matrix4d.M11] = 1 - 2 * (xx + zz);
		matrix[Matrix4d.M12] = 2 * (yz - xw);
		matrix[Matrix4d.M13] = 0;
		matrix[Matrix4d.M20] = 2 * (xz - yw);
		matrix[Matrix4d.M21] = 2 * (yz + xw);
		matrix[Matrix4d.M22] = 1 - 2 * (xx + yy);
		matrix[Matrix4d.M23] = 0;
		matrix[Matrix4d.M30] = 0;
		matrix[Matrix4d.M31] = 0;
		matrix[Matrix4d.M32] = 0;
		matrix[Matrix4d.M33] = 1;
	}

	/** Sets the quaternion to an identity Quaternion
	 * @return this quaternion for chaining */
	public Quaternion idt () {
		return this.set(1, 0, 0, 0);
	}

	/** @return If this quaternion is an identity Quaternion */
	public boolean isIdentity () {
		return MathUtils.isZero(q1) && MathUtils.isZero(q2) && MathUtils.isZero(q3) && MathUtils.isEqual(q0, 1d);
	}

	/** @return If this quaternion is an identity Quaternion */
	public boolean isIdentity (final double tolerance) {
		return MathUtils.isZero(q1, tolerance) && MathUtils.isZero(q2, tolerance) && MathUtils.isZero(q3, tolerance)
			&& MathUtils.isEqual(q0, 1d, tolerance);
	}



	/** Sets the quaternion components from the given axis and angle around that axis.
	 * 
	 * @param axis The axis
	 * @param radians The angle in radians
	 * @return This quaternion for chaining. */
	public Quaternion setFromAxisRad (final SGVec_3d axis, final double radians) {
		return setFromAxisRad(axis.x, axis.y, axis.z, radians);
	}


	/** Sets the quaternion components from the given axis and angle around that axis.
	 * @param x X direction of the axis
	 * @param y Y direction of the axis
	 * @param z Z direction of the axis
	 * @param radians The angle in radians
	 * @return This quaternion for chaining. */
	public Quaternion setFromAxisRad (final double x, final double y, final double z, final double radians) {
		double d = SGVec_3d.mag(x, y, z);
		if (d == 0d) return idt();
		d = 1d / d;
		double l_ang = radians < 0 ? MathUtils.PI2 - (-radians % MathUtils.PI2) : radians % MathUtils.PI2;
		double l_sin = (double)Math.sin(l_ang / 2);
		double l_cos = (double)Math.cos(l_ang / 2);
		return this.set(l_cos, d * x * l_sin, d * y * l_sin, d * z * l_sin).nor();
	}

	/** Sets the Quaternion from the given matrix, optionally removing any scaling. */
	public Quaternion setFromMatrix (boolean normalizeAxes, Matrix4d matrix) {
		return setFromAxes(normalizeAxes, matrix.val[Matrix4d.M00], matrix.val[Matrix4d.M01], matrix.val[Matrix4d.M02],
			matrix.val[Matrix4d.M10], matrix.val[Matrix4d.M11], matrix.val[Matrix4d.M12], matrix.val[Matrix4d.M20],
			matrix.val[Matrix4d.M21], matrix.val[Matrix4d.M22]);
	}
	
	/** Sets the Quaternion from the given matrix, optionally removing any scaling. */
	public Quaternion setFromMatrix (boolean normalizeAxes, double[] val) {
		return setFromAxes(normalizeAxes, val[Matrix4d.M00], val[Matrix4d.M01], val[Matrix4d.M02],
			val[Matrix4d.M10], val[Matrix4d.M11], val[Matrix4d.M12], val[Matrix4d.M20],
			val[Matrix4d.M21], val[Matrix4d.M22]);
	}

	/** Sets the Quaternion from the given rotation matrix, which must not contain scaling. */
	public Quaternion setFromMatrix (Matrix4d matrix) {
		return setFromMatrix(false, matrix);
	}

	/** Sets the Quaternion from the given matrix, optionally removing any scaling. */
	public Quaternion setFromMatrix (boolean normalizeAxes, Matrix3d matrix) {
		return setFromAxes(normalizeAxes, matrix.val[Matrix3d.M00], matrix.val[Matrix3d.M01], matrix.val[Matrix3d.M02],
			matrix.val[Matrix3d.M10], matrix.val[Matrix3d.M11], matrix.val[Matrix3d.M12], matrix.val[Matrix3d.M20],
			matrix.val[Matrix3d.M21], matrix.val[Matrix3d.M22]);
	}

	/** Sets the Quaternion from the given rotation matrix, which must not contain scaling. */
	public Quaternion setFromMatrix (Matrix3d matrix) {
		return setFromMatrix(false, matrix);
	}

	/** <p>
	 * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
	 * </p>
	 * 
	 * <p>
	 * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
	 * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
	 * </p>
	 * 
	 * @param xx x-axis x-coordinate
	 * @param xy x-axis y-coordinate
	 * @param xz x-axis z-coordinate
	 * @param yx y-axis x-coordinate
	 * @param yy y-axis y-coordinate
	 * @param yz y-axis z-coordinate
	 * @param zx z-axis x-coordinate
	 * @param zy z-axis y-coordinate
	 * @param zz z-axis z-coordinate */
	public Quaternion setFromAxes (double xx, double xy, double xz, double yx, double yy, double yz, double zx, double zy, double zz) {
		return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz);
	}

	/** <p>
	 * Sets the Quaternion from the given x-, y- and z-axis.
	 * </p>
	 * 
	 * <p>
	 * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
	 * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
	 * </p>
	 * 
	 * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
	 * @param xx x-axis x-coordinate
	 * @param xy x-axis y-coordinate
	 * @param xz x-axis z-coordinate
	 * @param yx y-axis x-coordinate
	 * @param yy y-axis y-coordinate
	 * @param yz y-axis z-coordinate
	 * @param zx z-axis x-coordinate
	 * @param zy z-axis y-coordinate
	 * @param zz z-axis z-coordinate */
	public Quaternion setFromAxes (boolean normalizeAxes, double xx, double xy, double xz, double yx, double yy, double yz, double zx,
		double zy, double zz) {
		if (normalizeAxes) {
			final double lx = 1d / SGVec_3d.mag(xx, xy, xz);
			final double ly = 1d / SGVec_3d.mag(yx, yy, yz);
			final double lz = 1d / SGVec_3d.mag(zx, zy, zz);
			xx *= lx;
			xy *= lx;
			xz *= lx;
			yx *= ly;
			yy *= ly;
			yz *= ly;
			zx *= lz;
			zy *= lz;
			zz *= lz;
		}
		// the trace is the sum of the diagonal elements; see
		// http://mathworld.wolfram.com/MatrixTrace.html
		final double t = xx + yy + zz;

		// we protect the division by s by ensuring that s>=1
		if (t >= 0) { // |w| >= .5
			double s = (double)Math.sqrt(t + 1); // |s|>=1 ...
			q0 = 0.5d * s;
			s = 0.5d / s; // so this division isn't bad
			q1 = (zy - yz) * s;
			q2 = (xz - zx) * s;
			q3 = (yx - xy) * s;
		} else if ((xx > yy) && (xx > zz)) {
			double s = (double)Math.sqrt(1.0 + xx - yy - zz); // |s|>=1
			q1 = s * 0.5d; // |x| >= .5
			s = 0.5d / s;
			q2 = (yx + xy) * s;
			q3 = (xz + zx) * s;
			q0 = (zy - yz) * s;
		} else if (yy > zz) {
			double s = (double)Math.sqrt(1.0 + yy - xx - zz); // |s|>=1
			q2 = s * 0.5d; // |y| >= .5
			s = 0.5d / s;
			q1 = (yx + xy) * s;
			q3 = (zy + yz) * s;
			q0 = (xz - zx) * s;
		} else {
			double s = (double)Math.sqrt(1.0 + zz - xx - yy); // |s|>=1
			q3 = s * 0.5d; // |z| >= .5
			s = 0.5d / s;
			q1 = (xz + zx) * s;
			q2 = (zy + yz) * s;
			q0 = (yx - xy) * s;
		}

		return this;
	}

	/** Set this quaternion to the rotation between two vectors.
	 * @param v1 The base vector, which should be normalized.
	 * @param v2 The target vector, which should be normalized.
	 * @return This quaternion for chaining */
	public Quaternion setFromCross (final SGVec_3d v1, final SGVec_3d v2) {
		final double dot = MathUtils.clamp(v1.dot(v2), -1d, 1d);
		final double angle = (double)Math.acos(dot);
		return setFromAxisRad(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle);
	}

	/** Set this quaternion to the rotation between two vectors.
	 * @param x1 The base vectors x value, which should be normalized.
	 * @param y1 The base vectors y value, which should be normalized.
	 * @param z1 The base vectors z value, which should be normalized.
	 * @param x2 The target vector x value, which should be normalized.
	 * @param y2 The target vector y value, which should be normalized.
	 * @param z2 The target vector z value, which should be normalized.
	 * @return This quaternion for chaining */
	public Quaternion setFromCross (final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		final double dot = MathUtils.clamp(SGVec_3d.dot(x1, y1, z1, x2, y2, z2), -1d, 1d);
		final double angle = (double)Math.acos(dot);
		return setFromAxisRad(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle);
	}

	/** Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
	 * [0,1]. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
	 * @param end the end quaternion
	 * @param alpha alpha in the range [0,1]
	 * @return this quaternion for chaining */
	public Quaternion slerp (Quaternion end, double alpha) {
		final double d = this.q1 * end.q1 + this.q2 * end.q2 + this.q3 * end.q3 + this.q0 * end.q0;
		double absDot = d < 0.d ? -d : d;

		// Set the first and second scale for the interpolation
		double scale0 = 1d - alpha;
		double scale1 = alpha;

		// Check if the angle between the 2 quaternions was big enough to
		// warrant such calculations
		if ((1 - absDot) > 0.d) {// Get the angle between the 2 quaternions,
			// and then store the sin() of that angle
			final double angle = (double)Math.acos(absDot);
			final double invSinTheta = 1d / (double)Math.sin(angle);

			// Calculate the scale for q1 and q2, according to the angle and
			// it's sine value
			scale0 = ((double)Math.sin((1d - alpha) * angle) * invSinTheta);
			scale1 = ((double)Math.sin((alpha * angle)) * invSinTheta);
		}

		if (d < 0.d) scale1 = -scale1;

		// Calculate the x, y, z and w values for the quaternion by using a
		// special form of linear interpolation for quaternions.
		q1 = (scale0 * q1) + (scale1 * end.q1);
		q2 = (scale0 * q2) + (scale1 * end.q2);
		q3 = (scale0 * q3) + (scale1 * end.q3);
		q0 = (scale0 * q0) + (scale1 * end.q0);

		// Return the interpolated quaternion
		return this;
	}

	/** Spherical linearly interpolates multiple quaternions and stores the result in this Quaternion. Will not destroy the data
	 * previously inside the elements of q. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where w_i=1/n.
	 * @param q List of quaternions
	 * @return This quaternion for chaining */
	public Quaternion slerp (Quaternion[] q) {

		// Calculate exponents and multiply everything from left to right
		final double w = 1.0d / q.length;
		set(q[0]).exp(w);
		for (int i = 1; i < q.length; i++)
			mul(tmp1.set(q[i]).exp(w));
		nor();
		return this;
	}

	/** Spherical linearly interpolates multiple quaternions by the given weights and stores the result in this Quaternion. Will not
	 * destroy the data previously inside the elements of q or w. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where the sum of w_i
	 * is 1. Lists must be equal in length.
	 * @param q List of quaternions
	 * @param w List of weights
	 * @return This quaternion for chaining */
	public Quaternion slerp (Quaternion[] q, double[] w) {

		// Calculate exponents and multiply everything from left to right
		set(q[0]).exp(w[0]);
		for (int i = 1; i < q.length; i++)
			mul(tmp1.set(q[i]).exp(w[i]));
		nor();
		return this;
	}

	/** Calculates (this quaternion)^alpha where alpha is a real number and stores the result in this quaternion. See
	 * http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
	 * @param alpha Exponent
	 * @return This quaternion for chaining */
	public Quaternion exp (double alpha) {

		// Calculate |q|^alpha
		double norm = len();
		double normExp = (double)Math.pow(norm, alpha);

		// Calculate theta
		double theta = (double)Math.acos(q0 / norm);

		// Calculate coefficient of basis elements
		double coeff = 0;
		if (Math.abs(theta) < 0.001) // If theta is small enough, use the limit of sin(alpha*theta) / sin(theta) instead of actual
// value
			coeff = normExp * alpha / norm;
		else
			coeff = (double)(normExp * Math.sin(alpha * theta) / (norm * Math.sin(theta)));

		// Write results
		q0 = (double)(normExp * Math.cos(alpha * theta));
		q1 *= coeff;
		q2 *= coeff;
		q3 *= coeff;

		// Fix any possible discrepancies
		nor();

		return this;
	}

	


	/** Get the dot product between the two quaternions (commutative).
	 * @param x1 the x component of the first quaternion
	 * @param y1 the y component of the first quaternion
	 * @param z1 the z component of the first quaternion
	 * @param w1 the w component of the first quaternion
	 * @param x2 the x component of the second quaternion
	 * @param y2 the y component of the second quaternion
	 * @param z2 the z component of the second quaternion
	 * @param w2 the w component of the second quaternion
	 * @return the dot product between the first and second quaternion. */
	public final static double dot (final double x1, final double y1, final double z1, final double w1, final double x2, final double y2,
		final double z2, final double w2) {
		return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
	}

	/** Get the dot product between this and the other quaternion (commutative).
	 * @param other the other quaternion.
	 * @return the dot product of this and the other quaternion. */
	public double dot (final Quaternion other) {
		return this.q1 * other.q1 + this.q2 * other.q2 + this.q3 * other.q3 + this.q0 * other.q0;
	}

	/** Get the dot product between this and the other quaternion (commutative).
	 * @param x the x component of the other quaternion
	 * @param y the y component of the other quaternion
	 * @param z the z component of the other quaternion
	 * @param w the w component of the other quaternion
	 * @return the dot product of this and the other quaternion. */
	public double dot (final double x, final double y, final double z, final double w) {
		return this.q1 * x + this.q2 * y + this.q3 * z + this.q0 * w;
	}

	/** Multiplies the components of this quaternion with the given scalar.
	 * @param scalar the scalar.
	 * @return this quaternion for chaining. */
	public Quaternion mul (double scalar) {
		this.q1 *= scalar;
		this.q2 *= scalar;
		this.q3 *= scalar;
		this.q0 *= scalar;
		return this;
	}
	
	public Quaternion getMultiplied(double scalar) {
		return this.copy().mul(scalar);
	}


	/** Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
	 * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
	 * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
	 * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
	 * zero.
	 * 
	 * @param axis vector which will receive the axis
	 * @return the angle in radians
	 * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a> */
	public double getAxisAngleRad (SGVec_3d axis) {
		if (this.q0 > 1) this.nor(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
		double angle = (double)(2.0 * Math.acos(this.q0));
		double s = Math.sqrt(1 - this.q0 * this.q0); // assuming quaternion normalised then w is less than 1, so term always positive.
		if (s < MathUtils.DOUBLE_ROUNDING_ERROR) { // test to avoid divide by zero, s is always positive due to sqrt
			// if s close to zero then direction of axis not important
			axis.x = this.q1; // if it is important that axis is normalised then replace with x=1; y=z=0;
			axis.y = this.q2;
			axis.z = this.q3;
		} else {
			axis.x = (double)(this.q1 / s); // normalise axis
			axis.y = (double)(this.q2 / s);
			axis.z = (double)(this.q3 / s);
		}

		return angle;
	}

	/** Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
	 * {@link #getAxisAngleRad(SGVec_3d)} to get both the axis and the angle of this rotation. Use
	 * {@link #getAngleAroundRad(SGVec_3d)} to get the angle around a specific axis.
	 * @return the angle in radians of the rotation */
	public double getAngleRad () {
		return (double)(2.0 * Math.acos((this.q0 > 1) ? (this.q0 / len()) : this.q0));
	}

	/** Get the angle in degrees of the rotation this quaternion represents. Use {@link #getAxisAngle(SGVec_3d)} to get both the axis
	 * and the angle of this rotation. Use {@link #getAngleAround(SGVec_3d)} to get the angle around a specific axis.
	 * @return the angle in degrees of the rotation */
	public double getAngle () {
		return getAngleRad();
	}

	/** Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis. </p> The swing and twist rotation can be used to reconstruct the original
	 * quaternion: this = swing * twist
	 * 
	 * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
	 * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
	 * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
	 * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
	 * @param twist will receive the twist rotation: the rotation around the specified axis
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a> */
	public void getSwingTwist (final double axisX, final double axisY, final double axisZ, final Quaternion swing,
		final Quaternion twist) {
		final double d = SGVec_3d.dot(this.q1, this.q2, this.q3, axisX, axisY, axisZ);
		twist.set(this.q0, axisX * d, axisY * d, axisZ * d).nor();
		if (d < 0) twist.mul(-1d);
		swing.set(twist).conjugate().mulLeft(this);
	}

	/** Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation around the
	 * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation around an
	 * axis perpendicular to the specified axis. </p> The swing and twist rotation can be used to reconstruct the original
	 * quaternion: this = swing * twist
	 * 
	 * @param axis the normalized axis for which to get the swing and twist rotation
	 * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
	 * @param twist will receive the twist rotation: the rotation around the specified axis
	 * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a> */
	public void getSwingTwist (final SGVec_3d axis, final Quaternion swing, final Quaternion twist) {
		getSwingTwist(axis.x, axis.y, axis.z, swing, twist);
	}

	/** Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
	 * @param axisX the x component of the normalized axis for which to get the angle
	 * @param axisY the y component of the normalized axis for which to get the angle
	 * @param axisZ the z component of the normalized axis for which to get the angle
	 * @return the angle in radians of the rotation around the specified axis */
	public double getAngleAroundRad (final double axisX, final double axisY, final double axisZ) {
		final double d = SGVec_3d.dot(this.q1, this.q2, this.q3, axisX, axisY, axisZ);
		final double l2 = Quaternion.len2(axisX * d, axisY * d, axisZ * d, this.q0);
		return MathUtils.isZero(l2) ? 0d : (double)(2.0 * Math.acos(MathUtils.clamp(
			(double)((d < 0 ? -this.q0 : this.q0) / Math.sqrt(l2)), -1d, 1d)));
	}

	/** Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
	 * @param axis the normalized axis for which to get the angle
	 * @return the angle in radians of the rotation around the specified axis */
	public double getAngleAroundRad (final SGVec_3d axis) {
		return getAngleAroundRad(axis.x, axis.y, axis.z);
	}

	/** Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
	 * @param axisX the x component of the normalized axis for which to get the angle
	 * @param axisY the y component of the normalized axis for which to get the angle
	 * @param axisZ the z component of the normalized axis for which to get the angle
	 * @return the angle in degrees of the rotation around the specified axis */
	public double getAngleAround (final double axisX, final double axisY, final double axisZ) {
		return getAngleAroundRad(axisX, axisY, axisZ);
	}

	/** Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
	 * @param axis the normalized axis for which to get the angle
	 * @return the angle in degrees of the rotation around the specified axis */
	public double getAngleAround (final SGVec_3d axis) {
		return getAngleAround(axis.x, axis.y, axis.z);
	}
	
	public double getQ0() {
		return q0; 
	}
	
	public double getQ1() {
		return q1; 
	}
	
	public double getQ2() {
		return q2; 
	}
	
	public double getQ3() {
		return q3; 
	}
}
