package com.insta.services;

import com.insta.dto.*;
import com.insta.interfaces.*;

public class ProfileServiceImpl implements ProfileService {
    @Override
    public void createProfile(String email, String password, String id) {
        Profile profile= new Profile(email, password, id);
        SearchCatalog searchCatalog= SearchCatalog.getInstance();
        searchCatalog.getProfiles().put(id, profile);
    }
}