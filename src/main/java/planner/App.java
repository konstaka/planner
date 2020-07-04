/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package planner;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class App extends Application {

    public static final DateTimeFormatter FULL_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final DateTimeFormatter DAY_AND_MONTH = DateTimeFormatter.ofPattern("dd.MM");

    public static String DB = "";

    private Stage stage;

    private MainController mainController;
    private Scene mainScene;

    private PlanSceneController planSceneController;
    private Scene planScene;

    private CommandController commandController;
    private Scene commandScene;


    @Override
    public void start(Stage stage) throws Exception {

        this.stage = stage;

        this.readyDb();
        this.downloadMapSql();

        this.initMainController();
        this.initPlanController();
        this.initCommandController();

        stage.setTitle("Planner 1.03-dev");
        stage.setScene(planScene);
        stage.show();

        planSceneController.loadOperation();
        planSceneController.updateCycle();
    }


    private void readyDb() {

        String userHome = System.getProperty("user.home");
        if (!userHome.endsWith("/")) {
            userHome += "/";
        }
        DB = "jdbc:sqlite:" + userHome + "planner.db";

        try {
            Connection conn = DriverManager.getConnection(App.DB);
            conn.prepareStatement("create table if not exists artefacts\n" +
                    "(\n" +
                    "    coordId int not null\n" +
                    "        constraint artefacts_pk\n" +
                    "            primary key,\n" +
                    "    small_arte int default 0,\n" +
                    "    large_arte int default 0,\n" +
                    "    unique_arte int default 0\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists attacker_info\n" +
                    "(\n" +
                    "    coordId int not null\n" +
                    "        constraint attacker_info_pk\n" +
                    "            primary key,\n" +
                    "    tsLvl int default 0 not null,\n" +
                    "    arteSpeed double default 1.0 not null,\n" +
                    "    heroBoots int default 0 not null,\n" +
                    "    unitSpeed int default 3 not null\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists attacks\n" +
                    "(\n" +
                    "    a_coordId int not null,\n" +
                    "    t_coordId int not null,\n" +
                    "    landing_time String not null,\n" +
                    "    waves int not null,\n" +
                    "    realTgt int not null,\n" +
                    "    conq int not null,\n" +
                    "    time_shift int not null,\n" +
                    "    unit_speed int not null,\n" +
                    "    server_speed int not null,\n" +
                    "    server_size int not null,\n" +
                    "    withHero int not null" +
                    ")").execute();
            conn.prepareStatement("create table if not exists operation_meta\n" +
                    "(\n" +
                    "    flex_seconds int,\n" +
                    "    defaultLandingTime String\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists participants\n" +
                    "(\n" +
                    "    id integer\n" +
                    "        constraint participants_pk\n" +
                    "            primary key autoincrement,\n" +
                    "    account String,\n" +
                    "    xCoord int not null,\n" +
                    "    yCoord int not null,\n" +
                    "    ts int not null,\n" +
                    "    speed double not null,\n" +
                    "    tribe int not null,\n" +
                    "    offstring String not null,\n" +
                    "    offsize int not null,\n" +
                    "    catas int not null,\n" +
                    "    chiefs int not null,\n" +
                    "    sendmin String,\n" +
                    "    sendmax String,\n" +
                    "    comment String\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists target_info\n" +
                    "(\n" +
                    "    coordId int not null\n" +
                    "        constraint target_info_pk\n" +
                    "            primary key,\n" +
                    "    randomShiftSeconds long not null\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists templates\n" +
                    "(\n" +
                    "    template1 String,\n" +
                    "    template2 String\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists updated\n" +
                    "(\n" +
                    "    last String not null\n" +
                    "        constraint updated_pk\n" +
                    "            primary key\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists village_data\n" +
                    "(\n" +
                    "    coordId int not null\n" +
                    "        constraint village_data_pk\n" +
                    "            primary key,\n" +
                    "    capital int default 0 not null,\n" +
                    "    offvillage int default 0 not null,\n" +
                    "    wwvillage int default 0\n" +
                    ")").execute();
            conn.prepareStatement("create table if not exists x_world\n" +
                    "(\n" +
                    "    coordId int not null\n" +
                    "        constraint x_world_pk\n" +
                    "            primary key,\n" +
                    "    xCoord int not null,\n" +
                    "    yCoord int not null,\n" +
                    "    tribe int not null,\n" +
                    "    villageId int not null,\n" +
                    "    villageName String not null,\n" +
                    "    playerId int not null,\n" +
                    "    playerName String not null,\n" +
                    "    allyId int not null,\n" +
                    "    allyName String not null,\n" +
                    "    population int not null\n" +
                    ")").execute();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void downloadMapSql() {

    }


    private void initMainController() throws IOException {
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent mainRoot = mainLoader.load();
        mainController = mainLoader.getController();
        mainScene = new Scene(mainRoot);
        mainScene.getStylesheets().add(getClass().getResource("main.css").toExternalForm());
        mainController.getToScene().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                mainController.getToScene().set("");
                this.switchTo(newValue);
            }
        });
        mainController.getNewOp().addListener((observable, oldValue, newValue) -> {
            if (observable.getValue()) {
                mainController.getNewOp().set(false);
                planSceneController.newOperation();
            }
        });
        mainController.getLoadOp().addListener((observable, oldValue, newValue) -> {
            if (observable.getValue()) {
                mainController.getLoadOp().set(false);
                planSceneController.loadOperation();
            }
        });
    }

    private void initPlanController() throws IOException {
        FXMLLoader planLoader = new FXMLLoader(getClass().getResource("plan.fxml"));
        Parent planRoot = planLoader.load();
        planSceneController = planLoader.getController();
        assert planRoot != null;
        planScene = new Scene(planRoot);
        planScene.getStylesheets().add(getClass().getResource("plan.css").toExternalForm());
        planSceneController.getToScene().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                planSceneController.getToScene().set("");
                this.switchTo(newValue);
            }
        });
    }

    private void initCommandController() throws IOException {
        FXMLLoader commandLoader = new FXMLLoader(getClass().getResource("commands.fxml"));
        Parent commandRoot = commandLoader.load();
        commandController = commandLoader.getController();
        commandScene = new Scene(commandRoot);
        commandScene.getStylesheets().add(getClass().getResource("commands.css").toExternalForm());
        commandController.getToScene().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                commandController.getToScene().set("");
                this.switchTo(newValue);
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }


    private void switchTo(String scene) {
        switch (scene) {
            case "updating":
                stage.setScene(mainScene);
                break;
            case "planning":
                stage.setScene(planScene);
                break;
            case "commands":
                if (planSceneController.getOperation() == null) {
                    break;
                }
                commandController.setAttackers(planSceneController.getOperation().getAttackers());
                commandController.updateCommands();
                stage.setScene(commandScene);
                break;
        }
        stage.show();
    }
}
