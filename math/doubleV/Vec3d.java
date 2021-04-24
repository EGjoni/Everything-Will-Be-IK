package math.doubleV;

import asj.CanLoad;
import math.floatV.Vec3f;

public abstract class Vec3d<T extends Vec3d<T>> implements Vecd<T>, CanLoad {
		
	private static final long serialVersionUID = 3840054589595372522L;

	/** the x-component of this vector **/
	public double x;
	/** the y-component of this vector **/
	public double y;
	/** the z-component of this vector **/
	public double z;

	public final static int X = 0, Y= 1, Z = 2;


	/** Constructs a vector at (0,0,0) */
	public Vec3d () {
	}

	/** Creates a vector with the given components
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component */
	public Vec3d (double x, double y, double z) {
		this.set(x, y, z);
	}
	

	/** Creates a vector from the given vector
	 * @param vector The vector */
	public Vec3d (T vector) {
		this.set(vector);
	}
	
	/** Creates a vector from the given vector
	 * @param vector The vector */
	public Vec3d (final Vec3f vector) {
		this.set(vector);
	}

	/** Creates a vector from the given array. The array must have at least 3 elements.
	 *
	 * @param values The array */
	public Vec3d (final double[] values) {
		this.set(values[0], values[1], values[2]);
	}


	/** Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public  T set (double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return (T)this;
	}

	public <V extends Vec3d<?>>T set (final V vector) {
		return this.set(vector.x, vector.y, vector.z);
	}
	
	public T set (final Vec3f vector) {
		return this.set(vector.getX(), vector.getY(), vector.getZ());
	}

	/** Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining */
	@Override
	public T set (final double[] values) {
		return this.set(values[0], values[1], values[2]);
	}

	/** Sets the components from the given spherical coordinate
	 * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
	 * @param polarAngle The angle between z-axis in radians [0, pi]
	 * @return This vector for chaining */
	public T setFromSpherical (double azimuthalAngle, double polarAngle) {
		double cosPolar = Math.cos(polarAngle);
		double sinPolar = Math.sin(polarAngle);

		double cosAzim = Math.cos(azimuthalAngle);
		double sinAzim = Math.sin(azimuthalAngle);

		return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
	}


	@Override
	public  <V extends Vecd<?>> T add (V vector) {
		return this.add(vector.getX(), vector.getY(), vector.getZ());
	}
	
	@Override
	public  <V extends Vec3f<?>> T add (V vector) {
		return this.add(vector.getX(), vector.getY(), vector.getZ());
	}
	
	@Override
	public  T add (double[] v) {
		return this.add(v[0], v[1], v[2]);
	}

	/** Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining. */
	public T add (double x, double y, double z) {
		this.x += x; 
		this.y+= y; 
		this.z += z;
		return (T) this;
	}

	/** Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining */
	public T add (double values) {
		return set(this.x + values, this.y + values, this.z + values);
	}

	public T sub (Vec3d<?> a_vec) {
		return  sub(a_vec.x, a_vec.y, a_vec.z);
	}
	
	@Override
	public <V extends Vecd<?>> T sub (V a_vec) {
		return sub(a_vec.getX(), a_vec.getY(), a_vec.getZ());
	}

	/** Subtracts the other vector from this vector.
	 *
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public T sub (double x, double y, double z) {
		return this.set(this.x - x, this.y - y, this.z - z);
	}

	/** Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining */
	public T sub (double value) {
		return this.set(this.x - value, this.y - value, this.z - value);
	}
	
	@Override
	public <V extends Vecd<?>> T mult (V other) {
		return this.set(x * other.getX(), y * other.getY(), z * other.getZ());
	}

	public <V extends Vec3d<?>> T mult (V other) {
		return this.set(x * other.x, y * other.y, z * other.z);
	}

	/** Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining */
	public T mult (double vx, double vy, double vz) {
		return this.set(this.x * vx, this.y * vy, this.z * vz);
	}
	

	@Override
	public T div(double n) {
		x /= n;
		y /= n;
		z /= n;
		return (T) this;
	}

	

	@Override
	public <V extends Vecd<?>> T mulAdd (V vec, double scalar) {
		this.x += vec.getX() * scalar;
		this.y += vec.getY() * scalar;
		this.z += vec.getZ() * scalar;
		return (T) this;
	}
	
	public T mulAdd (Vec3d<?> vec, double scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		this.z += vec.z * scalar;
		return (T)this;
	}

	@Override
	public <V extends Vecd<?>> T mulAdd (V vec, V mulVec) {
		this.x += vec.getX() * mulVec.getX();
		this.y += vec.getY() * mulVec.getY();
		this.z += vec.getZ() * mulVec.getZ();
		return (T) this;
	}
	
