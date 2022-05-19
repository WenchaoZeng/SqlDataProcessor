package com.zwc.sqldataprocessor.importer;

import com.zwc.sqldataprocessor.entity.DataList;

public interface Importer {
    DataList doImport(byte[] content);
}
