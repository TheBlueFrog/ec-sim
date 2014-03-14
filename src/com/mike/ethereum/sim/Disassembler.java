package com.mike.ethereum.sim;

import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;
import com.mike.ethereum.sim.InstructionSet.OpCode;

public class Disassembler
{
	static String run(u256s memory)
	{
		StringBuilder sb = new StringBuilder();
		
		int i = 0;
		while (i < memory.getList().size())
		{
			sb.append (String.format("[%3d] ", i));
			
			u256 it = memory.getList().get(i++);
			OpCode iit = InstructionSet.OpCode.parse(it.mValue);

			if (iit != null)
			{
				InstructionSet.Info info = InstructionSet.c_instructionInfo.get(iit);
				sb.append(info.name).append(" ");

				for (int j = 0; j < info.additional; ++j)
				{
					int k = i++;
					sb.append(String.format("0x%s (%s)",
							memory.getList().get(k).mValue.toString(16),
							memory.getList().get(k).mValue.toString()));
				}				
			}
			else
			{
				sb.append(String.format("0x%s (%s)",
						it.mValue.toString(16),
						it.mValue.toString()));
			}
			
			sb.append ("\n");
		}
		return sb.toString();
	}

}
