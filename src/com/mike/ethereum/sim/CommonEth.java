package com.mike.ethereum.sim;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;


public class CommonEth
{
/*
	using h512 = FixedHash<64>;
	using h256 = FixedHash<32>;
	using h160 = FixedHash<20>;
	using h256s = std::vector<h256>;
	using h160s = std::vector<h160>;
	using h256Set = std::set<h256>;
	using h160Set = std::set<h160>;
	using Address = h160;
	using Addresses = h160s;
*/
	static class Address
	{
		public byte[] mH = new byte[20]; // 160 bits

		public Address (u256 i)
		{
			// take rightmost 160 bits?
		}
	}
	
	static class h256
	{
		public byte[] mH = new byte[32]; // 256 bits

		public h256 (u256 _item) 
		{
			assert false : "NYI";
		}
	}
	
	static class u160
	{
		public java.math.BigInteger mValue;

		public u160(u256 i)
		{
			mValue = i.mValue;
		}
		public u160(long i)
		{
			mValue = new BigInteger(Long.toString(i));
		}
		public u160(java.math.BigInteger i)
		{
			mValue = i;
		}
		public u160(String s)
		{
			mValue = new BigInteger(s);
		}
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
		public u256(byte[] b) 
		{
			mValue = new BigInteger(b);
		}

		public int intValue()
		{
			return mValue.intValue();
		}
		public long longValue() 
		{
			return mValue.longValue();
		}

		public u256 mult(long i)
		{
			return new u256(mValue.multiply(new BigInteger(Long.toString(i))));
		}
		public u256 mult(u256 b)
		{
			return new u256(mValue.multiply(b.mValue));
		}
		public u256 divide(u256 b) 
		{
			return new u256(mValue.divide(b.mValue));
		}
		public u256 add(int i) 
		{
			return add (new u256(i));
		}
		public u256 add(u256 b)
		{
			return new u256(mValue.add(b.mValue));
		}
		public u256 subtract(u256 b) 
		{
			return new u256(mValue.subtract(b.mValue));
		}
		public u256 mod(u256 b) 
		{
			return new u256(mValue.mod(b.mValue));
		}
		
		public boolean equal(int i) 
		{
			return mValue.compareTo(new BigInteger(Integer.toString(i))) == 0;
		}
		public boolean equal(u256 i) 
		{
			return mValue.compareTo(i.mValue) == 0;
		}
		public boolean greaterThan(int i) 
		{
			return mValue.compareTo(new BigInteger(Integer.toString(i))) > 0;
		}
		public boolean greaterThan(u256 y)
		{
			return mValue.compareTo(y.mValue) > 0;
		}
		public boolean greaterThanEqual(int i) 
		{
			return mValue.compareTo(new BigInteger(Integer.toString(i))) >= 0;
		}
		public boolean greaterThanEqual(u256 y)
		{
			return mValue.compareTo(y.mValue) >= 0;
		}
		public boolean lessThan(int i) 
		{
			return mValue.compareTo(new BigInteger(Integer.toString(i))) < 0;
		}
		public boolean lessThan(u256 y) 
		{
			return mValue.compareTo(y.mValue) < 0;
		}
		public boolean lessThanEqual(int i) 
		{
			return mValue.compareTo(new BigInteger(Integer.toString(i))) <= 0;
		}
		public boolean lessThanEqual(u256 y) 
		{
			return mValue.compareTo(y.mValue) <= 0;
		}

		@Override
		public String toString()
		{
			return mValue.toString();
		}
		
		@Override
	    public int hashCode()
	    {
	        int result = HashCodeUtils.SEED;
	        result = HashCodeUtils.hash(result, mValue);
	        return result;
	    }

		/**
		 * we are sensitive to the file name and it's status, we
		 * do not care about the server path or the File object
		 */
	    @Override
	    public boolean equals(Object obj)
	    {
	        if (this == obj)
	            return true;
	        if (obj == null)
	            return false;
	        if (getClass() != obj.getClass())
	            return false;
	        
	        u256 d = (u256) obj;
	        
			return mValue.equals(d.mValue);
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
