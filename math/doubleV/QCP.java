package math.doubleV;

import math.floatV.SGVec_3f;
import math.floatV.Vec3f;
import math.doubleV.Rot;

public class QCP {

	/**
	 * Implementation of the Quaternionf-Based Characteristic Polynomial algorithm
	 * for RMSD and Superposition calculations.
	 * <p>
	 * Usage:
	 * <p>
	 * The input consists of 2 SGVec_3d arrays of equal length. The input
	 * coordinates are not changed.
	 *
	 * <pre>
	 *    SGVec_3d[] x = ...
	 *    SGVec_3d[] y = ...
	 *    SuperPositionQCP qcp = new SuperPositionQCP();
	 *    qcp.set(x, y);
	 * </pre>
	 * <p>
	 * or with weighting factors [0 - 1]]
	 *
	 * <pre>
	 *    double[] weights = ...
	 *    qcp.set(x, y, weights);
	 * </pre>
	 * <p>
	 * For maximum efficiency, create a SuperPositionQCP object once and reuse it.
	 * <p>
	 * A. Calculate rmsd only
	 *
	 * <pre>
	 * double rmsd = qcp.getRmsd();
	 * </pre>
	 * <p>
	 * B. Calculate a 4x4 transformation (rotation and translation) matrix
	 *
	 * <pre>
	 * Matrix4f rottrans = qcp.getTransformationMatrix();
	 * </pre>
	 * <p>
	 * C. Get transformated points (y superposed onto the reference x)
	 *
	 * <pre>
	 * SGVec_3d[] ySuperposed = qcp.getTransformedCoordinates();
	 * </pre>
	 * <p>
	 * Citations:
	 * <p>
	 * Liu P, Agrafiotis DK, & Theobald DL (2011) Reply to comment on: "Fast
	 * determination of the optimal rotation matrix for macromolecular
	 * superpositions." Journal of Computational Chemistry 32(1):185-186.
	 * [http://dx.doi.org/10.1002/jcc.21606]
	 * <p>
	 * Liu P, Agrafiotis DK, & Theobald DL (2010) "Fast determination of the optimal
	 * rotation matrix for macromolecular superpositions." Journal of Computational
	 * Chemistry 31(7):1561-1563. [http://dx.doi.org/10.1002/jcc.21439]
	 * <p>
	 * Douglas L Theobald (2005) "Rapid calculation of RMSDs using a
	 * quaternion-based characteristic polynomial." Acta Crystallogr A
	 * 61(4):478-480. [http://dx.doi.org/10.1107/S0108767305015266 ]
	 * <p>
	 * This is an adoption of the original C code QCProt 1.4 (2012, October 10) to
	 * Java. The original C source code is available from
	 * http://theobald.brandeis.edu/qcp/ and was developed by
	 * <p>
	 * Douglas L. Theobald Department of Biochemistry MS 009 Brandeis University 415
	 * South St Waltham, MA 02453 USA
	 * <p>
	 * dtheobald@brandeis.edu
	 * <p>
	 * Pu Liu Johnson & Johnson Pharmaceutical Research and Development, L.L.C. 665
	 * Stockton Drive Exton, PA 19341 USA
	 * <p>
	 * pliu24@its.jnj.com
	 * <p>
	 *
	 * @author Douglas L. Theobald (original C code)
	 * @author Pu Liu (original C code)
	 * @author Peter Rose (adopted to Java)
	 * @author Aleix Lafita (adopted to Java)
	 * @author Eron Gjoni (adopted to EWB IK)
	 */

	private double evec_prec = (double) 1E-6;
	private double eval_prec = (double) 1E-11;
	private int max_iterations = 5;

	public Vec3d<?>[] target;
	private Vec3d<?>[] moved;

	private double[] weight;
	private double wsum;

	private SGVec_3d targetCenter = new SGVec_3d();
	private SGVec_3d movedCenter = new SGVec_3d();

	private double e0;
	// private Matrix3f rotmat = new Matrix3f();
	// private Matrix4f transformation = new Matrix4f();
	private double rmsd = 0;
	private double Sxy, Sxz, Syx, Syz, Szx, Szy;
	private double SxxpSyy, Szz, mxEigenV, SyzmSzy, SxzmSzx, SxymSyx;
	private double SxxmSyy, SxypSyx, SxzpSzx;
	private double Syy, Sxx, SyzpSzy;
	private boolean rmsdCalculated = false;
	private boolean transformationCalculated = false;
	private boolean innerProductCalculated = false;
	private int length;

