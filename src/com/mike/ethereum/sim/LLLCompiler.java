package com.mike.ethereum.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;
import com.mike.ethereum.sim.InstructionSet.OpCode;

/*
from Instruction.cpp
	while (d != e)
	{
		skip to next token
		for (; d != e && !isalnum(*d) && *d != '(' && *d != ')' && *d != '_' && *d != '"' && !c_allowed.count(*d) && *d != ';'; ++d) {}
		if (done)
			break;

		switch (*d)
		{
		case ';':	skip to EOL
			break;
		case '(':
			exec = true;
			++d;
			break;
		case ')':
			if (exec)
			{
				++d;
				return true;
			}
			else
				// unexpected - return false as we don't know what to do with it.
				return false;
		default:
			{
				bool haveLiteral = false;
				u256 literalValue = 0;
				string t;
	
				if (*d == '"')
					read string, retain value
				else
					read number, retain value
	
				if have value 
					bareload
				else
				{
					if (t == "IF")
						if ( ! do if) return false
					else if (t == "WHEN" || t == "UNLESS")
						if ( ! do when) return false
					else if (t == "FOR")
						if ( ! do for) return false
					else if (t == "SEQ")
						if ( ! do seq) break;			////!!!
					else if (t == "AND")
						if ( ! do and) return false;
					else if (t == "OR")
						if ( ! do or ) return false;
					else
					{
						if ( is opcode)
							do opcode		// no return value
						else if (is arith)
							do arith		// no return value
						else if (is binary)
							do binary		// no return value
						else if ( is unary)
							do unary		// no reutrn value
						else
							cwarn << "Unknown assembler token" << t;
					}
				}
	
				if (!exec)
					return true;
			}  // default
		} // switch
	} // while
	
	return false;

 */


public class LLLCompiler
{
	private static final String TAG = LLLCompiler.class.getSimpleName();

	private class Input
	{
		String mS;
		int mI;
		
		public Input (String s)
		{
			mS = s;
			mI = 0;
		}
		public boolean more ()
		{
			return mI < mS.length();
		}
		public int ch()
		{
			return mS.charAt(mI);
		}
		public void next()
		{
			++mI;
		}

		// 				for (; d != e && (isalnum(*d) || *d == '_' || c_allowed.count(*d)); ++d) {}

		private String getToken()
		{
			int s = mI;
			while (    more ()
					&& (   Character.isAlphabetic(ch()) 
						|| Character.isDigit(ch())
						|| ch() == '_' 
						|| (c_allowed.indexOf(ch()) >= 0))) 
				++mI;
			
			return mS.substring(s, mI);
		}

		public String rest()
		{
			return mS.substring(mI);
		}
		public void advance(int consumed)
		{
			mI += consumed;
		}
		
		@Override
		public String toString()
		{
			return String.format("%d, %s^%s", 
					mI, 
					mS.substring(0, mI),
					mS.substring(mI));
		}
	}
	
	private boolean mLogging = false;
	
	public LLLCompiler (boolean logging)
	{		
		mLogging = logging;
	}
	
	Deque<Frame> mFrames = new ArrayDeque<Frame>();

	private Frame mRoot;
	
	public u256s compileLisp(String _code)
	{
//		char const* d = _code.data();
//		char const* e = _code.data() + _code.size();
		
//		Compiled c = new Compiled();
//
//		mFrames.push(new State (new Input(_code), c));

		mRoot = new Frame(_code, null);

		compileLispFragment(mRoot);
		
		dump(mRoot);
		
		return null;
//		return mLastFrame.mCompiled.getCode();
	}

	private void dump(Frame parent)
	{
		Disassembler.run(parent.mCompiled.o_code);
		
		for (Frame f : parent.mChild)
		{
			dump(f);
		}
	}
	
	private String c_allowed = "+-*/%<>=!";

	// shortcuts
	private Input mToS;
	private Compiled mCompiled;
	
	private class Frame
	{
		public Input mInput = null;
		public Compiled mCompiled = null;

		public List<Frame> mChild = new ArrayList<Frame>();
		
