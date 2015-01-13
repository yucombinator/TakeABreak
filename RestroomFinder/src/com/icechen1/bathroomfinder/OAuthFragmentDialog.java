package com.icechen1.bathroomfinder;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.icechen1.bathroomfinder.api.OpenStreetMapAPI;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class OAuthFragmentDialog extends DialogFragment {
	public interface OAuthCallback{
	    public void onCancel();
	    public void onSuccess();
	}

	private OAuthCallback mCallback;
	private WebView webview;
	
	public OAuthService service;
	public Token requestToken;
	public SharedPreferences settings;
	
	public OAuthFragmentDialog() {
		// TODO Auto-generated constructor stub
	}

	public static OAuthFragmentDialog newInstance() {
		OAuthFragmentDialog f = new OAuthFragmentDialog();
	    Bundle args = new Bundle();
	    f.setArguments(args);
	    return f;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OAuthCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		//int title = getArguments().getInt("title");
	    View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_login, null);
	    AlertDialog d = new AlertDialog.Builder(getActivity())
	    		.setTitle(getActivity().getResources().getString(R.string.login_dialog_title))
	    		.setView(view)

	    		.setNegativeButton(getActivity().getResources().getString(R.string.cancel),
	    				new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int whichButton) {
	    				dismiss();
	    			}
	    		}
	    				)
	    				.create();
        webview = (WebView) view.findViewById(R.id.webView);
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		//KEYBOARD NOT SHOWING UP BUGFIX
		EditText edit = (EditText) view.findViewById(R.id.edit);
	    edit.setFocusable(true);
	    edit.requestFocus();
	    
		new GetTokenOperation().execute();

		return d;
	}


	class GetTokenOperation extends AsyncTask<Void, Void, String> {
		@Override
        protected String doInBackground(Void... params) {
			Log.i("TakeABreak", "Generating request token...");   
			service = new ServiceBuilder()
	        .provider(OpenStreetMapAPI.class)
	        .apiKey("3rTM4by36P1CsMfiwZ1g58L1c1tZOpQWLUo591tx")
	        .apiSecret("Cl2NKl6LFAvKqXpI7Kvccwyamdz80rHAXLruWhbG")
	        .build();
			
    		requestToken = service.getRequestToken();
    		String authUrl = service.getAuthorizationUrl(requestToken);
            Log.i("TakeABreak", authUrl);        

              return authUrl;
        }      

        @Override
        protected void onPostExecute(String result) {
            webview.loadUrl(result);
            webview.setWebViewClient(new WebViewClient(){
                @Override
                public boolean shouldOverrideUrlLoading (WebView view, String url) {
                    Log.i("TakeABreak", url);   

                    //check for our custom callback protocol
                    //otherwise use default behavior
                    if(url.contains("oauth_verifier") )
                    {
                        //authorization complete hide webview for now.
                        webview.setVisibility(View.GONE);
                        Uri uri = Uri.parse(url);
                        String verifier = uri.getQueryParameter("oauth_verifier");
                        new GetVerifyOperation().execute(verifier);

                        dismiss();
                        return true;
                    }
                    return super.shouldOverrideUrlLoading(view, url);

                }});

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
  }   
	
	class GetVerifyOperation extends AsyncTask<String, Void, Void> {

		@Override
        protected Void doInBackground(String... verifier) {

            Verifier v = new Verifier(verifier[0]);

            //save this token for practical use.
            Token accessToken = service.getAccessToken(requestToken, v);

            OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.openstreetmap.org/api/capabilities");
            service.signRequest(accessToken, request);
            Response response = request.send();

            Log.i("TakeABreak", response.getBody());   

            SharedPreferences.Editor editor = settings.edit();

            editor.putString("accessToken", accessToken.getToken());
            editor.putString("accessSecret", accessToken.getSecret());

            // The requestToken is saved for use later on to verify the OAuth request.
            // See onResume() below
            editor.putString("requestToken", requestToken.getToken());
            editor.putString("requestSecret", requestToken.getSecret());

            editor.commit();     
            mCallback.onSuccess();
            
              return null;
        }      

        @Override
        protected void onPostExecute(Void result) {
//load stuff on mainactivity

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
  }   
}
