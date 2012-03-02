package nl.avscripting.multiroommpd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class MPDInstance {
	private MPDConnection conn;
	public ArrayList<MPDOutput> outputs;
	public String name;
	public int volume,playlistversion,nextsongid,nextsong,song,songid,time=0;
	public ArrayList<MPDPlaylistSong> playlist;
	public boolean repeat,random,single,consume,xfade,playing;
	private OnPlaylistChangedListener onPlaylistChangedListener;
	private Timer sleeptimer;
	public interface OnPlaylistChangedListener {
        public void onPlaylistChanged(MPDInstance instance);
    }
	
	public MPDInstance(String _name, String server, String password, int port){
		conn = new MPDConnection(server, password, port);
		name= _name;
		conn.connect();
		outputs=getOutputs();		
		playlist = new ArrayList<MPDPlaylistSong>();
		updateStatus();
		updatePlaylist();
	}
	public void setOnPlaylistChangedListener(OnPlaylistChangedListener listener){
		this.onPlaylistChangedListener = listener;
	}
	public String toString(){
		return this.name;
	}
	public MPDConnection getConnection(){
		return this.conn;
	}
	public void play(){
		conn.sendCommand("play");
	}
	public void play(MPDPlaylistSong song){
		conn.sendCommand("play "+song.pos);
	}
	public void pause(){
		conn.sendCommand("pause");
	}
	public void pauseSleep(){
		//pause and set sleeptimer = null
		sleeptimer = null;
		pause();
	}
	public void next(){
		conn.sendCommand("next");
	}	
	public void previous(){
		conn.sendCommand("previous");
	}
	
	public void random(boolean newValue){
		int val =newValue? 1 : 0;
		conn.sendCommand("random "+val);
	}
	public void repeat(boolean newValue){
		int val =newValue? 1 : 0;
		conn.sendCommand("repeat "+val);
	}
	
	public void clear(){
		conn.sendCommand("clear");
	}
	public boolean hasSleepTimer(){
		if(sleeptimer!=null){
			return true;
		}else{
			return false;
		}
	}
	public void setSleepTimer(int delayms){
		sleeptimer = new Timer(true);
		MPDSleepTimerTask task = new MPDSleepTimerTask(this);
		Log.i("Timer","Set at "+delayms/1000+"sec");
		sleeptimer.schedule(task, delayms);
	}
	public void cancelSleepTimer(){
		if(sleeptimer!=null){
			sleeptimer.cancel();
			sleeptimer = null;
		}
	}
	public void updatePlaylist(){
		ArrayList<MPDPlaylistSong> songs = new ArrayList<MPDPlaylistSong>();	
		ArrayList<String> arr_resp;
		arr_resp = conn.sendCommand("playlistinfo",true);
		playlist.clear();
		MPDPlaylistSong song = new MPDPlaylistSong();
		String data;
		Log.i("MPDInstance.updatePlaylist","GOT RESPONSE:"+arr_resp.size());
		int songcount = 0;
		for(int i=0;i<arr_resp.size();i++){
			data = arr_resp.get(i);
			if(data.length() > 5 && data.substring(0,5).equals("file:")){
				if(songcount>0){
					Log.i("MPDPlaylistSong",song.toString());
					playlist.add(song);						
				}
				songcount++;
				song = new MPDPlaylistSong();
				song.type = C.Song;
				song.file = data.substring(6,data.length());
			}
			if(data.length() > 6 && data.substring(0,6).equals("Title:")){				
				song.title = data.substring(7,data.length());
			}
			if(data.length() > 6 && data.substring(0,6).equals("Album:")){				
				song.album = data.substring(7,data.length());
			}
			if(data.length() > 7 && data.substring(0,7).equals("Artist:")){				
				song.artist = data.substring(8,data.length());
			}
			if(data.length() > 4 && data.substring(0,4).equals("Pos:")){				
				song.pos = Integer.valueOf(data.substring(5,data.length()));
			}
			if(data.length() > 3 && data.substring(0,3).equals("Id:")){				
				song.id = Integer.valueOf(data.substring(4,data.length()));
			}
		}
		playlist.add(song);
	}
	public void updateStatus(){
		HashMap m = conn.sendCommandWithResponse("status");
		if(m.containsKey("volume")){
			this.volume = Integer.parseInt(m.get("volume").toString());
		}
		if(m.containsKey("playlist")){
			int newVersion = Integer.parseInt(m.get("playlist").toString());
			if(newVersion != this.playlistversion){
				updatePlaylist();
			}
			this.playlistversion = newVersion;
		}
		if(m.containsKey("nextsongid")){
			this.nextsongid = Integer.parseInt(m.get("nextsongid").toString());
		}
		if(m.containsKey("nextsong")){
			this.nextsong = Integer.parseInt(m.get("nextsong").toString());
		}
		if(m.containsKey("song")){
			this.song = Integer.parseInt(m.get("song").toString());
		}
		if(m.containsKey("songid")){
			this.songid = Integer.parseInt(m.get("songid").toString());
		}
		if(m.containsKey("repeat")){
			this.repeat = 0!=Integer.parseInt(m.get("repeat").toString());
		}
		if(m.containsKey("random")){
			this.random = 0!=Integer.parseInt(m.get("random").toString());
		}
		if(m.containsKey("single")){
			this.single = 0!=Integer.parseInt(m.get("single").toString());
		}
		if(m.containsKey("consume")){
			this.consume = 0!=Integer.parseInt(m.get("consume").toString());
		}
		if(m.containsKey("xfade")){
			this.xfade = 0!=Integer.parseInt(m.get("xfade").toString());
		}
		if(m.containsKey("state")){
			if(m.get("state").toString().equals("play")){
				this.playing=true;
			}else{
				this.playing = false;			
			}
		}
		Log.i("MPDInstanceStatus",m.toString());
	}
	public void setOutput(MPDOutput out, boolean enabled){
		if(enabled){
			conn.sendCommand("enableoutput "+out.id);
			if(this.playing){
				this.pause();
				this.play();
			}
		}else{
			conn.sendCommand("disableoutput "+out.id);
		}
		outputs=getOutputs();
	}
	public void Add(ArrayList<MPDSong> songs){
		ArrayList<String> cmd=new ArrayList<String>();
		for(int i=0;i<songs.size();i++){
			cmd.add("add \""+songs.get(i).file+"\"");
		}
		conn.sendCommand(cmd,true);
		onPlaylistChangedListener.onPlaylistChanged(this);
	}
	//Load outputs
	public ArrayList<MPDOutput> getOutputs(){
		ArrayList<String>  response = conn.sendCommand("outputs",true);
		ArrayList<MPDOutput> outputs = new ArrayList<MPDOutput>();
		MPDOutput out = null;
		String data,name="";
		int id=-1;
		boolean enabled;
		for(int i=0;i<response.size();i++){
			data = response.get(i);
			if(data.contains("outputid: ")){
				id = Integer.parseInt(data.replace("outputid: ", ""));
			}
			if(data.contains("outputname: ")){
				name = data.replace("outputname: ", "");
			}
			if(data.contains("outputenabled: ")){				
				enabled =(Integer.parseInt(data.replace("outputenabled: ", ""))!=0);
				out = new MPDOutput(name, enabled, id);
				out.instance = this;
				outputs.add(out);
				Log.i("MPDInstance",out.toString());
			}
		}
		return outputs;
	}
}
