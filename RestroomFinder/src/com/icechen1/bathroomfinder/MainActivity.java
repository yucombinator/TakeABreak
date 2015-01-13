
package com.icechen1.bathroomfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.GeoPoint;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.rest.RestService;
import com.icechen1.bathroomfinder.OAuthFragmentDialog.OAuthCallback;
import com.icechen1.bathroomfinder.ViewMapFragment.onCameraListener;
import com.icechen1.bathroomfinder.api.OSMNode;
import com.icechen1.bathroomfinder.api.OSMWrapperAPI;
import com.icechen1.bathroomfinder.rest.RestClient;
import com.tapfortap.TapForTap;

@EActivity
public class MainActivity
    extends SherlockFragmentActivity
    implements onCameraListener, TabListener, AdListener, OAuthCallback
{
	/*TODO 
	 * ADD CREDITS FOR GMAPS AND OSM
	 * mark toilet using current location
	 * Streetview
	 * 
	 * Changelog
	 * Search
	 * New Icon
	 * Add Nodes
	 */
	

	public static String TAG = "BathroomFinder";
	static ArrayList <OSMNode> allTapItem = new ArrayList<OSMNode>();
	static ArrayList <OSMNode> allToiletItem = new ArrayList<OSMNode>();
	static ArrayList <OSMNode> allFoodItem = new ArrayList<OSMNode>();
	public boolean querying;
    Fragment mMapFragment;
    Fragment mListFragment;
    AdView adview;
	private ArrayList<Marker> searchMarkers;
	protected AsyncTask<String, Void, List<Address>> queryTask;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        if (null != searchView )
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);   
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() 
        {
            

			public boolean onQueryTextChange(String newText) 
            {

                return true;
            }

            public boolean onQueryTextSubmit(String query) 
            {
            	if(querying){
            		queryTask.cancel(true);
            	}
            	if(!isOnline()){
            		return false;
            	}
                if(query!=null && !query.equals("")){
                    queryTask = new GeocoderTask().execute(query);
                    querying = true;
                    setProgressBarIndeterminateVisibility(true); 
                }
                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.credits) {
        startActivity(new Intent(this, LegalNoticesActivity.class));

        return(true);
      }else if(item.getItemId() == R.id.satellite){
    	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
  		mMapFragment.changeView(ViewMapFragment.MapType.Satellite);
    	  
      }else if(item.getItemId() == R.id.hybrid){
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
    		mMapFragment.changeView(ViewMapFragment.MapType.Hybrid);
      }else if(item.getItemId() == R.id.map_only){
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
    		mMapFragment.changeView(ViewMapFragment.MapType.Map);
      }else if(item.getItemId() == R.id.terrain){
      	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
    		mMapFragment.changeView(ViewMapFragment.MapType.Terrain);
      }else if(item.getItemId() == R.id.add_new){
    	  if(isOnline()){
    	   	    if(PreferenceManager.getDefaultSharedPreferences(this).getString("accessToken", "").equals("")){
        	        FragmentManager fm = getSupportFragmentManager();
        	        OAuthFragmentDialog oAuthFragmentDialog = OAuthFragmentDialog.newInstance();
        	        oAuthFragmentDialog.show(fm, "fragment_login");
        	    }else{
        	    	//TODO verify token
        	    	if (adview != null) {
        	    		//adview.destroy();
        	        	adview.setVisibility(View.GONE);
        	    	}
        	    	addNewNode();
        	    }
    	  }
 

      }else	if (item.getItemId() == R.id.menu_settings) {
			// launch prefs
            Intent gopref = new Intent(this, PreferencesActivity.class);
			gopref.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(gopref);
			finish();
			return true;
		}

      return super.onOptionsItemSelected(item);
    }
    
    @UiThread
    public void addNewNode(){
    	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
  		mMapFragment.addNewNode();
    }
    
    public void showAddNodeFragmentDialog(View v){
    	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
  		mMapFragment.showAddNodeFragmentDialog();
    }
    @Override
    public void onResume(){
    	super.onResume();
        if (checkGooglePlayServicesAvailable()) {

          }
        isOnline();
        //TODO Make the ad reappear after resume
//    	if (adview != null) {
//    		//adview.destroy();
//        	adview.setVisibility(View.GONE);
//    	}
//    	ImageButton imgbtn = (ImageButton) findViewById(R.id.closeadbtn);
//    	imgbtn.setVisibility(View.GONE);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);  
        
        
     // Getting status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if(status==ConnectionResult.SUCCESS)
        {

        }
        else{

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
            return;
        }
        
        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        if(savedInstanceState != null){
        	allTapItem = (ArrayList<OSMNode>) savedInstanceState.get("allTapItem");
        	allToiletItem = (ArrayList<OSMNode>) savedInstanceState.get("allToiletItem");
        	allFoodItem = (ArrayList<OSMNode>) savedInstanceState.get("allFoodItem");
        }
        //TODO Semi-Transparent Action Bar
       // requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);       
       // getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        
        setContentView(R.layout.activity_main);
    	configureActionBar();
    	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		mMapFragment = getSupportFragmentManager().findFragmentByTag("map");
		if (mMapFragment == null) {
			// If not, instantiate and add it to the activity
			mMapFragment = new ViewMapFragment_();
			ft.add(R.id.container, mMapFragment, "map").commit();
		} else {
			// If it exists, simply attach it in order to show it
			ft.show(mMapFragment).commit();
		}
		TapForTap.initialize(this, "ef3209aec5ac8e0eb9682b8890b20a78");
		adview = (AdView) findViewById(R.id.adView);
		adview.setAdListener(this);
    	ImageButton imgbtn = (ImageButton) findViewById(R.id.closeadbtn);
    	imgbtn.setVisibility(View.GONE);
    	
        // Initiate a generic request to load it with an ad
        AdRequest request = new AdRequest();
        adview.loadAd(request);
        
        //Search markers
        searchMarkers = new ArrayList<Marker>();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState){
    	outState.putParcelableArrayList("allTapItem", allTapItem);
    	outState.putParcelableArrayList("allToiletItem", allToiletItem);
    	outState.putParcelableArrayList("allFoodItem", allFoodItem);
    	super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop(){
    	super.onStop();

    }
    
    @Override
    public void onDestroy(){
    	if (adview != null) {
    		adview.destroy();
    	}
    	super.onDestroy();
    }

    /**
     * Forward the Cancel button trigger to the fragment
     * @param v unused
     */
    public void cancel_node_add(View v){
    	cancel_node_add_handler();
    }
    
    @UiThread
    public void cancel_node_add_handler(){
    	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
  		mMapFragment.hideNewNodeOverlay();
    }
    
    public void closeAd(View v){
    	if (adview != null) {
    		//adview.destroy();
        	adview.setVisibility(View.GONE);
    	}
    	ImageButton imgbtn = (ImageButton) findViewById(R.id.closeadbtn);
    	imgbtn.setVisibility(View.GONE);
    }


    private void configureActionBar() {
/*        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab tab = actionBar.newTab()
                .setText("Map") //TODO String
                .setTabListener(this);
        actionBar.addTab(tab);

        tab = actionBar.newTab()
            .setText("List") //TODO String
            .setTabListener(this);
        actionBar.addTab(tab);*/
        
    }
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		
		if(tab.getPosition() == 0){
			// Check if the fragment is already initialized
			mMapFragment = getSupportFragmentManager().findFragmentByTag("map");
			if (mMapFragment == null) {
				// If not, instantiate and add it to the activity
				mMapFragment = new ViewMapFragment_();
				ft.add(R.id.container, mMapFragment, "map");
			} else {
				// If it exists, simply attach it in order to show it
				ft.show(mMapFragment);
			}
		}
		if(tab.getPosition() == 1){
			// Check if the fragment is already initialized
			mMapFragment = getSupportFragmentManager().findFragmentByTag("list");
			if (mListFragment == null) {
				// If not, instantiate and add it to the activity
			//	mListFragment = new RestroomListFragment_();
				ft.add(R.id.container, mListFragment, "list");
			} else {
				// If it exists, simply attach it in order to show it
				ft.show(mListFragment);
			}
		}

	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		if(tab.getPosition() == 0){
			if (mMapFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.hide(mMapFragment);
			}
		}
		if(tab.getPosition() == 1){
			if (mListFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.hide(mListFragment);
			}
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCameraLocationChange(LatLng loc) {
        setProgressBarIndeterminateVisibility(true); 
		updatePOIs(loc);
	}
	@Background
	public void updatePOIs(LatLng loc){
		double lat = loc.latitude;
		double lon = loc.longitude;
		Log.d(TAG,"Camera center: Lat: " + lat + " Long: " + lon);
		ViewMapFragment frag = (ViewMapFragment) mMapFragment;
		List<OSMNode> NodesList = null;
		boolean fountain = false;
		boolean toilet = false;
		boolean rest = false;
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_fountain", true)){
			fountain = true;
		}
		
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_toilet", true)){
			toilet = true;
		}
		
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_rest", false)){
			rest = true;
		}
		
		try {
			NodesList = OSMWrapperAPI.fetch(lon,lat,0.005,fountain,toilet,rest);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<OSMNode> geolistIterator;
		try {
		geolistIterator = NodesList.iterator(); 
		}
		//If the list is empty...
		catch (NullPointerException e){
			stopProgressbar();
			return;
		}

		try{
			while (geolistIterator.hasNext()) {
				//Log.d(MapActivity.tag,"Adding GeoPoint");
				OSMNode osmNode = geolistIterator.next();
				Map<String, String> tag = osmNode.getTags();

				//Remove duplicates
				//TODO: move this into a method
				boolean tapExists = false;
				boolean toiletExists = false;
				boolean foodExists = false;
				
				Iterator<OSMNode> tapItemIterator = allTapItem.iterator(); 
				while(tapItemIterator.hasNext()){
					OSMNode item = tapItemIterator.next();
					if(item.getId().equals(osmNode.getId())){
						tapExists = true;
					//	Log.d(TAG, item.getId() + ": Node Exists");
					}
					tapItemIterator.remove();
				}

				Iterator<OSMNode> toiletItemIterator = allToiletItem.iterator(); 
				while(toiletItemIterator.hasNext()){
					OSMNode item = toiletItemIterator.next();
					if(item.getId().equals(osmNode.getId())){
						toiletExists = true;
					//	Log.d(TAG, item.getId() + ": Node Exists");
					}
					toiletItemIterator.remove();
				}
				
				Iterator<OSMNode> foodItemIterator = allFoodItem.iterator(); 
				while(foodItemIterator.hasNext()){
					OSMNode item = foodItemIterator.next();
					if(item.getId().equals(osmNode.getId())){
						foodExists = true;
					//	Log.d(TAG, item.getId() + ": Node Exists");
					}
					foodItemIterator.remove();
				}


				if(tag.containsValue("drinking_water") && !tapExists){
					allTapItem.add(osmNode);	
				}
				else if(tag.containsValue("toilets")&& !toiletExists){
					allToiletItem.add(osmNode);	
				}
				else if((tag.containsValue("fast_food") || 
						tag.containsValue("cafe") || 
						tag.containsValue("food_court") ||
					    tag.containsValue("restaurant")) && !foodExists){
					allFoodItem.add(osmNode);	
				}
				frag.addMarker(osmNode);
				geolistIterator.remove();
			}
			Log.d(TAG, "Tap Overlay Size: " + allTapItem.size() + "\nToilet Overlay Size: " + allToiletItem.size() + "\nFood Overlay Size: " + allFoodItem.size());
		}
		catch(ConcurrentModificationException e){
			Log.e(TAG, "ConcurrentModificationException has occured " + e.getStackTrace());
		}
		stopProgressbar();

	}
	@UiThread
	public void stopProgressbar() {
	    setProgressBarIndeterminateVisibility(false); 
		
	}

	@Override
	public void onMyLocationChange(Location location) {
		Log.d(TAG, "onMyLocationChange()");

	}
	
	//Add item button
	public void addBtn_onClick(){
		// Instantiate an AlertDialog.Builder with its constructor for the Add item button
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		// 2. Chain together various setter methods to set the dialog characteristics
		builder.setMessage(R.string.text_add)
		       .setTitle(R.string.title_add);
		// Add the buttons
		builder.setPositiveButton(R.string.get_app_add, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   String appName = "de.blau.android";
		        	   try {
		        		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
		        		} catch (android.content.ActivityNotFoundException anfe) {
		        		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
		        		}
		           }
		       });
		builder.setNeutralButton(R.string.more_info_add, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
	        		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://wiki.openstreetmap.org/wiki/Editing")));		           }
		       });
		builder.setNegativeButton(R.string.close_add, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               // User cancelled the dialog
	           }
	       });
		// Create the AlertDialog
		AlertDialog dialog = builder.create();
        dialog.show();
	}
	
	  /** Check that Google Play services APK is installed and up to date. */
	  private boolean checkGooglePlayServicesAvailable() {
	    final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
	      showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
	      return false;
	    }
	    return true;
	  }
	  static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
	  void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
		    runOnUiThread(new Runnable() {
		      public void run() {
		        Dialog dialog =
		            GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, MainActivity.this,
		                REQUEST_GOOGLE_PLAY_SERVICES);
		        dialog.show();
		      }
		    });
		  }
	  
	  //Check for network status
	  public boolean isOnline() {
		    ConnectivityManager cm =
		        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo netInfo = cm.getActiveNetworkInfo();
		    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
		        return true;
		    }
			// Instantiate an AlertDialog.Builder with its constructor for the Add item button
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			// 2. Chain together various setter methods to set the dialog characteristics
			builder.setMessage(R.string.no_net_info)
			       .setTitle(R.string.no_net)
			       .setNegativeButton(R.string.close_add, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
			// Create the AlertDialog
			AlertDialog dialog = builder.create();
	        dialog.show();
		    return false;
		}

	@Override
	public void onDismissScreen(Ad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLeaveApplication(Ad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPresentScreen(Ad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReceiveAd(Ad arg0) {
    	ImageButton imgbtn = (ImageButton) findViewById(R.id.closeadbtn);
    	imgbtn.setVisibility(View.VISIBLE);
	}

	@Override
	public void onCancel() {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Login success for map editing
	 */
	@Override
	public void onSuccess() {
    	//TODO verify token
		addNewNode();
	}
	// An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<String, Void, List<Address>>{
 
        @Override
        protected List<Address> doInBackground(String... locationName) {
            // Creating an instance of Geocoder class
            Geocoder geocoder = new Geocoder(getBaseContext());
            List<Address> addresses = null;
 
            try {
                // Getting a maximum of 3 Address that matches the input text
                addresses = geocoder.getFromLocationName(locationName[0], 3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return addresses;
        }
 
        @Override
        protected void onPostExecute(List<Address> addresses) {
 
            if(addresses==null || addresses.size()==0){
                Toast.makeText(getBaseContext(), "No Location found", Toast.LENGTH_SHORT).show();
                setProgressBarIndeterminateVisibility(false); 

                return;
            }
 
          	ViewMapFragment mMapFragment = (ViewMapFragment) getSupportFragmentManager().findFragmentByTag("map");
          	
          	if(!searchMarkers.isEmpty()){
                // Clears all the existing markers on the map
                for(Marker m:searchMarkers){
                	m.remove();
                }
                searchMarkers.clear();
          	}
            
            // Adding Markers on Google Map for each matching address
            for(int i=0;i<addresses.size();i++){
 
                Address address = (Address) addresses.get(i);
 
                // Creating an instance of GeoPoint, to display in Google Map
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
 
                String addressText = String.format("%s, %s",
                address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                address.getCountryName());
 
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(addressText);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
 
              	searchMarkers.add(mMapFragment.mMap.addMarker(markerOptions));
 
                // Locate the first location
                if(i==0)
                	mMapFragment.mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
            querying = false;
            setProgressBarIndeterminateVisibility(false); 
        }
    }
}
