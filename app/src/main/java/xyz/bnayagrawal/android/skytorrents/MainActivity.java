package xyz.bnayagrawal.android.skytorrents;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import xyz.bnayagrawal.android.skytorrents.Data.Page;
import xyz.bnayagrawal.android.skytorrents.Utils.UriBuilder;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_TIME_OUT = 3000;
    private Document document;
    private ArrayList<Page> pages;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.pb_loading_indicator);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeRecyclerView();
        new NetworkUtils().execute(UriBuilder.buildUrl(UriBuilder.SortOrder.SORT_SEED_DESC));
    }

    protected void initializeRecyclerView() {
        pages = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_torrents);
        layoutManager = new LinearLayoutManager(MainActivity.this,
                LinearLayoutManager.VERTICAL,
                false);
        adapter = new TRAdapter(MainActivity.this,pages);
        recyclerView.setAdapter(adapter);
    }

    //called after document is retrieved from internet
    protected void onDocumentFetchComplete(Document document) {
        Toast.makeText(MainActivity.this,"Fetch complete",Toast.LENGTH_SHORT).show();
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
}
