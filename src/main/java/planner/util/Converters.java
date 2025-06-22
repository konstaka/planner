package planner.util;

import planner.entities.Attack;

public class Converters {


    /**
     * Computes the size of this off from submitted tribe and off size string.
     * @param tribe 1 = Roman, 2 = Teuton, 3 = Gaul
     * @param offString off size in the format (example) 1000+0+500+100+100
     * @return off consumption (romans computed without drinking trough)
     */
    public static int offSize(int tribe, String offString) {
        String[] offs = offString.split("\\+");
        int[] ints = new int[offs.length];
        for (int i = 0; i < offs.length; i++) {
            ints[i] = Integer.parseInt(offs[i].trim());
        }
        switch (tribe) {
            case 1: return ints[0] + 3*ints[1] + 4*ints[2];
            case 2: return ints[0] + ints[1] + 3*ints[2];
            case 3: return ints[0] + 2*ints[1] + 3*ints[2];
        }
        return 0;
    }


    /**
     * Converts tribe name to number.
     * @param tribe tribe
     * @return 1 = Roman, 2 = Teuton, 3 = Gaul, -1 = Unknown
     */
    public static int tribeNumber(String tribe) {
        switch (tribe) {
            case "Roman": return 1;
            case "Teuton": return 2;
            case "Gaul": return 3;
        }
        return -1;
    }


    /**
     * Creates string representations for different attack types.
     * @param attack the attack to be interpreted
     * @return attack type string, for example "Fake cata"
     */
    public static String attackType(Attack attack) {
        String ret = "";
        switch (attack.getUnitSpeed()) {
            case 5:
                ret += attack.isReal() ? "Conquer" : "Fake conquer";
                break;
            case 4:
                if (attack.isConq()) {
                    ret += attack.isReal() ? "Conquer" : "Fake conquer";
                } else {
                    ret += attack.isReal() ? "Ram" : "Fake ram";
                }
                break;
            case 3:
                if (attack.isConq()) {
                    ret += attack.isReal() ? "Conquer" : "Fake conquer";
                } else {
                    ret += attack.isReal() ? "Cata" : "Fake cata";
                }
                break;
            default:
                ret += attack.isReal() ? "Sweep" : "Fake sweep";
                break;
        }
        if (attack.isReal() && !attack.isWithHero()) ret += " NO HERO";
        if (!attack.isReal() && attack.isWithHero()) ret += " HEROFAKE";
        return ret;
    }


    /**
     * Tribe ID to tribe converter.
     * @param tribeNumber 1 = Roman, 2 = Teuton, 3 = Gaul
     * @return 1 = Roman, 2 = Teuton, 3 = Gaul
     */
    public static String toTribe(int tribeNumber) {
        switch (tribeNumber) {
            case 1: return "Roman";
            case 2: return "Teuton";
            case 3: return "Gaul";
        }
        return "Unknown";
    }


    /**
     * Converts artefact size and type to a string representation.
     * @param size 0, 1, 2
     * @param type 1, 2, 4, 5, 6, 8, 9, 10, 11
     * @return artefact string
     */
    public static String interpretArte(int size, int type) {
        String arte = "";
        switch (size) {
            case 0:
                arte = "Small ";
                break;
            case 1:
                arte = "Large ";
                break;
            case 2:
                arte = "Unique ";
                break;
            default:
                return "Invalid Artefact";
        }
        switch (type) {
            case 1: return "Buildplan";
            case 2: return arte + "Architect";
            case 4: return arte + "Boots";
            case 5: return arte + "Eyes";
            case 6: return arte + "Diet";
            case 8: return arte + "Trainer";
            case 9: return arte + "Storage";
            case 10: return arte + "Confuser";
            case 11: return arte + "Fool";
            default:
                return "Invalid Artefact";
        }
    }
}
