package nationGen.magic;


import com.elmokki.Generic;
import nationGen.NationGen;
import nationGen.NationGenAssets;
import nationGen.chances.EntityChances;
import nationGen.entities.*;
import nationGen.items.Item;
import nationGen.misc.*;
import nationGen.nation.Nation;
import nationGen.rostergeneration.CommanderGenerator;
import nationGen.rostergeneration.TroopGenerator;
import nationGen.rostergeneration.montagtemplates.MageMontagTemplate;
import nationGen.units.Unit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MageGenerator extends TroopGenerator {
	
	public List<MagicPattern> possiblePatterns = new ArrayList<>();
	
	private NationGenAssets assets;
	
	public MageGenerator(NationGen g, Nation n, NationGenAssets assets) {
		super(g, n, assets, "magegen");
	
		this.assets = assets;
		loadPatterns();
	}

	
	/**
	 * Lists common magic paths of given units
	 * @param all given units
	 * @param minlevel minimum level to consider
	 * @return list of integers signifying paths
	 */
	public static List<MagicPath> findCommonPaths(List<Unit> all, int minlevel)
	{
		List<MagicPath> list = new ArrayList<>();
		for(MagicPath path : MagicPath.values())
		{
			boolean ok = true;
			for(Unit u : all)
			{
				
				
				MagicPathInts p = u.getMagicPicks(false);
				if(p.get(path) < minlevel)
				{
					ok = false;
				}
				
			}
			
			if(ok)
				list.add(path);
		}

		
		return list;
	}
	
	/**
	 * Lists common magic paths of given units
	 * @param all given units
	 * @return list of integers signifying paths
	 */
	public static List<MagicPath> findCommonPaths(List<Unit> all)
	{
		return findCommonPaths(all, 1);
	}
	
	/**
	 * Finds the paths that distinguish given mage from mages in a list
	 * @param u The mage to distinguish
	 * @param all The mages to distinguish from
	 * @return List of integers signifying paths
	 */
	public static List<MagicPath> findDistinguishingPaths(Unit u, List<Unit> all)
	{
		MagicPathInts paths = u.getMagicPicks(false);
		
		
		List<MagicPath> list = new ArrayList<>();
		for(Unit u2 : all)
		{
			if(u2 == u)
				continue;
			
			MagicPathInts p2 = u2.getMagicPicks(false);
			for(MagicPath path : MagicPath.values())
			{
				if((paths.get(path) > p2.get(path)) && !list.contains(path))
					list.add(path);
			}
		}
		
		if(all.size() == 1)
		{
			for(MagicPath path : MagicPath.values())
			{
				if((paths.get(path) > 0))
					list.add(path);
			}
		}
		
		return list;
	}
	
	public List<MagicPath> getShuffledPrios(List<Unit> units)
	{
		List<Filter> orig = new ArrayList<>();
		List<MagicPath> newlist = new ArrayList<>();
		
		
		
		MagicPathInts old = getAllPicks(units, true);
		for(MagicPath path : MagicPath.NON_HOLY)
		{
			Filter f = new Filter(this.nationGen);
			f.name = path.name;
			if(old.get(path) > 0)
			{
				double oldvalue = Math.sqrt(old.get(path));
				f.basechance = (double)1 / (2*oldvalue);
			}
		
			if(f.basechance > 0)
				orig.add(f);
		}

		while(newlist.size() < 2)
		{

			Filter i = EntityChances.baseChances(orig).getRandom(this.random);
			orig.remove(i);
			newlist.add(MagicPath.fromName(i.name));
		}
		
		for(Filter f : orig)
		{
			double picks = old.get(MagicPath.fromName(f.name));
			if(picks > 0)
			{
				f.basechance = 2*Math.sqrt(picks);
			}
		}

		while(orig.size() > 0)
		{
			Filter i = EntityChances.baseChances(orig).getRandom(this.random);
			orig.remove(i);
			newlist.add(MagicPath.fromName(i.name));
		}
		
		return newlist;
		
	}
	
	private List<MagicPath> getPrios(Unit u)
	{
		List<Filter> orig = new ArrayList<>();
		List<MagicPath> newlist = new ArrayList<>();
		
		
		
		Tags source = new Tags();
		
		if(u.pose.tags.getArgs("magicpriority").isPresent())
			source.addAll(u.pose.tags.named("magicpriority"));
		else
			source.addAll(u.race.tags.named("magicpriority"));
		
		for(Theme t : nation.nationthemes)
			source.addAll(t.tags.named("magicpriority"));
		
	
		for(int i = 0; i < 8; i++)
		{
			Filter f = new Filter(this.nationGen);
			f.name = MagicPath.fromInt(i).name;
	
			for(Args value : source.getAllArgs("magicpriority"))
			{
				if(value.size() >= 2)
				{
					if(f.name.equals(value.get(0).get()))
					{
						f.basechance *= value.get(1).getDouble();
						
						// All paths need to be possible, so let's have a failsafe
						if(f.basechance == 0)
							f.basechance = 0.00001;
					
					}
				}
			}
	
			orig.add(f);
		}
		
		while(orig.size() > 0)
		{
			Filter i = EntityChances.baseChances(orig).getRandom(this.random);
			orig.remove(i);
			newlist.add(MagicPath.fromName(i.name));
		}
		
		return newlist;

	}
	
	private void applyStats(Unit u)
	{
		u.commands.add(Command.args("#att", "-1"));
		u.commands.add(Command.args("#def", "-1"));
		
		int mr = 1 + (int)Math.round(u.getMagicAmount(10) + u.getRandoms(1)); 

		
		int unitmr = 0;	
		for(Command c : u.getCommands())
		{
			if(c.command.equals("#mr"))
			{

				unitmr += c.args.get(0).getInt();
			}
		}
		
		if (unitmr > 12) 
			mr++;
		
		//flatten high MR races and cap at 18
		mr -= Math.max(0, Math.max(unitmr - 11, unitmr + mr - 18)); 
		
		if(mr > 0)
		{
			u.commands.add(Command.args("#mr", "+" + mr));
		}
	
	}


	
	public List<Unit> generateMages()
	{
		

		List<List<Unit>> mages = generateMageBases();
		
		
		// Tag mages
		for(int i = 0; i < 3; i++)
		{
			tagAll(mages.get(i), "schoolmage", i+1);
						
			for(Unit u : mages.get(i))
			{
				unitGen.addFreeTemplateFilters(u);
				unitGen.addTemplateFilter(u, "magetemplates", "default_magetemplates");
			}

		}

		
		int primaries = mages.get(2).size();
		int secondaries = mages.get(1).size();
		int tertiaries = mages.get(0).size();
		
		// Decide if primaries should be cap only
		boolean caponlyprimaries = false;
		

		if(secondaries > primaries)
			caponlyprimaries = true;	
		if(primaries > 0 && secondaries > 0 && this.random.nextDouble() < 0.70)
			caponlyprimaries = true;
		if(primaries + secondaries + tertiaries >= 5)
			caponlyprimaries = true;


				
		List<MagicPattern> available = getPatternsForTier(3, primaries);
		
		List<MagicPattern> primarypatterns = new ArrayList<>();
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
		MagicPattern primaryPattern = chandler.handleChanceIncs(mages.get(2).get(0), available).getRandom(this.random);


		// Primaries
		List<MagicPath> prio = this.getPrios(mages.get(2).get(0));
		for(int i = 0; i < primaries; i++)
		{						
			primarypatterns.add(primaryPattern);
				
		}
		

		// Secondaries

		// varySecs = true -> mages get different paths but same kind of pattern
		boolean varySecs = true;
		List<MagicPattern> secs = getPatternsWithPicks(this.getPatternsForTier(2, primaries), 0, primaryPattern.getPicks(0.25) - 1);
		List<MagicPattern> secondarypatterns = new ArrayList<MagicPattern>();

		
		if(primaries == secondaries && this.random.nextDouble() < 0.35)
			secs = getPatternsWithSpread(secs, 0, Math.max(0, primaryPattern.getPathsAtleastAt(1)));
		
		
		// A setting here is probably a good idea as well.
		if(secondaries > 1)
		{
			List<MagicPattern> derp = new ArrayList<MagicPattern>();
			int lowlim = 2;
			while((derp.size() == 0 && random.nextBoolean()) || lowlim == 2 && lowlim < 12)
			{
				derp = getPatternsWithSpread(secs, 0, lowlim);
				lowlim++;
			}
			
			secs = derp;
		}

		if(secs.size() == 0)
			System.out.println("WARNING: No secondary patterns when primary has " + primaryPattern.getPicks(50) + " picks. Debug info: " + primaries + "/" + secondaries);
		



		// More secondaries than primaries only if there's 1 secondary (10.1.2014.)
		if(secondaries > primaries && this.random.nextDouble() < 0.6 && secs.size() >= secondaries)
		{
			varySecs = false;
			for(int i = 0; i < secondaries; i++)
			{			
				
				MagicPattern p = chandler.handleChanceIncs(mages.get(1).get(0), secs).getRandom(this.random);
				
				secs.remove(p);
				secondarypatterns.add(p);
			}
		}
		else if(secondaries > 0)
		{
			
			MagicPattern p = chandler.handleChanceIncs(mages.get(1).get(0), secs).getRandom(this.random);

			for(int i = 0; i < secondaries; i++)
			{						
				secondarypatterns.add(p);
			}
		}
		

		// Tertiary
		
		
		int maxPicks = primaryPattern.getPicks(0.25) - 1;
		for(MagicPattern p : secondarypatterns)
			if(p.getPicks(0.25) - 1 < maxPicks)
			{
				maxPicks = p.getPicks(0.25) - 1;

			}
		int maxSpread = 8;
		if(tertiaries == secondaries)
			for(MagicPattern p : secondarypatterns)
				if(p.getPathsAtleastAt(1) + 1 < maxSpread)
					maxSpread = p.getPathsAtleastAt(1) + 1;
		
		
		List<MagicPattern> terts = getPatternsWithPicks(this.getPatternsForTier(1, primaries), 0, maxPicks);
		List<MagicPattern> tertiarypatterns = new ArrayList<>();
		
		if(tertiaries > 0)
		{
			MagicPattern p = chandler.handleChanceIncs(mages.get(0).get(0), terts).getRandom(this.random);
			for(int i = 0; i < tertiaries; i++)
			{						
				tertiarypatterns.add(p);
			}
		}		

		// Apply patterns
		List<List<MagicPattern>> all = new ArrayList<>();
		all.add(tertiarypatterns);
		all.add(secondarypatterns);
		all.add(primarypatterns);
		
		double slowrecrand = this.random.nextDouble();
		
	
		for(int i = 2; i >= 0; i--)
			for(int j = 0; j < mages.get(i).size(); j++)
			{
	
				
				List<Unit> parents = null;
				if(i < 2)
					parents = mages.get(i + 1);
				
				List<MagicPath> derp;
				if(i == 1 && !varySecs) // Secondary mages, specific case of pattern varying but magic order not
				{
					derp = prio;
				}
				else if(parents != null && mages.get(i).size() == parents.size() && parents.size() > 1) // Secondary and tertiary mages
				{
					derp = varyAt(getVaryPoint(all.get(i).get(j), mages.get(i).size()), prio, j, parents.get(j), parents);
				}
				else // Primary Mages
				{
					derp = varyAt(getVaryPoint(all.get(i).get(j), mages.get(i).size()), prio, j);
				}
				
				MagicFilter f = getMagicFilter(derp, all.get(i).get(j), mages.get(i).size());
				mages.get(i).get(j).appliedFilters.add(f);
				f.tags.add("description", getPathDescription(mages.get(i).get(j).getMagicPicks(true)));
				f.tags.addName("tier"+Integer.toString(i+1));
				

				// Sets mage name to match pattern. Debug purposes.
				mages.get(i).get(j).name.setType(all.get(i).get(j).toString(derp));				
					
					
				// SETTING HERE FOR MAGE INSANITY

				
				
				double slowrecmod = mages.get(i).get(j).getMagicAmount(0.25) / 10 + 0.25;
			
				int highpicks = all.get(i).get(j).getPathsAtleastAt(3, 0.25);
				if(highpicks > 2)
					slowrecmod *= 2;
				
				if(mages.get(i).get(j).getMagicAmount(0.25) <= 4)
					slowrecmod = 0;
	
				
				applyStats(mages.get(i).get(j));
				
				
				if(caponlyprimaries && i == 2)
				{
					mages.get(i).get(j).caponly = true;
				}

				if(slowrecrand < slowrecmod && i == 2)
				{
					mages.get(i).get(j).commands.add(Command.args("#rpcost", "4"));
				}
				else
				{
					mages.get(i).get(j).commands.add(Command.args("#rpcost", "2"));
				}


			}


		// Put to one unit list
		List<Unit> list = new ArrayList<>();
		for(List<Unit> l : mages)
			list.addAll(l);			


		tagAll(list, "identifier", "schoolmage");
		

		// Handle montags
		for(int i = 1; i <= 3; i++)
			for(Unit u : this.getMagesOfTier(list, i))
				unitGen.handleMontagUnits(u, new MageMontagTemplate(nation, nationGen, assets, i), "montagmages");

		
		// Diagnostics
		MagicPathInts picks = getAllPicks(list, true);
		MagicPathInts norand_picks = getAllPicks(list, false);
		
		int diversity = 0;
		int max = 0;
		int atmax = 0;
		for(MagicPath path : MagicPath.values())
		{
			if(picks.get(path) > 1 || (norand_picks.get(path) == 1 && (path == MagicPath.BLOOD || path == MagicPath.ASTRAL)))
				diversity++;
			
			if(picks.get(path) > max)
			{
				max = picks.get(path);
				atmax = 1;
			}
			 
		}
		
		for(MagicPath path : MagicPath.values())
		{
			if(picks.get(path) == max)
			{
				atmax++;
			}
		}
		

		


		
		// Extra mage
		int checks = 0;
		
		//guaranteed astral or blood access forfeits most extra mage chances
		if (norand_picks.get(MagicPath.ASTRAL) == 0 && norand_picks.get(MagicPath.BLOOD) == 0)
		{
			if(diversity < 4)
			{
				if (diversity < 3)
				{
					if (max < 4)
						checks++;
				
					if (this.random.nextDouble() < 0.8)
						checks++;
	
					if (diversity < 2 && atmax == 1 && this.random.nextDouble() < 0.8)
						checks++;
				}
	
				//there used to be an extra 1 in 10 chance for mage lists without guaranteed earth 1 or fire 1 - this seemed very arbitrary and fiddly so I did away with the restriction
				if (this.random.nextDouble() < 0.6)
					checks++;
	
				if (this.random.nextDouble() < 0.2)
					checks++;
			}

			//random astral/blood is still eligible for most checks, but nations without get a few extra chances
			if (picks.get(MagicPath.ASTRAL) == 0 && picks.get(MagicPath.BLOOD) == 0)
			{
				if(max < 3 && atmax < 4)
					checks++;
				else if(max < 4 && atmax < 2 && this.random.nextDouble() < 0.5)
					checks++;
			}

			if (primaries == 1 && secondaries == 1 && this.random.nextDouble() < (0.7 - 0.4 * checks)) 
				//random freebie: likely for 0 checks, unlikely for 1 check, impossible for 2+
				checks++;
		}
		else if(norand_picks.get(MagicPath.ASTRAL) < 2 && norand_picks.get(MagicPath.BLOOD) < 2 && atmax == 1 && diversity < 2 && this.random.nextDouble() < 0.5) 
			//weak astral/blood can still get a shot if diversity is really bad
			checks++;				

		List<Unit> extramages = new ArrayList<Unit>();
		
		if(checks > 0)
		{
			extramages = this.generateExtraMages(primaries, this.getShuffledPrios(list), checks);
			this.resolveAge(extramages);
			this.applyStats(extramages.get(0));


			
			extramages.get(0).caponly = this.random.nextBoolean();
			if(!caponlyprimaries)
				extramages.get(0).caponly = true;
			else if(primaries > 1 || secondaries > 1)
				extramages.get(0).caponly = true;
			else if(extramages.get(0).getMagicAmount(0.25) > 6 || (extramages.get(0).getMagicAmount(0.25) > 5 && random.nextBoolean()))
				extramages.get(0).caponly = true;
			
			if(!extramages.get(0).caponly && this.random.nextDouble() > 0.125 && extramages.get(0).getMagicAmount(0.25) > 3)
				extramages.get(0).commands.add(Command.args("#rpcost", "4"));
			else if(extramages.get(0).getMagicAmount(0.25) > 5 && this.random.nextDouble() > 0.5)
				extramages.get(0).commands.add(Command.args("#rpcost", "4"));
			else
				extramages.get(0).commands.add(Command.args("#rpcost", "2"));

			for(Unit u : extramages)		
				unitGen.handleMontagUnits(u, new MageMontagTemplate(nation, nationGen, assets, 4), "montagmages");						
		
			list.addAll(0, extramages);			
			mages.add(extramages);
		}

		// Handle leadership & ages

		for(int i = 0; i < mages.size(); i++)
		{
			for (Unit u : mages.get(i))
			{
				double leadership = randomizeLeadership(u, 0, getMageTier(u));
				determineSpecialLeadership(u, false);
			}

			this.resolveAge(mages.get(i));
		}

		// Handle montags
	
		
		// Apply filters
		applyMageFilters(list);
		
		//// Equip units
		
		/// Equip main mages
		
		// Primaries
		List<Unit> primarymages = mages.get(2);
		
		Unit u = primarymages.get(0);

		equipBase(u, 3);
		varyEquipment(u, primarymages, 3);

		// Secondaries
		List<Unit> secondarymages = mages.get(1);
		
		if(secondarymages.size() > 0)
		{
			u = secondarymages.get(0);
			
			if(secondarymages.size() == primarymages.size())
			{
				deriveEquipment(primarymages, secondarymages, 3, 2);
			}
			else
			{
				deriveEquipment(primarymages.get(0), u, 3, 2);
				varyEquipment(u, secondarymages, 2);
			}
		}
		
		// Tertiaries
		List<Unit> tertiarymages = mages.get(0);
		
		if(tertiarymages.size() > 0)
		{
			u = tertiarymages.get(0);
			
			if(secondarymages.size() > 0)
			{
				deriveEquipment(secondarymages.get(0), u, 2, 1);
			}
			else 
			{
				deriveEquipment(primarymages.get(0), u, 3, 1);
			}
		}
	
		
		/// Equip extra mage
		for(Unit un : extramages)
		{
			this.equipBase(un, 2);
		}
		
		return list;
	}
	
	
	private void applyPriestFilters(List<Unit> list)
	{
		// priests
		if(list.size() > 0)
		{
			int power = 0;
			final int maxStrength = list.stream()
					.flatMap(u -> u.tags.getAllValues("priest").stream())
					.map(Arg::getInt)
					.max(Integer::compareTo)
					.orElse(1);
			
			if(this.random.nextDouble() < 0.15 + maxStrength * 0.075)
			{
				power++;
				if(this.random.nextDouble() < 0.1 + maxStrength * 0.05)
				{
					power++;
					if(this.random.nextDouble() < 0.1 + maxStrength * 0.05)
					{
						power++;
						if(this.random.nextDouble() < 0.1 + maxStrength * 0.05)
						{
							power++;
						}
					}
				}
			}
			
			if(power == 0 && this.random.nextDouble() > 0.85)
				power = this.random.nextInt(3) + 2; // 2 to 4 
				
			String[] defaults = {"default_priestfilters", "default_priestfilters_summons"};
			this.applyFilters(list, power, defaults, "priestfilters");

		}
	}
	
	private void applyMageFilters(List<Unit> list2)
	{		
		
		List<Unit> list = new ArrayList<>(list2);
		
		// Diagnostics
		MagicPathInts picks = getAllPicks(list, true);
		MagicPathInts norand_picks = getAllPicks(list, false);
		
		int diversity = 0;
		int max = 0;
		int atmax = 0;
		for(MagicPath path : MagicPath.values())
		{
			if(picks.get(path) > 1 || (norand_picks.get(path) == 1 && (path == MagicPath.BLOOD || path == MagicPath.ASTRAL)))
				diversity++;
			
			if(picks.get(path) > max)
			{
				max = picks.get(path);
				atmax = 1;
			}
			else if(picks.get(path) == max)
			{
				atmax++;
			}
		}
		
		
		
		// Filters for main mages
	
		String[] defaults = {"default_magefilters", "default_magefilters_shapeshift", "default_magefilters_strongdefence"};
		//List<Filter> filters = ChanceIncHandler.retrieveFilters("magefilters", defaults, nationGen.filters, list.get(list.size() - 1).pose, list.get(list.size() - 1).race);

		double mod = 0.25;
		if(diversity < 3)
			mod += 0.2;
		if(diversity < 4)
			mod += 0.15;
		
		if(max > 4)
			mod -= 0.25;
		if(max < 4)
			mod += 0.1;
		if(atmax < 2 && max < 4)
			mod += 0.2;
		if(atmax < 3 && max < 4)
			mod += 0.1;
		
			
		if(picks.get(MagicPath.ASTRAL) > 0 && picks.get(MagicPath.EARTH) > 0)
			mod *= 0.75;
		if(picks.get(MagicPath.ASTRAL) > 2)
			mod *= 0.75;
		else if(norand_picks.get(MagicPath.ASTRAL) > 0)
			mod *= 0.8;
		if(picks.get(MagicPath.BLOOD) > 2)
			mod *= 0.75;
		

		int power = 0;

		if(this.random.nextDouble() < mod * 3)
		{
			power++;
			if(this.random.nextDouble() < mod * 2)
			{
				power++;
				if(this.random.nextDouble() < mod)
				{
					power++;
					if(this.random.nextDouble() < mod)
					{
						power++;
						if(this.random.nextDouble() < mod / 2)
						{
							power++;
							if(this.random.nextDouble() < mod / 2)
							{
								power++;
							}
						}
					}
				}
			}
		}
		

		if(power > 0 && this.random.nextDouble() > 0.5)
			power++;
		
		
		Optional<Unit> extra = list.stream()
				.filter(u -> !u.tags.contains("identifier", "schoolmage"))
				.findFirst();
		
		extra.ifPresent(list::remove);
		this.applyFilters(list, power, defaults, "magefilters");

		
		// Filters for extra mage
		extra.ifPresent(extraMage -> {
			int extraPower = this.random.nextInt(4); // 0 to 3
			if(this.random.nextBoolean())
				extraPower += this.random.nextInt(3); // 0 to 2;
			
			this.applyFilters(List.of(extraMage), extraPower, defaults, "magefilters");
		});
	}
	
	
	public static MagicPathInts getAllPicks(List<Unit> units, boolean randoms)
	{
		MagicPathInts picks = new MagicPathInts();
		
		units.forEach(u -> picks.maxWith(u.getMagicPicks(randoms)));

		return picks;
	}
	
	public MagicFilter getMagicFilter(List<MagicPath> prio, MagicPattern p, int primaries)
	{
		MagicFilter f = new MagicFilter(nationGen);
		f.prio = prio;
		f.pattern = p;
		f.name = "MAGICPICKS";		
		f.tags.addAll(p.tags);
		f.descSet = "mage";
		f.tags.add("prev","mage desc");
		f.tags.add("next","mage end");
		f.tags.addName("mage");
		assets.initializeFilters(List.of(f),"getMagicFilter()");
		
		//f.tags.addName("do_not_show_in_descriptions"); I'm not sure that this did anything, but we do want to show up in descriptions now
		return f;
	}
	
	public String getPathDescription(MagicPathInts paths)
	{
		StringBuilder pathDesc = new StringBuilder();
		List<String> pathNames = new ArrayList<>();
		List<MagicPath> bests = paths.getAllHighestPaths();
		
		for (MagicPath p : paths.getAllHighestPaths())
			pathNames.add(p.name);	
		
		if (pathNames.size() > 3)		
			return "various";
		else
		{
			for (int i = 0; i < pathNames.size(); i++)
			{
				if (i > 0)
				{
					if (pathNames.size() > 2)
						pathDesc.append(",");
					
					pathDesc.append(" ");
						
					if (i == pathNames.size() - 1)
						pathDesc.append("and ");
				}
				
				pathDesc.append(pathNames.get(i));			
			}
			
			if (pathDesc.length() == 0)
				pathDesc.append("no");
			
			return pathDesc.toString();
		}
	}
	
	private int getVaryPoint(MagicPattern p, int primaries)
	{
		if(primaries <= p.getPathsAtleastAt(1, 100) && (this.random.nextDouble() > 0.8 && p.getPathsAtleastAt(3, 0) < 2))
		{
			for(int i = 3; i > 0; i--)
			{
				if(p.getPathsAt(i) > 0)
					return (p.getPathsAtleastAt(i, 100) - 1);	
			}
		}
		
			
		
		return Math.max(0, (p.getPathsAtleastAt(1, 100) - 1));
	}
	
	private List<MagicPath> varyAt(int point, List<MagicPath> prio, int times)
	{
		List<MagicPath> nprio = new ArrayList<>(prio);
		MagicPath old = nprio.get(point);
		nprio.set(point, nprio.get(point + times));
		nprio.set(point + times, old);
		return nprio;
	}
	
	private List<MagicPath> varyAt(int point, List<MagicPath> prio, int times, Unit parent, List<Unit> parents)
	{
		List<MagicPath> nprio = new ArrayList<>(prio);

		
		List<MagicPath> dpaths = findDistinguishingPaths(parent, parents);
		
		if(dpaths.size() == 0)
			return varyAt(point, prio, times);
		
		int index = nprio.indexOf(dpaths.get(0));
		MagicPath old = nprio.get(point);
		nprio.set(point, dpaths.get(0));
		nprio.set(index, old);
		
		return nprio;
	}
	
	
	public List<MagicPattern> getPatternsForTier(int tier, int primaries)
	{
		List<MagicPattern> available = getPatternsOfPrimaries(getPatternsOfLevel(possiblePatterns, tier), primaries);
		if(available.size() == 0)
			System.out.println("WARNING: No level " + tier + " magic patterns for " + primaries + " primary mage setup.");
		
		
		
		return available;
	}
	
	public static List<MagicPattern> getPatternsOfLevel(List<MagicPattern> orig, int level)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		
		for(MagicPattern p : orig)
		{
			if(p.levels.size() == 0)
				list.add(p);
			else if(p.levels.contains(level))
				list.add(p);
		}
		
		return list;
	}
	
	
	public static List<MagicPattern> getPatternsOfPrimaries(List<MagicPattern> orig, int level)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		
		for(MagicPattern p : orig)
		{
			if(p.primaries.size() == 0)
				list.add(p);
			else if(p.primaries.contains(level))
				list.add(p);
		}
		
		return list;
	}
	
	
	public static List<MagicPattern> getPatternsWithSpread(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getPathsAtleastAt(1, 1) >= min && p.getPathsAtleastAt(1, 1) <= max)
				list.add(p);
		}
		return list;
	}
	
	public static List<MagicPattern> getPatternsWithPicks(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getPicks(0.25) >= min && p.getPicks(0.25) <= max )
				list.add(p);
		}
		return list;
	}
	
	public static List<MagicPattern> getHighestLevel(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getHighestReachable(0.25) >= min && p.getHighestReachable(0.25) <= max)
				list.add(p);
		}
		return list;
	}
	
	public static List<MagicPattern> getPathsAtHighest(List<MagicPattern> orig, int min, int max)
	{
		List<MagicPattern> list = new ArrayList<MagicPattern>();
		for(MagicPattern p : orig)
		{
			if(p.getPicksAtHighest() <= max && p.getPicksAtHighest() >= min)
				list.add(p);
		}
		return list;
	}
	
	
	private void loadPatterns()
	{
		// Patterns
		List<String> patternsets = nation.races.get(0).tags.getAllValues("magicpatterns").stream()
				.map(Arg::get)
				.collect(Collectors.toList());
		
		if(patternsets.size() == 0)
		{
			patternsets.add("defaultprimary");
			patternsets.add("defaultsecondary");
			patternsets.add("defaulttertiary");
		}
		
		for(String set : patternsets)
		{
			List<MagicPattern> fetch = assets.patterns.get(set);
			if(fetch == null)
				System.out.println("WARNING: Magic path pattern set " + set + " could not be found.");
			else
				possiblePatterns.addAll(fetch);
		}
	}
	

	
	private List<Unit> generateExtraMages(int primaries, List<MagicPath> prio, int power)
	{
		Race race = nation.races.get(0);
		
		double bonussecchance = 1;
		bonussecchance += race.tags.getDouble("secondaryracemagemod").orElse(0D);
		bonussecchance -= nation.races.get(1).tags.getDouble("primaryracemagemod").orElse(0D);
		
		if((this.random.nextDouble() < 0.075 * bonussecchance) && nation.races.get(1).hasRole("mage"))
			race = nation.races.get(1);
		else if(!nation.races.get(0).hasRole("mage"))
			race = nation.races.get(1);

		List<Unit> bases = generateBases("mage", race, 1, 2);
		
		int tier;
		if(power >= 5 || (power >= 4 && random.nextBoolean()) || (power >= 3 && random.nextDouble() > 0.66) | (power >= 2 && random.nextDouble() > 0.95))
			tier = 3;
		else if(power > 2 || (random.nextBoolean() && power != 1) || random.nextDouble() > 0.90)
			tier = 2;
		else
			tier = 1;	
			
		List<MagicPattern> available = getPatternsOfLevel(getPatternsForTier(0, primaries), tier);
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
		MagicPattern pattern = chandler.handleChanceIncs(bases.get(0), available).getRandom(this.random);

		pattern.getPathsAtleastAt(1);
		if(prio == null)
		{
			prio = this.getPrios(bases.get(0));
		}
	

		MagicFilter f = getMagicFilter(prio, pattern, 0);
		bases.get(0).appliedFilters.add(f);
		f.tags.add("description", getPathDescription(bases.get(0).getMagicPicks(true)));
		bases.get(0).tags.add("extramage",tier);
		bases.get(0).tags.addName("tier"+Integer.toString(tier));
		
		//f.prio = prio;
		//f.pattern = pattern;
		
		bases.get(0).color = nation.colors[2];		
		
		this.equipBase(bases.get(0), 2);
		
		return bases;

	}
	
	
	//to generate colomial mages, when making custom mage generator or anjusting existing for uw nations look here
	public List<Unit> generateColonialMages(int primaries, List<MagicPath> prio, int power, Tags specrec, String colonySubType, Race prim, Race sec)
	{
		Race race = prim;
		
		double bonussecchance = 1;
		bonussecchance += race.tags.getDouble("secondaryracemagemod").orElse(0D);
		bonussecchance -= sec.tags.getDouble("primaryracemagemod").orElse(0D);
		
		String sub =colonySubType.split("_")[0];
		
		if(sub=="mixrace")
		{
		if((this.random.nextDouble() < 0.075 * bonussecchance) && sec.hasRole("mage"))
			race = sec;
		else if(!prim.hasRole("mage"))
			race = sec;
		}
		//mostly safeguard to actually generate mages
		if(sub=="primrace")
		{
			if(!prim.hasRole("mage"))
				race = sec;
		}
		if(sub=="secrace")
		{
			race = sec;
			if(!sec.hasRole("mage"))
				race = prim;
		}
		
		
		List<Unit> bases = generateBases("mage", race, 1, power); //1 mage of 'power' tier, usually should be 2
		for(Unit u: bases)
			u.tags.addAll(specrec); //also here should be automatically applied colony tag, which is in colGen's specrec by default
		
		int tier;
		if(power >= 5 || (power >= 4 && random.nextBoolean()) || (power >= 3 && random.nextDouble() > 0.66) | (power >= 2 && random.nextDouble() > 0.95))
			tier = 3;
		else if(power > 2 || (random.nextBoolean() && power != 1) || random.nextDouble() > 0.90)
			tier = 2;
		else
			tier = 1;	
			
		List<MagicPattern> available = getPatternsOfLevel(getPatternsForTier(0, primaries), tier);
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
		MagicPattern pattern = chandler.handleChanceIncs(bases.get(0), available).getRandom(this.random);
		
		pattern.getPathsAtleastAt(1);
		if(prio == null)
		{
			prio = this.getPrios(bases.get(0));
		}
	
		//btw as yoiu can see all manipulations are happening only with first base, so only 1 additional mage for now
		MagicFilter f = getMagicFilter(prio, pattern, 0);
		bases.get(0).appliedFilters.add(f);
		f.tags.add("description", getPathDescription(bases.get(0).getMagicPicks(true)));
		bases.get(0).tags.add("extramage",tier);
		bases.get(0).tags.addName("tier"+Integer.toString(tier));
		
		//f.prio = prio;
		//f.pattern = pattern;
		
		bases.get(0).color = nation.colors[2];		
		
		this.equipBase(bases.get(0), power);
		this.resolveAge(bases);
		this.applyStats(bases.get(0));
		
		int priestrand = this.random.nextInt(4);
		if(priestrand>0 && priestrand<4) //50% chance of trying to become 1D or 2D priest
		this.upgradeToPriest(bases, priestrand);
		
		return bases;

	}
	
	private List<Unit> upgradeToPriest(List<Unit> magelist, int index)
	{
		//making mage holy, stolen from priestgen
		
				Random r = this.random;
				
				
				final double priest_H1_UpChance = Generic.getAllNationTags(nation).getAllValues("priest_H1_upgradechance").stream()
						.map(Arg::getDouble)
						.max(Double::compareTo)
						.orElse(0.75);
				final double priest_H2_UpChance = Generic.getAllNationTags(nation).getAllValues("priest_H2_upgradechance").stream()
						.map(Arg::getDouble)
						.max(Double::compareTo)
						.orElse(0.25);
				
				int priestStrengthFromRandom = 1;
				if(r.nextDouble() < priest_H1_UpChance)
				{
					priestStrengthFromRandom++;
					if(r.nextDouble() < priest_H2_UpChance)
						priestStrengthFromRandom++;
				}
				
				final int highestPriestLevel = Generic.getAllNationTags(nation).getAllValues("highestpriestlevel").stream()
						.map(Arg::getInt)
						.max(Integer::compareTo)
						.orElse(priestStrengthFromRandom);
				
				final int maxlevel = Generic.getAllNationTags(nation).getAllValues("maxpriestlevel").stream()
						.map(Arg::getInt)
						.max(Integer::compareTo)
						.orElse(10);
				
				final int maxStrength = Math.min(maxlevel, highestPriestLevel);
				
				final double magePriestChance = Generic.getAllNationTags(nation).getAllValues("magepriestchance").stream()
						.map(Arg::getDouble)
						.max(Double::compareTo)
						.orElse(0.3);
						
				boolean magePriests = r.nextDouble() < magePriestChance;
				
				//can upgrade up to T2
				int sacredMageTiers = 0;
				if(r.nextDouble() < 0.5)
				{
					sacredMageTiers++;
					if(r.nextDouble() < 0.5)
					{
						sacredMageTiers++;
					}
				}
				if(r.nextBoolean())
					sacredMageTiers = 0;
				
				// Adjust color
				Color priestcolor = nation.colors[1];
				if(!magePriests || (priestcolor.getRed() + priestcolor.getBlue() + priestcolor.getGreen() < 660))
				{
					priestcolor = priestcolor.brighter();
				}
				else
				{
					priestcolor = priestcolor.darker();
				}
				int priestextracost = Generic.getAllNationTags(nation).getInt("priestextracost").orElse(0);
				int priestminrpcost = Generic.getAllNationTags(nation).getInt("priestminrpcost").orElse(0);

				
				for (Unit u: magelist)
				{
					u.commands.add(new Command("#holy"));
					u.appliedFilters.add(this.getPriestPattern(sacredMageTiers, sacredMageTiers == maxStrength));						
					u.tags.addName("magepriest");
					u.commands.add(Command.args("#gcost", "+" + (10*sacredMageTiers + sacredMageTiers * priestextracost)));

					//force slow to recruit for H3s
					if (sacredMageTiers > 2 && u.getCommandValue("#rpcost", 0) == 2)
						u.commands.add(Command.args("#rpcost", "+2"));	
				}
				
				return magelist;
	}
	public List<Unit> generatePriests()
	{		

		List<List<Unit>> all = nation.getMagesInSeparateLists();
		List<Unit> extras = all.get(3);
		
		
		Random r = this.random;
		
		
		final double priest_H1_UpChance = Generic.getAllNationTags(nation).getAllValues("priest_H1_upgradechance").stream()
				.map(Arg::getDouble)
				.max(Double::compareTo)
				.orElse(0.75);
		
		
		final double priest_H2_UpChance = Generic.getAllNationTags(nation).getAllValues("priest_H2_upgradechance").stream()
				.map(Arg::getDouble)
				.max(Double::compareTo)
				.orElse(0.25);
		
		int priestStrengthFromRandom = 1;
		if(r.nextDouble() < priest_H1_UpChance)
		{
			priestStrengthFromRandom++;
			if(r.nextDouble() < priest_H2_UpChance)
				priestStrengthFromRandom++;
		}
		
		final int highestPriestLevel = Generic.getAllNationTags(nation).getAllValues("highestpriestlevel").stream()
				.map(Arg::getInt)
				.max(Integer::compareTo)
				.orElse(priestStrengthFromRandom);
		
		final int maxlevel = Generic.getAllNationTags(nation).getAllValues("maxpriestlevel").stream()
				.map(Arg::getInt)
				.max(Integer::compareTo)
				.orElse(10);
		
		final int maxStrength = Math.min(maxlevel, highestPriestLevel);
		
		final double magePriestChance = Generic.getAllNationTags(nation).getAllValues("magepriestchance").stream()
				.map(Arg::getDouble)
				.max(Double::compareTo)
				.orElse(0.3);
				
		boolean magePriests = r.nextDouble() < magePriestChance;
		
		// If we have extra mages, they can be priests 20% of the time if primary mages aren't
		boolean compensationMagePriests = (!magePriests && r.nextDouble() < 0.20);

		
		int sacredMageTiers = 0;
		if(r.nextDouble() < 0.5)
		{
			sacredMageTiers++;
			if(r.nextDouble() < 0.5)
			{
				sacredMageTiers++;
				if(r.nextDouble() < 0.5)
					sacredMageTiers++;

			}

		}
		
		if(magePriests && maxStrength < 3 && r.nextDouble() < 0.5)
			sacredMageTiers++;
			
	
		if(r.nextBoolean())
			sacredMageTiers = 0;
		
		
		if(extras.size() == 0)
			compensationMagePriests = false;

		if (compensationMagePriests)
			magePriests = true; //necessary to generate magepriests at all (code already distinguishes regular vs. compensation magepriests)
		
		
		
		// Adjust color
		Color priestcolor = nation.colors[1];
		if(!magePriests || (priestcolor.getRed() + priestcolor.getBlue() + priestcolor.getGreen() < 660))
		{
			priestcolor = priestcolor.brighter();
		}
		else
		{
			priestcolor = priestcolor.darker();
		}
		
		List<Unit> priests = new ArrayList<Unit>();
		
		int currentStrength = maxStrength;
		int atTier = 0;

		boolean doneWithMages = false;
		
		int priestsFrom = 0;
		if(magePriests && maxStrength > 1 && r.nextDouble() < 0.75)
		{
			if(r.nextDouble() < 0.25)
				priestsFrom = r.nextInt(maxStrength + 1);
			else
				priestsFrom = Math.min(2, maxStrength);
		}
		
		int priestextracost = Generic.getAllNationTags(nation).getInt("priestextracost").orElse(0);
		int priestminrpcost = Generic.getAllNationTags(nation).getInt("priestminrpcost").orElse(0);


		
		while(currentStrength > 0)
		{
			boolean done = false;
			
			if(atTier >= 0 && all.get(atTier).size() == 0 && atTier < 3)
				atTier++;
			
			if(magePriests && !doneWithMages)
			{
				if(compensationMagePriests && currentStrength == maxStrength)
				{
					for(Unit u : extras)
					{
						u.commands.add(new Command("#holy"));
						u.appliedFilters.add(this.getPriestPattern(currentStrength, currentStrength == maxStrength));						
						u.tags.addName("magepriest");
						u.commands.add(Command.args("#gcost", "+" + (10*currentStrength + currentStrength * priestextracost)));

						//force slow to recruit for H3s
						if (currentStrength > 2 && u.getCommandValue("#rpcost", 0) == 2)
							u.commands.add(Command.args("#rpcost", "+2"));	
					}
					done = true;
				}
				else if(atTier >= 0 && all.get(atTier).size() > 0 && !compensationMagePriests)
				{
					for(Unit u : all.get(atTier))
					{
						u.appliedFilters.add(this.getPriestPattern(currentStrength, currentStrength == maxStrength));
						u.commands.add(new Command("#holy"));
						u.tags.addName("magepriest");
						u.commands.add(Command.args("#gcost", "+" + (10*currentStrength + currentStrength * priestextracost)));


					}
					done = true;
					atTier++;
					
					if(r.nextDouble() < 0.2)
					{
						doneWithMages = true;
					}
				}
			}
			
			Race prace = nation.races.get(0);
			if(!nation.races.get(0).hasRole("priest"))
				prace = nation.races.get(1);
			
			
			
			if((currentStrength > 1 && !done) || currentStrength == 1 || currentStrength <= priestsFrom)
			{
				Unit u;
				if(priests.size() > 0)
				{
					u = deriveBasesFrom(1, "priest", priests.get(0), priests.get(0).race, currentStrength + 1, currentStrength).get(0);
				}
				else
				{
					int str = currentStrength;
					if(r.nextBoolean())
						str = Math.min(currentStrength + 1, 3);
					
					u = this.generateBases("priest", prace, 1, str).get(0);
				}
				u.commands.add(Command.args("#gcost", "+" + (20*currentStrength - 10) ));

				u.color = priestcolor;
				u.appliedFilters.add(this.getPriestPattern(currentStrength, currentStrength == maxStrength));
				u.commands.add(new Command("#holy"));
				u.commands.add(Command.args("#gcost", "+" + ((int)(Math.pow(2, currentStrength))*10 + currentStrength * priestextracost)));
				u.tags.add("priest", currentStrength);			
				
				u.commands.add(Command.args("#mr", "+" + (2 + currentStrength)));

				

				
				//List<String> body = new ArrayList<String>();	// Set a temporary null description for priests
				(new CommanderGenerator(this.nationGen, this.nation, assets)).generateDescription(u, false, false, false);
				//body.add();					
		
				//body.add("\"No description\"");
				//u.commands.add(new Command("#descr", body));

				priests.add(0, u);

			}	
			currentStrength--;
		}
		
	
		if(sacredMageTiers > 0)
		{
			for(int i = 0; i < 3; i++)
			{
				for(Unit u : all.get(i))
					u.commands.add(new Command("#holy"));
			
				sacredMageTiers--;
				if(sacredMageTiers == 0)
					break;
			}
		}
		
		
		// Apply filters
		applyPriestFilters(priests);

		// Equip priests
		int maxtier = priests.stream()
			.flatMap(u -> u.tags.getValue("priest").stream())
			.map(Arg::getInt)
			.max(Integer::compareTo)
			.orElse(0);
		
		Unit parent = null;
		
	
		for(int i = priests.size() - 1; i >= 0; i--)
		{

			Unit u = priests.get(i);
			int tier = u.tags.getValue("priest").orElseThrow().getInt();
			

			if(tier == maxtier)
			{
				equipBase(u, tier);
			}
			else
			{
				if (parent == null) {
					throw new IllegalStateException("Final priest's level isn't maxtier");
				}
				int ptier = parent.tags.getValue("priest").orElseThrow().getInt();
				this.deriveEquipment(parent, u, ptier, tier);
			}
			

			
			
			parent = u;
		}

		
		for(Unit u : priests)
		{
			int currentRP = 1;
			currentStrength = u.tags.getValue("priest").orElseThrow().getInt();
			
			// STR or cap only for h3
			if(currentStrength == 3 && r.nextDouble() > 0.5)
				currentRP = 4;
			else if(currentStrength == 3)
			{
				u.caponly = true;
				if(r.nextDouble() > 0.9)
					currentRP = 4;

			}


			double leaderBonus = 0;
			double leadership = 0; 

			// if there is only 1 tier of priest, give them a boost to leadership
			if (maxStrength == 1)
				leaderBonus = 0.4;

			leadership = randomizeLeadership(u, leaderBonus, currentStrength);

			if (leadership > 80)
				currentRP = Math.max(2, currentRP);

			currentRP = Integer.max(Integer.max(currentRP, Integer.min(2, currentStrength)), priestminrpcost);
			u.commands.add(new Command("#rpcost", new Arg(currentRP)));
			
			// Determine special leadership
			determineSpecialLeadership(u, true);
		}
		
		
		return priests;
	}
	
	public List<Unit> generateColonialPriests(int maxPriestPower, String colonySubType, Race prim, Race sec, Tags specrec)
	{
		//race deciding
		String sub =colonySubType.split("_")[0];
		Race prace=prim;
		
		if(sub=="mixrace")
		{
		if((this.random.nextDouble() < 0.075) && sec.hasRole("priest"))
			prace = sec;
		else if(!prim.hasRole("priest"))
			prace = sec;
		}
		//mostly safeguard to actually generate mages
		if(sub=="primrace")
		{
			if(!prim.hasRole("priest"))
				prace = sec;
		}
		if(sub=="secrace")
		{
			prace = sec;
			if(!sec.hasRole("priest"))
				prace = prim;
		}
		
		//getting limitation on possible priest level
		Random r = this.random;
		
		final double priest_H1_UpChance = Generic.getAllNationTags(nation).getAllValues("priest_H1_upgradechance").stream()
				.map(Arg::getDouble)
				.max(Double::compareTo)
				.orElse(0.75);
		
		
		final double priest_H2_UpChance = Generic.getAllNationTags(nation).getAllValues("priest_H2_upgradechance").stream()
				.map(Arg::getDouble)
				.max(Double::compareTo)
				.orElse(0.25);
		
		int priestStrengthFromRandom = 1;
		if(r.nextDouble() < priest_H1_UpChance)
		{
			priestStrengthFromRandom++;
			if(r.nextDouble() < priest_H2_UpChance)
				priestStrengthFromRandom++;
		}
		
		final int highestPriestLevel = Generic.getAllNationTags(nation).getAllValues("highestpriestlevel").stream()
				.map(Arg::getInt)
				.max(Integer::compareTo)
				.orElse(priestStrengthFromRandom);
		
		final int maxlevel = Generic.getAllNationTags(nation).getAllValues("maxpriestlevel").stream()
				.map(Arg::getInt)
				.max(Integer::compareTo)
				.orElse(10);
		
		
		
		int priestextracost = Generic.getAllNationTags(nation).getInt("priestextracost").orElse(0);
		int priestminrpcost = Generic.getAllNationTags(nation).getInt("priestminrpcost").orElse(0);

		
		//strength
		final int maxStrength = Math.min(maxlevel, highestPriestLevel);
		
		
		int currentPower=maxPriestPower;
		if(maxStrength<maxPriestPower)
			currentPower=maxStrength;
		//cost & smth
		int priestsFrom = 0;
		if(maxStrength > 1 && r.nextDouble() < 0.75)
		{
			if(r.nextDouble() < 0.25)
				priestsFrom = r.nextInt(maxStrength + 1);
			else
				priestsFrom = Math.min(2, maxStrength);
		}
		
		//starting generating
		// Adjust color
		Color priestcolor = nation.colors[1];
				if((priestcolor.getRed() + priestcolor.getBlue() + priestcolor.getGreen() < 660))
				{
					priestcolor = priestcolor.brighter();
				}
				else
				{
					priestcolor = priestcolor.darker();
				}
				
		List<Unit> priests = new ArrayList<Unit>();
		
		
		while(currentPower>0)
		{
		
		if((currentPower > 1) || currentPower == 1 || currentPower <= priestsFrom)
		{
			Unit u;
			if(priests.size() > 0)
			{
				u = deriveBasesFrom(1, "priest", priests.get(0), priests.get(0).race, currentPower + 1, currentPower).get(0);
			}
			else
			{
				int str = currentPower;
				if(r.nextBoolean())
					str = Math.min(currentPower + 1, 3);
				
				u = this.generateBases("priest", prace, 1, str).get(0);
			}
			u.commands.add(Command.args("#gcost", "+" + (20*currentPower - 10) ));

			u.color = priestcolor;
			u.appliedFilters.add(this.getPriestPattern(currentPower, currentPower == maxStrength));
			u.commands.add(new Command("#holy"));
			u.commands.add(Command.args("#gcost", "+" + ((int)(Math.pow(2, currentPower))*10 + currentPower * priestextracost)));
			u.tags.add("priest", currentPower);			
			u.tags.addAll(specrec);
			
			u.commands.add(Command.args("#mr", "+" + (2 + currentPower)));

			

			
			//List<String> body = new ArrayList<String>();	// Set a temporary null description for priests
			(new CommanderGenerator(this.nationGen, this.nation, assets)).generateDescription(u, false, false, false);
			//body.add();					
	
			//body.add("\"No description\"");
			//u.commands.add(new Command("#descr", body));

			priests.add(0, u);
		}
		currentPower--;
		}	
		
		
		return null;
	}
	
	public static void determineSpecialLeadership(Unit u, boolean isPriest)
	{
		boolean isUndead = false;
		
		String basiclevel = u.getLeaderLevel();
		
		List<String> others = new ArrayList<>();
		for(Command c : u.commands)
			if(c.command.equals("#undead"))
			{
				isUndead = true;
				others.add("undead");
			}
			else if (c.command.equals("#almostundead"))
				others.add("undead");
			else if (c.command.equals("#demon"))
				others.add("undead");
			else if(c.command.equals("#magicbeing"))
				others.add("magic");
		
		for(String str : others)
		{
			// Undead priests almost all have elevated undead leadership, so if they have leadership bump it up a level
			if(!u.hasLeaderLevel(str) && str.equals("undead") && isUndead && isPriest)
			{
				if(basiclevel.equals("no"))
					u.commands.add(new Command("#noundeadleader"));
				else if(basiclevel.equals("poor"))
					u.commands.add(new Command("#okayundeadleader"));
				else
				{
					u.commands.add(new Command("#" + basiclevel + str + "leader"));
					u.commands.add(Command.args("#undcommand", "+40"));
				}					
			}
			else if(!u.hasLeaderLevel(str))
				u.commands.add(new Command("#" + basiclevel + str + "leader"));
		}
	}
	
	/**
	 * The new method of actually applying filters. Used in HeroGenerator and here.
	 * @param units
	 * @param power
	 * @param defaults
	 * @param lookfor
	 */
	public void applyFilters(List<Unit> units, int power, String[] defaults, String lookfor)
	{
		if(units.size() == 0)
			return;
		

		// Filter selection is strict (see method getPossibleFiltersByPaths) for mages, but not for priests.
		// IF PRIESTS START TO CONSISTENTLY GET MAGIC WITH SOME NON-FILTER IMPLEMENTATION THIS NEEDS TO BECOME TAG BASED
		boolean strict = false;
		MagicPathInts picks = units.get(0).getMagicPicks(false);
		
		for(MagicPath path : MagicPath.NON_HOLY)
		{
			if(picks.get(path) > 0)
				strict = true;
		}

		
		
		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");

		while(power > 0)
		{
			List<Filter> moreFilters = new ArrayList<Filter>();
			
			//if(this.random.nextDouble() > 0.1)
			//	moreFilters = ChanceIncHandler.getFiltersWithPower(power, power, filters);
			

			

			
			List<Unit> mages = new ArrayList<Unit>();
			
			int tier = 4; 
			
			if (this.random.nextDouble() > 0.65 && getMagesOfTier(units, 2).size() > 0)
				tier = 2;
			else if (this.random.nextDouble() > 0.875 && getMagesOfTier(units, 1).size() > 0)
				tier = 1;
			else if (getMagesOfTier(units, 3).size() > 0)
				tier = 3;

			mages = getMagesOfTier(units, tier);

			if(mages.size() == 0) // Heroes and extra mages
				mages.addAll(units);

			
			// Figure whether to give different filters for mages of tier or not
			boolean different = allHaveDistinguishingPath(mages);
			boolean common = MageGenerator.findCommonPaths(mages).size() > 0;
			different = different && (this.random.nextBoolean() || !common);
			
			// If montag on all units, they should get different filters
			boolean montags = true;
			for(Unit u : units)
			{
				boolean gotit = false;
				for(Command c : u.getCommands())
					if(c.command.equals("#montag"))
						gotit = true;
				
				if(!gotit)
				{
					montags = false;
					break;
				}
			}
			
			if(montags)
				different = true;
			
			
			// Get filters
			List<Filter> filters = ChanceIncHandler.retrieveFilters(lookfor, defaults, assets.filters, mages.get(mages.size() - 1).pose, mages.get(mages.size() - 1).race);

			int at = this.random.nextInt(power + 6) - 5; // -4 to power
			while(moreFilters.size() == 0 && at >= -5)
			{
				moreFilters = getFiltersForTier(ChanceIncHandler.getFiltersWithPower(at, power, filters), tier);
				if(different)
					moreFilters = ChanceIncHandler.getValidUnitFilters(moreFilters, mages);
				else
					moreFilters = ChanceIncHandler.getValidUnitFilters(moreFilters, mages.get(0));

				at--;
			}
			

			if(moreFilters.size() < 2)
			{
				moreFilters.clear();
				moreFilters.addAll(filters);
			}
			
			List<Filter> actualFilters = this.getFiltersForTier(moreFilters, tier);
			
			if(actualFilters.size() == 0)
			{
				System.out.println(moreFilters);
				System.out.println("No filters for tier " + tier + ", original " + moreFilters.size() + " / " + mages.get(0).race);
				return;
			}
			
		
			List<Filter> oldFilters = new ArrayList<>();
			for(Unit u : units)
				oldFilters.addAll(u.appliedFilters);

			// Montags get totally random shit
			if(montags)
			{
				double maxpower = 0;
				for(Unit u : mages)
				{
					
					List<MagicPath> paths = new ArrayList<>();
					MagicPathInts gotpicks = u.getMagicPicks(true);
					
					for(MagicPath path : MagicPath.NON_HOLY)
					{
						if(gotpicks.get(path) > 1)
							paths.add(path);
					}
					List<Filter> givenFilters = this.getPossibleFiltersByPaths(actualFilters, paths);
					givenFilters.removeAll(u.appliedFilters);


					if(chandler.countPossibleFilters(givenFilters, u) == 0)
					{
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findCommonPaths(mages));
						givenFilters.removeAll(u.appliedFilters);

					}
					
					Filter f = chandler.handleChanceIncs(u, givenFilters).getRandom(this.random);
					if(f != null)
					{
						u.appliedFilters.add(f);
						
						if(f.power > maxpower)
							maxpower = f.power;
						

					}
			
					
					
				}
				power -= maxpower;

			}
			// If all mages of tier have distinguishing paths and either luck or no common paths, separate filters are rolled. 
			else if(different)
			{
				double maxpower = 0;
				for(Unit u : mages)
				{
					List<Filter> givenFilters;
					if(MageGenerator.findDistinguishingPaths(u, mages).size() == 0) {
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findCommonPaths(mages));
					} else {
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findDistinguishingPaths(u, mages));
					}
					givenFilters.removeAll(u.appliedFilters);
	
					
						

					if(chandler.countPossibleFilters(givenFilters, u) == 0)
					{
						givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findDistinguishingPaths(u, mages));
						givenFilters.removeAll(u.appliedFilters);
					}
					

					// CanAdd check
					givenFilters = ChanceIncHandler.getValidUnitFilters(givenFilters, mages);
					

					// Use old filters when feasible!
					List<Filter> tempFilters = new ArrayList<>(givenFilters);
					tempFilters.retainAll(oldFilters);
					tempFilters = ChanceIncHandler.getValidUnitFilters(tempFilters, mages);


					if(chandler.countPossibleFilters(tempFilters, u) > 0 && this.random.nextDouble() > 0.25)
						givenFilters = tempFilters;



					
					Filter f = chandler.handleChanceIncs(u, givenFilters).getRandom(this.random);

					if(f != null)
					{
						
						if(!handleMontagFilters(u, (int)Math.round(f.power), defaults, lookfor))
							u.appliedFilters.add(f);
			
						
						handleGiveToAll(f, units, tier);
	
						
						if(f.power > maxpower)
							maxpower = f.power;
					}
					else
						System.out.println("Nation " + nation.getSeed() + " had a null filter for a mage. Power was " + power + " and tier " + tier + ".");
					
				}
				power -= maxpower;
			}
			// Otherwise all mages get the same filter if possible
			else 
			{
				List<Filter> givenFilters = this.getPossibleFiltersByPaths(actualFilters, MageGenerator.findCommonPaths(mages));

				// Remove given filters to avoid duplicates
				for(Unit u : mages)
				{
					givenFilters.removeAll(u.appliedFilters);
				}


				// If no filters, failsafe into giving whatever
				if(chandler.countPossibleFilters(givenFilters, mages.get(0)) == 0)
				{
					givenFilters = new ArrayList<>(actualFilters);
					for(Unit u : mages)
					{
						givenFilters.removeAll(u.appliedFilters);
					}
				}
				

				// CanAdd check
				givenFilters = ChanceIncHandler.getValidUnitFilters(givenFilters, mages);

				
				// Use old filters when feasible!
				List<Filter> tempFilters = new ArrayList<>(givenFilters);
				tempFilters.retainAll(oldFilters);
				tempFilters = ChanceIncHandler.getValidUnitFilters(tempFilters, mages);


				if(chandler.countPossibleFilters(tempFilters, mages.get(0)) > 0 && this.random.nextDouble() > 0.25)
					givenFilters = tempFilters;
					

				
				Filter f = chandler.handleChanceIncs(mages.get(0), givenFilters).getRandom(this.random);
				
				for(Unit u : mages)
				{
					if(!handleMontagFilters(u, (int)Math.round(f.power), defaults, lookfor))
						u.appliedFilters.add(f);
				}
				
				handleGiveToAll(f, units, tier);
				
				power -= f.power;
			}
			
	
		}
	
	}
	
	
	private boolean handleMontagFilters(Unit u, int power, String[] defaults, String lookfor)
	{
		if(unitGen.getMontagUnits(u).size() == 0)
		{
			return false;
		}
		else
		{
			power = Math.max(1, power);
			this.applyFilters(unitGen.getMontagUnits(u), power, defaults, lookfor);
			
			return true;
		}

	}
	
	private void handleGiveToAll(Filter f, List<Unit> units, int tier)
	{
		if(f.tags.containsName("givetoall") && tier < 4 && tier > 0)
			for(int i = 1; i <= 3; i++)
			{
				if (!f.tags.contains("notfortier", String.valueOf(i))) {
					Stream.concat(getMagesOfTier(units, i).stream(), nation.selectCommanders("montagmage"))
							.filter(u -> !u.appliedFilters.contains(f))
							.forEach(u -> u.appliedFilters.add(f));
				}
			}
	}

	
	
	/**
	 * Returns filters that align with given path (either #personalmagic or possibly also if just chanceincs)
	 * This used to have a strict mode which tried to look at chanceincs for matching paths, but with changes to how
	 * chanceincs worked, I don't think it really worked anymore, and it might not even be necessary anymore with all
	 * sorts of other new commands. It has been removed, but if there is need for it again, the strict mode should
	 * "require a positive chanceinc" for the paths in addition to #personalmagic. Strict mode was only used for when
	 * mages were found to have distinguishing paths.
	 * @param filters Filters
	 * @param paths Paths
	 * @return
	 */
	private List<Filter> getPossibleFiltersByPaths(List<Filter> filters, List<MagicPath> paths)
	{
		
		List<Filter> list = new ArrayList<>();
		for(Filter f : filters)
		{
			if(f.tags.streamAllValues("personalmagic")
					.map(Arg::getMagicPath)
					.allMatch(paths::contains))
				list.add(f);
		}
		
		return list;
	}
	
	/** 
	 * Checks whether all mages given have a distinguishing path
	 * @param mages
	 * @return
	 */
	private boolean allHaveDistinguishingPath(List<Unit> mages)
	{
		boolean d = true;
		for(Unit u : mages)
		{
			if(MageGenerator.findDistinguishingPaths(u, mages).size() == 0)
				d = false;
		}
		return d;
	}
	
	/**
	 * Returns a list of mages of given tier out of the given list of mages
	 * @param mages List of mages
	 * @param tier Tier
	 * @return
	 */
	private List<Unit> getMagesOfTier(List<Unit> mages, int tier)
	{
		return mages.stream()
			.filter(u -> u.tags.getInt("priest").orElse(
				u.tags.getInt("schoolmage").orElse(-1)) == tier)
			.collect(Collectors.toList());
	}

	/**
	 * Returns tier of a given unit     
	 * Can look up the original tier of extramages 
	 * @param u Unit to look up
	 * @return
	 */

	private int getMageTier(Unit u)
	{
		int tier = -1;
		String[] mageTags = {"priest","schoolmage","extramage"};

		for (String s : mageTags)
		{
			if (u.tags.getInt(s).isPresent())
				tier = Integer.max(tier, u.tags.getValue(s).orElseThrow().getInt());
		}

		return tier;
	}		
	
	/**
	 * Returns a list of filters without #notfortier <tier>
	 * @param filters List of original filters
	 * @param tier Tier
	 * @return
	 */
	private List<Filter> getFiltersForTier(List<Filter> filters, int tier)
	{
		return filters.stream()
				.filter(f -> f.tags.getAllValues("notfortier").stream()
						.map(Arg::getInt)
						.noneMatch(i -> i.equals(tier)))
				.collect(Collectors.toList());
	}
	
	
