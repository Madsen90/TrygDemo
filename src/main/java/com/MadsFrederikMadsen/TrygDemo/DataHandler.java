package com.MadsFrederikMadsen.TrygDemo;

public interface DataHandler{

  public abstract KeyTimeRecord getRecord(String key, long timestamp);
  public abstract KeyTimeRecord createRecord(KeyTimeRecord newRecord);
  public abstract KeyTimeRecord updateRecord(KeyTimeRecord updatedRecord);
  public abstract boolean deleteRecord(String key, long timestamp);
}