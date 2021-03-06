package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.UtilLity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity,titleUpdateTime,degreeText,weatherInfoText,aqiText,
    pm25Text,comfortText,carWashText,sportText;
    private LinearLayout forecastLayout;
    private ImageView bingPicImg;
    public SwipeRefreshLayout swipeRefresh;
    public DrawerLayout drawerLayout;
    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        initView();
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        swipeRefresh.setColorSchemeResources(R.color.design_default_color_primary);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        final String weatherId;
        if (weatherString != null){
            //??????????????????????????????
            Weather weather = UtilLity.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else {
            //???????????????????????????????????????
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(weatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }

    }

    /**
     * ????????????????????????
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic",bingPic);
                edit.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    /**
     * ????????????id????????????????????????
     * @param weatherId
     */
    public void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"????????????????????????",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               final String responseText = response.body().string();
               final Weather weather = UtilLity.handleWeatherResponse(responseText);
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       if (weather != null && "ok".equals(weather.status)){
                           SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                           editor.putString("weather",responseText);
                           editor.apply();
                           showWeatherInfo(weather);
                       }else {
                           Toast.makeText(WeatherActivity.this,"??????????????????",Toast.LENGTH_SHORT).show();
                       }
                       //??????????????????????????????????????????
                       swipeRefresh.setRefreshing(false);
                   }
               });

            }
        });
        loadBingPic();
    }

    /**
     * ???????????????Weather?????????????????????
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
      if (weather != null && "ok".equals(weather.status)){
       String cityName = weather.basic.cityName;
       String updateTime = weather.basic.update.updateTime.split(" ")[1];
       String degree = weather.now.temperature + "???";
       String weatherInfo = weather.now.more.info;
       titleCity.setText(cityName);
       titleUpdateTime.setText(updateTime);
       degreeText.setText(degree);
       weatherInfoText.setText(weatherInfo);
       forecastLayout.removeAllViews();
       for (Forecast forecast:weather.forecastList){
           View view = LayoutInflater.from(this).inflate(R.layout.forcast_item, forecastLayout, false);
           TextView dateText= view.findViewById(R.id.date_text);
           TextView infoText= view.findViewById(R.id.info_text);
           TextView maxText = view.findViewById(R.id.max_text);
           TextView minText = view.findViewById(R.id.min_text);
           dateText.setText(forecast.date);
           infoText.setText(forecast.more.info);
           maxText.setText(forecast.temperature.max);
           minText.setText(forecast.temperature.min);
           forecastLayout.addView(view);
       }
       if (weather.aqi != null){
           aqiText.setText(weather.aqi.city.aqi);
           pm25Text.setText(weather.aqi.city.pm25);
       }
       String comfort = "?????????" + weather.suggestion.comfort.info;
       String carWash = "????????????" + weather.suggestion.carwash.info;
       String sport = "????????????" + weather.suggestion.sport.info;
       comfortText.setText(comfort);
       carWashText.setText(carWash);
       sportText.setText(sport);
       weatherLayout.setVisibility(View.VISIBLE);
       Intent intent = new Intent(this, AutoUpdateService.class);
       startService(intent);
        }else {
            Toast.makeText(WeatherActivity.this,"????????????????????????",Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        weatherLayout = findViewById(R.id.weather_layout);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        titleCity = findViewById(R.id.title_city);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forcast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
    }
}