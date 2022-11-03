package nationGen.rostergeneration;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.ChanceDistribution;
import nationGen.entities.Colony;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
import nationGen.magic.MageGenerator;
import nationGen.misc.*;
import nationGen.nation.Nation;
import nationGen.rostergeneration.montagtemplates.TroopMontagTemplate;
import nationGen.units.Unit;

import java.util.*;

public class ColonyGenerator {
	
	//common data
	NationGen nationGen;
	private Nation nation;
	private Random r;
	NationGenAssets assets;
	private Colony colony;
	
	//national&colonial info
	private Race primary;
	private Race secondary;
	private String colonyType;
	private String colonySubType;
	public Tags specrecInfo; //contains tags for specrec
	//unit amounts
	private int max; //max amount of units for nation
	private int primmax;
	private double secondmax;
	private int randomControlKey;
	Map<String, Integer> maxamounts = new HashMap<>();
	Map<String, Integer> amounts = new HashMap<>();
	
	
	public ColonyGenerator(NationGen g, Nation n, NationGenAssets assets, Colony col, String colType, int randControlKey)
	{
		nationGen = g;
		nation = n;
		this.r = new Random(n.random.nextInt());
		colonyType=colType;
		this.specrecInfo = new Tags();
		//setSpecrecTag();
		randomControlKey=randControlKey;
		colony=col;
		primary = nation.races.get(0);
		secondary = nation.races.get(1);
	}
	

	public void getUWColonyType(Race primary, Race secondary)
	{
		String Res = "none";
		double chance = r.nextDouble(1);
		
		Boolean primCanBuild = false;
		Boolean secCanBuild = false;
		
		//checking which races can build forts
		for(Command c : primary.nationcommands)
			if(c.command.equals("#uwbuild"))
				primCanBuild=true;
		for(Command c : secondary.nationcommands)
			if(c.command.equals("#uwbuild"))
				secCanBuild=true;
		
		Boolean primCanUw = false;
		Boolean secCanUw = false;
		
		if(primCanBuild||secCanBuild)
		{
			//checking primary race for being capable uw
			if(traitPresent(primary,"#amphibian")||traitPresent(primary,"#pooramphibian"))
				primCanUw=true;
			//checking secondary race for being capable uw
			if(traitPresent(secondary,"#amphibian")||traitPresent(secondary,"#pooramphibian"))
				secCanUw=true;
			
			//if(secCanBuild&&secCanUw)Res="secrace";
			if(primCanBuild&&secCanUw||secCanBuild&&secCanUw)Res="secrace";
			if(primCanBuild&&primCanUw)Res="primrace";
			if(primCanBuild&&primCanUw&&secCanUw)Res="mixrace"; //all times big (have priest&mage)
			if(Res=="mixrace"&&chance<=0.33)Res="secrace";
			if(Res=="mixrace"&&chance<=0.66&&chance>0.33)Res="primrace";
			if(chance>0.15&&chance<0.48&&(Res=="primrace"||Res=="secrace"))Res+=":big"; //~50% chance for turned mixed, and 33% for reqular
		//bebug
		System.out.println("uwcolony type is: "+Res+", primcanuw="+primCanUw+", seccanuw="+secCanUw);
		}
		
			
		colonySubType = Res;
	}
	
	public String getColonySubType(List<String> checkTraits)
	{
		String res = "none";
		double chance = r.nextDouble(1);
		
		boolean primGood=false;
		boolean secGood=false;
		
		for(String s:checkTraits)
		{
			if(traitPresent(primary, s))
			{
				primGood=true;
			}else primGood=false;
			if(traitPresent(secondary, s))
			{
				secGood=true;
			}else secGood=false;
		}
		
		recieveMaxes(randomControlKey);
		//should i recheck uwbuild for races?..
		//before it was checking primrace only and idk which uwbuild are applyed for the nation
		
		if(primGood&&secGood)
		{
			//mixed
			//but maybe also not-mixed as possibility
			if(chance>=0.33)// 2/3, all other chances are 1\2
			{
				res="mixrace";
				if(secondmax==0)res+="_all";
			}else
			{
				if(chance<=0.165)
				{
					res="primrace";
					if(chance>=0.0825)res+="_big";
				}else
				{
					res="secrace";
					if(chance>=0.2475)res+="_big";
					if(secondmax==0)res+="_all";
				}
			}
			
		}else
		{
			//non-mixed then
			if(primGood)
			{
				//primrace
				res="primrace";
				if(chance>=0.5)res+="_big";
			}
			if(secGood)
			{
				//secrace
				res="secrace";
				if(chance>=0.5)res+="_big";
				if(secondmax==0)res+="_all";
			}
		}
		colonySubType=res; //for both colonygen and colony
		return res;
	}
	//add here tangs when new colony types will arrive
	private void setSpecrecTag()
	{
		specrecInfo.add("colony", "");
	  if(colonyType=="uw")
		  specrecInfo.add("specrec", "uw");
	}
	
