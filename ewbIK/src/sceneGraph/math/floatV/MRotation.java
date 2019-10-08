/**
 * This class provides similar functionality to apache commons rotations
 * but it is mutable. It is licensed under the Apache Commons License.
 */

package sceneGraph.math.floatV;



import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.numerical.Precision;
import sceneGraph.numerical.Precision.*;





public class MRotation {
	public static final MRotation IDENTITY = new MRotation(1.0f, 0.0f, 0.0f, 0.0f, false);

	/** Scalar coordinate of the quaternion. */
	private float q0;

	/** First coordinate of the vectorial part of the quaternion. */
	private float q1;

	/** Second coordinate of the vectorial part of the quaternion. */
	private float q2;

	/** Third coordinate of the vectorial part of the quaternion. */
	private float q3;

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
	public MRotation(float q0, float q1, float q2, float q3,
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
		this(1.0f, 0.0f, 0.0f, 0.0f, false);
	}

	/**assumes no noralization required**/
	public MRotation(float q0, float q1, float q2, float q3) {
		this(q0,q1,q2,q3,false);
	}


	/** Build a rotation from an axis and an angle.
	 * <p>We use the convention that angles are oriented according to
	 * the effect of the rotation on vectors around the axis. That means
	 * that if (i, j, k) is a direct frame and if we first provide +k as
	 * the axis and &pi;/2 as the angle to this constructor, and then
	 * {@link #applyTo(SGVec_3f ) apply} the instance to +i, we will get
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
	public MRotation(SGVec_3f  axis, float angle) {

		float norm = axis.mag();
		/*if (norm == 0) {
			throw new MathIllegalArgumentException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_AXIS);
		}*/

		float halfAngle = -0.5f * angle;
		float coeff = MathUtils.sin(halfAngle) / norm;

