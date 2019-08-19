package com.example.pdftospeech;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.xw.repo.BubbleSeekBar;

public class SettingsActivity extends AppCompatActivity {

    // SeekBar
    BubbleSeekBar seekBarSpeed, seekBarPitch;

    /**
     * Create the app
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        INITIALIZE();
    }

    /**
     * Initialize Widgets
     */
    private void INITIALIZE(){
        // SeekBar
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        seekBarPitch = findViewById(R.id.seekBarPitch);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        // Speed
        Float speed = settings.getFloat("TTS_SPEED", 20);
        if(!speed.toString().contains("20")){
            seekBarSpeed.setProgress(speed);
        }
        // Pitch
        Float pitch = settings.getFloat("TTS_PITCH", 20);
        if(!pitch.toString().contains("20")){
            seekBarPitch.setProgress(pitch);
        }
    }

    /**
     * Initialize Back Listener
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Function to Save Speed, Pitch Settings
     */
    public void SaveTTSDetails() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("TTS_SPEED", seekBarSpeed.getProgressFloat());
        editor.putFloat("TTS_PITCH", seekBarPitch.getProgressFloat());
        editor.commit();
    }

    /**
     * Override onBackPressed to Save Speed, Pitch Settings
     */
    @Override
    public void onBackPressed() {
        SaveTTSDetails();
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
