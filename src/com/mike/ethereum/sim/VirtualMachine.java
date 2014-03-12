package com.mike.ethereum.sim;

import java.util.ArrayList;
import java.util.List;

import com.mike.ethereum.sim.CommonEth.Address;
import com.mike.ethereum.sim.CommonEth.u256;

public class VirtualMachine 
{
	// Convert from a 256-bit integer stack/memory entry into a 160-bit Address hash.
	// Currently we just pull out the right (low-order in BE) 160-bits.
	private Address asAddress(u256 _item)
	{
//		return right160( new h256 (_item));
	}

	private u256 fromAddress(Address _a)
	{
//		return (u160)_a;
	}

	public VirtualMachine ()
	{
	}

	void reset()
	{
	}

//		void require(u256 _n) { if (m_stack.size() < _n) throw StackTooSmall(_n, m_stack.size()); }

	private u256 m_runFee = new u256(0);

	public u256 runFee()
	{
		return m_runFee; 
	}

	private u256 m_curPC = new u256(0);
	private u256 m_nextPC = new u256(0);
	
	private	long m_stepCount = 0L;

//	std::map<u256, u256> m_temp;
	
	private int STACK_SIZE = 1000;
	List<u256> m_stack = new ArrayList<u256>(STACK_SIZE);

	void require(u256 _n)
	{
		if (_n.greaterThan(STACK_SIZE))
			throw new StackTooSmall(""); 
	}
	void require(int i)
	{
		if (i > STACK_SIZE)
			throw new StackTooSmall(""); 
	}
	
	private u256 popStack ()
	{
		int i = m_stack.size() - 1;
		return m_stack.remove(i);
	}
	private u256 peekStack(int offset)
	{
		int i = m_stack.size() - 1;
		return (m_stack.get(i - offset));
	}
	private void pushStack(u256 x)
	{
		m_stack.add(x);
	}
	
