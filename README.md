# swag-studio
Swag Studio is a Budokai Tenkaichi 3 tool made for GSC files (Dragon History scenarios) to display basic information about them.
Currently, it displays the following:
* what map and background music the scenario uses;
* what characters each team has;
* what conditions there are, and what scenes they lead to;
* what Z-Items are equipped/removed during a specific scene;
* what health or ki bonuses there may or may not be present;
* what events occur, and what characters they use (if any).

To ensure the program works, it comes with a vanilla copy of the Common Enemy GSC file (GSC-B-0.gsc).

## Usage/Preview
![preview1](https://github.com/ViveTheModder/swag-studio/blob/main/img/demo.png)
And just for fun, [here](https://github.com/ViveTheModder/swag-studio/blob/main/out/GSC-B-46.txt) is the output of the program if GSC-B-46.gsc (Galaxy Battle, the game's longest scenario) is given.

In case the text file was hard to follow, here is a visualization of the scenario's structure.
![preview2](https://github.com/ViveTheModder/swag-studio/blob/main/img/visualization.png)

## Known Issues
* As of now, this program is command-line only.
* I cannot get a proper runnable JAR release of the program because of certain issues with Eclipse's export feature.
  Because of that, there are no download/build instructions provided.
  TL;DR: Guilty as charged, skill issue on my part.
