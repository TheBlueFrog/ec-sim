package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.u256;


public class VirtualMachineEnvironment 
{
//	ExtVMFace(FeeStructure const& _fees, BlockInfo const& _previousBlock, BlockInfo const& _currentBlock, uint _currentNumber):
//		fees(_fees),
//		previousBlock(_previousBlock),
//		currentBlock(_currentBlock),
//		currentNumber(_currentNumber)
//	{}
//
//	ExtVMFace(Address _myAddress, Address _txSender, u256 _txValue, u256s const& _txData, FeeStructure const& _fees, BlockInfo const& _previousBlock, BlockInfo const& _currentBlock, uint _currentNumber):
//		myAddress(_myAddress),
//		txSender(_txSender),
//		txValue(_txValue),
//		txData(_txData),
//		fees(_fees),
//		previousBlock(_previousBlock),
//		currentBlock(_currentBlock),
//		currentNumber(_currentNumber)
//	{}

	public u256 store(u256 _n) 
	{
		return new u256(0);	// ?
	}
	
//	void setStore(u256 _n, u256 _v) {}
//	void mktx(Transaction& _t) {}
//	u256 balance(Address _a) { return 0; }
//	void payFee(bigint _fee) {}
//	u256 txCount(Address _a) { return 0; }
//	u256 extro(Address _a, u256 _pos) { return 0; }
//	u256 extroPrice(Address _a) { return 0; }
//	void suicide(Address _a) {}
//
//	Address myAddress;
//	Address txSender;
//	u256 txValue;
//	u256s txData;
	FeeStructure fees;
//	BlockInfo previousBlock;					///< The current block's information.
//	BlockInfo currentBlock;					///< The current block's information.
//	uint currentNumber;


}
