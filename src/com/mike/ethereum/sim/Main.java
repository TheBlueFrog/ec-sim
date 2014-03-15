package com.mike.ethereum.sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mike.ethereum.sim.CommonEth.Account;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;

public class Main
{
	private static final String TAG = Main.class.getSimpleName();

	static private boolean mLogging = false;
	
	public static void main(String[] args)
	{
		LLLCompiler x = new LLLCompiler(mLogging);
		Executor e = new Executor (true);
		
		// suck up all the contracts that are in the given dir
		// setup an account for them and get the code compiled
		
		List<Account> mContracts = new ArrayList<Account>();
		
		for (File f : getInputs(args[0]))
		{
			byte[] body = readAll(f);
			u256s memory = x.compileLisp(new String(body));

			if (mLogging)
				Log.d(TAG, "Disassembles to\n" + Disassembler.run(memory));			
			
			String address = CryptoUtil.asDecimal(CryptoUtil.SHA256 (f.getPath()));
			
			Account a = new Account (address, "2222");
			mContracts.add(a);
			a.setProgram(memory);

			Log.d(TAG, String.format("Compiled %s into %s",
					f.getPath(),
					a.getShortAddress()));
		}
				
		// trigger event
		
		Account sender = new Account ("111111111111", "1111");

		u256 amount = new u256 (333);
		u256s data = new u256s ();

		for (int i = 0; i < 3; ++i)
		{
			Log.d(TAG, String.format("%s sends %s to contract %s, balance %s", 
					sender.getShortAddress(),
					amount.toString(),
					mContracts.get(0).getShortAddress(),
					mContracts.get(0).getBalance().toString())); 
			
			e.doSendToContract(sender, amount, data, mContracts.get(0));
			
			Log.d(TAG, String.format("Contract %s finished, fees %s, balance %s", 
					mContracts.get(0).getShortAddress(), 
					e.getFees().toString(),
					mContracts.get(0).getBalance().toString()));
		}
	}

	static private byte[] readAll(File f) 
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

	static private List<File> getInputs(String dir) 
	{
		List<File> r = new ArrayList<File>();
		
		File[] v = new File(dir).listFiles().clone();

		for (File f : v)
		{
			if (f.getPath().endsWith(".lll"))
			{
				r.add(f);
			}
		}
		return r;
	}


}
