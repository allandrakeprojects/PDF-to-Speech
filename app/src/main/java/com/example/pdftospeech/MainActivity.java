package com.example.pdftospeech;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
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

    // Variables
    private boolean IsPDFView;
    private String FilePath;
    private String PageContent;
    private int PageCount;
    private int CurrentPage = 1;

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
    private void INITIALIZE(){
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

        // TTS
        textToSpeech = new TextToSpeech(this, this);
    }

    /**
     * Handle Button Listener
     */
    @Override
    public void onClick(View view) {
        // Button Add PDF
        if(view.getId() == R.id.buttonAddPDF){
            new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(1000)
                .withFilter(Pattern.compile(".*\\.pdf$")) // Filtering files and directories by file name using regexp
                .withFilterDirectories(false) // Set directories filterable (false by default)
                .withHiddenFiles(true) // Show hidden files and folders
                .start();
        }
        // Button Play
        else if(view.getId() == R.id.buttonPlay){
            ButtonPlay();
        }
        // Button Pause
        else if(view.getId() == R.id.buttonPause){
            ButtonPause();
        }
        // Button Stop
        else if(view.getId() == R.id.buttonStop){
            ButtonStop();
        }
        // Button Previous
        else if(view.getId() == R.id.buttonPrevious){
            ButtonPrevious();
        }
        // Button Next
        else if(view.getId() == R.id.buttonNext){
            ButtonNext();
        }
    }

    /**
     * Button Play
     */
    private void ButtonPlay(){
        buttonPlay.setVisibility(View.GONE);
        buttonPause.setVisibility(View.VISIBLE);
        DetailsPDF();
        Speak();
    }

    /**
     * Button Pause
     */
    private void ButtonPause(){
        buttonPlay.setVisibility(View.VISIBLE);
        buttonPause.setVisibility(View.GONE);

    }

    /**
     * Button Stop
     */
    private void ButtonStop(){
        buttonPlay.setVisibility(View.VISIBLE);
        buttonPause.setVisibility(View.GONE);
        textToSpeech.stop();
    }

    /**
     * Button Previous
     */
    private void ButtonPrevious(){
        if(CurrentPage != 1){
            CurrentPage--;

            pdfView.jumpTo(CurrentPage-1);
        }
    }

    /**
     * Button Next
     */
    private void ButtonNext(){
        if(CurrentPage < PageCount){
            CurrentPage++;

            pdfView.jumpTo(CurrentPage-1);
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

    private void DetailsPDF(){
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
    private void RequestMultiplePermission(){
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Start Settings
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            if(IsPDFView){
                ButtonStop();

                pdfView.setVisibility(View.GONE);
                linearLayoutButtons.setVisibility(View.GONE);
                buttonAddPDF.setVisibility(View.VISIBLE);

                pdfView.invalidate();

                IsPDFView = false;
            } else{
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
                Toast.makeText(getApplicationContext(), "Language is not supported on this device.", Toast.LENGTH_SHORT).show();
            } else {
                Speak();
            }

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    Toast.makeText(MainActivity.this, "asdsadasdsa done", Toast.LENGTH_SHORT).show();
                    buttonPlay.setVisibility(View.VISIBLE);
                    buttonPause.setVisibility(View.GONE);
                }

                @Override
                public void onError(String utteranceId) {
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Language is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback Detect Speak When Finished
     */
//    public void onUtteranceCompleted(String utteranceId) {
//        SetButtonEnable();
//    }

    public void SetButtonEnable(){
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
        textToSpeech.setSpeechRate(speed);
        // Pitch
        Float pitch = settings.getFloat("TTS_PITCH", 20);
        textToSpeech.setPitch(pitch);

        textToSpeech.speak(PageContent, TextToSpeech.QUEUE_FLUSH, hashMapAlarm);
    }

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