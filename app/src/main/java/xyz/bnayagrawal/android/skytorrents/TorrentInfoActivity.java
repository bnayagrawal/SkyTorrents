package xyz.bnayagrawal.android.skytorrents;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.jsoup.nodes.Document;

import java.io.IOException;

import xyz.bnayagrawal.android.skytorrents.Data.Torrent;
import xyz.bnayagrawal.android.skytorrents.Data.TorrentInfo;
import xyz.bnayagrawal.android.skytorrents.Utils.DocumentFetcher;
import xyz.bnayagrawal.android.skytorrents.Utils.TorrentInfoParser;
import xyz.bnayagrawal.android.skytorrents.Utils.UriBuilder;

public class TorrentInfoActivity extends AppCompatActivity
        implements DocumentFetcher.Listener, TorrentInfoParser.TorrentInfoParserListener {

    private Torrent torrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_activity_torrent_info);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        fetchTorrentInfo(getIntent().getExtras());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    public void onTorrentInfoParseComplete(TorrentInfo torrentInfo) {

    }

    @Override
    public void onTorrentInfoParseError(String message) {
        Toast.makeText(TorrentInfoActivity.this,message,Toast.LENGTH_LONG).show();
    }

    protected void fetchTorrentInfo(Bundle bundle) {
        torrent = new Torrent(
                bundle.getString("NAME"),
                bundle.getString("MAGNET_URL"),
                bundle.getString("DETAILS_URL"),
                bundle.getString("FILE_SIZE"),
                bundle.getInt("FiLE_COUNT"),
                bundle.getString("DATE_ADDED"),
                bundle.getInt("SEEDS"),
                bundle.getInt("PEERS")
        );
        new DocumentFetcher(this).execute(UriBuilder.buildInfoUrl(torrent.getDetailsUrl()));
    }

    @Override
    public void onDocumentFetchComplete(Document document) {
        new TorrentInfoParser(torrent,this).execute(document);
    }

    @Override
    public void onDocumentFetchError(IOException exception) {
        Toast.makeText(TorrentInfoActivity.this,exception.getMessage(),Toast.LENGTH_LONG).show();
    }
}
