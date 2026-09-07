package com.saiqa.dictionary;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {

    public static RetrofitInstance instance;
    public DictionaryApiResponse apiResponse;
    public DatamuseApi datamuseApi;

    RetrofitInstance() {
        String BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/";
        String DATAMUSE_URL = "https://api.datamuse.com/";

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Android; Mobile) DictionaryApp/1.0")
                            .header("Accept", "application/json")
                            .build();
                    return chain.proceed(requestWithUserAgent);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiResponse = retrofit.create(DictionaryApiResponse.class);

        Retrofit datamuseRetrofit = new Retrofit.Builder()
                .baseUrl(DATAMUSE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        datamuseApi = datamuseRetrofit.create(DatamuseApi.class);
    }

    public static synchronized RetrofitInstance setInstance() {
        if (instance == null) {
            instance = new RetrofitInstance();
        }
        return instance;
    }
}
