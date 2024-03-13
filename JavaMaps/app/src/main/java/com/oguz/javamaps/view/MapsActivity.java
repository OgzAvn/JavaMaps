package com.oguz.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.oguz.javamaps.Place;
import com.oguz.javamaps.R;
import com.oguz.javamaps.databinding.ActivityMapsBinding;
import com.oguz.javamaps.roomdb.PlaceDao;
import com.oguz.javamaps.roomdb.PlaceDatabase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener{

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    boolean info;

    LocationManager locationManager;
    LocationListener locationListener;
    ActivityResultLauncher<String> permissionLauncher;
    SharedPreferences sharedPreferences;

    PlaceDatabase db;
    PlaceDao placeDao;

    double selectedLatitude;
    double selectedLongitude;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    Place selectedPlace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();

        sharedPreferences = MapsActivity.this.getSharedPreferences("com.oguz.javamaps",MODE_PRIVATE);
        info =false;

        db = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places")
                //.allowMainThreadQueries() //MainThread de işlem yapmasına izin ver diyebilirim.//Room database önplanda çalışılmasını istemiyor.
                .build();
        placeDao = db.placeDao();

        selectedLatitude=0;
        selectedLongitude=0;

        binding.saveButton.setEnabled(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);//Listener istiyor listenerimiz arayüzün kendisi

        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");

        if(intentInfo.equals("new")){

            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            // LocationManager->Konum servislerine erişim sağlıyor.Bunları kullanarak kullanıcının konumu hakkında işlem yapabiliyoruz.
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                // LocationListener(Konum dinleyicisi)-> bu arayüz locationmanager den konumumuzun değiştiğine dair uyarıları alabilmek için kullandığımız bir arayüz
                @Override
                public void onLocationChanged(@NonNull Location location) { //Konum değişince ne olacağını yazıyoruz.
                    //System.out.println("location :" + location.toString());
                    //LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));

                    //Sadece 1 defe çalıştır uygulama açıkken

                    //SharedPreferences in içinde bir bilgi kaydedeceğim bir defa çalıştırıldı mı çalıştırılmadı mı?
                    info = sharedPreferences.getBoolean("info",false);
                    if(!info){
                        LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                        sharedPreferences.edit().putBoolean("info",true).apply();//Ve buraya true kaydederim ve bir daha çağrılmaz.
                    }


                }

            };
            //Konum değiştiğinde bize bildir diyebiliriz fakat bunun için kullanıcıdan izin istememiz gerekiyor.
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.getRoot(),"Permission needed for maps",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                }else{
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    //request permission
                }
            }else{ //Burada izni var.Ve konumu alabiliriz.
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                //Hemen burada son bilinen location almaya çalışabiliriz.
                Location lastlocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
                if(lastlocation != null){//Gerçekten bir konum geliyorsa
                    LatLng lastUserlocation = new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserlocation,15));
                }
                mMap.setMyLocationEnabled(true); // Konumunda mavi nokta gösteriyor.
            }


        }else{

            mMap.clear();
            selectedPlace = (Place) intent.getSerializableExtra("place");
            LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

            binding.placeNameText.setText(selectedPlace.name);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);

        }



        //Lat--> Latitude eylem demeK //lon --> Longitude boylam demek
        //48.8584463, 2.294771


    }

    private void registerLauncher(){
        permissionLauncher= registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {
                if(o){
                    if(ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        //Permission granted
                        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                        Location lastlocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
                        if (lastlocation != null) {//Gerçekten bir konum geliyorsa
                            LatLng lastUserlocation = new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserlocation, 15));

                        }
                    }
                }else{
                    //Permission denied
                    Toast.makeText(MapsActivity.this, "Permission needed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        //1.Kullanmadan önce onmap method u altında mMap.setonaplongclicklistener dememiz gerekiyor ve bir listener vermemiz gerekiyor.
        mMap.clear();//Önceden tıklananları sil demek
        mMap.addMarker(new MarkerOptions().position(latLng));

        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;

        binding.saveButton.setEnabled(true);
    }

    public void save(View view){

        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);

        //threading(işem yapılan yerler) -- > Main(UI) , Default Thread (CPU(yoğun işlemler) Intensive) , (IO (input-output), database)

        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();//Bunu io thread de yap diyorum

        //disposable//kullan at çöp torbası gibi geçmişi sil
        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io()) //işlemleri io da yap
                .observeOn(AndroidSchedulers.mainThread()) //mainThread de gözlemle
                .subscribe(MapsActivity.this::handleResponse) //Subscribe diyerek başlatıyoruz.
        );

    }

    private void handleResponse(){
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void delete(View view){

        if(selectedPlace!=null) {
            compositeDisposable.add(placeDao.delete(selectedPlace)
                    .observeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this::handleResponse)
            );

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}