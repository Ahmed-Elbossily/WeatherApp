package com.ahmedelbossily.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class WeatherController extends AppCompatActivity {

    // Request Codes:
    final int REQUEST_CODE = 123; // Request Code for permission request callback
    final int NEW_CITY_CODE = 456; // Request code for starting new activity for result callback

    final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    final String APP_ID = "837867c67c463215f40d0f9b55a9190b";

    // Time between location updates (5000 milliseconds or 5 seconds)
    final long MIN_TIME = 5000;

    // Distance between location updates (1000m or 1km)
    final float MIN_DISTANCE = 1000;

    final String TAG = "WeatherApp";

    final String LOCATION_PROVIDER = LocationManager.NETWORK_PROVIDER;

    boolean useLocation = true;
    TextView cityLabel;
    ImageView weatherImage;
    TextView temperatureLabel;
    ImageButton changeCityButton;

    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_controller_layout);

        cityLabel = findViewById(R.id.locationTV);
        weatherImage = findViewById(R.id.weatherSymbolIV);
        temperatureLabel = findViewById(R.id.tempTV);
        changeCityButton = findViewById(R.id.changeCityButton);

        changeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherController.this, ChangeCityController.class);
                startActivityForResult(intent, NEW_CITY_CODE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
        if (useLocation)
            getWeatherForCurrentLocation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() called");
        if (requestCode == NEW_CITY_CODE) {
            if (resultCode == RESULT_OK) {
                String city = data.getStringExtra("City");
                Log.d(TAG, "New city is " + city);
                useLocation = false;
                getWeatherForNewCity(city);
            }
        }
    }

    private void getWeatherForNewCity(String city) {
        Log.d(TAG, "Getting weather for new city");
        RequestParams params = new RequestParams();
        params.put("q", city);
        params.put("appid", APP_ID);
        connectWeatherApi(params);
    }

    private void getWeatherForCurrentLocation() {
        Log.d(TAG, "Getting weather for current location");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "onLocationChanged() callback received");
                String longitude = String.valueOf(location.getLongitude());
                String latitude = String.valueOf(location.getLatitude());

                Log.d(TAG, "longitude is: " + longitude);
                Log.d(TAG, "latitude is: " + latitude);

                RequestParams params = new RequestParams();
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("appid", APP_ID);
                connectWeatherApi(params);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "onStatusChanged() callback received. Status: " + status);
                Log.d(TAG, "2 means AVAILABLE, 1: TEMPORARILY_UNAVAILABLE, 0: OUT_OF_SERVICE");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "onProviderEnabled() callback received. Provider: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "onProviderDisabled() callback received. Provider: " + provider);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }

        Log.d(TAG, "Location Provider used: " + locationManager.getProvider(LOCATION_PROVIDER).getName());
        Log.d(TAG, "Location Provider is enabled: " + locationManager.isProviderEnabled(LOCATION_PROVIDER));
        Log.d(TAG, "Last known location (if any): " + locationManager.getLastKnownLocation(LOCATION_PROVIDER));
        Log.d(TAG, "Requesting location updates");

        locationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult(): Permission granted!");
                getWeatherForCurrentLocation();
            } else {
                Log.d(TAG, "Permission denied =( ");
            }
        }
    }

    private void connectWeatherApi(RequestParams params) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(WEATHER_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d(TAG, "Success! JSON: " + response.toString());
                WeatherDataModel weatherData = WeatherDataModel.fromJson(response);
                updateUI(weatherData);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e(TAG, "Fail " + throwable.toString());
                Toast.makeText(WeatherController.this, "Request Failed", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Status code " + statusCode);
                Log.d(TAG, "Here's what we got instead " + errorResponse.toString());
            }
        });
    }

    private void updateUI(WeatherDataModel weather) {
        temperatureLabel.setText(weather.getTemperature());
        cityLabel.setText(weather.getCity());

        int resourceID = getResources().getIdentifier(weather.getIconName(), "drawable", getPackageName());
        weatherImage.setImageResource(resourceID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
    }
}
