package math.floatV;

import asj.CanLoad;
import math.doubleV.Vec3d;
import math.floatV.Vec3f;

public abstract class Vec3f<T extends Vec3f<T>> implements Vecf<T>, CanLoad {
		
	private static final long serialVersionUID = 3840054589595372522L;

	/** the x-component of this vector **/
	public float x;
	/** the y-component of this vector **/
	public float y;
	/** the z-component of this vector **/
	public float z;

	public final static int X = 0, Y= 1, Z = 2;


	/** Constructs a vector at (0,0,0) */
	public Vec3f () {
	}

	/** Creates a vector with the given components
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component */
	public Vec3f (float x, float y, float z) {
		this.set(x, y, z);
	}
	

	/** Creates a vector from the given vector
	 * @param vector The vector */
	public Vec3f (T vector) {
		this.set(vector);
	}
	
	/** Creates a vector from the given vector
	 * @param vector The vector */
	public Vec3f (final Vec3d vector) {
		this.set(vector);
	}

	/** Creates a vector from the given array. The array must have at least 3 elements.
	 *
	 * @param values The array */
	public Vec3f (final float[] values) {
		this.set(values[0], values[1], values[2]);
	}


	/** Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public  T set (float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return (T)this;
	}

	public <V extends Vec3d<?>>T set (final V vector) {
		return this.set(this.set((float)vector.x, (float)vector.y, (float)vector.z));
	}
	
	public T set (final Vec3f vector) {
		return this.set(vector.getX(), vector.getY(), vector.getZ());
	}

	/** Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining */
	@Override
	public T set (final float[] values) {
		return this.set(values[0], values[1], values[2]);
	}

	/** Sets the components from the given spherical coordinate
	 * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
	 * @param polarAngle The angle between z-axis in radians [0, pi]
	 * @return This vector for chaining */
	public T setFromSpherical (float azimuthalAngle, float polarAngle) {
		float cosPolar = MathUtils.cos(polarAngle);
		float sinPolar = MathUtils.sin(polarAngle);

		float cosAzim = MathUtils.cos(azimuthalAngle);
		float sinAzim = MathUtils.sin(azimuthalAngle);

		return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
	}


	@Override
	public  <V extends Vecf<?>> T add (V vector) {
		return this.add(vector.getX(), vector.getY(), vector.getZ());
	}
	
	@Override
	public  <V extends Vec3f<?>> T add (V vector) {
		return this.add(vector.getX(), vector.getY(), vector.getZ());
	}
	
	@Override
	public  T add (float[] v) {
		return this.add(v[0], v[1], v[2]);
	}

	/** Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining. */
	public T add (float x, float y, float z) {
		this.x += x; 
		this.y+= y; 
		this.z += z;
		return (T) this;
	}

	/** Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining */
	public T add (float values) {
		return set(this.x + values, this.y + values, this.z + values);
	}

	public T sub (Vec3f<?> a_vec) {
		return  sub(a_vec.x, a_vec.y, a_vec.z);
	}
	
	@Override
	public <V extends Vecf<?>> T sub (V a_vec) {
		return sub(a_vec.getX(), a_vec.getY(), a_vec.getZ());
	}

	/** Subtracts the other vector from this vector.
	 *
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public T sub (float x, float y, float z) {
		return this.set(this.x - x, this.y - y, this.z - z);
	}

	/** Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining */
	public T sub (float value) {
		return this.set(this.x - value, this.y - value, this.z - value);
	}
	
	@Override
	public <V extends Vecf<?>> T mult (V other) {
		return this.set(x * other.getX(), y * other.getY(), z * other.getZ());
	}

	public <V extends Vec3f<?>> T mult (V other) {
		return this.set(x * other.x, y * other.y, z * other.z);
	}

	/** Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining */
	public T mult (float vx, float vy, float vz) {
		return this.set(this.x * vx, this.y * vy, this.z * vz);
	}
	

	@Override
	public T div(float n) {
		x /= n;
		y /= n;
		z /= n;
		return (T) this;
	}

	

	@Override
	public <V extends Vecf<?>> T mulAdd (V vec, float scalar) {
		this.x += vec.getX() * scalar;
		this.y += vec.getY() * scalar;
		this.z += vec.getZ() * scalar;
		return (T) this;
	}
	
	public T mulAdd (Vec3f<?> vec, float scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		this.z += vec.z * scalar;
		return (T)this;
	}

