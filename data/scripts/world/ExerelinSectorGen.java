package data.scripts.world;

import java.awt.Color;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.CoreScript;
import com.fs.starfarer.api.impl.campaign.events.CoreEventProbabilityManager;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import data.scripts.campaign.EconomyLogger;
import data.scripts.world.exerelin.*;
import data.scripts.world.exerelin.ExerelinSetupData;
import data.scripts.world.exerelin.ExerelinMarketConditionPicker;
import exerelin.plugins.*;
import exerelin.*;
import exerelin.utilities.ExerelinConfig;

@SuppressWarnings("unchecked")
public class ExerelinSectorGen implements SectorGeneratorPlugin
{
	private static String[] possibleSystemNames = {"Exerelin", "Askar", "Garil", "Yaerol", "Plagris", "Marot", "Caxort", "Laret", "Narbil", "Karit",
		"Raestal", "Bemortis", "Xanador", "Tralor", "Exoral", "Oldat", "Pirata", "Zamaror", "Servator", "Bavartis", "Valore", "Charbor", "Dresnen",
		"Firort", "Haidu", "Jira", "Wesmon", "Uxor"};
	private static String[] possiblePlanetNames = new String[] {"Baresh", "Zaril", "Vardu", "Drewler", "Trilar", "Polres", "Laret", "Erilatir",
		"Nambor", "Zat", "Raqueler", "Garret", "Carashil", "Qwerty", "Azerty", "Tyrian", "Savarra", "Torm", "Gyges", "Camanis", "Ixmucane", "Yar", "Tyrel",
		"Tywin", "Arya", "Sword", "Centuri", "Heaven", "Hell", "Sanctuary", "Hyperion", "Zaphod", "Vagar", "Green", "Blond", "Gabrielle", "Masset",
		"Effecer", "Gunsa", "Patiota", "Rayma", "Origea", "Litsoa", "Bimo", "Plasert", "Pizzart", "Shaper", "Coruscent", "Hoth", "Gibraltar", "Aurora",
		"Darwin", "Mendel", "Crick", "Franklin", "Watson", "Pauling",
		"Rutherford", "Maxwell", "Bohr", "Pauli", "Curie", "Meitner", "Heisenberg", "Feynman"};
	private static String[] possibleStationNames = new String[] {"Base", "Orbital", "Trading Post", "HQ", "Post", "Dock", "Mantle", "Ledge", "Customs", "Nest",
		"Port", "Quey", "Terminal", "Exchange", "View", "Wall", "Habitat", "Shipyard", "Backwater"};
	
	private List possibleSystemNamesList = new LinkedList(Arrays.asList(possibleSystemNames));
	private List possiblePlanetNamesList = new LinkedList(Arrays.asList(possiblePlanetNames));
	
	private static String[] possiblePlanetTypes = new String[] {"desert", "jungle", "frozen", "terran", "arid", "water"};
	private static String[] possiblePlanetTypesUninhabitable = new String[] {"barren", "lava", "toxic", "cryovolcanic", "rocky_metallic", "rocky_unstable", "rocky_ice", "gas_giant", "ice_giant"};
	private static String[] possibleMoonTypes = new String[] {"frozen", "barren", "lava", "toxic", "cryovolcanic", "rocky_metallic", "rocky_unstable", "rocky_ice"};
	private static String[] possibleStationImages = new String[] {"station_side00", "station_side02", "station_side04"};
	
	private static String[] factionIds = new String[]{"sindrian_diktat", "tritachyon", "luddic_church", "pirates", "hegemony", "independent"};
	private boolean isStartSystemChosen = false;

	public static Logger log = Global.getLogger(ExerelinSectorGen.class);

	private String getRandomFaction()
	{
		return factionIds[ExerelinUtils.getRandomInRange(0, 5)];
	}
	
	/*
	private void addCommodityStockpile(MarketAPI market, String commodityID, float amount)
	{
		CommodityOnMarketAPI commodity = market.getCommodityData(commodityID);
		CargoAPI cargo = market.getSubmarket(Submarkets.SUBMARKET_OPEN).getCargo();
		commodity.addToStockpile(amount);
		commodity.addToAverageStockpile(amount);
		cargo.addCommodity(commodityID, amount);
		//log.info("Adding " + amount + " " + commodityID + " to " + market.getName());
	}
	*/
	
