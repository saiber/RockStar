package com.onquantum.rockstar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.onquantum.rockstar.R;
import com.onquantum.rockstar.Settings;
import com.onquantum.rockstar.dialogs.DialogSelectPentatonic;
import com.onquantum.rockstar.guitars.GuitarAbstract;
import com.onquantum.rockstar.guitars.GuitarInterface;
import com.onquantum.rockstar.guitars.GuitarViewDefault;
import com.onquantum.rockstar.guitars.GuitarViewSlide;


/**
 * Created by saiber on 22.02.14.
 */
public class GuitarSimulatorActivity extends Activity implements DialogSelectPentatonic.OnPentatonicSelectListener,
        GuitarInterface {

    private Context context;
    GuitarAbstract guitarView;
    Handler handler;
    RelativeLayout relativeLayout;
    private View decorView;

    RelativeLayout controlPanel;
    ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = this;
        decorView = getWindow().getDecorView();

        setContentView(R.layout.guitar_layout);
        progressBar = (ProgressBar)findViewById(R.id.loading_spinner);
        relativeLayout = (RelativeLayout)findViewById(R.id.container);
        if(new Settings(context).getSlide()) {
            guitarView = new GuitarViewSlide(context);
        }else{
            guitarView = new GuitarViewDefault(context);
        }
        guitarView.setOnSoundLoadedCompleteListener(new GuitarAbstract.OnSoundLoadedCompleteListener() {
            @Override
            public void onSoundLoadedComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });
        relativeLayout.addView(guitarView);

        /*handler = new Handler() {
            public void handleMessage(android.os.Message msg){
                if(new Settings(context).getSlide()) {
                    guitarView = new GuitarViewSlide(context);
                }else{
                    guitarView = new GuitarViewDefault(context);
                }
                relativeLayout.addView(guitarView);
            };
        };*/

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                handler.sendEmptyMessage(0);
            }
        }).start();*/


        Typeface typeface = Typeface.createFromAsset(getAssets(), "font/BaroqueScript.ttf");
        ((TextView) this.findViewById(R.id.textView0)).setTypeface(typeface);

        controlPanel = (RelativeLayout)findViewById(R.id.playPentatonicPanel);

        ((ImageButton) this.findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                finish();
            }
        });
        ((ImageButton)this.findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogSelectPentatonic dialog = new DialogSelectPentatonic();
                dialog.show(getFragmentManager(),"DialogSelectPentatonic");
            }
        });
        ((ImageButton)this.findViewById(R.id.cancelButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlPanel.setVisibility(View.GONE);
                Animation animation = AnimationUtils.loadAnimation(context,R.anim.alpha_down);
                controlPanel.startAnimation(animation);
                guitarView.ClosePlayPentatonic();
            }
        });
    }

    @Override
    public void onPentatonicSelect(String fileName) {
        guitarView.LoadPentatonicFile(fileName);
    }

    @Override
    public void onPentatonicSuccessLoaded(String name) {
        controlPanel.setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.textView2)).setText(name);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.alpha_up);
        controlPanel.startAnimation(animation);
    }
}
