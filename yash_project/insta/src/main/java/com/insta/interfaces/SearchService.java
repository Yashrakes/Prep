package com.insta.interfaces;

import com.insta.dto.Profile;

import java.util.List;

public interface SearchService {
    List<Profile> searchProfile(String searchKey);
}
