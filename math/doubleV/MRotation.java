/**
 * This class provides similar functionality to apache commons rotations
 * but it is mutable. It is licensed under the Apache Commons License.
 */

package math.doubleV;

import numerical.Precision;
import numerical.Precision.*;

public class MRotation {
	public static final MRotation IDENTITY = new MRotation(1.0, 0.0, 0.0, 0.0, false);

	/** Scalar coordinate of the quaternion. */

	private double q0;

	/** First coordinate of the vectorial part of the quaternion. */
	private double q1;

	/** Second coordinate of the vectorial part of the quaternion. */
	private double q2;

	/** Third coordinate of the vectorial part of the quaternion. */
	private double q3;

	/** Build a rotation from the quaternion coordinates.
	 * <p>A rotation can be built from a <em>normalized</em> quaternion,
	 * i.e. a quaternion for which q<sub>0</sub><sup>2</sup> +
	 * q<sub>1</sub><sup>2</sup> + q<sub>2</sub><sup>2</sup> +
	 * q<sub>3</sub><sup>2</sup> = 1. If the quaternion is not normalized,
	 * the constructor can normalize it in a preprocessing step.</p>
	 * <p>Note that some conventions put the scalar part of the quaternion
	 * as the 4<sup>th</sup> component and the vector part as the first three
	 * components. This is <em>not</em> our convention. We put the scalar part
	 * as the first component.</p>
	 * @param q0 scalar part of the quaternion
	 * @param q1 first coordinate of the vectorial part of the quaternion
	 * @param q2 second coordinate of the vectorial part of the quaternion
	 * @param q3 third coordinate of the vectorial part of the quaternion
	 * @param needsNormalization if true, the coordinates are considered
	 * not to be normalized, a normalization preprocessing step is performed
	 * before using them
	 */
	public MRotation(double q0, double q1, double q2, double q3,
			boolean needsNormalization) {

		this.q0 = q0; 
		this.q1 = q1; 
		this.q2 = q2;
		this.q3 = q3;

		if(needsNormalization) setToNormalized();



	}

	/**
	 * creates an identity rotation
	 */
	public MRotation() {
		this(1.0, 0.0, 0.0, 0.0, false);
	}

	/**assumes no noralization required**/
	public MRotation(double q0, double q1, double q2, double q3) {
		this(q0,q1,q2,q3,false);
	}



