package org.seadroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.seadroid.R;

public class PersistenceDemoActivity extends Activity {
	
	// Following are for demonstrating use of a state object to store state
	// across configuration changes.
	
	// If the activity has a lot of state that's expensive to reconstruct or
	// re-fetch, or only needs to persist for the current session, save it in
	// a custom class to hand off to the system when there's a configuration
	// change (e.g. orientation change) that requires tearing down and
	// restarting the activity and UI.
	
	/** State that should persist for this application run, but that doesn't
	 *  need to be saved for later runs. */
	public class State {
		/** Place to preserve our per-session info. */
		public boolean username_changed_this_session = false;
	}
	
	/** Was username changed during this session? */
	private boolean username_changed_this_session = false;
	
	// Following are for demonstrating use of preferences:
	
	// A few things to store in preferences.  Keys for preferences are defined
	// here, not in resources, as they are not intended to be changed.
	
	/** Key for storing username. */
	private static final String USERNAME_KEY = "username";
	/** Key for storing password. */
	private static final String PASSWORD_KEY = "password";
	/** Local copy of saved username */
	private String username;
	/** Local copy of saved password */
	private String password;
	
	/** Store supplied username into persistent storage and local variable. */
	private void saveUsername(String newname) {
		username = newname;
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor prefs_editor = prefs.edit();
		prefs_editor.putString(USERNAME_KEY, username);
		prefs_editor.commit();
	}
	
	/** Read saved username from persistent storage into local variable.
	 *  Do this just once on first call. Later, use local value. */
	private String loadUsername() {
		if (username == null) {
		    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		    username = prefs.getString(USERNAME_KEY, "");
		}
		return username;
	}
	
	/** Store supplied password into persistent storage and local variable. */
	private void savePassword(String newpassword) {
		password = newpassword;
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor prefs_editor = prefs.edit();
		prefs_editor.putString(PASSWORD_KEY, password);
		prefs_editor.commit();
	}
	
	/** Read saved password from persistent storage into local variable.
	 *  Do this just once on first call. Later, use local value. */
	private String loadPassword() {
		if (password == null) {
		    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		    password = prefs.getString(USERNAME_KEY, "");
		}
		return password;
	}
	
	// Following are for demonstrating use of internal storage:
	
	/** Name of file for storing status log. */
	private static final String LOG_FILENAME = "log";
	
	/** Output stream set up to append to the log. */
	private FileOutputStream logOut = null;
	
	/** Record whether we got restarted or created. */
	protected String startPath = null;
	
	/** Read in the entire log and return it as a String.
	 *  A better method would tail the log... */
	private String readLog() {
		File logFile = new File(getFilesDir(), LOG_FILENAME);
		int logSize = 0;
		String logContents;
		
		if (logFile.exists())
		   logSize = (int) logFile.length();
		
		if (logSize != 0)
		{
			int actualLogSize = 0;
			
			try {
				FileInputStream logIn = openFileInput(LOG_FILENAME);
				byte[] buffer = new byte[logSize];
				actualLogSize = logIn.read(buffer, 0, logSize);
				logIn.close();
				logContents = new String(buffer, 0, actualLogSize);
			}
			catch (Exception e) {
				// Act as though the log is empty.
				logContents = new String();
			}
		} else {
			logContents = new String();
		}
		
		return logContents;
	}
	
	/** Create the status log if needed and open it for appending.  If we have
	 *  a log already, read it into the log view. */
    private void openLog() {
        // If we already have a log, read it in
        String logContents = readLog();
        updateLog(logContents, false);
        // Open the log for append, creating it if needed.  (Do this after
        // attempting to read -- don't need to read it if it's empty.)
    	try {
    		logOut = openFileOutput(LOG_FILENAME, Context.MODE_APPEND);
    	}
    	catch (Exception e) {
    		logOut = null;
    		updateLog("\nopenFileOutput failed in openLog.", false);
    	}
		updateLog("\nSuccessfully opened & read log in openLog.", true);
    }
    
