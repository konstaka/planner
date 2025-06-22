package planner.entities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Builder;
import lombok.Getter;
import planner.App;
import planner.util.Converters;

/**
 * This class represents a whole planned operation in memory and offers load and save methods for long-term storage.
 */
public class Operation {

    @Getter
    LocalDateTime defaultLandingTime = LocalDateTime.of(
            LocalDate.now().plusDays(2),
            LocalTime.of(8, 0));

    @Getter
    IntegerProperty flexSeconds = new SimpleIntegerProperty(0);

    @Getter
    List<TargetVillage> targets = new ArrayList<>();

    @Getter
    List<AttackerVillage> attackers = new ArrayList<>();

    @Getter
    Map<Integer, Map<Integer, Attack>> attacks = new HashMap<>();

    @Getter
    Map<Integer, List<Attack>> attacksPerPlayer = new HashMap<>();

    @Getter
    Map<Integer, LocalDateTime> landTimes = new HashMap<>();

    @Getter
    Set<Integer> scoutAccounts = new HashSet<>();

    @Getter
    Set<Integer> foolAccounts = new HashSet<>();

    @Getter
    Set<Integer> confuserAccounts = new HashSet<>();

    @Getter
    Set<Integer> architectAccounts = new HashSet<>();


    /**
     * Creates a new, empty operation. Gets world data and participants from database.
     * TODO move database queries to the Database class.
     */
    @Builder
    public Operation() {

        this.loadVillageData();
        this.assembleArteEffects();
        this.assembleAttackers();
        this.computeLandingTimes(true);
        this.createAttacks();
    }


