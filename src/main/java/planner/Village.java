package planner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.Getter;

public abstract class Village {

    @Getter
    protected int coordId;

    @Getter
    protected int xCoord;

    @Getter
    protected int yCoord;

    @Getter
    protected int tribe;

    @Getter
    protected int villageId;

    @Getter
    protected String villageName;

    @Getter
    protected int playerId;

    @Getter
    protected String playerName;

    @Getter
    protected int allyId;

    @Getter
    protected String allyName;

    @Getter
    protected int population;

    public Village(int coordId) {
        this.coordId = coordId;
        try {
            Connection conn = DriverManager.getConnection(App.getDB());
            String sql = "SELECT * FROM x_world WHERE coordId=" + coordId;
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            this.xCoord = rs.getInt("xCoord");
            this.yCoord = rs.getInt("yCoord");
            this.tribe = rs.getInt("tribe");
            this.villageId = rs.getInt("villageId");
            this.villageName = rs.getString("villageName");
            this.playerId = rs.getInt("playerId");
            this.playerName = rs.getString("playerName");
            this.allyId = rs.getInt("allyId");
            this.allyName = rs.getString("allyName");
            this.population = rs.getInt("population");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getCoords() {
        return this.xCoord + "|" + this.yCoord;
    }
}
