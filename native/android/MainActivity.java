package fr.tempo.sport;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TempoUpdaterPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
