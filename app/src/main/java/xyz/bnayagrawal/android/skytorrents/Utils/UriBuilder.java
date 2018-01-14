package xyz.bnayagrawal.android.skytorrents.Utils;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by binay on 13/1/18.
 */

public final class UriBuilder {
    /*
    *  URLs of skytorrents.in. These values are subject to change if the owner
    *  of the service provider makes any changes.
    *  */
    private static final String BASE_WEBSITE_URL = "https://www.skytorrents.in";
    private static final String BASE_WEBSITE_URL_TOP1000 = "https://www.skytorrents.in/top1000/all/";
    private static final String BASE_WEBSITE_QUERY_URL = "https://www.skytorrents.in/search/all/";

    /*
    *  Sorting directories (to be appended with a preceding forward slash, not a form data or query parameter)
    *  ex: data sorted by seeds DESC, url will be https://www.skytorrents.in/top1000/all/ed
    *  ex: data sorted by seeds ASC, url will be https://www.skytorrents.in/top1000/all/ea
    */
    private static final String SORT_SEED_DESC = "ed/";
    private static final String SORT_SEED_ASC = "ea/";
    private static final String SORT_PEERS_DESC = "pd/";
    private static final String SORT_PEERS_ASC = "pa/";
    private static final String SORT_BIG_TO_SMALL = "sd/";
    private static final String SORT_SMALL_TO_BIG = "sa/";
    private static final String SORT_LATEST = "ad/";
    private static final String SORT_OLDEST = "aa/";

    /* Page number (1st page) */
    private static final String PAGE_NO = "1/";

    /*
    * Query parameter
    * */
    private static final String QUERY_PARAM = "q";

    public static URL buildUrl(SortOrder sortOrder) {
        String raw_url = BASE_WEBSITE_URL_TOP1000 + getSortOrder(sortOrder) + PAGE_NO;
        Uri builtUri = Uri.parse(raw_url).buildUpon().build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static URL buildQueryUrl(String query, SortOrder sortOrder) {
        String raw_url = BASE_WEBSITE_QUERY_URL + getSortOrder(sortOrder) + PAGE_NO;
        Uri builtUri = Uri.parse(raw_url).buildUpon()
                .appendQueryParameter(QUERY_PARAM,query)
                .build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static URL buildInfoUrl(String infoUrl) {
        String raw_url = BASE_WEBSITE_URL + infoUrl;
        Uri builtUri = Uri.parse(raw_url).buildUpon().build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static URL buildUrlFromString(String mUrl) {
        Uri builtUri = Uri.parse(mUrl).buildUpon().build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public enum SortOrder {
        SORT_SEED_DESC,
        SORT_SEED_ASC,
        SORT_PEERS_DESC,
        SORT_PEERS_ASC,
        SORT_BIG_TO_SMALL,
        SORT_SMALL_TO_BIG,
        SORT_LATEST,
        SORT_OLDEST
    }

    private static final String getSortOrder(SortOrder sortOrder) {
        String sort = "ed";
        switch (sortOrder) {
            case SORT_SEED_DESC:
                sort = SORT_SEED_DESC;
                break;
            case SORT_SEED_ASC:
                sort = SORT_SEED_ASC;
                break;
            case SORT_PEERS_DESC:
                sort = SORT_PEERS_DESC;
                break;
            case SORT_PEERS_ASC:
                sort = SORT_PEERS_ASC;
                break;
            case SORT_BIG_TO_SMALL:
                sort = SORT_BIG_TO_SMALL;
                break;
            case SORT_SMALL_TO_BIG:
                sort = SORT_SMALL_TO_BIG;
                break;
            case SORT_LATEST:
                sort = SORT_LATEST;
                break;
            case SORT_OLDEST:
                sort = SORT_OLDEST;
                break;
        }
        return sort;
    }
}
