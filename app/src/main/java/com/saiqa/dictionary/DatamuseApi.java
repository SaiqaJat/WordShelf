package com.saiqa.dictionary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DatamuseApi {
    @GET("sug")
    Call<List<DatamuseSearchResult>> getSuggestions(@Query("s") String query);

    @GET("words")
    Call<List<DatamuseWordDetail>> getWordDetails(@Query("sp") String word, @Query("md") String metadata, @Query("max") int max);
}
