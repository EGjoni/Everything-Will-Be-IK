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

/** Encapsulates a <a href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> 4 by 4 matrix. Like
 * the {@link SGVec_3f} class it allows the chaining of methods by returning a reference to itself. For example:
 * 
 * <pre>
 * Matrix4f mat = new Matrix4f().trn(position).mul(camera.combined);
 * </pre>
 * 
 * @author badlogicgames@gmail.com */
public class Matrix4f implements Serializable {
	private static final long serialVersionUID = -2717655254359578617L;
	/** XX: Typically the unrotated X component for scaling, also the cosine of the angle when rotated on the Y and/or Z axis. On
	 * Vector3 multiplication this value is multiplied with the source X component and added to the target X component. */
	public static final int M00 = 0;
	/** XY: Typically the negative sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied
	 * with the source Y component and added to the target X component. */
	public static final int M01 = 4;
	/** XZ: Typically the sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied with the
	 * source Z component and added to the target X component. */
	public static final int M02 = 8;
	/** XW: Typically the translation of the X component. On Vector3 multiplication this value is added to the target X component. */
	public static final int M03 = 12;
	/** YX: Typically the sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied with the
	 * source X component and added to the target Y component. */
	public static final int M10 = 1;
	/** YY: Typically the unrotated Y component for scaling, also the cosine of the angle when rotated on the X and/or Z axis. On
	 * Vector3 multiplication this value is multiplied with the source Y component and added to the target Y component. */
	public static final int M11 = 5;
	/** YZ: Typically the negative sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied
	 * with the source Z component and added to the target Y component. */
	public static final int M12 = 9;
	/** YW: Typically the translation of the Y component. On Vector3 multiplication this value is added to the target Y component. */
	public static final int M13 = 13;
	/** ZX: Typically the negative sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied
	 * with the source X component and added to the target Z component. */
	public static final int M20 = 2;
	/** ZY: Typical the sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied with the
	 * source Y component and added to the target Z component. */
	public static final int M21 = 6;
	/** ZZ: Typically the unrotated Z component for scaling, also the cosine of the angle when rotated on the X and/or Y axis. On
	 * Vector3 multiplication this value is multiplied with the source Z component and added to the target Z component. */
	public static final int M22 = 10;
	/** ZW: Typically the translation of the Z component. On Vector3 multiplication this value is added to the target Z component. */
	public static final int M23 = 14;
	/** WX: Typically the value zero. On Vector3 multiplication this value is ignored. */
	public static final int M30 = 3;
	/** WY: Typically the value zero. On Vector3 multiplication this value is ignored. */
	public static final int M31 = 7;
	/** WZ: Typically the value zero. On Vector3 multiplication this value is ignored. */
	public static final int M32 = 11;
	/** WW: Typically the value one. On Vector3 multiplication this value is ignored. */
	public static final int M33 = 15;

	private static final float tmp[] = new float[16];
	public final float val[] = new float[16];



	/** Constructs an identity matrix */
	public Matrix4f () {
		val[M00] = 1f;
		val[M11] = 1f;
		val[M22] = 1f;
		val[M33] = 1f;
	}

	/** Constructs a matrix from the given matrix.
	 * 
	 * @param matrix The matrix to copy. (This matrix is not modified) */
	public Matrix4f (Matrix4f matrix) {
		this.set(matrix);
	}

	/** Constructs a matrix from the given float array. The array must have at least 16 elements; the first 16 will be copied.
	 * @param values The float array to copy. Remember that this matrix is in <a
	 *           href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order. (The float array is not modified) */
	public Matrix4f (float[] values) {
		this.set(values);
	}

	/** Constructs a rotation matrix from the given {@link Quaternionf}.
	 * @param quaternion The quaternion to be copied. (The quaternion is not modified) */
	public Matrix4f (Quaternionf quaternion) {
		this.set(quaternion);
	}

	/** Construct a matrix from the given translation, rotation and scale.
	 * @param position The translation
	 * @param rotation The rotation, must be normalized
	 * @param scale The scale */
	public Matrix4f (SGVec_3f position, Quaternionf rotation, SGVec_3f scale) {
		set(position, rotation, scale);
	}

	/** Sets the matrix to the given matrix.
	 * 
	 * @param matrix The matrix that is to be copied. (The given matrix is not modified)
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f set (Matrix4f matrix) {
		return this.set(matrix.val);
	}

	/** Sets the matrix to the given matrix as a float array. The float array must have at least 16 elements; the first 16 will be
	 * copied.
	 * 
	 * @param values The matrix, in float form, that is to be copied. Remember that this matrix is in <a
	 *           href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f set (float[] values) {
		System.arraycopy(values, 0, val, 0, val.length);
		return this;
	}

	/**
     * Copies the matrix values in the specified column into the array
     * @param column  the matrix column
     * @param v    the vector into which the matrix column values will be copied
     */
	public void getColumn(int column, float[] arrVec)
	{
		switch (column) {
		case 0:
			arrVec[0] = val[M00];
			arrVec[1] = val[M10];
			arrVec[2] = val[M20];
			arrVec[3] = val[M30];
			    break;
                
		case 1:
			arrVec[0] = val[M01];
			arrVec[1] = val[M11];
			arrVec[2] = val[M21];
			arrVec[3] = val[M31];
			    break;
                
		case 2:
			arrVec[0] = val[M02];
			arrVec[1] = val[M12];
			arrVec[2] = val[M22];
			arrVec[3] = val[M32];
			    break;
                
		case 3:
			arrVec[0] = val[M03];
			arrVec[1] = val[M13];
			arrVec[2] = val[M23];
			arrVec[3] = val[M33];
			break;
		}
	}
	
	public void setColumn(int column, float[] v) 
	{   
		setColumn(column, v[0], v[1], v[2], v[3]);
	}
	
	public void setColumn(int column, float x, float y, float z, float w)
	{
		switch (column) {
		case 0:
			val[M00] = x;
			val[M10] = y;
			val[M20] = z;
			val[M30] = w;
			break;

		case 1:
			val[M01] = x;
			val[M11] = y;
			val[M21] = z;
			val[M31] = w;
			break;

		case 2:
			val[M02] = x;
			val[M12] = y;
			val[M22] = z;
			val[M32] = w;
			break;

		case 3:
			val[M03] = x;
			val[M13] = y;
			val[M23] = z;
			val[M33] = w;
			break;
		}
	}

	/** Sets the matrix to a rotation matrix representing the quaternion.
	 * 
	 * @param quaternion The quaternion that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f set (Quaternionf quaternion) {
		return set(quaternion.getQ1(), quaternion.getQ2(), quaternion.getQ3(), quaternion.getQ0());
	}

	/** Sets the matrix to a rotation matrix representing the quaternion.
	 * 
	 * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
	 * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
	 * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
	 * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f set (float quaternionX, float quaternionY, float quaternionZ, float quaternionW) {
		return set(0f, 0f, 0f, quaternionX, quaternionY, quaternionZ, quaternionW);
	}

	/** Set this matrix to the specified translation and rotation.
	 * @param position The translation
	 * @param orientation The rotation, must be normalized
	 * @return This matrix for chaining */
	public Matrix4f set (SGVec_3f position, Quaternionf orientation) {
		return set(position.x, position.y, position.z, 
				orientation.getQ1(), orientation.getQ2(), orientation.getQ3(), orientation.getQ0());
	}

