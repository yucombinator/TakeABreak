package com.icechen1.bathroomfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.google.android.gms.maps.model.LatLng;
import com.icechen1.bathroomfinder.OAuthFragmentDialog.GetTokenOperation;
import com.icechen1.bathroomfinder.OAuthFragmentDialog.GetVerifyOperation;
import com.icechen1.bathroomfinder.api.OpenStreetMapAPI;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddNodeFragmentDialog extends DialogFragment{

	private double lat;
	private double lon;

	public AddNodeFragmentDialog() {
		// TODO Auto-generated constructor stub
	}

	public static AddNodeFragmentDialog newInstance(double lat, double lon) {
		AddNodeFragmentDialog f = new AddNodeFragmentDialog();
	    Bundle args = new Bundle();
	    args.putDouble("lat", lat);
	    args.putDouble("lon", lon);
	    f.setArguments(args);
	    return f;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_addnode, null);
        Bundle args = getArguments();
        lat = 0;
        lon = 0;
        if(args != null) {
        	lat = args.getDouble("lat");
        	lon = args.getDouble("lon");
           // Log.d("MyFragment", "found record_id of " + String.valueOf(record_id));
        }
        
        //UI References
        TextView latlontext = (TextView) view.findViewById(R.id.latlongText);
        final Spinner spinnerTypeChoice = (Spinner) view.findViewById(R.id.spinnerTypeChoice);
        final CheckBox insideCheck = (CheckBox) view.findViewById(R.id.insideCheck);
        final EditText Descrip = (EditText) view.findViewById(R.id.Descrip);
        
        //TOILET VALUES
        final CheckBox feeCheck = (CheckBox) view.findViewById(R.id.fees);
        final CheckBox wheelchair = (CheckBox) view.findViewById(R.id.wheelchair);
        final CheckBox hasFountain = (CheckBox) view.findViewById(R.id.has_fountain);
        
        final EditText open_hrs = (EditText) view.findViewById(R.id.opening_hours);
        
        final LinearLayout toiletOptions = (LinearLayout) view.findViewById(R.id.toilet_options);
        
		toiletOptions.setVisibility(View.GONE);

        latlontext.setText(new LatLng(lat,lon).toString());
        
    	List<String> list = new ArrayList<String>();
    	list.add("drinking_water");
    	list.add("toilets");
    	ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
    		android.R.layout.simple_spinner_item, list);
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	spinnerTypeChoice.setAdapter(dataAdapter);
    	spinnerTypeChoice.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				if(spinnerTypeChoice.getSelectedItem().equals("toilets")){
					toiletOptions.setVisibility(View.VISIBLE);
				}else{
					toiletOptions.setVisibility(View.GONE);
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

    	});

    	return new AlertDialog.Builder(getActivity())
    	.setTitle(getActivity().getResources().getString(R.string.addnode_dialog_title))
    	.setView(view)

    	.setNegativeButton(getActivity().getResources().getString(R.string.cancel),
    			new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			dismiss();
    		}
    	})
    	.setPositiveButton(getActivity().getResources().getString(R.string.submit),
    			new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			HashMap<String, String> params = new HashMap<String, String>();
    			params.put("lat", String.valueOf(lat));
    			params.put("lon", String.valueOf(lon));
    			params.put("amenity", (String) spinnerTypeChoice.getSelectedItem());
    			params.put("indoor", String.valueOf(insideCheck.isChecked()));
    			params.put("description", Descrip.getText().toString());
    			if(spinnerTypeChoice.getSelectedItem().equals("toilets")){
        			params.put("fee", String.valueOf(feeCheck.isChecked()));
        			params.put("wheelchair", String.valueOf(wheelchair.isChecked()));
        			params.put("drinking_water", String.valueOf(hasFountain.isChecked()));
        			params.put("opening_hours", open_hrs.getText().toString());
    			}
    			new AddNodeOperation((MainActivity)getActivity()).execute(params);
    		}
    	})
    	.create();
	}

	class AddNodeOperation extends AsyncTask<HashMap<String, String>, Void, Boolean> {
		MainActivity m_a;
		@Override
        protected Boolean doInBackground(HashMap<String, String>... params) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
			//Load the saved accessToken
			Token accessToken = new Token(settings.getString("accessToken", null),settings.getString("accessSecret", null));
			OAuthService service = new ServiceBuilder()
	        .provider(OpenStreetMapAPI.class)
	        .apiKey("3rTM4by36P1CsMfiwZ1g58L1c1tZOpQWLUo591tx")
	        .apiSecret("Cl2NKl6LFAvKqXpI7Kvccwyamdz80rHAXLruWhbG")
	        .build();
            Toast.makeText(m_a, m_a.getResources().getString(R.string.add_loading), Toast.LENGTH_LONG).show();
		    /**
		     * Getting a changeset from the api
		     */
			OAuthRequest changeset_request = new OAuthRequest(Verb.PUT, "http://api.openstreetmap.org/api/0.6/changeset/create");
			String changeset_payload = 
			"<osm>\n"+
			  "<changeset>\n"+
			    "<tag k=\"created_by\" v=\"TakeABreak 1.5\"/>\n"+
			  "</changeset>\n"+
			"</osm>";
			changeset_request.addPayload(changeset_payload );
			changeset_request.addHeader("Content-Length", Integer.toString(changeset_payload.length())); 
			changeset_request.addHeader("Content-Type", "text/xml"); 
			service.signRequest(accessToken, changeset_request); // the access token from step 4
			Response changeset_response = changeset_request.send();
		    Log.d("TakeABreak", "Changeset: "+ changeset_response.getBody());
		    
		    params[0].put("changeset", changeset_response.getBody());
			
		    /**
		     * PUT-ing the change proper.
		     */
		    
			OAuthRequest request = new OAuthRequest(Verb.PUT, "http://api.openstreetmap.org/api/0.6/node/create");
			String payload = generatePayload(params[0]);
			request.addPayload(payload);
			request.addHeader("Content-Length", Integer.toString(payload.length())); 
		    request.addHeader("Content-Type", "text/xml"); 
			service.signRequest(accessToken, request); // the access token from step 4
			Response response = request.send();
		    //Log.d("TakeABreak", response.toString());
		    
		    
		    Iterator<Entry<String, String>> it = response.getHeaders().entrySet().iterator();
		    Log.d("TakeABreak", response.getBody());
			//System.out.println(response.getBody());   
            return response.isSuccessful();
			//return true;
        }      

        private String generatePayload(HashMap<String, String> params) {
        	String lat = params.get("lat");
        	String lon = params.get("lon");
        	
        	String changeset = params.get("changeset");
        	
        	params.remove("lat");
        	params.remove("lon");
        	params.remove("changeset");
        	
			String payload = "<osm>"+
					"<node changeset=\""+changeset+"\" lat=\""+lat+"\" lon=\""+lon+"\">";
		    Iterator<Entry<String, String>> it = params.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        String key = (String) pairs.getKey();
		        String value = (String) pairs.getValue();
		        if(!value.equals("")){
		        	if(value.equals("true"))value="yes";
		        	if(value.equals("false"))value="no";
		        	payload = payload.concat("<tag k=\""+key+"\" v=\""+value+"\"/>");
		        }
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		    payload = payload.concat("</node>\n</osm>");
		    Log.d("TakeABreak", payload);
			return payload;
		}

		@Override
        protected void onPostExecute(Boolean result) {
			if(result){
                Toast.makeText(m_a, m_a.getResources().getString(R.string.add_success), Toast.LENGTH_LONG).show();
			}else{
                Toast.makeText(m_a, m_a.getResources().getString(R.string.add_failed), Toast.LENGTH_LONG).show();
			}
			//Hide the add UI
			m_a.cancel_node_add_handler();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
        AddNodeOperation(MainActivity a){
        	m_a = a;
        }
  }  

}
