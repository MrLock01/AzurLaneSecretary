package com.axcdevelopment.azurlanesecretary;

import android.net.Uri;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class Shipfu implements Comparable{

    private String name;
    private ArrayList<String> skins;
    private ArrayList<Uri> chibi;
    private ArrayList<Uri> standard;

    public ArrayList<Uri> getStandard() {
        return standard;
    }

    public void setStandard(ArrayList<Uri> standard) {
        this.standard = standard;
    }

    private ArrayList<Uri> voiceLines;

    public Shipfu() {
        this.name = "";
        this.skins = new ArrayList<>();
        this.chibi = new ArrayList<>();
        this.voiceLines = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getSkins() {
        return skins;
    }

    public void setSkins(ArrayList<String> skins) {
        this.skins = skins;
    }

    public ArrayList<Uri> getChibi() {
        return chibi;
    }

    public void setChibi(ArrayList<Uri> chibi) {
        this.chibi = chibi;
    }

    public ArrayList<Uri> getVoiceLines() {
        return voiceLines;
    }

    public void setVoiceLines(ArrayList<Uri> voiceLines) {
        this.voiceLines = voiceLines;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Shipfu) {
            Shipfu x = (Shipfu) o;
            return name.compareTo(x.getName());
        }
        return 0;
    }
}