	/** Sets the matrix to a rotation matrix representing the translation and quaternion.
	 * 
	 * @param translationX The X component of the translation that is to be used to set this matrix.
	 * @param translationY The Y component of the translation that is to be used to set this matrix.
	 * @param translationZ The Z component of the translation that is to be used to set this matrix.
	 * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
	 * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
	 * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
	 * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f set (float translationX, float translationY, float translationZ, float quaternionX, float quaternionY,
			float quaternionZ, float quaternionW) {
		final float xs = quaternionX * 2f, ys = quaternionY * 2f, zs = quaternionZ * 2f;
		final float wx = quaternionW * xs, wy = quaternionW * ys, wz = quaternionW * zs;
		final float xx = quaternionX * xs, xy = quaternionX * ys, xz = quaternionX * zs;
		final float yy = quaternionY * ys, yz = quaternionY * zs, zz = quaternionZ * zs;

		val[M00] = (1.0f - (yy + zz));
		val[M01] = (xy - wz);
		val[M02] = (xz + wy);
		val[M03] = translationX;

		val[M10] = (xy + wz);
		val[M11] = (1.0f - (xx + zz));
		val[M12] = (yz - wx);
		val[M13] = translationY;

		val[M20] = (xz - wy);
		val[M21] = (yz + wx);
		val[M22] = (1.0f - (xx + yy));
		val[M23] = translationZ;

		val[M30] = 0f;
		val[M31] = 0f;
		val[M32] = 0f;
		val[M33] = 1.0f;
		return this;
	}

	/** Set this matrix to the specified translation, rotation and scale.
	 * @param position The translation
	 * @param orientation The rotation, must be normalized
	 * @param scale The scale
	 * @return This matrix for chaining */
	public Matrix4f set (SGVec_3f position, Quaternionf orientation, SGVec_3f scale) {
		return set(position.x, position.y, position.z, 
				orientation.getQ1(), orientation.getQ2(), orientation.getQ3(), orientation.getQ0(), scale.x,
				scale.y, scale.z);
	}

	/** Sets the matrix to a rotation matrix representing the translation and quaternion.
	 * 
	 * @param translationX The X component of the translation that is to be used to set this matrix.
	 * @param translationY The Y component of the translation that is to be used to set this matrix.
	 * @param translationZ The Z component of the translation that is to be used to set this matrix.
	 * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
	 * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
	 * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
	 * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
	 * @param scaleX The X component of the scaling that is to be used to set this matrix.
	 * @param scaleY The Y component of the scaling that is to be used to set this matrix.
	 * @param scaleZ The Z component of the scaling that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f set (float translationX, float translationY, float translationZ, float quaternionX, float quaternionY,
			float quaternionZ, float quaternionW, float scaleX, float scaleY, float scaleZ) {
		final float xs = quaternionX * 2f, ys = quaternionY * 2f, zs = quaternionZ * 2f;
		final float wx = quaternionW * xs, wy = quaternionW * ys, wz = quaternionW * zs;
		final float xx = quaternionX * xs, xy = quaternionX * ys, xz = quaternionX * zs;
		final float yy = quaternionY * ys, yz = quaternionY * zs, zz = quaternionZ * zs;

		val[M00] = scaleX * (1.0f - (yy + zz));
		val[M01] = scaleY * (xy - wz);
		val[M02] = scaleZ * (xz + wy);
		val[M03] = translationX;

		val[M10] = scaleX * (xy + wz);
		val[M11] = scaleY * (1.0f - (xx + zz));
		val[M12] = scaleZ * (yz - wx);
		val[M13] = translationY;

		val[M20] = scaleX * (xz - wy);
		val[M21] = scaleY * (yz + wx);
		val[M22] = scaleZ * (1.0f - (xx + yy));
		val[M23] = translationZ;

		val[M30] = 0f;
		val[M31] = 0f;
		val[M32] = 0f;
		val[M33] = 1.0f;
		return this;
	}

	/** Sets the four columns of the matrix which correspond to the x-, y- and z-axis of the vector space this matrix creates as
	 * well as the 4th column representing the translation of any point that is multiplied by this matrix.
	 * 
	 * @param xAxis The x-axis.
	 * @param yAxis The y-axis.
	 * @param zAxis The z-axis.
	 * @param pos The translation vector. */
	public Matrix4f set (SGVec_3f xAxis, SGVec_3f yAxis, SGVec_3f zAxis, SGVec_3f pos) {
		val[M00] = xAxis.x;
		val[M01] = xAxis.y;
		val[M02] = xAxis.z;
		val[M10] = yAxis.x;
		val[M11] = yAxis.y;
		val[M12] = yAxis.z;
		val[M20] = zAxis.x;
		val[M21] = zAxis.y;
		val[M22] = zAxis.z;
		val[M03] = pos.x;
		val[M13] = pos.y;
		val[M23] = pos.z;
		val[M30] = 0;
		val[M31] = 0;
		val[M32] = 0;
		val[M33] = 1;
		return this;
	}

	/** @return a copy of this matrix */
	public Matrix4f cpy () {
		return new Matrix4f(this);
	}

	/** Adds a translational component to the matrix in the 4th column. The other columns are untouched.
	 * 
	 * @param vector The translation vector to add to the current matrix. (This vector is not modified)
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f trn (SGVec_3f vector) {
		val[M03] += vector.x;
		val[M13] += vector.y;
		val[M23] += vector.z;
		return this;
	}

	/** Adds a translational component to the matrix in the 4th column. The other columns are untouched.
	 * 
	 * @param x The x-component of the translation vector.
	 * @param y The y-component of the translation vector.
	 * @param z The z-component of the translation vector.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f trn (float x, float y, float z) {
		val[M03] += x;
		val[M13] += y;
		val[M23] += z;
		return this;
	}

	/** @return the backing float array */
	public float[] getValues () {
		return val;
	}

	/** Postmultiplies this matrix with the given matrix, storing the result in this matrix. For example:
	 * 
	 * <pre>
	 * A.mul(B) results in A := AB.
	 * </pre>
	 * 
	 * @param matrix The other matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together. */
	public Matrix4f mul (Matrix4f matrix) {
		mul(val, matrix.val);
		return this;
	}
	
	public Matrix4f mul (float [] matrixVal) {
		mul(val, matrixVal);
		return this;
	}
	



	/** Premultiplies this matrix with the given matrix, storing the result in this matrix. For example:
	 * 
	 * <pre>
	 * A.mulLeft(B) results in A := BA.
	 * </pre>
	 * 
	 * @param matrix The other matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together. */
	public Matrix4f mulLeft (Matrix4f matrix) {
		tmpMat.set(matrix);
		mul(tmpMat.val, this.val);
		return set(tmpMat);
	}
	
	public Matrix4f mulLeft (float[] matrix) {
		tmpMat.set(matrix);
		mul(tmpMat.val, this.val);
		return set(tmpMat);
	}

