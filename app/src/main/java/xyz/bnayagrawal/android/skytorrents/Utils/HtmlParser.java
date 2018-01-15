package xyz.bnayagrawal.android.skytorrents.Utils;

import android.os.AsyncTask;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.bnayagrawal.android.skytorrents.Data.Torrent;

/**
 * Created by binay on 13/1/18.
 */

public class HtmlParser extends AsyncTask<Document, Torrent, ArrayList<Torrent>> {
    private Listener mListener;
    private boolean errorOccurred = false;
    private String errorMessage = "";
    private HashMap<Integer, String> pageLinks;

    public HtmlParser(HtmlParser.Listener mListener) {
        this.mListener = mListener;
    }

    @Override
    protected ArrayList<Torrent> doInBackground(Document... documents) {
        Document document = documents[0];
        ArrayList<Torrent> torrents = new ArrayList<>();
        try {
            String name, fileSize, detailsUrl, magnetUrl, dateAdded;
            int fileCount, seeds, peers;
            Elements tdata, anchors;

            Elements tables = document.getElementsByTag("table");
            if (0 == tables.size()) {
                errorOccurred = true;
                errorMessage = "No torrent data found!";
                return null;
            }
            Element table = tables.get(0);
            Elements tbody = table.getElementsByTag("tbody");
            if (0 == tbody.size()) {
                return null;
            }
            Elements trow = (tbody.get(0)).getElementsByTag("tr");

            //for each row in table, parse torrent list
            for (Element row : trow) {
                tdata = row.getElementsByTag("td");
                //if for some reason
                if (tdata.size() >= 6) {
                    anchors = tdata.get(0).getElementsByTag("a");
                    if (anchors.size() >= 2) {
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
                    torrents.add(new Torrent(name, magnetUrl, detailsUrl, fileSize, fileCount, dateAdded, seeds, peers));
                }
            }

            //parse page urls
            pageLinks = new HashMap<>();
            int pageNumber;
            String title, text, pageUrl;
            Elements links = document.select(".content.has-text-centered > a");
            for (Element link : links) {
                text = link.text();
                title = link.attr("title");
                if ((null != text && StringUtil.isNumeric(text)) || (null != title && StringUtil.isNumeric(title))) {
                    pageNumber = Integer.parseInt(text);
                    pageUrl = link.attr("abs:href");
                    if (!pageLinks.containsKey(pageNumber))
                        pageLinks.put(pageNumber, pageUrl);
                }
            }

        } catch (Exception ex) {
            errorOccurred = true;
            errorMessage = ex.getMessage();
            ex.printStackTrace();
        }
        return torrents;
    }

    @Override
    protected void onPostExecute(ArrayList<Torrent> torrents) {
        if(errorOccurred) {
            mListener.onParseError(errorMessage);
            return;
        }
        mListener.onParseComplete(torrents, pageLinks);
    }

    public interface Listener {
        void onParseComplete(ArrayList<Torrent> torrents, HashMap<Integer, String> pageLinks);
        void onParseError(String message);
    }
}
