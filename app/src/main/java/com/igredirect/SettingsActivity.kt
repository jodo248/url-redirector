package com.igredirect

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.igredirect.databinding.ActivitySettingsBinding
import com.igredirect.databinding.DialogAddFrontendBinding
import com.igredirect.databinding.DialogAddRuleBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var settings: AppSettings
    private lateinit var frontendAdapter: FrontendAdapter
    private lateinit var ruleAdapter: RuleAdapter

    // SAF launchers for import / export
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { writeExport(it) } }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { readImport(it) } }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        settings = SettingsManager.load(this)
        setupAdapters()
        setupButtons()
        loadAll()
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    private fun setupAdapters() {
        frontendAdapter = FrontendAdapter(
            settings.frontends,
            onToggle = { fe, checked ->
                val idx = settings.frontends.indexOfFirst { it.id == fe.id }
                if (idx >= 0) {
                    settings.frontends[idx] = fe.copy(enabled = checked)
                    save(); refreshActiveFrontendSpinner()
                }
            },
            onDelete = { fe ->
                if (fe.isBuiltIn) {
                    toast(R.string.cannot_delete_builtin); return@FrontendAdapter
                }
                MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.confirm_delete_frontend, fe.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        settings.frontends.remove(fe)
                        frontendAdapter.notifyDataSetChanged()
                        save(); refreshActiveFrontendSpinner()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
        b.recyclerFrontends.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = frontendAdapter
            isNestedScrollingEnabled = false
        }

        ruleAdapter = RuleAdapter(
            settings.rules,
            onToggle = { rule, checked ->
                val idx = settings.rules.indexOfFirst { it.id == rule.id }
                if (idx >= 0) {
                    settings.rules[idx] = rule.copy(enabled = checked)
                    save()
                }
            },
            onDelete = { rule ->
                if (rule.isBuiltIn) {
                    toast(R.string.cannot_delete_builtin); return@RuleAdapter
                }
                MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.confirm_delete_rule, rule.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        settings.rules.remove(rule)
                        ruleAdapter.notifyDataSetChanged()
                        save()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
        b.recyclerRules.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = ruleAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupButtons() {
        b.btnAddFrontend.setOnClickListener { showAddFrontendDialog() }
        b.btnAddRule.setOnClickListener    { showAddRuleDialog()     }

        b.switchStripParams.setOnCheckedChangeListener { _, checked ->
            settings = settings.copy(stripQueryParams = checked); save()
        }

        b.btnExport.setOnClickListener {
            exportLauncher.launch("igredirect_settings.json")
        }
        b.btnImport.setOnClickListener {
            importLauncher.launch("application/json")
        }
    }

    // ─── Load / refresh ──────────────────────────────────────────────────────

    private fun loadAll() {
        refreshActiveFrontendSpinner()
        refreshBrowserSpinner()
        b.switchStripParams.isChecked = settings.stripQueryParams
        updateSetupBanner()
    }

    private fun updateSetupBanner() {
        val active = settings.getActiveFrontend()
        b.cardSetupBanner.visibility =
            if (active == null) android.view.View.VISIBLE else android.view.View.GONE
    }

    // Active-frontend spinner: "Auto (first enabled)" + each enabled frontend
    private fun refreshActiveFrontendSpinner() {
        val enabledList = settings.frontends.filter { it.enabled }
        val names = listOf(getString(R.string.auto_select)) + enabledList.map { it.name }

        b.spinnerActiveFrontend.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, names).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val sel = if (settings.activeFrontendId == null) 0
                  else enabledList.indexOfFirst { it.id == settings.activeFrontendId }
                      .let { if (it < 0) 0 else it + 1 }
        b.spinnerActiveFrontend.setSelection(sel, false)

        b.spinnerActiveFrontend.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                settings = settings.copy(
                    activeFrontendId = if (pos == 0) null else enabledList[pos - 1].id
                )
                save(); updateSetupBanner()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
    }

    // Browser spinner: "System default" + all installed browsers
    private fun refreshBrowserSpinner() {
        data class BrowserItem(val label: String, val pkg: String?)

        val browsers = getInstalledBrowsers()
        val items = listOf(BrowserItem(getString(R.string.system_default), null)) +
                    browsers.map { BrowserItem(it.second, it.first) }

        b.spinnerBrowser.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, items.map { it.label }).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        val sel = if (settings.preferredBrowserPackage == null) 0
                  else items.indexOfFirst { it.pkg == settings.preferredBrowserPackage }
                      .let { if (it < 0) 0 else it }
        b.spinnerBrowser.setSelection(sel, false)

        b.spinnerBrowser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                settings = settings.copy(preferredBrowserPackage = items[pos].pkg); save()
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
    }

    /** Returns list of (packageName, appLabel) for installed browsers. */
    @Suppress("DEPRECATION")
    private fun getInstalledBrowsers(): List<Pair<String, String>> {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PackageManager.MATCH_ALL else 0
        return packageManager.queryIntentActivities(probe, flags)
            .filter  { it.activityInfo.packageName != packageName }
            .map     { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private fun showAddFrontendDialog() {
        val db = DialogAddFrontendBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_frontend)
            .setView(db.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = db.etName.text?.toString()?.trim() ?: ""
                val url  = db.etUrl.text?.toString()?.trim()?.trimEnd('/') ?: ""
                when {
                    name.isBlank() -> toast(R.string.error_name_empty)
                    url.isBlank()  -> toast(R.string.error_url_empty)
                    !url.startsWith("http") -> toast(R.string.error_url_invalid)
                    else -> {
                        settings.frontends.add(
                            FrontendInstance(SettingsManager.newId(), name, url, true, false)
                        )
                        frontendAdapter.notifyItemInserted(settings.frontends.size - 1)
                        save(); refreshActiveFrontendSpinner()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddRuleDialog() {
        val db = DialogAddRuleBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_rule)
            .setView(db.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val name    = db.etName.text?.toString()?.trim() ?: ""
                val pattern = db.etPattern.text?.toString()?.trim() ?: ""
                when {
                    name.isBlank()    -> toast(R.string.error_name_empty)
                    pattern.isBlank() -> toast(R.string.error_pattern_empty)
                    else -> {
                        try {
                            Regex(pattern) // validate
                            settings.rules.add(
                                RedirectRule(SettingsManager.newId(), name, pattern, true, false)
                            )
                            ruleAdapter.notifyItemInserted(settings.rules.size - 1)
                            save()
                        } catch (_: Exception) {
                            toast(R.string.error_invalid_regex)
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ─── Import / Export ─────────────────────────────────────────────────────

    private fun writeExport(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(SettingsManager.exportJson(this).toByteArray()) }
            toast(R.string.export_success)
        } catch (_: Exception) {
            toast(R.string.export_failed)
        }
    }

    private fun readImport(uri: Uri) {
        val json = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return
        } catch (_: Exception) {
            toast(R.string.import_failed); return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.import_settings)
            .setMessage(R.string.import_confirm_message)
            .setPositiveButton(R.string.import_replace) { _, _ ->
                if (SettingsManager.importJson(this, json)) {
                    settings = SettingsManager.load(this)
                    frontendAdapter.replace(settings.frontends)
                    ruleAdapter.replace(settings.rules)
                    loadAll()
                    toast(R.string.import_success)
                } else {
                    toast(R.string.import_failed)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun save() { SettingsManager.save(this, settings) }

    private fun toast(resId: Int) =
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}
