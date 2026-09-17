package com.masterstore.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();

    private TextView productsText;

    private static final String PRODUCTS_URL =
            "https://master4store.com/wp-json/wc/store/v1/products?per_page=20";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        productsText = findViewById(android.R.id.content);

        loadProducts();
    }

    private void loadProducts() {

        Request request = new Request.Builder()
                .url(PRODUCTS_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "تعذر الاتصال بـ WooCommerce",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                if (!response.isSuccessful()) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "خطأ WooCommerce: " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                    return;
                }

                String json = response.body() != null
                        ? response.body().string()
                        : "";

                try {

                    JSONArray products = new JSONArray(json);

                    runOnUiThread(() -> {

                        Toast.makeText(
                                MainActivity.this,
                                "تم الاتصال بـ WooCommerce: "
                                        + products.length()
                                        + " منتج",
                                Toast.LENGTH_LONG
                        ).show();

                    });

                    for (int i = 0; i < products.length(); i++) {

                        JSONObject product = products.getJSONObject(i);

                        String name = product.optString("name");
                        String id = product.optString("id");

                        android.util.Log.d(
                                "MASTER_STORE",
                                "Product ID: " + id +
                                " | Name: " + name
                        );
                    }

                } catch (Exception e) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "بيانات المنتجات غير صحيحة",
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }
            }
        });
    }
}
