import java.util.List;

public class ElevatorSystem {
    private List<Elevator> elevators;

    public ElevatorSystem(List<Elevator> elevators) {
        this.elevators = elevators;
    }

    public void requestElevator(int floor, Direction direction) {
        Elevator bestElevator = findBestElevator(floor, direction);
        if (bestElevator != null) {
            bestElevator.requestFloor(floor);
        } else {
            System.out.println("No available elevators at the moment.");
        }
    }

    private Elevator findBestElevator(int floor, Direction direction) {
        Elevator bestElevator = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            int distance = Math.abs(elevator.getCurrentFloor() - floor);
            if (elevator.isIdle() || (elevator.getDirection() == direction && distance < minDistance)) {
                bestElevator = elevator;
                minDistance = distance;
            }
        }
        return bestElevator;
    }
}