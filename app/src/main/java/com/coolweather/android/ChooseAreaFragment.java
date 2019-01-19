package com.coolweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;
import org.litepal.tablemanager.Connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList = new ArrayList<>();//省列表

    private List<City> cityList = new ArrayList<>();//市列表

    private List<County> countyList = new ArrayList<>();//县列表

    private Province selectedProvince;//选中的省份

    private City selectedCity;//选中的城市

    private int currentLevel;//当前选中的级别

    /**
     * 这里的代码虽然非常多，可是逻辑却不复杂，我们来慢慢理一下。
     * 在onCreateView显示获取大了一些控件的实例，然后去初始化了ArrayAdapter，并将它设置为ListView的适配器。
     * 接着在onActivityCreated方法给ListView和Button设置了点击事件
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,
                dataList);
        listView.setAdapter(adapter);
        return view;
    }

    /**
     * 在onActivityCreated()方法的最后，调用了queryProvinces()方法，也就是从这里开始加载省级数据的。
     * queryProvinces()方法中首先会讲头布局的标题设置为中国，将返回按钮隐藏起来，因为省级列表已经不能再返回了。
     * 然后调用LitePal的查询接口从是数据库中读取省级数据，如果读取了就直接将数据显示到界面上，
     * 如果没有读取到就按照14.1节讲述的接口组装出一个请求地址，然后调用queryFromServer()方法来从服务器上查询数据。
     *
     *
     * @param savedInstanceState
     */
    /**
     * 当你点击了某个省的时候回进入到ListView的onItemClick()方法中，这个时候回根据的哪个区的级别来判断是
     * 去调用queryCities()方法还是queryCounties()方法；
     *      queryCities()：是查询市级数据
     *      queryCounties()：查询县级数据
     *      这2个方法内部的流程和queryprovinces()方法基本相同，这里就不重复讲解了。
     *
     * 另外有一点需要注意：在返回按钮的点击事件里，会对当前的ListView的列表级别来进行判断。
     *  如果当前是县级列表，那么久返回到市级列表，如果当前hi市级列表，那么久返回到省级列表。
     *   当返回奥省级列表是，返回按钮就会自动隐藏，从而也就不需要再做进一步的处理了
     *
     * 这样我们就把遍历全国省市县的功能完成了，可是碎片是不能直接显示在界面上的，因此我们还需要把它添加到活动里才行。
     *
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //因为活动创建后，这些listView才显示在界面上，搜易listView点击事件在onActivityCreated里面写
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */

    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);//隐藏返回按钮
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();//清空数据列表
            for (Province province : provinceList) {
                //数据列表添加省级的名字
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            //这个网站上是所有的省级列表
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }

    }


    /**
     * 查询选中省内所有的市，优先冲数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        //标题设置为：因为是城市，所以设置为省级的名字  返回按钮就要显示出来，方便返回到省
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId()))
                .find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();//清除数据列表
            for (City city : cityList) {
                dataList.add(city.getCityName());

            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;//标记选中到的级别为LEVEL_CITY = 1
        } else {//如果是小于0的话，说明没有数据，就要重新查询解析下市级的数据
            //获取省级的代号
            int provinceCode = selectedProvince.getProvinceCode();
            //注意：china的后面有斜杆
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }
    }

    /**
     * 优先选中室内所有的先，优先从数据库查询，如果没有再去服务器上查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //String.valueOf(selectedCity.getId()):获取到这个城市的Id ,然后转换成字符串
        countyList = LitePal.where("cityid = ?",String.valueOf(selectedCity.getId()))
                .find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county :countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            //注意：china后面有“/”  provinceCode后面也有/
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" +
                    cityCode;
            queryFromServer(address,"county");
        }
    }

    /**
     * 根据传入的地址好类型从服务器上查询省市级数据
     *
     * queryFromServer()方法会调用HttpUtil的sendOkHttpRequest方法来想服务器发送请求，响应的数据会回调到
     * onResponse()中，然后我们在这里调用Utility()的handleProvinceResponse()来解析和处理服务器返回的数据，
     * 并存储到数据库中。
     * 接下里的一步很关键，在解析和处理完数据之后，我们再次调用了 queryProvinces()方法来重新加载省级数据
     * queryProvinces()方法牵扯到了UI操作，切换到主线程（利用runOnUiThread）
     * 现在数据中已经有了数据，因此调用queryProvinces()就hiUI直接将数据显示得到界面上了。
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
//        showProgressDialog();//调用显示进度条对话框
        HttpUtil.sendOkHttpRequest(address, new Callback() {


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);

                } else if ("city".equals(type)) {//这里后面还要获取省级的Id
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if (result) {//如果是true的话，说明已经解析完结果了
                    //因为这里是碎片，要获取当前活动，queryProvinces()方法牵扯到了UI操作，切换到主线程
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ("province".equals(type)) {
                                //调用这个方法重新加载省级数据
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {//对请求响应失败的处理
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        //这里是获取上下文
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();//取消进度条对话框
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            //dialog弹出后会点击屏幕，dialog不消失；点击物理返回键dialog消失
            //cancel:取消  TouchOutside:触摸外部  意思说:触摸外部返回键才会取消进度条对话框
            progressDialog.setCanceledOnTouchOutside(false);
        }
        //别忘记显示了
        progressDialog.show();
    }


}

//---------------------------------------------------------

//public class ChooseAreaFragment extends Fragment {//ChooseAreaFragment
//    public static final int LEVEL_PROVINCE = 0;
//    public static final int LEVEL_CITY = 1;
//    public static final int LEVEL_COUNTY = 2;
//    private ProgressDialog progressDialog;
//    private ArrayAdapter<String> adapter;
//    private List<String> dataList = new ArrayList<>();
//    //省列表
//    private List<Province> provinceList;
//    //市列表
//    private List<City> cityList;
//    //县列表
//    private List<County> countyList;
//    //选中的省份
//    private Province selectedProvince;
//    //选中的城市
//    private City selectedCity;
//    //当前选中的级别
//    private int currentLevel;
//
//    private TextView titleText;
//    private Button backButton;
//    private ListView listView;
//    private View view;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        view = inflater.inflate(R.layout.choose_area, container, false);
//        initView();
//        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
//        listView.setAdapter(adapter);
//        return view;
//
//    }
//
//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        //需要写上这句话，来创建数据库，要不然会报空指针
////        Connector.getDatabase();
//        //列表监听器
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                //如果选择的是省
//                if (currentLevel == LEVEL_PROVINCE) {
//                    selectedProvince = provinceList.get(position);
//                    queryCities();
//                    //如果选择的是市
//                } else if (currentLevel == LEVEL_CITY) {
//                    selectedCity = cityList.get(position);
//                    queryCounties();
//                }
//            }
//        });
//        //回退按钮监听器
//        backButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentLevel == LEVEL_COUNTY) {
//                    queryCities();
//                } else if (currentLevel == LEVEL_CITY) {
//                    queryProvinces();
//                }
//            }
//        });
//        //调用 下面的方法
//        queryProvinces();
//    }
//
//    // 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
//    private void queryProvinces() {
//        //设置 显示标题为中国
//        titleText.setText("中国");
//        //设置回退按钮隐藏
//        backButton.setVisibility(View.GONE);
//        //查询 省实体类的数据
//        provinceList = LitePal.findAll(Province.class);
//        //判断是不是有数据 如果有
//        if (provinceList.size() > 0) {
//            //把要显示在listview的数据列表清空
//            dataList.clear();
//            //遍历省数据
//            for (Province province : provinceList) {
//                //把数据加到 要显示在listview的数据列表 上
//                dataList.add(province.getProvinceName());
//            }
//            //通知数据更新
//            adapter.notifyDataSetChanged();
//            //设置listview 默认选择第一个
//            listView.setSelection(0);
//            // flag  设置为省    为回退做flag
//            currentLevel = LEVEL_PROVINCE;
//            //如果没有数据
//        } else {
//            //去服务器获取数据
//            String address = "http://guolin.tech/api/china";
//            queryFromServier(address, "province");
//        }
//    }
//
//    //查询全国所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
//    private void queryCities() {
//        titleText.setText(selectedProvince.getProvinceName());
//        backButton.setVisibility(View.VISIBLE);
//        cityList = LitePal.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
//        if (cityList.size() > 0) {
//            dataList.clear();
//            for (City city : cityList) {
//                dataList.add(city.getCityName());
//            }
//            adapter.notifyDataSetChanged();
//
//            listView.setSelection(0);
//            currentLevel = LEVEL_CITY;
//
//        } else {
//            int provinceCode = selectedProvince.getProvinceCode();
//            String address = "http://guolin.tech/api/china/" + provinceCode;
//            queryFromServier(address, "city");
//        }
//    }
//
//    // 查询全国所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
//    private void queryCounties() {
//        titleText.setText(selectedCity.getCityName());
//        backButton.setVisibility(View.VISIBLE);
//        countyList = LitePal.where("cityid=?", String.valueOf(selectedCity.getId())).find(County.class);
//        if (countyList.size() > 0) {
//            dataList.clear();
//            for (County county : countyList) {
//                dataList.add(county.getCountyName());
//            }
//            adapter.notifyDataSetChanged();
//            listView.setSelection(0);
//            currentLevel = LEVEL_COUNTY;
//        } else {
//            int cityCode = selectedCity.getCityCode();
//            int provinceCode = selectedProvince.getProvinceCode();
//            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
//            queryFromServier(address, "county");
//        }
//    }
//
//    // 根据传入的地址和类型 从服务器查询省市县信息数据
//    private void queryFromServier(String address, final String type) {
//        //显示对话框
////        showProgressDialog();
//        HttpUtil.sendOkHttpRequest(address, new Callback() {
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                //把获取到的数据设置为字符串
//                String responseText = response.body().string();
//                //设置结果
//                boolean result = false;
//
//                if ("province".equals(type)) {
//                    //返回的省数据处理结果
//                    result = Utility.handleProvinceResponse(responseText);
//                } else if ("city".equals(type)) {
//                    //返回的市数据处理结果
//                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
//                } else if ("county".equals(type)) {
//                    //返回的县数据处理结果
//                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
//                }
//                //根据返回的数据结果更新数据
//                if (result) {
//                    //获取主线程（UI必须在主线程更新）
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            closeProgressDialog();
//                            if ("province".equals(type)) {
//                                queryProvinces();
//                            } else if ("city".equals(type)) {
//                                queryCities();
//                            } else if ("county".equals(type)) {
//                                queryCounties();
//                            }
//                        }
//                    });
//                }
//
//
//            }
//
//            @Override
//            public void onFailure(Call call, IOException e) {
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        closeProgressDialog();
//                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });
//    }
//
//    private void showProgressDialog() {
//        if (progressDialog == null) {
//            progressDialog = new ProgressDialog(getActivity());
//            progressDialog.setMessage("正在加载...");
//            progressDialog.setCanceledOnTouchOutside(false);
//        }
//        progressDialog.show();
//    }
//
//    private void closeProgressDialog() {
//        if (progressDialog != null) {
//            progressDialog.dismiss();
//        }
//    }
//
//    private void initView() {
//        titleText = view.findViewById(R.id.title_text);
//        backButton = view.findViewById(R.id.back_button);
//        listView = view.findViewById(R.id.list_view);
//    }


//}
