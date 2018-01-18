package xyz.bnayagrawal.android.skytorrents;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;
import xyz.bnayagrawal.android.skytorrents.Data.Page;
import xyz.bnayagrawal.android.skytorrents.Data.Torrent;
import xyz.bnayagrawal.android.skytorrents.Utils.DocumentFetcher;
import xyz.bnayagrawal.android.skytorrents.Utils.TorrentListParser;
import xyz.bnayagrawal.android.skytorrents.Utils.UriBuilder;

public class MainActivity extends AppCompatActivity
        implements DocumentFetcher.Listener,
        TorrentListParser.TorrentListParserListener,
        PopupMenu.OnMenuItemClickListener {

    /*
     * This variable will be used by recyclerView
     * to display torrent list. Must be instantiated
     * only once.
     */
    private ArrayList<Torrent> torrents;

    /*
     * Variables which will hold page information,
     * next page links, torrent list associated with
     * page number, the currently displayed page,sort order list etc.
     */
    private Page currentPage,currentPageTemp;
    private HashMap<Integer,Page> pages,pagesTemp;
    private HashMap<Integer,String> pageLinks,pageLinksTemp;
    private HashMap<UriBuilder.SortOrder,Integer> sortOrderMenuIdMap;

    private boolean errorViewShown = false;
    private boolean searchViewExpanded = false;
    private boolean performingSearch = false;
    private String searchQuery = "";

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private CardView layoutProgress;
    private TextView tvLoadingProgress;

    private LinearLayout layoutError;
    private TextView tvNetworkError;
    private Button retryButton;

    private CardView layoutPagination;
    private TextView tvPagination;
    private ImageView imgNextPage;
    private ImageView imgPreviousPage;

    private UriBuilder.SortOrder sortOrder = UriBuilder.SortOrder.SORT_SEED_DESC;
    private Animation fadeInAnimation,fadeOutAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar_activity_main));
        ActionBar actionbar = getSupportActionBar();
        if(null != actionbar) {
            actionbar.setTitle(getResources().getString(R.string.title));
            actionbar.setSubtitle("Displaying Top 1000");
        }

        //Hold reference of views.
        layoutProgress = findViewById(R.id.layout_progress);
        tvLoadingProgress = findViewById(R.id.tv_loading_progress);

        layoutPagination = findViewById(R.id.layout_pagination);
        tvPagination = findViewById(R.id.tv_pagination);
        imgNextPage = findViewById(R.id.img_page_next);
        imgPreviousPage = findViewById(R.id.img_page_previous);

        layoutError = findViewById(R.id.layout_error);
        tvNetworkError = findViewById(R.id.tv_network_error);
        retryButton = findViewById(R.id.button_retry);

        //animation
        fadeInAnimation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.fade_in);
        fadeOutAnimation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.fade_out);

        initSortOrderMenuIdMap();
        initializeRecyclerView();

        //start fetching data from https://skytorrents.in/top1000
        showLoadingProgress("Fetching data...");
        new DocumentFetcher(MainActivity.this).execute(UriBuilder.buildUrl(sortOrder));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_activity_main,menu);

        //Gets and stores reference of searchView
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        //This listener is set to handle when user opens or closes the searchView
        menu.getItem(0).setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                /*
                 * If network error was displayed, hide it
                 * else it will crash the app.
                 */
                if(errorViewShown)
                    hideNetworkError();

                searchViewExpanded = true;
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                //The searchView is collapsed or closed
                searchViewExpanded = false;
                searchQuery = "";
                clearSearch();

                /*
                 * If no item is being displayed in recycler view
                 * (mostly due to network error) try to reload page.
                 */
                if(!errorViewShown && adapter.getItemCount() == 0) {
                    showLoadingProgress("Refreshing...");
                    new DocumentFetcher(MainActivity.this).execute(UriBuilder.buildUrlFromString(currentPage.getPageUrl()));
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                //When user clicks on the sort button, show popup menu
                showPopupMenu(findViewById(R.id.action_sort));
                break;
            case R.id.action_about:
                showAboutDialog();
                Toast.makeText(MainActivity.this,"Coming soon...",Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_visit_skytorrents: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://skytorrents.in"));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this,
                            "Please install a web browser app to open this link!",
                            Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
        }
        return true;
    }

    /**
     * Shows popup menu anchored to the given view
     * @param v View to anchor the popup menu
     */
    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.context_menu_activity_main, popup.getMenu());
        popup.getMenu().findItem(sortOrderMenuIdMap.get(sortOrder)).setChecked(true);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    /* when user chose a sort order from the popup menu */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_relevance: {
                //if searchView is not expanded don't allow this sort order
                if(!searchViewExpanded) {
                    item.setChecked(false);
                    sortOrder = UriBuilder.SortOrder.SORT_SEED_DESC;
                    Toast.makeText(MainActivity.this,
                            "Order by relevance is only for searching\nPlease choose a different sort order",
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    sortOrder = UriBuilder.SortOrder.SORT_RELEVANCE;
                    reloadData();
                }
            }
                break;
            case R.id.menu_sort_by_seeds_desc:
                sortOrder = UriBuilder.SortOrder.SORT_SEED_DESC;
                reloadData();
                break;
            case R.id.menu_sort_by_seeds_asc:
                sortOrder = UriBuilder.SortOrder.SORT_SEED_ASC;
                reloadData();
                break;
            case R.id.menu_sort_by_peers_desc:
                sortOrder = UriBuilder.SortOrder.SORT_PEERS_DESC;
                reloadData();
                break;
            case R.id.menu_sort_by_peers_asc:
                sortOrder = UriBuilder.SortOrder.SORT_PEERS_ASC;
                reloadData();
                break;
            case R.id.menu_sort_by_big_to_small:
                sortOrder = UriBuilder.SortOrder.SORT_BIG_TO_SMALL;
                reloadData();
                break;
            case R.id.menu_sort_by_small_to_big:
                sortOrder = UriBuilder.SortOrder.SORT_SMALL_TO_BIG;
                reloadData();
                break;
            case R.id.menu_sort_by_latest:
                sortOrder = UriBuilder.SortOrder.SORT_LATEST;
                reloadData();
                break;
            case R.id.menu_sort_by_oldest:
                sortOrder = UriBuilder.SortOrder.SORT_OLDEST;
                reloadData();
                break;
        }
        return true;
    }

    /**
     * Stores sortOrder and menu id of the sortOrder in a hashMap
     * so we can retrieve menu id for the given sort order using
     * get(sortOrder) method.
     */
    private void initSortOrderMenuIdMap(){
        sortOrderMenuIdMap = new HashMap<>();
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_RELEVANCE,R.id.menu_sort_by_relevance);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_SEED_DESC, R.id.menu_sort_by_seeds_desc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_SEED_ASC, R.id.menu_sort_by_seeds_asc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_PEERS_DESC, R.id.menu_sort_by_peers_desc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_PEERS_ASC, R.id.menu_sort_by_peers_asc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_BIG_TO_SMALL, R.id.menu_sort_by_big_to_small);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_SMALL_TO_BIG, R.id.menu_sort_by_small_to_big);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_LATEST, R.id.menu_sort_by_latest);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_OLDEST, R.id.menu_sort_by_oldest);
    }

    /**
     * Initializes recyclerView and pages
     */
    private void initializeRecyclerView() {
        initPages(UriBuilder.buildUrl(sortOrder).toString(),true);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_torrents);
        layoutManager = new LinearLayoutManager(MainActivity.this,
                LinearLayoutManager.VERTICAL,
                false);
        adapter = new TRAdapter(MainActivity.this,torrents);

        /* Set Animation (sometimes causing inconsistency errors)*/
        FadeInUpAnimator animator = new FadeInUpAnimator();
        animator.setAddDuration(150);
        animator.setChangeDuration(150);
        animator.setRemoveDuration(300);
        animator.setMoveDuration(300);
        recyclerView.setItemAnimator(animator);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    /**
     * For initializing pages which will hold torrent list
     * @param url the page url which will be used to retrieve torrent list
     * @param instantiateTorrents create a new instance of torrents
     */
    private void initPages(String url,boolean instantiateTorrents) {
        pages = new HashMap<>();

        /*
        * torrents variable must be instantiated only once.
        * Since recyclerAdapter holds the reference of this torrents variable.
        * if the reference is changed or re-instantiated, recyclerView wont be able
        * to update any data since the reference won't be updated there.
        * */

        if(instantiateTorrents)
            torrents = new ArrayList<>();

        Page page = new Page(url,1);
        currentPage = page;
        pages.put(1,page);
    }

    /**
     * If the user changes the sorting order
     * this method is invoked
     */
    private void reloadData() {
        /* if retry button is shown due to network error
         * or if no item is being displayed in recyclerView
         */
        if(errorViewShown || adapter.getItemCount() == 0)
            return;

        if(performingSearch) {
            /*
             * If user is performing search then
             * performSearch again with chosen
             * sorting order. searchQuery will be empty
             * if user has expanded searchView.
             */
            if(searchQuery.length() != 0)
                performSearch(searchQuery);
        }
        else {
            //Reload pages with user chosen sort order.
            initPages(UriBuilder.buildUrl(sortOrder).toString(),false);
            showLoadingProgress("Fetching data...");
            new DocumentFetcher(MainActivity.this).execute(UriBuilder.buildUrl(sortOrder));
        }
    }

    /**
     * Thanks StackOverflow :)
     * Checks whether internet connection is available on the device.
     * @return Returns true if internet connection is available.
     */
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Invoked by DocumentFetcher after a given html page
     * is fetched from given url (of course from skyTorrents server).
     * @param document document object parsed from html
     */
    @Override
    public void onDocumentFetchComplete(Document document) {
        showLoadingProgress("Parsing data...");
        (new TorrentListParser(MainActivity.this)).execute(document);
    }

    /**
     * Invoked by DocumentFetcher if an exception occurs
     * while fetching document from given url.
     * @param ioException IOException object
     */
    @Override
    public void onDocumentFetchError(IOException ioException) {
        String message = ioException.getMessage();

        //Hide the loading progress.
        hideLoadingProgress();

        //If internet connection is not there and timed out error has not occurred
        if(!isOnline() && !message.contains("timed out")) {
            Toast.makeText(MainActivity.this, "Please check your internet connection!", Toast.LENGTH_LONG).show();
        }
        else if(message.contains("Handshake") || message.contains("ssl") || message.contains("timed out")) {
            /*
             * If handshake error or SSL error occurs
             * (may occur if website is blocked by the ISP
             */
            if(adapter.getItemCount() == 0) {
                /*
                 * If there's no item displayed in the recyclerView.
                 */
                showNetworkError(getResources().getString(R.string.network_error));
                setRetryButtonClickListener(currentPage.getPageUrl());
                return;
            }
            /*
             else the user can press the next page button to retry
             */
        } else {
            //If the error has occurred due to some other reason
            Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();

            //If recyclerView is not showing any item, show error with retry button
            if(adapter.getItemCount() == 0) {
                showNetworkError("Network error!\n" + message);
                setRetryButtonClickListener(currentPage.getPageUrl());
                return;
            }
        }

        /*
         * If the user has pressed the next page button, then a new page
         * was added and the currentPage was changed to the new page
         * but torrent data wasn't fetched due to network error.
         */
        if(currentPage.getPageNumber() > 1) {
            Toast.makeText(MainActivity.this,"Please try again!",Toast.LENGTH_SHORT).show();
            //change current page to previous page
            currentPage = pages.get(currentPage.getPageNumber() - 1);
            //remove the newly added empty page
            pages.remove(pages.size());
            //load the previous cached page
            loadCachedPage(currentPage.getPageNumber());
        }
    }

    /**
     * Sets onClick listener for retry button
     * @param url The document to fetch when this button is clicked.
     */
    private void setRetryButtonClickListener(final String url) {
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Hide this layout which displays the error
                hideNetworkError();
                showLoadingProgress("Retrying...");
                new DocumentFetcher(MainActivity.this).execute(
                        UriBuilder.buildUrlFromString(url));
            }
        });
    }

    /**
     * Invoked after TorrentListParser class has completed
     * parsing torrent list and page links from given html document.
     *
     * @param torrents contains list torrents parsed from the given html page
     * @param pageLinks contains page links (1,2,3...n)  parsed from given html page
     */
    @Override
    public void onTorrentListParseComplete(ArrayList<Torrent> torrents, final HashMap<Integer,String> pageLinks) {
        hideLoadingProgress();
        if(null == torrents) {
            //If for some reason
            Toast.makeText(MainActivity.this,"Parse error",Toast.LENGTH_SHORT).show();
            return;
        }

        //holds url of all pages (page 1,2,...n)
        this.pageLinks = pageLinks;

        //clear all items from recyclerView
        int size = torrents.size();
        if (size > 0) {
            this.torrents.clear();
            adapter.notifyItemRangeRemoved(0, size);
        }

        //add torrents to recyclerView and current page.
        ArrayList<Torrent> currentPageTorrents = currentPage.getTorrents();
        for(Torrent torrent: torrents) {
            currentPageTorrents.add(torrent);
            this.torrents.add(torrent);
            adapter.notifyItemInserted(torrents.size());
        }

        Toast.makeText(MainActivity.this,"Page load completed!",Toast.LENGTH_SHORT).show();

        //update pagination button click listeners
        setPaginationButtonClickListeners();
        tvPagination.setText("Page ".concat(String.valueOf(currentPage.getPageNumber())).concat(" of ").concat(String.valueOf(pageLinks.size())));
    }

    /**
     * Invoked by TorrentListParser class after an
     * error occurs during parsing torrent list from html
     * @param message error message
     */
    @Override
    public void onTorrentListParseError(String message) {
        hideLoadingProgress();
        Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();
    }

    /**
     * Loads cached page for the given page number
     * @param pageNumber page number to load data from.
     */
    private void loadCachedPage(int pageNumber) {
        //If for some reason the given page number is not cached
        if(!pages.containsKey(pageNumber)) {
            Toast.makeText(MainActivity.this,"Error occurred!",Toast.LENGTH_SHORT).show();
            return;
        }

        //remove current items from recycler view
        currentPage = pages.get(pageNumber);
        int size = torrents.size();
        if (size > 0) {
            this.torrents.clear();
            adapter.notifyItemRangeRemoved(0, size);
        }

        //loads torrent list of given page number to recyclerView
        ArrayList<Torrent> pageTorrents = pages.get(pageNumber).getTorrents();
        for(Torrent torrent: pageTorrents) {
            this.torrents.add(torrent);
            adapter.notifyItemInserted(torrents.size());
        }

        //update pagination button click listeners
        setPaginationButtonClickListeners();
        tvPagination.setText("Page ".concat(String.valueOf(currentPage.getPageNumber())).concat(" of ").concat(String.valueOf(pageLinks.size())));
    }

    /**
     * Sets onClick listeners for left and right button of pagination.
     */
    private void setPaginationButtonClickListeners() {

        int totalPages = pageLinks.size();
        final int currentPageNumber = currentPage.getPageNumber();

        //for left or previous page button
        if(1 == currentPageNumber) {
            //Since we are on the first page, we have to disable this button.
            imgPreviousPage.setColorFilter(
                    ContextCompat.getColor(MainActivity.this, R.color.buttonDisabled),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            imgPreviousPage.setOnClickListener(null);
        } else {
            //There are more than one page, so have to enable this button
            imgPreviousPage.setColorFilter(null);
            imgPreviousPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //load previous cached page as the page will be cached for sure.
                    loadCachedPage(currentPageNumber - 1);
                }
            });
        }

        //for right or next page button
        if(currentPageNumber != totalPages) {
            //if current page is not the last page.
            imgNextPage.setColorFilter(null);
            imgNextPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int nextPageNumber = currentPage.getPageNumber() + 1;
                    //if the page is already cached
                    if(pages.containsKey(nextPageNumber)) {
                        loadCachedPage(nextPageNumber);
                    } else {
                        //load next page from server.
                        Page page = new Page(pageLinks.get(nextPageNumber), nextPageNumber);
                        currentPage = page;
                        pages.put(nextPageNumber,page);
                        showLoadingProgress("Loading page ".concat(String.valueOf(nextPageNumber)));
                        new DocumentFetcher(MainActivity.this).execute(UriBuilder.buildUrlFromString(pageLinks.get(nextPageNumber)));
                    }
                }
            });
        } else {
            //if current page is the last page, we have to disable this button
            imgNextPage.setColorFilter(
                    ContextCompat.getColor(MainActivity.this, R.color.buttonDisabled),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            imgNextPage.setOnClickListener(null);
        }
    }

    /**
     * Hides the loading indicator and shows the pagination
     */
    private void hideLoadingProgress() {
        layoutProgress.setVisibility(View.GONE);
        layoutProgress.startAnimation(fadeOutAnimation);
        layoutPagination.setVisibility(View.VISIBLE);
        layoutPagination.startAnimation(fadeInAnimation);
    }

    /**
     * Hides pagination and shows loading indicator with a message
     */
    private void showLoadingProgress(String message) {
        tvLoadingProgress.setText(message);
        layoutProgress.setVisibility(View.VISIBLE);
        layoutProgress.startAnimation(fadeInAnimation);
        layoutPagination.setVisibility(View.GONE);
        layoutPagination.startAnimation(fadeOutAnimation);
    }

    /**
     * Hides both pagination and loading indicator.
     * Shows if a network error occurs with a message.
     */
    private void showNetworkError(String errorMessage) {
        errorViewShown = true;
        tvNetworkError.setText(errorMessage);
        layoutProgress.setVisibility(View.GONE);
        layoutProgress.startAnimation(fadeOutAnimation);
        layoutPagination.setVisibility(View.GONE);
        layoutPagination.startAnimation(fadeOutAnimation);
        layoutError.setVisibility(View.VISIBLE);
        layoutError.startAnimation(fadeInAnimation);
    }

    /**
     * Only hides network error view.
     */
    private void hideNetworkError() {
        layoutError.setVisibility(View.GONE);
        layoutError.startAnimation(fadeOutAnimation);
        errorViewShown = false;
    }

    /**
     * This method is called when user submits search query
     * @param query search query
     */
    private void performSearch(String query) {
        /*
         * When user opens searchView and submits the search query
         * the performingSearch variable contains "false" value,
         * so we have to store the non-search torrent list in temp
         * variables. If user queries again when searchView is open
         * the performingSearch variable will contain "true" and we
         * don't have to store reference of non-search torrent list
         * as we have already stored the reference.
         */
        if(!performingSearch) {
            /*
             * Hold reference of these variables in temp variables
             * so after user closes the searchView we can load
             * back the cached pages (non-search torrent list)
             * from these temp variables
             */
            currentPageTemp = currentPage;
            pagesTemp = pages;
            pageLinksTemp = pageLinks;
        }

        //Since we are performing search
        performingSearch = true;
        searchQuery = query;

        //re-instantiate variables to hold search data
        initPages(UriBuilder.buildQueryUrl(query, sortOrder).toString(),false);

        //perform the search
        showLoadingProgress("Performing search...");
        new DocumentFetcher(MainActivity.this).execute(UriBuilder.buildQueryUrl(query, sortOrder));
    }

    /**
     * Reload previous torrent list after user closes the searchView
     */
    private void clearSearch() {
        //if we are not performing search
        if(!performingSearch)
            return;

        /*
         * Restore reference from the temp variables
         * which holds non-search torrent list.
         */
        currentPage = currentPageTemp;
        pages = pagesTemp;
        pageLinks = pageLinksTemp;
        performingSearch = false;

        //If torrent list empty for the current page (May be due to network error)
        if(currentPage.getTorrents().size() == 0)
            new DocumentFetcher(MainActivity.this).execute(UriBuilder.buildUrlFromString(currentPage.getPageUrl()));
        else
            loadCachedPage(currentPage.getPageNumber());
    }

    /**
     * Shows about dialog.
     */
    protected void showAboutDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        View view = getLayoutInflater().inflate(R.layout.dialog_about,null,false);
        //set actions
        (view.findViewById(R.id.tv_dialog_view_source)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchUriInent("https://github.com/bnayagrawal/skytorrents");
            }
        });

        //set actions
        (view.findViewById(R.id.tv_dialog_view_website)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchUriInent("https://bnayagrawal.xyz");
            }
        });

        String appVersion = "Version ";
        //fetch app version
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersion = appVersion.concat(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            appVersion = "Version: 1.0";
            e.printStackTrace();
        }

        ((TextView)view.findViewById(R.id.tv_dialog_app_version)).setText(appVersion);
        builder.setView(view);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog aboutDialog = builder.create();
        aboutDialog.show();
    }

    /**
     * initiates action_view intent
     */
    private void launchUriInent(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "You may not app which handles magnet url!",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }
}
