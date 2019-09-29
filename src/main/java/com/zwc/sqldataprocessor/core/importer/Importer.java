package com.zwc.sqldataprocessor.core.importer;

import com.zwc.sqldataprocessor.core.entity.DataList;

public interface Importer {
    DataList doImport(byte[] content);
}
