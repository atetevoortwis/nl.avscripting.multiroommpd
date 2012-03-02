package nl.avscripting.multiroommpd;

public class MPDSong implements MPDEntity {
	public String file, artist, title, album="";
	public int time,track=0;
	public int type;
	public boolean compilation = false;
	public MPDSong(){
		this.type = C.Song;		
	}
	@Override public String toString(){
		return toString(false);
	}
	public String toString(boolean realValue){
		if(this.title == null){
			return "Untitled";
		}else{
			if(this.title.trim().equals("") && !realValue){
				return "No title";
			}else{
				return this.title;
			}
		}
	}
}
