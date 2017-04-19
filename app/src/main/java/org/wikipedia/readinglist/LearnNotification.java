package org.wikipedia.readinglist;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;

public class LearnNotification extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        DatabaseClient<ReadingListPageRow> client = WikipediaApp.getInstance().getDatabaseClient(ReadingListPageRow.class);
        Cursor c = client.select(ReadingListPageContract.PageWithDisk.URI, null, null, null);
        PageTitle title = null;
        try {
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    if (c.getInt(c.getColumnIndex("isViewed")) == 0) {
                        title = ReadingListDaoProxy.pageTitle(ReadingListPage.fromCursor(c));
                        break;
                    }
                }
            }
        } finally {
            c.close();
        }

        if (title != null) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_school_black_24dp)
                            .setContentTitle("Time to read an article!")
                            .setContentText(title.getDisplayText())
                            .setAutoCancel(true);

            HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);
            Intent resultIntent = PageActivity.newIntent(context, entry, entry.getTitle());


            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                    .addParentStack(ReadingListActivity.class)
                    .addNextIntent(resultIntent);

            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(0, mBuilder.build());
        }
    }
}