	public void go(VirtualMachineEnvironment _ext, long _steps)
	{
		for (boolean stopped = false; 
				! stopped && (_steps-- > 0); 
				m_curPC = m_nextPC, m_nextPC = m_curPC.add(1))
		{
			m_stepCount++;

			// INSTRUCTION...
			u256 rawInst = _ext.store(m_curPC);
			if (rawInst.greaterThan(0xff))
				throw new BadInstructionExeption("");
			
			Instruction inst = (Instruction)(uint8_t)rawInst;

//			// FEES...
//			long runFee = m_stepCount > 16 ? _ext.fees.m_stepFee.longValue() : 0;
//			long storeCostDelta = 0;
//			switch (inst)
//			{
//			case InstructionSet.OpCode.SSTORE:
//				require(2);
//				if (!_ext.store(m_stack.back()) && m_stack[m_stack.size() - 2])
//					storeCostDelta += _ext.fees.m_memoryFee;
//				if (_ext.store(m_stack.back()) && !m_stack[m_stack.size() - 2])
//					storeCostDelta -= _ext.fees.m_memoryFee;
//				// continue on to...
//			case InstructionSet.OpCode.SLOAD:
//				runFee += _ext.fees.m_dataFee;
//				break;
//
//			case InstructionSet.OpCode.EXTRO:
//			case InstructionSet.OpCode.BALANCE:
//				runFee += _ext.fees.m_extroFee;
//				break;
//
//			case InstructionSet.OpCode.MKTX:
//				runFee += _ext.fees.m_txFee;
//				break;
//
//			case InstructionSet.OpCode.SHA256:
//			case InstructionSet.OpCode.RIPEMD160:
//			case InstructionSet.OpCode.ECMUL:
//			case InstructionSet.OpCode.ECADD:
//			case InstructionSet.OpCode.ECSIGN:
//			case InstructionSet.OpCode.ECRECOVER:
//			case InstructionSet.OpCode.ECVALID:
//				runFee += _ext.fees.m_cryptoFee;
//				break;
//			default:
//				break;
//			}
//			_ext.payFee(runFee + storeCostDelta);
//			m_runFee += (u256)runFee;

			// EXECUTE...
			switch (inst)
			{
			case InstructionSet.OpCode.ADD:
				//pops two items and pushes S[-1] + S[-2] mod 2^256.
				require(2);
				int i = m_stack.size();
				u256 x = peek(-2).add(peek(-1));
				popStack();
				popStack();
				m_stack.add(x);
				break;
			case InstructionSet.OpCode.MUL:
				//pops two items and pushes S[-1] * S[-2] mod 2^256.
				require(2);
				m_stack[m_stack.size() - 2] *= m_stack.back();
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.SUB:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() - m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.DIV:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() / m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.SDIV:
				require(2);
				assert false : "NYI";
//				(s256&)m_stack[m_stack.size() - 2] = (s256&)m_stack.back() / (s256&)m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.MOD:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() % m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.SMOD:
				require(2);
				assert false : "NYI";
//				(s256&)m_stack[m_stack.size() - 2] = (s256&)m_stack.back() % (s256&)m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.EXP:
			{
				// TODO: better implementation?
				require(2);
				auto n = m_stack.back();
				auto x = m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				for (u256 i = 0; i < x; ++i)
					n *= n;
				m_stack.back() = n;
				break;
			}
			case InstructionSet.OpCode.NEG:
				require(1);
				m_stack.back() = ~(m_stack.back() - 1);
				break;
			case InstructionSet.OpCode.LT:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() < m_stack[m_stack.size() - 2] ? 1 : 0;
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.LE:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() <= m_stack[m_stack.size() - 2] ? 1 : 0;
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.GT:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() > m_stack[m_stack.size() - 2] ? 1 : 0;
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.GE:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() >= m_stack[m_stack.size() - 2] ? 1 : 0;
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.EQ:
				require(2);
				m_stack[m_stack.size() - 2] = m_stack.back() == m_stack[m_stack.size() - 2] ? 1 : 0;
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.NOT:
				require(1);
				m_stack.back() = m_stack.back() ? 0 : 1;
				break;
			case InstructionSet.OpCode.MYADDRESS:
				m_stack.push_back(fromAddress(_ext.myAddress));
				break;
			case InstructionSet.OpCode.TXSENDER:
				m_stack.push_back(fromAddress(_ext.txSender));
				break;
			case InstructionSet.OpCode.TXVALUE:
				m_stack.push_back(_ext.txValue);
				break;
			case InstructionSet.OpCode.TXDATAN:
				m_stack.push_back(_ext.txData.size());
				break;
			case InstructionSet.OpCode.TXDATA:
				require(1);
				m_stack.back() = m_stack.back() < _ext.txData.size() ? _ext.txData[(uint)m_stack.back()] : 0;
				break;
			case InstructionSet.OpCode.BLK_PREVHASH:
				m_stack.push_back(_ext.previousBlock.hash);
				break;
			case InstructionSet.OpCode.BLK_COINBASE:
				m_stack.push_back((u160)_ext.currentBlock.coinbaseAddress);
				break;
			case InstructionSet.OpCode.BLK_TIMESTAMP:
				m_stack.push_back(_ext.currentBlock.timestamp);
				break;
			case InstructionSet.OpCode.BLK_NUMBER:
				m_stack.push_back(_ext.currentNumber);
				break;
			case InstructionSet.OpCode.BLK_DIFFICULTY:
				m_stack.push_back(_ext.currentBlock.difficulty);
				break;
			case InstructionSet.OpCode.BLK_NONCE:
				m_stack.push_back(_ext.previousBlock.nonce);
				break;
			case InstructionSet.OpCode.BASEFEE:
				m_stack.push_back(_ext.fees.multiplier());
				break;
			case InstructionSet.OpCode.SHA256:
			{
				require(1);
				assert false : "NYI";
//				uint s = (uint)std::min(m_stack.back(), (u256)(m_stack.size() - 1) * 32);
//				m_stack.pop_back();
//
//				CryptoPP::SHA256 digest;
//				uint i = 0;
//				for (; s; s = (s >= 32 ? s - 32 : 0), i += 32)
//				{
//					bytes b = toBigEndian(m_stack.back());
//					digest.Update(b.data(), (int)std::min<u256>(32, s));			// b.size() == 32
//					m_stack.pop_back();
//				}
//				std::array<byte, 32> final;
//				digest.TruncatedFinal(final.data(), 32);
//				m_stack.push_back(fromBigEndian<u256>(final));
				break;
			}
			case InstructionSet.OpCode.RIPEMD160:
			{
				require(1);
				assert false : "NYI";
//				uint s = (uint)std::min(m_stack.back(), (u256)(m_stack.size() - 1) * 32);
//				m_stack.pop_back();
//
//				CryptoPP::RIPEMD160 digest;
//				uint i = 0;
//				for (; s; s = (s >= 32 ? s - 32 : 0), i += 32)
//				{
//					bytes b = toBigEndian(m_stack.back());
//					digest.Update(b.data(), (int)std::min<u256>(32, s));			// b.size() == 32
//					m_stack.pop_back();
//				}
//				std::array<byte, 20> final;
//				digest.TruncatedFinal(final.data(), 20);
//				// NOTE: this aligns to right of 256-bit container (low-order bytes).
//				// This won't work if they're treated as byte-arrays and thus left-aligned in a 256-bit container.
//				m_stack.push_back((u256)fromBigEndian<u160>(final));
				break;
			}
			case InstructionSet.OpCode.ECMUL:
			{
				// ECMUL - pops three items.
				// If (S[-2],S[-1]) are a valid point in secp256k1, including both coordinates being less than P, pushes (S[-1],S[-2]) * S[-3], using (0,0) as the point at infinity.
				// Otherwise, pushes (0,0).
				require(3);
				assert false : "NYI";

//				bytes pub(1, 4);
//				pub += toBigEndian(m_stack[m_stack.size() - 2]);
//				pub += toBigEndian(m_stack.back());
//				m_stack.pop_back();
//				m_stack.pop_back();
//
//				bytes x = toBigEndian(m_stack.back());
//				m_stack.pop_back();
//
//				if (secp256k1_ecdsa_pubkey_verify(pub.data(), (int)pub.size()))	// TODO: Check both are less than P.
//				{
//					secp256k1_ecdsa_pubkey_tweak_mul(pub.data(), (int)pub.size(), x.data());
//					m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&pub).cropped(1, 32)));
//					m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&pub).cropped(33, 32)));
//				}
//				else
//				{
//					m_stack.push_back(0);
//					m_stack.push_back(0);
//				}
				break;
			}
			case InstructionSet.OpCode.ECADD:
			{
				// ECADD - pops four items and pushes (S[-4],S[-3]) + (S[-2],S[-1]) if both points are valid, otherwise (0,0).
				require(4);
				assert false : "NYI";

//				bytes pub(1, 4);
//				pub += toBigEndian(m_stack[m_stack.size() - 2]);
//				pub += toBigEndian(m_stack.back());
//				m_stack.pop_back();
//				m_stack.pop_back();
//
//				bytes tweak(1, 4);
//				tweak += toBigEndian(m_stack[m_stack.size() - 2]);
//				tweak += toBigEndian(m_stack.back());
//				m_stack.pop_back();
//				m_stack.pop_back();
//
//				if (secp256k1_ecdsa_pubkey_verify(pub.data(),(int) pub.size()) && secp256k1_ecdsa_pubkey_verify(tweak.data(),(int) tweak.size()))
//				{
//					secp256k1_ecdsa_pubkey_tweak_add(pub.data(), (int)pub.size(), tweak.data());
//					m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&pub).cropped(1, 32)));
//					m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&pub).cropped(33, 32)));
//				}
//				else
//				{
//					m_stack.push_back(0);
//					m_stack.push_back(0);
//				}
				break;
			}
			case InstructionSet.OpCode.ECSIGN:
			{
				require(2);
				assert false : "NYI";
//				bytes sig(64);
//				int v = 0;
//
//				u256 msg = m_stack.back();
//				m_stack.pop_back();
//				u256 priv = m_stack.back();
//				m_stack.pop_back();
//				bytes nonce = toBigEndian(Transaction::kFromMessage(msg, priv));
//
//				if (!secp256k1_ecdsa_sign_compact(toBigEndian(msg).data(), 64, sig.data(), toBigEndian(priv).data(), nonce.data(), &v))
//					throw InvalidSignature();
//
//				m_stack.push_back(v + 27);
//				m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&sig).cropped(0, 32)));
//				m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&sig).cropped(32)));
				break;
			}
			case InstructionSet.OpCode.ECRECOVER:
			{
				require(4);
				assert false : "NYI";
//
//				bytes sig = toBigEndian(m_stack[m_stack.size() - 2]) + toBigEndian(m_stack.back());
//				m_stack.pop_back();
//				m_stack.pop_back();
//				int v = (int)m_stack.back();
//				m_stack.pop_back();
//				bytes msg = toBigEndian(m_stack.back());
//				m_stack.pop_back();
//
//				byte pubkey[65];
//				int pubkeylen = 65;
//				if (secp256k1_ecdsa_recover_compact(msg.data(), (int)msg.size(), sig.data(), pubkey, &pubkeylen, 0, v - 27))
//				{
//					m_stack.push_back(0);
//					m_stack.push_back(0);
//				}
//				else
//				{
//					m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&pubkey[1], 32)));
//					m_stack.push_back(fromBigEndian<u256>(bytesConstRef(&pubkey[33], 32)));
//				}
				break;
			}
			case InstructionSet.OpCode.ECVALID:
			{
				require(2);
				assert false : "NYI";
//				bytes pub(1, 4);
//				pub += toBigEndian(m_stack[m_stack.size() - 2]);
//				pub += toBigEndian(m_stack.back());
//				m_stack.pop_back();
//				m_stack.pop_back();
//
//				m_stack.back() = secp256k1_ecdsa_pubkey_verify(pub.data(), (int)pub.size()) ? 1 : 0;
				break;
			}
			case InstructionSet.OpCode.SHA3:
			{
				require(1);
				assert false : "NYI";
//				uint s = (uint)std::min(m_stack.back(), (u256)(m_stack.size() - 1) * 32);
//				m_stack.pop_back();
//
//				CryptoPP::SHA3_256 digest;
//				uint i = 0;
//				for (; s; s = (s >= 32 ? s - 32 : 0), i += 32)
//				{
//					bytes b = toBigEndian(m_stack.back());
//					digest.Update(b.data(), (int)std::min<u256>(32, s));			// b.size() == 32
//					m_stack.pop_back();
//				}
//				std::array<byte, 32> final;
//				digest.TruncatedFinal(final.data(), 32);
//				m_stack.push_back(fromBigEndian<u256>(final));
				break;
			}
			case InstructionSet.OpCode.PUSH:
			{
				m_stack.push_back(_ext.store(m_curPC + 1));
				m_nextPC = m_curPC + 2;
				break;
			}
			case InstructionSet.OpCode.POP:
				require(1);
				popStack();
				break;
			case InstructionSet.OpCode.DUP:
				require(1);
				pushStack(peekStack(0));
				break;
				
			/*case InstructionSet.OpCode.DUPN:
			{
				auto s = store(curPC + 1);
				if (s == 0 || s > stack.size())
					throw OperandOutOfRange(1, stack.size(), s);
				stack.push_back(stack[stack.size() - (uint)s]);
				nextPC = curPC + 2;
				break;
			}*/

			case InstructionSet.OpCode.SWAP:
			{
				require(2);
				x = popStack();
				u256 y = popStack();
				pushStack(y);
				pushStack(x);
				break;
			}
			/*case InstructionSet.OpCode.SWAPN:
			{
				require(1);
				auto d = stack.back();
				auto s = store(curPC + 1);
				if (s == 0 || s > stack.size())
					throw OperandOutOfRange(1, stack.size(), s);
				stack.back() = stack[stack.size() - (uint)s];
				stack[stack.size() - (uint)s] = d;
				nextPC = curPC + 2;
				break;
			}*/
			case InstructionSet.OpCode.MLOAD:
			{
				require(1);
//	#ifdef __clang__
//				auto mFinder = m_temp.find(m_stack.back());
//				if (mFinder != m_temp.end())
//					m_stack.back() = mFinder->second;
//				else
//					m_stack.back() = 0;
//	#else
				m_stack.back() = m_temp[m_stack.back()];
//	#endif
				break;
			}
			case InstructionSet.OpCode.MSTORE:
			{
				require(2);
//	#ifdef __clang__
//				auto mFinder = m_temp.find(m_stack.back());
//				if (mFinder == m_temp.end())
//					m_temp.insert(std::make_pair(m_stack.back(), m_stack[m_stack.size() - 2]));
//				else
//					mFinder->second = m_stack[m_stack.size() - 2];
//	#else
				m_temp[m_stack.back()] = m_stack[m_stack.size() - 2];
//	#endif
				m_stack.pop_back();
				m_stack.pop_back();
				break;
			}
			case InstructionSet.OpCode.SLOAD:
				require(1);
				m_stack.back() = _ext.store(m_stack.back());
				break;
			case InstructionSet.OpCode.SSTORE:
				require(2);
				_ext.setStore(m_stack.back(), m_stack[m_stack.size() - 2]);
				m_stack.pop_back();
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.JMP:
				require(1);
				m_nextPC = m_stack.back();
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.JMPI:
				require(2);
				if (m_stack.back())
					m_nextPC = m_stack[m_stack.size() - 2];
				m_stack.pop_back();
				m_stack.pop_back();
				break;
			case InstructionSet.OpCode.IND:
				m_stack.push_back(m_curPC);
				break;
			case InstructionSet.OpCode.EXTRO:
			{
				require(2);
				auto memoryAddress = m_stack.back();
				m_stack.pop_back();
				Address contractAddress = asAddress(m_stack.back());
				m_stack.back() = _ext.extro(contractAddress, memoryAddress);
				break;
			}
			case InstructionSet.OpCode.BALANCE:
			{
				require(1);
				m_stack.back() = _ext.balance(asAddress(m_stack.back()));
				break;
			}
			case InstructionSet.OpCode.MKTX:
			{
				require(3);

				Transaction t;
				t.receiveAddress = asAddress(m_stack.back());
				m_stack.pop_back();
				t.value = m_stack.back();
				m_stack.pop_back();

				auto itemCount = m_stack.back();
				m_stack.pop_back();
				if (m_stack.size() < itemCount)
					throw OperandOutOfRange(0, m_stack.size(), itemCount);
				t.data.reserve((uint)itemCount);
				for (auto i = 0; i < itemCount; ++i)
				{
					t.data.push_back(m_stack.back());
					m_stack.pop_back();
				}

				_ext.mktx(t);
				break;
			}
			case InstructionSet.OpCode.SUICIDE:
			{
				require(1);
				Address dest = asAddress(m_stack.back());
				_ext.suicide(dest);
				// ...follow through to...
			}
			case InstructionSet.OpCode.STOP:
				return;
			default:
				throw BadInstruction();
			}
		}
		if (_steps == (unsigned)-1)
			throw StepsDone();

}