	/** Add a message to the status log.  This appears in the status box, and
	 *  is saved to a file so it can be restored on restart.
	 *  The caller should add a return to the message if needed.
	 *  
	 *  @param msg The message to add to the status log.
	 *  @param writeToLogFile If true, message will also be written out to the
	 *  log file. */
	private void updateLog(String msg, boolean writeToLogFile) {
        TextView logView = (TextView)findViewById(R.id.log);
        logView.append(msg);
        if (writeToLogFile && (logOut != null)) {
        	try {
                logOut.write(msg.getBytes());
        	}
        	catch (Exception e) {
        		logOut = null;
                logView.append("\nupdateLog failed to write log:");
                logView.append("\n" + e.toString());
        	}
        }
	}
	
	/** Clear the log view and optionally delete the log file. */
	private void clearLog(boolean deleteLogFile) {
		TextView logView = (TextView)findViewById(R.id.log);
		logView.setText("");
		if (deleteLogFile) {
			deleteFile(LOG_FILENAME);
		}
	}
	
	/** Close the log in preparation for (possibly) shutting down. */
	private void closeLog() {
		clearLog(false);
		try {
		    logOut.close();
		}
		catch (Exception e) {}
		logOut = null;
	}
	
    // Save and restore state on system events.
    
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Remove title and notification bars to save space.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        
        // We'll log whether we got created or restarted -- see onStart.
        startPath = "onCreate";

        loadUsername();
        loadPassword();
        
        // Restore saved state.
        final State state = (State) getLastNonConfigurationInstance();
        if (state != null) {
        	username_changed_this_session = state.username_changed_this_session;
        }

        updateHello();
    }
        
    /** Called when the configuration changes.  Save appropriate state. */
    @Override
    public Object onRetainNonConfigurationInstance() {
        final State state = new State();
        state.username_changed_this_session = username_changed_this_session;
        return state;
    }
    
    /** Called when the activity is stopped, e.g. by the user going to the
     *  home screen. */
    protected void onStop() {
    	super.onStop();
    	updateLog("\nonStop received.", true);
    	// Close the log file and clear the log view.
    	closeLog();
    }
    
    /** Called when the activity was stopped and the user has returned to it,
     *  but *not* when the activity is created. */
    protected void onRestart() {
    	super.onRestart();
    	// Save a message to write to the log when it's open, to distinguish
    	// this path from onCreate.  Yes, could have opened the log in both
    	// onCreate and here, but doing that in onStart allows it to be done
    	// in one place -- writing these messages is just for show.
    	startPath = "onRestart";
    }
    
    /** Undo what we did in onStop.  The activity still retains its state, so
     *  we don't need to read in info that we didn't get rid of in onStop or
     *  onPause.  This is called after onCreate so we don't need to repeat
     *  work there.  Note onRestart is *not* called after onCreate. */
    protected void onStart() {
    	super.onStart();
        // Read in the status log if it exists, and open it for append.
        openLog();
        if (startPath != null) {
            updateLog("\n" + startPath + " received.", true);
            startPath = null;
        }
        updateLog("\nonStart received.", true);
    }
    
	// Miscellaneous UI handling
	
    /** Set the greeting text. */
	private void updateHello() {
		String name = loadUsername() != "" ? loadUsername() :
			                                 getString(R.string.default_username);
        String hello = String.format(getString(R.string.hello), name);
        TextView hello_view = (TextView)findViewById(R.id.hello);
        hello_view.setText(hello);
        
        String did = username_changed_this_session ? getString(R.string.did) :
        	                                         getString(R.string.didnt);
        String changed = String.format(getString(R.string.changed), did);
        updateLog(changed, true);
	}
	
    /** Service Set Username and Password button click by saving username and
     *  password in preferences. */
    public void loginClick(View view) {
    	EditText username_field = (EditText)findViewById(R.id.username);
    	String newusername = username_field.getText().toString();
    	// Note there is a race condition here -- may have a configuration
    	// change between setting this flag and storing the new username.
    	if (!newusername.equals(username)) {
    	    username_changed_this_session = true;
    	}
    	saveUsername(newusername);
    	EditText password_field = (EditText)findViewById(R.id.password);
    	savePassword(password_field.getText().toString());
    	
    	// Set the hello message.
    	updateHello();
    	
    	// Hide the on-screen keyboard. 
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(findViewById(R.id.username).getWindowToken(), 0);
    }
    
    /** Service a Clear Log File click:
     *  Clear the log view and delete the log file. */
    public void clearLogClick(View view) {
    	clearLog(true);
    }
}