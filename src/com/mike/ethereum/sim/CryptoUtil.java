package com.mike.ethereum.sim;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtil
{

	public static byte[] SHA256(String s)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(s.getBytes("UTF-8"));
			return hash;
		}
		catch (NoSuchAlgorithmException e)
		{
			
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	
	static public String asHex(byte[] b)
	{
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < b.length; i++) 
        {
            String hex = Integer.toHexString(0xff & b[i]);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
	}
	static public String asDecimal(byte[] b)
	{
		BigInteger x = new BigInteger(b);
		return x.toString();
	}
}
