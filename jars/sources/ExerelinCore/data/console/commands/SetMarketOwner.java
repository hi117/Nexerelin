package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import exerelin.campaign.SectorManager;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SetMarketOwner implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // do me!
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }
        
		SectorAPI sector = Global.getSector();
        String[] tmp = args.split(" ");
        
        String targetName = tmp[0];
        List<SectorEntityToken> entitiesToSearch = sector.getEntitiesWithTag("planet");
        entitiesToSearch.addAll(sector.getEntitiesWithTag("station"));
		//Console.showMessage(entitiesToSearch.size() + " valid targets for search found");
        
        SectorEntityToken target = CommandUtils.findBestTokenMatch(targetName, entitiesToSearch);          
        if (target == null)
        {
            Console.showMessage("Error: could not find entity with name '" + targetName + "'!");
            return CommandResult.ERROR;
        }
        
        MarketAPI market = target.getMarket();
        if (market == null) 
        {
            Console.showMessage("Error: entity '" + target.getName() + "' does not have a market");
            return CommandResult.ERROR;
        }
        
        FactionAPI defenderFaction = market.getFaction();
        String defenderFactionId = defenderFaction.getId();
        String attackerFactionId = "player_npc";
        
        if (tmp.length < 2)
        {
            List<String> factions = SectorManager.getLiveFactionIdsCopy();
            WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
            for (String factionId : factions)
            {
                if (!factionId.equals(defenderFactionId))
                    picker.add(factionId);
            }
            attackerFactionId = picker.pick();
        }
        else
        {
            String factionStr = tmp[1];
            FactionAPI fac = CommandUtils.findBestFactionMatch(factionStr);

            if (fac == null)
            {
                Console.showMessage("Error: no such faction '" + factionStr + "'!");
                return CommandResult.ERROR;
            }
            attackerFactionId = fac.getId();
        }
        
        FactionAPI attackerFaction = Global.getSector().getFaction(attackerFactionId);
        
        SectorManager.captureMarket(market, attackerFaction, defenderFaction, false, null, 0);
        Console.showMessage("Transferred market " + market.getName() + " from " + defenderFaction.getDisplayName() + " to " + attackerFaction.getDisplayName());
        
        return CommandResult.SUCCESS;
    }
}
