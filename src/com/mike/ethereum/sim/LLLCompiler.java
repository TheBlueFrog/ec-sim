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
	
	
	private class Compiled
	{
		u256s o_code = new u256s();
		List<Integer> o_locs = new ArrayList<Integer>();
		
		public Compiled ()
		{
		}

		public u256s getCode()
		{
			return o_code;
		}

		public void addInstruction(OpCode c) 
		{
			o_code.add(c.ordinal());
		}
		public void addU256(u256 v) 
		{
			o_code.add(v);
		}

		public List<Integer> getLocs() 
		{
			return o_locs;
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
				
				while (compileLispFragment(mToS.rest(), c))
					if (! mQuiet)
						Log.e(TAG, "Additional items in bare store. Ignoring.");

				bareLoad = false;
			}
		}
		
		compiled.addInstruction (InstructionSet.OpCode.PUSH);
		compiled.addU256(literalValue);
		
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
		List<Compiled> cc = new ArrayList<Compiled>();
		for (int i = 0; i < 4; ++i)
		{
			Compiled c = new Compiled();
			cc.add (c);

			if ( ! compileLispFragment(mToS.rest(), c))
				return false;

//			if (compileLispFragment(input.substring(d), _quiet, codes[3], locs[3]))
//			return false;
		}

		// Push the positive location.
		compiled.addInstruction(InstructionSet.OpCode.PUSH);
		int posLocation = compiled.getCode().size();
		o_locs.add(posLocation);
		compiled.addU256(0);

		// First fragment - predicate
		appendCode(compiled, cc.get(0));

		// Jump to positive if true.
		compiled.addInstruction(InstructionSet.OpCode.JMPI);

		// Second fragment - negative.
		appendCode(compiled, cc.get(2));

		// Jump to end after negative.
		compiled.addInstruction(InstructionSet.OpCode.PUSH);
		int endLocation = compiled.getCode().size();
		o_locs.add(endLocation);
		o_code.add(0);
		compiled.addInstruction(InstructionSet.OpCode.JMP);

		// Third fragment - positive.
		o_code.set(posLocation, compiled.getCode().size());
		appendCode(compiled, cc.get(1));

		// At end now.
		o_code.set(endLocation, compiled.getCode().size());
		return true;
	}

	private boolean handleWhen(String input, String t, Compiled compiled)
	{
		// Compile all the code...
		List<Compiled> cc = new ArrayList<Compiled>();
		for (int i = 0; i < 2; ++i)
		{
			Compiled c = new Compiled();
			cc.add (c);
			if ( ! compileLispFragment(mToS.rest(), c))
				return false;
		}
		
		// Push the positive location.
		compiled.addInstruction(InstructionSet.OpCode.PUSH);
		int endLocation = compiled.getCode().size();
		o_locs.add(endLocation);
		o_code.add(0);

		// First fragment - predicate
		appendCode(compiled, cc.get(0));

		// Jump to end...
		if (t == "WHEN")
			compiled.addInstruction(InstructionSet.OpCode.NOT);
		compiled.addInstruction(InstructionSet.OpCode.JMPI);

		// Second fragment - negative.
		appendCode(compiled, cc.get(1));

		// At end now.
		o_code.set(endLocation, compiled.getCode().size());
		return true;
	}

	private boolean handleFor(String input, String t, Compiled compiled)
	{
		// Compile all the code...
		List<Compiled> cc = new ArrayList<Compiled>();
		for (int i = 0; i < 3; ++i)
		{
			Compiled c = new Compiled();
			cc.add (c);
			if ( ! compileLispFragment(mToS.rest(), c))
				return false;
//		if (compileLispFragment(d, e, _quiet, codes[2], locs[2]))
//			return false;
		}
		
		int startLocation = compiled.getCode().size();

		// Push the positive location.
		compiled.addInstruction(InstructionSet.OpCode.PUSH);
		int endInsertion = compiled.getCode().size();
		o_locs.add(endInsertion);
		o_code.add(0);

		// First fragment - predicate
		appendCode(compiled, cc.get(0));

		// Jump to positive if true.
		compiled.addInstruction(InstructionSet.OpCode.NOT);
		compiled.addInstruction(InstructionSet.OpCode.JMPI);

		// Second fragment - negative.
		appendCode(compiled, cc.get(1));

		// Jump to end after negative.
		compiled.addInstruction(InstructionSet.OpCode.PUSH);
		o_locs.add(compiled.getCode().size());
		o_code.add(startLocation);
		compiled.addInstruction(InstructionSet.OpCode.JMP);

		// At end now.
		o_code.set(endInsertion, compiled.getCode().size());
		return true;
	}

	private boolean handleAnd(String input, String t, Compiled compiled)
	{		
		List<Compiled> cc = new ArrayList<Compiled>();
		while (mToS.more ())
		{
			Compiled c = new Compiled();
			cc.add(c);

			if ( ! compileLispFragment(mToS.rest(), c))
				return false;
		}

		if (cc.size() < 2)
			return false;

		// last one is empty.
		cc.remove(cc.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (cc.size() > 1)
		{
			compiled.addInstruction(InstructionSet.OpCode.PUSH);
			o_code.add(0);

			for (int i = 1; i < cc.size(); ++i)
			{
				// Push the false location.
				compiled.addInstruction(InstructionSet.OpCode.PUSH);
				ends.add(compiled.getCode().size());
				o_locs.add(ends.get(ends.size() - 1));
				o_code.add(0);

				// Check if true - predicate
				appendCode(compiled, cc.get(i));	

				// Jump to end...
				compiled.addInstruction(InstructionSet.OpCode.NOT);
				compiled.addInstruction(InstructionSet.OpCode.JMPI);
			}
			compiled.addInstruction(InstructionSet.OpCode.POP);
		}

		// Check if true - predicate
		appendCode(compiled, cc.get(0));

		// At end now.
		for (Integer i: ends)
			o_code.set(i, compiled.getCode().size());

		return true;
	}
	
	private boolean handleOr(String input, String t, Compiled compiled)
	{
		List<Compiled> cc = new ArrayList<Compiled>();
		while (mToS.more ())
		{
			Compiled c = new Compiled();
			cc.add(c);

			if (!compileLispFragment(mToS.rest(), c))
				return false;
		}

		if (cc.size() < 2)
			return false;

		// last one is empty.
		cc.remove(cc.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (cc.size() > 1)
		{
			compiled.addInstruction(InstructionSet.OpCode.PUSH);
			o_code.add(1);

			for (int i = 1; i < cc.size(); ++i)
			{
				// Push the false location.
				compiled.addInstruction(InstructionSet.OpCode.PUSH);
				ends.add(compiled.getCode().size());
				o_locs.add(ends.get(ends.size() - 1));
				o_code.add(0);

				// Check if true - predicate
				appendCode(compiled, cc.get(i));

				// Jump to end...
				compiled.addInstruction(InstructionSet.OpCode.JMPI);
			}
			compiled.addInstruction(InstructionSet.OpCode.POP);
		}

		// Check if true - predicate
		appendCode(compiled, cc.get(0));

		// At end now.
		for (Integer i: ends)
			o_code.set(i, compiled.getCode().size());

		return true;
	}
	
	private boolean handleOpCode (String input, String t, Compiled compiled, boolean exec)
	{
		InstructionSet.OpCode it = InstructionSet.OpCode.parse(t);
		if (it != null)
		{
			if (exec)
			{
				List<Compiled> cc = new ArrayList<Compiled>();
				{
					Compiled c = new Compiled();
					cc.add(c);
					while (mToS.more ()
							&& compileLispFragment(mToS.rest(), c))
					{
						Compiled c1 = new Compiled();
						cc.add(c1);
					}
				}
				
				for (Compiled c : cc)
					appendCode(compiled, c);
				
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
				while (mToS.more ()
						&& compileLispFragment(mToS.rest(), c))
				{
					fragments.add(new Compiled());
				}
				
				fragments.remove(fragments.size() - 1);
			}
			
			int i = fragments.size();
			if (i > 2)
			{
				Log.d(TAG, "Greater than two arguments given to binary operator " + t + "; using first two only.");
				while (fragments.size () > 2)
					fragments.remove(fragments.size() - 1);
			}
			
			for (Compiled c : fragments)
				appendCode(compiled, c);
			
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
					while (mToS.more ()
							&& compileLispFragment(mToS.rest(), c))
					{
						fragments.add(new Compiled());
					}
	
					fragments.remove(fragments.size() - 1);
				}
				
				int i = fragments.size();
				if (i > 1)
				{
					Log.d(TAG, "Greater than one argument given to unary operator " + t + "; using first only.");
					while (fragments.size () > 1)
						fragments.remove(fragments.size() - 1);
				}
				
				for (Compiled c : fragments)
					appendCode(compiled, c);

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
			
			Log.d(TAG, String.format("appendCode %s (%s)", 
					i.toString(),
					InstructionSet.OpCode.values()[k]));
			
			compiled.getCode().add(i);
		}
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
