# Functional requirements
1. multiple stores of vehicles
2. filter vehicles
3. provide search capability
4. 




# Objects
1. user
2. location
3. vehicle
4. store
5. reserve
6. Bill
7. payment

# Classes

VEHICLE -> id, number, company name, average
STATUS (ENUM) -> ACTIVE, INACTIVE
VEHICLETYPE (ENUM) -> CAR
STORE -> Location, storeId, List<Reservations>, `VehicleInventoryManagement`
VEHICLE INVENTORYMANAGMENT-> List<Vehicle>
LOCATION -> id, pincode, name,city ,country
RESERVATION -> id, User, vehicle, date, from to, location pickup, location drop, reservation type,                                        reservation status
RESERVATIONSTATUS (ENUM)-> SCHEDULED ,INPROGRESS,COMPLETED,CANCELLED
RESERVATIONTYPE (ENUM) -> HOURLY,DAILY
USER -> name,licensce
BILL -> reservation, amount,
VEHICLERENTALSYSTEM -> List<store>, List<User>





