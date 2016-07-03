/*
 * Most of the code in this class is just a copy and paste of the PVector library, modified to
 * use doubles instead of floats. See the processing.core library for licensing information. 
 */
package sceneGraph;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;


public class DVector {
	/**
	 * ( begin auto-generated from DVector_x.xml )
	 *
	 * The x component of the vector. This field (variable) can be used to both
	 * get and set the value (see above example.)
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:field
	 * @usage web_application
	 * @brief The x component of the vector
	 */
	public double x;

	/**
	 * ( begin auto-generated from DVector_y.xml )
	 *
	 * The y component of the vector. This field (variable) can be used to both
	 * get and set the value (see above example.)
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:field
	 * @usage web_application
	 * @brief The y component of the vector
	 */
	public double y;

	/**
	 * ( begin auto-generated from DVector_z.xml )
	 *
	 * The z component of the vector. This field (variable) can be used to both
	 * get and set the value (see above example.)
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:field
	 * @usage web_application
	 * @brief The z component of the vector
	 */
	public double z;

	/** Array so that this can be temporarily used in an array context */
	transient public double[] array;


	/**
	 * Constructor for an empty vector: x, y, and z are set to 0.
	 */
	public DVector() {
	}


	/**
	 * Constructor to create a DVector from a Processing PVector.
	 */

	public DVector(PVector input) {
		this.x = input.x;
		this.y = input.y;
		this.z = input.z;
	}

	/**
	 * Constructor for a 3D vector.
	 *
	 * @param  x the x coordinate.
	 * @param  y the y coordinate.
	 * @param  z the z coordinate.
	 */
	public DVector(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}


	/**
	 * Constructor for a 2D vector: z coordinate is set to 0.
	 */
	public DVector(double x, double y) {
		this.x = x;
		this.y = y;
		this.z = 0;
	}

	public DVector(JSONArray dvectorJSON){
		this.x = dvectorJSON.getDouble(0);
		this.y = dvectorJSON.getDouble(1);
		this.z = dvectorJSON.getDouble(2);
	}
	
	public DVector(Vector3D v3d) {
		this.x = v3d.getX();
		this.y = v3d.getY();
		this.z = v3d.getZ();
	}


