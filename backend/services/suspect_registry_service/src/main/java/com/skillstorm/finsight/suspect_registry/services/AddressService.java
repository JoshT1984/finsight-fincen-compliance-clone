package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.skillstorm.finsight.suspect_registry.models.Address;
import com.skillstorm.finsight.suspect_registry.repositories.AddressRepository;

@Service
public class AddressService {
  
  private final AddressRepository addressRepository;

  public AddressService(AddressRepository addressRepository) {
    this.addressRepository = addressRepository;
  }

  public List<Address> getAllAddresses() {
    return addressRepository.findAll();
  }

  public Address getAddressById(long id) {
    return addressRepository.findById(id).orElse(null);
  }

  public Address createAddress(Address address) {
    return addressRepository.save(address);
  }
}
