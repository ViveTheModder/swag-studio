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
![preview1](https://github.com/ViveTheModder/swag-studio/blob/main/img/prev-1.png)

![preview2](https://github.com/ViveTheModder/swag-studio/blob/main/img/prev-2.png)

And just for fun, here is the output of the program if GSC-B-46.gsc (Galaxy Battle, the game's longest scenario) is given:
[Battle Settings]
Map: Outer Space
BGM: Hot Soul
Player 1 Teammates: 4
Player 2 Teammates: 4

[Player 1's Team]
> Teammate 1
Character: Vegeta (Scouter)
Costume: 1
Damaged: false
COM Difficulty Level: 0
Strategy Z-Item: Goku Type
Initial Health: 100%
Z-Item #1: Master Roshi's Training
Z-Item #2: null
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: Dragon Break
Z-Item #7: Vanishing Break

> Teammate 2
Character: Bardock
Costume: 1
Damaged: false
COM Difficulty Level: 0
Strategy Z-Item: Goku Type
Initial Health: 100%
Z-Item #1: Master Roshi's Training
Z-Item #2: null
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: Dragon Break
Z-Item #7: Vanishing Break

> Teammate 3
Character: Fasha
Costume: 1
Damaged: false
COM Difficulty Level: 0
Strategy Z-Item: Goku Type
Initial Health: 100%
Z-Item #1: Defense -8
Z-Item #2: null
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: Dragon Break
Z-Item #7: Vanishing Break

> Teammate 4
Character: King Vegeta 
Costume: 1
Damaged: false
COM Difficulty Level: 0
Strategy Z-Item: Goku Type
Initial Health: 100%
Z-Item #1: null
Z-Item #2: null
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: Dragon Break
Z-Item #7: Vanishing Break

[Player 2's Team]
> Teammate 1
Character: Cui
Costume: 1
Damaged: false
COM Difficulty Level: 5
Strategy Z-Item: Ginyu Type
Initial Health: 100%
Z-Item #1: null
Z-Item #2: null
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: null
Z-Item #7: null

> Teammate 2
Character: Dodoria
Costume: 1
Damaged: false
COM Difficulty Level: 7
Strategy Z-Item: Recoome Type
Initial Health: 100%
Z-Item #1: null
Z-Item #2: null
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: Dragon Break
Z-Item #7: Vanishing Break

> Teammate 3
Character: Zarbon
Costume: 1
Damaged: false
COM Difficulty Level: 8
Strategy Z-Item: Gohan Type
Initial Health: 100%
Z-Item #1: King Kai's Training
Z-Item #2: Defense +32
Z-Item #3: null
Z-Item #4: null
Z-Item #5: null
Z-Item #6: Dragon Break
Z-Item #7: Vanishing Break

> Teammate 4
Character: Frieza - 1st Form
Costume: 1
Damaged: false
COM Difficulty Level: 9
Strategy Z-Item: Goku Type
Initial Health: 100%
Z-Item #1: Strength Enhancement
Z-Item #2: Defense +40
Z-Item #3: Attack +30
Z-Item #4: Power Body
Z-Item #5: null
Z-Item #6: Dragon Crush
Z-Item #7: Vanishing Rush

[Scene 0]
Condition: Win Battle
> GSAC ID: 1
Condition: Win Battle (Opponent)
> GSAC ID: 40
Event:     Nothing

[Scene 1]
Condition: Immediate
> GSAC ID: 2
[Changes to Opponent detected.]
> Gain 10000 HP (?)
Event:     Switch with Teammate #2 (Opponent)
Character: Dodoria

[Scene 2]
Condition: Win Battle
> GSAC ID: 3
Condition: Win Battle (Opponent)
> GSAC ID: 41
Event:     Nothing

[Scene 3]
Condition: Wait 10 seconds
> GSAC ID: 5
Condition: Win Battle
> GSAC ID: 38
Condition: Win Battle (Opponent)
> GSAC ID: 48
[Changes to Opponent detected.]
> Gain 15000 HP (?)
Event:     Switch with Teammate #3 (Opponent)
Character: Zarbon

[Scene 5]
Condition: Wait 30 seconds
> GSAC ID: 6
Condition: Win Battle (Opponent)
> GSAC ID: 42
Condition: Win Battle
> GSAC ID: 34
[Changes to Player 1 detected.]
> Gain 15000 HP (?)
Event:     Switch with Teammate #3
Character: Fasha

[Scene 6]
Condition: Immediate
> GSAC ID: 7
[Changes to Player 1 detected.]
> Gain 10000 HP (?)
Event:     Switch with Teammate #2
Character: Bardock

[Scene 7]
Condition: Wait 30 seconds
> GSAC ID: 8
Condition: Lose 3 Health Bars (Opponent)
> GSAC ID: 8
Condition: Win Battle (Opponent)
> GSAC ID: 42
Condition: Win Battle
> GSAC ID: 35
[Changes to Opponent detected.]
> Z-Item #2: Defense +15
> Z-Item #3: null
> Z-Item #4: null
> Z-Item #5: null
Event:     Nothing

[Scene 8]
Condition: Wait 10 seconds
> GSAC ID: 9
Condition: Win Battle (Opponent)
> GSAC ID: 43
Condition: Win Battle
> GSAC ID: 35
[Changes to Opponent detected.]
> Set current Health to 100%
> Z-Item #2: Defense +35
> Z-Item #3: Attack +40
> Z-Item #4: Ultimate Body
> Z-Item #5: null
Event:     Assign Transformation (Opponent)
Character: Zarbon - Post-Transformation

[Scene 9]
Condition: Immediate
> GSAC ID: 10
[Changes to Player 1 detected.]
> Gain 15000 HP (?)
Event:     Switch with Teammate #3
Character: Fasha

[Scene 10]
Condition: Win Battle
> GSAC ID: 30
Condition: Win Battle (Opponent)
> GSAC ID: 11
Event:     Nothing

[Scene 11]
Condition: Immediate
> GSAC ID: 12
[Changes to Player 1 detected.]
> Gain 10000 HP (?)
Event:     Switch with Teammate #2
Character: Bardock

[Scene 12]
Condition: Win Battle
> GSAC ID: 13
Condition: Win Battle (Opponent)
> GSAC ID: 44
[Changes to Player 1 detected.]
> Set current Health to 100%
> Z-Item #4: Attack +5
> Z-Item #5: Indignation!
[Changes to Opponent detected.]
> Z-Item #2: null
> Z-Item #3: null
> Z-Item #4: null
> Z-Item #5: null
> Z-Item #6: null
> Z-Item #7: null
Event:     Aerial Clash

[Scene 13]
Condition: Immediate
> GSAC ID: 14
[Changes to Opponent detected.]
> Gain 20000 HP (?)
Event:     Switch with Teammate #4 (Opponent)
Character: Frieza - 1st Form

[Scene 14]
Condition: Wait 15 seconds
> GSAC ID: 16
Condition: Win Battle
> GSAC ID: 33
Condition: Win Battle (Opponent)
> GSAC ID: 47
Event:     Nothing

[Scene 16]
Condition: Immediate
> GSAC ID: 17
[Changes to Player 1 detected.]
> Gain 20000 HP (?)
Event:     Switch with Teammate #4
Character: King Vegeta 

[Scene 17]
Condition: Win Battle (Opponent)
> GSAC ID: 18
Condition: Win Battle
> GSAC ID: 36
Event:     Nothing

[Scene 18]
Condition: Immediate
> GSAC ID: 19
[Changes to Player 1 detected.]
> Gain 5000 HP (?)
[Changes to Opponent detected.]
> Set current Health to 20%
Event:     Switch with Teammate #1
Character: Vegeta (Scouter)

[Scene 19]
Condition: Win Battle
> GSAC ID: 37
Condition: Win Battle (Opponent)
> GSAC ID: 20
Event:     Nothing

[Scene 20]
Condition: Immediate
> GSAC ID: 21
[Changes to Player 1 detected.]
> Gain 10000 HP (?)
[Changes to Opponent detected.]
> Set current Health to 20%
Event:     Switch with Teammate #2
Character: Bardock

[Scene 21]
Condition: Win Battle
> GSAC ID: 33
Condition: Win Battle (Opponent)
> GSAC ID: 47
[Changes to Player 1 detected.]
> Set current Health to 100%
[Changes to Opponent detected.]
> Z-Item #1: Strength Enhancement
> Z-Item #2: Defense +5
> Z-Item #3: Attack +20
> Z-Item #4: null
> Z-Item #5: null
> Z-Item #6: Dragon Crush
> Z-Item #7: Vanishing Rush
Event:     Normal Clash

[Scene 4]
Condition: Immediate
> GSAC ID: 5
[Changes to Player 1 detected.]
> Gain 15000 HP (?)
Event:     Switch with Teammate #3
Character: Fasha

[Scene 15]
Condition: Wait 25 seconds
> GSAC ID: 16
Condition: Win Battle (Opponent)
> GSAC ID: 45
Condition: Win Battle
> GSAC ID: 31
[Changes to Player 1 detected.]
> Set current Health to 100%
Event:     Nothing

## Known Issues
* As of now, this program is command-line only.
* I cannot get a proper runnable JAR release of the program because of certain issues with Eclipse's export feature.
  Because of that, there are no download/build instructions provided.
  TL;DR: Guilty as charged, skill issue on my part.
