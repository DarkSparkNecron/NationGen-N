package nationGen.rostergeneration;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.ChanceDistribution;
import nationGen.entities.Pose;
import nationGen.entities.Race;
import nationGen.items.Item;
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
	
	//national&colonial info
	private Race primary;
	private Race secondary;
	private String colonyType = "uwcolony"; //non-functional now
	private String colonySubType;
	private Tags specrecInfo; //contains tags for specrec
	//unit amounts
	private int max; //max amount of units for nation
	private int primmax;
	private double secondmax;
	Map<String, Integer> maxamounts = new HashMap<>();
	Map<String, Integer> amounts = new HashMap<>();
	
	
	public ColonyGenerator(NationGen g, Nation n, NationGenAssets assets)
	{
		nationGen = g;
		nation = n;
		this.r = new Random(n.random.nextInt());
		
		
	}
	

	private void getUWColonyType(Race primary, Race secondary)
	{
		String Res = "none";
		double chance = r.nextDouble(1);
		
		Boolean primCanBuild = false;
		Boolean secCanBuild = false;
		
		//checking which races can build forts
		for(Command c : primary.nationcommands)
			if(c.command.equals("#uwbuild"))
				primCanBuild=true;
		for(Command c : primary.nationcommands)
			if(c.command.equals("#uwbuild"))
				secCanBuild=true;
		
		Boolean primCanUw = false;
		Boolean secCanUw = false;
		
		if(primCanBuild||secCanBuild)
		{
			//checking primary race for being capable uw
			for(Command c : primary.specialcommands)
				if(c.command=="#amphibian" || c.command=="#pooramphibian")
					primCanUw=true;
			if(!primCanUw)
				for(Pose p : primary.poses)
					for(Command c : p.commands)
					 if(c.command=="#amphibian" || c.command=="#pooramphibian")
						 primCanUw=true;
			//checking secondary race for being capable uw
			for(Command c : secondary.specialcommands)
				if(c.command=="#amphibian" || c.command=="#pooramphibian")
					secCanUw=true;
			if(!primCanUw)
				for(Pose p : secondary.poses)
					for(Command c : p.commands)
					 if(c.command=="#amphibian" || c.command=="#pooramphibian")
						 secCanUw=true;
			
			//if(secCanBuild&&secCanUw)Res="secrace";
			if(primCanBuild&&secCanUw||secCanBuild&&secCanUw)Res="secrace";
			if(primCanBuild&&primCanUw)Res="primrace";
			if(primCanBuild&&primCanUw&&secCanUw)Res="mixrace"; //all times big (have priest&mage)
			if(Res=="mixrace"&&chance<=0.33)Res="secrace";
			if(Res=="mixrace"&&chance<=0.66&&chance>0.33)Res="primrace";
			if(chance>0.15&&chance<0.48&&(Res=="primrace"||Res=="secrace"))Res+=":big"; //~50% chance for turned mixed, and 33% for reqular
			
		}
		
			
		colonySubType = Res;
	}
	
	
	private void setSpecrecTag()
	{
	  if(colonyType=="uwcolony") specrecInfo.add("specrec", "uw");
	}
	
	public void recieveMaxes(int maxu, int primmaxu, double secmaxu)
	{
		primmax=primmaxu;
		max=maxu;
	    secondmax=secmaxu;
	}
	
	public void setMaxes(RosterGenerator ro)
	{
		max=ro.GetMaxUnits();
		primmax=ro.GetMaxPrimaryUnits();
		secondmax=ro.GetMaxSecondaryUnits();
		
		String SubType;
		Boolean BigModifier=false;
		double primsteal = 0;
		double secsteal = 0;
		double totalsteal = 0;
		
		BigModifier=colonySubType.endsWith("big");
		
		int l=colonySubType.length();
		SubType=colonySubType.split(":")[0];
		
		if(colonyType=="uwcolony")
		{
			//if secrace has <=25% it steals all secrace units
			//big colony can steal up to 50% of units if both races are amph or its just primerace and no secrace
			//reqular colony steals 25%-33%
			if(SubType=="primrace")
			{
				//steal 1\3 if big, or even 50% for non mixed.
				//steal 1\4-1\5 if not big.
			}
				else if(SubType=="secrace")
				{
					
				}else if(SubType=="mixrace")
				{
					
				}else System.out.println("colonySubType error! its unreadable");
		}
	}
}
