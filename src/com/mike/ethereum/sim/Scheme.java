package com.mike.ethereum.sim;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import jscheme.InputPort;


public class Scheme
{
	private static final String TAG = Scheme.class.getSimpleName();

	public static List<String> getSchemeFiles()
	{
		List<String> v = new ArrayList<String>();
		v.add("primitives.scm");
		v.add("basic-logging.scm");
		return v;
	}

	private jscheme.Scheme mScheme;

	private void init()
	{
		// can't do this, it will try to initScheme...
// 		add (Constants.ProcessEventInitScheme);

		List<String> schemeSource = getSchemeFiles();

		String[] v = new String[schemeSource.size()];
//		for (int i = 0; i < schemeSource.size(); ++i)
//			v[i] = DataStream.getDataFile(schemeSource.get(i)).getPath();

		mScheme = new jscheme.Scheme(v);


		// we supply the Scheme code with the event codes of
		// the kinds of events synthesized over there since
		// we want to keep them defined in one place
		//
//		String ss = String.format("(init %d %f %d %d)", 
//				Constants.ActivityEventDuration, 
//				getLowBatteryThreshold(mContext),
//				getPersistedLong (mContext, "DurationStart"),
//				getPersistedLong (mContext, "DurationType"));
//		
//		eval(ss);

		setSchemeVariable("logLowLevelActivity", false);
	}
	
	/** no type checking from here on... */
	private void setSchemeVariable (String variable, Object value)
	{
		jscheme.Environment e = mScheme.getGlobalEnvironment();
		try
		{
			e.set(variable.toLowerCase().intern(), value);
		}
		catch (RuntimeException ex)
		{
			Log.e(TAG, "failed attempt to set Scheme variable " + variable + ".  " + ex.getMessage());
		}
	}

	private void eval(String ss)
	{
		if (mScheme == null)
			init();

		synchronized (mScheme)
		{
			ByteArrayInputStream is = new ByteArrayInputStream(ss.getBytes());
			InputPort ip = new jscheme.InputPort(is);
			try
			{
				Object xx = mScheme.eval(ip.read());
			}
			catch (RuntimeException ex)
    		{
    			Log.e(TAG, ex.getMessage() + " while evaluating " + ss);
    		}
		}
	}

}