    /**
     * Load village data from DB to memory.
     * Join with cap/off/artefact/etc information.
     */
    private void loadVillageData() {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            String sql = "SELECT * FROM x_world " +
                    "LEFT JOIN village_data ON x_world.coordId=village_data.coordId " +
                    "LEFT JOIN artefacts on x_world.coordId = artefacts.coordId";
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            while (rs.next()) {
                TargetVillage t = new TargetVillage(rs.getInt("coordId"));
                if (rs.getInt("capital") == 1) t.setCapital(true);
                if (rs.getInt("offvillage") == 1) t.setOffvillage(true);
                if (rs.getInt("wwvillage") == 1) t.setWwvillage(true);
                int small = rs.getInt("small_arte");
                int large = rs.getInt("large_arte");
                int unique = rs.getInt("unique_arte");
                if (small != 0) {
                    t.setArtefact(Converters.interpretArte(0, small));
                }
                if (large != 0) {
                    t.setArtefact(Converters.interpretArte(1, large));
                }
                if (unique != 0) {
                    t.setArtefact(Converters.interpretArte(2, unique));
                }
                // Note down account-wide effects
                if (large == 5 || unique == 5) {
                    scoutAccounts.add(rs.getInt("playerId"));
                }
                if (small == 11 || unique == 11) {
                    foolAccounts.add(rs.getInt("playerId"));
                }
                if (large == 10 || unique == 10) {
                    confuserAccounts.add(rs.getInt("playerId"));
                }
                if (large == 2 || unique == 2) {
                    architectAccounts.add(rs.getInt("playerId"));
                }
                targets.add(t);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Checks the village artefact and possible account-wide effects.
     */
    private void assembleArteEffects() {
        for (TargetVillage t : targets) {
            if (t.getArtefact().contains("Eyes") || scoutAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Scout effect");
            }
            if (t.getArtefact().contains("Fool") || foolAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Fool effect");
            }
            if (t.getArtefact().contains("Confuser") || confuserAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Confuser effect");
            }
            if (t.getArtefact().contains("Architect") || architectAccounts.contains(t.getPlayerId())) {
                t.getArteEffects().add("Architect effect");
            }
        }
    }


    /**
     * Join participant info with world data.
     */
    private void assembleAttackers() {
        // Assemble attacking villages
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            ResultSet rs = conn.prepareStatement("SELECT * FROM participants").executeQuery();
            while (rs.next()) {
                for (Village v : targets) {
                    if (v.getXCoord() == rs.getInt("xCoord")
                            && v.getYCoord() == rs.getInt("yCoord")) {
                        AttackerVillage attacker = new AttackerVillage(v.getCoordId());
                        attacker.getTs().set(rs.getInt("ts"));
                        attacker.setSpeed(rs.getDouble("speed"));
                        attacker.setOffString(rs.getString("offstring"));
                        attacker.setOffSize(rs.getInt("offsize"));
                        attacker.setCatas(rs.getInt("catas"));
                        attacker.setChiefs(rs.getInt("chiefs"));
                        attacker.setSendMin(rs.getString("sendmin"));
                        attacker.setSendMax(rs.getString("sendmax"));
                        attacker.setComment(rs.getString("comment"));
                        attackers.add(attacker);
                        break;
                    }
                }
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Updates the default hitting time for every target based on default time and flex seconds.
     * @param randomise True if the times should be re-randomised
     */
    private void computeLandingTimes(boolean randomise) {
        for (TargetVillage target : targets) {
            long flexSec = target.getFlexSec();
            if (randomise) {
                // Randomise landing times:
                flexSec = Math.round(flexSeconds.get() * (Math.random() * 2 - 1));
            }
            target.setFlexSec(flexSec);
            LocalDateTime thisHit = defaultLandingTime.plusSeconds(flexSec);
            landTimes.put(target.getCoordId(), thisHit);
        }
    }


    /**
     * Creates a new, empty map of attacks for each participant.
     */
    private void createAttacks() {
        // Create the attack matrix
        for (AttackerVillage attacker : attackers) {
            Map<Integer, Attack> toAdd = new HashMap<>();
            for (TargetVillage target : targets) {
                // TODO make all magic numbers editable.
                // Currently: unit speed, server speed, server size
                // Waves needs to be 0 at this point to mark that the attack is not planned for now
                Attack a = new Attack(
                        target,
                        attacker,
                        new SimpleIntegerProperty(0),
                        new SimpleBooleanProperty(false),
                        new SimpleBooleanProperty(false),
                        3,
                        landTimes.get(target.getCoordId()),
                        new SimpleIntegerProperty(0),
                        1,
                        200);
                // TODO Somehow listen to changes that mark planned fake/real attacks.
                toAdd.put(target.getCoordId(), a);
            }
            attacks.put(attacker.getCoordId(), toAdd);
        }
    }


    /**
     * Updates the operation by pulling data from the controller.
     */
    public void update() {
        // TODO all kinds of stuff goes here.
        updateAlerts();
    }

    /**
     * Updates the alert status of all participants.
     */
    private void updateAlerts() {

        // Reset alerts, prune planned attacks
        for (AttackerVillage attackerVillage : attackers) {
            attackerVillage.getPlannedAttacks().removeIf(attack -> attack.getWaves().get() == 0);
            attackerVillage.getAlert().set(false);
        }

        // Alert if there are sending times too close to each other
        // TODO set fastest possible send interval individually per attacker
        // TODO highlight problematic rows and make their landing times individually adjustable
        // Clear attacks per player
        attacksPerPlayer = new HashMap<>();
        // Assemble attack lists player-wise
        for (AttackerVillage attackerVillage : attackers) {
            if (!attacksPerPlayer.containsKey(attackerVillage.getPlayerId())) {
                attacksPerPlayer.put(attackerVillage.getPlayerId(), new ArrayList<>());
            }
            attacksPerPlayer.get(attackerVillage.getPlayerId()).addAll(attackerVillage.getPlannedAttacks());
        }
        // Sort lists and see if there are too close sends
        for (List<Attack> attacksForPlayer : attacksPerPlayer.values()) {
            attacksForPlayer.sort(Comparator.comparing(Attack::getSendingTime));
            for (int i = 0; i < attacksForPlayer.size()-1; i++) {
                LocalDateTime send1 = attacksForPlayer.get(i).getSendingTime();
                LocalDateTime send2 = attacksForPlayer.get(i+1).getSendingTime();
                if (ChronoUnit.SECONDS.between(send1, send2) < 50) {
                    attacksForPlayer.get(i).getAttacker().getAlert().set(true);
                    attacksForPlayer.get(i+1).getAttacker().getAlert().set(true);
                }
            }
        }

        for (AttackerVillage a : attackers) {
            // Alert if two sending times are equal for the same target village (unlikely, but yeah)
            // This needs to look different
        }
    }




    /*
      TOOLS
     */


    /**
     * Loads last saved operation from the database.
     * @return Operation object
     */
    public static Operation load() {
        // TODO implementation
        return new Operation();
    }


    /**
     * Saves this operation to the database. Overwrites anything that was there already.
     * @return True if the save was successful
     */
    public boolean save() {
        // TODO implementation
        return false;
    }
}
