package exerelin.utilities.versionchecker;

import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class RecheckVersions implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
		try
		{
			VCModPluginCustom.onApplicationLoad();

			// TODO: Replace with context.isCampaignAccessible() after next console release
			if (context.isInCampaign() || (context.isInCombat()
					&& (Global.getCombatEngine().isInCampaign()
					|| Global.getCombatEngine().isInCampaignSim())))
			{
				Global.getSector().removeScriptsOfClass(UpdateNotificationScript.class);
				VCModPluginCustom.onGameLoad(false);
			}

			Console.showMessage("Update check started successfully.");
			return CommandResult.SUCCESS;
		}
		catch (Exception ex)
		{
			Console.showException("Something went wrong!", ex);
			return CommandResult.ERROR;
		}
    }
}
