public class FloorPanel {
    private ElevatorSystem elevatorSystem;
    private int floor;

    public FloorPanel(ElevatorSystem elevatorSystem, int floor) {
        this.elevatorSystem = elevatorSystem;
        this.floor = floor;
    }

    public void pressUp() {
        System.out.println("Up button pressed on floor " + floor);
        elevatorSystem.requestElevator(floor, Direction.UP);
    }

    public void pressDown() {
        System.out.println("Down button pressed on floor " + floor);
        elevatorSystem.requestElevator(floor, Direction.DOWN);
    }
}
