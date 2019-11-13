package com.hussein.ubber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CustomerMapsActivityext extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

private GoogleMap mMap;
        GoogleApiClient googleApiClient;
        Location lastlocation;
        Button logout ,call;
        LocationRequest mLocationRequest;
        LatLng Pickuploc ;

@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        logout=findViewById(R.id.logout);
        call=findViewById(R.id.calluber);
        call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference reference = FirebaseDatabase.getInstance().
                                getReference("Request");
                        GeoFire geoFire = new GeoFire(reference);
                        geoFire.setLocation(userid,new GeoLocation(lastlocation.getLatitude(),lastlocation.getLongitude()));
                       Pickuploc=new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude());
                        mMap.addMarker(new MarkerOptions()
                                .position(Pickuploc)
                                .draggable(true)
                                .title("Pickup  Here"));
                        call.setText("Getting your driver ");
                        getclosestdriver();
                }
        });
        logout.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View view) {
        FirebaseAuth.getInstance().
        signOut();
        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
        }
        });
        }
boolean driverfound = false ;
String driverfoundid ;
int radius=1 ;
        private void getclosestdriver() {
                DatabaseReference reference = FirebaseDatabase.getInstance().
                        getReference().child("driverava");

                GeoFire geoFire = new GeoFire(reference);
                GeoQuery geoQuery =geoFire.queryAtLocation(new GeoLocation(Pickuploc.latitude,Pickuploc.longitude),radius);
                geoQuery.removeAllListeners();
                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                                if (!driverfound) {
                                        driverfound = true;
                                        driverfoundid=key;
                                        DatabaseReference reference = FirebaseDatabase.getInstance().
                                                getReference().child("Users").child("Drivers").child(driverfoundid);
                                        String customerid= FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        HashMap hashMap= new HashMap();
                                        hashMap.put("RiderId",customerid);
                                        reference.updateChildren(hashMap);
                                        getdriverloc();
                                        call.setText("Locking for Driver Location.....");
                                }
                        }

                        @Override
                        public void onKeyExited(String key) {

                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {

                        }

                        @Override
                        public void onGeoQueryReady() {
if (driverfound==false){
        radius++;
        getclosestdriver();
}
                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {

                        }
                });
        }
        Marker marker ;

        private void getdriverloc() {
                DatabaseReference reference = FirebaseDatabase.getInstance().
                        getReference().child("DriverWorking").child(driverfoundid).child("l");
                reference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
if (dataSnapshot.exists()){
        List<Objects> map = (List<Objects>) dataSnapshot.getValue();
        double lat =0 ;
        double longit = 0;
        call.setText("Driver Found");
        if (map.get(0)!=null){
                lat=Double.valueOf(map.get(0).toString());

        }
        if (map.get(1)!=null){

                longit=Double.valueOf(map.get(1).toString());
        }
        LatLng latLng= new LatLng(lat,longit);
        if (marker!=null) {
                marker.remove();
        }
        marker=mMap.addMarker(new MarkerOptions().position(latLng).title("Your Driver"));
}
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                });
        }


        /**
 * Manipulates the map once available.
 * This callback is triggered when the map is ready to be used.
 * This is where we can add markers or lines, add listeners or move the camera. In this case,
 * we just add a marker near Sydney, Australia.
 * If Google Play services is not installed on the device, the user will be prompted to install
 * it inside the SupportMapFragment. This method will only be triggered once the user has
 * installed Google Play services and returned to the app.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
@Override
public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // TODO: Consider calling
        //    Activity#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for Activity#requestPermissions for more details.

        return;
        }
        bulidgoogelapiclit();
        mMap.setMyLocationEnabled(true);
        }

protected synchronized void bulidgoogelapiclit() {
        googleApiClient=new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiClient.connect();
        }

@Override
public void onLocationChanged(Location location) {
        lastlocation=location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


        }



@RequiresApi(api = Build.VERSION_CODES.M)
@Override
public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // TODO: Consider calling
        //    Activity#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for Activity#requestPermissions for more details.
        return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }

@Override
public void onConnectionSuspended(int i) {

        }

@Override
public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

@Override
protected void onStop() {
        super.onStop();



        }
}