		q0 = MathUtils.cos (halfAngle);
		q1 = coeff * axis.x;
		q2 = coeff * axis.y;
		q3 = coeff * axis.z;

	}
	
	/**
	 * modify this rotation to have the specified axis, 
	 * without changing the angle.  
	 *
	 * @param angle
	 */
	public void setAxis(SGVec_3f  newAxis) {
		
		float angle = this.getAngle();
		float norm = newAxis.mag();
		/*if (norm == 0) {
			throw new MathIllegalArgumentException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_AXIS);
		}*/

		float halfAngle = -0.5f * angle;
		float coeff = MathUtils.sin(halfAngle) / norm;

		q0 = MathUtils.cos (halfAngle);
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
	public void setAngle(float newAngle) {
		
		SGVec_3f  axis = getAxis();
		float norm = axis.mag();
		/*if (norm == 0) {
			throw new MathIllegalArgumentException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_AXIS);
		}*/

		float halfAngle = -0.5f * newAngle;
		float coeff = MathUtils.sin(halfAngle) / norm;

		q0 = MathUtils.cos (halfAngle);
		q1 = coeff * axis.x;
		q2 = coeff * axis.y;
		q3 = coeff * axis.z;
		
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
	public MRotation(float[][] m, float threshold)
			throws NotARotationMatrixException {

		// dimension check
		if ((m.length != 3) || (m[0].length != 3) ||
				(m[1].length != 3) || (m[2].length != 3)) {
			throw new NotARotationMatrixException(
					LocalizedFormats.ROTATION_MATRIX_DIMENSIONS,
					m.length, m[0].length);
		}

		// compute a "close" orthogonal matrix
		float[][] ort = orthogonalizeMatrix(m, threshold);

		// check the sign of the determinant
		float det = ort[0][0] * (ort[1][1] * ort[2][2] - ort[2][1] * ort[1][2]) -
				ort[1][0] * (ort[0][1] * ort[2][2] - ort[2][1] * ort[0][2]) +
				ort[2][0] * (ort[0][1] * ort[1][2] - ort[1][1] * ort[0][2]);
		if (det < 0.0) {
			throw new NotARotationMatrixException(
					LocalizedFormats.CLOSEST_ORTHOGONAL_MATRIX_HAS_NEGATIVE_DETERMINANT,
					det);
		}

		float[] quat = mat2quat(ort);
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
	public MRotation(SGVec_3f u1, SGVec_3f u2, SGVec_3f v1, SGVec_3f v2)
			throws MathArithmeticException {

		 // norms computation
		  float u1u1 = u1.dot(u1);
		  float u2u2 = u2.dot(u2);
		  float v1v1 = v1.dot(v1);
		  float v2v2 = v2.dot(v2);
		  if ((u1u1 == 0) || (u2u2 == 0) || (v1v1 == 0) || (v2v2 == 0)) {
		    throw new IllegalArgumentException("zero norm for rotation defining vector");
		  }

		  float u1x = u1.x;
		  float u1y = u1.y;
		  float u1z = u1.z;

		  float u2x = u2.x;
		  float u2y = u2.y;
		  float u2z = u2.z;

		  // normalize v1 in order to have (v1'|v1') = (u1|u1)
		  float coeff = MathUtils.sqrt (u1u1 / v1v1);
		  float v1x   = coeff * v1.x;
		  float v1y   = coeff * v1.y;
		  float v1z   = coeff * v1.z;
		  v1 = new SGVec_3f(v1x, v1y, v1z);

		  // adjust v2 in order to have (u1|u2) = (v1|v2) and (v2'|v2') = (u2|u2)
		  float u1u2   = u1.dot(u2);
		  float v1v2   = v1.dot(v2);
		  float coeffU = u1u2 / u1u1;
		  float coeffV = v1v2 / u1u1;
		  float beta   = MathUtils.sqrt((u2u2 - u1u2 * coeffU) / (v2v2 - v1v2 * coeffV));
		  float alpha  = coeffU - beta * coeffV;
		  float v2x    = alpha * v1x + beta * v2.x;
		  float v2y    = alpha * v1y + beta * v2.y;
		  float v2z    = alpha * v1z + beta * v2.z;
		  v2 = new SGVec_3f(v2x, v2y, v2z);

		  // preliminary computation (we use explicit formulation instead
		  // of relying on the Vector3D class in order to avoid building lots
		  // of temporary objects)
		  SGVec_3f uRef = u1;
		  SGVec_3f vRef = v1;
		  float dx1 = v1x - u1.x;
		  float dy1 = v1y - u1.y;
		  float dz1 = v1z - u1.z;
		  float dx2 = v2x - u2.x;
		  float dy2 = v2y - u2.y;
		  float dz2 = v2z - u2.z;
		  SGVec_3f k = new SGVec_3f(dy1 * dz2 - dz1 * dy2,
		                            dz1 * dx2 - dx1 * dz2,
		                            dx1 * dy2 - dy1 * dx2);
		  float c = k.x * (u1y * u2z - u1z * u2y) +
		             k.y * (u1z * u2x - u1x * u2z) +
		             k.z * (u1x * u2y - u1y * u2x);

		  if (MathUtils.abs(c) <=MathUtils.FLOAT_ROUNDING_ERROR) {
		    // the (q1, q2, q3) vector is in the (u1, u2) plane
		    // we try other vectors
			 SGVec_3f u3 = u1.crossCopy(u2);
			 SGVec_3f v3 = v1.crossCopy(v2);
		    float u3x  = u3.x;
		    float u3y  = u3.y;
		    float u3z  = u3.z;
		    float v3x  = v3.x;
		    float v3y  = v3.y;
		    float v3z  = v3.z;

		    float dx3 = v3x - u3x;
		    float dy3 = v3y - u3y;
		    float dz3 = v3z - u3z;
		    k = new SGVec_3f(dy1 * dz3 - dz1 * dy3,
		                     dz1 * dx3 - dx1 * dz3,
		                     dx1 * dy3 - dy1 * dx3);
		    c = k.x * (u1y * u3z - u1z * u3y) +
		        k.y * (u1z * u3x - u1x * u3z) +
		        k.z * (u1x * u3y - u1y * u3x);

		    if (MathUtils.abs(c) <= MathUtils.FLOAT_ROUNDING_ERROR) {
		      // the (q1, q2, q3) vector is aligned with u1:
		      // we try (u2, u3) and (v2, v3)
		      k = new SGVec_3f(dy2 * dz3 - dz2 * dy3,
		                       dz2 * dx3 - dx2 * dz3,
		                       dx2 * dy3 - dy2 * dx3);
		      c = k.x * (u2y * u3z - u2z * u3y) +
		          k.y * (u2z * u3x - u2x * u3z) +
		          k.z * (u2x * u3y - u2y * u3x);

		      if (MathUtils.abs(c) <= MathUtils.FLOAT_ROUNDING_ERROR) {
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
		      vRef = v2;

		    }

		  }

		  // compute the vectorial part
		  c = (float) Math.sqrt(c);
		  float inv = (float)1.0 / (c + c);
		  q1 = inv * k.x;
		  q2 = inv * k.y;
		  q3 = inv * k.z;

		  // compute the scalar part
		   k = new SGVec_3f(uRef.y * q3 - uRef.z * q2,
		                    uRef.z * q1 - uRef.x * q3,
		                    uRef.x * q2 - uRef.y * q1);
		   c = k.dot(k);
		  q0 = vRef.dot(k) / (c + c);
		
		/*// build orthonormalized base from u1, u2
		// this fails when vectors are null or colinear, which is forbidden to define a rotation
		final SGVec_3f u3 = u1.crossCopy(u2).normalize();
		u2 = u3.crossCopy(u1).normalize();
		u1 = u1.normalize();

		// build an orthonormalized base from v1, v2
		// this fails when vectors are null or colinear, which is forbidden to define a rotation
		final SGVec_3f v3 = v1.crossCopy(v2).normalize();
		v2 = v3.crossCopy(v1).normalize();
		v1 = v1.normalize();

		// buid a matrix transforming the first base into the second one
		final float[][] m = new float[][] {
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

		float[] quat = mat2quat(m);
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
	public MRotation(SGVec_3f u, SGVec_3f v) throws MathArithmeticException {

		float normProduct = u.mag() * v.mag();
		if (normProduct == 0) {
			throw new MathArithmeticException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_DEFINING_VECTOR);
		}

		float dot = u.dot(v);

		if (dot < ((2.0e-15 - 1.0) * normProduct)) {
			// special case u = -v: we select a PI angle rotation around
			// an arbitrary vector orthogonal to u
			SGVec_3f w = u.getOrthogonal();
			q0 = 0.0f;
			q1 = -w.x;
			q2 = -w.y;
			q3 = -w.z;
		} else {
			// general case: (u, v) defines a plane, we select
			// the shortest possible rotation: axis orthogonal to this plane
			q0 = MathUtils.sqrt(0.5f * (1.0f + dot / normProduct));
			float coeff = 1.0f / (2.0f * q0 * normProduct);
			SGVec_3f q = v.crossCopy(u);
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
	public MRotation(RotationOrder order,
			float alpha1, float alpha2, float alpha3) {
		MRotation r1 = new MRotation((SGVec_3f ) order.getA1(), alpha1);
		MRotation r2 = new MRotation((SGVec_3f ) order.getA2(), alpha2);
		MRotation r3 = new MRotation((SGVec_3f ) order.getA3(), alpha3);
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
	private static float[] mat2quat(final float[][] ort) {

		final float[] quat = new float[4];

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
		float s = ort[0][0] + ort[1][1] + ort[2][2];
		if (s > -0.19) {
			// compute q0 and deduce q1, q2 and q3
			quat[0] = (float)(0.5f * Math.sqrt(s + 1.0f));
			float inv = 0.25f / quat[0];
			quat[1] = inv * (ort[1][2] - ort[2][1]);
			quat[2] = inv * (ort[2][0] - ort[0][2]);
			quat[3] = inv * (ort[0][1] - ort[1][0]);
		} else {
			s = ort[0][0] - ort[1][1] - ort[2][2];
			if (s > -0.19) {
				// compute q1 and deduce q0, q2 and q3
				quat[1] =(float)(0.5f * Math.sqrt(s + 1.0));
				float inv = 0.25f / quat[1];
				quat[0] = inv * (ort[1][2] - ort[2][1]);
				quat[2] = inv * (ort[0][1] + ort[1][0]);
				quat[3] = inv * (ort[0][2] + ort[2][0]);
			} else {
				s = ort[1][1] - ort[0][0] - ort[2][2];
				if (s > -0.19) {
					// compute q2 and deduce q0, q1 and q3
					quat[2] = (float)(0.5f * Math.sqrt(s + 1.0));
					float inv = 0.25f / quat[2];
					quat[0] = inv * (ort[2][0] - ort[0][2]);
					quat[1] = inv * (ort[0][1] + ort[1][0]);
					quat[3] = inv * (ort[2][1] + ort[1][2]);
				} else {
					// compute q3 and deduce q0, q1 and q2
					s = ort[2][2] - ort[0][0] - ort[1][1];
					quat[3] = (float)(0.5f * Math.sqrt(s + 1.0));
					float inv = 0.25f / quat[3];
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

	/** Get the scalar coordinate of the quaternion.
	 * @return scalar coordinate of the quaternion
	 */
	public float getQ0() {
		return q0;
	}

	/** Get the first coordinate of the vectorial part of the quaternion.
	 * @return first coordinate of the vectorial part of the quaternion
	 */
	public float getQ1() {
		return q1;
	}

	/** Get the second coordinate of the vectorial part of the quaternion.
	 * @return second coordinate of the vectorial part of the quaternion
	 */
	public float getQ2() {
		return q2;
	}

	/** Get the third coordinate of the vectorial part of the quaternion.
	 * @return third coordinate of the vectorial part of the quaternion
	 */
	public float getQ3() {
		return q3;
	}

	/** Get the normalized axis of the rotation.
	 * @return normalized axis of the rotation
	 * @see #Rotation(SGVec_3f , float)
	 */
	public SGVec_3f getAxis() {
		float squaredSine = q1 * q1 + q2 * q2 + q3 * q3;
		if (squaredSine == 0) {
			return new SGVec_3f(1, 0, 0);
		} else if (q0 < 0) {
			float inverse = 1f / MathUtils.sqrt(squaredSine);
			return new SGVec_3f(q1 * inverse, q2 * inverse, q3 * inverse);
		}
		float inverse = -1 / MathUtils.sqrt(squaredSine);
		return new SGVec_3f(q1 * inverse, q2 * inverse, q3 * inverse);
	}


	public MRotation getInverse() {
		final float squareNorm = q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3;	

		return new MRotation(q0 / squareNorm,
				-q1 / squareNorm,
				-q2 / squareNorm,
				-q3 / squareNorm);
	}



	/** Get the angle of the rotation.
	 * @return angle of the rotation (between 0 and &pi;)
	 * @see #Rotation(SGVec_3f , float)
	 */
	public float getAngle() {
		if ((q0 < -0.1) || (q0 > 0.1)) {
			return 2 * MathUtils.asin(MathUtils.sqrt(q1 * q1 + q2 * q2 + q3 * q3));
		} else if (q0 < 0) {
			return 2 * MathUtils.acos(-q0);
		}
		return 2 * MathUtils.acos(q0);
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
	public float[] getAngles(RotationOrder order)
			throws CardanEulerSingularityException {

		if (order == RotationOrder.XYZ) {

			// r (SGVec_3f .plusK) coordinates are :
			//  sin (theta), -cos (theta) sin (phi), cos (theta) cos (phi)
			// (-r) (SGVec_3f .plusI) coordinates are :
			// cos (psi) cos (theta), -sin (psi) cos (theta), sin (theta)
			// and we can choose to have theta in the interval [-PI/2 ; +PI/2]
			SGVec_3f v1 = applyTo(RotationOrder.Z);
			SGVec_3f v2 = applyInverseTo(RotationOrder.X);
			if  ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				throw new CardanEulerSingularityException(true);
			}
			return new float[] {
					MathUtils.atan2(-(v1.y), v1.z),
					MathUtils.asin(v2.z),
					MathUtils.atan2(-(v2.y), v2.x)
			};

		} else if (order == RotationOrder.XZY) {

			// r (SGVec_3f .plusJ) coordinates are :
			// -sin (psi), cos (psi) cos (phi), cos (psi) sin (phi)
			// (-r) (SGVec_3f .plusI) coordinates are :
			// cos (theta) cos (psi), -sin (psi), sin (theta) cos (psi)
			// and we can choose to have psi in the interval [-PI/2 ; +PI/2]
			SGVec_3f v1 = applyTo(RotationOrder.X);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				throw new CardanEulerSingularityException(true);
			}
			return new float[] {
					MathUtils.atan2(v1.z, v1.y),
					-MathUtils.asin(v2.y),
					MathUtils.atan2(v2.z, v2.x)
			};

		} else if (order == RotationOrder.YXZ) {

			// r (SGVec_3f .plusK) coordinates are :
			//  cos (phi) sin (theta), -sin (phi), cos (phi) cos (theta)
			// (-r) (SGVec_3f .plusJ) coordinates are :
			// sin (psi) cos (phi), cos (psi) cos (phi), -sin (phi)
			// and we can choose to have phi in the interval [-PI/2 ; +PI/2]
			SGVec_3f v1 = applyTo(RotationOrder.Z);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				throw new CardanEulerSingularityException(true);
			}
			return new float[] {
					MathUtils.atan2(v1.x, v1.z),
					-MathUtils.asin(v2.z),
					MathUtils.atan2(v2.x, v2.y)
			};

		} else if (order == RotationOrder.YZX) {

			// r (SGVec_3f .plusI) coordinates are :
			// cos (psi) cos (theta), sin (psi), -cos (psi) sin (theta)
			// (-r) (SGVec_3f .plusJ) coordinates are :
			// sin (psi), cos (phi) cos (psi), -sin (phi) cos (psi)
			// and we can choose to have psi in the interval [-PI/2 ; +PI/2]
			SGVec_3f v1 = applyTo(RotationOrder.X);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				throw new CardanEulerSingularityException(true);
			}
			return new float[] {
					MathUtils.atan2(-(v1.z), v1.x),
					MathUtils.asin(v2.x),
					MathUtils.atan2(-(v2.z), v2.y)
			};

		} else if (order == RotationOrder.ZXY) {

			// r (SGVec_3f .plusJ) coordinates are :
			// -cos (phi) sin (psi), cos (phi) cos (psi), sin (phi)
			// (-r) (SGVec_3f .plusK) coordinates are :
			// -sin (theta) cos (phi), sin (phi), cos (theta) cos (phi)
			// and we can choose to have phi in the interval [-PI/2 ; +PI/2]
			SGVec_3f v1 = applyTo(RotationOrder.Y);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				throw new CardanEulerSingularityException(true);
			}
			return new float[] {
					MathUtils.atan2(-(v1.x), v1.y),
					MathUtils.asin(v2.y),
					MathUtils.atan2(-(v2.x), v2.z)
			};

		} else if (order == RotationOrder.ZYX) {

			// r (SGVec_3f .plusI) coordinates are :
			//  cos (theta) cos (psi), cos (theta) sin (psi), -sin (theta)
			// (-r) (SGVec_3f .plusK) coordinates are :
			// -sin (theta), sin (phi) cos (theta), cos (phi) cos (theta)
			// and we can choose to have theta in the interval [-PI/2 ; +PI/2]
			SGVec_3f v1 = applyTo(RotationOrder.X);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				throw new CardanEulerSingularityException(true);
			}
			return new float[] {
					MathUtils.atan2(v1.y, v1.x),
					-MathUtils.asin(v2.x),
					MathUtils.atan2(v2.y, v2.z)
			};

		} else if (order == RotationOrder.XYX) {

			// r (SGVec_3f .plusI) coordinates are :
			//  cos (theta), sin (phi1) sin (theta), -cos (phi1) sin (theta)
			// (-r) (SGVec_3f .plusI) coordinates are :
			// cos (theta), sin (theta) sin (phi2), sin (theta) cos (phi2)
			// and we can choose to have theta in the interval [0 ; PI]
			SGVec_3f v1 = applyTo(RotationOrder.X);
			SGVec_3f v2 = applyInverseTo(RotationOrder.X);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				throw new CardanEulerSingularityException(false);
			}
			return new float[] {
					MathUtils.atan2(v1.y, -v1.z),
					MathUtils.acos(v2.x),
					MathUtils.atan2(v2.y, v2.z)
			};

		} else if (order == RotationOrder.XZX) {

			// r (SGVec_3f .plusI) coordinates are :
			//  cos (psi), cos (phi1) sin (psi), sin (phi1) sin (psi)
			// (-r) (SGVec_3f .plusI) coordinates are :
			// cos (psi), -sin (psi) cos (phi2), sin (psi) sin (phi2)
			// and we can choose to have psi in the interval [0 ; PI]
			SGVec_3f v1 = applyTo(RotationOrder.X);
			SGVec_3f v2 = applyInverseTo(RotationOrder.X);
			if ((v2.x < -0.9999999999) || (v2.x > 0.9999999999)) {
				throw new CardanEulerSingularityException(false);
			}
			return new float[] {
					MathUtils.atan2(v1.z, v1.y),
					MathUtils.acos(v2.x),
					MathUtils.atan2(v2.z, -v2.y)
			};

		} else if (order == RotationOrder.YXY) {

			// r (SGVec_3f .plusJ) coordinates are :
			//  sin (theta1) sin (phi), cos (phi), cos (theta1) sin (phi)
			// (-r) (SGVec_3f .plusJ) coordinates are :
			// sin (phi) sin (theta2), cos (phi), -sin (phi) cos (theta2)
			// and we can choose to have phi in the interval [0 ; PI]
			SGVec_3f v1 = applyTo(RotationOrder.Y);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				throw new CardanEulerSingularityException(false);
			}
			return new float[] {
					MathUtils.atan2(v1.x, v1.z),
					MathUtils.acos(v2.y),
					MathUtils.atan2(v2.x, -v2.z)
			};

		} else if (order == RotationOrder.YZY) {

			// r (SGVec_3f .plusJ) coordinates are :
			//  -cos (theta1) sin (psi), cos (psi), sin (theta1) sin (psi)
			// (-r) (SGVec_3f .plusJ) coordinates are :
			// sin (psi) cos (theta2), cos (psi), sin (psi) sin (theta2)
			// and we can choose to have psi in the interval [0 ; PI]
			SGVec_3f v1 = applyTo(RotationOrder.Y);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Y);
			if ((v2.y < -0.9999999999) || (v2.y > 0.9999999999)) {
				throw new CardanEulerSingularityException(false);
			}
			return new float[] {
					MathUtils.atan2(v1.z, -v1.x),
					MathUtils.acos(v2.y),
					MathUtils.atan2(v2.z, v2.x)
			};

		} else if (order == RotationOrder.ZXZ) {

			// r (SGVec_3f .plusK) coordinates are :
			//  sin (psi1) sin (phi), -cos (psi1) sin (phi), cos (phi)
			// (-r) (SGVec_3f .plusK) coordinates are :
			// sin (phi) sin (psi2), sin (phi) cos (psi2), cos (phi)
			// and we can choose to have phi in the interval [0 ; PI]
			SGVec_3f v1 = applyTo(RotationOrder.Z);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				throw new CardanEulerSingularityException(false);
			}
			return new float[] {
					MathUtils.atan2(v1.x, -v1.y),
					MathUtils.acos(v2.z),
					MathUtils.atan2(v2.x, v2.y)
			};

		} else { // last possibility is ZYZ

			// r (SGVec_3f .plusK) coordinates are :
			//  cos (psi1) sin (theta), sin (psi1) sin (theta), cos (theta)
			// (-r) (SGVec_3f .plusK) coordinates are :
			// -sin (theta) cos (psi2), sin (theta) sin (psi2), cos (theta)
			// and we can choose to have theta in the interval [0 ; PI]
			SGVec_3f v1 = applyTo(RotationOrder.Z);
			SGVec_3f v2 = applyInverseTo(RotationOrder.Z);
			if ((v2.z < -0.9999999999) || (v2.z > 0.9999999999)) {
				throw new CardanEulerSingularityException(false);
			}
			return new float[] {
					MathUtils.atan2(v1.y, v1.x),
					MathUtils.acos(v2.z),
					MathUtils.atan2(v2.y, -v2.x)
			};

		}

	}

	/** Get the 3X3 matrix corresponding to the instance
	 * @return the matrix corresponding to the instance
	 */
	public Matrix3f getMatrix() {

		// products
		float q0q0  = q0 * q0;
		float q0q1  = q0 * q1;
		float q0q2  = q0 * q2;
		float q0q3  = q0 * q3;
		float q1q1  = q1 * q1;
		float q1q2  = q1 * q2;
		float q1q3  = q1 * q3;
		float q2q2  = q2 * q2;
		float q2q3  = q2 * q3;
		float q3q3  = q3 * q3;

		// create the matrix
		float[] values = new float[9];  
		values[Matrix3f.M00] = 2.0f * (q0q0 + q1q1) - 1.0f;
		values[Matrix3f.M10] = 2.0f * (q1q2 - q0q3);
		values[Matrix3f.M20] = 2.0f * (q1q3 + q0q2);

		values[Matrix3f.M01] = 2.0f * (q1q2 + q0q3);
		values[Matrix3f.M11] = 2.0f * (q0q0 + q2q2) - 1.0f;
		values[Matrix3f.M21] = 2.0f * (q2q3 - q0q1);

		values[Matrix3f.M02] = 2.0f * (q1q3 - q0q2);
		values[Matrix3f.M12] = 2.0f * (q2q3 + q0q1);
		values[Matrix3f.M22] = 2.0f * (q0q0 + q3q3) - 1.0f;
		
				
		Matrix3f result = new Matrix3f(values); 
		/*float[][] m = new float[3][];
		m[0] = new float[3];
		m[1] = new float[3];
		m[2] = new float[3];

		m [0][0] = 2.0 * (q0q0 + q1q1) - 1.0;
		m [1][0] = 2.0 * (q1q2 - q0q3);
		m [2][0] = 2.0 * (q1q3 + q0q2);

		m [0][1] = 2.0 * (q1q2 + q0q3);
		m [1][1] = 2.0 * (q0q0 + q2q2) - 1.0;
		m [2][1] = 2.0 * (q2q3 - q0q1);

		m [0][2] = 2.0 * (q1q3 - q0q2);
		m [1][2] = 2.0 * (q2q3 + q0q1);
		m [2][2] = 2.0 * (q0q0 + q3q3) - 1.0;*/

		return result;
	}
	
	public float[] toMatrix4Val() {
		float[] result = new float[16]; 
		return toMatrix4Val(result); 
	}
	
	public float[] toMatrix4Val(float[] storeIn) {
		float q0q0  = q0 * q0;
		float q0q1  = q0 * q1;
		float q0q2  = q0 * q2;
		float q0q3  = q0 * q3;
		float q1q1  = q1 * q1;
		float q1q2  = q1 * q2;
		float q1q3  = q1 * q3;
		float q2q2  = q2 * q2;
		float q2q3  = q2 * q3;
		float q3q3  = q3 * q3;

		// create the matrix
		storeIn[Matrix4f.M00] = 2.0f * (q0q0 + q1q1) - 1.0f;
		storeIn[Matrix4f.M10] = 2.0f * (q1q2 - q0q3);
		storeIn[Matrix4f.M20] = 2.0f * (q1q3 + q0q2);

		storeIn[Matrix4f.M01] = 2.0f * (q1q2 + q0q3);
		storeIn[Matrix4f.M11] = 2.0f * (q0q0 + q2q2) - 1.0f;
		storeIn[Matrix4f.M21] = 2.0f * (q2q3 - q0q1);

		storeIn[Matrix4f.M02] = 2.0f * (q1q3 - q0q2);
		storeIn[Matrix4f.M12] = 2.0f * (q2q3 + q0q1);
		storeIn[Matrix4f.M22] = 2.0f * (q0q0 + q3q3) - 1.0f;
		storeIn[Matrix4f.M33] = 1.0f;
		
		return storeIn;
	}

	/** Apply the rotation to a vector.
	 * @param u vector to apply the rotation to
	 * @return a new vector which is the image of u by the rotation
	 */
	public SGVec_3f applyTo(SGVec_3f u) {

		float x = u.x;
		float y = u.y;
		float z = u.z;

		float s = q1 * x + q2 * y + q3 * z;

		return new SGVec_3f(2 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x,
				2 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y,
				2 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z);

	}
	
	
     /** Multiplies the instance by a scalar.
     *
     * @param alpha Scalar factor.
     * @return a scaled quaternion.
     */
    public MRotation multiply(final float alpha) {
        return new MRotation(alpha * q0,
                              alpha * q1,
                              alpha * q2,
                              alpha * q3);
    }

     
     /** Returns the Hamilton product of the instance by a quaternion.
      *
      * @param q Quaternionf.
      * @return the product of this instance with {@code q}, in that order.
      */
     public MRotation multiply(final MRotation q) {
         return multiply(this, q);
     }
     
     public static MRotation multiply(final MRotation q1, final MRotation q2) {
       // Components of the first quaternion.
       final float q1a = q1.getQ0();
       final float q1b = q1.getQ1();
       final float q1c = q1.getQ2();
       final float q1f = q1.getQ3();
   
       // Components of the second quaternion.
       final float q2a = q2.getQ0();
       final float q2b = q2.getQ1();
       final float q2c = q2.getQ2();
       final float q2f = q2.getQ3();
   
       // Components of the product.
       final float w = q1a * q2a - q1b * q2b - q1c * q2c - q1f * q2f;
       final float x = q1a * q2b + q1b * q2a + q1c * q2f - q1f * q2c;
       final float y = q1a * q2c - q1b * q2f + q1c * q2a + q1f * q2b;
       final float z = q1a * q2f + q1b * q2c - q1c * q2b + q1f * q2a;
   
       return new MRotation(w, x, y, z);
   }
    
    
     /** Computes the dot-product of two quaternions.
     *
     * @param q1 Quaternionf.
     * @param q2 Quaternionf.
     * @return the dot product of {@code q1} and {@code q2}.
     */
    public static float dotProduct(final MRotation q1,
                                    final MRotation q2) {
        return q1.getQ0() * q2.getQ0() +
            q1.getQ1() * q2.getQ1() +
            q1.getQ2() * q2.getQ2() +
            q1.getQ3() * q2.getQ3();
    }
    
    /**
     * Computes the dot-product of the instance by a quaternion.
     *
     * @param q Quaternionf.
     * @return the dot product of this instance and {@code q}.
     */
    public float dotProduct(final MRotation q) {
        return dotProduct(this, q);
    }
     
	/** Apply the rotation to a vector stored in an array.
	 * @param in an array with three items which stores vector to rotate
	 * @param out an array with three items to put result to (it can be the same
	 * array as in)
	 */
	public void applyTo(final float[] in, final float[] out) {

		final float x = in[0];
		final float y = in[1];
		final float z = in[2];

		final float s = q1 * x + q2 * y + q3 * z;

		out[0] = 2 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x;
		out[1] = 2 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y;
		out[2] = 2 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z;

	}

	/** Apply the inverse of the rotation to a vector.
	 * @param u vector to apply the inverse of the rotation to
	 * @return a new vector which such that u is its image by the rotation
	 */
	public SGVec_3f applyInverseTo(SGVec_3f u) {

		float x = u.x;
		float y = u.y;
		float z = u.z;

		float s = q1 * x + q2 * y + q3 * z;
		float m0 = -q0;

		return new SGVec_3f(2 * (m0 * (x * m0 - (q2 * z - q3 * y)) + s * q1) - x,
				2 * (m0 * (y * m0 - (q3 * x - q1 * z)) + s * q2) - y,
				2 * (m0 * (z * m0 - (q1 * y - q2 * x)) + s * q3) - z);

	}

	/** Apply the inverse of the rotation to a vector stored in an array.
	 * @param in an array with three items which stores vector to rotate
	 * @param out an array with three items to put result to (it can be the same
	 * array as in)
	 */
	public void applyInverseTo(final float[] in, final float[] out) {

		final float x = in[0];
		final float y = in[1];
		final float z = in[2];

		final float s = q1 * x + q2 * y + q3 * z;
		final float m0 = -q0;

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


	public void set(float q0, float q1, float q2, float q3,
			boolean needsNormalization) {

		this.q0 = q0;
		this.q1 = q1;
		this.q2 = q2;
		this.q3 = q3;
		
		if(needsNormalization) setToNormalized();
	}
	
	public void setToNormalized() {
			// normalization preprocessing
			float inv = (float) (1.0d / MathUtils.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3));
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
	public float len() {
	    return MathUtils.sqrt(q0 * q0 +
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
	      final float norm = len(); 
	
	      return new MRotation(q0 / norm,
	                            q1 / norm,
	                            q2 / norm,
	                            q3 / norm);
	  }

	public void set(SGVec_3f u, SGVec_3f v) throws MathArithmeticException {

		float normProduct = u.mag() * v.mag();
		if (normProduct == 0) {
			throw new MathArithmeticException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_DEFINING_VECTOR);
		}

		float dot = u.dot(v);

		if (dot < ((2.0e-15 - 1.0) * normProduct)) {
			// special case u = -v: we select a PI angle rotation around
			// an arbitrary vector orthogonal to u
			SGVec_3f w = u.getOrthogonal();
			q0 = 0.0f;
			q1 = -w.x;
			q2 = -w.y;
			q3 = -w.z;
		} else {
			// general case: (u, v) defines a plane, we select
			// the shortest possible rotation: axis orthogonal to this plane
			q0 = (float)Math.sqrt(0.5f * (1.0 + dot / normProduct));
			float coeff = 1.0f / (float)(2.0 * q0 * normProduct);
			SGVec_3f q = v.crossCopy(u);
			q1 = coeff * q.x;
			q2 = coeff * q.y;
			q3 = coeff * q.z;
		}

	}

	public void set(SGVec_3f  axis, float angle) throws MathIllegalArgumentException {

		float norm = axis.mag();
		if (norm == 0) {
			throw new MathIllegalArgumentException(LocalizedFormats.ZERO_NORM_FOR_ROTATION_AXIS);
		}

		float halfAngle = -0.5f * angle;
		float coeff = MathUtils.sin(halfAngle) / norm;

		q0 = MathUtils.cos (halfAngle);
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
	private float[][] orthogonalizeMatrix(float[][] m, float threshold)
			throws NotARotationMatrixException {
		float[] m0 = m[0];
		float[] m1 = m[1];
		float[] m2 = m[2];
		float x00 = m0[0];
		float x01 = m0[1];
		float x02 = m0[2];
		float x10 = m1[0];
		float x11 = m1[1];
		float x12 = m1[2];
		float x20 = m2[0];
		float x21 = m2[1];
		float x22 = m2[2];
		float fn = 0;
		float fn1;

		float[][] o = new float[3][3];
		float[] o0 = o[0];
		float[] o1 = o[1];
		float[] o2 = o[2];

		// iterative correction: Xn+1 = Xn - 0.5 * (Xn.Mt.Xn - M)
		int i = 0;
		while (++i < 11) {

			// Mt.Xn
			float mx00 = m0[0] * x00 + m1[0] * x10 + m2[0] * x20;
			float mx10 = m0[1] * x00 + m1[1] * x10 + m2[1] * x20;
			float mx20 = m0[2] * x00 + m1[2] * x10 + m2[2] * x20;
			float mx01 = m0[0] * x01 + m1[0] * x11 + m2[0] * x21;
			float mx11 = m0[1] * x01 + m1[1] * x11 + m2[1] * x21;
			float mx21 = m0[2] * x01 + m1[2] * x11 + m2[2] * x21;
			float mx02 = m0[0] * x02 + m1[0] * x12 + m2[0] * x22;
			float mx12 = m0[1] * x02 + m1[1] * x12 + m2[1] * x22;
			float mx22 = m0[2] * x02 + m1[2] * x12 + m2[2] * x22;

			// Xn+1
			o0[0] = x00 - 0.5f * (x00 * mx00 + x01 * mx10 + x02 * mx20 - m0[0]);
			o0[1] = x01 - 0.5f * (x00 * mx01 + x01 * mx11 + x02 * mx21 - m0[1]);
			o0[2] = x02 - 0.5f * (x00 * mx02 + x01 * mx12 + x02 * mx22 - m0[2]);
			o1[0] = x10 - 0.5f * (x10 * mx00 + x11 * mx10 + x12 * mx20 - m1[0]);
			o1[1] = x11 - 0.5f * (x10 * mx01 + x11 * mx11 + x12 * mx21 - m1[1]);
			o1[2] = x12 - 0.5f * (x10 * mx02 + x11 * mx12 + x12 * mx22 - m1[2]);
			o2[0] = x20 - 0.5f * (x20 * mx00 + x21 * mx10 + x22 * mx20 - m2[0]);
			o2[1] = x21 - 0.5f * (x20 * mx01 + x21 * mx11 + x22 * mx21 - m2[1]);
			o2[2] = x22 - 0.5f * (x20 * mx02 + x21 * mx12 + x22 * mx22 - m2[2]);

			// correction on each elements
			float corr00 = o0[0] - m0[0];
			float corr01 = o0[1] - m0[1];
			float corr02 = o0[2] - m0[2];
			float corr10 = o1[0] - m1[0];
			float corr11 = o1[1] - m1[1];
			float corr12 = o1[2] - m1[2];
			float corr20 = o2[0] - m2[0];
			float corr21 = o2[1] - m2[1];
			float corr22 = o2[2] - m2[2];

			// Frobenius norm of the correction
			fn1 = corr00 * corr00 + corr01 * corr01 + corr02 * corr02 +
					corr10 * corr10 + corr11 * corr11 + corr12 * corr12 +
					corr20 * corr20 + corr21 * corr21 + corr22 * corr22;

			// convergence test
			if (MathUtils.abs(fn1 - fn) <= threshold) {
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
		throw new NotARotationMatrixException(
				LocalizedFormats.UNABLE_TO_ORTHOGONOLIZE_MATRIX,
				i - 1);
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
	public static float distance(MRotation r1,MRotation r2) {
		return r1.applyInverseTo(r2).getAngle();
	}

	
	public boolean equalTo(MRotation m) {
		return (this.q0 == m.getQ0() && this.q1 == m.getQ1() && this.q2 == m.getQ2() && this.q3 == m.getQ3());
	}
	
	public String toString() {
		String result = "axis: " + getAxis().toSGVec3f().toString();
		result += "\n angle : " + MathUtils.toDegrees(getAngle()) + " degrees " ;
		return result;
	}
}
