package com.valueyouth.timeweather;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.valueyouth.timeweather.gson.Forecast;
import com.valueyouth.timeweather.gson.Weather;
import com.valueyouth.timeweather.util.HttpUtil;
import com.valueyouth.timeweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    /**the main layout of the app*/
    private ScrollView weatherLayout;

    /**the layout for the forecast weather*/
    private LinearLayout forecastLayout;

    private TextView titleCity;

    /**the update time of weather*/
    private TextView titleUpdateTime;

    /**the temperature of now*/
    private TextView degreeText;

    private TextView weatherInfoText;

    /**the air quality index*/
    private TextView aqiText;

    /**the PM2.5*/
    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    private Button navButton;

    private String weatherID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        /*1. initialize the whole widget*/
        // the upper part
        titleCity = (TextView) findViewById(R.id.title_city);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        // the lower part
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);

        /*2.query the data of weather*/
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);

        if (weatherString != null) {  // have local cache
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }
        else { // have no local cache
            String weatherID = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherID);
        }
    }

    /**
     * request the weather info of the city by the weather id.
     * @param newWeatherID the weather id
     */
    public void requestWeather(final String newWeatherID) {
        String weatherURL = "http://guolin.tech/api/weather?cityid=" + newWeatherID +
                "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOKHttpRequest(weatherURL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(WeatherActivity.this, "服务器异常", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            weatherID = weather.basic.weatherID; // fix the question of refresh
                            showWeatherInfo(weather);
                        }
                        else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }



    /**
     * show the city weather info which is selected by the customer.
     * @param weather the info of weather
     */
    private void showWeatherInfo(Weather weather){
        /*1. handle the upper part data*/
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        /*2. handle the lower part data*/
        // the forecast
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecasts) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);

            forecastLayout.addView(view);

        }
        // the aqi
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        // the suggestion
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        /**3.make the view visible*/
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
