package com.mike.ethereum.sim;

public class Main
{

	public static void main(String[] args)
	{
		LLLCompiler x = new LLLCompiler();

		String[] a = new String[]
		{
 			   "(seq"
		  	 + "  (* 20 (basefee)) (stop)"
		  	 + ")",
			 
		  	   "(seq"
			 + "  (unless (>= (txvalue) (* 20 (basefee))) (stop))"
			 + ")"
		};
		
		for (String s : a)
			x.compileLisp(s, false);
	}

}
