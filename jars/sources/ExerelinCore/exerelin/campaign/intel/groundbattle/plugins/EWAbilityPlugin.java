package exerelin.campaign.intel.groundbattle.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import exerelin.campaign.intel.groundbattle.GBDataManager;
import exerelin.campaign.intel.groundbattle.GBDataManager.AbilityDef;
import exerelin.campaign.intel.groundbattle.GroundBattleIntel;
import exerelin.campaign.intel.groundbattle.GroundBattleSide;
import exerelin.campaign.intel.groundbattle.GroundUnit;
import exerelin.campaign.intel.groundbattle.dialog.AbilityDialogPlugin;
import exerelin.utilities.NexUtilsGUI;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EWAbilityPlugin extends AbilityPlugin {
	
	public static final String MEMORY_KEY_ECM_CACHE = "$nex_ecmRating_cache";
	public static float BASE_ECM_REQ = 1.5f;	// at size 3
	public static float GROUND_DEF_EFFECT_MULT = 0.75f;
	public static int BASE_COST = 50;	// at size 3
	
	@Override
	public void activate(InteractionDialogAPI dialog, PersonAPI user) {
		int powerLevel = getPowerLevel();
		super.activate(dialog, user);
		for (GroundUnit unit : getIntel().getSide(!side.isAttacker()).getUnits()) {
			unit.reorganize(powerLevel);
		}
		EWPersistentEffectPlugin pers = new EWPersistentEffectPlugin();
		pers.init(getIntel(), id, side.isAttacker(), powerLevel);
		getIntel().addOtherPlugin(pers);
		getIntel().reapply();
		
		logActivation(user);
	}
	
	@Override
	public Pair<String, Map<String, Object>> getDisabledReason(PersonAPI user) {
		int powerLevel = getPowerLevel();
		if (powerLevel <= 0) {
			Map<String, Object> params = new HashMap<>();
			
			String id = "prerequisitesNotMet";
			String desc = GroundBattleIntel.getString("ability_ew_insufficientECM");
			params.put("desc", desc);
			return new Pair<>(id, params);
		}
		
		// supplies check
		if (user != null && user.getFleet() != null) {
			int cost = getSupplyCost();
			float have = user.getFleet().getCargo().getMaxCapacity() * 0.5f;
			if (user == Global.getSector().getPlayerPerson()) {
				have = user.getFleet().getCargo().getSupplies();
			}
			
			if (cost > have) {
				Map<String, Object> params = new HashMap<>();
			
				String id = "notEnoughSupplies";
				String desc = String.format(GroundBattleIntel.getString("ability_ew_insufficientSupplies"), cost);
				params.put("desc", desc);
				return new Pair<>(id, params);
			}
		}
				
		Pair<String, Map<String, Object>> reason = super.getDisabledReason(user);
		return reason;
	}
	
	@Override
	public void dialogAddIntro(InteractionDialogAPI dialog) {
		dialog.getTextPanel().addPara(GroundBattleIntel.getString("ability_ew_blurb"));
		TooltipMakerAPI tooltip = dialog.getTextPanel().beginTooltip();
		generateTooltip(tooltip);
		dialog.getTextPanel().addTooltip();
		int cost = getSupplyCost();
		boolean canAfford = dialog.getTextPanel().addCostPanel(null,
					Commodities.SUPPLIES, cost, true);
		
		if (!canAfford) {
			dialog.getOptionPanel().setEnabled(AbilityDialogPlugin.OptionId.ACTIVATE, false);
		}
		
		addCooldownDialogText(dialog);
	}

	@Override
	public void generateTooltip(TooltipMakerAPI tooltip) {
		float opad = 10;
		Color h = Misc.getHighlightColor();
		tooltip.addPara(GroundBattleIntel.getString("ability_ew_tooltip1"), 0,
				h, "" + 1, String.format("%.2f×", GROUND_DEF_EFFECT_MULT));
		float needed = getNeededECMLevel();
		tooltip.addPara(GroundBattleIntel.getString("ability_ew_tooltip2"), opad,
				h, Math.round(needed) + "%", Math.round(needed * 2) + "%");
		float curr = getECMLevel();
		Color col = h;
		if (curr >= needed * 2) {
			col = Misc.getPositiveHighlightColor();
		} else if (curr < needed) {
			col = Misc.getNegativeHighlightColor();
		}
		tooltip.addPara(GroundBattleIntel.getString("ability_ew_tooltip3"), opad,
				col, Math.round(curr) + "%");
	}
	
	@Override
	public float getAIUsePriority() {
		GroundBattleIntel intel = getIntel();
		if (intel.getTurnNum() <= 2) {
			if (getDeployedProportion(intel.getSide(!this.side.isAttacker())) < 0.4f)
				return 0;
		}
		return 10;
	}
	
	/**
	 * Gets the proportion of units on the specified side that have been deployed, by strength
	 * @param side
	 * @return [0-1]
	 */
	public float getDeployedProportion(GroundBattleSide side) {
		float deployed = 0;
		float total = 0;
		for (GroundUnit unit : side.getUnits()) {
			float str = unit.getBaseStrength();
			total += str;
			if (unit.getLocation() != null) deployed += str;
		}
		
		if (total == 0) return 0;
		return deployed/total;
	}
	
	public int getSupplyCost() {
		int marketSize = getIntel().getMarket().getSize();
		return (int)Math.round(BASE_COST * Math.pow(2, marketSize - 3));
	}
	
	public float getNeededECMLevel() {
		int marketSize = getIntel().getMarket().getSize();
		return (float)(BASE_ECM_REQ * (marketSize - 1));
	}
	
	public int getPowerLevel() {
		float level = getECMLevel(), needed = getNeededECMLevel();
		if (level >= 2 * needed) {
			return 2;
		}
		else if (level >= needed) {
			return 1;
		}
		return 0;
	}
	
	public float getECMLevel() {
		if (!side.isAttacker()) {
			PersonAPI leader = side.getCommander();
			if (leader != null && leader.getStats().getSkillLevel(Skills.ELECTRONIC_WARFARE) > 1) {
				return getNeededECMLevel() * 2f;
			}
		}
		
		// can get ECM from all fleets in range
		int level = 0;
		List<CampaignFleetAPI> fleets = getIntel().getSupportingFleets(side.isAttacker());
		for (CampaignFleetAPI fleet : fleets) {
			
			// cache results
			if (fleet.getMemoryWithoutUpdate().contains(MEMORY_KEY_ECM_CACHE)) {
				level += fleet.getMemoryWithoutUpdate().getFloat(MEMORY_KEY_ECM_CACHE);
				continue;
			}
			
			int fleetLevel = 0;
			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				fleetLevel += member.getStats().getDynamic().getValue(Stats.ELECTRONIC_WARFARE_FLAT, 0);
			}
			level += fleetLevel;
			fleet.getMemoryWithoutUpdate().set(MEMORY_KEY_ECM_CACHE, fleetLevel, 0);
		}
		return level;
	}
	
	public static class EWPersistentEffectPlugin extends BaseGroundBattlePlugin {
		
		protected String abilityId;
		protected boolean isAttacker;
		protected int timeRemaining;
		
		public void init(GroundBattleIntel intel, String abilityId, boolean isAttacker, int timeRemaining) 
		{
			super.init(intel);
			this.abilityId = abilityId;
			this.isAttacker = isAttacker;
			this.timeRemaining = timeRemaining;
		}
		
		@Override
		public void apply() {
			GroundBattleSide side = intel.getSide(isAttacker);
			GroundBattleIntel.applyTagWithReason(side.getData(), "ability_ew_active", abilityId);
		}
		
		@Override
		public void unapply() {
			GroundBattleSide side = intel.getSide(isAttacker);
			GroundBattleIntel.unapplyTagWithReason(side.getData(), "ability_ew_active", abilityId);
		}
		
		@Override
		public void afterTurnResolve(int turn) {
			timeRemaining--;
			super.afterTurnResolve(turn);
		}
		
		@Override
		public boolean isDone() {
			return timeRemaining <= 0;
		}
		
		@Override
		public void addModifierEntry(TooltipMakerAPI info, CustomPanelAPI outer, 
				float width, float pad, Boolean isAttacker) {

			if (isAttacker == null || !isAttacker.equals(this.isAttacker)) return;
			
			AbilityDef ability = GBDataManager.getAbilityDef(abilityId);
			String icon = ability.icon;

			NexUtilsGUI.CustomPanelGenResult gen = NexUtilsGUI.addPanelWithFixedWidthImage(outer, 
					null, width, GroundBattlePlugin.MODIFIER_ENTRY_HEIGHT, ability.name, 
					width - GroundBattlePlugin.MODIFIER_ENTRY_HEIGHT - 8, 8, 
					icon, GroundBattlePlugin.MODIFIER_ENTRY_HEIGHT, 3, 
					ability.color, true, getModifierTooltip());

			info.addCustom(gen.panel, pad);
		}
		
		@Override
		public void processTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
			Color h = Misc.getHighlightColor();
			tooltip.addPara(GroundBattleIntel.getString("ability_ew_tooltip1"), 0,
					h, "" + 1, String.format("%.1f×", GROUND_DEF_EFFECT_MULT));
			tooltip.addPara(GroundBattleIntel.getString("ability_ew_tooltipTimeRemaining"), 3,
					h, timeRemaining + "");
		}
	}
}
