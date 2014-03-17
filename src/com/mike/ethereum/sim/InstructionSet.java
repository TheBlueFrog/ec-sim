package com.mike.ethereum.sim;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.mike.ethereum.sim.CommonEth.u256;

public class InstructionSet
{
	static public enum OpCode  
	{
		STOP,				// 0  halts execution
		ADD,
		MUL,				// 2
		SUB,
		DIV,				// 4
		SDIV,				// 5
		MOD,				// 6
		SMOD,				
		EXP,				// 8
		NEG,
		LT,					// 10
		LE,
		GT,					// 12
		GE,
		EQ,					// 14
		NOT,				// 15
		MYADDRESS,			// 16				/< pushes the transaction sender
		TXSENDER,			//				/< pushes the transaction sender
		TXVALUE	,			// 18				/< pushes the transaction value
		TXDATAN,			//				/< pushes the number of data items
		TXDATA,				// 20  			pops one item and pushes data item S[-1], or zero if index out of range
		BLK_PREVHASH,		//				/< pushes the hash of the previous block (NOT the current one since that's impossible!)
		BLK_COINBASE,		// 22			/< pushes the coinbase of the current block
		BLK_TIMESTAMP,		//				/< pushes the timestamp of the current block
		BLK_NUMBER,			// 24			< pushes the current block number
		BLK_DIFFICULTY,		// 25 			pushes the difficulty of the current block
		BLK_NONCE,
		BASEFEE,
		SHA256,				// 28				= 0x20, 32
		RIPEMD160,
		ECMUL,				// 30
		ECADD,				// 			
		ECSIGN,				// 32
		ECRECOVER,
		ECVALID,			// 34
		SHA3,				// 35
		PUSH,				// 36
		POP,
		DUP,				// 38
		SWAP,				// 
		MLOAD,				// 40
		MSTORE,
		SLOAD,				// 42
		SSTORE,				// 43
		JMP,				// 44
		JMPI,				// 45
		IND,				// 46
		EXTRO,
		BALANCE,			// 49
		MKTX,				// 49
		SUICIDE,;			// 50			spec says = 0x3f	63


		public static OpCode parse(String t)
		{
			try
			{
				OpCode o = valueOf(t);
				return o;
			}
			catch (Exception e) {}
			
			return null;
		}
		
		public static OpCode parse(BigInteger i)
		{
			final OpCode[] v = OpCode.values();
			if (i.compareTo(new BigInteger(Integer.toString(v.length))) < 0)
			{
				OpCode o = v[i.intValue()];
				return o;
			}
			
			return null;
		}

		public static OpCode parse(u256 x) 
		{
			return parse(x.mValue);
		}
	};

	static public class Info
	{
		public String name;	///< The name of the instruction.
		public int additional;		///< Additional items required in memory for this instructions (only for PUSH).
		public int args;			///< Number of items required on the stack for this instruction (and, for the purposes of ret, the number taken from the stack).
		public int ret;			///< Number of items placed (back) on the stack by this instruction, assuming args items were removed.
		
