package com.future.futurelauncher.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.future.futurelauncher.MainActivity
import com.future.futurelauncher.theme.ThemeClient
import com.future.futurelauncher.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val pm = context.packageManager
    
    var pages by mutableStateOf<List<List<LauncherItem>>>(listOf(List(16) { LauncherItem.Empty() }))
    var homePageIndex by mutableStateOf(0)
    var dialogState by mutableStateOf<LauncherDialog>(LauncherDialog.None)
    var focusedIndex by mutableStateOf(0)
    var pendingSlot by mutableStateOf<Pair<Int, Int>?>(null)
    var isEditMode by mutableStateOf(false)
    var isReorderMode by mutableStateOf(false)
    var isTopBarFocused by mutableStateOf(false)
    var topBarSelectedIndex by mutableStateOf(0)
    var isLeftPlusFocused by mutableStateOf(false)
    var isRightPlusFocused by mutableStateOf(false)
    var isEditModeBottomBarFocused by mutableStateOf(false)
    var editModeSelectedIndex by mutableStateOf(0)
    var theme by mutableStateOf(FutureTheme())
    var restrictToDefaultApps by mutableStateOf(true)
        private set

    companion object {
        private const val PREFS_NAME = "launcher_prefs"
        private const val KEY_PAGES_COUNT = "pages_count"
        private const val KEY_PAGE_PREFIX = "page_"
        private const val KEY_HOME_PAGE = "home_page"
        private const val KEY_REMOVED_APPS = "removed_app_ids"
        private const val KEY_RESTRICT_DEFAULT_APPS = "restrict_default_apps"
    }

    init {
        loadData()
        loadTheme()
        loadSettings()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restrictToDefaultApps = prefs.getBoolean(KEY_RESTRICT_DEFAULT_APPS, true)
    }

    /** אין הגדרה גלויה למשתמש להסרת ההגבלה - היא נפתחת רק דרך קוד סודי (5357)
     *  בתוך בורר האפליקציות, ונשארת פתוחה לצמיתות ברגע שנפתחה, בדיוק כמו אפשרויות למפתחים. */
    fun unlockAllApps() {
        if (restrictToDefaultApps) {
            restrictToDefaultApps = false
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_RESTRICT_DEFAULT_APPS, false).apply()
        }
    }

    // מקור האמת לעיצוב (כהה/בהיר, צבע הדגשה) הוא ThemeProvider של FutureUI -
    // משותף בין כל אפליקציות FutureOS, לא רק תוך-אפליקציה.
    fun loadTheme() {
        val shared = ThemeClient.getTheme(context)
        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
    }

    fun loadData() {
        viewModelScope.launch {
            val (savedPagesJsons, savedHomeIndex) = loadPagesFromPrefs()
            homePageIndex = savedHomeIndex
            val removedIds = loadRemovedAppIds()

            // pm.queryIntentActivities ו-loadLabel הם קריאות דיסק/binder אמיתיות -
            // הרצה שלהן ב-Dispatchers.Main (ברירת המחדל של viewModelScope) חוסמת
            // את ה-UI thread ועלולה לגרום ל-ANR, בעיקר עם הרבה אפליקציות מותקנות.
            val apps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                // אפליקציות שהוסרו במפורש ממסך הבית (removeItem) לא חוזרות אוטומטית -
                // "הוסר" אומר הוסר, לא "עדיין לא מוצג". הן עדיין מותקנות ונגישות דרך
                // "הוספת אפליקציה" בעריכה, פשוט לא נדחפות מחדש בכל טעינה.
                pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    val activityName = resolveInfo.activityInfo.name
                    val stableId = "app:$packageName/$activityName"
                    if (stableId in removedIds) return@mapNotNull null
                    LauncherItem.App(
                        id = stableId,
                        resolveInfo = resolveInfo,
                        label = resolveInfo.loadLabel(pm).toString()
                    )
                }
            }

            if (savedPagesJsons.isNotEmpty()) {
                val reconstructedPages = mutableListOf<List<LauncherItem>>()
                val remainingApps = apps.toMutableList()

                savedPagesJsons.forEach { pageJson ->
                    val pageItems = mutableListOf<LauncherItem>()
                    val jsonArray = JSONArray(pageJson)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getString("id")
                        val type = obj.optString("type", "app")
                        val label = obj.optString("label", "")
                        val customLabel = if (obj.isNull("customLabel")) null else obj.optString("customLabel")

                        val item = when (type) {
                            "app" -> {
                                val app = remainingApps.find { it.id == id }
                                if (app != null) {
                                    remainingApps.remove(app)
                                    app.apply { this.customLabel = customLabel }
                                } else {
                                    LauncherItem.Empty()
                                }
                            }
                            "folder" -> {
                                val folder = LauncherItem.Folder(id = id, label = label)
                                folder.customLabel = customLabel
                                val appsArray = obj.optJSONArray("apps")
                                if (appsArray != null) {
                                    for (j in 0 until appsArray.length()) {
                                        val appId = appsArray.getString(j)
                                        val app = remainingApps.find { it.id == appId }
                                        if (app != null) {
                                            remainingApps.remove(app)
                                            folder.apps.add(app)
                                        }
                                    }
                                }
                                folder
                            }
                            "widget" -> {
                                LauncherItem.Widget(
                                    id = id,
                                    widgetId = obj.getInt("widgetId"),
                                    label = label,
                                    spanX = obj.optInt("spanX", 1),
                                    spanY = obj.optInt("spanY", 1)
                                ).apply { this.customLabel = customLabel }
                            }
                            else -> {
                                LauncherItem.Empty(id = id).apply {
                                    this.isOccupiedBy = if (obj.isNull("occupiedBy")) null else obj.optString("occupiedBy")
                                }
                            }
                        }
                        pageItems.add(item)
                    }
                    while (pageItems.size < 16) pageItems.add(LauncherItem.Empty())
                    reconstructedPages.add(pageItems)
                }

                if (remainingApps.isNotEmpty()) {
                    val sortedRemaining = remainingApps.sortedBy { it.label.lowercase() }
                    sortedRemaining.chunked(16).forEach { chunk ->
                        val newPage = chunk.toMutableList<LauncherItem>()
                        while (newPage.size < 16) newPage.add(LauncherItem.Empty())
                        reconstructedPages.add(newPage)
                    }
                }
                pages = reconstructedPages
            } else {
                val sortedApps = apps.sortedBy { it.label.lowercase() }
                val initialPages = sortedApps.chunked(16).map { chunk ->
                    val page = chunk.toMutableList<LauncherItem>()
                    while (page.size < 16) page.add(LauncherItem.Empty())
                    page.toList()
                }
                pages = if (initialPages.isEmpty()) listOf(List(16) { LauncherItem.Empty() }) else initialPages
            }
        }
    }

    private fun loadRemovedAppIds(): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_REMOVED_APPS, emptySet()) ?: emptySet()
    }

    private fun markAppRemoved(appId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = loadRemovedAppIds().toMutableSet()
        current.add(appId)
        prefs.edit().putStringSet(KEY_REMOVED_APPS, current).apply()
    }

    private fun loadPagesFromPrefs(): Pair<List<String>, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pagesCount = prefs.getInt(KEY_PAGES_COUNT, 0)
        val homePage = prefs.getInt(KEY_HOME_PAGE, 0)
        val pagesJsons = mutableListOf<String>()
        for (i in 0 until pagesCount) {
            prefs.getString(KEY_PAGE_PREFIX + i, null)?.let { pagesJsons.add(it) }
        }
        return pagesJsons to homePage
    }

    fun savePages() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(KEY_PAGES_COUNT, pages.size)
        editor.putInt(KEY_HOME_PAGE, homePageIndex)
        pages.forEachIndexed { index, pageItems ->
            val jsonArray = JSONArray()
            pageItems.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("label", item.label)
                obj.put("customLabel", item.customLabel)
                when (item) {
                    is LauncherItem.App -> obj.put("type", "app")
                    is LauncherItem.Folder -> {
                        obj.put("type", "folder")
                        val appIds = JSONArray()
                        item.apps.forEach { appIds.put(it.id) }
                        obj.put("apps", appIds)
                    }
                    is LauncherItem.Widget -> {
                        obj.put("type", "widget")
                        obj.put("widgetId", item.widgetId)
                        obj.put("spanX", item.spanX)
                        obj.put("spanY", item.spanY)
                    }
                    is LauncherItem.Empty -> {
                        obj.put("type", "empty")
                        obj.put("occupiedBy", item.isOccupiedBy)
                    }
                }
                jsonArray.put(obj)
            }
            editor.putString(KEY_PAGE_PREFIX + index, jsonArray.toString())
        }
        editor.apply()
    }

    fun addWidget(widgetId: Int, currentPage: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return
        val newWidget = LauncherItem.Widget(
            id = "widget:${UUID.randomUUID()}",
            widgetId = widgetId,
            label = widgetInfo.loadLabel(pm) ?: "Widget"
        )

        val newPages = pages.toMutableList()
        val currentPageItems = newPages[currentPage].toMutableList()
        
        val targetIndex = pendingSlot?.let { if (it.first == currentPage) it.second else null } ?: 
                         currentPageItems.indexOfFirst { it is LauncherItem.Empty && it.isOccupiedBy == null }

        if (targetIndex != -1) {
            currentPageItems[targetIndex] = newWidget
            newPages[currentPage] = currentPageItems
            pages = newPages
            savePages()
            pendingSlot = null
        }
    }

    fun addFolder(pageIndex: Int, itemIndex: Int, name: String) {
        val newPages = pages.toMutableList()
        val pageItems = newPages[pageIndex].toMutableList()
        pageItems[itemIndex] = LauncherItem.Folder(
            id = "folder:${UUID.randomUUID()}",
            label = name
        )
        newPages[pageIndex] = pageItems
        pages = newPages
        savePages()
    }

    fun recalculateOccupiedSlots(items: MutableList<LauncherItem>) {
        items.forEachIndexed { _, item ->
            if (item is LauncherItem.Empty) item.isOccupiedBy = null
        }
        items.forEachIndexed { idx, item ->
            if (item is LauncherItem.Widget) {
                for (y in 0 until item.spanY) {
                    for (x in 0 until item.spanX) {
                        if (x == 0 && y == 0) continue
                        val occupiedIndex = idx + y * 4 + x
                        if (occupiedIndex < items.size && items[occupiedIndex] is LauncherItem.Empty) {
                            (items[occupiedIndex] as LauncherItem.Empty).isOccupiedBy = item.id
                        }
                    }
                }
            }
        }
    }

    fun removeItem(pageIndex: Int, itemIndex: Int) {
        val newPages = pages.toMutableList()
        val pageItems = newPages[pageIndex].toMutableList()
        val item = pageItems[itemIndex]

        if (item is LauncherItem.App) {
            markAppRemoved(item.id)
        }

        if (item is LauncherItem.Widget) {
            // Remove all associated occupied slots
            pageItems.forEachIndexed { idx, it ->
                if (it is LauncherItem.Empty && it.isOccupiedBy == item.id) {
                    pageItems[idx] = LauncherItem.Empty()
                }
            }
        }
        
        pageItems[itemIndex] = LauncherItem.Empty()
        recalculateOccupiedSlots(pageItems)
        newPages[pageIndex] = pageItems
        pages = newPages
        savePages()
    }

    fun moveItemBetweenPages(fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int) {
        val newPages = pages.toMutableList()
        val fromPageItems = newPages[fromPage].toMutableList()
        val toPageItems = newPages[toPage].toMutableList()
        
        val itemFrom = fromPageItems[fromIndex]
        val itemTo = toPageItems[toIndex]
        
        // Prevention of overlapping widgets
        if (itemFrom is LauncherItem.Widget || itemTo is LauncherItem.Widget) {
            // Simple check: don't allow moving/swapping widgets across pages for now if it's complex, 
            // or just ensure we recalculate everything.
            // But if it's an occupied slot, we definitely shouldn't move there.
            if ((itemFrom is LauncherItem.Empty && itemFrom.isOccupiedBy != null) ||
                (itemTo is LauncherItem.Empty && itemTo.isOccupiedBy != null)) {
                return
            }
        }

        fromPageItems[fromIndex] = itemTo
        toPageItems[toIndex] = itemFrom
        
        recalculateOccupiedSlots(fromPageItems)
        recalculateOccupiedSlots(toPageItems)
        
        newPages[fromPage] = fromPageItems
        newPages[toPage] = toPageItems
        pages = newPages
        savePages()
    }

    fun swapItems(pageIndex: Int, from: Int, to: Int) {
        val newPages = pages.toMutableList()
        val pageItems = newPages[pageIndex].toMutableList()
        
        val itemFrom = pageItems[from]
        val itemTo = pageItems[to]
        
        // Prevent putting something ON an occupied widget slot
        if ((itemTo is LauncherItem.Empty && itemTo.isOccupiedBy != null) ||
            (itemFrom is LauncherItem.Empty && itemFrom.isOccupiedBy != null)) {
            return
        }

        Collections.swap(pageItems, from, to)
        recalculateOccupiedSlots(pageItems)
        newPages[pageIndex] = pageItems
        pages = newPages
        focusedIndex = to
        savePages()
    }

    fun addPage(atIndex: Int) {
        val newPages = pages.toMutableList()
        newPages.add(atIndex, List(16) { LauncherItem.Empty() })
        pages = newPages
        if (homePageIndex >= atIndex) homePageIndex++
        savePages()
    }

    fun removePage(index: Int) {
        if (pages.size > 1) {
            // כל אפליקציה שהייתה בעמוד (כולל בתוך תיקיות) צריכה להיחשב "הוסרה
            // במפורש", אחרת loadData() ייצור לה עמוד חדש בפעם הבאה שהוא רץ
            // (למשל אחרי הפעלה מחדש של המכשיר) - בדיוק כמו removeItem בודד.
            pages[index].forEach { item ->
                when (item) {
                    is LauncherItem.App -> markAppRemoved(item.id)
                    is LauncherItem.Folder -> item.apps.forEach { markAppRemoved(it.id) }
                    else -> {}
                }
            }
            val newPages = pages.toMutableList()
            newPages.removeAt(index)
            pages = newPages
            homePageIndex = homePageIndex.coerceIn(0, newPages.size - 1)
            savePages()
        }
    }

    /** מוחק את הפריסה השמורה (עמודים, תיקיות, ווידג'טים) וחוזר לסידור אלפביתי של האפליקציות. */
    fun resetLayout() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        homePageIndex = 0
        loadData()
    }
}
