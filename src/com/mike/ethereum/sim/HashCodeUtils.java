package com.mike.ethereum.sim;

import java.math.BigInteger;
import java.util.Date;

/**
 * Collected methods which allow easy implementation of hashCode.
 *
 *
 * Example use case: *
 *
 * public int hashCode() {
 *     int result = HashCodeUtil.SEED;
 *     //collect the contributions of various fields
 *     result = HashCodeUtil.hash(result, fPrimitive);
 *     result = HashCodeUtil.hash(result, fObject);
 *     result = HashCodeUtil.hash(result, fArray);
 *     return result;
 * }
 *
 * @author Wim Tobback
 * @version 1.0
 * @since 1.0
 */
public final class HashCodeUtils {
    public static final int SEED = 47;
    private static final int fODD_PRIME_NUMBER = 37;

//    public static int hash(int seed, Object aObject) {
//        int result = seed;
//
//        if (aObject == null) {
//            result = hash(result, 0);
//        } else if (!isArray(aObject)) {
//            result = hash(result, aObject.hashCode());
//        } else {
//            int length = Array.getLength(aObject);
//
//            for (int idx = 0; idx < length; ++idx) {
//                Object item = Array.get(aObject, idx);
//                result = hash(result, item);
//            }
//        }
//
//        return result;
//    }
//    private static boolean isArray(Object aObject) {
//        return aObject.getClass().isArray();
//    }

    private static int firstTerm(int seed) {
        return fODD_PRIME_NUMBER * seed;
    }

    public static int hash(int seed, boolean aBoolean) {
        return firstTerm(seed) + (aBoolean ? 1 : 0);
    }
    public static int hash(int seed, char aChar) {
        return firstTerm(seed) + aChar;
    }
    public static int hash(int seed, int aInt) {
        return firstTerm(seed) + aInt;
    }
    public static int hash(int seed, long aLong) {
        return firstTerm(seed) + (int) (aLong ^ (aLong >>> 32));
    }
    public static int hash(int seed, float aFloat) {
        return hash(seed, Float.floatToIntBits(aFloat));
    }
    public static int hash(int seed, double aDouble) {
        return hash(seed, Double.doubleToLongBits(aDouble));
    }
    public static int hash(int seed, Date d) {
        return hash(seed, d.getTime());
    }
    public static int hash(int seed, String s)
    {
        if (s == null)
            return hash (seed, 0);
        else
            return s.hashCode();
    }

	public static int hash(int seed, BigInteger b)
	{
		return hash (seed, b.hashCode());
	}
    
    
}