		public Info (String s, int a, int b, int c)
		{
			name = s;
			additional = a;
			args = b;
			ret = c;		
		}
	};
	
	
	static public Map<InstructionSet.OpCode, Info> c_instructionInfo = 
			new HashMap<InstructionSet.OpCode, InstructionSet.Info>();
	static
	{
		c_instructionInfo.put(OpCode.STOP, 	        new Info("STOP", 	        0,   0,  0)); 
		c_instructionInfo.put(OpCode.ADD, 	        new Info("ADD", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.SUB, 	        new Info("SUB", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.MUL, 	        new Info("MUL", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.DIV, 	        new Info("DIV", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.SDIV, 	        new Info("SDIV", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.MOD, 	        new Info("MOD", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.SMOD, 	        new Info("SMOD", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.EXP, 	        new Info("EXP", 	        0,   2,  1)); 
		c_instructionInfo.put(OpCode.NEG, 	        new Info("NEG", 	        0,   1,  1)); 
		c_instructionInfo.put(OpCode.LT, 	        new Info("LT", 		        0,   2,  1)); 
		c_instructionInfo.put(OpCode.LE, 	        new Info("LE", 		        0,   2,  1)); 
		c_instructionInfo.put(OpCode.GT, 	        new Info("GT", 		        0,   2,  1)); 
		c_instructionInfo.put(OpCode.GE, 	        new Info("GE", 		        0,   2,  1)); 
		c_instructionInfo.put(OpCode.EQ, 	        new Info("EQ", 		        0,   2,  1)); 
		c_instructionInfo.put(OpCode.NOT, 			new Info("NOT", 	        0,   1,  1)); 
		c_instructionInfo.put(OpCode.MYADDRESS, 	new Info("MYADDRESS", 		0,   0,  1));
		c_instructionInfo.put(OpCode.TXSENDER, 		new Info("TXSENDER", 		0,   0,  1));
		c_instructionInfo.put(OpCode.TXVALUE, 		new Info("TXVALUE", 		0,   0,  1));
		c_instructionInfo.put(OpCode.TXDATAN, 		new Info("TXDATAN", 		0,   0,  1));
		c_instructionInfo.put(OpCode.TXDATA, 		new Info("TXDATA", 			0,   1,  1));
		c_instructionInfo.put(OpCode.BLK_PREVHASH, 	new Info("BLK_PREVHASH", 	0,   0,  1));
		c_instructionInfo.put(OpCode.BLK_COINBASE, 	new Info("BLK_COINBASE", 	0,   0,  1));
		c_instructionInfo.put(OpCode.BLK_TIMESTAMP, new Info("BLK_TIMESTAMP", 	0,   0,  1));
		c_instructionInfo.put(OpCode.BLK_NUMBER, 	new Info("BLK_NUMBER", 		0,   0,  1));
		c_instructionInfo.put(OpCode.BLK_DIFFICULTY, new Info("BLK_DIFFICULTY", 0,   0,  1));
		c_instructionInfo.put(OpCode.BLK_NONCE, 	new Info("BLK_NONCE", 		0,   0,  1));
		c_instructionInfo.put(OpCode.BASEFEE,		new Info("BASEFEE", 		0,   0,  1));
		c_instructionInfo.put(OpCode.SHA256, 		new Info("SHA256", 			0,  -1,  1));
		c_instructionInfo.put(OpCode.RIPEMD160, 	new Info("RIPEMD160", 		0,  -1,  1));
		c_instructionInfo.put(OpCode.ECMUL, 	    new Info("ECMUL", 			0,   3,  1));           
		c_instructionInfo.put(OpCode.ECADD, 	    new Info("ECADD", 			0,   4,  1));           
		c_instructionInfo.put(OpCode.ECSIGN, 	    new Info("ECSIGN", 			0,   2,  1));          
		c_instructionInfo.put(OpCode.ECRECOVER,     new Info("ECRECOVER", 		0,   4,  1));       
		c_instructionInfo.put(OpCode.ECVALID, 	    new Info("ECVALID",         0,   2,  1));   
		c_instructionInfo.put(OpCode.SHA3, 	        new Info("SHA3", 	        0,  -1,  1));   
		c_instructionInfo.put(OpCode.PUSH, 	        new Info("PUSH", 	        1,   0,  1));    // only additional 
		c_instructionInfo.put(OpCode.POP, 	        new Info("POP", 	        0,   1,  0));                       
		c_instructionInfo.put(OpCode.DUP, 	        new Info("DUP", 	        0,   1,  2));                       
		c_instructionInfo.put(OpCode.SWAP, 	        new Info("SWAP", 	        0,   2,  2));                       
		c_instructionInfo.put(OpCode.MLOAD,         new Info("MLOAD", 	        0,   1,  1));                       
		c_instructionInfo.put(OpCode.MSTORE, 	    new Info("MSTORE", 	        0,   2,  0));                       
		c_instructionInfo.put(OpCode.SLOAD, 	    new Info("SLOAD", 	        0,   1,  1));                       
		c_instructionInfo.put(OpCode.SSTORE, 	    new Info("SSTORE", 	        0,   2,  0));                       
		c_instructionInfo.put(OpCode.JMP, 		    new Info("JMP", 	        0,   1,  0));                       
		c_instructionInfo.put(OpCode.JMPI, 		    new Info("JMPI", 	        0,   2,  0));                       
		c_instructionInfo.put(OpCode.IND,		    new Info("IND", 	        0,   0,  1));                       
		c_instructionInfo.put(OpCode.EXTRO, 	    new Info("EXTRO", 	        0,   2,  1));                       
		c_instructionInfo.put(OpCode.BALANCE, 	    new Info("BALANCE",         0,   1,  1));                       
		c_instructionInfo.put(OpCode.MKTX, 		    new Info("MKTX", 	        0,  -3,  0));   
		c_instructionInfo.put(OpCode.SUICIDE, 	    new Info("SUICIDE",         0,   1,  0));    
	};

	// not needed OpCode.
//	Map<String, InstructionSet.OpCode> eth::c_instructions =
//		{
//			{ "STOP", Instruction::STOP },
//			{ "ADD", Instruction::ADD },
//			{ "SUB", Instruction::SUB },
//			{ "MUL", Instruction::MUL },
//			{ "DIV", Instruction::DIV },
//			{ "SDIV", Instruction::SDIV },
//			{ "MOD", Instruction::MOD },
//			{ "SMOD", Instruction::SMOD },
//			{ "EXP", Instruction::EXP },
//			{ "NEG", Instruction::NEG },
//			{ "LT", Instruction::LT },
//			{ "LE", Instruction::LE },
//			{ "GT", Instruction::GT },
//			{ "GE", Instruction::GE },
//			{ "EQ", Instruction::EQ },
//			{ "NOT", Instruction::NOT },
//			{ "MYADDRESS", Instruction::MYADDRESS },
//			{ "TXSENDER", Instruction::TXSENDER },
//			{ "TXVALUE", Instruction::TXVALUE },
//			{ "TXDATAN", Instruction::TXDATAN },
//			{ "TXDATA", Instruction::TXDATA },
//			{ "BLK_PREVHASH", Instruction::BLK_PREVHASH },
//			{ "BLK_COINBASE", Instruction::BLK_COINBASE },
//			{ "BLK_TIMESTAMP", Instruction::BLK_TIMESTAMP },
//			{ "BLK_NUMBER", Instruction::BLK_NUMBER },
//			{ "BLK_DIFFICULTY", Instruction::BLK_DIFFICULTY },
//			{ "BLK_NONCE", Instruction::BLK_NONCE },
//			{ "BASEFEE", Instruction::BASEFEE },
//			{ "SHA256", Instruction::SHA256 },
//			{ "RIPEMD160", Instruction::RIPEMD160 },
//			{ "ECMUL", Instruction::ECMUL },
//			{ "ECADD", Instruction::ECADD },
//			{ "ECSIGN", Instruction::ECSIGN },
//			{ "ECRECOVER", Instruction::ECRECOVER },
//			{ "ECVALID", Instruction::ECVALID },
//			{ "SHA3", Instruction::SHA3 },
//			{ "PUSH", Instruction::PUSH },
//			{ "POP", Instruction::POP },
//			{ "DUP", Instruction::DUP },
//			{ "SWAP", Instruction::SWAP },
//			{ "MLOAD", Instruction::MLOAD },
//			{ "MSTORE", Instruction::MSTORE },
//			{ "SLOAD", Instruction::SLOAD },
//			{ "SSTORE", Instruction::SSTORE },
//			{ "JMP", Instruction::JMP },
//			{ "JMPI", Instruction::JMPI },
//			{ "IND", Instruction::IND },
//			{ "EXTRO", Instruction::EXTRO },
//			{ "BALANCE", Instruction::BALANCE },
//			{ "MKTX", Instruction::MKTX },
//			{ "SUICIDE", Instruction::SUICIDE }
//		};

}
