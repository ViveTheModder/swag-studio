# swag-studio
Swag Studio is a Budokai Tenkaichi 3 tool made for GSC files (Dragon History scenarios) to display basic information about them.
It can display the following:
* what map and background music the scenario uses, and if it ever changes during certain scenes;
* what rewards the player gets after completing a scenario;
* what characters each team has;
* what conditions there are, and what scenes they lead to;
* what Z-Items are equipped/removed during a specific scene;
* what health or ki bonuses there may or may not be present;
* what events occur, and what characters they use (if any).
Although, the user can also choose to display cinematic information, which refers to:
* character positions/rotations;
* character animations (atm, not every animation has been documented properly, so the program just returns the animation ID);
* character voice lines, ones that play during scenes, but also in the background;
* enabling or disabling visual effects like the Aura Charge, Ki Charge, Camera Shake and MAX Power Mode explosion;
* enabling or disabling fades (fade-in / fade-out).

To ensure the program works, it comes with a folder containing vanilla copies of every GSC (from GSC-B-00.gsc to GSC-B-47.gsc).

## Usage/Preview
![preview1](https://github.com/ViveTheModder/swag-studio/blob/main/img/demo.png)
And just for fun, [here](https://github.com/ViveTheModder/swag-studio/blob/main/out/GSC-B-46.txt) is the output of the program if GSC-B-46.gsc (Galaxy Battle, the game's longest scenario) is given.

In case the text file was hard to follow, here is a visualization of the scenario's structure.
![preview2](https://github.com/ViveTheModder/swag-studio/blob/main/img/visualization.png)
