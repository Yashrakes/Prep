import java.util.PriorityQueue;

class Elevator {
    private int id;
    private int currentFloor;
    private Direction direction;
    private Status status;
    private PriorityQueue<Integer> requests;

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.status = Status.IDLE;
        this.requests = new PriorityQueue<>();
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isIdle() {
        return requests.isEmpty();
    }

    public void requestFloor(int floor) {
        requests.add(floor);
        processRequests();
    }

    private void processRequests() {
        if (requests.isEmpty()) {
            direction = Direction.IDLE;
            status = Status.IDLE;
            return;
        }

        int nextFloor = requests.poll();
        moveToFloor(nextFloor);
    }

    private void moveToFloor(int floor) {
        System.out.println("Elevator " + id + " moving from floor " + currentFloor + " to floor " + floor);
        currentFloor = floor;
        direction = (currentFloor < floor) ? Direction.UP : Direction.DOWN;
        status = Status.MOVING;

        // Simulating movement delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stopElevator();
    }

    private void stopElevator() {
        System.out.println("Elevator " + id + " stopped at floor " + currentFloor);
        status = Status.STOPPED;
        direction = Direction.IDLE;
        processRequests();
    }
}