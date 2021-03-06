package org.wikipedia.analytics;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Base class for all various types of events that are logged to EventLogging.
 *
 * Each Schema has its own class, and has its own constructor that makes it easy
 * to call from everywhere without having to duplicate param info at all places.
 * Updating schemas / revisions is also easier this way.
 */
public class EventLoggingEvent {
    private static final RequestBody EMPTY_REQ = RequestBody.create(null, new byte[0]);
    private static final String EVENTLOG_URL_PROD = "https://meta.wikimedia.org/beacon/event";
    private static final String EVENTLOG_URL_DEV = "https://deployment.wikimedia.beta.wmflabs.org/beacon/event";
    private static final String EVENTLOG_URL = ReleaseUtil.isPreBetaRelease()
            ? EVENTLOG_URL_DEV : EVENTLOG_URL_PROD;
    // https://github.com/wikimedia/mediawiki-extensions-EventLogging/blob/8b3cb1b/modules/ext.eventLogging.core.js#L57
    private static final int MAX_URL_LEN = 2000;

    private final JSONObject data;

    /**
     * Create an EventLoggingEvent that logs to a given revision of a given schema with
     * the gven data payload.
     *
     * @param schema Schema name (as specified on meta.wikimedia.org)
     * @param revID Revision of the schema to log to
     * @param wiki DBName (enwiki, dewiki, etc) of the wiki in which we are operating
     * @param eventData Data for the actual event payload. Considered to be
     *
     */
    public EventLoggingEvent(String schema, int revID, String wiki, JSONObject eventData) {
        data = new JSONObject();
        try {
            data.put("schema", schema);
            data.put("revision", revID);
            data.put("wiki", wiki);
            data.put("event", eventData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Log the current event.
     *
     * Returns immediately after queueing the network request in the background.
     */
    public void log() {
        new LogEventTask(data).execute();
    }

    private class LogEventTask extends SaneAsyncTask<Integer> {
        private final JSONObject data;

        LogEventTask(JSONObject data) {
            this.data = data;
        }

        @Override
        public Integer performTask() throws Throwable {
            String dataURL = Uri.parse(EVENTLOG_URL)
                    .buildUpon().query(data.toString())
                    .build().toString();

            if (dataURL.length() > MAX_URL_LEN) {
                L.logRemoteErrorIfProd(new RemoteLogException("EventLogging max length exceeded")
                        .put("length", String.valueOf(dataURL.length())));
            }

            Request request = new Request.Builder().url(dataURL).post(EMPTY_REQ).build();
            Response response = OkHttpConnectionFactory.getClient().newCall(request).execute();
            try {
                return response.code();
            } finally {
                response.close();
            }
        }

        @Override
        public void onCatch(Throwable caught) {
            // Do nothing bad. EL data is ok to lose.
            L.d("Lost EL data: " + data.toString());
        }
    }
}
