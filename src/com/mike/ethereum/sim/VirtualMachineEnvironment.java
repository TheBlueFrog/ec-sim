package com.mike.ethereum.sim;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mike.ethereum.sim.CommonEth.Account;
import com.mike.ethereum.sim.CommonEth.Address;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;


public class VirtualMachineEnvironment 
{
	private static final String TAG = VirtualMachineEnvironment.class.getSimpleName();


	private class BlockInfo
	{
	}

	public VirtualMachineEnvironment()
	{
	}
	
	private int mProgramSize = 0;
	
	/** load the contract into memory starting at address zero
	 *  and then recover the persistent storage and load
	 *  that also
	 */
	public void setContract (Account contract)
	{
		mStorage.clear();
		
		u256s program = contract.getProgram();
		
		for (int i = 0; i < program.size(); ++i)
			mStorage.put(new u256(i), program.get(i));
		
		mProgramSize = program.size();
		
		Map<u256, u256> st = contract.getStorage ();
		for (u256 s : st.keySet())
		{
			mStorage.put(s, st.get(s));
		}
	}

	public int getContractSize ()
	{
		return mStorage.size();
	}
	public Map<u256, u256> getStorage() 
	{
		return mStorage;
	}

	public void setup(
			Account _myAddress, 
			Account _txSender, 
			u256 _txValue, 
			u256s _txData, 
			FeeStructure _fees, 
			BlockInfo _previousBlock, 
			BlockInfo _currentBlock, 
			long _currentNumber)
	{
		myAddress = _myAddress;
		txSender = _txSender;
		txValue = _txValue;
		txData = _txData;
		fees = _fees;
//		previousBlock(_previousBlock),
//		currentBlock(_currentBlock),
//		currentNumber(_currentNumber)
	}

	private Map<u256, u256> mStorage = new HashMap<u256, u256>();
	
	/**
	 * where we keep the running program
	 * and persisted state for the contract
	 * 
	 * @param address
	 * @return	value at the address or zero if not set
	 */
	public u256 getStore(u256 address) 
	{
		if (mStorage.containsKey(address))
			return mStorage.get(address);
		else
			return new u256(0);
	}
	
	void setStore(u256 _n, u256 _v)
	{
		mStorage.put(_n,  _v);
	}
	
//	void mktx(Transaction& _t) {}
//	u256 txCount(Address _a) { return 0; }
//	u256 extroPrice(Address _a) { return 0; }

	Account myAddress;
	Account txSender;
	u256 txValue;
	u256s txData;
	FeeStructure fees;
//	BlockInfo previousBlock;					///< The current block's information.
//	BlockInfo currentBlock;					///< The current block's information.
//	uint currentNumber;

	public u256 extro(Address contractAddress, u256 x) 
	{
		assert false : "NYI";
		return new u256(0);
	}

	public u256 balance(Address asAddress)
	{
		assert false : "NYI";
		return new u256(0);
	}

	public void suicide(Address dest) 
	{
		assert false : "NYI";
	}

	public void payFee(u256 amt) 
	{
		myAddress.payFee(amt);
	}

	/** dump storage but only above the program itself */
	public void dumpStorage()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Storage (above program itself (0..%d)\n", mProgramSize));
		sb.append(String.format("%-35s %s\n", "Address", "Value"));
		
		for (Entry<u256, u256> x : mStorage.entrySet())
		{
			if (x.getKey().greaterThan(mProgramSize))
			{
				sb.append (String.format("%-35s%s\n",
						x.getKey().toString(),
						x.getValue().toString()));
			}
		}
		
		Log.d(TAG, sb.toString());
	}


}
