package com.icechen1.bathroomfinder;

import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.UiThread;
import com.icechen1.bathroomfinder.ViewMapFragment.MapType;
import com.icechen1.bathroomfinder.api.OSMNode;

@EFragment
public class ViewMapFragment extends SherlockFragment implements LocationSource.OnLocationChangedListener, GoogleMap.OnCameraChangeListener, OnInfoWindowClickListener{

	public Location userLocation;
	SupportMapFragment mMapFragment;
    private onCameraListener mCallback;
    GoogleMap mMap;
    LatLng last_location;
    FragmentTransaction fragmentTransaction;
	private CustomLocationSource customLocationSource;
	private RelativeLayout nodesAddOverlay;
	private Marker nodeAddmarker;
	private boolean ADDING;
	public static Context cxt;
	
    /**
     * An interface to pass data to the host Activity.
     * @author Icechen1
     */
    public interface onCameraListener {
        public void onCameraLocationChange(LatLng loc);
        public void onMyLocationChange(Location location);
    }
    
    public static enum MapType{
    	Satellite, Hybrid, Map, Terrain
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
    	if (savedInstanceState != null){
    		try{
    			Log.d(MainActivity.TAG, "restoring last location");
    			last_location = savedInstanceState.getParcelable("last_location");
    		}catch(Exception e){

    		}
    	}
    	//Load Map Type
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String type;
        cxt = this.getActivity();
        int maptype = GoogleMap.MAP_TYPE_NORMAL;
        try{
        	type = settings.getString("map_type", "normal");
        }catch (Exception e){
        	type = "normal";
        }
	    
        if(type.equals("satellite")){
        	maptype = GoogleMap.MAP_TYPE_SATELLITE;
        }else if (type.equals("normal")){
        	maptype = GoogleMap.MAP_TYPE_NORMAL;
        }else if (type.equals("terrain")){
        	maptype = GoogleMap.MAP_TYPE_TERRAIN;
        }else if (type.equals("hybrid")){
        	maptype = GoogleMap.MAP_TYPE_HYBRID;
        }
	    	    
    	GoogleMapOptions options = new GoogleMapOptions();
    	options.mapType(maptype) //TODO OPTIONS
    	.compassEnabled(true)
    	.rotateGesturesEnabled(true)
    	.scrollGesturesEnabled(true)
    	.tiltGesturesEnabled(true);
    	RelativeLayout RelativeLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_map, container, false);
    	mMapFragment = (SupportMapFragment) getFragmentManager().findFragmentByTag("mapfragment");
    	//create the fragment
    	if (mMapFragment == null){
    		mMapFragment = SupportMapFragment.newInstance(options);
    		fragmentTransaction =
    				getFragmentManager().beginTransaction();
    		fragmentTransaction.add(R.id.map_container, mMapFragment,"mapfragment");
    		fragmentTransaction.commit();
    	}
    	
    	//Overlay for adding nodes
    	nodesAddOverlay = (RelativeLayout) RelativeLayout.findViewById(R.id.node_add_container);
    	nodesAddOverlay.setVisibility(View.GONE);
    	
    	customLocationSource = new CustomLocationSource(getActivity().getApplicationContext());


