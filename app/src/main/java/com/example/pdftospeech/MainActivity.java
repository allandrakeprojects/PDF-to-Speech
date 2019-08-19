package com.example.pdftospeech;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextToSpeech.OnInitListener {

    // Button
    private ImageButton buttonAddPDF;

    // ImageView
    private ImageView buttonPlay, buttonPause, buttonStop, buttonNext, buttonPrevious;

    // LinearLayout
    private LinearLayout linearLayoutButtons;

    // PDFView
    private PDFView pdfView;

    // TTS
    private TextToSpeech textToSpeech;

    // MediaPlayer
    private MediaPlayer mMediaPlayer;

    // ProgressDialog
    ProgressDialog progressDialog;

    // Variables
    private boolean IsPDFView;
    private boolean IsPause;
    private final String SOUND_PATH_TEMP = Environment.getExternalStorageDirectory().getPath() + "/pdf_tts_temp.mp3";
    private String FilePath;
    private String PageContent;
    private int PageCount;
    private int CurrentPage = 1;
    private int CurrentWord = 0;

    /**
     * Create the app
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RequestMultiplePermission();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        INITIALIZE();
    }

    /**
     * Initialize Widgets
     */
    private void INITIALIZE() {
        // Button
        buttonAddPDF = findViewById(R.id.buttonAddPDF);

        // ImageView
        buttonPlay = findViewById(R.id.buttonPlay);
        buttonPause = findViewById(R.id.buttonPause);
        buttonStop = findViewById(R.id.buttonStop);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrevious = findViewById(R.id.buttonPrevious);

        // Listener
        buttonPlay.setOnClickListener(this);
        buttonPause.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        buttonNext.setOnClickListener(this);
        buttonPrevious.setOnClickListener(this);
        buttonAddPDF.setOnClickListener(this);

        // LinearLayout
        linearLayoutButtons = findViewById(R.id.linearLayoutButtons);

        // PDFView
        pdfView = findViewById(R.id.pdfView);

        // ProgressDialog
        progressDialog = new ProgressDialog(MainActivity.this);

        // TTS
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setOnUtteranceProgressListener(mProgressListener);
    }

    /**
     * Handle Button Listener
     */
    @Override
    public void onClick(View view) {
        // Button Add PDF
        if (view.getId() == R.id.buttonAddPDF) {
            new MaterialFilePicker()
                    .withActivity(this)
                    .withRequestCode(1000)
                    .withFilter(Pattern.compile(".*\\.pdf$")) // Filtering files and directories by file name using regexp
                    .withFilterDirectories(false) // Set directories filterable (false by default)
                    .withHiddenFiles(true) // Show hidden files and folders
                    .start();
        }
        // Button Play
        else if (view.getId() == R.id.buttonPlay) {
            ButtonPlay();
        }
        // Button Pause
        else if (view.getId() == R.id.buttonPause) {
            ButtonPause();
        }
        // Button Stop
        else if (view.getId() == R.id.buttonStop) {
            ButtonStop();
        }
        // Button Previous
        else if (view.getId() == R.id.buttonPrevious) {
            ButtonPrevious();
        }
        // Button Next
        else if (view.getId() == R.id.buttonNext) {
            ButtonNext(false);
        }
    }

    /**
     * Button Play
     */
    private void ButtonPlay() {
        buttonPlay.setVisibility(View.GONE);
        buttonPause.setVisibility(View.VISIBLE);

        if(!IsPause){
            DetailsPDF();

            if (PageContent != null && PageContent.length() > 0) {
                Speak();
            } else {
                Toast.makeText(this, "Make sure PDF have text on it.", Toast.LENGTH_SHORT).show();
            }
        } else{
            mMediaPlayer.seekTo(CurrentWord);
            mMediaPlayer.start();
        }
    }

    /**
     * Button Pause
     */
    private void ButtonPause() {
        buttonPlay.setVisibility(View.VISIBLE);
        buttonPause.setVisibility(View.GONE);

        mMediaPlayer.pause();
        IsPause = true;
        CurrentWord = mMediaPlayer.getCurrentPosition();
    }

    /**
     * Button Stop
     */
    private void ButtonStop() {
        buttonPlay.setVisibility(View.VISIBLE);
        buttonPause.setVisibility(View.GONE);

        mMediaPlayer.stop();

        IsPause = false;
        CurrentWord = 0;
    }

    /**
     * Button Previous
     */
    private void ButtonPrevious() {
        if (CurrentPage != 1) {
            CurrentPage--;

            pdfView.jumpTo(CurrentPage - 1);
        }
    }

    /**
     * Button Next
     */
    private void ButtonNext(boolean IsAuto) {
        if (CurrentPage < PageCount) {
            CurrentPage++;

            pdfView.jumpTo(CurrentPage - 1);

            if (IsAuto) {
                ButtonPlay();
            }
        }
    }

    /**
     * Handle File Path of Selected PDF
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            FilePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            File file = new File(FilePath);
            PDFView pdfView = findViewById(R.id.pdfView);
            pdfView.fromFile(file)
                    .enableSwipe(false)
                    .swipeHorizontal(true)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .enableAnnotationRendering(false)
                    .password(null)
                    .scrollHandle(null)
                    .enableAntialiasing(true)
                    .spacing(0)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .load();

            String filename = file.getName();
            // Toast.makeText(this, filename, Toast.LENGTH_SHORT).show();
            pdfView.setVisibility(View.VISIBLE);
            linearLayoutButtons.setVisibility(View.VISIBLE);
            buttonAddPDF.setVisibility(View.GONE);

            IsPDFView = true;

            DetailsPDF();
        }
    }

    /**
     * Get Details of PDF
     */
    private void DetailsPDF() {
        try {
            String parsedText = "";
            PdfReader reader = new PdfReader(FilePath);
            PageCount = reader.getNumberOfPages();
            PageContent = parsedText + PdfTextExtractor.getTextFromPage(reader, CurrentPage).trim(); //Extracting the content from the different page
            reader.close();
        } catch (Exception e) {
            Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initialize Multiple Permission
     */
    private void RequestMultiplePermission() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            Toast.makeText(MainActivity.this, "To able to access the file on your phone. Allow the permission.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    /**
     * Initialize Settings Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Handle Settings Listener
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // GOTO Settings
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            // Refresh
            if (IsPDFView) {
                ButtonStop();

                pdfView.setVisibility(View.GONE);
                linearLayoutButtons.setVisibility(View.GONE);
                buttonAddPDF.setVisibility(View.VISIBLE);

                pdfView.invalidate();

                IsPDFView = false;
                CurrentPage = 1;
            } else {
                Toast.makeText(this, "You must select PDF first.", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Initialize TTS
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Toast.makeText(getApplicationContext(), "Language is not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Toast.makeText(getApplicationContext(), "Language is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Function Speak
     */
    private void Speak() {
        HashMap<String, String> hashMapAlarm = new HashMap<>();
        hashMapAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        hashMapAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "-");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        // Speed
        Float speed = settings.getFloat("TTS_SPEED", 20);
        if (!speed.toString().contains("20")) {
            textToSpeech.setSpeechRate(speed);
        }
        // Pitch
        Float pitch = settings.getFloat("TTS_PITCH", 20);
        if (!pitch.toString().contains("20")) {
            textToSpeech.setPitch(pitch);
        }

        // Handle Media Player
        File soundFile = new File(SOUND_PATH_TEMP);
        if (soundFile.exists())
            soundFile.delete();

        textToSpeech.synthesizeToFile(PageContent, hashMapAlarm, SOUND_PATH_TEMP);

        // ProgressDialog
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading, generating speech...");
        progressDialog.show();
    }

    /**
     * Override TTS
     */
    private UtteranceProgressListener mProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {

        }

        @Override
        public void onError(String utteranceId) {

        }

        @Override
        public void onDone(String utteranceId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Handle Media Player

                    // ProgressDialog
                    progressDialog.dismiss();

                    mMediaPlayer = MediaPlayer.create(MainActivity.this, Uri.parse(SOUND_PATH_TEMP));
                    mMediaPlayer.start();
                    mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            File soundFile = new File(SOUND_PATH_TEMP);
                            if (soundFile.exists())
                                soundFile.delete();

                            ButtonStop();
                            ButtonNext(true);
                        }
                    });
                    mMediaPlayer.start();
                }
            });
        }
    };

    /**
     * Destroy TTS
     */
    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}