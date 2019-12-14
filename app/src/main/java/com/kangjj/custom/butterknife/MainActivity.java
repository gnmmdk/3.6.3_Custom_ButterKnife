package com.kangjj.custom.butterknife;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.kangjj.custom.butterknife.annotation.BindView;
import com.kangjj.custom.butterknife.annotation.OnClick;
import com.kangjj.custom.butterknife.library.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv1)
    TextView tv1;
    @BindView(R.id.tv2)
    TextView tv2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.tv1)
    public void click(View view) {
//        Log.e("netease >>> ", "Click >>> " + tv1.getText().toString());
    }

    @OnClick(R.id.tv2)
    public void click2() {
        Log.e("netease >>> ", "OnClick -> no parameter");
    }
}
