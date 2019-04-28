/*
 * Most of the code in this class is just a copy and paste of the SGVec_3f library, modified to
 * use doubles instead of floats. See the processing.core library for licensing information. 
 */
package sceneGraph;

import data.CanLoad;
//import processing.core.PVector;
import data.JSONArray;
import data.JSONObject;
import sceneGraph.math.floatV.SGVec_2f;
import sceneGraph.math.SGVec_3d;
import sceneGraph.math.floatV.SGVec_3f;
import sceneGraph.math.Vec3d;



public class IKVector extends SGVec_3d implements CanLoad {
	
	private static final long serialVersionUID = -2110876906687530611L;
	public final static IKVector X = new IKVector(1, 0, 0);
	public final static IKVector Y = new IKVector(0, 1, 0);
	public final static IKVector Z = new IKVector(0, 0, 1);
	public final static IKVector Zero = new IKVector(0, 0, 0);
	

	/** Array so that this can be temporarily used in an array context */
	transient public double[] array;


	/**
	 * Constructor for an empty vector: x, y, and z are set to 0.
	 */
	public IKVector() {
	}


	/**
	 * Constructor for a 3D vector.
	 *
	 * @param  x the x coordinate.
	 * @param  y the y coordinate.
	 * @param  z the z coordinate.
	 */
	public IKVector(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public IKVector(JSONArray dvectorJSON){
		this.x = dvectorJSON.getDouble(0);
		this.y = dvectorJSON.getDouble(1);
		this.z = dvectorJSON.getDouble(2);
	}

	/**
	 * Constructor for a 2D vector: z coordinate is set to 0.
	 */
	public IKVector(double x, double y) {
		this.x = x;
		this.y = y;
		this.z = 0;
	}


	public IKVector(SGVec_3d in) {
		this.x = in.x;
		this.y = in.y;
		this.z = in.z;
	}
	
	public IKVector(SGVec_3f in) {
		this.x = in.x;
		this.y = in.y;
		this.z = in.z;
	}

	public IKVector(SGVec_2f input) {
		this.x = input.x;
		this.y = input.y;
	}

	

	public IKVector(Vec3d p1) {
		this.x = (double)p1.getX();
		this.y = (double)p1.getY();
		this.z = (double)p1.getZ();
	}


	/**
	 * @return true if any of this IKVector's values are NaN. False otherwise. 
	 */
	public boolean hasNan() {
		return Double.isNaN(this.x) || Double.isNaN(this.y) || Double.isNaN(this.z);
	}



	/**
	 * Set the x, y (and maybe z) coordinates using a double[] array as the source.
	 * @param source array to copy from
	 */
	public IKVector set(double[] source) {
		if (source.length >= 2) {
			this.x = source[0];
			this.y = source[1];
		}
		if (source.length >= 3) {
			this.z = source[2];
		}
		return this;
	}
	
	
	


	/**
	 * ( begin auto-generated from DVector_random2D.xml )
	 *
	 * Make a new 2D unit vector with a random direction.  If you pass in "this"
	 * as an argument, it will use the StringFuncs's random number generator.  You can
	 * also pass in a target IKVector to fill.
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @return the random IKVector
	 * @brief Make a new 2D unit vector with a random direction.
	 * @see IKVector#random3D()
	 */
	static public IKVector random2D() {
		return random2D(null);
	}


	/**
	 * Make a new 2D unit vector with a random direction. Pass in the parent
	 * StringFuncs if you want randomSeed() to work (and be predictable). Or leave
	 * it null and be... random.
	 * @return the random IKVector
	 */
	static public IKVector random2D(IKVector target) {
		return fromAngle((double) (Math.random() * Math.PI*2d), target);
	}


	/**
	 * ( begin auto-generated from DVector_random3D.xml )
	 *
	 * Make a new 3D unit vector with a random direction.  If you pass in "this"
	 * as an argument, it will use the StringFuncs's random number generator.  You can
	 * also pass in a target IKVector to fill.
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @return the random IKVector
	 * @brief Make a new 3D unit vector with a random direction.
	 * @see IKVector#random2D()
	 */
	static public IKVector random3D() {
		return random3D(null);
	}



	static public IKVector random3D(IKVector target) {
		double angle;
		double vz;

		angle = (Math.random()*Math.PI*2d);
		vz    = (Math.random()*2-1d);

		double vx = (Math.sqrt(1d-vz*vz)*Math.cos(angle));
		double vy = (Math.sqrt(1d-vz*vz)*Math.sin(angle));
		if (target == null) {
			target = new IKVector(vx, vy, vz);
			//target.normalize(); // Should be unnecessary
		} else {
			target.set(vx,vy,vz);
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_sub.xml )
	 *
	 * Make a new 2D unit vector from an angle.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Make a new 2D unit vector from an angle
	 * @param angle the angle in radians
	 * @return the new unit IKVector
	 */
	static public IKVector fromAngle(double angle) {
		return fromAngle(angle,null);
	}


	/**
	 * Make a new 2D unit vector from an angle
	 *
	 * @param target the target vector (if null, a new vector will be created)
	 * @return the IKVector
	 */
	static public IKVector fromAngle(double angle, IKVector target) {
		if (target == null) {
			target = new IKVector(Math.cos(angle),Math.sin(angle),0);
		} else {
			target.set(Math.cos(angle),Math.sin(angle),0);
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_copy.xml )
	 *
	 * Gets a copy of the vector, returns a IKVector object.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Get a copy of the vector
	 */
	public IKVector copy() {
		return new IKVector(x, y, z);
	}


	@Deprecated
	public IKVector get() {
		return copy();
	}


	/**
	 * @param target
	 */
	public double[] get(double[] target) {
		if (target == null) {
			return new double[] { x, y, z };
		}
		if (target.length >= 2) {
			target[0] = x;
			target[1] = y;
		}
		if (target.length >= 3) {
			target[2] = z;
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_mag.xml )
	 *
	 * Calculates the magnitude (length) of the vector and returns the result
	 * as a double (this is simply the equation <em>sqrt(x*x + y*y + z*z)</em>.)
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Calculate the magnitude of the vector
	 * @return magnitude (length) of the vector
	 * @see IKVector#magSq()
	 */
	public double mag() {
		return Math.sqrt(x*x + y*y + z*z);
	}


	/**
	 * ( begin auto-generated from DVector_mag.xml )
	 *
	 * Calculates the squared magnitude of the vector and returns the result
	 * as a double (this is simply the equation <em>(x*x + y*y + z*z)</em>.)
	 * Faster if the real length is not required in the
	 * case of comparing vectors, etc.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Calculate the magnitude of the vector, squared
	 * @return squared magnitude of the vector
	 * @see IKVector#mag()
	 */
	public double magSq() {
		return (x*x + y*y + z*z);
	}


	/**
	 * ( begin auto-generated from DVector_add.xml )
	 *
	 * Adds x, y, and z components to a vector, adds one vector to another, or
	 * adds two independent vectors together. The version of the method that
	 * adds two vectors together is a static method and returns a IKVector, the
	 * others have no return value -- they act directly on the vector. See the
	 * examples for more context.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param v the vector to be added
	 * @brief Adds x, y, and z components to a vector, one vector to another, or two independent vectors
	 */
	public IKVector add(SGVec_3d v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}
	


	/**
	 * @param x x component of the vector
	 * @param y y component of the vector
	 */
	public IKVector add(double x, double y) {
		this.x += x;
		this.y += y;
		return this;
	}
	
	public IKVector add(SGVec_3f v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}


	/**
	 * @param z z component of the vector
	 */
	public IKVector add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}


	/**
	 * Add two vectors
	 * @param v1 a vector
	 * @param v2 another vector
	 */
	static public IKVector add(IKVector v1, IKVector v2) {
		return add(v1, v2, null);
	}


	/**
	 * Add two vectors into a target vector
	 * @param target the target vector (if null, a new vector will be created)
	 */
	static public IKVector add(IKVector v1, IKVector v2, IKVector target) {
		if (target == null) {
			target = new IKVector(v1.x + v2.x,v1.y + v2.y, v1.z + v2.z);
		} else {
			target.set(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_sub.xml )
	 *
	 * Subtracts x, y, and z components from a vector, subtracts one vector
	 * from another, or subtracts two independent vectors. The version of the
	 * method that subtracts two vectors is a static method and returns a
	 * IKVector, the others have no return value -- they act directly on the
	 * vector. See the examples for more context.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param v any variable of type IKVector
	 * @brief Subtract x, y, and z components from a vector, one vector from another, or two independent vectors
	 */
	public IKVector sub(SGVec_3d v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		return this;
	}
	
	public IKVector sub(SGVec_3f v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		return this;
	}


	/**
	 * @param x the x component of the vector
	 * @param y the y component of the vector
	 */
	public IKVector sub(double x, double y) {
		this.x -= x;
		this.y -= y;
		return this;
	}


	/**
	 * @param z the z component of the vector
	 */
	public IKVector sub(double x, double y, double z) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
		return this;
	}


	/**
	 * Subtract one vector from another
	 * @param v1 the x, y, and z components of a IKVector object
	 * @param v2 the x, y, and z components of a IKVector object
	 */
	static public IKVector sub(IKVector v1, IKVector v2) {
		return new IKVector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
	}



	/**
	 * Subtract one vector from another and store in another vector
	 * @param target IKVector in which to store the result
	 */
	static public IKVector sub(IKVector v1, IKVector v2, IKVector target) {
		if (target == null) {
			target = new IKVector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
		} else {
			target.set(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
		}
		return target;
	}	


	/**
	 * ( begin auto-generated from DVector_mult.xml )
	 *
	 * Multiplies a vector by a scalar or multiplies one vector by another.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Multiply a vector by a scalar
	 * @param n the number to multiply with the vector
	 */
	public IKVector mult(double n) {
		x *= n;
		y *= n;
		z *= n;
		return this;
	}


	/**
	 * @param v the vector to multiply by the scalar
	 */
	static public IKVector mult(IKVector v, double n) {
		return mult(v, n, null);
	}


	/**
	 * Multiply a vector by a scalar, and write the result into a target IKVector.
	 * @param target IKVector in which to store the result
	 */
	static public IKVector mult(IKVector v, double n, IKVector target) {
		if (target == null) {
			target = new IKVector(v.x*n, v.y*n, v.z*n);
		} else {
			target.set(v.x*n, v.y*n, v.z*n);
		}
		return target;
	}





	/**
	 * ( begin auto-generated from DVector_dist.xml )
	 *
	 * Calculates the Euclidean distance between two points (considering a
	 * point as a vector object).
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param v the x, y, and z coordinates of a IKVector
	 * @brief Calculate the distance between two points
	 */
	public double dist(IKVector v) {
		double dx = x - v.x;
		double dy = y - v.y;
		double dz = z - v.z;
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}


	/**
	 * @param v1 any variable of type IKVector
	 * @param v2 any variable of type IKVector
	 * @return the Euclidean distance between v1 and v2
	 */
	static public double dist(IKVector v1, IKVector v2) {
		double dx = v1.x - v2.x;
		double dy = v1.y - v2.y;
		double dz = v1.z - v2.z;
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}


	/**
	 * ( begin auto-generated from DVector_dot.xml )
	 *
	 * Calculates the dot product of two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param v any variable of type IKVector
	 * @return the dot product
	 * @brief Calculate the dot product of two vectors
	 */
	public double dot(IKVector v) {
		return x*v.x + y*v.y + z*v.z;
	}


	/**
	 * @param x x component of the vector
	 * @param y y component of the vector
	 * @param z z component of the vector
	 */
	public double dot(double x, double y, double z) {
		return this.x*x + this.y*y + this.z*z;
	}


	/**
	 * @param v1 any variable of type IKVector
	 * @param v2 any variable of type IKVector
	 */
	static public double dot(IKVector v1, IKVector v2) {
		return v1.x*v2.x + v1.y*v2.y + v1.z*v2.z;
	}


	/**
	 * ( begin auto-generated from DVector_cross.xml )
	 *
	 * Calculates and returns a vector composed of the cross product between
	 * two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @param v the vector to calculate the cross product
	 * @brief Calculate and return the cross product
	 */
	public IKVector cross(IKVector v) {
		return cross(v, null);
	}


	/**
	 * @param v any variable of type IKVector
	 * @param target IKVector to store the result
	 */
	public IKVector cross(IKVector v, IKVector target) {
		double crossX = y * v.z - v.y * z;
		double crossY = z * v.x - v.z * x;
		double crossZ = x * v.y - v.x * y;

		if (target == null) {
			target = new IKVector(crossX, crossY, crossZ);
		} else {
			target.set(crossX, crossY, crossZ);
		}
		return target;
	}


	/**
	 * @param v1 any variable of type IKVector
	 * @param v2 any variable of type IKVector
	 * @param target IKVector to store the result
	 */
	static public IKVector cross(IKVector v1, IKVector v2, IKVector target) {
		double crossX = v1.y * v2.z - v2.y * v1.z;
		double crossY = v1.z * v2.x - v2.z * v1.x;
		double crossZ = v1.x * v2.y - v2.x * v1.y;

		if (target == null) {
			target = new IKVector(crossX, crossY, crossZ);
		} else {
			target.set(crossX, crossY, crossZ);
		}
		return target;
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
	public IKVector projectOntoPlane(IKVector p1, IKVector p2) {
		return this.projectOntoPlane(p1.cross(p2));
	}
	
	
	/**
	 * Takes a vector representing the normal of a plane, and returns 
	 * the value of this vector projected onto that plane
	 * @param norm
	 * @return
	 */
	public IKVector projectOntoPlane(IKVector rawNorm) {
		IKVector norm = normalize(rawNorm);
		IKVector normProj = IKVector.mult(norm, this.dot(norm));
		normProj.mult(-1);
		
		return IKVector.add(normProj, this);
	}


	/**
	 * ( begin auto-generated from DVector_normalize.xml )
	 *
	 * Normalize the vector to length 1 (make it a unit vector).
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Normalize the vector to a length of 1
	 */
	public IKVector normalize() {
		double m = mag();
		if (m != 0 && m != 1) {
			div(m);
		}
		return this;
	}


	/**
	 * @param target Set to null to create a new vector
	 * @return a new vector (if target was null), or target
	 */
	public IKVector normalize(IKVector target) {
		if (target == null) {
			target = new IKVector();
		}
		double m = mag();
		if (m > 0) {
			target.set(x/m, y/m, z/m);
		} else {
			target.set(x, y, z);
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_limit.xml )
	 *
	 * Limit the magnitude of this vector to the value used for the <b>max</b> parameter.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param max the maximum magnitude for the vector
	 * @brief Limit the magnitude of the vector
	 */
	public IKVector limit(double max) {
		if (magSq() > max*max) {
			normalize();
			mult(max);
		}
		return this;
	}


	/**
	 * ( begin auto-generated from DVector_setMag.xml )
	 *
	 * Set the magnitude of this vector to the value used for the <b>len</b> parameter.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param len the new length for this vector
	 * @brief Set the magnitude of the vector
	 */
	public IKVector setMag(double len) {
		normalize();
		mult(len);
		return this;
	}

	public SGVec_3f to_Vec3f() {
		return new SGVec_3f((float)this.x, (float)this.y, (float)this.z);
	}


	public SGVec_2f to_Vec2f() {
		return new SGVec_2f((float)this.x, (float)this.y);
	}
		


	/**
	 * Sets the magnitude of this vector, storing the result in another vector.
	 * @param target Set to null to create a new vector
	 * @param len the new length for the new vector
	 * @return a new vector (if target was null), or target
	 */
	public IKVector setMag(IKVector target, double len) {
		target = normalize(target);
		target.mult(len);
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_setMag.xml )
	 *
	 * Calculate the angle of rotation for this vector (only 2D vectors)
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @return the angle of rotation
	 * @brief Calculate the angle of rotation for this vector
	 */
	public double heading() {
		double angle = Math.atan2(y, x);
		return angle;
	}


	@Deprecated
	public double heading2D() {
		return heading();
	}


	/**
	 * ( begin auto-generated from DVector_rotate.xml )
	 *
	 * Rotate the vector by an angle (only 2D vectors), magnitude remains the same
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Rotate the vector by an angle (2D only)
	 * @param theta the angle of rotation
	 */
	public IKVector rotate(double theta) {
		double temp = x;
		// Might need to check for rounding errors like with angleBetween function?
		x = x*Math.cos(theta) - y*Math.sin(theta);
		y = temp*Math.sin(theta) + y*Math.cos(theta);
		return this;
	}


	/**
	 * ( begin auto-generated from DVector_rotate.xml )
	 *
	 * Linear interpolate the vector to another vector
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @brief Linear interpolate the vector to another vector
	 * @param v the vector to lerp to
	 * @param amt  The amount of interpolation; some value between 0.0 (old vector) and 1.0 (new vector). 0.1 is very near the new vector. 0.5 is halfway in between.
	 * @see StringFuncs#lerp(double, double, double)
	 */
	public IKVector lerp(IKVector v, double amt) {

		x = dLerp(x, v.x, amt);
		y = dLerp(y, v.y, amt);
		z = dLerp(z, v.z, amt);
		return this;
	}


	/**
	 * Linear interpolate between two vectors (returns a new IKVector object)
	 * @param v1 the vector to start from
	 * @param v2 the vector to lerp to
	 */
	public static IKVector lerp(IKVector v1, IKVector v2, double amt) {
		IKVector v = v1.copy();
		v.lerp(v2, amt);
		return v;
	}


	/**
	 * Linear interpolate the vector to x,y,z values
	 * @param x the x component to lerp to
	 * @param y the y component to lerp to
	 * @param z the z component to lerp to
	 */
	public IKVector lerp(double x, double y, double z, double amt) {
		this.x = dLerp(this.x, x, amt);
		this.y = dLerp(this.y, y, amt);
		this.z = dLerp(this.z, z, amt);
		return this;
	}

	public double dLerp(double a, double b, double t) {
		return (1d-t)*a + t*b;
	}
	/**
	 * ( begin auto-generated from DVector_angleBetween.xml )
	 *
	 * Calculates and returns the angle (in radians) between two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage web_application
	 * @param v1 the x, y, and z components of a IKVector
	 * @param v2 the x, y, and z components of a IKVector
	 * @brief Calculate and return the angle between two vectors
	 */
	static public double angleBetween(IKVector v1, IKVector v2) {

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
			// http://code.google.com/p/processing/issues/detail?id=435
			return 0;
		}
		return (double) Math.acos(amt);
	}

	public IKVector getOrthogonal() {
		IKVector result = new IKVector(0,0,0);
		
		double threshold = this.mag() * 0.6;
		if(threshold > 0) {
			if (Math.abs(x) <= threshold) {
				double inverse  = 1 / Math.sqrt(y * y + z * z);
				return new IKVector(0, inverse * z, -inverse * y);
			} else if (Math.abs(y) <= threshold) {
				double inverse  = 1 / Math.sqrt(x * x + z * z);
				return new IKVector(-inverse * z, 0, inverse * x);
			}
			double inverse  = 1 / Math.sqrt(x * x + y * y);
			return new IKVector(inverse * y, -inverse * x, 0);
		}

		return result; 
	}

	@Override
	public String toString() {
	
		return "[ " + x + ", " + y + ", " + z + " ]";
	}


	/**
	 * ( begin auto-generated from DVector_array.xml )
	 *
	 * Return a representation of this vector as a double array. This is only
	 * for temporary use. If used in any other fashion, the contents should be
	 * copied by using the <b>IKVector.get()</b> method to copy into your own array.
	 *
	 * ( end auto-generated )
	 *
	 * @webref IKVector:method
	 * @usage: web_application
	 * @brief Return a representation of the vector as a double array
	 */
	public double[] array() {
		if (array == null) {
			array = new double[3];
		}
		array[0] = x;
		array[1] = y;
		array[2] = z;
		return array;
	}


	@Override
	public boolean equals(Object obj) {
		if ((obj instanceof IKVector)) {
			final IKVector p = (IKVector) obj;
			return x == p.x && y == p.y && z == p.z;
		} else
			return false;
	}
	
	public JSONArray toJSONArray() {
		JSONArray vec = new JSONArray();
		vec.append(this.x); vec.append(this.y); vec.append(this.z);
		return vec;
	}


	@Override
	public IKVector populateSelfFromJSON(JSONObject j) {
		JSONArray jarr = j.getJSONArray("dvec"); 
		this.x = jarr.getDouble(0);
		this.y = jarr.getDouble(1);
		this.z = jarr.getDouble(2);
		return this;
	}


	@Override
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject(); 
		result.setJSONArray("dvec", toJSONArray());
		return result;
	}
	
}