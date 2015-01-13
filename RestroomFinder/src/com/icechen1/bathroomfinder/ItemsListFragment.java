package com.icechen1.bathroomfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.android.maps.GeoPoint;
import com.googlecode.androidannotations.annotations.EFragment;
import com.icechen1.bathroomfinder.api.OSMNode;
import com.icechen1.bathroomfinder.utils.ListComparator;

@EFragment
public class ItemsListFragment extends SherlockListFragment {
	public static final String TAG = "listFragment";
	public static TextView mAddress = null;
	static ArrayList<OSMNode> mItem = new ArrayList<OSMNode>();
	
	public ItemsListFragment() {}
	static ArrayAdapter<OSMNode> mArrayAdapter;
	static Handler handler = new Handler();
    static Runnable r=new Runnable() {
    	   public void run() {
    		   try{
   	    		mArrayAdapter.notifyDataSetChanged();
    		   }catch(Exception e){
    			   Log.d("Amenities", "An error occured while populating the list.");
    		   }
    	   }
    };
	@Override
	public void onCreate(Bundle arg0) {
		super.onCreate(arg0);  
		setRetainInstance(true);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle data) {
		mArrayAdapter = new ArrayAdapter<OSMNode>(getSherlockActivity(), android.R.id.text1, mItem);
		mArrayAdapter.setNotifyOnChange(true);
		setListAdapter(mArrayAdapter);
		// Inflate the ListView layout file.
		return inflater.inflate(R.layout.list_fragment, null);
	}

//	public void updateList(){
//		mAddress = (TextView) getView().findViewById(R.id.location);
//	    new Thread(new Runnable() {
//	        public void run() {
//	    		mItem.clear();
//	    		//List<ManagedOverlayItem> List = new ArrayList<ManagedOverlayItem>();
//	    		Activity a = getActivity();
//	    		//Change to the map fragment display
//	    		if(a instanceof MainActivity) {
//		    		mItem.addAll(((MainActivity)a).allTapItem);
//		    		mItem.addAll(((MainActivity)a).allToiletItem);
//	    		}
//
//
//	    		
///*	    		Iterator<ManagedOverlayItem> Iterator = List.iterator();
//	    		while(Iterator.hasNext()){
//	    			ManagedOverlayItem item = Iterator.next();
//	    			mItem.add(item);
//	    		}*/
//	    		//Log.d("Amenities", "mItems Size: " + mItem.size());
//	    		if(mItem.size()>=2){
//	    			try{
//		    		Collections.sort(mItem, new ListComparator());
//	    			}catch(ArrayIndexOutOfBoundsException e){
//	    				Log.e("Amenities", "ArrayIndexOutOfBoundsException " + e.getStackTrace().toString());
//	    			}
//	    			catch(ConcurrentModificationException e){
//	    				Log.e("Amenities", "ConcurrentModificationException " + e.getStackTrace().toString());
//	    			}
//	    			catch(Exception e){
//	    				Log.e("Amenities", "ConcurrentModificationException " + e.getStackTrace().toString());
//	    			}
//	    		}
//	    		//Looper.prepare();
//
//	    	   // ListFragment.updateList();
//	    	    handler.post(r);
//
//	        }
//	    }).start();
//	}
//	@Override
//	public void onListItemClick(ListView l, View v, int position, long id){
//		ManagedOverlayItem selection = (ManagedOverlayItem) l.getItemAtPosition(position);
//		GeoPoint gp = selection.getPoint();
// 	    //Go to POI location
//		MapFragment.mapController.animateTo(gp);
//		Activity a = getActivity();
//		//Change to the map fragment display
//		if(a instanceof MapActivity) {
//		    ((MapActivity) a).showFragment(MapActivity.Exchanger.mMapFragment);
//		}
//	}
}
