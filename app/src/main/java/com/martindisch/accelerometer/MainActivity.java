package com.martindisch.accelerometer;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnStatusListener {

    private Fragment mCurrentFragment;
    private FragmentManager mFragmentManager;
    private SwipeRefreshLayout mSwipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        mSwipeContainer.setEnabled(false);
        mSwipeContainer.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));

        // exit if the device doesn't have BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_SHORT).show();
            finish();
        }

        // load ScanFragment
        mFragmentManager = getSupportFragmentManager();
        mCurrentFragment = ScanFragment.newInstance();
        mFragmentManager.beginTransaction().replace(R.id.container, mCurrentFragment).commit();
    }

    @Override
    public void onListFragmentInteraction(String address) {
        mCurrentFragment = DeviceFragment.newInstance(address);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
        transaction.replace(R.id.container, mCurrentFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onShowProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeContainer.setRefreshing(true);
            }
        });
    }

    @Override
    public void onHideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeContainer.setRefreshing(false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.about_title);
                builder.setMessage(R.string.about_message);
                builder.setNegativeButton(R.string.github, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(getString(R.string.github_url)));
                        startActivity(intent);
                    }
                });
                builder.setPositiveButton(R.string.close, null);
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
