package exerelin.campaign.events;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.MessagePriority;
import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import com.fs.starfarer.api.util.Misc;


public class FactionEliminatedEvent extends BaseEventPlugin {

	public static Logger log = Global.getLogger(FactionEliminatedEvent.class);
	private static final int DAYS_TO_KEEP = 180;
	
	private FactionAPI defeatedFaction;
	private FactionAPI victorFaction;
	private boolean playerDefeated;
	private boolean playerVictory;
	float age;
	private Map<String, Object> params;
		
	public boolean done;
		
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
		defeatedFaction = (FactionAPI)params.get("defeatedFaction");
		victorFaction = (FactionAPI)params.get("victorFaction");
		playerDefeated = (boolean)params.get("playerDefeated");
		playerVictory = (boolean)params.get("playerVictory");
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
	public void startEvent()
	{
		MessagePriority priority = MessagePriority.SECTOR;
		String stage = "normal";
		if (playerDefeated) stage = "player_defeat";
		else if (playerVictory) stage = "player_victory";
		Global.getSector().reportEventStage(this, stage, market.getPrimaryEntity(), priority);
	}

	@Override
	public String getEventName() {
		return (defeatedFaction.getDisplayName() + " driven from Exerelin");
	}
	
	@Override
	public String getCurrentImage() {
		return defeatedFaction.getLogo();
	}

	@Override
	public String getCurrentMessageIcon() {
		return defeatedFaction.getLogo();
	}
		
	@Override
	public CampaignEventPlugin.CampaignEventCategory getEventCategory() {
		return CampaignEventPlugin.CampaignEventCategory.EVENT;
	}
		
	@Override
	public Map<String, String> getTokenReplacements() {
		Map<String, String> map = super.getTokenReplacements();
		String defeated = defeatedFaction.getEntityNamePrefix();
		String theDefeated = defeatedFaction.getDisplayNameWithArticle();
		String victor = victorFaction.getDisplayName();
		String theVictor = victorFaction.getDisplayNameWithArticle();
		map.put("$defeatedFaction", defeated);
		map.put("$theDefeatedFaction", theDefeated);
		map.put("$DefeatedFaction", Misc.ucFirst(defeated));
		map.put("$TheDefeatedFaction", Misc.ucFirst(theDefeated));
		map.put("$victorFaction", victor);
		map.put("$theVictorFaction", theVictor);
		map.put("$VictorFaction", Misc.ucFirst(victor));
		map.put("$TheVictorFaction", Misc.ucFirst(theVictor));
		return map;
	}
	
	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean allowMultipleOngoingForSameTarget() {
		return true;
	}
}