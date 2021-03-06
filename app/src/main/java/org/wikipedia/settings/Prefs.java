package org.wikipedia.settings;

import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.reflect.TypeToken;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.SessionData;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.json.SessionUnmarshaller;
import org.wikipedia.json.TabUnmarshaller;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.ReleaseUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.logging.HttpLoggingInterceptor.Level;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.wikipedia.settings.PrefsIoUtil.contains;
import static org.wikipedia.settings.PrefsIoUtil.getBoolean;
import static org.wikipedia.settings.PrefsIoUtil.getInt;
import static org.wikipedia.settings.PrefsIoUtil.getKey;
import static org.wikipedia.settings.PrefsIoUtil.getLong;
import static org.wikipedia.settings.PrefsIoUtil.getString;
import static org.wikipedia.settings.PrefsIoUtil.getStringSet;
import static org.wikipedia.settings.PrefsIoUtil.remove;
import static org.wikipedia.settings.PrefsIoUtil.setBoolean;
import static org.wikipedia.settings.PrefsIoUtil.setInt;
import static org.wikipedia.settings.PrefsIoUtil.setLong;
import static org.wikipedia.settings.PrefsIoUtil.setString;
import static org.wikipedia.settings.PrefsIoUtil.setStringSet;

/** Shared preferences utility for convenient POJO access. */
public final class Prefs {
    @Nullable
    public static String getAppChannel() {
        return getString(R.string.preference_key_app_channel, null);
    }

    public static void setAppChannel(@Nullable String channel) {
        setString(R.string.preference_key_app_channel, channel);
    }

    @NonNull
    public static String getAppChannelKey() {
        return getKey(R.string.preference_key_app_channel);
    }

    @Nullable
    public static String getAppInstallId() {
        return getString(R.string.preference_key_reading_app_install_id, null);
    }

    public static void setAppInstallId(@Nullable String id) {
        // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
        setString(R.string.preference_key_reading_app_install_id, id);
    }

    @Nullable
    public static String getAppLanguageCode() {
        return getString(R.string.preference_key_language, null);
    }

    public static void setAppLanguageCode(@Nullable String code) {
        setString(R.string.preference_key_language, code);
    }

    public static int getThemeId() {
        return getInt(R.string.preference_key_color_theme, Theme.getFallback().getMarshallingId());
    }

    public static void setThemeId(int theme) {
        setInt(R.string.preference_key_color_theme, theme);
    }

    @NonNull
    public static String getCookieDomains() {
        return getString(R.string.preference_key_cookie_domains, "");
    }

    @NonNull
    public static List<String> getCookieDomainsAsList() {
        return SharedPreferenceCookieManager.makeList(getCookieDomains());
    }

    public static void setCookieDomains(@Nullable String domains) {
        setString(R.string.preference_key_cookie_domains, domains);
    }

    @NonNull
    public static String getCookiesForDomain(@NonNull String domain) {
        return getString(getCookiesForDomainKey(domain), "");
    }

    public static void setCookiesForDomain(@NonNull String domain, @Nullable String cookies) {
        setString(getCookiesForDomainKey(domain), cookies);
    }

    public static void removeCookiesForDomain(@NonNull String domain) {
        remove(getCookiesForDomainKey(domain));
    }

    public static boolean crashedBeforeActivityCreated() {
        return getBoolean(R.string.preference_key_crashed_before_activity_created, true);
    }

    public static void crashedBeforeActivityCreated(boolean crashed) {
        setBoolean(R.string.preference_key_crashed_before_activity_created, crashed);
    }

    public static boolean isCrashReportAutoUploadEnabled() {
        return getBoolean(R.string.preference_key_auto_upload_crash_reports, true);
    }

    public static boolean isShowDeveloperSettingsEnabled() {
        return getBoolean(R.string.preference_key_show_developer_settings, ReleaseUtil.isDevRelease());
    }

    public static void setShowDeveloperSettingsEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_show_developer_settings, enabled);
    }

    @NonNull
    public static String getEditTokenWikis() {
        return getString(R.string.preference_key_edittoken_wikis, "");
    }

    public static void setEditTokenWikis(@Nullable String wikis) {
        setString(R.string.preference_key_edittoken_wikis, wikis);
    }

    @Nullable
    public static String getEditTokenForWiki(@NonNull String wiki) {
        return getString(getEditTokenForWikiKey(wiki), null);
    }

    public static void setEditTokenForWiki(@NonNull String wiki, @Nullable String token) {
        setString(getEditTokenForWikiKey(wiki), token);
    }

    public static void removeEditTokenForWiki(@NonNull String wiki) {
        remove(getEditTokenForWikiKey(wiki));
    }

    public static void removeLoginUsername() {
        remove(R.string.preference_key_login_username);
    }

    @Nullable
    public static String getLoginPassword() {
        return getString(R.string.preference_key_login_password, null);
    }

    public static void setLoginPassword(@Nullable String password) {
        setString(R.string.preference_key_login_password, password);
    }

    public static boolean hasLoginPassword() {
        return contains(R.string.preference_key_login_password);
    }

    public static void removeLoginPassword() {
        remove(R.string.preference_key_login_password);
    }

    @NonNull
    public static Map<String, Integer> getLoginUserIds() {
        TypeToken<HashMap<String, Integer>> type = new TypeToken<HashMap<String, Integer>>(){};
        return GsonUnmarshaller.unmarshal(type, getString(R.string.preference_key_login_user_id_map, "{}"));
    }

    public static void setLoginUserIds(@Nullable Map<String, Integer> ids) {
        setString(R.string.preference_key_login_user_id_map, GsonMarshaller.marshal(ids));
    }

    public static void removeLoginUserIds() {
        remove(R.string.preference_key_login_user_id_map);
    }

    @Nullable
    public static String getLoginUsername() {
        return getString(R.string.preference_key_login_username, null);
    }

    public static void setLoginUsername(@Nullable String username) {
        setString(R.string.preference_key_login_username, username);
    }

    public static boolean hasLoginUsername() {
        return contains(R.string.preference_key_login_username);
    }

    @Nullable
    public static Set<String> getLoginGroups() {
        return getStringSet(R.string.preference_key_login_groups, null);
    }

    public static void setLoginGroups(@Nullable Set<String> groups) {
        setStringSet(R.string.preference_key_login_groups, groups);
    }

    public static void removeLoginGroups() {
        remove(R.string.preference_key_login_groups);
    }

    @Nullable
    public static String getMruLanguageCodeCsv() {
        return getString(R.string.preference_key_language_mru, null);
    }

    public static void setMruLanguageCodeCsv(@Nullable String csv) {
        setString(R.string.preference_key_language_mru, csv);
    }

    @NonNull
    public static String getRemoteConfigJson() {
        return getString(R.string.preference_key_remote_config, "{}");
    }

    public static void setRemoteConfigJson(@Nullable String json) {
        setString(R.string.preference_key_remote_config, json);
    }

    public static void setTabs(@NonNull List<Tab> tabs) {
        setString(R.string.preference_key_tabs, GsonMarshaller.marshal(tabs));
    }

    @NonNull
    public static List<Tab> getTabs() {
        return hasTabs()
                ? TabUnmarshaller.unmarshal(getString(R.string.preference_key_tabs, null))
                : Collections.<Tab>emptyList();
    }

    public static boolean hasTabs() {
        return contains(R.string.preference_key_tabs);
    }

    public static void clearTabs() {
        remove(R.string.preference_key_tabs);
    }

    public static Set<String> getHiddenCards() {
        Set<String> emptySet = new LinkedHashSet<>();
        //noinspection unchecked
        return hasHiddenCards() ? GsonUnmarshaller.unmarshal(emptySet.getClass(),
                getString(R.string.preference_key_feed_hidden_cards, null)) : emptySet;
    }

    public static void setHiddenCards(@NonNull Set<String> cards) {
        setString(R.string.preference_key_feed_hidden_cards, GsonMarshaller.marshal(cards));
    }

    public static boolean hasHiddenCards() {
        return contains(R.string.preference_key_feed_hidden_cards);
    }

    public static int getTabCount() {
        List<Tab> tabs = getTabs();
        // handle the case where we have a single tab with an empty backstack,
        // which shouldn't count as a valid tab:
        return tabs.isEmpty() ? 0 : tabs.get(0).getBackStack().isEmpty() ? 0 : tabs.size();
    }

    public static void setSessionData(@NonNull SessionData data) {
        setString(R.string.preference_key_session_data, GsonMarshaller.marshal(data));
    }

    @NonNull
    public static SessionData getSessionData() {
        return hasSessionData()
                ? SessionUnmarshaller.unmarshal(getString(R.string.preference_key_session_data, null))
                : new SessionData();
    }

    public static boolean hasSessionData() {
        return contains(R.string.preference_key_session_data);
    }

    public static int getSessionTimeout() {
        // return the timeout, but don't let it be less than the minimum
        return Math.max(getInt(R.string.preference_key_session_timeout, SessionFunnel.DEFAULT_SESSION_TIMEOUT), SessionFunnel.MIN_SESSION_TIMEOUT);
    }

    public static int getTextSizeMultiplier() {
        return getInt(R.string.preference_key_text_size_multiplier, 0);
    }

    public static void setTextSizeMultiplier(int multiplier) {
        setInt(R.string.preference_key_text_size_multiplier, multiplier);
    }

    public static boolean isEventLoggingEnabled() {
        return getBoolean(R.string.preference_key_eventlogging_opt_in, true);
    }

    public static boolean useRestBaseSetManually() {
        return getBoolean(R.string.preference_key_use_restbase_manual, false);
    }

    public static boolean useRestBase() {
        return getBoolean(R.string.preference_key_use_restbase, false);
    }

    public static void setUseRestBase(boolean enabled) {
        setBoolean(R.string.preference_key_use_restbase, enabled);
    }

    public static int getRbTicket(int defaultValue) {
        return getInt(R.string.preference_key_restbase_ticket, defaultValue);
    }

    public static void setRbTicket(int rbTicket) {
        setInt(R.string.preference_key_restbase_ticket, rbTicket);
    }

    @IntRange(from = RbSwitch.FAILED) public static int getRequestSuccessCounter(int defaultValue) {
        return getInt(R.string.preference_key_request_successes, defaultValue);
    }

    public static void setRequestSuccessCounter(@IntRange(from = RbSwitch.FAILED) int successes) {
        setInt(R.string.preference_key_request_successes, successes);
    }

    public static Level getRetrofitLogLevel() {
        String prefValue = getString(R.string.preference_key_retrofit_log_level, null);
        if (prefValue == null) {
            return ReleaseUtil.isDevRelease() ? Level.BASIC : Level.NONE;
        }
        switch (prefValue) {
            case "BASIC":
                return Level.BASIC;
            case "HEADERS":
                return Level.HEADERS;
            case "BODY":
                return Level.BODY;
            case "NONE":
            default:
                return Level.NONE;
        }
    }

    @NonNull
    public static String getRestbaseUriFormat() {
        return defaultIfBlank(getString(R.string.preference_key_restbase_uri_format, null),
                "%1$s://%2$s/api/rest_v1/");
    }

    @NonNull
    public static Uri getMediaWikiBaseUri() {
        return Uri.parse(defaultIfBlank(getString(R.string.preference_key_mediawiki_base_uri, null),
                Constants.WIKIPEDIA_URL));
    }

    public static boolean getMediaWikiBaseUriSupportsLangCode() {
        return getBoolean(R.string.preference_key_mediawiki_base_uri_supports_lang_code, true);
    }

    public static long getLastRunTime(@NonNull String task) {
        return getLong(getLastRunTimeKey(task), 0);
    }

    public static void setLastRunTime(@NonNull String task, long time) {
        setLong(getLastRunTimeKey(task), time);
    }

    public static long pageLastShown() {
        return getLong(R.string.preference_key_page_last_shown, 0);
    }

    public static void pageLastShown(long time) {
        setLong(R.string.preference_key_page_last_shown, time);
    }

    public static boolean isShowZeroInterstitialEnabled() {
        return getBoolean(R.string.preference_key_zero_interstitial, true);
    }

    public static int zeroConfigHashCode() {
        return getInt(R.string.preference_key_zero_config_hash, 0);
    }

    public static void zeroConfigHashCode(int hashCode) {
        setInt(R.string.preference_key_zero_config_hash, hashCode);
    }

    public static boolean isSelectTextTutorialEnabled() {
        return getBoolean(R.string.preference_key_select_text_tutorial_enabled, true);
    }

    public static void setSelectTextTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_select_text_tutorial_enabled, enabled);
    }

    public static boolean isShareTutorialEnabled() {
        return getBoolean(R.string.preference_key_share_tutorial_enabled, true);
    }

    public static void setShareTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_share_tutorial_enabled, enabled);
    }

    public static boolean isReadingListTutorialEnabled() {
        return getBoolean(R.string.preference_key_reading_list_tutorial_enabled, true);
    }

    public static void setReadingListTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_reading_list_tutorial_enabled, enabled);
    }

    public static boolean isTocTutorialEnabled() {
        return getBoolean(R.string.preference_key_toc_tutorial_enabled, true);
    }

    public static void setTocTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_toc_tutorial_enabled, enabled);
    }

    public static boolean isZeroTutorialEnabled() {
        return getBoolean(R.string.preference_key_zero_tutorial_enabled, true);
    }

    public static void setZeroTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_zero_tutorial_enabled, enabled);
    }

    public static boolean isImageDownloadEnabled() {
        return getBoolean(R.string.preference_key_show_images, true);
    }

    public static boolean areNewsCardsEnabled() {
        return getBoolean(R.string.preference_key_show_news_cards, true);
    }

    public static boolean areFeaturedArticleCardsEnabled() {
        return getBoolean(R.string.preference_key_show_featured_article_cards, true);
    }

    public static boolean areMostReadCardsEnabled() {
        return getBoolean(R.string.preference_key_show_most_read_cards, true);
    }

    public static boolean areFeaturedImageCardsEnabled() {
        return getBoolean(R.string.preference_key_show_featured_image_cards, true);
    }

    public static boolean areBecauseYouReadCardsEnabled() {
        return getBoolean(R.string.preference_key_show_because_you_read_cards, true);
    }

    public static boolean areContinueReadingCardsEnabled() {
        return getBoolean(R.string.preference_key_show_continue_reading_cards, true);
    }

    public static String getEditTokenForWikiKey(String wiki) {
        return getKey(R.string.preference_key_edittoken_for_wiki_format, wiki);
    }

    public static String getCookiesForDomainKey(@NonNull String domain) {
        return getKey(R.string.preference_key_cookies_for_domain_format, domain);
    }

    private static String getLastRunTimeKey(@NonNull String task) {
        return getKey(R.string.preference_key_last_run_time_format, task);
    }

    public static boolean isLinkPreviewEnabled() {
        return getBoolean(R.string.preference_key_show_link_previews, true);
    }

    public static int getReadingListSortMode(int defaultValue) {
        return getInt(R.string.preference_key_reading_list_sort_mode, defaultValue);
    }

    public static void setReadingListSortMode(int sortMode) {
        setInt(R.string.preference_key_reading_list_sort_mode, sortMode);
    }

    public static int getReadingListPageSortMode(int defaultValue) {
        return getInt(R.string.preference_key_reading_list_page_sort_mode, defaultValue);
    }

    public static void setReadingListPageSortMode(int sortMode) {
        setInt(R.string.preference_key_reading_list_page_sort_mode, sortMode);
    }

    public static boolean isMemoryLeakTestEnabled() {
        return getBoolean(R.string.preference_key_memory_leak_test, false);
    }

    public static boolean isDescriptionEditTutorialEnabled() {
        return getBoolean(R.string.preference_key_description_edit_tutorial_enabled, true);
    }

    public static void setDescriptionEditTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_description_edit_tutorial_enabled, enabled);
    }

    public static long getLastDescriptionEditTime() {
        return getLong(R.string.preference_key_last_description_edit_time, 0);
    }

    public static void setLastDescriptionEditTime(long time) {
        setLong(R.string.preference_key_last_description_edit_time, time);
    }

    public static int getTotalAnonDescriptionsEdited() {
        return getInt(R.string.preference_key_total_anon_descriptions_edited, 0);
    }

    public static void incrementTotalAnonDescriptionsEdited() {
        setInt(R.string.preference_key_total_anon_descriptions_edited, getTotalAnonDescriptionsEdited() + 1);
    }

    private Prefs() { }
}
