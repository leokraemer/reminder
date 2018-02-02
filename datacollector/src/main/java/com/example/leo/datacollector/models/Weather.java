/**
 * This is a tutorial source code
 * provided "as is" and without warranties.
 * <p>
 * For any question please visit the web site
 * http://www.survivingwithandroid.com
 * <p>
 * or write an email to
 * survivingwithandroid@gmail.com
 */
package com.example.leo.datacollector.models;

import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;

import com.example.leo.datacollector.R;
import com.example.leo.datacollector.datacollection.sensors.WeatherCaller;

import org.json.JSONException;

/*
 * Copyright (C) 2013 Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class Weather {

    public int id = -1;
    public long timestamp = -1;
    public Location location;
    public CurrentCondition currentCondition = new CurrentCondition();
    public Temperature temperature = new Temperature();
    public Wind wind = new Wind();
    public Rain rain = new Rain();
    public Snow snow = new Snow();
    public Clouds clouds = new Clouds();

    public byte[] iconData;

    public class CurrentCondition {
        public int weatherId;
        public String condition;
        public String descr;
        public String icon;
        public float pressure;
        public float humidity;
    }

    public class Temperature {
        public float temp;
        public float minTemp;
        public float maxTemp;
    }

    public class Wind {
        public float speed;
        public float deg;
    }

    public class Rain {
        public String time;
        public float ammount;
    }

    public class Snow {
        public String time;
        public float ammount;
    }

    public class Clouds {
        public int perc;
    }

    // @see https://openweathermap.org/weather-conditions
    public @DrawableRes
    int getWeatherIcon() {
        int weatherId = currentCondition.weatherId;
        //storm
        if (100 < weatherId && weatherId < 300)
            return R.drawable.weather_storm;
            //light rain
        else if (weatherId < 400)
            return R.drawable.weather_lightrain;
            //rain + snow
        else if (weatherId < 700)
            return R.drawable.weather_rain;
        else if (weatherId < 762)
            return R.drawable.weather_fog;
        else if (weatherId < 800)
            return R.drawable.weather_storm;
        else if (weatherId < 802)
            return R.drawable.weather_sunny;
        else if (weatherId < 803)
            return R.drawable.weather_mostlysunny;
        else if (weatherId == 804)
            return R.drawable.weather_cloudy;
        //night as error case
        return R.drawable.weather_sunny_n;
    }

    public boolean compare(Weather other) {
        //bad weather as baseline -> other must also be bad
        if (currentCondition.weatherId < 800) {
            return other.currentCondition.weatherId < 800;
        }
        //good weather -> other must also be good
        if (currentCondition.weatherId >= 800) {
            return other.currentCondition.weatherId >= 800;
        }
        //else false
        return false;
    }

}
