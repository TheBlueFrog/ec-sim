package com.mike.ethereum.sim;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mike.ethereum.sim.CommonEth.Address;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;

class Account
{
	private static final String TAG = Account.class.getSimpleName();
	
	// all known accounts
	static private List<Account> mAccounts = new ArrayList<>();
	
	/** return the Account of this Address */
	public static Account getFromAddress(Address sender)
	{
		for (Account a : mAccounts)
			if (a.getShortAddress().equals(sender))
				return a;
		
		return null;
	}

	/** return the Account of this full address */
	public static Account getFromAddress(u256 full)
	{
		for (Account a : mAccounts)
			if (a.getFullAddress().equals(full))
				return a;
		
		return null;
	}
	
	static public Account createAccount (u256 fullAddress, u256 balance)
	{
		Account a = new Account (fullAddress, balance);
		mAccounts.add(a);
		return a;
	}
	

	private u256 mFullAddress;
	private u256 mBalance;
	private u256s mProgram = new u256s();
	private Map<u256, u256> mPersistentStorage = new HashMap<u256, u256>();
	
	private Account (u256 address, u256 balance)
	{
		mFullAddress = new u256(address);
		mBalance = new u256(balance);
	}
	private Account(String address, String balance)
	{
		this (new u256(new BigInteger(address)), new u256(new BigInteger(balance)));
	}

	public u256 getBalance()
	{
		return mBalance;
	}

	public void payFee(u256 amt) 
	{
		mBalance = mBalance.subtract(amt);
		
		if (mBalance.lessThan(0))
		{
			Log.e(TAG,  String.format("OOOOPS, balance of %s has gone negative.", mFullAddress.toString()));
		}
	}

	public u256 getFullAddress() 
	{
		return mFullAddress;
	}

	public void setProgram(u256s memory) 
	{
		mProgram = memory;
	}
	public u256s getProgram() 
	{
		return mProgram;
	}

	public Map<u256, u256> getStorage() 
	{
		return mPersistentStorage;
	}
	public void saveStorage(Map<u256, u256> storage)
	{
		mPersistentStorage.clear();
		
		for (Entry<u256, u256> x : storage.entrySet())
		{
			if (x.getKey().greaterThan(mProgram.size()))
			{
				mPersistentStorage.put(x.getKey(), x.getValue());
			}
		}
	}
	
	public Address getAddress ()
	{
		return new Address(this);
	}
	public String getShortAddress()
	{
		return mFullAddress.toString().substring(0, 10) + "...";
	}
}