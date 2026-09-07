package com.saiqa.dictionary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DictionaryApiResponse {

    @GET("en/{word}")
    Call<List<WordResultData>> getWordMeaning(@Path("word") String word);


}
