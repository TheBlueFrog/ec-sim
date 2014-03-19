package com.mike.ethereum.sim;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;

public class Main
{
	private static final String TAG = Main.class.getSimpleName();

	static private boolean mLogging = true;
	
	public static void main(String[] args)
	{
		LLLCompiler x = new LLLCompiler(false);
		Executor e = new Executor (true);
		
		// suck up all the contracts that are in the given dir
		// setup an account for them and get the code compiled
		
		List<Account> mContracts = new ArrayList<Account>();
		
		for (File f : Util.filesWithExtension(args[0], "lll"))
		{
			byte[] body = Util.readAll(f);
			u256s memory = x.compileLisp(new String(body));

			if (mLogging)
				Log.d(TAG, "Disassembles to\n" + Disassembler.run(memory));			
			
			u256 address = new u256(Util.asDecimal(CryptoUtil.SHA256 (f.getPath())));
			u256 balance = new u256("2222");
			Account a = Account.createAccount(address, balance);
			mContracts.add(a);
			a.setProgram(memory);

			Log.d(TAG, String.format("Compiled %s into %s",
					f.getPath(),
					a.getShortAddress()));
		}
				
		// trigger event, specific to example.lll
		
		Account sender = Account.createAccount (new u256("111111111111"), new u256("1111"));

		{
			Transaction t = new Transaction (
					sender, 
					mContracts.get(0), 
					new u256 (333), 
					new u256s ());
	
			// make 3 deposits
			
			for (int i = 0; i < 3; ++i)
			{
				Log.d(TAG, String.format("%s deposits %s to contract (%s, balance %s)", 
						sender.getShortAddress(),
						t.getAmount().toString(),
						mContracts.get(0).getShortAddress(),
						mContracts.get(0).getBalance().toString())); 
				
				e.doToContractTransaction(t);
				
				Log.d(TAG, String.format("Contract %s finished, fees %s, balance %s", 
						mContracts.get(0).getShortAddress(), 
						e.getFees().toString(),
						mContracts.get(0).getBalance().toString()));
			}
		}
		
		{
			/*
			Finally, we'll make a withdrawal of fund back to our account. Let's 
			assume we want to take one ether back. All we must do is send a 
			transaction to the contract making sure to pay the charge of 135 
			times the basefee (100 szabo), and specify the amount to withdraw 
			as a single item in the Data. So change the Amount so it reads "13500 szabo" 	
			*/
			Log.d(TAG, String.format("%s withdraws %s from contract (%s, balance %s)", 
					sender.getShortAddress(),
					100,
					mContracts.get(0).getShortAddress(),
					mContracts.get(0).getBalance().toString())); 
	
			u256s data = new u256s ();
			data.add(new u256(97));
			
			for (int i = 0; i < 2; ++i)
			{
				Transaction t = new Transaction (
						sender, 
						mContracts.get(0), 
						new u256 (e.mFeeStructure.getBaseFee().mult(135L)), 
						data);
		
				e.doToContractTransaction(t);
		
				Log.d(TAG, String.format("Contract %s finished, fees %s, balance %s", 
						mContracts.get(0).getShortAddress(), 
						e.getFees().toString(),
						mContracts.get(0).getBalance().toString()));
			}
		}
	}
}
