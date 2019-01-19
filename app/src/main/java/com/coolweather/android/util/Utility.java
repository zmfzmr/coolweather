package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /**
     * 可以看到我们提供了handleProvinceResponse handleCityResponse handleCountyResponse 3个方法，
     * 分别用于解析和处理服务器返回的的省级 市级 和县级数据。处理的方式都是类似的
     *      （市级 和县级的参数不一样，仔细看看，市级的要设置省份id，县级的要设置城市id）
     * 先使用JSONArray和JSONObject将数据解析出来，然后组装成实体类对象，再调用save()方法节数据存储到数据库当中。
     * 由于这里的JSON数据结构比较建安，我们就不使用GSON来进行解析了
     */
    /**
     * 解析和处理服务器返回的省级数据
     * @param response
     * @return
     */
    public static boolean handleProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();//保存到数据库里面
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据，市级的还要设置个城市id
     * @param response
     * @param provinceId
     * @return
     */
    public static boolean handleCityResponse(String response,int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);//多了这个
                    city.save();//保存到数据库里面
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据 县级的要设置个城市id
     * @param response
     * @param cityId
     * @return
     */
    public static boolean handleCountyResponse(String response,int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    //                            注意：这里id是String类型
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();//保存到数据库里面
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
