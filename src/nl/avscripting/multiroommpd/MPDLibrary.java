package nl.avscripting.multiroommpd;

import java.util.ArrayList;
import java.util.Collections;

import android.util.Log;

public class MPDLibrary {
	MPDConnection conn;
	private ArrayList<MPDArtist> currentArtists = null;
	private ArrayList<MPDAlbum> currentAlbums = null;
	private ArrayList<MPDSong> currentSongs = null;
	private String currentArtist,currentAlbum="";
	public MPDLibrary(MPDConnection _conn){
		this.conn = _conn;			
	}
//	public String[] getArtists(){
//		ArrayList<String> arr_artists = conn.sendCommand("list artist");
//		Collections.sort(arr_artists);
//		String[] artists = new String[arr_artists.size()];
//		for(int i=0;i<arr_artists.size();i++){
//			artists[i]=arr_artists.get(i).replace("Artist: ","");
//		}
//		return artists;
//	}
	public ArrayList<MPDArtist> getArtists(){
		//cache artist list:		
		if(currentArtists!=null){
			return currentArtists;
		}else{
			//offset of 8 cuts off "Artist: "
			ArrayList<String> arr_artists = conn.sendCommand("list artist",8);
			Collections.sort(arr_artists);		
			ArrayList<MPDArtist> artists = new ArrayList<MPDArtist>();
			for(int i=0;i<arr_artists.size();i++){
				artists.add(new MPDArtist(arr_artists.get(i)));
			}
			currentArtists = artists;
			return artists;
		}
	}
	public ArrayList<MPDAlbum> getAlbums(){
		return getAlbums(null);		
	}
	public ArrayList<MPDAlbum> getAlbums(String artist){
		if(artist != null){
			artist = " \""+artist+"\"";
		}else{
			artist = "";
		}
		if(currentArtist!=null && currentArtist.equals(artist) && currentAlbums!=null){
			return currentAlbums;
		}else{
			currentArtist = artist;
			ArrayList<MPDAlbum> albums = new ArrayList<MPDAlbum>();
			ArrayList<String> arr_albums = conn.sendCommand("list album"+artist,7,true);
			Collections.sort(arr_albums);
			for(int i=0; i<arr_albums.size();i++){
				albums.add(new MPDAlbum(arr_albums.get(i)));
			}	
			currentAlbums = albums;
			return albums;
		}
	}
	public ArrayList<MPDSong> getSongs(String artist, String album){
		Log.i("MPDLibrary.getSongs",artist+", "+album);
		if(currentArtist!=null && currentAlbum!=null && currentArtist.equals(artist) && currentAlbum.equals(album) && currentSongs!=null){
			return currentSongs;
		}else{
			currentAlbum = album;
			ArrayList<MPDSong> songs = new ArrayList<MPDSong>();	
			ArrayList<String> arr_songs;
			if(album == null || album.length()==0){
				//look for artist, filter on album		
				arr_songs = conn.sendCommand("find artist \""+artist+"\"",true);
			}else{
				arr_songs = conn.sendCommand("find album \""+album+"\"",true);
			}
			MPDSong song = new MPDSong();
			String data;
			Log.i("MPDLibrary.getSongs","GOT RESPONSE:"+arr_songs.size());
			int songcount = 0;
			boolean compilation = false;
			for(int i=0;i<arr_songs.size();i++){
				data = arr_songs.get(i);
				if(data.substring(0,5).equals("file:")){
					if(songcount>0){
						if(song.artist!=null && song.artist.equals(artist) && (album==null || song.album.equals(album))){						
							songs.add(song);				
						}else{
							Log.i("MPDLibrary.getSongs","Dropped");
							compilation=true;
						}
					}
					songcount++;
					song = new MPDSong();
					song.type = C.Song;
					song.file = data.substring(6,data.length());
				}
				if(data.substring(0,6).equals("Title:")){				
					song.title = data.substring(7,data.length());
				}
				if(data.substring(0,6).equals("Album:")){				
					song.album = data.substring(7,data.length());
				}
				if(data.substring(0,7).equals("Artist:")){				
					song.artist = data.substring(8,data.length());
				}
			}
			if(song.artist!=null && song.artist.equals(artist)){						
				songs.add(song);				
			}else{
				Log.i("MPDLibrary.getSongs","Dropped");
			}
			//compilation?
			if(compilation){
				for(int i=0;i<songs.size();i++){
					songs.get(i).compilation=true;
				}
			}
			currentSongs = songs;
			return songs;
		}
	}
	
}
