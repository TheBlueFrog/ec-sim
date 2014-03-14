package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.u256;

public class FeeStructure 
{
	/// The fee structure. Values yet to be agreed on...
	
//	u256 c_stepFee = new u256 (1);
//	u256 c_dataFee = new u256 (20);
//	u256 c_memoryFee = new u256 (5);
//	u256 c_extroFee = new u256 (40);
//	u256 c_cryptoFee = new u256 (20);
//	u256 c_newContractFee = new u256 (100);
//	u256 c_txFee = new u256 (100);
//
	// test with more recognizable values
	u256 c_stepFee = new u256 (3);
	u256 c_dataFee = new u256 (22);
	u256 c_memoryFee = new u256 (55);
	u256 c_extroFee = new u256 (44);
	u256 c_cryptoFee = new u256 (222);
	u256 c_newContractFee = new u256 (101);
	u256 c_txFee = new u256 (99);

	u256 m_stepFee = c_stepFee;
	
	void setMultiplier(u256 _x)
	{
		assert false : "NYI";
	
//		m_stepFee = c_stepFee * _x;
//		m_dataFee = c_dataFee * _x;
//		m_memoryFee = c_memoryFee * _x;
//		m_extroFee = c_extroFee * _x;
//		m_cryptoFee = c_cryptoFee * _x;
//		m_newContractFee = c_newContractFee * _x;
//		m_txFee = c_txFee * _x;
	}

	u256 getStepFee() 
	{
		return m_stepFee.divide(c_stepFee);
	}

	public u256 getMemoryFee() 
	{
		return c_memoryFee;
	}

	public u256 getDataFee() 
	{
		return c_dataFee;
	}

	public u256 getCryptoFee() 
	{
		return c_cryptoFee;
	}
	public u256 getExtroFee() 
	{
		return c_extroFee;
	}
	public u256 getTransmitFee() 
	{
		return c_txFee;
	}


}
