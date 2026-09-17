package com.masterstore.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

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

    private LinearLayout productsContainer;
    private ProgressBar loadingBar;

    private String cartToken = "";

    private static final String PRODUCTS_URL =
            "https://master4store.com/wp-json/wc/store/v1/products?per_page=20";

    private static final String CART_URL =
            "https://master4store.com/wp-json/wc/store/v1/cart";

    private static final String ADD_TO_CART_URL =
            "https://master4store.com/wp-json/wc/store/v1/cart/add-item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            setContentView(R.layout.activity_main);

            productsContainer =
                    findViewById(R.id.productsContainer);

            loadingBar =
                    findViewById(R.id.loadingBar);

            if (productsContainer == null) {
                Toast.makeText(
                        this,
                        "خطأ: productsContainer غير موجود",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (loadingBar == null) {
                Toast.makeText(
                        this,
                        "خطأ: loadingBar غير موجود",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            loadProducts();
            getCartToken();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "حدث خطأ أثناء تشغيل التطبيق",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // تحميل المنتجات
    // =========================================================

    private void loadProducts() {

        if (loadingBar != null) {
            loadingBar.setVisibility(View.VISIBLE);
        }

        Request request = new Request.Builder()
                .url(PRODUCTS_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(
                    Call call,
                    IOException e
            ) {

                runOnUiThread(() -> {

                    if (loadingBar != null) {
                        loadingBar.setVisibility(View.GONE);
                    }

                    Toast.makeText(
                            MainActivity.this,
                            "تعذر الاتصال بالمتجر",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                try {

                    if (!response.isSuccessful()) {

                        runOnUiThread(() -> {

                            if (loadingBar != null) {
                                loadingBar.setVisibility(View.GONE);
                            }

                            Toast.makeText(
                                    MainActivity.this,
                                    "خطأ من المتجر: "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });

                        return;
                    }

                    String json =
                            response.body() != null
                                    ? response.body().string()
                                    : "[]";

                    JSONArray products =
                            new JSONArray(json);

                    runOnUiThread(() -> {

                        if (loadingBar != null) {
                            loadingBar.setVisibility(View.GONE);
                        }

                        displayProducts(products);
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        if (loadingBar != null) {
                            loadingBar.setVisibility(View.GONE);
                        }

                        Toast.makeText(
                                MainActivity.this,
                                "تعذر قراءة بيانات المنتجات",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }
        });
    }

    // =========================================================
    // الحصول على Cart Token
    // =========================================================

    private void getCartToken() {

        Request request = new Request.Builder()
                .url(CART_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(
                    Call call,
                    IOException e
            ) {

                runOnUiThread(() -> {

                    Toast.makeText(
                            MainActivity.this,
                            "تعذر الاتصال بالسلة",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }

            @Override
            public void onResponse(
                    Call call,
                    Response response
            ) throws IOException {

                try {

                    String token =
                            response.header("Cart-Token");

                    if (token != null
                            && !token.trim().isEmpty()) {

                        cartToken = token;

                    }

                } catch (Exception ignored) {
                }
            }
        });
    }

    // =========================================================
    // عرض المنتجات
    // =========================================================

    private void displayProducts(JSONArray products) {

        if (productsContainer == null) {
            return;
        }

        productsContainer.removeAllViews();

        if (products.length() == 0) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "لا توجد منتجات حالياً"
            );

            empty.setTextSize(18);

            empty.setGravity(
                    Gravity.CENTER
            );

            empty.setPadding(
                    20,
                    50,
                    20,
                    50
            );

            productsContainer.addView(empty);

            return;
        }

        for (int i = 0;
             i < products.length();
             i++) {

            try {

                JSONObject product =
                        products.getJSONObject(i);

                int productId =
                        product.optInt("id", 0);

                String name =
                        product.optString(
                                "name",
                                "منتج"
                        );

                String price =
                        getPrice(product);

                boolean inStock =
                        product.optBoolean(
                                "is_in_stock",
                                true
                        );

                String imageUrl =
                        getImage(product);

                addProductCard(
                        productId,
                        name,
                        price,
                        imageUrl,
                        inStock
                );

            } catch (Exception ignored) {
            }
        }
    }

    // =========================================================
    // السعر
    // =========================================================

    private String getPrice(JSONObject product) {

        try {

            JSONObject prices =
                    product.optJSONObject("prices");

            if (prices == null) {
                return "";
            }

            String rawPrice =
                    prices.optString(
                            "price",
                            ""
                    );

            String symbol =
                    prices.optString(
                            "currency_symbol",
                            ""
                    );

            return formatPrice(rawPrice)
                    + " "
                    + symbol;

        } catch (Exception e) {

            return "";
        }
    }

    // =========================================================
    // صورة المنتج
    // =========================================================

    private String getImage(JSONObject product) {

        try {

            JSONArray images =
                    product.optJSONArray("images");

            if (images == null
                    || images.length() == 0) {

                return "";
            }

            JSONObject image =
                    images.optJSONObject(0);

            if (image == null) {
                return "";
            }

            return image.optString(
                    "src",
                    ""
            );

        } catch (Exception e) {

            return "";
        }
    }

    // =========================================================
    // بطاقة المنتج
    // =========================================================

    private void addProductCard(
            int productId,
            String name,
            String price,
            String imageUrl,
            boolean inStock
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                20,
                20,
                20,
                20
        );

        card.setBackgroundColor(
                Color.WHITE
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                16,
                10,
                16,
                10
        );

        card.setLayoutParams(cardParams);

        // -----------------------------------------------------
        // الصورة
        // -----------------------------------------------------

        ImageView image =
                new ImageView(this);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        220
                );

        image.setLayoutParams(imageParams);

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        if (!imageUrl.isEmpty()) {

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(
                            android.R.drawable.ic_menu_gallery
                    )
                    .error(
                            android.R.drawable.ic_menu_gallery
                    )
                    .into(image);
        }

        card.addView(image);

        // -----------------------------------------------------
        // الاسم
        // -----------------------------------------------------

        TextView nameText =
                new TextView(this);

        nameText.setText(name);

        nameText.setTextSize(18);

        nameText.setTextColor(
                Color.rgb(23, 32, 23)
        );

        nameText.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        nameText.setPadding(
                0,
                15,
                0,
                5
        );

        card.addView(nameText);

        // -----------------------------------------------------
        // السعر
        // -----------------------------------------------------

        TextView priceText =
                new TextView(this);

        priceText.setText(price);

        priceText.setTextSize(17);

        priceText.setTextColor(
                Color.rgb(22, 163, 74)
        );

        priceText.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        card.addView(priceText);

        // -----------------------------------------------------
        // المخزون
        // -----------------------------------------------------

        TextView stockText =
                new TextView(this);

        if (inStock) {

            stockText.setText(
                    "● متوفر"
            );

            stockText.setTextColor(
                    Color.rgb(22, 163, 74)
            );

        } else {

            stockText.setText(
                    "● غير متوفر"
            );

            stockText.setTextColor(
                    Color.RED
            );
        }

        stockText.setPadding(
                0,
                8,
                0,
                8
        );

        card.addView(stockText);

        // -----------------------------------------------------
        // زر السلة
        // -----------------------------------------------------

        Button button =
                new Button(this);

        button.setText(
                "🛒 إضافة إلى السلة"
        );

        button.setEnabled(inStock);

        button.setOnClickListener(
                v -> {

                    if (cartToken == null
                            || cartToken.isEmpty()) {

                        Toast.makeText(
                                MainActivity.this,
                                "السلة غير جاهزة، حاول مرة أخرى",
                                Toast.LENGTH_SHORT
                        ).show();

                        getCartToken();

                        return;
                    }

                    addToCart(
                            productId,
                            button
                    );
                }
        );

        card.addView(button);

        productsContainer.addView(card);
    }

    // =========================================================
    // إضافة إلى السلة
    // =========================================================

    private void addToCart(
            int productId,
            Button button
    ) {

        if (cartToken == null
                || cartToken.isEmpty()) {

            Toast.makeText(
                    this,
                    "السلة غير جاهزة",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        button.setEnabled(false);

        button.setText(
                "جاري الإضافة..."
        );

        String url =
                ADD_TO_CART_URL
                        + "?id="
                        + productId
                        + "&quantity=1";

        okhttp3.RequestBody body =
                okhttp3.RequestBody.create(
                        new byte[0],
                        null
                );

        Request request =
                new Request.Builder()
                        .url(url)
                        .header(
                                "Cart-Token",
                                cartToken
                        )
                        .post(body)
                        .build();

        client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        runOnUiThread(() -> {

                            button.setEnabled(true);

                            button.setText(
                                    "🛒 إضافة إلى السلة"
                            );

                            Toast.makeText(
                                    MainActivity.this,
                                    "تعذر الاتصال بالسلة",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        String bodyText =
                                response.body() != null
                                        ? response.body().string()
                                        : "";

                        if (response.isSuccessful()) {

                            runOnUiThread(() -> {

                                button.setEnabled(true);

                                button.setText(
                                        "✓ تمت الإضافة"
                                );

                                Toast.makeText(
                                        MainActivity.this,
                                        "تمت إضافة المنتج للسلة ✓",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });

                        } else {

                            String error =
                                    getWooError(bodyText);

                            runOnUiThread(() -> {

                                button.setEnabled(true);

                                button.setText(
                                        "🛒 إضافة إلى السلة"
                                );

                                Toast.makeText(
                                        MainActivity.this,
                                        error,
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
                }
        );
    }

    // =========================================================
    // خطأ WooCommerce
    // =========================================================

    private String getWooError(
            String json
    ) {

        try {

            JSONObject object =
                    new JSONObject(json);

            String message =
                    object.optString(
                            "message",
                            ""
                    );

            if (!message.isEmpty()) {
                return message;
            }

            String code =
                    object.optString(
                            "code",
                            ""
                    );

            if (!code.isEmpty()) {

                return "خطأ WooCommerce: "
                        + code;
            }

        } catch (Exception ignored) {
        }

        return "تعذر إضافة المنتج إلى السلة";
    }

    // =========================================================
    // تنسيق السعر
    // =========================================================

    private String formatPrice(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "";
        }

        try {

            double number =
                    Double.parseDouble(value);

            if (number ==
                    Math.floor(number)) {

                return String.valueOf(
                        (long) number
                );
            }

            return String.format(
                    java.util.Locale.US,
                    "%.2f",
                    number
            );

        } catch (Exception e) {

            return value;
        }
    }
}