		public Frame (String s, Frame parent)
		{
			mInput = new Input(s);
			mCompiled = new Compiled();

			if (parent != null)
				parent.mChild.add(this);
		}
		public Frame (Input input, Frame parent)
		{
			mInput = input;
			mCompiled = new Compiled();
			
			parent.mChild.add(this);
		}
	}


	/**
	 * collect compiled output, output opcodes and patch points
	 */
	private class Compiled
	{
		/** output code */
		u256s o_code = new u256s();
		
		/** not sure yet */
		List<Integer> o_locs = new ArrayList<Integer>();
		
		public Compiled ()
		{
		}

		public u256s getCode()
		{
			return o_code;
		}

		/** append an instruction */
		public void addInstruction(OpCode c) 
		{
			o_code.add(c.ordinal());
		}

		/** PUSH has to have an operand, return offset
		 * of the location of the operand
		 */
		public int addPushInstructionWith(int operand)
		{
			o_code.add(InstructionSet.OpCode.PUSH.ordinal());
			int i = o_code.size();
			o_code.add(operand);
			return i;
		}

		public int addPushInstructionWith(u256 operand)
		{
			o_code.add(InstructionSet.OpCode.PUSH.ordinal());
			int i = o_code.size();
			o_code.add(operand);
			return i;
		}
		
		public void setCode(int i, int v)
		{
			o_code.set(i, v);
		}
		
		public List<Integer> getLocs() 
		{
			return o_locs;
		}

		public void addLoc(int k)
		{
			o_locs.add(k);
		}

	}

	private boolean compileLispFragment (Frame frame)//, Compiled compiled)
	{
		mToS = frame.mInput;
		mCompiled = frame.mCompiled;
		mFrames.push(frame);
		
		boolean exec = false;

		while (mToS.more ())
		{
			skipToToken();
			
			if (mToS.more ())
			{
				switch (mToS.ch())
				{
				case ';':
					skipToEoL(mToS.mS);
					break;
				case '(':
					exec = true;
					mToS.next();
					break;
				case ')':
					if (exec)
					{
						mToS.next();
						
						pop();
						return true;
					}
					else
					{
						pop();
						Log.e(TAG, "Unexpected ')'...");
						return false;// unexpected - return false as we don't know what to do with it.
					}
					
				default:
					{
						u256 literalValue = null;
						String token = null;
			
						if (mToS.ch() == '"')
							literalValue = handleLiteral(mToS.mS);
						else
						{
							token = mToS.getToken();
							if (token.length() > 0)
							{
								if (Character.isDigit(token.charAt(0)))
									literalValue = readNumeric(token);
							}
						}

						if (literalValue != null)
							handleBareLoad(mToS.mS, token, exec, literalValue);
						else 
						{
							token = token.toUpperCase();

							if ("SEQ".equals (token))
							{
								if ( ! handleSeq(mToS.mS, token))
									break;
							}
							else if ( ! handleKeywordOpCode (mToS.mS, token, exec))
							{
								Log.e(TAG,  "Unhandled token " + token);
								return false;	// bad syntax							
							}
						}
	
						if ( ! exec)
						{
							pop();
							return true;
						}
					}
					break;	// default
				}	// switch
			}	// if have more
		}	// while

		pop();
		return false;
	}

	private void pop()
	{
		int consumed = mToS.mI;
		
		mFrames.pop();
		if ( ! mFrames.isEmpty())
			mFrames.peek().mInput.advance (consumed);
	}
	
	private u256 handleLiteral(String input)
	{
		String s = readQuoted(mToS.rest());
//		h256 valHash;
//		memcpy(valHash.data(), s.data(), s.size());
//		memset(valHash.data() + s.size(), 0, 32 - s.size());
//		literalValue = (u256) valHash;
		return new u256(s);
	}


	private void skipToEoL(String input)
	{
		while (mToS.more () && mToS.ch() != '\n') 
			mToS.next(); 
	}


