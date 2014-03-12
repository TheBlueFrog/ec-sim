package com.mike.ethereum.sim;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;


public class CommonEth
{
	class h256
	{
		// hash 256 bits 32bytes
	}
	static class u256
	{
		public java.math.BigInteger mValue;
		
		public u256(u256 i)
		{
			mValue = i.mValue;
		}
		public u256(long i)
		{
			mValue = new BigInteger(Long.toString(i));
		}
		public u256(java.math.BigInteger i)
		{
			mValue = i;
		}
		public u256(String s)
		{
			mValue = new BigInteger(s);
		}

		public u256 mult(long i)
		{
			return new u256(mValue.multiply(new BigInteger(Long.toString(i))));
		}
		public u256 mult(u256 b)
		{
			return new u256(mValue.multiply(b.mValue));
		}
		public u256 add(u256 b)
		{
			return new u256(mValue.add(b.mValue));
		}
		
		@Override
		public String toString()
		{
			return mValue.toString();
		}
	}
	
	static class u256s
	{
		private List<u256> mList = new ArrayList<u256>();
		
		public void add (u256 a)
		{
			mList.add(a);
		}

		public void add(int i)
		{
			add(new u256(i));
		}
		
		public int size()
		{
			return mList.size();
		}

		public void set(int loc, int value)
		{
			mList.set(loc, new u256(value));
		}
		public void set(int loc, u256 value)
		{
			mList.set(loc, value);
		}

		public u256 get(int i)
		{
			return mList.get(i);
		}

		public u256 back()
		{
			return mList.get(mList.size() - 1);
		}

		public void removeLast()
		{
			mList.remove(mList.size() - 1);
		}

		public List<u256> getList()
		{
			return mList;
		}

	}
	

	static List<Pair<u256, String>> g_units = new ArrayList<>();
	static
	{
		// save a lot of cycles by doing this in the other order...
		
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000000L), "Uether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000), "Vether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000), "Dether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000000L), "Nether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000000), "Yether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L).mult(1000), "Zether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000000L), "Eether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000000), "Pether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L).mult(1000), "Tether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000000L), "Gether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000000), "Mether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L).mult(1000), "Kether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000000L), "Ether"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000000), "finney"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)).mult(1000), "szabo"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000000L)), "Gwei"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000000L)), "Mwei"));
		g_units.add(new Pair<u256, String>(
				(new u256(1000L)), "Kwei"));
		g_units.add(new Pair<u256, String>(
				(new u256(1L)), "wei"));
	};

	static public List<Pair<u256, String>> units()
	{
		return g_units;
	}

}
