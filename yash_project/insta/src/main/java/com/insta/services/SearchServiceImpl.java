package com.insta.services;

import com.insta.dto.*;
import com.insta.interfaces.*;

import java.util.*;

public class SearchServiceImpl implements SearchService {
    @Override
    public List<Profile> searchProfile(String searchKey) {
        Map<String, Profile> profiles= SearchCatalog.getInstance().getProfiles();
        return null;
    }
}