    	return RelativeLayout; 
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	super.onSaveInstanceState(savedInstanceState);
    	if (mMap != null) {
    		Log.d(MainActivity.TAG, "Saving current location");
    		savedInstanceState.putParcelable("last_location", mMap.getCameraPosition().target);
		 }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setRetainInstance(true);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (onCameraListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onCameraListener");
        }
    }
	@Override
	public void onDetach(){
		super.onDetach();
		mMapFragment = (SupportMapFragment) getFragmentManager().findFragmentByTag("mapfragment");
	}
	
	@Override
	public void onResume(){
		super.onResume();
		Log.d(MainActivity.TAG, "onResume()");
		customLocationSource.getBestAvailableProvider();
		mMap = ((SupportMapFragment) getFragmentManager().findFragmentByTag("mapfragment")).getMap();
		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			// The Map is verified. It is now safe to manipulate the map.
			mMap.setMyLocationEnabled(true);
			mMap.setOnCameraChangeListener(this);
            mMap.setLocationSource(customLocationSource);
			mMap.setInfoWindowAdapter(new PopupAdapter(getActivity().getLayoutInflater()));
			mMap.setOnInfoWindowClickListener(this);
			
			Log.d(MainActivity.TAG, "Setting mMap Options");

			//RESTORE ALL ITEMS
			if(last_location!= null){
				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(last_location, 14));
			}
			for (OSMNode item:MainActivity.allTapItem){
				Log.d(MainActivity.TAG, item.getId() + ": Restoring item");
				addMarker(item);
			}
			
			for (OSMNode item:MainActivity.allToiletItem){
				Log.d(MainActivity.TAG, item.getId() + ": Restoring item");
				addMarker(item);
			}
			
			for (OSMNode item:MainActivity.allFoodItem){
				Log.d(MainActivity.TAG, item.getId() + ": Restoring item");
				addMarker(item);
			}
		 }
	}
	
	@AfterViews
	public void setUpMap(){

	}
	@Override
	public void onLocationChanged(Location location) {
        LatLngBounds bounds = this.mMap.getProjection().getVisibleRegion().latLngBounds;

        if(!bounds.contains(new LatLng(location.getLatitude(), location.getLongitude())))
        {
             //Move the camera to the user's location if they are off-screen!
             mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
        mCallback.onMyLocationChange(location);
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
		LatLng location = arg0.target;
		mCallback.onCameraLocationChange(location);
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if (mMap != null) {
			// The Map is verified. It is now safe to manipulate the map.
			mMap.setMyLocationEnabled(false);
		}
	}
	
	/**
	 * Display the Node Add Overlay and a marker to move it around
	 */
	public void addNewNode(){
		if(ADDING) return;
		ADDING = true;
		nodesAddOverlay.setVisibility(View.VISIBLE);
		mMap.setInfoWindowAdapter(null);
		mMap = ((SupportMapFragment) getFragmentManager().findFragmentByTag("mapfragment")).getMap();
		MarkerOptions markerOptions = new MarkerOptions()
	       .position(new LatLng(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude))
	       .title("New Node")//TODO strings
	       .snippet("Long Press & Drag to move around")
	       .draggable(true)
	       .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
		nodeAddmarker = mMap.addMarker(markerOptions);
	}
	
	/**
	 * Remove Node Add Overlay
	 */
	public void hideNewNodeOverlay(){
		nodeAddmarker.remove();
		nodesAddOverlay.setVisibility(View.GONE);
		mMap.setInfoWindowAdapter(new PopupAdapter(getActivity().getLayoutInflater()));
		ADDING = false;
	}
	/**
	 * Shows fragment dialog to add new nodes
	 */
	public void showAddNodeFragmentDialog(){
		
        FragmentManager fm = getActivity().getSupportFragmentManager();
        AddNodeFragmentDialog addNodeFragmentDialog = AddNodeFragmentDialog.newInstance(
        		nodeAddmarker.getPosition().latitude,
        		nodeAddmarker.getPosition().longitude);
        
        addNodeFragmentDialog.show(fm, "fragment_add_node");
	}
	
	@Background
	public void addMarker(OSMNode Node){
		mMap = ((SupportMapFragment) getFragmentManager().findFragmentByTag("mapfragment")).getMap();
		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			String tag = "";
			StringBuffer snippet = new StringBuffer(); //Node.getTags().toString();

			for (Map.Entry<String, String> entry : Node.getTags().entrySet()) {
			    String key = entry.getKey();
			    Object value = entry.getValue();
			    
			    if (snippet.equals("")){
					snippet.append(key + " : " + value);
			    }else{
					snippet.append( "\n" + key + " : " + value);
			    }
				
				//Log.d(MainActivity.TAG,key + " : " + value);
			}
			BitmapDescriptor bm = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
			// The Map is verified. It is now safe to manipulate the map.
			if(Node.getTags().containsValue("drinking_water")){
				tag = "Water Fountain";
				// bm = BitmapDescriptorFactory.fromResource(R.drawable.tapicon);
				 bm = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
			 }
			 if(Node.getTags().containsValue("toilets")){
				 tag = "Restroom";
				// bm = BitmapDescriptorFactory.fromResource(R.drawable.toileticon);
				 bm = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
			 }
			 
			 if(Node.getTags().containsValue("fast_food") || 
					 Node.getTags().containsValue("cafe") || 
					 Node.getTags().containsValue("restaurant") ||
					 Node.getTags().containsValue("food_court")){
				 tag = "Restaurant";
				 String name = Node.getTagsMatching("name");
				 if(!name.contentEquals("N/A")){
					tag = name;
					snippet.append(Node.getTagsMatching("amenity"));
				 }
				// bm = BitmapDescriptorFactory.fromResource(R.drawable.toileticon);
				 bm = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
			 }
			 MarkerOptions MarkerOptions = new MarkerOptions()
		       .position(new LatLng(Node.getLat(), Node.getLon()))
		       .title(tag)
		       .snippet(snippet.toString())
		       .icon(bm);
			 UiAddMarker(MarkerOptions);
		 }
		 
	}
	@UiThread
	public void UiAddMarker(MarkerOptions MarkerOptions){
	mMap.addMarker(MarkerOptions);
	}
	public void changeView(MapType type) {
		mMap = ((SupportMapFragment) getFragmentManager().findFragmentByTag("mapfragment")).getMap();
		// Check if we were successful in obtaining the map.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
    	SharedPreferences.Editor edit = settings.edit();

		if (mMap != null) {
			if (type == MapType.Satellite) {
				mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				//Save it
	    	    edit.putString("map_type", "satellite");
	       	    edit.commit();
			}
			if (type == MapType.Hybrid) {
				mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				//Save it
	    	    edit.putString("map_type", "hybrid");
	       	    edit.commit();
			}
			if (type == MapType.Map) {
				mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				//Save it
	    	    edit.putString("map_type", "normal");
	       	    edit.commit();
			}
			if (type == MapType.Terrain) {
				mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
				//Save it
	    	    edit.putString("map_type", "terrain");
	       	    edit.commit();
			}
		 }
	}
	
	/* Our custom LocationSource. 
	 * We register this class to receive location updates from the Location Manager
	 * and for that reason we need to also implement the LocationListener interface. */
	public class CustomLocationSource implements LocationSource, LocationListener {

	    private OnLocationChangedListener mListener;
	    private LocationManager locationManager;
	    private final Criteria criteria = new Criteria();
	    private String bestAvailableProvider;
	    /* Updates are restricted to one every 10 seconds, and only when
	     * movement of more than 10 meters has been detected.*/
	    private final int minTime = 1000;     // minimum time interval between location updates, in milliseconds
	    private final int minDistance = 10;    // minimum distance between location updates, in meters
		private boolean at_user = false;

	    CustomLocationSource(Context mContext) {
	        // Get reference to Location Manager
	        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

	        // Specify Location Provider criteria
	          criteria.setAccuracy(Criteria.NO_REQUIREMENT);
	     //   criteria.setPowerRequirement(Criteria.POWER_LOW);
	       // criteria.setAltitudeRequired(true);
	     //   criteria.setBearingRequired(true);
	     //   criteria.setSpeedRequired(true);
	     //   criteria.setCostAllowed(true);
	    }

	    void getBestAvailableProvider() {
	        /* The preffered way of specifying the location provider (e.g. GPS, NETWORK) to use 
	         * is to ask the Location Manager for the one that best satisfies our criteria.
	         * By passing the 'true' boolean we ask for the best available (enabled) provider. */
	        bestAvailableProvider = locationManager.getBestProvider(criteria, true);
	    }

	    /* Activates this provider. This provider will notify the supplied listener
	     * periodically, until you call deactivate().
	     * This method is automatically invoked by enabling my-location layer. */
	    @Override
	    public void activate(OnLocationChangedListener listener) {
	        // We need to keep a reference to my-location layer's listener so we can push forward
	        // location updates to it when we receive them from Location Manager.
	        mListener = listener;

	        // Request location updates from Location Manager
	        if (bestAvailableProvider != null) {
	            locationManager.requestLocationUpdates(bestAvailableProvider, minTime, minDistance, this);
	            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
	            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
	        } else {
	            // (Display a message/dialog) No Location Providers currently available.
	        }
	    }

	    /* Deactivates this provider.
	     * This method is automatically invoked by disabling my-location layer. */
	    @Override
	    public void deactivate() {
	        // Remove location updates from Location Manager
	        locationManager.removeUpdates(this);

	        mListener = null;
	    }

	    @Override
	    public void onLocationChanged(Location location) {
	        /* Push location updates to the registered listener..
	         * (this ensures that my-location layer will set the blue dot at the new/received location) */
			last_location = new LatLng(location.getLatitude(), location.getLongitude());
	    	//Remove the loading splash screen

	        if (mListener != null) {
	            mListener.onLocationChanged(location);
	        }
	        
	        //Only follow the user once
	        if(at_user){
	        	return;
	        }
			//mCallback.onUserLocationChange(new LatLng(location.getLatitude(), location.getLongitude()));
	        /* ..and Animate camera to center on that location !
	         * (the reason for we created this custom Location Source !) */
	        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()),15.0f));
	        CameraPosition cameraPosition = new CameraPosition.Builder()
	        .target(last_location)      // Sets the center of the map to Mountain View
	        .zoom(15)                   // Sets the zoom
	      //  .bearing(90)                // Sets the orientation of the camera to east
	      //  .tilt(45)                   // Sets the tilt of the camera to 30 degrees
	        .build();                   // Creates a CameraPosition from the builder
	        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	        at_user =true;
				
	    }


		@Override
	    public void onStatusChanged(String s, int i, Bundle bundle) {

	    }

	    @Override
	    public void onProviderEnabled(String s) {

	    }

	    @Override
	    public void onProviderDisabled(String s) {

	    }
	}

	@Override
	public void onInfoWindowClick(Marker arg0) {
		//Handles Directions click
		
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
		Uri.parse("geo:0,0?q="+arg0.getPosition().latitude+","+arg0.getPosition().longitude+" (" + arg0.getTitle() + ")"));
		startActivity(intent);

	}
}
