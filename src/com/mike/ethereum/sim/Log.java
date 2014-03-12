package com.mike.ethereum.sim;

public class Log
{
	static public void d (String tag, String msg)
	{
		System.out.println(String.format("%30s  %s", tag, msg));
	}
	static public void e (String tag, String msg)
	{
		System.out.println(String.format("%30s  ERROR %s", tag, msg));
	}
}
