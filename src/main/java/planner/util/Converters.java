package planner.util;

import planner.entities.Attack;

public class Converters {


    /**
     * Creates string representations for different attack types.
     * @param attack the attack to be interpreted
     * @return attack type string, for example "Fake cata"
     */
    public static String attackType(Attack attack) {
        switch (attack.getUnitSpeed()) {
            case 5:
                return attack.isReal() ? "Conquer" : "Fake conquer";
            case 4:
                if (attack.isConq()) {
                    return attack.isReal() ? "Conquer" : "Fake conquer";
                } else {
                    return attack.isReal() ? "Ram" : "Fake ram";
                }
            case 3:
                if (attack.isConq()) {
                    return attack.isReal() ? "Conquer" : "Fake conquer";
                } else {
                    return attack.isReal() ? "Cata" : "Fake cata";
                }
            default:
                return attack.isReal() ? "Sweep" : "Fake sweep";
        }
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
