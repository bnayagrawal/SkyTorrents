package xyz.bnayagrawal.android.skytorrents.Utils;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.zip.Inflater;

import xyz.bnayagrawal.android.skytorrents.Data.Torrent;

/**
 * Created by binay on 13/1/18.
 */

public class HtmlParser extends AsyncTask<Document,Torrent,ArrayList<Torrent>> {
    private Listener mListener;

    public HtmlParser(HtmlParser.Listener mListener) {
        this.mListener = mListener;
    }

    @Override
    protected ArrayList<Torrent> doInBackground(Document... documents) {
        Document document = documents[0];
        ArrayList<Torrent> torrents = new ArrayList<>();
        try {
            String name,fileSize,detailsUrl,magnetUrl,dateAdded;
            int fileCount,seeds,peers;
            Elements tdata,anchors;
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

            Elements tables = document.getElementsByTag("table");
            if(0 == tables.size()) {
                torrents = null;
                return torrents;
            }
            Element table = tables.get(0);
            Elements tbody = table.getElementsByTag("tbody");
            if(0 == tbody.size()) {
                torrents = null;
                return torrents;
            }
            Elements trow = (tbody.get(0)).getElementsByTag("tr");
            //for each row in table
            for(Element row: trow) {
                tdata = row.getElementsByTag("td");
                //if for some reason
                if(tdata.size() >= 6) {
                    anchors = tdata.get(0).getElementsByTag("a");
                    if(anchors.size() >= 2) {
                        name = anchors.get(0).attr("title");
                        detailsUrl = anchors.get(0).attr("href");
                        magnetUrl = anchors.get(anchors.size() - 1).attr("href");
                    } else {
                        continue;
                    }
                    fileSize = tdata.get(1).text();
                    fileCount = Integer.parseInt(tdata.get(2).text());
                    dateAdded = tdata.get(3).text();
                    seeds = Integer.parseInt(tdata.get(4).text());
                    peers = Integer.parseInt(tdata.get(5).text());
                    torrents.add(new Torrent(name,magnetUrl,detailsUrl,fileSize,fileCount,dateAdded,seeds,peers));
                } else {
                    continue;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return torrents;
    }

    @Override
    protected void onPostExecute(ArrayList<Torrent> torrents) {
        mListener.onParseComplete(torrents);
    }

    public interface Listener {
        void onParseComplete(ArrayList<Torrent> torrents);
    }
}
