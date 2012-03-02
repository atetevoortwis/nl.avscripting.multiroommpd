package nl.avscripting.multiroommpd;

public class MPDAlbum implements MPDEntity{
	public String name;
	public int type;
	public MPDAlbum(String _name){
		this.type = C.Album;
		this.name = _name;
	}
	@Override public String toString(){
		return toString(false);
	}
	public String toString(boolean realValue){		
		if(this.name.trim().equals("") && !realValue){
			return "No name";
		}else{
			return this.name;
		}
	}
}
