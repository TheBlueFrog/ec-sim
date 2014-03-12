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
		u256s o_code = new u256s();
		List<Integer> o_locs = new ArrayList<Integer>();
		
		compileLispFragment(_code, o_code, o_locs);
		
		Log.d(TAG, disassemble(o_code));
		return o_code;
	}

	String disassemble(u256s _mem)
	{
		StringBuilder sb = new StringBuilder();
		int numerics = 0;
		for (u256 it : _mem.getList())
		{
			OpCode iit = InstructionSet.OpCode.parse(it.mValue);
			if ((numerics > 0) || (iit == null) )//|| (u256)(uint)iit->first != n)	// not an instruction or expecting an argument...
			{
				if (numerics > 0)
					numerics--;
				
				sb.append("0x")
					.append(it.mValue.toString(16))
					.append(" ");
			}
			else
			{
				InstructionSet.Info info = InstructionSet.c_instructionInfo.get(iit);
				sb.append(info.name).append(" ");
				numerics = info.additional;
			}
		}
		return sb.toString();
	}

	
	private String c_allowed = "+-*/%<>=!";

	private Input mToS;
	
	private boolean compileLispFragment (String input, u256s o_code, List<Integer> o_locs)
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
									handleBareLoad(input, token, o_code, o_locs, exec, literalValue);
								else if ( ! handleExpression (input, token, o_code, o_locs, exec))
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

	private boolean handleExpression(String input, String t, u256s o_code, List<Integer> o_locs, boolean exec)
	{
		t = t.toUpperCase();

		if ("IF".equals (t) && ( ! handleIf(input, t, o_code, o_locs)))
			return false;
		else if (("WHEN".equals (t) || "UNLESS".equals (t)) && ( ! handleWhen(input, t, o_code, o_locs)))
			return false;
		else if (("FOR".equals (t)) && ( ! handleFor(input, t, o_code, o_locs)))
			return false;
		else if (("SEQ".equals (t)) && ( ! handleSeq(input, t, o_code, o_locs)))
			return false;
		else if (("AND".equals (t)) && ( ! handleAnd(input, t, o_code, o_locs)))
			return false;
		else if (("OR".equals (t)) && ( ! handleOr (input, t, o_code, o_locs)))
			return false;
		else if ( ! handleOpCode (input, t, o_code, o_locs, exec))
		{
			if ( ! handleArith(input, t, o_code, o_locs))
			{
				handleUnaryBinary (input, t, o_code, o_locs);
			}
		}
		
		return true;
	}

	private void handleBareLoad(String input, String t, u256s o_code, List<Integer> o_locs, boolean exec, u256 literalValue)
	{
		boolean bareLoad = true;
		if (exec)
		{
			u256s codes = new u256s();
			List<Integer> locs = new ArrayList<Integer>();
			
			if (compileLispFragment(mToS.rest(), codes, locs))
			{
				appendCode(o_code, o_locs, codes, locs);
				
				while (compileLispFragment(mToS.rest(), codes, locs))
					if (! mQuiet)
						Log.e(TAG, "Additional items in bare store. Ignoring.");

				bareLoad = false;
			}
		}
		o_code.add(InstructionSet.OpCode.PUSH.ordinal());
		o_code.add(literalValue);
		if (exec)
			o_code.add(bareLoad ? InstructionSet.OpCode.SLOAD.ordinal() : InstructionSet.OpCode.SSTORE.ordinal());
	}

	private boolean handleSeq(String input, String t, u256s o_code, List<Integer> o_locs)
	{
		while (mToS.more ())
		{
			u256s codes = new u256s();
			List<Integer> locs = new ArrayList<Integer>();
			if (compileLispFragment(mToS.rest(), codes, locs))
				appendCode(o_code, o_locs, codes, locs);
			else
				return false;
		}
		return true;
	}

	private boolean handleIf(String input, String t, u256s o_code, List<Integer> o_locs)
	{
		// Compile all the code...
		List<u256s> codes = new ArrayList<u256s>();
		List<List<Integer>> locs = new ArrayList<List<Integer>>();
		for (int i = 0; i < 4; ++i)
		{
			u256s c = new u256s();
			List<Integer> ll = new ArrayList<Integer>();
			codes.add (c);
			locs.add(ll);
			if ( ! compileLispFragment(mToS.rest(), c, ll))
				return false;

//			if (compileLispFragment(input.substring(d), _quiet, codes[3], locs[3]))
//			return false;
		}

		// Push the positive location.
		o_code.add(InstructionSet.OpCode.PUSH.ordinal());
		int posLocation = o_code.size();
		o_locs.add(posLocation);
		o_code.add(0);

		// First fragment - predicate
		appendCode(o_code, o_locs, codes.get(0), locs.get(0));

		// Jump to positive if true.
		o_code.add(InstructionSet.OpCode.JMPI.ordinal());

		// Second fragment - negative.
		appendCode(o_code, o_locs, codes.get(2), locs.get(2));

		// Jump to end after negative.
		o_code.add(InstructionSet.OpCode.PUSH.ordinal());
		int endLocation = o_code.size();
		o_locs.add(endLocation);
		o_code.add(0);
		o_code.add(InstructionSet.OpCode.JMP.ordinal());

		// Third fragment - positive.
		o_code.set(posLocation, o_code.size());
		appendCode(o_code, o_locs, codes.get(1), locs.get(1));

		// At end now.
		o_code.set(endLocation, o_code.size());
		return true;
	}

	private boolean handleWhen(String input, String t, u256s o_code, List<Integer> o_locs)
	{
		// Compile all the code...
		List<u256s> codes = new ArrayList<u256s>();
		List<List<Integer>> locs = new ArrayList<List<Integer>>();
		for (int i = 0; i < 2; ++i)
		{
			u256s c = new u256s();
			List<Integer> ll = new ArrayList<Integer>();
			codes.add (c);
			locs.add(ll);
			if ( ! compileLispFragment(mToS.rest(), c, ll))
				return false;
		}
		
		// Push the positive location.
		o_code.add(InstructionSet.OpCode.PUSH.ordinal());
		int endLocation = o_code.size();
		o_locs.add(endLocation);
		o_code.add(0);

		// First fragment - predicate
		appendCode(o_code, o_locs, codes.get(0), locs.get(0));

		// Jump to end...
		if (t == "WHEN")
			o_code.add(InstructionSet.OpCode.NOT.ordinal());
		o_code.add(InstructionSet.OpCode.JMPI.ordinal());

		// Second fragment - negative.
		appendCode(o_code, o_locs, codes.get(1), locs.get(1));

		// At end now.
		o_code.set(endLocation, o_code.size());
		return true;
	}

	private boolean handleFor(String input, String t, u256s o_code, List<Integer> o_locs)
	{
		// Compile all the code...
		List<u256s> codes = new ArrayList<u256s>();
		List<List<Integer>> locs = new ArrayList<List<Integer>>();
		for (int i = 0; i < 3; ++i)
		{
			u256s c = new u256s();
			List<Integer> ll = new ArrayList<Integer>();
			codes.add (c);
			locs.add(ll);
			if ( ! compileLispFragment(mToS.rest(), c, ll))
				return false;
//		if (compileLispFragment(d, e, _quiet, codes[2], locs[2]))
//			return false;
		}
		
		int startLocation = o_code.size();

		// Push the positive location.
		o_code.add(InstructionSet.OpCode.PUSH.ordinal());
		int endInsertion = o_code.size();
		o_locs.add(endInsertion);
		o_code.add(0);

		// First fragment - predicate
		appendCode(o_code, o_locs, codes.get(0), locs.get(0));

		// Jump to positive if true.
		o_code.add(InstructionSet.OpCode.NOT.ordinal());
		o_code.add(InstructionSet.OpCode.JMPI.ordinal());

		// Second fragment - negative.
		appendCode(o_code, o_locs, codes.get(1), locs.get(1));

		// Jump to end after negative.
		o_code.add(InstructionSet.OpCode.PUSH.ordinal());
		o_locs.add(o_code.size());
		o_code.add(startLocation);
		o_code.add(InstructionSet.OpCode.JMP.ordinal());

		// At end now.
		o_code.set(endInsertion, o_code.size());
		return true;
	}

	private boolean handleAnd(String input, String t, u256s o_code, List<Integer> o_locs)
	{		
		u256s codes = new u256s();
		List<Integer> locs = new ArrayList<Integer>();
		while (mToS.more ())
		{
//			codes.resize(codes.size() + 1);
//			locs.resize(locs.size() + 1);
			if ( ! compileLispFragment(mToS.rest(), codes, locs))
				return false;
		}

		// last one is empty.
		if (codes.size() < 2)
			return false;

		codes.removeLast();
		locs.remove(locs.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (codes.size() > 1)
		{
			o_code.add(InstructionSet.OpCode.PUSH.ordinal());
			o_code.add(0);

			for (int i = 1; i < codes.size(); ++i)
			{
				// Push the false location.
				o_code.add(InstructionSet.OpCode.PUSH.ordinal());
				ends.add(o_code.size());
				o_locs.add(ends.get(ends.size() - 1));
				o_code.add(0);

				// Check if true - predicate
				appendCode(o_code, o_locs, codes, locs);	// -1 thing

				// Jump to end...
				o_code.add(InstructionSet.OpCode.NOT.ordinal());
				o_code.add(InstructionSet.OpCode.JMPI.ordinal());
			}
			o_code.add(InstructionSet.OpCode.POP.ordinal());
		}

		// Check if true - predicate
		appendCode(o_code, o_locs, codes, locs); // -1 thing

		// At end now.
		for (Integer i: ends)
			o_code.set(i, o_code.size());

		return true;
	}
	
	private boolean handleOr(String input, String t, u256s o_code, List<Integer> o_locs)
	{
		u256s codes = new u256s();
		List<Integer> locs = new ArrayList<Integer>();
		while (mToS.more ())
		{
//			codes.resize(codes.size() + 1);
//			locs.resize(locs.size() + 1);
			if (!compileLispFragment(mToS.rest(), codes, locs))
				return false;
		}

		// last one is empty.
		if (codes.size() < 2)
			return false;

		codes.removeLast();
		locs.remove(locs.size() - 1);

		List<Integer> ends = new ArrayList<Integer>();

		if (codes.size() > 1)
		{
			o_code.add(InstructionSet.OpCode.PUSH.ordinal());
			o_code.add(1);

			for (int i = 1; i < codes.size(); ++i)
			{
				// Push the false location.
				o_code.add(InstructionSet.OpCode.PUSH.ordinal());
				ends.add(o_code.size());
				o_locs.add(ends.get(ends.size() - 1));
				o_code.add(0);

				// Check if true - predicate
				appendCode(o_code, o_locs, codes, locs);	// something about -1

				// Jump to end...
				o_code.add(InstructionSet.OpCode.JMPI.ordinal());
			}
			o_code.add(InstructionSet.OpCode.POP.ordinal());
		}

		// Check if true - predicate
		appendCode(o_code, o_locs, codes, locs);	// and here

		// At end now.
		for (Integer i: ends)
			o_code.set(i, o_code.size());

		return true;
	}
	
	private boolean handleOpCode (String input, String t, u256s o_code, List<Integer> o_locs, boolean exec)
	{
		InstructionSet.OpCode it = InstructionSet.OpCode.parse(t);
		if (it != null)
		{
			if (exec)
			{
				List<Pair<u256s, List<Integer>>> fragments = new ArrayList<Pair<u256s, List<Integer>>>();
				Pair<u256s, List<Integer>> p = new Pair<u256s, List<Integer>>(new u256s(), new ArrayList<Integer>());
				fragments.add(p);
				while (mToS.more ()
						&& compileLispFragment(mToS.rest(), p.getValue0(), p.getValue1()))
				{
					p = new Pair<u256s, List<Integer>>(new u256s(), new ArrayList<Integer>());
					fragments.add(p);
				}

				for (Pair<u256s, List<Integer>> c : fragments)
					appendCode(o_code, o_locs, c.getValue0(), c.getValue1());
				o_code.add(it.ordinal());
			}
			else
			{
				o_code.add(InstructionSet.OpCode.PUSH.ordinal());
				o_code.add(it.ordinal());
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

	private boolean handleArith(String input, String t, u256s o_code, List<Integer> o_locs)
	{
		InstructionSet.OpCode it = c_arith.get(t);
		if (it != null)
		{
			int i = 0;
			while (mToS.more ())
			{
				u256s codes = new u256s();
				List<Integer> locs = new ArrayList<Integer>();
				if (compileLispFragment(mToS.rest(), codes, locs))
				{
					appendCode(o_code, o_locs, codes, locs);
					if (i != 0)
						o_code.add(it.ordinal());
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
	
	void handleUnaryBinary (String input, String t, u256s o_code, List<Integer> o_locs)
	{
		InstructionSet.OpCode it = c_binary.get(t);
		if (it != null)
		{
			List<Pair<u256s, List<Integer>>> fragments = new ArrayList<Pair<u256s, List<Integer>>>();
			Pair<u256s, List<Integer>> p = new Pair<u256s, List<Integer>>(new u256s(), new ArrayList<Integer>());
			fragments.add(p);
			while (mToS.more ()
					&& compileLispFragment(mToS.rest(), p.getValue0(), p.getValue1()))
			{
				p = new Pair<u256s, List<Integer>>(new u256s(), new ArrayList<Integer>());
				fragments.add(p);
			}
			
			fragments.remove(fragments.size() - 1);
			
			int i = fragments.size();
			if (i > 2)
				Log.d(TAG, "Greater than two arguments given to binary operator " + t + "; using first two only.");
			
			for (Pair<u256s, List<Integer>> c : fragments)
				if (--i < 2)
					appendCode(o_code, o_locs, c.getValue0(), c.getValue1());
			
			if (it == InstructionSet.OpCode.NOT)
				o_code.add(InstructionSet.OpCode.EQ.ordinal());
			o_code.add(it.ordinal());
		}
		else
		{
			it = c_unary.get(t);
			if (it != null)
			{
				List<Pair<u256s, List<Integer>>> fragments = new ArrayList<Pair<u256s, List<Integer>>>();
				Pair<u256s, List<Integer>> p = new Pair<u256s, List<Integer>>(new u256s(), new ArrayList<Integer>());
				fragments.add(p);
				while (mToS.more ()
						&& compileLispFragment(mToS.rest(), p.getValue0(), p.getValue1()))
				{
					p = new Pair<u256s, List<Integer>>(new u256s(), new ArrayList<Integer>());
					fragments.add(p);
				}

				fragments.remove(fragments.size() - 1);
				
				int i = fragments.size();
				if (i > 1)
					Log.d(TAG, "Greater than one argument given to unary operator " + t + "; using first only.");
				
				for (Pair<u256s, List<Integer>> c : fragments)
					if (--i < 1)
						appendCode(o_code, o_locs, c.getValue0(), c.getValue1());
				o_code.add(it.ordinal());
			}
			else if ( ! mQuiet)
				Log.d(TAG, "Unknown assembler token " + t);
		}
		
	}
	
	void appendCode(u256s o_code, List<Integer> o_locs, u256s _code, List<Integer> _locs)
	{
		for (Integer i: _locs)
		{
			Log.d(TAG, String.format("shift appending code by %d", o_code.size()));
			_code.set(i, _code.get(i).add(new u256(o_code.size())));	// relocation?

			int k = i + o_code.size();
			Log.d(TAG, String.format("appendLoc %d (%d)", i, k));
			o_locs.add(k);
		}
		for (u256 i: _code.getList())
		{
			long j = i.mValue.longValue();
			int k = i.mValue.intValue();
			
			Log.d(TAG, String.format("appendCode %s (%s)", 
					i.toString(),
					InstructionSet.OpCode.values()[k]));
			o_code.add(i);
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
