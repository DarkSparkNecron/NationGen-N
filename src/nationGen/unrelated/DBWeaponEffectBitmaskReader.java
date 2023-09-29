package nationGen.unrelated;

import java.io.IOException;

import com.elmokki.Generic;

public class DBWeaponEffectBitmaskReader {
	public static void main(String[] args) throws Exception
	{
		int effnbr=0;
		System.out.println("Enter effect bitmask from effect_weapon.cvs");
		effnbr=System.in.read();
		if(effnbr == 2)System.out.println("dt_norm");
		
		if(effnbr == 3)System.out.println("dt_stun");
		
		if(effnbr == 7)System.out.println("dt_poison");
		
		if(effnbr == 24)System.out.println("dt_holy");
		
		if(effnbr == 32)System.out.println("dt_large");
		
		if(effnbr == 33)System.out.println("dt_small");
		
		if(effnbr == 66)System.out.println("dt_paralyze");
		
		if(effnbr == 67)System.out.println("dt_weakness");
		
		if(effnbr == 73)System.out.println("dt_magic");
		
		if(effnbr == 74)System.out.println("dt_raise");
		
		if(effnbr == 103)System.out.println("dt_drain");
		
		if(effnbr == 104)System.out.println("dt_weapondrain");
		
		if(effnbr == 107)System.out.println("dt_demon");
		
		if(effnbr == 109)System.out.println("dt_cap");
		
		
		System.out.println("END");
		// Effect bitmasks
		/*
		long effbm = Long.parseLong(attr.GetValue(attr_id, "modifiers_mask"));
		
		
		if(Generic.containsLongBitmask(effbm, 1))
		{	
			db.setValue(key, "0", "nostr");
			db.setValue(key, "0", "bowstr");

		}
		else
		{

			if(!Generic.containsLongBitmask(effbm, 134217728) && Integer.parseInt(attr.GetValue(attr_id, "range_base", "0")) > 0)
			{
				db.setValue(key, "0", "nostr");
				db.setValue(key, "1", "bowstr");
			}
			else
			{
				db.setValue(key, "1", "nostr");
				db.setValue(key, "0", "bowstr");

			}
		}
		if(Generic.containsLongBitmask(effbm, 2))
			db.setValue(key, "1", "2h");
		else
			db.setValue(key, "0", "2h");
		
		if(Generic.containsLongBitmask(effbm, 4))
			db.setValue(key, "1", "flail");
		else
			db.setValue(key, "0", "flail");
		
		if(Generic.containsLongBitmask(effbm, 8))
			db.setValue(key, "1", "demononly");
		else
			db.setValue(key, "0", "demononly");
		
		if(Generic.containsLongBitmask(effbm, 32))
			db.setValue(key, "1", "fire");
		else
			db.setValue(key, "0", "fire");
		
		if(Generic.containsLongBitmask(effbm, 64))
			db.setValue(key, "1", "ap");
		else
			db.setValue(key, "0", "ap");
		
		
		if(Generic.containsLongBitmask(effbm, 128))
			db.setValue(key, "1", "an");
		else
			db.setValue(key, "0", "an");
		
		if(Generic.containsLongBitmask(effbm, 512))
			db.setValue(key, "1", "cold");
		else
			db.setValue(key, "0", "cold");
		
		if(Generic.containsLongBitmask(effbm, 2048))
			db.setValue(key, "1", "shock");
		else
			db.setValue(key, "0", "shock");
		
		if(Generic.containsLongBitmask(effbm, 4096))
			db.setValue(key, "1", "mrnegates");
		else
			db.setValue(key, "0", "mrnegates");
		
		if(Generic.containsLongBitmask(effbm, 32768))
			db.setValue(key, "1", "sacredonly");
		else
			db.setValue(key, "0", "sacredonly");
		
		if(Generic.containsLongBitmask(effbm, 131072))
			db.setValue(key, "1", "mind");
		else
			db.setValue(key, "0", "mind");
		
		if(Generic.containsLongBitmask(effbm, 2097152))
			db.setValue(key, "0", "magic");
		else
			db.setValue(key, "1", "magic");
		
		if(Generic.containsLongBitmask(effbm, 134217728))
			db.setValue(key, "1", "bonus");
		else
			db.setValue(key, "0", "bonus");
		
		if(Generic.containsLongBitmask(effbm, 2147483648L))
			db.setValue(key, "1", "charge");
		else
			db.setValue(key, "0", "charge");
		
		if(Generic.containsLongBitmask(effbm, 137438953472L))
			db.setValue(key, "1", "norepel");
		else
			db.setValue(key, "0", "norepel");

		if(Generic.containsLongBitmask(effbm, 274877906944L))
			db.setValue(key, "1", "dt_pierce");
		else
			db.setValue(key, "0", "dt_pierce");

		if(Generic.containsLongBitmask(effbm, 549755813888L))
			db.setValue(key, "1", "dt_blunt");
		else
			db.setValue(key, "0", "dt_blunt");
		
		if(Generic.containsLongBitmask(effbm, 1099511627776L))
			db.setValue(key, "1", "dt_slash");
		else
			db.setValue(key, "0", "dt_slash");
		
		
		if(Generic.containsLongBitmask(effbm, 2199023255552L))
			db.setValue(key, "1", "acid");
		else
			db.setValue(key, "0", "acid");
		
		
		if(Generic.containsLongBitmask(effbm, 4398046511104L))
			db.setValue(key, "1", "sizeresist");
		else
			db.setValue(key, "0", "sizeresist");
		*/
	}
}
