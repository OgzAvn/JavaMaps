package com.oguz.javamaps;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity //Eğer bunu yazmak bu sınıf Room u kullanacağımı anlamaz.
public class Place implements Serializable {

    @PrimaryKey(autoGenerate = true) //bunu yaptığımızda bütün İD ler otomatikmen oluşturalacaktır.
    public int id;
    @ColumnInfo(name = "name")
    public String name;
    @ColumnInfo(name = "latitude")
    public double latitude;
    @ColumnInfo(name = "longitude")
    public double longitude;

    public Place(String name , double latitude,double longitude){
        this.name=name;
        this.latitude=latitude;
        this.longitude=longitude;
    }

}
