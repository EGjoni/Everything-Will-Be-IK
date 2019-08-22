package IK.doubleIK;
/**
 * This code is a temporary matrix based implementation of Kaubsch alignment, taken from the cdk project with slight modifications to use Apache
 * commons Matrices and ewbIK Vectors. Credit below. 
 */
/* Copyright (C) 2004-2007  Rajarshi Guha <rajarshi@users.sourceforge.net>
 *                    2014  Egon Willighagen <egonw@users.sf.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import sceneGraph.math.Vec;
import sceneGraph.math.doubleV.EigenDecomp;
import sceneGraph.math.doubleV.EigenDecomp.MaxCountExceededException;
import sceneGraph.math.doubleV.MRotation;
import sceneGraph.math.doubleV.Rot;
import sceneGraph.math.doubleV.SGVec_3d;
import sceneGraph.numerical.Precision;

/**
 * Aligns two structures to minimize the RMSD using the Kabsch algorithm.
 *
 * <p>This class is an implementation of the Kabsch algorithm ({@cdk.cite KAB76}, {@cdk.cite KAB78})
 * and evaluates the optimal rotation matrix (U) to minimize the RMSD between the two structures.
 * Since the algorithm assumes that the number of points are the same in the two structures
 * it is the job of the caller to pass the proper number of atoms from the two structures. Constructors
 * which take whole <code>AtomContainer</code>'s are provided but they should have the same number
 * of atoms.
 * The algorithm allows for the use of atom weightings and by default all points are given a weight of 1.0
 *
 * <p>Example usage can be:
 * <pre>
 * AtomContainer ac1, ac2;
 *
 * try {
 *    KabschAlignment sa = new KabschAlignment(ac1.getAtoms(),ac2.getAtoms());
 *    sa.align();
 *    System.out.println(sa.getRMSD());
 * } catch (CDKException e){}
 * </pre>
 * In many cases, molecules will be aligned based on some common substructure.
 * In this case the center of masses calculated during alignment refer to these
 * substructures rather than the whole molecules. To superimpose the molecules
 * for display, the second molecule must be rotated and translated by calling
 * <code>rotateAtomContainer</code>. However, since this will also translate the
 * second molecule, the first molecule should also be translated to the center of mass
 * of the substructure specified for this molecule. This center of mass can be obtained
 * by a call to <code>getCenterOfMass</code> and then manually translating the coordinates.
 * Thus an example would be
 * <pre>
 * AtomContainer ac1, ac2;  // whole molecules
 * Atom[] a1, a2;           // some subset of atoms from the two molecules
 * KabschAlignment sa;
 *
 * try {
 *    sa = new KabschAlignment(a1,a2);
 *    sa.align();
 * } catch (CDKException e){}
 *
 * SGVec_3d cm1 = sa.getCenterOfMass();
 * for (int i = 0; i &lt; ac1.getAtomCount(); i++) {
 *    Atom a = ac1.getAtomAt(i);
 *    a.setX3d( a.getSGVec_3d().x - cm1.x );
 *    a.setY3d( a.getSGVec_3d().y - cm1.y );
 *    a.setY3d( a.getSGVec_3d().z - cm1.z );
 * }
 * sa.rotateAtomContainer(ac2);
 *
 * // display the two AtomContainer's
 *</pre>
 *
 * @author           Rajarshi Guha
 * @cdk.created      2004-12-11
 * @cdk.dictref      blue-obelisk:alignmentKabsch
 * @cdk.githash
 */
public class KabschAlignment {

    

    private double[][]   U;
    private double       rmsd   = -1.0;
    private SGVec_3d[]    p1, p2, rp;                                                          // rp are the rotated coordinates
    private double[]     wts;
    private int          npoint;
    private SGVec_3d      cm1, cm2;
    private double[]     atwt1, atwt2;
    Rot optimalRotation; 
    
    

    private SGVec_3d getCenterOfMass(SGVec_3d[] p, double[] atwt) {
        double x = 0.;
        double y = 0.;
        double z = 0.;
        double totalmass = 0.;
        for (int i = 0; i < p.length; i++) {
            x += atwt[i] * p[i].x;
            y += atwt[i] * p[i].y;
            z += atwt[i] * p[i].z;
            totalmass += atwt[i];
        }
        return (new SGVec_3d(x / totalmass, y / totalmass, z / totalmass));
    }