	private void addCommodityStockpile(MarketAPI market, String commodityID, float minMult, float maxMult)
	{
		float multDiff = maxMult - minMult;
		float mult = minMult + (float)(Math.random()) * multDiff;
		CommodityOnMarketAPI commodity = market.getCommodityData(commodityID);
		float demand = commodity.getDemand().getDemandValue();
		float amountToAdd = demand*mult;
		CargoAPI cargo = market.getSubmarket(Submarkets.SUBMARKET_OPEN).getCargo();
		commodity.addToStockpile(amountToAdd);
		commodity.addToAverageStockpile(amountToAdd);
		cargo.addCommodity(commodityID, amountToAdd * 0.15f);
		//log.info("Adding " + amount + " " + commodityID + " to " + market.getName());
	}
	
	private MarketAPI addMarketToEntity(int i, SectorEntityToken entity, String owningFactionId, String planetType, boolean isStation)
	{
		// don't make the markets too big; they'll screw up the economy big time
		int marketSize = 1;
		if (isStation) marketSize = ExerelinUtils.getRandomInRange(1, 2) + ExerelinUtils.getRandomInRange(1, 2);	// stations are on average smaller
		else marketSize = ExerelinUtils.getRandomInRange(2, 3) + ExerelinUtils.getRandomInRange(2, 3);
		
		// first planet in the system is a regional capital
		// it is always at least size 4 and has a military base
		if (i == 0)
		{
			if (marketSize < 4)
				marketSize = 4;
		}
		// Alex says "You can set marketSize via MarketAPI, but it's not "nicely" mutable like other stats at the moment."
		// so to be safe we only spawn the market after we already know what size it'll be
		
		MarketAPI newMarket = Global.getFactory().createMarket(entity.getId() + "_market", entity.getName(), marketSize);
		newMarket.setPrimaryEntity(entity);
		entity.setMarket(newMarket);
		
		newMarket.setFactionId(owningFactionId);
		newMarket.setBaseSmugglingStabilityValue(0);
		newMarket.getTariff().modifyFlat("generator", 0.2f);	// TODO put in a config or something 
		
		newMarket.addSubmarket(Submarkets.SUBMARKET_OPEN);
		newMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
		newMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		
		newMarket.addCondition("population_" + marketSize);

		if (i == 0)
		{
			newMarket.addCondition("regional_capital");
			newMarket.addCondition("military_base");
			newMarket.addSubmarket(Submarkets.GENERIC_MILITARY);
		}
		
		// planet type conditions
		if (planetType == "jungle") {
			newMarket.addCondition("jungle");

			if(ExerelinUtils.getRandomInRange(0, 3) == 0)
				newMarket.addCondition("orbital_burns");
		}
		if (planetType == "water")
			newMarket.addCondition("water");
		if (planetType == "arid")
			newMarket.addCondition("arid");
		if (planetType == "terran")
			newMarket.addCondition("terran");
		if (planetType == "desert")
			newMarket.addCondition("desert");
		if (planetType == "frozen")
			newMarket.addCondition("ice");
				
		if(marketSize < 4 && !isStation){
			newMarket.addCondition("frontier");
		}
		
		// add random market conditions
		ExerelinMarketConditionPicker picker = new ExerelinMarketConditionPicker();
		picker.AddMarketConditions(newMarket, marketSize, planetType, isStation);

		if (isStation && marketSize >= 3)
		{
			newMarket.addCondition("exerelin_recycling_plant");
		}
				
		// add per-faction market conditions
		String factionId = newMarket.getFaction().getId();
		if(factionId == "sindrian_diktat" && !isStation) {
			newMarket.addCondition("urbanized_polity");
		}
		if(factionId == "tritachyon") {
			newMarket.addCondition("free_market");
		}
		if(factionId == "luddic_church") {
			newMarket.addCondition("luddic_majority");
			//newMarket.addCondition("cottage_industry");
		}
		if(factionId == "pirates") {
			newMarket.addCondition("free_market");
		}
		if(factionId == "hegemony" && !isStation) {
			newMarket.addCondition("urbanized_polity");
		}
		if(factionId  == "independent" && !isStation) {
			int rand = ExerelinUtils.getRandomInRange(0, 3);
			if (rand == 0)
				newMarket.addCondition("urbanized_polity");
			else if (rand == 1)
				newMarket.addCondition("rural_polity");
		}
		
		// seed the market with some stuff to prevent initial shortage
		// because the vanilla one is broken for some reason
		addCommodityStockpile(newMarket, "green_crew", 0.4f, 0.5f);
		addCommodityStockpile(newMarket, "regular_crew", 0.4f, 0.5f);
		//addCommodityStockpile(newMarket, "veteran_crew", 0.05f, 0.1f);
		addCommodityStockpile(newMarket, "marines", 0.6f, 0.75f);
		addCommodityStockpile(newMarket, "supplies", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "fuel", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "food", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "domestic_goods", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "luxury_goods", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "heavy_machinery", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "metals", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "rare_metals", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "ore", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "rare_ore", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "organics", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "volatiles", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "drugs", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "organs", 0.7f, 0.8f);
		addCommodityStockpile(newMarket, "lobster", 0.7f, 0.8f);
		
		Global.getSector().getEconomy().addMarket(newMarket);
		entity.setFaction(owningFactionId);	// http://fractalsoftworks.com/forum/index.php?topic=8581.0
		
		return newMarket;
	}
	
