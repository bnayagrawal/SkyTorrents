package xyz.bnayagrawal.android.skytorrents.Utils;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.nodes.Document;

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
        Document document = documents[0];
        HashMap<String,String> fileNameSizeMap = new HashMap<>();
        try {
            String hash = "NA";
            int positiveVotes = 0,negativeVotes = 0;
            String html = document.html();
            int indexOfHash = html.indexOf("Hash");
            if(html.contains("Hash"))
                Log.d("Torrent Hash",html.substring(indexOfHash,indexOfHash + 45));
        } catch (Exception ex) {
            errorOccurred = true;
            errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(TorrentInfo torrentInfo) {
        if(errorOccurred)
            mTorrentInfoParserListener.onTorrentInfoParseError(errorMessage);
        else
            mTorrentInfoParserListener.onTorrentInfoParseComplete(torrentInfo);
    }
}
