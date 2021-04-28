package ap.mailo.main;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import ap.mailo.R;

public class MainActivity extends AppCompatActivity {

    public static final String KEY_FolderName = "folderName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}