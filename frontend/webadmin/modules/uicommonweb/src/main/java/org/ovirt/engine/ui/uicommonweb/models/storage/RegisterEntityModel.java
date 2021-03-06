package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QueryParametersBase;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;

public abstract class RegisterEntityModel<T, E extends ImportEntityData<T>> extends Model {

    private UICommand cancelCommand;
    private ListModel<E> entities;
    private ListModel<Cluster> cluster;
    private EntityModel<Map<Guid, List<Quota>>> clusterQuotasMap;
    private EntityModel<Map<Guid, Quota>> diskQuotaMap;
    private ListModel<Quota> storageQuota;
    private Guid storageDomainId;
    private StoragePool storagePool;

    public RegisterEntityModel() {
        setEntities(new ListModel<E>());
        setCluster(new ListModel<Cluster>());

        setClusterQuotasMap(new EntityModel<Map<Guid, List<Quota>>>());
        getClusterQuotasMap().setEntity(new HashMap<Guid, List<Quota>>());
        setDiskQuotaMap(new EntityModel<Map<Guid, Quota>>());
        getDiskQuotaMap().setEntity(new HashMap<Guid, Quota>());
        setStorageQuota(new ListModel<Quota>());
    }

    protected abstract void onSave();

    @Override
    public void initialize() {
        super.initialize();

        // Create and set commands
        UICommand onSaveCommand = UICommand.createDefaultOkUiCommand("OnSave", this); //$NON-NLS-1$
        getCommands().add(onSaveCommand);
        getCommands().add(getCancelCommand());

        updateClusters();
    }

    private void updateClusters() {
        AsyncDataProvider.getInstance().getDataCentersByStorageDomain(new AsyncQuery<>(storagePools -> {
            storagePool = storagePools.size() > 0 ? storagePools.get(0) : null;
            if (storagePool == null) {
                return;
            }

            AsyncDataProvider.getInstance().getClusterByServiceList(new AsyncQuery<>(clusters -> {
                for (ImportEntityData<T> entityData : entities.getItems()) {
                    List<Cluster> filteredClusters = AsyncDataProvider.getInstance().filterByArchitecture(clusters, entityData.getArchType());
                    entityData.getCluster().setItems(filteredClusters);
                    entityData.getCluster().setSelectedItem(Linq.firstOrNull(filteredClusters));
                }

                getCluster().setItems(clusters);
                getCluster().setSelectedItem(Linq.firstOrNull(clusters));

                updateClusterQuota(clusters);
                updateStorageQuota();
            }), storagePool.getId(), true, false);

        }), storageDomainId);
    }

    private void updateStorageQuota() {
        if (!isQuotaEnabled()) {
            return;
        }

        AsyncDataProvider.getInstance().getAllRelevantQuotasForStorageSorted(new AsyncQuery<>(
                quotas -> {
                    quotas = (quotas != null) ? quotas : new ArrayList<Quota>();

                    getStorageQuota().setItems(quotas);
                    getStorageQuota().setSelectedItem(Linq.firstOrNull(quotas));
                }), storageDomainId, null);
    }

    private void updateClusterQuota(List<Cluster> clusters) {
        if (!isQuotaEnabled()) {
            return;
        }

        List<QueryType> queries = new ArrayList<>();
        List<QueryParametersBase> params = new ArrayList<>();
        for (Cluster cluster : clusters) {
            queries.add(QueryType.GetAllRelevantQuotasForCluster);
            params.add(new IdQueryParameters(cluster.getId()));
        }

        Frontend.getInstance().runMultipleQueries(queries, params, result -> {
            Map<Guid, List<Quota>> clusterQuotasMap = new HashMap<>();
            for (int i = 0; i < result.getReturnValues().size(); i++) {
                List<Quota> quotas = result.getReturnValues().get(i).getReturnValue();
                Guid clusterId = ((IdQueryParameters) result.getParameters().get(i)).getId();

                clusterQuotasMap.put(clusterId, quotas);
            }
            getClusterQuotasMap().setEntity(clusterQuotasMap);
        });
    }

    public void selectQuotaByName(String name, ListModel<Quota> listModel) {
        for (Quota quota : listModel.getItems()) {
            if (quota.getQuotaName().equals(name)) {
                listModel.setSelectedItem(quota);
                break;
            }
        }
    }

    public List<String> getQuotaNames(List<Quota> quotas) {
        List<String> names = new ArrayList<>();
        if (quotas != null) {
            for (Quota quota : quotas) {
                names.add(quota.getQuotaName());
            }
        }
        return names;
    }

    public Quota getQuotaByName(String name, List<Quota> quotas) {
        for (Quota quota : quotas) {
            if (quota.getQuotaName().equals(name)) {
                return quota;
            }
        }
        return null;
    }

    protected void updateDiskQuotas(List<Disk> disks) {
        for (Disk disk : disks) {
            Quota quota = getDiskQuotaMap().getEntity().get(disk.getId());
            if (quota == null) {
                quota = getStorageQuota().getSelectedItem();
            }
            if (quota != null) {
                ((DiskImage) disk).setQuotaId(quota.getId());
            }
        }
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if ("OnSave".equals(command.getName())) { //$NON-NLS-1$
            onSave();
        }
    }

    protected void cancel() {
        getCancelCommand().execute();
    }

    @Override
    public UICommand getCancelCommand() {
        return cancelCommand;
    }

    public void setCancelCommand(UICommand cancelCommand) {
        this.cancelCommand = cancelCommand;
    }

    public ListModel<E> getEntities() {
        return entities;
    }

    public void setEntities(ListModel<E> entities) {
        this.entities = entities;
    }

    public ListModel<Cluster> getCluster() {
        return cluster;
    }

    private void setCluster(ListModel<Cluster> value) {
        cluster = value;
    }

    public Guid getStorageDomainId() {
        return storageDomainId;
    }

    public void setStorageDomainId(Guid storageDomainId) {
        this.storageDomainId = storageDomainId;
    }

    public EntityModel<Map<Guid, List<Quota>>> getClusterQuotasMap() {
        return clusterQuotasMap;
    }

    public void setClusterQuotasMap(EntityModel<Map<Guid, List<Quota>>> clusterQuotasMap) {
        this.clusterQuotasMap = clusterQuotasMap;
    }

    public EntityModel<Map<Guid, Quota>> getDiskQuotaMap() {
        return diskQuotaMap;
    }

    public void setDiskQuotaMap(EntityModel<Map<Guid, Quota>> diskQuotaMap) {
        this.diskQuotaMap = diskQuotaMap;
    }

    public ListModel<Quota> getStorageQuota() {
        return storageQuota;
    }

    public void setStorageQuota(ListModel<Quota> storageQuota) {
        this.storageQuota = storageQuota;
    }

    public boolean isQuotaEnabled() {
        return storagePool.getQuotaEnforcementType() != QuotaEnforcementTypeEnum.DISABLED;
    }
}
