package xyz.bnayagrawal.android.skytorrents.Data;

import java.util.HashMap;
import java.util.SimpleTimeZone;

/**
 * Created by binay on 15/1/18.
 */

public class TorrentInfo {
    private String name;
    private String magnetUrl;
    private String fileSize; //We are not parsing.
    private String dateAdded; //we are not parsing.
    private String hash;
    private int seeds;
    private int peers;
    private int positiveVotes;
    private int negativeVotes;
    private HashMap<String,String> fileNameSizeMap;

    public TorrentInfo (
            String name,
            String magnetUrl,
            String fileSize,
            String dateAdded,
            String hash,
            int seeds,
            int peers,
            int positiveVotes,
            int negativeVotes,
            HashMap<String,String> fileNameSizeMap
    ) {
        this.name = name;
        this.magnetUrl = magnetUrl;
        this.fileSize = fileSize;
        this.dateAdded = dateAdded;
        this.hash = hash;
        this.seeds = seeds;
        this.peers = peers;
        this.negativeVotes = negativeVotes;
        this.positiveVotes = positiveVotes;
        this.fileNameSizeMap = fileNameSizeMap;
    }

    public String getName(){return this.name;}
    public String getMagnetUrl() {return this.magnetUrl;}
    public String getFileSize() {return this.fileSize;}
    public String getDateAdded() {return this.dateAdded;}
    public String getHash() {return this.hash;}
    public int getSeeds() {return this.seeds;}
    public int getPeers() {return this.peers;}
    public int getPositiveVotes() {return this.positiveVotes;}
    public int getNegativeVotes() {return this.negativeVotes;}
    public HashMap<String,String> getFileNameSizeMap() {return this.fileNameSizeMap;}
}