	/**
	 * ( begin auto-generated from DVector_set.xml )
	 *
	 * Sets the x, y, and z component of the vector using two or three separate
	 * variables, the data from a DVector, or the values from a double array.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @param x the x component of the vector
	 * @param y the y component of the vector
	 * @param z the z component of the vector
	 * @brief Set the components of the vector
	 */
	public DVector set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}


	/**
	 * @param x the x component of the vector
	 * @param y the y component of the vector
	 */
	public DVector set(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}


	/**
	 * @param v any variable of type DVector
	 */
	public DVector set(DVector v) {
		x = v.x;
		y = v.y;
		z = v.z;
		return this;
	}


	/**
	 * Set the x, y (and maybe z) coordinates using a double[] array as the source.
	 * @param source array to copy from
	 */
	public DVector set(double[] source) {
		if (source.length >= 2) {
			x = source[0];
			y = source[1];
		}
		if (source.length >= 3) {
			z = source[2];
		}
		return this;
	}


	/**
	 * ( begin auto-generated from DVector_random2D.xml )
	 *
	 * Make a new 2D unit vector with a random direction.  If you pass in "this"
	 * as an argument, it will use the PApplet's random number generator.  You can
	 * also pass in a target DVector to fill.
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @return the random DVector
	 * @brief Make a new 2D unit vector with a random direction.
	 * @see DVector#random3D()
	 */
	static public DVector random2D() {
		return random2D(null);
	}


	/**
	 * Make a new 2D unit vector with a random direction. Pass in the parent
	 * PApplet if you want randomSeed() to work (and be predictable). Or leave
	 * it null and be... random.
	 * @return the random DVector
	 */
	static public DVector random2D(DVector target) {
		return fromAngle((double) (Math.random() * Math.PI*2), target);
	}


	/**
	 * ( begin auto-generated from DVector_random3D.xml )
	 *
	 * Make a new 3D unit vector with a random direction.  If you pass in "this"
	 * as an argument, it will use the PApplet's random number generator.  You can
	 * also pass in a target DVector to fill.
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @return the random DVector
	 * @brief Make a new 3D unit vector with a random direction.
	 * @see DVector#random2D()
	 */
	static public DVector random3D() {
		return random3D(null);
	}



	static public DVector random3D(DVector target) {
		double angle;
		double vz;

		angle = (double) (Math.random()*Math.PI*2);
		vz    = (double) (Math.random()*2-1);

		double vx = (double) (Math.sqrt(1-vz*vz)*Math.cos(angle));
		double vy = (double) (Math.sqrt(1-vz*vz)*Math.sin(angle));
		if (target == null) {
			target = new DVector(vx, vy, vz);
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
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Make a new 2D unit vector from an angle
	 * @param angle the angle in radians
	 * @return the new unit DVector
	 */
	static public DVector fromAngle(double angle) {
		return fromAngle(angle,null);
	}


	/**
	 * Make a new 2D unit vector from an angle
	 *
	 * @param target the target vector (if null, a new vector will be created)
	 * @return the DVector
	 */
	static public DVector fromAngle(double angle, DVector target) {
		if (target == null) {
			target = new DVector((double)Math.cos(angle),(double)Math.sin(angle),0);
		} else {
			target.set((double)Math.cos(angle),(double)Math.sin(angle),0);
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_copy.xml )
	 *
	 * Gets a copy of the vector, returns a DVector object.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Get a copy of the vector
	 */
	public DVector copy() {
		return new DVector(x, y, z);
	}


	@Deprecated
	public DVector get() {
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
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Calculate the magnitude of the vector
	 * @return magnitude (length) of the vector
	 * @see DVector#magSq()
	 */
	public double mag() {
		return (double) Math.sqrt(x*x + y*y + z*z);
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
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Calculate the magnitude of the vector, squared
	 * @return squared magnitude of the vector
	 * @see DVector#mag()
	 */
	public double magSq() {
		return (x*x + y*y + z*z);
	}


	/**
	 * ( begin auto-generated from DVector_add.xml )
	 *
	 * Adds x, y, and z components to a vector, adds one vector to another, or
	 * adds two independent vectors together. The version of the method that
	 * adds two vectors together is a static method and returns a DVector, the
	 * others have no return value -- they act directly on the vector. See the
	 * examples for more context.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @param v the vector to be added
	 * @brief Adds x, y, and z components to a vector, one vector to another, or two independent vectors
	 */
	public DVector add(DVector v) {
		x += v.x;
		y += v.y;
		z += v.z;
		return this;
	}


	/**
	 * @param x x component of the vector
	 * @param y y component of the vector
	 */
	public DVector add(double x, double y) {
		this.x += x;
		this.y += y;
		return this;
	}


	/**
	 * @param z z component of the vector
	 */
	public DVector add(double x, double y, double z) {
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
	static public DVector add(DVector v1, DVector v2) {
		return add(v1, v2, null);
	}


	/**
	 * Add two vectors into a target vector
	 * @param target the target vector (if null, a new vector will be created)
	 */
	static public DVector add(DVector v1, DVector v2, DVector target) {
		if (target == null) {
			target = new DVector(v1.x + v2.x,v1.y + v2.y, v1.z + v2.z);
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
	 * DVector, the others have no return value -- they act directly on the
	 * vector. See the examples for more context.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @param v any variable of type DVector
	 * @brief Subtract x, y, and z components from a vector, one vector from another, or two independent vectors
	 */
	public DVector sub(DVector v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		return this;
	}


	/**
	 * @param x the x component of the vector
	 * @param y the y component of the vector
	 */
	public DVector sub(double x, double y) {
		this.x -= x;
		this.y -= y;
		return this;
	}


	/**
	 * @param z the z component of the vector
	 */
	public DVector sub(double x, double y, double z) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
		return this;
	}


	/**
	 * Subtract one vector from another
	 * @param v1 the x, y, and z components of a DVector object
	 * @param v2 the x, y, and z components of a DVector object
	 */
	static public DVector sub(DVector v1, DVector v2) {
		return sub(v1, v2, null);
	}


	/**
	 * Subtract one vector from another and store in another vector
	 * @param target DVector in which to store the result
	 */
	static public DVector sub(DVector v1, DVector v2, DVector target) {
		if (target == null) {
			target = new DVector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
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
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Multiply a vector by a scalar
	 * @param n the number to multiply with the vector
	 */
	public DVector mult(double n) {
		x *= n;
		y *= n;
		z *= n;
		return this;
	}


	/**
	 * @param v the vector to multiply by the scalar
	 */
	static public DVector mult(DVector v, double n) {
		return mult(v, n, null);
	}


	/**
	 * Multiply a vector by a scalar, and write the result into a target DVector.
	 * @param target DVector in which to store the result
	 */
	static public DVector mult(DVector v, double n, DVector target) {
		if (target == null) {
			target = new DVector(v.x*n, v.y*n, v.z*n);
		} else {
			target.set(v.x*n, v.y*n, v.z*n);
		}
		return target;
	}


	/**
	 * ( begin auto-generated from DVector_div.xml )
	 *
	 * Divides a vector by a scalar or divides one vector by another.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Divide a vector by a scalar
	 * @param n the number by which to divide the vector
	 */
	public DVector div(double n) {
		x /= n;
		y /= n;
		z /= n;
		return this;
	}


	/**
	 * Divide a vector by a scalar and return the result in a new vector.
	 * @param v the vector to divide by the scalar
	 * @return a new vector that is v1 / n
	 */
	static public DVector div(DVector v, double n) {
		return div(v, n, null);
	}


	/**
	 * Divide a vector by a scalar and store the result in another vector.
	 * @param target DVector in which to store the result
	 */
	static public DVector div(DVector v, double n, DVector target) {
		if (target == null) {
			target = new DVector(v.x/n, v.y/n, v.z/n);
		} else {
			target.set(v.x/n, v.y/n, v.z/n);
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
	 * @webref DVector:method
	 * @usage web_application
	 * @param v the x, y, and z coordinates of a DVector
	 * @brief Calculate the distance between two points
	 */
	public double dist(DVector v) {
		double dx = x - v.x;
		double dy = y - v.y;
		double dz = z - v.z;
		return (double) Math.sqrt(dx*dx + dy*dy + dz*dz);
	}


	/**
	 * @param v1 any variable of type DVector
	 * @param v2 any variable of type DVector
	 * @return the Euclidean distance between v1 and v2
	 */
	static public double dist(DVector v1, DVector v2) {
		double dx = v1.x - v2.x;
		double dy = v1.y - v2.y;
		double dz = v1.z - v2.z;
		return (double) Math.sqrt(dx*dx + dy*dy + dz*dz);
	}


	/**
	 * ( begin auto-generated from DVector_dot.xml )
	 *
	 * Calculates the dot product of two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @param v any variable of type DVector
	 * @return the dot product
	 * @brief Calculate the dot product of two vectors
	 */
	public double dot(DVector v) {
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
	 * @param v1 any variable of type DVector
	 * @param v2 any variable of type DVector
	 */
	static public double dot(DVector v1, DVector v2) {
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
	 * @webref DVector:method
	 * @param v the vector to calculate the cross product
	 * @brief Calculate and return the cross product
	 */
	public DVector cross(DVector v) {
		return cross(v, null);
	}


	/**
	 * @param v any variable of type DVector
	 * @param target DVector to store the result
	 */
	public DVector cross(DVector v, DVector target) {
		double crossX = y * v.z - v.y * z;
		double crossY = z * v.x - v.z * x;
		double crossZ = x * v.y - v.x * y;

		if (target == null) {
			target = new DVector(crossX, crossY, crossZ);
		} else {
			target.set(crossX, crossY, crossZ);
		}
		return target;
	}


	/**
	 * @param v1 any variable of type DVector
	 * @param v2 any variable of type DVector
	 * @param target DVector to store the result
	 */
	static public DVector cross(DVector v1, DVector v2, DVector target) {
		double crossX = v1.y * v2.z - v2.y * v1.z;
		double crossY = v1.z * v2.x - v2.z * v1.x;
		double crossZ = v1.x * v2.y - v2.x * v1.y;

		if (target == null) {
			target = new DVector(crossX, crossY, crossZ);
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
	public DVector projectOntoPlane(DVector p1, DVector p2) {
		return this.projectOntoPlane(p1.cross(p2));
	}
	
	
	/**
	 * Takes a vector representing the normal of a plane, and returns 
	 * the value of this vector projected onto that plane
	 * @param norm
	 * @return
	 */
	public DVector projectOntoPlane(DVector rawNorm) {
		DVector norm = normalize(rawNorm);
		DVector normProj = DVector.mult(norm, this.dot(norm));
		normProj.mult(-1);
		
		return DVector.add(normProj, this);
	}


	/**
	 * ( begin auto-generated from DVector_normalize.xml )
	 *
	 * Normalize the vector to length 1 (make it a unit vector).
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Normalize the vector to a length of 1
	 */
	public DVector normalize() {
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
	public DVector normalize(DVector target) {
		if (target == null) {
			target = new DVector();
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
	 * @webref DVector:method
	 * @usage web_application
	 * @param max the maximum magnitude for the vector
	 * @brief Limit the magnitude of the vector
	 */
	public DVector limit(double max) {
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
	 * @webref DVector:method
	 * @usage web_application
	 * @param len the new length for this vector
	 * @brief Set the magnitude of the vector
	 */
	public DVector setMag(double len) {
		normalize();
		mult(len);
		return this;
	}

	public PVector toPVec() {
		return new PVector((float)this.x, (float)this.y, (float)this.z);
	}
	
	public Vector3D toVector3D() {
		return new Vector3D((float)this.x, (float)this.y, (float)this.z);
	}


	/**
	 * Sets the magnitude of this vector, storing the result in another vector.
	 * @param target Set to null to create a new vector
	 * @param len the new length for the new vector
	 * @return a new vector (if target was null), or target
	 */
	public DVector setMag(DVector target, double len) {
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
	 * @webref DVector:method
	 * @usage web_application
	 * @return the angle of rotation
	 * @brief Calculate the angle of rotation for this vector
	 */
	public double heading() {
		double angle = (double) Math.atan2(y, x);
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
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Rotate the vector by an angle (2D only)
	 * @param theta the angle of rotation
	 */
	public DVector rotate(double theta) {
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
	 * @webref DVector:method
	 * @usage web_application
	 * @brief Linear interpolate the vector to another vector
	 * @param v the vector to lerp to
	 * @param amt  The amount of interpolation; some value between 0.0 (old vector) and 1.0 (new vector). 0.1 is very near the new vector. 0.5 is halfway in between.
	 * @see PApplet#lerp(double, double, double)
	 */
	public DVector lerp(DVector v, double amt) {

		x = dLerp(x, v.x, amt);
		y = dLerp(y, v.y, amt);
		z = dLerp(z, v.z, amt);
		return this;
	}


	/**
	 * Linear interpolate between two vectors (returns a new DVector object)
	 * @param v1 the vector to start from
	 * @param v2 the vector to lerp to
	 */
	public static DVector lerp(DVector v1, DVector v2, double amt) {
		DVector v = v1.copy();
		v.lerp(v2, amt);
		return v;
	}


	/**
	 * Linear interpolate the vector to x,y,z values
	 * @param x the x component to lerp to
	 * @param y the y component to lerp to
	 * @param z the z component to lerp to
	 */
	public DVector lerp(double x, double y, double z, double amt) {
		this.x = dLerp(this.x, x, amt);
		this.y = dLerp(this.y, y, amt);
		this.z = dLerp(this.z, z, amt);
		return this;
	}

	public double dLerp(double a, double b, double t) {
		return (1-t)*a + t*b;
	}
	/**
	 * ( begin auto-generated from DVector_angleBetween.xml )
	 *
	 * Calculates and returns the angle (in radians) between two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
	 * @usage web_application
	 * @param v1 the x, y, and z components of a DVector
	 * @param v2 the x, y, and z components of a DVector
	 * @brief Calculate and return the angle between two vectors
	 */
	static public double angleBetween(DVector v1, DVector v2) {

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

	public DVector getOrthogonal() {
		DVector result = new DVector(0,0,0);
		
		double threshold = this.mag() * 0.6;
		if(threshold > 0) {
			if (Math.abs(x) <= threshold) {
				double inverse  = 1 / Math.sqrt(y * y + z * z);
				return new DVector(0, inverse * z, -inverse * y);
			} else if (Math.abs(y) <= threshold) {
				double inverse  = 1 / Math.sqrt(x * x + z * z);
				return new DVector(-inverse * z, 0, inverse * x);
			}
			double inverse  = 1 / Math.sqrt(x * x + y * y);
			return new DVector(inverse * y, -inverse * x, 0);
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
	 * copied by using the <b>DVector.get()</b> method to copy into your own array.
	 *
	 * ( end auto-generated )
	 *
	 * @webref DVector:method
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
		if (!(obj instanceof DVector)) {
			return false;
		}
		final DVector p = (DVector) obj;
		return x == p.x && y == p.y && z == p.z;
	}

	public JSONArray toJSONArray() {
		JSONArray vec = new JSONArray();
		vec.append(this.x); vec.append(this.y); vec.append(this.z);
		return vec;
	}



}