	/** Build a rotation from an axis and an angle.
	 * <p>We use the convention that angles are oriented according to
	 * the effect of the rotation on vectors around the axis. That means
	 * that if (i, j, k) is a direct frame and if we first provide +k as
	 * the axis and &pi;/2 as the angle to this constructor, and then
	 * {@link #applyTo(T ) apply} the instance to +i, we will get
	 * +j.</p>
	 * <p>Another way to represent our convention is to say that a rotation
	 * of angle &theta; about the unit vector (x, y, z) is the same as the
	 * rotation build from quaternion components { cos(-&theta;/2),
	 * x * sin(-&theta;/2), y * sin(-&theta;/2), z * sin(-&theta;/2) }.
	 * Note the minus sign on the angle!</p>
	 * <p>On the one hand this convention is consistent with a vectorial
	 * perspective (moving vectors in fixed frames), on the other hand it
	 * is different from conventions with a frame perspective (fixed vectors
	 * viewed from different frames) like the ones used for example in spacecraft
	 * attitude community or in the graphics community.</p>
	 * @param axis axis around which to rotate
	 * @param angle rotation angle.
	 * @exception MathIllegalArgumentException if the axis norm is zero
	 */
	public <V extends Vec3d<?>> MRotation(V axis, double angle)  {

		double norm = axis.mag();
		if (norm == 0) {
			try {
				throw new Exception("Zero Norm for Rotation defining vector");
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		double halfAngle = -0.5 * angle;					
		double coeff = Math.sin(halfAngle) / norm;

		q0 = Math.cos (halfAngle);
		q1 = coeff * axis.x;
		q2 = coeff * axis.y;
		q3 = coeff * axis.z;
	}

	/**
	 * modify this rotation to have the specified axis, 
	 * without changing the angle.  
	 *
	 * @param angle
	 * @throws Exception 
	 */
	public <T extends SGVec_3d> void setAxis(T  newAxis) throws Exception {

		double angle = this.getAngle();
		double norm = newAxis.mag();
		if (norm == 0) {
			try {
				throw new Exception("Zero Norm for Rotation Axis");
			} catch (MathIllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
			}
		}

		double halfAngle = -0.5 * angle;
		double coeff = Math.sin(halfAngle) / norm;

		q0 = Math.cos (halfAngle);
		q1 = coeff * newAxis.x;
		q2 = coeff * newAxis.y;
		q3 = coeff * newAxis.z;
	}

	/**
	 * modify this rotation to have the specified angle, 
	 * without changing the axis.  
	 *
	 * @param angle
	 */
	public void setAngle(double newAngle) {		
		double squaredSine = q1 * q1 + q2 * q2 + q3 * q3;
		if(squaredSine != 0) {
			double halfAngle = -0.5 * newAngle;
			double cosHalfAngle = Math.cos(halfAngle);

			double inverseCoeff = Math.sqrt(((1d-(cosHalfAngle*cosHalfAngle))/squaredSine));
			inverseCoeff = newAngle < 0 ? -inverseCoeff : inverseCoeff;

			q0 = q0<0 ? -cosHalfAngle : cosHalfAngle;
			q1 = inverseCoeff * q1;
			q2 = inverseCoeff * q2;
			q3 = inverseCoeff * q3;
		}
	}

	/**
	 * Modify this rotation to have the specified cos(angle/2) representation, 
	 * without changing the axis.  
	 *
	 * @param angle
	 */
	public void setQuadranceAngle(double cosHalfAngle) {
		double squaredSine = q1 * q1 + q2 * q2 + q3 * q3;		
		if(squaredSine != 0) {			
			double inverseCoeff = Math.sqrt(((1-(cosHalfAngle*cosHalfAngle))/squaredSine));
			//inverseCoeff = cosHalfAngle < 0 ? -inverseCoeff : inverseCoeff;
			q0 = q0<0 ? -cosHalfAngle : cosHalfAngle;
			q1 = inverseCoeff * q1;
			q2 = inverseCoeff * q2;
			q3 = inverseCoeff * q3;		
		}
	}


	public void clampToAngle(double angle) {
		double cosHalfAngle = Math.cos(0.5*angle);
		clampToQuadranceAngle(cosHalfAngle);
	}

	public void clampToQuadranceAngle(double cosHalfAngle) {
		double newCoeff = 1d-(cosHalfAngle*cosHalfAngle);
		double currentCoeff =q1 * q1 + q2 * q2 + q3 * q3;
		if(newCoeff>currentCoeff) 
			return;
		else {
			q0 = q0<0 ? -cosHalfAngle : cosHalfAngle;
			double compositeCoeff = Math.sqrt(newCoeff / currentCoeff); 
			q1*= compositeCoeff;
			q2*= compositeCoeff;
			q3*= compositeCoeff;
		}
	}
	
	
	
	/** Build a rotation from a 3X3 given as a 1d array with 9 elements, 
	 * or 4x4 matrix given as a 1d array with 16 elements. 
	 * This constructor will detect the appropriate case based on the length 
	 * of the input array. 
	 * Input array should be in column major order, so, for a 3x3 matrix, the
	 * indices correspond as follows: <br/> 
	 * 0, 3, 6 <br/>  
	 * 1, 4, 7 <br/> 
	 * 2, 5, 8 <br/>
	 *
	 *And for a 4x4 matrix the indices are: 
	 * <br/> 
	 * 0,  4,  8,  12 <br/>  
	 * 1,  5,  9,  13 <br/> 
	 * 2,  6, 10, 14 <br/>
 	 * 3,  7, 11, 15 <br/>
	 *
	 *
	 * <p>Rotation matrices are orthogonal matrices, i.e. unit matrices
	 * (which are matrices for which m.m<sup>T</sup> = I) with real
	 * coefficients. The module of the determinant of unit matrices is
	 * 1, among the orthogonal 3X3 matrices, only the ones having a
	 * positive determinant (+1) are rotation matrices.</p>
	 * <p>When a rotation is defined by a matrix with truncated values
	 * (typically when it is extracted from a technical sheet where only
	 * four to five significant digits are available), the matrix is not
	 * orthogonal anymore. This constructor handles this case
	 * transparently by using a copy of the given matrix and applying a
	 * correction to the copy in order to perfect its orthogonality. If
	 * the Frobenius norm of the correction needed is above the given
	 * threshold, then the matrix is considered to be too far from a
	 * true rotation matrix and an exception is thrown.<p>
	 * @param m rotation matrix
	 * @param is4x4 set to true if passing in a 4x4 matrix. 
	 * @param threshold convergence threshold for the iterative
	 * orthogonality correction (convergence is reached when the
	 * difference between two steps of the Frobenius norm of the
	 * correction is below this threshold)
	 * @exception NotARotationMatrixException if the matrix is not a 3X3
	 * matrix, or if it cannot be transformed into an orthogonal matrix
	 * with the given threshold, or if the determinant of the resulting
	 * orthogonal matrix is negative
	 */
	public MRotation(double[] m, double threshold)
			throws NotARotationMatrixException {

		// dimension check
		if ((m.length != 9 || m.length != 16)) {
			throw new NotARotationMatrixException(
					LocalizedFormats.ROTATION_MATRIX_DIMENSIONS,
					m.length);
		}

		double[][] im = new double[3][3];
		if(m.length == 9) {
			im[0][0] = m[0]; im[0][1] = m[0];  im[0][2] = m[0];
			im[0][0] = m[0]; im[0][1] = m[0];  im[0][2] = m[0];
			im[0][0] = m[0]; im[0][1] = m[0];  im[0][2] = m[0];
		}
		
		// compute a "close" orthogonal matrix
		double[][] ort = orthogonalizeMatrix(im, threshold);

		// check the sign of the determinant
		double det = ort[0][0] * (ort[1][1] * ort[2][2] - ort[2][1] * ort[1][2]) -
				ort[1][0] * (ort[0][1] * ort[2][2] - ort[2][1] * ort[0][2]) +
				ort[2][0] * (ort[0][1] * ort[1][2] - ort[1][1] * ort[0][2]);
		if (det < 0.0) {
			try {
				throw new Exception("Closest Orthogonal Has Negative Determinant");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(System.out);
			}
		}

		double[] quat = mat2quat(ort);
		q0 = quat[0];
		q1 = quat[1];
		q2 = quat[2];
		q3 = quat[3];

	}

	/** Build a rotation from a 3X3 matrix.
	 * <p>Rotation matrices are orthogonal matrices, i.e. unit matrices
	 * (which are matrices for which m.m<sup>T</sup> = I) with real
	 * coefficients. The module of the determinant of unit matrices is
	 * 1, among the orthogonal 3X3 matrices, only the ones having a
	 * positive determinant (+1) are rotation matrices.</p>
	 * <p>When a rotation is defined by a matrix with truncated values
	 * (typically when it is extracted from a technical sheet where only
	 * four to five significant digits are available), the matrix is not
	 * orthogonal anymore. This constructor handles this case
	 * transparently by using a copy of the given matrix and applying a
	 * correction to the copy in order to perfect its orthogonality. If
	 * the Frobenius norm of the correction needed is above the given
	 * threshold, then the matrix is considered to be too far from a
	 * true rotation matrix and an exception is thrown.<p>
	 * @param m rotation matrix
	 * @param threshold convergence threshold for the iterative
	 * orthogonality correction (convergence is reached when the
	 * difference between two steps of the Frobenius norm of the
	 * correction is below this threshold)
	 * @exception NotARotationMatrixException if the matrix is not a 3X3
	 * matrix, or if it cannot be transformed into an orthogonal matrix
	 * with the given threshold, or if the determinant of the resulting
	 * orthogonal matrix is negative
	 */
	public MRotation(double[][] m, double threshold)
			throws NotARotationMatrixException {

		// dimension check
		if ((m.length != 3) || (m[0].length != 3) ||
				(m[1].length != 3) || (m[2].length != 3)) {
			throw new NotARotationMatrixException(
					LocalizedFormats.ROTATION_MATRIX_DIMENSIONS,
					m.length, m[0].length);
		}

		// compute a "close" orthogonal matrix
		double[][] ort = orthogonalizeMatrix(m, threshold);

		// check the sign of the determinant
		double det = ort[0][0] * (ort[1][1] * ort[2][2] - ort[2][1] * ort[1][2]) -
				ort[1][0] * (ort[0][1] * ort[2][2] - ort[2][1] * ort[0][2]) +
				ort[2][0] * (ort[0][1] * ort[1][2] - ort[1][1] * ort[0][2]);
		if (det < 0.0) {
			try {
				throw new Exception("Closest Orthogonal Has Negative Determinant");
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		double[] quat = mat2quat(ort);
		q0 = quat[0];
		q1 = quat[1];
		q2 = quat[2];
		q3 = quat[3];

	}

	/** Build the rotation that transforms a pair of vector into another pair.
	 * <p>Except for possible scale factors, if the instance were applied to
	 * the pair (u<sub>1</sub>, u<sub>2</sub>) it will produce the pair
	 * (v<sub>1</sub>, v<sub>2</sub>).</p>
	 * <p>If the angular separation between u<sub>1</sub> and u<sub>2</sub> is
	 * not the same as the angular separation between v<sub>1</sub> and
	 * v<sub>2</sub>, then a corrected v'<sub>2</sub> will be used rather than
	 * v<sub>2</sub>, the corrected vector will be in the (v<sub>1</sub>,
	 * v<sub>2</sub>) plane.</p>
	 * @param u1 first vector of the origin pair
	 * @param u2 second vector of the origin pair
	 * @param v1 desired image of u1 by the rotation
	 * @param v2 desired image of u2 by the rotation
	 * @exception MathArithmeticException if the norm of one of the vectors is zero,
	 * or if one of the pair is degenerated (i.e. the vectors of the pair are colinear)
	 */
	public <V extends Vec3d<?>> MRotation(V u1, V u2, V v1, V v2) {

		// norms computation
		double u1u1 = u1.dot(u1);
		double u2u2 = u2.dot(u2);
		double v1v1 = v1.dot(v1);
		double v2v2 = v2.dot(v2);
		if ((u1u1 == 0) || (u2u2 == 0) || (v1v1 == 0) || (v2v2 == 0)) {
			throw new IllegalArgumentException("zero norm for rotation defining vector");
		}

		double u1x = u1.x;
		double u1y = u1.y;
		double u1z = u1.z;

		double u2x = u2.x;
		double u2y = u2.y;
		double u2z = u2.z;

		// normalize v1 in order to have (v1'|v1') = (u1|u1)
		double coeff = (double)Math.sqrt (u1u1 / v1v1);
		double v1x   = coeff * v1.x;
		double v1y   = coeff * v1.y;
		double v1z   = coeff * v1.z;
		SGVec_3d va1 = new SGVec_3d(v1x, v1y, v1z);

		// adjust v2 in order to have (u1|u2) = (v1|v2) and (v2'|v2') = (u2|u2)
		double u1u2   = u1.dot(u2);
		double va1v2   = va1.dot(v2);
		double coeffU = u1u2 / u1u1;
		double coeffV = va1v2 / u1u1;
		double beta   = (double)Math.sqrt((u2u2 - u1u2 * coeffU) / (v2v2 - va1v2 * coeffV));
		double alpha  = coeffU - beta * coeffV;
		double v2x    = alpha * v1x + beta * v2.x;
		double v2y    = alpha * v1y + beta * v2.y;
		double v2z    = alpha * v1z + beta * v2.z;
		V va2 = (V) v2.copy(); va2.set(v2x, v2y, v2z);

		// preliminary computation (we use explicit formulation instead
		// of relying on the Vector3D class in order to avoid building lots
		// of temporary objects)
		V uRef = u1;
		V vRef = (V) va1;
		double dx1 = v1x - u1.x;
		double dy1 = v1y - u1.y;
		double dz1 = v1z - u1.z;
		double dx2 = v2x - u2.x;
		double dy2 = v2y - u2.y;
		double dz2 = v2z - u2.z;
		SGVec_3d k = new SGVec_3d(dy1 * dz2 - dz1 * dy2,
				dz1 * dx2 - dx1 * dz2,
				dx1 * dy2 - dy1 * dx2);
		double c = k.x * (u1y * u2z - u1z * u2y) +
				k.y * (u1z * u2x - u1x * u2z) +
				k.z * (u1x * u2y - u1y * u2x);

		if (Math.abs(c) <= MathUtils.DOUBLE_ROUNDING_ERROR) {
			// the (q1, q2, q3) vector is in the (u1, u2) plane
			// we try other vectors
			V u3 = (V) u1.crossCopy(u2);
			SGVec_3d v3 = va1.crossCopy(va2);
			double u3x  = u3.x;
			double u3y  = u3.y;
			double u3z  = u3.z;
			double v3x  = v3.x;
			double v3y  = v3.y;
			double v3z  = v3.z;

			double dx3 = v3x - u3x;
			double dy3 = v3y - u3y;
			double dz3 = v3z - u3z;
			k = new SGVec_3d(dy1 * dz3 - dz1 * dy3,
					dz1 * dx3 - dx1 * dz3,
					dx1 * dy3 - dy1 * dx3);
			c = k.x * (u1y * u3z - u1z * u3y) +
					k.y * (u1z * u3x - u1x * u3z) +
					k.z * (u1x * u3y - u1y * u3x);

			if (Math.abs(c) <= MathUtils.DOUBLE_ROUNDING_ERROR) {
				// the (q1, q2, q3) vector is aligned with u1:
				// we try (u2, u3) and (v2, v3)
				k = new SGVec_3d(dy2 * dz3 - dz2 * dy3,
						dz2 * dx3 - dx2 * dz3,
						dx2 * dy3 - dy2 * dx3);
				c = k.x * (u2y * u3z - u2z * u3y) +
						k.y * (u2z * u3x - u2x * u3z) +
						k.z * (u2x * u3y - u2y * u3x);

				if (Math.abs(c) <= MathUtils.DOUBLE_ROUNDING_ERROR) {
					// the (q1, q2, q3) vector is aligned with everything
					// this is really the identity rotation
					q0 = 1.0f;
					q1 = 0.0f;
					q2 = 0.0f;
					q3 = 0.0f;
					return;
				}

				// we will have to use u2 and v2 to compute the scalar part
				uRef = u2;
				vRef = va2;

			}

		}

		// compute the vectorial part
		c = (double) Math.sqrt(c);
		double inv = (double)1.0 / (c + c);
		q1 = inv * k.x;
		q2 = inv * k.y;
		q3 = inv * k.z;

		// compute the scalar part
		k = new SGVec_3d(uRef.y * q3 - uRef.z * q2,
				uRef.z * q1 - uRef.x * q3,
				uRef.x * q2 - uRef.y * q1);
		c = k.dot(k);
		q0 = vRef.dot(k) / (c + c);

		/*// build orthonormalized base from u1, u2
		// this fails when vectors are null or colinear, which is forbidden to define a rotation
		final SGVec_3d u3 = u1.crossCopy(u2).normalize();
		u2 = u3.crossCopy(u1).normalize();
		u1 = u1.normalize();

		// build an orthonormalized base from v1, v2
		// this fails when vectors are null or colinear, which is forbidden to define a rotation
		final SGVec_3d v3 = v1.crossCopy(v2).normalize();
		v2 = v3.crossCopy(v1).normalize();
		v1 = v1.normalize();

		// buid a matrix transforming the first base into the second one
		final double[][] m = new double[][] {
			{
				MathArrays.linearCombination(u1.x, v1.x, u2.x, v2.x, u3.x, v3.x),
				MathArrays.linearCombination(u1.y, v1.x, u2.y, v2.x, u3.y, v3.x),
				MathArrays.linearCombination(u1.z, v1.x, u2.z, v2.x, u3.z, v3.x)
			},
			{
				MathArrays.linearCombination(u1.x, v1.y, u2.x, v2.y, u3.x, v3.y),
				MathArrays.linearCombination(u1.y, v1.y, u2.y, v2.y, u3.y, v3.y),
				MathArrays.linearCombination(u1.z, v1.y, u2.z, v2.y, u3.z, v3.y)
			},
			{
				MathArrays.linearCombination(u1.x, v1.z, u2.x, v2.z, u3.x, v3.z),
				MathArrays.linearCombination(u1.y, v1.z, u2.y, v2.z, u3.y, v3.z),
				MathArrays.linearCombination(u1.z, v1.z, u2.z, v2.z, u3.z, v3.z)
			}
		};

		double[] quat = mat2quat(m);
		q0 = quat[0];
		q1 = quat[1];
		q2 = quat[2];
		q3 = quat[3];*/

	}

	/** Build one of the rotations that transform one vector into another one.
	 * <p>Except for a possible scale factor, if the instance were
	 * applied to the vector u it will produce the vector v. There is an
	 * infinite number of such rotations, this constructor choose the
	 * one with the smallest associated angle (i.e. the one whose axis
	 * is orthogonal to the (u, v) plane). If u and v are colinear, an
	 * arbitrary rotation axis is chosen.</p>
	 * @param u origin vector
	 * @param v desired image of u by the rotation
	 * @exception MathArithmeticException if the norm of one of the vectors is zero
	 */
	public <V extends Vec3d<?>> MRotation(V u, V v) {

		double normProduct = u.mag() * v.mag();
		if (normProduct == 0) {
			//throw new MathArithmeticException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_DEFINING_VECTOR);
			this.q0 = 1d;
			this.q1= 0d;
			this.q2 =0d;
			this.q3=0d;
			return;
		}

		double dot = u.dot(v);

		if (dot < ((2.0e-15 - 1.0) * normProduct)) {
			// special case u = -v: we select a PI angle rotation around
			// an arbitrary vector orthogonal to u
			V w = (V) u.getOrthogonal();
			q0 = 0.0;
			q1 = -w.x;
			q2 = -w.y;
			q3 = -w.z;
		} else {
			// general case: (u, v) defines a plane, we select
			// the shortest possible rotation: axis orthogonal to this plane
			q0 = Math.sqrt(0.5 * (1.0 + dot / normProduct));
			double coeff = 1.0 / (2.0 * q0 * normProduct);
			V q = (V) v.crossCopy(u);
			q1 = coeff * q.x;
			q2 = coeff * q.y;
			q3 = coeff * q.z;
		}

	}

	/** Build a rotation from three Cardan or Euler elementary rotations.
	 * <p>Cardan rotations are three successive rotations around the
	 * canonical axes X, Y and Z, each axis being used once. There are
	 * 6 such sets of rotations (XYZ, XZY, YXZ, YZX, ZXY and ZYX). Euler
	 * rotations are three successive rotations around the canonical
	 * axes X, Y and Z, the first and last rotations being around the
	 * same axis. There are 6 such sets of rotations (XYX, XZX, YXY,
	 * YZY, ZXZ and ZYZ), the most popular one being ZXZ.</p>
	 * <p>Beware that many people routinely use the term Euler angles even
	 * for what really are Cardan angles (this confusion is especially
	 * widespread in the aerospace business where Roll, Pitch and Yaw angles
	 * are often wrongly tagged as Euler angles).</p>
	 * @param order order of rotations to use
	 * @param alpha1 angle of the first elementary rotation
	 * @param alpha2 angle of the second elementary rotation
	 * @param alpha3 angle of the third elementary rotation
	 */
	public  MRotation(RotationOrder order,
			double alpha1, double alpha2, double alpha3) {
		MRotation r1 = new MRotation(order.getA1(), alpha1);
		MRotation r2 = new MRotation(order.getA2(), alpha2);
		MRotation r3 = new MRotation(order.getA3(), alpha3);
		MRotation composed = r1.applyTo(r2.applyTo(r3));
		q0 = composed.q0;
		q1 = composed.q1;
		q2 = composed.q2;
		q3 = composed.q3;
	}



	/**
	 * @return a copy of this MRotation
	 */
	public MRotation copy() {
		return new MRotation(getQ0(), getQ1(), getQ2(), getQ3());
	}

	/** Convert an orthogonal rotation matrix to a quaternion.
	 * @param ort orthogonal rotation matrix
	 * @return quaternion corresponding to the matrix
	 */
	private static double[] mat2quat(final double[][] ort) {

		final double[] quat = new double[4];

		// There are different ways to compute the quaternions elements
		// from the matrix. They all involve computing one element from
		// the diagonal of the matrix, and computing the three other ones
		// using a formula involving a division by the first element,
		// which unfortunately can be zero. Since the norm of the
		// quaternion is 1, we know at least one element has an absolute
		// value greater or equal to 0.5, so it is always possible to
		// select the right formula and avoid division by zero and even
		// numerical inaccuracy. Checking the elements in turn and using
		// the first one greater than 0.45 is safe (this leads to a simple
		// test since qi = 0.45 implies 4 qi^2 - 1 = -0.19)
		double s = ort[0][0] + ort[1][1] + ort[2][2];
		if (s > -0.19) {
			// compute q0 and deduce q1, q2 and q3
			quat[0] = 0.5 * Math.sqrt(s + 1.0);
			double inv = 0.25 / quat[0];
			quat[1] = inv * (ort[1][2] - ort[2][1]);
			quat[2] = inv * (ort[2][0] - ort[0][2]);
			quat[3] = inv * (ort[0][1] - ort[1][0]);
		} else {
			s = ort[0][0] - ort[1][1] - ort[2][2];
			if (s > -0.19) {
				// compute q1 and deduce q0, q2 and q3
				quat[1] = 0.5 * Math.sqrt(s + 1.0);
				double inv = 0.25 / quat[1];
				quat[0] = inv * (ort[1][2] - ort[2][1]);
				quat[2] = inv * (ort[0][1] + ort[1][0]);
				quat[3] = inv * (ort[0][2] + ort[2][0]);
			} else {
				s = ort[1][1] - ort[0][0] - ort[2][2];
				if (s > -0.19) {
					// compute q2 and deduce q0, q1 and q3
					quat[2] = 0.5 * Math.sqrt(s + 1.0);
					double inv = 0.25 / quat[2];
					quat[0] = inv * (ort[2][0] - ort[0][2]);
					quat[1] = inv * (ort[0][1] + ort[1][0]);
					quat[3] = inv * (ort[2][1] + ort[1][2]);
				} else {
					// compute q3 and deduce q0, q1 and q2
					s = ort[2][2] - ort[0][0] - ort[1][1];
					quat[3] = 0.5 * Math.sqrt(s + 1.0);
					double inv = 0.25 / quat[3];
					quat[0] = inv * (ort[0][1] - ort[1][0]);
					quat[1] = inv * (ort[0][2] + ort[2][0]);
					quat[2] = inv * (ort[2][1] + ort[1][2]);
				}
			}
		}

		return quat;

	}

	/** Revert a rotation.
	 * Build a rotation which reverse the effect of another
	 * rotation. This means that if r(u) = v, then r.revert(v) = u. The
	 * instance is not changed.
	 * @return a new rotation whose effect is the reverse of the effect
	 * of the instance
	 */
	public MRotation revert() {
		return new MRotation(-q0, q1, q2, q3, false);
	}


	/** 
	 * sets the values of the given rotation equal to the inverse of this rotation
	 * @param storeIN
	 */
	public void revert(MRotation storeIn) {
		storeIn.set(-q0, q1, q2, q3, true);
	}

	/** Get the scalar coordinate of the quaternion.
	 * @return scalar coordinate of the quaternion
	 */
	public double getQ0() {
		return q0;
	}

	/** Get the first coordinate of the vectorial part of the quaternion.
	 * @return first coordinate of the vectorial part of the quaternion
	 */
	public double getQ1() {
		return q1;
	}

	/** Get the second coordinate of the vectorial part of the quaternion.
	 * @return second coordinate of the vectorial part of the quaternion
	 */
	public double getQ2() {
		return q2;
	}

	/** Get the third coordinate of the vectorial part of the quaternion.
	 * @return third coordinate of the vectorial part of the quaternion
	 */
	public double getQ3() {
		return q3;
	}

	/** Get the normalized axis of the rotation.
	 * @return normalized axis of the rotation
	 * @see #Rotation(T , double)
	 */
	public SGVec_3d getAxis() {
		double squaredSine = q1 * q1 + q2 * q2 + q3 * q3;
		if (squaredSine == 0) {
			return new SGVec_3d(1, 0, 0);
		} else if (q0 < 0) {
			double inverse = 1 / Math.sqrt(squaredSine);
			return new SGVec_3d(q1 * inverse, q2 * inverse, q3 * inverse);
		}
		double inverse = -1 / Math.sqrt(squaredSine);
		return new SGVec_3d(q1 * inverse, q2 * inverse, q3 * inverse);
	}

	/** Get the normalized axis of the rotation.
	 * @return normalized axis of the rotation
	 * @see #Rotation(T , double)
	 */
	public <T extends Vec3d<?>> void setToAxis(T v) {
		double squaredSine = q1 * q1 + q2 * q2 + q3 * q3;
		if (squaredSine == 0) {
			v.set(1, 0, 0);
			return;
		} else if (q0 < 0) {
			double inverse = 1 / Math.sqrt(squaredSine);
			v.set(q1 * inverse, q2 * inverse, q3 * inverse);
			return;
		}
		double inverse = -1 / Math.sqrt(squaredSine);
		v.set(q1 * inverse, q2 * inverse, q3 * inverse);
	}


	public MRotation getInverse() {
		final double squareNorm = q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3;
		if (squareNorm < Precision.SAFE_MIN_DOUBLE) {
			try {
				throw new Exception("Zero Norm");
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		return new MRotation(q0 / squareNorm,
				-q1 / squareNorm,
				-q2 / squareNorm,
				-q3 / squareNorm);
	}



	/** Get the angle of the rotation.
	 * @return angle of the rotation (between 0 and &pi;)
	 * @see #Rotation(T , double)
	 */
	public double getAngle() {
		if ((q0 < -0.1) || (q0 > 0.1)) {			
			return 2 * Math.asin(Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3));
		} else if (q0 < 0) {
			return 2 * Math.acos(-q0);
		}		
		return 2 * Math.acos(q0);
	}

	/** Get the Cardan or Euler angles corresponding to the instance.
	 * <p>The equations show that each rotation can be defined by two
	 * different values of the Cardan or Euler angles set. For example
	 * if Cardan angles are used, the rotation defined by the angles
	 * a<sub>1</sub>, a<sub>2</sub> and a<sub>3</sub> is the same as
	 * the rotation defined by the angles &pi; + a<sub>1</sub>, &pi;
	 * - a<sub>2</sub> and &pi; + a<sub>3</sub>. This method implements
	 * the following arbitrary choices:</p>
	 * <ul>
	 *   <li>for Cardan angles, the chosen set is the one for which the
	 *   second angle is between -&pi;/2 and &pi;/2 (i.e its cosine is
	 *   positive),</li>
	 *   <li>for Euler angles, the chosen set is the one for which the
	 *   second angle is between 0 and &pi; (i.e its sine is positive).</li>
	 * </ul>
	 * <p>Cardan and Euler angle have a very disappointing drawback: all
	 * of them have singularities. This means that if the instance is
	 * too close to the singularities corresponding to the given
	 * rotation order, it will be impossible to retrieve the angles. For
	 * Cardan angles, this is often called gimbal lock. There is
	 * <em>nothing</em> to do to prevent this, it is an intrinsic problem
	 * with Cardan and Euler representation (but not a problem with the
	 * rotation itself, which is perfectly well defined). For Cardan
	 * angles, singularities occur when the second angle is close to
	 * -&pi;/2 or +&pi;/2, for Euler angle singularities occur when the
	 * second angle is close to 0 or &pi;, this implies that the identity
	 * rotation is always singular for Euler angles!</p>
	 * @param order rotation order to use
	 * @return an array of three angles, in the order specified by the set
	 * @exception CardanEulerSingularityException if the rotation is
	 * singular with respect to the angles set specified
	 */
	public double[] getAngles(RotationOrder order) {

		if (order == RotationOrder.XYZ) {

			// r (T .plusK) coordinates are :
			//  sin (theta), -cos (theta) sin (phi), cos (theta) cos (phi)
			// (-r) (T .plusI) coordinates are :
			// cos (psi) cos (theta), -sin (psi) cos (theta), sin (theta)
			// and we can choose to have theta in the interval [-PI/2 ; +PI/2]
			SGVec_3d v1 = applyTo(RotationOrder.Z);
			SGVec_3d v2 = applyInverseTo(RotationOrder.X);
			if  ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(-(v1.y), v1.z),
					Math.asin(v2.z),
					Math.atan2(-(v2.y), v2.x)
			};

		} else if (order == RotationOrder.XZY) {

			// r (T .plusJ) coordinates are :
			// -sin (psi), cos (psi) cos (phi), cos (psi) sin (phi)
			// (-r) (T .plusI) coordinates are :
			// cos (theta) cos (psi), -sin (psi), sin (theta) cos (psi)
			// and we can choose to have psi in the interval [-PI/2 ; +PI/2]
			SGVec_3d v1 = applyTo(RotationOrder.X);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.z, v1.y),
					-Math.asin(v2.y),
					Math.atan2(v2.z, v2.x)
			};

		} else if (order == RotationOrder.YXZ) {

			// r (T .plusK) coordinates are :
			//  cos (phi) sin (theta), -sin (phi), cos (phi) cos (theta)
			// (-r) (T .plusJ) coordinates are :
			// sin (psi) cos (phi), cos (psi) cos (phi), -sin (phi)
			// and we can choose to have phi in the interval [-PI/2 ; +PI/2]
			SGVec_3d v1 = applyTo(RotationOrder.Z);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.x, v1.z),
					-Math.asin(v2.z),
					Math.atan2(v2.x, v2.y)
			};

		} else if (order == RotationOrder.YZX) {

			// r (T .plusI) coordinates are :
			// cos (psi) cos (theta), sin (psi), -cos (psi) sin (theta)
			// (-r) (T .plusJ) coordinates are :
			// sin (psi), cos (phi) cos (psi), -sin (phi) cos (psi)
			// and we can choose to have psi in the interval [-PI/2 ; +PI/2]
			SGVec_3d v1 = applyTo(RotationOrder.X);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(-(v1.z), v1.x),
					Math.asin(v2.x),
					Math.atan2(-(v2.z), v2.y)
			};

		} else if (order == RotationOrder.ZXY) {

			// r (T .plusJ) coordinates are :
			// -cos (phi) sin (psi), cos (phi) cos (psi), sin (phi)
			// (-r) (T .plusK) coordinates are :
			// -sin (theta) cos (phi), sin (phi), cos (theta) cos (phi)
			// and we can choose to have phi in the interval [-PI/2 ; +PI/2]
			SGVec_3d v1 = applyTo(RotationOrder.Y);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(-(v1.x), v1.y),
					Math.asin(v2.y),
					Math.atan2(-(v2.x), v2.z)
			};

		} else if (order == RotationOrder.ZYX) {

			// r (T .plusI) coordinates are :
			//  cos (theta) cos (psi), cos (theta) sin (psi), -sin (theta)
			// (-r) (T .plusK) coordinates are :
			// -sin (theta), sin (phi) cos (theta), cos (phi) cos (theta)
			// and we can choose to have theta in the interval [-PI/2 ; +PI/2]
			SGVec_3d v1 = applyTo(RotationOrder.X);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.y, v1.x),
					-Math.asin(v2.x),
					Math.atan2(v2.y, v2.z)
			};

		} else if (order == RotationOrder.XYX) {

			// r (T .plusI) coordinates are :
			//  cos (theta), sin (phi1) sin (theta), -cos (phi1) sin (theta)
			// (-r) (T .plusI) coordinates are :
			// cos (theta), sin (theta) sin (phi2), sin (theta) cos (phi2)
			// and we can choose to have theta in the interval [0 ; PI]
			SGVec_3d v1 = applyTo(RotationOrder.X);
			SGVec_3d v2 = applyInverseTo(RotationOrder.X);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.y, -v1.z),
					Math.acos(v2.x),
					Math.atan2(v2.y, v2.z)
			};

		} else if (order == RotationOrder.XZX) {

			// r (T .plusI) coordinates are :
			//  cos (psi), cos (phi1) sin (psi), sin (phi1) sin (psi)
			// (-r) (T .plusI) coordinates are :
			// cos (psi), -sin (psi) cos (phi2), sin (psi) sin (phi2)
			// and we can choose to have psi in the interval [0 ; PI]
			SGVec_3d v1 = applyTo(RotationOrder.X);
			SGVec_3d v2 = applyInverseTo(RotationOrder.X);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.z, v1.y),
					Math.acos(v2.x),
					Math.atan2(v2.z, -v2.y)
			};

		} else if (order == RotationOrder.YXY) {

			// r (T .plusJ) coordinates are :
			//  sin (theta1) sin (phi), cos (phi), cos (theta1) sin (phi)
			// (-r) (T .plusJ) coordinates are :
			// sin (phi) sin (theta2), cos (phi), -sin (phi) cos (theta2)
			// and we can choose to have phi in the interval [0 ; PI]
			SGVec_3d v1 = applyTo(RotationOrder.Y);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.x, v1.z),
					Math.acos(v2.y),
					Math.atan2(v2.x, -v2.z)
			};

		} else if (order == RotationOrder.YZY) {

			// r (T .plusJ) coordinates are :
			//  -cos (theta1) sin (psi), cos (psi), sin (theta1) sin (psi)
			// (-r) (T .plusJ) coordinates are :
			// sin (psi) cos (theta2), cos (psi), sin (psi) sin (theta2)
			// and we can choose to have psi in the interval [0 ; PI]
			SGVec_3d v1 = applyTo(RotationOrder.Y);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.z, -v1.x),
					Math.acos(v2.y),
					Math.atan2(v2.z, v2.x)
			};

		} else if (order == RotationOrder.ZXZ) {

			// r (T .plusK) coordinates are :
			//  sin (psi1) sin (phi), -cos (psi1) sin (phi), cos (phi)
			// (-r) (T .plusK) coordinates are :
			// sin (phi) sin (psi2), sin (phi) cos (psi2), cos (phi)
			// and we can choose to have phi in the interval [0 ; PI]
			SGVec_3d v1 = applyTo(RotationOrder.Z);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.x, -v1.y),
					Math.acos(v2.z),
					Math.atan2(v2.x, v2.y)
			};

		} else { // last possibility is ZYZ

			// r (T .plusK) coordinates are :
			//  cos (psi1) sin (theta), sin (psi1) sin (theta), cos (theta)
			// (-r) (T .plusK) coordinates are :
			// -sin (theta) cos (psi2), sin (theta) sin (psi2), cos (theta)
			// and we can choose to have theta in the interval [0 ; PI]
			SGVec_3d v1 = applyTo(RotationOrder.Z);
			SGVec_3d v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				try {
					throw new CardanEulerSingularityException(true);
				} catch (CardanEulerSingularityException e) {
					e.printStackTrace(System.out);
				}
			}
			return new double[] {
					Math.atan2(v1.y, v1.x),
					Math.acos(v2.z),
					Math.atan2(v2.y, -v2.x)
			};

		}

	}

	/** Get an array representing the 3X3 matrix corresponding to this rotation instance
	 * Indices are in column major order. In other words 
	 * <br/> 
	 * 0, 3, 6 <br/>  
	 * 1, 4, 7 <br/> 
	 * 2, 5, 8 <br/>
	 * @return the matrix corresponding to the instance
	 */
	public double[] getMatrix3Val() {

		// create the matrix
		double[] values = new double[9];
		setToMatrix3Val(values);
		return values;
	}

	/** set input to the 3X3 matrix corresponding to the instance 
	 *  Indices are in column major order. In other words 
	 * <br/> 
	 * 0, 3, 6 <br/>  
	 * 1, 4, 7 <br/> 
	 * 2, 5, 8 <br/>
	 * @return the matrix corresponding to the instance
	 */
	public void setToMatrix3Val(double[] storeIn) {

		// products
		double q0q0  = q0 * q0;
		double q0q1  = q0 * q1;
		double q0q2  = q0 * q2;
		double q0q3  = q0 * q3;
		double q1q1  = q1 * q1;
		double q1q2  = q1 * q2;
		double q1q3  = q1 * q3;
		double q2q2  = q2 * q2;
		double q2q3  = q2 * q3;
		double q3q3  = q3 * q3;

		// create the matrix
		storeIn[0] = 2.0 * (q0q0 + q1q1) - 1.0;
		storeIn[1] = 2.0 * (q1q2 - q0q3);
		storeIn[2] = 2.0 * (q1q3 + q0q2);

		storeIn[3] = 2.0 * (q1q2 + q0q3);
		storeIn[4] = 2.0 * (q0q0 + q2q2) - 1.0;
		storeIn[5] = 2.0 * (q2q3 - q0q1);

		storeIn[6] = 2.0 * (q1q3 - q0q2);
		storeIn[7] = 2.0 * (q2q3 + q0q1);
		storeIn[8] = 2.0 * (q0q0 + q3q3) - 1.0;

	}

	/**
	 *  Get an array representing the 4X4 matrix corresponding to this rotation instance. 
	 * Indices are in column major order. In other words 
	 *<br/> 
	 * 0,  4,  8,  12 <br/>  
	 * 1,  5,  9,  13 <br/> 
	 * 2,  6, 10, 14 <br/>
 	 * 3,  7, 11, 15 <br/>
	 * */
	public double[] toMatrix4Val() {
		double[] result = new double[16]; 
		return toMatrix4Val(result, false); 
	}

	/**
	 *  Get an array representing the 4X4 matrix corresponding to this rotation instance. 
	 * Indices are in column major order. In other words 
	 * <br/> 
	 * 0,  4,  8,  12 <br/>  
	 * 1,  5,  9,  13 <br/> 
	 * 2,  6, 10, 14 <br/>
 	 * 3,  7, 11, 15 <br/>
	 * @param storeIn the array to storevalues in. 
	 * @param zeroOut if true, will zero out any elements in the matrix not corresponding to this rotation. 
	 * */
	public double[] toMatrix4Val(double[] storeIn, boolean zeroOut) {
		double q0q0  = q0 * q0;
		double q0q1  = q0 * q1;
		double q0q2  = q0 * q2;
		double q0q3  = q0 * q3;
		double q1q1  = q1 * q1;
		double q1q2  = q1 * q2;
		double q1q3  = q1 * q3;
		double q2q2  = q2 * q2;
		double q2q3  = q2 * q3;
		double q3q3  = q3 * q3;

		// create the matrix
		storeIn[0] = 2.0 * (q0q0 + q1q1) - 1.0;
		storeIn[1] = 2.0 * (q1q2 - q0q3);
		storeIn[2] = 2.0 * (q1q3 + q0q2);

		storeIn[4] = 2.0 * (q1q2 + q0q3);
		storeIn[5] = 2.0 * (q0q0 + q2q2) - 1.0;
		storeIn[6] = 2.0 * (q2q3 - q0q1);


		storeIn[8] = 2.0 * (q1q3 - q0q2);
		storeIn[9] = 2.0 * (q2q3 + q0q1);
		storeIn[10] = 2.0 * (q0q0 + q3q3) - 1.0;
		storeIn[15] = 1.0;

		if(zeroOut) {
			storeIn[3] = 0.0;
			storeIn[7] = 0.0;
			storeIn[11] = 0.0;
			storeIn[12] = 0.0;
			storeIn[13] = 0.0;
			storeIn[14] = 0.0;
			
		}

		return storeIn;
	}

	/** Apply the rotation to a vector.
	 * @param u vector to apply the rotation to
	 * @return a new vector which is the image of u by the rotation
	 */
	public <T extends SGVec_3d> T applyTo(T u) {

		double x = u.x;
		double y = u.y;
		double z = u.z;

		double s = q1 * x + q2 * y + q3 * z;
		T result = (T) u.copy();
		result.set(2 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x,
				2 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y,
				2 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z);
		return result;
	}


	/** Multiplies the instance by a scalar.
	 *
	 * @param alpha Scalar factor.
	 * @return a scaled quaternion.
	 */
	public MRotation multiply(final double alpha) {
		return new MRotation(alpha * q0,
				alpha * q1,
				alpha * q2,
				alpha * q3);
	}


	/** Returns the Hamilton product of the instance by a quaternion.
	 *
	 * @param q Quaternion.
	 * @return the product of this instance with {@code q}, in that order.
	 */
	public MRotation multiply(final MRotation q) {
		return multiply(this, q);
	}

	public static MRotation multiply(final MRotation q1, final MRotation q2) {
		// Components of the first quaternion.
		final double q1a = q1.getQ0();
		final double q1b = q1.getQ1();
		final double q1c = q1.getQ2();
		final double q1d = q1.getQ3();

		// Components of the second quaternion.
		final double q2a = q2.getQ0();
		final double q2b = q2.getQ1();
		final double q2c = q2.getQ2();
		final double q2d = q2.getQ3();

		// Components of the product.
		final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
		final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
		final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
		final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

		return new MRotation(w, x, y, z);
	}


	/** Computes the dot-product of two quaternions.
	 *
	 * @param q1 Quaternion.
	 * @param q2 Quaternion.
	 * @return the dot product of {@code q1} and {@code q2}.
	 */
	public static double dotProduct(final MRotation q1,
			final MRotation q2) {
		return q1.getQ0() * q2.getQ0() +
				q1.getQ1() * q2.getQ1() +
				q1.getQ2() * q2.getQ2() +
				q1.getQ3() * q2.getQ3();
	}

	/**
	 * Computes the dot-product of the instance by a quaternion.
	 *
	 * @param q Quaternion.
	 * @return the dot product of this instance and {@code q}.
	 */
	public double dotProduct(final MRotation q) {
		return dotProduct(this, q);
	}

	/** Apply the rotation to a vector stored in an array.
	 * @param in an array with three items which stores vector to rotate
	 * @param out an array with three items to put result to (it can be the same
	 * array as in)
	 */
	public void applyTo(final double[] in, final double[] out) {

		final double x = in[0];
		final double y = in[1];
		final double z = in[2];

		final double s = q1 * x + q2 * y + q3 * z;

		out[0] = 2 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x;
		out[1] = 2 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y;
		out[2] = 2 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z;

	}

	/** Apply the inverse of the rotation to a vector.
	 * @param u vector to apply the inverse of the rotation to
	 * @return a new vector which such that u is its image by the rotation
	 */
	public <T extends SGVec_3d> T applyInverseTo(T u) {

		double x = u.x;
		double y = u.y;
		double z = u.z;

		double s = q1 * x + q2 * y + q3 * z;
		double m0 = -q0;

		T result = (T) u.copy();
		result.set(2 * (m0 * (x * m0 - (q2 * z - q3 * y)) + s * q1) - x,
				2 * (m0 * (y * m0 - (q3 * x - q1 * z)) + s * q2) - y,
				2 * (m0 * (z * m0 - (q1 * y - q2 * x)) + s * q3) - z);
		return result;

	}

	/** Apply the inverse of the rotation to a vector stored in an array.
	 * @param in an array with three items which stores vector to rotate
	 * @param out an array with three items to put result to (it can be the same
	 * array as in)
	 */
	public void applyInverseTo(final double[] in, final double[] out) {

		final double x = in[0];
		final double y = in[1];
		final double z = in[2];

		final double s = q1 * x + q2 * y + q3 * z;
		final double m0 = -q0;

		out[0] = 2 * (m0 * (x * m0 - (q2 * z - q3 * y)) + s * q1) - x;
		out[1] = 2 * (m0 * (y * m0 - (q3 * x - q1 * z)) + s * q2) - y;
		out[2] = 2 * (m0 * (z * m0 - (q1 * y - q2 * x)) + s * q3) - z;

	}

	/** Apply the instance to another rotation.
	 * Applying the instance to a rotation is computing the composition
	 * in an order compliant with the following rule : let u be any
	 * vector and v its image by r (i.e. r.applyTo(u) = v), let w be the image
	 * of v by the instance (i.e. applyTo(v) = w), then w = comp.applyTo(u),
	 * where comp = applyTo(r).
	 * @param r rotation to apply the rotation to
	 * @return a new rotation which is the composition of r by the instance
	 */
	public MRotation applyTo(MRotation r) {
		return new MRotation(r.q0 * q0 - (r.q1 * q1 + r.q2 * q2 + r.q3 * q3),
				r.q1 * q0 + r.q0 * q1 + (r.q2 * q3 - r.q3 * q2),
				r.q2 * q0 + r.q0 * q2 + (r.q3 * q1 - r.q1 * q3),
				r.q3 * q0 + r.q0 * q3 + (r.q1 * q2 - r.q2 * q1),
				false);
	}

	/** Apply the inverse of the instance to another rotation.
	 * Applying the inverse of the instance to a rotation is computing
	 * the composition in an order compliant with the following rule :
	 * let u be any vector and v its image by r (i.e. r.applyTo(u) = v),
	 * let w be the inverse image of v by the instance
	 * (i.e. applyInverseTo(v) = w), then w = comp.applyTo(u), where
	 * comp = applyInverseTo(r).
	 * @param r rotation to apply the rotation to
	 * @return a new rotation which is the composition of r by the inverse
	 * of the instance
	 */
	public MRotation applyInverseTo(MRotation r) {
		return new MRotation(-r.q0 * q0 - (r.q1 * q1 + r.q2 * q2 + r.q3 * q3),
				-r.q1 * q0 + r.q0 * q1 + (r.q2 * q3 - r.q3 * q2),
				-r.q2 * q0 + r.q0 * q2 + (r.q3 * q1 - r.q1 * q3),
				-r.q3 * q0 + r.q0 * q3 + (r.q1 * q2 - r.q2 * q1),
				false);
	}


	/** Apply the instance to another rotation. Store the result in the specified rotation
	 * Applying the instance to a rotation is computing the composition
	 * in an order compliant with the following rule : let u be any
	 * vector and v its image by r (i.e. r.applyTo(u) = v), let w be the image
	 * of v by the instance (i.e. applyTo(v) = w), then w = comp.applyTo(u),
	 * where comp = applyTo(r).
	 * @param r rotation to apply the rotation to
	 * @param output the rotation to store the result in
	 * @return a new rotation which is the composition of r by the instance
	 */
	public void applyTo(MRotation r, MRotation output) {
		output.set(r.q0 * q0 - (r.q1 * q1 + r.q2 * q2 + r.q3 * q3),
				r.q1 * q0 + r.q0 * q1 + (r.q2 * q3 - r.q3 * q2),
				r.q2 * q0 + r.q0 * q2 + (r.q3 * q1 - r.q1 * q3),
				r.q3 * q0 + r.q0 * q3 + (r.q1 * q2 - r.q2 * q1),
				false);
	}

	/** Apply the inverse of the instance to another rotation. Store the result in the specified rotation
	 * Applying the inverse of the instance to a rotation is computing
	 * the composition in an order compliant with the following rule :
	 * let u be any vector and v its image by r (i.e. r.applyTo(u) = v),
	 * let w be the inverse image of v by the instance
	 * (i.e. applyInverseTo(v) = w), then w = comp.applyTo(u), where
	 * comp = applyInverseTo(r).
	 * @param r rotation to apply the rotation to
	 * @param output the rotation to store the result in
	 * @return a new rotation which is the composition of r by the inverse
	 * of the instance
	 */
	public void applyInverseTo(MRotation r, MRotation output) {
		output.set(-r.q0 * q0 - (r.q1 * q1 + r.q2 * q2 + r.q3 * q3),
				-r.q1 * q0 + r.q0 * q1 + (r.q2 * q3 - r.q3 * q2),
				-r.q2 * q0 + r.q0 * q2 + (r.q3 * q1 - r.q1 * q3),
				-r.q3 * q0 + r.q0 * q3 + (r.q1 * q2 - r.q2 * q1),
				false);
	}

	
	public MRotation setToConjugate() {
		q1 = -q1;
		q2 = -q2;
		q3 = -q3;		
		return this;
	}

	public void set(double q0, double q1, double q2, double q3,
			boolean needsNormalization) {

		this.q0 = q0;
		this.q1 = q1;
		this.q2 = q2;
		this.q3 = q3;

		if(needsNormalization) setToNormalized();
	}

	public void setToNormalized() {
		// normalization preprocessing
		double inv = 1.0 / Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
		q0 *= inv;
		q1 *= inv;
		q2 *= inv;
		q3 *= inv;
	}


	/**
	 * Computes the norm of the quaternion.
	 *
	 * @return the norm.
	 */
	public double len() {
		return Math.sqrt(q0 * q0 +
				q1 * q1 +
				q2 * q2 +
				q3 * q3);
	}

	/**
	 * Computes the normalized quaternion (the versor of the instance).
	 * The norm of the quaternion must not be zero.
	 *
	 * @return a normalized quaternion.
	 * @throws ZeroException if the norm of the quaternion is zero.
	 */
	public MRotation normalize() {
		final double norm = len();

		if (norm < Precision.SAFE_MIN_DOUBLE) {
			try {
				throw new Exception("Zero Norm");
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		return new MRotation(q0 / norm,
				q1 / norm,
				q2 / norm,
				q3 / norm);
	}

	public <V extends Vec3d<?>> void set(V u, V v) {

		double normProduct = u.mag() * v.mag();
		if (normProduct == 0) {
			try {
				throw new Exception("Zero Norm for Rotation defining vector");
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		double dot = u.dot(v);

		if (dot < ((2.0e-15 - 1.0) * normProduct)) {
			// special case u = -v: we select a PI angle rotation around
			// an arbitrary vector orthogonal to u
			V w = (V) u.getOrthogonal();
			q0 = 0.0;
			q1 = -w.x;
			q2 = -w.y;
			q3 = -w.z;
		} else {
			// general case: (u, v) defines a plane, we select
			// the shortest possible rotation: axis orthogonal to this plane
			q0 = Math.sqrt(0.5 * (1.0 + dot / normProduct));
			double coeff = 1.0 / (2.0 * q0 * normProduct);
			V q = (V) v.crossCopy(u);
			q1 = coeff * q.x;
			q2 = coeff * q.y;
			q3 = coeff * q.z;
		}

	}

	public <V extends Vec3d<?>> void set(V  axis, double angle) {

		double norm = axis.mag();
		if (norm == 0) {
			try {
				throw new Exception("Zero Norm for Rotation defining vector");
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		double halfAngle = -0.5 * angle;
		double coeff = Math.sin(halfAngle) / norm;

		q0 = Math.cos (halfAngle);
		q1 = coeff * axis.x;
		q2 = coeff * axis.y;
		q3 = coeff * axis.z;
	}

	/** Perfect orthogonality on a 3X3 matrix.
	 * @param m initial matrix (not exactly orthogonal)
	 * @param threshold convergence threshold for the iterative
	 * orthogonality correction (convergence is reached when the
	 * difference between two steps of the Frobenius norm of the
	 * correction is below this threshold)
	 * @return an orthogonal matrix close to m
	 * @exception NotARotationMatrixException if the matrix cannot be
	 * orthogonalized with the given threshold after 10 iterations
	 */
	private double[][] orthogonalizeMatrix(double[][] m, double threshold) {
		double[] m0 = m[0];
		double[] m1 = m[1];
		double[] m2 = m[2];
		double x00 = m0[0];
		double x01 = m0[1];
		double x02 = m0[2];
		double x10 = m1[0];
		double x11 = m1[1];
		double x12 = m1[2];
		double x20 = m2[0];
		double x21 = m2[1];
		double x22 = m2[2];
		double fn = 0;
		double fn1;

		double[][] o = new double[3][3];
		double[] o0 = o[0];
		double[] o1 = o[1];
		double[] o2 = o[2];

		// iterative correction: Xn+1 = Xn - 0.5 * (Xn.Mt.Xn - M)
		int i = 0;
		while (++i < 11) {

			// Mt.Xn
			double mx00 = m0[0] * x00 + m1[0] * x10 + m2[0] * x20;
			double mx10 = m0[1] * x00 + m1[1] * x10 + m2[1] * x20;
			double mx20 = m0[2] * x00 + m1[2] * x10 + m2[2] * x20;
			double mx01 = m0[0] * x01 + m1[0] * x11 + m2[0] * x21;
			double mx11 = m0[1] * x01 + m1[1] * x11 + m2[1] * x21;
			double mx21 = m0[2] * x01 + m1[2] * x11 + m2[2] * x21;
			double mx02 = m0[0] * x02 + m1[0] * x12 + m2[0] * x22;
			double mx12 = m0[1] * x02 + m1[1] * x12 + m2[1] * x22;
			double mx22 = m0[2] * x02 + m1[2] * x12 + m2[2] * x22;

			// Xn+1
			o0[0] = x00 - 0.5 * (x00 * mx00 + x01 * mx10 + x02 * mx20 - m0[0]);
			o0[1] = x01 - 0.5 * (x00 * mx01 + x01 * mx11 + x02 * mx21 - m0[1]);
			o0[2] = x02 - 0.5 * (x00 * mx02 + x01 * mx12 + x02 * mx22 - m0[2]);
			o1[0] = x10 - 0.5 * (x10 * mx00 + x11 * mx10 + x12 * mx20 - m1[0]);
			o1[1] = x11 - 0.5 * (x10 * mx01 + x11 * mx11 + x12 * mx21 - m1[1]);
			o1[2] = x12 - 0.5 * (x10 * mx02 + x11 * mx12 + x12 * mx22 - m1[2]);
			o2[0] = x20 - 0.5 * (x20 * mx00 + x21 * mx10 + x22 * mx20 - m2[0]);
			o2[1] = x21 - 0.5 * (x20 * mx01 + x21 * mx11 + x22 * mx21 - m2[1]);
			o2[2] = x22 - 0.5 * (x20 * mx02 + x21 * mx12 + x22 * mx22 - m2[2]);

			// correction on each elements
			double corr00 = o0[0] - m0[0];
			double corr01 = o0[1] - m0[1];
			double corr02 = o0[2] - m0[2];
			double corr10 = o1[0] - m1[0];
			double corr11 = o1[1] - m1[1];
			double corr12 = o1[2] - m1[2];
			double corr20 = o2[0] - m2[0];
			double corr21 = o2[1] - m2[1];
			double corr22 = o2[2] - m2[2];

			// Frobenius norm of the correction
			fn1 = corr00 * corr00 + corr01 * corr01 + corr02 * corr02 +
					corr10 * corr10 + corr11 * corr11 + corr12 * corr12 +
					corr20 * corr20 + corr21 * corr21 + corr22 * corr22;

			// convergence test
			if (Math.abs(fn1 - fn) <= threshold) {
				return o;
			}

			// prepare next iteration
			x00 = o0[0];
			x01 = o0[1];
			x02 = o0[2];
			x10 = o1[0];
			x11 = o1[1];
			x12 = o1[2];
			x20 = o2[0];
			x21 = o2[1];
			x22 = o2[2];
			fn  = fn1;
		}
		// the algorithm did not converge after 10 iterations
		try {
			throw new Exception("Failed to converge on orthogonal matrix after 10 iterations");
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}  
		return null;
	}

	/** Compute the <i>distance</i> between two rotations.
	 * <p>The <i>distance</i> is intended here as a way to check if two
	 * rotations are almost similar (i.e. they transform vectors the same way)
	 * or very different. It is mathematically defined as the angle of
	 * the rotation r that prepended to one of the rotations gives the other
	 * one:</p>
	 * <pre>
	 *        r<sub>1</sub>(r) = r<sub>2</sub>
	 * </pre>
	 * <p>This distance is an angle between 0 and &pi;. Its value is the smallest
	 * possible upper bound of the angle in radians between r<sub>1</sub>(v)
	 * and r<sub>2</sub>(v) for all possible vectors v. This upper bound is
	 * reached for some v. The distance is equal to 0 if and only if the two
	 * rotations are identical.</p>
	 * <p>Comparing two rotations should always be done using this value rather
	 * than for example comparing the components of the quaternions. It is much
	 * more stable, and has a geometric meaning. Also comparing quaternions
	 * components is error prone since for example quaternions (0.36, 0.48, -0.48, -0.64)
	 * and (-0.36, -0.48, 0.48, 0.64) represent exactly the same rotation despite
	 * their components are different (they are exact opposites).</p>
	 * @param r1 first rotation
	 * @param r2 second rotation
	 * @return <i>distance</i> between r1 and r2
	 */
	public static double distance(MRotation r1,MRotation r2) {
		return r1.applyInverseTo(r2).getAngle();
	}


	public boolean equalTo(MRotation m) {
		return distance(this, m) < MathUtils.DOUBLE_ROUNDING_ERROR;
	}

	public String toString() {
		String result = "axis: " + getAxis().toVec3f().toString();
		result += "\n angle : " + (float)Math.toDegrees(getAngle()) + " degrees " ;
		result += "\n angle : " + (float)getAngle() + " radians " ;
		return result;
	}
}