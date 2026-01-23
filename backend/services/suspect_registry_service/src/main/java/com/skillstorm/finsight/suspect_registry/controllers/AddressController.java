package com.skillstorm.finsight.suspect_registry.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.suspect_registry.models.Address;
import com.skillstorm.finsight.suspect_registry.services.AddressService;

@RestController
@RequestMapping("/address")
public class AddressController {
  
  private final AddressService addressService;

  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  @GetMapping
  public ResponseEntity<List<Address>> getAllAddresses() {
    try {
      List<Address> addresses = addressService.getAllAddresses();
      return ResponseEntity.ok(addresses);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<Address> getAddressById(@PathVariable long id) {
    try {
      Address address = addressService.getAddressById(id);
      if (address != null) {
        return ResponseEntity.ok(address);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @PostMapping
  public ResponseEntity<Address> createAddress(Address address) {
    try {
      Address createdAddress = addressService.createAddress(address);
      return ResponseEntity.status(201).body(createdAddress);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }
}
