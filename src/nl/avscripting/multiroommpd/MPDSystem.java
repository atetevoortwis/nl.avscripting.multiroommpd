package nl.avscripting.multiroommpd;

import java.util.ArrayList;

public class MPDSystem {
	public ArrayList<MPDInstance> instances;
	public MPDSystem(){
		instances = new ArrayList<MPDInstance>();
		instances.add(new MPDInstance("C1","home.avscripting.nl", "m7n6b5", 6600));
		instances.add(new MPDInstance("C2","home.avscripting.nl", "m7n6b5", 6601));
	}
	public MPDLibrary getLibrary(){
		return new MPDLibrary(instances.get(0).getConnection());
	}
	public MPDInstance getCurrentInstance(){
		return instances.get(0);
	}
}
