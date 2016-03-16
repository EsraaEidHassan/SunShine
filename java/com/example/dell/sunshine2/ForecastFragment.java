package com.example.dell.sunshine2;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private ArrayAdapter<String> mForeCastAdapter;
    public ForecastFragment() {
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id=item.getItemId();
        if(id==R.id.action_refresh){
            //doSomehingWithActionOneItems();
            if(CheckNetwork.isInternetAvailable(getActivity())) //returns true if internet available
            {
                FetchWeatherTask weatherTask=new FetchWeatherTask();
                weatherTask.execute("94043");
                return true;
                //do something. loadwebview.
            }
            else
            {
                Toast.makeText(getActivity(), "No Internet Connection",Toast.LENGTH_SHORT).show();
                return false;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
            throws JSONException {
        // TODO: add parsing code here
        JSONObject weather = new JSONObject(weatherJsonStr);
        JSONArray days=weather.getJSONArray("list");
        JSONObject dayInfo=days.getJSONObject(dayIndex);
        JSONObject tempratureInfo=dayInfo.getJSONObject("temp");
        return tempratureInfo.getDouble("max");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView= inflater.inflate(R.layout.fragment_main, container, false);
        String[] forecastArray={"today-sunny-88/63",
                "tomorrow-Foggy-70/6",
                "Weds-cloudy-72/63",
                "Thurs-Rainy-64/53",
                "Fri-foggy-70/46",
                "Sat-sunny-76/68"};
       List<String> weekForecast =new ArrayList<String>(Arrays.asList(forecastArray));
        mForeCastAdapter=new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,weekForecast);
        ListView listview=(ListView)rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(mForeCastAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast=mForeCastAdapter.getItem(position);

                Intent intent=new Intent(getActivity(),DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
               // Toast.makeText(getActivity(),forecast,Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }



    public class FetchWeatherTask extends AsyncTask<String, Void, String []> {
        @Override
        protected void onPostExecute(String[] result) {
            if(result!=null){
                mForeCastAdapter.clear();
                for(String dayforecastStr : result){

                    mForeCastAdapter.add(dayforecastStr);
                }

            }
        }

        private final String LOG_TAG=FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String units="metric";
            String format="json";
            int numDays=7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                final String FORECAST_BASE_URL="http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM="q";
                final String FORMAT_PARAM="mode";
                final String Units_PARAM="units";
                final String Days_PARAM = "cnt";
                final String ID="AppID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM,params[0])
                        .appendQueryParameter(FORMAT_PARAM,format)
                        .appendQueryParameter(Units_PARAM,units)
                        .appendQueryParameter(Days_PARAM,Integer.toString(numDays))
                        .appendQueryParameter(ID,"2b2132fac38b37a911929d2cf6f921df")
                        .build();

               // URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&AppID=2b2132fac38b37a911929d2cf6f921df");
                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG,"Built Uri" + builtUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG,"Forecast JSON String: "+forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error "+ e.getMessage());

                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            }  finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                //String[] myForecastArray=getWeatherDataFromJson(forecastJsonStr,numDays);
                //List<String> weekForecast =new ArrayList<String>(Arrays.asList(myForecastArray));
                 //mForeCastAdapter=new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,weekForecast);

                return getWeatherDataFromJson(forecastJsonStr,numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error Jason parsing: "+ e.toString());
            }
            return null;
            // return null; // ana zawedtaha sa7 wala !!
        }



        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }
    }

}
