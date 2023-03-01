package com.MadsFrederikMadsen.TrygDemo;

import java.util.HashMap;
import java.util.Scanner;
import java.nio.file.*;
import java.io.IOException;
import com.opencsv.*;
import com.opencsv.exceptions.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class FileHandler implements DataHandler{
  private static volatile FileHandler singleton;
  private volatile HashMap<KeyTimeKey, KeyTimeRecord> dataStore;
  private final ReentrantReadWriteLock.ReadLock readLock;
  private final ReentrantReadWriteLock.WriteLock writeLock;

  // Synchronized in case Spring Boot initialises multiple KeyTimeServers
  public static synchronized FileHandler getHandler(){
    if(singleton == null)
      singleton = new FileHandler();
    return singleton;
  }

  private FileHandler(){
    synchronized(FileHandler.class){ // Nice to have if singleton pattern is removed
      var readWriteLock = new ReentrantReadWriteLock();
      readLock = readWriteLock.readLock();
      writeLock = readWriteLock.writeLock();
      readFile();
    }
  }

  // Initialises datastore, and loads data
  private synchronized void readFile(){ // synchronization is redundant due to singleton pattern, but method is not necessarily threadsafe by virtue of readwritelock.
    try{
      writeLock.lock(); // necessary to prevent concurrent reads while data is loaded

      dataStore = new HashMap<>();
      if(!Files.exists(Paths.get("datastore.csv"))){
        return; //don't read non-existing file.
      }

      var reader = new CSVReader(Files.newBufferedReader(Paths.get("datastore.csv")));
      var lines = reader.readAll();
      for (String[] recordTriplet : lines) {
        var timestamp = Long.parseLong(recordTriplet[1]);
        dataStore.put(new KeyTimeKey(recordTriplet[0], timestamp), 
          new KeyTimeRecord(recordTriplet[0], timestamp, recordTriplet[2]));
      }

    }catch(IOException | CsvException e){
      System.out.println(e); //TODO better failure handling
      throw new RuntimeException("Exiting due to IO failure");
    }
    catch(NumberFormatException | IndexOutOfBoundsException e){
      System.out.println(e); //TODO better failure handling
      throw new RuntimeException("Error in data");
    }
    finally {
      writeLock.unlock();
    }
  }

  // Overrides file with current data
  private synchronized void writeFile(){ // synchronization is redundant due to write lock, but necessary if used elsewhere
    try{
      //wipe and create new empty file 
      Files.deleteIfExists(Paths.get("datastore.csv"));
      Files.createFile(Paths.get("datastore.csv"));

      //write entire datastore to csv
      var writer = new CSVWriter(Files.newBufferedWriter(Paths.get("datastore.csv")));
      for (var record : dataStore.values()) {
        String[] recordTriplet = new String[3];
        recordTriplet[0] = record.key();
        recordTriplet[1] = Long.toString(record.timestamp());
        recordTriplet[2] = record.value();
        writer.writeNext(recordTriplet);
      }

      writer.close();

    }catch(IOException e){
      System.out.println(e); //TODO better failure handling
      throw new RuntimeException("Exiting due to IO failure");
    }
  }

  @Override
  public KeyTimeRecord getRecord(String key, long timestamp){
    readLock.lock();
    try{ 
      return dataStore.get(new KeyTimeKey(key, timestamp));
    }
    finally{ readLock.unlock(); }
  }

  // returns null if the record already exists
  @Override
  public KeyTimeRecord createRecord(KeyTimeRecord newRecord){
    writeLock.lock();
    try{
      var key = new KeyTimeKey(newRecord.key(),newRecord.timestamp());
      if(dataStore.containsKey(key))
        return null;

      dataStore.put(key, newRecord);
      writeFile(); // note that this is very inefficient, better to just append to the file
      return newRecord;
    }
    finally{ writeLock.unlock(); }
  }

  @Override
  public KeyTimeRecord updateRecord(KeyTimeRecord updatedRecord){
    writeLock.lock();
    try{ 
      var key = new KeyTimeKey(updatedRecord.key(),updatedRecord.timestamp());
      dataStore.put(key, updatedRecord);
      writeFile();
      return updatedRecord;
    }
    finally{ writeLock.unlock(); }
  }
  
  @Override
  public boolean deleteRecord(String keyString, long timestamp){
    writeLock.lock();
    try{ 
      var key = new KeyTimeKey(keyString,timestamp);
      if(!dataStore.containsKey(key))
        return false;

      dataStore.remove(key);
      writeFile();
      return true;
    }
    finally{ writeLock.unlock(); }
  }

  private record KeyTimeKey(String key, long timestamp) { }

}