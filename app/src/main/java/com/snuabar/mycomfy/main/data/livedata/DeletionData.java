package com.snuabar.mycomfy.main.data.livedata;

import java.util.Set;

public class DeletionData extends SelectionData {
    public final boolean includeAssociated;

    public DeletionData(Set<String> modelIdSet, boolean includeAssociated) {
        super(modelIdSet);
        this.includeAssociated = includeAssociated;
    }
}