	/**
	 * Constructor with option to set the precision values.
	 *
	 * @param centered
	 *            true if the point arrays are centered at the origin (faster),
	 *            false otherwise
	 * @param evec_prec
	 *            required eigenvector precision
	 * @param eval_prec
	 *            required eigenvalue precision
	 */
	public QCP(double evec_prec, double eval_prec) {
		this.evec_prec = evec_prec;
		this.eval_prec = eval_prec;
	}

	/**
	 * Sets the maximum number of iterations QCP should run before giving up. In
	 * most situations QCP converges in 3 or 4 iterations, but in some situations
	 * convergence occurs slowly or not at all, and so an exit condition is used.
	 * The default value is 20. Increase it for more stability.
	 * 
	 * @param max
	 */
	public void setMaxIterations(int max) {
		max_iterations = max;
	}

	/**
	 * Sets the two input coordinate arrays. These input arrays must be of equal
	 * length. Input coordinates are not modified.
	 *
	 * @param x
	 *            3f points of reference coordinate set
	 * @param y
	 *            3f points of coordinate set for superposition
	 */
	private void set(SGVec_3d[] target, SGVec_3d[] moved) {
		this.moved = target;
		this.target = moved;
		rmsdCalculated = false;
		transformationCalculated = false;
		innerProductCalculated = false;
	}

	/**
	 * Sets the two input coordinate arrays and weight array. All input arrays must
	 * be of equal length. Input coordinates are not modified.
	 *
	 * @param fixed
	 *            3f points of reference coordinate set
	 * @param moved
	 *            3f points of coordinate set for superposition
	 * @param weight
	 *            a weight in the inclusive range [0,1] for each point
	 */
	public <V extends Vec3d<?>> void set(V[] moved, V[] target, double[] weight, boolean translate) {
		this.target = target;
		this.moved = moved;
		this.weight = weight;
		rmsdCalculated = false;
		transformationCalculated = false;
		innerProductCalculated = false;

		if (translate) {
			moveToWeightedCenter(this.moved, weight, movedCenter);
			wsum = 0d; // set wsum to 0 so we don't double up.
			moveToWeightedCenter(this.target, weight, targetCenter);
			translate(movedCenter.multCopy(-1d), this.moved);
			translate(targetCenter.multCopy(-1d), this.target);
		} else {
			if (weight != null) {
				for (int i = 0; i < weight.length; i++) {
					wsum += weight[i];
				}
			} else {
				wsum = moved.length;
			}
		}

	}

	/**
	 * Return the RMSD of the superposition of input coordinate set y onto x. Note,
	 * this is the fasted way to calculate an RMSD without actually superposing the
	 * two sets. The calculation is performed "lazy", meaning calculations are only
	 * performed if necessary.
	 *
	 * @return root mean square deviation for superposition of y onto x
	 */
	public double getRmsd() {
		if (!rmsdCalculated) {
			calcRmsd(moved, target);
			rmsdCalculated = true;
		}
		return rmsd;
	}

	/**
	 * Weighted superposition.
	 *
	 * @param fixed
	 * @param moved
	 * @param weight
	 *            array of weigths for each equivalent point position
	 * @return
	 */
	public <V extends Vec3d<?>> Rot weightedSuperpose(V[] moved, V[] target, double[] weight, boolean translate) {
		set(moved, target, weight, translate);
		Rot result = getRotation();
		// transformation.set(rotmat);
		return result;// transformation;
	}
	
