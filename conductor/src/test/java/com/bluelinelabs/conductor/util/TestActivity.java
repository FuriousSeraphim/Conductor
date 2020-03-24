package com.bluelinelabs.conductor.util;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bluelinelabs.conductor.R;

public class TestActivity extends AppCompatActivity {

    public boolean isChangingConfigurations = false;
    public boolean isDestroying = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat);
    }

    @Override
    public boolean isChangingConfigurations() {
        return isChangingConfigurations;
    }

    @Override
    public boolean isDestroyed() {
        return isDestroying || super.isDestroyed();
    }
}
