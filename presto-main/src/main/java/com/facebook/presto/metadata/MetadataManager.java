package com.facebook.presto.metadata;

import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.tuple.TupleInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class MetadataManager
    implements Metadata
{
    private final Map<DataSourceType, Metadata> metadataSourceMap;

    @Inject
    public MetadataManager(NativeMetadata nativeMetadata, ImportMetadata importMetadata)
    {
        metadataSourceMap = ImmutableMap.<DataSourceType, Metadata>builder()
                .put(DataSourceType.NATIVE, checkNotNull(nativeMetadata, "nativeMetadata is null"))
                .put(DataSourceType.IMPORT, checkNotNull(importMetadata, "importMetadata is null"))
                .build();
    }

    @Override
    public FunctionInfo getFunction(QualifiedName name, List<TupleInfo.Type> parameterTypes)
    {
        checkNotNull(name, "name is null");
        checkNotNull(parameterTypes, "parameterTypes is null");

        // Only use NATIVE
        return metadataSourceMap.get(DataSourceType.NATIVE).getFunction(name, parameterTypes);
    }

    @Override
    public FunctionInfo getFunction(FunctionHandle handle)
    {
        Preconditions.checkNotNull(handle, "handle is null");

        return metadataSourceMap.get(DataSourceType.NATIVE).getFunction(handle);
    }

    @Override
    public TableMetadata getTable(String catalogName, String schemaName, String tableName)
    {
        checkNotNull(catalogName, "catalogName is null");
        checkNotNull(schemaName, "schemaName is null");
        checkNotNull(tableName, "tableName is null");

        DataSourceType dataSourceType = convertCatalogToDataSourceType(catalogName);
        return lookup(dataSourceType).getTable(catalogName, schemaName, tableName);
    }

    @Override
    public void createTable(TableMetadata table)
    {
        checkNotNull(table, "table is null");
        // Only use NATIVE
        metadataSourceMap.get(DataSourceType.NATIVE).createTable(table);
    }

    // Temporary placeholder to determine data source from catalog
    private DataSourceType convertCatalogToDataSourceType(String catalogName)
    {
        if (catalogName.equalsIgnoreCase("default")) {
            return DataSourceType.NATIVE;
        }
        else {
            return DataSourceType.IMPORT;
        }
    }

    private Metadata lookup(DataSourceType dataSourceType)
    {
        checkNotNull(dataSourceType, "dataSourceHandle is null");

        Metadata metadata = metadataSourceMap.get(dataSourceType);
        checkArgument(metadata != null, "tableMetadataSource does not exist: %s", dataSourceType);
        return metadata;
    }
}