	@Override
	public <V extends Vecf<?>> T mulAdd (V vec, V mulVec) {
		this.x += vec.getX() * mulVec.getX();
		this.y += vec.getY() * mulVec.getY();
		this.z += vec.getZ() * mulVec.getZ();
		return (T) this;
	}
	
	public T mulAdd (Vec3f<?> vec, Vec3f<?> mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		this.z += vec.z * mulVec.z;
		return (T) this;
	}

	/** @return The euclidean length */
	public static float mag (final float x, final float y, final float z) {
		return (float)MathUtils.sqrt(x * x + y * y + z * z);
	}

	@Override
	public float mag () {
		return (float)MathUtils.sqrt(x * x + y * y + z * z);
	}

	/** @return The squared euclidean length */
	public static float magSq (final float x, final float y, final float z) {
		return x * x + y * y + z * z;
	}

	@Override
	public float magSq () {
		return x * x + y * y + z * z;
	}

	/** @param vector The other vector
	 * @return Whether this and the other vector are equal */
	public boolean idt (final Vec3f vector) {
		return x == vector.x && y == vector.y && z == vector.z;
	}

	/** @return The euclidean distance between the two specified vectors */
	public static float dst (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		return (float)MathUtils.sqrt(a * a + b * b + c * c);
	}

	@Override
	public float dist (final Vec3f vector) {
		final float a = vector.x - x;
		final float b = vector.y - y;
		final float c = vector.z - z;
		return (float)MathUtils.sqrt(a * a + b * b + c * c);
	}

