package exerelin.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Nex_MarketCMD;
import data.scripts.crewReplacer_Job;
import data.scripts.crewReplacer_Main;
import exerelin.campaign.intel.groundbattle.GBConstants;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j;

/**
 * Handles interaction with the CrewReplacer mod.
 * https://github.com/Alaricdragon/Crew_Replacer
 */
@Log4j
public class CrewReplacerUtils {
	
	public static boolean enabled = Global.getSettings().getModManager().isModEnabled("aaacrew_replacer");
	public static boolean debugMode = false;

	static {
		if (enabled) {
			// register heavy arms "job"
			crewReplacer_Job jobHA = crewReplacer_Main.getJob(GBConstants.CREW_REPLACER_JOB_HEAVYARMS);
			jobHA.addNewCrew(Commodities.HAND_WEAPONS, 1, 1);
			
			crewReplacer_Job jobGBM = crewReplacer_Main.getJob(GBConstants.CREW_REPLACER_JOB_MARINES);
			jobGBM.addNewCrew(Commodities.MARINES, 1, 1);
			
			crewReplacer_Job jobTC = crewReplacer_Main.getJob(GBConstants.CREW_REPLACER_JOB_TANKCREW);
			jobTC.addNewCrew(Commodities.MARINES, 1, 1);

			if (debugMode) {
				jobHA.addNewCrew(Commodities.RARE_ORE, 0.2f, 1);
				//log.info(String.format("Commodity %s has power %s for job %s", Commodities.RARE_ORE, jobHA.getCrew(Commodities.RARE_ORE).crewPower, jobHA.name));
				jobGBM.addNewCrew(Commodities.ORE, 0.2f, 1);
				jobTC.addNewCrew(Commodities.ORE, 0.2f, 1);
				crewReplacer_Job jobMarines = crewReplacer_Main.getJob(Nex_MarketCMD.CREWREPLACER_JOB_RAID);
				jobMarines.addNewCrew(Commodities.ORE, 0.2f, 1);
				//jobMarines.organizePriority();
			}
		}
	}
	
	public static float getMarines(CampaignFleetAPI fleet, String jobId) {
		if (!enabled) return fleet.getCargo().getMarines();
		
		return crewReplacer_Main.getJob(jobId).getAvailableCrewPower(fleet);
	}

	public static float getHeavyArms(CampaignFleetAPI fleet, String jobId) {
		if (!enabled) return fleet.getCargo().getCommodityQuantity(Commodities.HAND_WEAPONS);

		return crewReplacer_Main.getJob(jobId).getAvailableCrewPower(fleet);
	}

	public static List<Integer> takeMarinesFromCargo(CampaignFleetAPI fleet, String jobId, int count) {
		if (!enabled) {
			List<Integer> list = new ArrayList<>();
			fleet.getCargo().removeMarines(count);
			list.add(count);
			return list;
		}
		return takeCommodityFromCargo(fleet, jobId, count);
	}

	public static List<Integer> takeHeavyArmsFromCargo(CampaignFleetAPI fleet, String jobId, int count) {
		if (!enabled) {
			List<Integer> list = new ArrayList<>();
			fleet.getCargo().removeCommodity(Commodities.HAND_WEAPONS, count);
			list.add(count);
			return list;
		}
		return takeCommodityFromCargo(fleet, jobId, count);
	}

	/**
	 * Gets a number of items from the fleet's cargo to do the specified job (actually removed from cargo).
	 * @param fleet
	 * @param jobId
	 * @param count
	 * @return A list of integers representing the counts of each taken crew type (empty if Crew Replacer is unavailable).
	 */
	// runcode exerelin.utilities.CrewReplacerUtils.takeCommodityFromCargo(Global.getSector().getPlayerFleet(), "raiding_marines", 10);
	public static List<Integer> takeCommodityFromCargo(CampaignFleetAPI fleet, String jobId, int count) {
		List<Integer> list = new ArrayList<>();
		if (!enabled) return list;
		
		crewReplacer_Job job = crewReplacer_Main.getJob(jobId);
		List<Float> crews = job.getCrewForJob(fleet, count); // crewPowerRequired is the amount of crew power you want crew replacer to get from an fleet.
		for (int index = 0; index < crews.size(); index++) {
			Float value = crews.get(index);
			int thisCount = (int)(float)value;
			if (thisCount <= 0) continue;
			list.add(thisCount);
			String commodityId = job.Crews.get(index).name;
			float power = job.Crews.get(index).crewPower;
			fleet.getCargo().removeCommodity(commodityId, thisCount);
			Global.getLogger(CrewReplacerUtils.class).info(String.format("  Removing %s of commodity %s for job %s, power %s", thisCount, commodityId, jobId, power));
		}
		return list;
	}
	
	public static void removeMarines(CampaignFleetAPI fleet, String jobId, int count, TextPanelAPI text) {
		if (!enabled) {
			fleet.getCargo().removeMarines(count);
			if (text != null) AddRemoveCommodity.addCommodityLossText(Commodities.MARINES, count, text);
			return;
		}
		crewReplacer_Job job = crewReplacer_Main.getJob(jobId);
		crewReplacer_Main.getJob(jobId).automaticlyGetDisplayAndApplyCrewLost(
				fleet, (int)job.getAvailableCrewPower(fleet), count, text);
	}

	public static void removeHeavyArms(CampaignFleetAPI fleet, String jobId, int count, TextPanelAPI text) {
		if (!enabled) {
			fleet.getCargo().removeCommodity(Commodities.HAND_WEAPONS, count);
			if (text != null) AddRemoveCommodity.addCommodityLossText(Commodities.HAND_WEAPONS, count, text);
			return;
		}
		crewReplacer_Job job = crewReplacer_Main.getJob(jobId);
		crewReplacer_Main.getJob(jobId).automaticlyGetDisplayAndApplyCrewLost(
				fleet, (int)job.getAvailableCrewPower(fleet), count, text);
	}

	public static float getCommodityPower(String jobId, String commodityId) {
		if (!enabled) return 1;
		
		crewReplacer_Job jobTemp = crewReplacer_Main.getJob("raiding_marines");
		float power = jobTemp.getCrew("fake_commodity_id").crewPower;
		
		
		crewReplacer_Job job = crewReplacer_Main.getJob(jobId);
		//log.info(String.format("Getting commodity %s power for job %s: %s", commodityId, jobId, job.getCrew(commodityId).crewPower));
		return job.getCrew(commodityId).crewPower;
	}

	public static String getCommodityIdForJob(String jobId, int index, String defaultId) {
		if (!enabled) return defaultId;

		crewReplacer_Job job = crewReplacer_Main.getJob(jobId);
		return job.Crews.get(index).name;
	}
}