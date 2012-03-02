package nl.avscripting.multiroommpd;

public class MPDOutput {
	public String name;
	public boolean enabled;
	public int id;
	public MPDInstance instance;
	public MPDOutput(String _name, boolean _enabled, int _id){
		name = _name;
		enabled = _enabled;
		id=_id;
	}
	public String toString(){
		return "MPDOutput name: "+name+", id: "+id+", enabled: "+enabled;
	}
}