	/** @return the distance between this point and the given point */
	public float dst (float x, float y, float z) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		return (float)MathUtils.sqrt(a * a + b * b + c * c);
	}

	/** @return the squared distance between the given points */
	public static float dst2 (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		return a * a + b * b + c * c;
	}

	@Override
	public float distSq (Vec3f point) {
		final float a = point.x - x;
		final float b = point.y - y;
		final float c = point.z - z;
		return a * a + b * b + c * c;
	}

	/** Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance */
	public float dst2 (float x, float y, float z) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		return a * a + b * b + c * c;
	}

	@Override
	public T normalize () {
		final float len2 = this.mag();
		if (len2 == 0f || len2 == 1f) return (T) this;
		return this.mult(1f / (float)len2);
	}

	/** @return The dot product between the two vectors */
	public static float dot (float x1, float y1, float z1, float x2, float y2, float z2) {
		return x1 * x2 + y1 * y2 + z1 * z2;
	}

	@Override
	public <V extends Vecf<?>> float dot (final V vector) {
		return x * vector.getX() + y * vector.getY() + z * vector.getZ();
	}
	
	public <V extends Vec3f<?>> float dot (final V vector) {
		return x * vector.x + y * vector.y + z * vector.z;
	}

	/** Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product */
	public float dot (float x, float y, float z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	@Override
	public <V extends Vecf<?>> T crs (final V vector) {
		return this.set(y * vector.getZ() - z * vector.getY(), z * vector.getX() - x * vector.getZ(), x * vector.getY() - y * vector.getX());
	}

	
	/** Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining */
	public <V extends Vec3f<?>> T crs (final V vector) {
		return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
	}
	
	/** Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining */
	public Vec3f crs (float x, float y, float z) {
		return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/** Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
	 * rotation and scale plus a 1x3 matrix representing the translation.
	 * @param matrix The matrix
	 * @return This vector for chaining */
	public T mul4x3 (float[] matrix) {
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
	public Vec3f getPlaneProjectionOf(T p1, T p2) {
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
	public boolean isUnit (final float margin) {
		return MathUtils.abs(magSq() - 1f) < margin;
	}

	@Override
	public boolean isZero () {
		return x == 0 && y == 0 && z == 0;
	}

	@Override
	public boolean isZero (final float margin) {
		return magSq() < margin;
	}

	@Override
	public boolean isOnLine (Vec3f other, float epsilon) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon;
	}

	@Override
	public boolean isOnLine (Vec3f other) {
		return magSq(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.DOUBLE_ROUNDING_ERROR;
	}

	@Override
	public boolean isCollinear (Vec3f other, float epsilon) {
		return isOnLine(other, epsilon) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinear (Vec3f other) {
		return isOnLine(other) && hasSameDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (Vec3f other, float epsilon) {
		return isOnLine(other, epsilon) && hasOppositeDirection(other);
	}

	@Override
	public boolean isCollinearOpposite (Vec3f other) {
		return isOnLine(other) && hasOppositeDirection(other);
	}

	@Override
	public boolean isPerpendicular (Vec3f vector) {
		return MathUtils.isZero(dot(vector));
	}

	@Override
	public boolean isPerpendicular (Vec3f vector, float epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	@Override
	public boolean hasSameDirection (Vec3f vector) {
		return dot(vector) > 0;
	}

	@Override
	public boolean hasOppositeDirection (Vec3f vector) {
		return dot(vector) < 0;
	}

	@Override
	public T lerp (final Vec3f target, float alpha) {
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
	public Vec3f slerp (final Vec3f target, float alpha) {
		final float dot = dot(target);
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

		// theta0 = angle between input vectors
		final float theta0 = (float)MathUtils.acos(dot);
		// theta = angle between this vector and result
		final float theta = theta0 * alpha;

		final float st = (float)MathUtils.sin(theta);
		final float tx = target.x - x * dot;
		final float ty = target.y - y * dot;
		final float tz = target.z - z * dot;
		final float l2 = tx * tx + ty * ty + tz * tz;
		final float dl = st * ((l2 < 0.0001f) ? 1f : 1f / (float)MathUtils.sqrt(l2));

		return mult((float)MathUtils.cos(theta)).add(tx * dl, ty * dl, tz * dl).normalize();
	}

	/** Converts this {@code Vector3} to a string in the format {@code (x,y,z)}.
	 * @return a string representation of this object. */
	@Override
	public String toString () {
		return "(" +(float) x + "," + (float)y + "," + (float)z + ")";
	}


	@Override
	public T limit (float limit) {
		return limitSq(limit * limit);
	}

	@Override
	public T limitSq (float limit2) {
		float len2 = magSq();
		if (len2 > limit2) {
			mult((float)MathUtils.sqrt(limit2 / len2));
		}
		return (T) this;
	}

	@Override
	public T setMag (float len) {
		return setMagSq(len * len);
	}

	@Override
	public T setMagSq (float len2) {
		float oldLen2 = magSq();
		return (oldLen2 == 0 || oldLen2 == len2) ? (T) this : mult((float)MathUtils.sqrt(len2 / oldLen2));
	}

	@Override
	public T clamp (float min, float max) {
		final float len2 = magSq();
		if (len2 == 0f) return (T) this;
		float max2 = max * max;
		if (len2 > max2) return mult((float)MathUtils.sqrt(max2 / len2));
		float min2 = min * min;
		if (len2 < min2) return mult((float)MathUtils.sqrt(min2 / len2));
		return (T) this;
	}

	public <V extends Vec3f<?>> boolean epsilonEquals (V other, float epsilon) {
		if (other == null) return false;
		if (MathUtils.abs(other.x - x) > epsilon) return false;
		if (MathUtils.abs(other.y - y) > epsilon) return false;
		if (MathUtils.abs(other.z - z) > epsilon) return false;
		return true;
	}

	@Override
	public <V extends Vecf<?>> boolean epsilonEquals (V other, float epsilon) {
		if (other == null) return false;
		if (MathUtils.abs(other.getX() - x) > epsilon) return false;
		if (MathUtils.abs(other.getY() - y) > epsilon) return false;
		if (MathUtils.abs(other.getZ() - z) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (float x, float y, float z, float epsilon) {
		if (MathUtils.abs(x - this.x) > epsilon) return false;
		if (MathUtils.abs(y - this.y) > epsilon) return false;
		if (MathUtils.abs(z - this.z) > epsilon) return false;
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
	public float getX() {
		return this.x;
	}

	@Override
	public float getY() {
		return this.y; 
	}


	@Override
	public void setX_(float x) {
		this.x = x;		
	}

	@Override
	public void setY_(float y) {
		this.y = y;
		
	}
	/**
	 * sets this vector's Z componen to the input value
	 * @param Z
	 */
	public void setZ_(float z) {
		this.z = z;
	}
	
	
	public T getOrthogonal() {
		T result = this.copy();				
		result.set(0,0,0);
		float threshold = this.mag() * 0.6f;
		if(threshold > 0) {
			if (MathUtils.abs(x) <= threshold) {
				float inverse  = 1 / MathUtils.sqrt(y * y + z * z);
				return result.set(0, inverse * z, -inverse * y);
			} else if (MathUtils.abs(y) <= threshold) {
				float inverse  = 1 / MathUtils.sqrt(x * x + z * z);
				return result.set(-inverse * z, 0, inverse * x);
			}
			float inverse  = 1 / MathUtils.sqrt(x * x + y * y);
			return result.set(inverse * y, -inverse * x, 0);
		}

		return result; 
	}

	
	
	public static float dot(SGVec_3f u, SGVec_3f v) {
		return u.dot(v);
	}
	
	public static <V extends Vec3f<?>> V add(V v1, V v2) {
		return add(v1, v2, null);
	}
	
	public static <V extends Vec3f<?>> V add(V v1, V v2, V target) {
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
	 * @param target SGVec_3f in which to store the result
	 */
	static public <V extends Vec3f<?>> V sub(V v1, V v2) {
		return sub(v1, v2, (V)null);
	}	
	
	static public <V extends Vec3f<?>> V mult(V  v, float n) {
		return mult(v, n, null);
	}

	static public <V extends Vec3f<?>> V mult(V v, float n, V target) {
		if (target == null) {
			target = (V) v.copy();			
		}
			target.set(v.x*n, v.y*n, v.z*n);
		return target;
	}
	
	static public <V extends Vec3f<?>> V div(V  v, float n) {
		return div(v, n, null);
	}

	static public <V extends Vec3f<?>> V div(V v, float n, V target) {
		if (target == null) {
			target = (V) v.copy();
		} 
			target.set(v.x/n, v.y/n, v.z/n);
		
		return target;
	}

	/**
	 * Subtract v3 from v1 and store in target
	 * @param target SGVec_3f in which to store the result
	 * @return 
	 */
	static public <V extends Vec3f<?>> V sub(V v1, V v2, V target) {
		if (target == null) {
			target = (V) v1.copy();
		} 
			target.set(v1.x - v2.x, 
					v1.y- v2.y, 
					v1.z - v2.z);
		
		return target;
	}
	
	/**
	 * @param v1 any variable of type SGVec_3f
	 * @param v2 any variable of type SGVec_3f
	 * @param target SGVec_3f to store the result
	 */
	public static <V extends Vec3f<?>> V cross(V v1, V v2, V target) {
		float crossX = v1.y * v2.z - v2.y * v1.z;
		float crossY = v1.z * v2.x - v2.z * v1.x;
		float crossZ = v1.x * v2.y - v2.x * v1.y;

		if (target == null) {
			target = (V) v1.copy(); 
		} 
			target.set(crossX, crossY, crossZ);
		
		return target;
	}	
	
	
	/**
	 * Linear interpolate between two vectors (returns a new SGVec_3f object)
	 * @param v1 the vector to start from
	 * @param v2 the vector to lerp to
	 */
	public static <V extends Vec3f> V lerp(V v1, V v2, float amt) {
		V v = (V)v1.copy();
		v.lerp(v2, amt);
		return v;
	}
	
	/**
	 * ( begin auto-generated from SGVec_3f_angleBetween.xml )
	 *
	 * Calculates and returns the angle (in radians) between two vectors.
	 *
	 * ( end auto-generated )
	 *
	 * @webref SGVec_3f:method
	 * @usage web_application
	 * @param v1 the x, y, and z components of a SGVec_3f
	 * @param v2 the x, y, and z components of a SGVec_3f
	 * @brief Calculate and return the angle between two vectors
	 */
	static public float angleBetween(Vec3f<?> v1, Vec3f<?> v2) {

		// We get NaN if we pass in a zero vector which can cause problems
		// Zero seems like a reasonable angle between a (0,0,0) vector and something else
		if (v1.x == 0 && v1.y == 0 && v1.z == 0 ) return 0.0f;
		if (v2.x == 0 && v2.y == 0 && v2.z == 0 ) return 0.0f;

		float dot = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
		float v1mag = MathUtils.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
		float v2mag = MathUtils.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);
		// This should be a number between -1 and 1, since it's "normalized"
		float amt = dot / (v1mag * v2mag);
		// But if it's not due to rounding error, then we need to fix it
		// http://code.google.com/p/processing/issues/detail?id=340
		// Otherwise if outside the range, acos() will return NaN
		// http://www.cppreference.com/wiki/c/math/acos
		if (amt <= -1) {
			return MathUtils.PI;
		} else if (amt >= 1) {
			return 0;
		}
		return (float) MathUtils.acos(amt);
	}



	@Override
	public T mult(float scalar) {
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
	public <F extends Vec3d<F>> void adoptValuesOf(F v) {
		setX_((float)v.getX());
		setY_((float)v.getY());
		setZ_((float)v.getZ());
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
	public float getZ() {
		return this.z;
	}

	
	
}
