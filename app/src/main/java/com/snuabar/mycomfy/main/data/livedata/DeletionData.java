package com.snuabar.mycomfy.main.data.livedata;

import java.util.Set;

public class DeletionData {
    public final Set<String> modelIdSet;
    public final boolean includeAssociated;

    public DeletionData(Set<String> modelIdSet, boolean includeAssociated) {
        this.modelIdSet = modelIdSet;
        this.includeAssociated = includeAssociated;
    }
}
