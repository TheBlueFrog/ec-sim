package com.mike.ethereum.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import com.mike.ethereum.sim.CommonEth.u256;
import com.mike.ethereum.sim.CommonEth.u256s;
import com.mike.ethereum.sim.InstructionSet.OpCode;

public class Frame 
{
	private static final String TAG = Frame.class.getSimpleName();
	
	static private String c_allowed = "+-*/%<>=!";

	static private class Input
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

		@Override
		public String toString() 
		{
			return Disassembler.run(o_code);
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

	private boolean mLogging = false;
	public Input mInput = null;
	public Compiled mCompiled = null;

	public Frame mParent;
	public List<Frame> mChildren = new ArrayList<Frame>();
	
	public Frame (String s, Frame parent)
	{
		this (new Input (s), parent);
	}
	public Frame (Input input, Frame parent)
	{
		mParent = parent;
		if (parent != null)
			parent.mChildren.add(this);

		mInput = input;
		mCompiled = new Compiled();
	}

	public u256s getCompiledCode()
	{
		return mCompiled.o_code;
	}
	public void setLogging(boolean b) 
	{
		mLogging = b;
	}

	public void outputOpCode(InstructionSet.OpCode o)
	{
		mCompiled.addInstruction(o);
	}
	public void outputChild(int i)
	{
		appendCode(mCompiled, mChildren.get(i).mCompiled);
	}
	public void outputChildren()
	{
		for (Frame c : mChildren)
		{
			appendCode (mCompiled, c.mCompiled);
		}
	}
	public void outputChildrenReversed()
	{
		for (int i = mChildren.size() - 1; i >= 0; --i)
		{
			Compiled c = mChildren.get(i).mCompiled;
			appendCode(mCompiled, c);
		}
	}

	Input getToS()
	{
		return mInput;
	}
	Compiled getCompiled()
	{
		return mCompiled;
	}

	public boolean compileLispFragment ()
	{
		boolean exec = false;

		while (getToS().more ())
		{
			skipToToken();
			
			if (getToS().more ())
			{
				switch (getToS().ch())
				{
				case ';':
					skipToEoL(getToS().mS);
					break;
				case '(':
					exec = true;
					getToS().next();
					break;
				case ')':
					if (exec)
					{
						getToS().next();
						
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
			
						if (getToS().ch() == '"')
							literalValue = handleLiteral(getToS().mS);
						else
						{
							token = getToS().getToken();
							if (token.length() > 0)
							{
								if (Character.isDigit(token.charAt(0)))
									literalValue = readNumeric(token);
							}
						}

						if (literalValue != null)
							handleBareLoad(getToS().mS, token, exec, literalValue);
						else 
						{
							token = token.toUpperCase();

							if ("SEQ".equals (token))
							{
								if ( ! handleSeq(getToS().mS, token))
									break;
							}
							else if ( ! handleKeywordOpCode (getToS().mS, token, exec))
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
		if (mParent != null)
		{
			int consumed = getToS().mI;
			mParent.mInput.advance (consumed);
		}
	}
	
	private u256 handleLiteral(String input)
	{
		String s = readQuoted(getToS().rest());
//		h256 valHash;
//		memcpy(valHash.data(), s.data(), s.size());
//		memset(valHash.data() + s.size(), 0, 32 - s.size());
//		literalValue = (u256) valHash;
		return new u256(s);
	}


	private void skipToEoL(String input)
	{
		while (getToS().more () && getToS().ch() != '\n') 
			getToS().next(); 
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

		while (    getToS().more ()
				&& ( ! Character.isAlphabetic(getToS().ch()))
				&& ( ! Character.isDigit(getToS().ch())) 
				&& getToS().ch() != '(' 
				&& getToS().ch() != ')' 
				&& getToS().ch() != '_' 
				&& getToS().ch() != '"' 
				&& (c_allowed.indexOf(getToS().ch()) < 0) 
				&& getToS().ch() != ';') 
			getToS().next(); 
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
			if (new Frame(getToS().rest(), this).compileLispFragment())
			{
				while (new Frame(getToS().rest(), this).compileLispFragment())
					if (mLogging)
						Log.e(TAG, "Additional items in bare store. Ignoring.");

				bareLoad = false;
			}
		}
		
		getCompiled().addPushInstructionWith(literalValue);
		
		if (exec)
			outputOpCode(bareLoad ? InstructionSet.OpCode.SLOAD : InstructionSet.OpCode.SSTORE);
	}

	private boolean handleSeq(String input, String t)
	{
		while (getToS().more ())
		{
			if ( ! new Frame(getToS().rest(), this).compileLispFragment())
				return false;
		}
		
		outputChildren();
		
		return true;
	}

	private boolean handleIf(String input, String t)
	{
		for (int i = 0; i < 3; ++i)
		{
			if ( ! new Frame(getToS().rest(), this).compileLispFragment())
				return false;
		}

		// save first patch location and push a placeholder
		// for the positive code location
		int patch1 = getCompiled().addPushInstructionWith(0);
		getCompiled().addLoc(patch1);

		// output predicate, it will leave zero/nonzero on tos
		outputChild(0);

		// jump to positive if nonzero
		outputOpCode(InstructionSet.OpCode.JMPI);

		// output false code block
		outputChild(2);

		// unconditional jump to after true code block
		int patch2 = getCompiled().addPushInstructionWith(0);
		getCompiled().addLoc(patch2);
		outputOpCode(InstructionSet.OpCode.JMP);

		// at start of true code block, patch first jump
		getCompiled().setCode(patch1, getCompiled().getCode().size());
		
		// output true code block
		outputChild(1);

		// patch conditional jump at end of false block
		getCompiled().setCode(patch2, getCompiled().getCode().size());
		return true;
	}

	private boolean handleWhen(String input, String t)
	{
		for (int i = 0; i < 2; ++i)
		{
			if ( ! new Frame(getToS().rest(), this).compileLispFragment())
				return false;
		}
		
		// record where we have to patch to get past this
		// whole statement
		int patch1 = getCompiled().addPushInstructionWith(0);
		getCompiled().addLoc(patch1);

		// output the predicate that will leave zero/nonzero on the stack
		outputChild(0);

		if (t == "WHEN")
			getCompiled().addInstruction(InstructionSet.OpCode.NOT);

		// jump if nonzero to the location to be patched 
		outputOpCode(InstructionSet.OpCode.JMPI);

		// append conditional code
		outputChild(1);

		// patch now that we know where we end
		getCompiled().setCode(patch1, getCompiled().getCode().size());
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
		for (int i = 0; i < 2; ++i)
		{
			if ( ! new Frame(getToS().rest(), this).compileLispFragment())
				return false;
		}
		
		int startLocation = getCompiled().getCode().size();

		// setup location to jump around this statement
		int patch1 = getCompiled().addPushInstructionWith(0);
		getCompiled().addLoc(patch1);

		// output predicate, it leaves zero/nonzero on stack
		outputChild(0);

		// jump out of this statement if zero on stack
		outputOpCode(InstructionSet.OpCode.NOT);
		outputOpCode(InstructionSet.OpCode.JMPI);

		// output body
		outputChild(1);

		// jump back to beginning
		int i = getCompiled().addPushInstructionWith(startLocation);
		getCompiled().addLoc(i);
		
		outputOpCode(InstructionSet.OpCode.JMP);

		// patch condition now that we know the end
		getCompiled().setCode(patch1, getCompiled().getCode().size());
		return true;
	}

	private boolean handleAnd(String input, String t)
	{		
		boolean more = getToS().more ();
		while (more)
		{
			if ( ! new Frame(getToS().rest(), this).compileLispFragment())
				more = false;
		}

		trimDeadChildren();
		
		if (mChildren.size() < 2)
			return false;

		List<Integer> ends = new ArrayList<Integer>();

		if (mChildren.size() > 1)
		{
			// seed with zero (false)
			getCompiled().addPushInstructionWith(0);

			for (int i = 1; i < mChildren.size(); ++i)
			{
				// Push the false location.
				int k = getCompiled().addPushInstructionWith(0);
				ends.add(k);
				getCompiled().addLoc(k);

				// [0] is predicate
				// [1] is true value
				// ... don't do last term yet
				outputChild(i - 1);	

				// Jump to end if this term is false (zero)
				outputOpCode(InstructionSet.OpCode.NOT);
				outputOpCode(InstructionSet.OpCode.JMPI);
			}

			// ?
			outputOpCode(InstructionSet.OpCode.POP);
		}

		// setup JMPI if last term is true
		outputChild(mChildren.size() - 1);	

		// at end now, patch everyone to go here
		for (Integer i: ends)
			getCompiled().setCode(i, getCompiled().getCode().size());

		return true;
	}
	
	private void trimDeadChildren() 
	{
		boolean changed = false;
		do
		{
			int last = mChildren.size() - 1;
			Frame c = mChildren.get(last);
			if ((c.mChildren.size() == 0) && (c.mCompiled.o_code.size() == 0))
			{
				mChildren.remove(last);
			}
		}
		while (changed);
	}
	
	private boolean handleOr(String input, String t)
	{
		boolean more = getToS().more ();
		while (more)
		{
			if ( ! new Frame(getToS().rest(), this).compileLispFragment())
				more = false;
		}

		if (mChildren.size() < 2)
			return false;

		List<Integer> ends = new ArrayList<Integer>();

		if (mChildren.size() > 1)
		{
			// for each of the terms except the last one
			
			// first push non-zero to setup a JMPI
			getCompiled().addPushInstructionWith(1);

			for (int i = 1; i < mChildren.size(); ++i)
			{
				// save beginning of this block
				int k = getCompiled().addPushInstructionWith(0);
				ends.add(k);
				getCompiled().addLoc(k);

				// this code block will leave zero/nonzero on tos
				outputChild(i - 1);

				// jump to end if nonzero (true)
				getCompiled().addInstruction(InstructionSet.OpCode.JMPI);
			}

			// gets here if all tests were zero, discard the 1 we pushed before
			getCompiled().addInstruction(InstructionSet.OpCode.POP);
		}

		// append the last one, either zero or nonzero
		outputChild(mChildren.size() - 1);

		// at end now, patch everyone to go here
		for (Integer i: ends)
			getCompiled().setCode(i, getCompiled().getCode().size());

		return true;
	}
	
	private boolean handleOpCode (String input, String t, boolean exec)
	{
		InstructionSet.OpCode it = InstructionSet.OpCode.parse(t);
		if (it != null)
		{
			if (exec)
			{
				while (    getToS().more ()
						&& new Frame(getToS().rest(), this).compileLispFragment())
					;

				switch (it)
				{
					case MKTX:
						// match VM
						outputChild(1);
						outputChild(2);
						outputChild(0);
						break;
						
					default:
						outputChildrenReversed();
						outputOpCode(it);
						break;
				}
			}
			else
			{
				outputOpCode(InstructionSet.OpCode.PUSH);
				outputOpCode(it);
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
			for (int i = 0; i < 2; ++i)
			{
				if (getToS().more ())
					if ( ! new Frame(getToS().rest(), this).compileLispFragment())
						return false;
			}
			
			outputChildrenReversed();
			outputOpCode(it);
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
			while (getToS().more () 
					&& new Frame(getToS().rest(), this).compileLispFragment())
				;
			
			if (mChildren.size() > 2)
			{
				Log.e(TAG, "Greater than two arguments given to binary operator " + t + "; using first two only.");
			}

			outputChildrenReversed();
			
			if (it == InstructionSet.OpCode.NOT)
				outputOpCode(InstructionSet.OpCode.EQ);

			outputOpCode(it);
		}
		else
		{
			it = c_unary.get(t);
			if (it != null)
			{
				while (getToS().more ()	&& new Frame(getToS().rest(), this).compileLispFragment())
					;
				
				if (mChildren.size() > 2)
				{
					Log.e(TAG, "Greater than one argument given to unary operator " + t + "; using first only.");
				}
				
				outputChildrenReversed();
				outputOpCode(it);
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

	u256 readNumeric(String s)
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
