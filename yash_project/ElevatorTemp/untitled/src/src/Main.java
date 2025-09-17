import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Elevator elevator1 = new Elevator(1);
        Elevator elevator2 = new Elevator(2);

        ElevatorSystem elevatorSystem = new ElevatorSystem(Arrays.asList(elevator1, elevator2));

        FloorPanel floorPanel = new FloorPanel(elevatorSystem, 3);
        floorPanel.pressUp(); // Request an elevator at floor 3

        UserPanel userPanel = new UserPanel(elevator1);
        userPanel.selectFloor(7); // Select destination inside elevator
    }
}