	/**
	 * Weighted superposition.
	 *
	 * @param fixed
	 * @param moved
	 * @param weight
	 *            array of weigths for each equivalent point position
	 * @return
	 */
	public <V extends Vec3f<?>> Rot weightedSuperpose( V[] moved, V[] target, float[] weight, boolean translate) {
		double[] weightd = null;
		if(weight != null) weightd = new double[moved.length];
		
		SGVec_3d[] movedd = new SGVec_3d[moved.length];
		SGVec_3d[] targetd = new SGVec_3d[target.length];
		
		for(int i =0; i< moved.length; i++) {
			if(weight != null)
				weightd[i] = weight[i];
			
			movedd[i] = new SGVec_3d((double)moved[i].x, (double)moved[i].y, (double)moved[i].z);
			targetd[i] = new SGVec_3d((double)target[i].x, (double)target[i].y, (double)target[i].z);
		}
		
		
		set(movedd, targetd, weightd, translate);
		Rot result = getRotation();
		//transformation.set(rotmat);
		return result;//transformation;
	}


	private Rot getRotation() {
		Rot result = null;
		if (!transformationCalculated) {
			if (!innerProductCalculated)
				innerProduct( target, moved);			
			result = calcRotation();
			transformationCalculated = true;
		}
		return result;
	}

	/**
	 * Calculates the RMSD value for superposition of y onto x. This requires the
	 * coordinates to be precentered.
	 *
	 * @param x
	 *            3f points of reference coordinate set
	 * @param y
	 *            3f points of coordinate set for superposition
	 */
	private <V extends Vec3d<?>> void calcRmsd(V[] x, V[] y) {
		// QCP doesn't handle alignment of single values, so if we only have one point
		// we just compute regular distance.
		if (x.length == 1) {
			rmsd = x[0].dist(y[0]);
			rmsdCalculated = true;
		} else {
			if (!innerProductCalculated)
				innerProduct(y, x);
			calcRmsd(wsum);
		}
	}

	/**
	 * Calculates the inner product between two coordinate sets x and y (optionally
	 * weighted, if weights set through
	 * {@link #set(SGVec_3d[], SGVec_3d[], double[])}). It also calculates an upper
	 * bound of the most positive root of the key matrix.
	 * http://theobald.brandeis.edu/qcp/qcprot.c
	 *
	 * @param coords1
	 * @param coords2
	 * @return
	 */
	private <V extends Vec3d<?>> void innerProduct(V[] coords1, V[] coords2) {
		double x1, x2, y1, y2, z1, z2;
		double g1 = 0d, g2 = 0d;

		Sxx = 0;
		Sxy = 0;
		Sxz = 0;
		Syx = 0;
		Syy = 0;
		Syz = 0;
		Szx = 0;
		Szy = 0;
		Szz = 0;

		if (weight != null) {
			// wsum = 0;
			for (int i = 0; i < coords1.length; i++) {

				// wsum += weight[i];

				x1 = weight[i] * coords1[i].x;
				y1 = weight[i] * coords1[i].y;
				z1 = weight[i] * coords1[i].z;

				g1 += x1 * coords1[i].x + y1 * coords1[i].y + z1 * coords1[i].z;

				x2 = coords2[i].x;
				y2 = coords2[i].y;
				z2 = coords2[i].z;

				g2 += weight[i] * (x2 * x2 + y2 * y2 + z2 * z2);

				Sxx += (x1 * x2);
				Sxy += (x1 * y2);
				Sxz += (x1 * z2);

				Syx += (y1 * x2);
				Syy += (y1 * y2);
				Syz += (y1 * z2);

				Szx += (z1 * x2);
				Szy += (z1 * y2);
				Szz += (z1 * z2);
			}
		} else {
			for (int i = 0; i < coords1.length; i++) {
				g1 += coords1[i].x * coords1[i].x + coords1[i].y * coords1[i].y + coords1[i].z * coords1[i].z;
				g2 += coords2[i].x * coords2[i].x + coords2[i].y * coords2[i].y + coords2[i].z * coords2[i].z;

				Sxx += coords1[i].x * coords2[i].x;
				Sxy += coords1[i].x * coords2[i].y;
				Sxz += coords1[i].x * coords2[i].z;

				Syx += coords1[i].y * coords2[i].x;
				Syy += coords1[i].y * coords2[i].y;
				Syz += coords1[i].y * coords2[i].z;

				Szx += coords1[i].z * coords2[i].x;
				Szy += coords1[i].z * coords2[i].y;
				Szz += coords1[i].z * coords2[i].z;
			}
			// wsum = coords1.length;
		}
		
		e0 = (g1 + g2) * 0.5d;
		
		SxzpSzx = Sxz + Szx;
		SyzpSzy = Syz + Szy;
		SxypSyx = Sxy + Syx;
		SyzmSzy = Syz - Szy;
		SxzmSzx = Sxz - Szx;
		SxymSyx = Sxy - Syx;
		SxxpSyy = Sxx + Syy;
		SxxmSyy = Sxx - Syy;
		mxEigenV = e0;

		innerProductCalculated = true;
	}

