package com.icechen1.bathroomfinder;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

 public class PreferencesActivity extends SherlockPreferenceActivity  {
	 
    //Called when the activity is first created. 
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_preferences);
        getSupportActionBar().setHomeButtonEnabled(true);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        final Preference pref = (Preference) findPreference("sign_out");        
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
        		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = settings.edit();

                editor.putString("accessToken", "");
                editor.putString("accessSecret", "");

                editor.putString("requestToken", "");
                editor.putString("requestSecret", "");

                editor.commit();    
                
                Toast.makeText(getBaseContext(), "Logged out", Toast.LENGTH_SHORT).show();
                return true; 
            }
        });
        
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                // app icon in action bar clicked; go home
            	finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onPause(){
        Intent goback = new Intent(this, MainActivity_.class);
        goback.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(goback);
    	super.onPause();
    }

    
}