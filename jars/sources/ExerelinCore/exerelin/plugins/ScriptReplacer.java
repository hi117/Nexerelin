package exerelin.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.intel.GenericMissionManager;
import com.fs.starfarer.api.impl.campaign.intel.GenericMissionManager.GenericMissionCreator;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager.GenericBarEventCreator;
import static exerelin.plugins.ExerelinModPlugin.log;
import java.util.ArrayList;

public class ScriptReplacer {
	public static <T extends EveryFrameScript> boolean replaceScript(SectorAPI sector, Class toRemove, T toAdd)
    {
        boolean removedAny = false;
        for (EveryFrameScript script : sector.getScripts())
        {
            if (toRemove.isInstance(script))
            {
                if (toAdd != null && toAdd.getClass().isInstance(script))
                    continue;
                
                log.info("Removing EveryFrameScript " + script.toString() + " | " + toRemove.getCanonicalName());
                
                sector.removeScript(script);
                if (script instanceof CampaignEventListener)
                    sector.removeListener((CampaignEventListener)script);
                
                if (toAdd != null)
                    sector.addScript(toAdd);
                
                removedAny = true;
                break;
            }
        }
        return removedAny;
    }
	
	public static <T extends GenericMissionCreator> boolean replaceMissionCreator(Class toRemove, T toAdd) {
		boolean removedAny = false;
		GenericMissionManager manager = GenericMissionManager.getInstance();
		if (!manager.hasMissionCreator(toRemove))
			return false;
        for (GenericMissionCreator creator : new ArrayList<>(manager.getCreators()))
        {
            if (toRemove.isInstance(creator))
            {
                if (toAdd != null && toAdd.getClass().isInstance(creator))
                    continue;
                
                log.info("Removing mission creator " + creator.toString() + " | " + toRemove.getCanonicalName());
                
                manager.getCreators().remove(creator);
                
                if (toAdd != null)
                    manager.addMissionCreator(toAdd);
                
                removedAny = true;
                break;
            }
        }
        return removedAny;
	}
	
	public static <T extends GenericBarEventCreator> boolean replaceBarEventCreator(Class toRemove, T toAdd) {
		boolean removedAny = false;
		BarEventManager bar = BarEventManager.getInstance();
		if (!bar.hasEventCreator(toRemove))
			return false;
		for (GenericBarEventCreator creator : new ArrayList<>(bar.getCreators()))
        {
            if (toRemove.isInstance(creator))
            {
                if (toAdd != null && toAdd.getClass().isInstance(creator))
                    continue;
                
                log.info("Removing bar event creator " + creator.toString() + " | " + toRemove.getCanonicalName());
                
                bar.getCreators().remove(creator);
                
                if (toAdd != null)
                    bar.addEventCreator(toAdd);
                
                removedAny = true;
                break;
            }
        }
        return removedAny;
	}
}
