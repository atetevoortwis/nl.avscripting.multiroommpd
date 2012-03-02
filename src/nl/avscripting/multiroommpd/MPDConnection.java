package nl.avscripting.multiroommpd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

public class MPDConnection {	
    	private String server,password;
    	private int port;
    	private Socket sock;
    	private BufferedReader inp;
    	BufferedWriter out;
    	public volatile boolean shutdownRequested = false;
    	public MPDConnection(String _server,String _password, int _port){
    		this.server = _server;
    		this.port = _port;
    		this.password = _password;
    		Log.i("CONNTHREADD","Init: "+server);
    	}
    	public void close(){
    		try{
    			sock.close();
    		}catch(Exception e){
    			Log.i("MPDConnection.close",e.toString());
    		}
			Log.i("MPDConnection.close","Done");
    	}
    	public HashMap sendCommandWithResponse(String cmd){
    		HashMap m = new HashMap();
    		try{
    		ArrayList<String> response = sendCommand(cmd);
    		for(int i=0;i<response.size();i++){
    			Log.i("HASHCOMMAND",response.get(i));
    			String[] keyvalue = response.get(i).split(": ");
    			m.put(keyvalue[0], keyvalue[1]);
    		}
    		}catch(Exception e){
    			Log.i("HASHCOMMAND",e.toString());
    		}
    		return m;
    	}
    	public ArrayList<String> sendCommand(String cmd){
    		//offset returns substring(offset)
    		return sendCommand(cmd,0,false);    		
    	}
    	public ArrayList<String> sendCommand(String cmd,boolean debug){
    		//offset returns substring(offset)
    		return sendCommand(cmd,0,debug);    		
    	}
    	public ArrayList<String> sendCommand(String cmd,int offset){
    		//offset returns substring(offset)
    		return sendCommand(cmd,offset,false);    		
    	}
    	public ArrayList<String>  sendCommand(String cmd,int offset, boolean debug){
    		ArrayList<String> cmds = new ArrayList<String>();
    		if(cmd != null){
    			cmds.add(cmd);
    		}
    		return sendCommand(cmds,offset,debug);  
    	}
    	public ArrayList<String> sendCommand(ArrayList<String> cmd, boolean debug){
    		return sendCommand(cmd, 0,  debug);
    	}
    	public ArrayList<String> sendCommand(ArrayList<String> cmd,int offset, boolean debug){
    		ArrayList<String> response = new ArrayList<String>();
    		if(sock.isConnected()==false){
    			if(debug){
    				Log.i("MPDConnection.sendCommand","Socket not connected, connecting:");
    			}
    			this.connect();
    			if(sock.isConnected()==false){
    				Log.i("MPDConnection.sendCommand","Socket not connected, giving up:");
    				return null;
    			}
    		}
    		
    		try{
	    		if(cmd!=null){
	    			Log.i("MPDConn.SendCommand","Sending "+cmd.size()+" commands");
	    			if(cmd.size()>1){
	    				//open command list
	    				out.write("command_list_begin");
	    				out.newLine();
	    			}
	    			for(int i=0;i<cmd.size();i++){
	    				if(cmd.get(i).length()>0){
		    				out.write(cmd.get(i));
		    				out.newLine();
	    				if(debug){
		    				Log.i("MPDConnection.sendCommand","SEND "+i+": "+cmd.get(i));
		    			}
	    				}else{
	    					Log.i("MPDConnection.sendCommand","SENDEMPTY");
	    				}
	    			}
	    			
	    			if(cmd.size()>1){
	    				//close command list
	    				out.write("command_list_end");
	    				out.newLine();
	    			}
	    			out.flush();
	    			
	    		}
	    		String data ;
	    		while(sock.isConnected()){
	    			data = inp.readLine();
	    			if(data.length()>=2 && data.substring(0,2).equals("OK")){
						break;						
					}
					if(data.length()>=3 && data.substring(0,3).equals("ACK")){	
						Log.e("MPDConn","GOT ACK: "+data);
						break;						
					}
					if(data != null){
						if(offset!=0 && data.length()>=offset){
							//return part of data (for example "ARTIST: U2", offset=8 -> "U2"
							data = data.substring(offset);
						}
						response.add(data);
						if(debug){
							Log.i("MPDConnection.sendCommand","RECV: "+data);	
						}
					}
					
	    		}
    		}catch(Exception e){
    			Log.i("MPDConnection.sendCommand",e.toString());
    		}
    		if(debug){
    			Log.i("MPDConnection.sendCommand","Finished");	
    		}
    		return response;
    	}
		public boolean connect() {
			// TODO Auto-generated method stub
			Log.i("CONNTHREAD","Connecting to "+String.valueOf(this.server));
			try{
				InetAddress ia = InetAddress.getByName(server);
				Log.i("THREAD_SOCKET_CONNECTED",ia.getHostAddress());
				sock = new Socket(ia,this.port);
				inp = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));	
				//get response
				sendCommand("",true);
				//password?
				if(this.password!=null){
					sendCommand("password "+this.password,true);
				}
				return true;
		    }catch(Exception e){
				Log.i("InetAddress",e.toString());
				return false;
			}
		}
    	
    
}
