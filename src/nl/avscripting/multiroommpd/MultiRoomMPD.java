package nl.avscripting.multiroommpd;

import java.util.ArrayList;

import com.example.helloandroid.R;

import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MultiRoomMPD extends TabActivity {
	/** Called when the activity is first created. */
	private MPDConnection conn;
	private MPDLibrary lib;
	private SimpleFilterableAdapter<MPDEntity> adp_library, adp_playlist;
	private static ArrayList<MPDEntity> lv_arr_library, lv_arr_playlist;
	private String currentView, currentArtist, currentAlbum, artistFilter,
			albumFilter = "";
	private int lvArtistPos, lvAlbumPos;

	private Button btnBack, btnFullAlbum, btnStream;
	private EditText editFilterLibrary, editFilterPlaylist;
	private ListView lv_library, lv_playlist;
	private GestureLibrary gLib;
	private static final String TAG = "HelloAndroid";
	private MediaPlayer mp;
	private MPDInstance currentInstance;
	private MPDInstance currentPlaylistInstance;
	private MPDSystem sys;

	private Button[] playButtons, prevButtons, nextButtons, sleepButtons;
	private ToggleButton[] repeatButtons, randomButtons;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TabHost mTabHost = getTabHost();

		mTabHost.addTab(mTabHost.newTabSpec("tab_test1").setIndicator("MUSIC")
				.setContent(R.id.tabLibrary));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test2")
				.setIndicator("PLAYLIST").setContent(R.id.tabPlaylist));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test3").setIndicator("ROOMS")
				.setContent(R.id.tabRooms));
		mTabHost.addTab(mTabHost.newTabSpec("tab_test4")
				.setIndicator("CONTROLS").setContent(R.id.tabControls));

		mTabHost.setCurrentTab(0);
		sys = new MPDSystem();
		currentInstance = sys.getCurrentInstance();

		lib = sys.getLibrary();

		initObjects();
		initRooms();
		initGestures();
		initListViewLibrary();
		initTextFilter();
		initControls();
		initListViewPlaylist();
	}

	/*
	 * Initialization methods:
	 */
	private void initControls() {
		LinearLayout l = (LinearLayout) findViewById(R.id.tabControls);
		l.setOrientation(l.VERTICAL);
		playButtons = new Button[sys.instances.size()];
		prevButtons = new Button[sys.instances.size()];
		nextButtons = new Button[sys.instances.size()];
		sleepButtons = new Button[sys.instances.size()];
		repeatButtons = new ToggleButton[sys.instances.size()];
		randomButtons = new ToggleButton[sys.instances.size()];

		for (int i = 0; i < sys.instances.size(); i++) {
			MPDInstance inst = sys.instances.get(i);
			LinearLayout line = new LinearLayout(l.getContext());
			line.setOrientation(line.HORIZONTAL);
			TextView t = new TextView(line.getContext());
			t.setText(inst.name);
			l.addView(t);

			Log.i("INITCONTROLS", "Making buttons for " + inst.name);
			prevButtons[i] = new Button(l.getContext());
			prevButtons[i].setText("<--");
			prevButtons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					MPDInstance inst = (MPDInstance) v.getTag();
					inst.previous();
				}
			});
			line.addView(prevButtons[i]);

			playButtons[i] = new Button(l.getContext());
			playButtons[i].setTag(inst);
			playButtons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// TODO Auto-generated method stub
					MPDInstance inst = (MPDInstance) v.getTag();
					if (inst.playing) {
						inst.pause();
					} else {
						inst.play();
					}
					updateControls();
				}
			});
			line.addView(playButtons[i]);

			nextButtons[i] = new Button(l.getContext());
			nextButtons[i].setText("-->");
			nextButtons[i].setTag(inst);
			nextButtons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					MPDInstance inst = (MPDInstance) v.getTag();
					inst.next();
				}
			});
			line.addView(nextButtons[i]);
			// new line
			l.addView(line);
			line = new LinearLayout(l.getContext());
			line.setOrientation(line.HORIZONTAL);
			randomButtons[i] = new ToggleButton(l.getContext());
			randomButtons[i].setTextOn("RAND");
			randomButtons[i].setTextOff("RAND");
			randomButtons[i].setTag(inst);
			randomButtons[i]
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							MPDInstance inst = (MPDInstance) buttonView
									.getTag();
							inst.random(isChecked);
						}
					});
			line.addView(randomButtons[i]);

			repeatButtons[i] = new ToggleButton(l.getContext());
			repeatButtons[i].setTextOn("REPEAT");
			repeatButtons[i].setTextOff("REPEAT");
			repeatButtons[i].setTag(inst);
			repeatButtons[i]
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							MPDInstance inst = (MPDInstance) buttonView
									.getTag();
							inst.repeat(isChecked);
						}
					});
			line.addView(repeatButtons[i]);

			// sleep button
			sleepButtons[i] = new Button(l.getContext());
			sleepButtons[i].setText("SLEEP");
			sleepButtons[i].setTag(inst);
			sleepButtons[i].setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					MPDInstance inst = (MPDInstance) v.getTag();
					if (inst.hasSleepTimer()) {
						inst.cancelSleepTimer();
						Button b = (Button) v;
						b.setText("SLEEP");
					} else {
						MPDSleepDialog dialog = new MPDSleepDialog(
								MultiRoomMPD.this);
						Log.i("MPDSleepDialog", "Show");
						dialog.instance = inst;
						dialog.show();
						dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
							public void onDismiss(DialogInterface dialog) {
								// Did the timer start?
								MPDSleepDialog dia = (MPDSleepDialog) dialog;
								if (dia.instance.hasSleepTimer()) {
									Button b = getSleepButtonByInstance(dia.instance);
									b.setText("CANCEL TIMER");
								}
							}
						});
					}
				}
			});
			line.addView(sleepButtons[i]);

			l.addView(line);
		}
		updateControls();
	}

	private Button getSleepButtonByInstance(MPDInstance inst) {
		for (int i = 0; i < sys.instances.size(); i++) {
			if (sys.instances.get(i).equals(inst)) {
				return sleepButtons[i];
			}
		}
		return null;
	}

	private void updateControls() {
		for (int i = 0; i < sys.instances.size(); i++) {
			MPDInstance inst = sys.instances.get(i);
			inst.updateStatus();
			Log.i("MPDUpdateControls", playButtons[i].getText().toString());
			randomButtons[i].setChecked(inst.random);
			repeatButtons[i].setChecked(inst.repeat);
			if (inst.playing) {
				playButtons[i].setText("PAUSE");
			} else {
				playButtons[i].setText("PLAY");
			}
		}
	}

	// save some frequently used objects (buttons, lists)
	private void initObjects() {
		lv_library = (ListView) findViewById(R.id.listViewLibrary);
		lv_playlist = (ListView) findViewById(R.id.listViewPlaylist);
		editFilterLibrary = (EditText) findViewById(R.id.editTextFilterLibrary);
		editFilterPlaylist = (EditText) findViewById(R.id.editTextFilterPlaylist);
		// init backbutton
		btnBack = (Button) findViewById(R.id.btnBack);
		btnBack.setVisibility(View.INVISIBLE);

		btnFullAlbum = (Button) findViewById(R.id.btnFullAlbum);
		btnFullAlbum.setVisibility(View.INVISIBLE);
	}

	private void initRooms() {
		LinearLayout l = (LinearLayout) findViewById(R.id.tabRooms);
		for (int i = 0; i < sys.instances.size(); i++) {
			LinearLayout line = new LinearLayout(l.getContext());
			line.setOrientation(line.HORIZONTAL);
			TextView t = new TextView(line.getContext());
			t.setText(sys.instances.get(i).name);
			line.addView(t);
			for (int j = 0; j < sys.instances.get(i).outputs.size(); j++) {
				ToggleButton b = new ToggleButton(line.getContext());
				b.setTextOff(sys.instances.get(i).outputs.get(j).name);
				b.setTextOn(sys.instances.get(i).outputs.get(j).name);
				b.setChecked(sys.instances.get(i).outputs.get(j).enabled);
				b.setTag(sys.instances.get(i).outputs.get(j));
				b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// TODO Auto-generated method stub
						MPDOutput o = (MPDOutput) buttonView.getTag();
						o.instance.setOutput(o, isChecked);
					}
				});
				line.addView(b);
			}
			l.addView(line);
		}
	}

	private void initGestures() {
		gLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
		if (!gLib.load()) {
			Log.w(TAG, "could not load gesture library");
			finish();
		} else {
			Log.i(TAG, "Loaded gesture library");
			Log.i(TAG, gLib.getGestureEntries().size() + "");
		}
		GestureOverlayView gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestures);
		gestureOverlayView
				.addOnGesturePerformedListener(new GestureOverlayView.OnGesturePerformedListener() {
					public void onGesturePerformed(GestureOverlayView overlay,
							Gesture gesture) {
						// TODO Auto-generated method stub
						ArrayList<Prediction> predictions = gLib
								.recognize(gesture);
						Log.i("GESS", "TURE");
						// one prediction needed

						if (predictions.size() > 0) {
							Prediction prediction = predictions.get(0);
							// checking prediction
							if (prediction.score > 0.9) {
								// and action
								if (prediction.name.equals("left")) {
									onBtnBackClick(null);
								}
								if(prediction.name.equals("pause")){
									currentInstance.pause();
								}
								if(prediction.name.equals("next")){
									currentInstance.next();
								}
								
							}							
						}
					}
				});
		gestureOverlayView.setGestureStrokeType(GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE);
		
	}

	private void initTextFilter() {
		editFilterLibrary.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				Log.i("TEXTFILTER", s.toString());
				Filter f = adp_library.getFilter();

				f.filter(s.toString());
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}
		});
	}

	private void initListViewPlaylist() {
		// we start at the artist list
		currentPlaylistInstance = currentInstance;
		// lv_arr holds the current data for the lv (artists, albums or songs)
		lv_arr_playlist = new ArrayList<MPDEntity>();
		// init data adapter
		adp_playlist = new SimpleFilterableAdapter<MPDEntity>(
				MultiRoomMPD.this, android.R.layout.simple_list_item_1,
				lv_arr_playlist);
		lv_playlist.setAdapter(adp_playlist);
		// show the artist list
		showPlaylist();

		Spinner playlistInstanceSpinner = (Spinner) findViewById(R.id.spinnerPlaylistInstances);

		ArrayAdapter<MPDInstance> adp = new ArrayAdapter<MPDInstance>(
				MultiRoomMPD.this, android.R.layout.simple_spinner_item,
				sys.instances);
		adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		playlistInstanceSpinner.setAdapter(adp);
		playlistInstanceSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parentView,
							View selectedItemView, int position, long id) {
						MPDInstance inst = (MPDInstance) parentView
								.getItemAtPosition(position);
						Log.i("SPINNERSELECT", inst.name);
						currentPlaylistInstance = inst;
						inst.updateStatus();
						showPlaylist();
						// TODO Auto-generated method stub

					}

					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});
		// on playlistitem click
		lv_playlist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				MPDPlaylistSong song = (MPDPlaylistSong) arg0
						.getItemAtPosition(position);
				currentPlaylistInstance.play(song);
			}
		});
		// playlist changed listeners
		for (int i = 0; i < sys.instances.size(); i++) {
			sys.instances.get(i).setOnPlaylistChangedListener(
					new MPDInstance.OnPlaylistChangedListener() {

						public void onPlaylistChanged(MPDInstance instance) {
							// TODO Auto-generated method stub
							Log.i("PLAYLISTEVENT", "Fire!");
							instance.updatePlaylist();
							showPlaylist();
						}
					});
		}
	}

	private AdapterContextMenuInfo lastMenuInfo = null;

	private void initListViewLibrary() {
		// we start at the artist list
		currentView = "artists";
		// lv_arr holds the current data for the lv (artists, albums or songs)
		lv_arr_library = new ArrayList<MPDEntity>();
		// init data adapter
		adp_library = new SimpleFilterableAdapter<MPDEntity>(MultiRoomMPD.this,
				android.R.layout.simple_list_item_1, lv_arr_library);
		lv_library.setAdapter(adp_library);
		// show the artist list
		showArtists();

		// initialize the context menus for the artist, albums and song-views
		registerForContextMenu(lv_library);
		lv_library
				.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						if (currentView.equals("artists")) {
							AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
							lastMenuInfo = info;
							menu.setHeaderTitle("Titel");
							// String[] menuItems =
							// getResources().getStringArray(R.array.menu);
							menu.add(0, C.contextArtist_Add, Menu.NONE, "Add");
							menu.add(0, C.contextArtist_Replace, Menu.NONE,
									"Replace");
							SubMenu subAddTo = menu.addSubMenu("Add to...");
							for (int i = 0; i < sys.instances.size(); i++) {
								subAddTo.add(C.contextArtistSubMenu_AddTo, i,
										i, sys.instances.get(i).name);
							}
							SubMenu subReplaceTo = menu
									.addSubMenu("Replace to...");
							for (int i = 0; i < sys.instances.size(); i++) {
								subReplaceTo.add(
										C.contextArtistSubMenu_ReplaceTo, i, i,
										sys.instances.get(i).name);
							}
						}
						if (currentView.equals("albums")) {
							AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
							lastMenuInfo = info;
							menu.setHeaderTitle("Titel");
							// String[] menuItems =
							// getResources().getStringArray(R.array.menu);
							menu.add(0, C.contextArtist_Add, Menu.NONE, "Add");
							menu.add(0, C.contextArtist_Replace, Menu.NONE,
									"Replace");
							SubMenu subAddTo = menu.addSubMenu("Add to...");
							for (int i = 0; i < sys.instances.size(); i++) {
								subAddTo.add(C.contextArtistSubMenu_AddTo, i,
										i, sys.instances.get(i).name);
							}
							SubMenu subReplaceTo = menu
									.addSubMenu("Replace to...");
							for (int i = 0; i < sys.instances.size(); i++) {
								subReplaceTo.add(
										C.contextArtistSubMenu_ReplaceTo, i, i,
										sys.instances.get(i).name);
							}
						}
					}
				});
		// set onitemclick event:
		lv_library.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				// TODO Auto-generated method stub
				onListViewItemClick(arg0, arg1, position, arg3);
			}
		});
	}

	/*
	 * Events
	 */
	// Start http stream
	public void onBtnStreamClick(View v) {
		new Thread(new Runnable() {
			public void run() {
				try {
					if (mp == null) {
						mp = new MediaPlayer();
					}
					if(mp.isPlaying()){
						mp.stop();
						mp=null;
						return;
					}
					mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						
						public void onPrepared(MediaPlayer mp) {
							// TODO Auto-generated method stub
							Toast.makeText(MultiRoomMPD.this,"Stream started", Toast.LENGTH_SHORT).show();
						}
					});
					mp.reset();
					mp.setDataSource("http://home.avscripting.nl:8000");
					mp.prepare();
					mp.start();

				} catch (Exception e) {
					Log.i("MPDStream", e.toString());
				}
			}
		}).start();
	}

	// Back button clicked (or gestured)
	public void onBtnBackClick(View v) {
		// Go back to previous view, depending on current view:
		if (currentView == "artists") {
			// clear filter if applicable
			Filter f = adp_library.getFilter();
			f.filter("");
			artistFilter = "";
			this.editFilterLibrary.setText("");
		}
		if (currentView == "albums") {
			// back to all artists (with filter?)
			currentView = "artists";
			showArtists();
			lv_library.postDelayed(new Runnable() {
				public void run() {
					// lv.onRestoreInstanceState(stateArtists);
					if (!artistFilter.equals("")) {
						adp_library.getFilter().filter(artistFilter);
					}
					lv_library.setSelection(lvArtistPos);
					// lv.smoothScrollToPosition(t);
					Log.i("MPDBACK", "Restore ArtistState: " + lvArtistPos
							+ ", " + artistFilter);
				}
			}, 100);

			btnBack.setVisibility(View.INVISIBLE);
		}
		if (currentView == "songs") {
			currentView = "albums";
			showAlbums(currentArtist);
			btnFullAlbum.setVisibility(View.INVISIBLE);
		}
	}

	// ListView-item clicked:
	public void onListViewItemClick(AdapterView<?> arg0, View arg1,
			int position, long arg3) {
		// TODO Auto-generated method stub

		Log.i("CLICK", "Done");
		if (currentView.equals("artists")) {
			lvArtistPos = position;
			artistFilter = editFilterLibrary.getText().toString();
			btnBack.setVisibility(View.VISIBLE);
			String artist = ((MPDArtist) lv_library.getItemAtPosition(position))
					.toString(true);
			Log.i("LV", artist);
			showAlbums(artist);
			currentArtist = artist;
			Log.i("MPDLib", "Found albums: " + lv_arr_library.size());

			currentView = "albums";
			hideKeyboard();

			return;
		}
		if (currentView.equals("albums")) {
			lvAlbumPos = position;
			Log.i("LV", "Show songs " + currentArtist);
			albumFilter = editFilterLibrary.getText().toString();
			btnBack.setVisibility(View.VISIBLE);
			String album = ((MPDAlbum) lv_library.getItemAtPosition(position))
					.toString(true);
			showSongs(currentArtist, album);
			currentView = "songs";
			currentAlbum = album;
			return;
		}
		if (currentView.equals("songs")) {
			MPDSong songClicked = (MPDSong) lv_library
					.getItemAtPosition(position);
			Log.i("MPDSong", songClicked.file);
		}
	}

	// Context menu item clicked:

	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		if (info == null) {
			// Grab it from the member placeholder. If this is still null,it's a
			// bug (?)
			info = lastMenuInfo;
		}

		int menuItemIndex = item.getItemId();

		String menuItemName = item.toString();
		if (currentView.equals("artists")) {
			MPDArtist selectedArtist = (MPDArtist) lv_library
					.getItemAtPosition(info.position);
			if (item.getGroupId() == 0) {
				// options:
				switch (item.getItemId()) {
				case C.contextArtist_Add:
					currentInstance
							.Add(lib.getSongs(selectedArtist.name, null));
					break;
				case C.contextArtist_Replace:
					currentInstance.clear();
					currentInstance
							.Add(lib.getSongs(selectedArtist.name, null));
					currentInstance.play();
					break;
				}
			} else {
				// a submenu item (Add to..., Replace to...) has been clicked
				Log.i("SUBMENUOPTION",
						item.getGroupId() + " " + item.getItemId());
				MPDInstance inst = sys.instances.get(item.getItemId());
				switch (item.getGroupId()) {
				case C.contextArtistSubMenu_AddTo:
					// add to specific instance
					Log.i("SUBMENUOPTION", "Add " + selectedArtist.name
							+ " to " + inst.name);
					inst.Add(lib.getSongs(selectedArtist.name, null));
					break;
				case C.contextArtistSubMenu_ReplaceTo:
					// replace to specific instance
					Log.i("SUBMENUOPTION", "Replace " + selectedArtist.name
							+ " to " + inst.name);
					inst.clear();
					inst.Add(lib.getSongs(selectedArtist.name, null));
					inst.play();
					break;
				}
			}
		}
		if (currentView.equals("albums")) {
			MPDAlbum selectedAlbum = (MPDAlbum) lv_library
					.getItemAtPosition(info.position);
			if (item.getGroupId() == 0) {
				// options:
				switch (item.getItemId()) {
				case C.contextArtist_Add:
					currentInstance.Add(lib.getSongs(currentArtist,
							selectedAlbum.name));
					break;
				case C.contextArtist_Replace:
					currentInstance.clear();
					currentInstance.Add(lib.getSongs(currentArtist,
							selectedAlbum.name));
					currentInstance.play();
					break;
				}
			} else {
				// a submenu item (Add to..., Replace to...) has been clicked
				Log.i("SUBMENUOPTION",
						item.getGroupId() + " " + item.getItemId());
				MPDInstance inst = sys.instances.get(item.getItemId());
				switch (item.getGroupId()) {
				case C.contextArtistSubMenu_AddTo:
					// add to specific instance
					Log.i("SUBMENUOPTION", "Add " + currentArtist + ", "
							+ selectedAlbum + " to " + inst.name);
					inst.Add(lib.getSongs(currentArtist, selectedAlbum.name));
					break;
				case C.contextArtistSubMenu_ReplaceTo:
					// replace to specific instance
					Log.i("SUBMENUOPTION", "Replace " + currentArtist + ", "
							+ selectedAlbum + " to " + inst.name);
					inst.clear();
					inst.Add(lib.getSongs(currentArtist, selectedAlbum.name));
					inst.play();
					break;
				}
			}
		}
		return true;
	}

	public void onContextMenuClosed(Menu menu) {
		// We don't need it anymore
		Log.i("CONTEXTMENU", "CLOSED");
		lastMenuInfo = null;
	}

	public void showKeyboard() {
		InputMethodManager imm = (InputMethodManager) MultiRoomMPD.this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		}
	}

	public void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) MultiRoomMPD.this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(
					((EditText) findViewById(R.id.editTextFilterPlaylist))
							.getWindowToken(), 0);
		}
	}

	public void showPlaylist() {
		lv_arr_playlist.clear();
		lv_arr_playlist.addAll(currentPlaylistInstance.playlist);
		adp_playlist.notifyDataSetChanged();
	}

	public void showArtists() {
		showArtists(null);
	}

	public void showArtists(String filter) {
		lv_arr_library.clear();
		lv_arr_library.addAll(lib.getArtists());
		// lv.setAdapter(adp);
		adp_library.notifyDataSetChanged();
	}

	public void showAlbums(String artist) {

		lv_arr_library.clear();
		Filter f = adp_library.getFilter();

		f.filter("");
		lv_arr_library.addAll(lib.getAlbums(artist));
		adp_library.notifyDataSetChanged();

		// lv.setAdapter(adp);

	}

	public void showSongs(String artist, String album) {
		lv_arr_library.clear();
		lv_arr_library.addAll(lib.getSongs(artist, album));

		// lv.setAdapter(adp);
		adp_library.notifyDataSetChanged();
		if (lv_arr_library.size() > 0) {
			MPDSong s = (MPDSong) lv_arr_library.get(0);
			if (s.compilation) {
				btnFullAlbum.setVisibility(View.VISIBLE);
			} else {
				btnFullAlbum.setVisibility(View.INVISIBLE);
			}
		} else {
			btnFullAlbum.setVisibility(View.INVISIBLE);
		}
	}

}