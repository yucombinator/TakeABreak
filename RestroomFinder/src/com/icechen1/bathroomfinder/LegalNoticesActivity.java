package com.icechen1.bathroomfinder;

import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class LegalNoticesActivity extends SherlockActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_legal);

    TextView legal=(TextView)findViewById(R.id.legal);
    legal.setText("Data under Open Database License \n© OpenStreetMap contributors\n");
    legal.append(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this));

    getSupportActionBar().setHomeButtonEnabled(true);
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
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
}