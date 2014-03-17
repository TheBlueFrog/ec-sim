package com.mike.ethereum.sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Util
{

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

	static byte[] readAll(File f) 
	{
		FileInputStream fis;
		try 
		{
			fis = new FileInputStream (f);
			byte[] buffer = new byte[(int) f.length()];
			fis.read(buffer);
			fis.close();
			return buffer;
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static List<File> filesWithExtension(String dir, String extension) 
	{
		List<File> r = new ArrayList<File>();
		
		File[] v = new File(dir).listFiles().clone();
	
		for (File f : v)
		{
			if (f.getPath().endsWith("." + extension))
			{
				r.add(f);
			}
		}
		return r;
	}

}
