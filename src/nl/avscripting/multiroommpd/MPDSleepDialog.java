package nl.avscripting.multiroommpd;

import com.example.helloandroid.R;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TimePicker;

public class MPDSleepDialog extends Dialog {
	public MPDInstance instance;
	private TimePicker picker;
	private Button button;
	private SeekBar seekSeconds;
	public int hour=0, minute=0, second=0;
	public MPDSleepDialog(final Context context)
    {
		super(context);
		this.setCancelable(true);
        // This is the layout XML file that describes your Dialog layout
        this.setContentView(R.layout.sleep_dialog);  
         picker = (TimePicker)findViewById(R.id.timePicker1);
        picker.setIs24HourView(true);
        picker.setCurrentHour(0);
        picker.setCurrentMinute(0);
        
        seekSeconds = (SeekBar)findViewById(R.id.seekBarSeconds);
        seekSeconds.setMax(60);
        
        button = (Button) findViewById(R.id.buttonSleepDialog);
        button.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.i("SLEEPY","Clicked");
				int time = hour*3600+minute*60+second;
				instance.setSleepTimer(time*1000);
				dismiss();
			}
		});
        //update time:
        picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {			
			public void onTimeChanged(TimePicker view, int hourOfDay, int newMinute) {
				hour = hourOfDay;
				minute = newMinute;
				updateButton();
			}
		});
        seekSeconds.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				second = seekBar.getProgress();
				updateButton();
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				second = seekBar.getProgress();
				updateButton();
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				second = seekBar.getProgress();
				updateButton();
			}
		});
    }
	public void updateButton() {
		button.setText("Sleeping in: "+hour+":"+minute+":"+second);
	}
}
