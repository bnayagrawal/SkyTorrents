package xyz.bnayagrawal.android.skytorrents;

import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;
import xyz.bnayagrawal.android.skytorrents.Data.Page;
import xyz.bnayagrawal.android.skytorrents.Data.Torrent;
import xyz.bnayagrawal.android.skytorrents.Utils.HtmlParser;
import xyz.bnayagrawal.android.skytorrents.Utils.UriBuilder;

public class MainActivity extends AppCompatActivity implements HtmlParser.Listener,PopupMenu.OnMenuItemClickListener {

    private final int REQUEST_TIME_OUT = 30000;
    private Page currentPage,currentPageTemp;
    private HashMap<Integer,Page> pages,pagesTemp;
    private HashMap<UriBuilder.SortOrder,Integer> sortOrderMenuIdMap;
    private ArrayList<Torrent> torrents;
    private HashMap<Integer,String> pageLinks,pageLinksTemp;
    private boolean searchPerformed = false;
    private String searchQuery = "";

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private CardView layoutProgress;
    private TextView tvLoadingProgress;

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

        layoutProgress = findViewById(R.id.layout_progress);
        tvLoadingProgress = findViewById(R.id.tv_loading_progress);

        layoutPagination = findViewById(R.id.layout_pagination);
        tvPagination = findViewById(R.id.tv_pagination);
        imgNextPage = findViewById(R.id.img_page_next);
        imgPreviousPage = findViewById(R.id.img_page_previous);

