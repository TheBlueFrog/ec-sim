package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.u256;

class Executor 
{
	private static final String TAG = Executor.class.getSimpleName();

	VirtualMachineEnvironment vme = new VirtualMachineEnvironment();
	FeeStructure mFeeStructure = new FeeStructure();
	
	u256 mFees = new u256(0);

	private boolean mLogging = true;
	
	public Executor (boolean logging)
	{
		mLogging  = logging;
	}

	public void doToContractTransaction (Transaction t)
	{
		vme.setContract(t.getReceiver());

		vme.setup(t, mFeeStructure, null, null, 0);
		
		execute();//contract, sender, amount, data);

		t.getReceiver().saveStorage(vme.getStorage());
		vme.dumpStorage();
	}

	public u256 getFees()
	{
		return mFees;
	}

	private void execute()//Account _myAddress, Account _txSender, u256 _txValue, u256s _txData)
	{
		VirtualMachine vm = new VirtualMachine(false); 
	
		try
		{
			vm.go(vme, 10000);
		} 
		catch (   BadInstructionExeption 
				| StackTooSmallException 
				| StepsDoneException 
				| StackUnderflowException 
				| OperandOutOfRangeException e) 
		{
			e.printStackTrace();
		}
	
		mFees.add(vm.runFee());
	}
}