package exerelin.campaign.ai;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.alliances.Alliance;
import exerelin.campaign.diplomacy.DiplomacyBrain;
import exerelin.campaign.diplomacy.DiplomacyTraits;
import exerelin.utilities.NexConfig;

public class SAIUtils {

    /**
     * If {@code wantPositive} is true, high disposition increases the action priority. If it is false, low disposition increases priority.
     * @param aiFactionId
     * @param wantPositive
     * @param stat
     */
    public static void applyPriorityModifierForDisposition(String aiFactionId, boolean wantPositive, MutableStat stat) {

        DiplomacyBrain brain = DiplomacyManager.getManager().getDiplomacyBrain(aiFactionId);
        if (brain.getDisposition(aiFactionId) != null) {
            float disposition = brain.getDisposition(aiFactionId).disposition.getModifiedValue();
            boolean isPositive;
            if (disposition <= DiplomacyBrain.DISLIKE_THRESHOLD) isPositive = false;
            else if (disposition >= DiplomacyBrain.LIKE_THRESHOLD) isPositive = true;
            else return;

            if (isPositive == wantPositive) {
                String desc = StrategicAI.getString(isPositive ? "statDispositionPositive" : "statDispositionNegative", true);
                float mult = isPositive ? SAIConstants.POSITIVE_DISPOSITION_MULT : SAIConstants.NEGATIVE_DISPOSITION_MULT;
                String source = isPositive ? "disposition_positive" : "disposition_negative";

                stat.modifyMult(source, mult, desc);
            }
        }
    }

    public static void applyPriorityModifierForAlignment(String aiFactionId, MutableStat stat, Alliance.Alignment alignment) {
        float alignValue = NexConfig.getFactionConfig(aiFactionId).getAlignments().get(alignment).getModifiedValue();
        //log.info("Align value: " + alignValue);
        stat.modifyMult("alignment_" + alignment.getName(), 1 + SAIConstants.MAX_ALIGNMENT_MODIFIER_FOR_PRIORITY * alignValue,
                StrategicAI.getString("statAlignment", true) + ": " + Misc.ucFirst(alignment.getName()));
    }

    public static void applyPriorityModifierForTrait(String aiFactionId, MutableStat stat, String trait, float mult, boolean force) {
        if (!DiplomacyTraits.hasTrait(aiFactionId, trait) && !force)
            return;

        DiplomacyTraits.TraitDef traitDef = DiplomacyTraits.getTrait(trait);
        stat.modifyMult("trait_" + trait, mult, StrategicAI.getString("statTrait", true) + ": " + traitDef.name);
    }

}