        //animation
        fadeInAnimation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.fade_in);
        fadeOutAnimation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.fade_out);

        initSortOrderMenuIdMap();
        initializeRecyclerView();

        //start fetching data
        showLoadingProgress("Fetching data...");
        new NetworkUtils().execute(UriBuilder.buildUrl(sortOrder));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_activity_main,menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        menu.getItem(0).setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                clearSearch();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort:
                showPopupMenu(findViewById(R.id.action_sort));
                break;
        }
        return true;
    }

    public void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.context_menu_activity_main, popup.getMenu());
        popup.getMenu().findItem(sortOrderMenuIdMap.get(sortOrder)).setChecked(true);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
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

    protected void initSortOrderMenuIdMap(){
        sortOrderMenuIdMap = new HashMap<>();
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_SEED_DESC, R.id.menu_sort_by_seeds_desc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_SEED_ASC, R.id.menu_sort_by_seeds_asc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_PEERS_DESC, R.id.menu_sort_by_peers_desc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_PEERS_ASC, R.id.menu_sort_by_peers_asc);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_BIG_TO_SMALL, R.id.menu_sort_by_big_to_small);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_SMALL_TO_BIG, R.id.menu_sort_by_small_to_big);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_LATEST, R.id.menu_sort_by_latest);
        sortOrderMenuIdMap.put(UriBuilder.SortOrder.SORT_OLDEST, R.id.menu_sort_by_oldest);
    }

    protected void initializeRecyclerView() {
        initPages(UriBuilder.buildUrl(sortOrder).toString(),true);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_torrents);
        layoutManager = new LinearLayoutManager(MainActivity.this,
                LinearLayoutManager.VERTICAL,
                false);
        adapter = new TRAdapter(MainActivity.this,torrents);

        //TODO: FIX APP CRASH DUE TO SOME ANIMATION
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

    protected void initPages(String url,boolean instantiateTorrents) {
        pages = new HashMap<>();

        /*
        * torrents must be instantiated only once.
        * Since recyclerAdapter holds the reference of this torrents reference.
        * if the reference is changed or re-instantiated, recycler wont be able
        * to update any data since the reference won't be updated there.
        * */

        if(instantiateTorrents)
            torrents = new ArrayList<>();

        Page page = new Page(url,1);
        currentPage = page;
        pages.put(1,page);
    }

    protected void reloadData() {
        if(searchPerformed) {
            performSearch(searchQuery);
        }
        else {
            initPages(UriBuilder.buildUrl(sortOrder).toString(),false);
            showLoadingProgress("Fetching sorted data...");
            new NetworkUtils().execute(UriBuilder.buildUrl(sortOrder));
        }
    }

    //called after document is retrieved from internet
    protected void onDocumentFetchComplete(Document document) {
        showLoadingProgress("Parsing data...");
        (new HtmlParser(this)).execute(document);
    }

    //document fetch error
    protected void onFetchError(IOException ioException) {
        Toast.makeText(MainActivity.this,ioException.getMessage(),Toast.LENGTH_LONG).show();
        hideLoadingProgress();
    }

    class NetworkUtils extends AsyncTask<URL,Void,Document> {
        private IOException ioException = null;
        @Override
        protected Document doInBackground(URL... urls) {
            Document document;
            URL url = urls[0];
            try {
                document = Jsoup.connect(url.toString()).timeout(REQUEST_TIME_OUT).get();
            } catch (IOException ioe) {
                document = null;
                this.ioException = ioe;
                ioe.printStackTrace();
            }
            return document;
        }

        @Override
        protected void onPostExecute(Document document) {
            if(null != document)
                onDocumentFetchComplete(document);
            else {
                //TODO: report error
                onFetchError(ioException);
            }
        }
    }

    @Override
    public void onParseComplete(ArrayList<Torrent> torrents, final HashMap<Integer,String> pageLinks) {
        hideLoadingProgress();
        if(null == torrents) {
            Toast.makeText(MainActivity.this,"Parse error",Toast.LENGTH_SHORT).show();
            return;
        }

        this.pageLinks = pageLinks;
        ArrayList<Torrent> currentPageTorrents = currentPage.getTorrents();

        //clear all items from recyclerView
        int size = torrents.size();
        if (size > 0) {
            this.torrents.clear();
            adapter.notifyItemRangeRemoved(0, size);
        }

        //add items to recyclerView
        for(Torrent torrent: torrents) {
            currentPageTorrents.add(torrent);
            this.torrents.add(torrent);
            adapter.notifyItemInserted(torrents.size());
        }

        Toast.makeText(MainActivity.this,"Page load completed!",Toast.LENGTH_SHORT).show();
        tvPagination.setText("Page ".concat(String.valueOf(currentPage.getPageNumber())).concat(" of ").concat(String.valueOf(pageLinks.size())));

        //pagination
        if(currentPage.getPageNumber() != pageLinks.size()) {
            //if page number is 1
            if(currentPage.getPageNumber() == 1) {
                imgPreviousPage.setColorFilter(
                        ContextCompat.getColor(MainActivity.this, R.color.buttonDisabled), android.graphics.PorterDuff.Mode.SRC_IN);
                imgPreviousPage.setOnClickListener(null);
            } else {
                imgPreviousPage.setColorFilter(null);
                imgPreviousPage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //load previous page
                        loadCachedPage(currentPage.getPageNumber() - 1);
                    }
                });
            }
            //Load next page
            imgNextPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pageNumber = currentPage.getPageNumber() + 1;
                    //if the page is already cached
                    if(pages.containsKey(pageNumber)) {
                        loadCachedPage(pageNumber);
                    } else {
                        Page page = new Page(pageLinks.get(pageNumber), pageNumber);
                        currentPage = page;
                        pages.put(pageNumber,page);
                        showLoadingProgress("Loading page ".concat(String.valueOf(pageNumber)));
                        new NetworkUtils().execute(UriBuilder.buildUrlFromString(pageLinks.get(pageNumber)));
                    }
                }
            });
        } else {
            //if last page
            imgNextPage.setColorFilter(
                    ContextCompat.getColor(MainActivity.this, R.color.buttonDisabled), android.graphics.PorterDuff.Mode.SRC_IN);
            imgNextPage.setOnClickListener(null);
        }
    }

    @Override
    public void onParseError(String message) {
        hideLoadingProgress();
        Toast.makeText(MainActivity.this,message,Toast.LENGTH_LONG).show();
    }

    protected void loadCachedPage(int pageNumber) {
        if(!pages.containsKey(pageNumber)) {
            Toast.makeText(MainActivity.this,"Error occurred!",Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Torrent> pageTorrents = pages.get(pageNumber).getTorrents();
        currentPage = pages.get(pageNumber);
        int size = torrents.size();
        if (size > 0) {
            this.torrents.clear();
            adapter.notifyItemRangeRemoved(0, size);
        }

        //add items to recyclerView
        for(Torrent torrent: pageTorrents) {
            this.torrents.add(torrent);
            adapter.notifyItemInserted(torrents.size());
        }

        if(pageNumber == 1) {
            imgPreviousPage.setColorFilter(
                    ContextCompat.getColor(MainActivity.this, R.color.buttonDisabled), android.graphics.PorterDuff.Mode.SRC_IN);
            imgPreviousPage.setOnClickListener(null);
        } else {
            imgPreviousPage.setColorFilter(null);
            imgPreviousPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //load previous page
                    loadCachedPage(currentPage.getPageNumber() - 1);
                }
            });
        }
        if(pageNumber != pageLinks.size()) {
            imgNextPage.setColorFilter(null);
            imgNextPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pageNumber = currentPage.getPageNumber() + 1;
                    //if the page is already cached
                    if(pages.containsKey(pageNumber)) {
                        loadCachedPage(pageNumber);
                    } else {
                        Page page = new Page(pageLinks.get(pageNumber), pageNumber);
                        currentPage = page;
                        pages.put(pageNumber,page);
                        showLoadingProgress("Loading page ".concat(String.valueOf(pageNumber)));
                        new NetworkUtils().execute(UriBuilder.buildUrlFromString(pageLinks.get(pageNumber)));
                    }
                }
            });
        } else {
            imgNextPage.setColorFilter(
                    ContextCompat.getColor(MainActivity.this, R.color.buttonDisabled), android.graphics.PorterDuff.Mode.SRC_IN);
            imgNextPage.setOnClickListener(null);
        }
        tvPagination.setText("Page ".concat(String.valueOf(currentPage.getPageNumber())).concat(" of ").concat(String.valueOf(pageLinks.size())));
    }

    protected void hideLoadingProgress() {
        layoutProgress.setVisibility(View.GONE);
        layoutProgress.startAnimation(fadeOutAnimation);
        layoutPagination.setVisibility(View.VISIBLE);
        layoutPagination.startAnimation(fadeInAnimation);
    }

    protected void showLoadingProgress(String message) {
        tvLoadingProgress.setText(message);
        layoutProgress.setVisibility(View.VISIBLE);
        layoutProgress.startAnimation(fadeInAnimation);
        layoutPagination.setVisibility(View.GONE);
        layoutPagination.startAnimation(fadeOutAnimation);
    }

    protected void performSearch(String query) {
        if(!searchPerformed) {
            //hold torrent data
            currentPageTemp = currentPage;
            pagesTemp = pages;
            pageLinksTemp = pageLinks;
        }
        //hold torrent search result
        searchPerformed = true;
        searchQuery = query;
        initPages(UriBuilder.buildQueryUrl(query, sortOrder).toString(),false);

        showLoadingProgress("Performing search...");
        new NetworkUtils().execute(UriBuilder.buildQueryUrl(query, sortOrder));
    }

    protected void clearSearch() {
        if(!searchPerformed)
            return;
        currentPage = currentPageTemp;
        pages = pagesTemp;
        pageLinks = pageLinksTemp;
        searchPerformed = false;
        loadCachedPage(currentPage.getPageNumber());
    }
}
