## NoteBot for rusherhack

Notebot Plugin for rusherhack

# Please keep in mind that there will be no module in the external tab, only the `*notebot` command

### Usage
 use asterisk (*) default RusherHack prefix before every command
 
  to start put .nbs and .midi/mid files to .minecraft/rusherhack/notebot/songs
  
  do `*notebot queue add <filename>` (example Megalovania.nbs)
  
  and `*notebot start`
  
  if you want to remove something from queue use `*notebot queue remove <index>` or clear the whole queue with `*notebot queue clear`
  to find song index use `*notebot queue list` and find number to the left of the song 

  if you want a song to be looped do `*notebot loop song` and if you want your queue to be looped do `*notebot loop queue`
  
### Building
 - `git clone https://github.com/Lokfid/RusherHackNoteBot`
 - `cd RusherHackNoteBot`
 - `./gradlew build`
  find your file in /build/libs
