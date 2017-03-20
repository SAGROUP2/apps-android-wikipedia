package org.wikipedia.page.bottomcontent;

import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.LongPressHandler.ListViewContextMenuListener;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageContainerLongPressHandler;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.SuggestionsTask;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ConfigurableListView;
import org.wikipedia.views.ConfigurableTextView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import static org.wikipedia.util.L10nUtil.formatDateRelative;
import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class BottomContentHandler implements BottomContentInterface,
                                                ObservableWebView.OnScrollChangeListener,
                                                ObservableWebView.OnContentHeightChangedListener {

    private final PageFragment parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;
    private final LinkHandler linkHandler;
    private PageTitle pageTitle;
    private final WikipediaApp app;

    private View bottomContentContainer;
    private TextView pageLastUpdatedText;
    private TextView pageLicenseText;

    public BottomContentHandler(final PageFragment parentFragment,
                                CommunicationBridge bridge, ObservableWebView webview,
                                LinkHandler linkHandler, ViewGroup hidingView) {
        this.parentFragment = parentFragment;
        this.bridge = bridge;
        this.webView = webview;
        this.linkHandler = linkHandler;
        app = WikipediaApp.getInstance();

        bottomContentContainer = hidingView;
        webview.addOnScrollChangeListener(this);
        webview.addOnContentHeightChangedListener(this);

        pageLastUpdatedText = (TextView) bottomContentContainer.findViewById(R.id.page_last_updated_text);
        pageLicenseText = (TextView) bottomContentContainer.findViewById(R.id.page_license_text);

        TextView pageExternalLink = (TextView) bottomContentContainer.findViewById(R.id.page_external_link);
        pageExternalLink.setPaintFlags(pageExternalLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        pageExternalLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visitInExternalBrowser(parentFragment.getContext(), Uri.parse(pageTitle.getMobileUri()));
            }
        });

        // hide ourselves by default
        hide();
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
        if (bottomContentContainer.getVisibility() == View.GONE) {
            return;
        }
        int contentHeight = (int)(webView.getContentHeight() * DimenUtil.getDensityScalar());
        int bottomOffset = contentHeight - scrollY - webView.getHeight();
        int bottomHeight = bottomContentContainer.getHeight();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomContentContainer.getLayoutParams();
        if (bottomOffset > bottomHeight) {
            if (params.bottomMargin != -bottomHeight) {
                params.bottomMargin = -bottomHeight;
                params.topMargin = 0;
                bottomContentContainer.setLayoutParams(params);
                bottomContentContainer.setVisibility(View.INVISIBLE);
            }
        } else {
            params.bottomMargin = -bottomOffset;
            params.topMargin = -bottomHeight;
            bottomContentContainer.setLayoutParams(params);
            if (bottomContentContainer.getVisibility() != View.VISIBLE) {
                bottomContentContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onContentHeightChanged(int contentHeight) {
        if (bottomContentContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        // trigger a manual scroll event to update our position
        onScrollChanged(webView.getScrollY(), webView.getScrollY(), false);
    }

    /**
     * Hide the bottom content entirely.
     * It can only be shown again by calling beginLayout()
     */
    @Override
    public void hide() {
        bottomContentContainer.setVisibility(View.GONE);
    }

    @Override
    public void beginLayout() {
        setupAttribution();
        layoutContent();
    }

    private void layoutContent() {
        if (!parentFragment.isAdded()) {
            return;
        }
        bottomContentContainer.setVisibility(View.INVISIBLE);
        // keep trying until our layout has a height...
        if (bottomContentContainer.getHeight() == 0) {
            final int postDelay = 50;
            bottomContentContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutContent();
                }
            }, postDelay);
            return;
        }

        bottomContentContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // pad the bottom of the webview, to make room for ourselves
        int totalHeight = bottomContentContainer.getMeasuredHeight();
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(totalHeight / DimenUtil.getDensityScalar()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);
        // ^ sending the padding event will guarantee a ContentHeightChanged event to be triggered,
        // which will update our margin based on the scroll offset, so we don't need to do it here.
    }

    private void setupAttribution() {
        Page page = parentFragment.getPage();
        pageLicenseText.setText(StringUtil.fromHtml(String
                .format(parentFragment.getContext().getString(R.string.content_license_html),
                        parentFragment.getContext().getString(R.string.cc_by_sa_3_url))));
        pageLicenseText.setMovementMethod(new LinkMovementMethod());

        // Don't display last updated message for main page or file pages, because it's always wrong
        if (page.isMainPage() || page.isFilePage()) {
            pageLastUpdatedText.setVisibility(View.GONE);
        } else {
            PageTitle title = page.getTitle();
            String lastUpdatedHtml = "<a href=\"" + title.getUriForAction("history")
                    + "\">" + parentFragment.getContext().getString(R.string.last_updated_text,
                    formatDateRelative(page.getPageProperties().getLastModified())
                            + "</a>");
            // TODO: Hide the Talk link if already on a talk page
            PageTitle talkPageTitle = new PageTitle("Talk", title.getPrefixedText(), title.getWikiSite());
            String discussionHtml = "<a href=\"" + talkPageTitle.getCanonicalUri() + "\">"
                    + parentFragment.getContext().getString(R.string.talk_page_link_text) + "</a>";
            pageLastUpdatedText.setText(StringUtil.fromHtml(lastUpdatedHtml + " &mdash; " + discussionHtml));
            pageLastUpdatedText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
            pageLastUpdatedText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public PageTitle getTitle() {
        return pageTitle;
    }

    @Override
    public void setTitle(PageTitle newTitle) {
        pageTitle = newTitle;
    }
}
