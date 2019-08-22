package sceneGraph.numerical;


public class Precision {

	public static final double SAFE_MIN_DOUBLE;
    private static final long EXPONENT_OFFSET_DOUBLE = 1023l;
    public static final double EPSILON_DOUBLE;
	
    public static final double SAFE_MIN_FLOAT;
    private static final long EXPONENT_OFFSET_FLOAT = 127;
    public static final double EPSILON_FLOAT;
    
	static {
        EPSILON_DOUBLE = Double.longBitsToDouble((EXPONENT_OFFSET_DOUBLE - 53l) << 52);
        SAFE_MIN_DOUBLE = Double.longBitsToDouble((EXPONENT_OFFSET_DOUBLE - 1022l) << 52);
        
        
        EPSILON_FLOAT = Double.longBitsToDouble((EXPONENT_OFFSET_FLOAT - 24l) << 23);
        SAFE_MIN_FLOAT = Double.longBitsToDouble((EXPONENT_OFFSET_FLOAT - 126) << 23);
    }
	
	 public static int compareTo(double x, double y, double eps) {
        if (equals(x, y, eps)) {
            return 0;
        } else if (x < y) {
            return -1;
        }
        return 1;
    }

    /**
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)}
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     *
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * @return <ul><li>0 if  {@link #equals(double, double, int) equals(x, y, maxUlps)}</li>
     *       <li>&lt; 0 if !{@link #equals(double, double, int) equals(x, y, maxUlps)} &amp;&amp; x &lt; y</li>
     *       <li>> 0 if !{@link #equals(double, double, int) equals(x, y, maxUlps)} &amp;&amp; x > y</li></ul>
     */
    public static int compareTo(final double x, final double y, final int maxUlps) {
        if (equals(x, y, maxUlps)) {
            return 0;
        } else if (x < y) {
            return -1;
        }
        return 1;
    }
    
    /**
        * Returns true iff they are equal as defined by
        * {@link #equals(float,float,int) equals(x, y, 1)}.
        *
        * @param x first value
        * @param y second value
        * @return {@code true} if the values are equal.
        */
       public static boolean equals(float x, float y) {
           return equals(x, y, 1);
       }
    
       /**
        * Returns true if both arguments are NaN or neither is NaN and they are
        * equal as defined by {@link #equals(float,float) equals(x, y, 1)}.
        *
        * @param x first value
        * @param y second value
        * @return {@code true} if the values are equal or both are NaN.
        * @since 2.2
        */
       public static boolean equalsIncludingNaN(float x, float y) {
           return (Float.isNaN(x) && Float.isNaN(y)) || equals(x, y, 1);
       }
    
       /**
        * Returns true if both arguments are equal or within the range of allowed
        * error (inclusive).
        *
        * @param x first value
        * @param y second value
        * @param eps the amount of absolute error to allow.
        * @return {@code true} if the values are equal or within range of each other.
        * @since 2.2
        */
       public static boolean equals(float x, float y, float eps) {
           return equals(x, y, 1) || Math.abs(y - x) <= eps;
       } 
  
        /**
        * Returns true if both arguments are NaN or if they are equal as defined
        * by {@link #equals(float,float,int) equals(x, y, maxUlps)}.
        *
        * @param x first value
        * @param y second value
        * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
        * values between {@code x} and {@code y}.
        * @return {@code true} if both arguments are NaN or if there are less than
        * {@code maxUlps} floating point values between {@code x} and {@code y}.
        * @since 2.2
        */
       public static boolean equalsIncludingNaN(float x, float y, int maxUlps) {
           return (Float.isNaN(x) && Float.isNaN(y)) || equals(x, y, maxUlps);
       }
    
       /**
        * Returns true iff they are equal as defined by
        * {@link #equals(double,double,int) equals(x, y, 1)}.
        *
        * @param x first value
        * @param y second value
        * @return {@code true} if the values are equal.
        */
       public static boolean equals(double x, double y) {
           return equals(x, y, 1);
       }
    
       /**
        * Returns true if both arguments are NaN or neither is NaN and they are
        * equal as defined by {@link #equals(double,double) equals(x, y, 1)}.
        *
        * @param x first value
        * @param y second value
        * @return {@code true} if the values are equal or both are NaN.
        * @since 2.2
        */
       public static boolean equalsIncludingNaN(double x, double y) {
           return (Double.isNaN(x) && Double.isNaN(y)) || equals(x, y, 1);
       }
    
       /**
        * Returns {@code true} if there is no double value strictly between the
        * arguments or the difference between them is within the range of allowed
        * error (inclusive).
        *
        * @param x First value.
        * @param y Second value.
        * @param eps Amount of allowed absolute error.
        * @return {@code true} if the values are two adjacent floating point
        * numbers or they are within range of each other.
        */
       public static boolean equals(double x, double y, double eps) {
           return equals(x, y, 1) || Math.abs(y - x) <= eps;
       }
    
	
	
	public static class MathArithmeticException extends Exception {
		
	}
	
public static class MathIllegalArgumentException extends Exception {
		
	}

public static class ZeroException extends Exception {
	
}

public static class LocalizedFormats extends Exception {

	public static final String ZERO_NORM_FOR_ROTATION_AXIS = "0";
	
}


public static class CardanEulerSingularityException extends Exception {
	
}

public static class NotARotationMatrixException extends Exception {
	
}
	
}
