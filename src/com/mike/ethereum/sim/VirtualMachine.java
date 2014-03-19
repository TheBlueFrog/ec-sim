package com.mike.ethereum.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mike.ethereum.sim.CommonEth.Address;
import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.exceptions.BadInstructionExeption;
import com.mike.ethereum.sim.exceptions.OperandOutOfRangeException;
import com.mike.ethereum.sim.exceptions.StackTooSmallException;
import com.mike.ethereum.sim.exceptions.StackUnderflowException;
import com.mike.ethereum.sim.exceptions.StepsDoneException;

public class VirtualMachine 
{
	private static final String TAG = VirtualMachine.class.getSimpleName();

	// Convert from a 256-bit integer stack/memory entry into a 160-bit Address hash.
	// Currently we just pull out the right (low-order in BE) 160-bits.
	private Address asAddress(u256 a)
	{
		return Account.getFromAddress(a).getAddress();
	}

	private u256 fromAddress(Address _a)
	{
		return new u256 (_a.getHash());
	}

	public VirtualMachine (boolean logging)
	{
		mLogging = logging;
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
//	private u256 m_nextPC = new u256(0);
	private void incPC()
	{
		m_curPC = m_curPC.add(1);
	}
	private void setPC(u256 i)
	{
		m_curPC = i;
	}
	
	private	long m_stepCount = 0L;

	private Map<u256, u256> mMemory = new HashMap<u256, u256>();
	
	private int STACK_SIZE = 1000;
	List<u256> m_stack = new ArrayList<u256>(STACK_SIZE);

	void require(u256 _n) throws StackTooSmallException
	{
		if (_n.greaterThan(STACK_SIZE))
			throw new StackTooSmallException(""); 
	}
	void require(int i) throws StackTooSmallException, StackUnderflowException
	{
		if (i > STACK_SIZE)
			throw new StackTooSmallException(""); 
		
		if (m_stack.size() < i)
			throw new StackUnderflowException (String.format("Requires %d items, only %d on stack.  PC = %s", 
					i, 
					m_stack.size(),
					m_curPC.toString()));
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

	public boolean mLogging = true;
	
	private void dumpStack()
	{
		if (mLogging)
		{
			for(int i = m_stack.size() - 1; i >= 0; --i)
			{
				u256 u = m_stack.get(i);
				Log.d(TAG, String.format("Stack[%2d] %s", i, u.toString()));
			}
			Log.d(TAG, "");
		}
	}
	
	public void go(VirtualMachineEnvironment _ext, long _steps) 
			throws 
				BadInstructionExeption, 
				StackTooSmallException, 
				StepsDoneException, 
				StackUnderflowException, 
				OperandOutOfRangeException
	{
		boolean stopped = false;
		while ( ! stopped && (_steps-- > 0))
//				m_curPC = m_nextPC, m_nextPC = m_curPC.add(1))
		{
			m_stepCount++;

			boolean mNoIncPC = false;	// instructions that mess with the PC can
										// defeat the normal increment

			// INSTRUCTION...
			u256 rawInst = _ext.getStore(m_curPC);
			
			if (mLogging)
				Log.d(TAG, String.format ("CurPC %s", m_curPC.toString()));
			
			if (rawInst.greaterThan(0xff))
				throw new BadInstructionExeption("");
			
			InstructionSet.OpCode inst = InstructionSet.OpCode.parse(rawInst);

			{
				// FEES...
				u256 runFee = m_stepCount > 16 ? _ext.fees.getStepFee() : new u256(0);
				u256 storeCostDelta = new u256(0);
				
				switch (inst)
				{
				case SSTORE:
					require(2);
	//				if ( ! _ext.store(peekStack(0)) && peekStack(1))	// ?
						storeCostDelta.add(_ext.fees.getMemoryFee());
	//				if (_ext.store(m_stack.back()) && !m_stack[m_stack.size() - 2])	// ?
						storeCostDelta.subtract(_ext.fees.getMemoryFee());
					// continue on to...
				case SLOAD:
					runFee.add(_ext.fees.getDataFee());
					break;
	
				case EXTRO:
				case BALANCE:
					runFee.add(_ext.fees.getExtroFee());
					break;
	
				case MKTX:
					runFee.add(_ext.fees.getTransmitFee());
					break;
	
				case SHA256:
				case RIPEMD160:
				case ECMUL:
				case ECADD:
				case ECSIGN:
				case ECRECOVER:
				case ECVALID:
					runFee.add(_ext.fees.getCryptoFee());
					break;
				default:
					break;
				}
				
				_ext.payFee(runFee.add(storeCostDelta));
//				m_runFee += (u256)runFee;
			}
			
			u256 x;
			u256 y;
			
			// EXECUTE...
			if (mLogging)
				Log.d(TAG, String.format("Execute %s, ", inst.toString()));
			
			switch (inst)
			{
			case ADD:
				//pops two items and pushes S[-1] + S[-2] mod 2^256.
				require(2);
				x = popStack();
				y = popStack();
				pushStack(y.add(x));
				break;
			case MUL:
				//pops two items and pushes S[-1] * S[-2] mod 2^256.
				require(2);
				x = popStack();
				y = popStack();
				pushStack(y.mult(x));
				break;
			case SUB:
				/* updated spec
				 	SUB -2 +1
					S[0] := S'[0] + S'[1]
					assume that should be 
					S[0] := S'[0] - S'[1]
				 */
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.subtract(y));
				break;
			case DIV:
				/* updated spec
			 		SUB -2 +1
					S[0] := S'[0] / S'[1]
				 */
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.divide(y));	// do floor?
				break;
			case SDIV:
				require(2);
				assert false : "NYI";
//				(s256&)m_stack[m_stack.size() - 2] = (s256&)m_stack.back() / (s256&)m_stack[m_stack.size() - 2];
//				m_stack.pop_back();
				break;
			case MOD:
				require(2);
				x = popStack();
				y = popStack();
				pushStack(y.mod(x));
				break;
			case SMOD:
				require(2);
				assert false : "NYI";
//				(s256&)m_stack[m_stack.size() - 2] = (s256&)m_stack.back() % (s256&)m_stack[m_stack.size() - 2];
//				m_stack.pop_back();
				break;
			case EXP:
			{
				// TODO: better implementation?
				require(2);
				assert false : "NYI";
//				auto n = m_stack.back();
//				auto x = m_stack[m_stack.size() - 2];
//				m_stack.pop_back();
//				for (u256 i = 0; i < x; ++i)
//					n *= n;
//				m_stack.back() = n;
				break;
			}
			case NEG:
				require(1);
				assert false : "NYI";
//				m_stack.back() = ~(m_stack.back() - 1);
				break;
				
				/* from redone spec
						LT -2 +1
						S[0] := S'[0] < S'[1] ? 1 : 0
						0x0b: LE -2 +1
						S[0] := S'[0] <= S'[1] ? 1 : 0
						0x0c: GT -2 +1
						S[0] := S'[0] > S'[1] ? 1 : 0
						0x0d: GE -2 +1
						S[0] := S'[0] >= S'[1] ? 1 : 0
						0x0e: EQ -2 +1
						S[0] := S'[0] == S'[1] ? 1 : 0
				 */
			case LT:
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.lessThan(y) ? new u256(1) : new u256(0));
				break;
			case LE:
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.lessThanEqual(y) ? new u256(1) : new u256(0));
				break;
			case GT:
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.greaterThan(y) ? new u256(1) : new u256(0));
				break;
			case GE:
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.greaterThanEqual(y) ? new u256(1) : new u256(0));
				break;
			case EQ:
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x.equal(y) ? new u256(1) : new u256(0));
				break;
			case NOT:
				require(1);
				x = popStack();
				pushStack(x.equal(0) ? new u256(1) : new u256(0));
				break;
			case MYADDRESS:
				pushStack(fromAddress(_ext.myAccount.getAddress()));
				break;
			case TXSENDER:
				pushStack(fromAddress(_ext.txSender.getAddress()));
				break;
			case TXVALUE:
				pushStack(_ext.txValue);
				break;
			case TXDATAN:
				pushStack(new u256(_ext.txData.size()));
				break;
			case TXDATA:
				require(1);
				x = popStack();
				int ix = x.intValue();
				int i = _ext.txData.size();
				pushStack(x.lessThan(_ext.txData.size()) ? _ext.txData.get(ix) : new u256(0));
				break;
			case BLK_PREVHASH:
				assert false : "NYI";
