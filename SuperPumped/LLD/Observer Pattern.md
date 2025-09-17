
**Theory**: https://refactoring.guru/design-patterns/observer
**Example**: https://refactoring.guru/design-patterns/observer/java/example

---


>**Observer** is a behavioral design pattern that lets you define a subscription mechanism to notify multiple objects about any events that happen to the object they’re observing.
>
> **Example**: Any scenario that requires event listeners fall under this pattern, example you have a weather station that monitors temperature in regular intervals and you have multiple display types, such as tv display and mobile display that are require to update the displayed temperature every time the weather stations finds a change in the reading value




## Applicability

- Use the Observer pattern when changes to the state of one object may require changing other objects, and the actual set of objects is unknown beforehand or changes dynamically.
- Use the pattern when some objects in your app must observe others, but only for a limited time or in specific cases.
	- The subscription list is dynamic, so subscribers can join or leave the list whenever they need to.


---
