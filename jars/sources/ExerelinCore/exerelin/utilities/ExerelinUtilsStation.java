package exerelin.utilities;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exerelin.SectorManager;
import exerelin.commandQueue.CommandRemoveCargo;

public class ExerelinUtilsStation
{
    private static int CREW_INCREMENT = 300;
    private static float FUEL_INCREMENT = 300;
    private static float SUPPLIES_INCREMENT = 600;
    private static int MARINES_INCREMENT = 150;

    public static ExerelinUtilsFleet.ExerelinFleetSize getSpawnFleetSizeForStation(SectorEntityToken station)
    {
        ExerelinFactionConfig factionConfig = ExerelinConfig.getExerelinFactionConfig(station.getFaction().getId());
        Double multiplier = factionConfig.baseFleetCostMultiplier; //TODO APPLY THIS

        int crew = station.getCargo().getCrew(CargoAPI.CrewXPLevel.REGULAR);
        float supplies = station.getCargo().getSupplies();
        float fuel = station.getCargo().getFuel();
        int marines = station.getCargo().getMarines();

        if(crew > CREW_INCREMENT * 4
                && fuel > FUEL_INCREMENT * 4
                && marines > MARINES_INCREMENT * 4
                && supplies > SUPPLIES_INCREMENT * 4)
            return ExerelinUtilsFleet.ExerelinFleetSize.EXTRA_LARGE;
        else if(crew > CREW_INCREMENT * 3
                && fuel > FUEL_INCREMENT * 3
                && marines > MARINES_INCREMENT * 3
                && supplies > SUPPLIES_INCREMENT * 3)
            return ExerelinUtilsFleet.ExerelinFleetSize.LARGE;
        else if(crew > CREW_INCREMENT * 2
                && fuel > FUEL_INCREMENT * 2
                && marines > MARINES_INCREMENT * 2
                && supplies > SUPPLIES_INCREMENT * 2)
            return ExerelinUtilsFleet.ExerelinFleetSize.MEDIUM;
        else if(crew > CREW_INCREMENT
                && fuel > FUEL_INCREMENT
                && marines > MARINES_INCREMENT
                && supplies > SUPPLIES_INCREMENT)
            return ExerelinUtilsFleet.ExerelinFleetSize.SMALL;
        else
            return null;
    }

    public static void removeResourcesFromStationForFleetSize(SectorEntityToken station, ExerelinUtilsFleet.ExerelinFleetSize fleetSize)
    {
        int crew = 0;
        float supplies = 0;
        float fuel = 0;
        int marines = 0;

        switch(fleetSize)
        {
            case SMALL:
                crew = CREW_INCREMENT;
                supplies = SUPPLIES_INCREMENT;
                fuel = FUEL_INCREMENT;
                marines = MARINES_INCREMENT;
                break;
            case MEDIUM:
                crew = CREW_INCREMENT * 2;
                supplies = SUPPLIES_INCREMENT * 2;
                fuel = FUEL_INCREMENT * 2;
                marines = MARINES_INCREMENT * 2;
                break;
            case LARGE:
                crew = CREW_INCREMENT * 3;
                supplies = SUPPLIES_INCREMENT * 3;
                fuel = FUEL_INCREMENT * 3;
                marines = MARINES_INCREMENT * 3;
                break;
            case EXTRA_LARGE:
                crew = CREW_INCREMENT * 4;
                supplies = SUPPLIES_INCREMENT * 4;
                fuel = FUEL_INCREMENT * 4;
                marines = MARINES_INCREMENT * 4;
                break;
        }

        SectorManager.getCurrentSectorManager().getCommandQueue().addCommandToQueue(new CommandRemoveCargo(station.getCargo(), "regular_crew", CargoAPI.CargoItemType.RESOURCES, crew));
        SectorManager.getCurrentSectorManager().getCommandQueue().addCommandToQueue(new CommandRemoveCargo(station.getCargo(), "fuel", CargoAPI.CargoItemType.RESOURCES, fuel));
        SectorManager.getCurrentSectorManager().getCommandQueue().addCommandToQueue(new CommandRemoveCargo(station.getCargo(), "supplies", CargoAPI.CargoItemType.RESOURCES, supplies));
        SectorManager.getCurrentSectorManager().getCommandQueue().addCommandToQueue(new CommandRemoveCargo(station.getCargo(), "marines", CargoAPI.CargoItemType.RESOURCES, marines));
    }
}