	private void skipToToken()
	{
		//	for (; d != e  
		//       && !isalnum(*d) 
		//       && *d != '(' 
		//       && *d != ')' 
		//       && *d != '_' 
		//       && *d != '"' 
		//       && !c_allowed.count(*d) 
		//       && *d != ';'; ++d) {}

		while (    mToS.more ()
				&& ( ! Character.isAlphabetic(mToS.ch()))
				&& ( ! Character.isDigit(mToS.ch())) 
				&& mToS.ch() != '(' 
				&& mToS.ch() != ')' 
				&& mToS.ch() != '_' 
				&& mToS.ch() != '"' 
				&& (c_allowed.indexOf(mToS.ch()) < 0) 
				&& mToS.ch() != ';') 
			mToS.next(); 
	}

	private boolean handleKeywordOpCode(String input, String t, boolean exec)
	{
		if ("IF".equals (t)) 
		{
			if ( ! handleIf(input, t))
				return false;
		}
		else if (("WHEN".equals (t)) || "UNLESS".equals (t))
		{
			if ( ! handleWhen(input, t))
				return false;
		}
		else if ("FOR".equals (t)) 
		{
			if ( ! handleFor(input, t))
				return false;
		}
		else if ("AND".equals (t))
		{
			if ( ! handleAnd(input, t))
				return false;
		}
		else if ("OR".equals (t))
		{
			if ( ! handleOr (input, t))
				return false;
		}
		else if ( ! handleOpCode (input, t, exec))
		{
			if ( ! handleArith(input, t))
			{
				handleUnaryBinary (input, t);
			}
		}
		
		return true;
	}