	public void generate(SectorAPI sector)
	{
		log.info("Starting sector generation...");

		// build systems
		for(int i = 0; i < ExerelinSetupData.getInstance().numSystems; i ++)
			buildSystem(sector, i);

		new Exerelin().generate(sector);

		sector.registerPlugin(new CoreCampaignPluginImpl());
		sector.addScript(new CoreScript());
		sector.addScript(new EconomyLogger());
		sector.addScript(new CoreEventProbabilityManager());
		sector.addScript(new EconomyFleetManager());

		sector.registerPlugin(new ExerelinCoreCampaignPlugin());

		log.info("Finished sector generation");
	}

	public void buildSystem(SectorAPI sector, int systemNum)
	{
		// Create star system with random name
		int systemNameIndex = ExerelinUtils.getRandomInRange(0, possibleSystemNamesList.size() - 1);
		if (systemNum == 0) systemNameIndex = 0;	// there is always a star named Exerelin
		StarSystemAPI system = sector.createStarSystem((String)possibleSystemNamesList.get(systemNameIndex));
				possibleSystemNamesList.remove(systemNameIndex);
		String systemName = system.getName();
		
		int maxSectorSize = ExerelinSetupData.getInstance().maxSectorSize;
		if((ExerelinSetupData.getInstance().numSystems == sector.getStarSystems().size()
		|| ExerelinUtils.getRandomInRange(0,2) == 0)
		&& !isStartSystemChosen)
		{
			log.info("Setting start location " + systemName);
			sector.setRespawnLocation(system);
			sector.getRespawnCoordinates().set(-2500, -3500);
			//sector.setCurrentLocation(system);
			isStartSystemChosen = true;
		}

		// Set star/light colour/background
		SectorEntityToken star;
	
		int starType = 0;
		if(ExerelinConfig.useMultipleBackgroundsAndStars)
			starType = ExerelinUtils.getRandomInRange(0, 10);
		else
			starType = ExerelinUtils.getRandomInRange(0, 1);

		// TODO refactor to remove endless nested ifs
		if(starType == 0)
		{
			star = system.initStar(systemName, "star_yellow", 500f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			//system.setLightColor(new Color(255, 180, 180));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
			else
				system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		}
		else if(starType == 1)
		{
			star = system.initStar(systemName, "star_red", 900f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(255, 180, 180));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/backgrounds/background3.jpg");
			else
				system.setBackgroundTextureFilename("graphics/backgrounds/background1.jpg");
		}
		else if(starType == 2)
		{
			star = system.initStar(systemName, "star_blue", 400f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(135,206,250));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/blue_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/blue_background2.jpg");
		}
		else if(starType == 3)
		{
			star = system.initStar(systemName, "star_white", 300f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			//system.setLightColor(new Color(185,185,240));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/white_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/white_background2.jpg");
		}
		else if(starType == 4)
		{
			star = system.initStar(systemName, "star_orange", 900f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(255,220,0));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/orange_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/orange_background2.jpg");
		}
		else if(starType == 5)
		{
			star = system.initStar(systemName, "star_yellowwhite", 400f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(255,255,224));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
			else
				system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		}
		else if(starType == 6)
		{
			star = system.initStar(systemName, "star_bluewhite", 400f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(135,206,250));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/bluewhite_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/bluewhite_background2.jpg");
		}
		else if(starType == 7)
		{
			star = system.initStar(systemName, "star_purple", 700f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(218,112,214));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/purple_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/purple_background2.jpg");
		}
		else if(starType == 8)
		{
			star = system.initStar(systemName, "star_dark", 700f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(105,105,105));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/dark_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/dark_background2.jpg");
		}
		else if(starType == 9)
		{
			star = system.initStar(systemName, "star_green", 600f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(240,255,240));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/green_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/green_background2.jpg");
		}
		else
		{
			star = system.initStar(systemName, "star_greenwhite", 600f, (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize), (float)ExerelinUtils.getRandomInRange(maxSectorSize*-1, maxSectorSize));
			system.setLightColor(new Color(240,255,240));
			if(ExerelinUtils.getRandomInRange(0,1) == 0)
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/greenwhite_background1.jpg");
			else
				system.setBackgroundTextureFilename("graphics/exerelin/backgrounds/greenwhite_background1.jpg");
		}


		// Build base planets
		int numBasePlanets;
		if(ExerelinSetupData.getInstance().numSystems != 1)
			numBasePlanets = ExerelinUtils.getRandomInRange(ExerelinConfig.minimumPlanets, ExerelinSetupData.getInstance().maxPlanets);
		else
			numBasePlanets = ExerelinSetupData.getInstance().maxPlanets;
		int distanceStepping = (ExerelinSetupData.getInstance().maxSystemSize-4000)/numBasePlanets;
		Boolean gasPlanetCreated = false;
		for(int i = 0; i < numBasePlanets; i = i + 1)
		{
			boolean inhabitable = Math.random() < 0.7f;
			String planetType = "";
			if (inhabitable)
				planetType = possiblePlanetTypes[ExerelinUtils.getRandomInRange(0, possiblePlanetTypes.length - 1)];
			else
				planetType = possiblePlanetTypesUninhabitable[ExerelinUtils.getRandomInRange(0, possiblePlanetTypesUninhabitable.length - 1)];

			String name = "";
			String id = "";
			int planetNameIndex = ExerelinUtils.getRandomInRange(0, possiblePlanetNamesList.size() - 1);
						name = (String)(possiblePlanetNamesList.get(planetNameIndex));
						possiblePlanetNamesList.remove(planetNameIndex);

			// Assign system name to planet as a prefix
			// no need anymore, intel screen already gives star name on everything
			//name = system.getStar().getName() + "-" + name;
			id = name.replace(' ','_');

			float radius;
			float angle = ExerelinUtils.getRandomInRange(1, 360);
			float distance = 3000 + (distanceStepping * (i  + 1)) + ExerelinUtils.getRandomInRange((distanceStepping/3)*-1, distanceStepping/3);
			float orbitDays = distance / 16 * ExerelinUtils.getRandomInRange(1, 3);
			if(planetType.equalsIgnoreCase("gas_giant") || planetType.equalsIgnoreCase("ice_giant"))
			{
				radius = ExerelinUtils.getRandomInRange(325, 375);
				//name = name + " Gaseous";
				gasPlanetCreated = true;
				inhabitable = false;
			}
			else
				radius = ExerelinUtils.getRandomInRange(150, 250);

			// At least one gas giant per system
			if(!gasPlanetCreated && i == numBasePlanets - 1)
			{
				planetType = (ExerelinUtils.getRandomInRange(0, 1) == 1) ? "gas_giant" : "ice_giant";
				radius = ExerelinUtils.getRandomInRange(325, 375);
				//name = name + " Gaseous";
				gasPlanetCreated = true;
				inhabitable = false;
			}

			SectorEntityToken newPlanet = system.addPlanet(id, star, name, planetType, angle, radius, distance, orbitDays);

			// 50% Chance to build moons around planet
			if(ExerelinUtils.getRandomInRange(0, 1) == 1)
			{
				// Build moons
				for(int j = 0; j < ExerelinUtils.getRandomInRange(0, ExerelinSetupData.getInstance().maxMoonsPerPlanet - 1); j = j + 1)
				{
					String ext = "";
					if(j == 0)
						ext = "I";
					if(j == 1)
						ext = "II";
					if(j == 2)
						ext = "III";

					String moonType = possibleMoonTypes[ExerelinUtils.getRandomInRange(0, possibleMoonTypes.length - 1)];
					angle = ExerelinUtils.getRandomInRange(1, 360);
					distance = ExerelinUtils.getRandomInRange(650, 1300);
					float moonRadius = ExerelinUtils.getRandomInRange(50, 100);
					orbitDays = distance / 16 * ExerelinUtils.getRandomInRange(1, 3);
					system.addPlanet(name + " " + ext, newPlanet, name + " " + ext, moonType, angle, moonRadius, distance, orbitDays);
				}
			}

			// 20% chance of rings around planet / 50% chance if a gas giant
			float ringChance = (planetType.equalsIgnoreCase("gas_giant") || planetType.equalsIgnoreCase("ice_giant")) ? 0.5f : 0.2f;
			if(Math.random() < ringChance)
			{
				int ringType = ExerelinUtils.getRandomInRange(0,3);

				if(ringType == 0)
				{
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 2, Color.white, 256f, radius*2, 40f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 2, Color.white, 256f, radius*2, 60f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 2, Color.white, 256f, radius*2, 80f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 2, Color.white, 256f, (int)(radius*2.5), 80f);
				}
				else if (ringType == 1)
				{
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 2, Color.white, 256f, radius*3, 70f);
					//system.addRingBand(newPlanet, "misc", "rings1", 256f, 3, Color.white, 256f, (int)(radius*2.5), 90f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 3, Color.white, 256f, (int)(radius*3.5), 110f);
				}
				else if (ringType == 2)
				{
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 3, Color.white, 256f, radius*3, 70f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 3, Color.white, 256f, (int)(radius*3), 90f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 3, Color.white, 256f, (int)(radius*3), 110f);
				}
				else if (ringType == 3)
				{
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 0, Color.white, 256f, radius*2, 50f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 0, Color.white, 256f, radius*2, 70f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 0, Color.white, 256f, radius*2, 80f);
					system.addRingBand(newPlanet, "misc", "rings1", 256f, 1, Color.white, 256f, (int)(radius*2.5), 90f);
				}
			}

