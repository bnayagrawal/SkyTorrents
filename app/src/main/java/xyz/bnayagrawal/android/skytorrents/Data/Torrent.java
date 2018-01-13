package xyz.bnayagrawal.android.skytorrents.Data;

import java.util.Date;

/**
 * Created by binay on 13/1/18.
 */

public class Torrent {
    private String name;
    private String magnetUrl;
    private String detailsUrl;
    private String fileSize; //We are not parsing.
    private String dateAdded; //we are not parsing.
    private int fileCount;
    private int seeds;
    private int peers;

    public Torrent(
            String name,
            String magnetUrl,
            String detailsUrl,
            String fileSize,
            int fileCount,
            String dateAdded,
            int seeds,
            int peers
    ) {
        this.name = name;
        this.magnetUrl = magnetUrl;
        this.detailsUrl = detailsUrl;
        this.fileSize = fileSize;
        this.fileCount = fileCount;
        this.dateAdded = dateAdded;
        this.seeds = seeds;
        this.peers = peers;
    }

    public String getName(){return this.name;}
    public String getMagnetUrl() {return this.magnetUrl;}
    public String getDetailsUrl() {return this.detailsUrl;}
    public String getFileSize() {return this.fileSize;}
    public String getDateAdded() {return this.dateAdded;}
    public int getFileCount() {return this.fileCount;}
    public int getSeeds() {return this.seeds;}
    public int getPeers() {return this.peers;}
}
