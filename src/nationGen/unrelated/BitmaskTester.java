package nationGen.unrelated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.elmokki.Generic;

public class BitmaskTester {

	public static void main(String[] args) throws Exception {
		System.out.println("Enter bitmask to be dissected. Must have L at the end of number200");
		long bitmask = 2105472L;//System.in.read();  //274880004098L;
		List<Integer> trues = new ArrayList<Integer>();
		System.out.println(bitmask + ": ");
		for(int i = 1; i < 64; i++)
		{
			if(Generic.containsLongBitmask(bitmask, (long)Math.pow(2, i)))
			{
				System.out.println(i + " -> " + Math.pow(2, i) + ": " + Generic.containsLongBitmask(bitmask, (long)Math.pow(2, i)));
				trues.add(i);
			}
		}
		System.out.println(trues);
	}

}
