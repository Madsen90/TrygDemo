package com.MadsFrederikMadsen.TrygDemo;

import java.net.URI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;

@SpringBootApplication
@RestController
public class KeyTimeServer {

  private DataHandler datahandler = FileHandler.getHandler();


  private ResponseEntity<KeyTimeRecord> createdResponse(KeyTimeRecord createdRecord){
     URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
          .path("/")
          .queryParam("key",createdRecord.key())
          .queryParam("timestamp", createdRecord.timestamp())
          .build()
          .toUri();

    return ResponseEntity.created(uri).body(createdRecord);
  }


  @GetMapping("/")
  public ResponseEntity<KeyTimeRecord> get(@RequestParam String key, @RequestParam long timestamp) {
    var foundRecord = datahandler.getRecord(key, timestamp);
    if(foundRecord == null)
      return ResponseEntity.notFound().build();
    
    return ResponseEntity.ok(foundRecord);
  }

  @PostMapping("/")
  public ResponseEntity<KeyTimeRecord> post(@RequestBody KeyTimeRecord record) {
    var createdRecord = datahandler.createRecord(record);
    if(createdRecord == null)
      return ResponseEntity.status(409).build(); // 409: conflict
    
    return createdResponse(createdRecord);
   
  }

  @PutMapping("/")
  public ResponseEntity<KeyTimeRecord> put(@RequestBody KeyTimeRecord record) {
    var foundRecord = datahandler.getRecord(record.key(), record.timestamp());
    var updatedRecord = datahandler.updateRecord(record);
    if(foundRecord == null)
      return createdResponse(updatedRecord);

    return ResponseEntity.ok(updatedRecord);
  }
  
  @DeleteMapping("/")
  public ResponseEntity delete(@RequestParam String key, @RequestParam long timestamp) {
    if(datahandler.deleteRecord(key, timestamp))
      return ResponseEntity.ok().build();

    return ResponseEntity.notFound().build();
  }
  
}