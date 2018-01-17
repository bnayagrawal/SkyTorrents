package xyz.bnayagrawal.android.skytorrents;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

import xyz.bnayagrawal.android.skytorrents.Data.Torrent;
import xyz.bnayagrawal.android.skytorrents.Data.TorrentInfo;
import xyz.bnayagrawal.android.skytorrents.Utils.DocumentFetcher;
import xyz.bnayagrawal.android.skytorrents.Utils.TorrentInfoParser;
import xyz.bnayagrawal.android.skytorrents.Utils.UriBuilder;

public class TorrentInfoActivity extends AppCompatActivity
        implements DocumentFetcher.Listener, TorrentInfoParser.TorrentInfoParserListener {

    private Torrent torrent;
    private LinearLayout layoutTorrentFiles;
    private Toast toast;

    private TextView tvTorrentName;
    private TextView tvTorrentHash;
    private TextView tvTorrentAddedDate;
    private TextView tvTorrentSeeds;
    private TextView tvTorrentPeers;
    private TextView tvTorrentTotalFileSize;
    private TextView tvTorrentInfoFilesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_activity_torrent_info);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        layoutTorrentFiles = findViewById(R.id.layout_torrent_files);
        tvTorrentName = findViewById(R.id.tv_torrent_info_name);
        tvTorrentHash = findViewById(R.id.tv_torrent_info_hash);
        tvTorrentAddedDate = findViewById(R.id.tv_torrent_info_added_date);
        tvTorrentSeeds = findViewById(R.id.tv_torrent_info_seeds);
        tvTorrentPeers = findViewById(R.id.tv_torrent_info_peers);
        tvTorrentTotalFileSize = findViewById(R.id.tv_torrent_info_total_files_size);
        tvTorrentInfoFilesCount = findViewById(R.id.tv_torrent_info_files_count);

        (findViewById(R.id.btn_open_magnet_link)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(torrent.getMagnetUrl()));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(TorrentInfoActivity.this,
                            "You may not app which handles magnet url!",
                            Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        (findViewById(R.id.btn_copy_magnet_link)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(TorrentInfoActivity.this,
                        "Magnet link copied to clipboard!",
                        Toast.LENGTH_LONG).show();
            }
        });

        toast = Toast.makeText(TorrentInfoActivity.this,"Loading...",Toast.LENGTH_SHORT);
        toast.show();
        fetchTorrentInfo(getIntent().getExtras());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_share:
                shareMagnetLink(torrent.getMagnetUrl());
                break;
            case R.id.action_open_in_browser:
                openLinkInBrowser(torrent.getDetailsUrl());
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_activity_torrrent_info,menu);
        return true;
    }

    @Override
    public void onTorrentInfoParseComplete(TorrentInfo torrentInfo) {
        tvTorrentName.setText(torrentInfo.getName());
        tvTorrentAddedDate.setText("Added ".concat(torrentInfo.getDateAdded()));
        tvTorrentSeeds.setText("Seeds ".concat(String.valueOf(torrentInfo.getSeeds())));
        tvTorrentPeers.setText("Peers ".concat(String.valueOf(torrentInfo.getPeers())));
        tvTorrentHash.setText(torrentInfo.getHash());
        tvTorrentTotalFileSize.setText("Total size: ".concat(torrentInfo.getFileSize()));
        tvTorrentInfoFilesCount.setText("Torrent Files (" + String.valueOf(torrentInfo.getFileNameSizeMap().size()) + ")");

        View view;
        TextView textView;
        LayoutInflater layoutInflater = getLayoutInflater();
        for(Map.Entry<String,String> entry: torrentInfo.getFileNameSizeMap().entrySet()) {
            view = layoutInflater.inflate(R.layout.template_file_item,null,false);
            textView = (TextView) view.findViewById(R.id.tv_torrent_file_item_name);
            textView.setText(entry.getKey());
            textView = (TextView) view.findViewById(R.id.tv_torrent_file_item_size);
            textView.setText(entry.getValue());
            layoutTorrentFiles.addView(view);
        }

        if(null != toast)
            toast.cancel();
        toast = Toast.makeText(TorrentInfoActivity.this,"Loading complete!",Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onTorrentInfoParseError(String message) {
        if(null != toast)
            toast.cancel();
        toast = Toast.makeText(TorrentInfoActivity.this,"Parsing error\n" + message,Toast.LENGTH_LONG);
        toast.show();
        onBackPressed();
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
        if(null != toast)
            toast.cancel();
        toast = Toast.makeText(TorrentInfoActivity.this,"Document fetch error!",Toast.LENGTH_LONG);
        toast.show();
        onBackPressed();
    }

    private void shareMagnetLink(String magnetUrl) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,magnetUrl);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(TorrentInfoActivity.this,
                    "You may not have app installed to perform this action.",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void openLinkInBrowser(String torrentInfoUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(UriBuilder.buildInfoUrl(torrentInfoUrl).toString()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(TorrentInfoActivity.this,
                    "Please install a web browser app to open this link!",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }
}
