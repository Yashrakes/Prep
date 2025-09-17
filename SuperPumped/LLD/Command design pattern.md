
Absolutely. Let's go through a **concrete example** to clearly show how **not using the Command Pattern leads to inefficient and rigid code**.

---

## 🎯 Scenario: Remote Control for Multiple Devices

You want a `RemoteControl` to support:

- Turning **Light** on/off
    
- Turning **Fan** on/off
    
- Maybe later support **TV**, **AC**, etc.
    

---

## ❌ Without Command Pattern

### 🔧 Code (Tightly Coupled Approach):

java

CopyEdit

`class Light {     public void turnOn() { System.out.println("Light ON"); }     public void turnOff() { System.out.println("Light OFF"); } }  class Fan {     public void start() { System.out.println("Fan STARTED"); }     public void stop() { System.out.println("Fan STOPPED"); } }  class RemoteControl {     private Light light;     private Fan fan;      public RemoteControl(Light light, Fan fan) {         this.light = light;         this.fan = fan;     }      public void pressLightOn() {         light.turnOn();     }      public void pressLightOff() {         light.turnOff();     }      public void pressFanStart() {         fan.start();     }      public void pressFanStop() {         fan.stop();     } }`

### 👎 Problems:

1. **Every new device** → change `RemoteControl` class.
    
2. No ability to **pass behavior as an object** (e.g., to store, queue, undo).
    
3. Can't add **custom actions at runtime** without changing the class.
    

---

## ✅ With Command Pattern

Let’s fix all that:

### ✅ Step 1: Command Interface

java

CopyEdit

`interface Command {     void execute(); }`

---

### ✅ Step 2: Receivers

java

CopyEdit

`class Light {     public void turnOn() { System.out.println("Light ON"); }     public void turnOff() { System.out.println("Light OFF"); } }  class Fan {     public void start() { System.out.println("Fan STARTED"); }     public void stop() { System.out.println("Fan STOPPED"); } }`

---

### ✅ Step 3: Commands

java

CopyEdit

`class LightOnCommand implements Command {     private Light light;     public LightOnCommand(Light light) { this.light = light; }     public void execute() { light.turnOn(); } }  class FanStartCommand implements Command {     private Fan fan;     public FanStartCommand(Fan fan) { this.fan = fan; }     public void execute() { fan.start(); } }`

---

### ✅ Step 4: Remote Control (Generic)

java

CopyEdit

`class RemoteControl {     private Command command;     public void setCommand(Command command) {         this.command = command;     }     public void pressButton() {         command.execute();     } }`

---

### ✅ Client Code

java

CopyEdit

`public class Main {     public static void main(String[] args) {         Light light = new Light();         Fan fan = new Fan();          Command lightOn = new LightOnCommand(light);         Command fanStart = new FanStartCommand(fan);          RemoteControl remote = new RemoteControl();          remote.setCommand(lightOn);         remote.pressButton(); // Light ON          remote.setCommand(fanStart);         remote.pressButton(); // Fan STARTED     } }`

---

## 💡 How This Solves Inefficiency:

|Problem in Non-Command Version|Solved By Command Pattern|
|---|---|
|`RemoteControl` tightly coupled to devices|Can work with any device using Command interface|
|Adding a new device requires changes|Just create new Command class|
|Can't store/queue/undo actions|Can easily do with `List<Command>`|
|Can't customize behavior at runtime|Just set different `Command` object dynamically|

---

## ✅ BONUS: Queue/Undo Example

Want to implement **undo**?

Just maintain a `Stack<Command>` and call `undo()` from command if needed.