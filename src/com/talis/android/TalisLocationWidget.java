package com.talis.android;

import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.AuthSchemeRegistry;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

public class TalisLocationWidget extends AppWidgetProvider {

	public static String ACTION_WIDGET_LOCATE = "ActionWidgetLocate";

	private double myLat;
	private double myLng;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	  RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);

	  Intent locateIntent = new Intent(context, TalisLocationWidget.class);
	  locateIntent.setAction(ACTION_WIDGET_LOCATE);

	  PendingIntent locatePendingIntent = PendingIntent.getBroadcast(context, 0, locateIntent, 0);
	  
	  remoteViews.setOnClickPendingIntent(R.id.button_status, locatePendingIntent);

	  appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
	  
	  
	}
	
	
	@Override
	public void onReceive(Context context, Intent intent) {		
		String msg = null;
		if (intent.getAction().equals(ACTION_WIDGET_LOCATE)) {
			LocationManager locationManager =
				(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			if (locationManager != null) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 
						0, new LocationUpdateHandler(locationManager, context));	  
				msg = "Updating location...";
				
// DEBUG				
//	            Model model = ModelFactory.createDefaultModel();
//	            model.createStatement(
//	            		model.createResource("http://www.example.com/1"), 
//	            		model.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#Point"), 
//	            		model.createResource("http://www.example.com/1/point"));
//	            model.createStatement(
//	            		model.createResource("http://www.example.com/1/point"), 
//	            		model.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#lat"), 
//	            		model.createLiteral(""+myLat));
//	            model.createStatement(
//	            		model.createResource("http://www.example.com/1/point"), 
//	            		model.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#long"), 
//	            		model.createLiteral(""+myLng));
//	            StringWriter stringWriter = new StringWriter();
//	            model.write(stringWriter);
//	            String rdf = stringWriter.toString();
//	Toast.makeText(context, "Sending rdf: " + rdf, Toast.LENGTH_SHORT).show();
// DEBUG				
				
			} else {
				msg = "No location service found.";
			}

		}			

		if (msg != null) {
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
		}

		super.onReceive(context, intent);
	}

	
	public class LocationUpdateHandler implements LocationListener {

		private LocationManager myLocationManager;
		private Context myContext;
		
		public LocationUpdateHandler(LocationManager locationManager, Context context) {
			myLocationManager = locationManager;
			myContext = context;
		}
		
		@Override
		public void onLocationChanged(Location location) {
            myLat =  location.getLatitude();
            myLng = location.getLongitude();	

            String msg = String.format("Location lat: %s long: %s", myLat, myLng);
			Toast.makeText(myContext, msg, Toast.LENGTH_SHORT).show();
            
            myLocationManager.removeUpdates(this);
            
            Model model = ModelFactory.createDefaultModel();
            model.add(
            		model.createStatement(
            				model.createResource("http://www.example.com/1"), 
            				model.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#Point"), 
            				model.createResource("http://www.example.com/1/point")) 
            );
            model.add(
            		model.createStatement(
            				model.createResource("http://www.example.com/1/point"), 
            				model.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#lat"), 
            				model.createLiteral(""+myLat))
            );
            model.add(
            		model.createStatement(
            				model.createResource("http://www.example.com/1/point"), 
            				model.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#long"), 
            				model.createLiteral(""+myLng))
            );
            StringWriter stringWriter = new StringWriter();
            model.write(stringWriter);
            String rdf = stringWriter.toString();
            try {
            	URI uri = new URI("http://api.talis.com/stores/yourstore/meta");
                DefaultHttpClient client = getClient(uri,"youruser", "yourpassword");
        		HttpPost request = new HttpPost(uri);
        		HttpEntity entity = new StringEntity(rdf);
        		request.setEntity(entity);
        		request.setHeader("content-type", "application/rdf+xml");
        		HttpResponse response = client.execute(request);

        		msg = String.format("Http Response: %s ", response.getStatusLine().getStatusCode());
    			Toast.makeText(myContext, msg, Toast.LENGTH_SHORT).show();
        		
            } catch (Exception e) {
    			Toast.makeText(myContext, e.getMessage(), Toast.LENGTH_SHORT).show();            	
            }
        }

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
	}

	public DefaultHttpClient getClient(URI uri, String username, String password) {
        DefaultHttpClient ret = null;

        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setBooleanParameter("http.protocol.expect-continue", false);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
        ret = new DefaultHttpClient(manager, params);
        if (username != null && password != null) {
    		Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
    		ret.getCredentialsProvider().setCredentials(
    				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM), defaultcreds);
    		
    		ret.getCredentialsProvider().setCredentials(
                new AuthScope(uri.getAuthority(), uri.getPort()),
                new UsernamePasswordCredentials(username, password)
                );
        }
        return ret;
    }	
	
}
