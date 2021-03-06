package com.foxlee.testdatabinding;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DataBindingUtil.setContentView(this,R.layout.activity_main);
        NewsListFragment fragment = new NewsListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentView, fragment)
                .commit();

    }

}