			// Add market to inhabitable planets
			if(inhabitable)
			{			
				String owningFactionId = getRandomFaction();
				newPlanet.setFaction(owningFactionId);
				addMarketToEntity(i, newPlanet, owningFactionId, planetType, false);
			}
		}


		// Build asteroid belts
		List planets = system.getPlanets();
		int numAsteroidBelts;
		if(ExerelinSetupData.getInstance().numSystems != 1)
			numAsteroidBelts = ExerelinUtils.getRandomInRange(ExerelinConfig.minimumAsteroidBelts, ExerelinSetupData.getInstance().maxAsteroidBelts);
		else
			numAsteroidBelts = ExerelinSetupData.getInstance().maxAsteroidBelts;

		for(int j = 0; j < numAsteroidBelts; j = j + 1)
		{
			PlanetAPI planet = null;
			while(planet == null)
				planet = (PlanetAPI)planets.get(ExerelinUtils.getRandomInRange(0, planets.size() - 1));

			float orbitRadius;
			int numAsteroids;

			if (planet.getFullName().contains(" I") || planet.getFullName().contains(" II") || planet.getFullName().contains(" III"))
			{
				orbitRadius = ExerelinUtils.getRandomInRange(250, 350);
				numAsteroids = 2;
			}
			else if(planet.isGasGiant())
			{
				orbitRadius = ExerelinUtils.getRandomInRange(700, 900);
				numAsteroids = 10;
			}
			else if (planet.isStar())
			{
				orbitRadius = ExerelinUtils.getRandomInRange(1000, 8000);
				numAsteroids = 50;
			}
			else
			{
				orbitRadius = ExerelinUtils.getRandomInRange(400, 550);
				numAsteroids = 6;
			}


			float width = ExerelinUtils.getRandomInRange(10, 50);
			float minOrbitDays = ExerelinUtils.getRandomInRange(240, 360);
			float maxOrbitDays = ExerelinUtils.getRandomInRange(360, 480);
			system.addAsteroidBelt(planet, numAsteroids, orbitRadius, width, minOrbitDays, maxOrbitDays);
		}

		// Always put an asteroid belt around the sun
		system.addAsteroidBelt(star, 25, ExerelinUtils.getRandomInRange(1000, 8000), ExerelinUtils.getRandomInRange(10, 50), ExerelinUtils.getRandomInRange(240, 360), ExerelinUtils.getRandomInRange(360, 480));

		// Another one if medium system size
		if(ExerelinSetupData.getInstance().maxSystemSize > 16000)
			system.addAsteroidBelt(star, 50, ExerelinUtils.getRandomInRange(12000, 25000), ExerelinUtils.getRandomInRange(50, 100), ExerelinUtils.getRandomInRange(480, 720), ExerelinUtils.getRandomInRange(720, 960));

		// And another one if a large system
		if(ExerelinSetupData.getInstance().maxSystemSize > 32000)
			system.addAsteroidBelt(star, 75, ExerelinUtils.getRandomInRange(25000, 35000), ExerelinUtils.getRandomInRange(100, 150), ExerelinUtils.getRandomInRange(960, 1440), ExerelinUtils.getRandomInRange(1440, 1920));

		// Build stations
		int numStation;
		if(ExerelinSetupData.getInstance().numSystems != 1)
			numStation = ExerelinUtils.getRandomInRange(ExerelinConfig.minimumStations, Math.min(ExerelinSetupData.getInstance().maxStations, numBasePlanets*2));
		else
			numStation = ExerelinSetupData.getInstance().maxStations;
		int currentPlanet = 0;
		int k = 0;
		while(k < numStation)
		{
			PlanetAPI planet = (PlanetAPI)planets.get(currentPlanet);
			currentPlanet = currentPlanet + 1;

			if(currentPlanet == planets.size())
			{
				currentPlanet = 0;
			}

			if(planet.isStar())
			{
				//continue; // Skip sun
				if (Math.random() < 0.67)
					continue;	// one-third chance of deciding to let our station orbit the sun independently without a planet
			}
				
			boolean isGasGiant = planet.isGasGiant();
			MarketAPI existingMarket = planet.getMarket();
			
			if (existingMarket == null)
			{
				if (Math.random() < 0.2)
					continue; // 20% chance to skip uninhabited planet
			}
			else
			{
				if (Math.random() < 0.35)
					continue; // 35% chance to skip an inhabited planet
			}

			Boolean nameOK = false;
			String name = "";
			while(!nameOK)
			{
				name = possibleStationNames[ExerelinUtils.getRandomInRange(0, possibleStationNames.length - 1)];
				nameOK = true;
				for(int l = 0; l < system.getOrbitalStations().size(); l++)
				{
					String possibleName = planet.getFullName() + " " + name;
					if(((SectorEntityToken)system.getOrbitalStations().get(l)).getFullName().contains(possibleName))
						nameOK = false;
				}
			}

			int angle = ExerelinUtils.getRandomInRange(1, 360);
			int orbitRadius = 300;
			if (planet.getFullName().contains(" I") || planet.getFullName().contains(" II") || planet.getFullName().contains(" III"))
				orbitRadius = 200;
			else if (isGasGiant)
				orbitRadius = 500;
			else if (planet.isStar())
				orbitRadius = ExerelinUtils.getRandomInRange(1000, 16000);
			int orbitDays = orbitRadius / 25;	// ExerelinUtils.getRandomInRange(50, 100);
				
			String owningFactionId = getRandomFaction();
			FactionAPI planetFaction = planet.getFaction();
			if (planetFaction != null && !planetFaction.getId().equalsIgnoreCase("neutral"))
				owningFactionId = planetFaction.getId();
			name = planet.getFullName() + " " + name;
			String id = name.replace(' ','_');
			String image = possibleStationImages[ExerelinUtils.getRandomInRange(0, possibleStationImages.length - 1)];
			
			if (existingMarket == null)	// de novo station, probably orbiting an uninhabitable planet
			{
				SectorEntityToken newStation = system.addCustomEntity(id, name, image, owningFactionId);
				newStation.setCircularOrbitPointingDown(planet, angle, orbitRadius, orbitDays);
				addMarketToEntity(-1, newStation, owningFactionId, "", true);
			}
			else	// append a station to an existing inhabited planet
			{
				// these are smaller on the system map than the other ones when zoomed out
				SectorEntityToken newStation = system.addCustomEntity(id, name, image, owningFactionId);
				newStation.setCircularOrbitPointingDown(planet, angle, orbitRadius, orbitDays);
				existingMarket.addCondition("orbital_station");
				existingMarket.addCondition("exerelin_recycling_plant");
				newStation.setMarket(existingMarket);
			}
			k = k + 1;
		}


		// Build hyperspace exits
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(system.getId() + "_jump", "Jump Point Alpha");
		OrbitAPI orbit = Global.getFactory().createCircularOrbit(system.createToken(0,0), 0f, 1200, 120);
		jumpPoint.setOrbit(orbit);

		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		system.autogenerateHyperspaceJumpPoints(true, true);

		// Build comm relay
		SectorEntityToken relay = system.addCustomEntity(system.getId() + "_relay", // unique id
				systemName + " Relay", // name - if null, defaultName from custom_entities.json will be used
				"comm_relay", // type of object, defined in custom_entities.json
				"neutral"); // faction
		relay.setCircularOrbit(star, 90, 1200, 180);
	}
}