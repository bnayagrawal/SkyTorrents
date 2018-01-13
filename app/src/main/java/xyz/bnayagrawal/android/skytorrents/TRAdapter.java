package xyz.bnayagrawal.android.skytorrents;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import xyz.bnayagrawal.android.skytorrents.Data.Torrent;

/**
 * Created by binay on 13/1/18.
 */

public class TRAdapter extends RecyclerView.Adapter<TRAdapter.ViewHolder> {
    private ArrayList<Torrent> torrents;
    private Context context;

    public TRAdapter(Context context, ArrayList<Torrent> torrents) {
        this.context = context;
        this.torrents = torrents;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.torrent_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String shortDescription;

        //cycle through each page
        holder.imgMagnetLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: Launch torrent detail activity
            }
        });

        holder.tvTorrentName.setText(torrents.get(position).getName());
        shortDescription = "Uploaded " + torrents.get(position).getDateAdded() + ", Files " + String.valueOf(torrents.get(position).getFileCount());
        holder.tvTorrentDesc.setText(shortDescription);
        holder.tvFileSize.setText(torrents.get(position).getFileSize());

        holder.tvSeeds.setText(
                context.getResources().getString(R.string.seeds)
                        .concat(" ")
                        .concat(String.valueOf(torrents.get(position).getSeeds()))
        );

        holder.tvPeers.setText(
                context.getResources().getString(R.string.peers)
                        .concat(" ")
                        .concat(String.valueOf(torrents.get(position).getPeers()))
        );
    }

    @Override
    public int getItemCount() {
        return torrents.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTorrentName;
        private TextView tvTorrentDesc;
        private TextView tvFileSize;
        private TextView tvSeeds;
        private TextView tvPeers;
        private ImageView imgMagnetLink;

        public ViewHolder(View view) {
            super(view);
            tvTorrentName = view.findViewById(R.id.tv_torrent_name);
            tvTorrentDesc = view.findViewById(R.id.tv_torrent_desc);
            tvFileSize = view.findViewById(R.id.tv_torrent_size);
            tvSeeds = view.findViewById(R.id.tv_torrent_seeds);
            tvPeers = view.findViewById(R.id.tv_torrent_peers);
            imgMagnetLink = view.findViewById(R.id.img_magnet_link);
        }
    }
}
