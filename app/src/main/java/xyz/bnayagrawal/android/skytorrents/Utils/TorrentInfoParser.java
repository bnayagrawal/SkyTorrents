package xyz.bnayagrawal.android.skytorrents.Utils;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

import xyz.bnayagrawal.android.skytorrents.Data.Torrent;
import xyz.bnayagrawal.android.skytorrents.Data.TorrentInfo;

/**
 * Created by binay on 15/1/18.
 */

public class TorrentInfoParser extends AsyncTask<Document,Void,TorrentInfo> {
    private TorrentInfoParser.TorrentInfoParserListener mTorrentInfoParserListener;
    private boolean errorOccurred = false;
    private String errorMessage = "";
    private Torrent torrent;

    public interface TorrentInfoParserListener {
        void onTorrentInfoParseComplete(TorrentInfo torrentInfo);
        void onTorrentInfoParseError(String message);
    }

    public TorrentInfoParser(Torrent torrent, TorrentInfoParser.TorrentInfoParserListener mListener) {
        this.torrent = torrent;
        this.mTorrentInfoParserListener = mListener;
    }

    @Override
    protected TorrentInfo doInBackground(Document... documents) {
        TorrentInfo torrentInfo = null;
        Document document = documents[0];
        HashMap<String,String> fileNameSizeMap = new HashMap<>();
        try {
            String hash = "-NA-";
            int positiveVotes = 0,negativeVotes = 0;
            String html = document.html();
            int indexOfHash = html.indexOf("Hash");
            if(html.contains("Hash"))
                hash = html.substring(indexOfHash,indexOfHash + 45);
            Elements table = document.select("table.is-bordered");
            Elements tbody = table.get(0).getElementsByTag("tbody");
            Elements tr = tbody.get(0).getElementsByTag("tr");
            Elements td;
            for(int i = 0; i < tr.size(); i++) {
                td = tr.get(i).getElementsByTag("td");
                if(td.size() != 0 && !td.html().contains("<b>File Name</b>"))
                    fileNameSizeMap.put(td.get(0).text(),td.get(1).text());
            }
            torrentInfo = new TorrentInfo(
                    torrent.getName(),
                    torrent.getMagnetUrl(),
                    torrent.getFileSize(),
                    torrent.getDateAdded(),
                    hash,
                    torrent.getSeeds(),
                    torrent.getPeers(),
                    0,0,
                    fileNameSizeMap
            );
        } catch (Exception ex) {
            errorOccurred = true;
            errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
        return torrentInfo;
    }

    @Override
    protected void onPostExecute(TorrentInfo torrentInfo) {
        if(errorOccurred)
            mTorrentInfoParserListener.onTorrentInfoParseError(errorMessage);
        else
            mTorrentInfoParserListener.onTorrentInfoParseComplete(torrentInfo);
    }
}
