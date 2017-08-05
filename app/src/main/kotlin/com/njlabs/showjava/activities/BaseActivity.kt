package com.njlabs.showjava.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.utils.Tools
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


abstract class BaseActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    protected lateinit var toolbar: Toolbar
    protected lateinit var context: Context
    protected lateinit var prefs: SharedPreferences

    abstract fun init(savedInstanceState: Bundle?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.storage_permission_rationale),
                    Constants.STORAGE_PERMISSION_REQUEST,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            init(savedInstanceState)
        } else {
            init(savedInstanceState)
            postPermissionsGrant()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    fun setupLayout(layoutRef: Int) {
        setContentView(layoutRef)
        setupToolbar(null)
        setupGoogleAds()
    }

    fun setupLayout(layoutRef: Int, title: String) {
        setContentView(layoutRef)
        setupToolbar(title)
        setupGoogleAds()
    }

    fun setupLayoutNoActionBar(layoutRef: Int) {
        setContentView(layoutRef)
    }

    private fun setupToolbar(title: String?) {
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (title != null) {
            supportActionBar?.title = title
        } else {
            if (isPro()) {
                val activityInfo: ActivityInfo
                try {
                    activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
                    val currentTitle = activityInfo.loadLabel(packageManager).toString()
                    if (currentTitle.trim { it <= ' ' } == "Show Java") {
                        supportActionBar?.title = "Show Java Pro"
                    }
                } catch (ignored: PackageManager.NameNotFoundException) {

                }

            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupGoogleAds() {
        val mAdView = findViewById<AdView>(R.id.adView)
        if (mAdView != null) {
            mAdView.visibility = View.GONE
            if (!isPro()) {
                val adRequest = AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .addTestDevice(getString(R.string.adUnitId))
                        .build()
                mAdView.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(errorCode: Int) {
                        super.onAdFailedToLoad(errorCode)
                        mAdView.visibility = View.GONE
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        mAdView.visibility = View.VISIBLE
                    }
                }
                mAdView.loadAd(adRequest)
                if (!Tools.checkDataConnection(context)) {
                    mAdView.visibility = View.GONE
                }
            }
        }
    }

    private fun isPro(): Boolean {
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.about_option -> {
                startActivity(Intent(baseContext, DecompilerActivity::class.java))
                // startActivity(Intent(baseContext, About::class.java))
                return true
            }
            R.id.bug_report_option -> {
                val uri = Uri.parse("https://github.com/niranjan94/show-java/issues/new")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                return true
            }
            R.id.settings_option -> {
                // startActivity(Intent(baseContext, SettingsActivity::class.java))
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    open fun postPermissionsGrant() {}

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        postPermissionsGrant()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (perms.isNotEmpty() || EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                    .build()
                    .show()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (!EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(
                        this,
                        R.string.storage_permission_rationale,
                        Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                postPermissionsGrant()
            }
        }
    }

}
