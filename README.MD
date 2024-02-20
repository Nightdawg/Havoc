# HAVOC
## Haven & hearth custom client

### Languages Used
- JAVA, json
-------------

### Project Description
***WE'RE NO LONGER UPDATING THIS CLIENT***

Havoc repo: https://github.com/Nightdawg/Havoc.git
Loftar's repo: git://sh.seatribe.se/hafen-client

DON'T BOTHER USING LOFTAR'S GITHUB REPO!!! Use his seatribe repo, cause he often times forgets updating the one on github.

Quick set up for future users:

1. Fork the Havoc repo
2. Pull from Loftar's repo
3. Merge conflicts, if any (good luck)
4. Check if all fetched res files are up to date (in the IDE terminal: java -cp bin/hafen.jar haven.Resource find-updates)
5. (IF there are res updates) Deal with Loftar's fetched res fuckery (Read his readme to understand how it works)

Note: Loftar usually says "I have already pushed all the changes 2 weeks ahead of the world wipe", then proceeds to break 5 res files and push more updates 1 hour before the wipe. 
Idk if it's schizophrenia or whatever, but just so you know.

Anyway good luck have fun

---
## Installation
### Windows

use the .bat file, make sure you have java up to date

### Debian / Ubuntu installation

Open a terminal and copy paste this lign.
```properties
sudo apt update && sudo apt install default-jre -y && mkdir -p ~/Game/Haven/ && cd ~/Game/Haven/ && wget https://github.com/Nightdawg/Havoc/releases/latest/download/Havoc.v1.1.0.zip -O Havoc.zip && unzip ./Havoc.zip && rm -v ./Havoc.zip && chmod -R 775 ./ && exit
```
**Make a launcher with icon**
```properties
cp ~/Game/Haven/Havoc/Haven-havoc.desktop ~/.local/share/applications/
```
It *should* be in application menu => Game
