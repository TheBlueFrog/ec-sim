package com.mike.ethereum.sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.mike.ethereum.sim.CommonEth.Account;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;

public class Main
{
	private static final String TAG = Main.class.getSimpleName();

	public static void main(String[] args)
	{
		LLLCompiler x = new LLLCompiler();
		Executor e = new Executor ();
		
		for (File f : getInputs(args[0]))
		{
			Log.d(TAG, "Compile " + f.getPath());
			
			byte[] body = readAll(f);
			u256s memory = x.compileLisp(new String(body), false);

			Log.d(TAG, "Disassembles to\n" + Disassembler.run(memory));			
			
			Account contract = new Account (new u256(new BigInteger("22222222")), new u256(new BigInteger("22222222")));
			contract.setProgram(memory);
			
			Account sender = new Account (new u256(new BigInteger("11112222")), new u256(new BigInteger("11112222")));

			u256 amount = new u256 (333);
			u256s data = new u256s ();

			for (int i = 0; i < 3; ++i)
			{
				Log.d(TAG, String.format("%s sends %s to contract %s, balance %s", 
						sender.getAddress().toString(),
						amount.toString(),
						contract.getAddress().toString(),
						contract.getBalance().toString())); 
				
				e.doSendToContract(sender, amount, data, contract);
				
				Log.d(TAG, String.format("Contract %s finished, fees %s, balance %s", 
						contract.getAddress().toString(), 
						e.getFees().toString(),
						contract.getBalance().toString()));
			}
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

	static private class Executor 
	{
		VirtualMachineEnvironment vme = new VirtualMachineEnvironment();
		FeeStructure mFeeStructure = new FeeStructure();
		
		u256 mFees = new u256(0);
		
		public Executor ()
		{
		}

		public void doSendToContract (Account sender, u256 amount, u256s data, Account contract)
		{
			vme.setContract(contract);

			vme.setup(contract, sender, amount, data, mFeeStructure, null, null, 0);
			
			execute(contract, sender, amount, data);

			contract.saveStorage(vme.getStorage());
			vme.dumpStorage();
		}
	
		public u256 getFees()
		{
			return mFees;
		}

		private void execute(Account _myAddress, Account _txSender, u256 _txValue, u256s _txData)
		{
			VirtualMachine vm = new VirtualMachine(); 
		
			try
			{
				vm.go(vme, 10000);
			} 
			catch (BadInstructionExeption | StackTooSmall | StepsDoneException | StackUnderflowException e) 
			{
				e.printStackTrace();
			}
		
			mFees.add(vm.runFee());
		}
	}


}
