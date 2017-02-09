package com.martindisch.accelerometer;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements ScanFragment.OnListFragmentInteractionListener {

    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // exit if the device doesn't have BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_SHORT).show();
            finish();
        }

        // load ScanFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        mCurrentFragment = ScanFragment.newInstance();
        fragmentManager.beginTransaction().replace(R.id.container, mCurrentFragment).commit();
    }

    @Override
    public void onListFragmentInteraction(String address) {
        // TODO: switch to details fragment
    }
}
