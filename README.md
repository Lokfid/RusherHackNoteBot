## NoteBot for rusherhack

Notebot Plugin for rusherhack

### Usage
 use asterisk (*) default RusherHack prefix before every command
 
  to start put .nbs and .midi/mid files to ./minecraft/rusherhack/notebot/songs
  
  do `*notebot queue add` `filename` (example Megalovania.nbs)
  and * notebot start
  
  if you want to remove something from queue use `*notebot queue remove` `index` or clear the whole queue with `*notebot queue clear`
  to find song index use `*notebot queue list` and find number to the left of the song 
  
### Building
 - `git clone https://github.com/Lokfid/RusherHackNoteBot`
 - `cd RusherHackNoteBot`
 - `./gradlew build`
  find your file in /build/libs
