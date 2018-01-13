package xyz.bnayagrawal.android.skytorrents.Data;

import java.util.ArrayList;

/**
 * Created by binay on 13/1/18.
 */

public class Page {
    private int pageNumber;
    private String pageUrl;
    private ArrayList<Torrent> torrents;

    public Page(String pageUrl, int pageNumber) {
        this.pageUrl = pageUrl;
        this.pageNumber = pageNumber;
        torrents = new ArrayList<>();
    }

    public String getPageUrl() {return this.pageUrl;}
    public int getPageNumber() {return this.pageNumber;}
    public ArrayList<Torrent> getTorrents() {return this.torrents;}
}
