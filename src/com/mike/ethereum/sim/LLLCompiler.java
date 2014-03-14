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
	
	public LLLCompiler ()
	{		
	}
	
	boolean mQuiet = true;
	Deque<Input> mInput = new ArrayDeque<Input>();
	
	public u256s compileLisp(String _code, boolean _quiet)
	{
		mQuiet = _quiet;
		
//		char const* d = _code.data();
//		char const* e = _code.data() + _code.size();
		Compiled c = new Compiled();
		
		compileLispFragment(_code, c);
		
		return c.getCode();
	}

	private String c_allowed = "+-*/%<>=!";

	private Input mToS;
	
	/**
	 * collect information as we compile
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


	private boolean compileLispFragment (String input, Compiled compiled)
	{
		mInput.push(new Input(input));
		mToS = mInput.peek();
		
		boolean exec = false;

		while (mToS.more ())
		{
			skipToToken();
			
			if (mToS.more ())
			{
				switch (mToS.ch())
				{
				case ';':
					skipToEoL(input);
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
						return false;// unexpected - return false as we don't know what to do with it.
					}
					
				default:
					{
						u256 literalValue = null;
						String token = null;
			
						if (mToS.ch() == '"')
							literalValue = handleLiteral(input);
						else
						{
							token = mToS.getToken();
							if (token.length() > 0)
							{
								if (Character.isDigit(token.charAt(0)))
									literalValue = readNumeric(token);
								
								if (literalValue != null)
									handleBareLoad(input, token, compiled, exec, literalValue);
								else if ( ! handleKeywordOpCode (input, token, compiled, exec))
									break;							
							}
						}
	
						if ( ! exec)
						{
							pop();
							return true;
						}
					}
					break;
				}
			}
		}

		pop();
		return false;
	}

	private void pop()
	{
		int consumed = mInput.peek().mI;
		mInput.pop();
		if ( ! mInput.isEmpty())
			mInput.peek().advance (consumed);
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

	private boolean handleKeywordOpCode(String input, String t, Compiled compiled, boolean exec)
	{
		t = t.toUpperCase();

		if ("IF".equals (t)) 
		{
			if ( ! handleIf(input, t, compiled))
				return false;
		}
		else if (("WHEN".equals (t)) || "UNLESS".equals (t))
		{
			if ( ! handleWhen(input, t, compiled))
				return false;
		}
		else if ("FOR".equals (t)) 
		{
			if ( ! handleFor(input, t, compiled))
				return false;
		}
		else if ("SEQ".equals (t))
		{
			if ( ! handleSeq(input, t, compiled))
				return false;
		}
		else if ("AND".equals (t))
		{
			if ( ! handleAnd(input, t, compiled))
				return false;
		}
		else if ("OR".equals (t))
		{
			if ( ! handleOr (input, t, compiled))
				return false;
		}
		else if ( ! handleOpCode (input, t, compiled, exec))
		{
			if ( ! handleArith(input, t, compiled))
			{
				handleUnaryBinary (input, t, compiled);
			}
		}
		
		return true;
	}

	private void handleBareLoad(String input, String t, Compiled compiled, boolean exec, u256 literalValue)
	{
		boolean bareLoad = true;
		if (exec)
		{
			Compiled c = new Compiled();
			
			if (compileLispFragment(mToS.rest(), c))
			{
				appendCode(compiled, c);
				
				Compiled j = new Compiled();
				while (compileLispFragment(mToS.rest(), j))
					if (! mQuiet)
						Log.e(TAG, "Additional items in bare store. Ignoring.");

				bareLoad = false;
			}
		}
		
		compiled.addPushInstructionWith(literalValue);
		
		if (exec)
			compiled.addInstruction(bareLoad ? InstructionSet.OpCode.SLOAD : InstructionSet.OpCode.SSTORE);
	}

	private boolean handleSeq(String input, String t, Compiled compiled)
	{
		while (mToS.more ())
		{
			Compiled c = new Compiled();
			if (compileLispFragment(mToS.rest(), c))
				appendCode(compiled, c);
			else
				return false;
		}
		return true;
	}

	private boolean handleIf(String input, String t, Compiled compiled)
	{
		// Compile all the code...
		List<Compiled> fragments = new ArrayList<Compiled>();
		for (int i = 0; i < 3; ++i)
		{
			Compiled c = new Compiled();
			fragments.add (c);

			if ( ! compileLispFragment(mToS.rest(), c))
				return false;
		}

		// save first patch location and push a placeholder
		// for the positive code location
		int patch1 = compiled.addPushInstructionWith(0);
		compiled.addLoc(patch1);

		// output predicate, it will leave zero/nonzero on tos
		appendCode(compiled, fragments.get(0));

		// jump to positive if nonzero
		compiled.addInstruction(InstructionSet.OpCode.JMPI);

		// output false code block
		appendCode(compiled, fragments.get(2));

		// unconditional jump to after true code block
		int patch2 = compiled.addPushInstructionWith(0);
		compiled.addLoc(patch2);
		compiled.addInstruction(InstructionSet.OpCode.JMP);

		// at start of true code block, patch first jump
		compiled.setCode(patch1, compiled.getCode().size());
		
		// output true code block
		appendCode(compiled, fragments.get(1));

		// patch conditional jump at end of false block
		compiled.setCode(patch2, compiled.getCode().size());
		return true;
	}

	private boolean handleWhen(String input, String t, Compiled compiled)
	{
		// Compile all the code...
		List<Compiled> fragments = new ArrayList<Compiled>();
		for (int i = 0; i < 2; ++i)
		{
			Compiled c = new Compiled();
			fragments.add (c);
			if ( ! compileLispFragment(mToS.rest(), c))
				return false;
		}
		
		// record where we have to patch to get past this
		// whole statement
		int patch1 = compiled.addPushInstructionWith(0);
		compiled.addLoc(patch1);

		// output the predicate that will leave zero/nonzero on the
		// stack
		appendCode(compiled, fragments.get(0));

		if (t == "WHEN")
			compiled.addInstruction(InstructionSet.OpCode.NOT);

		// jump if nonzero to the location to be patched 
		compiled.addInstruction(InstructionSet.OpCode.JMPI);

		// append conditional code
		appendCode(compiled, fragments.get(1));

		// patch now that we know where we end
		compiled.setCode(patch1, compiled.getCode().size());
		return true;
	}

	/** closer to a while statement, (while (predicate) (body)) 
	 * @param input
	 * @param t
	 * @param compiled
	 * @return
	 */
	private boolean handleFor(String input, String t, Compiled compiled)
	{
		// Compile all the code...
		List<Compiled> fragments = new ArrayList<Compiled>();
		for (int i = 0; i < 2; ++i)
		{
			Compiled c = new Compiled();
			fragments.add (c);
			if ( ! compileLispFragment(mToS.rest(), c))
				return false;
//		if (compileLispFragment(d, e, _quiet, codes[2], locs[2]))
//			return false;
		}
		
		int startLocation = compiled.getCode().size();

		// setup location to jump around this statement
		int patch1 = compiled.addPushInstructionWith(0);
		compiled.addLoc(patch1);

		// output predicate, it leaves zero/nonzero on stack
		appendCode(compiled, fragments.get(0));

		// jump out of this statement if zero on stack
		compiled.addInstruction(InstructionSet.OpCode.NOT);
		compiled.addInstruction(InstructionSet.OpCode.JMPI);

		// output body
		appendCode(compiled, fragments.get(1));

		// jump back to beginning
		int i = compiled.addPushInstructionWith(startLocation);
		compiled.addLoc(i);
		
		compiled.addInstruction(InstructionSet.OpCode.JMP);

		// patch condition now that we know the end
		compiled.setCode(patch1, compiled.getCode().size());
		return true;
	}

	private boolean handleAnd(String input, String t, Compiled compiled)
	{		
		List<Compiled> fragments = new ArrayList<Compiled>();
		boolean more = mToS.more ();
		while (more)
		{
			Compiled c = new Compiled();
			fragments.add(c);

			if ( ! compileLispFragment(mToS.rest(), c))
				more = false;
		}

		if (fragments.size() < 2)
			return false;

		// last one is empty.
		fragments.remove(fragments.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (fragments.size() > 1)
		{
			// see with zero (false)
			compiled.addPushInstructionWith(0);

			for (int i = 1; i < fragments.size(); ++i)
			{
				// Push the false location.
				int k = compiled.addPushInstructionWith(0);
				ends.add(k);
				compiled.addLoc(k);

				// output predicate
				appendCode(compiled, fragments.get(i - 1));	

				// Jump to end if zero 
				compiled.addInstruction(InstructionSet.OpCode.NOT);
				compiled.addInstruction(InstructionSet.OpCode.JMPI);
			}

			compiled.addInstruction(InstructionSet.OpCode.POP);
		}

		// check if last one is true
		appendCode(compiled, fragments.get(fragments.size() - 1));

		// at end now, patch everyone to go here
		for (Integer i: ends)
			compiled.setCode(i, compiled.getCode().size());

		return true;
	}
	
	private boolean handleOr(String input, String t, Compiled compiled)
	{
		List<Compiled> fragments = new ArrayList<Compiled>();
		boolean more = mToS.more ();
		while (more)
		{
			Compiled c = new Compiled();
			fragments.add(c);

			if ( ! compileLispFragment(mToS.rest(), c))
				more = false;
		}

		if (fragments.size() < 2)
			return false;

		// last one is empty drop it
		fragments.remove(fragments.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (fragments.size() > 1)
		{
			// for each of the terms except the last one
			
			// first push non-zero to setup a JMPI
			compiled.addPushInstructionWith(1);

			for (int i = 1; i < fragments.size(); ++i)
			{
				// save beginning of this block
				int k = compiled.addPushInstructionWith(0);
				ends.add(k);
				compiled.addLoc(k);

				// this code block will leave zero/nonzero on tos
				appendCode(compiled, fragments.get(i - 1));

				// jump to end if nonzero (true)
				compiled.addInstruction(InstructionSet.OpCode.JMPI);
			}

			// gets here if all tests were zero, discard the 1 we pushed before
			compiled.addInstruction(InstructionSet.OpCode.POP);
		}

		// append the last one, either zero or nonzero
		appendCode(compiled, fragments.get(fragments.size() - 1));

		// at end now, patch everyone to go here
		for (Integer i: ends)
			compiled.setCode(i, compiled.getCode().size());

		return true;
	}
	
	private boolean handleOpCode (String input, String t, Compiled compiled, boolean exec)
	{
		InstructionSet.OpCode it = InstructionSet.OpCode.parse(t);
		if (it != null)
		{
			if (exec)
			{
				List<Compiled> fragments = new ArrayList<Compiled>();
				{
					Compiled c = new Compiled();
					fragments.add(c);
					while (mToS.more ()
							&& compileLispFragment(mToS.rest(), c))
					{
						Compiled c1 = new Compiled();
						fragments.add(c1);
					}
				}
				
				// why reverse order, possibly to match change to opcodes
				for (int i = fragments.size() - 1; i >= 0; --i)
//				for (int i = 0; i < fragments.size(); ++i)
				{
					Compiled c = fragments.get(i);
					appendCode(compiled, c);
				}
				
				compiled.addInstruction(it);
			}
			else
			{
				compiled.addInstruction(InstructionSet.OpCode.PUSH);
				compiled.addInstruction(it);
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

	private boolean handleArith(String input, String t, Compiled compiled)
	{
		InstructionSet.OpCode it = c_arith.get(t);
		if (it != null)
		{
			int i = 0;
			while (mToS.more ())
			{
				Compiled c = new Compiled();
				if (compileLispFragment(mToS.rest(), c))
				{
					appendCode(compiled, c);
					if (i != 0)
						compiled.addInstruction(it);
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
	
	void handleUnaryBinary (String input, String t, Compiled compiled)
	{
		InstructionSet.OpCode it = c_binary.get(t);
		if (it != null)
		{
			List<Compiled> fragments = new ArrayList<Compiled>();
			{
				Compiled c = new Compiled();
				fragments.add(c);
				while (mToS.more () && compileLispFragment(mToS.rest(), c))
				{
					c = new Compiled();
					fragments.add(c);
				}
				
				fragments.remove(fragments.size() - 1);
			}
			
			if (fragments.size() > 2)
			{
				Log.d(TAG, "Greater than two arguments given to binary operator " + t + "; using first two only.");
				while (fragments.size () > 2)
					fragments.remove(fragments.size() - 1);
			}
			
			// why reverse order, possibly to match change to opcodes
			for (int i = fragments.size() - 1; i >= 0; --i)
//			for (int i = 0; i < fragments.size(); ++i)
			{
				Compiled c = fragments.get(i);
				appendCode(compiled, c);
			}
			
			if (it == InstructionSet.OpCode.NOT)
				compiled.addInstruction(InstructionSet.OpCode.EQ);

			compiled.addInstruction(it);
		}
		else
		{
			it = c_unary.get(t);
			if (it != null)
			{
				List<Compiled> fragments = new ArrayList<Compiled>();
				{
					Compiled c = new Compiled();
					fragments.add(c);
					while (mToS.more ()	&& compileLispFragment(mToS.rest(), c))
					{
						c = new Compiled();
						fragments.add(c);
					}
	
					fragments.remove(fragments.size() - 1);
				}
				
				if (fragments.size() > 2)
				{
					Log.d(TAG, "Greater than one argument given to unary operator " + t + "; using first only.");
					while (fragments.size () > 1)
						fragments.remove(fragments.size() - 1);
				}
				
				// why reverse order, possibly to match change to opcodes
				for (int i = fragments.size() - 1; i >= 0; --i)
//				for (int i = 0; i < fragments.size(); ++i)
				{
					Compiled c = fragments.get(i);
					appendCode(compiled, c);
				}

				compiled.addInstruction(it);
			}
			else if ( ! mQuiet)
				Log.d(TAG, "Unknown assembler token " + t);
		}		
	}
	
	void appendCode(Compiled compiled, Compiled c)
	{
		for (Integer i: c.getLocs())
		{
			Log.d(TAG, String.format("shift appending code by %d", compiled.getCode().size()));
			c.getCode().set(i, c.getCode().get(i).add(new u256(compiled.getCode().size())));	// relocation?

			int k = i + compiled.getCode().size();
			Log.d(TAG, String.format("appendLoc %d (%d)", i, k));
			compiled.getLocs().add(k);
		}
		for (u256 i: c.getCode().getList())
		{
			int k = i.mValue.intValue();
			
			Log.d(TAG, String.format("appendCode %s (%s or %d)", 
					i.toString(),
					k < InstructionSet.OpCode.values().length ? InstructionSet.OpCode.values()[k] : "n/a",
					k));
			
			compiled.getCode().add(i);
		}
		
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
			if (! mQuiet)
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
