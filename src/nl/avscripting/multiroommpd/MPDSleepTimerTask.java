package nl.avscripting.multiroommpd;

import java.util.TimerTask;

import android.util.Log;

public class MPDSleepTimerTask extends TimerTask {
	private MPDInstance instance;
	public MPDSleepTimerTask(MPDInstance _inst) {
        this.instance = _inst;
    }

    @Override
    public void run() {
        // You can do anything you want with param 
    	Log.i("Timer","Finished");
    	instance.pauseSleep();
    }

}
