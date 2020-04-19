package planner.entities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

import javafx.beans.property.SimpleBooleanProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import planner.App;
import planner.util.Converters;

/**
 * This class represents a whole planned operation in memory.
 * Offers load and save methods for long-term storage.
 */
public class Operation {

    @Getter @Setter
    LocalDateTime defaultLandingTime = LocalDateTime.of(
            LocalDate.now().plusDays(2),
            LocalTime.of(8, 0));

    @Getter @Setter
    int randomShiftWindow = 0;

    @Getter
    List<TargetVillage> targets = new ArrayList<>();

    @Getter
    List<AttackerVillage> attackers = new ArrayList<>();

    @Getter
    Map<Integer, Map<Integer, Attack>> attacks = new HashMap<>();

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
        this.computeLandingTimes(false);
        this.createAttacks();
    }


    /**
     * Load village data from DB to memory.
     * Join with cap/off/artefact/etc information.
     * TODO move database queries to the Database class.
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
        for (TargetVillage target : targets) {
            if (target.getArtefact().contains("Eyes") || scoutAccounts.contains(target.getPlayerId())) {
                target.getArteEffects().add("Scout effect");
            }
            if (target.getArtefact().contains("Fool") || foolAccounts.contains(target.getPlayerId())) {
                target.getArteEffects().add("Fool effect");
            }
            if (target.getArtefact().contains("Confuser") || confuserAccounts.contains(target.getPlayerId())) {
                target.getArteEffects().add("Confuser effect");
            }
            if (target.getArtefact().contains("Architect") || architectAccounts.contains(target.getPlayerId())) {
                target.getArteEffects().add("Architect effect");
            }
        }
    }


    /**
     * Join participant info with world data.
     */
    private void assembleAttackers() {

        // Assemble attacking villages
        // TODO Notify somewhere if the village is not found, this could mean wrong coordinates or deleted account.
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            ResultSet rs = conn.prepareStatement("SELECT * FROM participants").executeQuery();
            while (rs.next()) {
                for (Village v : targets) {
                    if (v.getXCoord() == rs.getInt("xCoord")
                            && v.getYCoord() == rs.getInt("yCoord")) {
                        AttackerVillage attacker = new AttackerVillage(v.getCoordId());
                        attacker.setTs(rs.getInt("ts"));
                        attacker.setArteSpeed(rs.getDouble("speed"));
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
    public void computeLandingTimes(boolean randomise) {

        for (TargetVillage target : targets) {
            long randomShiftSeconds = target.getRandomShiftSeconds();
            if (randomise) {
                // Randomise landing times:
                randomShiftSeconds = Math.round(randomShiftWindow * (Math.random() * 2 - 1));
            }
            target.setRandomShiftSeconds(randomShiftSeconds);
            LocalDateTime thisHit = defaultLandingTime.plusSeconds(randomShiftSeconds);
            landTimes.put(target.getCoordId(), thisHit);
        }
        for (Map<Integer, Attack> attackMap : attacks.values()) {
            for (Attack attack : attackMap.values()) {
                attack.setLandingTime(landTimes.get(attack.getTarget().getCoordId()));
            }
        }
    }


    /**
     * Creates a new, empty map of attacks for each participant.
     */
    private void createAttacks() {
        // Create the attack matrix
        for (AttackerVillage attacker : attackers) {
            Map<Integer, Attack> attackerAttacks = new HashMap<>();
            for (TargetVillage target : targets) {
                // TODO make all magic numbers editable.
                // TODO: server speed, server size
                // Waves needs to be 0 at this point to mark that the attack is not planned for now
                Attack attack = new Attack(
                        target,
                        attacker,
                        0,
                        false,
                        false,
                        attacker.getUnitSpeed().get(),
                        landTimes.get(target.getCoordId()),
                        0,
                        1,
                        200,
                        false,
                        false,
                        new SimpleBooleanProperty(false));
                // Listen to updates in unit speeds
                attack.getAttacker().getUnitSpeed().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.equals(oldValue)) attack.setUnitSpeed(newValue.intValue());
                });
                attackerAttacks.put(target.getCoordId(), attack);
            }
            attacks.put(attacker.getCoordId(), attackerAttacks);
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
            attackerVillage.getPlannedAttacks().removeIf(attack -> attack.getWaves() == 0);
            attackerVillage.getPlannedAttacks().forEach(attack -> attack.setConflicting(false));
            attackerVillage.setAlert(false);
        }

        // Alert if there are sending times too close to each other
        // TODO set fastest possible send interval individually per player
        Map<Integer, List<Attack>> attacksPerPlayer = new HashMap<>();
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
                    attacksForPlayer.get(i).setConflicting(true);
                    attacksForPlayer.get(i+1).setConflicting(true);
                    attacksForPlayer.get(i).getAttacker().setAlert(true);
                    attacksForPlayer.get(i+1).getAttacker().setAlert(true);
                }
            }
        }

        // Alert if two sending times are equal for the same target village (unlikely, but yeah)
        Map<Integer, List<Attack>> attacksPerTarget = new HashMap<>();
        for (AttackerVillage attackerVillage : attackers) {
            for (Attack attack : attackerVillage.getPlannedAttacks()) {
                if (!attacksPerTarget.containsKey(attack.getTarget().getCoordId())) {
                    attacksPerTarget.put(attack.getTarget().getCoordId(), new ArrayList<>());
                }
                attacksPerTarget.get(attack.getTarget().getCoordId()).add(attack);
            }
        }
        for (List<Attack> attacksForTarget : attacksPerTarget.values()) {
            attacksForTarget.sort(Comparator.comparing(Attack::getSendingTime));
            for (int i = 0; i < attacksForTarget.size()-1; i++) {
                LocalDateTime send1 = attacksForTarget.get(i).getSendingTime();
                LocalDateTime send2 = attacksForTarget.get(i+1).getSendingTime();
                LocalDateTime land1 = attacksForTarget.get(i).getLandingTime();
                LocalDateTime land2 = attacksForTarget.get(i+1).getLandingTime();
                if (Math.abs(ChronoUnit.SECONDS.between(send1, send2)) < 1
                        && Math.abs(ChronoUnit.SECONDS.between(land1, land2)) < 1) {
                    attacksForTarget.get(i).setConflicting(true);
                    attacksForTarget.get(i+1).setConflicting(true);
                    attacksForTarget.get(i).getAttacker().setAlert(true);
                    attacksForTarget.get(i+1).getAttacker().setAlert(true);
                }
            }
        }
    }




    /*
      TOOLS
     */


    /**
     * Loads last saved operation from the database.
     * @return Operation object or null if there was a problem.
     * TODO move DB operations to the Database class
     */
    public static Operation load() {
        Operation operation = new Operation();
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            // Get landing time and flex seconds
            ResultSet rs1 = conn.prepareStatement("SELECT * FROM operation_meta").executeQuery();
            while (rs1.next()) {
                operation.defaultLandingTime = LocalDateTime.parse(
                        rs1.getString("defaultLandingTime"), App.FULL_DATE_TIME);
                operation.randomShiftWindow = rs1.getInt("flex_seconds");
            }
            // Get attacker info
            ResultSet rs2 = conn.prepareStatement("SELECT * FROM attacker_info").executeQuery();
            while (rs2.next()) {
                for (AttackerVillage attackerVillage : operation.getAttackers()) {
                    if (attackerVillage.getCoordId() == rs2.getInt("coordId")) {
                        attackerVillage.setTs(rs2.getInt("tsLvl"));
                        attackerVillage.setArteSpeed(rs2.getDouble("arteSpeed"));
                        attackerVillage.setHeroBoots(rs2.getInt("heroBoots"));
                        attackerVillage.getUnitSpeed().set(rs2.getInt("unitSpeed"));
                        break;
                    }
                }
            }
            // Get attack data. Setting landing time here is redundant.
            ResultSet rs3 = conn.prepareStatement("SELECT * FROM attacks").executeQuery();
            while (rs3.next()) {
                int a_coordId = rs3.getInt("a_coordId");
                int t_coordId = rs3.getInt("t_coordId");
                Attack attack = operation.getAttacks().get(a_coordId).get(t_coordId);
                attack.setWaves(rs3.getInt("waves"));
                attack.setReal(rs3.getInt("realTgt") == 1);
                attack.setConq(rs3.getInt("conq") == 1);
                attack.setWithHero(rs3.getInt("withHero") == 1);
                attack.setLandingTimeShift(rs3.getInt("time_shift"));
                attack.setUnitSpeed(rs3.getInt("unit_speed"));
                attack.setServerSpeed(rs3.getInt("server_speed"));
                attack.setServerSize(rs3.getInt("server_size"));
                if (attack.getWaves() > 0) {
                    for (AttackerVillage attackerVillage : operation.getAttackers()) {
                        if (attackerVillage.getCoordId() == a_coordId) {
                            attackerVillage.getPlannedAttacks().add(attack);
                        }
                    }
                }
            }
            // Get target specific landing time shifts
            Map<Integer, Long> landingTimeShifts = new HashMap<>();
            ResultSet rs4 = conn.prepareStatement("SELECT * FROM target_info").executeQuery();
            while (rs4.next()) {
                landingTimeShifts.put(
                        rs4.getInt("coordId"),
                        rs4.getLong("randomShiftSeconds"));
            }
            for (TargetVillage targetVillage : operation.getTargets()) {
                if (!landingTimeShifts.containsKey(targetVillage.getCoordId())) {
                    landingTimeShifts.put(targetVillage.getCoordId(), 0L);
                }
                targetVillage.setRandomShiftSeconds(landingTimeShifts.get(targetVillage.getCoordId()));
            }
            // Compute landing times for all attacks
            operation.computeLandingTimes(false);
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return operation;
    }


    /**
     * Saves this operation to the database. Overwrites anything that was there already.
     * @return True if the save was successful
     * TODO move DB operations to the Database class
     */
    public boolean save() {
        try {
            Connection conn = DriverManager.getConnection(App.DB);
            // Save metadata; landing time, flex seconds
            conn.prepareStatement("DELETE FROM operation_meta").execute();
            String sql = "INSERT INTO operation_meta VALUES ("
                    + randomShiftWindow + ",'"
                    + defaultLandingTime.format(App.FULL_DATE_TIME) + "')";
            conn.prepareStatement(sql).execute();
            // Save attack and attacker data
            conn.prepareStatement("DELETE FROM attacks").execute();
            conn.prepareStatement("DELETE FROM attacker_info").execute();
            String attackInsert = "INSERT INTO attacks VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement attackInserts = conn.prepareStatement(attackInsert);
            for (AttackerVillage attacker : attackers) {
                conn.prepareStatement("INSERT INTO attacker_info VALUES ("
                        + attacker.getCoordId() + ","
                        + attacker.getTs() + "," +
                        attacker.getArteSpeed() + "," +
                        attacker.getHeroBoots() + "," +
                        attacker.getUnitSpeed().get() + ")"
                ).execute();
                for (Attack attack : attacker.getPlannedAttacks()) {
                    int a_coordId = attack.getAttacker().getCoordId();
                    int t_coordId = attack.getTarget().getCoordId();
                    attackInserts.setInt(1, a_coordId);
                    attackInserts.setInt(2, t_coordId);
                    attackInserts.setString(3, landTimes.get(t_coordId).format(App.FULL_DATE_TIME));
                    attackInserts.setInt(4, attack.getWaves());
                    attackInserts.setInt(5, (attack.isReal() ? 1 : 0));
                    attackInserts.setInt(6, (attack.isConq() ? 1 : 0));
                    attackInserts.setInt(7, attack.getLandingTimeShift());
                    attackInserts.setInt(8, attack.getUnitSpeed());
                    attackInserts.setInt(9, attack.getServerSpeed());
                    attackInserts.setInt(10, attack.getServerSize());
                    attackInserts.setInt(11, (attack.isWithHero() ? 1 : 0));
                    attackInserts.addBatch();
                }
            }
            attackInserts.executeBatch();
            // Save target specific landing time shifts
            conn.prepareStatement("DELETE FROM target_info").execute();
            String targetInsert = "INSERT INTO target_info VALUES(?,?)";
            PreparedStatement targetInserts = conn.prepareStatement(targetInsert);
            for (TargetVillage targetVillage : targets) {
                targetInserts.setInt(1, targetVillage.getCoordId());
                targetInserts.setLong(2, targetVillage.getRandomShiftSeconds());
                targetInserts.addBatch();
            }
            targetInserts.executeBatch();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