	/** Transposes the matrix.
	 * 
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f tra () {
		tmp[M00] = val[M00];
		tmp[M01] = val[M10];
		tmp[M02] = val[M20];
		tmp[M03] = val[M30];
		tmp[M10] = val[M01];
		tmp[M11] = val[M11];
		tmp[M12] = val[M21];
		tmp[M13] = val[M31];
		tmp[M20] = val[M02];
		tmp[M21] = val[M12];
		tmp[M22] = val[M22];
		tmp[M23] = val[M32];
		tmp[M30] = val[M03];
		tmp[M31] = val[M13];
		tmp[M32] = val[M23];
		tmp[M33] = val[M33];
		return set(tmp);
	}

	/** Sets the matrix to an identity matrix.
	 * 
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f idt () {
		val[M00] = 1;
		val[M01] = 0;
		val[M02] = 0;
		val[M03] = 0;
		val[M10] = 0;
		val[M11] = 1;
		val[M12] = 0;
		val[M13] = 0;
		val[M20] = 0;
		val[M21] = 0;
		val[M22] = 1;
		val[M23] = 0;
		val[M30] = 0;
		val[M31] = 0;
		val[M32] = 0;
		val[M33] = 1;
		return this;
	}

	/** Inverts the matrix. Stores the result in this matrix.
	 * 
	 * @return This matrix for the purpose of chaining methods together.
	 * @throws RuntimeException if the matrix is singular (not invertible) */
	public Matrix4f inv () {
		float l_det = val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11]
				* val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10]
						* val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
								+ val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32]
										* val[M13] + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31]
												* val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10]
														* val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33]
																+ val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12]
																		* val[M33] - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
		if (l_det == 0f) throw new RuntimeException("non-invertible matrix");
		float inv_det = 1.0f / l_det;
		tmp[M00] = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32] - val[M11]
				* val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
		tmp[M01] = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32] + val[M01]
				* val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
		tmp[M02] = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32] - val[M01]
				* val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
		tmp[M03] = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22] + val[M01]
				* val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
		tmp[M10] = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32] + val[M10]
				* val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
		tmp[M11] = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32] - val[M00]
				* val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
		tmp[M12] = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32] + val[M00]
				* val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
		tmp[M13] = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22] - val[M00]
				* val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
		tmp[M20] = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31] - val[M10]
				* val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
		tmp[M21] = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31] + val[M00]
				* val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
		tmp[M22] = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31] - val[M00]
				* val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
		tmp[M23] = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21] + val[M00]
				* val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
		tmp[M30] = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31] + val[M10]
				* val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
		tmp[M31] = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31] - val[M00]
				* val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
		tmp[M32] = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31] + val[M00]
				* val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
		tmp[M33] = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00]
				* val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];
		val[M00] = tmp[M00] * inv_det;
		val[M01] = tmp[M01] * inv_det;
		val[M02] = tmp[M02] * inv_det;
		val[M03] = tmp[M03] * inv_det;
		val[M10] = tmp[M10] * inv_det;
		val[M11] = tmp[M11] * inv_det;
		val[M12] = tmp[M12] * inv_det;
		val[M13] = tmp[M13] * inv_det;
		val[M20] = tmp[M20] * inv_det;
		val[M21] = tmp[M21] * inv_det;
		val[M22] = tmp[M22] * inv_det;
		val[M23] = tmp[M23] * inv_det;
		val[M30] = tmp[M30] * inv_det;
		val[M31] = tmp[M31] * inv_det;
		val[M32] = tmp[M32] * inv_det;
		val[M33] = tmp[M33] * inv_det;
		return this;
	}

	/**
	 * Sets the value of this matrix to the matrix inverse of the passed (user declared) matrix m1
	 * @param m1 matrix to invert
	 * */
	public void toInverseOf(Matrix4f m1) {
		inv(m1.val, val);
	}

	/** @return The determinant of this matrix */
	public float det () {
		return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11]
				* val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10]
						* val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
								+ val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32]
										* val[M13] + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31]
												* val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10]
														* val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33]
																+ val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12]
																		* val[M33] - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
	}

	/** @return The determinant of the 3x3 upper left matrix */
	public float det3x3 () {
		return val[M00] * val[M11] * val[M22] + val[M01] * val[M12] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00]
				* val[M12] * val[M21] - val[M01] * val[M10] * val[M22] - val[M02] * val[M11] * val[M20];
	}

	/** Sets the matrix to a projection matrix with a near- and far plane, a field of view in degrees and an aspect ratio. Note that
	 * the field of view specified is the angle in degrees for the height, the field of view for the width will be calculated
	 * according to the aspect ratio.
	 * 
	 * @param near The near plane
	 * @param far The far plane
	 * @param fovy The field of view of the height in degrees
	 * @param aspectRatio The "width over height" aspect ratio
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToProjection (float near, float far, float fovy, float aspectRatio) {
		idt();
		float l_fd = (float)(1.0 / Math.tan((fovy * (MathUtils.PI / 180f)) / 2.0f));
		float l_a1 = (far + near) / (near - far);
		float l_a2 = (2 * far * near) / (near - far);
		val[M00] = l_fd / aspectRatio;
		val[M10] = 0;
		val[M20] = 0;
		val[M30] = 0;
		val[M01] = 0;
		val[M11] = l_fd;
		val[M21] = 0;
		val[M31] = 0;
		val[M02] = 0;
		val[M12] = 0;
		val[M22] = l_a1;
		val[M32] = -1;
		val[M03] = 0;
		val[M13] = 0;
		val[M23] = l_a2;
		val[M33] = 0;

		return this;
	}

	/** Sets the matrix to a projection matrix with a near/far plane, and left, bottom, right and top specifying the points on the
	 * near plane that are mapped to the lower left and upper right corners of the viewport. This allows to create projection
	 * matrix with off-center vanishing point.
	 * 
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @param near The near plane
	 * @param far The far plane
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToProjection (float left, float right, float bottom, float top, float near, float far) {
		float x = 2.0f * near / (right - left);
		float y = 2.0f * near / (top - bottom);
		float a = (right + left) / (right - left);
		float b = (top + bottom) / (top - bottom);
		float l_a1 = (far + near) / (near - far);
		float l_a2 = (2 * far * near) / (near - far);
		val[M00] = x;
		val[M10] = 0;
		val[M20] = 0;
		val[M30] = 0;
		val[M01] = 0;
		val[M11] = y;
		val[M21] = 0;
		val[M31] = 0;
		val[M02] = a;
		val[M12] = b;
		val[M22] = l_a1;
		val[M32] = -1;
		val[M03] = 0;
		val[M13] = 0;
		val[M23] = l_a2;
		val[M33] = 0;

		return this;
	}

	/** Sets this matrix to an orthographic projection matrix with the origin at (x,y) extending by width and height. The near plane
	 * is set to 0, the far plane is set to 1.
	 * 
	 * @param x The x-coordinate of the origin
	 * @param y The y-coordinate of the origin
	 * @param width The width
	 * @param height The height
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToOrtho2D (float x, float y, float width, float height) {
		setToOrtho(x, x + width, y, y + height, 0, 1);
		return this;
	}

	/** Sets this matrix to an orthographic projection matrix with the origin at (x,y) extending by width and height, having a near
	 * and far plane.
	 * 
	 * @param x The x-coordinate of the origin
	 * @param y The y-coordinate of the origin
	 * @param width The width
	 * @param height The height
	 * @param near The near plane
	 * @param far The far plane
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToOrtho2D (float x, float y, float width, float height, float near, float far) {
		setToOrtho(x, x + width, y, y + height, near, far);
		return this;
	}

	/** Sets the matrix to an orthographic projection like glOrtho (http://www.opengl.org/sdk/docs/man/xhtml/glOrtho.xml) following
	 * the OpenGL equivalent
	 * 
	 * @param left The left clipping plane
	 * @param right The right clipping plane
	 * @param bottom The bottom clipping plane
	 * @param top The top clipping plane
	 * @param near The near clipping plane
	 * @param far The far clipping plane
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToOrtho (float left, float right, float bottom, float top, float near, float far) {

		this.idt();
		float x_orth = 2 / (right - left);
		float y_orth = 2 / (top - bottom);
		float z_orth = -2 / (far - near);

		float tx = -(right + left) / (right - left);
		float ty = -(top + bottom) / (top - bottom);
		float tz = -(far + near) / (far - near);

		val[M00] = x_orth;
		val[M10] = 0;
		val[M20] = 0;
		val[M30] = 0;
		val[M01] = 0;
		val[M11] = y_orth;
		val[M21] = 0;
		val[M31] = 0;
		val[M02] = 0;
		val[M12] = 0;
		val[M22] = z_orth;
		val[M32] = 0;
		val[M03] = tx;
		val[M13] = ty;
		val[M23] = tz;
		val[M33] = 1;

		return this;
	}

	/** Sets the 4th column to the translation vector.
	 * 
	 * @param vector The translation vector
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setTranslation (SGVec_3f vector) {
		val[M03] = vector.x;
		val[M13] = vector.y;
		val[M23] = vector.z;
		return this;
	}

	/** Sets the 4th column to the translation vector.
	 * 
	 * @param x The X coordinate of the translation vector
	 * @param y The Y coordinate of the translation vector
	 * @param z The Z coordinate of the translation vector
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setTranslation (float x, float y, float z) {
		val[M03] = x;
		val[M13] = y;
		val[M23] = z;
		return this;
	}

	/** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the
	 * translation vector.
	 * 
	 * @param vector The translation vector
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToTranslation (SGVec_3f vector) {
		idt();
		val[M03] = vector.x;
		val[M13] = vector.y;
		val[M23] = vector.z;
		return this;
	}

	/** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the
	 * translation vector.
	 * 
	 * @param x The x-component of the translation vector.
	 * @param y The y-component of the translation vector.
	 * @param z The z-component of the translation vector.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToTranslation (float x, float y, float z) {
		idt();
		val[M03] = x;
		val[M13] = y;
		val[M23] = z;
		return this;
	}

	/** Sets this matrix to a translation and scaling matrix by first overwriting it with an identity and then setting the
	 * translation vector in the 4th column and the scaling vector in the diagonal.
	 * 
	 * @param translation The translation vector
	 * @param scaling The scaling vector
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToTranslationAndScaling (SGVec_3f translation, SGVec_3f scaling) {
		idt();
		val[M03] = translation.x;
		val[M13] = translation.y;
		val[M23] = translation.z;
		val[M00] = scaling.x;
		val[M11] = scaling.y;
		val[M22] = scaling.z;
		return this;
	}

	/** Sets this matrix to a translation and scaling matrix by first overwriting it with an identity and then setting the
	 * translation vector in the 4th column and the scaling vector in the diagonal.
	 * 
	 * @param translationX The x-component of the translation vector
	 * @param translationY The y-component of the translation vector
	 * @param translationZ The z-component of the translation vector
	 * @param scalingX The x-component of the scaling vector
	 * @param scalingY The x-component of the scaling vector
	 * @param scalingZ The x-component of the scaling vector
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToTranslationAndScaling (float translationX, float translationY, float translationZ, float scalingX,
			float scalingY, float scalingZ) {
		idt();
		val[M03] = translationX;
		val[M13] = translationY;
		val[M23] = translationZ;
		val[M00] = scalingX;
		val[M11] = scalingY;
		val[M22] = scalingZ;
		return this;
	}

	static Quaternionf quat = new Quaternionf();
	static Quaternionf quat2 = new Quaternionf();

	/** Sets the matrix to a rotation matrix around the given axis.
	 * 
	 * @param axis The axis
	 * @param degrees The angle in degrees
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToRotation (SGVec_3f axis, float degrees) {
		if (degrees == 0) {
			idt();
			return this;
		}
		return set(quat.set(axis, degrees));
	}

	/** Sets the matrix to a rotation matrix around the given axis.
	 * 
	 * @param axis The axis
	 * @param radians The angle in radians
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToRotationRad (SGVec_3f axis, float radians) {
		if (radians == 0) {
			idt();
			return this;
		}
		return set(quat.setFromAxisRad(axis, radians));
	}

	/** Sets the matrix to a rotation matrix around the given axis.
	 * 
	 * @param axisX The x-component of the axis
	 * @param axisY The y-component of the axis
	 * @param axisZ The z-component of the axis
	 * @param degrees The angle in degrees
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToRotation (float axisX, float axisY, float axisZ, float degrees) {
		if (degrees == 0) {
			idt();
			return this;
		}
		return set(quat.setFromAxis(axisX, axisY, axisZ, degrees));
	}

	/** Sets the matrix to a rotation matrix around the given axis.
	 * 
	 * @param axisX The x-component of the axis
	 * @param axisY The y-component of the axis
	 * @param axisZ The z-component of the axis
	 * @param radians The angle in radians
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToRotationRad (float axisX, float axisY, float axisZ, float radians) {
		if (radians == 0) {
			idt();
			return this;
		}
		return set(quat.setFromAxisRad(axisX, axisY, axisZ, radians));
	}

	/** Set the matrix to a rotation matrix between two vectors.
	 * @param v1 The base vector
	 * @param v2 The target vector
	 * @return This matrix for the purpose of chaining methods together */
	public Matrix4f setToRotation (final SGVec_3f v1, final SGVec_3f v2) {
		return set(quat.setFromCross(v1, v2));
	}

	/** Set the matrix to a rotation matrix between two vectors.
	 * @param x1 The base vectors x value
	 * @param y1 The base vectors y value
	 * @param z1 The base vectors z value
	 * @param x2 The target vector x value
	 * @param y2 The target vector y value
	 * @param z2 The target vector z value
	 * @return This matrix for the purpose of chaining methods together */
	public Matrix4f setToRotation (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2) {
		return set(quat.setFromCross(x1, y1, z1, x2, y2, z2));
	}

	/** Sets this matrix to a rotation matrix from the given euler angles.
	 * @param yaw the yaw in degrees
	 * @param pitch the pitch in degrees
	 * @param roll the roll in degrees
	 * @return This matrix */
	public Matrix4f setFromEulerAngles (float yaw, float pitch, float roll) {
		quat.setEulerAngles(yaw, pitch, roll);
		return set(quat);
	}

	/** Sets this matrix to a rotation matrix from the given euler angles.
	 * @param yaw the yaw in radians
	 * @param pitch the pitch in radians
	 * @param roll the roll in radians
	 * @return This matrix */
	public Matrix4f setFromEulerAnglesRad (float yaw, float pitch, float roll) {
		quat.setEulerAnglesRad(yaw, pitch, roll);
		return set(quat);
	}

	/** Sets this matrix to a scaling matrix
	 * 
	 * @param vector The scaling vector
	 * @return This matrix for chaining. */
	public Matrix4f setToScaling (SGVec_3f vector) {
		idt();
		val[M00] = vector.x;
		val[M11] = vector.y;
		val[M22] = vector.z;
		return this;
	}

	/** Sets this matrix to a scaling matrix
	 * 
	 * @param x The x-component of the scaling vector
	 * @param y The y-component of the scaling vector
	 * @param z The z-component of the scaling vector
	 * @return This matrix for chaining. */
	public Matrix4f setToScaling (float x, float y, float z) {
		idt();
		val[M00] = x;
		val[M11] = y;
		val[M22] = z;
		return this;
	}

	static final SGVec_3f l_vez = new SGVec_3f();
	static final SGVec_3f l_vex = new SGVec_3f();
	static final SGVec_3f l_vey = new SGVec_3f();

	/** Sets the matrix to a look at matrix with a direction and an up vector. Multiply with a translation matrix to get a camera
	 * model view matrix.
	 * 
	 * @param direction The direction vector
	 * @param up The up vector
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f setToLookAt (SGVec_3f direction, SGVec_3f up) {
		l_vez.set(direction).normalize();
		l_vex.set(direction).normalize();
		l_vex.crs(up).normalize();
		l_vey.set(l_vex).crs(l_vez).normalize();
		idt();
		val[M00] = l_vex.x;
		val[M01] = l_vex.y;
		val[M02] = l_vex.z;
		val[M10] = l_vey.x;
		val[M11] = l_vey.y;
		val[M12] = l_vey.z;
		val[M20] = -l_vez.x;
		val[M21] = -l_vez.y;
		val[M22] = -l_vez.z;

		return this;
	}

	static final SGVec_3f tmpVec = new SGVec_3f();
	protected static final Matrix4f tmpMat = new Matrix4f();

	/** Sets this matrix to a look at matrix with the given position, target and up vector.
	 * 
	 * @param position the position
	 * @param target the target
	 * @param up the up vector
	 * @return This matrix */
	public Matrix4f setToLookAt (SGVec_3f position, SGVec_3f target, SGVec_3f up) {
		tmpVec.set(target).sub(position);
		setToLookAt(tmpVec, up);
		this.mul(tmpMat.setToTranslation(-position.x, -position.y, -position.z));

		return this;
	}

	static final SGVec_3f right = new SGVec_3f();
	static final SGVec_3f tmpForward = new SGVec_3f();
	static final SGVec_3f tmpUp = new SGVec_3f();

	public Matrix4f setToWorld (SGVec_3f position, SGVec_3f forward, SGVec_3f up) {
		tmpForward.set(forward).normalize();
		right.set(tmpForward).crs(up).normalize();
		tmpUp.set(right).crs(tmpForward).normalize();

		this.set(right, tmpUp, tmpForward.mult(-1), position);
		return this;
	}

	public String toString () {
		return "[" + val[M00] + ", " + val[M01] + ", " + val[M02] + ", " + val[M03] + "]\n" + 
					"[" + val[M10] + ", " + val[M11] + ", "	+  val[M12] + ", " + val[M13] + "]\n" +
					"[ "+ val[M20] + ", " + val[M21] + ", " + val[M22] + ", " + val[M23] + "]\n" +
					"["+  val[M30] + ", " + val[M31] + ", " + val[M32] + ", " + val[M33] + "]\n";
	}

	/** Linearly interpolates between this matrix and the given matrix mixing by alpha
	 * @param matrix the matrix
	 * @param alpha the alpha value in the range [0,1]
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f lerp (Matrix4f matrix, float alpha) {
		for (int i = 0; i < 16; i++)
			this.val[i] = this.val[i] * (1 - alpha) + matrix.val[i] * alpha;
		return this;
	}

	/** Averages the given transform with this one and stores the result in this matrix. Translations and scales are lerped while
	 * rotations are slerped.
	 * @param other The other transform
	 * @param w Weight of this transform; weight of the other transform is (1 - w)
	 * @return This matrix for chaining */
	public Matrix4f avg (Matrix4f other, float w) {
		getScale(tmpVec);
		other.getScale(tmpForward);

		getRotation(quat);
		other.getRotation(quat2);

		getTranslation(tmpUp);
		other.getTranslation(right);

		setToScaling(tmpVec.mult(w).add(tmpForward.mult(1 - w)));
		rotate(quat.slerp(quat2, 1 - w));
		setTranslation(tmpUp.mult(w).add(right.mult(1 - w)));

		return this;
	}

	/** Averages the given transforms and stores the result in this matrix. Translations and scales are lerped while rotations are
	 * slerped. Does not destroy the data contained in t.
	 * @param t List of transforms
	 * @return This matrix for chaining */
	public Matrix4f avg (Matrix4f[] t) {
		final float w = 1.0f / t.length;

		tmpVec.set(t[0].getScale(tmpUp).mult(w));
		quat.set(t[0].getRotation(quat2).exp(w));
		tmpForward.set(t[0].getTranslation(tmpUp).mult(w));

		for (int i = 1; i < t.length; i++) {
			tmpVec.add(t[i].getScale(tmpUp).mult(w));
			quat.mul(t[i].getRotation(quat2).exp(w));
			tmpForward.add(t[i].getTranslation(tmpUp).mult(w));
		}
		quat.nor();

		setToScaling(tmpVec);
		rotate(quat);
		setTranslation(tmpForward);

		return this;
	}

	/** Averages the given transforms with the given weights and stores the result in this matrix. Translations and scales are
	 * lerped while rotations are slerped. Does not destroy the data contained in t or w; Sum of w_i must be equal to 1, or
	 * unexpected results will occur.
	 * @param t List of transforms
	 * @param w List of weights
	 * @return This matrix for chaining */
	public Matrix4f avg (Matrix4f[] t, float[] w) {
		tmpVec.set(t[0].getScale(tmpUp).mult(w[0]));
		quat.set(t[0].getRotation(quat2).exp(w[0]));
		tmpForward.set(t[0].getTranslation(tmpUp).mult(w[0]));

		for (int i = 1; i < t.length; i++) {
			tmpVec.add(t[i].getScale(tmpUp).mult(w[i]));
			quat.mul(t[i].getRotation(quat2).exp(w[i]));
			tmpForward.add(t[i].getTranslation(tmpUp).mult(w[i]));
		}
		quat.nor();

		setToScaling(tmpVec);
		rotate(quat);
		setTranslation(tmpForward);

		return this;
	}

	/** Sets this matrix to the given 3x3 matrix. The third column of this matrix is set to (0,0,1,0).
	 * @param mat the matrix */
	public Matrix4f set (Matrix3f mat) {
		val[0] = mat.val[0];
		val[1] = mat.val[1];
		val[2] = mat.val[2];
		val[3] = 0;
		val[4] = mat.val[3];
		val[5] = mat.val[4];
		val[6] = mat.val[5];
		val[7] = 0;
		val[8] = 0;
		val[9] = 0;
		val[10] = 1;
		val[11] = 0;
		val[12] = mat.val[6];
		val[13] = mat.val[7];
		val[14] = 0;
		val[15] = mat.val[8];
		return this;
	}



	public Matrix4f scl (SGVec_3f scale) {
		val[M00] *= scale.x;
		val[M11] *= scale.y;
		val[M22] *= scale.z;
		return this;
	}

	public Matrix4f scl (float x, float y, float z) {
		val[M00] *= x;
		val[M11] *= y;
		val[M22] *= z;
		return this;
	}

	public Matrix4f scl (float scale) {
		val[M00] *= scale;
		val[M11] *= scale;
		val[M22] *= scale;
		return this;
	}

	public SGVec_3f getTranslation (SGVec_3f position) {
		position.x = val[M03];
		position.y = val[M13];
		position.z = val[M23];
		return position;
	}

	/** Gets the rotation of this matrix.
	 * @param rotation The {@link Quaternionf} to receive the rotation
	 * @param normalizeAxes True to normalize the axes, necessary when the matrix might also include scaling.
	 * @return The provided {@link Quaternionf} for chaining. */
	public Quaternionf getRotation (Quaternionf rotation, boolean normalizeAxes) {
		return rotation.setFromMatrix(normalizeAxes, this);
	}

	/** Gets the rotation of this matrix.
	 * @param rotation The {@link Quaternionf} to receive the rotation
	 * @return The provided {@link Quaternionf} for chaining. */
	public Quaternionf getRotation (Quaternionf rotation) {
		return rotation.setFromMatrix(this);
	}

	/** @return the squared scale factor on the X axis */
	public float getScaleXSquared () {
		return val[Matrix4f.M00] * val[Matrix4f.M00] + val[Matrix4f.M01] * val[Matrix4f.M01] + val[Matrix4f.M02] * val[Matrix4f.M02];
	}

	/** @return the squared scale factor on the Y axis */
	public float getScaleYSquared () {
		return val[Matrix4f.M10] * val[Matrix4f.M10] + val[Matrix4f.M11] * val[Matrix4f.M11] + val[Matrix4f.M12] * val[Matrix4f.M12];
	}

	/** @return the squared scale factor on the Z axis */
	public float getScaleZSquared () {
		return val[Matrix4f.M20] * val[Matrix4f.M20] + val[Matrix4f.M21] * val[Matrix4f.M21] + val[Matrix4f.M22] * val[Matrix4f.M22];
	}

	/** @return the scale factor on the X axis (non-negative) */
	public float getScaleX () {
		return (MathUtils.isZero(val[Matrix4f.M01]) && MathUtils.isZero(val[Matrix4f.M02])) ? MathUtils.abs(val[Matrix4f.M00])
				: (float)MathUtils.sqrt(getScaleXSquared());
	}

	/** @return the scale factor on the Y axis (non-negative) */
	public float getScaleY () {
		return (MathUtils.isZero(val[Matrix4f.M10]) && MathUtils.isZero(val[Matrix4f.M12])) ? MathUtils.abs(val[Matrix4f.M11])
				: (float)MathUtils.sqrt(getScaleYSquared());
	}

	/** @return the scale factor on the X axis (non-negative) */
	public float getScaleZ () {
		return (MathUtils.isZero(val[Matrix4f.M20]) && MathUtils.isZero(val[Matrix4f.M21])) ? MathUtils.abs(val[Matrix4f.M22])
				: (float)MathUtils.sqrt(getScaleZSquared());
	}

	/** @param scale The vector which will receive the (non-negative) scale components on each axis.
	 * @return The provided vector for chaining. */
	public SGVec_3f getScale (SGVec_3f scale) {
		return scale.set(getScaleX(), getScaleY(), getScaleZ());
	}

	/** removes the translational part and transposes the matrix. */
	public Matrix4f toNormalMatrix () {
		val[M03] = 0;
		val[M13] = 0;
		val[M23] = 0;
		return inv().tra();
	}

	// @off
	/*JNI
	#include <memory.h>
	#include <stdio.h>
	#include <string.h>

	#define M00 0
	#define M01 4
	#define M02 8
	#define M03 12
	#define M10 1
	#define M11 5
	#define M12 9
	#define M13 13
	#define M20 2
	#define M21 6
	#define M22 10
	#define M23 14
	#define M30 3
	#define M31 7
	#define M32 11
	#define M33 15

	static inline void matrix4_mul(float* mata, float* matb) {
		float tmp[16];
		tmp[M00] = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
		tmp[M01] = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
		tmp[M02] = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
		tmp[M03] = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
		tmp[M10] = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
		tmp[M11] = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
		tmp[M12] = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
		tmp[M13] = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
		tmp[M20] = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
		tmp[M21] = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
		tmp[M22] = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
		tmp[M23] = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
		tmp[M30] = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
		tmp[M31] = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
		tmp[M32] = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
		tmp[M33] = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];
		memcpy(mata, tmp, sizeof(float) *  16);
	}

	static inline float matrix4_det(float* val) {
		return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11]
	 * val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10]
	 * val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
				+ val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32]
	 * val[M13] + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31]
	 * val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10]
	 * val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33]
				+ val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12]
	 * val[M33] - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
	}

	static inline bool matrix4_inv(float* val) {
		float tmp[16];
		float l_det = matrix4_det(val);
		if (l_det == 0) return false;
		tmp[M00] = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32] - val[M11]
	 * val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
		tmp[M01] = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32] + val[M01]
	 * val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
		tmp[M02] = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32] - val[M01]
	 * val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
		tmp[M03] = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22] + val[M01]
	 * val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
		tmp[M10] = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32] + val[M10]
	 * val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
		tmp[M11] = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32] - val[M00]
	 * val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
		tmp[M12] = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32] + val[M00]
	 * val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
		tmp[M13] = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22] - val[M00]
	 * val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
		tmp[M20] = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31] - val[M10]
	 * val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
		tmp[M21] = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31] + val[M00]
	 * val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
		tmp[M22] = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31] - val[M00]
	 * val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
		tmp[M23] = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21] + val[M00]
	 * val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
		tmp[M30] = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31] + val[M10]
	 * val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
		tmp[M31] = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31] - val[M00]
	 * val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
		tmp[M32] = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31] + val[M00]
	 * val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
		tmp[M33] = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00]
	 * val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];

		float inv_det = 1.0f / l_det;
		val[M00] = tmp[M00] * inv_det;
		val[M01] = tmp[M01] * inv_det;
		val[M02] = tmp[M02] * inv_det;
		val[M03] = tmp[M03] * inv_det;
		val[M10] = tmp[M10] * inv_det;
		val[M11] = tmp[M11] * inv_det;
		val[M12] = tmp[M12] * inv_det;
		val[M13] = tmp[M13] * inv_det;
		val[M20] = tmp[M20] * inv_det;
		val[M21] = tmp[M21] * inv_det;
		val[M22] = tmp[M22] * inv_det;
		val[M23] = tmp[M23] * inv_det;
		val[M30] = tmp[M30] * inv_det;
		val[M31] = tmp[M31] * inv_det;
		val[M32] = tmp[M32] * inv_det;
		val[M33] = tmp[M33] * inv_det;
		return true;
	}

	static inline void matrix4_mulVec(float* mat, float* vec) {
		float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03];
		float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13];
		float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23];
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}

	static inline void matrix4_proj(float* mat, float* vec) {
		float inv_w = 1.0f / (vec[0] * mat[M30] + vec[1] * mat[M31] + vec[2] * mat[M32] + mat[M33]);
		float x = (vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03]) * inv_w;
		float y = (vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13]) * inv_w; 
		float z = (vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23]) * inv_w;
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}

	static inline void matrix4_rot(float* mat, float* vec) {
		float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02];
		float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12];
		float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22];
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}
	 */

	/** Multiplies the matrix mata with matrix matb, storing the result in mata. The arrays are assumed to hold 4x4 column major
	 * matrices as you can get from {@link Matrix4f#val}. This is the same as {@link Matrix4f#mul(Matrix4f)}.
	 *
	 * @param mata the first matrix.
	 * @param matb the second matrix. */
	public static void mul (float[] mata, float[] matb) {
		float[] tmp = new float[16];
		tmp[M00] = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
		tmp[M01] = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
		tmp[M02] = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
		tmp[M03] = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
		tmp[M10] = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
		tmp[M11] = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
		tmp[M12] = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
		tmp[M13] = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
		tmp[M20] = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
		tmp[M21] = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
		tmp[M22] = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
		tmp[M23] = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
		tmp[M30] = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
		tmp[M31] = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
		tmp[M32] = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
		tmp[M33] = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];

		for(int i = 0; i<mata.length; i++) mata[i] = tmp[i];
	}
	
	/** Multiplies the matrix mata with matrix matb, storing the result in this matrix. The arrays are assumed to hold 4x4 column major
	 * matrices as you can get from {@link Matrix4f#val}. This is the same as {@link Matrix4f#mul(Matrix4f)}.
	 *
	 * @param mata the first matrix.
	 * @param matb the second matrix. */
	public void setToMulOf (Matrix4f matA, Matrix4f matB) {		
		float[] mata = matA.val; float[] matb = matB.val;
		float[] tmp = matA != matB ? this.val : new float[16];
		tmp[M00] = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
		tmp[M01] = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
		tmp[M02] = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
		tmp[M03] = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
		tmp[M10] = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
		tmp[M11] = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
		tmp[M12] = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
		tmp[M13] = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
		tmp[M20] = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
		tmp[M21] = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
		tmp[M22] = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
		tmp[M23] = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
		tmp[M30] = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
		tmp[M31] = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
		tmp[M32] = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
		tmp[M33] = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];

		if(matB == matA)
			for(int i = 0; i<mata.length; i++) this.val[i] = tmp[i];
	}
	/*-{ }-*/; /*
	}
		matrix4_mul(mata, matb);
	 */

	/** Multiplies the vector with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
	 * from {@link Matrix4f#val}. The vector array is assumed to hold a 3-component vector, with x being the first element, y being
	 * the second and z being the last component. The result is stored in the vector array. This is the same as
	 * {@link SGVec_3f#mul(Matrix4f)}.
	 * @param mat the matrix
	 * @param vec the vector. */
	public static  void mulVec (float[] mat, float[] vec) {
		float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03];
		float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13];
		float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23];
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}
	
	
	/** Multiplies the vector with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
	 * from {@link Matrix4f#val}. The vector array is assumed to hold a 3-component vector, with x being the first element, y being
	 * the second and z being the last component. The result is stored in the vector array. This is the same as
	 * {@link SGVec_3f#mul(Matrix4f)}.
	 * @param mat the matrix
	 * @param vec the vector. */
	public static  void mulVec (float[] mat, SGVec_3f vecIn, SGVec_3f vecOut) {
		float x = vecIn.x * mat[M00] + vecIn.y * mat[M01] + vecIn.z * mat[M02] + mat[M03];
		float y = vecIn.x * mat[M10] + vecIn.y * mat[M11] + vecIn.z * mat[M12] + mat[M13];
		float z = vecIn.x * mat[M20] + vecIn.y * mat[M21] + vecIn.z * mat[M22] + mat[M23];
		vecOut.x = x;
		vecOut.y = y;
		vecOut.z = z;
	}
	
	
	/**Transforms the vector by this matrix and stores the result in the vector
	 * {@link SGVec_3f#mul(Matrix4f)}.
	 * @param mat the matrix
	 * @param vec the vector. */
	public  void transform (SGVec_3f input, SGVec_3f output) {
		mulVec(this.val, input, output);
	}
	/*-{ }-*/; /*
	}
		matrix4_mulVec(mat, vec);
	 */

	/** Multiplies the vectors with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
	 * from {@link Matrix4f#val}. The vectors array is assumed to hold 3-component vectors. Offset specifies the offset into the
	 * array where the x-component of the first vector is located. The numVecs parameter specifies the number of vectors stored in
	 * the vectors array. The stride parameter specifies the number of floats between subsequent vectors and must be >= 3. This is
	 * the same as {@link Vector3#mul(Matrix4f)} applied to multiple vectors.
	 * 
	 * @param mat the matrix
	 * @param vecs the vectors
	 * @param offset the offset into the vectors array
	 * @param numVecs the number of vectors
	 * @param stride the stride between vectors in floats */
	//public static native void mulVec (float[] mat, float[] vecs, int offset, int numVecs, int stride) /*-{ }-*/; 
	/*
		float* vecPtr = vecs + offset;
		for(int i = 0; i < numVecs; i++) {
			matrix4_mulVec(mat, vecPtr);
			vecPtr += stride;
		}
	 */

	/** Multiplies the vector with the given matrix, performing a division by w. The matrix array is assumed to hold a 4x4 column
	 * major matrix as you can get from {@link Matrix4f#val}. The vector array is assumed to hold a 3-component vector, with x being
	 * the first element, y being the second and z being the last component. The result is stored in the vector array. This is the
	 * same as {@link SGVec_3f#prj(Matrix4f)}.
	 * @param mat the matrix
	 * @param vec the vector. */
	public static  void prj (float[] mat, float[] vec) { 
		float inv_w = 1.0f / (vec[0] * mat[M30] + vec[1] * mat[M31] + vec[2] * mat[M32] + mat[M33]);
		float x = (vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03]) * inv_w;
		float y = (vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13]) * inv_w; 
		float z = (vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23]) * inv_w;
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;
	}
	/*-{ }-*/; /*
	}
		matrix4_proj(mat, vec);
	 */

	/** Multiplies the vectors with the given matrix, , performing a division by w. The matrix array is assumed to hold a 4x4 column
	 * major matrix as you can get from {@link Matrix4f#val}. The vectors array is assumed to hold 3-component vectors. Offset
	 * specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter specifies
	 * the number of vectors stored in the vectors array. The stride parameter specifies the number of floats between subsequent
	 * vectors and must be >= 3. This is the same as {@link Vector3#prj(Matrix4f)} applied to multiple vectors.
	 * 
	 * @param mat the matrix
	 * @param vecs the vectors
	 * @param offset the offset into the vectors array
	 * @param numVecs the number of vectors
	 * @param stride the stride between vectors in floats */
	//public static  void prj (float[] mat, float[] vecs, int offset, int numVecs, int stride) /*-{ }-*/; 
	/*
		float* vecPtr = vecs + offset;
		for(int i = 0; i < numVecs; i++) {
			matrix4_proj(mat, vecPtr);
			vecPtr += stride;
		}
	 */

	/** Multiplies the vector with the top most 3x3 sub-matrix of the given matrix. The matrix array is assumed to hold a 4x4 column
	 * major matrix as you can get from {@link Matrix4f#val}. The vector array is assumed to hold a 3-component vector, with x being
	 * the first element, y being the second and z being the last component. The result is stored in the vector array. This is the
	 * same as {@link SGVec_3f#rot(Matrix4f)}.
	 * @param mat the matrix
	 * @param vec the vector. */
	public static  void rot (float[] mat, float[] vec) {
		float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02];
		float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12];
		float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22];
		vec[0] = x;
		vec[1] = y;
		vec[2] = z;

	}
	/*-{ }-*/; /*
	}
		matrix4_rot(mat, vec);
	 */

	/** Multiplies the vectors with the top most 3x3 sub-matrix of the given matrix. The matrix array is assumed to hold a 4x4
	 * column major matrix as you can get from {@link Matrix4f#val}. The vectors array is assumed to hold 3-component vectors.
	 * Offset specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter
	 * specifies the number of vectors stored in the vectors array. The stride parameter specifies the number of floats between
	 * subsequent vectors and must be >= 3. This is the same as {@link Vector3#rot(Matrix4f)} applied to multiple vectors.
	 * 
	 * @param mat the matrix
	 * @param vecs the vectors
	 * @param offset the offset into the vectors array
	 * @param numVecs the number of vectors
	 * @param stride the stride between vectors in floats */
	//public static  void rot (float[] mat, float[] vecs, int offset, int numVecs, int stride) /*-{ }-*/; 
	/*
		float* vecPtr = vecs + offset;
		for(int i = 0; i < numVecs; i++) {
			matrix4_rot(mat, vecPtr);
			vecPtr += stride;
		}
	 */

	/** Computes the inverse of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from
	 * {@link Matrix4f#val}.
	 * @param values the matrix values.
	 * @return false in case the inverse could not be calculated, true otherwise. */
	public static  boolean inv (float[] val) {
		float[] tmp = new float[16];
		float l_det = det(val);
		if (l_det == 0) return false;
		tmp[M00] = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32] - val[M11]
				* val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
		tmp[M01] = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32] + val[M01]
				* val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
		tmp[M02] = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32] - val[M01]
				* val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
		tmp[M03] = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22] + val[M01]
				* val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
		tmp[M10] = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32] + val[M10]
				* val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
		tmp[M11] = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32] - val[M00]
				* val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
		tmp[M12] = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32] + val[M00]
				* val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
		tmp[M13] = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22] - val[M00]
				* val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
		tmp[M20] = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31] - val[M10]
				* val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
		tmp[M21] = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31] + val[M00]
				* val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
		tmp[M22] = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31] - val[M00]
				* val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
		tmp[M23] = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21] + val[M00]
				* val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
		tmp[M30] = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31] + val[M10]
				* val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
		tmp[M31] = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31] - val[M00]
				* val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
		tmp[M32] = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31] + val[M00]
				* val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
		tmp[M33] = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00]
				* val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];

		float inv_det = 1.0f / l_det;
		val[M00] = tmp[M00] * inv_det;
		val[M01] = tmp[M01] * inv_det;
		val[M02] = tmp[M02] * inv_det;
		val[M03] = tmp[M03] * inv_det;
		val[M10] = tmp[M10] * inv_det;
		val[M11] = tmp[M11] * inv_det;
		val[M12] = tmp[M12] * inv_det;
		val[M13] = tmp[M13] * inv_det;
		val[M20] = tmp[M20] * inv_det;
		val[M21] = tmp[M21] * inv_det;
		val[M22] = tmp[M22] * inv_det;
		val[M23] = tmp[M23] * inv_det;
		val[M30] = tmp[M30] * inv_det;
		val[M31] = tmp[M31] * inv_det;
		val[M32] = tmp[M32] * inv_det;
		val[M33] = tmp[M33] * inv_det;
		return true;
	}

	/** Computes the inverse of m1, and stores the value in storeIn. The matrix array is assumed to hold a 4x4 column major matrix as you can get from
	 * {@link Matrix4f#val}.
	 * @param values the matrix values.
	 * @return false in case the inverse could not be calculated, true otherwise. */
	public static  boolean inv (float[] m1, float[] storeIn) {
		float[] tmp = storeIn; 
		float[] val = m1; 
		float l_det = det(val);
		if (l_det == 0) return false;
		tmp[M00] = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32] - val[M11]
				* val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
		tmp[M01] = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32] + val[M01]
				* val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
		tmp[M02] = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32] - val[M01]
				* val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
		tmp[M03] = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22] + val[M01]
				* val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
		tmp[M10] = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32] + val[M10]
				* val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
		tmp[M11] = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32] - val[M00]
				* val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
		tmp[M12] = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32] + val[M00]
				* val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
		tmp[M13] = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22] - val[M00]
				* val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
		tmp[M20] = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31] - val[M10]
				* val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
		tmp[M21] = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31] + val[M00]
				* val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
		tmp[M22] = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31] - val[M00]
				* val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
		tmp[M23] = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21] + val[M00]
				* val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
		tmp[M30] = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31] + val[M10]
				* val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
		tmp[M31] = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31] - val[M00]
				* val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
		tmp[M32] = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31] + val[M00]
				* val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
		tmp[M33] = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00]
				* val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];

		float inv_det = 1.0f / l_det;
		storeIn[M00] = tmp[M00] * inv_det;
		storeIn[M01] = tmp[M01] * inv_det;
		storeIn[M02] = tmp[M02] * inv_det;
		storeIn[M03] = tmp[M03] * inv_det;
		storeIn[M10] = tmp[M10] * inv_det;
		storeIn[M11] = tmp[M11] * inv_det;
		storeIn[M12] = tmp[M12] * inv_det;
		storeIn[M13] = tmp[M13] * inv_det;
		storeIn[M20] = tmp[M20] * inv_det;
		storeIn[M21] = tmp[M21] * inv_det;
		storeIn[M22] = tmp[M22] * inv_det;
		storeIn[M23] = tmp[M23] * inv_det;
		storeIn[M30] = tmp[M30] * inv_det;
		storeIn[M31] = tmp[M31] * inv_det;
		storeIn[M32] = tmp[M32] * inv_det;
		storeIn[M33] = tmp[M33] * inv_det;
		return true;
	}


	/*-{ }-*/; /*
	}
		return matrix4_inv(values);
	 */

	/** Computes the determinante of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get
	 * from {@link Matrix4f#val}.
	 * @param values the matrix values.
	 * @return the determinante. */
	public static  float det (float[] val) {
		return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11]
				* val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10]
						* val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
								+ val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32]
										* val[M13] + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31]
												* val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10]
														* val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33]
																+ val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12]
																		* val[M33] - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
	}
	
	/** @return The determinant of this matrix */
	public float  determinant() {
		return det();
	}
	/*-{ }-*/; /*
	}
		return matrix4_det(values);
	 */

	// @on
	/** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES'
	 * glTranslate/glRotate/glScale
	 * @param translation
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f translate (SGVec_3f translation) {
		return translate(translation.x, translation.y, translation.z);
	}

	/** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 * @param x Translation in the x-axis.
	 * @param y Translation in the y-axis.
	 * @param z Translation in the z-axis.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f translate (float x, float y, float z) {
		tmp[M00] = 1;
		tmp[M01] = 0;
		tmp[M02] = 0;
		tmp[M03] = x;
		tmp[M10] = 0;
		tmp[M11] = 1;
		tmp[M12] = 0;
		tmp[M13] = y;
		tmp[M20] = 0;
		tmp[M21] = 0;
		tmp[M22] = 1;
		tmp[M23] = z;
		tmp[M30] = 0;
		tmp[M31] = 0;
		tmp[M32] = 0;
		tmp[M33] = 1;

		mul(val, tmp);
		return this;
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 * 
	 * @param axis The vector axis to rotate around.
	 * @param degrees The angle in degrees.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f rotate (SGVec_3f axis, float degrees) {
		if (degrees == 0) return this;
		quat.set(axis, degrees);
		return rotate(quat);
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 * 
	 * @param axis The vector axis to rotate around.
	 * @param radians The angle in radians.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f rotateRad (SGVec_3f axis, float radians) {
		if (radians == 0) return this;
		quat.setFromAxisRad(axis, radians);
		return rotate(quat);
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale
	 * @param axisX The x-axis component of the vector to rotate around.
	 * @param axisY The y-axis component of the vector to rotate around.
	 * @param axisZ The z-axis component of the vector to rotate around.
	 * @param degrees The angle in degrees
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f rotate (float axisX, float axisY, float axisZ, float degrees) {
		if (degrees == 0) return this;
		quat.setFromAxis(axisX, axisY, axisZ, degrees);
		return rotate(quat);
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale
	 * @param axisX The x-axis component of the vector to rotate around.
	 * @param axisY The y-axis component of the vector to rotate around.
	 * @param axisZ The z-axis component of the vector to rotate around.
	 * @param radians The angle in radians
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f rotateRad (float axisX, float axisY, float axisZ, float radians) {
		if (radians == 0) return this;
		quat.setFromAxisRad(axisX, axisY, axisZ, radians);
		return rotate(quat);
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 * 
	 * @param rotation
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f rotate (Quaternionf rotation) {
		rotation.toMatrix(tmp);
		mul(val, tmp);
		return this;
	}

	/** Postmultiplies this matrix by the rotation between two vectors.
	 * @param v1 The base vector
	 * @param v2 The target vector
	 * @return This matrix for the purpose of chaining methods together */
	public Matrix4f rotate (final SGVec_3f v1, final SGVec_3f v2) {
		return rotate(quat.setFromCross(v1, v2));
	}

	/** Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 * @param scaleX The scale in the x-axis.
	 * @param scaleY The scale in the y-axis.
	 * @param scaleZ The scale in the z-axis.
	 * @return This matrix for the purpose of chaining methods together. */
	public Matrix4f scale (float scaleX, float scaleY, float scaleZ) {
		tmp[M00] = scaleX;
		tmp[M01] = 0;
		tmp[M02] = 0;
		tmp[M03] = 0;
		tmp[M10] = 0;
		tmp[M11] = scaleY;
		tmp[M12] = 0;
		tmp[M13] = 0;
		tmp[M20] = 0;
		tmp[M21] = 0;
		tmp[M22] = scaleZ;
		tmp[M23] = 0;
		tmp[M30] = 0;
		tmp[M31] = 0;
		tmp[M32] = 0;
		tmp[M33] = 1;

		mul(val, tmp);
		return this;
	}

	/** Copies the 4x3 upper-left sub-matrix into float array. The destination array is supposed to be a column major matrix.
	 * @param dst the destination matrix */
	public void extract4x3Matrix (float[] dst) {
		dst[0] = val[M00];
		dst[1] = val[M10];
		dst[2] = val[M20];
		dst[3] = val[M01];
		dst[4] = val[M11];
		dst[5] = val[M21];
		dst[6] = val[M02];
		dst[7] = val[M12];
		dst[8] = val[M22];
		dst[9] = val[M03];
		dst[10] = val[M13];
		dst[11] = val[M23];
	}

	/** @return True if this matrix has any rotation or scaling, false otherwise */
	public boolean hasRotationOrScaling () {
		return !(MathUtils.isEqual(val[M00], 1) && MathUtils.isEqual(val[M11], 1) && MathUtils.isEqual(val[M22], 1)
				&& MathUtils.isZero(val[M01]) && MathUtils.isZero(val[M02]) && MathUtils.isZero(val[M10]) && MathUtils.isZero(val[M12])
				&& MathUtils.isZero(val[M20]) && MathUtils.isZero(val[M21]));
	}
}