//	/**
//	 * The old method
//	 * @param units
//	 * @param power
//	 * @param filters
//	 */
//	public void applyFilters_old(List<Unit> units, int power, List<Filter> filters)
//	{
//		if(units.size() == 0)
//			return;
//
//
//		ChanceIncHandler chandler = new ChanceIncHandler(nation, "magegen");
//		while(power > 0)
//		{
//			List<Filter> moreFilters = ChanceIncHandler.getFiltersWithPower(power, power, filters);
//
//
//			if(chandler.handleChanceIncs(units.get(0), moreFilters).size() == 0 || this.random.nextDouble() > 0.7)
//				moreFilters = ChanceIncHandler.getFiltersWithPower(power - 1, power, filters);
//			if(chandler.handleChanceIncs(units.get(0), moreFilters).size() == 0 || power == 1)
//				moreFilters = ChanceIncHandler.getFiltersWithPower(-100, power, filters);
//			if(chandler.handleChanceIncs(units.get(0), moreFilters).size() == 0)
//				break;
//
//
//			Filter f = Filter.getRandom(this.random, chandler.handleChanceIncs(units.get(0), moreFilters));
//
//
//			// validate
//
//			int added = 0;
//			for(Unit u : units)
//			{
//				boolean ok = true;
//
//				// Validate suitable paths
//				int[] paths = u.getMagicPicks();
//				for(String tag : f.tags)
//				{
//					List<String> args = Generic.parseArgs(tag);
//					if(args.get(0).equals("personalmagic") && args.size() > 1)
//					{
//						int path = Generic.PathToInteger(args.get(1));
//						if(paths[path] == 0)
//							ok = false;
//					}
//				}
//
//				// Validate tier things
//				int tier = 0;
//				if(Generic.getTagValue(u.tags, "schoolmage") != null)
//					tier = Integer.parseInt(Generic.getTagValue(u.tags, "schoolmage"));
//				if(Generic.getTagValue(u.tags, "priest") != null && tier == 0)
//					tier = Integer.parseInt(Generic.getTagValue(u.tags, "priest"));
//
//				if(Generic.getTagValue(f.tags, "notfortier") != null)
//				{
//					int nottier = Integer.parseInt(Generic.getTagValue(f.tags, "notfortier"));
//					if(tier == nottier)
//						ok = false;
//				}
//
//				if(ok && ChanceIncHandler.canAdd(u, f))
//				{
//					added++;
//					u.appliedFilters.add(f);
//				}
//
//			}
//
//			filters.remove(f);
//			if(added > 0)
//			{
//				power -= f.power;
//			}
//
//		}
//
//	}
	


	public List<List<Unit>> generateMageBases()
	{

		
		// PRIMARIES
		Race race = nation.races.get(0);
		
		double bonussecchance = 1;
		bonussecchance += race.tags.getDouble("secondaryracemagemod").orElse(0D);
		bonussecchance -= nation.races.get(1).tags.getDouble("primaryracemagemod").orElse(0D);
		
		

		if(this.random.nextDouble() < 0.075 * bonussecchance && nation.races.get(1).hasRole("mage"))
		{
			if(this.hasPoseForTier("mage", nation.races.get(1), 3))
				race = nation.races.get(1);
		}
		else if(!nation.races.get(0).hasRole("mage")) // This hopefully never happens since it can cause issues unless handled very delicately.
			race = nation.races.get(1);
		
		
		final double onechance = race.tags.getDouble("oneprimarychance").orElse(0.75);
		final double twochance = race.tags.getDouble("twoprimarychance").orElse(0.2);
		final double threechance = race.tags.getDouble("threeprimarychance").orElse(0.05);
		
		double rand = this.random.nextDouble() * (onechance + twochance + threechance);
		
		final int primaryamount;
		if(rand < onechance)
			primaryamount = 1;
		else if(rand < onechance + twochance)
			primaryamount = 2;
		else
			primaryamount = 3;

		
		List<Unit> primaries = generateBases("mage", race, primaryamount, 3);
		


		
		
		
		// SECONDARIES
		boolean changed_at_secondary = false;
		if(this.random.nextDouble() < 0.1 * bonussecchance && race != nation.races.get(0)){/*do nothing*/}
		else
			race = nation.races.get(0);
		
		if((race == nation.races.get(0) && this.random.nextDouble() < 0.1 && hasPoseForTier("mage", nation.races.get(1), 2)) || !hasPoseForTier("mage", nation.races.get(0), 2))
		{
			changed_at_secondary = true;
			race = nation.races.get(1);
		}

		
		double randroll = this.random.nextDouble();
		int secondaryamount = 1;
		// 3 primaries - 35% chance for 3 secondaries.
		if(primaryamount == 3 && randroll < 0.35)
			secondaryamount = 3;
		// 3 primaries - 15% chance for 0 secondaries.
		else if(primaryamount == 3 && randroll < 0.50)
			secondaryamount = 0;
		
		// 2 primaries - 65% chance for 2 secondaries. Otherwise 1 or 0.
		if(primaryamount == 2 && randroll < 0.65)
			secondaryamount = 2;
		// 2 primaries - 15% chance for 0 secondaries. 
		else if(primaryamount == 2 && randroll < 0.75)
			secondaryamount = 0;

		// 1 primaries - 25% chance for 3 secondaries.
		if(primaryamount == 1 && randroll < 0.1)
			secondaryamount = 3;
		// 1 primaries - 25% chance for 2 secondaries.
		else if(primaryamount == 1 && randroll < 0.3)
			secondaryamount = 2;
		
		List<Unit> secondaries = new ArrayList<Unit>();
		if(secondaryamount > 0)
		{
			secondaries = deriveBasesFrom(secondaryamount, "mage", primaries.get(0), race, 3, 2);
		}
		
	
		// TERTIARIES
		List<Unit> tertiaries = new ArrayList<>();
		
		// Tertiaries might be of secondary race if primaries aren't.
		bonussecchance = 1;
		bonussecchance += race.tags.getDouble("secondaryracetertiarymagemod").orElse(0D);
		bonussecchance -= nation.races.get(1).tags.getDouble("primaryracetertiarymagemod").orElse(0D);
		
		if((0.05 * bonussecchance > this.random.nextDouble() && hasPoseForTier("mage", nation.races.get(1), 1)) || !hasPoseForTier("mage", nation.races.get(0), 1) || changed_at_secondary)
			race = nation.races.get(1);
		else
			race = nation.races.get(0);
		
		

		int tertiaryamount = 1;
		
		if((primaryamount + secondaryamount > 3 && this.random.nextDouble() < 0.25) || (primaryamount + secondaryamount > 4 && this.random.nextDouble() < 0.5) || primaryamount + secondaryamount > 5)
			tertiaryamount = 0;
	
		

		if(tertiaryamount > 0 && secondaryamount > 0)
		{
			tertiaries = deriveBasesFrom(tertiaryamount, "mage", secondaries.get(0), race, 2, 1);
		}
		else if(tertiaryamount > 0)
		{
			tertiaries = deriveBasesFrom(tertiaryamount, "mage", primaries.get(0), race, 3, 1);
		}
		


		
		List<List<Unit>> list = new ArrayList<List<Unit>>();
		list.add(tertiaries);
		list.add(secondaries);
		list.add(primaries);
		return list;
	}
	
	
	
	private void tagAll(List<Unit> units, String tagName, Object... values)
	{
		if(units == null)
			return;
	
		for(Unit u : units)
			u.tags.addArgs(tagName, values);
	}
	
	private boolean shouldChangePose(String posename, Race newrace, Race oldrace, Pose current, int currenttier, int futuretier)
	{
		boolean future = false;
		boolean past = false;
		

		
		// Change pose if we have specific next pose
		for(Pose p : newrace.getPoses(posename))
			if(p.tags.contains("tier", futuretier) && p != current)
				future = true;
		
		// Current doesn't contain pose for next tier
		if(current.tags.contains("tier", currenttier) && !current.tags.contains("tier", futuretier))
			past = true;
		
		// Forbidden from the future tier
		if(current.tags.contains("notfortier", futuretier))
			past = true;
		
		// We have a specific pose and current doesn't contain the required pose
		if(future && !current.tags.contains("tier", futuretier))
			past = true;
		
		// Random chance
		if(future && this.random.nextDouble() < 0.075)
			past = true;
		
		if(oldrace != newrace)
			past = true;
		

			
		return past;
	}
	

	private List<Unit> deriveBasesFrom(int amount, String posename, Unit parent, Race race, int fromTier, int toTier)
	{
		List<Unit> units = new ArrayList<>();
		
		Pose newpose = null;
		if(shouldChangePose(posename, race, parent.race, parent.pose, fromTier, toTier))
		{
			List<Pose> poses = this.getPossiblePoses(posename, race, toTier);
			if(poses.size() > 0)
				newpose = chandler.getRandom(poses, race);

		}
		else
		{
			newpose = parent.pose;
		}
		
		
		for(int i = 0; i < amount; i++)
		{
			Unit nu = unitGen.generateUnit(race, newpose);
			units.add(nu);
		}
		
		return units;
		
	}
	
	public void deriveEquipment(Unit parent, Unit follower, int fromTier, int toTier)
	{
		deriveEquipment(List.of(parent), List.of(follower), fromTier, toTier);
	}

	
	private void polishUnit(Unit unit)
	{
		
		if(unit.getSlot("weapon") != null)
		{
			boolean troop = Generic.getAllUnitTags(unit).containsName("troopweapon")
					|| unit.getSlot("weapon").tags.containsName("troopweapon");
			
			if(troop)
			{
				if(nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "lgt") >= 3)
				{
					if(unit.getSlot("offhand") != null && !unit.getSlot("offhand").armor)
						unit.setSlot("offhand", null);
				}
				else if(nationGen.weapondb.GetInteger(unit.getSlot("weapon").id, "2h") == 1)
				{
					unit.setSlot("offhand", null);
				}
			}	
		
		}
	}
	
	public void deriveEquipment(List<Unit> parents, List<Unit> followers, int fromTier, int toTier)
	{
		
		int max = parents.size();
		if(followers.size() < parents.size())
			max = followers.size();
		
		// Handle varyslot
		String oldslot = parents.get(0).tags.getString("varyslot").orElse(null);
		String newslot = null;
		
		if(followers.size() > 1)
		{
			List<String> possibleslots = this.getPossibleVarySlots(followers.get(0));
			if(possibleslots.contains(oldslot))
				newslot = oldslot;
			else
				newslot = possibleslots.get(random.nextInt(possibleslots.size()));
			
			tagAll(followers, "varyslot", newslot);
		}
		
		

		ItemSet used = new ItemSet();
		for(int k = 0; k < max; k++)
		{
			Unit u = parents.get(k); // Old unit
			Unit nu = followers.get(k); // New unit
			
			List<String> deriveslots = nu.pose.tags.getAllArgs("deriveslots").stream()
					.flatMap(List::stream)
					.map(Arg::get)
					.collect(Collectors.toList());
			
			if (deriveslots.isEmpty()) {
				deriveslots = nu.race.tags.getAllArgs("deriveslots").stream()
						.flatMap(List::stream)
						.map(Arg::get)
						.collect(Collectors.toList());
			}
			
			if (deriveslots.isEmpty()) {
				deriveslots = List.of("weapon", "offhand", "helmet", "armor", "hair", "cloakb", "mount");
			}
			
			
			for(String slot : deriveslots)
			{
				
				if(u.pose.getItems(slot) == null || nu.pose.getItems(slot) == null || nu.getSlot(slot) != null)
					continue;
				
				
				// Just targeted tier		
				ItemSet all = nu.pose.getItems(slot).filterTag(new Command("tier", new Arg(toTier)));
				if(all.possibleItems() == 0) // Allows tiers below toTier
					for(int i = toTier + 1; i <= fromTier; i++)
						all = all.filterOutTag(new Command("tier", new Arg(i)));
				if(all.possibleItems() == 0 && toTier != 1) // Just everything
					all = nu.pose.getItems(slot).getCopy();
				
				
				if(u.getSlot(slot) != null)
				{
					Item newitem = getDerivedItem(u.getSlot(slot), nu, used);
					
					// Failure may result in using old armor in tier 2 to 1 
					if((newitem == null || !newitem.tags.contains("eliteversion", u.getSlot(slot).name)) && slot.equals("armor") && toTier < 2 && fromTier == 2 && u.pose == nu.pose)
						newitem = u.getSlot(slot);
					

					
					if(newitem == null)
					{
						if(nu.pose == u.pose && (u.getSlot(slot).tags.contains("tier", toTier) || !u.getSlot(slot).tags.contains("tier", fromTier)))
						{
							newitem = u.getSlot(slot);
						}
					}
					if(newitem == null)
					{				
						
	
						if(all.possibleItems() > 1)
						{
							all.remove(u.getSlot(slot));

						}
						
						ItemSet temp = new ItemSet();
						temp.addAll(all);
						temp.removeAll(exclusions);
						if(chandler.countPossibleFilters(temp, nu) > 0)
						{
							all = temp;
						}
		
						newitem = chandler.handleChanceIncs(nu, all).getRandom(this.random);
					
					}
	
			
					if(slot.equalsIgnoreCase(newslot))
						exclusions.add(newitem);
					
					if(newitem != null)
					{
						nu.setSlot(slot, newitem);
						used.add(newitem);
					}
					
				}

			}
			
			



			// This should fill in missing slots like mount
			unitGen.armorUnit(nu, null, exclusions, new Command("tier", new Arg(toTier)), true);
			unitGen.armUnit(nu, null, exclusions, new Command("tier", new Arg(toTier)), true);
			

			
			if(toTier < 2)
			{
				if(Generic.getAllUnitTags(nu).containsName("mage_nolowtierhat"))
				{
					nu.setSlot("helmet", null);

				}
				if(!Generic.getAllUnitTags(nu).containsName("mage_lowtiercloak"))
				{
					nu.setSlot("cloakb", null);
					nu.setSlot("cloakf", null);
				}
			}
			
			
			
			this.polishUnit(nu);
			
		}
		
	
	}
	
	private Item getDerivedItem(Item item, Unit u, ItemSet used)
	{
		if(used == null)
			used = new ItemSet();
	

		ItemSet items = u.pose.getItems(item.slot).filterRemoveSameSprite(exclusions);
		items.removeAll(exclusions);
		
		if(chandler.countPossibleFilters(items, u) == 0)
		{
			items = u.pose.getItems(item.slot).filterRemoveSameSprite(exclusions);
		}
		
		ItemSet possibles = new ItemSet();
		for(Item i : items)
		{
			if(i.tags.contains("eliteversion", item.name))
			{
				possibles.add(i);
			}
		}
		
		possibles.remove(item);
		
		ItemSet test = new ItemSet();
		test.addAll(possibles);
		test.retainAll(used);
		if(chandler.handleChanceIncs(u, test).hasPossible())
		{
			possibles = test;
		}

		return chandler.getRandom(possibles, u);
	}
	
	
	public List<Unit> generateBases(String posename, Race race, int amount, int tier)
	{
		
		List<Unit> units = new ArrayList<>();
		
	
		Pose p = chandler.getRandom(getPossiblePoses(posename, race, tier), race);
		if(p == null)
			p = chandler.getRandom(getPossiblePoses(posename, race, tier - 1), race);
	
		if(p == null)
			amount = 0;

		
		for(int i = 0; i < amount; i++)
		{
			Unit u = unitGen.generateUnit(race, p);
			units.add(u);
		}
		
		return units;

	}
	
	

	public void equipBase(Unit u, int tier)
	{
		
		unitGen.armorUnit(u, used, exclusions, new Command("tier", new Arg(tier)), false);
		unitGen.armUnit(u, used, exclusions, new Command("tier", new Arg(tier)), false);
		if(tier == 3)
		{
			u.setSlot("cloakb", unitGen.getSuitableItem("cloakb", u, exclusions, used, null));
		}
		

		if(!exclusions.contains(u.getSlot("weapon")))
			exclusions.add(u.getSlot("weapon"));
		
	
		this.polishUnit(u);
	}
	
	/* not used at present - deprecate?
	public List<Unit> generateaNew(String posename, Race race, int amount, int tier)
	{
		
		
		List<Unit> list = this.generateBases(posename, race, amount, tier);
		
		Unit u = list.get(0);
		
		this.equipBase(u, tier);
		
		if(tier == 3)
		{
			u.setSlot("cloakb", unitGen.getSuitableItem("cloakb", u, exclusions, used, null));
		}
		
	
		this.varyEquipment(u, list, tier);
		return list;
	}
	*/
	
	
	private boolean hasPoseForTier(String posename, Race race, int tier)
	{
		boolean isPrimaryRace = (race == nation.races.get(0));
		for(Pose p : race.getPoses(posename))
		{
			if(p.tags.contains("tier", tier) && (isPrimaryRace || !p.tags.containsName("primaryraceonly")))
				return true;
		}
		

		for(Pose p : race.getPoses(posename))
		{
			if(!p.tags.contains("notfortier ", tier) && (isPrimaryRace || !p.tags.containsName("primaryraceonly")))
				return true;
		}
		
		return false;
		
	}
	
	private List<Pose> getPossiblePoses(String posename, Race race, int tier)
	{
		boolean isPrimaryRace = (race == nation.races.get(0));
		
		List<Pose> possibles = new ArrayList<>();
		for(Pose p : race.getPoses(posename))
		{
			if(p.tags.contains("tier", tier) && (isPrimaryRace || !p.tags.containsName("primaryraceonly")))
			{
				possibles.add(p);
			}
		}
		
		if(possibles.size() == 0)
		{
			for(Pose p : race.getPoses(posename))
			{
				if(!p.tags.contains("notfortier", + tier) && (isPrimaryRace || !p.tags.containsName("primaryraceonly")))
				{
					possibles.add(p);
				}
			}
		}
		
		
		if(possibles.size() == 0)
		{
			System.out.println("CRITICAL ERROR: No possible pose for " + race.name + " " + posename + " for tier " + tier + ". Nation seed: " + nation.getSeed() +  " and main race " + nation.races.get(0));
		}
		return possibles;
			
	}
	
	
	private List<String> getPossibleVarySlots(Unit u)
	{
		//  Get slot to vary
		List<String> possibleslots = u.pose.tags.getAllValues("varyslot").stream()
				.map(Arg::get).collect(Collectors.toList());
		
		if(possibleslots.size() == 0)
		{
			if(!u.pose.tags.containsName("dontvaryhat")
					&& u.pose.getItems("helmet") != null
					&& chandler.countPossibleFilters(u.pose.getItems("helmet"), u) > 0)
			{
				possibleslots.add("helmet");
			}
			if(!u.pose.tags.containsName("dontvaryweapon")
					&& u.pose.getItems("weapon") != null
					&& chandler.countPossibleFilters(u.pose.getItems("weapon"), u) > 0)
			{
				possibleslots.add("weapon");
			}
		}
		
		return possibleslots;
	}

	
	private void varyEquipment(Unit u, List<Unit> all, int tier)
	{
		int amount = all.size();
		
		// Copy gear
		for(Unit other : all)
		{
			if(other == u)
				continue;
			
			u.slotmap.slots().forEach(slot -> other.setSlot(slot, u.getSlot(slot)));
		}
	
		
		List<String> possibleslots = this.getPossibleVarySlots(u);
		
		if(possibleslots.size() == 0)
			return;
		
		String slot = possibleslots.get(this.random.nextInt(possibleslots.size()));
		
		// Vary item
		ItemSet stuff = u.pose.getItems(slot).filterTag(new Command("tier", new Arg(tier)));
		stuff.removeAll(exclusions);
		stuff.remove(u.getSlot(slot));
		
		if(chandler.countPossibleFilters(stuff, u) == 0)
		{
			stuff = u.pose.getItems(slot).filterTag(new Command("tier", new Arg(tier)));
			stuff.removeAll(exclusions);
			stuff.remove(u.getSlot(slot));
		}




		
		exclusions.add(u.getSlot(slot));
		
		for(int i = 1; i < amount; i++)
		{
			Unit nu = all.get(i);
			Item newitem = unitGen.getSuitableItem(slot, nu, exclusions, used, new Command("tier", new Arg(tier)));
			nu.setSlot(slot, newitem);
			exclusions.add(nu.getSlot(slot));
			
			
		}
		
		tagAll(all, "varyslot", slot);
		for(Unit un : all)
			this.polishUnit(un);

	}
	
	


	
	public void resolveAge(List<Unit> units)
	{
		Random r = this.random;
		double random = r.nextDouble();
		for(Unit u : units)
		{
			
			double picks = u.getMagicAmount(20);
			double limit = 0.05 + (picks - 2) * 0.1; 
			if(picks < 4)
				limit = 0;
			
			if(random < limit && u.getSlot("mount") == null)
			{
				u.commands.add(Command.args("#mapmove", "-1"));
			}
			else if(picks > 6)
			{
				u.commands.add(Command.args("#older", "-20"));
			}
			else if(picks > 5)
			{
				u.commands.add(Command.args("#older", "-15"));
			}
			else if(picks > 4)
			{
				u.commands.add(Command.args("#older", "-10"));
			}

		}
		

	}
	
	
	private MagicFilter getPriestPattern(int level, boolean max)
	{
		MagicPattern p = new MagicPattern(nationGen);
		p.picks.put(1, level);
	
		List<MagicPath> prio = new ArrayList<>();
		prio.add(MagicPath.HOLY);
		
		MagicFilter m = new MagicFilter(nationGen);
		m.pattern = p;
		m.prio = prio;
		m.name = "PRIESTPICKS";
		m.descSet = "priest";		
		m.tags.add("description","");
		m.tags.add("next","priest start");		
		//m.tags.addName("do_not_show_in_descriptions");
		m.tags.addName("tier"+Integer.toString(level));
		if (max)
			m.tags.addName("highpriest");
		assets.initializeFilters(List.of(m),"getPriestPattern()");
		
		return m;
	
	}

	private double randomizeLeadership(Unit u, double bonus, int tier)
	{
		double leaderrandom = this.random.nextDouble() + bonus;
		double leadership = 0;
		double adjustedLeadership = 0;
		double minimumLeadership = 0;
		
		if (u.tags.containsName("priest"))
			leaderrandom += 0.1;
		
		if (u.tags.containsName("warriormage"))
		{	
			//just max out the bonus for anything with warriormage
			leaderrandom += 0.75;
		}
		else
		{
			if (tier > 1)
			{
				leaderrandom += 0.1;
			
				if (tier == 3)
					leaderrandom += 0.1;
			
				//advanced priests are usually good leaders
				if (u.tags.containsName("priest") && this.random.nextDouble() < 0.85)
				{
					leaderrandom += 0.55;
					minimumLeadership = 40;
				}
			}
			
			//mages with leadership bonuses get an bigger than normal bonus
			if (!u.tags.containsName("priest"))
			{
				if (tier == 3 || u.tags.containsName("#superior_leader") || u.tags.containsName("#good_leader"))
					leaderrandom += 0.4;
			}
		}
		
		if (u.tags.containsName("#good_leader")) leaderrandom += 0.2;
		if (u.tags.containsName("#superior_leader")) leaderrandom += 0.4;
		
		leaderrandom += bonus;
		
		//final leadership values are rounded to multiples of 5, but 10/40/80/120 thresholds are used to determine leadership level
		if (leaderrandom > 1.75)
			leadership = 120;
		else if (leaderrandom > 1.7)
			leadership = 81;
		else if (leaderrandom > 1.55)
			leadership = 80;
		else if (leaderrandom > 1.5)
			leadership = 61;
		else if (leaderrandom > 1.4)
			leadership = 60;
		else if (leaderrandom > 1)
			leadership = 40;
		else if (leaderrandom > 0.9)
			leadership = 39;
		else if (leaderrandom > 0.1)
			leadership = 10;
		else
			leadership = 0;
		
		
		leadership = Math.max(leadership, minimumLeadership);
		
		
		if (leadership > 80)
		{
			u.commands.add(new Command("#expertleader"));
			adjustedLeadership = (Math.round(leadership / 5) * 5) - 120;
		}
		else if (leadership > 60)
		{
			u.commands.add(new Command("#goodleader"));
			adjustedLeadership = (Math.round(leadership / 5) * 5) - 80;
		}
		else if (leadership > 10)
		{
			u.commands.add(new Command("#okleader"));
			adjustedLeadership = (Math.round(leadership / 5) * 5) - 40;
		}
		else if (leadership > 0)
		{
			u.commands.add(new Command("#poorleader"));
			adjustedLeadership = (Math.round(leadership / 5) * 5) - 10;
		}
		else
			u.commands.add(new Command("#noleader"));
		
		
		double maximumCost = 50;
		double cost = 0;
		
		cost = Math.round(leadership / 2);
		
		//priests and warriormages get a discount
		if (u.tags.containsName("priest"))
		{
			//high leadership priests charge a higher premium than mages - probably because you already have enough reasons to recruit the mages?
			maximumCost = 60;
		
			if (cost >= 20)
				cost -= 10;
			else
				cost -= 5;
		}
		else if (u.tags.containsName("warriormage"))
			cost -= 10;
		
		cost = Math.min(cost, maximumCost);
		
		if (adjustedLeadership != 0)
			u.commands.add(Command.args("#command", Double.toString(adjustedLeadership)));
		
		if (cost != 0)
			u.commands.add(Command.args("#gcost", "+" + Double.toString(cost)));
		
		return leadership;
	}

}
