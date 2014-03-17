package com.mike.ethereum.sim;

public class Log
{
	static public void d (String tag, String msg)
	{
		System.out.println(String.format("%-15s  %s", tag, msg));
	}
	static public void e (String tag, String msg)
	{
		System.out.println(String.format("%-15s  ERROR %s", tag, msg));
	}
}
