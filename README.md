# Java Burglar Alarm System (Real-Time CEP with Esper)

> **Course:** 25CSSE03H – Software Development for Real-Time Systems  
> **Design Methodology:** COMET  
> **Core Framework:** Esper Complex Event Processing (CEP)

A real-time, software-driven burglar alarm system designed using the COMET methodology. The system monitors intrusion sensors, power supply status, and console inputs, processing events in real-time via the **Esper CEP framework** to trigger appropriate responses within strict timing constraints. Hardware is treated as a black box and simulated via dummy software devices for testing.

---

## Features & Real-Time Constraints

| Stimuli / Event                          | System Response                                      | Max Response Time |
|------------------------------------------|------------------------------------------------------|-------------------|
| Sensor Polling                           | Poll all intrusion sensors & fetch status            | `500ms`           |
| Sensor Triggered                         | Detect intrusion & identify sensor/room              | `100ms`           |
| Main Power Drop (10%–20%)                | Switch to backup battery                             | `50ms`            |
| Main Power Drop (>20%)                   | Switch to backup battery + trigger intrusion alert   | `150ms`           |
| Console Panic Button Pressed             | Trigger intrusion alert & log room                   | `100ms`           |
| Console Clear Alarms Button Pressed      | Silence alarm & turn off intruded room lights        | `1s`              |
| Intrusion Detected                       | Sound alarm, turn on room lights, contact police     | `2s`              |
| Power Supply / Sensor Failure            | Notify service technician with failed component      | `3s`              |

---

## Tech Stack

- **Language:** Java 8
- **CEP Engine:** Esper 
- **Build Tool:** Apache Ant *(configurable for Maven/Gradle)*
- **Testing:** JUnit 5
- **Design:** COMET Methodology, UML (Use Case, Sequence, State, Class)

## Prerequisites

- JDK 8
- Git
