package nationGen.entities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.misc.Tags;
import nationGen.nation.Nation;
import nationGen.rostergeneration.ColonyGenerator;
import nationGen.units.Unit;

public class Colony {
	NationGen nationGen;
	private Nation nation;
	private Random r;
	
	public Map<String, List<Unit>> unitlists = new LinkedHashMap<>();
	public Map<String, List<Unit>> comlists = new LinkedHashMap<>();
	
	public Race primary;
	public Race secondary;
	private String colonyType;
	public String colonySubType;
	public Tags specrecInfo;
	private ColonyGenerator colGen;
	
	
	public Colony(NationGen g, Nation n, NationGenAssets assets, String colType)
	{
		nationGen = g;
		nation = n;
		primary = nation.races.get(0);
		secondary = nation.races.get(1);
		this.r = new Random(n.random.nextInt());
		colonyType=colType;
		colGen=new ColonyGenerator(g,n,assets,this,colType,n.random.nextInt());
		
	}
	public Colony(NationGen g, Nation n, NationGenAssets assets, String colType, int randomControlKey)
	{
		nationGen = g;
		nation = n;
		primary = nation.races.get(0);
		secondary = nation.races.get(1);
		this.r = new Random(n.random.nextInt());
		colonyType=colType;
		colGen=new ColonyGenerator(g,n,assets,this,colType,randomControlKey);
	}

}