	private void calcRmsd(double len) {

		if (max_iterations > 0) {
			double Sxx2 = Sxx * Sxx;
			double Syy2 = Syy * Syy;
			double Szz2 = Szz * Szz;

			double Sxy2 = Sxy * Sxy;
			double Syz2 = Syz * Syz;
			double Sxz2 = Sxz * Sxz;

			double Syx2 = Syx * Syx;
			double Szy2 = Szy * Szy;
			double Szx2 = Szx * Szx;

			double SyzSzymSyySzz2 = 2.0 * (Syz * Szy - Syy * Szz);
			double Sxx2Syy2Szz2Syz2Szy2 = Syy2 + Szz2 - Sxx2 + Syz2 + Szy2;

			double c2 = -2.0d * (Sxx2 + Syy2 + Szz2 + Sxy2 + Syx2 + Sxz2 + Szx2 + Syz2 + Szy2);
			double c1 = 8.0d * (Sxx * Syz * Szy + Syy * Szx * Sxz + Szz * Sxy * Syx - Sxx * Syy * Szz - Syz * Szx * Sxy
					- Szy * Syx * Sxz);

			double Sxy2Sxz2Syx2Szx2 = Sxy2 + Sxz2 - Syx2 - Szx2;

			double c0 = Sxy2Sxz2Syx2Szx2 * Sxy2Sxz2Syx2Szx2
					+ (Sxx2Syy2Szz2Syz2Szy2 + SyzSzymSyySzz2) * (Sxx2Syy2Szz2Syz2Szy2 - SyzSzymSyySzz2)
					+ (-(SxzpSzx) * (SyzmSzy) + (SxymSyx) * (SxxmSyy - Szz))
							* (-(SxzmSzx) * (SyzpSzy) + (SxymSyx) * (SxxmSyy + Szz))
					+ (-(SxzpSzx) * (SyzpSzy) - (SxypSyx) * (SxxpSyy - Szz))
							* (-(SxzmSzx) * (SyzmSzy) - (SxypSyx) * (SxxpSyy + Szz))
					+ (+(SxypSyx) * (SyzpSzy) + (SxzpSzx) * (SxxmSyy + Szz))
							* (-(SxymSyx) * (SyzmSzy) + (SxzpSzx) * (SxxpSyy + Szz))
					+ (+(SxypSyx) * (SyzmSzy) + (SxzmSzx) * (SxxmSyy - Szz))
							* (-(SxymSyx) * (SyzpSzy) + (SxzmSzx) * (SxxpSyy - Szz));

			int i;
			for (i = 1; i < (max_iterations + 1); ++i) {
				double oldg = mxEigenV;
				double Y = 1d / mxEigenV;
				double Y2 = Y * Y;
				double delta = ((((Y * c0 + c1) * Y + c2) * Y2 + 1) / ((Y * c1 + 2 * c2) * Y2 * Y + 4));
				mxEigenV -= delta;

				if (MathUtils.abs(mxEigenV - oldg) < MathUtils.abs(eval_prec * mxEigenV))
					break;
			}
		}

		rmsd = MathUtils.sqrt(MathUtils.abs(2.0f * (e0 - mxEigenV) / len));

	}

