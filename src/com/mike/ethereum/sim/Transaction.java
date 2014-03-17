package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.Address;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;

public class Transaction
{
	private Account mSender;
	private Account mReceiver;
	private u256 mAmount;
	private u256s mData = new u256s();

	public Transaction(Address sender, Address receiver, u256 amount)
	{
		mSender = Account.getFromAddress(sender);
		mReceiver = Account.getFromAddress(receiver);
		mAmount = amount;
	}
	public Transaction(Address sender, Address receiver, u256 amount, u256s data)
	{
		mSender = Account.getFromAddress(sender);
		mReceiver = Account.getFromAddress(receiver);
		mAmount = amount;
		mData = data;
	}

	public Transaction(Account sender, Account receiver, u256 amount, u256s data)
	{
		mSender = sender;
		mReceiver = receiver;
		mAmount = amount;
		mData = data;
	}

	public void addData(u256 d)
	{
		mData.add (d);
	}

	public Account getSender()
	{
		return mSender;
	}
	public Account getReceiver()
	{
		return mReceiver;
	}
	public u256 getAmount()
	{
		return mAmount;
	}
	public u256s getData()
	{
		return mData;
	}

}
