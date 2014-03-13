package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.Address;
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
 			  "(seq"
		  	+ "  (* 20 (basefee)) (stop)"
		  	+ ")",
			 
//		  	  "(seq"
//			+ "  (unless (>= (txvalue) (* 20 (basefee))) (stop))"
//			+ ")",
			 
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
		};
		
		for (String s : a)
		{
			Log.d(TAG, "Compile " + s);
			u256s memory = x.compileLisp(s, false);

			Log.d(TAG, "Disassembles to " + disassemble(memory));
			
			new Executor (memory);
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
		
		public Executor (u256s memory)
		{
			FeeStructure fs = new FeeStructure();
			
			vme.setContract(memory);

			// send a transaction to the contract
			
			Address contract = new Address (new u256(101));
			Address sender = new Address (new u256(102));
			u256 amount = new u256 (1);
			u256s data = new u256s ();
			u256 fees = new u256(0);
			
			vme.setup(contract, sender, amount, data, fs, null, null, 0);
			
			execute(contract, sender, amount, data, fees);
		}
	
		private void execute(Address _myAddress, Address _txSender, u256 _txValue, u256s _txData, u256 totalFee)
		{
			VirtualMachine vm = new VirtualMachine(); 
		
			try
			{
				vm.go(vme, 1000);
			} 
			catch (BadInstructionExeption | StackTooSmall | StepsDoneException e) 
			{
				e.printStackTrace();
			}
		
			totalFee.add(vm.runFee());
		}
	}


}