	private void handleBareLoad(String input, String t, boolean exec, u256 literalValue)
	{
		boolean bareLoad = true;
		if (exec)
		{
//			Compiled c = new Compiled();
			
			if (compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
			{
//				appendCode(mCompiled, mLastFrame.mCompiled);
				
//				Compiled j = new Compiled();
				while (compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
					if (mLogging)
						Log.e(TAG, "Additional items in bare store. Ignoring.");

				bareLoad = false;
			}
		}
		
		mCompiled.addPushInstructionWith(literalValue);
		
		if (exec)
			mCompiled.addInstruction(bareLoad ? InstructionSet.OpCode.SLOAD : InstructionSet.OpCode.SSTORE);
	}

	private boolean handleSeq(String input, String t)
	{
		while (mToS.more ())
		{
//			Compiled c = new Compiled();
			if (compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
			{
				//appendCode(mCompiled, mLastFrame.mCompiled);
			}
			else
				return false;
		}
		return true;
	}

	private boolean handleIf(String input, String t)
	{
		// Compile all the code...
		List<Compiled> fragments = new ArrayList<Compiled>();
		for (int i = 0; i < 3; ++i)
		{
//			Compiled c = new Compiled();
//			fragments.add (c);

			if ( ! compileLispFragment(new Frame(new Input (mToS.rest()), mFrames.peek())))
				return false;
			
//			fragments.add(mLastFrame.mCompiled);
		}

		// save first patch location and push a placeholder
		// for the positive code location
		int patch1 = mCompiled.addPushInstructionWith(0);
		mCompiled.addLoc(patch1);

		// output predicate, it will leave zero/nonzero on tos
		appendCode(mCompiled, fragments.get(0));

		// jump to positive if nonzero
		mCompiled.addInstruction(InstructionSet.OpCode.JMPI);

		// output false code block
		appendCode(mCompiled, fragments.get(2));

		// unconditional jump to after true code block
		int patch2 = mCompiled.addPushInstructionWith(0);
		mCompiled.addLoc(patch2);
		mCompiled.addInstruction(InstructionSet.OpCode.JMP);

		// at start of true code block, patch first jump
		mCompiled.setCode(patch1, mCompiled.getCode().size());
		
		// output true code block
		appendCode(mCompiled, fragments.get(1));

		// patch conditional jump at end of false block
		mCompiled.setCode(patch2, mCompiled.getCode().size());
		return true;
	}

	private boolean handleWhen(String input, String t)
	{
		// Compile all the code...
		List<Compiled> fragments = new ArrayList<Compiled>();
		for (int i = 0; i < 2; ++i)
		{
//			Compiled c = new Compiled();
//			fragments.add (c);
			if ( ! compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
				return false;
			
//			fragments.add(mLastFrame.mCompiled);
		}
		
		// record where we have to patch to get past this
		// whole statement
		int patch1 = mCompiled.addPushInstructionWith(0);
		mCompiled.addLoc(patch1);

		// output the predicate that will leave zero/nonzero on the
		// stack
		appendCode(mCompiled, fragments.get(0));

		if (t == "WHEN")
			mCompiled.addInstruction(InstructionSet.OpCode.NOT);

		// jump if nonzero to the location to be patched 
		mCompiled.addInstruction(InstructionSet.OpCode.JMPI);

		// append conditional code
		appendCode(mCompiled, fragments.get(1));

		// patch now that we know where we end
		mCompiled.setCode(patch1, mCompiled.getCode().size());
		return true;
	}

	/** closer to a while statement, (while (predicate) (body)) 
	 * @param input
	 * @param t
	 * @param compiled
	 * @return
	 */
	private boolean handleFor(String input, String t)
	{
		// Compile all the code...
		List<Compiled> fragments = new ArrayList<Compiled>();
		for (int i = 0; i < 2; ++i)
		{
//			Compiled c = new Compiled();
//			fragments.add (c);
			if ( ! compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
				return false;

//			fragments.add(mLastFrame.mCompiled);

//		if (compileLispFragment(d, e, _quiet, codes[2], locs[2]))
//			return false;
		}
		
		int startLocation = mCompiled.getCode().size();

		// setup location to jump around this statement
		int patch1 = mCompiled.addPushInstructionWith(0);
		mCompiled.addLoc(patch1);

		// output predicate, it leaves zero/nonzero on stack
		appendCode(mCompiled, fragments.get(0));

		// jump out of this statement if zero on stack
		mCompiled.addInstruction(InstructionSet.OpCode.NOT);
		mCompiled.addInstruction(InstructionSet.OpCode.JMPI);

		// output body
		appendCode(mCompiled, fragments.get(1));

		// jump back to beginning
		int i = mCompiled.addPushInstructionWith(startLocation);
		mCompiled.addLoc(i);
		
		mCompiled.addInstruction(InstructionSet.OpCode.JMP);

		// patch condition now that we know the end
		mCompiled.setCode(patch1, mCompiled.getCode().size());
		return true;
	}

	private boolean handleAnd(String input, String t)
	{		
		List<Compiled> fragments = new ArrayList<Compiled>();
		boolean more = mToS.more ();
		while (more)
		{
//			Compiled c = new Compiled();
//			fragments.add(c);

			if ( ! compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
				more = false;
			else
			{
//				fragments.add(mLastFrame.mCompiled);
			}
		}

		if (fragments.size() < 2)
			return false;

		// last one is empty.
//		fragments.remove(fragments.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (fragments.size() > 1)
		{
			// see with zero (false)
			mCompiled.addPushInstructionWith(0);

			for (int i = 1; i < fragments.size(); ++i)
			{
				// Push the false location.
				int k = mCompiled.addPushInstructionWith(0);
				ends.add(k);
				mCompiled.addLoc(k);

				// output predicate
				appendCode(mCompiled, fragments.get(i - 1));	

				// Jump to end if zero 
				mCompiled.addInstruction(InstructionSet.OpCode.NOT);
				mCompiled.addInstruction(InstructionSet.OpCode.JMPI);
			}

			mCompiled.addInstruction(InstructionSet.OpCode.POP);
		}

		// check if last one is true
		appendCode(mCompiled, fragments.get(fragments.size() - 1));

		// at end now, patch everyone to go here
		for (Integer i: ends)
			mCompiled.setCode(i, mCompiled.getCode().size());

		return true;
	}
	
	private boolean handleOr(String input, String t)
	{
		List<Compiled> fragments = new ArrayList<Compiled>();
		boolean more = mToS.more ();
		while (more)
		{
//			Compiled c = new Compiled();
//			fragments.add(c);

			if ( ! compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
				more = false;
			else
			{
//				fragments.add(mLastFrame.mCompiled);
			}
		}

		if (fragments.size() < 2)
			return false;

		// last one is empty drop it
//		fragments.remove(fragments.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (fragments.size() > 1)
		{
			// for each of the terms except the last one
			
			// first push non-zero to setup a JMPI
			mCompiled.addPushInstructionWith(1);

			for (int i = 1; i < fragments.size(); ++i)
			{
				// save beginning of this block
				int k = mCompiled.addPushInstructionWith(0);
				ends.add(k);
				mCompiled.addLoc(k);

				// this code block will leave zero/nonzero on tos
				appendCode(mCompiled, fragments.get(i - 1));

				// jump to end if nonzero (true)
				mCompiled.addInstruction(InstructionSet.OpCode.JMPI);
			}

			// gets here if all tests were zero, discard the 1 we pushed before
			mCompiled.addInstruction(InstructionSet.OpCode.POP);
		}

		// append the last one, either zero or nonzero
		appendCode(mCompiled, fragments.get(fragments.size() - 1));

		// at end now, patch everyone to go here
		for (Integer i: ends)
			mCompiled.setCode(i, mCompiled.getCode().size());

		return true;
	}
	
	private boolean handleOpCode (String input, String t, boolean exec)
	{
		InstructionSet.OpCode it = InstructionSet.OpCode.parse(t);
		if (it != null)
		{
			if (exec)
			{
				List<Compiled> fragments = new ArrayList<Compiled>();
				{
//					Compiled c = new Compiled();
//					fragments.add(c);
					while (mToS.more ()
							&& compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
					{
//						Compiled c1 = new Compiled();
//						fragments.add(c1);

//						fragments.add(mLastFrame.mCompiled);
					}
				}
				
				// why reverse order, possibly to match change to opcodes
				for (int i = fragments.size() - 1; i >= 0; --i)
//				for (int i = 0; i < fragments.size(); ++i)
				{
					Compiled c = fragments.get(i);
					appendCode(mCompiled, c);
				}
				
				mCompiled.addInstruction(it);
			}
			else
			{
				mCompiled.addInstruction(InstructionSet.OpCode.PUSH);
				mCompiled.addInstruction(it);
			}
			
			return true;
		}

		return false;
	}
	
	static private Map<String, InstructionSet.OpCode> c_arith = new HashMap<String, InstructionSet.OpCode>();
	static
	{
		c_arith.put("+", InstructionSet.OpCode.ADD);
		c_arith.put("-", InstructionSet.OpCode.SUB); 
		c_arith.put("*", InstructionSet.OpCode.MUL);
		c_arith.put("/", InstructionSet.OpCode.DIV);
		c_arith.put("%", InstructionSet.OpCode.MOD);
	};

	private boolean handleArith(String input, String t)
	{
		InstructionSet.OpCode it = c_arith.get(t);
		if (it != null)
		{
			int i = 0;
			while (mToS.more ())
			{
//				Compiled c = new Compiled();
				if (compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
				{
//					appendCode(mCompiled, mLastFrame.mCompiled);

					if (i != 0)
						mCompiled.addInstruction(it);
					++i;
				}
				else
					break;
			}
			return true;
		}

		return false;
	}

	static private Map<String, InstructionSet.OpCode> c_binary = new HashMap<String, InstructionSet.OpCode>();
	static
	{
		c_binary.put("<", InstructionSet.OpCode.LT); 
		c_binary.put("<=", InstructionSet.OpCode.LE);
		c_binary.put(">", InstructionSet.OpCode.GT);
		c_binary.put(">=", InstructionSet.OpCode.GE);
		c_binary.put("=", InstructionSet.OpCode.EQ);
		c_binary.put("!=", InstructionSet.OpCode.NOT);
	};
	
	static private Map<String, InstructionSet.OpCode>  c_unary = new HashMap<String, InstructionSet.OpCode>();
	static
	{
		c_unary.put("!", InstructionSet.OpCode.NOT);
	};
	
	void handleUnaryBinary (String input, String t)
	{
		InstructionSet.OpCode it = c_binary.get(t);
		if (it != null)
		{
			List<Compiled> fragments = new ArrayList<Compiled>();
			{
//				Compiled c = new Compiled();
//				fragments.add(c);
				while (mToS.more () && compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
				{
//					c = new Compiled();
//					fragments.add(mLastFrame.mCompiled);
				}
				
//				fragments.remove(fragments.size() - 1);
			}
			
			if (fragments.size() > 2)
			{
				Log.e(TAG, "Greater than two arguments given to binary operator " + t + "; using first two only.");
				while (fragments.size () > 2)
					fragments.remove(fragments.size() - 1);
			}
			
			// why reverse order, possibly to match change to opcodes
			for (int i = fragments.size() - 1; i >= 0; --i)
//			for (int i = 0; i < fragments.size(); ++i)
			{
				Compiled c = fragments.get(i);
				appendCode(mCompiled, c);
			}
			
			if (it == InstructionSet.OpCode.NOT)
				mCompiled.addInstruction(InstructionSet.OpCode.EQ);

			mCompiled.addInstruction(it);
		}
		else
		{
			it = c_unary.get(t);
			if (it != null)
			{
				List<Compiled> fragments = new ArrayList<Compiled>();
				{
//					Compiled c = new Compiled();
//					fragments.add(c);
					while (mToS.more ()	&& compileLispFragment(new Frame(mToS.rest(), mFrames.peek())))
					{
//						c = new Compiled();
//						fragments.add(mLastFrame.mCompiled);
					}
	
//					fragments.remove(fragments.size() - 1);
				}
				
				if (fragments.size() > 2)
				{
					Log.e(TAG, "Greater than one argument given to unary operator " + t + "; using first only.");
					while (fragments.size () > 1)
						fragments.remove(fragments.size() - 1);
				}
				
				// why reverse order, possibly to match change to opcodes
				for (int i = fragments.size() - 1; i >= 0; --i)
//				for (int i = 0; i < fragments.size(); ++i)
				{
					Compiled c = fragments.get(i);
					appendCode(mCompiled, c);
				}

				mCompiled.addInstruction(it);
			}
			else 
				Log.e(TAG, "Unknown assembler token " + t);
		}		
	}
	
	void appendCode(Compiled compiled, Compiled c)
	{
		for (Integer i: c.getLocs())
		{
			if (mLogging)
				Log.d(TAG, String.format("shift appending code by %d", compiled.getCode().size()));
			
			c.getCode().set(i, c.getCode().get(i).add(new u256(compiled.getCode().size())));	// relocation?

			int k = i + compiled.getCode().size();

			if (mLogging)
				Log.d(TAG, String.format("appendLoc %d (%d)", i, k));
			
			compiled.getLocs().add(k);
		}
		for (u256 i: c.getCode().getList())
		{
			int k = i.mValue.intValue();
			
			if (mLogging)
				Log.d(TAG, String.format("appendCode %s (%s or %d)", 
						i.toString(),
						k < InstructionSet.OpCode.values().length ? InstructionSet.OpCode.values()[k] : "n/a",
						k));
			
			compiled.getCode().add(i);
		}
		
		if (mLogging)
			Log.d(TAG, "After appending code\n" + Disassembler.run(compiled.getCode()));
	}


	String readQuoted(String s)
	{
		String ret = "";
		boolean escaped = false;
		int d = 0;
		for ( ; d < s.length()
				&& (escaped || s.charAt(d) != '"'); ++d)
			if ( ! escaped && s.charAt(d) == '\\')
				escaped = true;
			else
				ret += s.charAt(d);
		if (d < s.length())
			++d;	// skip last "

		if (ret.length() > 32)
		{
//			if (mLogging)
				Log.e(TAG, "String literal > 32 characters. Cropping.");
			ret = ret.substring(0, 32);
		}

		return ret;
	}

	static u256 readNumeric(String s)
	{
		u256 x = new u256(1);
		for (Pair<CommonEth.u256, String> i: CommonEth.units())
			if (s.endsWith(i.getValue1()))
			{
				s = s.substring(0, s.length() - i.getValue1().length());
				x = i.getValue0();
				break;
			}
		
//		try
//		{
			return x.mult(new u256(s));
//		}
//		catch (...)
//		{
//			if (!_quiet)
//				cwarn << "Invalid numeric" << _v;
//		}
//		return 0;
	}

}
