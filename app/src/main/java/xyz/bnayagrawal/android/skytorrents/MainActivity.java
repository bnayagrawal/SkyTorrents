package xyz.bnayagrawal.android.skytorrents;

import android.app.ActionBar;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Timer;

import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;
import xyz.bnayagrawal.android.skytorrents.Data.Page;
import xyz.bnayagrawal.android.skytorrents.Data.Torrent;
import xyz.bnayagrawal.android.skytorrents.Utils.HtmlParser;
import xyz.bnayagrawal.android.skytorrents.Utils.UriBuilder;

public class MainActivity extends AppCompatActivity implements HtmlParser.Listener{

    private final int REQUEST_TIME_OUT = 30000;
    private Page currentPage;
    private ArrayList<Page> pages;
    private ArrayList<Torrent> allTorrents;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionbar = getActionBar();
        if(null != actionbar) {
            actionbar.setSubtitle("Top 1000");
        }

        progressBar = findViewById(R.id.pb_loading_indicator);
        initializeRecyclerView();
        new NetworkUtils().execute(UriBuilder.buildUrl(UriBuilder.SortOrder.SORT_SEED_DESC));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_activity_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    protected void initializeRecyclerView() {
        pages = new ArrayList<>();
        allTorrents = new ArrayList<>();
        Page page = new Page(UriBuilder.buildUrl(UriBuilder.SortOrder.SORT_SEED_DESC).toString(),1);
        currentPage = page;
        pages.add(page);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_torrents);
        layoutManager = new LinearLayoutManager(MainActivity.this,
                LinearLayoutManager.VERTICAL,
                false);
        adapter = new TRAdapter(MainActivity.this,allTorrents);

        /* Set Animation*/
        FadeInUpAnimator animator = new FadeInUpAnimator();
        animator.setAddDuration(150);
        animator.setChangeDuration(150);
        animator.setRemoveDuration(150);
        animator.setMoveDuration(150);
        recyclerView.setItemAnimator(animator);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    //called after document is retrieved from internet
    protected void onDocumentFetchComplete(Document document) {
        progressBar.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        (new HtmlParser(this)).execute(document);
    }

    //document fetch error
    protected void onFetchError(IOException ioException) {
        Toast.makeText(MainActivity.this,ioException.getMessage(),Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.INVISIBLE);
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
    public void onParseComplete(ArrayList<Torrent> torrents) {
        if(null == torrents) {
            Toast.makeText(MainActivity.this,"Parse error",Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Torrent> torrentList = currentPage.getTorrents();
        for(Torrent torrent: torrents) {
            torrentList.add(torrent);
            allTorrents.add(torrent);
            adapter.notifyItemInserted(allTorrents.size());
        }
    }

}
