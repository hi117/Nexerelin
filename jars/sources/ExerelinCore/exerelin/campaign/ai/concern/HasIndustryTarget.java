package exerelin.campaign.ai.concern;

import com.fs.starfarer.api.campaign.econ.Industry;

/**
 * Any action targetable for a {@code SabotageIndustryAction} must implement this interface.
 */
public interface HasIndustryTarget {

    Industry getTargetIndustry();

}