    /**
     * Sets up variables for the alignment algorithm.
     *
     * The algorithm allows for atom weighting and the default is 1.0 for all
     * atoms.
     *
     * @param al1 An array of {@link IAtom} objects
     * @param al2 An array of {@link IAtom} objects. This array will have its coordinates rotated
     *            so that the RMDS is minimized to the coordinates of the first array
     * @throws CDKException if the number of Atom's are not the same in the two arrays
     */
    public KabschAlignment(SGVec_3d[] al1, double[] weights1, SGVec_3d[] al2, double[] weights2) {
     
        this.npoint = al1.length;
        this.p1 = al1; //first set of points
        this.p2 = al2; //second set of points 
        this.wts = new double[this.npoint];

        this.atwt1 = weights1;
        this.atwt2 = weights2;

        
        if(atwt1 == null) {
        	atwt1 = new double[npoint];
        	for(int i=0; i<this.npoint; i++) 
        		atwt1[i] = 1.0d;
        }
        if(atwt2 == null) {
        	atwt2 = new double[npoint];
        	for(int i=0; i<this.npoint; i++) 
        		atwt2[i] = 1.0d;
        }        

        for (int i = 0; i < this.npoint; i++)
            this.wts[i] = 1.0;
    }

    /**
     * Perform an alignment.
     *
     * This method aligns to set of atoms which should have been specified
     * prior to this call
     */
    public void align() {

        RealMatrix tmp;

       // in normal Kaubsch alignment, we would translate here, 
       // but since we're rotating around a fixed point (predefined as the bone origin) we skip that step and jump straight to the rotation.
        
     

        // get the R matrix
        double[][] tR = new double[3][3];
        for (int i = 0; i < this.npoint; i++) {
            wts[i] = 1.0;
        }
        for (int i = 0; i < this.npoint; i++) {
            tR[0][0] += p1[i].x * p2[i].x * wts[i];
            tR[0][1] += p1[i].x * p2[i].y * wts[i];
            tR[0][2] += p1[i].x * p2[i].z * wts[i];

            tR[1][0] += p1[i].y * p2[i].x * wts[i];
            tR[1][1] += p1[i].y * p2[i].y * wts[i];
            tR[1][2] += p1[i].y * p2[i].z * wts[i];

            tR[2][0] += p1[i].z * p2[i].x * wts[i];
            tR[2][1] += p1[i].z * p2[i].y * wts[i];
            tR[2][2] += p1[i].z * p2[i].z * wts[i];
        }
        double[][] R = new double[3][3];
        tmp = MatrixUtils.createRealMatrix(tR);
        R = tmp.transpose().getData();

        // now get the RtR (=R'R) matrix
        double[][] RtR = new double[3][3];
        RealMatrix apacheR = MatrixUtils.createRealMatrix(R);
        tmp = tmp.multiply(apacheR);
        RtR = tmp.getData();

        // get eigenvalues of RRt (a's)
        RealMatrix apacheRtR = MatrixUtils.createRealMatrix(RtR);
        double[] resultEigVals = new double[3];
        double[][] resultEigVecs = new double[3][3];
        computeEigensystemFromSymmetricMatrix3(apacheRtR, resultEigVals, resultEigVecs);
        EigenDecomposition ed = new EigenDecomposition(apacheRtR);
        double[] mu = ed.getRealEigenvalues();
        double[][] a = ed.getV().getData();
        
        try {
			EigenDecomp ed2 = new EigenDecomp(apacheRtR.getData());
			double[] mu2 = ed2.getRealEigenValues(); 
			double[][] a2 = ed2.getV();
		} catch (MaxCountExceededException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        

         // make sure that the a3 = a1 x a2
        a[0][2] = (a[1][0] * a[2][1]) - (a[1][1] * a[2][0]);
        a[1][2] = (a[0][1] * a[2][0]) - (a[0][0] * a[2][1]);
        a[2][2] = (a[0][0] * a[1][1]) - (a[0][1] * a[1][0]);

        // lets work out the b vectors
        double[][] b = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    b[i][j] += R[i][k] * a[k][j];
                }
                b[i][j] = b[i][j] / Math.sqrt(mu[j]);
            }
        }

        // normalize and set b3 = b1 x b2
        double norm1 = 0.;
        double norm2 = 0.;
        for (int i = 0; i < 3; i++) {
            norm1 += b[i][0] * b[i][0];
            norm2 += b[i][1] * b[i][1];
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        for (int i = 0; i < 3; i++) {
            b[i][0] = b[i][0] / norm1;
            b[i][1] = b[i][1] / norm2;
        }
        b[0][2] = (b[1][0] * b[2][1]) - (b[1][1] * b[2][0]);
        b[1][2] = (b[0][1] * b[2][0]) - (b[0][0] * b[2][1]);
        b[2][2] = (b[0][0] * b[1][1]) - (b[0][1] * b[1][0]);

        // get the rotation matrix
        double[][] tU = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    tU[i][j] += b[i][k] * a[j][k];
                }
            }
        }
        
        optimalRotation = new Rot(new MRotation(tU, Precision.EPSILON_DOUBLE));
        
        // take the transpose
        U = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                U[i][j] = tU[j][i];
            }
        }

        // now eval the RMS error
        // first, rotate the second set of points and ...
        this.rp = new SGVec_3d[this.npoint];
        for (int i = 0; i < this.npoint; i++) {
            this.rp[i] = new SGVec_3d(U[0][0] * p2[i].x + U[0][1] * p2[i].y + U[0][2] * p2[i].z, U[1][0] * p2[i].x
                    + U[1][1] * p2[i].y + U[1][2] * p2[i].z, U[2][0] * p2[i].x + U[2][1] * p2[i].y + U[2][2] * p2[i].z);
        }

        // ... then eval rms
        double rms = 0.;
        for (int i = 0; i < this.npoint; i++) {
            rms += (p1[i].x - this.rp[i].x) * (p1[i].x - this.rp[i].x) + (p1[i].y - this.rp[i].y)
                    * (p1[i].y - this.rp[i].y) + (p1[i].z - this.rp[i].z) * (p1[i].z - this.rp[i].z);
        }
        this.rmsd = Math.sqrt(rms / this.npoint);
    }
    
   

    public Rot getRotation() {
    	//optimalRotation = new Rot(new MRotation(getRotationMatrix(), Precision.EPSILON_DOUBLE));
    	return optimalRotation;    	
    }
    
    
    /**
     * Returns the RMSD from the alignment.
     *
     * If align() has not been called the return value is -1.0
     *
     * @return The RMSD for this alignment
     * @see #align
     */
    public double getRMSD() {
        return (this.rmsd);
    }

    
    /**
     * Returns the center of mass for the first molecule or fragment used in the calculation.
     *
     * This method is useful when using this class to align the coordinates
     * of two molecules and them displaying them superimposed. Since the center of
     * mass used during the alignment may not be based on the whole molecule (in
     * general common substructures are aligned), when preparing molecules for display
     * the first molecule should be translated to the center of mass. Then displaying the
     * first molecule and the rotated version of the second one will result in superimposed
     * structures.
     *
     * @return A SGVec_3d containing the coordinates of the center of mass
     */
    public SGVec_3d getCenterOfMass() {
        return (this.cm1);
    }

    public static void computeEigensystemFromSymmetricMatrix3(RealMatrix matrix, double[] resultEigenvalues,
            double[][] resultEigenvectors)
        {
          
            // Take from "Mathematics for 3D Game Programming and Computer Graphics, Second Edition" by Eric Lengyel,
            // Listing 14.6 (pages 441-444).

            final double EPSILON = Precision.EPSILON_DOUBLE;
            final int MAX_SWEEPS = 32;

            // Since the Matrix is symmetric, m12=m21, m13=m31, and m23=m32. Therefore we can ignore the values m21, m31,
            // and m32.
            double[][] matrixArr = matrix.getData();
                        
            
            double m11 = matrixArr[0][0];
            double m12 = matrixArr[1][0];
            double m13 = matrixArr[2][0];
            
            double m21= matrixArr[0][1];
            double m22 = matrixArr[1][1];
            double m23 = matrixArr[2][1];
            
            double m31 = matrixArr[0][2];
            double m32 = matrixArr[1][2];
            double m33 = matrixArr[2][2];

            double[][] r = new double[3][3];
            r[0][0] = r[1][1] = r[2][2] = 1d;

            for (int a = 0; a < MAX_SWEEPS; a++)
            {
                // Exit if off-diagonal entries small enough
                if ((Math.abs(m12) < EPSILON) && (Math.abs(m13) < EPSILON) && (Math.abs(m23) < EPSILON))
                    break;

                // Annihilate (1,2) entry
                if (m12 != 0d)
                {
                    double u = (m22 - m11) * 0.5 / m12;
                    double u2 = u * u;
                    double u2p1 = u2 + 1d;
                    double t = (u2p1 != u2) ?
                        ((u < 0d) ? -1d : 1d) * (Math.sqrt(u2p1) - Math.abs(u))
                        : 0.5 / u;
                    double c = 1d / Math.sqrt(t * t + 1d);
                    double s = c * t;

                    m11 -= t * m12;
                    m22 += t * m12;
                    m12 = 0d;

                    double temp = c * m13 - s * m23;
                    m23 = s * m13 + c * m23;
                    m13 = temp;

                    for (int i = 0; i < 3; i++)
                    {
                        temp = c * r[i][0] - s * r[i][1];
                        r[i][1] = s * r[i][0] + c * r[i][1];
                        r[i][0] = temp;
                    }
                }

                // Annihilate (1,3) entry
                if (m13 != 0d)
                {
                    double u = (m33 - m11) * 0.5 / m13;
                    double u2 = u * u;
                    double u2p1 = u2 + 1d;
                    double t = (u2p1 != u2) ?
                        ((u < 0d) ? -1d : 1d) * (Math.sqrt(u2p1) - Math.abs(u))
                        : 0.5 / u;
                    double c = 1d / Math.sqrt(t * t + 1d);
                    double s = c * t;

                    m11 -= t * m13;
                    m33 += t * m13;
                    m13 = 0d;

                    double temp = c * m12 - s * m23;
                    m23 = s * m12 + c * m23;
                    m12 = temp;

                    for (int i = 0; i < 3; i++)
                    {
                        temp = c * r[i][0] - s * r[i][2];
                        r[i][2] = s * r[i][0] + c * r[i][2];
                        r[i][0] = temp;
                    }
                }

                // Annihilate (2,3) entry
                if (m23 != 0d)
                {
                    double u = (m33 - m22) * 0.5 / m23;
                    double u2 = u * u;
                    double u2p1 = u2 + 1d;
                    double t = (u2p1 != u2) ?
                        ((u < 0d) ? -1d : 1d) * (Math.sqrt(u2p1) - Math.abs(u))
                        : 0.5 / u;
                    double c = 1d / Math.sqrt(t * t + 1d);
                    double s = c * t;

                    m22 -= t * m23;
                    m33 += t * m23;
                    m23 = 0d;

                    double temp = c * m12 - s * m13;
                    m13 = s * m12 + c * m13;
                    m12 = temp;

                    for (int i = 0; i < 3; i++)
                    {
                        temp = c * r[i][1] - s * r[i][2];
                        r[i][2] = s * r[i][1] + c * r[i][2];
                        r[i][1] = temp;
                    }
                }
            }

            resultEigenvalues[0] = m11;
            resultEigenvalues[1] = m22;
            resultEigenvalues[2] = m33;

           resultEigenvectors[0][0] = r[0][0]; resultEigenvectors[0][1] = r[1][0];  resultEigenvectors[0][2] = r[2][0];
           resultEigenvectors[1][0] = r[0][1];	resultEigenvectors[1][1] = r[1][1];	resultEigenvectors[1][2] =r[2][1];
           resultEigenvectors[2][0] = r[0][2]; resultEigenvectors[2][1] = r[1][2]; resultEigenvectors[2][2] = r[2][2];
            
        }


}
