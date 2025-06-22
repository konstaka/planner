package planner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.Getter;

public abstract class Village {

    @Getter
    private int coordId;

    @Getter
    private int xCoord;

    @Getter
    private int yCoord;

    @Getter
    private int tribe;

    @Getter
    private int villageId;

    @Getter
    private String villageName;

    @Getter
    private int playerId;

    @Getter
    private String playerName;

    @Getter
    private int allyId;

    @Getter
    private String allyName;

    @Getter
    private int population;

    public Village(int coordId) {
        this.coordId = coordId;
        try {
            Connection conn = DriverManager.getConnection(App.DB);
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
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getCoords() {
        return this.xCoord + "|" + this.yCoord;
    }
}