	public void recieveMaxes(int maxu, int primmaxu, double secmaxu)
	{
		primmax=primmaxu;
		max=maxu;
	    secondmax=secmaxu;
	}
	//this method predicts maxes by using in setup the same integer for random initialization
	//probably classic troopgen, but without execution
	public void recieveMaxes(int controllingRandomKey)
	{
		RosterGenerator tempRostGen = new RosterGenerator(nationGen,nation,assets,controllingRandomKey);
		tempRostGen.setup(tempRostGen.GetClassicMaxamounts());
		max=tempRostGen.getMaxUnits();
		primmax=tempRostGen.getMaxPrimaryUnits();
		secondmax=tempRostGen.getMaxSecondaryUnits();
		tempRostGen = null;
		System.gc();
	}
	
	//changes max, primmax and secondmax to affect generation of the main nation roster
	public void setMaxes(RosterGenerator ro)
	{
		max=ro.getMaxUnits();
		primmax=ro.getMaxPrimaryUnits();
		secondmax=ro.getMaxSecondaryUnits();
		
		String SubType;
		Boolean BigModifier=false;
		double primsteal = 0;
		double secsteal = 0;
		double totalsteal = 0;
		
		BigModifier=colonySubType.endsWith("big");
		
		int l=colonySubType.length();
		SubType=colonySubType.split(":")[0];
		
		if(colonyType=="uwcolony"&&colonySubType!="none")
		{
			//if secrace has <=25% it steals all secrace units
			//big colony can steal up to 50% of units if both races are amph or its just primerace and no secrace
			//reqular colony steals 25%-33%
			Boolean SecLowPop = true;
			if(secondmax/max>0.25)SecLowPop=false;
			/*if(SecLowPop)
			{
				secsteal=secondmax;
				secondmax=0;
			}*/
			
			double StealCoef=0;
			double StealMod = 1;
			
			if(BigModifier)StealCoef=0.33; else StealCoef=0.2;
			//this is because for some reason this values for some times are extremely big and generation is certainly limited by max due to values being bigger than it
			//without it after stealing max will be negative
			if(primmax>=max)primmax=max;
			if(secondmax>=max)secondmax=max;
			
			if(SubType=="primrace")
			{
				//steal 1\3 if big, or even 50% for non mixed.
				//steal 1\4-1\5 if not big.
			   primsteal=Math.round(primmax*StealCoef*StealMod);
			   System.out.println(primsteal+"="+primmax*StealCoef*StealMod+"("+primmax+","+StealCoef+","+StealMod+")");
			   primmax-=primsteal;
			}
				else if(SubType=="secrace")
				{
					if(SecLowPop)StealCoef=1;
					secsteal=Math.round(secondmax*StealCoef*StealMod);
					secondmax-=secsteal;
				}else if(SubType=="mixrace")
				{
					StealMod=0.5; //because we stealing units from both places, colony is twice as big, which is should not be
					primsteal=Math.round(primmax*StealCoef*StealMod);
					if(SecLowPop)
						{StealCoef=1; StealMod=1;}
					secsteal=Math.round(secondmax*StealCoef*StealMod);
					primmax-=primsteal;
					secondmax-=secsteal;
				}else System.out.println("colonySubType error! its unreadable: "+colonySubType+" | "+SubType);
		}
		totalsteal=primsteal+secsteal;
		max-=totalsteal;
		//bebug
		System.out.println("Steal calk: "+colonySubType+", max "+max+","+", primmax "+primmax+", secmax "+secondmax);
		System.out.println("primasteal: "+primsteal+", secsteal: "+secsteal+", total: "+totalsteal);
	}
	
	public int returnNewMax()
	{
		return max;
	}
	public int returnNewPrimMax()
	{
		return primmax;
	}
	public double returnNewSecMax()
	{
		return secondmax;
	}
	
	public Map<String, Integer> getUwMaxamounts()
	{
		Map<String, Integer> maxamounts = new HashMap<>();
		// 0-2 ranged maximum
		maxamounts.put("ranged",r.nextInt(2)); // 0-2
		maxamounts.put("infantry", 6);
		//no mounted as currenty is a lack if uw mounts
		maxamounts.put("mounted", 0);
		maxamounts.put("chariot", 0);
		return maxamounts;
	}
	
	private boolean traitPresent(Race race, String traitCommand)
	{
		boolean yes=false;
		for(Command c : race.specialcommands)
			if(c.command.equals(traitCommand))
				yes=true;
		if(!yes)
		for(Command c : race.unitcommands)
			if(c.command.equals(traitCommand))
				yes=true;
		if(!yes)
		for(Command c : race.nationcommands)
			if(c.command.equals(traitCommand))
				yes=true;
		if(!yes)
			for(Pose p : race.poses)
				for(Command c : p.commands)
				 if(c.command.equals(traitCommand))
					 yes=true;
		return yes;
	}
	
	public void init()
	{
		List<String> ls = new ArrayList<>();
		
		setSpecrecTag();
		if(colonyType=="uw")
		{
			ls.add("#amphibian");
			ls.add("#pooramphibian");
			//currently have no realisation among races
			//ls.add("#aquatic");
			//ls.add("#giftofwater");
		}
		colonySubType = getColonySubType(ls);
		//got colonySubType and need to return it somehow back
		
		//btw its magic layout time
		
		MageGenerator magMeg = new MageGenerator(nationGen, nation, assets);
		List<Unit> colMages = magMeg.generateColonialMages(max, null, max, specrecInfo, colonySubType, primary, secondary);
		//define primaries, list of magic paths and "power"
		
	}
}