	public T mulAdd (Vec3d<?> vec, Vec3d<?> mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		this.z += vec.z * mulVec.z;
		return (T) this;
	}

	/** @return The euclidean length */
	public static double mag (final double x, final double y, final double z) {
		return (double)Math.sqrt(x * x + y * y + z * z);
	}

	@Override
	public double mag () {
		return (double)Math.sqrt(x * x + y * y + z * z);
	}

	/** @return The squared euclidean length */
	public static double magSq (final double x, final double y, final double z) {
		return x * x + y * y + z * z;
	}

	@Override
	public double magSq () {
		return x * x + y * y + z * z;
	}

	/** @param vector The other vector
	 * @return Whether this and the other vector are equal */
	public boolean idt (final Vec3d vector) {
		return x == vector.x && y == vector.y && z == vector.z;
	}

	/** @return The euclidean distance between the two specified vectors */
	public static double dst (final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		final double a = x2 - x1;
		final double b = y2 - y1;
		final double c = z2 - z1;
		return (double)Math.sqrt(a * a + b * b + c * c);
	}

	@Override
	public double dist (final Vec3d vector) {
		final double a = vector.x - x;
		final double b = vector.y - y;
		final double c = vector.z - z;
		return (double)Math.sqrt(a * a + b * b + c * c);
	}

	/** @return the distance between this point and the given point */
	public double dst (double x, double y, double z) {
		final double a = x - this.x;
		final double b = y - this.y;
		final double c = z - this.z;
		return (double)Math.sqrt(a * a + b * b + c * c);
	}

	/** @return the squared distance between the given points */
	public static double dst2 (final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
		final double a = x2 - x1;
		final double b = y2 - y1;
		final double c = z2 - z1;
		return a * a + b * b + c * c;
	}

	@Override
	public double distSq (Vec3d point) {
		final double a = point.x - x;
		final double b = point.y - y;
		final double c = point.z - z;
		return a * a + b * b + c * c;
	}

	/** Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance */
	public double dst2 (double x, double y, double z) {
		final double a = x - this.x;
		final double b = y - this.y;
		final double c = z - this.z;
		return a * a + b * b + c * c;
	}

	@Override
	public T normalize () {
		final double len2 = this.mag();
		if (len2 == 0d || len2 == 1d) return (T) this;
		return this.mult(1d / (double)len2);
	}

	/** @return The dot product between the two vectors */
	public static double dot (double x1, double y1, double z1, double x2, double y2, double z2) {
		return x1 * x2 + y1 * y2 + z1 * z2;
	}

	@Override
	public <V extends Vecd<?>> double dot (final V vector) {
		return x * vector.getX() + y * vector.getY() + z * vector.getZ();
	}
	
	public <V extends Vec3d<?>> double dot (final V vector) {
		return x * vector.x + y * vector.y + z * vector.z;
	}

	/** Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product */
	public double dot (double x, double y, double z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	@Override
	public <V extends Vecd<?>> T crs (final V vector) {
		return this.set(y * vector.getZ() - z * vector.getY(), z * vector.getX() - x * vector.getZ(), x * vector.getY() - y * vector.getX());
	}

	
	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	public <V extends Vec3d<?>> T crs (final V vector) {
		return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
	}
	
	/** Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public Vec3d crs (double x, double y, double z) {
		return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/** Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
	 * rotation and scale plus a 1x3 matrix representing the translation.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public T mul4x3 (double[] matrix) {
		return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7]
			+ matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11]);
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
	public Vec3d getPlaneProjectionOf(T p1, T p2) {
		return this.getPlaneProjectionOf((T) p1.crossCopy(p2));
	}
	
	
	/**
	 * Takes a vector representing the normal of a plane, and returns 
	 * the value of this vector projected onto that plane
	 * @param norm
	 * @return
	 */
	public T getPlaneProjectionOf(T rawNorm) {
		T norm = (T) rawNorm.copy().normalize();
		T normProj = (T) norm.multCopy(this.dot(norm));
		normProj.mult(-1);
		
		return (T) normProj.addCopy(this);
	}


	@Override
	public boolean isUnit () {
		return isUnit(0.000000001f);
	}

	@Override
	public boolean isUnit (final double margin) {
		return Math.abs(magSq() - 1f) < margin;
	}

	@Override
	public boolean isZero () {
		return x == 0 && y == 0 && z == 0;
	}

	@Override
	public boolean isZero (final double margin) {
		return magSq() < margin;
	}

	@Override
	public boolean isOnLine (Vec3d other, double epsilon) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon;
	}

