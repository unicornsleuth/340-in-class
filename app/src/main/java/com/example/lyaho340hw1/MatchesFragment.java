package com.example.lyaho340hw1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class MatchesFragment extends Fragment {

    private MatchViewModel vm;
    private LocationManager locationManager;
    public double longitudeGPS, latitudeGPS;
    private ContentAdapter adapter;
    private int maxDistance = 10;

    private static final String TAG = MatchesFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = getContext();
        ScrollView scrollView = (ScrollView) inflater.inflate(
                R.layout.recycler_view, container, false);

        RecyclerView recyclerView = (RecyclerView) scrollView.findViewById(R.id.my_recycler_view);
//        RecyclerView recyclerView = (RecyclerView) inflater.inflate(
//                R.layout.recycler_view, container, false);

        vm = new MatchViewModel();
        adapter = new ContentAdapter(recyclerView.getContext(), vm);
        recyclerView.setAdapter(adapter);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        toggleNetworkUpdates();

        Log.d(TAG, "onCreateView invoked");
        return recyclerView;
    }

    private boolean checkLocation() {
        if(!isLocationEnabled()) {
            showAlert();
        }
        Log.e(TAG, "checkLocation invoked");
        return isLocationEnabled();
    }

    private boolean isLocationEnabled() {
        Log.e(TAG, "isLocationEnabled invoked");
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(R.string.enable_location)
                .setMessage(getString(R.string.enable_location_message))
                .setPositiveButton(R.string.location_settings, (paramDialogInterface, paramInt) -> {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                })
                .setNegativeButton(R.string.location_cancel, (paramDialogInterface, paramInt) -> {});
        dialog.show();
    }

    // this turns location on/off - set as property to start/stop button (takes View view)
    public void toggleNetworkUpdates() {
        Context context = getContext();
        if(!checkLocation()) {
            return;
        }
//        Button button = (Button) view;
//        if(button.getText().equals(getResources().getString(R.string.pause))) {
//            locationManager.removeUpdates(locationListenerNetwork);
//            button.setText(R.string.resume);
//        }
//        else {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 10, locationListenerNetwork);

                Log.e(TAG, "network provider started running");
                //button.setText(R.string.pause);
            }
//        }
        Log.e(TAG, "toggleNetworkUpdates invoked");
    }

    private final LocationListener locationListenerNetwork = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            longitudeGPS = location.getLongitude();
            latitudeGPS = location.getLatitude();
            Activity activity = getActivity();
            activity.runOnUiThread(() -> {
                // Change Display
                adapter.filterArrayByDistance(latitudeGPS, longitudeGPS, maxDistance);
                Log.e(TAG, "onLocationChanged invoked");
                Toast.makeText(activity, "showing matches within 10 miles", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    @Override
    public void onPause() {
        vm.clear();
        super.onPause();
    }

    // this is for what happens in each "match"
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView image;
        public ImageButton likeButton;
        public TextView matchName;
        public ViewGroup parent;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.fragment_matches, parent, false));
            this.parent = parent;
            image = (ImageView) itemView.findViewById(R.id.match_picture);
            likeButton = (ImageButton) itemView.findViewById(R.id.like_button);
            matchName = (TextView) itemView.findViewById(R.id.match_name);
        }


    }

    public static class ContentAdapter extends RecyclerView.Adapter<ViewHolder> {
        private MatchViewModel vm;
        private ArrayList<Match> matchList;

        public ContentAdapter(Context context, MatchViewModel viewModel) {
            this.vm = viewModel;
            matchList = new ArrayList<>();
            context.getResources();
            vm.getMatches((ArrayList<Match> matches) -> {
                for (Match match : matches) {
                    if (!matchList.contains(match)) {
                        matchList.add(match);
                    }
                }
                notifyDataSetChanged();
            });
        }

        public boolean gpsIsWithinDistance(double distance, double startLat, double startLong, Match match) {
            float[] results = new float[1];
            Location.distanceBetween(startLat, startLong, match.getLatitude(), match.getLongitude(), results);
            results[0] = (float) (results[0]/1609.344);
            Log.e(TAG, results[0] + " <= " + distance + "?");
            return results[0] <= distance;
        }

        public void filterArrayByDistance(double startLat, double startLong, double distance) {
            vm.clear();
            vm.getMatches((ArrayList<Match> matches) -> {
                for (Match match : matches) {
                    if (!matchList.contains(match)
                            && gpsIsWithinDistance(distance, startLat, startLong, match)) {
                        matchList.add(match);
                    }
                }
            });
            Log.e(TAG, "filterArrayByDistance invoked");
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.d(TAG, matchList.get(0).getImageUrl());
            Match currentMatch;
            if (matchList != null) {
                currentMatch = matchList.get(position % matchList.size());
                // CONDITIONAL RENDERING
            //    if (currentMatch).getLatitude()
                Log.d(TAG, matchList.get(0).getImageUrl());
                Picasso.get().load(currentMatch.getImageUrl()).into(holder.image);
                holder.matchName.setText(currentMatch.getName());
            } else { currentMatch = new Match(); }
            holder.likeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Context context = holder.parent.getContext();
                    StringBuilder text;
                    if (holder.likeButton.getContentDescription().equals(context.getString(R.string.liked))) {
                        holder.likeButton.setImageResource(R.drawable.ic_favorite_border);
                        holder.likeButton.setContentDescription(context.getString(R.string.not_liked));
                        currentMatch.setLiked(false);
                        text = new StringBuilder("You unliked ");
                    } else {
                        holder.likeButton.setImageResource(R.drawable.ic_favorite_fill);
                        holder.likeButton.setContentDescription(context.getString(R.string.liked));
                        currentMatch.setLiked(true);
                        text = new StringBuilder("You liked ");
                    }

                    text.append(holder.matchName.getText());
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return matchList.size();
        }
    }
}