//				m_stack.push_back(_ext.previousBlock.hash);
				break;
			case BLK_COINBASE:
				assert false : "NYI";
//				m_stack.push_back((u160)_ext.currentBlock.coinbaseAddress);
				break;
			case BLK_TIMESTAMP:
				assert false : "NYI";
//				m_stack.push_back(_ext.currentBlock.timestamp);
				break;
			case BLK_NUMBER:
				assert false : "NYI";
//				m_stack.push_back(_ext.currentNumber);
				break;
			case BLK_DIFFICULTY:
				assert false : "NYI";
//				m_stack.push_back(_ext.currentBlock.difficulty);
				break;
			case BLK_NONCE:
				assert false : "NYI";
//				m_stack.push_back(_ext.previousBlock.nonce);
				break;
			case BASEFEE:
				pushStack(_ext.fees.getStepFee());
				break;
			case SHA256:
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
			case RIPEMD160:
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
			case ECMUL:
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
			case ECADD:
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
			case ECSIGN:
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
			case ECRECOVER:
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
			case ECVALID:
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
			case SHA3:
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
			case PUSH:								// like a load immediate, extra instruction
				incPC ();
				pushStack(_ext.getStore(m_curPC));
				break;
			case POP:
				require(1);
				popStack();
				break;
			case DUP:
				require(1);
				pushStack(peekStack(0));
				break;
				
			/*case DUPN:
			{
				auto s = store(curPC + 1);
				if (s == 0 || s > stack.size())
					throw OperandOutOfRange(1, stack.size(), s);
				stack.push_back(stack[stack.size() - (uint)s]);
				nextPC = curPC + 2;
				break;
			}*/

			case SWAP:
			{
				require(2);
				x = popStack();
				y = popStack();
				pushStack(x);
				pushStack(y);
				break;
			}
			/*case SWAPN:
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
			case MLOAD:								// pops two items and sets the item in memory at index S[-1] to S[-2]
			{										// thats wrong - pop Adress, push item in memory at Address
				require(1);
//	#ifdef __clang__
//				auto mFinder = m_temp.find(m_stack.back());
//				if (mFinder != m_temp.end())
//					m_stack.back() = mFinder->second;
//				else
//					m_stack.back() = 0;
//	#else
				x = popStack();
				pushStack(mMemory.get(x));
//	#endif
				break;
			}
			case MSTORE:							// pops two items and sets the item in memory at index S[-1] to S[-2]
			{
				require(2);
//	#ifdef __clang__
//				auto mFinder = m_temp.find(m_stack.back());
//				if (mFinder == m_temp.end())
//					m_temp.insert(std::make_pair(m_stack.back(), m_stack[m_stack.size() - 2]));
//				else
//					mFinder->second = m_stack[m_stack.size() - 2];
//	#else
				x = popStack();
				y = popStack();
				mMemory.put(x, y);
//	#endif
				break;
			}
			case SLOAD:								// spec says - pops two items and sets the item in storage at index S[-1] to S[-2]
				require(1);							// that's wrong,  pop Address and get item in storage at Address and push on stack 
				x = popStack();
				pushStack(_ext.getStore(x));
				break;
			case SSTORE:							// pops two items and sets the item in storage at index S[-1] to S[-2]
				/*
				 	SSTORE -2 +0
					P[ S'[0] ] := S'[1]
				 */
				require(2);
				x = popStack();
				y = popStack();
				_ext.setStore(x, y);
				break;
			case JMP:								// pops one item and sets the index pointer (PC) to S[-1]
				require(1);
				setPC(popStack());
				mNoIncPC = true;
				break;
			case JMPI:  							// JMPI - pops two items and sets the index pointer (PC) to S[-2] only if S[-1] is nonzero
				require(2);
				x = popStack();
				y = popStack();
				if ( ! x.equal(0))
				{
					setPC(y);
					mNoIncPC = true;
				}
				break;
			case IND:								//  IND - pushes the index pointer (PC)
				pushStack(m_curPC);
				break;
			case EXTRO:
			{
				require(2);
				x = popStack();
				y = popStack();
				Address contractAddress = asAddress(y);
				pushStack(_ext.extro(contractAddress, x));
				break;
			}
			case BALANCE:
			{
				require(1);
				x = popStack();
				pushStack(_ext.balance(asAddress(x)));
				break;
			}
			case MKTX:
				/*
					MKTX -(minimum: 3) +0
					Immediately executes a transaction where:
					The recipient is given by S'[0], when interpreted as an address.
					The value is given by S'[1]
					The data of the transaction is given by S'[3], S'[4], ... S'[ 2 + S'[2] ]
					Thus the number of data items of the transaction is given by S'[2].
					NOTE: This transaction is not queued; full ramifications take effect 
					immediately, including balance transfers and any contract invocations.
				 */
			{
				require(3);

				Transaction t = new Transaction (
						_ext.myAccount.getAddress(),
						asAddress(popStack()),			// receiver
						popStack()); 					// amount

				int itemCount = popStack().intValue();
				if (m_stack.size() < itemCount)
					throw new OperandOutOfRangeException(String.format("MKTX,  too few arguments, stack has %d, need %d", 
							m_stack.size(), 
							itemCount));
				for (int j = 0; j < itemCount; ++j)
				{
					t.addData(popStack());
				}

				_ext.mktx(t);
				break;
			}
			case SUICIDE:
			{
				require(1);
				Address dest = asAddress(peekStack(0));
				_ext.suicide(dest);
				// ...follow through to...
			}
			case STOP:
				return;
			default:
				throw new BadInstructionExeption("Unknown opcode " + inst.toString());
			}
			
			if ( ! mNoIncPC)
				incPC();
			
			dumpStack();
		}
		
		if (_steps == -1)
			throw new StepsDoneException("Ran out of steps");
	}

	private Exception OperandOutOfRange(String format)
	{
		// TODO Auto-generated method stub
		return null;
	}

}