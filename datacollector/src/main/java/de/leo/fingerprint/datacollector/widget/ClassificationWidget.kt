package de.leo.fingerprint.datacollector.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.database.JITAI_EVENT
import de.leo.fingerprint.datacollector.database.JITAI_ID
import de.leo.fingerprint.datacollector.database.JitaiDatabase
import de.leo.fingerprint.datacollector.datacollection.DataCollectorService
import de.leo.fingerprint.datacollector.jitai.manage.Jitai
import de.leo.fingerprint.datacollector.utils.USER_CLASSIFICATION_NOW
import org.jetbrains.anko.intentFor


/**
 * Created by Leo on 30.01.2018.
 */
class ClassificationWidget() : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, Ids: IntArray) {
        for (i in Ids.indices) {
            val views = RemoteViews(context.getPackageName(), R.layout.classificationwidget)
            views.setRemoteAdapter(R.id.widget_jitai_list, context.intentFor<WidgetService>())
            views.setPendingIntentTemplate(R.id.widget_jitai_list, PendingIntent.getService
            (context,
             0,
             context.intentFor<DataCollectorService>().setAction(USER_CLASSIFICATION_NOW),
             PendingIntent.FLAG_UPDATE_CURRENT))
            appWidgetManager.updateAppWidget(Ids[i], views)
        }
    }
}

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetViews(this.applicationContext)
    }
}

class WidgetViews(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    override fun getViewAt(position: Int): RemoteViews? {
        val views = RemoteViews(context.packageName, R.layout.widgetlistitem)
        views.setTextViewText(R.id.widget_jitai_name, jitai[position].goal)
        views.setTextViewText(R.id.widget_jitai_message, jitai[position].message)
        val intent = context.intentFor<DataCollectorService>(
                JITAI_EVENT to Jitai.NOW, JITAI_ID to jitai[position].id)
        val jitai_intent = context.intentFor<DataCollectorService>(JITAI_ID to -1)
        views.setOnClickFillInIntent(R.id.widget_now, intent)
        views.setOnClickFillInIntent(R.id.widget_item_background, jitai_intent)
        return views
    }

    private lateinit var db: JitaiDatabase

    private lateinit var jitai: MutableList<Jitai>

    override fun onCreate() {
        db = JitaiDatabase.getInstance(context)
        onDataSetChanged()

    }

    override fun onDataSetChanged() {
        jitai = db.getActiveJitai()
    }

    override fun onDestroy() {}


    override fun getCount(): Int = jitai.size

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return jitai[position].id.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
