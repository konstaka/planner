package planner;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.junit.Before;
import org.junit.Test;

public class AttackTest {

    private Attack att;

    private static final TargetVillage target = mock(TargetVillage.class);
    private static final AttackerVillage attacker = mock(AttackerVillage.class);
    private static final IntegerProperty waves = new SimpleIntegerProperty(4);
    private static final BooleanProperty real = new SimpleBooleanProperty(false);
    private static final BooleanProperty conq = new SimpleBooleanProperty(false);
    private static final int unitSpeed = 3;
    private static final String land = "2020-04-11 12:00:00";
    private static final DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime landingTime = LocalDateTime.parse(land, f);
    private static final IntegerProperty landingTimeShift = new SimpleIntegerProperty(0);
    private static final int serverSpeed = 1;
    private static final int serverSize = 200;

    @Before
    public void setUp() {
        this.att = new Attack(
                target,
                attacker,
                waves,
                real,
                conq,
                unitSpeed,
                landingTime,
                landingTimeShift,
                serverSpeed,
                serverSize
        );
    }

    @Test
    public void shortTT() {
        when(attacker.getXCoord()).thenReturn(0);
        when(attacker.getYCoord()).thenReturn(0);
        when(target.getXCoord()).thenReturn(0);
        when(target.getYCoord()).thenReturn(1);
        this.att.setUnitSpeed(19);
        when(attacker.getSpeed()).thenReturn(2.0);
        when(attacker.getTs()).thenReturn(new SimpleIntegerProperty(0));
        String send = "2020-04-11 11:58:25";
        assertEquals(95L, this.att.travelSeconds());
        assertEquals(LocalDateTime.parse(send, f), this.att.getSendingTime());
    }

    @Test
    public void shortTT2() {
        when(attacker.getXCoord()).thenReturn(-5);
        when(attacker.getYCoord()).thenReturn(4);
        when(target.getXCoord()).thenReturn(4);
        when(target.getYCoord()).thenReturn(-1);
        this.att.setUnitSpeed(19);
        when(attacker.getSpeed()).thenReturn(2.0);
        when(attacker.getTs()).thenReturn(new SimpleIntegerProperty(0));
        String send = "2020-04-11 11:43:45";
        assertEquals(975L, this.att.travelSeconds());
        assertEquals(LocalDateTime.parse(send, f), this.att.getSendingTime());
    }

    @Test
    public void longTT() {
        when(attacker.getXCoord()).thenReturn(-122);
        when(attacker.getYCoord()).thenReturn(4);
        when(target.getXCoord()).thenReturn(44);
        when(target.getYCoord()).thenReturn(66);
        this.att.setUnitSpeed(19);
        when(attacker.getSpeed()).thenReturn(2.0);
        when(attacker.getTs()).thenReturn(new SimpleIntegerProperty(0));
        String send = "2020-04-11 07:20:13";
        assertEquals(16787L, this.att.travelSeconds());
        assertEquals(LocalDateTime.parse(send, f), this.att.getSendingTime());
    }

    @Test
    public void longTTts() {
        when(attacker.getXCoord()).thenReturn(-122);
        when(attacker.getYCoord()).thenReturn(4);
        when(target.getXCoord()).thenReturn(44);
        when(target.getYCoord()).thenReturn(66);
        this.att.setUnitSpeed(19);
        when(attacker.getSpeed()).thenReturn(2.0);
        when(attacker.getTs()).thenReturn(new SimpleIntegerProperty(10));
        String send = "2020-04-11 10:05:41";
        assertEquals(6859L, this.att.travelSeconds());
        assertEquals(LocalDateTime.parse(send, f), this.att.getSendingTime());
    }

    @Test
    public void around1() {
        when(attacker.getXCoord()).thenReturn(-199);
        when(attacker.getYCoord()).thenReturn(-198);
        when(target.getXCoord()).thenReturn(197);
        when(target.getYCoord()).thenReturn(200);
        this.att.setUnitSpeed(3);
        when(attacker.getSpeed()).thenReturn(1.0);
        when(attacker.getTs()).thenReturn(new SimpleIntegerProperty(0));
        String send = "2020-04-11 10:03:23";
        assertEquals(LocalDateTime.parse(send, f), this.att.getSendingTime());
    }

    @Test
    public void around2() {
        when(attacker.getXCoord()).thenReturn(-199);
        when(attacker.getYCoord()).thenReturn(-198);
        when(target.getXCoord()).thenReturn(397);
        when(target.getYCoord()).thenReturn(300);
        this.att.setUnitSpeed(19);
        this.att.setServerSize(400);
        when(attacker.getSpeed()).thenReturn(1.0);
        when(attacker.getTs()).thenReturn(new SimpleIntegerProperty(3));
        String send = "2020-04-10 23:34:17";
        assertEquals(LocalDateTime.parse(send, f), this.att.getSendingTime());
    }
}
