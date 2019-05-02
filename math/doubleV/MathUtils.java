/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package sceneGraph.math.doubleV;

import java.util.Random;

import sceneGraph.math.RandomXS128;

/** Utility and fast math functions.
 * <p>
 * Thanks to Riven on JavaGaming.org for the basis of sin/cos/floor/ceil.
 * @author Nathan Sweet */
public final class MathUtils {
	static public final double nanoToSec = 1 / 1000000000f;

	// ---
	static public final float FLOAT_ROUNDING_ERROR =   0.000001f; // 32 bits, 23 of which may hold the significand for a precision of 6 digits
	static public final double DOUBLE_ROUNDING_ERROR = 0.000000000000001d; // 64, 52 of which represent the significand for a precision of 15 digits. 
	static public final double PI = Math.PI;
	static public final float PI_f = (float)Math.PI;
	static public final double PI2 = PI * 2d;
	static public final float PI2_f = (float)Math.PI;

	static public final double E = Math.E;

	static private final int SIN_BITS = 14; // 16KB. Adjust for accuracy.
	static private final int SIN_MASK = ~(-1 << SIN_BITS);
	static private final int SIN_COUNT = SIN_MASK + 1;

	static private final double radFull = PI * 2;
	static private final double degFull = 360;
	static private final double radToIndex = SIN_COUNT / radFull;
	static private final double degToIndex = SIN_COUNT / degFull;

	/** multiply by this to convert from radians to degrees */
	static public final double radiansToDegrees = 180f / PI;
	static public final double radDeg = radiansToDegrees;
	/** multiply by this to convert from degrees to radians */
	static public final double degreesToRadians = PI / 180;
	static public final double degRad = degreesToRadians;

	static private class Sin {
		static final double[] table = new double[SIN_COUNT];

		static {
			for (int i = 0; i < SIN_COUNT; i++)
				table[i] = (double)Math.sin((i + 0.5f) / SIN_COUNT * radFull);
			for (int i = 0; i < 360; i += 90)
				table[(int)(i * degToIndex) & SIN_MASK] = (double)Math.sin(i * degreesToRadians);
		}
	}
	
	static private class Sinf {
		static final float[] table = new float[SIN_COUNT];

		static {
			for (int i = 0; i < SIN_COUNT; i++)
				table[i] = (float)Math.sin((i + 0.5f) / SIN_COUNT * radFull);
			for (int i = 0; i < 360; i += 90)
				table[(int)(i * degToIndex) & SIN_MASK] = (float)Math.sin(i * degreesToRadians);
		}
	}

	/** Returns the sine in radians from a lookup table. */
	static public double sin (double radians) {
		return Sin.table[(int)(radians * radToIndex) & SIN_MASK];
	}

	/** Returns the cosine in radians from a lookup table. */
	static public double cos (double radians) {
		return Sin.table[(int)((radians + PI / 2) * radToIndex) & SIN_MASK];
	}

	/** Returns the sine in radians from a lookup table. */
	static public double sinDeg (double degrees) {
		return Sin.table[(int)(degrees * degToIndex) & SIN_MASK];
	}

	/** Returns the cosine in radians from a lookup table. */
	static public double cosDeg (double degrees) {
		return Sin.table[(int)((degrees + 90) * degToIndex) & SIN_MASK];
	}

	// ---

	/** Returns atan2 in radians, faster but less accurate than Math.atan2. Average error of 0.00231 radians (0.1323 degrees),
	 * largest error of 0.00488 radians (0.2796 degrees). */
	static public double atan2 (double y, double x) {
		if (x == 0f) {
			if (y > 0f) return PI / 2;
			if (y == 0f) return 0f;
			return -PI / 2;
		}
		final double atan, z = y / x;
		if (Math.abs(z) < 1f) {
			atan = z / (1f + 0.28f * z * z);
			if (x < 0f) return atan + (y < 0f ? -PI : PI);
			return atan;
		}
		atan = PI / 2 - z / (z * z + 0.28f);
		return y < 0f ? atan - PI : atan;
	}
	
	/** Returns the sine in radians from a lookup table. */
	static public float sin (float radians) {
		return Sinf.table[(int)(radians * radToIndex) & SIN_MASK];
	}

	/** Returns the cosine in radians from a lookup table. */
	static public float cos (float radians) {
		return Sinf.table[(int)((radians + PI / 2) * radToIndex) & SIN_MASK];
	}

	/** Returns the sine in radians from a lookup table. */
	static public float sinDeg (float degrees) {
		return Sinf.table[(int)(degrees * degToIndex) & SIN_MASK];
	}

	/** Returns the cosine in radians from a lookup table. */
	static public float cosDeg (float degrees) {
		return Sinf.table[(int)((degrees + 90) * degToIndex) & SIN_MASK];
	}

	// ---

	/** Returns atan2 in radians, faster but less accurate than Math.atan2. Average error of 0.00231 radians (0.1323 degrees),
	 * largest error of 0.00488 radians (0.2796 degrees). */
	static public float atan2 (float y, float x) {
		if (x == 0f) {
			if (y > 0f) return PI_f / 2;
			if (y == 0f) return 0f;
			return -PI_f / 2;
		}
		final float atan, z = y / x;
		if (Math.abs(z) < 1f) {
			atan = z / (1f + 0.28f * z * z);
			if (x < 0f) return atan + (y < 0f ? -PI_f : PI_f);
			return atan;
		}
		atan = PI_f / 2 - z / (z * z + 0.28f);
		return y < 0f ? atan - PI_f : atan;
	}

	// ---

	static public Random random = new RandomXS128();

