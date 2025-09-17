public class UserPanel {
    private Elevator elevator;

    public UserPanel(Elevator elevator) {
        this.elevator = elevator;
    }

    public void selectFloor(int floor) {
        System.out.println("User selected floor: " + floor);
        elevator.requestFloor(floor);
    }
}