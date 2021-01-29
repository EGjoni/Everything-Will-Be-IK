package math.pga;

public class PGA3D {
		// basis names
		public static String[] basis = { "1","e0","e1","e2","e3","e01","e02","e03","e12","e31","e23","e021","e013","e032","e123","e0123" };

		protected float[] _mVec = new float[16];

		public PGA3D() {
			
		}
		
		public PGA3D(float f , int idx)
		{
			_mVec[idx] = f;
		}
		
		/** returns the raw array underlying the representation of this PGA3D object.
		 */
		public float[] data() {
			return _mVec;
		}

		public float get(int idx) {
			return _mVec[idx];
		}
		
		public float set(int idx) {
			return _mVec[idx];
		}

		/** 
		 * returns a copy of the given multivector  with the order of its basis blades reversed.  
		 */
		public static PGA3D reverse (PGA3D a)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			res[0]=aa[0];
			res[1]=aa[1];
			res[2]=aa[2];
			res[3]=aa[3];
			res[4]=aa[4];
			res[5]=-aa[5];
			res[6]=-aa[6];
			res[7]=-aa[7];
			res[8]=-aa[8];
			res[9]=-aa[9];
			res[10]=-aa[10];
			res[11]=-aa[11];
			res[12]=-aa[12];
			res[13]=-aa[13];
			res[14]=-aa[14];
			res[15]=aa[15];
			return t;
		}

		/// <summary>
		/// PGA3D.Dual : res = !a
		/// Poincare duality operator.
		/// </summary>
		/** 
		 * returns the dual of the given multivector.    
		 */
		public static PGA3D dual(PGA3D a)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			res[0]=aa[15];
			res[1]=aa[14];
			res[2]=aa[13];
			res[3]=aa[12];
			res[4]=aa[11];
			res[5]=aa[10];
			res[6]=aa[9];
			res[7]=aa[8];
			res[8]=aa[7];
			res[9]=aa[6];
			res[10]=aa[5];
			res[11]=aa[4];
			res[12]=aa[3];
			res[13]=aa[2];
			res[14]=aa[1];
			res[15]=aa[0];
			return t;
		}

		/** 
		 * @return a copy of the Clifford Conjugate of this multivector. 
		 */
		public  PGA3D Conjugate ()
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			res[0]=_mVec[0];
			res[1]=-_mVec[1];
			res[2]=-_mVec[2];
			res[3]=-_mVec[3];
			res[4]=-_mVec[4];
			res[5]=-_mVec[5];
			res[6]=-_mVec[6];
			res[7]=-_mVec[7];
			res[8]=-_mVec[8];
			res[9]=-_mVec[9];
			res[10]=-_mVec[10];
			res[11]=_mVec[11];
			res[12]=_mVec[12];
			res[13]=_mVec[13];
			res[14]=_mVec[14];
			res[15]=_mVec[15];
			return t;
		}

		/**
		 * @return a copy of the involution of this multivector.  
		 */
		public  PGA3D Involute ()
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			res[0]=_mVec[0];
			res[1]=-_mVec[1];
			res[2]=-_mVec[2];
			res[3]=-_mVec[3];
			res[4]=-_mVec[4];
			res[5]=_mVec[5];
			res[6]=_mVec[6];
			res[7]=_mVec[7];
			res[8]=_mVec[8];
			res[9]=_mVec[9];
			res[10]=_mVec[10];
			res[11]=-_mVec[11];
			res[12]=-_mVec[12];
			res[13]=-_mVec[13];
			res[14]=-_mVec[14];
			res[15]=_mVec[15];
			return t;
		}

		/**
		 * Returns a multivector equal  to the geometric product of a and b.
		 */
		public static PGA3D mult(PGA3D a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			float[] bb = b._mVec;
			res[0]=bb[0]*aa[0]+bb[2]*aa[2]+bb[3]*aa[3]+bb[4]*aa[4]-bb[8]*aa[8]-bb[9]*aa[9]-bb[10]*aa[10]-bb[14]*aa[14];
			res[1]=bb[1]*aa[0]+bb[0]*aa[1]-bb[5]*aa[2]-bb[6]*aa[3]-bb[7]*aa[4]+bb[2]*aa[5]+bb[3]*aa[6]+bb[4]*aa[7]+bb[11]*aa[8]+bb[12]*aa[9]+bb[13]*aa[10]+bb[8]*aa[11]+bb[9]*aa[12]+bb[10]*aa[13]+bb[15]*aa[14]-bb[14]*aa[15];
			res[2]=bb[2]*aa[0]+bb[0]*aa[2]-bb[8]*aa[3]+bb[9]*aa[4]+bb[3]*aa[8]-bb[4]*aa[9]-bb[14]*aa[10]-bb[10]*aa[14];
			res[3]=bb[3]*aa[0]+bb[8]*aa[2]+bb[0]*aa[3]-bb[10]*aa[4]-bb[2]*aa[8]-bb[14]*aa[9]+bb[4]*aa[10]-bb[9]*aa[14];
			res[4]=bb[4]*aa[0]-bb[9]*aa[2]+bb[10]*aa[3]+bb[0]*aa[4]-bb[14]*aa[8]+bb[2]*aa[9]-bb[3]*aa[10]-bb[8]*aa[14];
			res[5]=bb[5]*aa[0]+bb[2]*aa[1]-bb[1]*aa[2]-bb[11]*aa[3]+bb[12]*aa[4]+bb[0]*aa[5]-bb[8]*aa[6]+bb[9]*aa[7]+bb[6]*aa[8]-bb[7]*aa[9]-bb[15]*aa[10]-bb[3]*aa[11]+bb[4]*aa[12]+bb[14]*aa[13]-bb[13]*aa[14]-bb[10]*aa[15];
			res[6]=bb[6]*aa[0]+bb[3]*aa[1]+bb[11]*aa[2]-bb[1]*aa[3]-bb[13]*aa[4]+bb[8]*aa[5]+bb[0]*aa[6]-bb[10]*aa[7]-bb[5]*aa[8]-bb[15]*aa[9]+bb[7]*aa[10]+bb[2]*aa[11]+bb[14]*aa[12]-bb[4]*aa[13]-bb[12]*aa[14]-bb[9]*aa[15];
			res[7]=bb[7]*aa[0]+bb[4]*aa[1]-bb[12]*aa[2]+bb[13]*aa[3]-bb[1]*aa[4]-bb[9]*aa[5]+bb[10]*aa[6]+bb[0]*aa[7]-bb[15]*aa[8]+bb[5]*aa[9]-bb[6]*aa[10]+bb[14]*aa[11]-bb[2]*aa[12]+bb[3]*aa[13]-bb[11]*aa[14]-bb[8]*aa[15];
			res[8]=bb[8]*aa[0]+bb[3]*aa[2]-bb[2]*aa[3]+bb[14]*aa[4]+bb[0]*aa[8]+bb[10]*aa[9]-bb[9]*aa[10]+bb[4]*aa[14];
			res[9]=bb[9]*aa[0]-bb[4]*aa[2]+bb[14]*aa[3]+bb[2]*aa[4]-bb[10]*aa[8]+bb[0]*aa[9]+bb[8]*aa[10]+bb[3]*aa[14];
			res[10]=bb[10]*aa[0]+bb[14]*aa[2]+bb[4]*aa[3]-bb[3]*aa[4]+bb[9]*aa[8]-bb[8]*aa[9]+bb[0]*aa[10]+bb[2]*aa[14];
			res[11]=bb[11]*aa[0]-bb[8]*aa[1]+bb[6]*aa[2]-bb[5]*aa[3]+bb[15]*aa[4]-bb[3]*aa[5]+bb[2]*aa[6]-bb[14]*aa[7]-bb[1]*aa[8]+bb[13]*aa[9]-bb[12]*aa[10]+bb[0]*aa[11]+bb[10]*aa[12]-bb[9]*aa[13]+bb[7]*aa[14]-bb[4]*aa[15];
			res[12]=bb[12]*aa[0]-bb[9]*aa[1]-bb[7]*aa[2]+bb[15]*aa[3]+bb[5]*aa[4]+bb[4]*aa[5]-bb[14]*aa[6]-bb[2]*aa[7]-bb[13]*aa[8]-bb[1]*aa[9]+bb[11]*aa[10]-bb[10]*aa[11]+bb[0]*aa[12]+bb[8]*aa[13]+bb[6]*aa[14]-bb[3]*aa[15];
			res[13]=bb[13]*aa[0]-bb[10]*aa[1]+bb[15]*aa[2]+bb[7]*aa[3]-bb[6]*aa[4]-bb[14]*aa[5]-bb[4]*aa[6]+bb[3]*aa[7]+bb[12]*aa[8]-bb[11]*aa[9]-bb[1]*aa[10]+bb[9]*aa[11]-bb[8]*aa[12]+bb[0]*aa[13]+bb[5]*aa[14]-bb[2]*aa[15];
			res[14]=bb[14]*aa[0]+bb[10]*aa[2]+bb[9]*aa[3]+bb[8]*aa[4]+bb[4]*aa[8]+bb[3]*aa[9]+bb[2]*aa[10]+bb[0]*aa[14];
			res[15]=bb[15]*aa[0]+bb[14]*aa[1]+bb[13]*aa[2]+bb[12]*aa[3]+bb[11]*aa[4]+bb[10]*aa[5]+bb[9]*aa[6]+bb[8]*aa[7]+bb[7]*aa[8]+bb[6]*aa[9]+bb[5]*aa[10]-bb[4]*aa[11]-bb[3]*aa[12]-bb[2]*aa[13]-bb[1]*aa[14]+bb[0]*aa[15];
			return t;
		}

		/**
		 * 
		 * @param a 
		 * @param b
		 * @return the wedge product of the given multivectors. 
		 */
		public static PGA3D meet(PGA3D a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			float[] bb = b._mVec;
			res[0]=bb[0]*aa[0];
			res[1]=bb[1]*aa[0]+bb[0]*aa[1];
			res[2]=bb[2]*aa[0]+bb[0]*aa[2];
			res[3]=bb[3]*aa[0]+bb[0]*aa[3];
			res[4]=bb[4]*aa[0]+bb[0]*aa[4];
			res[5]=bb[5]*aa[0]+bb[2]*aa[1]-bb[1]*aa[2]+bb[0]*aa[5];
			res[6]=bb[6]*aa[0]+bb[3]*aa[1]-bb[1]*aa[3]+bb[0]*aa[6];
			res[7]=bb[7]*aa[0]+bb[4]*aa[1]-bb[1]*aa[4]+bb[0]*aa[7];
			res[8]=bb[8]*aa[0]+bb[3]*aa[2]-bb[2]*aa[3]+bb[0]*aa[8];
			res[9]=bb[9]*aa[0]-bb[4]*aa[2]+bb[2]*aa[4]+bb[0]*aa[9];
			res[10]=bb[10]*aa[0]+bb[4]*aa[3]-bb[3]*aa[4]+bb[0]*aa[10];
			res[11]=bb[11]*aa[0]-bb[8]*aa[1]+bb[6]*aa[2]-bb[5]*aa[3]-bb[3]*aa[5]+bb[2]*aa[6]-bb[1]*aa[8]+bb[0]*aa[11];
			res[12]=bb[12]*aa[0]-bb[9]*aa[1]-bb[7]*aa[2]+bb[5]*aa[4]+bb[4]*aa[5]-bb[2]*aa[7]-bb[1]*aa[9]+bb[0]*aa[12];
			res[13]=bb[13]*aa[0]-bb[10]*aa[1]+bb[7]*aa[3]-bb[6]*aa[4]-bb[4]*aa[6]+bb[3]*aa[7]-bb[1]*aa[10]+bb[0]*aa[13];
			res[14]=bb[14]*aa[0]+bb[10]*aa[2]+bb[9]*aa[3]+bb[8]*aa[4]+bb[4]*aa[8]+bb[3]*aa[9]+bb[2]*aa[10]+bb[0]*aa[14];
			res[15]=bb[15]*aa[0]+bb[14]*aa[1]+bb[13]*aa[2]+bb[12]*aa[3]+bb[11]*aa[4]+bb[10]*aa[5]+bb[9]*aa[6]+bb[8]*aa[7]+bb[7]*aa[8]+bb[6]*aa[9]+bb[5]*aa[10]-bb[4]*aa[11]-bb[3]*aa[12]-bb[2]*aa[13]-bb[1]*aa[14]+bb[0]*aa[15];
			return t;
		}

		/**
		 * @param a
		 * @param b
		 * @return the join product of the given of the given multivectors (two points join to a line, a line and a point join into a plane, etc) 
		 */
		public static PGA3D join(PGA3D a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			float[] bb = b._mVec;
			res[15]=1*(aa[15]*bb[15]);
			res[14]=-1*(aa[14]*-1*bb[15]+aa[15]*bb[14]*-1);
			res[13]=-1*(aa[13]*-1*bb[15]+aa[15]*bb[13]*-1);
			res[12]=-1*(aa[12]*-1*bb[15]+aa[15]*bb[12]*-1);
			res[11]=-1*(aa[11]*-1*bb[15]+aa[15]*bb[11]*-1);
			res[10]=1*(aa[10]*bb[15]+aa[13]*-1*bb[14]*-1-aa[14]*-1*bb[13]*-1+aa[15]*bb[10]);
			res[9]=1*(aa[9]*bb[15]+aa[12]*-1*bb[14]*-1-aa[14]*-1*bb[12]*-1+aa[15]*bb[9]);
			res[8]=1*(aa[8]*bb[15]+aa[11]*-1*bb[14]*-1-aa[14]*-1*bb[11]*-1+aa[15]*bb[8]);
			res[7]=1*(aa[7]*bb[15]+aa[12]*-1*bb[13]*-1-aa[13]*-1*bb[12]*-1+aa[15]*bb[7]);
			res[6]=1*(aa[6]*bb[15]-aa[11]*-1*bb[13]*-1+aa[13]*-1*bb[11]*-1+aa[15]*bb[6]);
			res[5]=1*(aa[5]*bb[15]+aa[11]*-1*bb[12]*-1-aa[12]*-1*bb[11]*-1+aa[15]*bb[5]);
			res[4]=1*(aa[4]*bb[15]-aa[7]*bb[14]*-1+aa[9]*bb[13]*-1-aa[10]*bb[12]*-1-aa[12]*-1*bb[10]+aa[13]*-1*bb[9]-aa[14]*-1*bb[7]+aa[15]*bb[4]);
			res[3]=1*(aa[3]*bb[15]-aa[6]*bb[14]*-1-aa[8]*bb[13]*-1+aa[10]*bb[11]*-1+aa[11]*-1*bb[10]-aa[13]*-1*bb[8]-aa[14]*-1*bb[6]+aa[15]*bb[3]);
			res[2]=1*(aa[2]*bb[15]-aa[5]*bb[14]*-1+aa[8]*bb[12]*-1-aa[9]*bb[11]*-1-aa[11]*-1*bb[9]+aa[12]*-1*bb[8]-aa[14]*-1*bb[5]+aa[15]*bb[2]);
			res[1]=1*(aa[1]*bb[15]+aa[5]*bb[13]*-1+aa[6]*bb[12]*-1+aa[7]*bb[11]*-1+aa[11]*-1*bb[7]+aa[12]*-1*bb[6]+aa[13]*-1*bb[5]+aa[15]*bb[1]);
			res[0]=1*(aa[0]*bb[15]+aa[1]*bb[14]*-1+aa[2]*bb[13]*-1+aa[3]*bb[12]*-1+aa[4]*bb[11]*-1+aa[5]*bb[10]+aa[6]*bb[9]+aa[7]*bb[8]+aa[8]*bb[7]+aa[9]*bb[6]+aa[10]*bb[5]-aa[11]*-1*bb[4]-aa[12]*-1*bb[3]-aa[13]*-1*bb[2]-aa[14]*-1*bb[1]+aa[15]*bb[0]);
			return t;
		}

		/**
		 * 
		 * @param a
		 * @param b
		 * @return the inner product of the given multivectors
		 */
		public static PGA3D dot (PGA3D a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			float[] bb = b._mVec;
			res[0]=bb[0]*aa[0]+bb[2]*aa[2]+bb[3]*aa[3]+bb[4]*aa[4]-bb[8]*aa[8]-bb[9]*aa[9]-bb[10]*aa[10]-bb[14]*aa[14];
			res[1]=bb[1]*aa[0]+bb[0]*aa[1]-bb[5]*aa[2]-bb[6]*aa[3]-bb[7]*aa[4]+bb[2]*aa[5]+bb[3]*aa[6]+bb[4]*aa[7]+bb[11]*aa[8]+bb[12]*aa[9]+bb[13]*aa[10]+bb[8]*aa[11]+bb[9]*aa[12]+bb[10]*aa[13]+bb[15]*aa[14]-bb[14]*aa[15];
			res[2]=bb[2]*aa[0]+bb[0]*aa[2]-bb[8]*aa[3]+bb[9]*aa[4]+bb[3]*aa[8]-bb[4]*aa[9]-bb[14]*aa[10]-bb[10]*aa[14];
			res[3]=bb[3]*aa[0]+bb[8]*aa[2]+bb[0]*aa[3]-bb[10]*aa[4]-bb[2]*aa[8]-bb[14]*aa[9]+bb[4]*aa[10]-bb[9]*aa[14];
			res[4]=bb[4]*aa[0]-bb[9]*aa[2]+bb[10]*aa[3]+bb[0]*aa[4]-bb[14]*aa[8]+bb[2]*aa[9]-bb[3]*aa[10]-bb[8]*aa[14];
			res[5]=bb[5]*aa[0]-bb[11]*aa[3]+bb[12]*aa[4]+bb[0]*aa[5]-bb[15]*aa[10]-bb[3]*aa[11]+bb[4]*aa[12]-bb[10]*aa[15];
			res[6]=bb[6]*aa[0]+bb[11]*aa[2]-bb[13]*aa[4]+bb[0]*aa[6]-bb[15]*aa[9]+bb[2]*aa[11]-bb[4]*aa[13]-bb[9]*aa[15];
			res[7]=bb[7]*aa[0]-bb[12]*aa[2]+bb[13]*aa[3]+bb[0]*aa[7]-bb[15]*aa[8]-bb[2]*aa[12]+bb[3]*aa[13]-bb[8]*aa[15];
			res[8]=bb[8]*aa[0]+bb[14]*aa[4]+bb[0]*aa[8]+bb[4]*aa[14];
			res[9]=bb[9]*aa[0]+bb[14]*aa[3]+bb[0]*aa[9]+bb[3]*aa[14];
			res[10]=bb[10]*aa[0]+bb[14]*aa[2]+bb[0]*aa[10]+bb[2]*aa[14];
			res[11]=bb[11]*aa[0]+bb[15]*aa[4]+bb[0]*aa[11]-bb[4]*aa[15];
			res[12]=bb[12]*aa[0]+bb[15]*aa[3]+bb[0]*aa[12]-bb[3]*aa[15];
			res[13]=bb[13]*aa[0]+bb[15]*aa[2]+bb[0]*aa[13]-bb[2]*aa[15];
			res[14]=bb[14]*aa[0]+bb[0]*aa[14];
			res[15]=bb[15]*aa[0]+bb[0]*aa[15];
			return t;
		}

		/** 
		 * @param a
		 * @param b
		 * @return a + b
		 */
		public static PGA3D add(PGA3D a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			float[] bb = b._mVec;
			res[0] = aa[0]+bb[0];
			res[1] = aa[1]+bb[1];
			res[2] = aa[2]+bb[2];
			res[3] = aa[3]+bb[3];
			res[4] = aa[4]+bb[4];
			res[5] = aa[5]+bb[5];
			res[6] = aa[6]+bb[6];
			res[7] = aa[7]+bb[7];
			res[8] = aa[8]+bb[8];
			res[9] = aa[9]+bb[9];
			res[10] = aa[10]+bb[10];
			res[11] = aa[11]+bb[11];
			res[12] = aa[12]+bb[12];
			res[13] = aa[13]+bb[13];
			res[14] = aa[14]+bb[14];
			res[15] = aa[15]+bb[15];
			return t;
		}

		/** 
		 * @param a
		 * @param b
		 * @return a - b
		 */
		public static PGA3D sub(PGA3D a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			float[] bb = b._mVec;
			res[0] = aa[0]-bb[0];
			res[1] = aa[1]-bb[1];
			res[2] = aa[2]-bb[2];
			res[3] = aa[3]-bb[3];
			res[4] = aa[4]-bb[4];
			res[5] = aa[5]-bb[5];
			res[6] = aa[6]-bb[6];
			res[7] = aa[7]-bb[7];
			res[8] = aa[8]-bb[8];
			res[9] = aa[9]-bb[9];
			res[10] = aa[10]-bb[10];
			res[11] = aa[11]-bb[11];
			res[12] = aa[12]-bb[12];
			res[13] = aa[13]-bb[13];
			res[14] = aa[14]-bb[14];
			res[15] = aa[15]-bb[15];
			return t;
		}

		/**
		 * 
		 * @param a
		 * @param b
		 * @return scalar a * multivector b. 
		 */
		public static PGA3D mult(float a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] bb = b._mVec;
			res[0] = a*bb[0];
			res[1] = a*bb[1];
			res[2] = a*bb[2];
			res[3] = a*bb[3];
			res[4] = a*bb[4];
			res[5] = a*bb[5];
			res[6] = a*bb[6];
			res[7] = a*bb[7];
			res[8] = a*bb[8];
			res[9] = a*bb[9];
			res[10] = a*bb[10];
			res[11] = a*bb[11];
			res[12] = a*bb[12];
			res[13] = a*bb[13];
			res[14] = a*bb[14];
			res[15] = a*bb[15];
			return t;
		}

		/**
		 * @param a
		 * @param b
		 * @return scalar b * multivector a
		 */
		public static PGA3D mult(PGA3D a, float b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			res[0] = aa[0]*b;
			res[1] = aa[1]*b;
			res[2] = aa[2]*b;
			res[3] = aa[3]*b;
			res[4] = aa[4]*b;
			res[5] = aa[5]*b;
			res[6] = aa[6]*b;
			res[7] = aa[7]*b;
			res[8] = aa[8]*b;
			res[9] = aa[9]*b;
			res[10] = aa[10]*b;
			res[11] = aa[11]*b;
			res[12] = aa[12]*b;
			res[13] = aa[13]*b;
			res[14] = aa[14]*b;
			res[15] = aa[15]*b;
			return t;
		}

		/**
		 * @param a
		 * @param b
		 * @return scalar a + multivector b
		 */
		public static PGA3D add (float a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] bb = b._mVec;
			res[0] = a+bb[0];
			res[1] = bb[1];
			res[2] = bb[2];
			res[3] = bb[3];
			res[4] = bb[4];
			res[5] = bb[5];
			res[6] = bb[6];
			res[7] = bb[7];
			res[8] = bb[8];
			res[9] = bb[9];
			res[10] = bb[10];
			res[11] = bb[11];
			res[12] = bb[12];
			res[13] = bb[13];
			res[14] = bb[14];
			res[15] = bb[15];
			return t;
		}

		/**
		 * @param a
		 * @param b
		 * @return multivector a + scalar b
		 */
		public static PGA3D add (PGA3D a, float b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			res[0] = aa[0]+b;
			res[1] = aa[1];
			res[2] = aa[2];
			res[3] = aa[3];
			res[4] = aa[4];
			res[5] = aa[5];
			res[6] = aa[6];
			res[7] = aa[7];
			res[8] = aa[8];
			res[9] = aa[9];
			res[10] = aa[10];
			res[11] = aa[11];
			res[12] = aa[12];
			res[13] = aa[13];
			res[14] = aa[14];
			res[15] = aa[15];
			return t;
		}

		/**
		 * 
		 * @param a
		 * @param b
		 * @return scalar a - multivector b
		 */
		public static PGA3D sub (float a, PGA3D b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] bb = b._mVec;
			res[0] = a-bb[0];
			res[1] = -bb[1];
			res[2] = -bb[2];
			res[3] = -bb[3];
			res[4] = -bb[4];
			res[5] = -bb[5];
			res[6] = -bb[6];
			res[7] = -bb[7];
			res[8] = -bb[8];
			res[9] = -bb[9];
			res[10] = -bb[10];
			res[11] = -bb[11];
			res[12] = -bb[12];
			res[13] = -bb[13];
			res[14] = -bb[14];
			res[15] = -bb[15];
			return t;
		}

		/**
		 * 
		 * @param a
		 * @param b
		 * @return multivector a - scalar b
		 */
		public static PGA3D sub(PGA3D a, float b)
		{
			PGA3D t = new PGA3D();
			float[] res = t._mVec;
			float[] aa = a._mVec;
			res[0] = aa[0]-b;
			res[1] = aa[1];
			res[2] = aa[2];
			res[3] = aa[3];
			res[4] = aa[4];
			res[5] = aa[5];
			res[6] = aa[6];
			res[7] = aa[7];
			res[8] = aa[8];
			res[9] = aa[9];
			res[10] = aa[10];
			res[11] = aa[11];
			res[12] = aa[12];
			res[13] = aa[13];
			res[14] = aa[14];
			res[15] = aa[15];
			return t;
		}

        /**
         * @return euclidean norm of this multivector (strictly positive)
         */
		public float norm() { 
			return (float) Math.sqrt(
					Math.abs(
							PGA3D.mult(this, this.Conjugate())._mVec[0])
					);}
		
		/**
		 * @return ideal norm of this multivector (signed)
		 */
		public float inorm() { return _mVec[1] !=0.0f ?_mVec[1] : (_mVec[15] !=0.0f ? _mVec[15] : PGA3D.dual(this).norm());}
		
		/**
		 * 
		 * @return this multivector, normalized (aka, the product of this multivector and 1/its euclidean norm. 
		 */
		public PGA3D normalized() { return PGA3D.mult(this, (1/norm()));}
		
		
		
		
		// PGA is plane based. Vectors are planes.
		public static PGA3D e0 = new PGA3D(1f, 1);
		public static PGA3D e1 = new PGA3D(1f, 2);
		public static PGA3D e2 = new PGA3D(1f, 3);
		public static PGA3D e3 = new PGA3D(1f, 4);
		
		// PGA lines are bivectors.
		public static PGA3D e01 = meet(e0, e1); 
		public static PGA3D e02 = meet(e0, e2);
		public static PGA3D e03 = meet(e0, e3);
		public static PGA3D e12 = meet(e1, e2); 
		public static PGA3D e31 = meet(e3, e1);
		public static PGA3D e23 = meet(e2, e3);
		
		// PGA points are trivectors.
		public static PGA3D e123 = meet(meet(e1, e2), e3); // the origin
		public static PGA3D e032 = meet(meet(e0, e3), e2);
		public static PGA3D e013 = meet(meet(e0, e1), e3);
		public static PGA3D e021 = meet(meet(e0, e2), e1);

		/**
		 * A plane defined using its homogenous equation ax + by + cz + d = 0
		 * @param a
		 * @param b
		 * @param c
		 * @param d
		 * @return a multivector corresponding to a plane
		 * */
		public static PGA3D plane(float a, float b, float c, float d) { 
			return add(
									add(mult(a, e1),
											mult(b,e2)),  
									add(mult(c,e3), 
											mult(d,e0))
									);}
		
		/**
		 * define point by xyz coordinates
		 * @param x
		 * @param y
		 * @param z
		 * @return a multivector corresponding to a point.
		 */
		public static PGA3D point(float x, float y, float z) { 
			return add(
								e123,
								add( 
										add(mult(x, e032), mult(y, e013)), 
										mult(z, e021)
									)
							); }
		
		/**
		 * a rotation around the given line by the given angle (equivalent to complex exponentiation)
		 * @param angle
		 * @param line
		 * @return
		 */
		public static PGA3D rotor(float angle, PGA3D line) { 
			return add(
					((float) Math.cos(angle/2.0f)),  mult(((float) Math.sin(angle/2.0f)), line.normalized()));
		}
		
		/**
		 * a rotation around the given line by the given angle (equivalent to dual exponentiation)
		 * @param angle
		 * @param line
		 * @return
		 */
		public static PGA3D translator(float dist, PGA3D line) { 
			return add(1.0f,  mult(dist/2.0f,  line)); 
		}
		
		
		// circle(t) with t going from 0 to 1.
		public static PGA3D circle(float t, float radius, PGA3D line) {
		  return mult(rotor(t*2.0f*(float) Math.PI,line), translator(radius, mult(e1, e0)));
		}
		
		// a torus is now the product of two circles. 
		/*public static PGA3D torus(float s, float t, float r1, PGA3D l1, float r2, PGA3D l2) {
		  return circle(s,r2,l2)*circle(t,r1,l1);
		}*/
		
		
		/*public String toString()
		{
			String sb = "";
			int n = 0;
			for (int i = 0; i < 16; i++) {
				if (_mVec[i] != 0.0f) {
					sb ($"{_mVec[i]}{(i == 0 ? string.Empty : basis[i])} + ");
					n++;
			        }
			}
			if (n==0) sb.Append("0");
			return sb.ToString().TrimEnd(' ', '+');
		}
	}	*/
}