	/** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
	static public int random (int range) {
		return random.nextInt(range + 1);
	}

	/** Returns a random number between start (inclusive) and end (inclusive). */
	static public int random (int start, int end) {
		return start + random.nextInt(end - start + 1);
	}

	/** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
	static public long random (long range) {
		return (long)(random.nextDouble() * range);
	}

	/** Returns a random number between start (inclusive) and end (inclusive). */
	static public long random (long start, long end) {
		return start + (long)(random.nextDouble() * (end - start));
	}

	/** Returns a random boolean value. */
	static public boolean randomBoolean () {
		return random.nextBoolean();
	}


	/** Returns -1 or 1, randomly. */
	static public int randomSign () {
		return 1 | (random.nextInt() >> 31);
	}


	// ---

	/** Returns the next power of two. Returns the specified value if the value is already a power of two. */
	static public int nextPowerOfTwo (int value) {
		if (value == 0) return 1;
		value--;
		value |= value >> 1;
		value |= value >> 2;
		value |= value >> 4;
		value |= value >> 8;
		value |= value >> 16;
		return value + 1;
	}

	static public boolean isPowerOfTwo (int value) {
		return value != 0 && (value & value - 1) == 0;
	}

	// ---

	static public short clamp (short value, short min, short max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	static public int clamp (int value, int min, int max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	static public long clamp (long value, long min, long max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	static public float clamp (float value, float min, float max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	static public double clamp (double value, double min, double max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	// ---

	/** Linearly interpolates between fromValue to toValue on progress position. */
	static public double lerp (double fromValue, double toValue, double progress) {
		return fromValue + (toValue - fromValue) * progress;
	}

	/** Linearly interpolates between two angles in radians. Takes into account that angles wrap at two pi and always takes the
	 * direction with the smallest delta angle.
	 * 
	 * @param fromRadians start angle in radians
	 * @param toRadians target angle in radians
	 * @param progress interpolation value in the range [0, 1]
	 * @return the interpolated angle in the range [0, PI2[ */
	public static double lerpAngle (double fromRadians, double toRadians, double progress) {
		double delta = ((toRadians - fromRadians + PI2 + PI) % PI2) - PI;
		return (fromRadians + delta * progress + PI2) % PI2;
	}

	/** Linearly interpolates between two angles in degrees. Takes into account that angles wrap at 360 degrees and always takes
	 * the direction with the smallest delta angle.
	 * 
	 * @param fromDegrees start angle in degrees
	 * @param toDegrees target angle in degrees
	 * @param progress interpolation value in the range [0, 1]
	 * @return the interpolated angle in the range [0, 360[ */
	public static double lerpAngleDeg (double fromDegrees, double toDegrees, double progress) {
		double delta = ((toDegrees - fromDegrees + 360 + 180) % 360) - 180;
		return (fromDegrees + delta * progress + 360) % 360;
	}

	// ---

	static private final int BIG_ENOUGH_INT = 16 * 1024;
	static private final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	static private final double CEIL = 0.9999999;
	static private final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5f;

	/** Returns the largest integer less than or equal to the specified double. This method will only properly floor doubles from
	 * -(2^14) to (double.MAX_VALUE - 2^14). */
	static public int floor (double value) {
		return (int)(value + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
	}

	/** Returns the largest integer less than or equal to the specified double. This method will only properly floor doubles that are
	 * positive. Note this method simply casts the double to int. */
	static public int floorPositive (double value) {
		return (int)value;
	}

	/** Returns the smallest integer greater than or equal to the specified double. This method will only properly ceil doubles from
	 * -(2^14) to (double.MAX_VALUE - 2^14). */
	static public int ceil (double value) {
		return BIG_ENOUGH_INT - (int)(BIG_ENOUGH_FLOOR - value);
	}

	/** Returns the smallest integer greater than or equal to the specified double. This method will only properly ceil doubles that
	 * are positive. */
	static public int ceilPositive (double value) {
		return (int)(value + CEIL);
	}

	/** Returns the closest integer to the specified double. This method will only properly round doubles from -(2^14) to
	 * (double.MAX_VALUE - 2^14). */
	static public int round (double value) {
		return (int)(value + BIG_ENOUGH_ROUND) - BIG_ENOUGH_INT;
	}

	/** Returns the closest integer to the specified double. This method will only properly round doubles that are positive. */
	static public int roundPositive (double value) {
		return (int)(value + 0.5f);
	}

	/** Returns true if the value is zero (using the default tolerance as upper bound) */
	static public boolean isZero (double value) {
		return Math.abs(value) <= DOUBLE_ROUNDING_ERROR;
	}

	/** Returns true if the value is zero.
	 * @param tolerance represent an upper bound below which the value is considered zero. */
	static public boolean isZero (double value, double tolerance) {
		return Math.abs(value) <= tolerance;
	}

	/** Returns true if a is nearly equal to b. The function uses the default doubleing error tolerance.
	 * @param a the first value.
	 * @param b the second value. */
	static public boolean isEqual (double a, double b) {
		return Math.abs(a - b) <= DOUBLE_ROUNDING_ERROR;
	}

	/** Returns true if a is nearly equal to b.
	 * @param a the first value.
	 * @param b the second value.
	 * @param tolerance represent an upper bound below which the two values are considered equal. */
	static public boolean isEqual (double a, double b, double tolerance) {
		return Math.abs(a - b) <= tolerance;
	}

	/** @return the logarithm of value with base a */
	static public double log (double a, double value) {
		return (double)(Math.log(value) / Math.log(a));
	}

	/** @return the logarithm of value with base 2 */
	static public double log2 (double value) {
		return log(2, value);
	}
}
