package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import exerelin.campaign.ExerelinSetupData;
import exerelin.utilities.ExerelinConfig;
import exerelin.utilities.ExerelinUtils;


public class NGCGetExerelinDefaults extends BaseCommandPlugin {
	 
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		//ExerelinSetupData.resetInstance();
		ExerelinSetupData.getInstance().resetAvailableFactions();
		ExerelinSetupData setupData = ExerelinSetupData.getInstance();
		MemoryAPI map = memoryMap.get(MemKeys.LOCAL);
		map.set("$numSystems", setupData.numSystems, 0);
		map.set("$numSystemsEmpty", setupData.numSystemsEmpty, 0);
		map.set("$maxPlanets", setupData.maxPlanets, 0);
		map.set("$maxStations", setupData.maxStations, 0);
		map.set("$randomStartRelationships", setupData.randomStartRelationships, 0);
		map.set("$omniFacPresent", setupData.omnifactoryPresent, 0);
		map.set("$randomOmnifactoryLocation", setupData.randomOmnifactoryLocation, 0);
		map.set("$primsMarketPreset", setupData.prismMarketPresent, 0);
		map.set("$respawnFactions", setupData.respawnFactions, 0);
		map.set("$onlyRespawnStartingFactions", setupData.onlyRespawnStartingFactions, 0);
		map.set("$numStartFactions", "all");
		
		map.set("$corvusMode", setupData.corvusMode, 0);
		map.set("$hardMode", setupData.hardMode, 0);
		map.set("$haveSSP", ExerelinUtils.isSSPInstalled(), 0);
		return true;
	}
}






