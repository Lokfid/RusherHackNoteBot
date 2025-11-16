package org.lokfid.type;

public class Note {
    public int pitch;
    public int instrument;

    public Note(int pitch, int instrument){
        this.pitch = pitch;
        this.instrument = instrument;
    }

    @Override
    public int hashCode(){
        return pitch * 31 + instrument;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Note other)) return false;

        return instrument == other.instrument && pitch == other.pitch;
    }
}
