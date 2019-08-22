package sceneGraph.math.doubleV;

import java.util.Arrays;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import sceneGraph.numerical.Precision;

public class EigenDecomp {
	boolean isSymmetric;
	private double[] realEigenvalues;
    /** Imaginary part of the realEigenvalues. */
    private double[] imagEigenvalues;
    private double[] main;
    /** Secondary diagonal of the tridiagonal matrix. */
    private double[] secondary;
    private static final byte MAX_ITER = 30;
    private SGVec_3d[] eigenvectors;
    private final double householderVectors[][] = new double[3][];
    
	public EigenDecomp(final double[][] matrix) throws MaxCountExceededException {
		final double symTol = 10 * 3 * 3 * Precision.EPSILON_DOUBLE;
		main = new double[3];
		secondary = new double[2];
		householderVectors[0] = Arrays.copyOf(matrix[0], matrix[0].length);
		householderVectors[1] = Arrays.copyOf(matrix[1], matrix[1].length);
		householderVectors[2] = Arrays.copyOf(matrix[2], matrix[2].length);
		isSymmetric = true;
		double[][] getTransformedToTriDiagonal = getTransformedToTriDiagonal();
		findEigenVectors(getTransformedToTriDiagonal);		
	}

	public double[] getRealEigenValues() {
        return realEigenvalues.clone();
    }
	
	 public double[][] getV() {
	        
         final int m = eigenvectors.length;
         double[][] v = new double[3][];
         for (int k = 0; k < m; ++k) {
        	 double[] eigenK = {eigenvectors[k].x, eigenvectors[k].y, eigenvectors[k].z}; 
             v[k] = eigenK;
         }
     
     // return the cached matrix
     return v;
 }
	