	private Rot calcRotation() {

		// QCP doesn't handle single targets, so if we only have one point and one
		// target, we just rotate by the angular distance between them
		if (moved.length == 1) {
			return new Rot(moved[0], target[0]);
		} else {

			double a11 = SxxpSyy + Szz - mxEigenV;
			double a12 = SyzmSzy;
			double a13 = -SxzmSzx;
			double a14 = SxymSyx;
			double a21 = SyzmSzy;
			double a22 = SxxmSyy - Szz - mxEigenV;
			double a23 = SxypSyx;
			double a24 = SxzpSzx;
			double a31 = a13;
			double a32 = a23;
			double a33 = Syy - Sxx - Szz - mxEigenV;
			double a34 = SyzpSzy;
			double a41 = a14;
			double a42 = a24;
			double a43 = a34;
			double a44 = Szz - SxxpSyy - mxEigenV;
			double a3344_4334 = a33 * a44 - a43 * a34;
			double a3244_4234 = a32 * a44 - a42 * a34;
			double a3243_4233 = a32 * a43 - a42 * a33;
			double a3143_4133 = a31 * a43 - a41 * a33;
			double a3144_4134 = a31 * a44 - a41 * a34;
			double a3142_4132 = a31 * a42 - a41 * a32;
			double q1 = a22 * a3344_4334 - a23 * a3244_4234 + a24 * a3243_4233;
			double q2 = -a21 * a3344_4334 + a23 * a3144_4134 - a24 * a3143_4133;
			double q3 = a21 * a3244_4234 - a22 * a3144_4134 + a24 * a3142_4132;
			double q4 = -a21 * a3243_4233 + a22 * a3143_4133 - a23 * a3142_4132;

			double qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

			/*
			 * The following code tries to calculate another column in the adjoint matrix
			 * when the norm of the current column is too small. Usually this commented
			 * block will never be activated. To be absolutely safe this should be
			 * uncommented, but it is most likely unnecessary.
			 */
			if (qsqr < evec_prec) {
				q1 = a12 * a3344_4334 - a13 * a3244_4234 + a14 * a3243_4233;
				q2 = -a11 * a3344_4334 + a13 * a3144_4134 - a14 * a3143_4133;
				q3 = a11 * a3244_4234 - a12 * a3144_4134 + a14 * a3142_4132;
				q4 = -a11 * a3243_4233 + a12 * a3143_4133 - a13 * a3142_4132;
				qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

				if (qsqr < evec_prec) {
					double a1324_1423 = a13 * a24 - a14 * a23, a1224_1422 = a12 * a24 - a14 * a22;
					double a1223_1322 = a12 * a23 - a13 * a22, a1124_1421 = a11 * a24 - a14 * a21;
					double a1123_1321 = a11 * a23 - a13 * a21, a1122_1221 = a11 * a22 - a12 * a21;

					q1 = a42 * a1324_1423 - a43 * a1224_1422 + a44 * a1223_1322;
					q2 = -a41 * a1324_1423 + a43 * a1124_1421 - a44 * a1123_1321;
					q3 = a41 * a1224_1422 - a42 * a1124_1421 + a44 * a1122_1221;
					q4 = -a41 * a1223_1322 + a42 * a1123_1321 - a43 * a1122_1221;
					qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

					if (qsqr < evec_prec) {
						q1 = a32 * a1324_1423 - a33 * a1224_1422 + a34 * a1223_1322;
						q2 = -a31 * a1324_1423 + a33 * a1124_1421 - a34 * a1123_1321;
						q3 = a31 * a1224_1422 - a32 * a1124_1421 + a34 * a1122_1221;
						q4 = -a31 * a1223_1322 + a32 * a1123_1321 - a33 * a1122_1221;
						qsqr = q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4;

						if (qsqr < evec_prec) {
							/*
							 * if qsqr is still too small, return the identity rotation
							 */
							return new Rot();
						}
					}
				}
			}
			return new Rot(q1, q2, q3, q4, true);
		}
	}

	public double getRmsd(SGVec_3d[] fixed, SGVec_3d[] moved) {
		set(moved, fixed);
		return getRmsd();
	}

	public static <V extends Vec3d<?>> void translate(V trans, V[] x) {
		for (V p : x) {
			p.add(trans);
		}
	}

	public <V extends Vec3d<?>> V moveToWeightedCenter(V[] toCenter, double[] weight, V center) {

		if (weight != null) {
			for (int i = 0; i < toCenter.length; i++) {
				center.mulAdd(toCenter[i], weight[i]);
				wsum += weight[i];
			}

			center.div(wsum);
		} else {
			for (int i = 0; i < toCenter.length; i++) {
				center.add(toCenter[i]);
				wsum++;
			}
			center.div(wsum);
		}

		return center;
	}
	


	public SGVec_3d getTranslation() {
		return targetCenter.subCopy(movedCenter);
	}

}
