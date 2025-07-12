
This repository contains the **native implementation of interaction modes between a smartphone and a smartwatch**. It includes the development and validation of **bidirectional communication prototypes** using Android and Wear OS native APIs. 

Specifically, it covers the following interaction modes:

- **D-Pad:** Sending directional inputs (up, down, left, right) between devices.  
- **Forcebars:** Selecting and transmitting intensity levels.  
- **Joystick:** Capturing analog direction and magnitude inputs.  
- **Tap:** Sending simple tap signals as discrete events.

These prototypes are designed to verify **real-time communication reliability**, latency, and data integrity for future integration into game or interactive applications. All use cases are documented, including preconditions, actor definitions, and summarized flows.

# 🚀 Main Objectives

- Develop native Android and Wear OS apps for each interaction mode.
- Validate **bidirectional message transmission** between devices.
- Document implementation details and use cases for integration into Unity in the next development stage.

## 📂 Structure

- `/app_mobile`: Smartphone native app implementation.
- `/app_wear`: Smartwatch native app implementation.

📑 Use Cases

### 👆 Use Case: Tap interaction

| **Field**         | **Description**                                                                                                                                                                                                                     |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Use Case**      | Send instruction using Tap between smartphone and smartwatch.                                                                                                                                                                       |
| **Actor**         | User and System.                                                                                                                                                                                                                    |
| **Purpose**       | Verify communication using tap interaction.                                                                                                                                                                                         |
| **Summary**       | The user opens both apps. The interface has a tap button. When pressed on the smartphone, a signal is sent to the smartwatch for display. The process is repeated from the smartwatch to the smartphone to validate the connection. |
| **Preconditions** | (1) Apps installed. (2) Permissions granted. (3) Devices paired via Bluetooth.                                                                                                                                                      |

### 🕹️ Use Case: D-Pad interaction

| **Field**         | **Description**                                                                                                                                                                                                                                                                                                          |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Use Case**      | Send instruction using D-Pad between smartphone and smartwatch.                                                                                                                                                                                                                                                          |
| **Actor**         | User and System.                                                                                                                                                                                                                                                                                                         |
| **Purpose**       | Verify communication using the D-Pad interaction method.                                                                                                                                                                                                                                                                 |
| **Summary**       | The user opens both applications. The interface displays a D-Pad with directional buttons (up, down, left, right). The user presses a direction on the smartphone, which sends the signal to the smartwatch for visualization. The process is repeated from the smartwatch to the smartphone to validate the connection. |
| **Preconditions** | (1) Apps installed. (2) Permissions granted. (3) Devices paired via Bluetooth.                                                                                                                                                                                                                                           |

### 🛠️ Use Case: Forcebars interaction

| **Field**         | **Description**                                                                                                                                                                                                                                                         |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Use Case**      | Send instruction using Forcebars between smartphone and smartwatch.                                                                                                                                                                                                     |
| **Actor**         | User and System.                                                                                                                                                                                                                                                        |
| **Purpose**       | Verify communication using Forcebars for intensity selection.                                                                                                                                                                                                           |
| **Summary**       | The user opens both apps. The interface shows force bars to select intensity levels. The user selects a level on the smartphone, which is sent to the smartwatch for display. The process is repeated from the smartwatch to the smartphone to validate the connection. |
| **Preconditions** | (1) Apps installed. (2) Permissions granted. (3) Devices paired via Bluetooth.                                                                                                                                                                                          |

### 🎮 Use Case: Joystick interaction

| **Field**         | **Description**                                                                                                                                                                                                                                                           |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Use Case**      | Send instruction using Joystick between smartphone and smartwatch.                                                                                                                                                                                                        |
| **Actor**         | User and System.                                                                                                                                                                                                                                                          |
| **Purpose**       | Verify communication using joystick analog input.                                                                                                                                                                                                                         |
| **Summary**       | The user opens both apps. The interface displays a joystick capturing direction and magnitude. The user moves the joystick on the smartphone, sending values to the smartwatch. The process is repeated from the smartwatch to the smartphone to validate the connection. |
| **Preconditions** | (1) Apps installed. (2) Permissions granted. (3) Devices paired via Bluetooth.                                                                                                                                                                                            |
## Notes

This repository is part of a Research Article titled "A Communication Module for Smartphone–Smartwatch Integration in Pervasive Games Using Unity 3D and Native Android", for more details about each stage and the performance analyses performed, download the associated paper (ref to the paper's doi)

## Authors

* **Claudio Rubio Naranjo** - [ClaudixoPG](https://github.com/ClaudixoPG); crubio17@alumnos.utalca.cl; claudiorubio23@gmail.com
* **Felipe Besoain** - [Fbesoain](https://github.com/fbesoain); fbesoain@utalca.cl

## Acknowledgments

* Research funded by Agencia Nacional de Investigación y Desarrollo, ANID-Subdirección del Capital Humano/Doctorado Nacional/2023-21232404 and FONDECYT Iniciación grant 11220438.

- Universidad de Talca
