package ru.yourok.torrserve.ui.activities.main

import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.yourok.torrserve.BuildConfig
import ru.yourok.torrserve.R
import ru.yourok.torrserve.app.App
import ru.yourok.torrserve.ext.clearStackFragment
import ru.yourok.torrserve.server.api.Api
import ru.yourok.torrserve.server.local.TorrService
import ru.yourok.torrserve.settings.Settings
import ru.yourok.torrserve.ui.fragments.add.AddFragment
import ru.yourok.torrserve.ui.fragments.donate.DonateFragment
import ru.yourok.torrserve.ui.fragments.donate.DonateMessage
import ru.yourok.torrserve.ui.fragments.main.servfinder.ServerFinderFragment
import ru.yourok.torrserve.ui.fragments.main.servsets.ServerSettingsFragment
import ru.yourok.torrserve.ui.fragments.main.settings.SettingsFragment
import ru.yourok.torrserve.ui.fragments.main.torrents.TorrentsFragment
import ru.yourok.torrserve.ui.fragments.main.update.apk.ApkUpdateFragment
import ru.yourok.torrserve.ui.fragments.main.update.apk.UpdaterApk
import ru.yourok.torrserve.ui.fragments.main.update.server.ServerUpdateFragment
import ru.yourok.torrserve.ui.fragments.main.update.server.UpdaterServer
import ru.yourok.torrserve.utils.Format.dp2px
import ru.yourok.torrserve.utils.Net
import ru.yourok.torrserve.utils.Permission
import ru.yourok.torrserve.utils.ThemeUtil
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: StatusViewModel
    private val themeUtil = ThemeUtil()
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (BuildConfig.DEBUG) Log.d("MainActivity", "handleOnBackPressed()")
            if (isMenuVisible)
                closeMenu()
            else if (supportFragmentManager.backStackEntryCount > 0)
                supportFragmentManager.popBackStack()
            else finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        themeUtil.onCreate(this)
        setContentView(R.layout.main_activity)

        Permission.requestPermissionWithRationale(this)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        viewModel = ViewModelProvider(this)[StatusViewModel::class.java]

        setupNavigator()

        lifecycleScope.launch(Dispatchers.IO) {
            TorrService.start()
            if (TorrService.wait(10)) {
                if (TorrService.isLocal()) {
                    val ver = Api.echo()
                    if (ver.startsWith("1.1.")) {
                        withContext(Dispatchers.Main) {
                            ServerUpdateFragment().show(this@MainActivity, R.id.container, true)
                            App.toast(R.string.need_update_server, true)
                        }
                    }
                } else {
                    val ver = Api.echo()
                    if (ver.startsWith("1.1.")) {
                        withContext(Dispatchers.Main) {
                            App.toast(R.string.not_support_old_server, true)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (App.inForeground)
                        if (TorrService.isLocal())
                            ServerUpdateFragment().show(this@MainActivity, R.id.container, true)
                        else
                            ServerFinderFragment().show(this@MainActivity, R.id.container, true)
                    App.toast(R.string.need_install_server, true)
                }
            }
        }

        if (savedInstanceState == null) {
            clearStackFragment()
            TorrentsFragment().apply {
                show(this@MainActivity, R.id.container)
            }
            DonateMessage.showDonate(this)
            checkUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        themeUtil.onResume(this)
        //TorrService.start()
        updateStatus()
        if (Settings.showFab()) setupFab()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        themeUtil.onConfigurationChanged(this, newConfig)
    }

    private fun updateStatus() {
        val host = viewModel.getHost()
        host.observe(this) {
            findViewById<TextView>(R.id.tvCurrentHost)?.text = it.removePrefix("http://")
        }
        val data = viewModel.get()
        data.observe(this) {
            findViewById<TextView>(R.id.tvStatus)?.text = it
        }
    }

    private val isMenuVisible: Boolean
        get() {
            return findViewById<DrawerLayout>(R.id.drawerLayout)?.isDrawerOpen(GravityCompat.START) == true
        }

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerOpened(drawerView: View) {
            super.onDrawerOpened(drawerView)
            if (Settings.showFab()) showFab(false)
        }

        override fun onDrawerClosed(drawerView: View) {
            super.onDrawerClosed(drawerView)
            if (Settings.showFab()) showFab(true)
        }
    }

    private fun closeMenu() {
        findViewById<DrawerLayout>(R.id.drawerLayout)?.apply {
            addDrawerListener(drawerListener)
            closeDrawers()
        }
    }

    private fun openMenu() {
        findViewById<DrawerLayout>(R.id.drawerLayout)?.apply {
            addDrawerListener(drawerListener)
            openDrawer(GravityCompat.START)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        supportFragmentManager.fragments.forEach {
            when (it) {
                is AddFragment ->
                    if (it.onKeyUp(keyCode))
                        return true

                is TorrentsFragment ->
                    if (it.onKeyUp(keyCode))
                        return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        supportFragmentManager.fragments.forEach {
            when (it) {
                is AddFragment -> {
                    if (it.onKeyDown(keyCode))
                        return true
                }

                is TorrentsFragment ->
                    if (it.onKeyDown(keyCode))
                        return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isMenuVisible)
                closeMenu()
            else
                openMenu()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (UpdaterApk.check())
                withContext(Dispatchers.Main) {
                    App.toast(R.string.found_new_app_update, true)
                }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            UpdaterServer.check()
        }
    }

    private fun showFab(show: Boolean = true) {
        val fab: FloatingActionButton? = findViewById(R.id.fab)
        if (show)
            fab?.show()
        else
            fab?.hide()
    }

    private fun setupFab() { // Fab TODO: animate?
        val fab: FloatingActionButton? = findViewById(R.id.fab)
        fab?.apply {
            setImageDrawable(AppCompatResources.getDrawable(this.context, R.mipmap.ic_launcher)) // R.drawable.ts_round
            customSize = dp2px(32f)
            setMaxImageSize(dp2px(30f))
            setOnClickListener {
                if (isMenuVisible) {
                    closeMenu()
                    showFab(true)
                } else {
                    openMenu()
                    showFab(false)
                }
            }
        }
        if (!isMenuVisible) {
            showFab(true)
        }
    }

    private fun setupNavigator() {
        // Logo
        findViewById<FrameLayout>(R.id.header)?.setOnClickListener {
            val currFragment = supportFragmentManager.findFragmentById(R.id.container)
            if (currFragment is TorrentsFragment)
                ServerFinderFragment().show(this, R.id.container, true)
            else {
                clearStackFragment()
                TorrentsFragment().show(this, R.id.container)
            }
            closeMenu()
        }
        findViewById<FrameLayout>(R.id.header)?.setOnLongClickListener {
            ServerSettingsFragment().show(this, R.id.container, true)
            closeMenu()
            true
        }

        findViewById<FrameLayout>(R.id.btnAdd)?.setOnClickListener {
            AddFragment().show(this@MainActivity, R.id.container, true)
            closeMenu()
        }

        findViewById<FrameLayout>(R.id.btnRemoveAll)?.setOnClickListener { _ ->
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.remove_all_warn)
                .setPositiveButton(R.string.yes) { dialog, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val list = Api.listTorrent()
                            list.forEach {
                                Api.remTorrent(it.hash)
                            }
                            withContext(Dispatchers.Main) { dialog.dismiss() }
                        } catch (e: Exception) {
                            e.message?.let {
                                App.toast(it)
                            }
                        }
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
            dialog.getButton(BUTTON_POSITIVE)?.requestFocus()
            closeMenu()
        }

        findViewById<FrameLayout>(R.id.btnPlaylist)?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (Api.listTorrent().isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.parse(Net.getHostUrl("/playlistall/all.m3u")), "video/*")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        App.context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.message?.let {
                        App.toast(it)
                    }
                }
            }
            closeMenu()
        }

        findViewById<FrameLayout>(R.id.btnDonate)?.setOnClickListener {
            DonateFragment().show(this, R.id.container, true)
            closeMenu()
        }

        findViewById<FrameLayout>(R.id.btnUpdate)?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                if (UpdaterApk.check())
                    withContext(Dispatchers.Main) {
                        ApkUpdateFragment().show(this@MainActivity, R.id.container, true)
                    }
                else
                    withContext(Dispatchers.Main) {
                        ServerUpdateFragment().show(this@MainActivity, R.id.container, true)
                    }
            }
            closeMenu()
        }

        findViewById<FrameLayout>(R.id.btnSettings)?.setOnClickListener {
            SettingsFragment().show(this@MainActivity, R.id.container)
            closeMenu()
        }

        findViewById<FrameLayout>(R.id.btnExit)?.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(getString(R.string.exit_text))
                .setPositiveButton(R.string.exit) { _, _ ->
                    TorrService.stop()
                    finishAffinity()
                    exitProcess(0)
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
            dialog.getButton(BUTTON_POSITIVE)?.requestFocus()
            closeMenu()
        }
    }
}