	@Override
	public boolean isOnLine (Vec3d other) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.DOUBLE_ROUNDING_ERROR;
	}

	@Override
	public boolean isCollinear (Vec3d other, double epsilon) {
		return isOnLine(other, epsilon) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinear (Vec3d other) {
		return isOnLine(other) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (Vec3d other, double epsilon) {
		return isOnLine(other, epsilon) && hasOppositeDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (Vec3d other) {
		return isOnLine(other) && hasOppositeDirection(other);
	}

	@Override
	public boolean isPerpendicular (Vec3d vector) {
		return MathUtils.isZero(dot(vector));
	}

	@Override
	public boolean isPerpendicular (Vec3d vector, double epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	@Override
	public boolean hasSameDirection (Vec3d vector) {
		return dot(vector) > 0;
	}

	@Override
	public boolean hasOppositeDirection (Vec3d vector) {
		return dot(vector) < 0;
	}

	@Override
	public T lerp (final Vec3d target, double alpha) {
		x += alpha * (target.x - x);
		y += alpha * (target.y - y);
		z += alpha * (target.z - z);
		return (T) this;
	}

	/** Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
	 * stored in this vector.
	 *
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	public Vec3d slerp (final Vec3d target, double alpha) {
		final double dot = dot(target);
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

		// theta0 = angle between input vectors
		final double theta0 = (double)Math.acos(dot);
		// theta = angle between this vector and result
		final double theta = theta0 * alpha;

		final double st = (double)Math.sin(theta);
		final double tx = target.x - x * dot;
		final double ty = target.y - y * dot;
		final double tz = target.z - z * dot;
		final double l2 = tx * tx + ty * ty + tz * tz;
		final double dl = st * ((l2 < 0.0001f) ? 1f : 1f / (double)Math.sqrt(l2));

		return mult((double)Math.cos(theta)).add(tx * dl, ty * dl, tz * dl).normalize();
	}

	/** Converts this {@code Vector3} to a string in the format {@code (x,y,z)}.
	 * @return a string representation of this object. */
	@Override
	public String toString () {
		return "(" +(float) x + "," + (float)y + "," + (float)z + ")";
	}


	@Override
	public T limit (double limit) {
		return limitSq(limit * limit);
	}

	@Override
	public T limitSq (double limit2) {
		double len2 = magSq();
		if (len2 > limit2) {
			mult((double)Math.sqrt(limit2 / len2));
		}
		return (T) this;
	}

	@Override
	public T setMag (double len) {
		return setMagSq(len * len);
	}

	@Override
	public T setMagSq (double len2) {
		double oldLen2 = magSq();
		return (oldLen2 == 0 || oldLen2 == len2) ? (T) this : mult((double)Math.sqrt(len2 / oldLen2));
	}

	@Override
	public T clamp (double min, double max) {
		final double len2 = magSq();
		if (len2 == 0f) return (T) this;
		double max2 = max * max;
		if (len2 > max2) return mult((double)Math.sqrt(max2 / len2));
		double min2 = min * min;
		if (len2 < min2) return mult((double)Math.sqrt(min2 / len2));
		return (T) this;
	}

	public <V extends Vec3d<?>> boolean epsilonEquals (V other, double epsilon) {
		if (other == null) return false;
		if (Math.abs(other.x - x) > epsilon) return false;
		if (Math.abs(other.y - y) > epsilon) return false;
		if (Math.abs(other.z - z) > epsilon) return false;
		return true;
	}

	@Override
	public <V extends Vecd<?>> boolean epsilonEquals (V other, double epsilon) {
		if (other == null) return false;
		if (Math.abs(other.getX() - x) > epsilon) return false;
		if (Math.abs(other.getY() - y) > epsilon) return false;
		if (Math.abs(other.getZ() - z) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (double x, double y, double z, double epsilon) {
		if (Math.abs(x - this.x) > epsilon) return false;
		if (Math.abs(y - this.y) > epsilon) return false;
		if (Math.abs(z - this.z) > epsilon) return false;
		return true;
	}

	@Override
	public T setZero () {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		return (T) this;
	}

	
	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y; 
	}


	@Override
	public void setX_(double x) {
		this.x = x;		
	}

	@Override
	public void setY_(double y) {
		this.y = y;
		
	}
	/**
	 * sets this vector's Z componen to the input value
	 * @param Z
	 */
	public void setZ_(double z) {
		this.z = z;
	}
	
	
	public T getOrthogonal() {
		T result = this.copy();				
		result.set(0,0,0);
		double threshold = this.mag() * 0.6;
		if(threshold > 0) {
			if (Math.abs(x) <= threshold) {
				double inverse  = 1 / Math.sqrt(y * y + z * z);
				return result.set(0, inverse * z, -inverse * y);
			} else if (Math.abs(y) <= threshold) {
				double inverse  = 1 / Math.sqrt(x * x + z * z);
				return result.set(-inverse * z, 0, inverse * x);
			}
			double inverse  = 1 / Math.sqrt(x * x + y * y);
			return result.set(inverse * y, -inverse * x, 0);
		}

		return result; 
	}

	
	
	public static double dot(SGVec_3d u, SGVec_3d v) {
		return u.dot(v);
	}
	
	public static <V extends Vec3d<?>> V add(V v1, V v2) {
		return add(v1, v2, null);
	}
	
	public static <V extends Vec3d<?>> V add(V v1, V v2, V target) {
		if (target == null) {
			target = (V) v1.copy();
			v1.set(
					v1.x + v2.x, 
					v1.y + v2.y, 
					v1.z + v2.z);
		} else {
			target.set(v1.x + v2.x, 
					v1.y+ v2.y, 
					v1.z + v2.z);
		}
		return target;
	}
	
	

	
	/**
	 * Subtract one vector from another and store in another vector
	 * @param target SGVec_3d in which to store the result
	 */
	static public <V extends Vec3d<?>> V sub(V v1, V v2) {
		return sub(v1, v2, (V)null);
	}	
	
	static public <V extends Vec3d<?>> V mult(V  v, double n) {
		return mult(v, n, null);
	}

	static public <V extends Vec3d<?>> V mult(V v, double n, V target) {
		if (target == null) {
			target = (V) v.copy();			
		}
			target.set(v.x*n, v.y*n, v.z*n);
		return target;
	}
	
	static public <V extends Vec3d<?>> V div(V  v, double n) {
		return div(v, n, null);
	}

	static public <V extends Vec3d<?>> V div(V v, double n, V target) {
		if (target == null) {
			target = (V) v.copy();
		} 
			target.set(v.x/n, v.y/n, v.z/n);
		
		return target;
	}

	/**
	 * Subtract v3 from v1 and store in target
	 * @param target SGVec_3d in which to store the result
	 * @return 
	 */
	static public <V extends Vec3d<?>> V sub(V v1, V v2, V target) {
		if (target == null) {
			target = (V) v1.copy();
		} 
			target.set(v1.x - v2.x, 
					v1.y- v2.y, 
					v1.z - v2.z);
		
		return target;
	}
	
	/**
	 * @param v1 any variable of type SGVec_3d
	 * @param v2 any variable of type SGVec_3d
	 * @param target SGVec_3d to store the result
	 */
	public static <V extends Vec3d<?>> V cross(V v1, V v2, V target) {
		double crossX = v1.y * v2.z - v2.y * v1.z;
		double crossY = v1.z * v2.x - v2.z * v1.x;
		double crossZ = v1.x * v2.y - v2.x * v1.y;

		if (target == null) {
			target = (V) v1.copy(); 
		} 
			target.set(crossX, crossY, crossZ);
		
		return target;
	}	
	
	
	/**
	 * Linear interpolate between two vectors (returns a new SGVec_3d object)
	 * @param v1 the vector to start from
	 * @param v2 the vector to lerp to
	 */
	public static <V extends Vec3d> V lerp(V v1, V v2, double amt) {
		V v = (V)v1.copy();
		v.lerp(v2, amt);
		return v;
	}
	
	/**
	 * ( begin auto-generated from SGVec_3d_angleBetween.xml )
	 *
	 * Calculates and returns the angle (in radians) between two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref SGVec_3d:method
	 * @usage web_application
	 * @param v1 the x, y, and z components of a SGVec_3d
	 * @param v2 the x, y, and z components of a SGVec_3d
	 * @brief Calculate and return the angle between two vectors
	 */
	static public double angleBetween(Vec3d<?> v1, Vec3d<?> v2) {

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
			return 0;
		}
		return (double) Math.acos(amt);
	}



	@Override
	public T mult(double scalar) {
		this.x*=scalar; 
		this.y*=scalar;
		this.z*=scalar;
		return (T) this;
	}
	
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public <F extends Vec3f<F>> void adoptValuesOf(F v) {
		setX_(v.getX());
		setY_(v.getY());
		setZ_(v.getZ());
	}
	
	
	/**
	 * should cause this Vector to adopt the xyz values
	 * of the input vector. This method has a default implementation
	 * that simply calls setX_(), setY_(), setZ() but you should override it if your
	 * vector implementation requires more than that. 
	 * @param v
	 */
	public void adoptValuesOf(T v) {
		setX_(v.getX());
		setY_(v.getY());
		setZ_(v.getZ());
	}	
		
	/**
	 * @return the Z component of this vector. 
	 */
	public double getZ() {
		return this.z;
	}

	
	
}