    /**
     * Find eigenvalues and eigenvectors (Dubrulle et al., 1971)
     *
     * @param householderMatrix Householder matrix of the transformation
     * to tridiagonal form.
     * @throws MaxCountExceededException 
     */
    private void findEigenVectors(final double[][] householderMatrix) throws MaxCountExceededException {
        final double[][]z = householderMatrix.clone();
        final int n = 3;
        realEigenvalues = new double[n];
        imagEigenvalues = new double[n];
        final double[] e = new double[n];
        for (int i = 0; i < n - 1; i++) {
            realEigenvalues[i] = main[i];
            e[i] = secondary[i];
        }
        realEigenvalues[n - 1] = main[n - 1];
        e[n - 1] = 0;

        // Determine the largest main and secondary value in absolute term.
        double maxAbsoluteValue = 0;
        for (int i = 0; i < n; i++) {
            if (MathUtils.abs(realEigenvalues[i]) > maxAbsoluteValue) {
                maxAbsoluteValue = MathUtils.abs(realEigenvalues[i]);
            }
            if (MathUtils.abs(e[i]) > maxAbsoluteValue) {
                maxAbsoluteValue = MathUtils.abs(e[i]);
            }
        }
        // Make null any main and secondary value too small to be significant
        if (maxAbsoluteValue != 0) {
            for (int i=0; i < n; i++) {
                if (MathUtils.abs(realEigenvalues[i]) <= Precision.EPSILON_DOUBLE * maxAbsoluteValue) {
                    realEigenvalues[i] = 0;
                }
                if (MathUtils.abs(e[i]) <= Precision.EPSILON_DOUBLE * maxAbsoluteValue) {
                    e[i]=0;
                }
            }
        }

        for (int j = 0; j < n; j++) {
            int its = 0;
            int m;
            do {
                for (m = j; m < n - 1; m++) {
                    double delta = MathUtils.abs(realEigenvalues[m]) +
                        MathUtils.abs(realEigenvalues[m + 1]);
                    if (MathUtils.abs(e[m]) + delta == delta) {
                        break;
                    }
                }
                if (m != j) {
                    if (its == MAX_ITER) {
                        throw new MaxCountExceededException();
                    }
                    its++;
                    double q = (realEigenvalues[j + 1] - realEigenvalues[j]) / (2 * e[j]);
                    double t = MathUtils.sqrt(1 + q * q);
                    if (q < 0.0) {
                        q = realEigenvalues[m] - realEigenvalues[j] + e[j] / (q - t);
                    } else {
                        q = realEigenvalues[m] - realEigenvalues[j] + e[j] / (q + t);
                    }
                    double u = 0.0;
                    double s = 1.0;
                    double c = 1.0;
                    int i;
                    for (i = m - 1; i >= j; i--) {
                        double p = s * e[i];
                        double h = c * e[i];
                        if (MathUtils.abs(p) >= MathUtils.abs(q)) {
                            c = q / p;
                            t = MathUtils.sqrt(c * c + 1.0);
                            e[i + 1] = p * t;
                            s = 1.0 / t;
                            c *= s;
                        } else {
                            s = p / q;
                            t = MathUtils.sqrt(s * s + 1.0);
                            e[i + 1] = q * t;
                            c = 1.0 / t;
                            s *= c;
                        }
                        if (e[i + 1] == 0.0) {
                            realEigenvalues[i + 1] -= u;
                            e[m] = 0.0;
                            break;
                        }
                        q = realEigenvalues[i + 1] - u;
                        t = (realEigenvalues[i] - q) * s + 2.0 * c * h;
                        u = s * t;
                        realEigenvalues[i + 1] = q + u;
                        q = c * t - h;
                        for (int ia = 0; ia < n; ia++) {
                            p = z[ia][i + 1];
                            z[ia][i + 1] = s * z[ia][i] + c * p;
                            z[ia][i] = c * z[ia][i] - s * p;
                        }
                    }
                    if (t == 0.0 && i >= j) {
                        continue;
                    }
                    realEigenvalues[j] -= u;
                    e[j] = q;
                    e[m] = 0.0;
                }
            } while (m != j);
        }

        //Sort the eigen values (and vectors) in increase order
        for (int i = 0; i < n; i++) {
            int k = i;
            double p = realEigenvalues[i];
            for (int j = i + 1; j < n; j++) {
                if (realEigenvalues[j] > p) {
                    k = j;
                    p = realEigenvalues[j];
                }
            }
            if (k != i) {
                realEigenvalues[k] = realEigenvalues[i];
                realEigenvalues[i] = p;
                for (int j = 0; j < n; j++) {
                    p = z[j][i];
                    z[j][i] = z[j][k];
                    z[j][k] = p;
                }
            }
        }

        // Determine the largest eigen value in absolute term.
        maxAbsoluteValue = 0;
        for (int i = 0; i < n; i++) {
            if (MathUtils.abs(realEigenvalues[i]) > maxAbsoluteValue) {
                maxAbsoluteValue=MathUtils.abs(realEigenvalues[i]);
            }
        }
        // Make null any eigen value too small to be significant
        if (maxAbsoluteValue != 0.0) {
            for (int i=0; i < n; i++) {
                if (MathUtils.abs(realEigenvalues[i]) < Precision.EPSILON_DOUBLE * maxAbsoluteValue) {
                    realEigenvalues[i] = 0;
                }
            }
        }
        eigenvectors = new SGVec_3d[n];
        final double[] tmp = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tmp[j] = z[j][i];
            }
            eigenvectors[i] = new SGVec_3d(tmp);
        }
    }


	


	private double[][] getTransformedToTriDiagonal() {		

		final int m = householderVectors.length;
		final double[] z = new double[m];
		for (int k = 0; k < m - 1; k++) {

			//zero-out a row and a column simultaneously
			final double[] hK = householderVectors[k];
			main[k] = hK[k];
			double xNormSqr = 0;
			for (int j = k + 1; j < m; ++j) {
				final double c = hK[j];
				xNormSqr += c * c;
			}
			final double a = (hK[k + 1] > 0) ? -MathUtils.sqrt(xNormSqr) : MathUtils.sqrt(xNormSqr);
			secondary[k] = a;
			if (a != 0.0) {
				// apply Householder transform from left and right simultaneously

				hK[k + 1] -= a;
				final double beta = -1 / (a * hK[k + 1]);

				// compute a = beta A v, where v is the Householder vector
				// this loop is written in such a way
				//   1) only the upper triangular part of the matrix is accessed
				//   2) access is cache-friendly for a matrix stored in rows
				Arrays.fill(z, k + 1, m, 0);
				for (int i = k + 1; i < m; ++i) {
					final double[] hI = householderVectors[i];
					final double hKI = hK[i];
					double zI = hI[i] * hKI;
					for (int j = i + 1; j < m; ++j) {
						final double hIJ = hI[j];
						zI   += hIJ * hK[j];
						z[j] += hIJ * hKI;
					}
					z[i] = beta * (z[i] + zI);
				}

				// compute gamma = beta vT z / 2
						double gamma = 0;
				for (int i = k + 1; i < m; ++i) {
					gamma += z[i] * hK[i];
				}
				gamma *= beta / 2;

				// compute z = z - gamma v
				for (int i = k + 1; i < m; ++i) {
					z[i] -= gamma * hK[i];
				}

				// update matrix: A = A - v zT - z vT
				// only the upper triangular part of the matrix is updated
				for (int i = k + 1; i < m; ++i) {
					final double[] hI = householderVectors[i];
					for (int j = i; j < m; ++j) {
						hI[j] -= hK[i] * z[j] + z[i] * hK[j];
					}
				}
			}
		}
		main[m - 1] = householderVectors[m - 1][m - 1];
		return getQ();
	}
	
	
	
	 public double[][] getQ() {
		 double[][] QT = getQT(); 
		double[][] Q = new double[3][3];
	        for (int i = 0; i < 3; i++) {
	            for (int j = 0; j < 3; j++) {
	                Q[i][j] = QT[j][i];
	            }
	        }
	        return Q;
	    }

	
	public double[][] getQT() {
        
            final int m = householderVectors.length;
            double[][] qta = new double[m][m];

            // build up first part of the matrix by applying Householder transforms
            for (int k = m - 1; k >= 1; --k) {
                final double[] hK = householderVectors[k - 1];
                qta[k][k] = 1;
                if (hK[k] != 0.0) {
                    final double inv = 1.0 / (secondary[k - 1] * hK[k]);
                    double beta = 1.0 / secondary[k - 1];
                    qta[k][k] = 1 + beta * hK[k];
                    for (int i = k + 1; i < m; ++i) {
                        qta[k][i] = beta * hK[i];
                    }
                    for (int j = k + 1; j < m; ++j) {
                        beta = 0;
                        for (int i = k + 1; i < m; ++i) {
                            beta += qta[j][i] * hK[i];
                        }
                        beta *= inv;
                        qta[j][k] = beta * hK[k];
                        for (int i = k + 1; i < m; ++i) {
                            qta[j][i] += beta * hK[i];
                        }
                    }
                }
            }
            qta[0][0] = 1;
            
        

        // return the cached matrix
        return qta;
    }

	
	
	public class MaxCountExceededException extends Exception {}
}
