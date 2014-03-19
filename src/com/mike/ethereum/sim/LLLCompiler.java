package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.u256s;

/** wrapper for the compiler itself */

public class LLLCompiler
{
	private static final String TAG = LLLCompiler.class.getSimpleName();

	private boolean mLogging = false;
	
	public LLLCompiler (boolean logging)
	{		
		mLogging = logging;
	}
	
	private Frame mRoot;
	
	public u256s compileLisp(String _code)
	{
		mRoot = new Frame(_code, null);
		mRoot.setLogging (mLogging);
		
		mRoot.compileLispFragment();
		mRoot.outputChildren();
		
		return mRoot.getCompiledCode();
	}
	
}
