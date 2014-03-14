package com.mike.ethereum.sim;

import java.math.BigInteger;

import com.mike.ethereum.sim.CommonEth.Account;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;
import com.mike.ethereum.sim.InstructionSet.OpCode;

public class Main
{
	private static final String TAG = Main.class.getSimpleName();

	public static void main(String[] args)
	{
		LLLCompiler x = new LLLCompiler();

		String[] a = new String[]
		{
// 			  "(seq"
//		  	+ "  (* 20 (basefee)) (stop)"
//		  	+ ")",
			 
//	ok		  "(* 10 13)",
//	ok		  "(unless (7) (stop))",
//  ok			"(sload (txsender))",
				
			  "  (unless (>= (txvalue) (* 20 (basefee))) (stop))"
			,
			 
		  	  "(seq"
			+ "  (unless (>= (txvalue) (* 20 (basefee))) (stop))"
			+ ")",
			 
			  "(seq"
			+ "  (unless (>= (txvalue) (* 20 (basefee))) (stop))"
			+ "  (sstore"
			+ "    (txsender)"
			+ "    (+"
			+ "      (sload (txsender))"
			+ "		 (- (txvalue) (* 20 (basefee)))"
			+ "	   )"
			+ "  )"
			+ ")",
			
"			not being parsed properly",
		};
		
		for (String s : a)
		{
			Log.d(TAG, "Compile " + s);
			u256s memory = x.compileLisp(s, false);

			Log.d(TAG, "Disassembles to " + disassemble(memory));
			
			Account contract = new Account (new u256(new BigInteger("22222222")), new u256(new BigInteger("22222222")));
			Account sender = new Account (new u256(new BigInteger("11112222")), new u256(new BigInteger("11112222")));

			u256 amount = new u256 (333);
			u256s data = new u256s ();
			
			Log.d(TAG, String.format("%s sends %s to contract %s, balance %s", 
					sender.getAddress().toString(),
					amount.toString(),
					contract.getAddress().toString(),
					contract.getBalance().toString())); 
			
			Executor e = new Executor (sender, amount, data, contract, memory);
			
			Log.d(TAG, String.format("Contract %s finished, fees %s, balance %s", 
					contract.getAddress().toString(), 
					e.getFees().toString(),
					contract.getBalance().toString()));
		}
	}
	
	static String disassemble(u256s memory)
	{
		StringBuilder sb = new StringBuilder();
		int numerics = 0;
		for (u256 it : memory.getList())
		{
			OpCode iit = InstructionSet.OpCode.parse(it.mValue);
			if ((numerics > 0) || (iit == null) )//|| (u256)(uint)iit->first != n)	// not an instruction or expecting an argument...
			{
				if (numerics > 0)
					numerics--;
				
				sb.append("0x")
					.append(it.mValue.toString(16))
					.append(" ");
			}
			else
			{
				InstructionSet.Info info = InstructionSet.c_instructionInfo.get(iit);
				sb.append(info.name).append(" ");
				numerics = info.additional;
			}
		}
		return sb.toString();
	}

	static private class Executor 
	{
		VirtualMachineEnvironment vme = new VirtualMachineEnvironment();
		u256 mFees = new u256(0);
		
		public Executor (Account sender, u256 amount, u256s data, Account contract, u256s memory)
		{
			FeeStructure fs = new FeeStructure();
			
			vme.setContract(memory);

			// send a transaction to the contract
			
			vme.setup(contract, sender, amount, data, fs, null, null, 0);
			
			execute(contract, sender, amount, data);
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
			catch (BadInstructionExeption | StackTooSmall | StepsDoneException e) 
			{
				e.printStackTrace();
			}
		
			mFees.add(vm.runFee());
		}
	}


}
