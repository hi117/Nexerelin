package exerelin.campaign.events;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseOnMessageDeliveryScript;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.DiplomacyManager.DiplomacyEventDef;


public class DiplomacyEvent extends BaseEventPlugin {

	public static Logger log = Global.getLogger(DiplomacyEvent.class);
	protected static final int DAYS_TO_KEEP = 30;
	
	protected FactionAPI otherFaction;
	protected DiplomacyEventDef event;
	protected float delta;
	protected float age;
	protected Map<String, Object> params;
		
	protected boolean done;
		
	@Override
	public void init(String type, CampaignEventTarget eventTarget) {
		super.init(type, eventTarget);
		params = new HashMap<>();
		done = false;
		age = 0;
	}
	
	@Override
	public void setParam(Object param) {
		params = (HashMap)param;
		otherFaction = (FactionAPI)params.get("otherFaction");
		delta = (Float)params.get("delta");
		event = (DiplomacyEventDef)params.get("event");
	}
		
	@Override
	public void advance(float amount)
	{
		if (done)
		{
			return;
		}
		age = age + Global.getSector().getClock().convertToDays(amount);
		if (age > DAYS_TO_KEEP)
		{
			done = true;
			return;
		}
	}
	
	@Override
	public void startEvent() {
		// we can set the reputation change only on message delivery
		// but problem is, the token replacement method needs to know the relationship change NOW
		//DiplomacyManager.adjustRelations(event, market, market.getFaction(), otherFaction, delta);
		MessagePriority priority = MessagePriority.DELIVER_IMMEDIATELY;
		Global.getSector().reportEventStage(this, event.stage, market.getPrimaryEntity(), priority, new BaseOnMessageDeliveryScript() {
			final DiplomacyEventDef thisEvent = event;
			final float thisDelta = delta;
			final MarketAPI thisMarket = market;
			final FactionAPI fac = market.getFaction();
			final FactionAPI otherFac = otherFaction;

			public void beforeDelivery(CommMessageAPI message) {
			//DiplomacyManager.adjustRelations(thisEvent, thisMarket, fac, otherFac, thisDelta);
			}
		});
		log.info("Diplomacy event: " + event.stage);
	}

	@Override
	public String getEventName() {
		return (faction.getEntityNamePrefix() + " - " + otherFaction.getEntityNamePrefix() + " diplomatic event");
	}
	
	/*
	@Override
	public String getCurrentImage() {
		return newOwner.getLogo();
	}

	@Override
	public String getCurrentMessageIcon() {
		return newOwner.getLogo();
	}
	*/
		
	@Override
	public CampaignEventPlugin.CampaignEventCategory getEventCategory() {
		return CampaignEventPlugin.CampaignEventCategory.DO_NOT_SHOW_IN_MESSAGE_FILTER;
	}
	
	protected String getNewRelationStr()
	{
		RepLevel level = faction.getRelationshipLevel(otherFaction.getId());
		int repInt = (int) Math.ceil((faction.getRelationship(otherFaction.getId())) * 100f);
		
		String standing = "" + repInt + "/100" + " (" + level.getDisplayName().toLowerCase() + ")";
		return standing;
	}
	
	@Override
	public Map<String, String> getTokenReplacements() {
		Map<String, String> map = super.getTokenReplacements();
		String otherFactionStr = otherFaction.getEntityNamePrefix();
		String theOtherFactionStr = otherFaction.getDisplayNameWithArticle();
		map.put("$otherFaction", otherFactionStr);
		map.put("$theOtherFaction", theOtherFactionStr);
		map.put("$OtherFaction", Misc.ucFirst(otherFactionStr));
		map.put("$TheOtherFaction", Misc.ucFirst(theOtherFactionStr));
		map.put("$deltaAbs", "" + (int)Math.ceil(Math.abs(delta*100f)));
		map.put("$newRelationStr", getNewRelationStr());
		return map;
	}
	
	@Override
	public String[] getHighlights(String stageId) {
		List<String> result = new ArrayList<>();
		addTokensToList(result, "$deltaAbs");
		addTokensToList(result, "$newRelationStr");
		return result.toArray(new String[0]);
	}
	
	@Override
	public Color[] getHighlightColors(String stageId) {
		Color colorDelta = delta > 0 ? Global.getSettings().getColor("textFriendColor") : Global.getSettings().getColor("textEnemyColor");
		Color colorNew = faction.getRelColor(otherFaction.getId());
		return new Color[] {colorDelta, colorNew};
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean allowMultipleOngoingForSameTarget() {
		return true;
	}
	
	@Override
	public boolean showAllMessagesIfOngoing() {
		return false;
	}
}